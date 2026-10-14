# `com.sprint.monew.user.application`

유스케이스와 트랜잭션 경계(`@Transactional`)입니다. HTTP 타입(`HttpServletRequest` 등)에 의존하지 않습니다.

## 추천 클래스·인터페이스

| 이름 | 종류 | 역할 |
|---|---|---|
| `UserService` | class | 가입·로그인·수정·삭제 유스케이스. `UserQueryApi` 구현 |
| `UserCleanupScheduler` | class | 논리 삭제 1일 경과 사용자 물리 삭제 (`@Scheduled` 또는 Batch) |

**관련 이슈**: [#12](https://github.com/SB14-GoS/sb14-monew-team01/issues/12) · [#13](https://github.com/SB14-GoS/sb14-monew-team01/issues/13) · [#14](https://github.com/SB14-GoS/sb14-monew-team01/issues/14) · [#15](https://github.com/SB14-GoS/sb14-monew-team01/issues/15) · [#16](https://github.com/SB14-GoS/sb14-monew-team01/issues/16) · [#42](https://github.com/SB14-GoS/sb14-monew-team01/issues/42)

> 패키지 골격 유지용 안내 파일입니다. 이 폴더에 첫 클래스를 추가하면 삭제하세요. 클래스 이름은 추천이며 팀 합의로 바꿔도 됩니다.
