package com.fugisawa.quemfaz.infrastructure.sms

interface SmsSender {
    suspend fun sendSms(phoneNumber: String, message: String)
}
