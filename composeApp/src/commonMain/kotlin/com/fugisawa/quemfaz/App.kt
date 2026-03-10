package com.fugisawa.quemfaz

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fugisawa.quemfaz.di.appModule
import com.fugisawa.quemfaz.navigation.Screen
import com.fugisawa.quemfaz.network.ApiClient
import com.fugisawa.quemfaz.screens.*
import com.fugisawa.quemfaz.session.AuthState
import com.fugisawa.quemfaz.session.SessionManager
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

@Composable
fun App(baseUrl: String = BASE_URL_DEFAULT) {
    KoinApplication(application = {
        modules(appModule)
    }) {
        MaterialTheme {
            val sessionManager = koinInject<SessionManager>()
            val authState by sessionManager.authState.collectAsState()
            val currentCity by sessionManager.currentCity.collectAsState()
            
            // Inject ApiClient with baseUrl
            val koin = org.koin.compose.getKoin()
            remember { koin.get<ApiClient> { org.koin.core.parameter.parametersOf(baseUrl) } }

            var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
            var navigationStack by remember { mutableStateOf(listOf<Screen>(Screen.Home)) }

            val navigateTo: (Screen) -> Unit = { screen ->
                navigationStack = navigationStack + screen
                currentScreen = screen
            }

            val navigateBack: () -> Unit = {
                if (navigationStack.size > 1) {
                    navigationStack = navigationStack.dropLast(1)
                    currentScreen = navigationStack.last()
                }
            }

            Surface(modifier = Modifier.fillMaxSize()) {
                when (authState) {
                    is AuthState.Loading -> {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is AuthState.Blocked -> {
                        BlockedUserScreen(onContactSupport = { /* TODO */ })
                    }
                    is AuthState.Unauthenticated -> {
                        AuthFlow(navigateTo)
                    }
                    is AuthState.Authenticated -> {
                        if (currentCity == null && currentScreen != Screen.CitySelection) {
                            currentScreen = Screen.CitySelection
                        }
                        MainFlow(currentScreen, currentCity, navigateTo, navigateBack)
                    }
                }
            }
        }
    }
}

@Composable
fun AuthFlow(navigateTo: (Screen) -> Unit) {
    val viewModel: AuthViewModel = koinInject()
    val uiState by viewModel.uiState.collectAsState()

    var currentAuthStep by remember { mutableStateOf("phone") }
    var phoneForOtp by remember { mutableStateOf("") }

    when (currentAuthStep) {
        "phone" -> {
            PhoneLoginScreen(
                onSendOtp = {
                    phoneForOtp = it
                    viewModel.startOtp(it)
                },
                uiState = uiState
            )
            if (uiState is AuthUiState.OtpSent) {
                currentAuthStep = "otp"
            }
        }
        "otp" -> {
            OtpVerificationScreen(
                phone = phoneForOtp,
                onVerifyOtp = { viewModel.verifyOtp(phoneForOtp, it) },
                uiState = uiState
            )
            if (uiState is AuthUiState.ProfileCompletionRequired) {
                currentAuthStep = "profile"
            }
        }
        "profile" -> {
            CompleteUserProfileScreen(
                onComplete = { name, photo -> viewModel.completeProfile(name, photo) },
                uiState = uiState
            )
        }
    }
}

@Composable
fun MainFlow(
    currentScreen: Screen,
    currentCity: String?,
    navigateTo: (Screen) -> Unit,
    navigateBack: () -> Unit
) {
    when (currentScreen) {
        is Screen.Home -> {
            HomeScreen(
                currentCity = currentCity,
                onCityClick = { navigateTo(Screen.CitySelection) },
                onSearch = { navigateTo(Screen.SearchResults) /* For MVP we use a simple state */ },
                onOfferServices = { navigateTo(Screen.OnboardingStart) }
            )
        }
        is Screen.CitySelection -> {
            val viewModel: HomeViewModel = koinInject()
            CitySelectionScreen(
                cities = viewModel.supportedCities,
                onCitySelected = {
                    viewModel.selectCity(it)
                    navigateTo(Screen.Home)
                }
            )
        }
        is Screen.OnboardingStart -> {
            val viewModel: OnboardingViewModel = koinInject()
            val uiState by viewModel.uiState.collectAsState()
            OnboardingScreens(
                uiState = uiState,
                onCreateDraft = { viewModel.createDraft(it) },
                onConfirm = { desc, services, city, neighborhoods, phone ->
                    viewModel.confirmProfile(desc, services, city, neighborhoods, phone)
                },
                onFinish = { navigateTo(Screen.MyProfile) }
            )
        }
        else -> {
            Text("Screen $currentScreen not yet fully wired in MVP Shell")
        }
    }
}