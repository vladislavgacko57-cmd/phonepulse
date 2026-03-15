package com.phonepulse.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun CompareScreen(
    cert1: Certificate,
    cert2: Certificate
) {
    val modules1 = cert1.results.associateBy { it.moduleName }
    val modules2 = cert2.results.associateBy { it.moduleName }
    val allModules = (modules1.keys + modules2.keys).distinct()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
            .padding(16.dp)
    ) {
        item {
            Text(
                "📊 Сравнение диагностик",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(16.dp))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2733)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ScoreColumn("Диагностика 1", cert1.overallScore, cert1.grade, cert1.timestamp.take(10))
                    DiffIndicator(cert1.overallScore, cert2.overallScore)
                    ScoreColumn("Диагностика 2", cert2.overallScore, cert2.grade, cert2.timestamp.take(10))
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2733)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PriceColumn(cert1.recommendedPriceMin, cert1.recommendedPriceMax)
                    Text("vs", color = Color(0xFF8899AA), fontSize = 14.sp)
                    PriceColumn(cert2.recommendedPriceMin, cert2.recommendedPriceMax)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("По модулям:", color = Color(0xFF8899AA), fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
        }

        items(allModules) { moduleName ->
            val score1 = modules1[moduleName]?.score ?: 0
            val score2 = modules2[moduleName]?.score ?: 0
            val diff = score2 - score1

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2733)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        ModuleNames.get(moduleName),
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "$score1",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor(score1),
                        modifier = Modifier.width(40.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        when {
                            diff > 0 -> "▲+$diff"
                            diff < 0 -> "▼$diff"
                            else -> "="
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            diff > 0 -> Color(0xFF00C853)
                            diff < 0 -> Color(0xFFFF1744)
                            else -> Color(0xFF8899AA)
                        },
                        modifier = Modifier.width(56.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "$score2",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor(score2),
                        modifier = Modifier.width(40.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreColumn(label: String, score: Int, grade: String, date: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = Color(0xFF8899AA))
        Text("$score", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = gradeColor(grade))
        Text(grade, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = gradeColor(grade))
        Text(date, fontSize = 11.sp, color = Color(0xFF556677))
    }
}

@Composable
private fun DiffIndicator(score1: Int, score2: Int) {
    val diff = score2 - score1
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 12.dp)
    ) {
        Text(
            when {
                diff > 0 -> "▲"
                diff < 0 -> "▼"
                else -> "="
            },
            fontSize = 24.sp,
            color = when {
                diff > 0 -> Color(0xFF00C853)
                diff < 0 -> Color(0xFFFF1744)
                else -> Color(0xFF8899AA)
            }
        )
        if (diff != 0) {
            Text(
                "${if (diff > 0) "+" else ""}$diff",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (diff > 0) Color(0xFF00C853) else Color(0xFFFF1744)
            )
        }
    }
}

@Composable
private fun PriceColumn(min: Int?, max: Int?) {
    if (min != null && max != null) {
        Text(
            "%,d – %,d ₽".format(min, max),
            fontSize = 14.sp,
            color = Color(0xFF00C9A7),
            fontWeight = FontWeight.SemiBold
        )
    } else {
        Text("Н/Д", fontSize = 14.sp, color = Color(0xFF8899AA))
    }
}

private fun scoreColor(score: Int): Color = when {
    score >= 80 -> Color(0xFF00C853)
    score >= 50 -> Color(0xFFFFB300)
    else -> Color(0xFFFF1744)
}

private fun gradeColor(grade: String): Color = when (grade) {
    "S" -> Color(0xFF00C9A7)
    "A" -> Color(0xFF00C853)
    "B" -> Color(0xFFFFB300)
    "C" -> Color(0xFFFF9100)
    else -> Color(0xFFFF1744)
}
