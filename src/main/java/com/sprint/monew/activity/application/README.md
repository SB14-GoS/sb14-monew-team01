# `com.sprint.monew.activity.application`

유스케이스와 트랜잭션 경계(`@Transactional`)입니다. HTTP 타입(`HttpServletRequest` 등)에 의존하지 않습니다.

## 추천 클래스·인터페이스

| 이름 | 종류 | 역할 |
|---|---|---|
| `UserActivityQueryService` | class | 활동 내역 조회 |
| `UserActivityProjector` | class | 구독·댓글·좋아요·기사 조회 이벤트 구독 → Mongo 문서 갱신 (최근 10건 유지) |

**관련 이슈**: [#35](https://github.com/SB14-GoS/sb14-monew-team01/issues/35) · [#44](https://github.com/SB14-GoS/sb14-monew-team01/issues/44)

> 패키지 골격 유지용 안내 파일입니다. 이 폴더에 첫 클래스를 추가하면 삭제하세요. 클래스 이름은 추천이며 팀 합의로 바꿔도 됩니다.
