package com.example.javaspringboottask.refresh.controller;


import com.example.javaspringboottask.global.exception.CustomResponseStatusException;
import com.example.javaspringboottask.global.exception.ErrorCode;
import com.example.javaspringboottask.refresh.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.example.javaspringboottask.global.constant.TokenPrefix.TOKEN_PREFIX;

@Tag(
        name = "refresh토큰 재발급 API",
        description = "refresh토큰 재발급 관련 API"
)
@RestController
@RequiredArgsConstructor
public class RefreshTokenController {

    private final RefreshTokenService refreshTokenService;

    /**
     * 리프레시 토큰을 사용해 새로운 액세스 토큰을 발급.
     *
     * @param refreshToken   쿠키에 저장된 리프레시 토큰 값
     * @param authentication 현재 인증된 인증 객
     * @return 새로운 액세스 토큰을 AUTHORIZATION 헤더에 포함해 반환
     */
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(@CookieValue("refreshToken") String refreshToken
        , Authentication authentication) {
        try {
            // 리프레시 토큰으로 새로운 액세스 토큰 발급
            String newAccessToken = refreshTokenService.generateAccessTokenFromRefreshToken(
                refreshToken);

            // 새로 발급된 액세스 토큰을 AUTHORIZATION 헤더에 포함하여 반환
            return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, TOKEN_PREFIX + newAccessToken)
                .build();

        } catch (CustomResponseStatusException e) {
            // 리프레시 토큰이 만료되었거나 유효하지 않은 경우
            if (e.getErrorCode() == ErrorCode.UNAUTHORIZED_TOKEN) {
                // 만료된 리프레시 토큰 삭제
                refreshTokenService.deleteRefreshToken(authentication);

                // 쿠키 삭제를 위해 만료시간을 0으로 설정
                ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "")
                    .httpOnly(true)
                    .path("/")
                    .maxAge(0)
                    .build();

                // UNAUTHORIZED 상태 코드와 함께 만료된 쿠키 반환
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .build();
            }
            // 다른 예외는 그대로 전파
            throw e;
        }
    }
}
