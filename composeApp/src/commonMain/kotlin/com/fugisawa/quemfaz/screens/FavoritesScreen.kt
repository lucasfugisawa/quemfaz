package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fugisawa.quemfaz.ui.components.ErrorMessage
import com.fugisawa.quemfaz.ui.components.FullScreenLoading
import com.fugisawa.quemfaz.ui.preview.LightDarkScreenPreview
import com.fugisawa.quemfaz.ui.preview.PreviewSamples
import com.fugisawa.quemfaz.ui.strings.Strings
import com.fugisawa.quemfaz.ui.theme.AppTheme
import com.fugisawa.quemfaz.ui.theme.Spacing

@Composable
fun FavoritesScreen(
    uiState: FavoritesUiState,
    onProfileClick: (String) -> Unit,
    onRetry: () -> Unit,
    onFindProfessionals: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.screenEdge, vertical = Spacing.md),
    ) {
        Text(
            Strings.Favorites.TITLE,
            style = MaterialTheme.typography.headlineMedium,
        )
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
                        Text(
                            Strings.Favorites.EMPTY_TITLE,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            Strings.Favorites.EMPTY_SUBTITLE,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (onFindProfessionals != null) {
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Button(
                                onClick = onFindProfessionals,
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Text(Strings.Favorites.FIND_PROFESSIONALS)
                            }
                        }
                    }
                }
            }
            is FavoritesUiState.Content -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(uiState.favorites) { profile ->
                        ProfessionalCard(profile, onClick = { onProfileClick(profile.id) })
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
