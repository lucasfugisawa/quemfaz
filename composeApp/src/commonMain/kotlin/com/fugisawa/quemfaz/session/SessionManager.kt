package com.fugisawa.quemfaz.session

import com.fugisawa.quemfaz.contract.auth.UserProfileResponse
import com.fugisawa.quemfaz.network.ApiClient
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Authenticated : AuthState()
    object Blocked : AuthState()
    object Loading : AuthState()
}

class SessionManager(
    private val sessionStorage: SessionStorage,
    private val settings: Settings,
    private val getApiClient: () -> ApiClient
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<UserProfileResponse?>(null)
    val currentUser: StateFlow<UserProfileResponse?> = _currentUser.asStateFlow()

    private val _currentCity = MutableStateFlow<String?>(settings.getStringOrNull("current_city"))
    val currentCity: StateFlow<String?> = _currentCity.asStateFlow()

    init {
        val token = sessionStorage.getToken()
        if (token != null) {
            _authState.value = AuthState.Authenticated
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun setAuthenticated(token: String, refreshToken: String, user: UserProfileResponse? = null) {
        sessionStorage.saveToken(token)
        sessionStorage.saveRefreshToken(refreshToken)
        _currentUser.value = user
        _authState.value = AuthState.Authenticated
    }

    fun updateTokens(token: String, refreshToken: String) {
        sessionStorage.saveToken(token)
        sessionStorage.saveRefreshToken(refreshToken)
    }

    fun setCurrentUser(user: UserProfileResponse) {
        _currentUser.value = user
    }

    fun setBlocked() {
        // Clear the token and user so the blocked state is clean.
        // On next cold start the user goes through OTP again and will be blocked again.
        sessionStorage.clear()
        _currentUser.value = null
        _authState.value = AuthState.Blocked
    }

    fun logout() {
        val refreshToken = sessionStorage.getRefreshToken()
        if (refreshToken != null) {
            scope.launch {
                getApiClient().logout(refreshToken)
            }
        }
        sessionStorage.clear()
        _currentUser.value = null
        // City is intentionally preserved — it is a device-level preference, not user-specific.
        _authState.value = AuthState.Unauthenticated
    }

    fun setCity(cityName: String) {
        settings["current_city"] = cityName
        _currentCity.value = cityName
    }
}
