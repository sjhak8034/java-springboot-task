package com.example.javaspringboottask.global.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

public class DelegatedAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final HandlerExceptionResolver resolver;

    public DelegatedAuthenticationEntryPoint(@Qualifier("handlerExceptionResolver")HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * 인증되지 않은 요청을 처리합니다.
     *
     * @param request       요청 객체로, 문제가 발생한 HTTP 요청 정보
     * @param response      응답 객체로, 클라이언트로 보낼 정보
     * @param authException 인증되지 않은 경우 발생하는 예외 객체
     */

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
            resolver.resolveException(request, response, null, authException);
    }
}
