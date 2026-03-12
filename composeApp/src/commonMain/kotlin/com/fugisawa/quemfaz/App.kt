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
import com.fugisawa.quemfaz.contract.engagement.ContactChannelDto
import com.fugisawa.quemfaz.platform.openUrl
import com.fugisawa.quemfaz.domain.moderation.ReportReason

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
                        val authViewModel: AuthViewModel = koinInject()
                        val currentUser by sessionManager.currentUser.collectAsState()
                        LaunchedEffect(Unit) {
                            if (currentUser == null) {
                                authViewModel.fetchCurrentUser()
                            }
                        }
                        LaunchedEffect(currentCity) {
                            if (currentCity == null && currentScreen != Screen.CitySelection) {
                                currentScreen = Screen.CitySelection
                            }
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
    val homeViewModel: HomeViewModel = koinInject()
    val searchUiState by homeViewModel.searchUiState.collectAsState()

    var currentQuery by remember { mutableStateOf("") }
    var currentProfileId by remember { mutableStateOf("") }

    when (currentScreen) {
        is Screen.Home -> {
            HomeScreen(
                currentCity = currentCity,
                onCityClick = { navigateTo(Screen.CitySelection) },
                onSearch = { query ->
                    currentQuery = query
                    homeViewModel.search(query)
                    navigateTo(Screen.SearchResults)
                },
                onOfferServices = { navigateTo(Screen.OnboardingStart) }
            )
        }
        is Screen.CitySelection -> {
            CitySelectionScreen(
                cities = homeViewModel.supportedCities,
                onCitySelected = {
                    homeViewModel.selectCity(it)
                    navigateTo(Screen.Home)
                }
            )
        }
        is Screen.SearchResults -> {
            SearchResultsScreen(
                query = currentQuery,
                uiState = searchUiState,
                onProfileClick = { id ->
                    currentProfileId = id
                    navigateTo(Screen.ProfessionalProfile)
                }
            )
        }
        is Screen.ProfessionalProfile -> {
            val profileViewModel: ProfileViewModel = koinInject()
            val profileUiState by profileViewModel.uiState.collectAsState()
            LaunchedEffect(currentProfileId) {
                profileViewModel.loadProfile(currentProfileId)
                profileViewModel.trackProfileView(currentProfileId)
            }
            ProfessionalProfileScreen(
                id = currentProfileId,
                uiState = profileUiState,
                onContactClick = { channel ->
                    profileViewModel.trackContactClick(currentProfileId, channel)
                    val profile = (profileUiState as? ProfileUiState.Content)?.profile
                    when (channel) {
                        ContactChannelDto.WHATSAPP -> {
                            val digits = profile?.whatsAppPhone?.filter { it.isDigit() }
                            if (!digits.isNullOrBlank()) openUrl("https://wa.me/$digits")
                        }
                        ContactChannelDto.PHONE_CALL -> {
                            val phone = profile?.contactPhone?.ifBlank { null }
                            if (phone != null) openUrl("tel:$phone")
                        }
                    }
                },
                onFavoriteToggle = { profileViewModel.toggleFavorite(currentProfileId) },
                onReportSubmit = { reason ->
                    profileViewModel.reportProfile(currentProfileId, reason, null)
                }
            )
        }
        is Screen.Favorites -> {
            FavoritesScreen(onNavigateBack = navigateBack)
        }
        is Screen.MyProfile -> {
            val authViewModel: AuthViewModel = koinInject()
            val myProfileSessionManager: com.fugisawa.quemfaz.session.SessionManager = koinInject()
            val currentUser by myProfileSessionManager.currentUser.collectAsState()
            val profileUiState by authViewModel.uiState.collectAsState()
            val hydrationFailed by authViewModel.hydrationFailed.collectAsState()
            MyProfileScreen(
                currentUser = currentUser,
                uiState = profileUiState,
                hydrationFailed = hydrationFailed,
                onSaveProfile = { name, photo -> authViewModel.completeProfile(name, photo) },
                onNavigateToFavorites = { navigateTo(Screen.Favorites) },
                onChangeCity = { navigateTo(Screen.CitySelection) },
                onManageProfessionalProfile = { navigateTo(Screen.EditProfessionalProfile) },
                onRetry = { authViewModel.fetchCurrentUser() },
                onLogout = { myProfileSessionManager.logout() }
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
        is Screen.EditProfessionalProfile -> {
            val viewModel: com.fugisawa.quemfaz.screens.EditProfessionalProfileViewModel = koinInject()
            val uiState by viewModel.uiState.collectAsState()
            LaunchedEffect(Unit) {
                viewModel.loadProfile()
            }
            com.fugisawa.quemfaz.screens.EditProfessionalProfileScreen(
                uiState = uiState,
                onSave = { desc, city, neighborhoods, contactPhone, whatsAppPhone ->
                    viewModel.saveProfile(desc, city, neighborhoods, contactPhone, whatsAppPhone)
                },
                onNavigateBack = navigateBack
            )
        }
        else -> {
            Text("Screen $currentScreen not yet fully wired in MVP Shell")
        }
    }
}