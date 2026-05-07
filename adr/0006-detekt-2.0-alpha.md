# ADR-0006: detekt 2.0.0-alpha.3 채택

- 상태: **Superseded by ADR-0007** (2026-05-06)
- 일시: 2026-05-06

> ⚠️ 이 결정은 빌드 검증 후 거부되었습니다.
> alpha 버전을 starter kit이 채택하면 fork되는 모든 프로젝트가 위험을 상속받습니다.
> 또한 `dev.detekt:2.0.0-alpha.3` plugin이 Gradle Plugin Portal에서 resolve 불가했습니다.
> 최종 결정은 **ADR-0007: detekt 임시 제거** 참고.

## 맥락

starter kit이 Kotlin 2.3.21을 사용 (ADR-0002). 첫 빌드에서 detekt 에러:

```
detekt was compiled with Kotlin 2.0.10 but is currently running with 2.3.21.
This is not supported.
```

detekt release 조사 결과:

| detekt 버전 | 지원 Kotlin | 출시일 |
|---|---|---|
| 1.23.8 (최신 stable) | 2.0.21 | 2025-02 |
| 2.0.0-alpha.1 | 2.2.20 | 2025-10 |
| 2.0.0-alpha.2 | 2.3.0 | 2026-01 |
| **2.0.0-alpha.3** | **2.3.21** | **2026-04-24** |

Kotlin 2.3.21을 지원하는 detekt은 alpha 릴리스뿐이다.

## 결정

**detekt 2.0.0-alpha.3을 채택**한다.

ADR-0002에서 cutting-edge 스택(Kotlin 2.3, JVM 25, Spring Boot 4)을 선택한 결과로, 정적 분석 도구도 같이 cutting-edge로 따라갈 수밖에 없다.

## 위험과 완화

| 위험 | 완화 |
|---|---|
| alpha 릴리스의 룰/설정 변경 | `buildUponDefaultConfig = true` 유지, baseline 파일 활용 |
| 1.23.x → 2.0 마이그레이션 가이드 미흡 | 첫 빌드 후 발생하는 룰 위반은 baseline에 일괄 등록 |
| GA 지연 시 alpha 장기 사용 | detekt 2.0 GA 출시 즉시 업그레이드, ADR 갱신 |
| plugin DSL/configuration 변경 가능 | starter의 detekt.yml은 minimal하게 유지 |

## 거부된 대안

- **detekt 1.23.8 유지**: Kotlin 2.3 환경에서 빌드 자체가 실패. 사용 불가.
- **detekt 제거, ktlint만 사용**: 룰 분석을 포기 → 코드 품질 가드레일 약화.
- **Kotlin 2.0.21로 다운그레이드**: ADR-0002의 Java 25 결정과 충돌. 거부.

## 후속 조치

- detekt 2.0 GA 릴리스 모니터링 (release page watch).
- GA 출시 시 즉시 업그레이드 + 이 ADR Superseded로 갱신.
