# `com.sprint.monew.global.exception`



## 추천 클래스·인터페이스

| 이름 | 종류 | 역할 |
|---|---|---|
| `ErrorCode` | interface | `HttpStatus getStatus()`, `String getMessage()` — 도메인별 enum이 구현 |
| `MonewException` | class | `ErrorCode` + details를 가진 공통 런타임 예외 |
| `ErrorResponse` | record | timestamp, code, message, details, exceptionType, status (명세 형식) |
| `GlobalExceptionHandler` | @RestControllerAdvice | MonewException·Validation·기타 예외 → ErrorResponse |

**관련 이슈**: [#4](https://github.com/SB14-GoS/sb14-monew-team01/issues/4)

> 패키지 골격 유지용 안내 파일입니다. 이 폴더에 첫 클래스를 추가하면 삭제하세요. 클래스 이름은 추천이며 팀 합의로 바꿔도 됩니다.
