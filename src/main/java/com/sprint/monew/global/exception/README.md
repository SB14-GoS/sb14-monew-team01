# 공통 예외 처리

각 담당자는 도메인 부모 예외를 상속해 구체적인 예외를 만듭니다. 예: `UserNotFoundException extends UserException`. 서비스에서 구체적인 예외를 던지면 공통 핸들러가 HTTP 상태와 `ErrorResponse` JSON으로 변환합니다.

작성 순서와 예시는 [공통 예외 처리 사용 가이드](EXCEPTION_HANDLING.md)를 참고하세요.

## 구성

| 이름 | 종류 | 역할 |
|---|---|---|
| `ErrorCode` | interface | `HttpStatus getStatus()`, `String getMessage()`, `Level getLevel()` — 도메인별 enum이 구현 |
| `CommonErrorCode` | enum | 공통 입력 오류·서버 오류 코드. `ErrorCode` 구현 |
| `MonewException` | class | `ErrorCode` + details를 가진 공통 런타임 예외 |
| `ErrorResponse` | record | timestamp, code, message, details, exceptionType, status (명세 형식) |
| `GlobalExceptionHandler` | @RestControllerAdvice | MonewException·Validation·기타 예외 → ErrorResponse |

**관련 이슈**: [#4](https://github.com/SB14-GoS/sb14-monew-team01/issues/4)
