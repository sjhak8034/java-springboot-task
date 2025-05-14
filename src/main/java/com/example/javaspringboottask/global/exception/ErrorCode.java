package com.example.javaspringboottask.global.exception;


import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    /**
     * Server
     */
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "유효성 검사 실패"),
    CONSTRAINT_VIOLATION(HttpStatus.CONFLICT, "제약 조건 위반"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류가 발생하였습니다."),

    /**
     * not found
     */
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "User를 찾을 수 없습니다"),

    /**
     * user
     */
    DUPLICATE_USERNAME(HttpStatus.BAD_REQUEST, "중복된 username 입니다"),
    /**
     * unAuthorized
     */
    NOT_ALLOW_USER(HttpStatus.UNAUTHORIZED, "USER 권한은 사용할 수 없는 기능입니다"),
    FORBIDDEN_ADMIN_ROLE_REQUIRED(HttpStatus.UNAUTHORIZED, "ADMIN 권한만 사용할 수 있는 기능입니다"),

    /**
     * Jwt error
     */
    UNAUTHORIZED_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 비어있거나 만료되었거나 지원하지않습니다."),
    BLACKLIST_TOKEN(HttpStatus.UNAUTHORIZED, "블랙리스트된 토큰입니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
