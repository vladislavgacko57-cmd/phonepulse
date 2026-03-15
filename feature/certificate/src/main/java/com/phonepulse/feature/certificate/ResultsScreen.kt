package com.phonepulse.feature.certificate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phonepulse.core.model.Certificate

@Composable
fun ResultsScreen(
    certificate: Certificate,
    onViewCertificate: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(Modifier.height(32.dp))

            val gradeColor = when (certificate.grade) {
                "S" -> Color(0xFF00C9A7)
                "A" -> Color(0xFF00C853)
                "B" -> Color(0xFFFFB300)
                "C" -> Color(0xFFFF9100)
                else -> Color(0xFFFF1744)
            }

            Text(
                text = "${certificate.overallScore}",
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = gradeColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Грейд: ${certificate.grade}",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = gradeColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${certificate.device.manufacturer} ${certificate.device.model}",
                fontSize = 16.sp,
                color = Color(0xFF8899AA),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2733)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("💰 Рекомендуемая цена", fontSize = 16.sp, color = Color(0xFF8899AA))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${certificate.recommendedPriceMin?.let { "%,d".format(it) } ?: "?"} – " +
                            "${certificate.recommendedPriceMax?.let { "%,d".format(it) } ?: "?"} ₽",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00C9A7)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        items(certificate.results) { result ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2733)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = result.moduleName.replaceFirstChar { it.uppercase() },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        result.details.entries.take(3).forEach { (key, value) ->
                            Text(
                                text = "$key: $value",
                                fontSize = 12.sp,
                                color = Color(0xFF8899AA)
                            )
                        }
                    }
                    Text(
                        text = "${result.score}/100",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            result.score >= 80 -> Color(0xFF00C853)
                            result.score >= 50 -> Color(0xFFFFB300)
                            else -> Color(0xFFFF1744)
                        }
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onViewCertificate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF845EC2))
            ) {
                Text("Показать сертификат с QR", fontSize = 16.sp)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
