package com.fugisawa.quemfaz.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fugisawa.quemfaz.contract.profile.InterpretedServiceDto
import com.fugisawa.quemfaz.ui.preview.LightDarkPreview
import com.fugisawa.quemfaz.ui.preview.PreviewSamples
import com.fugisawa.quemfaz.ui.theme.AppTheme
import com.fugisawa.quemfaz.ui.theme.Spacing

private const val MATCH_LEVEL_PRIMARY = "PRIMARY"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServiceChipList(
    services: List<InterpretedServiceDto>,
    maxItems: Int = Int.MAX_VALUE
) {
    if (services.isEmpty()) return
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        services.take(maxItems).forEach { service ->
            val isPrimary = service.matchLevel == MATCH_LEVEL_PRIMARY
            SuggestionChip(
                onClick = {},
                label = { Text(service.displayName) },
                colors = if (isPrimary)
                    SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                else
                    SuggestionChipDefaults.suggestionChipColors(),
            )
        }
    }
}

// ── Previews ──

@LightDarkPreview
@Composable
private fun ServiceChipListPreview() {
    AppTheme { Surface(modifier = Modifier.padding(8.dp)) { ServiceChipList(services = PreviewSamples.sampleServices) } }
}

@LightDarkPreview
@Composable
private fun ServiceChipListLongPreview() {
    AppTheme { Surface(modifier = Modifier.padding(8.dp)) { ServiceChipList(services = PreviewSamples.sampleServicesLong) } }
}
