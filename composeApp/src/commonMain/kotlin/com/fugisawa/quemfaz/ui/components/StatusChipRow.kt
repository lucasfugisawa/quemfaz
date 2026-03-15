package com.fugisawa.quemfaz.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.Surface
import com.fugisawa.quemfaz.ui.preview.LightDarkPreview
import com.fugisawa.quemfaz.ui.theme.AppTheme
import com.fugisawa.quemfaz.ui.theme.Spacing

@Composable
fun StatusChipRow(
    activeRecently: Boolean,
    profileComplete: Boolean,
    daysSinceActive: Int? = null,
) {
    val activityText = when {
        daysSinceActive == 0 -> "Active today"
        daysSinceActive == 1 -> "Active yesterday"
        daysSinceActive != null && daysSinceActive in 2..7 -> "Active $daysSinceActive days ago"
        daysSinceActive != null && daysSinceActive in 8..30 -> "Active this month"
        daysSinceActive != null && daysSinceActive >= 31 -> null  // spec: 31+ → hidden
        activeRecently -> "Active recently"  // fallback for old responses without daysSinceActive
        else -> null
    }
    val showActivity = activityText != null
    if (!showActivity && !profileComplete) return
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        if (activityText != null) {
            SuggestionChip(
                onClick = {},
                label = { Text(activityText, style = MaterialTheme.typography.labelSmall) }
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

// ── Previews ──

@LightDarkPreview
@Composable
private fun StatusChipRowBothPreview() {
    AppTheme { Surface { StatusChipRow(activeRecently = true, profileComplete = true, daysSinceActive = 0) } }
}

@LightDarkPreview
@Composable
private fun StatusChipRowActiveOnlyPreview() {
    AppTheme { Surface { StatusChipRow(activeRecently = true, profileComplete = false, daysSinceActive = 3) } }
}
