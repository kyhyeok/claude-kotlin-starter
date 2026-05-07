# spring-kotlin-starter

사내 표준 Kotlin + Spring Boot starter kit.

## 스택

| 영역 | 선택 |
|---|---|
| 언어 | Kotlin 2.3.21 (K2 컴파일러, JVM 25 bytecode 지원) |
| JVM | Java 25 (LTS) |
| 프레임워크 | Spring Boot 4.0.6 |
| 빌드 | Gradle (Kotlin DSL) + version catalog |
| 영속성 | JPA + jOOQ + Flyway + PostgreSQL |
| 인증 | OAuth2 Resource Server (HS256, Refresh Token via Redis) |
| 테스트 | JUnit 5 + AssertJ + MockK + Testcontainers + ArchUnit |
| 문서 | Spring REST Docs → OpenAPI 3 → Swagger UI (`restdocs-api-spec`) |
| 정적 분석 | ktlint (detekt은 GA 출시 시 재도입 — ADR-0007) |
| 로깅 | Logback + JSON 구조 (`logstash-logback-encoder`) |
| CI | GitHub Actions |

## 이어서 작업하기 (세션 재개)

```bash
cd "/Users/kimyoonhyeok/QR 베이스가 될 프로젝트/spring-kotlin-starter"
cat NEXT_STEPS.md          # 다음 작업 단계 확인
./gradlew clean build      # 빌드가 여전히 통과하는지 검증
git log --oneline | head   # 어디까지 했는지 확인
```

→ 자세한 가이드는 [NEXT_STEPS.md](./NEXT_STEPS.md) 참고.

## 5분 안에 시작하기 (새 프로젝트 시작 시)

```bash
# 1. clone
git clone <this-repo> my-new-project
cd my-new-project

# 2. 패키지 base name 일괄 치환 (com.kim.starter → com.yourorg.yourapp)
#    *.kt, *.java(package-info), build.gradle.kts(group+jOOQ target),
#    settings.gradle.kts, application*.yml + 디렉토리 이동(git mv)
./scripts/rename-package.sh com.kim.starter com.yourorg.yourapp --dry-run  # 변경 대상 미리보기
./scripts/rename-package.sh com.kim.starter com.yourorg.yourapp            # 실제 치환
#    상세 결정사항: adr/0014-rename-package-script.md

# 3. 로컬 인프라 (PostgreSQL + Redis)는 Spring Boot가 자동 기동
#    docker desktop 실행만 해두면 됨

# 4. 빌드 + 실행
./gradlew clean build      # 컴파일 + 단위/통합 테스트 통과 검증
./gradlew bootRun          # 새 group으로 부팅

# 5. 검증
curl http://localhost:8080/health                # {"status":"UP",...}
open http://localhost:8080/swagger-ui.html       # OpenAPI 3 문서
```

## 디렉토리 구조 (헥사고날)

```
src/main/kotlin/com/kim/starter/
├── StarterApplication.kt
├── domain/                  ← 도메인 모델 (Spring 의존 0)
│   └── shared/              ← VO 등 공통 도메인 객체
├── application/             ← Use Case
│   ├── provided/            ← 외부에 제공할 인터페이스 (controller가 의존)
│   └── required/            ← 외부에 요구할 포트 (Repository, JwtIssuer)
├── adapter/                 ← Inbound/Outbound 어댑터
│   ├── webapi/              ← REST 컨트롤러, ApiControllerAdvice
│   ├── persistence/         ← JPA Repository, jOOQ Query Processor
│   └── security/            ← JWT, SecurityConfig, PasswordEncoder
└── config/                  ← 횡단 Bean (Clock, Jackson, ...)
```

**의존 방향: `adapter → application → domain`** (역방향 금지, ArchUnit으로 강제).

## 주요 명령

```bash
./gradlew build            # 컴파일 + 테스트 + 정적분석
./gradlew test             # 테스트만
./gradlew ktlintFormat     # 포매팅 자동 적용
./gradlew detekt           # 정적분석
./gradlew openapi3         # REST Docs로 OpenAPI 스펙 생성 (test 통과 후)
./gradlew bootRun          # 로컬 서버 (docker-compose 자동 기동)
```

## 다음 작업 (Day 2 이후)

- [x] 첫 Flyway 마이그레이션(V1 members) + jOOQ codegen 활성화
- [x] JWT 어댑터 (`NimbusJwtIssuer`, `RedisRefreshTokenStore`)
- [x] Auth API (`POST /auth/{register,login,refresh,logout}`)
- [x] 통합 테스트 (Testcontainers + MockMvcTester)
- [x] Swagger UI 호스팅 통합 (REST Docs → OpenAPI 3 → `/swagger-ui.html`)
- [x] `rename-package.sh` 스크립트 (새 프로젝트 시작 자동화 — ADR-0014)
- [ ] CI 강화 (GitHub Actions + Codecov + Dependabot)
- [ ] Micrometer + Prometheus 검증
- [ ] detekt 2.0 GA 모니터링 (ADR-0007 재실행)

## 문서

- [개발가이드.md](./개발가이드.md) — 헥사고날 의존 방향, 패키지 규칙
- [adr/](./adr/) — Architecture Decision Records

## 라이선스

내부용.
