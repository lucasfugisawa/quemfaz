package com.fugisawa.quemfaz.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
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
import com.fugisawa.quemfaz.ui.components.ChatBubble
import com.fugisawa.quemfaz.ui.components.ProfileAvatar
import com.fugisawa.quemfaz.ui.components.ServiceCategoryPicker
import com.fugisawa.quemfaz.ui.components.ServiceListItem
import com.fugisawa.quemfaz.ui.components.VoiceInputButton
import com.fugisawa.quemfaz.ui.preview.LightDarkScreenPreview
import com.fugisawa.quemfaz.ui.preview.PreviewSamples
import com.fugisawa.quemfaz.ui.theme.AppTheme
import com.fugisawa.quemfaz.ui.strings.Strings
import com.fugisawa.quemfaz.ui.theme.Spacing
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

// Maps each state to its step ordinal for slide direction detection.
private fun OnboardingUiState.stepIndex() = when (this) {
    is OnboardingUiState.BirthDateRequired -> 0
    is OnboardingUiState.NaturalPresentation -> 1
    is OnboardingUiState.NeedsClarification -> 1
    is OnboardingUiState.SmartConfirmation -> 2
    is OnboardingUiState.PhotoRequired -> 2
    is OnboardingUiState.ProfilePreview -> 3
    else -> -1
}

/** Returns true if the person born on [birthMillis] is at least 18 years old relative to [nowMillis]. */
private fun isAtLeast18(birthMillis: Long, nowMillis: Long): Boolean {
    val (by, bm, bd) = epochMillisToDateParts(birthMillis)
    val (ny, nm, nd) = epochMillisToDateParts(nowMillis)
    val age = ny - by - (if (nm < bm || (nm == bm && nd < bd)) 1 else 0)
    return age >= 18
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
    onConfirmFromSmartConfirmation: (CreateProfessionalProfileDraftResponse, List<String>, String) -> Unit,
    onProceedWithManualServices: (CreateProfessionalProfileDraftResponse, Set<String>) -> Unit,
    onPickPhoto: () -> Unit,
    onPublishProfile: (fullName: String?, knownName: String?, confirmedServiceIds: List<String>, confirmedDescription: String) -> Unit,
    onSubmitClarifications: (String, List<ClarificationAnswer>) -> Unit,
    onSkipClarification: (CreateProfessionalProfileDraftResponse) -> Unit,
    onBack: () -> Unit,
    onFinish: (ProfessionalProfileResponse) -> Unit,
) {
    var inputText by remember { mutableStateOf("") }
    var usedVoice by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf(0) }

    // Update currentStep whenever uiState changes to a non-Loading, non-terminal state.
    LaunchedEffect(uiState) {
        currentStep = when (uiState) {
            is OnboardingUiState.BirthDateRequired -> 0
            is OnboardingUiState.NaturalPresentation -> 1
            is OnboardingUiState.NeedsClarification -> 1
            is OnboardingUiState.SmartConfirmation -> 2
            is OnboardingUiState.PhotoRequired -> 2
            is OnboardingUiState.ProfilePreview -> 3
            is OnboardingUiState.Loading,
            is OnboardingUiState.Published,
            is OnboardingUiState.Error -> currentStep
        }
    }

    Scaffold { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = Spacing.screenEdge)) {
            val showStepIndicator = uiState !is OnboardingUiState.Published &&
                                    uiState !is OnboardingUiState.Error

            val showBack = uiState is OnboardingUiState.BirthDateRequired ||
                           uiState is OnboardingUiState.NaturalPresentation ||
                           uiState is OnboardingUiState.NeedsClarification ||
                           uiState is OnboardingUiState.SmartConfirmation ||
                           uiState is OnboardingUiState.PhotoRequired ||
                           uiState is OnboardingUiState.ProfilePreview

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
                    // Capture current time once at composition — displayedMonthMillis defaults to "now"
                    val nowMillis = remember { datePickerState.displayedMonthMillis }
                    var showDatePicker by remember { mutableStateOf(false) }
                    var displayDate by remember { mutableStateOf("") }
                    var isoDate by remember { mutableStateOf("") }
                    var selectedMillis by remember { mutableStateOf<Long?>(null) }
                    val isUnderage = selectedMillis?.let { !isAtLeast18(it, nowMillis) } ?: false

                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                Strings.Onboarding.BIRTH_DATE_TITLE,
                                style = MaterialTheme.typography.headlineLarge,
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Text(
                                Strings.Onboarding.BIRTH_DATE_SUBTITLE,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                    isError = isUnderage,
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = MaterialTheme.shapes.medium,
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) { showDatePicker = true }
                                )
                            }

                            if (isUnderage) {
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                Text(
                                    Strings.Onboarding.BIRTH_DATE_UNDERAGE,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.md))

                        Button(
                            onClick = { onSubmitDateOfBirth(isoDate) },
                            modifier = Modifier.fillMaxWidth().height(Spacing.ctaButtonHeight),
                            enabled = isoDate.isNotBlank() && !isUnderage,
                            shape = MaterialTheme.shapes.medium,
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
                                        selectedMillis = millis
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
                is OnboardingUiState.NaturalPresentation -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                        ) {
                            ChatBubble {
                                Text(
                                    Strings.Onboarding.STEP_1_MESSAGE,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }

                            Spacer(modifier = Modifier.height(Spacing.lg))

                            Button(
                                onClick = {
                                    usedVoice = true
                                    // Voice input button below handles transcription
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Text(Strings.Onboarding.SPEAK_WHAT_I_DO)
                            }

                            Spacer(modifier = Modifier.height(Spacing.md))

                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                VoiceInputButton(
                                    onTranscription = {
                                        inputText = it
                                        usedVoice = true
                                    },
                                )
                            }

                            Spacer(modifier = Modifier.height(Spacing.md))

                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                modifier = Modifier.fillMaxWidth().height(150.dp),
                                placeholder = { Text(Strings.Onboarding.DESCRIPTION_PLACEHOLDER, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                                shape = MaterialTheme.shapes.medium
                            )

                            Spacer(modifier = Modifier.height(Spacing.sm))

                            Text(
                                Strings.Onboarding.SPEAK_NATURALLY_HINT,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Spacer(modifier = Modifier.height(Spacing.md))

                        Button(
                            onClick = { onCreateDraft(inputText, if (usedVoice) InputMode.VOICE else InputMode.TEXT) },
                            modifier = Modifier.fillMaxWidth().height(Spacing.ctaButtonHeight),
                            enabled = inputText.isNotBlank(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(Strings.Common.CONTINUE, style = MaterialTheme.typography.titleMedium)
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
                            ChatBubble {
                                Text(
                                    Strings.Onboarding.MORE_INFO_TITLE,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Spacer(modifier = Modifier.height(Spacing.xs))
                                Text(
                                    Strings.Onboarding.MORE_INFO_SUBTITLE,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }

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
                is OnboardingUiState.SmartConfirmation -> {
                    val draft = state.draft

                    if (draft.interpretedServices.isEmpty()) {
                        // Manual service selection fallback
                        var manualSelectedServices by remember { mutableStateOf(emptySet<String>()) }
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(Strings.Onboarding.SELECT_SERVICES_TITLE, style = MaterialTheme.typography.headlineLarge)
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Text(Strings.Onboarding.SELECT_SERVICES_SUBTITLE, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.height(Spacing.md))

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

                            Spacer(modifier = Modifier.height(Spacing.md))

                            Button(
                                onClick = { onProceedWithManualServices(draft, manualSelectedServices) },
                                modifier = Modifier.fillMaxWidth().height(Spacing.ctaButtonHeight),
                                enabled = manualSelectedServices.isNotEmpty(),
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Text(Strings.Common.CONTINUE, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    } else {
                        // Smart confirmation: merged ReviewServices + ReviewDescription
                        var cityDropdownExpanded by remember { mutableStateOf(false) }
                        var confirmedServiceIds by remember { mutableStateOf(state.confirmedServiceIds) }
                        var additionalServiceIds by remember { mutableStateOf(emptySet<String>()) }
                        var showAddServicePicker by remember { mutableStateOf(false) }
                        var descriptionText by remember { mutableStateOf(state.confirmedDescription) }

                        Column(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                            ) {
                                ChatBubble {
                                    Text(
                                        Strings.Onboarding.STEP_2_MESSAGE,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }

                                Spacer(modifier = Modifier.height(Spacing.md))

                                // Services list with remove buttons
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(Spacing.md)) {
                                        confirmedServiceIds.forEach { serviceId ->
                                            val displayName = draft.interpretedServices.find { it.serviceId == serviceId }?.displayName
                                                ?: catalog?.services?.find { it.id == serviceId }?.displayName
                                                ?: serviceId
                                            ServiceListItem(
                                                serviceName = displayName,
                                                trailingContent = {
                                                    IconButton(
                                                        onClick = { confirmedServiceIds = confirmedServiceIds - serviceId },
                                                        modifier = Modifier.size(24.dp),
                                                    ) {
                                                        Icon(Icons.Default.Close, contentDescription = Strings.EditProfile.REMOVE, modifier = Modifier.size(16.dp))
                                                    }
                                                },
                                            )
                                        }

                                        // Additional manually-added services
                                        additionalServiceIds.forEach { serviceId ->
                                            val displayName = catalog?.services?.find { it.id == serviceId }?.displayName ?: serviceId
                                            ServiceListItem(
                                                serviceName = displayName,
                                                trailingContent = {
                                                    IconButton(
                                                        onClick = { additionalServiceIds = additionalServiceIds - serviceId },
                                                        modifier = Modifier.size(24.dp),
                                                    ) {
                                                        Icon(Icons.Default.Close, contentDescription = Strings.EditProfile.REMOVE, modifier = Modifier.size(16.dp))
                                                    }
                                                },
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(Spacing.sm))

                                // Add more services
                                if (catalog != null) {
                                    TextButton(onClick = { showAddServicePicker = true }) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(Spacing.xs))
                                        Text(Strings.Onboarding.ADD_SERVICE)
                                    }
                                }

                                Spacer(modifier = Modifier.height(Spacing.md))

                                // Editable description
                                Text(Strings.Onboarding.PROFILE_DESCRIPTION_LABEL, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                OutlinedTextField(
                                    value = descriptionText,
                                    onValueChange = { descriptionText = it },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                                    maxLines = 6,
                                    shape = MaterialTheme.shapes.medium,
                                )

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

                            // Primary action: confirm
                            Button(
                                onClick = {
                                    val allServiceIds = confirmedServiceIds + additionalServiceIds.toList()
                                    onConfirmFromSmartConfirmation(draft, allServiceIds, descriptionText)
                                },
                                modifier = Modifier.fillMaxWidth().height(Spacing.ctaButtonHeight),
                                enabled = (confirmedServiceIds.isNotEmpty() || additionalServiceIds.isNotEmpty()) && descriptionText.isNotBlank(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(Strings.Onboarding.THATS_CORRECT, style = MaterialTheme.typography.titleMedium)
                            }

                            Spacer(modifier = Modifier.height(Spacing.sm))

                            // Secondary action: go back to explain better
                            OutlinedButton(
                                onClick = onBack,
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Text(Strings.Onboarding.LET_ME_EXPLAIN)
                            }
                            Spacer(modifier = Modifier.height(Spacing.md))
                        }

                        // Add service picker dialog
                        if (showAddServicePicker && catalog != null) {
                            val alreadySelected = confirmedServiceIds.toSet() + additionalServiceIds
                            var pickerSelection by remember { mutableStateOf(emptySet<String>()) }

                            AlertDialog(
                                onDismissRequest = { showAddServicePicker = false },
                                title = { Text(Strings.Onboarding.ADD_SERVICE) },
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
                is OnboardingUiState.ProfilePreview -> {
                    val sessionManager: SessionManager = koinInject()
                    val currentUser by sessionManager.currentUser.collectAsState()
                    val needsFullName = currentUser?.fullName.isNullOrBlank()
                    var fullNameInput by remember { mutableStateOf("") }
                    var knownNameInput by remember { mutableStateOf("") }

                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        ) {
                            ChatBubble {
                                Text(
                                    Strings.Onboarding.STEP_3_MESSAGE,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }

                            Spacer(modifier = Modifier.height(Spacing.md))

                            // Profile preview card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            ) {
                                Column(modifier = Modifier.padding(Spacing.md)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ProfileAvatar(
                                            name = currentUser?.fullName,
                                            photoUrl = currentUser?.photoUrl,
                                            size = Spacing.professionalAvatarSize,
                                        )
                                        Spacer(modifier = Modifier.width(Spacing.md))
                                        Column {
                                            if (knownNameInput.isNotBlank()) {
                                                Text(knownNameInput, style = MaterialTheme.typography.titleMedium)
                                            }
                                            Text(
                                                currentUser?.fullName ?: fullNameInput.ifBlank { "" },
                                                style = MaterialTheme.typography.bodyMedium,
                                            )
                                            Text(
                                                selectedCity ?: "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(Spacing.sm))

                                    Text(
                                        state.confirmedDescription,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )

                                    Spacer(modifier = Modifier.height(Spacing.sm))

                                    state.confirmedServiceIds.forEach { serviceId ->
                                        val displayName = state.draft.interpretedServices.find { it.serviceId == serviceId }?.displayName ?: serviceId
                                        ServiceListItem(serviceName = displayName)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(Spacing.lg))

                            // Full name field — only shown when user has no fullName set
                            if (needsFullName) {
                                Text(
                                    Strings.Onboarding.FULL_NAME_REQUIRED,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                OutlinedTextField(
                                    value = fullNameInput,
                                    onValueChange = { fullNameInput = it },
                                    label = { Text(Strings.Onboarding.FULL_NAME_LABEL) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                )
                                Spacer(modifier = Modifier.height(Spacing.md))
                            }

                            // Known name (optional)
                            Text(Strings.Auth.KNOWN_NAME_TITLE, style = MaterialTheme.typography.titleSmall)
                            Text(
                                Strings.Auth.KNOWN_NAME_SUBTITLE,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            OutlinedTextField(
                                value = knownNameInput,
                                onValueChange = { knownNameInput = it },
                                label = { Text(Strings.Auth.KNOWN_NAME_LABEL) },
                                placeholder = { Text(Strings.Auth.KNOWN_NAME_PLACEHOLDER, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                        }

                        Spacer(modifier = Modifier.height(Spacing.md))

                        // Primary: publish
                        Button(
                            onClick = {
                                val fullName = if (needsFullName) fullNameInput.trim() else null
                                onPublishProfile(fullName, knownNameInput.trim().ifBlank { null }, state.confirmedServiceIds, state.confirmedDescription)
                            },
                            modifier = Modifier.fillMaxWidth().height(Spacing.ctaButtonHeight),
                            enabled = !needsFullName || fullNameInput.isNotBlank(),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text(Strings.Onboarding.PUBLISH_PROFILE, style = MaterialTheme.typography.titleMedium)
                        }

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        // Secondary: go back to edit
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text(Strings.Onboarding.EDIT_DESCRIPTION)
                        }
                        Spacer(modifier = Modifier.height(Spacing.md))
                    }
                }
                is OnboardingUiState.Published -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("🎉", style = MaterialTheme.typography.displayLarge)
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Text(Strings.Onboarding.PROFILE_PUBLISHED, style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            Strings.Onboarding.PROFILE_PUBLISHED_SUBTITLE,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(Spacing.sectionGap))

                        Button(
                            onClick = { onFinish(state.profile) },
                            modifier = Modifier.fillMaxWidth().height(Spacing.ctaButtonHeight),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text(Strings.Onboarding.VIEW_MY_PROFILE, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                is OnboardingUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("❌", style = MaterialTheme.typography.displayLarge)
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Text(Strings.Onboarding.ERROR_TITLE, style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(Spacing.lg))

                        when (state.source) {
                            OnboardingErrorSource.DESCRIPTION -> {
                                Button(
                                    onClick = onBack,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                ) {
                                    Text(Strings.Errors.CHANGE_DESCRIPTION)
                                }
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                OutlinedButton(
                                    onClick = { onCreateDraft(inputText, InputMode.TEXT) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                ) {
                                    Text(Strings.Common.RETRY)
                                }
                            }
                            else -> {
                                // Birth date, photo, publish errors — generic "Voltar"
                                Button(
                                    onClick = onBack,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                ) {
                                    Text(Strings.Common.BACK)
                                }
                            }
                        }
                    }
                }
            }
            }
        }               // end Column
    }                   // end Scaffold
}

// ── Previews ──

private val previewCallbacks: @Composable (OnboardingUiState) -> @Composable () -> Unit = { uiState ->
    @Composable {
        OnboardingScreens(
            uiState = uiState,
            selectedCity = "Franca",
            catalog = null,
            onSubmitDateOfBirth = {},
            onCreateDraft = { _, _ -> },
            onSelectCity = {},
            onConfirmFromSmartConfirmation = { _, _, _ -> },
            onProceedWithManualServices = { _, _ -> },
            onPickPhoto = {},
            onPublishProfile = { _, _, _, _ -> },
            onSubmitClarifications = { _, _ -> },
            onSkipClarification = {},
            onBack = {},
            onFinish = { _ -> },
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun OnboardingBirthDatePreview() {
    AppTheme { OnboardingScreens(uiState = OnboardingUiState.BirthDateRequired, selectedCity = null, catalog = null, onSubmitDateOfBirth = {}, onCreateDraft = { _, _ -> }, onSelectCity = {}, onConfirmFromSmartConfirmation = { _, _, _ -> }, onProceedWithManualServices = { _, _ -> }, onPickPhoto = {}, onPublishProfile = { _, _, _, _ -> }, onSubmitClarifications = { _, _ -> }, onSkipClarification = {}, onBack = {}, onFinish = { _ -> }) }
}

@LightDarkScreenPreview
@Composable
private fun OnboardingNaturalPresentationPreview() {
    AppTheme { OnboardingScreens(uiState = OnboardingUiState.NaturalPresentation, selectedCity = null, catalog = null, onSubmitDateOfBirth = {}, onCreateDraft = { _, _ -> }, onSelectCity = {}, onConfirmFromSmartConfirmation = { _, _, _ -> }, onProceedWithManualServices = { _, _ -> }, onPickPhoto = {}, onPublishProfile = { _, _, _, _ -> }, onSubmitClarifications = { _, _ -> }, onSkipClarification = {}, onBack = {}, onFinish = { _ -> }) }
}

@LightDarkScreenPreview
@Composable
private fun OnboardingLoadingPreview() {
    AppTheme { OnboardingScreens(uiState = OnboardingUiState.Loading, selectedCity = null, catalog = null, onSubmitDateOfBirth = {}, onCreateDraft = { _, _ -> }, onSelectCity = {}, onConfirmFromSmartConfirmation = { _, _, _ -> }, onProceedWithManualServices = { _, _ -> }, onPickPhoto = {}, onPublishProfile = { _, _, _, _ -> }, onSubmitClarifications = { _, _ -> }, onSkipClarification = {}, onBack = {}, onFinish = { _ -> }) }
}

@LightDarkScreenPreview
@Composable
private fun OnboardingSmartConfirmationPreview() {
    AppTheme {
        OnboardingScreens(
            uiState = OnboardingUiState.SmartConfirmation(
                PreviewSamples.sampleDraftResponse,
                listOf("paint-residential"),
                "Pintor residencial com 10 anos de experiência.",
            ),
            selectedCity = "Franca",
            catalog = null,
            onSubmitDateOfBirth = {}, onCreateDraft = { _, _ -> }, onSelectCity = {}, onConfirmFromSmartConfirmation = { _, _, _ -> }, onProceedWithManualServices = { _, _ -> }, onPickPhoto = {}, onPublishProfile = { _, _, _, _ -> }, onSubmitClarifications = { _, _ -> }, onSkipClarification = {}, onBack = {}, onFinish = { _ -> }
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
            onSubmitDateOfBirth = {}, onCreateDraft = { _, _ -> }, onSelectCity = {}, onConfirmFromSmartConfirmation = { _, _, _ -> }, onProceedWithManualServices = { _, _ -> }, onPickPhoto = {}, onPublishProfile = { _, _, _, _ -> }, onSubmitClarifications = { _, _ -> }, onSkipClarification = {}, onBack = {}, onFinish = { _ -> }
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
            onSubmitDateOfBirth = {}, onCreateDraft = { _, _ -> }, onSelectCity = {}, onConfirmFromSmartConfirmation = { _, _, _ -> }, onProceedWithManualServices = { _, _ -> }, onPickPhoto = {}, onPublishProfile = { _, _, _, _ -> }, onSubmitClarifications = { _, _ -> }, onSkipClarification = {}, onBack = {}, onFinish = { _ -> }
        )
    }
}
