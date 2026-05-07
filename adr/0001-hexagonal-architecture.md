# ADR-0001: 헥사고날 아키텍처 + 도메인 모델 패턴 채택

- 상태: Accepted
- 일시: 2026-05-06

## 맥락

흔히 반복되는 결함:
- DTO ↔ JPA 엔티티 경계가 흐려져 권한 우회 가능
- Service 인터페이스가 형식적 장식이 되어 DIP가 무너짐
- 컨트롤러가 `JpaRepository`를 직접 의존해 영속성 결합

## 결정

헥사고날 + 도메인 모델 패턴을 표준으로 채택한다.

```
adapter → application(provided/required) → domain
```

ArchUnit `ArchitectureTest`로 의존 방향을 컴파일/테스트 단계에서 강제한다.

## 결과

- 새 기능마다 `provided`/`required` 인터페이스 작성 비용 발생 (영구).
- 컨트롤러는 인터페이스만 의존, 구현체 직참조 금지.
- 도메인 객체는 Spring 의존 0 (JPA 어노테이션만 허용).

## 거부된 대안

- **Layered (Controller/Service/Repository)**: 표면적 분리만. 영속성이 도메인까지 올라옴.
- **Clean Architecture (4계층)**: 작은 팀에는 과함. 헥사고날의 3계층으로 충분.
