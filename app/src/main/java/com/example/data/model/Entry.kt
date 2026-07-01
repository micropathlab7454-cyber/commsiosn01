package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entries")
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // format: "YYYY-MM-DD"
    val doctorId: Int,
    val doctorName: String,
    val patientName: String,
    val age: Int,
    val test: String,
    val amount: Double,
    val doctorAmount: Double,
    val labCharge: Double,
    val otherAmount: Double,
    val receiptNumber: String = "",
    val time: String = "",
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)
