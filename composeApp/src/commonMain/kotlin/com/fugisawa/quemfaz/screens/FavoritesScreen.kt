package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import com.fugisawa.quemfaz.ui.components.ErrorMessage
import com.fugisawa.quemfaz.ui.components.FullScreenLoading
import com.fugisawa.quemfaz.ui.preview.LightDarkScreenPreview
import com.fugisawa.quemfaz.ui.preview.PreviewSamples
import com.fugisawa.quemfaz.ui.theme.AppTheme
import com.fugisawa.quemfaz.ui.theme.Spacing

@Composable
fun FavoritesScreen(
    uiState: FavoritesUiState,
    onProfileClick: (String) -> Unit,
    onRetry: () -> Unit,
    onFindProfessionals: (() -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxSize().padding(Spacing.md)) {
        Text("Favorites", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(Spacing.md))

        when (uiState) {
            is FavoritesUiState.Loading -> FullScreenLoading()
            is FavoritesUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ErrorMessage(message = uiState.message, onRetry = onRetry)
                }
            }
            is FavoritesUiState.Empty -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.padding(horizontal = Spacing.lg),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(Spacing.emptyStateIconSize),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text("No favorites yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Professionals you save will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (onFindProfessionals != null) {
                            Button(onClick = onFindProfessionals) {
                                Text("Find professionals")
                            }
                        }
                    }
                }
            }
            is FavoritesUiState.Content -> {
                LazyColumn {
                    items(uiState.favorites) { profile ->
                        ProfessionalCard(profile, onClick = { onProfileClick(profile.id) })
                        Spacer(modifier = Modifier.height(Spacing.sm))
                    }
                }
            }
        }
    }
}

// ── Previews ──

@LightDarkScreenPreview
@Composable
private fun FavoritesLoadingPreview() {
    AppTheme { FavoritesScreen(uiState = FavoritesUiState.Loading, onProfileClick = {}, onRetry = {}) }
}

@LightDarkScreenPreview
@Composable
private fun FavoritesContentPreview() {
    AppTheme {
        FavoritesScreen(
            uiState = FavoritesUiState.Content(listOf(PreviewSamples.sampleProfile, PreviewSamples.sampleProfile2)),
            onProfileClick = {},
            onRetry = {}
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun FavoritesEmptyPreview() {
    AppTheme { FavoritesScreen(uiState = FavoritesUiState.Empty, onProfileClick = {}, onRetry = {}) }
}

@LightDarkScreenPreview
@Composable
private fun FavoritesErrorPreview() {
    AppTheme { FavoritesScreen(uiState = FavoritesUiState.Error("Failed to load favorites."), onProfileClick = {}, onRetry = {}) }
}
