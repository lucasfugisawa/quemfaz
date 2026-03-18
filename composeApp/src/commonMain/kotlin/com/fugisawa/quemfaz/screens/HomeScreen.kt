package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.fugisawa.quemfaz.contract.auth.UserProfileResponse
import com.fugisawa.quemfaz.contract.search.PopularServicesResponse
import com.fugisawa.quemfaz.ui.components.ProfileAvatar
import com.fugisawa.quemfaz.ui.components.VoiceInputButton
import com.fugisawa.quemfaz.ui.strings.Strings
import com.fugisawa.quemfaz.ui.preview.LightDarkScreenPreview
import com.fugisawa.quemfaz.ui.preview.PreviewSamples
import com.fugisawa.quemfaz.ui.theme.AppTheme
import com.fugisawa.quemfaz.ui.theme.Spacing
import com.fugisawa.quemfaz.ui.theme.TagBlueBg
import com.fugisawa.quemfaz.ui.theme.TagBlueText
import com.fugisawa.quemfaz.ui.theme.TagGreenBg
import com.fugisawa.quemfaz.ui.theme.TagGreenText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentUser: UserProfileResponse?,
    currentCity: String?,
    showEarnMoneyCard: Boolean,
    popularServices: PopularServicesResponse?,
    searchHistory: List<String>,
    onCityClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSearch: (String) -> Unit,
    onOfferServices: () -> Unit,
    onDismissOfferServices: () -> Unit,
    onPopularServiceClick: (String) -> Unit,
    onNavigateToCategoryBrowsing: () -> Unit,
    onVoiceInput: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.screenEdge)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(Spacing.lg))

        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    Strings.Home.GREETING,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    Strings.Home.WHAT_LOOKING_FOR,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = "\uD83D\uDCCD ${currentCity ?: Strings.Home.SELECT_CITY}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onCityClick() },
                )
            }
            ProfileAvatar(
                name = currentUser?.fullName?.ifBlank { null },
                photoUrl = currentUser?.photoUrl,
                size = Spacing.homeAvatarSize,
                modifier = Modifier.clickable { onProfileClick() },
            )
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        // Smart search input with inline voice button
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    Strings.Home.SEARCH_HINT,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            },
            leadingIcon = {
                VoiceInputButton(
                    compact = true,
                    onTranscription = { transcription ->
                        query = transcription
                        onVoiceInput(transcription)
                        if (transcription.isNotBlank() && currentCity != null) {
                            onSearch(transcription)
                        }
                    },
                )
            },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (query.isNotBlank() && currentCity != null) {
                        onSearch(query)
                    }
                }
            ),
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        // Helper text
        Text(
            Strings.Home.SPEAK_OR_TYPE,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        // Example phrases
        Text(
            Strings.Home.EXAMPLE_HEADER,
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                Text(
                    Strings.Home.EXAMPLE_1,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    Strings.Home.EXAMPLE_2,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        // Search history chips
        if (searchHistory.isNotEmpty()) {
            Text(
                Strings.Home.RECENT_SEARCHES,
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                itemsIndexed(searchHistory) { index, historyItem ->
                    val isEven = index % 2 == 0
                    SuggestionChip(
                        onClick = {
                            query = historyItem
                            onSearch(historyItem)
                        },
                        label = { Text(historyItem) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isEven) TagBlueBg else TagGreenBg,
                            labelColor = if (isEven) TagBlueText else TagGreenText,
                        ),
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.md))
        }

        // Popular services
        if (popularServices != null && popularServices.services.isNotEmpty()) {
            Text(
                text = if (popularServices.isLocalResults) "Mais buscados na sua cidade"
                       else "Mais buscados no QuemFaz",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(popularServices.services) { service ->
                    SuggestionChip(
                        onClick = {
                            query = service.displayName
                            onPopularServiceClick(service.displayName)
                        },
                        label = { Text(service.displayName) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.md))
        }

        // Category browsing link
        TextButton(
            onClick = onNavigateToCategoryBrowsing,
        ) {
            Text("Ver todas as categorias")
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        // Earn money card
        if (showEarnMoneyCard) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.lg),
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    onClick = onOfferServices
                ) {
                    Row(
                        modifier = Modifier.padding(start = Spacing.screenEdge, top = Spacing.screenEdge, bottom = Spacing.screenEdge, end = Spacing.xl).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(Strings.Home.OFFER_SERVICES, style = MaterialTheme.typography.titleMedium)
                            Text(
                                Strings.Home.OFFER_SERVICES_DESCRIPTION,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                        Text("\uD83D\uDE80", style = MaterialTheme.typography.headlineLarge)
                    }
                }
                IconButton(
                    onClick = onDismissOfferServices,
                    modifier = Modifier.align(Alignment.TopEnd).size(36.dp),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = Strings.Home.DISMISS,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}

// -- Previews --

@LightDarkScreenPreview
@Composable
private fun HomeScreenWithCityPreview() {
    AppTheme {
        HomeScreen(
            currentUser = PreviewSamples.sampleUser,
            currentCity = "São Paulo",
            showEarnMoneyCard = true,
            popularServices = null,
            searchHistory = listOf("Eletricista", "Pintor"),
            onCityClick = {},
            onProfileClick = {},
            onSearch = {},
            onOfferServices = {},
            onDismissOfferServices = {},
            onPopularServiceClick = {},
            onNavigateToCategoryBrowsing = {},
            onVoiceInput = {},
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun HomeScreenNoCityPreview() {
    AppTheme {
        HomeScreen(
            currentUser = PreviewSamples.sampleUser,
            currentCity = null,
            showEarnMoneyCard = true,
            popularServices = null,
            searchHistory = emptyList(),
            onCityClick = {},
            onProfileClick = {},
            onSearch = {},
            onOfferServices = {},
            onDismissOfferServices = {},
            onPopularServiceClick = {},
            onNavigateToCategoryBrowsing = {},
            onVoiceInput = {},
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun HomeScreenProfessionalPreview() {
    AppTheme {
        HomeScreen(
            currentUser = PreviewSamples.sampleUser,
            currentCity = "São Paulo",
            showEarnMoneyCard = false,
            popularServices = null,
            searchHistory = emptyList(),
            onCityClick = {},
            onProfileClick = {},
            onSearch = {},
            onOfferServices = {},
            onDismissOfferServices = {},
            onPopularServiceClick = {},
            onNavigateToCategoryBrowsing = {},
            onVoiceInput = {},
        )
    }
}
