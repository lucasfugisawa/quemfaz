package com.fugisawa.quemfaz.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fugisawa.quemfaz.contract.profile.InterpretedServiceDto
import com.fugisawa.quemfaz.ui.theme.Spacing

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
            SuggestionChip(onClick = {}, label = { Text(service.displayName) })
        }
    }
}
