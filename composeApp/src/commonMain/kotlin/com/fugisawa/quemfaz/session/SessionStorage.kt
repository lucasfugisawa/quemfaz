package com.fugisawa.quemfaz.session

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

interface SessionStorage {
    fun saveToken(token: String?)
    fun getToken(): String?
    fun saveRefreshToken(token: String?)
    fun getRefreshToken(): String?
    fun clear()
}

class SettingsSessionStorage(private val settings: Settings) : SessionStorage {
    private companion object {
        const val KEY_TOKEN = "auth_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
    }

    override fun saveToken(token: String?) {
        if (token == null) {
            settings.remove(KEY_TOKEN)
        } else {
            settings[KEY_TOKEN] = token
        }
    }

    override fun getToken(): String? = settings.getStringOrNull(KEY_TOKEN)

    override fun saveRefreshToken(token: String?) {
        if (token == null) {
            settings.remove(KEY_REFRESH_TOKEN)
        } else {
            settings[KEY_REFRESH_TOKEN] = token
        }
    }

    override fun getRefreshToken(): String? = settings.getStringOrNull(KEY_REFRESH_TOKEN)

    override fun clear() {
        settings.remove(KEY_TOKEN)
        settings.remove(KEY_REFRESH_TOKEN)
    }
}
