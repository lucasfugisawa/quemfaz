package com.fugisawa.quemfaz.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fugisawa.quemfaz.contract.profile.ClarificationAnswer
import com.fugisawa.quemfaz.contract.profile.CreateProfessionalProfileDraftResponse
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import com.fugisawa.quemfaz.contract.catalog.CatalogResponse
import com.fugisawa.quemfaz.domain.city.SupportedCities
import com.fugisawa.quemfaz.session.SessionManager
import com.fugisawa.quemfaz.ui.components.ServiceCategoryPicker
import com.fugisawa.quemfaz.ui.preview.LightDarkScreenPreview
import com.fugisawa.quemfaz.ui.preview.PreviewSamples
import com.fugisawa.quemfaz.ui.theme.AppTheme
import com.fugisawa.quemfaz.ui.strings.Strings
import com.fugisawa.quemfaz.ui.theme.Spacing
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

// Maps each state to its step ordinal for slide direction detection.
// Idle and NeedsClarification share index 1 — transitions between them always pass
// through Loading (which triggers a fade), so the same-index == forward shortcut is safe.
private fun OnboardingUiState.stepIndex() = when (this) {
    is OnboardingUiState.Idle -> 1
    is OnboardingUiState.NeedsClarification -> 1
    is OnboardingUiState.ReviewServices -> 2
    is OnboardingUiState.ReviewDescription -> 3
    is OnboardingUiState.PhotoRequired -> 4
    is OnboardingUiState.KnownName -> 5
    else -> -1
}

@Composable
fun OnboardingScreens(
    uiState: OnboardingUiState,
    selectedCity: String?,
    catalog: CatalogResponse?,
    onCreateDraft: (String) -> Unit,
    onSelectCity: (String) -> Unit,
    onProceedFromServices: (CreateProfessionalProfileDraftResponse, List<String>) -> Unit,
    onProceedWithManualServices: (CreateProfessionalProfileDraftResponse, Set<String>) -> Unit,
    onProceedFromDescription: (CreateProfessionalProfileDraftResponse, List<String>, String) -> Unit,
    onPickPhoto: () -> Unit,
    onSubmitKnownName: (knownName: String?, confirmedServiceIds: List<String>, confirmedDescription: String) -> Unit,
    onSubmitClarifications: (String, List<ClarificationAnswer>) -> Unit,
    onSkipClarification: (CreateProfessionalProfileDraftResponse) -> Unit,
    onBack: () -> Unit,
    onFinish: (ProfessionalProfileResponse) -> Unit,
) {
    var inputText by remember { mutableStateOf("") }
    var currentStep by remember { mutableStateOf(1) }

    // Update currentStep whenever uiState changes to a non-Loading, non-terminal state.
    // During Loading, currentStep stays frozen so the indicator doesn't flicker.
    LaunchedEffect(uiState) {
        currentStep = when (uiState) {
            is OnboardingUiState.Idle -> 1
            is OnboardingUiState.NeedsClarification -> 1
            is OnboardingUiState.ReviewServices -> 2
            is OnboardingUiState.ReviewDescription -> 3
            is OnboardingUiState.PhotoRequired -> 4
            is OnboardingUiState.KnownName -> 5
            is OnboardingUiState.Loading,
            is OnboardingUiState.Published,
            is OnboardingUiState.Error -> currentStep
        }
    }

    Scaffold { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = Spacing.screenEdge)) {
            // Step indicator — visible for all active steps, hidden for terminal states
            val showStepIndicator = uiState !is OnboardingUiState.Published &&
                                    uiState !is OnboardingUiState.Error

            val showBack = uiState is OnboardingUiState.NeedsClarification ||
                           uiState is OnboardingUiState.ReviewServices ||
                           uiState is OnboardingUiState.ReviewDescription ||
                           uiState is OnboardingUiState.PhotoRequired ||
                           uiState is OnboardingUiState.KnownName

            // Header row with properly aligned back button and step indicator
            if (showStepIndicator || showBack) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(Spacing.smallButtonHeight),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.Common.BACK)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(Spacing.smallButtonHeight))
                    }
                    if (showStepIndicator) {
                        Text(
                            text = Strings.Onboarding.stepIndicator(currentStep),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.width(Spacing.smallButtonHeight))
                }
            }
            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    val isLoadingOrTerminal = targetState is OnboardingUiState.Loading ||
                        targetState is OnboardingUiState.Error ||
                        targetState is OnboardingUiState.Published ||
                        initialState is OnboardingUiState.Loading ||
                        initialState is OnboardingUiState.Error ||
                        initialState is OnboardingUiState.Published
                    if (isLoadingOrTerminal) {
                        fadeIn() togetherWith fadeOut()
                    } else {
                        val forward = targetState.stepIndex() >= initialState.stepIndex()
                        slideInHorizontally { if (forward) it else -it } togetherWith
                            slideOutHorizontally { if (forward) -it else it }
                    }
                },
                label = "onboardingContent",
                modifier = Modifier.fillMaxSize(),
            ) { state ->
                when (state) {
                is OnboardingUiState.Idle -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                        ) {
                            Text(Strings.Onboarding.BECOME_PROFESSIONAL, style = MaterialTheme.typography.headlineLarge)
                            Text(Strings.Onboarding.DESCRIBE_SERVICES, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.height(Spacing.sectionGap))

                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                placeholder = { Text(Strings.Onboarding.DESCRIPTION_PLACEHOLDER) },
                                shape = MaterialTheme.shapes.medium
                            )
                        }

                        Spacer(modifier = Modifier.height(Spacing.md))

                        Button(
                            onClick = { onCreateDraft(inputText) },
                            modifier = Modifier.fillMaxWidth().height(Spacing.ctaButtonHeight),
                            enabled = inputText.isNotBlank(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(Strings.Onboarding.ANALYZE_SERVICES, style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(Spacing.md))
                    }
                }
                is OnboardingUiState.Loading -> {
                    val phrases = listOf(
                        Strings.Onboarding.LOADING_INTERPRETING,
                        Strings.Onboarding.LOADING_ANALYZING,
                        Strings.Onboarding.LOADING_ORGANIZING,
                        Strings.Onboarding.LOADING_ALMOST_READY
                    )
                    var phraseIndex by remember { mutableStateOf(0) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(1800L)
                            phraseIndex = (phraseIndex + 1) % phrases.size
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(Spacing.md))
                        AnimatedContent(
                            targetState = phraseIndex,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "loadingPhrase",
                        ) { index ->
                            Text(phrases[index], style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                is OnboardingUiState.NeedsClarification -> {
                    val questions = state.draft.followUpQuestions
                    val answers = remember { mutableStateListOf(*Array(questions.size) { "" }) }

                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                        ) {
                            Text(Strings.Onboarding.MORE_INFO_TITLE, style = MaterialTheme.typography.headlineLarge)
                            Text(Strings.Onboarding.MORE_INFO_SUBTITLE, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.height(Spacing.lg))

                            questions.forEachIndexed { index, question ->
                                Text(question, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                OutlinedTextField(
                                    value = answers[index],
                                    onValueChange = { answers[index] = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text(Strings.Onboarding.YOUR_ANSWER) },
                                    singleLine = true,
                                    shape = MaterialTheme.shapes.medium
                                )
                                Spacer(modifier = Modifier.height(Spacing.md))
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        Button(
                            onClick = {
                                val clarificationAnswers = questions.mapIndexed { index, question ->
                                    ClarificationAnswer(question, answers[index])
                                }
                                onSubmitClarifications(state.originalDescription, clarificationAnswers)
                            },
                            enabled = answers.any { it.isNotBlank() },
                            modifier = Modifier.fillMaxWidth().height(Spacing.ctaButtonHeight),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(Strings.Onboarding.SUBMIT_ANSWERS, style = MaterialTheme.typography.titleMedium)
                        }

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        TextButton(
                            onClick = { onSkipClarification(state.draft) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(Strings.Onboarding.SKIP_AND_CONTINUE)
                        }
                        Spacer(modifier = Modifier.height(Spacing.sm))
                    }
                }
                is OnboardingUiState.ReviewServices -> {
                    val draft = state.draft
                    if (draft.interpretedServices.isEmpty()) {
                        var manualSelectedServices by remember { mutableStateOf(emptySet<String>()) }
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(Strings.Onboarding.SELECT_SERVICES_TITLE, style = MaterialTheme.typography.headlineLarge)
                            Text(Strings.Onboarding.SELECT_SERVICES_SUBTITLE, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.height(16.dp))

                            if (catalog != null) {
                                ServiceCategoryPicker(
                                    categories = catalog.categories,
                                    services = catalog.services,
                                    selectedServiceIds = manualSelectedServices,
                                    onSelectionChanged = { manualSelectedServices = it },
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { onProceedWithManualServices(draft, manualSelectedServices) },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                enabled = manualSelectedServices.isNotEmpty(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(Strings.Common.CONTINUE, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    } else {
                        var cityDropdownExpanded by remember { mutableStateOf(false) }

                        Column(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                            ) {
                                Text(Strings.Onboarding.REVIEW_TITLE, style = MaterialTheme.typography.headlineLarge)
                                Text(Strings.Onboarding.REVIEW_SUBTITLE, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                Spacer(modifier = Modifier.height(Spacing.sectionGap))

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(Spacing.md)) {
                                        Text(Strings.Onboarding.INTERPRETED_SERVICES, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                        @OptIn(ExperimentalLayoutApi::class)
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                        ) {
                                            draft.interpretedServices.forEach { service ->
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = { Text(service.displayName) }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(Spacing.lg))

                                // City selector
                                Text(Strings.Onboarding.YOUR_CITY, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                @OptIn(ExperimentalMaterial3Api::class)
                                ExposedDropdownMenuBox(
                                    expanded = cityDropdownExpanded,
                                    onExpandedChange = { cityDropdownExpanded = it },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    OutlinedTextField(
                                        value = selectedCity ?: "",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(Strings.Onboarding.CITY_LABEL) },
                                        placeholder = { Text(Strings.Onboarding.CITY_PLACEHOLDER) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityDropdownExpanded) },
                                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                    )
                                    ExposedDropdownMenu(
                                        expanded = cityDropdownExpanded,
                                        onDismissRequest = { cityDropdownExpanded = false },
                                    ) {
                                        SupportedCities.all.forEach { city ->
                                            DropdownMenuItem(
                                                text = { Text(city) },
                                                onClick = {
                                                    onSelectCity(city)
                                                    cityDropdownExpanded = false
                                                },
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(Spacing.md))

                            Button(
                                onClick = {
                                    onProceedFromServices(draft, draft.interpretedServices.map { it.serviceId })
                                },
                                modifier = Modifier.fillMaxWidth().height(Spacing.ctaButtonHeight),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(Strings.Onboarding.LOOKS_GOOD, style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(modifier = Modifier.height(Spacing.md))
                        }
                    }
                }
                is OnboardingUiState.ReviewDescription -> {
                    var descriptionText by remember {
                        mutableStateOf(state.draft.editedDescription.ifBlank { state.draft.normalizedDescription })
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(Spacing.md),
                        ) {
                            Text(Strings.Onboarding.PROFILE_DESCRIPTION_TITLE, style = MaterialTheme.typography.headlineLarge)
                            Text(
                                Strings.Onboarding.PROFILE_DESCRIPTION_SUBTITLE,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedTextField(
                                value = descriptionText,
                                onValueChange = { descriptionText = it },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                                label = { Text(Strings.Onboarding.PROFILE_DESCRIPTION_LABEL) },
                                maxLines = 8,
                                shape = MaterialTheme.shapes.medium,
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Button(
                            onClick = { onProceedFromDescription(state.draft, state.confirmedServiceIds, descriptionText) },
                            enabled = descriptionText.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(Spacing.ctaButtonHeight),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text(Strings.Common.CONTINUE, style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(Spacing.md))
                    }
                }
                is OnboardingUiState.PhotoRequired -> {
                    val sessionManager: SessionManager = koinInject()
                    val currentUser by sessionManager.currentUser.collectAsState()
                    val displayName = currentUser?.fullName ?: ""

                    ProfilePhotoScreen(
                        currentPhotoUrl = currentUser?.photoUrl,
                        displayName = displayName,
                        headline = Strings.Auth.PHOTO_PROMPT,
                        showSkip = false,
                        isLoading = false,
                        error = null,
                        onPickImage = { onPickPhoto() },
                        onSkip = null,
                    )
                }
                is OnboardingUiState.KnownName -> {
                    var knownNameInput by remember { mutableStateOf("") }

                    Column(
                        modifier = Modifier.fillMaxSize().padding(0.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(Strings.Auth.KNOWN_NAME_TITLE, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            Strings.Auth.KNOWN_NAME_SUBTITLE,
                            style = MaterialTheme.typography.bodyMedium,
                        )

                        OutlinedTextField(
                            value = knownNameInput,
                            onValueChange = { knownNameInput = it },
                            label = { Text(Strings.Auth.KNOWN_NAME_LABEL) },
                            placeholder = { Text(Strings.Auth.KNOWN_NAME_PLACEHOLDER) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )

                        Button(
                            onClick = { onSubmitKnownName(knownNameInput.trim().ifBlank { null }, state.confirmedServiceIds, state.confirmedDescription) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(Strings.Common.CONTINUE)
                        }

                        TextButton(
                            onClick = { onSubmitKnownName(null, state.confirmedServiceIds, state.confirmedDescription) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(Strings.Auth.SKIP)
                        }
                    }
                }
                is OnboardingUiState.Published -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎉", style = MaterialTheme.typography.displayLarge)
                        Text(Strings.Onboarding.PROFILE_PUBLISHED, style = MaterialTheme.typography.headlineMedium)
                        Text(Strings.Onboarding.PROFILE_PUBLISHED_SUBTITLE, style = MaterialTheme.typography.bodyMedium)

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = { onFinish(state.profile) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(Strings.Onboarding.VIEW_MY_PROFILE, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                is OnboardingUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("❌", style = MaterialTheme.typography.displayLarge)
                        Text(Strings.Onboarding.ERROR_TITLE, style = MaterialTheme.typography.headlineSmall)
                        Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(onClick = { onCreateDraft(inputText) }) {
                            Text(Strings.Common.RETRY)
                        }
                    }
                }
            }
            }
        }               // end Column
    }                   // end Scaffold
}

// ── Previews ──

@LightDarkScreenPreview
@Composable
private fun OnboardingIdlePreview() {
    AppTheme { OnboardingScreens(uiState = OnboardingUiState.Idle, selectedCity = null, catalog = null, onCreateDraft = {}, onSelectCity = {}, onProceedFromServices = { _, _ -> }, onProceedWithManualServices = { _, _ -> }, onProceedFromDescription = { _, _, _ -> }, onPickPhoto = {}, onSubmitKnownName = { _, _, _ -> }, onSubmitClarifications = { _, _ -> }, onSkipClarification = {}, onBack = {}, onFinish = { _ -> }) }
}

@LightDarkScreenPreview
@Composable
private fun OnboardingLoadingPreview() {
    AppTheme { OnboardingScreens(uiState = OnboardingUiState.Loading, selectedCity = null, catalog = null, onCreateDraft = {}, onSelectCity = {}, onProceedFromServices = { _, _ -> }, onProceedWithManualServices = { _, _ -> }, onProceedFromDescription = { _, _, _ -> }, onPickPhoto = {}, onSubmitKnownName = { _, _, _ -> }, onSubmitClarifications = { _, _ -> }, onSkipClarification = {}, onBack = {}, onFinish = { _ -> }) }
}

@LightDarkScreenPreview
@Composable
private fun OnboardingReviewServicesPreview() {
    AppTheme {
        OnboardingScreens(
            uiState = OnboardingUiState.ReviewServices(PreviewSamples.sampleDraftResponse),
            selectedCity = "Franca",
            catalog = null,
            onCreateDraft = {}, onSelectCity = {}, onProceedFromServices = { _, _ -> }, onProceedWithManualServices = { _, _ -> }, onProceedFromDescription = { _, _, _ -> }, onPickPhoto = {}, onSubmitKnownName = { _, _, _ -> }, onSubmitClarifications = { _, _ -> }, onSkipClarification = {}, onBack = {}, onFinish = { _ -> }
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun OnboardingReviewDescriptionPreview() {
    AppTheme {
        OnboardingScreens(
            uiState = OnboardingUiState.ReviewDescription(
                PreviewSamples.sampleDraftResponse,
                listOf("paint-residential"),
            ),
            selectedCity = "Franca",
            catalog = null,
            onCreateDraft = {}, onSelectCity = {}, onProceedFromServices = { _, _ -> }, onProceedWithManualServices = { _, _ -> }, onProceedFromDescription = { _, _, _ -> }, onPickPhoto = {}, onSubmitKnownName = { _, _, _ -> }, onSubmitClarifications = { _, _ -> }, onSkipClarification = {}, onBack = {}, onFinish = { _ -> }
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun OnboardingPublishedPreview() {
    AppTheme {
        OnboardingScreens(
            uiState = OnboardingUiState.Published(PreviewSamples.sampleProfile),
            selectedCity = null,
            catalog = null,
            onCreateDraft = {}, onSelectCity = {}, onProceedFromServices = { _, _ -> }, onProceedWithManualServices = { _, _ -> }, onProceedFromDescription = { _, _, _ -> }, onPickPhoto = {}, onSubmitKnownName = { _, _, _ -> }, onSubmitClarifications = { _, _ -> }, onSkipClarification = {}, onBack = {}, onFinish = { _ -> }
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun OnboardingErrorPreview() {
    AppTheme {
        OnboardingScreens(
            uiState = OnboardingUiState.Error("AI service is temporarily unavailable. Please try again in a few minutes."),
            selectedCity = null,
            catalog = null,
            onCreateDraft = {}, onSelectCity = {}, onProceedFromServices = { _, _ -> }, onProceedWithManualServices = { _, _ -> }, onProceedFromDescription = { _, _, _ -> }, onPickPhoto = {}, onSubmitKnownName = { _, _, _ -> }, onSubmitClarifications = { _, _ -> }, onSkipClarification = {}, onBack = {}, onFinish = { _ -> }
        )
    }
}
