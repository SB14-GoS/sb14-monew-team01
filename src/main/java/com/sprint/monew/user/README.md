# `com.sprint.monew.user`

**사용자 모듈** — 회원가입·로그인·정보 수정·논리/물리 삭제를 담당합니다.

하위 패키지: `api`(공개) · `web` · `application` · `domain` · `infra` (내부)

**흐름**: 가입 → 로그인(이후 요청 헤더 `Monew-Request-User-ID`) → 닉네임 수정 → 논리 삭제 → 1일 뒤 배치가 물리 삭제

**관련 이슈**: [#12](https://github.com/SB14-GoS/sb14-monew-team01/issues/12) · [#13](https://github.com/SB14-GoS/sb14-monew-team01/issues/13) · [#14](https://github.com/SB14-GoS/sb14-monew-team01/issues/14) · [#15](https://github.com/SB14-GoS/sb14-monew-team01/issues/15) · [#16](https://github.com/SB14-GoS/sb14-monew-team01/issues/16) · [#42](https://github.com/SB14-GoS/sb14-monew-team01/issues/42)

> 패키지 골격 유지용 안내 파일입니다. 이 폴더에 첫 클래스를 추가하면 삭제하세요. 클래스 이름은 추천이며 팀 합의로 바꿔도 됩니다.
