# `com.sprint.monew.interest.infra`

QueryDSL 구현, 외부 API·S3 클라이언트처럼 기술에 의존하는 코드입니다. `domain`의 인터페이스나 `application/port`를 구현합니다. 커스텀 조회 인터페이스(`XxxQueryRepository`)는 **domain**에 두고, 여기에는 구현(`XxxQueryRepositoryImpl`)만 둡니다 — 반대로 두면 domain → infra 의존이 생겨 `ArchitectureTest`가 실패합니다.

## 추천 클래스·인터페이스

| 이름 | 종류 | 역할 |
|---|---|---|
| `InterestQueryRepositoryImpl` | class | 이름·키워드 부분일치 검색 + 이름/구독자 수 정렬 + 커서 페이지네이션 (QueryDSL) |

**관련 이슈**: [#20](https://github.com/SB14-GoS/sb14-monew-team01/issues/20)

> 패키지 골격 유지용 안내 파일입니다. 이 폴더에 첫 클래스를 추가하면 삭제하세요. 클래스 이름은 추천이며 팀 합의로 바꿔도 됩니다.
