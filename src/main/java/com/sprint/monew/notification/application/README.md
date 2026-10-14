# `com.sprint.monew.notification.application`

유스케이스와 트랜잭션 경계(`@Transactional`)입니다. HTTP 타입(`HttpServletRequest` 등)에 의존하지 않습니다.

## 추천 클래스·인터페이스

| 이름 | 종류 | 역할 |
|---|---|---|
| `NotificationService` | class | 미확인 알림 조회(시간순 커서), 확인, 전체 확인 |
| `NotificationEventHandler` | class | 다른 모듈 이벤트 구독 → 알림 생성 (`@TransactionalEventListener` 등) |
| `NotificationCleanupJobConfig` | @Configuration | 확인 후 7일 경과 알림 삭제 배치 (매일) |

**관련 이슈**: [#32](https://github.com/SB14-GoS/sb14-monew-team01/issues/32) · [#33](https://github.com/SB14-GoS/sb14-monew-team01/issues/33) · [#34](https://github.com/SB14-GoS/sb14-monew-team01/issues/34) · [#43](https://github.com/SB14-GoS/sb14-monew-team01/issues/43)

> 패키지 골격 유지용 안내 파일입니다. 이 폴더에 첫 클래스를 추가하면 삭제하세요. 클래스 이름은 추천이며 팀 합의로 바꿔도 됩니다.
