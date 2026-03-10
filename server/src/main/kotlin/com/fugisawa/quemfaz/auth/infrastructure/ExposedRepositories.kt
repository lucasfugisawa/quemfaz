package com.fugisawa.quemfaz.auth.infrastructure

import com.fugisawa.quemfaz.auth.domain.*
import com.fugisawa.quemfaz.core.id.UserId
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.and
import java.time.Instant

object UsersTable : Table("users") {
    val id = varchar("id", 128)
    val name = varchar("name", 255).nullable()
    val photoUrl = varchar("photo_url", 1024).nullable()
    val status = varchar("status", 50)
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

class ExposedUserRepository : UserRepository {
    override fun create(user: User): User = transaction {
        UsersTable.insert {
            it[id] = user.id.value
            it[name] = user.name
            it[photoUrl] = user.photoUrl
            it[status] = user.status.name
            it[createdAt] = user.createdAt
            it[updatedAt] = user.updatedAt
        }
        user
    }

    override fun findById(id: UserId): User? = transaction {
        UsersTable.selectAll().where { UsersTable.id eq id.value }
            .map { mapUser(it) }
            .singleOrNull()
    }

    override fun updateProfile(id: UserId, name: String, photoUrl: String?): User? = transaction {
        val updated = UsersTable.update({ UsersTable.id eq id.value }) {
            it[UsersTable.name] = name
            it[UsersTable.photoUrl] = photoUrl
            it[updatedAt] = Instant.now()
        }
        if (updated > 0) findById(id) else null
    }

    private fun mapUser(it: ResultRow) = User(
        id = UserId(it[UsersTable.id]),
        name = it[UsersTable.name],
        photoUrl = it[UsersTable.photoUrl],
        status = UserStatus.valueOf(it[UsersTable.status]),
        createdAt = it[UsersTable.createdAt],
        updatedAt = it[UsersTable.updatedAt]
    )
}

class ExposedUserPhoneAuthIdentityRepository : UserPhoneAuthIdentityRepository {
    override fun findByPhoneNumber(phoneNumber: String): UserPhoneAuthIdentity? = transaction {
        UserPhoneAuthIdentitiesTable.selectAll().where { UserPhoneAuthIdentitiesTable.phoneNumber eq phoneNumber }
            .map { mapIdentity(it) }
            .singleOrNull()
    }

    override fun findByUserId(userId: UserId): UserPhoneAuthIdentity? = transaction {
        UserPhoneAuthIdentitiesTable.selectAll().where { UserPhoneAuthIdentitiesTable.userId eq userId.value }
            .map { mapIdentity(it) }
            .singleOrNull()
    }

    override fun create(identity: UserPhoneAuthIdentity): UserPhoneAuthIdentity = transaction {
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

    override fun markVerified(id: String, verifiedAt: Instant): Boolean = transaction {
        UserPhoneAuthIdentitiesTable.update({ UserPhoneAuthIdentitiesTable.id eq id }) {
            it[isVerified] = true
            it[UserPhoneAuthIdentitiesTable.verifiedAt] = verifiedAt
            it[updatedAt] = Instant.now()
        } > 0
    }

    private fun mapIdentity(it: ResultRow) = UserPhoneAuthIdentity(
        id = it[UserPhoneAuthIdentitiesTable.id],
        userId = UserId(it[UserPhoneAuthIdentitiesTable.userId]),
        phoneNumber = it[UserPhoneAuthIdentitiesTable.phoneNumber],
        isVerified = it[UserPhoneAuthIdentitiesTable.isVerified],
        verifiedAt = it[UserPhoneAuthIdentitiesTable.verifiedAt],
        createdAt = it[UserPhoneAuthIdentitiesTable.createdAt],
        updatedAt = it[UserPhoneAuthIdentitiesTable.updatedAt]
    )
}

class ExposedOtpChallengeRepository : OtpChallengeRepository {
    override fun create(challenge: OtpChallenge): OtpChallenge = transaction {
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

    override fun findLatestActiveByPhoneNumber(phoneNumber: String): OtpChallenge? = transaction {
        val now = Instant.now()
        OtpChallengesTable.selectAll().where {
            (OtpChallengesTable.phoneNumber eq phoneNumber) and
                    (OtpChallengesTable.consumedAt.isNull()) and
                    (OtpChallengesTable.expiresAt greater now)
        }.orderBy(OtpChallengesTable.createdAt, org.jetbrains.exposed.sql.SortOrder.DESC)
            .limit(1)
            .map { mapChallenge(it) }
            .singleOrNull()
    }

    override fun markConsumed(id: String, consumedAt: Instant): Boolean = transaction {
        OtpChallengesTable.update({ OtpChallengesTable.id eq id }) {
            it[OtpChallengesTable.consumedAt] = consumedAt
        } > 0
    }

    override fun incrementAttemptCount(id: String): Int = transaction {
        OtpChallengesTable.update({ OtpChallengesTable.id eq id }) {
            it.update(attemptCount, attemptCount + 1)
        }
        OtpChallengesTable.selectAll().where { OtpChallengesTable.id eq id }
            .map { it[OtpChallengesTable.attemptCount] }
            .singleOrNull() ?: -1
    }

    private fun mapChallenge(it: ResultRow) = OtpChallenge(
        id = it[OtpChallengesTable.id],
        phoneNumber = it[OtpChallengesTable.phoneNumber],
        otpCodeHash = it[OtpChallengesTable.otpCodeHash],
        expiresAt = it[OtpChallengesTable.expiresAt],
        attemptCount = it[OtpChallengesTable.attemptCount],
        maxAttempts = it[OtpChallengesTable.maxAttempts],
        consumedAt = it[OtpChallengesTable.consumedAt],
        createdAt = it[OtpChallengesTable.createdAt]
    )
}
