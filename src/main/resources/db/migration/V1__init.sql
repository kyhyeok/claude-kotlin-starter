-- V1: 회원(members) 테이블 — starter scope의 최소 user 모델 (ADR-0018)
--
-- 인증의 최소 의존성. Refresh Token은 Redis에 저장(ADR-0003)하므로 별도 테이블 없음.
-- 활성/정지 상태가 필요한 fork된 서비스는 V2 마이그레이션으로 enum/별도 컬럼을 추가한다.

CREATE TABLE members (
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE  members               IS '회원 계정 (starter scope의 최소 user 모델)';
COMMENT ON COLUMN members.email         IS '로그인 식별자(고유)';
COMMENT ON COLUMN members.password_hash IS 'PasswordEncoder로 해시된 비밀번호';
COMMENT ON COLUMN members.is_active     IS '활성 회원 여부 — fork된 서비스가 비활성/정지 시나리오 추가 시 별도 컬럼/enum 도입';
