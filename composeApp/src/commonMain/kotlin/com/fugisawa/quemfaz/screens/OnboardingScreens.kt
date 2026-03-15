package com.fugisawa.quemfaz.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.fugisawa.quemfaz.ui.theme.Spacing
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

// Maps each state to its step ordinal for slide direction detection.
// Idle and NeedsClarification share index 1 — transitions between them always pass
// through Loading (which triggers a fade), so the same-index == forward shortcut is safe.
private fun OnboardingUiState.stepIndex() = when (this) {
    is OnboardingUiState.Idle -> 1
    is OnboardingUiState.NeedsClarification -> 1
    is OnboardingUiState.DraftReady -> 2
    is OnboardingUiState.PhotoRequired -> 3
    is OnboardingUiState.KnownName -> 4
    else -> -1
}

@Composable
fun OnboardingScreens(
    uiState: OnboardingUiState,
    selectedCity: String?,
    catalog: CatalogResponse?,
    onCreateDraft: (String) -> Unit,
    onSelectCity: (String) -> Unit,
    onProceedFromDraft: (CreateProfessionalProfileDraftResponse) -> Unit,
    onProceedWithManualServices: (CreateProfessionalProfileDraftResponse, Set<String>) -> Unit,
    onPickPhoto: (draft: CreateProfessionalProfileDraftResponse) -> Unit,
    onSubmitKnownName: (knownName: String?, draft: CreateProfessionalProfileDraftResponse) -> Unit,
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
            is OnboardingUiState.DraftReady -> 2
            is OnboardingUiState.PhotoRequired -> 3
            is OnboardingUiState.KnownName -> 4
            is OnboardingUiState.Loading,
            is OnboardingUiState.Published,
            is OnboardingUiState.Error -> currentStep  // no change
        }
    }

    Scaffold { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp)) {
            // Step indicator — visible for all active steps, hidden for terminal states
            val showStepIndicator = uiState !is OnboardingUiState.Published &&
                                    uiState !is OnboardingUiState.Error
            if (showStepIndicator) {
                Text(
                    text = "Step $currentStep of 4",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            val showBack = uiState is OnboardingUiState.NeedsClarification ||
                           uiState is OnboardingUiState.DraftReady ||
                           uiState is OnboardingUiState.PhotoRequired ||
                           uiState is OnboardingUiState.KnownName

            if (showBack) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.TopStart),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            }

            // Offset content below the step indicator to avoid overlap
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = if (showStepIndicator) Spacing.lg else Spacing.none)
            ) {
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
                        Text("Become a professional", style = MaterialTheme.typography.headlineLarge)
                        Text("Describe your services in your own words. We'll help you organize them.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(32.dp))

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            placeholder = { Text("e.g. I am a residential painter with 10 years of experience. I work in Batatais and Ribeirão Preto. I also do small wall repairs.") },
                            shape = MaterialTheme.shapes.medium
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { onCreateDraft(inputText) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = inputText.isNotBlank(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Analyze my services", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                is OnboardingUiState.Loading -> {
                    val phrases = listOf(
                        "Interpreting your description...",
                        "Analyzing your services...",
                        "Organizing your profile...",
                        "Almost ready..."
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
                        Text("We need a bit more info", style = MaterialTheme.typography.headlineLarge)
                        Text("Please answer the questions below so we can better understand your services.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(24.dp))

                        questions.forEachIndexed { index, question ->
                            Text(question, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = answers[index],
                                onValueChange = { answers[index] = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Your answer") },
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = {
                                val clarificationAnswers = questions.mapIndexed { index, question ->
                                    ClarificationAnswer(question, answers[index])
                                }
                                onSubmitClarifications(state.originalDescription, clarificationAnswers)
                            },
                            enabled = answers.any { it.isNotBlank() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Submit answers", style = MaterialTheme.typography.titleMedium)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { onSkipClarification(state.draft) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Skip and continue")
                        }
                    }
                }
                is OnboardingUiState.DraftReady -> {
                    val draft = state.draft
                    if (draft.interpretedServices.isEmpty()) {
                        var manualSelectedServices by remember { mutableStateOf(emptySet<String>()) }
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text("Selecione seus serviços", style = MaterialTheme.typography.headlineLarge)
                            Text("Não conseguimos identificar seus serviços automaticamente. Selecione abaixo os serviços que você oferece.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

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
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center,
                                ) {
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
                                Text("Continuar", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    } else {
                        var cityDropdownExpanded by remember { mutableStateOf(false) }

                        Column(modifier = Modifier.fillMaxSize()) {
                            Text("Review your profile", style = MaterialTheme.typography.headlineLarge)
                            Text("This is how customers will see your services.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.height(32.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Description:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                    Text(draft.normalizedDescription, style = MaterialTheme.typography.bodyLarge)

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text("Interpreted services:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                    @OptIn(ExperimentalLayoutApi::class)
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                            Spacer(modifier = Modifier.height(24.dp))

                            Text("Your city:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
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
                                    label = { Text("City") },
                                    placeholder = { Text("Select a city") },
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

                            Spacer(modifier = Modifier.weight(1f))

                            Button(
                                onClick = { onProceedFromDraft(draft) },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text("Looks good, continue", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
                is OnboardingUiState.PhotoRequired -> {
                    val sessionManager: SessionManager = koinInject()
                    val currentUser by sessionManager.currentUser.collectAsState()
                    val displayName = currentUser?.let { "${it.firstName} ${it.lastName}" } ?: ""

                    ProfilePhotoScreen(
                        currentPhotoUrl = currentUser?.photoUrl,
                        displayName = displayName,
                        headline = "Add a profile photo so clients can recognize you.",
                        showSkip = false,
                        isLoading = false,
                        error = null,
                        onPickImage = { onPickPhoto(state.draft) },
                        onSkip = null,
                    )
                }
                is OnboardingUiState.KnownName -> {
                    var knownNameInput by remember { mutableStateOf("") }

                    Column(
                        modifier = Modifier.fillMaxSize().padding(0.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text("Do you have a known name?", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "If clients know you by a nickname or trade name, enter it here.",
                            style = MaterialTheme.typography.bodyMedium,
                        )

                        OutlinedTextField(
                            value = knownNameInput,
                            onValueChange = { knownNameInput = it },
                            label = { Text("Known name (optional)") },
                            placeholder = { Text("e.g. Joãozinho da Tinta") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )

                        Button(
                            onClick = { onSubmitKnownName(knownNameInput.trim().ifBlank { null }, state.draft) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Continue")
                        }

                        TextButton(
                            onClick = { onSubmitKnownName(null, state.draft) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Skip")
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
                        Text("Profile Published!", style = MaterialTheme.typography.headlineMedium)
                        Text("You're now visible to customers.", style = MaterialTheme.typography.bodyMedium)

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = { onFinish(state.profile) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("View my profile", style = MaterialTheme.typography.titleMedium)
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
                        Text("Something went wrong", style = MaterialTheme.typography.headlineSmall)
                        Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(onClick = { onCreateDraft(inputText) }) {
                            Text("Retry")
                        }
                    }
                }
            }
            }
        }               // end Column
    }                   // end Box
}                       // end Scaffold
}

// ── Previews ──

@LightDarkScreenPreview
@Composable
private fun OnboardingIdlePreview() {
    AppTheme { OnboardingScreens(uiState = OnboardingUiState.Idle, selectedCity = null, catalog = null, onCreateDraft = {}, onSelectCity = {}, onProceedFromDraft = {}, onProceedWithManualServices = { _, _ -> }, onPickPhoto = {}, onSubmitKnownName = { _, _ -> }, onSubmitClarifications = { _, _ -> }, onSkipClarification = {}, onBack = {}, onFinish = { _ -> }) }
}

@LightDarkScreenPreview
@Composable
private fun OnboardingLoadingPreview() {
    AppTheme { OnboardingScreens(uiState = OnboardingUiState.Loading, selectedCity = null, catalog = null, onCreateDraft = {}, onSelectCity = {}, onProceedFromDraft = {}, onProceedWithManualServices = { _, _ -> }, onPickPhoto = {}, onSubmitKnownName = { _, _ -> }, onSubmitClarifications = { _, _ -> }, onSkipClarification = {}, onBack = {}, onFinish = { _ -> }) }
}

@LightDarkScreenPreview
@Composable
private fun OnboardingDraftReadyPreview() {
    AppTheme {
        OnboardingScreens(
            uiState = OnboardingUiState.DraftReady(PreviewSamples.sampleDraftResponse),
            selectedCity = "Franca",
            catalog = null,
            onCreateDraft = {}, onSelectCity = {}, onProceedFromDraft = {}, onProceedWithManualServices = { _, _ -> }, onPickPhoto = {}, onSubmitKnownName = { _, _ -> }, onSubmitClarifications = { _, _ -> }, onSkipClarification = {}, onBack = {}, onFinish = { _ -> }
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
            onCreateDraft = {}, onSelectCity = {}, onProceedFromDraft = {}, onProceedWithManualServices = { _, _ -> }, onPickPhoto = {}, onSubmitKnownName = { _, _ -> }, onSubmitClarifications = { _, _ -> }, onSkipClarification = {}, onBack = {}, onFinish = { _ -> }
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
            onCreateDraft = {}, onSelectCity = {}, onProceedFromDraft = {}, onProceedWithManualServices = { _, _ -> }, onPickPhoto = {}, onSubmitKnownName = { _, _ -> }, onSubmitClarifications = { _, _ -> }, onSkipClarification = {}, onBack = {}, onFinish = { _ -> }
        )
    }
}
