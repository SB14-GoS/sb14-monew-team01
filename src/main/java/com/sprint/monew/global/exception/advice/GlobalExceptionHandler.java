package com.sprint.monew.global.exception.advice;

import com.sprint.monew.global.exception.CommonErrorCode;
import com.sprint.monew.global.exception.ErrorCode;
import com.sprint.monew.global.exception.ErrorResponse;
import com.sprint.monew.global.exception.MonewException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MonewException.class)
    /* 도메인 로직에서 지정해서 던짐 */
    public ResponseEntity<ErrorResponse> handleDiscodeitException(MonewException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.atLevel(errorCode.getLevel())
                .setCause(e)
                .log("요청 처리 실패 - code: {}, details: {}", errorCode, e.getDetails());

        ErrorResponse errorResponse = new ErrorResponse(
                e.getTimestamp(),
                errorCode.toString(),
                errorCode.getMessage(),
                e.getDetails(),
                e.getClass().getSimpleName(),
                errorCode.getStatus().value());
        return ResponseEntity.status(errorCode.getStatus()).body(errorResponse);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    /* 요청이 잘못들어옴 */
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
        ErrorCode errorCode = CommonErrorCode.INCORRECT_REQUEST;
        log.atLevel(errorCode.getLevel())
                .log("잘못된 요청- exceptionType: {}", e.getClass().getSimpleName());
        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                errorCode.toString(),
                errorCode.getMessage(),
                Map.of(),
                e.getClass().getSimpleName(),
                errorCode.getStatus().value());
        return ResponseEntity.status(errorCode.getStatus()).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    /* @Valid에 걸림 */
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        ErrorCode errorCode = CommonErrorCode.INCORRECT_REQUEST;
        Map<String, Object> details = new LinkedHashMap<>();
        e.getBindingResult()
                .getFieldErrors()
                .forEach(fieldError -> details.put(fieldError.getField(), fieldError.getDefaultMessage()));
        log.atLevel(errorCode.getLevel())
                .log(
                        "유효성 검사 실패 - code: {}, errorCount: {}",
                        errorCode,
                        e.getBindingResult().getErrorCount());
        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                errorCode.toString(),
                errorCode.getMessage(),
                details,
                e.getClass().getSimpleName(),
                errorCode.getStatus().value());
        return ResponseEntity.status(errorCode.getStatus()).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    /* 나머지 예외 */
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("예상하지 못한 내부 오류", e);
        ErrorCode errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;
        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                errorCode.toString(),
                errorCode.getMessage(),
                Map.of(),
                e.getClass().getSimpleName(),
                errorCode.getStatus().value());
        return ResponseEntity.status(errorCode.getStatus()).body(errorResponse);
    }
}
