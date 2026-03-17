package com.fugisawa.quemfaz.catalog.application

import com.fugisawa.quemfaz.catalog.domain.SignalRepository
import com.fugisawa.quemfaz.catalog.domain.UnmatchedServiceSignal
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class SignalCaptureService(
    private val signalRepository: SignalRepository,
    private val catalogService: CatalogService,
    private val provisionalServiceCreator: ProvisionalServiceCreator,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun captureSignal(
        rawDescription: String,
        source: String,
        userId: String?,
        cityName: String?,
        safetyClassification: String?,
        safetyReason: String?,
        forceProvision: Boolean = false,
    ): String? {
        return try {
            val provisionalId = provisionalServiceCreator.tryProvision(
                rawDescription, source, userId, cityName, safetyClassification, safetyReason,
                forceProvision = forceProvision,
            )
            val bestMatch = catalogService.search(rawDescription).firstOrNull()
            signalRepository.create(
                UnmatchedServiceSignal(
                    id = UUID.randomUUID().toString(),
                    rawDescription = rawDescription,
                    source = source,
                    userId = userId,
                    bestMatchServiceId = bestMatch?.id,
                    bestMatchConfidence = if (bestMatch != null) "low" else "none",
                    provisionalServiceId = provisionalId,
                    cityName = cityName,
                    safetyClassification = safetyClassification,
                    safetyReason = safetyReason,
                    createdAt = Instant.now(),
                ),
            )
            provisionalId
        } catch (e: Exception) {
            logger.error("Failed to capture unmatched service signal: {}", e.message)
            null
        }
    }
}
