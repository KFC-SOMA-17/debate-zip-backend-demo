package com.debatezip.api;

import com.debatezip.common.exception.BusinessException;
import com.debatezip.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 도메인이 던진 BusinessException을 HTTP 응답으로 변환한다.
// 도메인은 HTTP를 모르고, common의 ErrorCode는 정수 status만 들고 있다.
// HTTP 매핑 책임은 이 모듈(api)이 진다.
@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorResponse(String code, String message) {
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity
                .status(HttpStatus.valueOf(errorCode.getStatus()))
                .body(new ErrorResponse(errorCode.getCode(), ex.getMessage()));
    }
}
