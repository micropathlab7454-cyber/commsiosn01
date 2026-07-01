package com.example.data.repository

import android.content.Context
import com.example.data.database.LabDao
import com.example.data.model.Doctor
import com.example.data.model.Entry
import com.example.data.model.MonthlyStatement
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class LabRepository(
    private val context: Context,
    private val labDao: LabDao
) {
    val allDoctors: Flow<List<Doctor>> = labDao.getAllDoctors()
    val allEntries: Flow<List<Entry>> = labDao.getAllEntries()
    val recycleBinEntries: Flow<List<Entry>> = labDao.getRecycleBinEntries()
    val allStatements: Flow<List<MonthlyStatement>> = labDao.getAllMonthlyStatements()

    suspend fun purgeOldDeletedEntries(limitTime: Long) = labDao.purgeOldDeletedEntries(limitTime)

    suspend fun insertDoctor(doctor: Doctor): Long = labDao.insertDoctor(doctor)
    suspend fun updateDoctor(doctor: Doctor) = labDao.updateDoctor(doctor)
    suspend fun deleteDoctor(doctor: Doctor) = labDao.deleteDoctor(doctor)

    suspend fun insertEntry(entry: Entry): Long = labDao.insertEntry(entry)
    suspend fun updateEntry(entry: Entry) = labDao.updateEntry(entry)
    suspend fun deleteEntry(entry: Entry) = labDao.deleteEntry(entry)

    suspend fun insertStatement(statement: MonthlyStatement): Long = labDao.insertMonthlyStatement(statement)
    suspend fun deleteStatement(statement: MonthlyStatement) = labDao.deleteMonthlyStatement(statement)
    suspend fun getStatementByReportId(reportId: String): MonthlyStatement? = labDao.getStatementByReportId(reportId)

    // Database Backup and Restore
    fun backupDatabase(): File? {
        return try {
            val dbFile = context.getDatabasePath("micro_path_lab_db")
            if (!dbFile.exists()) return null

            val backupDir = context.getExternalFilesDir("Backups") ?: context.filesDir
            if (!backupDir.exists()) backupDir.mkdirs()

            val backupFile = File(backupDir, "micro_path_lab_db_backup.sqlite")
            
            copyFile(dbFile, backupFile)
            
            val walFile = File(dbFile.path + "-wal")
            if (walFile.exists()) {
                copyFile(walFile, File(backupFile.path + "-wal"))
            }
            val shmFile = File(dbFile.path + "-shm")
            if (shmFile.exists()) {
                copyFile(shmFile, File(backupFile.path + "-shm"))
            }
            
            backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun restoreDatabase(backupFile: File): Boolean {
        return try {
            val dbFile = context.getDatabasePath("micro_path_lab_db")
            
            dbFile.delete()
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()

            copyFile(backupFile, dbFile)
            
            val backupWal = File(backupFile.path + "-wal")
            if (backupWal.exists()) {
                copyFile(backupWal, File(dbFile.path + "-wal"))
            }
            val backupShm = File(backupFile.path + "-shm")
            if (backupShm.exists()) {
                copyFile(backupShm, File(dbFile.path + "-shm"))
            }
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun copyFile(src: File, dst: File) {
        FileInputStream(src).use { inStream ->
            FileOutputStream(dst).use { outStream ->
                val inChannel = inStream.channel
                val outChannel = outStream.channel
                inChannel.transferTo(0, inChannel.size(), outChannel)
            }
        }
    }
}
