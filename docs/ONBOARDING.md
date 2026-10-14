# 온보딩 가이드

처음 합류했다면 이 문서를 위에서부터 그대로 따라 하세요. 막히면 30분 안에 디스코드에 공유합니다.

## 1. 준비물 설치

| 도구 | 버전 | 확인 명령 |
|---|---|---|
| JDK | 17 | `java -version` |
| IntelliJ IDEA | 최신 | |
| Docker Desktop | 최신 (실행 상태여야 함) | `docker info` |
| Git | 최신 | `git --version` |

JDK 17이 없으면 IntelliJ `File → Project Structure → SDK → Download JDK`에서 17을 받습니다.

## 2. 프로젝트 받기

```bash
git clone https://github.com/SB14-GoS/sb14-monew-team01.git
cd sb14-monew-team01
```

IntelliJ에서 `Open` → 폴더 선택 → **Gradle 프로젝트로 열기**. 오른쪽 아래 진행바(Gradle 동기화)가 끝날 때까지 기다립니다.

- `File → Project Structure → Project SDK`가 **17**인지 확인
- `Settings → Build Tools → Gradle → Gradle JVM`도 **17**

## 3. 실행

**Docker Desktop을 먼저 켭니다.**

- 앱 실행: IntelliJ 상단 실행 목록에서 **MonewApplication (local)** → ▶
  - 처음 실행하면 PostgreSQL·MongoDB 이미지를 받느라 1~2분 걸립니다.
  - 확인: http://localhost:8080/actuator/health → `{"status":"UP"}`
- 전체 테스트: 실행 목록에서 **All Tests** → ▶ (테스트용 DB 컨테이너가 자동으로 뜹니다)

터미널을 쓰고 싶다면:

```bash
./gradlew bootRun   # 앱 실행
./gradlew test      # 테스트
./gradlew build     # 포맷 검사 + 테스트 + 구조 규칙 검사 (PR 올리기 전에 실행)
```

## 4. 작업 시작하기

1. [Projects 보드](https://github.com/orgs/SB14-GoS/projects/1)에서 내 이슈를 찾아 **Assignees에 나를 지정**하고 Status를 `In Progress`로 옮깁니다.
2. 최신 dev에서 브랜치를 만듭니다.
   ```bash
   git checkout dev
   git pull
   git checkout -b feature/#12-user-signup     # feature/#이슈번호-영문요약
   ```
3. 만들 패키지의 `README.md`를 엽니다. 추천 클래스 이름과 규칙이 적혀 있습니다. 첫 클래스를 만들면 그 README는 삭제합니다.

## 5. 개발 순서 (TDD)

한 기능을 아래 세 단계로 나눠 **단계마다 커밋**합니다.

| 단계 | 하는 일 | 커밋 예시 |
|---|---|---|
| Red | 실패하는 테스트를 먼저 작성 | `test: 회원가입 이메일 중복 실패 테스트 추가 #12` |
| Green | 테스트가 통과하는 최소한의 코드 작성 | `feat: 회원가입 이메일 중복 검사 구현 #12` |
| Refactor | 동작은 그대로 두고 코드 정리 | `refactor: 회원가입 검증 로직 메서드 분리 #12` |

- 통합 테스트(실제 DB 필요)는 클래스에 `@IntegrationTest`를 붙입니다. 예시: `src/test/java/com/sprint/monew/support/IntegrationTestInfraTest.java`
- 엔티티는 `BaseEntity`(또는 논리 삭제가 필요하면 `BaseSoftDeleteEntity`)를 상속합니다.

### 커밋할 때 자동으로 일어나는 일

- **코드 자동 정리**: 커밋 직전에 포매터가 Java 파일을 정리합니다. 따로 할 일은 없습니다.
- **메시지 검사**: 첫 줄이 `type: 요약 #이슈번호` 형식이 아니면 커밋이 거부됩니다.
  - type: `feat` `fix` `refactor` `test` `docs` `chore` `style` `infra`

## 6. PR 올리기

```bash
./gradlew build                          # 로컬에서 먼저 통과 확인
git push -u origin feature/#12-user-signup
```

GitHub에서 **Compare & pull request** → base가 `dev`인지 확인 → 템플릿을 채웁니다.

- 본문에 `close #12`를 적으면 머지될 때 이슈가 자동으로 닫힙니다.
- 아직 작업 중이면 **Draft**로 엽니다.
- PR을 열면 자동으로:
  - CI가 빌드·테스트를 돌리고 커버리지를 코멘트로 남깁니다. ❌가 뜨면 `Details`에서 원인을 확인합니다.
  - 라벨이 붙고 보드에서 `In Review`로 이동합니다.
  - 리뷰어(김예준·김어진)가 지정됩니다.

## 7. 리뷰 반영과 머지

- 리뷰 코멘트를 반영해 커밋을 추가하고 다시 push합니다. (새 커밋이 올라가면 기존 승인이 풀리니 다시 요청합니다)
- **승인 1개 + CI 통과** 후 `Rebase and merge`로 머지합니다. 다른 머지 방식은 막혀 있습니다.
- 머지 후 브랜치는 자동 삭제됩니다. 로컬도 정리합니다.
  ```bash
  git checkout dev && git pull
  git branch -d feature/#12-user-signup
  ```

### dev가 바뀌어 충돌이 났다면

```bash
git fetch origin
git rebase origin/dev
# 충돌 파일을 고친 뒤
git add .
git rebase --continue
git push --force-with-lease      # 내 브랜치에만 사용. dev/main에는 절대 쓰지 않습니다
```

혼자 해결이 어려우면 rebase를 멈추고(`git rebase --abort`) 디스코드에 공유합니다.

## 8. 자주 겪는 문제

| 증상 | 해결 |
|---|---|
| `Cannot connect to the Docker daemon` | Docker Desktop을 켜고 다시 실행 |
| 앱 실행 시 MongoDB 연결 실패, health가 `DOWN` | 컨테이너 일부만 떠 있는 상태. `docker compose down` 후 다시 실행 |
| 포트 5432·27017 사용 중 | 다른 프로젝트의 DB 컨테이너를 종료 (`docker ps`로 확인) |
| 커밋이 `커밋 메시지 형식이 맞지 않습니다`로 거부됨 | 첫 줄을 `feat: 요약 #이슈번호` 형식으로 |
| CI에서 `spotlessCheck` 실패 | `./gradlew spotlessApply` 후 커밋 |
| CI에서 `ArchitectureTest` 실패 | 다른 모듈의 `api` 패키지가 아닌 클래스를 참조한 것. 메시지에 나온 클래스를 `api`로 옮기거나 공개 인터페이스를 사용 |
| IntelliJ에서 클래스가 빨간색 | Gradle 탭 → 🔄 Reload All Gradle Projects |
| Java 버전 오류 | Project SDK와 Gradle JVM을 17로 |

## 참고 문서

- [협업 규칙 (CONTRIBUTING.md)](../CONTRIBUTING.md)
- [요구사항](https://codeit.notion.site/MongoDB-PostgreSQL-v2-3276fd228e8d81c89265f6b5469efbb0)
- [API 명세 (Swagger)](http://sprint-project-1196140422.ap-northeast-2.elb.amazonaws.com/sb/monew/api/swagger-ui/index.html)
