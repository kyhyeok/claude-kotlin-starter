package com.kim.starter.domain.member

/**
 * Auth 슬라이스에서 사용하는 도메인 예외 (starter scope의 최소 — ADR-0018).
 *
 * starter는 회원 등록 중복 + 로그인 실패만 분기하고, 비활성/정지 회원 차단·회원 미발견 같은
 * 추가 분기는 fork된 서비스가 도메인에 박는다. 이메일 존재 leak 방지를 위해 로그인 실패는
 * 미등록·비밀번호 불일치를 단일 [InvalidCredentialException]으로 통합한다.
 *
 * HTTP 상태 매핑은 `adapter.webapi.ApiControllerAdvice`에서 단일 책임으로 처리한다.
 */
class DuplicateEmailException(
    email: String,
) : RuntimeException("이미 등록된 이메일입니다: $email")

class InvalidCredentialException : RuntimeException("이메일 또는 비밀번호가 일치하지 않습니다")
