package com.phonepulse.feature.certificate

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.phonepulse.core.common.Constants
import com.phonepulse.core.model.Certificate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPOutputStream

@Composable
fun CertificateScreen(
    certificate: Certificate,
    onShare: () -> Unit,
    onSavePdf: () -> Unit = {}
) {
    val qrBitmap = remember(certificate) { generateQRBitmap(certificate) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2733)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "СЕРТИФИКАТ СОСТОЯНИЯ",
                    fontSize = 14.sp,
                    color = Color(0xFF8899AA),
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "PhonePulse",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00C9A7)
                )
                Spacer(Modifier.height(16.dp))

                Text(
                    text = "${certificate.device.manufacturer} ${certificate.device.model}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))
                val gradeColor = when (certificate.grade) {
                    "S" -> Color(0xFF00C9A7)
                    "A" -> Color(0xFF00C853)
                    "B" -> Color(0xFFFFB300)
                    "C" -> Color(0xFFFF9100)
                    else -> Color(0xFFFF1744)
                }
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${certificate.overallScore}",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = gradeColor
                    )
                    Text(
                        text = "/100",
                        fontSize = 20.sp,
                        color = Color(0xFF8899AA),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(
                        text = certificate.grade,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = gradeColor,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))
                if (certificate.recommendedPriceMin != null && certificate.recommendedPriceMax != null) {
                    Text("Рекомендуемая цена", fontSize = 12.sp, color = Color(0xFF8899AA))
                    Text(
                        text = "%,d - %,d ₽".format(certificate.recommendedPriceMin, certificate.recommendedPriceMax),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF00C9A7)
                    )
                }

                Spacer(Modifier.height(24.dp))
                qrBitmap?.let { bmp ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "QR код сертификата",
                            modifier = Modifier
                                .size(200.dp)
                                .padding(12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = certificate.certId,
                    fontSize = 12.sp,
                    color = Color(0xFF8899AA),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = certificate.timestamp.take(10),
                    fontSize = 12.sp,
                    color = Color(0xFF556677)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onShare,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C9A7))
        ) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Поделиться сертификатом", fontSize = 16.sp)
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onSavePdf,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2733))
        ) {
            Text("Сохранить PDF", fontSize = 16.sp, color = Color.White)
        }

        Spacer(Modifier.height(32.dp))
    }
}

private fun generateQRBitmap(certificate: Certificate): Bitmap? {
    return try {
        val json = Json.encodeToString(certificate)
        var compressed = gzipCompress(json.toByteArray())

        if (compressed.size > 2500) {
            val light = certificate.copy(
                results = certificate.results.map { it.copy(details = emptyMap()) }
            )
            compressed = gzipCompress(Json.encodeToString(light).toByteArray())
        }

        val encoded = Base64.getEncoder().encodeToString(compressed)
        val webUrl = "${Constants.CertificateWebBaseUrl}#$encoded"
        val qrContent = webUrl

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        bitmap
    } catch (_: Exception) {
        null
    }
}

private fun gzipCompress(data: ByteArray): ByteArray {
    val bos = ByteArrayOutputStream()
    GZIPOutputStream(bos).use { it.write(data) }
    return bos.toByteArray()
}
