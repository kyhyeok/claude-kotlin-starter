# ADR-0010: jOOQ codegen은 Flyway SQL을 DDLDatabase로 직접 파싱한다

- 상태: Accepted
- 일시: 2026-05-07

## 맥락

jOOQ 코드 생성기는 스키마 메타데이터를 어디에서 읽을지 선택해야 한다. starter kit 단계에서 가능한 입력 소스는 세 가지다.

- **A. Live DB**: 동작 중인 PostgreSQL(JDBC)에 연결해 information_schema를 읽는다 — jOOQ 표준 패턴.
- **B. Testcontainers**: codegen 전에 임시 PostgreSQL 컨테이너를 부팅 → Flyway 적용 → 메타데이터 추출 → 컨테이너 폐기.
- **C. DDLDatabase**: `db/migration/V*.sql`을 jOOQ가 in-memory 파서(H2 기반)로 직접 해석.

starter kit은 fork 후 5분 안에 시작 가능해야 하고, CI는 외부 서비스 의존이 적을수록 안정적이다. ADR-0001(헥사고날)에 따라 codegen 산출물은 `adapter/persistence/jooq` 어댑터에만 노출되며 도메인이 의존하지 않는다.

ADR-0009와의 관계: codegen은 read-only지만 옵션 A는 **사람이 띄운** DB에 연결하는 흐름이라 codegen 시점이 DB 가용성에 묶인다. 옵션 C는 DB 자체를 요구하지 않으므로 ADR-0009의 "DB 적용 명령은 사람"과 무관한 정적 작업이 된다.

## 결정

**`org.jooq.meta.extensions.ddl.DDLDatabase`로 `src/main/resources/db/migration/*.sql`을 직접 파싱해 코드 생성한다.**

- 입력 디렉토리: `src/main/resources/db/migration` (Flyway와 동일 — 단일 진실 공급원).
- 정렬: `sort = "flyway"` (Flyway의 V/U/R 우선순위 규칙과 동일).
- 파서 다이얼렉트: `parser.dialect = "POSTGRES"` — V*.sql이 PostgreSQL 문법(`BIGSERIAL`, `TIMESTAMPTZ` 등)을 사용.
- 기본 스키마: `unqualifiedSchema = "PUBLIC"` — V1이 스키마를 명시하지 않음. jOOQ 입력 스키마는 `PUBLIC`으로 매핑.
- 이름 케이스: `defaultNameCase = "lower"` — PostgreSQL의 unquoted identifier는 lowercase 정규화.
- 출력 패키지: `com.kim.starter.adapter.persistence.jooq` (헥사고날 어댑터 안).
- 생성 디렉토리: `build/generated/jooq/main` (clean 시 같이 사라지므로 git ignore 별도 필요 없음).
- 코드 생성기: `org.jooq.codegen.KotlinGenerator`.

codegen 클래스패스에 `org.jooq:jooq-meta-extensions`(jOOQ와 동일 버전)를 추가한다.

## 결과

- 새 프로젝트가 fork한 즉시 `./gradlew jooqCodegen`이 docker 없이 동작.
- CI는 PostgreSQL 서비스 컨테이너를 띄우지 않아도 codegen 가능.
- Flyway 마이그레이션 파일이 jOOQ codegen의 단일 진실 공급원이 되어 "DB와 코드의 동기화"가 명시적.
- `./gradlew clean` 한 번이면 생성 코드까지 같이 사라져 cache 오염 없음.
- DDLDatabase는 H2 기반 파서를 사용하므로 PostgreSQL 전용 기능(고급 인덱스 옵션, 부분 인덱스, 익스텐션, RLS 등) 일부는 미지원 가능. 한계 케이스가 발생하면 본 ADR을 Superseded로 두고 옵션 A로 전환한다.

## 거부된 대안

- **A. Live DB**: 가장 보수적이고 잘 검증되었지만 codegen마다 docker-compose가 떠 있어야 함. starter 단계의 단순함을 해친다. CI에 별도 PostgreSQL 서비스 컨테이너 필요. 한계 케이스가 누적되면 옵션 C에서 A로 승격.
- **B. Testcontainers**: 외부 의존 0과 PostgreSQL 정확성을 동시에 확보하지만 codegen마다 컨테이너 부팅 ~10초 + Gradle task 배관(~50줄). starter kit 단계의 over-engineering. Day 2-4 통합 테스트의 Testcontainers 도입과 별개로 codegen용 컨테이너를 재활용할 가치는 크지 않음.

## 후속 결정 트리거 (옵션 A로 전환할 신호)

다음 중 하나가 발생하면 ADR-0010을 Superseded 처리하고 옵션 A로 전환한다.

- DDLDatabase 파서가 V*.sql을 거부하는 PostgreSQL 문법 도입 (예: 부분 인덱스, GIN/GIST, materialized view, 확장 모듈).
- 생성된 jOOQ 클래스의 컬럼 타입이 실제 PostgreSQL과 어긋나 런타임 캐스팅 오류 발생.
- jOOQ 메타 확장과 Spring Boot 4의 호환성 문제로 codegen이 실패.
