-- V1: 회원(members) 테이블
--
-- 인증의 최소 의존성. Refresh Token은 Redis에 저장(ADR-0003)하므로 별도 테이블 없음.
-- status는 enum 대신 VARCHAR로 두어 마이그레이션 친화적으로 관리(추가/제거가 ALTER TYPE보다 단순).

CREATE TABLE members (
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_members_status ON members (status);

COMMENT ON TABLE  members               IS '회원 계정';
COMMENT ON COLUMN members.email         IS '로그인 식별자(고유)';
COMMENT ON COLUMN members.password_hash IS 'PasswordEncoder로 해시된 비밀번호';
COMMENT ON COLUMN members.status        IS 'PENDING / ACTIVE / INACTIVE / BANNED';
