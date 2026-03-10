package com.fugisawa.quemfaz.search.canonical

import com.fugisawa.quemfaz.core.id.CanonicalServiceId
import com.fugisawa.quemfaz.domain.service.CanonicalService
import com.fugisawa.quemfaz.domain.service.CanonicalServices

object CanonicalServiceLookup {
    fun getAll(): List<CanonicalService> = CanonicalServices.all

    fun findById(id: CanonicalServiceId): CanonicalService? = CanonicalServices.findById(id)

    fun findByDisplayName(name: String): CanonicalService? = CanonicalServices.findByDisplayName(name)

    fun findByAlias(alias: String): List<CanonicalService> {
        return CanonicalServices.all.filter { service ->
            service.baseAliases.any { it.equals(alias, ignoreCase = true) }
        }
    }
}
