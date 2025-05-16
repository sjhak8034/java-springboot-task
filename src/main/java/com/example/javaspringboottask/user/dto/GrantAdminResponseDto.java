package com.example.javaspringboottask.user.dto;

import com.example.javaspringboottask.user.entity.type.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class GrantAdminResponseDto {

    @Schema(description = "권한이 부여된 사용자 이름", example = "john_doe")
    private final String username;

    @Schema(description = "권한이 부여된 사용자 닉네임", example = "admin_john")
    private final String nickname;

    @Schema(description = "변경된 사용자 권한", example = "ROLE_ADMIN")
    private final Role role;
}