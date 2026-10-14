# 협업 규칙

## 브랜치 전략

| 브랜치 | 용도 | 병합 대상 | 머지 방식 |
|---|---|---|---|
| `main` | 배포 버전. 직접 커밋 금지 | `dev` → `main` | **Fast-forward** (팀장이 반영) |
| `dev` | 개발 통합 브랜치 (기본 브랜치) | `feature/*`, `fix/*` PR | **Rebase and merge** |
| `feature/#이슈번호-기능` | 기능 개발 | → `dev` | Rebase |
| `fix/#이슈번호-내용` | 버그 수정 | → `dev` | Rebase |
| `hotfix/#이슈번호-내용` | 배포 후 긴급 수정 | → `dev` 후 즉시 main 반영 | Rebase |

브랜치별 규칙은 레포 룰셋으로 강제됩니다.

- `dev-rebase-only`: PR + 김예준·김어진 중 1명 승인 + Rebase and merge만 허용
- `main-fast-forward-only`: 관리자만 갱신 가능, 강제 푸시·삭제 금지, 선형 히스토리

main과 dev는 **한 줄 히스토리**를 유지합니다. main은 항상 dev의 과거 지점과 같은 커밋입니다.

### dev → main 반영 (릴리스)

1. `dev` → `main` PR을 열어 변경 내용을 리뷰받습니다.
2. 승인 후 **머지 버튼을 누르지 않고** 팀장이 fast-forward로 반영합니다. PR은 자동으로 Merged 처리됩니다.

```bash
git fetch origin
git push origin origin/dev:main
```


예: `feature/#12-user-signup`

## 작업 흐름

1. 이슈를 자신에게 할당하고 GitHub Projects 보드에서 `In Progress`로 옮깁니다.
2. 최신 `dev`에서 브랜치를 만듭니다.
3. TDD로 구현합니다. **Red → Green → Refactor 단계마다 커밋**합니다.
4. `dev`로 PR을 열고 본문에 `close #이슈번호`를 적습니다.
5. **김예준·김어진 중 1명 이상 Approve** 후 Merge합니다. (`.github/CODEOWNERS`로 강제, 본인 PR은 다른 한 명이 승인)
6. `dev`로의 Merge는 **Rebase and merge만** 사용합니다. (Merge commit 비활성화, 선형 히스토리 강제)
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

## 패키지 구조

도메인 우선 + 경계 규칙 + 외부 연동만 포트/어댑터 (`com.sprint.monew`)

```
{모듈}/                 user · interest · article · comment · notification · activity · backup
├── api/               다른 모듈에 공개하는 계약 (조회 인터페이스, DTO record, 이벤트)
├── web/               Controller, Request/Response DTO, Swagger 문서 인터페이스
├── application/       Service(유스케이스, 트랜잭션), 이벤트 핸들러, 배치
│   └── port/          외부 연동 인터페이스 (article: 뉴스 출처, backup: 파일 저장소)
├── domain/            Entity, 도메인 규칙, Repository
└── infra/             QueryDSL 구현, 외부 API·S3 클라이언트
global/                config · exception · common · web (도메인 로직 금지)
```

규칙 (`ArchitectureTest`가 빌드 때 검사합니다)

- 다른 모듈은 **`api` 패키지만** 참조합니다. 다른 모듈의 Entity·Repository·Service 직접 사용 금지
- 다른 모듈 Entity와 JPA 연관관계를 맺지 않고 **ID로만** 참조합니다.
- `domain`은 `web`·`application`·`infra`에 의존하지 않습니다.
- `web`은 `infra`를 직접 사용하지 않습니다.
- 모듈 간 순환 의존 금지

## 실행

```bash
./gradlew bootRun          # local 프로파일 (PostgreSQL·MongoDB 필요, #2 docker-compose)
./gradlew test             # test 프로파일 (H2)
```

환경 변수는 `.env.example`을 참고합니다.

## 코드 컨벤션

- 변수·메서드: camelCase / 클래스: PascalCase / 상수: UPPER_SNAKE_CASE
- 패키지: 소문자
- API 응답은 Swagger 명세와 필드명·타입을 정확히 맞춥니다.
- 민감 정보(API 키, DB 비밀번호)는 환경 변수로만 다루고 커밋하지 않습니다.

## 팀 규칙

- 30분 이상 막히면 팀원·멘토·주강사에게 공유합니다.
- 디스코드 메시지를 확인하면 이모지로 표시합니다.
- 코드 리뷰는 요청 후 24시간 안에 합니다.
