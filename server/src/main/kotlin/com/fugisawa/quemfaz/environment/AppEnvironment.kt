package com.fugisawa.quemfaz.environment

enum class AppEnvironment(
    val value: String,
) {
    LOCAL("local"),
    DEV("dev"),
    SANDBOX("sandbox"),
    PRODUCTION("production"),
    ;

    companion object {
        fun fromString(value: String?): AppEnvironment = entries.find { it.value.equals(value, ignoreCase = true) } ?: LOCAL
    }
}
