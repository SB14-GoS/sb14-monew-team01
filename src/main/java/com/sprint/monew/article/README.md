# `com.sprint.monew.article`

**뉴스 기사 모듈** — 기사 수집(Naver API·RSS)·조회·조회수·삭제를 담당합니다. 외부 뉴스 출처는 `application/port`로 감쌉니다.

하위 패키지: `api`(공개) · `web` · `application` · `domain` · `infra` (내부)

**흐름**: 매시간 배치 → 출처별 수집 → 관심사 키워드 필터 → 원본 링크 중복 제거 → 저장 → `ArticlesCollectedEvent` 발행(알림)

**관련 이슈**: [#22](https://github.com/SB14-GoS/sb14-monew-team01/issues/22) · [#23](https://github.com/SB14-GoS/sb14-monew-team01/issues/23) · [#24](https://github.com/SB14-GoS/sb14-monew-team01/issues/24) · [#25](https://github.com/SB14-GoS/sb14-monew-team01/issues/25) · [#26](https://github.com/SB14-GoS/sb14-monew-team01/issues/26) · [#36](https://github.com/SB14-GoS/sb14-monew-team01/issues/36) · [#37](https://github.com/SB14-GoS/sb14-monew-team01/issues/37) · [#38](https://github.com/SB14-GoS/sb14-monew-team01/issues/38) · [#39](https://github.com/SB14-GoS/sb14-monew-team01/issues/39)

> 패키지 골격 유지용 안내 파일입니다. 이 폴더에 첫 클래스를 추가하면 삭제하세요. 클래스 이름은 추천이며 팀 합의로 바꿔도 됩니다.
