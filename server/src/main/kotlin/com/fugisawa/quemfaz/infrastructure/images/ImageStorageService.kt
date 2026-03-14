package com.fugisawa.quemfaz.infrastructure.images

interface ImageStorageService {
    suspend fun store(data: ByteArray, contentType: String): String  // returns URL like "/api/images/{id}"
    suspend fun retrieve(id: String): StoredImage?
}

data class StoredImage(
    val data: ByteArray,
    val contentType: String,
)
