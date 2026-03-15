package com.phonepulse.feature.certificate

import androidx.compose.foundation.background
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
import com.phonepulse.core.common.ModuleNames
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
                text = "\u0413\u0440\u0435\u0439\u0434: ${certificate.grade}",
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
            Text(
                text = "Build.MODEL: ${certificate.device.model}",
                fontSize = 11.sp,
                color = Color(0xFF556677),
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
                    Text(
                        text = "\uD83D\uDCB0 \u0420\u0435\u043A\u043E\u043C\u0435\u043D\u0434\u0443\u0435\u043C\u0430\u044F \u0446\u0435\u043D\u0430",
                        fontSize = 16.sp,
                        color = Color(0xFF8899AA)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${certificate.recommendedPriceMin?.let { "%,d".format(it) } ?: "?"} \u2013 " +
                            "${certificate.recommendedPriceMax?.let { "%,d".format(it) } ?: "?"} \u20BD",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00C9A7)
                    )

                    if (certificate.matchedModel != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = certificate.matchedModel!!,
                            fontSize = 13.sp,
                            color = Color(0xFF8899AA)
                        )
                    }

                    certificate.priceSource?.let { source ->
                        Text(
                            text = when (source) {
                                "exact_match" -> "\u2705 Model matched exactly"
                                "partial_match" -> "\u2705 Model recognized"
                                "fuzzy_match" -> "\u26A0\uFE0F Approximate match"
                                "ram_storage_fallback" -> "\u26A0\uFE0F Price by RAM/Storage"
                                else -> "\u26A0\uFE0F Base estimate"
                            },
                            fontSize = 11.sp,
                            color = if (source.contains("match")) Color(0xFF00C853) else Color(0xFFFFB300)
                        )
                    }

                    certificate.priceDbUpdated?.let {
                        Text(
                            text = "\u0411\u0430\u0437\u0430 \u0446\u0435\u043D: $it",
                            fontSize = 10.sp,
                            color = Color(0xFF556677),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
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
                            text = ModuleNames.get(result.moduleName),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        if (result.summary.isNotEmpty()) {
                            Text(
                                text = result.summary,
                                fontSize = 12.sp,
                                color = Color(0xFF8899AA),
                                maxLines = 2
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
                Text(
                    text = "\u041F\u043E\u043A\u0430\u0437\u0430\u0442\u044C \u0441\u0435\u0440\u0442\u0438\u0444\u0438\u043A\u0430\u0442 \u0441 QR",
                    fontSize = 16.sp
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
