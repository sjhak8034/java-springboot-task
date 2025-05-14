package com.example.javaspringboottask.global.util;


import com.example.javaspringboottask.user.entity.User;
import com.example.javaspringboottask.user.repository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.EntityNotFoundException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtProvider {

    /**
     * JWT 시크릿 키.
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * 액세스 토큰 만료시간(밀리초).
     */
    @Getter
    @Value("${jwt.expiry-millis}")
    private long expiryMillis;

    /**
     * 리프레시 토큰 만료시간(밀리초))
     */
    @Getter
    @Value("${jwt.refresh-expiry-millis}")
    private long refreshExpiryMillis;

    private final UserRepository userRepository;

    /**
     * authentication 을 받아와 액세스 토큰을 생성하는 메서드
     *
     * @param authentication 유저 이메일을 얻기 위한 인증 객체
     * @return String 액세스 토큰값을 반환
     * @throws EntityNotFoundException 해당 이메일을 가진 유저가 존재하지 않으면 발생
     */
    public String generateAccessToken(Authentication authentication)
            throws EntityNotFoundException {

        String email = authentication.getName();

        User user = isValidUserEmail(email);

        // 현재 날짜를 얻어오고 만료 시간을 더하여 액세스 토큰의 만료시간을 설정
        Date currentDate = new Date();
        Date expireDate = new Date(currentDate.getTime() + this.expiryMillis);

        //토큰을 생성할 때 이메일, 생성시간, 만료시간, 유저 권한을 담아서 생성
        return Jwts.builder()
                .subject(email)
                .issuedAt(currentDate)
                .expiration(expireDate)
                .claim("role", user.getRole())
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * authentication 을 받아와 리프레시 토큰을 생성하는 메서드
     *
     * @param authentication 유저 이메일을 얻기 위한 인증 객체
     * @return String 리프레시 토큰 값을 반환
     * @throws EntityNotFoundException 해당 이메일을 가진 유저가 존재하지 않으면 발생
     */
    public String generateRefreshToken(Authentication authentication)
            throws EntityNotFoundException {

        String email = authentication.getName(); // Authentication에서 사용자 이메일 가져오기

        isValidUserEmail(email);

        Date currentDate = new Date();
        Date expireDate = new Date(currentDate.getTime() + this.refreshExpiryMillis);

        return Jwts.builder()
                .subject(email)
                .issuedAt(currentDate)
                .expiration(expireDate)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * authentication 인증 객체에서 받아온 이메일이 유효한지 검증 후 반환
     *
     * @param username authentication 인증 객체에서 받아온 이메일
     * @return 정상적으로 반환될 시 user 객체 반환
     */
    private User isValidUserEmail(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("해당 username에 맞는 값이 존재하지 않습니다."));
    }

    /**
     * 주어진 토큰에서 사용자 이메일(Username)을 추출합
     *
     * @param token JWT 토큰
     * @return 추출된 사용자 이메일
     */
    public String getUsername(String token) {
        Claims claims = this.getClaims(token);
        return claims.getSubject();
    }

    /**
     * 주어진 토큰의 유효성을 검증
     *
     * @param token JWT 토큰
     * @return 토큰이 유효하면 true, 그렇지 않으면 false
     * @throws JwtException 토큰이 잘못된 형식일 경우 발생
     */
    public boolean validToken(String token) throws JwtException {
        try {
            return !this.tokenExpired(token); // 토큰 만료 여부 확인
        } catch (MalformedJwtException e) { // 토큰이 잘못된 형식일 때
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) { // 토큰이 만료되었을 때
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) { // 지원하지 않는 토큰 형식일 때
            log.error("JWT token is unsupported: {}", e.getMessage());
        }
        return false; // 유효하지 않은 토큰이라는 의미를 반환
    }

    /**
     * 리프레시 토큰의 유효성을 검증
     *
     * @param token 리프레시 토큰
     * @return 토큰이 유효하면 true, 그렇지 않으면 false
     */
    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = this.getClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            log.error("Invalid Refresh Token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 주어진 토큰에서 클레임 정보를 추출
     *
     * @param token JWT 토큰
     * @return 토큰의 클레임 정보
     * @throws MalformedJwtException 토큰이 비어있거나 형식이 잘못된 경우 발생
     */
    private Claims getClaims(String token) {
        // 토큰이 비어있지 않은지 확인
        if (!StringUtils.hasText(token)) {
            throw new MalformedJwtException("토큰이 비어 있습니다.");
        }

        // 토큰을 파싱하고 서명을 검증, 클레임 데이터 반환
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 주어진 토큰이 만료되었는지 확인
     *
     * @param token JWT 토큰
     * @return 토큰이 만료되었으면 true, 그렇지 않으면 false
     */
    private boolean tokenExpired(String token) {
        final Date expiration = this.getExpirationDateFromToken(token); //토큰에서 만료 날짜 추출
        return expiration.before(new Date()); // 만료 시간이 현재 시간 이전인지 확인
    }

    /**
     * 주어진 토큰에서 만료 날짜를 가져옴
     *
     * @param token JWT 토큰
     * @return 토큰의 만료 날짜 객체
     */
    private Date getExpirationDateFromToken(String token) {
        //토큰에서 클레임 데이터를 가져와 만료시간을 추출
        //람다를 사용 claims -> claims.getExpiration() 를 메서드 참조로 표현
        return this.resolveClaims(token, Claims::getExpiration);
    }

    /**
     * 주어진 클레임 처리기를 사용해 토큰의 클레임 데이터를 반환
     *
     * @param token          JWT 토큰
     * @param claimsResolver 클레임 처리 함수, 클레임에서 만료 날짜를 추출하는 역할 부여됨
     * @param <T>            반환 데이터 타입
     * @return 처리된 클레임 데이터
     */
    private <T> T resolveClaims(String token, Function<Claims, T> claimsResolver) {
        // 토큰에서 전체 클레임 데이터를 파싱
        final Claims claims = this.getClaims(token);
        // 클레임 데이터에서 필요한 값을 추출, getExpirationDateFromToken 에서 Claims::getExpiration 을
        // 부여받은 것이 claimsResolver 이기 때문에 apply를 하면 claim 에서 만료 날짜만 가져옴
        return claimsResolver.apply(claims);
    }

    /**
     * 주어진 토큰의 만료 시간을 밀리초 단위로 반환
     *
     * @param token JWT 토큰
     * @return 토큰의 만료 시간 (밀리초 단위)
     */
    public long getExpirationFromToken(String token) {
        // JWT 토큰에서 만료 시간을 추출하여 밀리초 단위로 반환
        Claims claims = this.getClaims(token);
        return claims.getExpiration().getTime();
    }
}