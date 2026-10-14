# `com.sprint.monew.comment.application`

유스케이스와 트랜잭션 경계(`@Transactional`)입니다. HTTP 타입(`HttpServletRequest` 등)에 의존하지 않습니다.

## 추천 클래스·인터페이스

| 이름 | 종류 | 역할 |
|---|---|---|
| `CommentService` | class | 등록·조회·수정(본인만)·삭제. `UserQueryApi`·`ArticleQueryApi` 사용 |
| `CommentLikeService` | class | 좋아요/취소, 좋아요 수 반영, 이벤트 발행 |

**관련 이슈**: [#27](https://github.com/SB14-GoS/sb14-monew-team01/issues/27) · [#28](https://github.com/SB14-GoS/sb14-monew-team01/issues/28) · [#29](https://github.com/SB14-GoS/sb14-monew-team01/issues/29) · [#30](https://github.com/SB14-GoS/sb14-monew-team01/issues/30) · [#31](https://github.com/SB14-GoS/sb14-monew-team01/issues/31)

> 패키지 골격 유지용 안내 파일입니다. 이 폴더에 첫 클래스를 추가하면 삭제하세요. 클래스 이름은 추천이며 팀 합의로 바꿔도 됩니다.
