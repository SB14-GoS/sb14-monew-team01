# `com.sprint.monew.article.application.port`

외부 시스템을 감싸는 인터페이스만 둡니다. 구현은 같은 모듈의 `infra`에 둡니다. 테스트에서는 Fake 구현으로 대체합니다.

## 추천 클래스·인터페이스

| 이름 | 종류 | 역할 |
|---|---|---|
| `NewsSourceClient` | interface | `ArticleSource source()`, `List<RawArticle> fetch(Collection<String> keywords)` |
| `RawArticle` | record | 출처 응답을 통일한 모델: source, sourceUrl, title, publishDate, summary |

**관련 이슈**: [#36](https://github.com/SB14-GoS/sb14-monew-team01/issues/36) · [#37](https://github.com/SB14-GoS/sb14-monew-team01/issues/37) · [#38](https://github.com/SB14-GoS/sb14-monew-team01/issues/38)

> 패키지 골격 유지용 안내 파일입니다. 이 폴더에 첫 클래스를 추가하면 삭제하세요. 클래스 이름은 추천이며 팀 합의로 바꿔도 됩니다.
