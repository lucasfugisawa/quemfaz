package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse

@Composable
fun SearchResultsScreen(
    query: String,
    uiState: SearchUiState,
    onProfileClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Results for \"$query\"", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        when (uiState) {
            is SearchUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            is SearchUiState.Error -> {
                Text(uiState.message, color = MaterialTheme.colorScheme.error)
            }
            is SearchUiState.Success -> {
                if (uiState.response.results.isEmpty()) {
                    Text("No professionals found.")
                } else {
                    LazyColumn {
                        items(uiState.response.results) { profile ->
                            ProfessionalCard(profile, onClick = { onProfileClick(profile.id) })
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
            is SearchUiState.Idle -> {}
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfessionalCard(
    profile: ProfessionalProfileResponse,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            profile.name?.take(1)?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(profile.name ?: "Anonymous", style = MaterialTheme.typography.titleMedium)
                    Text(profile.cityName, style = MaterialTheme.typography.bodySmall)
                    if (profile.neighborhoods.isNotEmpty()) {
                        Text(
                            profile.neighborhoods.take(2).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (profile.services.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    profile.services.take(3).forEach { service ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(service.displayName) }
                        )
                    }
                }
            }
            if (profile.activeRecently || profile.profileComplete) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (profile.activeRecently) {
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    "Active recently",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                    if (profile.profileComplete) {
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    "Complete profile",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
