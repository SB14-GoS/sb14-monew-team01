# `com.sprint.monew.notification.infra`

QueryDSL 구현, 외부 API·S3 클라이언트처럼 기술에 의존하는 코드입니다. `domain`의 인터페이스나 `application/port`를 구현합니다. 커스텀 조회 인터페이스(`XxxQueryRepository`)는 **domain**에 두고, 여기에는 구현(`XxxQueryRepositoryImpl`)만 둡니다 — 반대로 두면 domain → infra 의존이 생겨 `ArchitectureTest`가 실패합니다.

## 추천 클래스·인터페이스

| 이름 | 종류 | 역할 |
|---|---|---|
| `NotificationQueryRepositoryImpl` | class | 미확인 알림 커서 조회 (QueryDSL) |
| `NotificationBulkWriter` | class | 구독자 다수 알림 일괄 INSERT (JDBC batch) |

**관련 이슈**: [#32](https://github.com/SB14-GoS/sb14-monew-team01/issues/32) · [#33](https://github.com/SB14-GoS/sb14-monew-team01/issues/33)

> 패키지 골격 유지용 안내 파일입니다. 이 폴더에 첫 클래스를 추가하면 삭제하세요. 클래스 이름은 추천이며 팀 합의로 바꿔도 됩니다.
