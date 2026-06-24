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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;

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
                .role("USER")
                .isActive(true)
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

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
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
        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
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

    /**
     * AUTH-05-1 비밀번호 찾기 (인증번호 전송)
     */
    public void sendAuthCode(String email) {
        User user = userMapper.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        // 6자리 난수 인증번호 생성
        SecureRandom random = new SecureRandom();
        int codeNum = random.nextInt(900000) + 100000;
        String code = String.valueOf(codeNum);

        // Redis 저장 (5분)
        String redisKey = "auth:code:" + email;
        redisTemplate.opsForValue().set(redisKey, code, 5, TimeUnit.MINUTES);

        // 이메일 발송
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("[Zipfra] 비밀번호 찾기 인증번호 안내");
            message.setText("안녕하세요.\nZipfra 비밀번호 찾기 인증번호입니다.\n\n인증번호: " + code + "\n\n해당 인증번호는 5분간 유효합니다.");
            mailSender.send(message);
            log.info("인증메일 발송 완료: {}", email);
        } catch (Exception e) {
            log.error("메일 발송 실패", e);
            throw new RuntimeException("메일 발송에 실패했습니다.", e);
        }
    }

    /**
     * AUTH-05-2 비밀번호 찾기 (인증번호 검증 및 임시 비밀번호 발급)
     */
    @Transactional(transactionManager = "primaryTransactionManager")
    public String forgotPassword(String email, String code) {
        if (email == null || code == null) {
            throw new ApiException(ErrorCode.INVALID_PARAM); // Or custom ErrorCode
        }

        String redisKey = "auth:code:" + email;
        String savedCode = redisTemplate.opsForValue().get(redisKey);

        if (savedCode == null) {
            throw new RuntimeException("인증번호가 만료되었거나 존재하지 않습니다.");
        }

        if (!savedCode.equals(code)) {
            throw new RuntimeException("인증번호가 일치하지 않습니다.");
        }

        // 인증 성공 후 코드 삭제
        redisTemplate.delete(redisKey);

        User user = userMapper.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        // 8자리 임시 비밀번호 생성 (특수문자 ~ 추가)
        String tempPassword = java.util.UUID.randomUUID().toString().substring(0, 8) + "A1~";
        String hashedPassword = passwordEncoder.encode(tempPassword);

        userMapper.updatePasswordByEmail(email, hashedPassword);

        return tempPassword;
    }
}
