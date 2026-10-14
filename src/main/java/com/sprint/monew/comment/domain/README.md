# `com.sprint.monew.comment.domain`

Entity와 도메인 규칙입니다. `web`·`application`·`infra`를 import하지 않습니다. 다른 모듈 Entity는 연관관계 대신 ID(`UUID`)로 참조합니다.

## 추천 클래스·인터페이스

| 이름 | 종류 | 역할 |
|---|---|---|
| `Comment` | @Entity | articleId, userId(ID로 보관), content, likeCount, deletedAt |
| `CommentLike` | @Entity | (commentId, userId) unique |
| `CommentRepository / CommentLikeRepository` | interface | Spring Data JPA. 커스텀 조회는 `CommentQueryRepository` 상속 |
| `CommentQueryRepository` | interface | 기사별 정렬·커서 계약. 구현은 infra |
| `CommentErrorCode` | enum | COMMENT_NOT_FOUND, NOT_COMMENT_OWNER, ALREADY_LIKED … |

**관련 이슈**: [#27](https://github.com/SB14-GoS/sb14-monew-team01/issues/27) · [#29](https://github.com/SB14-GoS/sb14-monew-team01/issues/29) · [#31](https://github.com/SB14-GoS/sb14-monew-team01/issues/31)

> 패키지 골격 유지용 안내 파일입니다. 이 폴더에 첫 클래스를 추가하면 삭제하세요. 클래스 이름은 추천이며 팀 합의로 바꿔도 됩니다.
