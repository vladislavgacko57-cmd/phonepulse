package com.phonepulse.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PlaceholderScreen(
    title: String,
    description: String,
    primaryButtonText: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryButtonText: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Text(
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (primaryButtonText != null && onPrimaryAction != null) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onPrimaryAction
            ) {
                Text(primaryButtonText)
            }
        }
        if (secondaryButtonText != null && onSecondaryAction != null) {
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                onClick = onSecondaryAction
            ) {
                Text(secondaryButtonText)
            }
        }
    }
}
