package com.example.javaspringboottask.user.dto;

import com.example.javaspringboottask.global.valid.ValidPassword;
import com.example.javaspringboottask.user.entity.type.Role;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Getter
@RequiredArgsConstructor
public class SignupRequestDto {

    @Length(min=5, max = 20)
    private final String username;
    @ValidPassword
    private final String password;
    @Length(min=5, max = 20)
    private final String nickname;
}
