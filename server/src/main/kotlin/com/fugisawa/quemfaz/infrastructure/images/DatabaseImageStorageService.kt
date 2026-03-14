package com.fugisawa.quemfaz.infrastructure.images

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.util.UUID

object StoredImagesTable : Table("stored_images") {
    val id          = varchar("id", 128)
    val data        = blob("data")
    val contentType = varchar("content_type", 128)
    val createdAt   = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

class DatabaseImageStorageService : ImageStorageService {
    override suspend fun store(data: ByteArray, contentType: String): String =
        newSuspendedTransaction {
            val id = UUID.randomUUID().toString()
            StoredImagesTable.insert {
                it[StoredImagesTable.id]          = id
                it[StoredImagesTable.data]        = org.jetbrains.exposed.sql.statements.api.ExposedBlob(data)
                it[StoredImagesTable.contentType] = contentType
                it[StoredImagesTable.createdAt]   = Instant.now()
            }
            "/api/images/$id"
        }

    override suspend fun retrieve(id: String): StoredImage? =
        newSuspendedTransaction {
            StoredImagesTable
                .selectAll()
                .where { StoredImagesTable.id eq id }
                .map { StoredImage(it[StoredImagesTable.data].bytes, it[StoredImagesTable.contentType]) }
                .singleOrNull()
        }
}
