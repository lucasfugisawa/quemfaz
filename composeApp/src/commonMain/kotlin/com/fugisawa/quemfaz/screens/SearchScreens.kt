package com.fugisawa.quemfaz.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.fugisawa.quemfaz.contract.catalog.CatalogResponse
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import com.fugisawa.quemfaz.ui.components.ServiceCategoryPicker
import com.fugisawa.quemfaz.ui.components.ShimmerBox
import com.fugisawa.quemfaz.ui.components.AppScreen
import com.fugisawa.quemfaz.ui.components.ErrorMessage
import com.fugisawa.quemfaz.ui.components.FullScreenLoading
import com.fugisawa.quemfaz.ui.components.ProfileAvatar
import com.fugisawa.quemfaz.ui.components.ServiceChipList
import com.fugisawa.quemfaz.ui.components.StatusChipRow
import com.fugisawa.quemfaz.ui.preview.LightDarkPreview
import com.fugisawa.quemfaz.ui.preview.LightDarkScreenPreview
import com.fugisawa.quemfaz.ui.preview.PreviewSamples
import com.fugisawa.quemfaz.ui.strings.Strings
import com.fugisawa.quemfaz.ui.theme.AppTheme
import com.fugisawa.quemfaz.ui.theme.Spacing

@Composable
fun SearchResultsScreen(
    query: String,
    uiState: SearchUiState,
    catalog: CatalogResponse? = null,
    favoritedProfileIds: Set<String> = emptySet(),
    onFavoriteToggle: (profileId: String) -> Unit = {},
    onProfileClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    hasMore: Boolean = false,
    onLoadMore: () -> Unit = {},
    onCategorySelected: (serviceId: String) -> Unit = {},
) {
    AppScreen(title = Strings.Search.resultsTitle(query), onNavigateBack = onNavigateBack) {
        when (uiState) {
            is SearchUiState.Loading -> {
                LazyColumn(contentPadding = PaddingValues(Spacing.md)) {
                    items(3) {
                        ProfessionalCardSkeleton()
                        Spacer(modifier = Modifier.height(Spacing.sm))
                    }
                }
            }
            is SearchUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorMessage(uiState.message)
                }
            }
            is SearchUiState.Success -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Interpreted services banner — shown when AI mapped the query to services
                    AnimatedVisibility(
                        visible = uiState.response.interpretedServices.isNotEmpty(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Text(
                            text = Strings.Search.showingResults(uiState.response.interpretedServices.joinToString(" · ") { it.displayName }),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
                        )
                    }

                    if (uiState.response.results.isEmpty()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(Spacing.emptyStateIconSize),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(Strings.Search.NO_RESULTS_TITLE, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    Strings.Search.NO_RESULTS_SUBTITLE,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = Strings.Search.BROWSE_BY_CATEGORY,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            )
                            if (catalog != null) {
                                ServiceCategoryPicker(
                                    categories = catalog.categories,
                                    services = catalog.services,
                                    selectedServiceIds = emptySet(),
                                    onSelectionChanged = { selected ->
                                        selected.firstOrNull()?.let { serviceId ->
                                            onCategorySelected(serviceId)
                                        }
                                    },
                                    multiSelect = false,
                                )
                            }
                        }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(Spacing.md)) {
                            items(uiState.response.results) { profile ->
                                ProfessionalCard(
                                    profile = profile,
                                    onClick = { onProfileClick(profile.id) },
                                    isFavorited = profile.id in favoritedProfileIds,
                                    onFavoriteToggle = { onFavoriteToggle(profile.id) }
                                )
                                Spacer(modifier = Modifier.height(Spacing.sm))
                            }
                            if (hasMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(Spacing.md),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Button(onClick = onLoadMore) {
                                            Text(Strings.Search.LOAD_MORE)
                                        }
                                    }
                                }
                            }
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
    onClick: () -> Unit,
    isFavorited: Boolean = false,
    onFavoriteToggle: (() -> Unit)? = null
) {
    // Transition must be declared at composable scope — not inside the conditional.
    // Compose requires state/animation calls to be unconditional (no if/when guards).
    val favTransition = updateTransition(isFavorited, label = "favoriteTransition")
    val favScale by favTransition.animateFloat(
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            )
        },
        label = "favoriteScale",
    ) { favorited -> if (favorited) 1.2f else 1f }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(
                    name = profile.knownName ?: profile.fullName,
                    photoUrl = profile.photoUrl,
                    size = Spacing.professionalAvatarSize
                )
                Spacer(modifier = Modifier.width(Spacing.sm + Spacing.xs))
                Column(modifier = Modifier.weight(1f)) {
                    Text(profile.knownName ?: profile.fullName, style = MaterialTheme.typography.titleMedium)
                    Text(profile.cityName, style = MaterialTheme.typography.bodySmall)
                }
                // Inline favorite button — only shown when a toggle callback is provided
                if (onFavoriteToggle != null) {
                    IconButton(onClick = onFavoriteToggle) {
                        Icon(
                            imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isFavorited) Strings.Search.REMOVE_FAVORITE else Strings.Search.ADD_FAVORITE,
                            tint = if (isFavorited) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.graphicsLayer { scaleX = favScale; scaleY = favScale },
                        )
                    }
                }
            }
            if (profile.services.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                ServiceChipList(services = profile.services, maxItems = 3)
            }
            if (profile.activeRecently || profile.profileComplete || profile.daysSinceActive != null) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                StatusChipRow(
                    activeRecently = profile.activeRecently,
                    profileComplete = profile.profileComplete,
                    daysSinceActive = profile.daysSinceActive,
                )
            }
        }
    }
}

@Composable
fun ProfessionalCardSkeleton() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShimmerBox(
                    modifier = Modifier
                        .size(Spacing.professionalAvatarSize)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(Spacing.sm + Spacing.xs))
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.55f).height(16.dp))
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.35f).height(12.dp))
                }
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                repeat(3) {
                    ShimmerBox(
                        modifier = Modifier
                            .width(60.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                }
            }
        }
    }
}

// ── Previews ──

@LightDarkScreenPreview
@Composable
private fun SearchResultsLoadingPreview() {
    AppTheme { SearchResultsScreen(query = "plumber", uiState = SearchUiState.Loading, onProfileClick = {}, onNavigateBack = {}) }
}

@LightDarkScreenPreview
@Composable
private fun SearchResultsContentPreview() {
    AppTheme {
        SearchResultsScreen(
            query = "plumber",
            uiState = SearchUiState.Success(PreviewSamples.sampleSearchResponse),
            onProfileClick = {},
            onNavigateBack = {}
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun SearchResultsEmptyPreview() {
    AppTheme {
        SearchResultsScreen(
            query = "underwater basket weaving",
            uiState = SearchUiState.Success(PreviewSamples.sampleSearchResponseEmpty),
            onProfileClick = {},
            onNavigateBack = {}
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun SearchResultsErrorPreview() {
    AppTheme {
        SearchResultsScreen(
            query = "plumber",
            uiState = SearchUiState.Error("Network error. Check your connection and try again."),
            onProfileClick = {},
            onNavigateBack = {}
        )
    }
}

@LightDarkPreview
@Composable
private fun ProfessionalCardPreview() {
    AppTheme { ProfessionalCard(profile = PreviewSamples.sampleProfile, onClick = {}) }
}

@LightDarkPreview
@Composable
private fun ProfessionalCardLongTextPreview() {
    AppTheme { ProfessionalCard(profile = PreviewSamples.sampleProfileLongText, onClick = {}) }
}

@LightDarkPreview
@Composable
private fun ProfessionalCardMinimalPreview() {
    AppTheme { ProfessionalCard(profile = PreviewSamples.sampleProfileMinimal, onClick = {}) }
}
