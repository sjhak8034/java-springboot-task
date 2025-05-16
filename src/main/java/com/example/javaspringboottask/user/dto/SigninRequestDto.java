package com.example.javaspringboottask.user.dto;

import com.example.javaspringboottask.global.valid.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Getter
@RequiredArgsConstructor
public class SigninRequestDto {

    @Schema(description = "사용자 이름 (5~20자)", example = "john_doe123")
    @Length(min = 5, max = 20)
    @NotBlank
    private final String username;

    @Schema(description = "비밀번호 (복잡성 유효성 검사가 적용됨)", example = "Password123!")
    @ValidPassword
    @NotBlank
    private final String password;
}