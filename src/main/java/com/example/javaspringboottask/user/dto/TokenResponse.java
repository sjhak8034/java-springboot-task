package com.example.javaspringboottask.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TokenResponse {

    @Schema(
            description = "Access Token (Bearer 토큰 형식, Authorization 헤더에 포함됨)",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    private final String accessToken;

    @Schema(
            description = "Refresh Token (HttpOnly 쿠키로 전송됨)",
            example = "dGhpc0lzQVNlY3JldFJlZnJlc2hUb2tlbg=="
    )
    private final String refreshToken;
}