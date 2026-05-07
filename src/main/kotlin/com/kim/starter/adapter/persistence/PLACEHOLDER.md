# adapter/persistence

JPA Repository, jOOQ Query Processor 등 영속성 어댑터를 두는 곳.

## 작성 규칙

- application/required의 포트 인터페이스를 구현한다.
- JPA 엔티티는 domain 패키지의 객체와 분리하거나 같이 쓰는 것은 팀 결정.
  - 단일 모델: 도메인 객체에 `@Entity` (빠른 시작, 작은 트레이드오프)
  - 엄격한 헥사고날: 도메인 객체와 Persistence 모델 분리 (변환 비용 발생)
- 단순 CRUD: JPA. 복잡한 검색/리포팅: jOOQ.

이 파일은 패키지가 비어있을 때만 존재. 첫 어댑터를 추가하면 삭제.
