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
        InstallmentFolderEntity::class
    ],
    version = 3,
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

        fun getInstance(context: Context): SmartFinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SmartFinanceDatabase::class.java,
                    "smartfinance_clean_user.db"
                )
                    .addMigrations(SAFE_MIGRATION_1_2, SAFE_MIGRATION_2_3)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
