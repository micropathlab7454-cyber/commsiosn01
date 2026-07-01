package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.model.Entry
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    data class PdfResult(
        val file: File,
        val reportId: String,
        val secureLink: String
    )

    fun generateDoctorStatementPdf(
        context: Context,
        doctorName: String,
        monthName: String,
        year: Int,
        entries: List<Entry>,
        customFileName: String? = null
    ): PdfResult? {
        val reportId = "MPL-${year}-${monthName.uppercase(Locale.US).take(3)}-${doctorName.replace(" ", "").take(4).uppercase(Locale.US)}-${(1000..9999).random()}"
        
        // Secure Link that will open the report
        val secureLink = "https://micro-pathology-lab.web.app/reports/$reportId.pdf"

        // Generate QR code with all required metadata
        val qrContent = """
            Micro Pathology Lab
            Doctor Monthly Statement
            Doctor: $doctorName
            Period: $monthName $year
            Report ID: $reportId
            Link: $secureLink
        """.trimIndent()

        val qrCodeBitmap = QrCodeGenerator.generateQrCode(qrContent, size = 110)

        val pdfDocument = PdfDocument()

        // Page dimensions for A4 (595 x 842 points)
        val pageWidth = 595
        val pageHeight = 842
        val leftMargin = 40
        val rightMargin = 555
        val topMargin = 50
        val bottomMargin = 790

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = Color.rgb(0, 102, 102) // Dark Teal theme
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.rgb(100, 100, 100)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
        }

        val headerBgPaint = Paint().apply {
            color = Color.rgb(230, 242, 242) // Very light teal background for header
            style = Paint.Style.FILL
        }

        val totalRowBgPaint = Paint().apply {
            color = Color.rgb(240, 240, 240) // Light grey background
            style = Paint.Style.FILL
        }

        val totalTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = 8.5f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        // Totals
        val totalPatients = entries.size
        val totalAmount = entries.sumOf { it.amount }
        val totalDoctorAmount = entries.sumOf { it.doctorAmount }
        val totalLabCharge = entries.sumOf { it.labCharge }
        val totalOtherAmount = entries.sumOf { it.otherAmount }

        // Table configurations
        val colDateWidth = 65
        val colNameWidth = 105
        val colAgeWidth = 30
        val colTestWidth = 75
        val colAmountWidth = 60
        val colDocAmountWidth = 60
        val colLabChargeWidth = 55
        val colOtherWidth = 60

        val colXDate = leftMargin
        val colXName = colXDate + colDateWidth
        val colXAge = colXName + colNameWidth
        val colXTest = colXAge + colAgeWidth
        val colXAmount = colXTest + colTestWidth
        val colXDocAmount = colXAmount + colAmountWidth
        val colXLabCharge = colXDocAmount + colDocAmountWidth
        val colXOther = colXLabCharge + colLabChargeWidth

        var pageNumber = 1
        var currentY = topMargin

        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        fun drawPageHeader(canv: Canvas) {
            currentY = topMargin
            
            // Fetch Lab Profile Info
            val prefs = com.example.data.pref.UserPrefs(context)
            val labName = prefs.labName.ifBlank { "Micro Pathology Lab" }
            val labAddress = prefs.labAddress
            val labPhone = prefs.labPhone
            val labEmail = prefs.labEmail

            // Draw Lab Name
            canv.drawText(labName, leftMargin.toFloat(), (currentY + 15).toFloat(), titlePaint)
            
            var offset = 28
            val labDetailsPaint = Paint().apply {
                color = Color.GRAY
                textSize = 8f
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            }

            if (labAddress.isNotBlank()) {
                canv.drawText(labAddress, leftMargin.toFloat(), (currentY + offset).toFloat(), labDetailsPaint)
                offset += 11
            }

            if (labPhone.isNotBlank() || labEmail.isNotBlank()) {
                val details = listOfNotNull(
                    if (labPhone.isNotBlank()) "Phone: $labPhone" else null,
                    if (labEmail.isNotBlank()) "Email: $labEmail" else null
                ).joinToString(" | ")
                canv.drawText(details, leftMargin.toFloat(), (currentY + offset).toFloat(), labDetailsPaint)
                offset += 11
            }

            // Draw Subtitle
            canv.drawText("Doctor Monthly Statement", leftMargin.toFloat(), (currentY + offset + 8).toFloat(), subtitlePaint)
            
            // Draw Report Metadata
            boldPaint.textSize = 9f
            textPaint.textSize = 9f
            val metaY1 = currentY + 12
            val metaY2 = currentY + 24
            val metaY3 = currentY + 36
            
            canv.drawText("Doctor Name:", 350f, metaY1.toFloat(), boldPaint)
            canv.drawText(doctorName, 430f, metaY1.toFloat(), textPaint)
            
            canv.drawText("Month & Year:", 350f, metaY2.toFloat(), boldPaint)
            canv.drawText("$monthName, $year", 430f, metaY2.toFloat(), textPaint)
            
            canv.drawText("Report ID:", 350f, metaY3.toFloat(), boldPaint)
            canv.drawText(reportId, 430f, metaY3.toFloat(), textPaint)
            
            // Adjust header ending Y position based on how many profile lines were drawn
            val headerHeight = if (offset > 28) offset + 15 else 55
            currentY += headerHeight
            
            // Draw a Divider Line
            canv.drawLine(leftMargin.toFloat(), currentY.toFloat(), rightMargin.toFloat(), currentY.toFloat(), linePaint)
            currentY += 15
        }

        fun drawTableHeader(canv: Canvas) {
            // Header Row Background
            canv.drawRect(leftMargin.toFloat(), currentY.toFloat(), rightMargin.toFloat(), (currentY + 20).toFloat(), headerBgPaint)
            boldPaint.textSize = 8.5f
            
            val textY = (currentY + 14).toFloat()
            canv.drawText("Date", (colXDate + 4).toFloat(), textY, boldPaint)
            canv.drawText("Patient Name", (colXName + 4).toFloat(), textY, boldPaint)
            canv.drawText("Age", (colXAge + 4).toFloat(), textY, boldPaint)
            canv.drawText("Test", (colXTest + 4).toFloat(), textY, boldPaint)
            canv.drawText("Amount", (colXAmount + 4).toFloat(), textY, boldPaint)
            canv.drawText("Doc Amt", (colXDocAmount + 4).toFloat(), textY, boldPaint)
            canv.drawText("Lab Chg", (colXLabCharge + 4).toFloat(), textY, boldPaint)
            canv.drawText("Other", (colXOther + 4).toFloat(), textY, boldPaint)
            
            // Draw grid lines
            canv.drawRect(leftMargin.toFloat(), currentY.toFloat(), rightMargin.toFloat(), (currentY + 20).toFloat(), linePaint)
            currentY += 20
        }

        fun drawPageFooter(canv: Canvas, pNum: Int) {
            textPaint.textSize = 8f
            textPaint.color = Color.GRAY
            canv.drawText("Page $pNum", (rightMargin - 35).toFloat(), (pageHeight - 30).toFloat(), textPaint)
            val format = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.US)
            canv.drawText("Generated on: ${format.format(Date())}", leftMargin.toFloat(), (pageHeight - 30).toFloat(), textPaint)
            textPaint.color = Color.BLACK
        }

        // Setup page 1
        drawPageHeader(canvas)
        drawTableHeader(canvas)

        textPaint.textSize = 8.5f

        // Draw Entries
        for (i in entries.indices) {
            val entry = entries[i]
            
            // Check overflow
            if (currentY + 20 > bottomMargin - 50) {
                // Finish current page
                drawPageFooter(canvas, pageNumber)
                pdfDocument.finishPage(page)
                
                // Start a new page
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                
                drawPageHeader(canvas)
                drawTableHeader(canvas)
                textPaint.textSize = 8.5f
            }

            val textY = (currentY + 14).toFloat()
            canvas.drawText(entry.date, (colXDate + 4).toFloat(), textY, textPaint)
            canvas.drawText(entry.patientName, (colXName + 4).toFloat(), textY, textPaint)
            canvas.drawText(entry.age.toString(), (colXAge + 4).toFloat(), textY, textPaint)
            canvas.drawText(entry.test, (colXTest + 4).toFloat(), textY, textPaint)
            canvas.drawText("₹${entry.amount}", (colXAmount + 4).toFloat(), textY, textPaint)
            canvas.drawText("₹${entry.doctorAmount}", (colXDocAmount + 4).toFloat(), textY, textPaint)
            canvas.drawText("₹${entry.labCharge}", (colXLabCharge + 4).toFloat(), textY, textPaint)
            canvas.drawText("₹${entry.otherAmount}", (colXOther + 4).toFloat(), textY, textPaint)

            // Draw divider between rows
            canvas.drawLine(leftMargin.toFloat(), (currentY + 20).toFloat(), rightMargin.toFloat(), (currentY + 20).toFloat(), linePaint)
            currentY += 20
        }

        // Check overflow for TOTAL row
        if (currentY + 20 > bottomMargin - 50) {
            // Finish current page
            drawPageFooter(canvas, pageNumber)
            pdfDocument.finishPage(page)
            
            // Start a new page
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            
            drawPageHeader(canvas)
            drawTableHeader(canvas)
        }

        // Draw TOTAL row background (light grey)
        canvas.drawRect(leftMargin.toFloat(), currentY.toFloat(), rightMargin.toFloat(), (currentY + 20).toFloat(), totalRowBgPaint)

        // Draw TOTAL row text
        val totalTextY = (currentY + 14).toFloat()
        canvas.drawText("TOTAL", (colXDate + 4).toFloat(), totalTextY, totalTextPaint)
        canvas.drawText("-", (colXName + 4).toFloat(), totalTextY, totalTextPaint)
        canvas.drawText("-", (colXAge + 4).toFloat(), totalTextY, totalTextPaint)
        canvas.drawText("-", (colXTest + 4).toFloat(), totalTextY, totalTextPaint)
        canvas.drawText("₹$totalAmount", (colXAmount + 4).toFloat(), totalTextY, totalTextPaint)
        canvas.drawText("₹$totalDoctorAmount", (colXDocAmount + 4).toFloat(), totalTextY, totalTextPaint)
        canvas.drawText("₹$totalLabCharge", (colXLabCharge + 4).toFloat(), totalTextY, totalTextPaint)
        canvas.drawText("₹$totalOtherAmount", (colXOther + 4).toFloat(), totalTextY, totalTextPaint)

        // Draw divider line under the TOTAL row
        canvas.drawLine(leftMargin.toFloat(), (currentY + 20).toFloat(), rightMargin.toFloat(), (currentY + 20).toFloat(), linePaint)
        currentY += 20

        // Check if QR code fits on the current page, if not, move to next page
        if (currentY + 150 > bottomMargin) {
            drawPageFooter(canvas, pageNumber)
            pdfDocument.finishPage(page)
            
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            
            drawPageHeader(canvas)
        } else {
            currentY += 15 // Add spacing between table and QR code
        }

        // Draw QR Code
        if (qrCodeBitmap != null) {
            val qrX = rightMargin - 120
            val qrY = currentY
            
            canvas.drawBitmap(qrCodeBitmap, qrX.toFloat(), qrY.toFloat(), null)
            
            textPaint.textSize = 7.5f
            boldPaint.textSize = 7.5f
            canvas.drawText("Scan to View Doctor Monthly Statement", (qrX - 10).toFloat(), (qrY + 122).toFloat(), boldPaint)
            canvas.drawText("Report ID: $reportId", (qrX - 10).toFloat(), (qrY + 132).toFloat(), textPaint)
        }

        drawPageFooter(canvas, pageNumber)
        pdfDocument.finishPage(page)

        // Save PDF file
        val finalFileName = customFileName?.trim()?.let {
            if (it.endsWith(".pdf", ignoreCase = true)) it else "$it.pdf"
        } ?: "Statement_${doctorName.replace(" ", "_")}_$monthName-$year.pdf"

        val statementsDir = File(context.getExternalFilesDir(null), "Statements")
        if (!statementsDir.exists()) statementsDir.mkdirs()

        val localFile = File(statementsDir, finalFileName)
        
        try {
            val fos = FileOutputStream(localFile)
            pdfDocument.writeTo(fos)
            fos.close()
            pdfDocument.close()
            
            // Save in Downloads folder as well (so user can find it in their system Downloads folder easily)
            saveToSystemDownloads(context, localFile, finalFileName)

            return PdfResult(localFile, reportId, secureLink)
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            return null
        }
    }

    private fun saveToSystemDownloads(context: Context, srcFile: File, fileName: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
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
