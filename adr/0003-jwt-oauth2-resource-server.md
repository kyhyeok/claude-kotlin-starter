# ADR-0003: JWT 인증 — OAuth2 Resource Server (HS256) + Redis Refresh Token

- 상태: Accepted
- 일시: 2026-05-06

## 맥락

자체 `JwtAuthFilter` 구현에서 흔히 발견되는 결함:
- 쿠키 분기 검증 누락
- 매 요청 DB 조회
- Refresh Token subject에 가변 식별자(`loginId` 등) 주입 → access token 재발급 시 타입 변환 실패
- 시크릿이 `application.yml`에 평문 commit

## 결정

- **Spring Security OAuth2 Resource Server** 사용 (자체 필터 구현 금지).
- **알고리즘: HS256** (대칭키, 단일 서비스 베이스. RS256은 마이크로서비스 확장 시 마이그레이션).
- **Access Token**: 15분 (PT15M).
- **Refresh Token**: 7일 (P7D), Redis에 저장 (TTL 자동 만료, 로그아웃 시 즉시 폐기).
- **시크릿 주입**: 환경변수 `JWT_SECRET` 또는 Vault.

### 포트/어댑터

```kotlin
// application/required (포트, 도메인이 의존)
interface JwtIssuer { ... }
interface RefreshTokenStore { ... }

// adapter/security (어댑터)
class NimbusJwtIssuer : JwtIssuer
class RedisRefreshTokenStore : RefreshTokenStore
```

## 결과

- Refresh Token은 Spring 표준에 없어 직접 구현. 포트 패턴으로 격리.
- 토큰 검증을 Spring Security가 처리 → 컨트롤러는 `Authentication` 또는 `@AuthenticationPrincipal` 사용.

## 거부된 대안

- **자체 JwtFilter**: 위 결함 패턴의 재발 위험.
- **RS256 (비대칭)**: 단일 서비스에는 과함. 마이크로서비스 확장 시 변경.
- **세션 기반 인증**: stateful, 수평 확장 어려움.
