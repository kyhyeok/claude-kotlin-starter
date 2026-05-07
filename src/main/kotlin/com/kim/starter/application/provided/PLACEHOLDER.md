# application/provided

이 패키지는 "외부에 제공하는" Use Case 인터페이스를 정의한다.

예시:
```kotlin
interface MemberRegister {
    fun register(request: RegisterCommand): Member
    fun activate(memberId: Long): Member
}
```

## 작성 규칙

- 인터페이스만 둔다. 구현은 `application/` 루트에 `*Service`로.
- 컨트롤러(adapter.webapi)는 이 인터페이스만 의존한다 (구현체 직참조 금지).
- 입출력은 도메인 객체 또는 Command/Query record로.

이 파일은 패키지가 비어있을 때만 존재. 첫 인터페이스를 추가하면 삭제.
