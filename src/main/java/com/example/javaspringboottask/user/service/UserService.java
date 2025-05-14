package com.example.javaspringboottask.user.service;

import com.example.javaspringboottask.global.exception.CustomResponseStatusException;
import com.example.javaspringboottask.global.exception.ErrorCode;
import com.example.javaspringboottask.global.util.JwtProvider;
import com.example.javaspringboottask.refresh.service.RefreshTokenService;
import com.example.javaspringboottask.user.dto.*;
import com.example.javaspringboottask.user.entity.User;
import com.example.javaspringboottask.user.entity.type.Role;
import com.example.javaspringboottask.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    /**
     * 회원가입 service
     * 중복된 username이 존재하는지 확인합니다.
     * default role은 USER입니다.
     * @param requestDto
     * @return SignupResponseDto
     */
    @Transactional
    public SignupResponseDto signup(SignupRequestDto requestDto) {
        Optional<User> duplicateUser = userRepository.findByUsername(requestDto.getUsername());

        if (duplicateUser.isPresent()) {
            throw new CustomResponseStatusException(ErrorCode.DUPLICATE_USERNAME);
        }

        User user = new User(requestDto.getUsername(), requestDto.getNickname(),bCryptPasswordEncoder.encode(requestDto.getPassword()));

        User savedUser = userRepository.save(user);

        return new SignupResponseDto(savedUser.getUsername(),savedUser.getNickname(),savedUser.getRole());
    }

    @Transactional
    public TokenResponse tokenGenerate(SigninRequestDto requestDto) {
        Optional<User> signinUser = userRepository.findByUsername(requestDto.getUsername());

        if(signinUser.isEmpty()) {
            throw new CustomResponseStatusException(ErrorCode.NOT_FOUND_USER);
        }
        User user = signinUser.get();
        Authentication authentication;
        // 이 과정에서 Provider 가 인증 처리를 진행 (사용자 정보 조회, 비밀번호 검증)
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            requestDto.getUsername(),
                            requestDto.getPassword()
                    )
            );
            // 인증 성공 처리
        } catch (BadCredentialsException e) {
            // 비밀번호가 틀린 경우
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"비밀번호가 올바르지 않습니다.");
        } catch (UsernameNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"해당 사용자를 찾을 수 없습니다.");
        } catch (AuthenticationException e) {
            // 그 외 인증 관련 예외
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"인증에 실패했습니다.");
        }


        // 인증 객체를 SecurityContext에 저장
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // access, refresh 토큰 생성 후 반환
        String accessToken = jwtProvider.generateAccessToken(authentication);
        String refreshToken = jwtProvider.generateRefreshToken(authentication);

        refreshTokenService.saveRefreshToken(user.getId(), refreshToken);


        return new TokenResponse(accessToken,refreshToken);
    }

    @Transactional
    public GrantAdminResponseDto grantAdmin(Long userId, String username) {
        Optional<User> user = userRepository.findById(userId);
        if(user.isEmpty()) {
            throw new CustomResponseStatusException(ErrorCode.NOT_FOUND_USER);
        }
        Optional<User> admin = userRepository.findByUsername(username);
        if(admin.isEmpty()) {
            throw new CustomResponseStatusException(ErrorCode.DUPLICATE_USERNAME);
        }
        if(! Role.ADMIN.equals(admin.get().getRole())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"관리자 권한이 필요한 요청입니다. 접근 권한이 없습니다.");
        }
        User grantedUser = user.get();
        grantedUser.grantAdmin();
        return new GrantAdminResponseDto(grantedUser.getUsername(),grantedUser.getNickname(),grantedUser.getRole());
    }



}
