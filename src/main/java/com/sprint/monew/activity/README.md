# `com.sprint.monew.activity`

**사용자 활동 내역 모듈** — 구독 관심사, 최근 댓글 10, 최근 좋아요 댓글 10, 최근 본 기사 10을 조회합니다. 심화: MongoDB 조회 모델로 미리 갱신합니다.

하위 패키지: `api`(공개) · `web` · `application` · `domain` · `infra` (내부)

**흐름**: 다른 모듈 이벤트 수신 → MongoDB 문서 갱신 → 조회 API는 문서를 그대로 반환

**관련 이슈**: [#35](https://github.com/SB14-GoS/sb14-monew-team01/issues/35) · [#44](https://github.com/SB14-GoS/sb14-monew-team01/issues/44)

> 패키지 골격 유지용 안내 파일입니다. 이 폴더에 첫 클래스를 추가하면 삭제하세요. 클래스 이름은 추천이며 팀 합의로 바꿔도 됩니다.
