package com.phonepulse.feature.certificate

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.phonepulse.core.model.Certificate
import com.phonepulse.core.model.TestStatus
import java.io.File

object PdfExporter {

    fun generateAndShare(context: Context, certificate: Certificate) {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.parseColor("#00C9A7")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 14f
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 13f
            isAntiAlias = true
        }
        val scorePaint = Paint().apply {
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        var y = 50f

        canvas.drawText("PhonePulse - Сертификат состояния", 40f, y, titlePaint)
        y += 30f
        canvas.drawText("Дата: ${certificate.timestamp.take(10)}  |  ID: ${certificate.certId}", 40f, y, subtitlePaint)
        y += 8f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 25f

        canvas.drawText("Устройство: ${certificate.device.manufacturer} ${certificate.device.model}", 40f, y, bodyPaint)
        y += 20f
        canvas.drawText(
            "Android ${certificate.device.androidVersion}  |  RAM: ${"%.1f".format(certificate.device.ramGb)}GB  |  Storage: ${"%.0f".format(certificate.device.storageGb)}GB",
            40f,
            y,
            bodyPaint
        )
        y += 35f

        val gradeColor = when (certificate.grade) {
            "S" -> Color.parseColor("#00C9A7")
            "A" -> Color.parseColor("#00C853")
            "B" -> Color.parseColor("#FFB300")
            "C" -> Color.parseColor("#FF9100")
            else -> Color.parseColor("#FF1744")
        }
        scorePaint.color = gradeColor
        canvas.drawText("${certificate.overallScore}/100", 40f, y + 40f, scorePaint)

        scorePaint.textSize = 36f
        canvas.drawText("Грейд: ${certificate.grade}", 250f, y + 40f, scorePaint)
        y += 70f

        if (certificate.recommendedPriceMin != null && certificate.recommendedPriceMax != null) {
            val pricePaint = Paint().apply {
                color = Color.parseColor("#00C9A7")
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText(
                "Рекомендуемая цена: %,d - %,d ₽".format(
                    certificate.recommendedPriceMin,
                    certificate.recommendedPriceMax
                ),
                40f,
                y,
                pricePaint
            )
            y += 30f
        }

        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 20f

        canvas.drawText("Результаты по модулям:", 40f, y, subtitlePaint.apply { textSize = 16f })
        y += 25f

        val moduleNames = mapOf(
            "battery" to "Батарея",
            "display" to "Экран",
            "audio" to "Аудио",
            "camera" to "Камеры",
            "sensors" to "Датчики",
            "connectivity" to "Связь",
            "storage" to "Память",
            "controls" to "Управление"
        )

        for (result in certificate.results) {
            val statusSymbol = when (result.status) {
                TestStatus.PASSED -> "OK"
                TestStatus.WARNING -> "!"
                TestStatus.FAILED -> "X"
                TestStatus.SKIPPED -> "-"
            }
            val name = moduleNames[result.moduleName] ?: result.moduleName
            canvas.drawText("$statusSymbol  $name", 50f, y, bodyPaint)

            val scoreModPaint = Paint(bodyPaint).apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = when {
                    result.score >= 80 -> Color.parseColor("#00C853")
                    result.score >= 50 -> Color.parseColor("#FFB300")
                    else -> Color.parseColor("#FF1744")
                }
            }
            canvas.drawText("${result.score}/100", 450f, y, scoreModPaint)
            y += 22f

            result.details.entries.take(3).forEach { (k, v) ->
                canvas.drawText("    $k: $v", 65f, y, subtitlePaint.apply { textSize = 11f })
                y += 15f
            }
            y += 5f

            if (y > 780f) break
        }

        y = 810f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 15f
        subtitlePaint.textSize = 10f
        canvas.drawText("Сгенерировано приложением PhonePulse", 40f, y, subtitlePaint)

        pdf.finishPage(page)

        val file = File(context.cacheDir, "PhonePulse_${certificate.certId}.pdf")
        file.outputStream().use { pdf.writeTo(it) }
        pdf.close()

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "PhonePulse: Сертификат ${certificate.certId}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться PDF"))
    }
}
