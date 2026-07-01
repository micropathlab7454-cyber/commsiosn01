package com.example.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.model.Entry
import java.io.File
import java.io.FileOutputStream

object ExcelGenerator {

    data class ExcelResult(
        val file: File,
        val fileName: String
    )

    fun generateDoctorStatementExcel(
        context: Context,
        doctorName: String,
        monthName: String,
        year: Int,
        entries: List<Entry>
    ): ExcelResult? {
        val fileName = "Statement_${doctorName.replace(" ", "_")}_${monthName}_$year.csv"
        val statementsDir = File(context.getExternalFilesDir(null), "Statements")
        if (!statementsDir.exists()) statementsDir.mkdirs()

        val localFile = File(statementsDir, fileName)

        try {
            FileOutputStream(localFile).use { fos ->
                fos.bufferedWriter().use { writer ->
                    // Lab Details (from Profile if set, else defaults)
                    val prefs = com.example.data.pref.UserPrefs(context)
                    val labName = prefs.labName
                    val labAddress = prefs.labAddress
                    val labPhone = prefs.labPhone
                    val labEmail = prefs.labEmail

                    writer.write("\"$labName\"\n")
                    if (labAddress.isNotBlank()) writer.write("\"$labAddress\"\n")
                    if (labPhone.isNotBlank() || labEmail.isNotBlank()) {
                        writer.write("\"Phone: $labPhone | Email: $labEmail\"\n")
                    }
                    writer.write("\n")

                    // Report Header
                    writer.write("\"Doctor Monthly Statement\"\n")
                    writer.write("\"Doctor:\",\"$doctorName\"\n")
                    writer.write("\"Period:\",\"$monthName $year\"\n")
                    writer.write("\n")

                    // Table Headers
                    writer.write("\"Receipt Number\",\"Date\",\"Time\",\"Patient Name\",\"Age\",\"Test Name\",\"Total Amount (INR)\",\"Doctor Amount (INR)\",\"Lab Charge (INR)\",\"Other Amount (INR)\"\n")

                    // Table Data
                    for (entry in entries) {
                        val receipt = entry.receiptNumber.ifBlank { "N/A" }
                        val time = entry.time.ifBlank { "N/A" }
                        writer.write("\"$receipt\",\"${entry.date}\",\"$time\",\"${entry.patientName.replace("\"", "\"\"")}\",${entry.age},\"${entry.test.replace("\"", "\"\"")}\",${entry.amount},${entry.doctorAmount},${entry.labCharge},${entry.otherAmount}\n")
                    }

                    writer.write("\n")

                    // Summary / Totals
                    val totalPatients = entries.size
                    val totalAmount = entries.sumOf { it.amount }
                    val totalDoctorAmount = entries.sumOf { it.doctorAmount }
                    val totalLabCharge = entries.sumOf { it.labCharge }
                    val totalOtherAmount = entries.sumOf { it.otherAmount }

                    writer.write("\"Summary\",\"Totals\"\n")
                    writer.write("\"Total Patients\",$totalPatients\n")
                    writer.write("\"Total Amount (INR)\",$totalAmount\n")
                    writer.write("\"Total Doctor Amount (INR)\",$totalDoctorAmount\n")
                    writer.write("\"Total Lab Charge (INR)\",$totalLabCharge\n")
                    writer.write("\"Total Other Amount (INR)\",$totalOtherAmount\n")
                }
            }

            // Save in system Downloads folder as well
            saveToSystemDownloads(context, localFile, fileName)

            return ExcelResult(localFile, fileName)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun saveToSystemDownloads(context: Context, srcFile: File, fileName: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MicroPathologyLab")
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        srcFile.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val destFolder = File(downloadsDir, "MicroPathologyLab")
                if (!destFolder.exists()) destFolder.mkdirs()
                val destFile = File(destFolder, fileName)
                srcFile.inputStream().use { input ->
                    destFile.outputStream().use { out ->
                        input.copyTo(out)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
