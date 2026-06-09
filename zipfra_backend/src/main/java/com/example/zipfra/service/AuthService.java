package com.example.zipfra.service;

import com.example.zipfra.domain.User;
import com.example.zipfra.dto.auth.LoginRequest;
import com.example.zipfra.dto.auth.SignupRequest;
import com.example.zipfra.dto.auth.TokenDto;
import com.example.zipfra.exception.ApiException;
import com.example.zipfra.exception.ErrorCode;
import com.example.zipfra.mapper.mysql.UserMapper;
import com.example.zipfra.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * AUTH-01 회원 가입
     */
    @Transactional(transactionManager = "primaryTransactionManager")
    public void signup(SignupRequest request) {
        // 이메일 중복 검사
        if (userMapper.findByEmail(request.getEmail()).isPresent()) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 비밀번호 해싱 및 사용자 삽입
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        User user = User.builder()
                .email(request.getEmail())
                .password(hashedPassword)
                .nickname(request.getNickname())
                .build();

        userMapper.insertUser(user);
    }

    /**
     * AUTH-02 로그인
     */
    @Transactional(transactionManager = "primaryTransactionManager", readOnly = true)
    public TokenDto login(LoginRequest request) {
        User user = userMapper.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID, HttpStatus.UNAUTHORIZED.getReasonPhrase()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ApiException(ErrorCode.TOKEN_INVALID, HttpStatus.UNAUTHORIZED.getReasonPhrase());
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // Redis rt:{userId} 저장 (14일 TTL)
        String redisKey = "rt:" + user.getId();
        redisTemplate.opsForValue().set(redisKey, refreshToken, 14, TimeUnit.DAYS);

        return new TokenDto(accessToken, refreshToken);
    }

    /**
     * AUTH-03 Refresh Token 재발급 (RTR)
     */
    public TokenDto refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtUtil.parseClaims(refreshToken);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new ApiException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        String userIdStr = claims.getSubject();
        if (userIdStr == null) {
            throw new ApiException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        Long userId = Long.valueOf(userIdStr);

        // Redis rt:{userId} 조회
        String redisKey = "rt:" + userId;
        String storedToken = redisTemplate.opsForValue().get(redisKey);

        if (storedToken == null) {
            throw new ApiException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        // RTR 검증: 불일치 시 탈취로 간주하고 즉시 RT 삭제
        if (!storedToken.equals(refreshToken)) {
            redisTemplate.delete(redisKey);
            throw new ApiException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        // 사용자 조회
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        // 새로운 토큰 쌍 발급
        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());

        // Redis 갱신 (14일 TTL)
        redisTemplate.opsForValue().set(redisKey, newRefreshToken, 14, TimeUnit.DAYS);

        return new TokenDto(newAccessToken, newRefreshToken);
    }

    /**
     * AUTH-04 로그아웃
     */
    public void logout(String accessToken, Long userId) {
        // Redis에서 rt:{userId} 삭제
        String rtKey = "rt:" + userId;
        redisTemplate.delete(rtKey);

        // Access Token 남은 유효 기간 계산하여 Blacklist 등록
        long remainingMs = jwtUtil.getRemainingExpirationMs(accessToken);
        if (remainingMs > 0) {
            String blKey = "bl:" + accessToken;
            redisTemplate.opsForValue().set(blKey, "logout", remainingMs, TimeUnit.MILLISECONDS);
        }
    }
}
