package com.fugisawa.quemfaz.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fugisawa.quemfaz.contract.profile.ClarificationAnswer
import com.fugisawa.quemfaz.contract.profile.InputMode
import com.fugisawa.quemfaz.contract.profile.CreateProfessionalProfileDraftResponse
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import com.fugisawa.quemfaz.contract.catalog.CatalogResponse
import com.fugisawa.quemfaz.domain.city.SupportedCities
import com.fugisawa.quemfaz.session.SessionManager
import com.fugisawa.quemfaz.ui.components.ServiceCategoryPicker
import com.fugisawa.quemfaz.ui.components.VoiceInputButton
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
    is OnboardingUiState.BirthDateRequired -> 0
    is OnboardingUiState.Idle -> 1
    is OnboardingUiState.NeedsClarification -> 1
    is OnboardingUiState.ReviewServices -> 2
    is OnboardingUiState.ReviewDescription -> 3
    is OnboardingUiState.PhotoRequired -> 4
    is OnboardingUiState.KnownName -> 5
    else -> -1
}

/** Converts epoch millis (UTC midnight) to (year, month, day) using civil_from_days algorithm. */
private fun epochMillisToDateParts(millis: Long): Triple<Int, Int, Int> {
    val totalDays = (millis / 86_400_000).toInt()
    val z = totalDays + 719468
    val era = (if (z >= 0) z else z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val y = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = mp + (if (mp < 10) 3 else -9)
    val year = y + (if (m <= 2) 1 else 0)
    return Triple(year, m, d)
}

@Composable
fun OnboardingScreens(
    uiState: OnboardingUiState,
    selectedCity: String?,
    catalog: CatalogResponse?,
    onSubmitDateOfBirth: (dateOfBirth: String) -> Unit,
    onCreateDraft: (String, InputMode) -> Unit,
    onSelectCity: (String) -> Unit,
    onProceedFromServices: (CreateProfessionalProfileDraftResponse, List<String>) -> Unit,
    onProceedWithManualServices: (CreateProfessionalProfileDraftResponse, Set<String>) -> Unit,
    onProceedFromDescription: (CreateProfessionalProfileDraftResponse, List<String>, String) -> Unit,
    onPickPhoto: () -> Unit,
    onSubmitKnownName: (fullName: String?, knownName: String?, confirmedServiceIds: List<String>, confirmedDescription: String) -> Unit,
    onSubmitClarifications: (String, List<ClarificationAnswer>) -> Unit,
    onSkipClarification: (CreateProfessionalProfileDraftResponse) -> Unit,
    onBack: () -> Unit,
    onFinish: (ProfessionalProfileResponse) -> Unit,
) {
    var inputText by remember { mutableStateOf("") }
    var usedVoice by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf(0) }

    // Update currentStep whenever uiState changes to a non-Loading, non-terminal state.
    // During Loading, currentStep stays frozen so the indicator doesn't flicker.
    LaunchedEffect(uiState) {
        currentStep = when (uiState) {
            is OnboardingUiState.BirthDateRequired -> 0
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

            val showBack = uiState is OnboardingUiState.BirthDateRequired ||
                           uiState is OnboardingUiState.Idle ||
                           uiState is OnboardingUiState.NeedsClarification ||
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
                @OptIn(ExperimentalMaterial3Api::class)
                when (state) {
                is OnboardingUiState.BirthDateRequired -> {
                    val datePickerState = rememberDatePickerState()
                    var showDatePicker by remember { mutableStateOf(false) }
                    var displayDate by remember { mutableStateOf("") }
                    var isoDate by remember { mutableStateOf("") }

                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                Strings.Onboarding.BIRTH_DATE_TITLE,
                                style = MaterialTheme.typography.headlineLarge
                            )
                            Text(
                                Strings.Onboarding.BIRTH_DATE_SUBTITLE,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(Spacing.sectionGap))

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = displayDate,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(Strings.Onboarding.BIRTH_DATE_LABEL) },
                                    placeholder = { Text(Strings.Onboarding.BIRTH_DATE_PLACEHOLDER, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                                    trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = Strings.Onboarding.SELECT_DATE) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = MaterialTheme.shapes.medium
                                )
                                // Invisible overlay to capture clicks and open date picker
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) { showDatePicker = true }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.md))

                        Button(
                            onClick = {
                                // Server validates 18+ age requirement authoritatively
                                onSubmitDateOfBirth(isoDate)
                            },
                            modifier = Modifier.fillMaxWidth().height(Spacing.ctaButtonHeight),
                            enabled = isoDate.isNotBlank(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(Strings.Common.CONTINUE, style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(Spacing.md))
                    }

                    if (showDatePicker) {
                        DatePickerDialog(
                            onDismissRequest = { showDatePicker = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    showDatePicker = false
                                    datePickerState.selectedDateMillis?.let { millis ->
                                        val (year, month, day) = epochMillisToDateParts(millis)
                                        displayDate = "${day.toString().padStart(2, '0')}/${month.toString().padStart(2, '0')}/${year.toString().padStart(4, '0')}"
                                        isoDate = "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
                                    }
                                }) { Text("OK") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDatePicker = false }) { Text(Strings.Common.CANCEL) }
                            },
                        ) {
                            DatePicker(state = datePickerState)
                        }
                    }
                }
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
                                placeholder = { Text(Strings.Onboarding.DESCRIPTION_PLACEHOLDER, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                                shape = MaterialTheme.shapes.medium
                            )

                            Spacer(modifier = Modifier.height(Spacing.sm))

                            Text(
                                Strings.Onboarding.DESCRIPTION_EXAMPLE,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(Spacing.md))

                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                VoiceInputButton(
                                    onTranscription = {
                                        inputText = it
                                        usedVoice = true
                                    },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.md))

                        Button(
                            onClick = { onCreateDraft(inputText, if (usedVoice) InputMode.VOICE else InputMode.TEXT) },
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
                                    placeholder = { Text(Strings.Onboarding.YOUR_ANSWER, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
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
                        var additionalServiceIds by remember { mutableStateOf(emptySet<String>()) }
                        var showAddServicePicker by remember { mutableStateOf(false) }

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

                                        // Show manually added services
                                        if (additionalServiceIds.isNotEmpty() && catalog != null) {
                                            Spacer(modifier = Modifier.height(Spacing.sm))
                                            Text(Strings.Onboarding.ADDITIONAL_SERVICES, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                            @OptIn(ExperimentalLayoutApi::class)
                                            FlowRow(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                            ) {
                                                additionalServiceIds.forEach { serviceId ->
                                                    val displayName = catalog.services.find { it.id == serviceId }?.displayName ?: serviceId
                                                    InputChip(
                                                        selected = true,
                                                        onClick = { additionalServiceIds = additionalServiceIds - serviceId },
                                                        label = { Text(displayName) },
                                                        trailingIcon = {
                                                            Icon(Icons.Default.Close, contentDescription = Strings.EditProfile.REMOVE, modifier = Modifier.size(16.dp))
                                                        },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(Spacing.sm))

                                // Add more services button
                                if (catalog != null) {
                                    OutlinedButton(onClick = { showAddServicePicker = true }) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(Strings.Onboarding.ADD_MORE_SERVICES)
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
                                        placeholder = { Text(Strings.Onboarding.CITY_PLACEHOLDER, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
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
                                    val allServiceIds = draft.interpretedServices.map { it.serviceId } + additionalServiceIds.toList()
                                    onProceedFromServices(draft, allServiceIds)
                                },
                                modifier = Modifier.fillMaxWidth().height(Spacing.ctaButtonHeight),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(Strings.Onboarding.LOOKS_GOOD, style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(modifier = Modifier.height(Spacing.md))
                        }

                        // Add service picker dialog
                        if (showAddServicePicker && catalog != null) {
                            val interpretedIds = draft.interpretedServices.map { it.serviceId }.toSet()
                            val alreadySelected = interpretedIds + additionalServiceIds
                            var pickerSelection by remember { mutableStateOf(emptySet<String>()) }

                            AlertDialog(
                                onDismissRequest = { showAddServicePicker = false },
                                title = { Text(Strings.Onboarding.ADD_MORE_SERVICES) },
                                text = {
                                    Box(modifier = Modifier.heightIn(max = 400.dp)) {
                                        ServiceCategoryPicker(
                                            categories = catalog.categories,
                                            services = catalog.services.filter { it.id !in alreadySelected },
                                            selectedServiceIds = pickerSelection,
                                            onSelectionChanged = { pickerSelection = it },
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        additionalServiceIds = additionalServiceIds + pickerSelection
                                        showAddServicePicker = false
                                    }) { Text(Strings.EditProfile.ADD) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showAddServicePicker = false }) { Text(Strings.Common.CANCEL) }
                                },
                            )
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
                    val sessionManager: SessionManager = koinInject()
                    val currentUser by sessionManager.currentUser.collectAsState()
                    val needsFullName = currentUser?.fullName.isNullOrBlank()
                    var fullNameInput by remember { mutableStateOf("") }
                    var knownNameInput by remember { mutableStateOf("") }

                    Column(
                        modifier = Modifier.fillMaxSize().padding(0.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Full name field — only shown when user has no fullName set
                        if (needsFullName) {
                            Text(
                                Strings.Onboarding.FULL_NAME_REQUIRED,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                            OutlinedTextField(
                                value = fullNameInput,
                                onValueChange = { fullNameInput = it },
                                label = { Text(Strings.Onboarding.FULL_NAME_LABEL) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                        }

                        Text(Strings.Auth.KNOWN_NAME_TITLE, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            Strings.Auth.KNOWN_NAME_SUBTITLE,
                            style = MaterialTheme.typography.bodyMedium,
                        )

                        OutlinedTextField(
                            value = knownNameInput,
                            onValueChange = { knownNameInput = it },
                            label = { Text(Strings.Auth.KNOWN_NAME_LABEL) },
                            placeholder = { Text(Strings.Auth.KNOWN_NAME_PLACEHOLDER, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )

                        Button(
                            onClick = {
                                val fullName = if (needsFullName) fullNameInput.trim() else null
                                onSubmitKnownName(fullName, knownNameInput.trim().ifBlank { null }, state.confirmedServiceIds, state.confirmedDescription)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !needsFullName || fullNameInput.isNotBlank(),
                        ) {
                            Text(Strings.Common.CONTINUE)
                        }

                        if (!needsFullName) {
                            TextButton(
                                onClick = { onSubmitKnownName(null, null, state.confirmedServiceIds, state.confirmedDescription) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(Strings.Auth.SKIP)
                            }
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
                        Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(onClick = onBack) {
                            Text(Strings.Errors.CHANGE_DESCRIPTION)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(onClick = { onCreateDraft(inputText, InputMode.TEXT) }) {
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
private fun OnboardingBirthDatePreview() {
    AppTheme { OnboardingScreens(uiState = OnboardingUiState.BirthDateRequired, selectedCity = null, catalog = null, onSubmitDateOfBirth = {}, onCreateDraft = { _, _ -> }, onSelectCity = {}, onProceedFromServices = { _, _ -> }, onProceedWithManualServices = { _, _ -> }, onProceedFromDescription = { _, _, _ -> }, onPickPhoto = {}, onSubmitKnownName = { _, _, _, _ -> }, onSubmitClarifications = { _, _ -> }, onSkipClarification = {}, onBack = {}, onFinish = { _ -> }) }
}

@LightDarkScreenPreview
@Composable
private fun OnboardingIdlePreview() {
    AppTheme { OnboardingScreens(uiState = OnboardingUiState.Idle, selectedCity = null, catalog = null, onSubmitDateOfBirth = {}, onCreateDraft = { _, _ -> }, onSelectCity = {}, onProceedFromServices = { _, _ -> }, onProceedWithManualServices = { _, _ -> }, onProceedFromDescription = { _, _, _ -> }, onPickPhoto = {}, onSubmitKnownName = { _, _, _, _ -> }, onSubmitClarifications = { _, _ -> }, onSkipClarification = {}, onBack = {}, onFinish = { _ -> }) }
}

@LightDarkScreenPreview
@Composable
private fun OnboardingLoadingPreview() {
    AppTheme { OnboardingScreens(uiState = OnboardingUiState.Loading, selectedCity = null, catalog = null, onSubmitDateOfBirth = {}, onCreateDraft = { _, _ -> }, onSelectCity = {}, onProceedFromServices = { _, _ -> }, onProceedWithManualServices = { _, _ -> }, onProceedFromDescription = { _, _, _ -> }, onPickPhoto = {}, onSubmitKnownName = { _, _, _, _ -> }, onSubmitClarifications = { _, _ -> }, onSkipClarification = {}, onBack = {}, onFinish = { _ -> }) }
}

@LightDarkScreenPreview
@Composable
private fun OnboardingReviewServicesPreview() {
    AppTheme {
        OnboardingScreens(
            uiState = OnboardingUiState.ReviewServices(PreviewSamples.sampleDraftResponse),
            selectedCity = "Franca",
            catalog = null,
            onSubmitDateOfBirth = {}, onCreateDraft = { _, _ -> }, onSelectCity = {}, onProceedFromServices = { _, _ -> }, onProceedWithManualServices = { _, _ -> }, onProceedFromDescription = { _, _, _ -> }, onPickPhoto = {}, onSubmitKnownName = { _, _, _, _ -> }, onSubmitClarifications = { _, _ -> }, onSkipClarification = {}, onBack = {}, onFinish = { _ -> }
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
            onSubmitDateOfBirth = {}, onCreateDraft = { _, _ -> }, onSelectCity = {}, onProceedFromServices = { _, _ -> }, onProceedWithManualServices = { _, _ -> }, onProceedFromDescription = { _, _, _ -> }, onPickPhoto = {}, onSubmitKnownName = { _, _, _, _ -> }, onSubmitClarifications = { _, _ -> }, onSkipClarification = {}, onBack = {}, onFinish = { _ -> }
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
            onSubmitDateOfBirth = {}, onCreateDraft = { _, _ -> }, onSelectCity = {}, onProceedFromServices = { _, _ -> }, onProceedWithManualServices = { _, _ -> }, onProceedFromDescription = { _, _, _ -> }, onPickPhoto = {}, onSubmitKnownName = { _, _, _, _ -> }, onSubmitClarifications = { _, _ -> }, onSkipClarification = {}, onBack = {}, onFinish = { _ -> }
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
            onSubmitDateOfBirth = {}, onCreateDraft = { _, _ -> }, onSelectCity = {}, onProceedFromServices = { _, _ -> }, onProceedWithManualServices = { _, _ -> }, onProceedFromDescription = { _, _, _ -> }, onPickPhoto = {}, onSubmitKnownName = { _, _, _, _ -> }, onSubmitClarifications = { _, _ -> }, onSkipClarification = {}, onBack = {}, onFinish = { _ -> }
        )
    }
}
