package com.example.javaspringboottask.global.filter;

import com.example.javaspringboottask.global.config.SecurityProperties;
import com.example.javaspringboottask.global.exception.CustomResponseStatusException;
import com.example.javaspringboottask.global.exception.ErrorCode;
import com.example.javaspringboottask.global.util.AuthenticationScheme;
import com.example.javaspringboottask.global.util.JwtProvider;
import com.example.javaspringboottask.refresh.service.RefreshTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider; // JWT 토큰 생성 및 검증 유틸리티
    private final UserDetailsService userDetailsService; // 사용자 정보를 로드하는 서비스
    private final RefreshTokenService refreshTokenService; // 리프레시 토큰, Redis 관련 서비스
    private final SecurityProperties securityProperties; // 화이트리스트 및 HTTP Method 관련 설정
    private final AntPathMatcher pathMatcher = new AntPathMatcher(); // 경로 패턴 매칭 유틸리티

    /**
     * 요청이 필터 체인을 통과하기 전에 실행되는 메서드. 화이트리스트 및 HTTP Method 패턴을 확인한 후, 인증 과정을 수행.
     *
     * @param request     HTTP 요청
     * @param response    HTTP 응답
     * @param filterChain 필터 체인 객체
     * @throws ServletException 필터 실행 중 발생한 예외
     * @throws IOException      필터 실행 중 I/O 예외
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 요청 URI와 HTTP Method 가져오기
            String requestUri = request.getRequestURI();
            String method = request.getMethod();

            // 1. 화이트리스트 확인: 해당 요청 URI가 화이트리스트에 포함되어 있으면 필터를 통과시킴
            if (securityProperties.getWhiteList().stream()
                    .anyMatch(pattern -> pathMatcher.match(pattern, requestUri))) {
                filterChain.doFilter(request, response);
                return;
            }

            // 2. HTTP Method 및 특정 경로 패턴 확인
            Map<HttpMethod, List<String>> methodPatterns = securityProperties.getMethodSpecificPatterns();
            if (methodPatterns.containsKey(HttpMethod.valueOf(method))) {
                if (methodPatterns.get(HttpMethod.valueOf(method)).stream()
                        .anyMatch(pattern -> pathMatcher.match(pattern, requestUri))) {
                    filterChain.doFilter(request, response);
                    return;
                }
            }

            // 3. 인증 처리
            this.authenticate(request);

            // 4. 필터 체인 계속 실행
            filterChain.doFilter(request, response);
        } catch (CustomResponseStatusException ex) {
            response.setStatus(ex.getErrorCode().getHttpStatus().value());
            response.setContentType("application/json");
            response.getWriter()
                    .write("{\"error\": \"" + ex.getErrorCode() + ex.getMessage() + "\"}");
        }
    }

    /**
     * 요청에서 JWT 토큰을 추출하고 인증 과정을 수행.
     *
     * @param request HTTP 요청 객체
     */
    private void authenticate(HttpServletRequest request) throws CustomResponseStatusException {
        try {
            // 요청 헤더에서 토큰 추출
            String token = this.getTokenFromRequest(request);

            // 토큰이 유효하지 않으면 인증 처리하지 않음
            if (token == null || !jwtProvider.validToken(token)) {
                return;
            }

            // 블랙리스트 토큰 검증 (예외 발생 시 catch 후 응답 처리)
            if (refreshTokenService.isTokenBlacklisted(token)) {
                throw new CustomResponseStatusException(ErrorCode.BLACKLIST_TOKEN);
            }

            // 유효한 토큰인 경우 사용자 정보를 가져와 인증 객체 생성
            String username = jwtProvider.getUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // SecurityContext에 인증 객체 저장
            this.setAuthentication(request, userDetails);
        } catch (CustomResponseStatusException ex) {
            throw ex; // 예외를 필터에서 처리하도록 던짐
        }
    }

    /**
     * HTTP 요청 헤더에서 Authorization 토큰 값을 추출.
     *
     * @param request HTTP 요청 객체
     * @return 추출된 JWT 토큰 값 (존재하지 않으면 null 반환)
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        // Authorization 헤더에서 토큰 값을 가져옴
        final String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        // "Bearer " 접두어와 토큰 값 분리
        final String headerPrefix = AuthenticationScheme.generateType(AuthenticationScheme.BEARER);

        // 토큰이 존재하고 "Bearer "로 시작하는지 확인
        boolean tokenFound =
                StringUtils.hasText(bearerToken) && bearerToken.startsWith(headerPrefix);

        // "Bearer "를 제거하고 토큰 값만 반환
        if (tokenFound) {
            return bearerToken.substring(headerPrefix.length());
        }

        // 유효한 토큰이 없는 경우 null 반환
        return null;
    }

    /**
     * 사용자 정보를 기반으로 SecurityContext에 인증 객체를 저장.
     *
     * @param request     HTTP 요청 객체
     * @param userDetails 사용자 정보를 포함한 UserDetails 객체
     */
    private void setAuthentication(HttpServletRequest request, UserDetails userDetails) {
        // 인증 객체 생성 (사용자 정보, 자격 증명, 권한 목록 포함)
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        // 요청 세부 정보를 인증 객체에 추가
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // SecurityContext에 인증 객체 저장
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
