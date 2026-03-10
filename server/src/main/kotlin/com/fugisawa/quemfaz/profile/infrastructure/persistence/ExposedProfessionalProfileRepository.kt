package com.fugisawa.quemfaz.profile.infrastructure.persistence

import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.domain.service.ServiceMatchLevel
import com.fugisawa.quemfaz.profile.domain.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

object ProfessionalProfilesTable : Table("professional_profiles") {
    val id = varchar("id", 128)
    val userId = varchar("user_id", 128)
    val description = text("description").nullable()
    val normalizedDescription = text("normalized_description").nullable()
    val contactPhone = varchar("contact_phone", 50).nullable()
    val whatsappPhone = varchar("whatsapp_phone", 50).nullable()
    val cityName = varchar("city_name", 255).nullable()
    val completeness = customEnumeration("completeness", "profile_completeness", { ProfileCompleteness.valueOf(it as String) }, { it.name })
    val status = customEnumeration("status", "profile_status", { ProfessionalProfileStatus.valueOf(it as String) }, { it.name })
    val lastActiveAt = timestamp("last_active_at").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object ProfessionalProfileNeighborhoodsTable : Table("professional_profile_neighborhoods") {
    val professionalProfileId = varchar("professional_profile_id", 128) references ProfessionalProfilesTable.id
    val neighborhoodName = varchar("neighborhood_name", 255)

    override val primaryKey = PrimaryKey(professionalProfileId, neighborhoodName)
}

object ProfessionalProfileServicesTable : Table("professional_profile_services") {
    val professionalProfileId = varchar("professional_profile_id", 128) references ProfessionalProfilesTable.id
    val serviceId = varchar("service_id", 128)
    val matchLevel = customEnumeration("match_level", "service_match_level", { ServiceMatchLevel.valueOf(it as String) }, { it.name })

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

    override fun findByUserId(userId: UserId): ProfessionalProfile? = transaction {
        ProfessionalProfilesTable.selectAll().where { ProfessionalProfilesTable.userId eq userId.value }
            .map { mapProfile(it) }
            .singleOrNull()
    }

    override fun findById(id: ProfessionalProfileId): ProfessionalProfile? = transaction {
        ProfessionalProfilesTable.selectAll().where { ProfessionalProfilesTable.id eq id.value }
            .map { mapProfile(it) }
            .singleOrNull()
    }

    override fun save(profile: ProfessionalProfile): ProfessionalProfile = transaction {
        val exists = ProfessionalProfilesTable.selectAll().where { ProfessionalProfilesTable.id eq profile.id.value }.any()
        if (exists) {
            ProfessionalProfilesTable.update({ ProfessionalProfilesTable.id eq profile.id.value }) {
                it[description] = profile.description
                it[normalizedDescription] = profile.normalizedDescription
                it[contactPhone] = profile.contactPhone
                it[whatsappPhone] = profile.whatsappPhone
                it[cityName] = profile.cityName
                it[completeness] = profile.completeness
                it[status] = profile.status
                it[lastActiveAt] = profile.lastActiveAt
                it[updatedAt] = profile.updatedAt
            }
        } else {
            ProfessionalProfilesTable.insert {
                it[id] = profile.id.value
                it[userId] = profile.userId.value
                it[description] = profile.description
                it[normalizedDescription] = profile.normalizedDescription
                it[contactPhone] = profile.contactPhone
                it[whatsappPhone] = profile.whatsappPhone
                it[cityName] = profile.cityName
                it[completeness] = profile.completeness
                it[status] = profile.status
                it[lastActiveAt] = profile.lastActiveAt
                it[createdAt] = profile.createdAt
                it[updatedAt] = profile.updatedAt
            }
        }

        ProfessionalProfileNeighborhoodsTable.deleteWhere { professionalProfileId eq profile.id.value }
        profile.neighborhoods.forEach { neighborhood ->
            ProfessionalProfileNeighborhoodsTable.insert {
                it[professionalProfileId] = profile.id.value
                it[neighborhoodName] = neighborhood
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

    override fun listPublishedByCity(cityName: String): List<ProfessionalProfile> = transaction {
        ProfessionalProfilesTable.selectAll()
            .where { (ProfessionalProfilesTable.cityName eq cityName) and (ProfessionalProfilesTable.status eq ProfessionalProfileStatus.PUBLISHED) }
            .map { mapProfile(it) }
    }

    override fun updateStatus(id: ProfessionalProfileId, status: ProfessionalProfileStatus): Boolean = transaction {
        ProfessionalProfilesTable.update({ ProfessionalProfilesTable.id eq id.value }) {
            it[ProfessionalProfilesTable.status] = status
            it[updatedAt] = Instant.now()
        } > 0
    }

    private fun mapProfile(row: ResultRow): ProfessionalProfile {
        val profileId = row[ProfessionalProfilesTable.id]
        val neighborhoods = ProfessionalProfileNeighborhoodsTable.selectAll()
            .where { ProfessionalProfileNeighborhoodsTable.professionalProfileId eq profileId }
            .map { it[ProfessionalProfileNeighborhoodsTable.neighborhoodName] }

        val services = ProfessionalProfileServicesTable.selectAll()
            .where { ProfessionalProfileServicesTable.professionalProfileId eq profileId }
            .map {
                ProfessionalProfileService(
                    serviceId = it[ProfessionalProfileServicesTable.serviceId],
                    matchLevel = it[ProfessionalProfileServicesTable.matchLevel]
                )
            }

        val portfolioPhotos = ProfessionalProfilePortfolioPhotosTable.selectAll()
            .where { ProfessionalProfilePortfolioPhotosTable.professionalProfileId eq profileId }
            .map {
                PortfolioPhoto(
                    id = it[ProfessionalProfilePortfolioPhotosTable.id],
                    photoUrl = it[ProfessionalProfilePortfolioPhotosTable.photoUrl],
                    caption = it[ProfessionalProfilePortfolioPhotosTable.caption],
                    createdAt = it[ProfessionalProfilePortfolioPhotosTable.createdAt]
                )
            }

        return ProfessionalProfile(
            id = ProfessionalProfileId(profileId),
            userId = UserId(row[ProfessionalProfilesTable.userId]),
            description = row[ProfessionalProfilesTable.description],
            normalizedDescription = row[ProfessionalProfilesTable.normalizedDescription],
            contactPhone = row[ProfessionalProfilesTable.contactPhone],
            whatsappPhone = row[ProfessionalProfilesTable.whatsappPhone],
            cityName = row[ProfessionalProfilesTable.cityName],
            neighborhoods = neighborhoods,
            services = services,
            portfolioPhotos = portfolioPhotos,
            completeness = row[ProfessionalProfilesTable.completeness],
            status = row[ProfessionalProfilesTable.status],
            lastActiveAt = row[ProfessionalProfilesTable.lastActiveAt] ?: row[ProfessionalProfilesTable.createdAt],
            createdAt = row[ProfessionalProfilesTable.createdAt],
            updatedAt = row[ProfessionalProfilesTable.updatedAt]
        )
    }
}
