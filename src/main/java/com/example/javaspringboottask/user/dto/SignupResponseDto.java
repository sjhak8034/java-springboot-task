package com.example.javaspringboottask.user.dto;


import com.example.javaspringboottask.user.entity.type.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;


@Getter
@RequiredArgsConstructor
public class SignupResponseDto {

    @Schema(description = "사용자 이름", example = "john_doe123")
    private final String username;

    @Schema(description = "사용자 닉네임", example = "cool_nick")
    private final String nickname;

    @Schema(description = "사용자 권한 (예: ROLE_USER, ROLE_ADMIN)", example = "ROLE_USER")
    private final Role role;
}