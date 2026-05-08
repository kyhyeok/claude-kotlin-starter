# ADR-0019: 보안 헤더·CORS·Rate limiting — 운영 직전 표준 게이트

- 상태: Accepted
- 일시: 2026-05-08
- 관련: ADR-0001(헥사고날), ADR-0012(Auth 슬라이스), ADR-0016(Prometheus), ADR-0018(starter scope)

## 맥락

Day 4-2까지 starter kit은 인증·관측성·테스트 인프라를 갖췄지만 운영 직전에 반드시 박아야 하는 보안 레이어가 비어 있었다. fork된 서비스마다 같은 보안 설정을 반복하지 않도록 **도메인 무관 인프라**로 starter에 박는다(ADR-0018 §판단 룰 통과 — "도메인 단어 없는 인프라").

## 결정

### 1. 보안 헤더 (SecurityConfig.headers { })

5종 응답 헤더를 Spring Security `HeadersConfigurer` DSL로 박음:

| 헤더 | 값 | 근거 |
|---|---|---|
| `Content-Security-Policy` | `default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'; img-src 'self' data:` | Swagger UI가 인라인 스크립트·스타일 사용 → `'unsafe-inline'` 허용 |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | HTTPS 가정 (운영은 reverse proxy/Ingress TLS terminate) |
| `X-Frame-Options` | `DENY` | Spring Security default와 동일, 명시적 박제 |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | 표준 보수적 정책 |
| `Permissions-Policy` | `camera=(), microphone=(), geolocation=()` | 불필요한 브라우저 API 비활성 |

`permissionsPolicy` DSL이 Spring Security 7에서 deprecated → `HeaderWriter` 람다(`response.setHeader`)로 직접 박음.

### 2. CORS (CorsConfig + CorsProperties)

- `@ConfigurationProperties("app.cors")` — `allowedOrigins / allowedMethods / allowedHeaders / exposedHeaders / maxAge`
- 기본값: `allowedOrigins: [http://localhost:3000]` (sample). **fork된 서비스가 실제 도메인으로 교체 필수.**
- `allowedOrigins`가 빈 list이면 `CorsConfiguration.allowedOrigins = null` → CORS 완전 비활성.
- Spring Security `.cors { }` + `CorsConfigurationSource` bean 자동 연결.

### 3. Rate limiting (RateLimitInterceptor + RateLimitProperties)

**라이브러리 선택**: 자체 Redis 고정 윈도우 카운터 — 의존 0 추가, Redis는 이미 박혀 있음(ADR-0012).

**거부된 후보**:
- Bucket4j: Spring Boot 4 BOM 외부 의존 추가 필요. starter kit의 가벼움 원칙(ADR-0018) 위배.
- resilience4j-ratelimiter: in-memory 단일 인스턴스 전용. 스케일 아웃 시 각 인스턴스가 독립 카운터.

**구현**:
- Redis 키: `ratelimit:<path>:<ip>:<minute>` — 분 단위 고정 윈도우
- INCR + EXPIRE(60s) — 새 윈도우마다 TTL 자동 리셋
- IP 추출: `X-Forwarded-For` 첫 토큰 우선, fallback `remoteAddr`
- 초과 시: HTTP 429 + `Retry-After: 60`
- `HandlerInterceptor` 채택 — Security 필터체인 이후 Spring MVC 레이어에서 동작

**기본 규칙** (fork된 서비스가 도메인 진화 시 조정):
- `/auth/login`: IP당 분당 10회
- `/auth/register`: IP당 분당 5회

**테스트 전략**:
- 통합 테스트에서는 비활성(`app.ratelimit.rules: []`) — 공유 IP로 카운터 누적 시 다른 테스트에 429 전파
- `RateLimitInterceptorTest` 단위 테스트(MockK)로 핵심 로직(통과 / 429 차단 / IP 추출 / 경로 매칭) 검증

## 결과

- 새 파일: `adapter/security/CorsConfig.kt`, `CorsProperties.kt`, `adapter/security/ratelimit/RateLimitConfig.kt`, `RateLimitInterceptor.kt`, `RateLimitProperties.kt`
- 수정: `SecurityConfig.kt`(headers + cors 추가), `application.yml`(app.cors + app.ratelimit), `application-test.yml`(rate limit 비활성)
- 테스트: `api/health/GET_specs.kt`(헤더 5종), `api/auth/login/OPTIONS_specs.kt`(CORS preflight), `adapter/security/ratelimit/RateLimitInterceptorTest.kt`(단위 4건)
- 총 테스트: 45 (이전 41 + 신규 4)
