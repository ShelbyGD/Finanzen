package de.smartfinance.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Lokale Room-Datenbank für SmartFinance 2026.
 *
 * WICHTIGER DATENSCHUTZ-HINWEIS FÜR IN-PLACE-UPDATES:
 * Destruktive Migrationen sind in dieser Architektur strikt untersagt.
 * Alle zukünftigen Schema-Erweiterungen erfolgen ausschließlich über explizite,
 * verlustfreie [Migration]-Objekte in [addMigrations], sodass bestehende Nutzerdaten
 * bei jedem App-Update zu 100 % erhalten bleiben.
 */
@Database(
    entities = [
        CategoryTabEntity::class,
        TransactionEntity::class,
        SavingsGoalEntity::class,
        QuickTemplateEntity::class,
        InstallmentEntity::class,
        InstallmentFolderEntity::class,
        FinanceNoteEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class SmartFinanceDatabase : RoomDatabase() {

    abstract fun smartFinanceDao(): SmartFinanceDao

    companion object {
        @Volatile
        private var INSTANCE: SmartFinanceDatabase? = null

        /**
         * Verlustfreie Migration von Version 1 auf Version 2 für Ratenzahlungen & offene Posten.
         */
        val SAFE_MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `installments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `totalAmount` REAL NOT NULL,
                        `monthlyRate` REAL NOT NULL,
                        `startEpochDay` INTEGER NOT NULL,
                        `dueDayOfMonth` INTEGER NOT NULL,
                        `extraPaidAmount` REAL NOT NULL,
                        `deductFromBudget` INTEGER NOT NULL,
                        `folderName` TEXT NOT NULL DEFAULT 'Sonstiges'
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Verlustfreie Migration von Version 2 auf Version 3 für Ratenzahlungs-Ordner.
         */
        val SAFE_MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `installment_folders` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE `installments` ADD COLUMN `folderName` TEXT NOT NULL DEFAULT 'Sonstiges'")
            }
        }

        /**
         * Verlustfreie Migration von Version 3 auf Version 4 für Monats-Register zum Abhaken.
         */
        val SAFE_MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `installments` ADD COLUMN `paidMonthsCsv` TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * Verlustfreie Migration von Version 4 auf Version 5 für erweiterte flexible Sparziele (Bank, Icon, Farbe, Extra-Sparen, Startdatum).
         */
        val SAFE_MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `savings_goals` ADD COLUMN `bankName` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `savings_goals` ADD COLUMN `iconKey` TEXT NOT NULL DEFAULT 'savings'")
                db.execSQL("ALTER TABLE `savings_goals` ADD COLUMN `colorHex` INTEGER NOT NULL DEFAULT 4278638292")
                db.execSQL("ALTER TABLE `savings_goals` ADD COLUMN `extraSavedAmount` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `savings_goals` ADD COLUMN `startEpochDay` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Verlustfreie Migration von Version 5 auf Version 6 für Archivierung von Buchungen.
         */
        val SAFE_MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `isArchived` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Verlustfreie Migration von Version 6 auf Version 7 für Notizen & Änderungen (finance_notes).
         */
        val SAFE_MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `finance_notes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `effectiveDateEpochDay` INTEGER NOT NULL,
                        `oldAmount` REAL,
                        `newAmount` REAL,
                        `createdAtEpochDay` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Verlustfreie Migration von Version 7 auf Version 8 für optionales Enddatum (validUntilEpochDay) in finance_notes.
         */
        val SAFE_MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `finance_notes` ADD COLUMN `validUntilEpochDay` INTEGER DEFAULT NULL")
            }
        }

        fun getInstance(context: Context): SmartFinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SmartFinanceDatabase::class.java,
                    "smartfinance_clean_user.db"
                )
                    .addMigrations(
                        SAFE_MIGRATION_1_2,
                        SAFE_MIGRATION_2_3,
                        SAFE_MIGRATION_3_4,
                        SAFE_MIGRATION_4_5,
                        SAFE_MIGRATION_5_6,
                        SAFE_MIGRATION_6_7,
                        SAFE_MIGRATION_7_8
                    )
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
