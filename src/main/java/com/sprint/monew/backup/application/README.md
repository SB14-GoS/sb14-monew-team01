# `com.sprint.monew.backup.application`

유스케이스와 트랜잭션 경계(`@Transactional`)입니다. HTTP 타입(`HttpServletRequest` 등)에 의존하지 않습니다.

## 추천 클래스·인터페이스

| 이름 | 종류 | 역할 |
|---|---|---|
| `ArticleBackupService` | class | 날짜별 기사 직렬화(JSON) → `FileStorage`에 저장 |
| `ArticleRestoreService` | class | 백업 vs DB 비교 → 유실 기사 `ArticleRestoreApi`로 등록, `ArticleRestoreResultDto` 반환 |
| `ArticleBackupJobConfig` | @Configuration | 매일 백업 배치 (chunk 단위) |

**관련 이슈**: [#40](https://github.com/SB14-GoS/sb14-monew-team01/issues/40) · [#41](https://github.com/SB14-GoS/sb14-monew-team01/issues/41)

> 패키지 골격 유지용 안내 파일입니다. 이 폴더에 첫 클래스를 추가하면 삭제하세요. 클래스 이름은 추천이며 팀 합의로 바꿔도 됩니다.
