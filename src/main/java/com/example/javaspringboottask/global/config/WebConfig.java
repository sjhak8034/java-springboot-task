package com.example.javaspringboottask.global.config;

import com.example.javaspringboottask.user.entity.type.Role;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security의 전반적인 설정을 담당하는 클래스. CORS 설정, 보안 필터 체인, 예외 처리 및 인증 프로바이더를 설정한다.
 */
@Configuration
@EnableWebSecurity // SecurityFilterChain 빈을 설정하기 위해 필요
@RequiredArgsConstructor
public class WebConfig {

    private final JwtAuthFilter jwtAuthFilter; // JWT 인증 필터
    private final AuthenticationProvider authenticationProvider; // 인증 제공자 (AuthenticationProvider)
    private final AuthenticationEntryPoint authEntryPoint; // 인증 실패 시 처리할 핸들러
    private final AccessDeniedHandler accessDeniedHandler; // 접근 거부 시 처리할 핸들러
    private final SecurityProperties securityProperties; // 보안 설정 속성값을 관리하는 클래스

    /**
     * CORS 설정을 정의하는 메서드. 모든 출처, 모든 HTTP 메서드, 모든 헤더를 허용하고 인증 정보를 포함할 수 있도록 설정한다.
     *
     * @return CORS 설정을 포함한 CorsConfigurationSource 객체
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*")); // 모든 도메인 허용
        configuration.addAllowedMethod("*"); // 모든 HTTP 메서드 허용
        configuration.addAllowedHeader("*"); // 모든 헤더 허용
        configuration.addExposedHeader("Authorization"); // 클라이언트가 접근 가능한 헤더
        configuration.addExposedHeader("Set-Cookie"); // 클라이언트가 접근 가능한 쿠키 설정
        configuration.setAllowCredentials(true); // 인증 정보 포함 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 모든 요청 URL에 대해 적용
        return source;
    }

    /**
     * Spring Security 필터 체인을 설정하는 메서드. JWT 기반 인증 및 인가 설정, CORS 정책, 예외 처리, 세션 관리 등을 수행한다.
     *
     * @param http HttpSecurity 객체
     * @return SecurityFilterChain 객체
     * @throws Exception 예외 발생 시 처리
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 설정 적용
                .csrf(AbstractHttpConfigurer::disable) // CSRF 보호 비활성화 (JWT 사용 시 필요)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(securityProperties.getWhiteList().toArray(new String[0]))
                        .permitAll() // 화이트 리스트 경로 허용
                        .requestMatchers(HttpMethod.POST, "/refresh").permitAll() // JWT 토큰 갱신 허용
                        .requestMatchers(HttpMethod.POST, "/users/logout").permitAll()
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll() // 정적 리소스 허용
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.INCLUDE,
                                DispatcherType.ERROR).permitAll() // 특정 요청 유형 허용
                        .requestMatchers(securityProperties.getAdminAuthList().toArray(new String[0]))
                        .hasRole(Role.ADMIN.getName().toUpperCase()) // 관리자 권한이 필요한 요청 경로 설정
                        .requestMatchers(securityProperties.getUserAuthList().toArray(new String[0]))
                        .hasRole(Role.USER.getName().toUpperCase()) // 판매자 권한이 필요한 요청 경로 설정
                        .anyRequest().authenticated() // 나머지 요청은 인증 필요
                )
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(authEntryPoint) // 인증 실패 시 핸들러 지정
                        .accessDeniedHandler(accessDeniedHandler)) // 접근 거부 시 핸들러 지정
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS)) // 세션을 사용하지 않는 Stateless 방식 적용
                .authenticationProvider(authenticationProvider) // 커스텀 인증 제공자 설정
                .addFilterAfter(jwtAuthFilter, ExceptionTranslationFilter.class); // JWT 인증 필터 추가

        return http.build();
    }
}

