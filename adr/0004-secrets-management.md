# ADR-0004: 시크릿 관리

- 상태: Accepted
- 일시: 2026-05-06

## 맥락

`application.yml`에 JWT 시크릿/DB 비밀번호를 평문 commit하는 관행이 흔하다. 한 번 git에 들어가면 history rewrite 없이는 영구 노출.

## 결정

### 1. application.yml에는 placeholder만

```yaml
jwt.secret: ${JWT_SECRET:dev-only-secret-change-me-in-production-must-be-at-least-32-bytes}
```

- `:` 뒤의 default 값은 **로컬 개발용**으로 명백히 표시.
- 운영/스테이징은 환경변수 또는 Vault에서 주입.

### 2. 환경별 주입 방법

| 환경 | 방법 |
|---|---|
| 로컬 개발 | `.env.local` (gitignore) → IDE Run Config |
| CI | GitHub Actions Secrets |
| 스테이징/운영 | AWS Secrets Manager / HashiCorp Vault / K8s Secret |

### 3. 시크릿 노출 시 절차

1. 즉시 시크릿 rotate (JWT는 모든 토큰 무효화).
2. git history rewrite는 **하지 않음** (협업자 영향 큼). 대신 노출된 시크릿이 더 이상 유효하지 않음을 보장.
3. 사후 분석 → ADR 갱신.

## 결과

- 모든 새 프로젝트는 첫 commit 전에 `.env.local` 사용 강제.
- pre-commit 훅으로 잠재 시크릿 검출 (`gitleaks`) 도입 권장.

## 거부된 대안

- **`application-local.yml`에 시크릿 commit**: 여전히 git에 들어감.
- **빌드 시 secret bake**: 빌드 산출물에 박혀 더 위험.
