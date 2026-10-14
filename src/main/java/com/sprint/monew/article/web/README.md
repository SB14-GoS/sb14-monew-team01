# `com.sprint.monew.article.web`

HTTP 요청/응답 변환만 합니다. 비즈니스 로직과 Repository 직접 호출은 금지합니다. 응답 DTO 필드명은 Swagger 명세와 정확히 맞춥니다.

## 추천 클래스·인터페이스

| 이름 | 종류 | 역할 |
|---|---|---|
| `ArticleController` | class | `GET /api/articles`, `GET /api/articles/{id}`, `POST /api/articles/{id}/article-views`, `GET /api/articles/sources`, `GET /api/articles/restore`, `DELETE /api/articles/{id}`, `DELETE /api/articles/{id}/hard` |
| `ArticleApiDocs` | interface | Swagger 문서 |
| `dto/ArticleSearchRequest` | record | keyword, interestId, sourceIn, publishDateFrom/To, orderBy, direction, cursor, after, limit |
| `dto/ArticleDto` | record | id, source, sourceUrl, title, publishDate, summary, commentCount, viewCount, viewedByMe |
| `dto/ArticleViewDto` | record | 기사 뷰 등록 응답 (명세 참고) |
| `dto/ArticleRestoreResultDto` | record | restoreDate, restoredArticleIds, restoredArticleCount |

**관련 이슈**: [#22](https://github.com/SB14-GoS/sb14-monew-team01/issues/22) · [#23](https://github.com/SB14-GoS/sb14-monew-team01/issues/23) · [#24](https://github.com/SB14-GoS/sb14-monew-team01/issues/24) · [#25](https://github.com/SB14-GoS/sb14-monew-team01/issues/25) · [#26](https://github.com/SB14-GoS/sb14-monew-team01/issues/26) · [#41](https://github.com/SB14-GoS/sb14-monew-team01/issues/41)

> 패키지 골격 유지용 안내 파일입니다. 이 폴더에 첫 클래스를 추가하면 삭제하세요. 클래스 이름은 추천이며 팀 합의로 바꿔도 됩니다.
