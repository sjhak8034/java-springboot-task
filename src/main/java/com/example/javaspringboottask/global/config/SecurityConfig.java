package com.example.javaspringboottask.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Spring Security의 인증 관련 설정을 담당하는 클래스.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService; // 사용자 인증 정보를 관리하는 서비스

    /**
     * 비밀번호 암호화를 위한 BCryptPasswordEncoder를 빈으로 등록한다.
     *
     * @return BCryptPasswordEncoder 객체
     */
    @Bean
    BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager를 빈으로 등록. Spring Security의 인증을 관리하는 역할을 한다.
     *
     * @param config AuthenticationConfiguration 객체
     * @return AuthenticationManager 객체
     * @throws Exception 예외 발생 시 처리
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 사용자 인증을 담당하는 AuthenticationProvider를 빈으로 등록. DaoAuthenticationProvider를 사용하여 데이터베이스 기반 인증을
     * 수행한다.
     *
     * @return AuthenticationProvider 객체
     */
    @Bean
    AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService); // 사용자 정보 서비스 설정
        provider.setPasswordEncoder(bCryptPasswordEncoder()); // 비밀번호 암호화 설정
        return provider;
    }
}
