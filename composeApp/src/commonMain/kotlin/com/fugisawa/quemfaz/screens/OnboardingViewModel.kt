package com.fugisawa.quemfaz.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fugisawa.quemfaz.contract.auth.SetProfilePhotoRequest
import com.fugisawa.quemfaz.contract.auth.UpdateDateOfBirthRequest
import com.fugisawa.quemfaz.contract.auth.CompleteUserProfileRequest
import com.fugisawa.quemfaz.contract.profile.*
import com.fugisawa.quemfaz.contract.catalog.CatalogResponse
import com.fugisawa.quemfaz.network.CatalogApiClient
import com.fugisawa.quemfaz.network.FeatureApiClients
import com.fugisawa.quemfaz.session.SessionManager
import com.fugisawa.quemfaz.ui.strings.Strings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class OnboardingUiState {
    object BirthDateRequired : OnboardingUiState()
    object Idle : OnboardingUiState()
    object Loading : OnboardingUiState()
    data class NeedsClarification(
        val originalDescription: String,
        val draft: CreateProfessionalProfileDraftResponse,
    ) : OnboardingUiState()
    data class ReviewServices(val draft: CreateProfessionalProfileDraftResponse) : OnboardingUiState()
    data class ReviewDescription(
        val draft: CreateProfessionalProfileDraftResponse,
        val confirmedServiceIds: List<String>,
    ) : OnboardingUiState()
    data class PhotoRequired(
        val draft: CreateProfessionalProfileDraftResponse,
        val confirmedServiceIds: List<String>,
        val confirmedDescription: String,
    ) : OnboardingUiState()
    data class KnownName(
        val draft: CreateProfessionalProfileDraftResponse,
        val confirmedServiceIds: List<String>,
        val confirmedDescription: String,
    ) : OnboardingUiState()
    data class Published(val profile: ProfessionalProfileResponse) : OnboardingUiState()
    data class Error(val message: String) : OnboardingUiState()
}

class OnboardingViewModel(
    private val apiClients: FeatureApiClients,
    private val sessionManager: SessionManager,
    private val catalogApiClient: CatalogApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(
        if (sessionManager.currentUser.value?.dateOfBirth != null)
            OnboardingUiState.Idle
        else
            OnboardingUiState.BirthDateRequired
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _selectedCity = MutableStateFlow<String?>(null)
    val selectedCity: StateFlow<String?> = _selectedCity.asStateFlow()

    private val _catalog = MutableStateFlow<CatalogResponse?>(null)
    val catalog: StateFlow<CatalogResponse?> = _catalog.asStateFlow()

    private var clarificationRound = 0

    init {
        viewModelScope.launch {
            try {
                _catalog.value = catalogApiClient.getCatalog()
            } catch (_: Exception) { }
        }
    }

    fun initializeCity(mainScreenCity: String?) {
        if (_selectedCity.value == null) {
            _selectedCity.value = mainScreenCity
        }
    }

    fun selectCity(city: String) {
        _selectedCity.value = city
    }

    fun submitDateOfBirth(dateOfBirth: String) {
        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading
            try {
                apiClients.updateDateOfBirth(UpdateDateOfBirthRequest(dateOfBirth))
                val updatedUser = apiClients.getCurrentProfile()
                sessionManager.setCurrentUser(updatedUser)
                _uiState.value = OnboardingUiState.Idle
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(Strings.Errors.FAILED_SAVE_DATE_OF_BIRTH)
            }
        }
    }

    fun createDraft(inputText: String, inputMode: InputMode = InputMode.TEXT) {
        clarificationRound = 0
        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading
            try {
                val response = apiClients.createDraft(
                    CreateProfessionalProfileDraftRequest(inputText, inputMode)
                )
                if (response.interpretedServices.isEmpty() && response.blockedDescriptions.isNotEmpty()) {
                    _uiState.value = OnboardingUiState.Error(Strings.Errors.SERVICE_NOT_ALLOWED)
                } else if (response.llmUnavailable || response.followUpQuestions.isEmpty()) {
                    _uiState.value = OnboardingUiState.ReviewServices(response)
                } else {
                    _uiState.value = OnboardingUiState.NeedsClarification(inputText, response)
                }
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(e.message ?: Strings.Errors.FAILED_CREATE_DRAFT)
            }
        }
    }

    fun submitClarifications(originalDescription: String, answers: List<ClarificationAnswer>) {
        clarificationRound++
        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading
            try {
                val response = apiClients.clarifyDraft(
                    ClarifyDraftRequest(
                        originalDescription = originalDescription,
                        clarificationAnswers = answers,
                        clarificationRound = clarificationRound,
                    )
                )
                // Client-side cap: after 1 round, always proceed to ReviewServices
                if (response.interpretedServices.isEmpty() && response.blockedDescriptions.isNotEmpty()) {
                    _uiState.value = OnboardingUiState.Error(Strings.Errors.SERVICE_NOT_ALLOWED)
                } else if (response.llmUnavailable || response.followUpQuestions.isEmpty() || clarificationRound >= 1) {
                    _uiState.value = OnboardingUiState.ReviewServices(response)
                } else {
                    _uiState.value = OnboardingUiState.NeedsClarification(originalDescription, response)
                }
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(e.message ?: Strings.Errors.FAILED_PROCESS_CLARIFICATIONS)
            }
        }
    }

    fun skipClarification(draft: CreateProfessionalProfileDraftResponse) {
        _uiState.value = OnboardingUiState.ReviewServices(draft)
    }

    fun goBack() {
        _uiState.value = when (val current = _uiState.value) {
            is OnboardingUiState.BirthDateRequired -> current
            is OnboardingUiState.Idle -> OnboardingUiState.BirthDateRequired
            is OnboardingUiState.NeedsClarification -> OnboardingUiState.Idle
            is OnboardingUiState.ReviewServices -> OnboardingUiState.Idle
            is OnboardingUiState.ReviewDescription -> OnboardingUiState.ReviewServices(current.draft)
            is OnboardingUiState.PhotoRequired -> OnboardingUiState.ReviewDescription(
                current.draft, current.confirmedServiceIds
            )
            is OnboardingUiState.KnownName -> {
                val hasPhoto = sessionManager.currentUser.value?.photoUrl != null
                if (hasPhoto) OnboardingUiState.ReviewDescription(current.draft, current.confirmedServiceIds)
                else OnboardingUiState.PhotoRequired(current.draft, current.confirmedServiceIds, current.confirmedDescription)
            }
            is OnboardingUiState.Error -> OnboardingUiState.Idle
            else -> current
        }
    }

    fun proceedFromServices(draft: CreateProfessionalProfileDraftResponse, confirmedServiceIds: List<String>) {
        _uiState.value = OnboardingUiState.ReviewDescription(draft, confirmedServiceIds)
    }

    fun proceedFromDescription(
        draft: CreateProfessionalProfileDraftResponse,
        confirmedServiceIds: List<String>,
        confirmedDescription: String,
    ) {
        val hasPhoto = sessionManager.currentUser.value?.photoUrl != null
        _uiState.value = if (hasPhoto) {
            OnboardingUiState.KnownName(draft, confirmedServiceIds, confirmedDescription)
        } else {
            OnboardingUiState.PhotoRequired(draft, confirmedServiceIds, confirmedDescription)
        }
    }

    fun submitPhoto(
        data: ByteArray,
        mimeType: String,
        draft: CreateProfessionalProfileDraftResponse,
        confirmedServiceIds: List<String>,
        confirmedDescription: String,
    ) {
        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading
            try {
                val uploadResponse = apiClients.uploadImage(data, mimeType)
                val userResponse = apiClients.setProfilePhoto(
                    SetProfilePhotoRequest(photoUrl = uploadResponse.url)
                )
                sessionManager.setCurrentUser(userResponse)
                _uiState.value = OnboardingUiState.KnownName(draft, confirmedServiceIds, confirmedDescription)
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(e.message ?: Strings.Errors.FAILED_UPLOAD_PHOTO)
            }
        }
    }

    fun proceedWithManualServices(
        draft: CreateProfessionalProfileDraftResponse,
        selectedServiceIds: Set<String>,
    ) {
        val manualServices = selectedServiceIds.map { serviceId ->
            val catalogEntry = _catalog.value?.services?.find { it.id == serviceId }
            InterpretedServiceDto(
                serviceId = serviceId,
                displayName = catalogEntry?.displayName ?: serviceId,
                matchLevel = "PRIMARY",
            )
        }
        val updatedDraft = draft.copy(interpretedServices = manualServices)
        _uiState.value = OnboardingUiState.ReviewDescription(
            updatedDraft,
            selectedServiceIds.toList(),
        )
    }

    fun submitKnownName(
        fullName: String?,
        knownName: String?,
        confirmedServiceIds: List<String>,
        confirmedDescription: String,
    ) {
        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading
            try {
                if (!fullName.isNullOrBlank()) {
                    val updatedUser = apiClients.submitName(CompleteUserProfileRequest(fullName.trim()))
                    sessionManager.setCurrentUser(updatedUser)
                }
                val response = apiClients.confirmProfile(
                    ConfirmProfessionalProfileRequest(
                        description = confirmedDescription,
                        selectedServiceIds = confirmedServiceIds,
                        cityName = _selectedCity.value,
                        portfolioPhotoUrls = emptyList(),
                    )
                )
                if (!knownName.isNullOrBlank()) {
                    apiClients.setKnownName(SetKnownNameRequest(knownName = knownName.trim()))
                }
                _uiState.value = OnboardingUiState.Published(response)
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(e.message ?: Strings.Errors.FAILED_PUBLISH_PROFILE)
            }
        }
    }
}
