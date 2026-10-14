# `com.sprint.monew.interest.application`

유스케이스와 트랜잭션 경계(`@Transactional`)입니다. HTTP 타입(`HttpServletRequest` 등)에 의존하지 않습니다.

## 추천 클래스·인터페이스

| 이름 | 종류 | 역할 |
|---|---|---|
| `InterestService` | class | 등록(유사도 검사)·키워드 수정·삭제·목록 조회. `InterestQueryApi` 구현 |
| `SubscriptionService` | class | 구독/취소, 구독자 수 반영, 이벤트 발행 |

**관련 이슈**: [#17](https://github.com/SB14-GoS/sb14-monew-team01/issues/17) · [#18](https://github.com/SB14-GoS/sb14-monew-team01/issues/18) · [#19](https://github.com/SB14-GoS/sb14-monew-team01/issues/19) · [#20](https://github.com/SB14-GoS/sb14-monew-team01/issues/20) · [#21](https://github.com/SB14-GoS/sb14-monew-team01/issues/21)

> 패키지 골격 유지용 안내 파일입니다. 이 폴더에 첫 클래스를 추가하면 삭제하세요. 클래스 이름은 추천이며 팀 합의로 바꿔도 됩니다.
