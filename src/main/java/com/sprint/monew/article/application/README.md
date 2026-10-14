# `com.sprint.monew.article.application`

유스케이스와 트랜잭션 경계(`@Transactional`)입니다. HTTP 타입(`HttpServletRequest` 등)에 의존하지 않습니다.

## 추천 클래스·인터페이스

| 이름 | 종류 | 역할 |
|---|---|---|
| `ArticleService` | class | 목록·단건 조회, 논리/물리 삭제. `ArticleQueryApi` 구현 |
| `ArticleViewService` | class | 사용자당 1회 조회수 반영 + 이벤트 발행 |
| `ArticleCollectService` | class | 출처별 수집 → 키워드 필터 → 중복 제거 → 저장 → 이벤트 발행 |
| `ArticleCollectJobConfig` | @Configuration | Spring Batch Job (매시간, 출처별 Step) |

**관련 이슈**: [#22](https://github.com/SB14-GoS/sb14-monew-team01/issues/22) · [#23](https://github.com/SB14-GoS/sb14-monew-team01/issues/23) · [#24](https://github.com/SB14-GoS/sb14-monew-team01/issues/24) · [#26](https://github.com/SB14-GoS/sb14-monew-team01/issues/26) · [#38](https://github.com/SB14-GoS/sb14-monew-team01/issues/38) · [#39](https://github.com/SB14-GoS/sb14-monew-team01/issues/39)

> 패키지 골격 유지용 안내 파일입니다. 이 폴더에 첫 클래스를 추가하면 삭제하세요. 클래스 이름은 추천이며 팀 합의로 바꿔도 됩니다.
