package com.fugisawa.quemfaz.catalog.routing

import com.fugisawa.quemfaz.catalog.application.AdminCatalogService
import com.fugisawa.quemfaz.config.AppConfig
import com.fugisawa.quemfaz.contract.catalog.PendingServiceResponse
import com.fugisawa.quemfaz.contract.catalog.ReviewServiceRequest
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.adminCatalogRoutes() {
    val adminCatalogService by inject<AdminCatalogService>()
    val appConfig by inject<AppConfig>()

    authenticate("auth-jwt") {
        route("/admin/catalog") {
            get("/pending") {
                val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
                if (userId !in appConfig.admin.adminUserIds) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin access required"))
                    return@get
                }
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val pending = adminCatalogService.listPendingServices(limit = limit, offset = offset)
                call.respond(pending.map { view ->
                    PendingServiceResponse(
                        id = view.service.id,
                        displayName = view.service.displayName,
                        description = view.service.description,
                        categoryId = view.service.categoryId,
                        aliases = view.service.aliases,
                        signalCount = view.signalCount,
                        sources = view.sources,
                        cities = view.cities,
                        createdAt = view.service.createdAt.toString(),
                    )
                })
            }

            post("/{serviceId}/approve") {
                val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
                if (userId !in appConfig.admin.adminUserIds) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin access required"))
                    return@post
                }
                val serviceId = call.parameters["serviceId"]!!
                try {
                    adminCatalogService.approveService(serviceId, userId)
                    call.respond(HttpStatusCode.OK, mapOf("status" to "approved"))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                }
            }

            post("/{serviceId}/reject") {
                val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
                if (userId !in appConfig.admin.adminUserIds) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin access required"))
                    return@post
                }
                val serviceId = call.parameters["serviceId"]!!
                val request = call.receive<ReviewServiceRequest>()
                try {
                    adminCatalogService.rejectService(serviceId, request.reason, userId)
                    call.respond(HttpStatusCode.OK, mapOf("status" to "rejected"))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                }
            }

            post("/{serviceId}/merge") {
                val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
                if (userId !in appConfig.admin.adminUserIds) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin access required"))
                    return@post
                }
                val serviceId = call.parameters["serviceId"]!!
                val request = call.receive<ReviewServiceRequest>()
                val mergeIntoId = request.mergeIntoServiceId
                    ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "mergeIntoServiceId required"))
                try {
                    adminCatalogService.mergeService(serviceId, mergeIntoId, request.reason, userId)
                    call.respond(HttpStatusCode.OK, mapOf("status" to "merged"))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                }
            }
        }
    }
}
