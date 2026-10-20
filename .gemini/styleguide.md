# MoNew 코드 리뷰 가이드

## 언어

- PR 요약과 모든 리뷰 코멘트는 **한국어**로 작성합니다.

## 기술 스택

- Java 17, Spring Boot 4 (Spring Framework 7), Jackson 3, Spring Data JPA, QueryDSL, PostgreSQL, Testcontainers
- 위 버전 기준으로 리뷰합니다. 구버전 API(Spring Boot 2·3, Jackson 2)로 바꾸라는 제안은 하지 않습니다.

## 지적하지 않는 것

- 코드 포맷, 들여쓰기, import 순서: `spotless`(palantir-java-format)가 커밋 시 자동 적용합니다.
- 테스트 커버리지 수치: CI의 JaCoCo가 검사합니다.
- 기능에 영향 없는 취향 차이(변수명 선호, 메서드 순서 등)

## 중점적으로 볼 것

### 모듈 경계 (`CONTRIBUTING.md` 패키지 구조)

- 모듈: `user` · `interest` · `article` · `comment` · `notification` · `activity` · `backup`
- 다른 모듈은 `api` 패키지만 참조합니다. 다른 모듈의 Entity·Repository·Service를 직접 사용하면 지적합니다.
- 다른 모듈 Entity와 JPA 연관관계(`@ManyToOne` 등)를 맺지 않고 ID(`UUID`)로만 참조합니다.
- `domain`은 `web`·`application`·`infra`에 의존하지 않습니다. `web`은 `infra`를 직접 사용하지 않습니다.
- `global`에는 도메인 로직을 두지 않습니다.

### 예외 처리

- 도메인 예외는 `MonewException`을 상속하고 `ErrorCode`를 사용합니다.
- 클라이언트 잘못(4xx)인데 500으로 응답되는 경로가 없는지 확인합니다.
- 예외를 삼키거나 원인(cause)을 버리는 코드를 지적합니다.

### 데이터·트랜잭션

- 트랜잭션 범위: 쓰기 메서드의 `@Transactional` 누락, 조회 메서드의 `readOnly` 누락
- 카운트 컬럼(`subscriber_count`, `comment_count`, `view_count`, `like_count`)은 `UPDATE ... SET x = x + 1` 원자 쿼리로 갱신합니다. 엔티티에서 `x++` 후 저장하면 동시 요청 시 갱신이 유실됩니다.
- N+1 쿼리 가능성
- 논리 삭제 대상(`users`, `articles`, `comments`) 조회 시 `deleted_at IS NULL` 필터 누락
- 중복 방지 규칙: 이메일, 기사 원본 링크, 구독, 기사 조회(1인 1회), 댓글 좋아요

### API

- 커서 페이지네이션: `limit + 1`개 조회로 `hasNext` 판단, `cursor` + `after`(보조 커서, 생성 시각) 사용
- 요청 헤더 `Monew-Request-User-ID`로 요청자를 식별합니다.
- API 명세의 길이 제한을 Bean Validation으로 검증하는지 확인합니다. (닉네임 1~20, 비밀번호 6~20, 관심사 이름 1~50, 키워드 1~10개, 댓글 1~500)
- 응답 필드명·enum 값이 명세와 다르면 지적합니다. (알림 `resourceType`은 소문자 `interest`/`comment`)

### 테스트

- 새 기능에 테스트가 없거나 실패·경계값 케이스가 빠진 경우
- 팀은 TDD(Red → Green → Refactor)로 작업합니다.

## 코멘트 형식

- 문제, 이유, 수정 제안 순으로 짧게 씁니다.
- 수정 코드는 가능하면 suggestion 블록으로 제시합니다.
- 확실하지 않은 내용은 단정하지 말고 질문 형태로 남깁니다.
