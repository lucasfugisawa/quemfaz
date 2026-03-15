package com.fugisawa.quemfaz.catalog.routing

import com.fugisawa.quemfaz.catalog.application.CatalogService
import com.fugisawa.quemfaz.contract.catalog.CatalogResponse
import com.fugisawa.quemfaz.contract.catalog.CatalogServiceDto
import com.fugisawa.quemfaz.contract.catalog.ServiceCategoryDto
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.catalogRoutes() {
    val catalogService by inject<CatalogService>()

    route("/services") {
        get("/catalog") {
            val clientVersion = call.request.headers["If-None-Match"]
            val currentVersion = catalogService.getCatalogVersion()

            if (clientVersion == currentVersion) {
                call.respond(HttpStatusCode.NotModified)
                return@get
            }

            val categories = catalogService.getCategories().map {
                ServiceCategoryDto(it.id, it.displayName, it.sortOrder)
            }
            val services = catalogService.getActiveServices().map {
                CatalogServiceDto(it.id, it.displayName, it.description, it.categoryId, it.aliases)
            }

            call.response.header(HttpHeaders.ETag, currentVersion)
            call.respond(CatalogResponse(currentVersion, categories, services))
        }
    }
}
