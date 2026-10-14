# `com.sprint.monew.interest.web`

HTTP 요청/응답 변환만 합니다. 비즈니스 로직과 Repository 직접 호출은 금지합니다. 응답 DTO 필드명은 Swagger 명세와 정확히 맞춥니다.

## 추천 클래스·인터페이스

| 이름 | 종류 | 역할 |
|---|---|---|
| `InterestController` | class | `GET/POST /api/interests`, `PATCH/DELETE /api/interests/{id}`, `POST/DELETE /api/interests/{id}/subscriptions` |
| `InterestApiDocs` | interface | Swagger 문서 |
| `dto/InterestRegisterRequest` | record | name, keywords (둘 다 필수) |
| `dto/InterestUpdateRequest` | record | keywords |
| `dto/InterestSearchRequest` | record | keyword, orderBy(name·subscriberCount), direction, cursor, after, limit |
| `dto/InterestDto` | record | id, name, keywords, subscriberCount, subscribedByMe |
| `dto/SubscriptionDto` | record | 구독 응답 (명세 참고) |

**관련 이슈**: [#17](https://github.com/SB14-GoS/sb14-monew-team01/issues/17) · [#18](https://github.com/SB14-GoS/sb14-monew-team01/issues/18) · [#19](https://github.com/SB14-GoS/sb14-monew-team01/issues/19) · [#20](https://github.com/SB14-GoS/sb14-monew-team01/issues/20) · [#21](https://github.com/SB14-GoS/sb14-monew-team01/issues/21)

> 패키지 골격 유지용 안내 파일입니다. 이 폴더에 첫 클래스를 추가하면 삭제하세요. 클래스 이름은 추천이며 팀 합의로 바꿔도 됩니다.
