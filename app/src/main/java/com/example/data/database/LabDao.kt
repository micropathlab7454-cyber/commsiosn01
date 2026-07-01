package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.data.model.Doctor
import com.example.data.model.Entry
import com.example.data.model.MonthlyStatement
import kotlinx.coroutines.flow.Flow

@Dao
interface LabDao {
    // Doctors
    @Query("SELECT * FROM doctors ORDER BY name ASC")
    fun getAllDoctors(): Flow<List<Doctor>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctor(doctor: Doctor): Long

    @Update
    suspend fun updateDoctor(doctor: Doctor)

    @Delete
    suspend fun deleteDoctor(doctor: Doctor)

    // Entries
    @Query("SELECT * FROM entries WHERE isDeleted = 0 ORDER BY date DESC, id DESC")
    fun getAllEntries(): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE doctorId = :doctorId AND isDeleted = 0 ORDER BY date DESC")
    fun getEntriesByDoctor(doctorId: Int): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getRecycleBinEntries(): Flow<List<Entry>>

    @Query("DELETE FROM entries WHERE isDeleted = 1 AND deletedAt < :limitTime")
    suspend fun purgeOldDeletedEntries(limitTime: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: Entry): Long

    @Update
    suspend fun updateEntry(entry: Entry)

    @Delete
    suspend fun deleteEntry(entry: Entry)

    // Monthly Statements
    @Query("SELECT * FROM monthly_statements ORDER BY dateGenerated DESC")
    fun getAllMonthlyStatements(): Flow<List<MonthlyStatement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthlyStatement(statement: MonthlyStatement): Long

    @Delete
    suspend fun deleteMonthlyStatement(statement: MonthlyStatement)
    
    @Query("SELECT * FROM monthly_statements WHERE reportId = :reportId LIMIT 1")
    suspend fun getStatementByReportId(reportId: String): MonthlyStatement?
}
