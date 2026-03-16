package com.fugisawa.quemfaz.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.unit.sp
import com.fugisawa.quemfaz.contract.auth.UserProfileResponse
import com.fugisawa.quemfaz.contract.search.PopularServicesResponse
import com.fugisawa.quemfaz.ui.components.ProfileAvatar
import com.fugisawa.quemfaz.ui.components.VoiceInputButton
import com.fugisawa.quemfaz.ui.strings.Strings
import com.fugisawa.quemfaz.ui.preview.LightDarkScreenPreview
import com.fugisawa.quemfaz.ui.preview.PreviewSamples
import com.fugisawa.quemfaz.ui.theme.AppTheme
import com.fugisawa.quemfaz.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentUser: UserProfileResponse?,
    currentCity: String?,
    showEarnMoneyCard: Boolean,
    popularServices: PopularServicesResponse?,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.clickable { onCityClick() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("\uD83D\uDCCD", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            currentCity ?: Strings.Home.SELECT_CITY,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    ProfileAvatar(
                        name = currentUser?.fullName?.ifBlank { null },
                        photoUrl = currentUser?.photoUrl,
                        size = 32.dp,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clickable { onProfileClick() }
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                Strings.Home.TITLE,
                style = MaterialTheme.typography.displayMedium,
                lineHeight = 52.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Diga ou digite o que voc\u00EA precisa...") },
                leadingIcon = { Text("\uD83D\uDD0D", modifier = Modifier.padding(start = 8.dp)) },
                shape = MaterialTheme.shapes.large,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            Text(
                "Ex: \"Preciso de algu\u00E9m para pintar minha casa\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                VoiceInputButton(
                    onTranscription = { transcription ->
                        query = transcription
                        onVoiceInput(transcription)
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            val isSearchEnabled = query.isNotBlank() && currentCity != null
            val searchButtonScale by animateFloatAsState(
                targetValue = if (isSearchEnabled) 1f else 0.96f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "searchButtonScale",
            )

            Button(
                onClick = { onSearch(query) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Spacing.ctaButtonHeight)
                    .graphicsLayer { scaleX = searchButtonScale; scaleY = searchButtonScale },
                enabled = isSearchEnabled,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(Strings.Home.SEARCH, style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Popular searches section
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
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        onClick = onOfferServices
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 24.dp, end = 48.dp).fillMaxWidth(),
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
                Spacer(modifier = Modifier.height(24.dp))
            }
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
            currentCity = "S\u00E3o Paulo",
            showEarnMoneyCard = true,
            popularServices = null,
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
            currentCity = "S\u00E3o Paulo",
            showEarnMoneyCard = false,
            popularServices = null,
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
