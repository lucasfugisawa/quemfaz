package com.fugisawa.quemfaz.infrastructure.sms

import com.fugisawa.quemfaz.config.AwsSmsConfig
import org.slf4j.LoggerFactory

class AwsSmsSender(private val config: AwsSmsConfig) : SmsSender {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun sendSms(phoneNumber: String, message: String) {
        // TODO: Implement actual AWS SNS / Pinpoint integration
        logger.warn("AWS SMS Sender selected but not yet fully implemented. Region: ${config.region}")
        throw NotImplementedError("AWS SMS integration is not yet implemented.")
    }
}
