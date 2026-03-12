package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import com.fugisawa.quemfaz.ui.components.AppScreen
import com.fugisawa.quemfaz.ui.components.ErrorMessage
import com.fugisawa.quemfaz.ui.components.FullScreenLoading
import com.fugisawa.quemfaz.ui.components.ProfileAvatar
import com.fugisawa.quemfaz.ui.components.ServiceChipList
import com.fugisawa.quemfaz.ui.components.StatusChipRow
import com.fugisawa.quemfaz.ui.theme.Spacing

@Composable
fun SearchResultsScreen(
    query: String,
    uiState: SearchUiState,
    onProfileClick: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    AppScreen(title = "Results for \"$query\"", onNavigateBack = onNavigateBack) {
        when (uiState) {
            is SearchUiState.Loading -> FullScreenLoading()
            is SearchUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorMessage(uiState.message)
                }
            }
            is SearchUiState.Success -> {
                if (uiState.response.results.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No professionals found.")
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(Spacing.md)) {
                        items(uiState.response.results) { profile ->
                            ProfessionalCard(profile, onClick = { onProfileClick(profile.id) })
                            Spacer(modifier = Modifier.height(Spacing.sm))
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
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(name = profile.name, size = 56.dp)
                Spacer(modifier = Modifier.width(Spacing.sm + Spacing.xs))
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
                Spacer(modifier = Modifier.height(Spacing.sm))
                ServiceChipList(services = profile.services, maxItems = 3)
            }
            if (profile.activeRecently || profile.profileComplete) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                StatusChipRow(
                    activeRecently = profile.activeRecently,
                    profileComplete = profile.profileComplete
                )
            }
        }
    }
}
