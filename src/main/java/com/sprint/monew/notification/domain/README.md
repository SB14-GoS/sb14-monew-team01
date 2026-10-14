# `com.sprint.monew.notification.domain`

Entity와 도메인 규칙입니다. `web`·`application`·`infra`를 import하지 않습니다. 다른 모듈 Entity는 연관관계 대신 ID(`UUID`)로 참조합니다.

## 추천 클래스·인터페이스

| 이름 | 종류 | 역할 |
|---|---|---|
| `Notification` | @Entity | userId, content, resourceType, resourceId, confirmed |
| `ResourceType` | enum | INTEREST, COMMENT |
| `NotificationRepository` | interface | Spring Data JPA. 커스텀 조회는 `NotificationQueryRepository` 상속 |
| `NotificationQueryRepository` | interface | 미확인 알림 커서 조회 계약. 구현은 infra |
| `NotificationErrorCode` | enum | NOTIFICATION_NOT_FOUND … |

**관련 이슈**: [#32](https://github.com/SB14-GoS/sb14-monew-team01/issues/32) · [#34](https://github.com/SB14-GoS/sb14-monew-team01/issues/34)

> 패키지 골격 유지용 안내 파일입니다. 이 폴더에 첫 클래스를 추가하면 삭제하세요. 클래스 이름은 추천이며 팀 합의로 바꿔도 됩니다.
