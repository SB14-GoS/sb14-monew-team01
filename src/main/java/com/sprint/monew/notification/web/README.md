# `com.sprint.monew.notification.web`

HTTP 요청/응답 변환만 합니다. 비즈니스 로직과 Repository 직접 호출은 금지합니다. 응답 DTO 필드명은 Swagger 명세와 정확히 맞춥니다.

## 추천 클래스·인터페이스

| 이름 | 종류 | 역할 |
|---|---|---|
| `NotificationController` | class | `GET /api/notifications`, `PATCH /api/notifications`, `PATCH /api/notifications/{id}` |
| `NotificationApiDocs` | interface | Swagger 문서 |
| `dto/NotificationDto` | record | id, createdAt, updatedAt, confirmed, userId, content, resourceType, resourceId |

**관련 이슈**: [#33](https://github.com/SB14-GoS/sb14-monew-team01/issues/33) · [#34](https://github.com/SB14-GoS/sb14-monew-team01/issues/34)

> 패키지 골격 유지용 안내 파일입니다. 이 폴더에 첫 클래스를 추가하면 삭제하세요. 클래스 이름은 추천이며 팀 합의로 바꿔도 됩니다.
