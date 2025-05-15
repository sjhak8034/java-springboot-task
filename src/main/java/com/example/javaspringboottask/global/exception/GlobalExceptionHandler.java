package com.example.javaspringboottask.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class GlobalExceptionHandler {



    // CustomResponseStatusException에 대한 처리
    @ExceptionHandler(CustomResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleCustomResponseStatusException(CustomResponseStatusException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getErrorCode().getHttpStatus().value(),
                ex.getErrorCode().name(),
                ex.getErrorCode().getMessage()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // ResponseStatusException에 대한 처리
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getStatusCode().value(),
                ex.getMessage(),
                ex.getReason()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}


