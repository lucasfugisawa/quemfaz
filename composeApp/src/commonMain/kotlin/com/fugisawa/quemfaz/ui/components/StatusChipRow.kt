package com.fugisawa.quemfaz.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.fugisawa.quemfaz.ui.theme.Spacing

@Composable
fun StatusChipRow(activeRecently: Boolean, profileComplete: Boolean) {
    if (!activeRecently && !profileComplete) return
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        if (activeRecently) {
            SuggestionChip(
                onClick = {},
                label = { Text("Active recently", style = MaterialTheme.typography.labelSmall) }
            )
        }
        if (profileComplete) {
            SuggestionChip(
                onClick = {},
                label = { Text("Complete profile", style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}
