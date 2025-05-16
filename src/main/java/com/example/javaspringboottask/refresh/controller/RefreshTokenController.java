package com.example.javaspringboottask.refresh.controller;


import com.example.javaspringboottask.global.exception.CustomResponseStatusException;
import com.example.javaspringboottask.global.exception.ErrorCode;
import com.example.javaspringboottask.refresh.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
        name = "Refresh 토큰 재발급 API",
        description = "Refresh 토큰을 이용해 Access Token을 재발급합니다."
)
@RestController
@RequiredArgsConstructor
public class RefreshTokenController {

    private final RefreshTokenService refreshTokenService;
    private static final String TOKEN_PREFIX = "Bearer ";

    @Operation(
            summary = "AccessToken 재발급",
            description = "쿠키에 저장된 Refresh Token을 사용하여 새로운 Access Token을 발급합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "새로운 액세스 토큰 발급 성공"),
                    @ApiResponse(responseCode = "401", description = "Refresh 토큰 만료 또는 유효하지 않음", content = @Content)
            }
    )
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            @Parameter(description = "HttpOnly 쿠키로 전달되는 Refresh Token", required = true)
            @CookieValue("refreshToken") String refreshToken,

            @Parameter(hidden = true) // Swagger UI에는 굳이 노출 안 해도 됨
            Authentication authentication
    ) {
        try {
            String newAccessToken = refreshTokenService.generateAccessTokenFromRefreshToken(refreshToken);

            return ResponseEntity.ok()
                    .header(HttpHeaders.AUTHORIZATION, TOKEN_PREFIX + newAccessToken)
                    .build();

        } catch (CustomResponseStatusException e) {
            if (e.getErrorCode() == ErrorCode.UNAUTHORIZED_TOKEN) {
                refreshTokenService.deleteRefreshToken(authentication);

                ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "")
                        .httpOnly(true)
                        .path("/")
                        .maxAge(0)
                        .build();

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                        .build();
            }
            throw e;
        }
    }
}