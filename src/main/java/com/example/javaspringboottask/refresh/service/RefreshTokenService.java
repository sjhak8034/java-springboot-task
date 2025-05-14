package com.example.javaspringboottask.refresh.service;


import com.example.javaspringboottask.global.exception.CustomResponseStatusException;
import com.example.javaspringboottask.global.exception.ErrorCode;
import com.example.javaspringboottask.global.util.JwtProvider;
import com.example.javaspringboottask.user.entity.User;
import com.example.javaspringboottask.user.repository.UserRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.example.javaspringboottask.global.constant.TokenPrefix.TOKEN_PREFIX;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;
    private final Cache<String, Boolean> blacklistCache = Caffeine.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .maximumSize(10000)
        .build();


    private static final String REFRESH_TOKEN_PREFIX = "RT:"; // Redis 리프레시 토큰 키 접두사
    private static final String BLACKLIST_TOKEN_PREFIX = "BL:"; // 액세스 토큰  블랙리스트 키 접두사
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 7L; // 리프레시 토큰 만료 시간 (7일)

    /**
     * Redis에 리프레시 토큰 저장. Key: RT:{userId}, Value: refreshToken
     *
     * @param userId 저장할 사용자 ID
     * @param token  리프레시 토큰 값
     */
    public void saveRefreshToken(Long userId, String token) {
        String key = REFRESH_TOKEN_PREFIX + userId;

        // Redis에 토큰 저장 및 만료 시간 설정
        redisTemplate.opsForValue().set(key, token, Duration.ofDays(REFRESH_TOKEN_EXPIRE_TIME));
        log.info("리프레시 토큰 저장 userId : {}", userId);
    }

    /**
     * 리프레시 토큰을 사용해 새로운 액세스 토큰을 발급.
     *
     * @param refreshToken 클라이언트에서 전달받은 리프레시 토큰
     * @return 새로 발급된 액세스 토큰
     */
    public String generateAccessTokenFromRefreshToken(String refreshToken) {
        // 리프레시 토큰 검증
        validateRefreshToken(refreshToken);

        // 토큰에서 사용자 이메일 추출
        String email = jwtProvider.getUsername(refreshToken);

        // UserDetails 객체 생성
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        // 새로운 인증 객체 생성
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities());

        // JWT 액세스 토큰 생성 후 반환
        return jwtProvider.generateAccessToken(authentication);
    }

    /**
     * 리프레시 토큰의 유효성을 검증.
     * 1. JWT 토큰 자체의 유효성 확인.
     * 2. Redis 에 저장된 토큰과 비교.
     *
     * @param refreshToken 검증할 리프레시 토큰
     */
    public void validateRefreshToken(String refreshToken) {

        // JWT 토큰 자체의 유효성 검증
        if (!jwtProvider.validateRefreshToken(refreshToken)) {
            log.error("JWT refresh token validation failed");
            throw new CustomResponseStatusException(ErrorCode.UNAUTHORIZED_TOKEN);
        }

        // Redis에 저장된 리프레시 토큰 검증 과정
        // 1. 토큰에 담겨있는 user email 을 가져온다.
        String username = jwtProvider.getUsername(refreshToken);

        // 2. 이메일을 통해 해당 유저 객체를 찾는다.
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new CustomResponseStatusException(ErrorCode.NOT_FOUND_USER));

        // 3. redisTemplate 의 get 을 사용해 RT:userId 가 키인 값의 value -> 즉 리프레시 토큰값을 가져온다.
        String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + user.getId());

        log.info("Stored token : {}", storedToken);
        log.info("Received refresh token: {}", refreshToken);

        // 4. 현재 들어온 리프레시토큰과 같지않거나 null 이라면 유효하지 않은 토큰 에러를 반환
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            log.error("Stored token mismatch or null. Stored: {}, Received: {}", storedToken,
                refreshToken);
            throw new CustomResponseStatusException(ErrorCode.UNAUTHORIZED_TOKEN);
        }
    }

    /**
     * 로그아웃 시 호출하여 Redis 에서 해당 사용자의 리프레시 토큰을 제거.
     *
     * @param authentication 현재 인증된 인증 객체
     * @throws CustomResponseStatusException 이메일에 해당하는 사용자가 존재하지 않을 때 발생
     */
    public void deleteRefreshToken(Authentication authentication) {

        if (authentication == null) {
            // 인증 객체가 null일 경우 로그에 경고 메시지 출력 후 종료
            log.warn("인증 객체가 존재하지 않습니다.");
            return;
        }

        // 삭제 시도
        try {
            String username = authentication.getName();

            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomResponseStatusException(ErrorCode.NOT_FOUND_USER));

            // Redis 에서 삭제할 키 생성 (RT:{userId} 형식)
            String key = REFRESH_TOKEN_PREFIX + user.getId();

            // Redis 에서 해당 키 삭제
            Boolean deleted = redisTemplate.delete(key);

            // 삭제 여부에 따라 로그 출력
            if (Boolean.TRUE.equals(deleted)) {
                log.info("리프레시 토큰 삭제 userId : {}", user.getId());
            } else {
                log.warn("해당하는 리프레시 토큰 조회 실패 userId : {}", user.getId());
            }
        } catch (Exception ex) { //삭제 실패시 에러 반환
            log.error("리프레시 토큰 삭제 실패", ex);
            throw new RuntimeException("리프레시 토큰 삭제 실패", ex);
        }
    }

    /**
     * 액세스 토큰을 Redis 블랙리스트에 추가. 만료되지 않은 액세스 토큰을 블랙리스트에 등록하여 인증에 사용되지 않도록 설정.
     *
     * @param bearerToken 클라이언트에서 Authorization 헤더로 전달받은 Bearer 토큰 (예: "Bearer <액세스 토큰>")
     */
    public void addToBlacklist(String bearerToken) {

        if (!bearerToken.startsWith(TOKEN_PREFIX)) {
            // Bearer 토큰 형식이 올바르지 않을 경우 경고 로그 출력 후 종료
            log.warn("올바르지 않은 토큰 형식입니다.");
            return;
        }

        try {
            // Bearer 접두어 제거 후 액세스 토큰 추출
            String accessToken = bearerToken.substring(7);

            // 토큰 유효성 검증
            if (!jwtProvider.validToken(accessToken)) {
                // 유효하지 않은 토큰이라면 블랙리스트 추가 작업 건너뜀
                log.warn("Attempting to blacklist invalid token");
                return;
            }

            // 토큰 만료 시간 추출
            long expiration = jwtProvider.getExpirationFromToken(accessToken);

            // 현재 시간과 만료 시간 차이를 TTL 로 계산
            long ttl = expiration - System.currentTimeMillis();

            if (ttl > 0) {
                // TTL이 0보다 크다면 Redis 에 블랙리스트 키로 저장 (BL: accessToken 값)
                redisTemplate.opsForValue().set(
                    BLACKLIST_TOKEN_PREFIX + accessToken,
                    "blacklisted",
                    ttl,
                    TimeUnit.MILLISECONDS
                );

                blacklistCache.put(accessToken, true);

                log.info("블랙리스트 추가 작업 완료");
            } else {
                // 토큰이 이미 만료된 경우 블랙리스트 추가 작업 건너뜀
                log.warn("이미 만료된 토큰, 블랙리스트 추가 작업 불필요");
            }
        } catch (Exception ex) {
            log.error("블랙리스트 추가 작업 중 에러 발생", ex);
            throw new RuntimeException("블랙리스트 추가 작업 중 에러 발생", ex);
        }
    }

    /**
     * 액세스 토큰이 블랙리스트에 있는지 확인. Redis 에서 블랙리스트 키를 검색하여 해당 토큰이 등록되어 있는지 확인. JwtAuthFilter 에서 사용, 인증할 때
     * 확인
     *
     * @param accessToken 검증할 액세스 토큰
     * @return 블랙리스트에 등록되어 있다면 true, 그렇지 않으면 false
     */
    public boolean isTokenBlacklisted(String accessToken) {
        // 로컬 캐시 확인
        Boolean cacheResult = blacklistCache.getIfPresent(accessToken);
        if (cacheResult != null) {
            return cacheResult;
        }

        //캐시에 없으면 Redis에서 BL:<액세스 토큰> 키가 존재하는지 확인
        //존재한다면 true 가 리턴될 것이고 캐시에도 해당 토큰이 블랙리스트라는 정보가 들어감
        boolean isBlacklisted = Boolean.TRUE.equals(
            redisTemplate.hasKey(BLACKLIST_TOKEN_PREFIX + accessToken)
        );

        log.info("블랙리스트 값 확인");
        blacklistCache.put(accessToken, isBlacklisted);

        return isBlacklisted;
    }
}
