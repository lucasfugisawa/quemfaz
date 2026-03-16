package com.fugisawa.quemfaz

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fugisawa.quemfaz.contract.engagement.ContactChannelDto
import com.fugisawa.quemfaz.di.appModule
import com.fugisawa.quemfaz.navigation.Screen
import com.fugisawa.quemfaz.network.ApiClient
import com.fugisawa.quemfaz.platform.PlatformBackHandler
import com.fugisawa.quemfaz.platform.launch
import com.fugisawa.quemfaz.platform.openUrl
import com.fugisawa.quemfaz.platform.rememberImagePickerLauncher
import com.fugisawa.quemfaz.screens.*
import com.fugisawa.quemfaz.session.AuthState
import com.fugisawa.quemfaz.session.SessionManager
import com.fugisawa.quemfaz.ui.strings.Strings
import com.fugisawa.quemfaz.ui.theme.AppTheme
import com.fugisawa.quemfaz.ui.theme.Spacing
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

val LocalBaseUrl = staticCompositionLocalOf { BASE_URL_DEFAULT }

enum class NavigationDirection { TAB, PUSH, POP }

@Composable
fun App(baseUrl: String = BASE_URL_DEFAULT) {
    KoinApplication(application = {
        modules(appModule)
    }) {
        CompositionLocalProvider(LocalBaseUrl provides baseUrl) {
            AppTheme {
            val sessionManager = koinInject<SessionManager>()
            val authState by sessionManager.authState.collectAsState()
            val currentCity by sessionManager.currentCity.collectAsState()

            // Inject ApiClient with baseUrl
            val koin = org.koin.compose.getKoin()
            remember { koin.get<ApiClient> { org.koin.core.parameter.parametersOf(baseUrl) } }

            var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
            var navigationStack by remember { mutableStateOf(listOf<Screen>(Screen.Home)) }
            var navigationDirection by remember { mutableStateOf(NavigationDirection.TAB) }

            val navigateTo: (Screen) -> Unit = { screen ->
                navigationDirection = NavigationDirection.PUSH
                navigationStack = navigationStack + screen
                currentScreen = screen
            }

            val navigateBack: () -> Unit = {
                if (navigationStack.size > 1) {
                    navigationDirection = NavigationDirection.POP
                    navigationStack = navigationStack.dropLast(1)
                    currentScreen = navigationStack.last()
                }
            }

            // Tab navigation: resets the stack to just the target screen.
            val navigateToTab: (Screen) -> Unit = { screen ->
                navigationDirection = NavigationDirection.TAB
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

                        MainFlow(
                            currentScreen = currentScreen,
                            currentCity = currentCity,
                            navigateTo = navigateTo,
                            navigateBack = navigateBack,
                            navigateToTab = navigateToTab,
                            authViewModel = authViewModel,
                            sessionManager = sessionManager,
                            navigationDirection = navigationDirection,
                        )
                    }
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

    // Allow back navigation within the auth flow (OTP → phone, name → OTP)
    PlatformBackHandler(enabled = currentAuthStep != "phone") {
        when (currentAuthStep) {
            "otp" -> { currentAuthStep = "phone"; viewModel.resetToIdle() }
            "name" -> { currentAuthStep = "otp" }
        }
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
                onSubmitName = { fullName -> viewModel.submitName(fullName) },
                uiState = uiState,
            )
        }
    }

    if (uiState is AuthUiState.Success) {
        LaunchedEffect(uiState) {
            navigateTo(Screen.Home)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFlow(
    currentScreen: Screen,
    currentCity: String?,
    navigateTo: (Screen) -> Unit,
    navigateBack: () -> Unit,
    navigateToTab: (Screen) -> Unit,
    authViewModel: AuthViewModel,
    sessionManager: SessionManager,
    navigationDirection: NavigationDirection = NavigationDirection.TAB,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val homeViewModel: HomeViewModel = koinInject()
    val searchUiState by homeViewModel.searchUiState.collectAsState()
    val favoritedProfileIds by homeViewModel.favoritedProfileIds.collectAsState()
    val hasMoreResults by homeViewModel.hasMore.collectAsState()
    val homeCatalog by homeViewModel.catalog.collectAsState()

    LaunchedEffect(homeViewModel) {
        homeViewModel.toastMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Auth-related state collected once here — all screens in this shell share the same owner.
    val currentUser by sessionManager.currentUser.collectAsState()
    val authUiState by authViewModel.uiState.collectAsState()
    val hydrationFailed by authViewModel.hydrationFailed.collectAsState()

    val showEarnMoneyCard by homeViewModel.showEarnMoneyCard.collectAsState()
    val popularServices by homeViewModel.popularServices.collectAsState()

    LaunchedEffect(currentCity) {
        homeViewModel.loadPopularServices(currentCity)
    }

    var currentQuery by remember { mutableStateOf("") }
    var currentProfileId by remember { mutableStateOf("") }

    var showCitySheet by remember { mutableStateOf(false) }
    val citySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(currentCity) {
        if (currentCity == null) showCitySheet = true
    }

    val isTopLevelScreen = currentScreen == Screen.Home ||
            currentScreen == Screen.Favorites ||
            currentScreen == Screen.MyProfile

    // Global back handler: pop the navigation stack on hardware/gesture back
    // when not on a root tab screen. Screen-specific handlers (e.g. Onboarding)
    // can override this by registering their own PlatformBackHandler.
    PlatformBackHandler(enabled = !isTopLevelScreen) {
        navigateBack()
    }

    Scaffold(
        bottomBar = {
            if (isTopLevelScreen) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentScreen == Screen.Home,
                        onClick = { navigateToTab(Screen.Home) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == Screen.Home) Icons.Filled.Home
                                              else Icons.Outlined.Home,
                                contentDescription = Strings.Navigation.HOME
                            )
                        },
                        label = { Text(Strings.Navigation.HOME) }
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.Favorites,
                        onClick = { navigateToTab(Screen.Favorites) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == Screen.Favorites) Icons.Filled.Favorite
                                              else Icons.Outlined.FavoriteBorder,
                                contentDescription = Strings.Navigation.FAVORITES
                            )
                        },
                        label = { Text(Strings.Navigation.FAVORITES) }
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.MyProfile,
                        onClick = { navigateToTab(Screen.MyProfile) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == Screen.MyProfile) Icons.Filled.Person
                                              else Icons.Outlined.Person,
                                contentDescription = Strings.Navigation.PROFILE
                            )
                        },
                        label = { Text(Strings.Navigation.PROFILE) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    when (navigationDirection) {
                        NavigationDirection.TAB -> fadeIn() togetherWith fadeOut()
                        NavigationDirection.PUSH -> slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                        NavigationDirection.POP -> slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                    }
                },
                label = "screenTransition",
                modifier = Modifier.fillMaxSize(),
            ) { screen ->
                when (screen) {
                    is Screen.Home -> {
                        HomeScreen(
                            currentUser = currentUser,
                            currentCity = currentCity,
                            showEarnMoneyCard = showEarnMoneyCard,
                            popularServices = popularServices,
                            onCityClick = { showCitySheet = true },
                            onProfileClick = { navigateToTab(Screen.MyProfile) },
                            onSearch = { query ->
                                currentQuery = query
                                homeViewModel.search(query)
                                navigateTo(Screen.SearchResults)
                            },
                            onOfferServices = { navigateTo(Screen.OnboardingStart) },
                            onDismissOfferServices = { homeViewModel.dismissOfferServicesCard() },
                            onPopularServiceClick = { serviceName ->
                                currentQuery = serviceName
                                homeViewModel.search(serviceName)
                                navigateTo(Screen.SearchResults)
                            },
                            onNavigateToCategoryBrowsing = { navigateTo(Screen.CategoryBrowsing) },
                            onVoiceInput = { homeViewModel.setInputMode(com.fugisawa.quemfaz.contract.profile.InputMode.VOICE) },
                        )
                    }
                    is Screen.CitySelection -> {
                        CitySelectionScreen(
                            cities = homeViewModel.supportedCities,
                            currentCity = currentCity,
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
                            catalog = homeCatalog,
                            favoritedProfileIds = favoritedProfileIds,
                            onFavoriteToggle = { profileId -> homeViewModel.toggleFavoriteFromSearch(profileId) },
                            onProfileClick = { id ->
                                currentProfileId = id
                                navigateTo(Screen.ProfessionalProfile)
                            },
                            onNavigateBack = navigateBack,
                            hasMore = hasMoreResults,
                            onLoadMore = { homeViewModel.loadMoreResults() },
                            onCategorySelected = { serviceId -> homeViewModel.searchByServiceId(serviceId) },
                        )
                    }
                    is Screen.ProfessionalProfile -> {
                        val profileViewModel: ProfileViewModel = koinInject()
                        val profileUiState by profileViewModel.uiState.collectAsState()
                        val profileDisabled by profileViewModel.profileDisabled.collectAsState()
                        LaunchedEffect(currentProfileId) {
                            profileViewModel.loadProfile(currentProfileId)
                            profileViewModel.trackProfileView(currentProfileId)
                        }
                        LaunchedEffect(profileDisabled) {
                            if (profileDisabled) navigateBack()
                        }
                        ProfessionalProfileScreen(
                            id = currentProfileId,
                            uiState = profileUiState,
                            onContactClick = { channel ->
                                profileViewModel.trackContactClick(currentProfileId, channel)
                                val profile = (profileUiState as? ProfileUiState.Content)?.profile
                                when (channel) {
                                    ContactChannelDto.WHATSAPP -> {
                                        val digits = profile?.phone?.filter { it.isDigit() }
                                        if (!digits.isNullOrBlank()) openUrl("https://wa.me/$digits")
                                    }
                                    ContactChannelDto.PHONE_CALL -> {
                                        val phone = profile?.phone?.ifBlank { null }
                                        if (phone != null) openUrl("tel:$phone")
                                    }
                                }
                            },
                            onFavoriteToggle = { profileViewModel.toggleFavorite(currentProfileId) },
                            onReportSubmit = { reason ->
                                profileViewModel.reportProfile(currentProfileId, reason, null)
                            },
                            onEditProfile = { navigateTo(Screen.EditProfessionalProfile) },
                            onDisableProfile = { profileViewModel.disableProfile() },
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
                            onRetry = { favoritesViewModel.loadFavorites() },
                            onFindProfessionals = { navigateToTab(Screen.Home) },
                        )
                    }
                    is Screen.MyProfile -> {
                        MyProfileScreen(
                            currentUser = currentUser,
                            uiState = authUiState,
                            hydrationFailed = hydrationFailed,
                            onSaveName = { fullName -> authViewModel.submitName(fullName) },
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
                        val selectedCity by viewModel.selectedCity.collectAsState()
                        val onboardingCatalog by viewModel.catalog.collectAsState()

                        // Onboarding-specific back: navigate within steps for non-Idle,
                        // pop back to previous screen for Idle/Loading.
                        val isOnboardingInProgress =
                            uiState is OnboardingUiState.NeedsClarification ||
                            uiState is OnboardingUiState.ReviewServices ||
                            uiState is OnboardingUiState.ReviewDescription ||
                            uiState is OnboardingUiState.PhotoRequired ||
                            uiState is OnboardingUiState.KnownName
                        PlatformBackHandler(enabled = isOnboardingInProgress) {
                            viewModel.goBack()
                        }

                        LaunchedEffect(currentCity) {
                            viewModel.initializeCity(currentCity)
                        }

                        val imagePicker = rememberImagePickerLauncher { data, mimeType ->
                            val photoState = (uiState as? OnboardingUiState.PhotoRequired) ?: return@rememberImagePickerLauncher
                            viewModel.submitPhoto(data, mimeType, photoState.draft, photoState.confirmedServiceIds, photoState.confirmedDescription)
                        }

                        OnboardingScreens(
                            uiState = uiState,
                            selectedCity = selectedCity,
                            catalog = onboardingCatalog,
                            onSubmitDateOfBirth = { viewModel.submitDateOfBirth(it) },
                            onCreateDraft = { viewModel.createDraft(it) },
                            onSelectCity = { viewModel.selectCity(it) },
                            onProceedFromServices = { draft, serviceIds -> viewModel.proceedFromServices(draft, serviceIds) },
                            onProceedWithManualServices = { draft, serviceIds -> viewModel.proceedWithManualServices(draft, serviceIds) },
                            onProceedFromDescription = { draft, serviceIds, description -> viewModel.proceedFromDescription(draft, serviceIds, description) },
                            onPickPhoto = { imagePicker.launch() },
                            onSubmitKnownName = { knownName, serviceIds, description -> viewModel.submitKnownName(knownName, serviceIds, description) },
                            onSubmitClarifications = { desc, answers -> viewModel.submitClarifications(desc, answers) },
                            onSkipClarification = { draft -> viewModel.skipClarification(draft) },
                            onBack = {
                                if (uiState is OnboardingUiState.BirthDateRequired) {
                                    navigateBack()
                                } else {
                                    viewModel.goBack()
                                }
                            },
                            onFinish = { profile ->
                                currentProfileId = profile.id
                                navigateToTab(Screen.Home)
                                navigateTo(Screen.ProfessionalProfile)
                            }
                        )
                    }
                    is Screen.CategoryBrowsing -> {
                        CategoryBrowsingScreen(
                            onServiceClick = { serviceName ->
                                currentQuery = serviceName
                                homeViewModel.search(serviceName)
                                navigateTo(Screen.SearchResults)
                            },
                            onBack = { navigateBack() }
                        )
                    }
                    is Screen.EditProfessionalProfile -> {
                        val viewModel: EditProfessionalProfileViewModel = koinInject()
                        val uiState by viewModel.uiState.collectAsState()
                        val editedServiceIds by viewModel.editedServiceIds.collectAsState()
                        val editCatalog by viewModel.catalog.collectAsState()
                        LaunchedEffect(Unit) {
                            viewModel.loadProfile()
                        }
                        EditProfessionalProfileScreen(
                            uiState = uiState,
                            editedServiceIds = editedServiceIds,
                            catalog = editCatalog,
                            onAddService = viewModel::addService,
                            onRemoveService = viewModel::removeService,
                            onSave = { desc, city, phone ->
                                viewModel.saveProfile(desc, city, phone)
                            },
                            onNavigateBack = navigateBack,
                            onGoToOnboarding = { navigateTo(Screen.OnboardingStart) }
                        )
                    }
                    else -> {
                        Text("Screen $screen not yet fully wired in MVP Shell")
                    }
                }
            }
        }
    }

    if (showCitySheet) {
        ModalBottomSheet(
            onDismissRequest = { showCitySheet = false },
            sheetState = citySheetState,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = Spacing.screenEdge)
                    .padding(bottom = Spacing.xl)
            ) {
                Text(Strings.CitySelection.TITLE, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(Spacing.md))
                homeViewModel.supportedCities.forEach { city ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                homeViewModel.selectCity(city)
                                showCitySheet = false
                            }
                            .padding(vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = city == currentCity,
                            onClick = {
                                homeViewModel.selectCity(city)
                                showCitySheet = false
                            }
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(city, style = MaterialTheme.typography.bodyLarge)
                    }
                    HorizontalDivider(
                        thickness = Spacing.divider,
                    )
                }
            }
        }
    }
}
