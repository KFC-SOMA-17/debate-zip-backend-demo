package com.debatezip.common.exception;

// HTTP 상태값을 들고 있지만, spring-web에 의존하지 않기 위해 정수로 보관한다.
// HTTP 매핑은 api 모듈의 GlobalExceptionHandler에서 수행한다.
public enum ErrorCode {

    // 공통
    INTERNAL_SERVER_ERROR(500, "C001", "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT(400, "C002", "잘못된 입력값입니다."),
    RESOURCE_NOT_FOUND(404, "C003", "요청한 리소스를 찾을 수 없습니다."),
    UNAUTHORIZED(401, "C004", "인증이 필요합니다."),
    FORBIDDEN(403, "C005", "접근 권한이 없습니다."),

    // STT (Speech-to-Text)
    STT_RECOGNITION_FAILED(502, "S001", "음성 인식에 실패했습니다."),
    STT_CANCELED(502, "S002", "음성 인식이 중단되었습니다.");

    private final int status;
    private final String code;
    private final String message;

    ErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
