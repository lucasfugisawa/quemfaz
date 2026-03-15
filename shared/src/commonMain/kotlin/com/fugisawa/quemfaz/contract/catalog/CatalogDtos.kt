package com.fugisawa.quemfaz.contract.catalog

import kotlinx.serialization.Serializable

@Serializable
data class CatalogResponse(
    val version: String,
    val categories: List<ServiceCategoryDto>,
    val services: List<CatalogServiceDto>,
)

@Serializable
data class ServiceCategoryDto(
    val id: String,
    val displayName: String,
    val sortOrder: Int,
)

@Serializable
data class CatalogServiceDto(
    val id: String,
    val displayName: String,
    val description: String,
    val categoryId: String,
    val aliases: List<String>,
)

@Serializable
data class PendingServiceResponse(
    val id: String,
    val displayName: String,
    val description: String,
    val categoryId: String,
    val aliases: List<String>,
    val signalCount: Int,
    val sources: List<String>,
    val cities: List<String>,
    val createdAt: String,
)

@Serializable
data class ReviewServiceRequest(
    val reason: String = "",
    val mergeIntoServiceId: String? = null,
)
