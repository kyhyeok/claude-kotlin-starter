# KISS — Keep It Simple, Stupid

> **이 문서는 언제 읽나**: SKILL.md 결정 트리에서 "이 코드 너무 복잡한데", "재구현", "자체 store/wrapper" 같은 신호가 잡혔을 때.
>
> **출처**: Kelly Johnson, Lockheed Skunk Works, 1960년대. 원문은 쉼표 없는 _"Keep it simple stupid"_.

"엔지니어가 멍청하다"는 뜻이 아니라 — _전장에서 평범한 정비공이 단순한 도구만으로 고칠 수 있어야 한다_는 의미였다. 소프트웨어로 번역하면: **단순한 해법이 복잡한 해법보다 낫다.** "영리해 보이는" 해법이 _이해하기 쉬운_ 해법보다 우선될 가치는 거의 없다.

> Einstein 보강:
> *"Everything should be made as simple as possible, but not simpler."*
> 단순함을 너무 밀면 _필요한 분기·검증·에러 처리_까지 빠진다. KISS는 "복잡함 회피"이지 "필수 처리 생략"이 아니다.

---

## 1. 통과 신호

- 라이브러리 기본 동작을 그대로 신뢰한다 (React Query 캐시, ky retry, RHF resolver, Zod 검증)
- shadcn 컴포넌트를 그대로 쓰고 forwardRef를 직접 만들지 않는다
- HTTP 클라이언트 인스턴스 1개 (`client.ts`) — 도메인별 클라이언트를 늘리지 않는다
- 복잡한 알고리즘 대신 명확한 brute-force가 있다면 brute-force를 쓴다 (성능 문제가 _측정으로_ 확인되기 전엔)

## 2. 위반 신호

- 이유 없이 Zustand / Recoil / Jotai를 추가 (서버 상태는 React Query만으로 충분한데 클라이언트 store를 끌어옴)
- React Query를 한 번 더 감싼 `useApiQuery` 같은 추상화 (정당화 없음)
- 단순 `useState`로 충분한 곳에 `useReducer` / Context API
- 정규식·재귀·고차함수 체이닝으로 한 줄에 다 욱여넣은 코드 (이해 비용 ≫ 줄 수 절약)
- "유연성"을 위해 옵션 객체로 받는 props (현재 호출자는 1개)
- ky의 retry/timeout/auth를 처음부터 직접 구현

---

## 3. KISS의 한계 — 단순함은 공짜가 아니다

KISS를 _핑계로_ 다음을 생략하면 안 된다:

- 발생 _가능한_ 에러 처리 (YAGNI는 _발생 불가능한_ 시나리오에만 적용)
- 본질적 책임 분리 (SRP는 KISS와 충돌해도 본질적 분리는 유지)
- 사용자 입력 검증 (보안·안정성은 단순함보다 우선)

### KISS 정당화 체크

라이브러리·기존 패턴 위에 _새 추상화_를 얹기 _전_ 다음 질문:

1. 기본 동작으로는 정말 안 되는가? (먼저 시도해봤는가)
2. 이 추상화의 사용처가 _현재_ 2개 이상인가? (1개면 YAGNI 위반)
3. 추상화 이름만 보고 호출자가 동작을 정확히 추측할 수 있는가?
4. 6개월 뒤의 내가/팀원이 이 코드를 _바로_ 이해할 수 있는가?

세 가지 이상 _no_라면 추상화를 만들지 말고 라이브러리 기본 사용으로 돌아가라.

---

## 4. 관련 충돌

- **KISS ↔ DRY**: 추상화가 호출자의 이해 비용을 늘린다면 중복이 낫다 (KISS 우선)
- **KISS ↔ SRP** (좁은 범위): 분리 자체가 새 추상화·새 파일을 만든다면 KISS 우선
- **KISS ↔ SRP** (넓은 범위): 한 모듈이 여러 액터를 섬기게 되면 SRP 우선

---

## 5. KISS 보강 격언 (필요 시 인용)

- Einstein: *"Everything should be made as simple as possible, but not simpler."*
- Antoine de Saint-Exupéry: *"Perfection is achieved not when there is nothing more to add, but when there is nothing left to take away."*
- Bjarne Stroustrup: *"Make simple tasks simple."*
