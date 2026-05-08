package com.kim.starter.adapter.webapi.auth

import com.kim.starter.application.auth.provided.LoginAuthenticator
import com.kim.starter.application.auth.provided.LogoutHandler
import com.kim.starter.application.auth.provided.MemberRegister
import com.kim.starter.application.auth.provided.TokenRefresher
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 인증 API.
 *
 * - register/login/refresh: 토큰 없이 호출 가능 (`SecurityConfig.permitAll`).
 * - logout: 유효한 Access Token 필수. SecurityContext의 [Jwt.subject]로 본인 식별 →
 *   클라이언트가 임의의 memberId를 폐기하는 경로를 차단한다.
 *
 * 도메인 예외(Duplicate/InvalidCredential) → HTTP 매핑은
 * [com.kim.starter.adapter.webapi.ApiControllerAdvice]에서 단일 책임으로 처리.
 */
@RestController
@RequestMapping("/auth")
class AuthApi(
    private val memberRegister: MemberRegister,
    private val loginAuthenticator: LoginAuthenticator,
    private val tokenRefresher: TokenRefresher,
    private val logoutHandler: LogoutHandler,
) {
    @PostMapping("/register")
    fun register(
        @Valid @RequestBody body: RegisterRequest,
    ): ResponseEntity<MemberResponse> {
        val member =
            memberRegister.register(
                MemberRegister.RegisterCommand(
                    email = body.email,
                    rawPassword = body.password,
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(MemberResponse.from(member))
    }

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody body: LoginRequest,
    ): TokenResponse {
        val tokens =
            loginAuthenticator.login(
                LoginAuthenticator.LoginCommand(
                    email = body.email,
                    rawPassword = body.password,
                ),
            )
        return TokenResponse.from(tokens)
    }

    @PostMapping("/refresh")
    fun refresh(
        @Valid @RequestBody body: RefreshRequest,
    ): TokenResponse {
        val tokens = tokenRefresher.refresh(body.refreshToken)
        return TokenResponse.from(tokens)
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(
        @AuthenticationPrincipal jwt: Jwt,
    ) {
        val memberId =
            jwt.subject?.toLongOrNull()
                ?: error("JWT subject가 Member.id의 String 표현이 아닙니다: ${jwt.subject}")
        logoutHandler.logout(memberId)
    }
}
