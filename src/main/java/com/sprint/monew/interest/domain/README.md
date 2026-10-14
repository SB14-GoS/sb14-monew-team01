# `com.sprint.monew.interest.domain`

Entity와 도메인 규칙입니다. `web`·`application`·`infra`를 import하지 않습니다. 다른 모듈 Entity는 연관관계 대신 ID(`UUID`)로 참조합니다.

## 추천 클래스·인터페이스

| 이름 | 종류 | 역할 |
|---|---|---|
| `Interest` | @Entity | name(unique), subscriberCount, 키워드 컬렉션(애그리거트 루트) |
| `Keyword` | @Entity 또는 @ElementCollection | 관심사 키워드 |
| `Subscription` | @Entity | (interestId, userId) unique |
| `SimilarityPolicy` | class (도메인 서비스) | 이름 유사도 80% 판정. 알고리즘(편집거리·Jaro-Winkler 등)은 팀 결정 |
| `InterestRepository / SubscriptionRepository` | interface | Spring Data JPA. `InterestQueryRepository`를 함께 상속 |
| `InterestQueryRepository` | interface | 커스텀 조회 계약(검색·정렬·커서). 구현은 infra의 `InterestQueryRepositoryImpl` |
| `InterestErrorCode` | enum | SIMILAR_NAME_EXISTS, INTEREST_NOT_FOUND, ALREADY_SUBSCRIBED … |

**관련 이슈**: [#17](https://github.com/SB14-GoS/sb14-monew-team01/issues/17) · [#21](https://github.com/SB14-GoS/sb14-monew-team01/issues/21)

> 패키지 골격 유지용 안내 파일입니다. 이 폴더에 첫 클래스를 추가하면 삭제하세요. 클래스 이름은 추천이며 팀 합의로 바꿔도 됩니다.
