package com.example.jobtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [JobApplication::class], version = 3, exportSchema = false)
abstract class JobDatabase : RoomDatabase() {
    abstract fun jobDao(): JobDao

    companion object {
        @Volatile
        private var INSTANCE: JobDatabase? = null

        // Migration from version 1 to 2 - Add dateApplied and applicationMethod columns
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // These columns might already exist, so we'll use a try-catch
                try {
                    database.execSQL("ALTER TABLE job_table ADD COLUMN dateApplied TEXT DEFAULT '01/01/1970'")
                } catch (e: Exception) {
                    // Column might already exist
                }
                try {
                    database.execSQL("ALTER TABLE job_table ADD COLUMN applicationMethod TEXT DEFAULT 'COMPANY_WEBSITE'")
                } catch (e: Exception) {
                    // Column might already exist
                }
            }
        }

        // Migration from version 2 to 3 - Handle any potential issues
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // This is a no-op migration to handle any potential issues
            }
        }

        fun getDatabase(context: Context): JobDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JobDatabase::class.java,
                    "job_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration() // This will recreate the database if migration fails
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}