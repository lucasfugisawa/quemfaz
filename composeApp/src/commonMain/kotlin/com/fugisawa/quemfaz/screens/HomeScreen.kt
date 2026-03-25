package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.screenEdge)
                .verticalScroll(rememberScrollState())
                .then(
                    if (showEarnMoneyCard) Modifier.padding(bottom = 120.dp)
                    else Modifier.padding(bottom = Spacing.lg)
                ),
        ) {
            Spacer(modifier = Modifier.height(Spacing.lg))

            // Header row: heading + avatar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    Strings.Home.WHAT_LOOKING_FOR,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                )
                ProfileAvatar(
                    name = currentUser?.fullName?.ifBlank { null },
                    photoUrl = currentUser?.photoUrl,
                    size = Spacing.homeAvatarSize,
                    modifier = Modifier.clickable { onProfileClick() },
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // City selector — prominent, clearly tappable
            Surface(
                onClick = onCityClick,
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = currentCity ?: Strings.Home.SELECT_CITY,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Smart search input — multi-line, mic with left padding
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
                    Box(modifier = Modifier.padding(start = Spacing.xs)) {
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
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = false,
                maxLines = 3,
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

            // Rotating example phrase
            Text(
                Strings.Home.EXAMPLE_HEADER,
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            RotatingExampleBubble()

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

            // Popular services — wrapped layout
            if (popularServices != null && popularServices.services.isNotEmpty()) {
                Text(
                    text = if (popularServices.isLocalResults) "Mais buscados na sua cidade"
                    else "Mais buscados no QuemFaz",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.none),
                ) {
                    popularServices.services.take(8).forEach { service ->
                        SuggestionChip(
                            onClick = {
                                query = service.displayName
                                onPopularServiceClick(service.displayName)
                            },
                            label = { Text(service.displayName) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.md))
            }

            // Category browsing link with arrow
            TextButton(
                onClick = onNavigateToCategoryBrowsing,
            ) {
                Text(Strings.Home.VIEW_ALL_CATEGORIES)
                Spacer(modifier = Modifier.width(Spacing.xs))
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }

            // Extra space at bottom if no CTA
            if (!showEarnMoneyCard) {
                Spacer(modifier = Modifier.height(Spacing.md))
            }
        }

        // Floating CTA card anchored near bottom
        if (showEarnMoneyCard) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenEdge, vertical = Spacing.md),
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    onClick = onOfferServices,
                ) {
                    Row(
                        modifier = Modifier
                            .padding(start = Spacing.screenEdge, top = Spacing.md, bottom = Spacing.md, end = Spacing.xl)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                Strings.Home.OFFER_SERVICES,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Text(
                                Strings.Home.OFFER_SERVICES_SUBTITLE,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                        Spacer(modifier = Modifier.width(Spacing.sm))
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
        }
    }
}

@Composable
private fun RotatingExampleBubble() {
    val phrases = Strings.Home.EXAMPLE_PHRASES

    // Shuffle-cycle: consume a shuffled order, reshuffle when exhausted,
    // avoiding the same phrase at the boundary between cycles.
    val shuffledQueue = remember { mutableStateListOf<String>() }
    var currentPhrase by remember { mutableStateOf("") }
    var displayedChars by remember { mutableStateOf(0) }
    var advanceTrigger by remember { mutableStateOf(0) }

    fun nextPhrase(): String {
        if (shuffledQueue.isEmpty()) {
            val shuffled = phrases.shuffled().toMutableList()
            // Avoid repeating the last shown phrase at the boundary
            if (shuffled.firstOrNull() == currentPhrase && shuffled.size > 1) {
                val swap = (1 until shuffled.size).random()
                shuffled[0] = shuffled[swap].also { shuffled[swap] = shuffled[0] }
            }
            shuffledQueue.addAll(shuffled)
        }
        return shuffledQueue.removeFirst()
    }

    // Pick initial phrase once
    LaunchedEffect(Unit) {
        currentPhrase = nextPhrase()
    }

    LaunchedEffect(advanceTrigger) {
        if (currentPhrase.isEmpty()) return@LaunchedEffect
        // Type out the phrase character by character
        displayedChars = 0
        for (i in 1..currentPhrase.length) {
            displayedChars = i
            delay(35) // typing speed
        }
        // Pause after fully typed
        delay(2500)
        // Move to next phrase
        currentPhrase = nextPhrase()
        advanceTrigger++
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.md),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                currentPhrase.take(displayedChars),
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                // Reserve full height so the bubble doesn't resize
                minLines = 1,
            )
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
