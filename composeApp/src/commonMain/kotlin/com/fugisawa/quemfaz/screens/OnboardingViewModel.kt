package com.fugisawa.quemfaz.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fugisawa.quemfaz.contract.auth.SetProfilePhotoRequest
import com.fugisawa.quemfaz.contract.auth.UpdateDateOfBirthRequest
import com.fugisawa.quemfaz.contract.auth.CompleteUserProfileRequest
import com.fugisawa.quemfaz.contract.profile.ClarificationAnswer
import com.fugisawa.quemfaz.contract.profile.ClarifyDraftRequest
import com.fugisawa.quemfaz.contract.profile.ConfirmProfessionalProfileRequest
import com.fugisawa.quemfaz.contract.profile.CreateProfessionalProfileDraftRequest
import com.fugisawa.quemfaz.contract.profile.CreateProfessionalProfileDraftResponse
import com.fugisawa.quemfaz.contract.profile.InputMode
import com.fugisawa.quemfaz.contract.profile.InterpretedServiceDto
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import com.fugisawa.quemfaz.contract.profile.SetKnownNameRequest
import com.fugisawa.quemfaz.contract.catalog.CatalogResponse
import com.fugisawa.quemfaz.contract.city.CityResponse
import com.fugisawa.quemfaz.network.CatalogApiClient
import com.fugisawa.quemfaz.network.FeatureApiClients
import com.fugisawa.quemfaz.session.SessionManager
import com.fugisawa.quemfaz.ui.strings.Strings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class OnboardingErrorSource { BIRTH_DATE, DESCRIPTION, PHOTO, PUBLISH }

sealed class OnboardingUiState {
    object BirthDateRequired : OnboardingUiState()
    object NaturalPresentation : OnboardingUiState()
    object Loading : OnboardingUiState()
    data class NeedsClarification(
        val originalDescription: String,
        val draft: CreateProfessionalProfileDraftResponse,
    ) : OnboardingUiState()
    data class SmartConfirmation(
        val draft: CreateProfessionalProfileDraftResponse,
        val confirmedServiceIds: List<String>,
        val confirmedDescription: String,
    ) : OnboardingUiState()
    data class PhotoRequired(
        val draft: CreateProfessionalProfileDraftResponse,
        val confirmedServiceIds: List<String>,
        val confirmedDescription: String,
    ) : OnboardingUiState()
    data class ProfilePreview(
        val draft: CreateProfessionalProfileDraftResponse,
        val confirmedServiceIds: List<String>,
        val confirmedDescription: String,
    ) : OnboardingUiState()
    data class Published(val profile: ProfessionalProfileResponse) : OnboardingUiState()
    data class Error(
        val message: String,
        val source: OnboardingErrorSource = OnboardingErrorSource.DESCRIPTION,
    ) : OnboardingUiState()
}

class OnboardingViewModel(
    private val apiClients: FeatureApiClients,
    private val sessionManager: SessionManager,
    private val catalogApiClient: CatalogApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(
        if (sessionManager.currentUser.value?.dateOfBirth != null)
            OnboardingUiState.NaturalPresentation
        else
            OnboardingUiState.BirthDateRequired
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _selectedCityId = MutableStateFlow<String?>(null)
    val selectedCityId: StateFlow<String?> = _selectedCityId.asStateFlow()

    private val _cities = MutableStateFlow<List<CityResponse>>(emptyList())
    val cities: StateFlow<List<CityResponse>> = _cities.asStateFlow()

    private val _catalog = MutableStateFlow<CatalogResponse?>(null)
    val catalog: StateFlow<CatalogResponse?> = _catalog.asStateFlow()

    private var clarificationRound = 0

    init {
        viewModelScope.launch {
            try {
                _catalog.value = catalogApiClient.getCatalog()
            } catch (_: Exception) { }
        }
        viewModelScope.launch {
            try {
                _cities.value = apiClients.getCities().cities
            } catch (_: Exception) { }
        }
    }

    fun initializeCity(cityId: String?) {
        if (_selectedCityId.value == null) {
            _selectedCityId.value = cityId
        }
    }

    fun selectCity(cityId: String) {
        _selectedCityId.value = cityId
    }

    fun getCityDisplayName(cityId: String?): String? =
        _cities.value.find { it.id == cityId }?.name

    fun submitDateOfBirth(dateOfBirth: String) {
        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading
            try {
                apiClients.updateDateOfBirth(UpdateDateOfBirthRequest(dateOfBirth))
                val updatedUser = apiClients.getCurrentProfile()
                sessionManager.setCurrentUser(updatedUser)
                _uiState.value = OnboardingUiState.NaturalPresentation
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(
                    Strings.Errors.FAILED_SAVE_DATE_OF_BIRTH,
                    source = OnboardingErrorSource.BIRTH_DATE,
                )
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
                    val description = response.editedDescription.ifBlank { response.normalizedDescription }
                    val serviceIds = response.interpretedServices.map { it.serviceId }
                    _uiState.value = OnboardingUiState.SmartConfirmation(response, serviceIds, description)
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
                if (response.interpretedServices.isEmpty() && response.blockedDescriptions.isNotEmpty()) {
                    _uiState.value = OnboardingUiState.Error(Strings.Errors.SERVICE_NOT_ALLOWED)
                } else if (response.llmUnavailable || response.followUpQuestions.isEmpty() || clarificationRound >= 1) {
                    val description = response.editedDescription.ifBlank { response.normalizedDescription }
                    val serviceIds = response.interpretedServices.map { it.serviceId }
                    _uiState.value = OnboardingUiState.SmartConfirmation(response, serviceIds, description)
                } else {
                    _uiState.value = OnboardingUiState.NeedsClarification(originalDescription, response)
                }
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(e.message ?: Strings.Errors.FAILED_PROCESS_CLARIFICATIONS)
            }
        }
    }

    fun skipClarification(draft: CreateProfessionalProfileDraftResponse) {
        val description = draft.editedDescription.ifBlank { draft.normalizedDescription }
        val serviceIds = draft.interpretedServices.map { it.serviceId }
        _uiState.value = OnboardingUiState.SmartConfirmation(draft, serviceIds, description)
    }

    fun goBack() {
        _uiState.value = when (val current = _uiState.value) {
            is OnboardingUiState.BirthDateRequired -> current
            is OnboardingUiState.NaturalPresentation -> OnboardingUiState.BirthDateRequired
            is OnboardingUiState.NeedsClarification -> OnboardingUiState.NaturalPresentation
            is OnboardingUiState.SmartConfirmation -> OnboardingUiState.NaturalPresentation
            is OnboardingUiState.PhotoRequired -> OnboardingUiState.SmartConfirmation(
                current.draft, current.confirmedServiceIds, current.confirmedDescription
            )
            is OnboardingUiState.ProfilePreview -> {
                val hasPhoto = sessionManager.currentUser.value?.photoUrl != null
                if (hasPhoto) OnboardingUiState.SmartConfirmation(current.draft, current.confirmedServiceIds, current.confirmedDescription)
                else OnboardingUiState.PhotoRequired(current.draft, current.confirmedServiceIds, current.confirmedDescription)
            }
            is OnboardingUiState.Error -> when (current.source) {
                OnboardingErrorSource.BIRTH_DATE -> OnboardingUiState.BirthDateRequired
                OnboardingErrorSource.DESCRIPTION -> OnboardingUiState.NaturalPresentation
                OnboardingErrorSource.PHOTO -> OnboardingUiState.NaturalPresentation
                OnboardingErrorSource.PUBLISH -> OnboardingUiState.NaturalPresentation
            }
            else -> current
        }
    }

    fun confirmFromSmartConfirmation(
        draft: CreateProfessionalProfileDraftResponse,
        confirmedServiceIds: List<String>,
        confirmedDescription: String,
    ) {
        val hasPhoto = sessionManager.currentUser.value?.photoUrl != null
        _uiState.value = if (hasPhoto) {
            OnboardingUiState.ProfilePreview(draft, confirmedServiceIds, confirmedDescription)
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
                _uiState.value = OnboardingUiState.ProfilePreview(draft, confirmedServiceIds, confirmedDescription)
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(
                    e.message ?: Strings.Errors.FAILED_UPLOAD_PHOTO,
                    source = OnboardingErrorSource.PHOTO,
                )
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
        val description = updatedDraft.editedDescription.ifBlank { updatedDraft.normalizedDescription }
        _uiState.value = OnboardingUiState.SmartConfirmation(
            updatedDraft,
            selectedServiceIds.toList(),
            description,
        )
    }

    fun publishProfile(
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
                        cityId = _selectedCityId.value,
                        portfolioPhotoUrls = emptyList(),
                    )
                )
                if (!knownName.isNullOrBlank()) {
                    apiClients.setKnownName(SetKnownNameRequest(knownName = knownName.trim()))
                }
                _uiState.value = OnboardingUiState.Published(response)
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(
                    e.message ?: Strings.Errors.FAILED_PUBLISH_PROFILE,
                    source = OnboardingErrorSource.PUBLISH,
                )
            }
        }
    }
}
