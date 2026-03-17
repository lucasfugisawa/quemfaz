package com.fugisawa.quemfaz.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.Surface
import com.fugisawa.quemfaz.ui.preview.LightDarkPreview
import com.fugisawa.quemfaz.ui.strings.Strings
import com.fugisawa.quemfaz.ui.theme.AppTheme
import com.fugisawa.quemfaz.ui.theme.Spacing

@Composable
fun StatusChipRow(
    activeRecently: Boolean,
    profileComplete: Boolean?,
    daysSinceActive: Int? = null,
) {
    val activityText = when {
        daysSinceActive == 0 -> Strings.StatusChip.ACTIVE_TODAY
        daysSinceActive == 1 -> Strings.StatusChip.ACTIVE_YESTERDAY
        daysSinceActive != null && daysSinceActive in 2..7 -> Strings.StatusChip.activeDaysAgo(daysSinceActive)
        daysSinceActive != null && daysSinceActive in 8..30 -> Strings.StatusChip.ACTIVE_THIS_MONTH
        daysSinceActive != null && daysSinceActive >= 31 -> null  // spec: 31+ → hidden
        activeRecently -> Strings.StatusChip.ACTIVE_RECENTLY  // fallback for old responses without daysSinceActive
        else -> null
    }
    val showActivity = activityText != null
    val showIncompleteChip = profileComplete == false  // null = don't show, true = complete, false = incomplete CTA
    if (!showActivity && !showIncompleteChip) return
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        if (activityText != null) {
            SuggestionChip(
                onClick = {},
                label = { Text(activityText, style = MaterialTheme.typography.labelSmall) }
            )
        }
        if (showIncompleteChip) {
            SuggestionChip(
                onClick = {},
                label = { Text(Strings.StatusChip.COMPLETE_PROFILE, style = MaterialTheme.typography.labelSmall) }
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
