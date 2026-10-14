# `com.sprint.monew.global.web`

HTTP 요청/응답 변환만 합니다. 비즈니스 로직과 Repository 직접 호출은 금지합니다. 응답 DTO 필드명은 Swagger 명세와 정확히 맞춥니다.

## 추천 클래스·인터페이스

| 이름 | 종류 | 역할 |
|---|---|---|
| `RequestUserId` | @interface | 컨트롤러 파라미터에 붙이는 어노테이션 |
| `RequestUserIdArgumentResolver` | class | `Monew-Request-User-ID` 헤더 → UUID 주입, 누락 시 예외 |
| `MdcLoggingFilter` | class | 요청 ID·IP를 MDC + 응답 헤더에 추가 |
| `WebConfig` | @Configuration | ArgumentResolver 등록 |

**관련 이슈**: [#7](https://github.com/SB14-GoS/sb14-monew-team01/issues/7) · [#8](https://github.com/SB14-GoS/sb14-monew-team01/issues/8)

> 패키지 골격 유지용 안내 파일입니다. 이 폴더에 첫 클래스를 추가하면 삭제하세요. 클래스 이름은 추천이며 팀 합의로 바꿔도 됩니다.
