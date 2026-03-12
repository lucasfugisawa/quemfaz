package com.fugisawa.quemfaz.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fugisawa.quemfaz.contract.auth.CompleteUserProfileRequest
import com.fugisawa.quemfaz.contract.auth.StartOtpRequest
import com.fugisawa.quemfaz.contract.auth.VerifyOtpRequest
import com.fugisawa.quemfaz.network.FeatureApiClients
import com.fugisawa.quemfaz.session.SessionManager
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class OtpSent(val phone: String) : AuthUiState()
    object ProfileCompletionRequired : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(
    private val apiClients: FeatureApiClients,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _hydrationFailed = MutableStateFlow(false)
    val hydrationFailed: StateFlow<Boolean> = _hydrationFailed.asStateFlow()

    fun startOtp(phoneNumber: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = apiClients.startOtp(StartOtpRequest(phoneNumber))
                if (response.success) {
                    _uiState.value = AuthUiState.OtpSent(phoneNumber)
                } else {
                    _uiState.value = AuthUiState.Error(response.message)
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun verifyOtp(phoneNumber: String, otpCode: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = apiClients.verifyOtp(VerifyOtpRequest(phoneNumber, otpCode))
                if (response.success) {
                    val token = response.token
                    sessionManager.setAuthenticated(token)

                    if (response.requiresProfileCompletion) {
                        _uiState.value = AuthUiState.ProfileCompletionRequired
                    } else {
                        fetchCurrentUser()
                        _uiState.value = AuthUiState.Success
                    }
                } else {
                    _uiState.value = AuthUiState.Error("Invalid OTP")
                }
            } catch (e: ResponseException) {
                if (e.response.status == HttpStatusCode.Forbidden) {
                    // Server returned 403: user is blocked.
                    sessionManager.setBlocked()
                } else {
                    _uiState.value = AuthUiState.Error("Invalid OTP code")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun completeProfile(name: String, photoUrl: String?) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = apiClients.completeProfile(CompleteUserProfileRequest(name, photoUrl))
                sessionManager.setCurrentUser(response)
                _uiState.value = AuthUiState.Success
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun logout() {
        sessionManager.logout()
    }

    fun fetchCurrentUser() {
        viewModelScope.launch {
            try {
                val profile = apiClients.getCurrentProfile()
                if (profile.status == "BLOCKED") {
                    // User was blocked server-side since their token was issued.
                    sessionManager.setBlocked()
                } else {
                    sessionManager.setCurrentUser(profile)
                    _hydrationFailed.value = false
                }
            } catch (e: Exception) {
                _hydrationFailed.value = true
            }
        }
    }
}
