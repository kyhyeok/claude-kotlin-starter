package com.kim.starter.domain.member

/**
 * 회원 도메인 예외.
 *
 * HTTP 상태 매핑은 `adapter.webapi.ApiControllerAdvice`에서 단일 책임으로 처리한다.
 * 도메인은 의미만 박고, 트랜스포트 변환은 어댑터에서.
 */
class DuplicateEmailException(
    email: Email,
) : RuntimeException("이미 등록된 이메일입니다: ${email.value}")

class MemberNotFoundException(
    criteria: String,
) : RuntimeException("회원을 찾을 수 없습니다: $criteria")

class MemberNotActiveException(
    status: MemberStatus,
) : RuntimeException("활성 상태가 아닌 회원입니다: $status")

class InvalidCredentialException : RuntimeException("이메일 또는 비밀번호가 일치하지 않습니다")
