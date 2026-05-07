package com.kim.starter.adapter.webapi

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
 * 도메인 예외는 별도 핸들러를 추가해 매핑한다.
 * 예: @ExceptionHandler(DuplicateEmailException::class) → CONFLICT
 */
@RestControllerAdvice
class ApiControllerAdvice(
    private val clock: Clock,
) : ResponseEntityExceptionHandler() {
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
