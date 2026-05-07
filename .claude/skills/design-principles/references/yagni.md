# YAGNI — You Aren't Gonna Need It

> **이 문서는 언제 읽나**: SKILL.md 결정 트리에서 "추상화 추가", "라이브러리 래퍼", "좀 더 유연하게", "혹시 모를 케이스" 같은 신호가 잡혔을 때.
>
> **출처**: Kent Beck × Chet Hendrickson의 C3 프로젝트 대화에서 시작 (eXtreme Programming 핵심 실천). Martin Fowler의 [bliki/Yagni](https://martinfowler.com/bliki/Yagni.html)가 정전 정리.

**Fowler의 정전 정의**:
> *"Always implement things when you actually need them, never when you just foresee that you need them."*
> "**실제로 필요할 때** 구현하라. _필요할 것 같다_고 미리 만들지 마라."

> *"Even if you are totally sure that you will need a feature later on, don't implement it now. Usually, it will turn out either you don't need it after all, or what you actually need is quite different from what you foresaw needing earlier."*

---

## 1. Fowler가 정리한 "추정 기능(presumptive feature)의 4가지 비용"

미리 만들면 다음 비용을 _전부_ 부담해야 한다:

1. **Cost of build** — 만드는 데 든 시간 그 자체
2. **Cost of delay** — 그 시간에 정말 필요한 기능이 늦어진 비용
3. **Cost of carry** — 만들어진 코드가 _이후 모든 변경_에서 따라 움직여야 하는 비용 (리팩토링, 테스트, 리뷰 부담)
4. **Cost of repair** — 막상 쓸 때 모양이 달라서 _다시 고쳐야_ 하는 비용

이 넷 중 (3) Cost of carry가 가장 음험하다. 안 쓰는 코드도 매 변경마다 컴파일·테스트·리뷰 대상이다.

---

## 2. 통과 신호

- 한 번만 쓰는 헬퍼는 호출 지점에 _인라인_으로 둔다
- props 인터페이스는 _현재_ 필요한 것만 받는다
- 함수 시그니처에 미사용 옵션 매개변수가 없다
- generic `<T>`은 _두 번째 호출자_가 등장한 시점에 도입한다

## 3. 위반 신호

- "옵션이 늘어날 수 있으니" config 객체로 받는 props (현재 옵션 1개)
- 사용처가 없는 `<T extends ...>` 제네릭
- 빈 인터페이스 / 미사용 export
- "나중에 다른 백엔드도 붙일 수 있게" 같은 가정 기반 plugin/strategy 레이어
- 한 곳에서만 쓰이는 strategy 패턴
- _발생 불가능한 시나리오_에 대한 try/catch
- "혹시 모를 케이스"를 위한 `?? []` / `?? {}` 남발 (해당 변수가 사실상 절대 nullable이 아닐 때)

---

## 4. YAGNI에 대한 가장 흔한 오해 정정

YAGNI는 다음 중 어느 것도 _아니다_:

### ❌ "리팩토링을 하지 마라"

리팩토링은 _현재 코드를 단순하게_ 만드는 작업이므로 YAGNI 적용 대상이 _아니다_.

> Fowler 원문: *"Refactoring isn't a violation of yagni because refactoring makes the code more malleable."*

### ❌ "테스트를 쓰지 마라"

YAGNI는 _자기-테스트 코드(SelfTestingCode)와 지속적 통합_ 위에서만 안전하게 작동한다. 테스트 없이 YAGNI를 적용하면 변경에 대한 안전망이 사라져 리팩토링이 막힌다.

### ❌ "에러 처리를 빼라"

_현재 발생 가능한_ 에러는 처리한다. YAGNI는 _발생할지 모르는_ 에러에 대한 처리만 막는다.

| 처리해야 함 | 처리하지 않음 (YAGNI) |
|---|---|
| 네트워크 실패, 타임아웃 (실제 발생) | "혹시 우주선이 떨어지면" 같은 경우 |
| 사용자 입력 검증 (실제 들어옴) | 절대 호출 안 되는 분기의 fallback |
| API 4xx/5xx 응답 (실제 옴) | 타입 시스템이 이미 막은 케이스의 try/catch |

### ❌ "설계를 하지 마라"

YAGNI는 _추측 기능_을 막는 것이지 _현재 요구사항을 위한 설계_를 막는 것이 아니다. 현재 요구사항을 위해 필요한 인터페이스·분리·추상화는 모두 정당하다.

---

## 5. YAGNI 적용 시 자기 점검 질문

추상화·옵션·분기·확장 포인트를 추가하기 _전_ 다음 질문:

1. _현재_ 호출자/사용자가 이걸 정말 쓰는가?
2. 만약 안 쓴다면, 6개월 안에 쓸 _명확한 일정/요구사항_이 있는가?
3. 없다면, 6개월 뒤에 _현재의 가정대로_ 필요할 거라는 보장이 있는가?
4. 만약 6개월 뒤 다르게 필요해지면 — 지금 만든 것을 _버리고 새로 만들기_ 쉬운가?

3번이 _no_라면 추가하지 마라. 4번이 _yes_라면 그제서야 만들어도 좋다 (하지만 그래도 보통은 만들지 않는 게 낫다).

---

## 6. 관련 충돌 — YAGNI는 거의 항상 우선

| 충돌 | 결과 |
|---|---|
| YAGNI ↔ DRY | YAGNI 우선 (Rule of Three까지 기다린다) |
| YAGNI ↔ OCP | YAGNI 우선 (확장 포인트는 두 번째 사용처에) |
| YAGNI ↔ ISP | YAGNI 우선 (인터페이스 분리는 두 번째 클라이언트에) |
| YAGNI ↔ "현재 발생 가능한 에러 처리" | 에러 처리 우선 (YAGNI는 _추정_ 에러에만) |

---

## 7. 코드에서 YAGNI 위반을 발견했을 때

발견한 위반을 _수정_할지는 다음 기준:

- 사용자 요청 _범위 내_의 코드 → 수정한다
- 사용자 요청 _범위 밖_의 기존 코드 → **언급만 하고 수정하지 않는다** (CLAUDE.md "외과적 변경" 원칙)

리뷰 보고에는 "📌 범위 밖" 섹션에 별도 PR 권장으로 기록.
