# SOLID — 5원칙 상세

> **이 문서는 언제 읽나**: SKILL.md의 §3 결정 트리에서 SOLID 관련 신호가 잡혔을 때, 또는 PR 리뷰의 §5 "신호 빠른 점검"에서 SOLID 위반이 의심될 때.
>
> **출처**: Robert C. Martin, "Design Principles and Design Patterns" (2000), *Agile Software Development: Principles, Patterns, and Practices* (2003). 두문자어 정리 Michael Feathers.

SOLID는 Martin이 "나쁜 설계의 3대 증상"을 막기 위해 묶은 다섯 원칙이다:

- **Rigidity** (경직성) — 한 곳을 바꾸면 너무 많은 곳이 따라 바뀐다
- **Fragility** (취약성) — 한 곳을 바꾸면 엉뚱한 곳이 깨진다
- **Immobility** (부동성) — 다른 곳에 재사용하려 해도 떼어낼 수 없다

각 원칙은 _이 셋 중 어떤 증상을 막는가_의 관점에서 보면 더 잘 이해된다.

---

## 1.1 S — Single Responsibility Principle (SRP)

**정전 정의** (Robert C. Martin):
> *"A class should have one, and only one, reason to change."*
> "클래스는 변경되어야 할 이유를 단 하나만 가져야 한다."

**핵심 오해 정정**: SRP는 "한 가지 일만 한다"가 아니다. **"변경의 이유가 하나"**다. 즉 _누가_ 그 모듈의 변경을 요청하는가의 관점에서 본다 (회계팀이 보내는 변경과 인사팀이 보내는 변경이 같은 클래스에 떨어진다면 SRP 위반).

**막는 증상**: Rigidity. 책임이 섞여 있으면 한 책임의 변경이 다른 책임의 코드를 흔든다.

### 통과 신호

- 모듈이 _하나의 액터(역할/요청자)_의 변경 요구만 받는다
- "이 파일은 무엇을 하는가?"에 한 문장으로 답할 때 "and"가 들어가지 않는다
- 변경 요구가 들어왔을 때 영향받는 파일이 1~2개로 좁혀진다

### 위반 신호

- 한 함수가 fetch + 변환 + 표시 + 라우팅 + 인증 검사를 다 한다
- 한 컴포넌트가 데이터 페칭과 비즈니스 결정을 둘 다 갖는다
- 파일 설명에 "그리고", "또한"이 두 번 이상 등장한다
- "이 파일을 누가 바꾸나?"에 답할 때 서로 다른 팀/도메인이 두 개 이상 나온다

### 예 — TypeScript/React

**위반**:
```ts
function UserDashboard() {
  const [users, setUsers] = useState([]);
  useEffect(() => {
    fetch('/api/users').then(r => r.json()).then(data => {
      // 변환 로직
      const formatted = data.map(u => ({ ...u, fullName: `${u.first} ${u.last}` }));
      // 권한 검사
      const visible = formatted.filter(u => u.role !== 'admin' || isAdmin);
      setUsers(visible);
    });
  }, []);
  return <ul>{users.map(...)}</ul>;
}
```
→ fetch + 변환 + 권한 + 표시 4가지 액터.

**개선**:
```ts
function UserDashboard() {
  const { data: users } = useUsersQuery(); // 데이터 + 변환 + 권한은 훅 안
  return <ul>{users.map(...)}</ul>;        // 컴포넌트는 표시만
}
```

### 과도 적용 금지

SRP를 극단으로 밀면 _함수 하나당 클래스 하나_ 같은 안티패턴이 나온다. SRP는 **응집도(cohesion)**의 도구이지 _분해(decomposition)_의 강박이 아니다. 메서드 1개짜리 클래스가 다섯 개 생기고 결국 어디선가 그것들을 모두 주입받아 합치고 있다면 — 분리 자체가 잘못된 추상화일 수 있다.

---

## 1.2 O — Open/Closed Principle (OCP)

**정전 정의** (Bertrand Meyer 1988, Martin이 다듬음):
> *"Software entities should be open for extension, but closed for modification."*
> "소프트웨어 개체는 확장에는 열려 있어야 하고, 수정에는 닫혀 있어야 한다."

**의도**: 새 동작을 추가할 때 _이미 잘 동작하는 코드_를 건드리지 않고 새 코드를 _추가_하는 방식으로 구현하라. 기존 코드 수정은 회귀(regression) 위험을 만든다.

**막는 증상**: Fragility. 잘 돌아가던 곳을 건드려야 새 기능이 들어가는 구조라면 매번 새 버그가 생긴다.

### 통과 신호

- 새 동작 추가가 _기존 코드 수정_ 없이 _새 파일/함수 추가_만으로 가능
- 라이브러리/프레임워크가 제공하는 확장 포인트(hook, plugin, strategy, resolver, ky `beforeRequest`, RHF resolver 등)를 사용한다
- 생성된 코드(generated)·서드파티 컴포넌트 소스를 직접 수정하지 않는다

### 위반 신호

- 새 케이스마다 기존 함수의 `switch`/`if-else`에 분기를 추가한다
- 라이브러리/생성 코드 소스를 직접 편집한다
- 한 곳을 고쳤을 때 컴파일/타입은 통과해도 _다른 호출자가 다른 의미로_ 동작하게 된다

### YAGNI와의 함정 — 가장 자주 잘못 적용되는 부분

OCP는 _확장 포인트를 미리 만들어두라_는 뜻이 **아니다**. "미래에 다른 결제 방식이 올 수 있으니 Strategy 패턴으로 추상화하자" 같은 추측은 YAGNI 위반이다.

**규칙**: 확장 포인트는 _두 번째 사용처가 등장할 때_ 만든다. 한 가지 동작만 있는데 추상 인터페이스부터 짜지 마라.

---

## 1.3 L — Liskov Substitution Principle (LSP)

**정전 정의** (Barbara Liskov 1987 OOPSLA, 후일 Liskov & Wing 1994년 형식화):
> *"Subtypes must be substitutable for their base types without altering the correctness of the program."*
> "하위 타입은 상위 타입의 자리에 들어가도 프로그램의 정확성을 깨지 않아야 한다."

**의도**: 상속/구현 계약을 _문법_ 수준이 아니라 _의미·행동_ 수준에서 지키라. 시그니처가 같다고 같은 타입이 아니다.

**막는 증상**: Fragility. 다형성을 쓰는 호출자가 _구체 타입에 따라_ 분기해야 한다면 다형성이 깨진 것이다.

### 프론트엔드 해석 (TypeScript/React)

- `Pick<...>` / `Omit<...>`로 props를 좁힐 때 의미가 일관되어야 한다
- 컴포넌트의 prop 인터페이스를 좁힌 뒤 그 컴포넌트가 _상위 인터페이스의 기대_를 깨면 안 된다 (e.g. `Button`을 받는 곳에 `onClick`이 무시되는 변종을 넣지 않기)
- generated DTO를 그대로 props로 노출하지 말고 _필요한 모양_으로 좁혀서 전달

### 위반 신호

- `as any` / `as unknown as X` 캐스팅 (특히 generated 타입 우회)
- 하위 타입에서 상위 타입의 메서드를 throw로 막아버림 (`throw new Error("not supported")`)
- 호출자가 `instanceof` / `'kind' in obj`로 분기를 강제당함
- props가 옵셔널인지 필수인지의 의미가 컴포넌트마다 다름

### 예

**위반**:
```ts
interface Bird { fly(): void; }
class Penguin implements Bird {
  fly() { throw new Error("Penguins can't fly"); } // ❌ 계약 파괴
}
```

**개선**:
```ts
interface Bird { /* 비행 능력은 별도 인터페이스로 */ }
interface Flyable { fly(): void; }
class Penguin implements Bird {} // Flyable 구현 X
```

---

## 1.4 I — Interface Segregation Principle (ISP)

**정전 정의** (Robert C. Martin):
> *"Clients should not be forced to depend upon interfaces they do not use."*
> "클라이언트는 자신이 사용하지 않는 인터페이스에 의존하도록 강요받아서는 안 된다."

**의도**: 거대한 만능 인터페이스 하나보다 _역할 단위_로 작은 인터페이스 여러 개로 쪼갠다.

**막는 증상**: Rigidity. 만능 인터페이스의 한 메서드만 바뀌어도 그것을 안 쓰는 클라이언트까지 재컴파일/재배포된다.

### 프론트엔드 해석

- 한 컴포넌트가 form/list/detail 모드 세 가지를 모두 받는 거대 props → **모드별 분리** (또는 Discriminated Union)
- 한 훅이 너무 많은 옵션 객체를 받는다면 → 사용 시나리오별로 분리

### 위반 신호

- props에 mode/variant 같은 분기 prop이 있고, mode마다 _서로 다른_ prop들이 옵셔널로 추가됨 (Discriminated Union 없이)
- 컴포넌트가 받는 props 중 _각 호출 지점에서 절반 이상이 항상 `undefined`_

### YAGNI와의 균형

인터페이스를 _미리_ 잘게 쪼개지 마라. 두 번째 클라이언트가 _덜 쓴다는 사실_이 드러났을 때 쪼갠다. (충돌 시 YAGNI 우선)

---

## 1.5 D — Dependency Inversion Principle (DIP)

**정전 정의** (Robert C. Martin):
> *"High-level modules should not depend on low-level modules. Both should depend on abstractions."*
> *"Abstractions should not depend on details. Details should depend on abstractions."*

**의도**: 고수준 정책(비즈니스/UI)이 저수준 세부(HTTP 클라이언트, DB, 외부 SDK)에 직접 의존하지 않게 한다. 둘 다 _도메인 추상화_에 의존시킨다.

**막는 증상**: Immobility. 저수준이 갈아치워질 때 고수준까지 함께 갈리는 것을 막는다.

### 프론트엔드 해석 (전형적인 layered baseline)

```
컴포넌트 (고수준)
    ↓
features/<도메인> 훅 (도메인 추상화)
    ↓
generated API 클라이언트 (저수준 세부)
```

역방향이나 단축은 금지.

### 통과 신호

```ts
// ✅ 컴포넌트는 도메인 훅에만 의존
import { useUsersQuery } from '@/features/users';
```

### 위반 신호 — 이건 거의 항상 🚨 높음

```ts
// ❌ 컴포넌트가 generated 직접 import
import { getUsers } from '@/lib/api/generated/users';

// ❌ 컴포넌트가 raw fetch / apiClient 직접 사용
const data = await apiClient.get('/users').json();
const data = await fetch('/api/users').then(r => r.json());
```

이런 위반이 보이면: 컴포넌트는 도메인 훅(`features/<도메인>`)을 통하도록 수정. 도메인 훅이 없다면 만들어야 한다 (단, 사용자 요청 범위 내일 때만).

---

## 검증·해소 요약

| 신호 | 어느 원칙 | 심각도 |
|---|---|---|
| 컴포넌트 → generated 직접 import | DIP | 🚨 높음 |
| `as any` / `as unknown as X` | LSP | 🚨 높음 |
| generated 코드 수동 편집 | OCP | 🚨 높음 |
| 한 컴포넌트가 fetch+변환+표시+결정 | SRP | ⚠️ 중간 |
| 거대 만능 props 컴포넌트 | ISP | ⚠️ 중간 |
| "혹시" 미리 만든 Strategy/Factory | OCP (YAGNI 위반) | ⚠️ 중간 |
