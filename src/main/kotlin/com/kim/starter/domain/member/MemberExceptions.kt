package com.kim.starter.domain.member

// 로그인 실패 분기는 InvalidCredential 단일화 (이메일 존재 leak 방지). 비활성/미발견은 fork에서 추가.
class DuplicateEmailException(
    email: String,
) : RuntimeException("이미 등록된 이메일입니다: $email")

class InvalidCredentialException : RuntimeException("이메일 또는 비밀번호가 일치하지 않습니다")
