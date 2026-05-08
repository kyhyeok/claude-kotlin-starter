package com.kim.starter.adapter.webapi

import com.kim.starter.domain.member.DuplicateEmailException
import com.kim.starter.domain.member.InvalidCredentialException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.time.Clock
import java.time.Instant

/**
 * RFC 7807 ProblemDetail 기반 표준 에러 응답.
 *
 * 도메인 예외 → HTTP 상태 매핑은 모두 이곳에서 단일 책임으로 처리한다.
 * 도메인 코드는 의미만 박고, 트랜스포트 변환은 어댑터에서.
 */
@RestControllerAdvice
class ApiControllerAdvice(
    private val clock: Clock,
) : ResponseEntityExceptionHandler() {
    @ExceptionHandler(DuplicateEmailException::class)
    fun handleDuplicateEmail(ex: DuplicateEmailException): ProblemDetail = problem(HttpStatus.CONFLICT, ex)

    @ExceptionHandler(InvalidCredentialException::class)
    fun handleInvalidCredential(ex: InvalidCredentialException): ProblemDetail = problem(HttpStatus.UNAUTHORIZED, ex)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ProblemDetail = problem(HttpStatus.BAD_REQUEST, ex)

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(ex: IllegalStateException): ProblemDetail = problem(HttpStatus.CONFLICT, ex)

    private fun problem(
        status: HttpStatus,
        ex: Exception,
    ): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, ex.message ?: status.reasonPhrase).apply {
            setProperty("timestamp", Instant.now(clock))
            setProperty("exception", ex.javaClass.simpleName)
        }
}
