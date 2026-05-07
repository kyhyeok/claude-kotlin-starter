---
name: design-principles
description: SOLID, DRY, KISS, YAGNI 설계 원칙의 정전 참조. 코드 작성, 코드 리뷰, 리팩토링, 새 컴포넌트/모듈/훅/추상화 생성, 아키텍처 결정, PR 리뷰, 또는 architecture-reviewer 서브에이전트 호출 시 반드시 이 스킬을 참조한다. "리뷰", "리팩토링", "원칙", "SOLID", "DRY", "KISS", "YAGNI", "추상화", "중복", "responsibility", "개선" 같은 키워드가 등장하거나, 컴포넌트·훅·유틸 함수를 새로 만들거나 기존 코드를 분리·통합하는 작업이라면 적용 대상이다. 사용자가 명시적으로 부르지 않더라도 위 작업 중에는 이 스킬을 자동으로 적용하라.
---

# 설계 원칙 (SOLID · DRY · KISS · YAGNI)

> **이 스킬이 무엇인가**: 코드 작성·리뷰·리팩토링 시 적용하는 5+3 원칙의 _정전(canonical) 참조_.
> **이 스킬이 _아닌_ 것**: 프로젝트별 파일 경로/패턴 매핑 — 그것은 `architecture-reference.md`(있다면)에 있다.
>
> **우선순위 (충돌 시)**: `CLAUDE.md` > 프로젝트별 매핑 문서 > 이 스킬.

---

## 1. 언제 이 스킬을 펼치는가

다음 중 **하나라도** 해당되면 본문과 필요한 `references/*.md`를 읽고 적용한다:

- 새 컴포넌트·훅·유틸·모듈을 만든다
- 기존 코드를 분리·통합·이동한다 (리팩토링)
- 추상화·인터페이스·제네릭을 도입하려 한다
- PR/diff를 리뷰한다
- `architecture-reviewer` 서브에이전트가 호출된다
- 사용자가 "원칙대로", "이거 SOLID 위반인가", "중복 같은데" 같은 질문을 한다

작업이 단순 오타 수정·로그 추가·이름 변경 같은 _외과적 변경_이라면 이 스킬을 펼치지 않는다 (CLAUDE.md의 외과적 변경 원칙 우선).

---

## 2. 원칙 한 줄 요약 (기억용 인덱스)

| 원칙 | 한 줄 정의 | 깊은 참조 |
|---|---|---|
| **S**RP | 한 모듈은 _한 액터_의 변경 요구만 받는다 | `references/solid.md` |
| **O**CP | 새 동작은 _기존 코드 수정_ 없이 _추가_로 구현한다 | `references/solid.md` |
| **L**SP | 하위 타입은 상위 타입 자리에 의미적으로 대체 가능해야 한다 | `references/solid.md` |
| **I**SP | 클라이언트는 안 쓰는 인터페이스 의존을 강요받지 않는다 | `references/solid.md` |
| **D**IP | 고수준은 추상에, 추상은 세부에 의존하지 않는다 | `references/solid.md` |
| **DRY** | 모든 _지식_은 단일하고 모호하지 않은 표현을 가진다 | `references/dry.md` |
| **KISS** | 단순한 해법이 영리한 해법보다 낫다 | `references/kiss.md` |
| **YAGNI** | _추정_ 기능은 만들지 말고 _실제 필요할 때_ 만든다 | `references/yagni.md` |

---

## 3. 결정 트리 — 어느 references 파일을 읽을지

작업이 다음에 해당하면 _그_ 참조 파일을 먼저 읽는다:

- 책임 분리·역할·인터페이스 설계 → `references/solid.md`
- "이거 중복 아닌가"·헬퍼 추출·공통화 → `references/dry.md`
- 추상화 추가·라이브러리 래퍼·"좀 더 유연하게" → `references/yagni.md` (먼저), 그래도 추상화가 정당하면 `references/solid.md` (OCP)
- "이 코드 너무 복잡한데"·재구현·자체 store → `references/kiss.md`
- PR 전체 리뷰 → 5개 파일 모두 훑되, **§5 신호 빠른 점검**부터 본다

여러 파일을 동시에 읽지 마라. 한 번에 한 파일.

---

## 4. 원칙 간 충돌 — 우선순위 규칙

여러 원칙이 충돌할 때 다음 규칙으로 푼다. 더 자세한 이유는 각 references 파일에.

| 충돌 | 우선 | 이유 |
|---|---|---|
| DRY ↔ YAGNI | **YAGNI** | 잘못된 추상화의 비용 ≫ 중복의 비용 (Sandi Metz) |
| OCP ↔ YAGNI | **YAGNI** | 확장 포인트는 _두 번째 사용처_가 등장할 때 만든다 |
| SRP ↔ KISS | **KISS** (작은 범위에서) | 분리 자체가 새 추상화·새 파일을 만든다 |
| DRY ↔ KISS | **KISS** (조건부) | 추상화가 호출자의 이해 비용을 늘린다면 중복이 낫다 |
| ISP ↔ YAGNI | **YAGNI** | 두 번째 클라이언트의 _다른 요구_가 드러난 시점에 분리 |

상위 우선순위(CLAUDE.md): **안정성 > 유지보수성 > 보안 > 성능**. 이 순서를 깨면서 원칙을 적용하지 마라.

---

## 5. 신호 빠른 점검 (PR 리뷰용)

코드를 _훑을 때_ 다음 신호가 보이면 해당 references 파일로 들어가 정밀 점검한다:

### 🚨 높음 — 거의 항상 위반

- 컴포넌트가 generated 코드/raw fetch/`apiClient`를 _직접_ import (DIP) → `solid.md`
- `as any` / `as unknown as X` 캐스팅 (LSP) → `solid.md`
- generated 코드를 _수동 편집_ (OCP) → `solid.md`
- 같은 비즈니스 규칙이 _두 곳_에 명백히 정의 (DRY) → `dry.md`
- 발생 불가능한 시나리오에 대한 try/catch (YAGNI) → `yagni.md`

### ⚠️ 중간 — 맥락 확인 필요

- 한 컴포넌트가 fetch + 변환 + 표시 + 결정을 모두 수행 (SRP) → `solid.md`
- 사용처가 _하나뿐_인 추상화/제네릭/Strategy (YAGNI) → `yagni.md`
- 거대한 만능 props 컴포넌트 (ISP) → `solid.md`
- 라이브러리 기본 동작을 _다시 구현_ (KISS) → `kiss.md`
- "혹시 모를" `?? []` / `?? {}` 남발 (YAGNI) → `yagni.md`

### 💡 낮음 — 개선 제안

- 한 번만 쓰이는 헬퍼가 별도 파일로 분리됨 (YAGNI) → `yagni.md`
- 옵션 객체로 받는 props인데 현재 옵션이 1개 (YAGNI) → `yagni.md`
- 우연한 일치를 묶은 추상화 (DRY 오용) → `dry.md`

---

## 6. 적용 체크리스트

### 코드 작성 _전_

- [ ] 사용자 요청 범위가 명확한가? 범위 밖 변경을 하려 하지 않는가?
- [ ] 새 추상화·모듈·패턴을 만들기 전에 _이미 있는 것_을 먼저 찾았는가?
- [ ] 추가하려는 것이 _현재 호출자_가 정말 필요로 하는가? "혹시 모를"은 아닌가?

### 코드 작성 _중_

- [ ] 발생 _가능한_ 에러만 처리하고 있는가? 발생 불가능한 시나리오는 처리하지 않는가?
- [ ] props·인자가 _현재_ 호출자가 쓰는 것만 받는가?
- [ ] `as any`·`as unknown as X`가 없는가?
- [ ] 같은 비즈니스 규칙을 두 곳에 적지 않았는가?

### 코드 작성 _후_ (자기 리뷰)

- [ ] 새로 만든 파일이 한 가지 액터의 변경만 받는가? "and"가 들어가는가?
- [ ] 만든 추상화에 _현재_ 사용처가 2개 이상 있는가? 1개라면 인라인하라
- [ ] generic `<T>`이 있다면 _현재_ 호출자 중 두 군데 이상이 다른 타입을 넣는가? 아니라면 제거
- [ ] 라이브러리 기본 동작을 _재구현_한 부분이 있는가? 라이브러리 사용으로 되돌려라

---

## 7. 리뷰 보고 형식 (architecture-reviewer 출력 템플릿)

```
## 아키텍처 리뷰 결과

### 🚨 높음 (반드시 수정)
- [원칙명] <file>:<line>
  - 위반: <구체적 사실>
  - 정전 위배: <어떤 정의에서 어떻게 어긋나는지 — references/*.md 인용>
  - 권장: <어느 패턴이 정답인지>
  - 사용자 요청 범위 내인가? (yes/no)

### ⚠️ 중간 (권장 수정)
- ...

### 💡 낮음 (선택)
- ...

### 📌 범위 밖 (수정 X — 언급만)
- 기존 코드의 위반. 별도 PR 권장.
```

**중요**: _사용자 요청 범위 밖_ 위반은 **언급만** 하고 수정하지 않는다. CLAUDE.md "외과적 변경" 원칙.

---

## 8. 자주 발생하는 적용 실수

다음은 _이 스킬을 잘못 적용한_ 사례다. 자기 점검에 사용하라.

- **"DRY 광신"** — 코드 모양만 같은 두 함수를 묶는다. DRY는 _지식_의 중복이지 _코드_의 중복이 아니다. → `references/dry.md` §1
- **"추측 OCP"** — 미래를 위해 Strategy/Factory를 미리 만든다. OCP는 _두 번째 사용처가 왔을 때_ 적용. → `references/solid.md` §1.2
- **"SRP 분쇄기"** — 한 파일을 5개로 쪼개고 props drilling 4단을 만든다. SRP는 _액터_ 단위지 코드 길이 단위가 아니다. → `references/solid.md` §1.1
- **"YAGNI 핑계로 검증 생략"** — 발생 가능한 에러까지 안 처리한다. YAGNI는 _추정_ 기능에만 적용. → `references/yagni.md` §3
- **"KISS 핑계로 SRP 무시"** — 한 파일에 fetch/변환/표시/라우팅을 다 욱여넣는다. KISS는 _불필요한_ 복잡함 회피이지 본질적 분리 회피가 아니다. → `references/kiss.md` §2

---

## 9. 원전 출처 (인용 시 사용)

| 원칙 | 1차 출처 |
|---|---|
| SOLID 두문자어 | Robert C. Martin, "Design Principles and Design Patterns" (2000); 정리 Michael Feathers |
| SRP | Tom DeMarco, *Structured Analysis and Systems Specification* (1979) → Martin이 "변경의 이유"로 재정의 |
| OCP | Bertrand Meyer, *Object-Oriented Software Construction* (1988) |
| LSP | Barbara Liskov, OOPSLA 1987; Liskov & Wing 1994년 형식화 |
| ISP / DIP | Robert C. Martin, *Agile Software Development: Principles, Patterns, and Practices* (2003) |
| DRY | Andy Hunt & Dave Thomas, *The Pragmatic Programmer* (1999) |
| KISS | Kelly Johnson, Lockheed Skunk Works, 1960년대 |
| YAGNI | Kent Beck × Chet Hendrickson(C3, XP); Martin Fowler bliki/Yagni |
| Rule of Three | Martin Fowler, *Refactoring* (1999) |
| "Wrong abstraction" | Sandi Metz, "The Wrong Abstraction" (2016) |
