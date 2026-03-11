package com.fugisawa.quemfaz.moderation.routing

import com.fugisawa.quemfaz.config.AppConfig
import com.fugisawa.quemfaz.contract.moderation.CreateReportRequest
import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.ReportId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.moderation.application.CreateProfileReportService
import com.fugisawa.quemfaz.moderation.application.ModerationService
import com.fugisawa.quemfaz.moderation.domain.ReportStatus
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.moderationRoutes() {
    val createProfileReportService by inject<CreateProfileReportService>()
    val moderationService by inject<ModerationService>()
    val appConfig by inject<AppConfig>()

    authenticate("auth-jwt") {
        route("/reports") {
            post("/professional-profile") {
                val userId =
                    call
                        .principal<JWTPrincipal>()
                        ?.payload
                        ?.getClaim("userId")
                        ?.asString()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val request = call.receive<CreateReportRequest>()
                try {
                    val report = createProfileReportService.execute(UserId(userId), request)
                    call.respond(HttpStatusCode.Created, mapOf("id" to report.id.value))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                }
            }
        }

        route("/admin") {
            val adminCheck = createRouteScopedPlugin("AdminCheck") {
                onCall { call ->
                    val userId =
                        call
                            .principal<JWTPrincipal>()
                            ?.payload
                            ?.getClaim("userId")
                            ?.asString()
                    if (userId == null || !appConfig.admin.adminUserIds.contains(userId)) {
                        call.respond(HttpStatusCode.Forbidden, "Admin access required")
                    }
                }
            }
            install(adminCheck)

            route("/reports") {
                get {
                    val status = call.parameters["status"]?.let { ReportStatus.valueOf(it.uppercase()) }
                    val reports = moderationService.listReports(status)
                    call.respond(reports)
                }

                get("/{reportId}") {
                    val reportId = call.parameters["reportId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val report =
                        moderationService.getReportDetails(ReportId(reportId))
                            ?: return@get call.respond(HttpStatusCode.NotFound)
                    call.respond(report)
                }

                post("/{reportId}/dismiss") {
                    val reportId = call.parameters["reportId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                    moderationService.dismissReport(ReportId(reportId))
                    call.respond(HttpStatusCode.OK)
                }
            }

            route("/professional-profiles") {
                post("/{profileId}/block") {
                    val profileId = call.parameters["profileId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                    val reportId = call.parameters["reportId"]?.let { ReportId(it) }
                    moderationService.blockProfessionalProfile(ProfessionalProfileId(profileId), reportId)
                    call.respond(HttpStatusCode.OK)
                }
            }

            route("/users") {
                post("/{userId}/block") {
                    val userId = call.parameters["userId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                    val reportId = call.parameters["reportId"]?.let { ReportId(it) }
                    moderationService.blockUser(UserId(userId), reportId)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}
