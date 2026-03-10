package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import com.fugisawa.quemfaz.contract.search.SearchProfessionalsResponse

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

@Composable
fun ProfessionalCard(
    profile: ProfessionalProfileResponse,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
                // Photo placeholder
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(profile.name?.take(1) ?: "?")
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(profile.name ?: "Anonymous", style = MaterialTheme.typography.titleMedium)
                    Text(profile.cityName, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                profile.services.joinToString(", ") { it.displayName },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )
        }
    }
}
