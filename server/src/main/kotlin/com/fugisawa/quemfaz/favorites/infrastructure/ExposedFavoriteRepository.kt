package com.fugisawa.quemfaz.favorites.infrastructure

import com.fugisawa.quemfaz.core.id.FavoriteId
import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.favorites.domain.Favorite
import com.fugisawa.quemfaz.favorites.domain.FavoriteRepository
import com.fugisawa.quemfaz.profile.infrastructure.persistence.ProfessionalProfilesTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object FavoritesTable : Table("favorites") {
    val id = varchar("id", 128)
    val userId = varchar("user_id", 128) references UsersTable.id
    val professionalProfileId = varchar("professional_profile_id", 128) references ProfessionalProfilesTable.id
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

private object UsersTable : Table("users") {
    val id = varchar("id", 128)
    override val primaryKey = PrimaryKey(id)
}

class ExposedFavoriteRepository : FavoriteRepository {
    override fun add(favorite: Favorite) =
        transaction {
            FavoritesTable.insert {
                it[id] = favorite.id.value
                it[userId] = favorite.userId.value
                it[professionalProfileId] = favorite.professionalProfileId.value
                it[createdAt] = favorite.createdAt
            }
            Unit
        }

    override fun remove(
        userId: UserId,
        professionalProfileId: ProfessionalProfileId,
    ) = transaction {
        FavoritesTable.deleteWhere {
            (FavoritesTable.userId eq userId.value) and (FavoritesTable.professionalProfileId eq professionalProfileId.value)
        }
        Unit
    }

    override fun exists(
        userId: UserId,
        professionalProfileId: ProfessionalProfileId,
    ): Boolean =
        transaction {
            FavoritesTable
                .selectAll()
                .where {
                    (FavoritesTable.userId eq userId.value) and (FavoritesTable.professionalProfileId eq professionalProfileId.value)
                }.any()
        }

    override fun listByUserId(userId: UserId): List<Favorite> =
        transaction {
            FavoritesTable
                .selectAll()
                .where { FavoritesTable.userId eq userId.value }
                .map {
                    Favorite(
                        id = FavoriteId(it[FavoritesTable.id]),
                        userId = UserId(it[FavoritesTable.id]),
                        professionalProfileId = ProfessionalProfileId(it[FavoritesTable.professionalProfileId]),
                        createdAt = it[FavoritesTable.createdAt],
                    )
                }
        }
}
