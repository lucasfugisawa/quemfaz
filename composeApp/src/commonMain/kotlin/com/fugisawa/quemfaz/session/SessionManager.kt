package com.fugisawa.quemfaz.session

import com.fugisawa.quemfaz.contract.auth.UserProfileResponse
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Authenticated : AuthState()
    object Blocked : AuthState()
    object Loading : AuthState()
}

class SessionManager(
    private val sessionStorage: SessionStorage,
    private val settings: Settings
) {
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

    fun setAuthenticated(token: String, user: UserProfileResponse? = null) {
        sessionStorage.saveToken(token)
        _currentUser.value = user
        _authState.value = AuthState.Authenticated
    }

    fun setCurrentUser(user: UserProfileResponse) {
        _currentUser.value = user
    }

    fun setBlocked() {
        _authState.value = AuthState.Blocked
    }

    fun logout() {
        sessionStorage.clear()
        _currentUser.value = null
        _authState.value = AuthState.Unauthenticated
    }

    fun setCity(cityName: String) {
        settings["current_city"] = cityName
        _currentCity.value = cityName
    }
}
