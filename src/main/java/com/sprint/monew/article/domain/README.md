# `com.sprint.monew.article.domain`

Entity와 도메인 규칙입니다. `web`·`application`·`infra`를 import하지 않습니다. 다른 모듈 Entity는 연관관계 대신 ID(`UUID`)로 참조합니다.

## 추천 클래스·인터페이스

| 이름 | 종류 | 역할 |
|---|---|---|
| `Article` | @Entity | source, sourceUrl(unique), title, publishDate, summary, commentCount, viewCount, deletedAt |
| `ArticleInterest` | @Entity | 기사-관심사 매핑 (interestId는 ID로 보관) |
| `ArticleView` | @Entity | (articleId, userId) unique |
| `ArticleSource` | enum | NAVER, HANKYUNG, CHOSUN, YONHAP |
| `KeywordMatcher` | class | 제목·요약에 키워드 포함 여부 판정 |
| `ArticleRepository / ArticleViewRepository` | interface | Spring Data JPA. 커스텀 조회는 `ArticleQueryRepository` 상속 |
| `ArticleQueryRepository` | interface | 검색·필터·정렬·커서 계약. 구현은 infra |
| `ArticleErrorCode` | enum | ARTICLE_NOT_FOUND … |

**관련 이슈**: [#22](https://github.com/SB14-GoS/sb14-monew-team01/issues/22) · [#24](https://github.com/SB14-GoS/sb14-monew-team01/issues/24) · [#38](https://github.com/SB14-GoS/sb14-monew-team01/issues/38)

> 패키지 골격 유지용 안내 파일입니다. 이 폴더에 첫 클래스를 추가하면 삭제하세요. 클래스 이름은 추천이며 팀 합의로 바꿔도 됩니다.
