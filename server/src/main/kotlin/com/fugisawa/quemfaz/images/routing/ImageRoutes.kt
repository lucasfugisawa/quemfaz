package com.fugisawa.quemfaz.images.routing

import com.fugisawa.quemfaz.contract.image.UploadImageResponse
import com.fugisawa.quemfaz.infrastructure.images.ImageStorageService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.readAllParts
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import org.koin.ktor.ext.inject

private val ALLOWED_CONTENT_TYPES = setOf("image/jpeg", "image/png", "image/webp")
private const val MAX_BYTES = 5 * 1024 * 1024  // 5 MB

fun Route.imageRoutes() {
    val imageStorageService by inject<ImageStorageService>()

    route("/api/images") {
        authenticate("auth-jwt") {
            post("/upload") {
                val multipart = call.receiveMultipart()
                val parts = multipart.readAllParts()
                val filePart = parts.filterIsInstance<PartData.FileItem>().firstOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Missing image part"))

                val contentType = filePart.contentType?.toString()
                if (contentType !in ALLOWED_CONTENT_TYPES) {
                    filePart.dispose()
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("message" to "Unsupported image type. Allowed: image/jpeg, image/png, image/webp"),
                    )
                }

                val bytes = filePart.provider().readRemaining().readByteArray()
                filePart.dispose()

                if (bytes.size > MAX_BYTES) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("message" to "Image exceeds maximum size of 5 MB"),
                    )
                }

                val url = imageStorageService.store(bytes, contentType!!)
                call.respond(UploadImageResponse(url = url))
            }
        }

        get("/{id}") {
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest)

            val image = imageStorageService.retrieve(id)
                ?: return@get call.respond(HttpStatusCode.NotFound)

            call.respondBytes(
                bytes = image.data,
                contentType = ContentType.parse(image.contentType),
            )
        }
    }
}
