# pt-BR Localization Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Translate all user-facing strings in the QuemFaz application to Brazilian Portuguese using a centralized `Strings.kt` file.

**Architecture:** Create a single `Strings.kt` file with nested objects per feature area (Home, Auth, Onboarding, etc.). Replace all hardcoded string literals in Composables and ViewModels with references to `Strings.*` constants. No i18n framework — single-language, constant-based approach.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, `const val` string constants

**Spec:** `docs/superpowers/specs/2026-03-15-pt-br-localization-design.md`

---

## Chunk 1: Create Strings.kt

### Task 1: Create `Strings.kt` with all translations

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/ui/strings/Strings.kt`

- [ ] **Step 1: Create the Strings.kt file**

Create the file at `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/ui/strings/Strings.kt` with the full content below:

```kotlin
package com.fugisawa.quemfaz.ui.strings

object Strings {

    object Common {
        const val RETRY = "Tentar novamente"
        const val CANCEL = "Cancelar"
        const val CONTINUE = "Continuar"
        const val SAVE = "Salvar"
        const val BACK = "Voltar"
    }

    object Navigation {
        const val HOME = "Início"
        const val FAVORITES = "Favoritos"
        const val PROFILE = "Perfil"
    }

    object Home {
        const val SELECT_CITY = "Selecionar cidade"
        const val TITLE = "O que você\nprecisa hoje?"
        const val SEARCH_PLACEHOLDER = "Pintor, eletricista, faxineira, encanador..."
        const val SEARCH = "Buscar"
        const val OFFER_SERVICES = "Ofereça seus serviços"
        const val OFFER_SERVICES_DESCRIPTION = "Descreva o que você faz — nós montamos seu perfil."
        const val DISMISS = "Dispensar"
        const val ADDED_FAVORITE = "Adicionado aos favoritos"
        const val REMOVED_FAVORITE = "Removido dos favoritos"
        const val FAVORITE_ERROR = "Não foi possível atualizar favoritos. Tente novamente."
    }

    object Auth {
        const val WELCOME_TITLE = "Boas-vindas ao Quem Faz"
        const val WELCOME_SUBTITLE = "Encontre profissionais ou ofereça seus serviços."
        const val PHONE_LABEL = "Número de celular"
        const val PHONE_PLACEHOLDER = "(11) 99999-9999"
        const val OTP_TITLE = "Código de verificação"
        const val OTP_CONFIRM = "Confirmar"
        const val NAME_TITLE = "Qual é o seu nome?"
        const val NAME_LABEL = "Nome completo"
        const val PHOTO_PROMPT = "Adicione uma foto para que possam reconhecer você."
        const val PHOTO_CHOOSE = "Escolher foto"
        const val PHOTO_SKIP = "Pular por enquanto"
        const val KNOWN_NAME_TITLE = "Você tem um nome conhecido?"
        const val KNOWN_NAME_SUBTITLE = "Se as pessoas conhecem você por um apelido ou nome profissional, coloque aqui."
        const val KNOWN_NAME_LABEL = "Nome conhecido (opcional)"
        const val KNOWN_NAME_PLACEHOLDER = "Ex.: Joãozinho da Tinta"
        const val SKIP = "Pular"

        fun otpSubtitle(phone: String) = "Digite o código enviado para $phone"
    }

    object Onboarding {
        const val BECOME_PROFESSIONAL = "Torne-se profissional"
        const val DESCRIBE_SERVICES = "Descreva seus serviços com suas próprias palavras. Nós ajudamos a organizar."
        const val DESCRIPTION_PLACEHOLDER = "Ex.: Sou pintor residencial com 10 anos de experiência. Trabalho em Batatais e Ribeirão Preto. Também faço pequenos reparos em paredes."
        const val ANALYZE_SERVICES = "Analisar meus serviços"
        const val LOADING_INTERPRETING = "Interpretando sua descrição..."
        const val LOADING_ANALYZING = "Analisando seus serviços..."
        const val LOADING_ORGANIZING = "Organizando seu perfil..."
        const val LOADING_ALMOST_READY = "Quase pronto..."
        const val MORE_INFO_TITLE = "Precisamos de mais algumas informações"
        const val MORE_INFO_SUBTITLE = "Responda as perguntas abaixo para entendermos melhor seus serviços."
        const val YOUR_ANSWER = "Sua resposta"
        const val SUBMIT_ANSWERS = "Enviar respostas"
        const val SKIP_AND_CONTINUE = "Pular e continuar"
        const val SELECT_SERVICES_TITLE = "Selecione seus serviços"
        const val SELECT_SERVICES_SUBTITLE = "Não conseguimos identificar seus serviços automaticamente. Selecione abaixo os serviços que você oferece."
        const val REVIEW_TITLE = "Revise seus serviços"
        const val REVIEW_SUBTITLE = "Estes são os serviços que identificamos."
        const val INTERPRETED_SERVICES = "Serviços identificados:"
        const val YOUR_CITY = "Sua cidade:"
        const val CITY_LABEL = "Cidade"
        const val CITY_PLACEHOLDER = "Selecione uma cidade"
        const val LOOKS_GOOD = "Tudo certo, continuar"
        const val PROFILE_DESCRIPTION_TITLE = "Descrição do perfil"
        const val PROFILE_DESCRIPTION_SUBTITLE = "Esta é a descrição que as pessoas verão no seu perfil. Você pode editá-la se quiser."
        const val PROFILE_DESCRIPTION_LABEL = "Descrição"
        const val PROFILE_PUBLISHED = "Perfil publicado!"
        const val PROFILE_PUBLISHED_SUBTITLE = "Você já pode ser encontrado(a) por quem precisa dos seus serviços."
        const val VIEW_MY_PROFILE = "Ver meu perfil"
        const val ERROR_TITLE = "Algo deu errado"

        fun stepIndicator(current: Int) = "Passo $current de 5"
    }

    object Profile {
        const val DISABLE_DIALOG_TITLE = "Desativar perfil profissional"
        const val DISABLE_DIALOG_MESSAGE = "Seu perfil profissional será desativado e não aparecerá mais nos resultados de busca. Sua conta continua ativa. Você pode reativá-lo a qualquer momento adicionando serviços novamente."
        const val DISABLE_BUTTON = "Desativar"
        const val SERVICES = "Serviços"
        const val DISABLE_PROFILE = "Desativar perfil profissional"
        const val REPORT_PROFILE = "Denunciar perfil"
        const val EDIT_PROFILE = "Editar perfil"
        const val WHATSAPP = "WhatsApp"
        const val CALL = "Ligar"
        const val REPORT_DIALOG_TITLE = "Denunciar perfil"
        const val REPORT_SELECT_REASON = "Selecione o motivo:"
        const val REPORT_SPAM = "Spam"
        const val REPORT_INAPPROPRIATE = "Conteúdo impróprio"
        const val REPORT_WRONG_PHONE = "Número de telefone incorreto"
        const val REPORT_FAKE = "Perfil falso"
        const val REPORT_ABUSIVE = "Comportamento abusivo"
        const val REPORT_OTHER = "Outro"
        const val REPORT_BUTTON = "Denunciar"
        const val FALLBACK_TITLE = "Profissional"

        fun portfolioPhotoDescription(index: Int, total: Int) = "Foto do portfólio $index de $total"
    }

    object EditProfile {
        const val TITLE = "Perfil profissional"
        const val NO_PROFILE = "Você ainda não tem um perfil profissional."
        const val SETUP_PROFILE = "Criar perfil profissional"
        const val GO_BACK = "Voltar"
        const val SERVICES = "Serviços"
        const val REMOVE = "Remover"
        const val ALL_SERVICES_REMOVED = "Você removeu todos os serviços. Seu perfil profissional será desativado. Você pode reativá-lo a qualquer momento adicionando serviços novamente."
        const val ADD_SERVICE = "Adicionar serviço"
        const val ADD_SERVICES_DIALOG = "Adicionar serviços"
        const val ADD = "Adicionar"
        const val DESCRIPTION = "Descrição"
        const val CITY = "Cidade"
        const val CONTACT_PHONE = "Telefone de contato"
        const val WHATSAPP_PHONE = "Telefone do WhatsApp"
        const val SAVE_SUCCESS = "Perfil salvo com sucesso."
    }

    object MyProfile {
        const val ERROR_LOADING = "Erro ao carregar perfil"
        const val TITLE = "Meu perfil"
        const val ADD_PHOTO_TITLE = "Adicione uma foto de perfil"
        const val ADD_PHOTO_SUBTITLE = "Profissionais com foto recebem mais contatos."
        const val ADD_PHOTO_BUTTON = "Adicionar foto"
        const val FIRST_NAME = "Nome"
        const val LAST_NAME = "Sobrenome"
        const val SAVE_NAME = "Salvar nome"
        const val CHANGE_PHOTO = "Trocar foto"
        const val MY_FAVORITES = "Meus favoritos"
        const val CHANGE_CITY = "Trocar cidade"
        const val PROFESSIONAL_PROFILE = "Perfil profissional"
        const val LOGOUT = "Sair"
    }

    object Favorites {
        const val TITLE = "Favoritos"
        const val EMPTY_TITLE = "Nenhum favorito ainda"
        const val EMPTY_SUBTITLE = "Profissionais que você salvar aparecem aqui."
        const val FIND_PROFESSIONALS = "Buscar profissionais"
    }

    object Search {
        const val NO_RESULTS_TITLE = "Nenhum profissional encontrado"
        const val NO_RESULTS_SUBTITLE = "Tente buscar por outro serviço ou cidade."
        const val BROWSE_BY_CATEGORY = "Ou navegue por categoria"
        const val LOAD_MORE = "Carregar mais"
        const val REMOVE_FAVORITE = "Remover dos favoritos"
        const val ADD_FAVORITE = "Adicionar aos favoritos"

        fun resultsTitle(query: String) = "Resultados para \"$query\""
        fun showingResults(services: String) = "Resultados para: $services"
    }

    object BlockedUser {
        const val TITLE = "Conta bloqueada"
        const val MESSAGE = "Sua conta foi bloqueada temporariamente. Se você acredita que isso foi um engano, entre em contato com nosso suporte."
        const val CONTACT_SUPPORT = "Falar com o suporte"
    }

    object CitySelection {
        const val TITLE = "Selecione sua cidade"
    }

    object StatusChip {
        const val ACTIVE_TODAY = "Ativo(a) hoje"
        const val ACTIVE_YESTERDAY = "Ativo(a) ontem"
        const val ACTIVE_THIS_MONTH = "Ativo(a) este mês"
        const val ACTIVE_RECENTLY = "Ativo(a) recentemente"
        const val COMPLETE_PROFILE = "Completar perfil"

        fun activeDaysAgo(days: Long) = "Ativo(a) há $days dias"
    }

    object Errors {
        const val INVALID_OTP = "Código inválido"
        const val NAME_REQUIRED = "O nome é obrigatório"
        const val UNKNOWN_ERROR = "Erro desconhecido"
        const val FAILED_SAVE_NAME = "Não foi possível salvar o nome"
        const val FAILED_UPLOAD_PHOTO = "Não foi possível enviar a foto"
        const val FAILED_CREATE_DRAFT = "Não foi possível criar o rascunho"
        const val FAILED_PROCESS_CLARIFICATIONS = "Não foi possível processar as respostas"
        const val FAILED_PUBLISH_PROFILE = "Não foi possível publicar o perfil"
        const val FAILED_LOAD_PROFILE = "Não foi possível carregar o perfil"
        const val FAILED_SAVE_PROFILE = "Não foi possível salvar o perfil"
        const val FAILED_LOAD_FAVORITES = "Não foi possível carregar os favoritos"
        const val SEARCH_FAILED = "A busca falhou"
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `cd "C:/Users/Lucas Fugisawa/Developer/quemfaz" && ./gradlew :composeApp:compileKotlinDesktop 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/ui/strings/Strings.kt
git commit -m "feat: add centralized pt-BR Strings.kt for localization"
```

---

## Chunk 2: Replace strings in App.kt and HomeScreen.kt

### Task 2: Replace strings in App.kt (navigation + city selection)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/App.kt`

- [ ] **Step 1: Add import**

Add to the imports at the top of `App.kt`:
```kotlin
import com.fugisawa.quemfaz.ui.strings.Strings
```

- [ ] **Step 2: Replace bottom navigation labels**

Replace the three `label = { Text("...") }` calls in the `NavigationBar`:
- `Text("Home")` → `Text(Strings.Navigation.HOME)`
- `Text("Favorites")` → `Text(Strings.Navigation.FAVORITES)`
- `Text("Profile")` → `Text(Strings.Navigation.PROFILE)`

Also replace their `contentDescription` values with the same constants.

- [ ] **Step 3: Replace city selection modal string**

Replace `"Select your city"` → `Strings.CitySelection.TITLE`

- [ ] **Step 4: Verify it compiles**

Run: `cd "C:/Users/Lucas Fugisawa/Developer/quemfaz" && ./gradlew :composeApp:compileKotlinDesktop 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

### Task 3: Replace strings in HomeScreen.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/HomeScreen.kt`

- [ ] **Step 1: Add import**

Add to the imports:
```kotlin
import com.fugisawa.quemfaz.ui.strings.Strings
```

- [ ] **Step 2: Replace all hardcoded strings**

Replace each string with its `Strings.Home.*` constant:
- `"Select City"` → `Strings.Home.SELECT_CITY`
- `"What do you\nneed today?"` → `Strings.Home.TITLE`
- `"Plumber, tutor, cleaner, electrician..."` → `Strings.Home.SEARCH_PLACEHOLDER`
- `"Search"` → `Strings.Home.SEARCH`
- `"Offer your services"` → `Strings.Home.OFFER_SERVICES`
- `"Describe what you do — AI builds your profile."` → `Strings.Home.OFFER_SERVICES_DESCRIPTION`
- `"Dismiss"` (contentDescription) → `Strings.Home.DISMISS`

- [ ] **Step 3: Verify it compiles**

Run: `cd "C:/Users/Lucas Fugisawa/Developer/quemfaz" && ./gradlew :composeApp:compileKotlinDesktop 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

### Task 4: Replace strings in HomeViewModel.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/HomeViewModel.kt`

- [ ] **Step 1: Add import**

```kotlin
import com.fugisawa.quemfaz.ui.strings.Strings
```

- [ ] **Step 2: Replace toast and error messages**

- `"Removed from favorites"` → `Strings.Home.REMOVED_FAVORITE`
- `"Added to favorites"` → `Strings.Home.ADDED_FAVORITE`
- `"Could not update favorites. Try again."` → `Strings.Home.FAVORITE_ERROR`
- `"Search failed"` → `Strings.Errors.SEARCH_FAILED`

- [ ] **Step 3: Verify it compiles**

Run: `cd "C:/Users/Lucas Fugisawa/Developer/quemfaz" && ./gradlew :composeApp:compileKotlinDesktop 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/App.kt \
       composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/HomeScreen.kt \
       composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/HomeViewModel.kt
git commit -m "feat: localize App.kt, HomeScreen, and HomeViewModel to pt-BR"
```

---

## Chunk 3: Replace strings in Auth flow

### Task 5: Replace strings in AuthScreens.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/AuthScreens.kt`

- [ ] **Step 1: Add import**

```kotlin
import com.fugisawa.quemfaz.ui.strings.Strings
```

- [ ] **Step 2: Replace PhoneLoginScreen strings**

- `"Welcome to Quem Faz"` → `Strings.Auth.WELCOME_TITLE`
- `"Find professionals or offer your services."` → `Strings.Auth.WELCOME_SUBTITLE`
- `"Phone Number"` (label) → `Strings.Auth.PHONE_LABEL`
- `"(11) 99999-9999"` (placeholder) → `Strings.Auth.PHONE_PLACEHOLDER`
- `"Continue"` (button) → `Strings.Common.CONTINUE`

- [ ] **Step 3: Replace OTP screen strings**

- `"Verify OTP"` → `Strings.Auth.OTP_TITLE`
- `"Enter the code sent to $phone"` → `Strings.Auth.otpSubtitle(phone)`
- `"Verify and Login"` → `Strings.Auth.OTP_CONFIRM`

- [ ] **Step 4: Replace name input screen strings**

- `"What's your name?"` → `Strings.Auth.NAME_TITLE`
- `"Full name"` → `Strings.Auth.NAME_LABEL`
- `"Continue"` → `Strings.Common.CONTINUE`

- [ ] **Step 5: Replace photo screen strings**

- `"Add a profile photo so clients can recognize you."` → `Strings.Auth.PHOTO_PROMPT`
- `"Choose photo"` → `Strings.Auth.PHOTO_CHOOSE`
- `"Skip for now"` → `Strings.Auth.PHOTO_SKIP`

- [ ] **Step 6: Replace known name screen strings**

- `"Do you have a known name?"` → `Strings.Auth.KNOWN_NAME_TITLE`
- `"If clients know you by a nickname or trade name, enter it here."` → `Strings.Auth.KNOWN_NAME_SUBTITLE`
- `"Known name (optional)"` → `Strings.Auth.KNOWN_NAME_LABEL`
- `"e.g. Joãozinho da Tinta"` → `Strings.Auth.KNOWN_NAME_PLACEHOLDER`
- `"Continue"` → `Strings.Common.CONTINUE`
- `"Skip"` → `Strings.Auth.SKIP`

- [ ] **Step 7: Verify it compiles**

Run: `cd "C:/Users/Lucas Fugisawa/Developer/quemfaz" && ./gradlew :composeApp:compileKotlinDesktop 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

### Task 6: Replace strings in AuthViewModel.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/AuthViewModel.kt`

- [ ] **Step 1: Add import**

```kotlin
import com.fugisawa.quemfaz.ui.strings.Strings
```

- [ ] **Step 2: Replace error messages**

- `"Invalid OTP"` → `Strings.Errors.INVALID_OTP`
- `"Invalid OTP code"` → `Strings.Errors.INVALID_OTP`
- `"Name is required"` → `Strings.Errors.NAME_REQUIRED`
- `"Unknown error"` → `Strings.Errors.UNKNOWN_ERROR`
- `"Failed to save name"` → `Strings.Errors.FAILED_SAVE_NAME`
- `"Failed to upload photo"` → `Strings.Errors.FAILED_UPLOAD_PHOTO`

- [ ] **Step 3: Verify it compiles**

Run: `cd "C:/Users/Lucas Fugisawa/Developer/quemfaz" && ./gradlew :composeApp:compileKotlinDesktop 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/AuthScreens.kt \
       composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/AuthViewModel.kt
git commit -m "feat: localize AuthScreens and AuthViewModel to pt-BR"
```

---

## Chunk 4: Replace strings in Onboarding flow

### Task 7: Replace strings in OnboardingScreens.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingScreens.kt`

- [ ] **Step 1: Add import**

```kotlin
import com.fugisawa.quemfaz.ui.strings.Strings
```

- [ ] **Step 2: Replace back button and step indicator**

- `"Back"` (contentDescription, line 103) → `Strings.Common.BACK`
- `"Step $currentStep of 5"` → `Strings.Onboarding.stepIndicator(currentStep)`

- [ ] **Step 3: Replace describe services screen strings**

- `"Become a professional"` → `Strings.Onboarding.BECOME_PROFESSIONAL`
- `"Describe your services in your own words. We'll help you organize them."` → `Strings.Onboarding.DESCRIBE_SERVICES`
- `"e.g. I am a residential painter..."` → `Strings.Onboarding.DESCRIPTION_PLACEHOLDER`
- `"Analyze my services"` → `Strings.Onboarding.ANALYZE_SERVICES`

- [ ] **Step 4: Replace loading phrases**

Replace the loading phrases list:
- `"Interpreting your description..."` → `Strings.Onboarding.LOADING_INTERPRETING`
- `"Analyzing your services..."` → `Strings.Onboarding.LOADING_ANALYZING`
- `"Organizing your profile..."` → `Strings.Onboarding.LOADING_ORGANIZING`
- `"Almost ready..."` → `Strings.Onboarding.LOADING_ALMOST_READY`

- [ ] **Step 5: Replace clarification screen strings**

- `"We need a bit more info"` → `Strings.Onboarding.MORE_INFO_TITLE`
- `"Please answer the questions below..."` → `Strings.Onboarding.MORE_INFO_SUBTITLE`
- `"Your answer"` → `Strings.Onboarding.YOUR_ANSWER`
- `"Submit answers"` → `Strings.Onboarding.SUBMIT_ANSWERS`
- `"Skip and continue"` → `Strings.Onboarding.SKIP_AND_CONTINUE`

- [ ] **Step 6: Replace service selection strings (already partially PT)**

- `"Selecione seus serviços"` → `Strings.Onboarding.SELECT_SERVICES_TITLE`
- `"Não conseguimos identificar..."` → `Strings.Onboarding.SELECT_SERVICES_SUBTITLE`
- `"Continuar"` → `Strings.Common.CONTINUE`

- [ ] **Step 7: Replace review screen strings**

- `"Review your services"` → `Strings.Onboarding.REVIEW_TITLE`
- `"These are the services we identified."` → `Strings.Onboarding.REVIEW_SUBTITLE`
- `"Interpreted services:"` → `Strings.Onboarding.INTERPRETED_SERVICES`
- `"Your city:"` → `Strings.Onboarding.YOUR_CITY`
- `"City"` → `Strings.Onboarding.CITY_LABEL`
- `"Select a city"` → `Strings.Onboarding.CITY_PLACEHOLDER`
- `"Looks good, continue"` → `Strings.Onboarding.LOOKS_GOOD`

- [ ] **Step 8: Replace profile description strings (already partially PT)**

- `"Descrição do perfil"` → `Strings.Onboarding.PROFILE_DESCRIPTION_TITLE`
- `"Esta é a descrição que os clientes verão..."` → `Strings.Onboarding.PROFILE_DESCRIPTION_SUBTITLE` (note: this changes "clientes" to "pessoas")
- `"Descrição"` → `Strings.Onboarding.PROFILE_DESCRIPTION_LABEL`
- `"Continuar"` → `Strings.Common.CONTINUE`

- [ ] **Step 9: Replace success/error screen strings**

- `"Profile Published!"` → `Strings.Onboarding.PROFILE_PUBLISHED`
- `"You're now visible to customers."` → `Strings.Onboarding.PROFILE_PUBLISHED_SUBTITLE`
- `"View my profile"` → `Strings.Onboarding.VIEW_MY_PROFILE`
- `"Something went wrong"` → `Strings.Onboarding.ERROR_TITLE`
- `"Retry"` → `Strings.Common.RETRY`

- [ ] **Step 10: Replace photo prompt string (onboarding reuses auth photo screen)**

- `"Add a profile photo so clients can recognize you."` → `Strings.Auth.PHOTO_PROMPT`

- [ ] **Step 11: Verify it compiles**

Run: `cd "C:/Users/Lucas Fugisawa/Developer/quemfaz" && ./gradlew :composeApp:compileKotlinDesktop 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

### Task 8: Replace strings in OnboardingViewModel.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingViewModel.kt`

- [ ] **Step 1: Add import**

```kotlin
import com.fugisawa.quemfaz.ui.strings.Strings
```

- [ ] **Step 2: Replace error messages**

- `"Failed to upload photo"` → `Strings.Errors.FAILED_UPLOAD_PHOTO`
- `"Failed to create draft"` → `Strings.Errors.FAILED_CREATE_DRAFT`
- `"Failed to process clarifications"` → `Strings.Errors.FAILED_PROCESS_CLARIFICATIONS`
- `"Failed to publish profile"` → `Strings.Errors.FAILED_PUBLISH_PROFILE`

- [ ] **Step 3: Verify it compiles**

Run: `cd "C:/Users/Lucas Fugisawa/Developer/quemfaz" && ./gradlew :composeApp:compileKotlinDesktop 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingScreens.kt \
       composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/OnboardingViewModel.kt
git commit -m "feat: localize OnboardingScreens and OnboardingViewModel to pt-BR"
```

---

## Chunk 5: Replace strings in Profile screens

### Task 9: Replace strings in ProfileScreens.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/ProfileScreens.kt`

- [ ] **Step 1: Add import**

```kotlin
import com.fugisawa.quemfaz.ui.strings.Strings
```

- [ ] **Step 2: Replace profile fallback title**

- `"Professional"` (fallback title) → `Strings.Profile.FALLBACK_TITLE`

- [ ] **Step 3: Replace disable profile dialog strings**

- `"Disable Professional Profile"` (dialog title) → `Strings.Profile.DISABLE_DIALOG_TITLE`
- Dialog message text → `Strings.Profile.DISABLE_DIALOG_MESSAGE`
- `"Disable"` (button) → `Strings.Profile.DISABLE_BUTTON`
- `"Cancel"` (button) → `Strings.Common.CANCEL`

- [ ] **Step 4: Replace profile view strings**

- `"Portfolio photo ${index + 1} of ${size}"` → `Strings.Profile.portfolioPhotoDescription(index + 1, profile.portfolioPhotoUrls.size)`
- `"Services"` → `Strings.Profile.SERVICES`
- `"Disable Professional Profile"` (button) → `Strings.Profile.DISABLE_PROFILE`
- `"Report Profile"` (button) → `Strings.Profile.REPORT_PROFILE`
- `"Edit Profile"` (button) → `Strings.Profile.EDIT_PROFILE`
- `"WhatsApp"` → `Strings.Profile.WHATSAPP`
- `"Call"` → `Strings.Profile.CALL`

- [ ] **Step 5: Replace report dialog strings**

- `"Report Profile"` (dialog title) → `Strings.Profile.REPORT_DIALOG_TITLE`
- `"Select a reason:"` → `Strings.Profile.REPORT_SELECT_REASON`
- `"Report"` (button) → `Strings.Profile.REPORT_BUTTON`
- `"Cancel"` → `Strings.Common.CANCEL`

- [ ] **Step 6: Replace `toDisplayName()` report reason labels**

Update the `toDisplayName()` extension function to use `Strings.Profile.*` constants:
- `SPAM` → `Strings.Profile.REPORT_SPAM`
- `INAPPROPRIATE_CONTENT` → `Strings.Profile.REPORT_INAPPROPRIATE`
- `WRONG_PHONE_NUMBER` → `Strings.Profile.REPORT_WRONG_PHONE`
- `FAKE_PROFILE` → `Strings.Profile.REPORT_FAKE`
- `ABUSIVE_BEHAVIOR` → `Strings.Profile.REPORT_ABUSIVE`
- `OTHER` → `Strings.Profile.REPORT_OTHER`

- [ ] **Step 7: Verify it compiles**

Run: `cd "C:/Users/Lucas Fugisawa/Developer/quemfaz" && ./gradlew :composeApp:compileKotlinDesktop 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

### Task 10: Replace strings in ProfileViewModel.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/ProfileViewModel.kt`

- [ ] **Step 1: Add import and replace error messages**

```kotlin
import com.fugisawa.quemfaz.ui.strings.Strings
```

- `"Failed to load profile"` → `Strings.Errors.FAILED_LOAD_PROFILE`

- [ ] **Step 2: Verify it compiles**

Run: `cd "C:/Users/Lucas Fugisawa/Developer/quemfaz" && ./gradlew :composeApp:compileKotlinDesktop 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/ProfileScreens.kt \
       composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/ProfileViewModel.kt
git commit -m "feat: localize ProfileScreens and ProfileViewModel to pt-BR"
```

---

## Chunk 6: Replace strings in remaining screens

### Task 11: Replace strings in EditProfessionalProfileScreen.kt and ViewModel

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/EditProfessionalProfileScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/EditProfessionalProfileViewModel.kt`

- [ ] **Step 1: Add import to both files**

```kotlin
import com.fugisawa.quemfaz.ui.strings.Strings
```

- [ ] **Step 2: Replace EditProfessionalProfileScreen.kt strings**

- `"Professional Profile"` (title) → `Strings.EditProfile.TITLE`
- `"Back"` (contentDescription) → `Strings.Common.BACK`
- `"You don't have a professional profile yet."` → `Strings.EditProfile.NO_PROFILE`
- `"Set up professional profile"` → `Strings.EditProfile.SETUP_PROFILE`
- `"Go Back"` → `Strings.EditProfile.GO_BACK` (appears twice: NoProfile state line 74 and Error state line 90 — replace both)
- `"Serviços"` → `Strings.EditProfile.SERVICES`
- `"Remover"` → `Strings.EditProfile.REMOVE`
- `"You removed all services..."` → `Strings.EditProfile.ALL_SERVICES_REMOVED`
- `"Adicionar serviço"` → `Strings.EditProfile.ADD_SERVICE`
- `"Adicionar serviços"` → `Strings.EditProfile.ADD_SERVICES_DIALOG`
- `"Adicionar"` → `Strings.EditProfile.ADD`
- `"Cancelar"` → `Strings.Common.CANCEL`
- `"Description"` → `Strings.EditProfile.DESCRIPTION`
- `"City"` → `Strings.EditProfile.CITY`
- `"Contact Phone"` → `Strings.EditProfile.CONTACT_PHONE`
- `"WhatsApp Phone"` → `Strings.EditProfile.WHATSAPP_PHONE`
- `"Profile saved successfully."` → `Strings.EditProfile.SAVE_SUCCESS`
- `"Save"` → `Strings.Common.SAVE`

- [ ] **Step 3: Replace EditProfessionalProfileViewModel.kt error messages**

- `"Failed to save profile"` → `Strings.Errors.FAILED_SAVE_PROFILE`

- [ ] **Step 4: Verify it compiles**

Run: `cd "C:/Users/Lucas Fugisawa/Developer/quemfaz" && ./gradlew :composeApp:compileKotlinDesktop 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/EditProfessionalProfileScreen.kt \
       composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/EditProfessionalProfileViewModel.kt
git commit -m "feat: localize EditProfessionalProfileScreen to pt-BR"
```

### Task 12: Replace strings in MyProfileScreen.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/MyProfileScreen.kt`

- [ ] **Step 1: Add import**

```kotlin
import com.fugisawa.quemfaz.ui.strings.Strings
```

- [ ] **Step 2: Replace all strings**

- `"Error loading profile"` → `Strings.MyProfile.ERROR_LOADING`
- `"Retry"` → `Strings.Common.RETRY`
- `"My Profile"` → `Strings.MyProfile.TITLE`
- `"Add a profile photo"` → `Strings.MyProfile.ADD_PHOTO_TITLE`
- `"Clients are more likely to contact professionals with a photo."` → `Strings.MyProfile.ADD_PHOTO_SUBTITLE`
- `"Add photo"` → `Strings.MyProfile.ADD_PHOTO_BUTTON`
- `"First name"` → `Strings.MyProfile.FIRST_NAME`
- `"Last name"` → `Strings.MyProfile.LAST_NAME`
- `"Save name"` → `Strings.MyProfile.SAVE_NAME`
- `"Change photo"` → `Strings.MyProfile.CHANGE_PHOTO`
- `"My Favorites"` → `Strings.MyProfile.MY_FAVORITES`
- `"Change City"` → `Strings.MyProfile.CHANGE_CITY`
- `"Professional Profile"` → `Strings.MyProfile.PROFESSIONAL_PROFILE`
- `"Logout"` → `Strings.MyProfile.LOGOUT`

- [ ] **Step 3: Verify it compiles**

Run: `cd "C:/Users/Lucas Fugisawa/Developer/quemfaz" && ./gradlew :composeApp:compileKotlinDesktop 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/MyProfileScreen.kt
git commit -m "feat: localize MyProfileScreen to pt-BR"
```

### Task 13: Replace strings in FavoritesScreen.kt and FavoritesViewModel.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/FavoritesScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/FavoritesViewModel.kt`

- [ ] **Step 1: Add imports to both files**

```kotlin
import com.fugisawa.quemfaz.ui.strings.Strings
```

- [ ] **Step 2: Replace FavoritesScreen.kt strings**

- `"Favorites"` → `Strings.Favorites.TITLE`
- `"No favorites yet"` → `Strings.Favorites.EMPTY_TITLE`
- `"Professionals you save will appear here."` → `Strings.Favorites.EMPTY_SUBTITLE`
- `"Find professionals"` → `Strings.Favorites.FIND_PROFESSIONALS`

- [ ] **Step 3: Replace FavoritesViewModel.kt error message**

- `"Failed to load favorites"` → `Strings.Errors.FAILED_LOAD_FAVORITES`

- [ ] **Step 4: Verify it compiles**

Run: `cd "C:/Users/Lucas Fugisawa/Developer/quemfaz" && ./gradlew :composeApp:compileKotlinDesktop 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/FavoritesScreen.kt \
       composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/FavoritesViewModel.kt
git commit -m "feat: localize FavoritesScreen to pt-BR"
```

### Task 14: Replace strings in SearchScreens.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/SearchScreens.kt`

- [ ] **Step 1: Add import**

```kotlin
import com.fugisawa.quemfaz.ui.strings.Strings
```

- [ ] **Step 2: Replace all strings**

- `"Showing results for: ${...}"` → `Strings.Search.showingResults(uiState.response.interpretedServices.joinToString(" · ") { it.displayName })`
- `"No professionals found"` → `Strings.Search.NO_RESULTS_TITLE`
- `"Try a different search term or city."` → `Strings.Search.NO_RESULTS_SUBTITLE`
- `"Ou navegue por categoria"` → `Strings.Search.BROWSE_BY_CATEGORY`
- `"Load more"` → `Strings.Search.LOAD_MORE`
- `"Remove from favorites"` / `"Add to favorites"` (contentDescription) → `Strings.Search.REMOVE_FAVORITE` / `Strings.Search.ADD_FAVORITE`
- The `AppScreen(title = ...)` call for search results → use `Strings.Search.resultsTitle(query)` if available

- [ ] **Step 3: Verify it compiles**

Run: `cd "C:/Users/Lucas Fugisawa/Developer/quemfaz" && ./gradlew :composeApp:compileKotlinDesktop 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/SearchScreens.kt
git commit -m "feat: localize SearchScreens to pt-BR"
```

---

## Chunk 7: Replace strings in remaining screens and components

### Task 15: Replace strings in BlockedUserScreen.kt and CitySelectionScreen.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/BlockedUserScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/CitySelectionScreen.kt`

- [ ] **Step 1: Add imports to both files**

```kotlin
import com.fugisawa.quemfaz.ui.strings.Strings
```

- [ ] **Step 2: Replace BlockedUserScreen.kt strings**

- `"Account Blocked"` → `Strings.BlockedUser.TITLE`
- `"Your account has been temporarily blocked..."` → `Strings.BlockedUser.MESSAGE`
- `"Contact Support"` → `Strings.BlockedUser.CONTACT_SUPPORT`

- [ ] **Step 3: Replace CitySelectionScreen.kt strings**

- `"Select your city"` → `Strings.CitySelection.TITLE`

- [ ] **Step 4: Verify it compiles**

Run: `cd "C:/Users/Lucas Fugisawa/Developer/quemfaz" && ./gradlew :composeApp:compileKotlinDesktop 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/BlockedUserScreen.kt \
       composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/CitySelectionScreen.kt
git commit -m "feat: localize BlockedUserScreen and CitySelectionScreen to pt-BR"
```

### Task 16: Replace strings in UI components

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/ui/components/AppScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/ui/components/ErrorMessage.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/ui/components/StatusChipRow.kt`

- [ ] **Step 1: Add imports to all three files**

```kotlin
import com.fugisawa.quemfaz.ui.strings.Strings
```

- [ ] **Step 2: Replace AppScreen.kt strings**

- `"Back"` (contentDescription for back arrow icon, line 36) → `Strings.Common.BACK`

- [ ] **Step 3: Replace ErrorMessage.kt strings**

- `"Retry"` → `Strings.Common.RETRY`

- [ ] **Step 4: Replace StatusChipRow.kt strings**

- `"Active today"` → `Strings.StatusChip.ACTIVE_TODAY`
- `"Active yesterday"` → `Strings.StatusChip.ACTIVE_YESTERDAY`
- `"Active $daysSinceActive days ago"` → `Strings.StatusChip.activeDaysAgo(daysSinceActive)`
- `"Active this month"` → `Strings.StatusChip.ACTIVE_THIS_MONTH`
- `"Active recently"` → `Strings.StatusChip.ACTIVE_RECENTLY`
- `"Complete profile"` → `Strings.StatusChip.COMPLETE_PROFILE`

- [ ] **Step 5: Verify it compiles**

Run: `cd "C:/Users/Lucas Fugisawa/Developer/quemfaz" && ./gradlew :composeApp:compileKotlinDesktop 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/ui/components/AppScreen.kt \
       composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/ui/components/ErrorMessage.kt \
       composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/ui/components/StatusChipRow.kt
git commit -m "feat: localize AppScreen, ErrorMessage, and StatusChipRow components to pt-BR"
```

---

## Chunk 8: Final verification

### Task 17: Full build verification and string audit

- [ ] **Step 1: Run full composeApp compilation**

Run: `cd "C:/Users/Lucas Fugisawa/Developer/quemfaz" && ./gradlew :composeApp:compileKotlinDesktop 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Audit for any remaining hardcoded English strings**

Search all `.kt` files in `composeApp/src/commonMain/` for remaining hardcoded `Text("` calls that are not using `Strings.*` references. Any remaining English user-facing strings should be flagged and replaced.

Run: `grep -rn 'Text("' composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/ composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/ui/components/ | grep -v 'Strings\.' | grep -v '//' | grep -v 'import'`

Expected: No user-facing English strings remaining (dynamic content like `Text(profile.name)` is fine).

- [ ] **Step 3: Verify no remaining English in ViewModels**

Run: `grep -rn '"Failed\|"Error\|"Invalid\|"Unknown\|"Search failed\|"Could not\|"Added to\|"Removed from' composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/screens/`

Expected: No matches (all should reference `Strings.*` now).

- [ ] **Step 4: Commit any fixes from the audit**

If any remaining strings were found and fixed:
```bash
git add -u composeApp/src/commonMain/
git commit -m "fix: replace remaining hardcoded English strings with pt-BR"
```
