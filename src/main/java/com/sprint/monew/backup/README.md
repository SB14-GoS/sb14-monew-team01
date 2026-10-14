# `com.sprint.monew.backup`

**백업·복구 모듈** — 기사 데이터를 날짜 단위로 S3에 백업하고, 백업과 DB를 비교해 유실분을 복구합니다. 파일 저장소는 `application/port`로 감쌉니다.

하위 패키지: `api`(공개) · `web` · `application` · `domain` · `infra` (내부)

**흐름**: 매일 배치 → 날짜별 기사 조회(`ArticleQueryApi`) → S3 업로드 / 복구 요청 → S3 백업과 DB 비교 → 없는 기사만 `ArticleRestoreApi`로 등록

**관련 이슈**: [#40](https://github.com/SB14-GoS/sb14-monew-team01/issues/40) · [#41](https://github.com/SB14-GoS/sb14-monew-team01/issues/41) · [#46](https://github.com/SB14-GoS/sb14-monew-team01/issues/46)

> 패키지 골격 유지용 안내 파일입니다. 이 폴더에 첫 클래스를 추가하면 삭제하세요. 클래스 이름은 추천이며 팀 합의로 바꿔도 됩니다.
