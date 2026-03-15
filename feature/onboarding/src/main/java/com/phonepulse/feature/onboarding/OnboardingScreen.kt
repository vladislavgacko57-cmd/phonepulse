package com.phonepulse.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val titles = listOf(
        stringResource(R.string.onboarding_title_1),
        stringResource(R.string.onboarding_title_2),
        stringResource(R.string.onboarding_title_3)
    )
    val descriptions = listOf(
        stringResource(R.string.onboarding_desc_1),
        stringResource(R.string.onboarding_desc_2),
        stringResource(R.string.onboarding_desc_3)
    )
    val emojis = listOf("\uD83D\uDCF1", "\uD83D\uDCDC", "\uD83D\uDCB0")

    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
            .padding(24.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onComplete, modifier = Modifier.testTag("onboarding_skip_button")) {
                Text(stringResource(R.string.skip), color = Color(0xFF8899AA))
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(emojis[page], fontSize = 80.sp)
                Spacer(Modifier.height(32.dp))
                Text(
                    text = titles[page],
                    modifier = Modifier.testTag("onboarding_title"),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = descriptions[page],
                    fontSize = 16.sp,
                    color = Color(0xFF8899AA),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(if (index == pagerState.currentPage) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pagerState.currentPage) Color(0xFF00C9A7)
                            else Color(0xFF1A2733)
                        )
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (pagerState.currentPage < 2) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                } else {
                    onComplete()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("onboarding_next_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C9A7))
        ) {
            Text(
                if (pagerState.currentPage < 2) stringResource(R.string.next) else stringResource(R.string.start),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}
