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
            val currentCityId by sessionManager.currentCityId.collectAsState()

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
                        BlockedUserScreen(onContactSupport = { openUrl("mailto:${AppLinks.SUPPORT_EMAIL}") })
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

                        val currentUser by sessionManager.currentUser.collectAsState()

                        // Check if the user needs to re-accept updated terms/privacy.
                        // The server provides the required versions; compare against what the user accepted.
                        val needsTermsUpdate = currentUser?.let { user ->
                            val requiredTerms = user.requiredTermsVersion
                            val requiredPrivacy = user.requiredPrivacyVersion
                            requiredTerms != null && requiredPrivacy != null && (
                                user.termsVersion != requiredTerms ||
                                user.privacyVersion != requiredPrivacy
                            )
                        } ?: false

                        MainFlow(
                            currentScreen = currentScreen,
                            currentCityId = currentCityId,
                            navigateTo = navigateTo,
                            navigateBack = navigateBack,
                            navigateToTab = navigateToTab,
                            authViewModel = authViewModel,
                            sessionManager = sessionManager,
                            navigationDirection = navigationDirection,
                        )

                        if (needsTermsUpdate) {
                            TermsUpdateDialog(
                                onAccept = {
                                    val user = currentUser!!
                                    authViewModel.acceptTerms(
                                        termsVersion = user.requiredTermsVersion!!,
                                        privacyVersion = user.requiredPrivacyVersion!!,
                                    )
                                    authViewModel.fetchCurrentUser()
                                },
                                onLogout = { authViewModel.logout() },
                            )
                        }
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
            "terms" -> { /* cannot go back from terms */ }
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
            // Existing user (no profile completion) → show terms before entering the app
            if (uiState is AuthUiState.Success && currentAuthStep == "otp") {
                LaunchedEffect(uiState) { currentAuthStep = "terms" }
            }
        }
        "name" -> {
            NameInputScreen(
                onSubmitName = { fullName -> viewModel.submitName(fullName) },
                uiState = uiState,
            )
            // New user finished name → show terms
            if (uiState is AuthUiState.Success && currentAuthStep == "name") {
                LaunchedEffect(uiState) { currentAuthStep = "terms" }
            }
        }
        "terms" -> {
            TermsAcceptanceScreen(
                onAccept = {
                    viewModel.acceptTerms()
                    navigateTo(Screen.Home)
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFlow(
    currentScreen: Screen,
    currentCityId: String?,
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
    val searchHistory by homeViewModel.searchHistory.collectAsState()

    val cities by homeViewModel.cityRepository.cities.collectAsState()

    LaunchedEffect(currentCityId) {
        homeViewModel.loadPopularServices(currentCityId)
    }

    var currentQuery by remember { mutableStateOf("") }
    var currentProfileId by remember { mutableStateOf("") }

    var showCitySheet by remember { mutableStateOf(false) }
    val citySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(currentCityId) {
        if (currentCityId == null) showCitySheet = true
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
                            currentCity = homeViewModel.cityRepository.getCityDisplayName(currentCityId),
                            showEarnMoneyCard = showEarnMoneyCard,
                            popularServices = popularServices,
                            searchHistory = searchHistory,
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
                            cities = cities,
                            currentCityId = currentCityId,
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
                        val selectedCityId by viewModel.selectedCityId.collectAsState()
                        val onboardingCities by viewModel.cityRepository.cities.collectAsState()
                        val onboardingCatalog by viewModel.catalog.collectAsState()

                        val isOnboardingInProgress =
                            uiState is OnboardingUiState.NaturalPresentation ||
                            uiState is OnboardingUiState.NeedsClarification ||
                            uiState is OnboardingUiState.SmartConfirmation ||
                            uiState is OnboardingUiState.PhotoRequired ||
                            uiState is OnboardingUiState.ProfilePreview
                        PlatformBackHandler(enabled = isOnboardingInProgress) {
                            viewModel.goBack()
                        }

                        LaunchedEffect(currentCityId) {
                            viewModel.initializeCity(currentCityId)
                        }

                        val imagePicker = rememberImagePickerLauncher { data, mimeType ->
                            val photoState = (uiState as? OnboardingUiState.PhotoRequired) ?: return@rememberImagePickerLauncher
                            viewModel.submitPhoto(data, mimeType, photoState.draft, photoState.confirmedServiceIds, photoState.confirmedDescription)
                        }

                        OnboardingScreens(
                            uiState = uiState,
                            selectedCityId = selectedCityId,
                            selectedCityDisplayName = viewModel.cityRepository.getCityDisplayName(selectedCityId),
                            cities = onboardingCities,
                            catalog = onboardingCatalog,
                            onSubmitDateOfBirth = { viewModel.submitDateOfBirth(it) },
                            onCreateDraft = { text, mode -> viewModel.createDraft(text, mode) },
                            onSelectCity = { viewModel.selectCity(it) },
                            onConfirmFromSmartConfirmation = { draft, serviceIds, description -> viewModel.confirmFromSmartConfirmation(draft, serviceIds, description) },
                            onProceedWithManualServices = { draft, serviceIds -> viewModel.proceedWithManualServices(draft, serviceIds) },
                            onPickPhoto = { imagePicker.launch() },
                            onPublishProfile = { fullName, knownName, serviceIds, description -> viewModel.publishProfile(fullName, knownName, serviceIds, description) },
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
                        val editCities by viewModel.cityRepository.cities.collectAsState()
                        LaunchedEffect(Unit) {
                            viewModel.loadProfile()
                        }
                        EditProfessionalProfileScreen(
                            uiState = uiState,
                            editedServiceIds = editedServiceIds,
                            catalog = editCatalog,
                            cities = editCities,
                            onAddService = viewModel::addService,
                            onRemoveService = viewModel::removeService,
                            onSave = { desc, cityId ->
                                viewModel.saveProfile(desc, cityId)
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
                cities.forEach { city ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                homeViewModel.selectCity(city.id)
                                showCitySheet = false
                            }
                            .padding(vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = city.id == currentCityId,
                            onClick = {
                                homeViewModel.selectCity(city.id)
                                showCitySheet = false
                            }
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(city.name, style = MaterialTheme.typography.bodyLarge)
                    }
                    HorizontalDivider(
                        thickness = Spacing.divider,
                    )
                }
            }
        }
    }
}
