package com.example.javaspringboottask.user.dto;

import com.example.javaspringboottask.user.entity.type.Role;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class GrantAdminResponseDto {
    private final String username;
    private final String nickname;
    private final Role role;
}
