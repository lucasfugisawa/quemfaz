package com.fugisawa.quemfaz

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fugisawa.quemfaz.di.appModule
import com.fugisawa.quemfaz.navigation.Screen
import com.fugisawa.quemfaz.network.ApiClient
import com.fugisawa.quemfaz.platform.rememberImagePickerLauncher
import com.fugisawa.quemfaz.screens.*
import com.fugisawa.quemfaz.session.AuthState
import com.fugisawa.quemfaz.session.SessionManager
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import com.fugisawa.quemfaz.contract.engagement.ContactChannelDto
import com.fugisawa.quemfaz.platform.openUrl
import com.fugisawa.quemfaz.domain.moderation.ReportReason
import com.fugisawa.quemfaz.ui.theme.AppTheme

@Composable
fun App(baseUrl: String = BASE_URL_DEFAULT) {
    KoinApplication(application = {
        modules(appModule)
    }) {
        AppTheme {
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

            // Tab navigation: resets the stack to just the target screen.
            val navigateToTab: (Screen) -> Unit = { screen ->
                navigationStack = listOf(screen)
                currentScreen = screen
            }

            // Reset nav state to Home whenever the user leaves the authenticated shell
            // (logout, block). This prevents stale nested screens on re-login.
            LaunchedEffect(authState) {
                if (authState == AuthState.Unauthenticated || authState == AuthState.Blocked) {
                    currentScreen = Screen.Home
                    navigationStack = listOf(Screen.Home)
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
                        BlockedUserScreen(onContactSupport = { openUrl("mailto:suporte@quemfaz.com") })
                    }
                    is AuthState.Unauthenticated -> {
                        AuthFlow(navigateTo)
                    }
                    is AuthState.Authenticated -> {
                        // Single AuthViewModel instance for this auth session.
                        // Passed into MainFlow so all screens share the same hydration state.
                        val authViewModel: AuthViewModel = koinInject()

                        LaunchedEffect(Unit) {
                            authViewModel.fetchCurrentUser()
                        }

                        // City gate: if city is not set, force CitySelection and prevent bypass.
                        // Keyed on both currentCity AND currentScreen so re-checking on every
                        // screen change catches back-navigation attempts to bypass the gate.
                        LaunchedEffect(currentCity, currentScreen) {
                            if (currentCity == null && currentScreen != Screen.CitySelection) {
                                // Replace the entire stack so there is nothing to go back to.
                                navigationStack = listOf(Screen.CitySelection)
                                currentScreen = Screen.CitySelection
                            }
                        }

                        MainFlow(
                            currentScreen = currentScreen,
                            currentCity = currentCity,
                            navigateTo = navigateTo,
                            navigateBack = navigateBack,
                            navigateToTab = navigateToTab,
                            authViewModel = authViewModel,
                            sessionManager = sessionManager
                        )
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
    val sessionManager: SessionManager = koinInject()
    val currentUser by sessionManager.currentUser.collectAsState()

    var currentAuthStep by remember { mutableStateOf("phone") }
    var phoneForOtp by remember { mutableStateOf("") }

    val imagePicker = rememberImagePickerLauncher { data, mimeType ->
        viewModel.submitPhoto(data, mimeType)
    }

    when (currentAuthStep) {
        "phone" -> {
            PhoneLoginScreen(
                onSendOtp = { phoneForOtp = it; viewModel.startOtp(it) },
                uiState = uiState,
            )
            if (uiState is AuthUiState.OtpSent) currentAuthStep = "otp"
        }
        "otp" -> {
            OtpVerificationScreen(
                phone = phoneForOtp,
                onVerifyOtp = { viewModel.verifyOtp(phoneForOtp, it) },
                uiState = uiState,
            )
            if (uiState is AuthUiState.ProfileCompletionRequired) currentAuthStep = "name"
        }
        "name" -> {
            NameInputScreen(
                onSubmitName = { firstName, lastName -> viewModel.submitName(firstName, lastName) },
                uiState = uiState,
            )
            if (uiState is AuthUiState.PhotoUploadRequired) currentAuthStep = "photo"
        }
        "photo" -> {
            val displayName = currentUser?.let { "${it.firstName} ${it.lastName}" } ?: ""
            ProfilePhotoScreen(
                currentPhotoUrl = currentUser?.photoUrl,
                displayName = displayName,
                headline = "Add a profile photo",
                showSkip = true,
                isLoading = uiState is AuthUiState.Loading,
                error = (uiState as? AuthUiState.Error)?.message,
                onPickImage = { imagePicker.launch() },
                onSkip = { viewModel.skipPhoto() },
            )
        }
    }

    if (uiState is AuthUiState.Success) {
        LaunchedEffect(Unit) {
            navigateTo(Screen.Home)
        }
    }
}

@Composable
fun MainFlow(
    currentScreen: Screen,
    currentCity: String?,
    navigateTo: (Screen) -> Unit,
    navigateBack: () -> Unit,
    navigateToTab: (Screen) -> Unit,
    authViewModel: AuthViewModel,
    sessionManager: SessionManager
) {
    val homeViewModel: HomeViewModel = koinInject()
    val searchUiState by homeViewModel.searchUiState.collectAsState()

    // Auth-related state collected once here — all screens in this shell share the same owner.
    val currentUser by sessionManager.currentUser.collectAsState()
    val authUiState by authViewModel.uiState.collectAsState()
    val hydrationFailed by authViewModel.hydrationFailed.collectAsState()

    val showEarnMoneyCard by homeViewModel.showEarnMoneyCard.collectAsState()

    var currentQuery by remember { mutableStateOf("") }
    var currentProfileId by remember { mutableStateOf("") }

    val isTopLevelScreen = currentScreen == Screen.Home ||
            currentScreen == Screen.Favorites ||
            currentScreen == Screen.MyProfile

    Scaffold(
        bottomBar = {
            if (isTopLevelScreen) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentScreen == Screen.Home,
                        onClick = { navigateToTab(Screen.Home) },
                        icon = { Text("🏠") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.Favorites,
                        onClick = { navigateToTab(Screen.Favorites) },
                        icon = { Text("⭐") },
                        label = { Text("Favorites") }
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.MyProfile,
                        onClick = { navigateToTab(Screen.MyProfile) },
                        icon = { Text("👤") },
                        label = { Text("Profile") }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (currentScreen) {
                is Screen.Home -> {
                    HomeScreen(
                        currentUser = currentUser,
                        currentCity = currentCity,
                        showEarnMoneyCard = showEarnMoneyCard,
                        onCityClick = { navigateTo(Screen.CitySelection) },
                        onProfileClick = { navigateToTab(Screen.MyProfile) },
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
                            // Always reset to Home after city selection — clears gate stack.
                            navigateToTab(Screen.Home)
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
                        },
                        onNavigateBack = navigateBack
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
                        },
                        onNavigateBack = navigateBack
                    )
                }
                is Screen.Favorites -> {
                    val favoritesViewModel: FavoritesViewModel = koinInject()
                    val favoritesUiState by favoritesViewModel.uiState.collectAsState()
                    LaunchedEffect(Unit) {
                        favoritesViewModel.loadFavorites()
                    }
                    FavoritesScreen(
                        uiState = favoritesUiState,
                        onProfileClick = { id ->
                            currentProfileId = id
                            navigateTo(Screen.ProfessionalProfile)
                        },
                        onRetry = { favoritesViewModel.loadFavorites() }
                    )
                }
                is Screen.MyProfile -> {
                    MyProfileScreen(
                        currentUser = currentUser,
                        uiState = authUiState,
                        hydrationFailed = hydrationFailed,
                        onSaveName = { firstName, lastName -> authViewModel.submitName(firstName, lastName) },
                        onSavePhoto = { data, mimeType -> authViewModel.submitPhoto(data, mimeType) },
                        onNavigateToFavorites = { navigateToTab(Screen.Favorites) },
                        onChangeCity = { navigateTo(Screen.CitySelection) },
                        onManageProfessionalProfile = { navigateTo(Screen.EditProfessionalProfile) },
                        onRetry = { authViewModel.fetchCurrentUser() },
                        onLogout = { authViewModel.logout() }
                    )
                }
                is Screen.OnboardingStart -> {
                    val viewModel: OnboardingViewModel = koinInject()
                    val uiState by viewModel.uiState.collectAsState()

                    val imagePicker = rememberImagePickerLauncher { data, mimeType ->
                        val draft = (uiState as? OnboardingUiState.PhotoRequired)?.draft ?: return@rememberImagePickerLauncher
                        viewModel.submitPhoto(data, mimeType, draft)
                    }

                    OnboardingScreens(
                        uiState = uiState,
                        onCreateDraft = { viewModel.createDraft(it) },
                        onProceedFromDraft = { draft -> viewModel.proceedFromDraft(draft) },
                        onPickPhoto = { _ -> imagePicker.launch() },
                        onSubmitKnownName = { knownName, draft -> viewModel.submitKnownName(knownName, draft) },
                        onSubmitClarifications = { desc, answers ->
                            viewModel.submitClarifications(desc, answers)
                        },
                        onSkipClarification = { draft -> viewModel.skipClarification(draft) },
                        onFinish = { navigateToTab(Screen.MyProfile) }
                    )
                }
                is Screen.EditProfessionalProfile -> {
                    val viewModel: EditProfessionalProfileViewModel = koinInject()
                    val uiState by viewModel.uiState.collectAsState()
                    LaunchedEffect(Unit) {
                        viewModel.loadProfile()
                    }
                    EditProfessionalProfileScreen(
                        uiState = uiState,
                        onSave = { desc, city, neighborhoods, contact, whatsapp, photo ->
                            viewModel.saveProfile(desc, city, neighborhoods, contact, whatsapp, photo)
                        },
                        onNavigateBack = navigateBack,
                        onGoToOnboarding = { navigateTo(Screen.OnboardingStart) }
                    )
                }
                else -> {
                    Text("Screen $currentScreen not yet fully wired in MVP Shell")
                }
            }
        }
    }
}
