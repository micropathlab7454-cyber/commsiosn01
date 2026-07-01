package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Doctor
import com.example.data.model.Entry
import com.example.data.model.MonthlyStatement

@Database(entities = [Doctor::class, Entry::class, MonthlyStatement::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun labDao(): LabDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "micro_path_lab_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
