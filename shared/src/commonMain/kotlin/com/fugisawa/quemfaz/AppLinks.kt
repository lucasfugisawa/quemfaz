package com.fugisawa.quemfaz

/**
 * Centralized legal/support URLs and contact information.
 *
 * Update these values once the documents are hosted at their final public URLs.
 * All screens that reference legal pages or support contacts read from this object.
 */
object AppLinks {
    /** Public URL for the Terms of Use document. */
    const val TERMS_OF_USE_URL = "https://quemfaz.com.br/termos"

    /** Public URL for the Privacy Policy document. */
    const val PRIVACY_POLICY_URL = "https://quemfaz.com.br/privacidade"

    /** Public URL for the Community Guidelines document. */
    const val COMMUNITY_GUIDELINES_URL = "https://quemfaz.com.br/diretrizes"

    /** Support e-mail address shown throughout the app. */
    const val SUPPORT_EMAIL = "suporte@quemfaz.com.br"

    /** Current version identifier for the Terms of Use (bump when terms change). */
    const val TERMS_VERSION = "1.0.0"

    /** Current version identifier for the Privacy Policy (bump when policy changes). */
    const val PRIVACY_POLICY_VERSION = "1.0.0"
}
