package com.fugisawa.quemfaz.images.routing

import com.fugisawa.quemfaz.contract.image.UploadImageResponse
import com.fugisawa.quemfaz.infrastructure.images.ImageStorageService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
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
                var mimeError = false
                var sizeError = false
                var missingPart = true
                var uploadResult: Pair<ByteArray, String>? = null

                multipart.forEachPart { part ->
                    if (part is PartData.FileItem && uploadResult == null && !mimeError) {
                        missingPart = false
                        val contentType = part.contentType?.toString()
                        if (contentType !in ALLOWED_CONTENT_TYPES) {
                            mimeError = true
                            part.dispose()
                        } else {
                            val bytes = part.provider().readRemaining().readByteArray()
                            part.dispose()
                            if (bytes.size > MAX_BYTES) {
                                sizeError = true
                            } else {
                                uploadResult = bytes to contentType!!
                            }
                        }
                    } else {
                        part.dispose()
                    }
                }

                when {
                    missingPart -> return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("message" to "Missing image part"),
                    )
                    mimeError -> return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("message" to "Unsupported image type. Allowed: image/jpeg, image/png, image/webp"),
                    )
                    sizeError -> return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("message" to "Image exceeds maximum size of 5 MB"),
                    )
                    else -> {
                        val (bytes, contentType) = uploadResult!!
                        val url = imageStorageService.store(bytes, contentType)
                        call.respond(UploadImageResponse(url = url))
                    }
                }
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
