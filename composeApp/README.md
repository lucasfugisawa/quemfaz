# composeApp

Shared UI and client logic for all platforms (Android, iOS, Web). Built with Compose Multiplatform.

---

## Role in the architecture

`composeApp` owns everything the user sees and interacts with. It depends on `shared` for API contracts and domain types. It communicates with the server exclusively through `ApiClient` + `FeatureApiClients`.

---

## Source sets

| Source set | Purpose |
|---|---|
| `commonMain` | All shared UI screens, ViewModels, navigation, session, DI, network client |
| `androidMain` | Android-specific: `MainActivity` context access, URL launcher impl |
| `iosMain` | iOS-specific: `MainViewController`, URL launcher impl |
| `jsMain` / `webMain` | JS/WASM entry points |
| `commonTest` | Client-side unit tests |

All meaningful code lives in `commonMain`. Platform source sets are thin.

---

## Key packages

```
commonMain/kotlin/com/fugisawa/quemfaz/
├── screens/          UI screens (one file per feature area)
├── ui/
│   ├── components/   Reusable Composables
│   └── theme/        Design system (colors, typography, shapes, spacing)
├── navigation/       Screen sealed class + App.kt (shell and navigation)
├── network/          ApiClient, FeatureApiClients
├── session/          SessionManager, SessionStorage, AuthState
├── di/               Koin module definitions
└── platform/         UrlLauncher (expect/actual abstraction)
```

---

## Screens

| Screen | File | Auth required |
|---|---|---|
| PhoneLogin, OtpVerification, CompleteUserProfile | `AuthScreens.kt` | No |
| Home (search entry) | `HomeScreen.kt` | No |
| SearchResults | `SearchScreens.kt` | No |
| ProfessionalProfile | `ProfileScreens.kt` | No (favorites/report require auth) |
| Favorites | `FavoritesScreen.kt` | Yes |
| MyProfile | `MyProfileScreen.kt` | Yes |
| Onboarding (BirthDate → NaturalPresentation → SmartConfirmation → ProfilePreview) | `OnboardingScreens.kt` | Yes |
| EditProfessionalProfile | `EditProfessionalProfileScreen.kt` | Yes |
| CitySelection | `CitySelectionScreen.kt` | Post-auth gate |
| BlockedUser | `BlockedUserScreen.kt` | N/A |

---

## Navigation

Navigation uses a **custom screen-stack system** — not the Compose Navigation library.

**How it works:**

- Screens are a sealed class `Screen` in `navigation/Screen.kt`
- `App.kt` holds the navigation stack as `mutableStateListOf<Screen>`
- `navigateTo(screen)` → push (enables back)
- `navigateToTab(screen)` → replace entire stack (bottom-nav tab switches)
- `navigateBack()` → pop
- After authentication, if `currentCity == null`, the city gate redirects to `CitySelectionScreen` (no back navigation out)

**Do not introduce Compose Navigation** — the current approach is intentional for its simplicity at this scale.

---

## Session management

`SessionManager` is the single source of truth for auth state.

```
AuthState: Loading | Unauthenticated | Authenticated | Blocked
```

- Exposes `authState: StateFlow<AuthState>`, `currentUser: StateFlow<UserProfileResponse?>`, `currentCity: StateFlow<String?>`
- Persists JWT token and city via `SettingsSessionStorage` (multiplatform-settings)
- Token is injected into every request by `ApiClient` — do not pass tokens manually
- 401 responses trigger `SessionManager.logout()` globally via `ApiClient`'s `onUnauthorized` callback

---

## Network layer

- `ApiClient` — the Ktor `HttpClient` wrapper. Handles JWT header injection, 401 global handling, JSON content negotiation.
- `FeatureApiClients` — groups feature-scoped clients: `AuthApiClient`, `ProfileApiClient`, `SearchApiClient`, `EngagementApiClient`, `FavoritesApiClient`, `ModerationApiClient`.

Feature clients must not re-implement auth. All auth concerns are in `ApiClient`.

---

## Design system

All design tokens are in `ui/theme/`. Always use them — never hardcode values.

| Token object | Use for |
|---|---|
| `AppTheme` | Root theme wrapper — wrap every screen in this |
| `AppSpacing` | All `padding`, `spacedBy`, `Modifier.padding()` |
| `AppTypography` | All `Text` styles |
| `AppShapes` | Corner radii |
| `AppColorScheme` | Colors via `MaterialTheme.colorScheme` (light/dark handled automatically) |

---

## Reusable components

Located in `ui/components/`. Check these before creating new ones.

| Component | Purpose |
|---|---|
| `AppScreen` | Common scaffold with `TopAppBar` — use for all screens |
| `ErrorMessage` | Standardized error display |
| `FullScreenLoading` | Loading state overlay |
| `ProfileAvatar` | Professional profile photo/placeholder |
| `ServiceChipList` | Horizontal chip list of services |
| `StatusChipRow` | Trust signal chips (complete profile, recently active) |
| `ChatBubble` | Rounded surfaceVariant card for conversational system messages |
| `ServiceListItem` | Row with checkmark icon + service name (used in profile & onboarding) |
| `VoiceInputButton` | Voice input button with optional `compact` mode (40dp inline) and pulse animation when listening |

---

## Dependency injection

Koin modules are defined in `di/AppModule.kt`.

- `single {}` for `SessionManager`, `ApiClient`, `FeatureApiClients`
- `factory {}` for all ViewModels

ViewModels are injected into Composables via `koinViewModel<T>()` or `koinInject<T>()`.

---

## Adding a new screen

1. Add the `Screen` subclass to `navigation/Screen.kt`
2. Create the ViewModel (extend `ViewModel`, expose `StateFlow`)
3. Register the ViewModel as `factory {}` in `di/AppModule.kt`
4. Add the Composable screen function in the appropriate `screens/` file
5. Wire the route in `App.kt` / `MainFlow()`
6. Update this README (screens table, any new component or pattern)
