package com.fugisawa.quemfaz.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fugisawa.quemfaz.domain.service.CanonicalServices
import com.fugisawa.quemfaz.domain.service.ServiceCategory
import com.fugisawa.quemfaz.ui.theme.Spacing

@Composable
fun ServiceCategoryPicker(
    selectedServiceIds: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    multiSelect: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val servicesByCategory = remember {
        ServiceCategory.entries.associateWith { category ->
            CanonicalServices.findByCategory(category)
        }.filter { it.value.isNotEmpty() }
    }

    LazyColumn(modifier = modifier) {
        servicesByCategory.forEach { (category, services) ->
            item {
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(
                        start = Spacing.md,
                        top = Spacing.md,
                        bottom = Spacing.xs,
                    ),
                )
            }
            items(services) { service ->
                val isSelected = service.id.value in selectedServiceIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val newSelection = if (multiSelect) {
                                if (isSelected) selectedServiceIds - service.id.value
                                else selectedServiceIds + service.id.value
                            } else {
                                setOf(service.id.value)
                            }
                            onSelectionChanged(newSelection)
                        }
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (multiSelect) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null,
                        )
                    } else {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                        )
                    }
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = service.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
