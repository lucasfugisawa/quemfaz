package com.fugisawa.quemfaz.infrastructure.sms

import org.slf4j.LoggerFactory

class FakeSmsSender : SmsSender {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun sendSms(phoneNumber: String, message: String) {
        logger.info("--------------------------------------------------")
        logger.info("[FAKE SMS] To: $phoneNumber")
        logger.info("[FAKE SMS] Content: $message")
        logger.info("--------------------------------------------------")
    }
}
