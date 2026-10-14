# `com.sprint.monew.notification`

**알림 모듈** — 관심사 기사 등록·댓글 좋아요 알림 생성, 조회, 확인, 정리 배치를 담당합니다. 다른 모듈의 이벤트를 구독합니다.

하위 패키지: `api`(공개) · `web` · `application` · `domain` · `infra` (내부)

**흐름**: `ArticlesCollectedEvent`·`CommentLikedEvent` 수신 → 알림 생성 → 사용자 확인 → 7일 경과 시 배치 삭제

**관련 이슈**: [#32](https://github.com/SB14-GoS/sb14-monew-team01/issues/32) · [#33](https://github.com/SB14-GoS/sb14-monew-team01/issues/33) · [#34](https://github.com/SB14-GoS/sb14-monew-team01/issues/34) · [#43](https://github.com/SB14-GoS/sb14-monew-team01/issues/43)

> 패키지 골격 유지용 안내 파일입니다. 이 폴더에 첫 클래스를 추가하면 삭제하세요. 클래스 이름은 추천이며 팀 합의로 바꿔도 됩니다.
