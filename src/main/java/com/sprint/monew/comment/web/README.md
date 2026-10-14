# `com.sprint.monew.comment.web`

HTTP 요청/응답 변환만 합니다. 비즈니스 로직과 Repository 직접 호출은 금지합니다. 응답 DTO 필드명은 Swagger 명세와 정확히 맞춥니다.

## 추천 클래스·인터페이스

| 이름 | 종류 | 역할 |
|---|---|---|
| `CommentController` | class | `GET/POST /api/comments`, `PATCH/DELETE /api/comments/{id}`, `DELETE /api/comments/{id}/hard`, `POST/DELETE /api/comments/{id}/comment-likes` |
| `CommentApiDocs` | interface | Swagger 문서 |
| `dto/CommentRegisterRequest` | record | articleId, userId, content |
| `dto/CommentUpdateRequest` | record | content |
| `dto/CommentSearchRequest` | record | articleId, orderBy(createdAt·likeCount), direction, cursor, after, limit |
| `dto/CommentDto` | record | id, articleId, userId, userNickname, content, likeCount, likedByMe, createdAt |
| `dto/CommentLikeDto` | record | 좋아요 응답 (명세 참고) |

**관련 이슈**: [#27](https://github.com/SB14-GoS/sb14-monew-team01/issues/27) · [#28](https://github.com/SB14-GoS/sb14-monew-team01/issues/28) · [#29](https://github.com/SB14-GoS/sb14-monew-team01/issues/29) · [#30](https://github.com/SB14-GoS/sb14-monew-team01/issues/30) · [#31](https://github.com/SB14-GoS/sb14-monew-team01/issues/31)

> 패키지 골격 유지용 안내 파일입니다. 이 폴더에 첫 클래스를 추가하면 삭제하세요. 클래스 이름은 추천이며 팀 합의로 바꿔도 됩니다.
