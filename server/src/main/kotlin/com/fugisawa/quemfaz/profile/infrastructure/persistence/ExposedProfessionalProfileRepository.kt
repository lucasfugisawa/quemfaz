package com.fugisawa.quemfaz.profile.infrastructure.persistence

import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.domain.service.ServiceMatchLevel
import com.fugisawa.quemfaz.profile.domain.PortfolioPhoto
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfile
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileRepository
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileService
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileStatus
import com.fugisawa.quemfaz.profile.domain.ProfileCompleteness
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

object ProfessionalProfilesTable : Table("professional_profiles") {
    val id = varchar("id", 128)
    val userId = varchar("user_id", 128)
    val knownName = varchar("known_name", 255).nullable()
    val description = text("description").nullable()
    val normalizedDescription = text("normalized_description").nullable()
    val cityId = varchar("city_id", 128).nullable()
    val completeness =
        customEnumeration(
            "completeness",
            "profile_completeness",
            { ProfileCompleteness.valueOf(it as String) },
            {
                val pgObject = org.postgresql.util.PGobject()
                pgObject.type = "profile_completeness"
                pgObject.value = it.name
                pgObject
            },
        )
    val status =
        customEnumeration(
            "status",
            "profile_status",
            { ProfessionalProfileStatus.valueOf(it as String) },
            {
                val pgObject = org.postgresql.util.PGobject()
                pgObject.type = "profile_status"
                pgObject.value = it.name
                pgObject
            },
        )
    val lastActiveAt = timestamp("last_active_at").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val viewCount = integer("view_count").default(0)
    val contactClickCount = integer("contact_click_count").default(0)

    override val primaryKey = PrimaryKey(id)
}

object ProfessionalProfileServicesTable : Table("professional_profile_services") {
    val professionalProfileId = varchar("professional_profile_id", 128) references ProfessionalProfilesTable.id
    val serviceId = varchar("service_id", 128)
    val matchLevel =
        customEnumeration(
            "match_level",
            "service_match_level",
            { ServiceMatchLevel.valueOf(it as String) },
            {
                val pgObject = org.postgresql.util.PGobject()
                pgObject.type = "service_match_level"
                pgObject.value = it.name
                pgObject
            },
        )

    override val primaryKey = PrimaryKey(professionalProfileId, serviceId)
}

object ProfessionalProfilePortfolioPhotosTable : Table("professional_profile_portfolio_photos") {
    val id = varchar("id", 128)
    val professionalProfileId = varchar("professional_profile_id", 128) references ProfessionalProfilesTable.id
    val photoUrl = varchar("photo_url", 1024)
    val caption = text("caption").nullable()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

class ExposedProfessionalProfileRepository : ProfessionalProfileRepository {
    override fun findByUserId(userId: UserId): ProfessionalProfile? =
        transaction {
            ProfessionalProfilesTable
                .selectAll()
                .where { ProfessionalProfilesTable.userId eq userId.value }
                .map { mapProfile(it) }
                .singleOrNull()
        }

    override fun findById(id: ProfessionalProfileId): ProfessionalProfile? =
        transaction {
            ProfessionalProfilesTable
                .selectAll()
                .where { ProfessionalProfilesTable.id eq id.value }
                .map { mapProfile(it) }
                .singleOrNull()
        }

    override fun save(profile: ProfessionalProfile): ProfessionalProfile =
        transaction {
            val exists = ProfessionalProfilesTable.selectAll().where { ProfessionalProfilesTable.id eq profile.id.value }.any()
            if (exists) {
                ProfessionalProfilesTable.update({ ProfessionalProfilesTable.id eq profile.id.value }) {
                    it[knownName] = profile.knownName
                    it[description] = profile.description
                    it[normalizedDescription] = profile.normalizedDescription
                    it[cityId] = profile.cityId
                    it[completeness] = profile.completeness
                    it[status] = profile.status
                    it[lastActiveAt] = profile.lastActiveAt
                    it[updatedAt] = profile.updatedAt
                }
            } else {
                ProfessionalProfilesTable.insert {
                    it[id] = profile.id.value
                    it[userId] = profile.userId.value
                    it[knownName] = profile.knownName
                    it[description] = profile.description
                    it[normalizedDescription] = profile.normalizedDescription
                    it[cityId] = profile.cityId
                    it[completeness] = profile.completeness
                    it[status] = profile.status
                    it[lastActiveAt] = profile.lastActiveAt
                    it[createdAt] = profile.createdAt
                    it[updatedAt] = profile.updatedAt
                }
            }

            ProfessionalProfileServicesTable.deleteWhere { professionalProfileId eq profile.id.value }
            profile.services.forEach { service ->
                ProfessionalProfileServicesTable.insert {
                    it[professionalProfileId] = profile.id.value
                    it[serviceId] = service.serviceId
                    it[matchLevel] = service.matchLevel
                }
            }

            ProfessionalProfilePortfolioPhotosTable.deleteWhere { professionalProfileId eq profile.id.value }
            profile.portfolioPhotos.forEach { photo ->
                ProfessionalProfilePortfolioPhotosTable.insert {
                    it[id] = photo.id
                    it[professionalProfileId] = profile.id.value
                    it[photoUrl] = photo.photoUrl
                    it[caption] = photo.caption
                    it[createdAt] = photo.createdAt
                }
            }

            profile
        }

    override fun listPublishedByCity(cityId: String): List<ProfessionalProfile> =
        transaction {
            ProfessionalProfilesTable
                .selectAll()
                .where {
                    (ProfessionalProfilesTable.cityId eq cityId) and
                        (ProfessionalProfilesTable.status eq ProfessionalProfileStatus.PUBLISHED)
                }.map { mapProfile(it) }
        }

    override fun search(
        serviceIds: List<String>,
        cityId: String?,
    ): List<ProfessionalProfile> =
        transaction {
            val profilesWithServices =
                ProfessionalProfileServicesTable
                    .selectAll()
                    .where { ProfessionalProfileServicesTable.serviceId inList serviceIds }
                    .map { it[ProfessionalProfileServicesTable.professionalProfileId] }
                    .distinct()

            if (profilesWithServices.isEmpty()) return@transaction emptyList()

            val query =
                ProfessionalProfilesTable
                    .selectAll()
                    .where {
                        (ProfessionalProfilesTable.id inList profilesWithServices) and
                            (ProfessionalProfilesTable.status eq ProfessionalProfileStatus.PUBLISHED)
                    }

            if (cityId != null) {
                // We don't filter strictly by city to allow "nearby" or "no city" profiles to appear in ranking.
                // But we could optimize by loading only profiles from the same city + those with null city.
                // query.andWhere { (ProfessionalProfilesTable.cityId eq cityId) or (ProfessionalProfilesTable.cityId.isNull()) }
            }

            query.map { mapProfile(it) }
        }

    override fun updateStatus(
        id: ProfessionalProfileId,
        status: ProfessionalProfileStatus,
    ): Boolean =
        transaction {
            ProfessionalProfilesTable.update({ ProfessionalProfilesTable.id eq id.value }) {
                it[ProfessionalProfilesTable.status] = status
                it[updatedAt] = Instant.now()
            } > 0
        }

    override fun updateKnownName(
        id: ProfessionalProfileId,
        knownName: String?,
    ): Boolean =
        transaction {
            ProfessionalProfilesTable.update({ ProfessionalProfilesTable.id eq id.value }) {
                it[ProfessionalProfilesTable.knownName] = knownName
                it[updatedAt] = Instant.now()
            } > 0
        }

    override fun incrementViewCount(id: ProfessionalProfileId) {
        transaction {
            ProfessionalProfilesTable.update({ ProfessionalProfilesTable.id eq id.value }) {
                with(SqlExpressionBuilder) {
                    it[viewCount] = viewCount + 1
                }
            }
        }
    }

    override fun incrementContactClickCount(id: ProfessionalProfileId) {
        transaction {
            ProfessionalProfilesTable.update({ ProfessionalProfilesTable.id eq id.value }) {
                with(SqlExpressionBuilder) {
                    it[contactClickCount] = contactClickCount + 1
                }
            }
        }
    }

    override fun updateLastActiveAt(id: ProfessionalProfileId) {
        transaction {
            ProfessionalProfilesTable.update({ ProfessionalProfilesTable.id eq id.value }) {
                it[lastActiveAt] = Instant.now()
                it[updatedAt] = Instant.now()
            }
        }
    }

    private fun mapProfile(row: ResultRow): ProfessionalProfile {
        val profileId = row[ProfessionalProfilesTable.id]

        val services =
            ProfessionalProfileServicesTable
                .selectAll()
                .where { ProfessionalProfileServicesTable.professionalProfileId eq profileId }
                .map {
                    ProfessionalProfileService(
                        serviceId = it[ProfessionalProfileServicesTable.serviceId],
                        matchLevel = it[ProfessionalProfileServicesTable.matchLevel],
                    )
                }

        val portfolioPhotos =
            ProfessionalProfilePortfolioPhotosTable
                .selectAll()
                .where { ProfessionalProfilePortfolioPhotosTable.professionalProfileId eq profileId }
                .map {
                    PortfolioPhoto(
                        id = it[ProfessionalProfilePortfolioPhotosTable.id],
                        photoUrl = it[ProfessionalProfilePortfolioPhotosTable.photoUrl],
                        caption = it[ProfessionalProfilePortfolioPhotosTable.caption],
                        createdAt = it[ProfessionalProfilePortfolioPhotosTable.createdAt],
                    )
                }

        return ProfessionalProfile(
            id = ProfessionalProfileId(profileId),
            userId = UserId(row[ProfessionalProfilesTable.userId]),
            knownName = row[ProfessionalProfilesTable.knownName],
            description = row[ProfessionalProfilesTable.description],
            normalizedDescription = row[ProfessionalProfilesTable.normalizedDescription],
            cityId = row[ProfessionalProfilesTable.cityId],
            services = services,
            portfolioPhotos = portfolioPhotos,
            completeness = row[ProfessionalProfilesTable.completeness],
            status = row[ProfessionalProfilesTable.status],
            lastActiveAt = row[ProfessionalProfilesTable.lastActiveAt] ?: row[ProfessionalProfilesTable.createdAt],
            createdAt = row[ProfessionalProfilesTable.createdAt],
            updatedAt = row[ProfessionalProfilesTable.updatedAt],
            viewCount = row[ProfessionalProfilesTable.viewCount],
            contactClickCount = row[ProfessionalProfilesTable.contactClickCount],
        )
    }
}
