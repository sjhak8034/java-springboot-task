package com.example.javaspringboottask.user.controller;

import com.example.javaspringboottask.global.exception.CustomResponseStatusException;
import com.example.javaspringboottask.global.exception.ErrorCode;
import com.example.javaspringboottask.refresh.service.RefreshTokenService;
import com.example.javaspringboottask.user.dto.*;
import com.example.javaspringboottask.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;

import static com.example.javaspringboottask.global.constant.TokenPrefix.TOKEN_PREFIX;

@Tag(
        name = "인증/인가 API",
        description = "인증/인가 관련 API"
)

@RestController
@RequestMapping()
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    @Operation(
            summary = "회원가입"
    )
    @PostMapping("/signup")
    public ResponseEntity<SignupResponseDto> signup(@RequestBody @Valid SignupRequestDto requestDto) {

        return ResponseEntity.ok(userService.signup(requestDto));
    }

    /**
     * 사용자 로그인
     *
     * @param requestDto 로그인 관련 정보를 담고있는 요청 DTO
     * @return 정상 처리시 헤더에 액세스토큰, 쿠키에 리프레시 토큰을 반환
     */
    @Operation(
            summary = "로그인"
    )
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid SigninRequestDto requestDto) {

        TokenResponse tokenResponse = userService.tokenGenerate(requestDto);
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken",
                        tokenResponse.getRefreshToken())
                .httpOnly(true)
                .path("/")
                .secure(false)
                .maxAge(7 * 24 * 60 * 60)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.AUTHORIZATION, TOKEN_PREFIX + tokenResponse.getAccessToken())
                .body(tokenResponse);
    }

    @Operation(
            summary = "관리자 권한 부여"
    )
    @PostMapping("/admin/users/{userId}/roles")
    public ResponseEntity<GrantAdminResponseDto> grantAdmin(@PathVariable Long userId, Authentication authentication){
        String username = authentication.getName();
        return ResponseEntity.ok(userService.grantAdmin(userId,username));
    }


}
