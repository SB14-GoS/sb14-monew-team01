# 📰 모뉴 (MoNew)

**관심 있는 주제의 뉴스를 한곳에서 읽는 서비스입니다.**

모뉴는 네이버 뉴스와 한국경제·조선일보·연합뉴스 RSS에서 매시간 기사를 수집합니다. 관심사를 구독하면 해당 주제의 키워드와 맞는 기사만 모아 볼 수 있습니다.

![Java 17](https://img.shields.io/badge/Java_17-007396?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot 4.1](https://img.shields.io/badge/Spring_Boot_4.1-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Spring Batch](https://img.shields.io/badge/Spring_Batch-6DB33F?style=flat-square&logo=spring&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-47A248?style=flat-square&logo=mongodb&logoColor=white)
![AWS ECS](https://img.shields.io/badge/AWS_ECS-232F3E?style=flat-square&logo=amazonwebservices&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white)
[![JaCoCo 테스트 커버리지 87.54%](https://img.shields.io/badge/JaCoCo%20coverage-87.54%25-brightgreen?style=flat-square)](https://github.com/SB14-GoS/sb14-monew-team01/pull/195#issuecomment-6723138502)

[서비스 체험](https://monew.project-archive.site/) · [시스템 설계](#시스템-설계) · [로컬 실행](#로컬-실행) · [개발 문서](#개발-문서)

![모뉴 메인 화면](docs/images/main.png)

## 모뉴에서 할 수 있는 일

1. **관심사를 구독합니다.** 기존 관심사에서 ‘인공지능’을 구독하거나, 원하는 주제가 없으면 ‘AI’, ‘생성형 AI’ 같은 키워드를 넣어 새 관심사를 등록합니다.
2. **기사를 찾습니다.** 제목과 요약을 키워드로 검색하고, 관심사·출처·발행 기간으로 필터링할 수 있습니다.
3. **기사에 의견을 남기고 알림을 받습니다.** 기사에 댓글을 달고 댓글에 좋아요를 누를 수 있습니다. 구독한 주제에 새 기사가 올라오거나 내 댓글에 좋아요가 달리면 알림을 받습니다.
4. **내 활동을 돌아봅니다.** 구독 중인 관심사, 작성한 댓글, 좋아요, 최근 본 기사를 한 화면에서 확인합니다.

<details>
<summary>기능별 화면 보기</summary>

**관심사 구독**

![모뉴 관심사 구독 화면](docs/images/feature-interest.png)

**댓글과 좋아요**

![모뉴 댓글과 좋아요 화면](docs/images/feature-comment.png)

**알림**

![모뉴 알림 화면](docs/images/feature-notification.png)

**활동 내역**

![모뉴 활동 내역 화면](docs/images/feature-activity.png)

</details>

## 기술 스택

| 분야 | 기술 |
| --- | --- |
| 백엔드 | Java 17, Spring Boot 4.1, Spring MVC, JPA·Hibernate, QueryDSL, Spring Batch |
| 프론트엔드 | 제공된 React 정적 번들 |
| 데이터 | PostgreSQL, MongoDB Atlas, Amazon S3 |
| 인프라·배포 | Amazon ECS(EC2), ECR, RDS, CloudFront, Route 53, GitHub Actions |
| 모니터링 | Spring Boot Actuator, Micrometer, Amazon CloudWatch Logs |
| 테스트·품질 | JUnit Jupiter, Mockito, Testcontainers, ArchUnit, JaCoCo, Spotless |
| 개발 환경 | Gradle, Docker, Docker Compose, IntelliJ IDEA |
| 협업·API 문서 | GitHub, Notion, CodeRabbit, Swagger UI |

## 시스템 설계

### 시스템 구성

![CloudFront 요청 경로, ECS의 일곱 모듈, 커밋 후 이벤트, 수집과 백업 배치, 데이터 저장소와 로그 흐름](docs/images/system-architecture.png)

[시스템 구성 Excalidraw 원본](docs/system-architecture.excalidraw)

<details>
<summary>기사 수집·백업과 정리 배치 일정 보기</summary>

| 기본 실행 시각 (KST) | 작업 |
| --- | --- |
| 매시 정각 | 네이버 뉴스 API와 RSS 피드 36개에서 기사를 가져옵니다. 관심사 키워드로 기사를 연결하고 원본 URL로 중복을 걸러냅니다. |
| 매일 01:00 | 전날 저장한 기사와 관심사 연결을 일별 JSON으로 S3에 백업합니다. |
| 매일 03:00 | 확인한 지 7일이 지난 알림을 정리합니다. |
| 매일 04:00 | 탈퇴한 지 하루가 지난 회원을 물리 삭제합니다. |

복구는 지정한 기간의 S3 백업을 읽어 PostgreSQL에 없는 기사만 원본 URL 기준으로 복원합니다.

</details>

### 데이터 관계도 (ERD)

![사용자·관심사·기사와 연결된 테이블, MongoDB 활동 내역 문서의 관계](docs/images/erd.png)

[ERD Excalidraw 원본](docs/erd.excalidraw)

활동 화면은 MongoDB에서 사용자별 문서 한 건을 읽습니다. 문서에는 구독 중인 관심사와 댓글·좋아요·읽은 기사 각각의 최근 10건까지를 담습니다.

### 배포 흐름

![dev 대상 PR의 자동 검사와 팀원 리뷰, main 반영 이후 ECR 이미지 푸시와 ECS 배포 흐름](docs/images/deployment.png)

[배포 흐름 Excalidraw 원본](docs/deployment.excalidraw)

### 코드 아키텍처

기능별로 모듈을 나누고 각 모듈 안에서 웹 요청 처리, 애플리케이션 로직, 도메인 모델, 외부 연동을 분리합니다. 모듈 사이의 호출은 공개 `api`를 통해 이뤄지며, ArchUnit 테스트가 계층 의존성과 모듈 간 순환 의존을 검사합니다.

<details>
<summary>전체 구성과 계층 구조 보기</summary>

#### 1. 전체 구성

```text
sb14-monew-team01/
├── .github/workflows/                 # CI와 배포 워크플로
├── docs/                              # 프로젝트 문서와 다이어그램
├── src/
│   ├── main/
│   │   ├── java/com/sprint/monew/
│   │   │   ├── user/                  # 사용자
│   │   │   ├── interest/              # 관심사와 구독
│   │   │   ├── article/               # 기사 수집과 조회
│   │   │   ├── comment/               # 댓글과 좋아요
│   │   │   ├── notification/          # 알림
│   │   │   ├── activity/              # 활동 내역
│   │   │   ├── backup/                # 기사 백업과 복구
│   │   │   └── global/                # 공통 설정, 예외, 로깅, 메트릭
│   │   └── resources/                 # 환경별 설정과 정적 웹 파일
│   └── test/                          # 단위·통합·아키텍처 테스트
├── build.gradle                       # 의존성과 빌드 설정
├── compose.yaml                       # 로컬 PostgreSQL·MongoDB 실행
└── Dockerfile                         # 백엔드 컨테이너 이미지
```

#### 2. 계층 구성 · 기사 모듈 예시

```text
src/main/java/com/sprint/monew/article/
├── api/                               # 다른 모듈에 공개하는 계약과 이벤트
│   └── ArticleQueryApi.java
├── web/                               # HTTP 요청과 응답
│   └── ArticleController.java
├── application/                       # 서비스 로직과 트랜잭션
│   ├── ArticleService.java
│   └── port/NewsSourceClient.java     # 외부 뉴스 수집 인터페이스
├── domain/                            # 기사 모델과 저장소 인터페이스
│   ├── Article.java
│   └── repository/ArticleQueryRepository.java
└── infra/                             # QueryDSL 조회와 외부 뉴스 연동 구현
    └── ArticleQueryRepositoryImpl.java
```

**기사 검색 요청 흐름**

`ArticleController` → `ArticleService` → `ArticleQueryRepository` (`ArticleQueryRepositoryImpl`) → PostgreSQL

</details>

## 로컬 개발

### 로컬 실행

JDK 17과 Docker가 필요합니다. Docker를 실행한 뒤 아래 명령을 실행하세요. 기본 `local` 프로필에서는 Docker Compose가 PostgreSQL과 MongoDB를 자동으로 시작합니다.

```bash
git clone https://github.com/SB14-GoS/sb14-monew-team01.git
cd sb14-monew-team01
./gradlew bootRun
```

브라우저에서 [로컬 서비스](http://localhost:8080/)를 열거나, 아래 명령으로 서버 상태와 기사 출처 목록을 확인할 수 있습니다.

```bash
curl -fsS http://localhost:8080/actuator/health
curl -fsS http://localhost:8080/api/articles/sources
```

첫 번째 요청은 `status`가 `UP`인 응답을, 두 번째는 `NAVER`, `HANKYUNG`, `CHOSUN`, `YEONHAP`가 포함된 JSON 배열을 반환합니다. 로컬 서버의 API 문서는 [Swagger UI](http://localhost:8080/swagger-ui/index.html)에서 볼 수 있습니다.

네이버 뉴스 수집이나 S3 연동이 필요하면 `.env.example`을 `.env`로 복사한 뒤 해당 기능에 필요한 `NAVER_*`, `AWS_*` 값을 채웁니다. 기본 실행에는 별도 DB 접속 정보가 필요하지 않습니다. [환경변수 예시](.env.example) · [온보딩 가이드](docs/ONBOARDING.md)

### 테스트와 커버리지

**전체 테스트 커버리지 87.54%** (JaCoCo, 2026년 11월 9일 GitHub Actions 측정). [CI 커버리지 결과](https://github.com/SB14-GoS/sb14-monew-team01/pull/195#issuecomment-6723138502)

```bash
./gradlew test   # 단위·통합 테스트와 커버리지 리포트 생성
./gradlew build  # 포맷 검사, 테스트, 빌드
```

통합 테스트는 Testcontainers로 PostgreSQL과 MongoDB를 실행합니다. 로컬 커버리지 리포트는 `build/reports/jacoco/test/html/index.html`에서 확인할 수 있습니다.

## 개발 문서

| 구분 | 문서 |
| --- | --- |
| 요구사항과 API | [프로젝트 요구사항](https://codeit.notion.site/MongoDB-PostgreSQL-v2-3276fd228e8d81c89265f6b5469efbb0) · [코드잇 제공 API 명세](http://sprint-project-1196140422.ap-northeast-2.elb.amazonaws.com/sb/monew/api/swagger-ui/index.html) |
| 개발 가이드 | [공통 예외 처리](src/main/java/com/sprint/monew/global/exception/EXCEPTION_HANDLING.md) · [CI](.github/workflows/ci.yml) · [CD](.github/workflows/cd.yml) |
| 협업 | [개발 규칙](#개발-규칙) · [팀 노션](https://various-smoke-6b3.notion.site/Notion-SB14-MoNew-Team-ec6ff2cbac808318a03e015d40fea585) |

### 개발 규칙

<details>
<summary>브랜치·PR·코드 규칙 보기</summary>

| 구분 | 규칙 |
| --- | --- |
| 브랜치 | 최신 `dev`에서 `feature/#12-user-signup`, `fix/#이슈번호-내용` 형식으로 작업합니다. 운영 긴급 수정은 `hotfix/#이슈번호-내용`에서 작업하고 `dev` 반영 후 `main`에 반영합니다. |
| 이슈 | 작업 이슈를 본인에게 할당하고 보드를 `In Progress`로 옮깁니다. 작업 유형·도메인 라벨과 마일스톤을 지정합니다. |
| 커밋 | `타입: 요약 #이슈번호` 형식을 사용합니다. 기능 개발은 Red → Green → Refactor 단계별로 커밋합니다. |
| PR | `dev`로 PR을 열고 본문에 `Closes #이슈번호`를 적습니다. CI 통과와 [CODEOWNERS](.github/CODEOWNERS) 1명 승인 후 **Rebase and merge** 합니다. 브랜치를 최신화할 때는 `origin/dev`에 rebase합니다. |
| 릴리스 | `dev` → `main` PR에서 리뷰를 받은 뒤 팀장이 **fast-forward**로 반영합니다. `main` 푸시가 운영 배포를 시작합니다. |
| 자동 검사 | Gradle 빌드가 Git hooks를 등록합니다. 커밋 전에 Spotless 포맷을 적용하고 메시지 형식을 확인합니다. 푸시 전 `./gradlew build` 검사는 Docker를 사용합니다. |
| 코드 | 요청·응답 DTO를 API 명세와 맞춥니다. 엔티티는 `BaseEntity` 또는 `BaseSoftDeleteEntity`를 상속하고, 논리 삭제 대상의 조회에는 삭제 조건을 반영합니다. 다른 모듈의 엔티티는 ID로 참조합니다. |
| 스타일과 설정 | 클래스는 PascalCase, 변수·메서드는 camelCase, 상수는 UPPER_SNAKE_CASE, 패키지는 소문자를 사용합니다. API 키와 DB 비밀번호는 환경 변수로 관리합니다. |
| 팀 협업 | 30분 이상 막히면 팀에 공유하고, 디스코드 메시지는 확인 표시를 남깁니다. 리뷰 요청을 받은 뒤 24시간 안에 리뷰합니다. |

</details>

## 팀

SB14 중급 프로젝트 · Team 01 · 2026.10.20–2026.11.09

| 팀원 | 담당 작업 |
| --- | --- |
| [김예준](https://github.com/kim-yejunn) · 팀장 | 일정 관리·코드 리뷰, 공통 모듈, 기사 조회 기록, 알림 조회, 활동 내역, 기사 백업·복구 |
| [김어진](https://github.com/rladjwls02) | 뉴스 기사 기능, 네이버·RSS 기사 수집 및 배치, 코드 리뷰 |
| [조예준](https://github.com/joyejun) | 댓글·좋아요, 알림 정리 배치 |
| [신상엽](https://github.com/Smil-limS) | 관심사·구독, 알림 생성, AWS 인프라와 배포 자동화 |
| [배원기](https://github.com/wongibae) | 사용자 기능, 탈퇴 회원 정리 배치, 프로젝트 문서 |
