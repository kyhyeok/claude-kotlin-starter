package com.kim.starter.application.auth

import com.kim.starter.adapter.security.JwtProperties
import com.kim.starter.adapter.security.NimbusJwtIssuer
import com.kim.starter.application.auth.provided.LoginAuthenticator
import com.kim.starter.application.required.MemberRepository
import com.kim.starter.application.required.RefreshTokenStore
import com.kim.starter.domain.member.Email
import com.kim.starter.domain.member.InvalidCredentialException
import com.kim.starter.domain.member.Member
import com.kim.starter.domain.member.MemberNotActiveException
import com.kim.starter.domain.member.MemberStatus
import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import javax.crypto.spec.SecretKeySpec

@DisplayName("AuthenticationService")
class AuthenticationServiceTest {
    private val secret = "test-secret-must-be-at-least-32-bytes-long-for-hs256"
    private val key = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
    private val encoder = NimbusJwtEncoder(ImmutableSecret<SecurityContext>(key))
    private val decoder =
        NimbusJwtDecoder
            .withSecretKey(key)
            .build()
            .apply {
                setJwtValidator(OAuth2TokenValidator<Jwt> { OAuth2TokenValidatorResult.success() })
            }
    private val now = Instant.parse("2026-05-07T00:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val properties =
        JwtProperties(
            secret = secret,
            accessTokenTtl = Duration.ofMinutes(15),
            refreshTokenTtl = Duration.ofDays(7),
        )
    private val jwtIssuer = NimbusJwtIssuer(encoder, clock, properties)

    private val members = mockk<MemberRepository>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val refreshTokenStore = mockk<RefreshTokenStore>(relaxUnitFun = true)
    private val service =
        AuthenticationService(
            members = members,
            passwordEncoder = passwordEncoder,
            jwtIssuer = jwtIssuer,
            jwtDecoder = decoder,
            refreshTokenStore = refreshTokenStore,
        )

    private val email = Email("user@example.com")
    private val activeMember =
        mockk<Member> {
            every { id } returns 42L
            every { passwordHash } returns "hashed"
            every { isActive } returns true
            every { currentStatus } returns MemberStatus.ACTIVE
        }

    @Test
    fun `login은 일치하는 이메일과 비밀번호로 Access와 Refresh Token을 발급한다`() {
        every { members.findByEmail(email) } returns activeMember
        every { passwordEncoder.matches("rawPwd", "hashed") } returns true

        val tokens = service.login(LoginAuthenticator.LoginCommand(email, "rawPwd"))

        assertThat(tokens.accessToken.subject).isEqualTo("42")
        assertThat(tokens.refreshToken.subject).isEqualTo("42")
        verify(exactly = 1) {
            refreshTokenStore.save("42", tokens.refreshToken.value, tokens.refreshToken.ttl)
        }
    }

    @Test
    fun `login은 등록되지 않은 이메일을 InvalidCredentialException으로 거부한다`() {
        every { members.findByEmail(email) } returns null

        assertThatThrownBy {
            service.login(LoginAuthenticator.LoginCommand(email, "rawPwd"))
        }.isInstanceOf(InvalidCredentialException::class.java)

        verify(exactly = 0) { refreshTokenStore.save(any(), any(), any()) }
    }

    @Test
    fun `login은 비밀번호 불일치를 InvalidCredentialException으로 거부한다`() {
        every { members.findByEmail(email) } returns activeMember
        every { passwordEncoder.matches("wrong", "hashed") } returns false

        assertThatThrownBy {
            service.login(LoginAuthenticator.LoginCommand(email, "wrong"))
        }.isInstanceOf(InvalidCredentialException::class.java)
    }

    @Test
    fun `login은 비활성 회원을 MemberNotActiveException으로 거부한다`() {
        val bannedMember =
            mockk<Member> {
                every { id } returns 42L
                every { passwordHash } returns "hashed"
                every { isActive } returns false
                every { currentStatus } returns MemberStatus.BANNED
            }
        every { members.findByEmail(email) } returns bannedMember
        every { passwordEncoder.matches("rawPwd", "hashed") } returns true

        assertThatThrownBy {
            service.login(LoginAuthenticator.LoginCommand(email, "rawPwd"))
        }.isInstanceOf(MemberNotActiveException::class.java)
    }

    @Test
    fun `refresh는 typ refresh 클레임 + Redis 일치를 모두 만족하면 새 토큰 쌍을 발급한다`() {
        val rt = jwtIssuer.issueRefreshToken("42")
        every { refreshTokenStore.find("42") } returns rt.value

        val tokens = service.refresh(rt.value)

        assertThat(tokens.accessToken.subject).isEqualTo("42")
        assertThat(tokens.refreshToken.subject).isEqualTo("42")
        // rotation: 새 RT가 Redis에 저장됨
        verify(exactly = 1) {
            refreshTokenStore.save("42", tokens.refreshToken.value, tokens.refreshToken.ttl)
        }
    }

    @Test
    fun `refresh는 잘못된 서명의 토큰을 InvalidCredentialException으로 거부한다`() {
        assertThatThrownBy {
            service.refresh("invalid.jwt.token")
        }.isInstanceOf(InvalidCredentialException::class.java)
    }

    @Test
    fun `refresh는 typ가 access인 토큰을 거부한다 (AT 도용 차단)`() {
        val accessToken = jwtIssuer.issueAccessToken("42")

        assertThatThrownBy {
            service.refresh(accessToken.value)
        }.isInstanceOf(InvalidCredentialException::class.java)

        verify(exactly = 0) { refreshTokenStore.save(any(), any(), any()) }
    }

    @Test
    fun `refresh는 Redis에 다른 RT가 저장되어 있으면 거부한다 (RT 재사용 차단)`() {
        val oldRt = jwtIssuer.issueRefreshToken("42")
        every { refreshTokenStore.find("42") } returns "different-rt-value"

        assertThatThrownBy {
            service.refresh(oldRt.value)
        }.isInstanceOf(InvalidCredentialException::class.java)
    }

    @Test
    fun `logout은 회원의 활성 RT를 폐기한다`() {
        service.logout(memberId = 42L)

        verify(exactly = 1) { refreshTokenStore.revoke("42") }
    }
}
