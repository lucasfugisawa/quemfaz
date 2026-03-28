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
        const val WHAT_LOOKING_FOR = "O que você está\nprocurando?"
        const val SEARCH_HINT = "Conte o que você precisa..."
        const val SPEAK_OR_TYPE = "Fale ou digite naturalmente"
        const val SEARCH_ACTION = "Buscar"
        const val EXAMPLE_HEADER = "Você pode digitar ou falar assim:"
        const val RECENT_SEARCHES = "Suas últimas buscas:"
        const val VIEW_ALL_CATEGORIES = "Ver todas as categorias"
        const val OFFER_SERVICES_SUBTITLE = "Conta pra gente o que você faz e montaremos o seu perfil automaticamente."

        val EXAMPLE_PHRASES = listOf(
            "Minha pia tá vazando, preciso de ajuda",
            "Tô precisando de um eletricista urgente",
            "Alguém faz faxina pesada aqui na região?",
            "Quero pintar a casa toda, preciso de um pintor",
            "Preciso de alguém pra cortar a grama",
            "Meu chuveiro queimou, quem troca?",
            "Comprei uns móveis e preciso montar",
            "Pia da cozinha entupiu, alguém resolve?",
            "Preciso de um encanador pra hoje se possível",
            "Tô procurando alguém pra limpar meu sofá",
            "Quem instala ar-condicionado por aqui?",
            "Preciso de pintor e eletricista pro meu apê",
            "Meu portão não tá abrindo direito",
            "Alguém faz pequenos reparos aqui perto?",
            "Quero deixar meu quintal bonito, quem ajuda?",
            "A parede do quarto tá com infiltração",
            "Preciso de um fotógrafo pra festa de aniversário",
            "Alguém faz frete de geladeira aqui na cidade?",
            "Tô procurando uma manicure que atenda em casa",
            "Preciso de alguém pra cuidar do meu cachorro no fim de semana",
            "Meu computador tá muito lento, quem arruma?",
            "Quero colocar câmeras de segurança em casa",
            "Preciso de aulas particulares de matemática pro meu filho",
            "Vou mudar de casa e preciso de ajuda com a mudança",
        )
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

        const val BIRTH_DATE_TITLE = "Data de nascimento"
        const val BIRTH_DATE_SUBTITLE = "Para oferecer serviços, você precisa ter pelo menos 18 anos."
        const val BIRTH_DATE_LABEL = "Data de nascimento"
        const val BIRTH_DATE_PLACEHOLDER = "DD/MM/AAAA"
        const val BIRTH_DATE_INVALID = "Data inválida"
        const val BIRTH_DATE_UNDERAGE = "Você precisa ter pelo menos 18 anos para oferecer serviços."
        const val BIRTH_DATE_FORMAT_HINT = "Use o formato DD/MM/AAAA"
        const val DESCRIPTION_EXAMPLE = "Ex: \"Sou pintor residencial com 10 anos de experiência\""
        const val ADD_MORE_SERVICES = "Adicionar mais serviços"
        const val FULL_NAME_REQUIRED = "O nome completo é obrigatório para profissionais."
        const val FULL_NAME_LABEL = "Nome completo"
        const val SELECT_DATE = "Selecionar"
        const val ADDITIONAL_SERVICES = "Serviços adicionais:"

        const val STEP_1_MESSAGE = "Vamos começar! 🚀\nMe conte: o que você faz?"
        const val SPEAK_WHAT_I_DO = "Falar o que faço"
        const val SPEAK_NATURALLY_HINT = "Fale naturalmente como você se apresentaria para um cliente ou vizinho"
        const val STEP_2_MESSAGE = "Entendi! ✨\nBaseado no que você disse, você oferece:"
        const val THATS_CORRECT = "Está correto!"
        const val LET_ME_EXPLAIN = "Deixe-me explicar melhor"
        const val ADD_SERVICE = "Adicionar"
        const val STEP_3_MESSAGE = "Seu perfil ficará assim:"
        const val PUBLISH_PROFILE = "Publicar perfil"
        const val EDIT_DESCRIPTION = "Editar descrição"

        fun stepIndicator(current: Int) = "Passo ${current + 1} de 3"
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
        const val ABOUT = "Sobre"
        const val OFFERED_SERVICES = "Serviços oferecidos"

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
        const val CATALOG_UNAVAILABLE = "Não foi possível carregar o catálogo. Tente novamente."
        const val DESCRIPTION = "Descrição"
        const val CITY = "Cidade"
        const val PHONE = "Telefone"
        const val SAVE_SUCCESS = "Perfil salvo com sucesso."
    }

    object MyProfile {
        const val ERROR_LOADING = "Erro ao carregar perfil"
        const val TITLE = "Meu perfil"
        const val ADD_PHOTO_TITLE = "Adicione uma foto de perfil"
        const val ADD_PHOTO_SUBTITLE = "Profissionais com foto recebem mais contatos."
        const val ADD_PHOTO_BUTTON = "Adicionar foto"
        const val FULL_NAME = "Nome completo"
        const val SAVE_NAME = "Salvar nome"
        const val CHANGE_PHOTO = "Trocar foto"
        const val MY_FAVORITES = "Meus favoritos"
        const val CHANGE_CITY = "Trocar cidade"
        const val PROFESSIONAL_PROFILE = "Perfil profissional"
        const val TERMS_OF_USE = "Termos de Uso"
        const val PRIVACY_POLICY = "Política de Privacidade"
        const val COMMUNITY_GUIDELINES = "Diretrizes da Comunidade"
        const val DELETE_ACCOUNT = "Excluir conta"
        const val DELETE_ACCOUNT_DIALOG_TITLE = "Excluir conta"
        const val DELETE_ACCOUNT_DIALOG_MESSAGE = "Para solicitar a exclusão da sua conta e de todos os seus dados pessoais, envie um e-mail para nosso suporte. Responderemos em até 15 dias."
        const val DELETE_ACCOUNT_DIALOG_CONFIRM = "Enviar e-mail"
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

    object Categories {
        const val TITLE = "Categorias"
        const val ERROR_LOADING = "Erro ao carregar categorias"
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

        fun activeDaysAgo(days: Int) = "Ativo(a) há $days dias"
    }

    object Legal {
        const val TERMS_TITLE = "Termos e Privacidade"
        const val TERMS_ACCEPT_PREFIX = "Li e concordo com os "
        const val TERMS_LINK = "Termos de Uso"
        const val TERMS_AND = " e a "
        const val PRIVACY_LINK = "Política de Privacidade"
        const val TERMS_ACCEPT_SUFFIX = "."
        const val ACCEPT_AND_CONTINUE = "Aceitar e continuar"
        const val ONBOARDING_TERMS_PREFIX = "Ao publicar, você concorda com os "
        const val ONBOARDING_TERMS_SUFFIX = " do QuemFaz."
        const val TERMS_UPDATED_TITLE = "Termos atualizados"
        const val TERMS_UPDATED_MESSAGE = "Nossos Termos de Uso e/ou Política de Privacidade foram atualizados. Para continuar usando o QuemFaz, é necessário aceitar os novos termos."
        const val TERMS_UPDATED_ACCEPT = "Aceitar"
        const val TERMS_UPDATED_REJECT = "Recusar"
        const val TERMS_REJECT_CONFIRM_TITLE = "Tem certeza?"
        const val TERMS_REJECT_CONFIRM_MESSAGE = "Sem aceitar os termos atualizados, não é possível continuar usando o QuemFaz. Você será desconectado(a) da sua conta."
        const val TERMS_REJECT_CONFIRM_LOGOUT = "Sair da conta"
        const val TERMS_REJECT_CONFIRM_BACK = "Voltar"
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
        const val FAILED_SAVE_DATE_OF_BIRTH = "Não foi possível salvar a data de nascimento"
        const val SEARCH_FAILED = "A busca falhou"
        const val SERVICE_NOT_ALLOWED = "O serviço descrito não é permitido na plataforma. Por favor, descreva um serviço diferente."
        const val CHANGE_DESCRIPTION = "Alterar descrição"
    }
}
