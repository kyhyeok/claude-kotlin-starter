# ADR-0005: JSON 구조 로깅 (logstash-logback-encoder)

- 상태: Accepted
- 일시: 2026-05-06

## 맥락

운영 환경에서 로그 검색/분석 시 plain text는 한계. ELK / Datadog / Grafana Loki 모두 JSON 로그를 가정.

## 결정

- **로컬 (`local` profile)**: 사람이 읽기 좋은 컬러 콘솔 포맷.
- **그 외 모든 환경**: `LogstashEncoder` 기반 JSON 한 줄 구조.
- MDC에 `traceId`, `spanId`, `userId` 자동 포함.
- `customFields`로 `app` 이름 기본 포함.

### 활용 예시 (서비스 코드)

```kotlin
import org.slf4j.MDC

MDC.put("userId", member.id.toString())
try {
    log.info("Member registered: {}", member.email)  // JSON 출력 시 userId가 자동 포함
} finally {
    MDC.remove("userId")
}
```

## 결과

- 로컬 가독성 ↔ 운영 검색성 모두 확보.
- Datadog/Loki와 즉시 호환.
- `traceId`/`spanId`는 Micrometer Tracing이 채움 (의존성 추가 시).

## 거부된 대안

- **plain text 통일**: 운영 검색 어려움.
- **OpenTelemetry log SDK 직접 사용**: 현 시점에서는 Logback + JSON으로 충분.
