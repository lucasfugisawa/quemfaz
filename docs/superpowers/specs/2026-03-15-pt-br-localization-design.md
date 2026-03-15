# QuemFaz — Brazilian Portuguese Localization Design

## Overview

Translate and adapt all user-facing content in the QuemFaz application to Brazilian Portuguese, with a consistent tone, vocabulary, and language style that feels natural, modern, and welcoming.

**Scope:** User-facing strings only. Source code, identifiers, logs, and server API messages remain in English.

---

## Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Localization infrastructure | Centralized `Strings.kt` with nested objects per feature | Single-language app; centralizes all text for consistency without i18n overhead |
| Server messages | Remain in English | API-level concerns; client owns user-facing error display |
| Logs | Remain in English | Developer-facing; consistent with codebase language |
| Pronoun | "Você" (implicit when possible) | National standard, warm, universally understood |
| Gender neutrality | Prefer neutral constructions; "(a)" as fallback | Inclusive language; never masculine-only |
| App name in UI | "Quem Faz" (with space) | More readable in Portuguese |
| Service seekers | Addressed as "você", never labeled "cliente" | Avoids transactional tone; matches conversational voice |
| Service providers | "profissional" / "profissionais" | Respectful, clear, gender-neutral |
| Action button vocabulary | "Salvar" (persist data), "Concluir" (finish process), "Confirmar" (confirm action) | Gives users clearer intent signals per context |

---

## Tone & Voice Guidelines

**Voice character:** Friendly neighbor who happens to be tech-savvy. Professional platform, everyday language.

**Principles:**

- **Direto** — short sentences, active voice, no corporate fluff
- **Acolhedor** — warm, uses "você" naturally, never cold or bureaucratic
- **Inclusivo** — gender-neutral by default; prefer neutral constructions, use "(a)" when needed
- **Simples** — accessible to people with limited tech experience; avoid jargon

**Do:**

- "O que você precisa hoje?"
- "Boas-vindas ao Quem Faz"
- "Profissionais que você salvou aparecem aqui"

**Don't:**

- "Bem-vindo ao sistema QuemFaz" (corporate, gendered)
- "Nenhum resultado foi encontrado para sua consulta" (bureaucratic)
- "Favor informar o número de telefone" (overly formal)

---

## Key Terminology

| Concept | pt-BR | Notes |
|---|---|---|
| Professional / service provider | profissional | Gender-neutral |
| Services | serviços | |
| Profile | perfil | |
| Professional profile | perfil profissional | |
| Search | buscar / busca | More natural than "pesquisar" |
| Favorites | favoritos | |
| City | cidade | |
| Phone number | número de celular | "Celular" more natural in Brazil |
| Photo | foto | |
| Log in | entrar | Avoids gendered forms |
| Log out | sair | |
| Continue | continuar | |
| Cancel | cancelar | |
| Retry | tentar novamente | |
| Save (data) | salvar | For persisting forms/data |
| Finish (process) | concluir | For completing multi-step flows |
| Confirm (action) | confirmar | For confirming discrete actions |
| Back | voltar | |
| Report | denunciar | |
| Disable (profile) | desativar | Non-destructive sounding |
| Account | conta | |
| Support | suporte | |
| Verification code | código de verificação | Avoids OTP jargon |
| Known name | nome conhecido | |
| Description | descrição | |

---

## Localization Infrastructure

### File: `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/ui/strings/Strings.kt`

A single Kotlin object with nested objects per feature area:

```kotlin
object Strings {
    object Common { ... }
    object Home { ... }
    object Auth { ... }
    object Onboarding { ... }
    object Profile { ... }
    object EditProfile { ... }
    object MyProfile { ... }
    object Favorites { ... }
    object Search { ... }
    object BlockedUser { ... }
    object CitySelection { ... }
    object StatusChip { ... }
}
```

Each screen/component references `Strings.FeatureName.CONSTANT` instead of hardcoded literals.

---

## String Translations

### Home

| Key | pt-BR |
|---|---|
| SELECT_CITY | "Selecionar cidade" |
| TITLE | "O que você\nprecisa hoje?" |
| SEARCH_PLACEHOLDER | "Pintor, eletricista, faxineira, encanador..." |
| SEARCH | "Buscar" |
| OFFER_SERVICES | "Ofereça seus serviços" |
| OFFER_SERVICES_DESCRIPTION | "Descreva o que você faz — nós montamos seu perfil." |
| DISMISS | "Dispensar" |

### Auth

| Key | pt-BR |
|---|---|
| WELCOME_TITLE | "Boas-vindas ao Quem Faz" |
| WELCOME_SUBTITLE | "Encontre profissionais ou ofereça seus serviços." |
| PHONE_LABEL | "Número de celular" |
| PHONE_PLACEHOLDER | "(11) 99999-9999" |
| CONTINUE | "Continuar" |
| OTP_TITLE | "Código de verificação" |
| OTP_SUBTITLE | "Digite o código enviado para %s" |
| OTP_CONFIRM | "Confirmar" |
| NAME_TITLE | "Qual é o seu nome?" |
| NAME_LABEL | "Nome completo" |
| PHOTO_PROMPT | "Adicione uma foto para que possam reconhecer você." |
| PHOTO_CHOOSE | "Escolher foto" |
| PHOTO_SKIP | "Pular por enquanto" |
| KNOWN_NAME_TITLE | "Você tem um nome conhecido?" |
| KNOWN_NAME_SUBTITLE | "Se as pessoas conhecem você por um apelido ou nome profissional, coloque aqui." |
| KNOWN_NAME_LABEL | "Nome conhecido (opcional)" |
| KNOWN_NAME_PLACEHOLDER | "Ex.: Joãozinho da Tinta" |
| SKIP | "Pular" |

### Onboarding

| Key | pt-BR |
|---|---|
| STEP_INDICATOR | "Passo %d de 5" |
| BECOME_PROFESSIONAL | "Torne-se profissional" |
| DESCRIBE_SERVICES | "Descreva seus serviços com suas próprias palavras. Nós ajudamos a organizar." |
| DESCRIPTION_PLACEHOLDER | "Ex.: Sou pintor residencial com 10 anos de experiência. Trabalho em Batatais e Ribeirão Preto. Também faço pequenos reparos em paredes." |
| ANALYZE_SERVICES | "Analisar meus serviços" |
| LOADING_INTERPRETING | "Interpretando sua descrição..." |
| LOADING_ANALYZING | "Analisando seus serviços..." |
| LOADING_ORGANIZING | "Organizando seu perfil..." |
| LOADING_ALMOST_READY | "Quase pronto..." |
| MORE_INFO_TITLE | "Precisamos de mais algumas informações" |
| MORE_INFO_SUBTITLE | "Responda as perguntas abaixo para entendermos melhor seus serviços." |
| YOUR_ANSWER | "Sua resposta" |
| SUBMIT_ANSWERS | "Enviar respostas" |
| SKIP_AND_CONTINUE | "Pular e continuar" |
| SELECT_SERVICES_TITLE | "Selecione seus serviços" |
| SELECT_SERVICES_SUBTITLE | "Não conseguimos identificar seus serviços automaticamente. Selecione abaixo os serviços que você oferece." |
| REVIEW_TITLE | "Revise seus serviços" |
| REVIEW_SUBTITLE | "Estes são os serviços que identificamos." |
| INTERPRETED_SERVICES | "Serviços identificados:" |
| YOUR_CITY | "Sua cidade:" |
| CITY_LABEL | "Cidade" |
| CITY_PLACEHOLDER | "Selecione uma cidade" |
| LOOKS_GOOD | "Tudo certo, continuar" |
| PROFILE_DESCRIPTION_TITLE | "Descrição do perfil" |
| PROFILE_DESCRIPTION_SUBTITLE | "Esta é a descrição que as pessoas verão no seu perfil. Você pode editá-la se quiser." |
| PROFILE_DESCRIPTION_LABEL | "Descrição" |
| PROFILE_PUBLISHED | "Perfil publicado!" |
| PROFILE_PUBLISHED_SUBTITLE | "Você já pode ser encontrado(a) por quem precisa dos seus serviços." |
| VIEW_MY_PROFILE | "Ver meu perfil" |
| ERROR_TITLE | "Algo deu errado" |
| RETRY | "Tentar novamente" |

### Profile

| Key | pt-BR |
|---|---|
| DISABLE_DIALOG_TITLE | "Desativar perfil profissional" |
| DISABLE_DIALOG_MESSAGE | "Seu perfil profissional será desativado e não aparecerá mais nos resultados de busca. Sua conta continua ativa. Você pode reativá-lo a qualquer momento adicionando serviços novamente." |
| DISABLE_BUTTON | "Desativar" |
| CANCEL | "Cancelar" |
| PORTFOLIO_PHOTO_DESCRIPTION | "Foto do portfólio %d de %d" |
| SERVICES | "Serviços" |
| DISABLE_PROFILE | "Desativar perfil profissional" |
| REPORT_PROFILE | "Denunciar perfil" |
| EDIT_PROFILE | "Editar perfil" |
| WHATSAPP | "WhatsApp" |
| CALL | "Ligar" |
| REPORT_DIALOG_TITLE | "Denunciar perfil" |
| REPORT_SELECT_REASON | "Selecione o motivo:" |
| REPORT_SPAM | "Spam" |
| REPORT_INAPPROPRIATE | "Conteúdo impróprio" |
| REPORT_WRONG_PHONE | "Número de telefone incorreto" |
| REPORT_FAKE | "Perfil falso" |
| REPORT_ABUSIVE | "Comportamento abusivo" |
| REPORT_OTHER | "Outro" |
| REPORT_BUTTON | "Denunciar" |

### Edit Profile

| Key | pt-BR |
|---|---|
| TITLE | "Perfil profissional" |
| BACK | "Voltar" |
| NO_PROFILE | "Você ainda não tem um perfil profissional." |
| SETUP_PROFILE | "Criar perfil profissional" |
| GO_BACK | "Voltar" |
| SERVICES | "Serviços" |
| REMOVE | "Remover" |
| ALL_SERVICES_REMOVED | "Você removeu todos os serviços. Seu perfil profissional será desativado. Você pode reativá-lo a qualquer momento adicionando serviços novamente." |
| ADD_SERVICE | "Adicionar serviço" |
| ADD_SERVICES_DIALOG | "Adicionar serviços" |
| ADD | "Adicionar" |
| CANCEL | "Cancelar" |
| DESCRIPTION | "Descrição" |
| CITY | "Cidade" |
| CONTACT_PHONE | "Telefone de contato" |
| WHATSAPP_PHONE | "Telefone do WhatsApp" |
| SAVE_SUCCESS | "Perfil salvo com sucesso." |
| SAVE | "Salvar" |

### My Profile

| Key | pt-BR |
|---|---|
| ERROR_LOADING | "Erro ao carregar perfil" |
| RETRY | "Tentar novamente" |
| TITLE | "Meu perfil" |
| ADD_PHOTO_TITLE | "Adicione uma foto de perfil" |
| ADD_PHOTO_SUBTITLE | "Profissionais com foto recebem mais contatos." |
| ADD_PHOTO_BUTTON | "Adicionar foto" |
| FIRST_NAME | "Nome" |
| LAST_NAME | "Sobrenome" |
| SAVE_NAME | "Salvar nome" |
| CHANGE_PHOTO | "Trocar foto" |
| MY_FAVORITES | "Meus favoritos" |
| CHANGE_CITY | "Trocar cidade" |
| PROFESSIONAL_PROFILE | "Perfil profissional" |
| LOGOUT | "Sair" |

### Favorites

| Key | pt-BR |
|---|---|
| TITLE | "Favoritos" |
| EMPTY_TITLE | "Nenhum favorito ainda" |
| EMPTY_SUBTITLE | "Profissionais que você salvar aparecem aqui." |
| FIND_PROFESSIONALS | "Buscar profissionais" |

### Search

| Key | pt-BR |
|---|---|
| SHOWING_RESULTS | "Resultados para: %s" |
| NO_RESULTS_TITLE | "Nenhum profissional encontrado" |
| NO_RESULTS_SUBTITLE | "Tente buscar por outro serviço ou cidade." |
| BROWSE_BY_CATEGORY | "Ou navegue por categoria" |
| LOAD_MORE | "Carregar mais" |
| REMOVE_FAVORITE | "Remover dos favoritos" |
| ADD_FAVORITE | "Adicionar aos favoritos" |

### Blocked User

| Key | pt-BR |
|---|---|
| TITLE | "Conta bloqueada" |
| MESSAGE | "Sua conta foi bloqueada temporariamente. Se você acredita que isso foi um engano, entre em contato com nosso suporte." |
| CONTACT_SUPPORT | "Falar com o suporte" |

### City Selection

| Key | pt-BR |
|---|---|
| TITLE | "Selecione sua cidade" |

### Status Chip (Common Component)

| Key | pt-BR |
|---|---|
| ACTIVE_TODAY | "Ativo(a) hoje" |
| ACTIVE_YESTERDAY | "Ativo(a) ontem" |
| ACTIVE_DAYS_AGO | "Ativo(a) há %d dias" |
| ACTIVE_THIS_MONTH | "Ativo(a) este mês" |
| ACTIVE_RECENTLY | "Ativo(a) recentemente" |
| COMPLETE_PROFILE | "Completar perfil" |

### Common

| Key | pt-BR |
|---|---|
| RETRY | "Tentar novamente" |
| CANCEL | "Cancelar" |
| CONTINUE | "Continuar" |
| SAVE | "Salvar" |
| BACK | "Voltar" |

---

## Implementation Notes

1. **`Strings.kt` location:** `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/ui/strings/Strings.kt`
2. **String formatting:** Use `String.format()` or Kotlin string templates for dynamic values (e.g., `STEP_INDICATOR.format(currentStep)`)
3. **Report reason display names:** The `toDisplayName()` extension in `shared/` should reference `Strings.Profile` constants, or the mapping should move to `composeApp/` to keep `shared/` language-agnostic
4. **Already-Portuguese strings:** Some screens already have Portuguese strings — these will be normalized to use `Strings.*` references like everything else
5. **Content descriptions:** Accessibility strings are also translated for screen reader users
6. **Server messages:** Remain in English — the client handles all user-facing error presentation
