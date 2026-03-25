package com.fugisawa.quemfaz.catalog.routing

import com.fugisawa.quemfaz.catalog.application.CatalogService
import com.fugisawa.quemfaz.contract.catalog.CatalogResponse
import com.fugisawa.quemfaz.contract.catalog.CatalogServiceDto
import com.fugisawa.quemfaz.contract.catalog.ServiceCategoryDto
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
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

            val categories =
                catalogService.getCategories().map {
                    ServiceCategoryDto(it.id, it.displayName, it.sortOrder)
                }
            val services =
                catalogService.getActiveServices().map {
                    CatalogServiceDto(it.id, it.displayName, it.description, it.categoryId, it.aliases)
                }

            call.response.header(HttpHeaders.ETag, currentVersion)
            call.respond(CatalogResponse(currentVersion, categories, services))
        }
    }
}
