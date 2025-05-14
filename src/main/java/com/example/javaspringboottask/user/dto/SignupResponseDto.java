package com.example.javaspringboottask.user.dto;


import com.example.javaspringboottask.user.entity.type.Role;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class SignupResponseDto {

    private final String username;

    private final String nickname;

    private final Role role;
}
