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

// RFC 7807 ProblemDetail. 도메인 예외 → HTTP 매핑은 모두 여기에서 단일 책임.
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
