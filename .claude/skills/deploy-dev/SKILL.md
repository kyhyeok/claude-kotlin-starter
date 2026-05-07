---
name: deploy-dev
description: 현재 브랜치를 dev 환경에 배포합니다. 아래 단계를 순서대로 실행하세요.
allowed-tools: Bash, AskUserQuestion
---

0. 현재 브랜치명을 확인:
```bash
git branch --show-current
```
- 이 값을 `CURRENT_BRANCH`로 기억하세요.
- 만약 현재 브랜치가 `dev`이면 "이미 dev 브랜치입니다. 머지 없이 push만 진행합니다."를 출력하고 Step 1을 건너뛰세요.

1. dev 브랜치로 전환 후 현재 브랜치 머지:
```bash
git checkout dev && git merge $CURRENT_BRANCH
```

2. dev 브랜치를 리모트로 Push:
```bash
git push origin dev
```

3. Jenkins 빌드 트리거:
```bash
curl -s -o /dev/null -w "%{http_code}" -X POST "http://jenkins.tablero.co.kr/job/dev_tablero_new_api/build" --user "ai-claude:11791aa0bf006e2b6315cd016507571875"
```
- 성공 시 HTTP 201 응답이어야 합니다.

4. 원래 브랜치로 복귀:
```bash
git checkout $CURRENT_BRANCH
```

각 단계의 결과를 출력하고, 실패 시 즉시 중단하세요.