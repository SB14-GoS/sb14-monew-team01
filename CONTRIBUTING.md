# 협업 규칙

## 브랜치 전략

| 브랜치 | 용도 | 병합 대상 |
|---|---|---|
| `main` | 배포 버전. 직접 커밋 금지 | `dev` → `main` PR |
| `dev` | 개발 통합 브랜치 (기본 브랜치) | `feature/*`, `fix/*` PR |
| `feature/#이슈번호-기능` | 기능 개발 | → `dev` |
| `fix/#이슈번호-내용` | 버그 수정 | → `dev` |
| `hotfix/#이슈번호-내용` | 배포 후 긴급 수정 | → `main` & `dev` |

예: `feature/#12-user-signup`

## 작업 흐름

1. 이슈를 자신에게 할당하고 GitHub Projects 보드에서 `In Progress`로 옮깁니다.
2. 최신 `dev`에서 브랜치를 만듭니다.
3. TDD로 구현합니다. **Red → Green → Refactor 단계마다 커밋**합니다.
4. `dev`로 PR을 열고 본문에 `close #이슈번호`를 적습니다.
5. **김예준·김어진 중 1명 이상 Approve** 후 Merge합니다. (`.github/CODEOWNERS`로 강제, 본인 PR은 다른 한 명이 승인)
6. Merge는 **Rebase and merge만** 사용합니다. (Squash·Merge commit 비활성화, 선형 히스토리 강제)
7. 충돌이 나면 디스코드에 공유하고, 아래처럼 rebase로 해결한 뒤 다시 푸시합니다.

```bash
git fetch origin
git rebase origin/dev
# 충돌 해결 후
git add .
git rebase --continue
git push --force-with-lease
```

## 커밋 컨벤션

```
<type>: <요약> #이슈번호

<본문: 무엇을, 왜>
```

| type | 사용 |
|---|---|
| `feat` | 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 동작 변경 없는 구조 개선 |
| `test` | 테스트 추가·수정 |
| `docs` | 문서 |
| `chore` | 빌드·설정·의존성 |
| `style` | 포맷 변경 (코드 동작 변경 없음) |

TDD 커밋 예시:

```
test: 회원가입 이메일 중복 실패 테스트 추가 #12      ← Red
feat: 회원가입 이메일 중복 검사 구현 #12            ← Green
refactor: 회원가입 검증 로직 메서드 분리 #12        ← Refactor
```

## 이슈 규칙

- 제목 형식: `[Feat] 회원가입`, `[Fix] ...`, `[Chore] ...`
- 라벨: 유형(`feat` `fix` `chore` `test` `docs` `infra` `batch`) + 도메인(`domain:*`)
- 마일스톤: M1 기반 세팅 → M2 핵심 API → M3 배치·백업·심화 → M4 배포·문서

## 코드 컨벤션

- 변수·메서드: camelCase / 클래스: PascalCase / 상수: UPPER_SNAKE_CASE
- 패키지: 소문자
- API 응답은 Swagger 명세와 필드명·타입을 정확히 맞춥니다.
- 민감 정보(API 키, DB 비밀번호)는 환경 변수로만 다루고 커밋하지 않습니다.

## 팀 규칙

- 30분 이상 막히면 팀원·멘토·주강사에게 공유합니다.
- 디스코드 메시지를 확인하면 이모지로 표시합니다.
- 코드 리뷰는 요청 후 24시간 안에 합니다.
