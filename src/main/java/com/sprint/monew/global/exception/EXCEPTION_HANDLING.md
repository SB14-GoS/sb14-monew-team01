# 공통 예외 처리 사용 가이드

각 도메인 담당자는 `UserException`, `ArticleException` 같은 도메인 공통 부모를 상속해 `UserNotFoundException`, `ArticleNotFoundException` 같은 구체적인 예외를 만듭니다. 서비스에서는 구체적인 예외를 던지고, `GlobalExceptionHandler`가 HTTP 상태 코드와 `ErrorResponse` JSON으로 변환합니다.

**팀의 사용 방식은 다음으로 통일합니다.**

1. 도메인 enum에 오류 코드·HTTP 상태·메시지·로그 레벨을 정의합니다.
2. 제공된 도메인 부모 예외를 상속해 오류 상황별 클래스를 만듭니다.
3. 세부 예외의 생성자에서 사용할 오류 코드를 고정하고 `details`를 구성합니다.
4. 서비스에서는 `throw new UserNotFoundException(userId)`처럼 세부 예외를 던집니다.
5. 공통 핸들러는 상속 관계를 통해 처리하므로 세부 예외용 핸들러를 따로 추가하지 않습니다.

```text
RuntimeException
└─ MonewException                  공통 예외의 최상위 부모
   └─ UserException                사용자 도메인의 공통 부모
      └─ UserNotFoundException     사용자 조회 실패를 나타내는 세부 예외
```

현재 공통 코드에 `MonewException`과 도메인별 부모 예외 6개가 준비되어 있습니다. 이 문서의 `UserNotFoundException`, `ArticleNotFoundException`은 각 담당자가 추가할 세부 클래스의 예시입니다.

```text
서비스에서 UserNotFoundException 발생
        ↓ UserException → MonewException 상속
MonewException을 처리하는 공통 핸들러
        ↓
오류 코드의 상태·메시지를 사용해 JSON 응답
```

## 1. 구체적인 예외를 만들어 사용하기

### 1-1. 오류 코드 enum 확인하기

사용자 오류는 `com.sprint.monew.user.domain.UserErrorCode`에 정의합니다. 현재 아래 코드가 준비되어 있습니다.

```java
package com.sprint.monew.user.domain;

import com.sprint.monew.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(Level.WARN, HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다.");

    private final Level level;
    private final HttpStatus status;
    private final String message;
}
```

`ErrorCode`는 인터페이스이므로 `ErrorCode.USER_NOT_FOUND`라는 상수는 없습니다. **실제 상수가 있는 `UserErrorCode.USER_NOT_FOUND`를 사용합니다.** 다른 도메인도 `ArticleErrorCode.ARTICLE_NOT_FOUND`처럼 해당 도메인의 enum을 사용합니다.

### 1-2. 제공된 도메인 부모 확인하기

`com.sprint.monew.global.exception.user.UserException`은 이미 준비되어 있습니다.

```java
package com.sprint.monew.global.exception.user;

import com.sprint.monew.global.exception.ErrorCode;
import com.sprint.monew.global.exception.MonewException;
import java.util.Map;

public class UserException extends MonewException {

    public UserException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }
}
```

이 부모 클래스는 사용자 도메인의 여러 세부 예외를 묶고, 오류 코드와 추가 정보를 `MonewException`에 전달합니다. 오류 상황마다 이 부모 클래스를 수정하지 않습니다.

### 1-3. 세부 예외 클래스 만들기

사용자 담당자는 `src/main/java/com/sprint/monew/user/domain/UserNotFoundException.java`를 만들어 다음처럼 작성합니다. 아래 클래스는 추가 방법을 보여주는 예시입니다.

```java
package com.sprint.monew.user.domain;

import com.sprint.monew.global.exception.user.UserException;
import java.util.Map;
import java.util.UUID;

public class UserNotFoundException extends UserException {

    public UserNotFoundException(UUID userId) {
        super(UserErrorCode.USER_NOT_FOUND, Map.of("userId", userId));
    }
}
```

생성자의 각 부분은 다음 의미입니다.

| 코드 | 의미 |
|---|---|
| `extends UserException` | 사용자 도메인 부모 예외를 상속 |
| `UUID userId` | 조회에 실패한 사용자 ID를 전달받음 |
| `UserErrorCode.USER_NOT_FOUND` | 이 예외가 사용할 오류 코드를 생성자 안에서 고정 |
| `Map.of("userId", userId)` | 응답의 `details`에 사용자 ID를 담음 |
| `super(오류코드, 추가정보)` | 부모 생성자를 호출해 공통 예외에 정보를 전달 |

부모는 `(ErrorCode, Map<String, Object>)`를 받지만, 세부 예외는 해당 오류에 필요한 값만 받습니다. 위 클래스의 호출자가 오류 코드를 다시 고를 필요는 없습니다. `new UserNotFoundException(userId)`만으로 코드·메시지·상태가 결정됩니다.

### 1-4. 서비스에서 세부 예외 던지기

서비스의 사용자 조회 실패 분기에서는 다음처럼 사용합니다. `userId`는 조회에 사용한 사용자 ID입니다.

```java
import com.sprint.monew.user.domain.UserNotFoundException;

// 서비스 메서드 안에서 사용
throw new UserNotFoundException(userId);
```

Spring Data Repository에서 `Optional`로 조회하는 경우에는 아래처럼 연결할 수 있습니다. `userRepository`는 해당 서비스의 사용자 Repository를 뜻합니다.

```java
return userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));
```

서비스에서는 오류 코드·상태·메시지를 매번 조합하거나 `ErrorResponse`를 직접 생성하지 않습니다. 세부 예외를 던지면 공통 핸들러가 응답을 구성합니다.

### 1-5. 실제 응답 확인하기

`USER_NOT_FOUND`에는 HTTP 상태 `NOT_FOUND`, 메시지 `유저를 찾을 수 없습니다.`, 로그 레벨 `WARN`이 지정되어 있습니다. 위 예외의 HTTP 상태는 **404**이고, 응답 본문은 다음 형태입니다. 시간과 사용자 ID는 예시입니다.

```json
{
  "timestamp": "2026-10-20T01:00:00Z",
  "code": "USER_NOT_FOUND",
  "message": "유저를 찾을 수 없습니다.",
  "details": {
    "userId": "123e4567-e89b-12d3-a456-426614174000"
  },
  "exceptionType": "UserNotFoundException",
  "status": 404
}
```

추가 정보가 없는 하위 예외는 부모 생성자를 호출할 때 `super(오류코드, Map.of())`를 사용합니다. 서비스에서 도메인 부모 예외를 직접 생성하기보다, 상황에 맞는 구체적인 예외를 사용합니다.

### 1-6. 작성할 때 구분할 것

| 항목 | 사용 방식 |
|---|---|
| 서비스에서 던질 예외 | `new UserNotFoundException(userId)` |
| 세부 예외의 부모 | `UserException` |
| 세부 예외에서 부모에게 전달할 값 | `UserErrorCode.USER_NOT_FOUND`, `Map.of("userId", userId)` |
| 새 오류 코드 추가 위치 | 해당 도메인의 `UserErrorCode` 같은 enum |
| 세부 예외마다 별도 핸들러 추가 | 필요 없음. `MonewException` 핸들러가 처리 |
| 기존 공통 부모를 서비스에서 직접 생성 | 팀의 사용 방식은 오류 상황별 세부 예외를 던지는 것으로 통일 |

각 담당자가 추가할 파일은 **도메인 오류 코드 enum과 오류 상황별 세부 예외 클래스**입니다. 사용자 외 도메인도 3절의 기사 예시와 같은 방식으로 작성합니다.

## 2. 각 클래스의 역할

| 구성 | 역할 |
|---|---|
| `ErrorCode` | 모든 오류 코드가 제공할 상태·메시지·로그 레벨을 정한 인터페이스 |
| `CommonErrorCode` | 공통 입력 오류와 서버 오류를 정의한 enum |
| `UserErrorCode` 등 도메인 enum | 담당 도메인의 오류 목록과 응답 정보를 정의. `ErrorCode` 구현 |
| `MonewException` | 오류 코드, 추가 정보, 발생 시각을 보관하는 공통 예외 |
| `UserException` 등 도메인 부모 예외 | `MonewException`을 상속하며 해당 도메인의 하위 예외가 사용할 공통 부모 |
| `UserNotFoundException` 등 구체적인 예외 | 도메인 부모를 상속하고 오류 코드와 추가 정보를 정함. 각 도메인 담당자가 추가 |
| `GlobalExceptionHandler` | 예외를 로그에 남기고 HTTP 응답으로 변환 |
| `ErrorResponse` | 응답 JSON의 필드를 정의한 record |

`ErrorCode`의 계약은 다음과 같습니다.

```java
public interface ErrorCode {
    HttpStatus getStatus();
    String getMessage();
    Level getLevel();
}
```

`HttpStatus`는 `org.springframework.http.HttpStatus`, `Level`은 `org.slf4j.event.Level`입니다. 핸들러는 `getStatus().value()`로 상태 코드를 숫자로 바꿔 응답 본문에 넣습니다. 로그 레벨은 서버 로그에 사용하며 응답 필드에는 포함하지 않습니다.

## 3. 새 도메인 오류 코드 추가하기

각 모듈의 `domain` 패키지에 `ErrorCode`를 구현하는 enum을 추가합니다. 현재 구현된 사용자 오류 코드는 [UserErrorCode.java](../../user/domain/UserErrorCode.java)를 참고하세요.

다음은 기사 담당자가 `ArticleErrorCode`를 새로 추가할 때의 예시입니다.

```java
package com.sprint.monew.article.domain;

import com.sprint.monew.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ArticleErrorCode implements ErrorCode {
    ARTICLE_NOT_FOUND(Level.WARN, HttpStatus.NOT_FOUND, "기사를 찾을 수 없습니다.");

    private final Level level;
    private final HttpStatus status;
    private final String message;
}
```

오류 코드를 만든 뒤 도메인 부모 예외를 상속하는 구체적인 예외를 추가합니다. 다음도 기사 담당자가 새로 추가할 클래스의 예시입니다.

```java
package com.sprint.monew.article.domain;

import com.sprint.monew.global.exception.article.ArticleException;
import java.util.Map;
import java.util.UUID;

public class ArticleNotFoundException extends ArticleException {

    public ArticleNotFoundException(UUID articleId) {
        super(ArticleErrorCode.ARTICLE_NOT_FOUND, Map.of("articleId", articleId));
    }
}
```

서비스에서는 다음처럼 던집니다.

```java
import com.sprint.monew.article.domain.ArticleNotFoundException;

// 서비스 메서드 안에서 사용
throw new ArticleNotFoundException(articleId);
```

이 경우 공통 핸들러가 404 응답을 만듭니다. 오류 코드를 추가할 때마다 핸들러에 분기를 추가할 필요는 없습니다.

응답의 `code`는 현재 enum의 `toString()` 값입니다. 기본값은 `ARTICLE_NOT_FOUND` 같은 상수 이름이므로, 오류 코드 enum에서는 `toString()`을 재정의하지 않습니다.

### 이미 준비된 도메인 부모 예외

아래 클래스는 모두 `MonewException`을 상속하고, `(ErrorCode errorCode, Map<String, Object> details)` 생성자를 제공합니다. 각 담당자는 이 클래스를 부모로 삼아 구체적인 예외를 만듭니다.

| 도메인 | 예외 클래스 | 패키지 |
|---|---|---|
| 사용자 | `UserException` | `com.sprint.monew.global.exception.user` |
| 기사 | `ArticleException` | `com.sprint.monew.global.exception.article` |
| 댓글 | `CommentException` | `com.sprint.monew.global.exception.comment` |
| 관심사 | `InterestException` | `com.sprint.monew.global.exception.interest` |
| 알림 | `NotificationException` | `com.sprint.monew.global.exception.notification` |
| 활동 내역 | `ActivityException` | `com.sprint.monew.global.exception.activity` |

새 오류를 추가할 때는 다음 순서로 작성합니다.

1. 도메인 enum에 상태·메시지·로그 레벨을 정의합니다.
2. 위 도메인 부모를 상속하는 구체적인 예외를 만듭니다.
3. 구체적인 예외의 생성자에서 오류 코드와 추가 정보를 부모 생성자에 전달합니다.
4. 서비스에서 구체적인 예외를 던집니다.

`CommonErrorCode`에는 여러 도메인이 공통으로 사용하는 오류를 둡니다.

## 4. 공통 핸들러가 처리하는 경우

| 예외 | 발생 상황 | HTTP 상태 | 응답 코드 |
|---|---|---|---|
| `MonewException` 및 하위 예외 | 서비스에서 오류 코드를 지정해 던짐 | 해당 오류 코드의 설정값 | 해당 enum 상수 이름 |
| `HttpMessageNotReadableException` | 잘못된 JSON, 필수 요청 본문 누락 등 | 400 | `INCORRECT_REQUEST` |
| `MethodArgumentTypeMismatchException` | UUID가 필요한 경로 변수에 `abc`를 전달하는 등 타입 변환 실패 | 400 | `INCORRECT_REQUEST` |
| `MethodArgumentNotValidException` | DTO 바인딩 과정에서 `@Valid` 검증 실패 | 400 | `INCORRECT_REQUEST` |
| 위 핸들러에 해당하지 않는 `Exception` | 별도 처리되지 않은 예외 | 500 | `INTERNAL_SERVER_ERROR` |

Spring은 메서드 작성 순서가 아니라 발생한 예외 타입에 맞는 핸들러를 선택합니다. `UserNotFoundException → UserException → MonewException`처럼 여러 단계로 상속해도 첫 번째 처리에 포함되므로 구체적인 예외마다 별도 핸들러를 추가할 필요가 없습니다.

### 요청 형식 오류와 검증 오류의 차이

`{"title": }`처럼 JSON 문법이 잘못되면 요청을 읽는 과정에서 실패합니다. 이때는 `HttpMessageNotReadableException` 핸들러가 처리하며 `details`는 빈 객체입니다.

`{"title": ""}`는 읽을 수 있는 JSON입니다. 다만 DTO의 `title`에 `@NotBlank`가 있고 컨트롤러에서 `@Valid @RequestBody`로 검증한다면 `MethodArgumentNotValidException`이 발생합니다.

이때 응답의 `details`에는 필드별 검증 메시지가 들어갑니다. 아래 메시지는 `@NotBlank(message = "제목은 필수입니다.")`로 지정한 경우의 예시입니다.

```json
{
  "timestamp": "2026-10-20T01:00:00Z",
  "code": "INCORRECT_REQUEST",
  "message": "올바른 입력이 아닙니다.",
  "details": {
    "title": "제목은 필수입니다."
  },
  "exceptionType": "MethodArgumentNotValidException",
  "status": 400
}
```

현재 `details`에는 필드당 메시지 하나를 저장합니다. 같은 필드에서 여러 검증이 실패하면 나중에 처리한 메시지가 앞의 메시지를 덮어씁니다.

로그의 `errorCount`는 이번 요청의 검증 오류 건수입니다. 서버 전체의 누적 오류 횟수가 아니며, 응답 JSON에도 포함하지 않습니다. 한 필드에 여러 검증 오류가 생길 수 있으므로 `details`에 담긴 필드 개수와 다를 수 있습니다.

## 5. 응답 필드와 로그

| 필드 | 의미 |
|---|---|
| `timestamp` | UTC 기준 시각. 도메인 예외는 예외 생성 시각, 나머지는 핸들러 처리 시각 |
| `code` | 클라이언트에서 오류를 구분할 문자열 |
| `message` | 클라이언트에 전달할 오류 설명 |
| `details` | 대상 ID 또는 검증 실패 필드 등 추가 정보 |
| `exceptionType` | 실제 예외 클래스의 단순 이름. 예: `UserNotFoundException` |
| `status` | HTTP 상태 코드 숫자. 예: `404` |

Java 필드는 `Instant timestamp`입니다. `Instant`는 시간 타입이고, `timestamp`는 JSON에 표시할 필드 이름입니다.

예상하지 못한 예외는 서버 로그에 원인과 스택 트레이스를 남깁니다. 응답의 `message`에는 `예상하지 못한 서버 오류가 발생했습니다.`라는 공통 메시지를 사용합니다.

도메인에서 예상한 조회 실패 등은 해당 도메인 예외로 던집니다. 일반 `IllegalArgumentException`이나 `NoSuchElementException`을 직접 던지면 현재 구성에서는 마지막 핸들러가 500으로 처리합니다.

## 6. 변경 후 확인하기

1. 도메인 예외가 지정한 HTTP 상태와 JSON 응답을 만드는지 확인합니다.
2. 잘못된 JSON·UUID 요청이 400으로 응답하는지 확인합니다.
3. `@Valid` 검증 실패가 400과 필드별 `details`를 반환하는지 확인합니다.
4. 예상하지 못한 예외가 500과 공통 메시지를 반환하는지 확인합니다.

코드 자동 정리와 전체 빌드는 다음 명령으로 실행합니다.

```bash
./gradlew spotlessApply
./gradlew build
```

`spotlessApply`는 Java 소스의 들여쓰기·줄바꿈 등을 정리합니다. JSON 응답 필드를 변경하는 명령이 아닙니다. 전체 빌드에는 DB 연동 테스트가 포함되므로 Docker 실행 등 환경 준비는 [온보딩 가이드](../../../../../../../../docs/ONBOARDING.md)를 따릅니다.
