package com.example.javaspringboottask.user.controller;

import com.example.javaspringboottask.global.exception.CustomResponseStatusException;
import com.example.javaspringboottask.global.exception.ErrorCode;
import com.example.javaspringboottask.user.dto.*;
import com.example.javaspringboottask.user.entity.User;
import com.example.javaspringboottask.user.entity.type.Role;
import com.example.javaspringboottask.user.repository.UserRepository;
import com.example.javaspringboottask.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;


    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    // 회원가입 성공
    @Test
    @Transactional
    void signup_success() throws Exception {
        SignupRequestDto request = new SignupRequestDto("test@example2.com", "Password123!", "nickname");
        SignupResponseDto response = new SignupResponseDto(request.getUsername(), request.getNickname(), Role.USER);


        this.mockMvc.perform(MockMvcRequestBuilders.post("/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(response.getUsername()))
                .andExpect(jsonPath("$.nickname").value(response.getNickname()))
                .andExpect(jsonPath("$.role").value(response.getRole().name()));

    }

    // 회원가입 실패 - 중복 username
    @Test
    @Transactional
    void signup_duplicateEmail() throws Exception {
        String username = "test@example2.com";
        String password = "Password123!";
        String nickname = "nickname";
        User user = new User(username, nickname ,bCryptPasswordEncoder.encode(password));
        userRepository.save(user);

        SignupRequestDto request = new SignupRequestDto(username, password, nickname);


        mockMvc.perform(MockMvcRequestBuilders.post("/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ErrorCode.DUPLICATE_USERNAME.getMessage()));
    }

    // 로그인 성공
    @Test
    @Transactional
    void login_success() throws Exception {
        String username = "test@example2.com";
        String password = "Password123!";
        String nickname = "nickname";
        User user = new User(username, nickname ,bCryptPasswordEncoder.encode(password));
        userRepository.save(user);

        SigninRequestDto signinRequestDto = new SigninRequestDto(username, password);


        mockMvc.perform(MockMvcRequestBuilders.post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signinRequestDto)))
                .andExpect(status().isOk())  // 상태 코드가 200 OK인지 확인
                .andExpect(jsonPath("$.accessToken").exists())  // 응답에 accessToken이 존재하는지 확인
                .andExpect(jsonPath("$.refreshToken").exists())  // 응답에 refreshToken이 존재하는지 확인
                .andExpect(cookie().exists("refreshToken"));  // refreshToken 쿠키 존재 여부 확인
    }

    // 로그인 실패 - 잘못된 비밀번호
    @Test
    @Transactional
    void login_invalidCredentials() throws Exception {

        String username = "test@example2.com";
        String password = "Password123!";
        String nickname = "nickname";
        User user = new User(username, nickname ,bCryptPasswordEncoder.encode("wrondpassword!"));
        userRepository.save(user);
        SigninRequestDto request = new SigninRequestDto(username, password);


        mockMvc.perform(MockMvcRequestBuilders.post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("비밀번호가 올바르지 않습니다."));
    }

    // 관리자 권한 부여 - 성공
    @Test
    @Transactional
    void grantAdmin_success() throws Exception {
        String adminName = "test@example2.com";
        String nickname = "nickname";
        String password = "Password123!";
        User admin = new User(adminName, nickname ,bCryptPasswordEncoder.encode(password),Role.ADMIN);
        userRepository.save(admin);

        String username = "test@example3.com";
        User user = new User(username, nickname ,bCryptPasswordEncoder.encode(password),Role.USER);
        userRepository.save(user);

        GrantAdminResponseDto response = new GrantAdminResponseDto(username, nickname, Role.ADMIN);

        SigninRequestDto signinRequestDto = new SigninRequestDto(adminName, password);


        String accessToken = mockMvc.perform(MockMvcRequestBuilders.post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signinRequestDto)))
                .andExpect(status().isOk())  // 상태 코드가 200 OK인지 확인
                .andExpect(jsonPath("$.accessToken").exists())  // 응답에 accessToken이 존재하는지 확인
                .andExpect(jsonPath("$.refreshToken").exists())  // 응답에 refreshToken이 존재하는지 확인
                .andExpect(cookie().exists("refreshToken"))
                .andReturn().getResponse().getHeader("Authorization");  // refreshToken 쿠키 존재 여부 확인


        mockMvc.perform(MockMvcRequestBuilders.post("/admin/users/{userId}/roles", user.getId())
                        .header("Authorization", accessToken))  // JWT 토큰을 헤더에 포함시켜서 요청
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value(Role.ADMIN.toString()))
                .andExpect(jsonPath("$.nickname").value(nickname));

    }

    // 관리자 권한 부여 - 일반 사용자 접근
    @Test
    @Transactional
    void grantAdmin_forbidden() throws Exception {
        String adminName = "test@example2.com";
        String nickname = "nickname";
        String password = "Password123!";

        User admin = new User(adminName, nickname ,bCryptPasswordEncoder.encode(password),Role.USER);
        userRepository.save(admin);

        String username = "test@example3.com";
        User user = new User(username, nickname ,bCryptPasswordEncoder.encode(password),Role.USER);
        userRepository.save(user);

        GrantAdminResponseDto response = new GrantAdminResponseDto(username, nickname, Role.ADMIN);

        SigninRequestDto signinRequestDto = new SigninRequestDto(username, password);


        String accessToken = mockMvc.perform(MockMvcRequestBuilders.post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signinRequestDto)))
                .andExpect(status().isOk())  // 상태 코드가 200 OK인지 확인
                .andExpect(jsonPath("$.accessToken").exists())  // 응답에 accessToken이 존재하는지 확인
                .andExpect(jsonPath("$.refreshToken").exists())  // 응답에 refreshToken이 존재하는지 확인
                .andExpect(cookie().exists("refreshToken"))
                .andReturn().getResponse().getHeader("Authorization");  // refreshToken 쿠키 존재 여부 확인


        mockMvc.perform(MockMvcRequestBuilders.post("/admin/users/{userId}/roles", user.getId())
                        .header("Authorization", accessToken))  // JWT 토큰을 헤더에 포함시켜서 요청
                .andExpect(status().isForbidden())  // 사용자 찾을 수 없다는 응답
                .andExpect(jsonPath("$.message").value("요청한 리소스에 접근할 권한이 없습니다."));  // 응답 메시지 검증

    }

    // 관리자 권한 부여 - 존재하지 않는 사용자
    @Test
    @Transactional
    void grantAdmin_userNotFound() throws Exception {
        Long userId = 999L;

        String adminName = "test@example3.com";
        String nickname = "nickname";
        String password = "Password123!";

        User admin = new User(adminName, nickname ,bCryptPasswordEncoder.encode(password),Role.ADMIN);
        userRepository.save(admin);

        SigninRequestDto signinRequestDto = new SigninRequestDto(adminName, password);


        String accessToken = mockMvc.perform(MockMvcRequestBuilders.post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signinRequestDto)))
                .andExpect(status().isOk())  // 상태 코드가 200 OK인지 확인
                .andExpect(jsonPath("$.accessToken").exists())  // 응답에 accessToken이 존재하는지 확인
                .andExpect(jsonPath("$.refreshToken").exists())  // 응답에 refreshToken이 존재하는지 확인
                .andExpect(cookie().exists("refreshToken"))
                .andReturn().getResponse().getHeader("Authorization");  // refreshToken 쿠키 존재 여부 확인


        mockMvc.perform(MockMvcRequestBuilders.post("/admin/users/{userId}/roles", userId)
                        .header("Authorization", accessToken))  // JWT 토큰을 헤더에 포함시켜서 요청
                .andExpect(status().isBadRequest())  // 사용자 찾을 수 없다는 응답
                .andExpect(jsonPath("$.message").value("User를 찾을 수 없습니다"));  // 응답 메시지 검증
    }
}
