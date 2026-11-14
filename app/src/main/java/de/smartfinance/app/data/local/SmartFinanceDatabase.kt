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
        QuickTemplateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SmartFinanceDatabase : RoomDatabase() {

    abstract fun smartFinanceDao(): SmartFinanceDao

    companion object {
        @Volatile
        private var INSTANCE: SmartFinanceDatabase? = null

        /**
         * Vorbereitete verlustfreie Beispiel-Migration von Version 1 auf Version 2,
         * damit zukünftige Schema-Updates niemals bestehende Tabellen löschen.
         */
        val SAFE_MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Verlustfreies Schema-Upgrade ohne Datenverlust
            }
        }

        fun getInstance(context: Context): SmartFinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SmartFinanceDatabase::class.java,
                    "smartfinance_2026.db"
                )
                    .addMigrations(SAFE_MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
