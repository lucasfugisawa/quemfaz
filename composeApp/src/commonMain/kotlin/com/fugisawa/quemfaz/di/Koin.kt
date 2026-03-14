package com.fugisawa.quemfaz.di

import com.fugisawa.quemfaz.network.ApiClient
import com.fugisawa.quemfaz.network.FeatureApiClients
import com.fugisawa.quemfaz.screens.AuthViewModel
import com.fugisawa.quemfaz.screens.EditProfessionalProfileViewModel
import com.fugisawa.quemfaz.screens.HomeViewModel
import com.fugisawa.quemfaz.screens.OnboardingViewModel
import com.fugisawa.quemfaz.screens.FavoritesViewModel
import com.fugisawa.quemfaz.screens.ProfileViewModel
import com.fugisawa.quemfaz.session.SessionManager
import com.fugisawa.quemfaz.session.SessionStorage
import com.fugisawa.quemfaz.session.SettingsSessionStorage
import com.russhwolf.settings.Settings
import org.koin.dsl.module

val appModule = module {
    single<Settings> { Settings() }
    single<SessionStorage> { SettingsSessionStorage(get()) }
    single { SessionManager(get(), get(), getApiClient = { get<ApiClient>() }) }
    single { (baseUrl: String) ->
        ApiClient(
            sessionStorage = get(),
            baseUrl = baseUrl,
            onUnauthorized = { get<SessionManager>().logout() },
            onTokenRefreshed = { token, refreshToken -> get<SessionManager>().updateTokens(token, refreshToken) }
        )
    }
    single { FeatureApiClients(get()) }

    factory { AuthViewModel(get(), get()) }
    factory { HomeViewModel(get(), get()) }
    factory { ProfileViewModel(get()) }
    factory { OnboardingViewModel(get()) }
    factory { FavoritesViewModel(get()) }
    factory { EditProfessionalProfileViewModel(get()) }
}
