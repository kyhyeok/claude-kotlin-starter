# domain/shared

여러 애그리거트가 공유하는 Value Object를 두는 곳.

예시: `Email`, `PhoneNumber`, `Money`, `UserId` 등.

## 작성 규칙

- 모든 VO는 `@JvmInline value class` 또는 `data class`로 정의
- 생성자에서 invariant 검증 (잘못된 값으로 객체가 만들어질 수 없도록)
- Spring/JPA 의존 0

이 파일은 패키지가 비어있을 때만 존재. 첫 VO를 추가하면 이 파일은 삭제.
