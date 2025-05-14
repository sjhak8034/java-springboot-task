package com.example.javaspringboottask.global.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

/**
 * 사용자가 인증은 되었지만 요청한 자원에 접근 권한이 없을 경우 발생하는 {@link AccessDeniedException}을 처리합니다.
 * 이 클래스는 Spring의 {@link HandlerExceptionResolver}를 사용하여 예외 처리를 위임하므로, 애플리케이션 전반에서 일관된 방식으로 에러를 처리할
 * 수 있습니다.
 */
@Component
public class DelegatedAccessDeniedHandler implements AccessDeniedHandler {

    private final HandlerExceptionResolver resolver;

    public DelegatedAccessDeniedHandler(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }
    /**
     * 접근 권한이 없는 요청을 처리합니다.
     *
     * @param request               요청 객체로, 문제가 발생한 HTTP 요청 정보
     * @param response              응답 객체로, 클라이언트로 보낼 정보
     * @param accessDeniedException 접근 권한이 없을 때 발생하는 예외 객체
     * @throws IOException      입출력 관련 예외가 발생한 경우.
     * @throws ServletException 서블릿 처리 중 예외가 발생한 경우.
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
        response.setCharacterEncoding("UTF-8"); // 인코딩 설정 추가
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write("""
            {
              "status": 403,
              "error": "Forbidden",
              "message": "요청한 리소스에 접근할 권한이 없습니다."
            }
        """);
        resolver.resolveException(request,response,null,accessDeniedException);
    }

}
