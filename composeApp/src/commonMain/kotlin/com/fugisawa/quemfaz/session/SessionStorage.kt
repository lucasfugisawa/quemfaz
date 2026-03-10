package com.fugisawa.quemfaz.session

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

interface SessionStorage {
    fun saveToken(token: String?)
    fun getToken(): String?
    fun clear()
}

class SettingsSessionStorage(private val settings: Settings) : SessionStorage {
    private companion object {
        const val KEY_TOKEN = "auth_token"
    }

    override fun saveToken(token: String?) {
        if (token == null) {
            settings.remove(KEY_TOKEN)
        } else {
            settings[KEY_TOKEN] = token
        }
    }

    override fun getToken(): String? = settings.getStringOrNull(KEY_TOKEN)

    override fun clear() {
        settings.remove(KEY_TOKEN)
    }
}
