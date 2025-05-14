package com.example.javaspringboottask.global.exception;

import org.springframework.web.server.ResponseStatusException;

public class CustomResponseStatusException extends ResponseStatusException {

    private final ErrorCode errorCode;

    public CustomResponseStatusException(ErrorCode errorCode) {
        super(errorCode.getHttpStatus(), errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
