package com.fugisawa.quemfaz.profile.application

import com.fugisawa.quemfaz.contract.profile.SetKnownNameRequest
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileRepository

sealed class SetKnownNameResult {
    object Success : SetKnownNameResult()
    object NotFound : SetKnownNameResult()
}

class SetKnownNameService(
    private val profileRepository: ProfessionalProfileRepository,
) {
    fun execute(userId: UserId, request: SetKnownNameRequest): SetKnownNameResult {
        val existing = profileRepository.findByUserId(userId) ?: return SetKnownNameResult.NotFound
        profileRepository.updateKnownName(existing.id, request.knownName)
        return SetKnownNameResult.Success
    }
}
