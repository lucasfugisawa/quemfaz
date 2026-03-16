package com.fugisawa.quemfaz.auth.infrastructure

import com.fugisawa.quemfaz.auth.domain.OtpChallenge
import com.fugisawa.quemfaz.auth.domain.OtpChallengeRepository
import com.fugisawa.quemfaz.auth.domain.User
import com.fugisawa.quemfaz.auth.domain.UserPhoneAuthIdentity
import com.fugisawa.quemfaz.auth.domain.UserPhoneAuthIdentityRepository
import com.fugisawa.quemfaz.auth.domain.UserRepository
import com.fugisawa.quemfaz.auth.domain.UserStatus
import com.fugisawa.quemfaz.core.id.UserId
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalDate

object UsersTable : Table("users") {
    val id = varchar("id", 128)
    val fullName = text("full_name")
    val photoUrl = varchar("photo_url", 1024).nullable()
    val status =
        customEnumeration(
            "status",
            "user_status",
            { UserStatus.valueOf(it as String) },
            {
                val pgObject = org.postgresql.util.PGobject()
                pgObject.type = "user_status"
                pgObject.value = it.name
                pgObject
            },
        )
    val dateOfBirth = date("date_of_birth").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object UserPhoneAuthIdentitiesTable : Table("user_phone_auth_identities") {
    val id = varchar("id", 128)
    val userId = varchar("user_id", 128) references UsersTable.id
    val phoneNumber = varchar("phone_number", 50)
    val isVerified = bool("is_verified")
    val verifiedAt = timestamp("verified_at").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object OtpChallengesTable : Table("otp_challenges") {
    val id = varchar("id", 128)
    val phoneNumber = varchar("phone_number", 50)
    val otpCodeHash = varchar("otp_code_hash", 255)
    val expiresAt = timestamp("expires_at")
    val attemptCount = integer("attempt_count")
    val maxAttempts = integer("max_attempts")
    val consumedAt = timestamp("consumed_at").nullable()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

object RefreshTokensTable : Table("refresh_tokens") {
    val token = varchar("token", 255)
    val userId = varchar("user_id", 128) references UsersTable.id
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at")
    val revokedAt = timestamp("revoked_at").nullable()

    override val primaryKey = PrimaryKey(token)
}

class ExposedUserRepository : UserRepository {
    override fun create(user: User): User =
        transaction {
            UsersTable.insert {
                it[id] = user.id.value
                it[fullName] = user.fullName
                it[photoUrl] = user.photoUrl
                it[status] = user.status
                it[dateOfBirth] = user.dateOfBirth
                it[createdAt] = user.createdAt
                it[updatedAt] = user.updatedAt
            }
            user
        }

    override fun findById(id: UserId): User? =
        transaction {
            UsersTable
                .selectAll()
                .where { UsersTable.id eq id.value }
                .map { mapUser(it) }
                .singleOrNull()
        }

    override fun updateName(id: UserId, fullName: String): User? =
        transaction {
            UsersTable.update({ UsersTable.id eq id.value }) {
                it[UsersTable.fullName] = fullName
                it[updatedAt]           = Instant.now()
            }
            findById(id)
        }

    override fun updateDateOfBirth(id: UserId, dateOfBirth: LocalDate): User? =
        transaction {
            UsersTable.update({ UsersTable.id eq id.value }) {
                it[UsersTable.dateOfBirth] = dateOfBirth
                it[updatedAt]              = Instant.now()
            }
            findById(id)
        }

    override fun updatePhotoUrl(id: UserId, photoUrl: String): User? =
        transaction {
            UsersTable.update({ UsersTable.id eq id.value }) {
                it[UsersTable.photoUrl] = photoUrl
                it[updatedAt]          = Instant.now()
            }
            findById(id)
        }

    override fun updateStatus(
        id: UserId,
        status: UserStatus,
    ): Boolean =
        transaction {
            UsersTable.update({ UsersTable.id eq id.value }) {
                it[UsersTable.status] = status
                it[updatedAt] = Instant.now()
            } > 0
        }

    private fun mapUser(it: ResultRow) =
        User(
            id = UserId(it[UsersTable.id]),
            fullName = it[UsersTable.fullName],
            photoUrl = it[UsersTable.photoUrl],
            status = it[UsersTable.status],
            dateOfBirth = it[UsersTable.dateOfBirth],
            createdAt = it[UsersTable.createdAt],
            updatedAt = it[UsersTable.updatedAt],
        )
}

class ExposedUserPhoneAuthIdentityRepository : UserPhoneAuthIdentityRepository {
    override fun findByPhoneNumber(phoneNumber: String): UserPhoneAuthIdentity? =
        transaction {
            UserPhoneAuthIdentitiesTable
                .selectAll()
                .where { UserPhoneAuthIdentitiesTable.phoneNumber eq phoneNumber }
                .map { mapIdentity(it) }
                .singleOrNull()
        }

    override fun findByUserId(userId: UserId): UserPhoneAuthIdentity? =
        transaction {
            UserPhoneAuthIdentitiesTable
                .selectAll()
                .where { UserPhoneAuthIdentitiesTable.userId eq userId.value }
                .map { mapIdentity(it) }
                .singleOrNull()
        }

    override fun create(identity: UserPhoneAuthIdentity): UserPhoneAuthIdentity =
        transaction {
            UserPhoneAuthIdentitiesTable.insert {
                it[id] = identity.id
                it[userId] = identity.userId.value
                it[phoneNumber] = identity.phoneNumber
                it[isVerified] = identity.isVerified
                it[verifiedAt] = identity.verifiedAt
                it[createdAt] = identity.createdAt
                it[updatedAt] = identity.updatedAt
            }
            identity
        }

    override fun markVerified(
        id: String,
        verifiedAt: Instant,
    ): Boolean =
        transaction {
            UserPhoneAuthIdentitiesTable.update({ UserPhoneAuthIdentitiesTable.id eq id }) {
                it[isVerified] = true
                it[UserPhoneAuthIdentitiesTable.verifiedAt] = verifiedAt
                it[updatedAt] = Instant.now()
            } > 0
        }

    private fun mapIdentity(it: ResultRow) =
        UserPhoneAuthIdentity(
            id = it[UserPhoneAuthIdentitiesTable.id],
            userId = UserId(it[UserPhoneAuthIdentitiesTable.userId]),
            phoneNumber = it[UserPhoneAuthIdentitiesTable.phoneNumber],
            isVerified = it[UserPhoneAuthIdentitiesTable.isVerified],
            verifiedAt = it[UserPhoneAuthIdentitiesTable.verifiedAt],
            createdAt = it[UserPhoneAuthIdentitiesTable.createdAt],
            updatedAt = it[UserPhoneAuthIdentitiesTable.updatedAt],
        )
}

class ExposedOtpChallengeRepository : OtpChallengeRepository {
    override fun create(challenge: OtpChallenge): OtpChallenge =
        transaction {
            OtpChallengesTable.insert {
                it[id] = challenge.id
                it[phoneNumber] = challenge.phoneNumber
                it[otpCodeHash] = challenge.otpCodeHash
                it[expiresAt] = challenge.expiresAt
                it[attemptCount] = challenge.attemptCount
                it[maxAttempts] = challenge.maxAttempts
                it[createdAt] = challenge.createdAt
            }
            challenge
        }

    override fun findLatestActiveByPhoneNumber(phoneNumber: String): OtpChallenge? =
        transaction {
            val now = Instant.now()
            OtpChallengesTable
                .selectAll()
                .where {
                    (OtpChallengesTable.phoneNumber eq phoneNumber) and
                        (OtpChallengesTable.consumedAt.isNull()) and
                        (OtpChallengesTable.expiresAt greater now)
                }.orderBy(OtpChallengesTable.createdAt, org.jetbrains.exposed.sql.SortOrder.DESC)
                .limit(1)
                .map { mapChallenge(it) }
                .singleOrNull()
        }

    override fun markConsumed(
        id: String,
        consumedAt: Instant,
    ): Boolean =
        transaction {
            OtpChallengesTable.update({ OtpChallengesTable.id eq id }) {
                it[OtpChallengesTable.consumedAt] = consumedAt
            } > 0
        }

    override fun incrementAttemptCount(id: String): Int =
        transaction {
            OtpChallengesTable.update({ OtpChallengesTable.id eq id }) {
                it.update(attemptCount, attemptCount + 1)
            }
            OtpChallengesTable
                .selectAll()
                .where { OtpChallengesTable.id eq id }
                .map { it[OtpChallengesTable.attemptCount] }
                .singleOrNull() ?: -1
        }

    private fun mapChallenge(it: ResultRow) =
        OtpChallenge(
            id = it[OtpChallengesTable.id],
            phoneNumber = it[OtpChallengesTable.phoneNumber],
            otpCodeHash = it[OtpChallengesTable.otpCodeHash],
            expiresAt = it[OtpChallengesTable.expiresAt],
            attemptCount = it[OtpChallengesTable.attemptCount],
            maxAttempts = it[OtpChallengesTable.maxAttempts],
            consumedAt = it[OtpChallengesTable.consumedAt],
            createdAt = it[OtpChallengesTable.createdAt],
        )
}

class ExposedRefreshTokenRepository : com.fugisawa.quemfaz.auth.domain.RefreshTokenRepository {
    override fun create(refreshToken: com.fugisawa.quemfaz.auth.domain.RefreshToken): com.fugisawa.quemfaz.auth.domain.RefreshToken =
        transaction {
            RefreshTokensTable.insert {
                it[token] = refreshToken.token
                it[userId] = refreshToken.userId.value
                it[expiresAt] = refreshToken.expiresAt
                it[createdAt] = refreshToken.createdAt
                it[revokedAt] = refreshToken.revokedAt
            }
            refreshToken
        }

    override fun findByToken(token: String): com.fugisawa.quemfaz.auth.domain.RefreshToken? =
        transaction {
            RefreshTokensTable
                .selectAll()
                .where { RefreshTokensTable.token eq token }
                .map { mapRefreshToken(it) }
                .singleOrNull()
        }

    override fun revokeByUserId(userId: UserId): Unit =
        transaction {
            RefreshTokensTable.update({ (RefreshTokensTable.userId eq userId.value) and (RefreshTokensTable.revokedAt.isNull()) }) {
                it[revokedAt] = Instant.now()
            }
        }

    override fun revokeByToken(token: String): Unit =
        transaction {
            RefreshTokensTable.update({ (RefreshTokensTable.token eq token) and (RefreshTokensTable.revokedAt.isNull()) }) {
                it[revokedAt] = Instant.now()
            }
        }

    override fun deleteExpired(now: Instant): Unit =
        transaction {
            RefreshTokensTable.deleteWhere {
                expiresAt less now
            }
        }

    private fun mapRefreshToken(it: ResultRow) =
        com.fugisawa.quemfaz.auth.domain.RefreshToken(
            token = it[RefreshTokensTable.token],
            userId = UserId(it[RefreshTokensTable.userId]),
            expiresAt = it[RefreshTokensTable.expiresAt],
            createdAt = it[RefreshTokensTable.createdAt],
            revokedAt = it[RefreshTokensTable.revokedAt],
        )
}
