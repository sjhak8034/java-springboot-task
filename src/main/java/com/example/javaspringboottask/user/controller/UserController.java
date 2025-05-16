package com.example.javaspringboottask.user.controller;

import com.example.javaspringboottask.global.exception.CustomResponseStatusException;
import com.example.javaspringboottask.global.exception.ErrorCode;
import com.example.javaspringboottask.refresh.service.RefreshTokenService;
import com.example.javaspringboottask.user.dto.*;
import com.example.javaspringboottask.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(summary = "회원가입", description = "새로운 사용자 계정을 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "중복된 사용자 이름 존재"),
    })
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
            summary = "로그인",
            description = "입력한 사용자 정보로 로그인하고, AccessToken과 RefreshToken을 반환합니다. " +
                    "RefreshToken은 HttpOnly 쿠키로, AccessToken은 Authorization 헤더에 담겨 전달됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "아이디 없음 / 비밀번호 불일치 / 인증 실패"),
    })
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
            summary = "관리자 권한 부여",
            description = "지정된 userId에 대해 ROLE_ADMIN 권한을 부여합니다. 현재 인증된 사용자의 이름도 함께 사용됩니다." +
                    "테스트용 adminId: john_doe, adminPassword: Password123!"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "권한 부여 성공"),
            @ApiResponse(responseCode = "400", description = "접근 권한이 없는 경우 / 이미 존재하는 관리자"),
            @ApiResponse(responseCode = "404", description = "대상 사용자 혹은 관리자 유저를 찾을 수 없음")
    })
    @PostMapping("/admin/users/{userId}/roles")
    public ResponseEntity<GrantAdminResponseDto> grantAdmin(
            @PathVariable Long userId,
            Authentication authentication) {

        String username = authentication.getName();
        return ResponseEntity.ok(userService.grantAdmin(userId, username));
    }

}
