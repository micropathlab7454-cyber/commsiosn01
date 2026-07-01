package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_statements")
data class MonthlyStatement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val doctorId: Int,
    val doctorName: String,
    val month: Int, // 1-12
    val year: Int,
    val reportId: String, // unique report ID
    val dateGenerated: String,
    val totalPatients: Int,
    val totalAmount: Double,
    val totalDoctorAmount: Double,
    val totalLabCharge: Double,
    val totalOtherAmount: Double,
    val secureLink: String = "", // online link for the QR code
    val localPath: String = "" // local file path on the device
)
