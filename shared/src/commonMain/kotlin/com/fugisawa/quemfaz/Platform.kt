package com.fugisawa.quemfaz

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform