# `com.sprint.monew.interest.api`

다른 모듈이 **유일하게** 참조할 수 있는 패키지입니다. 인터페이스·record·이벤트만 두고 Entity는 노출하지 않습니다.

## 추천 클래스·인터페이스

| 이름 | 종류 | 역할 |
|---|---|---|
| `InterestQueryApi` | interface | `findAllKeywords()`(수집용), `getSubscriberIds(UUID interestId)`(알림용), `getName(UUID)` |
| `InterestKeywords` | record | interestId, name, List<String> keywords |
| `InterestSubscribedEvent / InterestUnsubscribedEvent` | record (이벤트) | 활동 내역 갱신용 |

**관련 이슈**: [#21](https://github.com/SB14-GoS/sb14-monew-team01/issues/21) · [#32](https://github.com/SB14-GoS/sb14-monew-team01/issues/32) · [#38](https://github.com/SB14-GoS/sb14-monew-team01/issues/38)

> 패키지 골격 유지용 안내 파일입니다. 이 폴더에 첫 클래스를 추가하면 삭제하세요. 클래스 이름은 추천이며 팀 합의로 바꿔도 됩니다.
