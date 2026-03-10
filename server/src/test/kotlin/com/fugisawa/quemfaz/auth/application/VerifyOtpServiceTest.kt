package com.fugisawa.quemfaz.auth.application

import com.fugisawa.quemfaz.auth.domain.OtpChallenge
import com.fugisawa.quemfaz.auth.domain.OtpHasher
import com.fugisawa.quemfaz.auth.domain.User
import com.fugisawa.quemfaz.auth.domain.UserPhoneAuthIdentity
import com.fugisawa.quemfaz.auth.domain.UserStatus
import com.fugisawa.quemfaz.auth.infrastructure.ExposedOtpChallengeRepository
import com.fugisawa.quemfaz.auth.infrastructure.ExposedUserPhoneAuthIdentityRepository
import com.fugisawa.quemfaz.auth.infrastructure.ExposedUserRepository
import com.fugisawa.quemfaz.auth.infrastructure.OtpChallengesTable
import com.fugisawa.quemfaz.auth.infrastructure.UserPhoneAuthIdentitiesTable
import com.fugisawa.quemfaz.auth.infrastructure.UsersTable
import com.fugisawa.quemfaz.auth.token.TokenService
import com.fugisawa.quemfaz.contract.auth.VerifyOtpRequest
import com.fugisawa.quemfaz.core.id.UserId
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VerifyOtpServiceTest {
    @Before
    fun setup() {
        Database.connect(
            "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
        )
        transaction {
            exec("CREATE TYPE IF NOT EXISTS user_status AS ENUM ('ACTIVE', 'BLOCKED')")
            SchemaUtils.drop(UsersTable, UserPhoneAuthIdentitiesTable, OtpChallengesTable)
            SchemaUtils.create(UsersTable, UserPhoneAuthIdentitiesTable, OtpChallengesTable)
        }
    }

    private class FakeOtpHasher : OtpHasher {
        override fun hash(otpCode: String): String = "hashed_$otpCode"

        override fun verify(
            otpCode: String,
            hash: String,
        ): Boolean = hash == "hashed_$otpCode"
    }

    private val userRepo = ExposedUserRepository()
    private val identityRepo = ExposedUserPhoneAuthIdentityRepository()
    private val otpRepo = ExposedOtpChallengeRepository()
    private val hasher = FakeOtpHasher()
    private val tokenService =
        TokenService(
            com.fugisawa.quemfaz.config
                .JwtConfig("secret", "issuer", "audience", 3600000),
        )

    private val service = VerifyOtpService(userRepo, identityRepo, otpRepo, hasher, tokenService)

    @Test
    fun `should verify correctly and create new user on first login`() {
        val phone = "5511999999999"
        val code = "123456"
        transaction {
            otpRepo.create(OtpChallenge("c1", phone, "hashed_$code", Instant.now().plusSeconds(300), 0, 3, null, Instant.now()))
        }

        val result = service.verifyOtp(VerifyOtpRequest(phone, code))

        assertTrue(result is VerifyOtpResult.Success)
        val success = result
        assertTrue(success.response.isNewUser)
        transaction {
            assertEquals(1, UsersTable.selectAll().count())
            assertEquals(1, UserPhoneAuthIdentitiesTable.selectAll().count())
        }
    }

    @Test
    fun `should verify correctly for existing user`() {
        val phone = "5511999999999"
        val code = "123456"
        val userId = UserId("u1")
        transaction {
            userRepo.create(User(userId, "Test", null, UserStatus.ACTIVE, Instant.now(), Instant.now()))
            identityRepo.create(UserPhoneAuthIdentity("i1", userId, phone, true, Instant.now(), Instant.now(), Instant.now()))
            otpRepo.create(OtpChallenge("c1", phone, "hashed_$code", Instant.now().plusSeconds(300), 0, 3, null, Instant.now()))
        }

        val result = service.verifyOtp(VerifyOtpRequest(phone, code))

        assertTrue(result is VerifyOtpResult.Success)
        val success = result
        assertTrue(!success.response.isNewUser)
        assertEquals(userId.value, success.response.userId)
    }

    @Test
    fun `should reject invalid code`() {
        val phone = "5511999999999"
        transaction {
            otpRepo.create(OtpChallenge("c1", phone, "hashed_123456", Instant.now().plusSeconds(300), 0, 3, null, Instant.now()))
        }

        val result = service.verifyOtp(VerifyOtpRequest(phone, "wrong"))

        assertTrue(result is VerifyOtpResult.Failure)
        assertEquals("Invalid code", result.message)
        transaction {
            val challenge = otpRepo.findLatestActiveByPhoneNumber(phone)
            assertEquals(1, challenge?.attemptCount)
        }
    }

    @Test
    fun `should reject blocked user`() {
        val phone = "5511999999999"
        val code = "123456"
        val userId = UserId("u1")
        transaction {
            userRepo.create(User(userId, "Test", null, UserStatus.BLOCKED, Instant.now(), Instant.now()))
            identityRepo.create(UserPhoneAuthIdentity("i1", userId, phone, true, Instant.now(), Instant.now(), Instant.now()))
            otpRepo.create(OtpChallenge("c1", phone, "hashed_$code", Instant.now().plusSeconds(300), 0, 3, null, Instant.now()))
        }

        val result = service.verifyOtp(VerifyOtpRequest(phone, code))

        assertTrue(result is VerifyOtpResult.Failure)
        assertEquals("User is blocked", result.message)
    }
}
