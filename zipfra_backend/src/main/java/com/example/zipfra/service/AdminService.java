package com.example.zipfra.service;

import com.example.zipfra.domain.User;
import com.example.zipfra.exception.ApiException;
import com.example.zipfra.exception.ErrorCode;
import com.example.zipfra.mapper.mysql.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserMapper userMapper;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * ADMIN-01 회원 삭제 (관리자 권한으로 계정 논리적 삭제)
     */
    @Transactional(transactionManager = "primaryTransactionManager")
    public void deleteUserForcefully(Long targetUserId) {
        User user = userMapper.findById(targetUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        // Soft delete 처리
        userMapper.deactivateUser(targetUserId);

        // 해당 회원의 Refresh Token 강제 삭제 (로그아웃 처리)
        // Access Token은 클라이언트가 들고 있으나, 다음 만료 시점에 갱신 불가
        // (블랙리스트 처리는 토큰을 모르므로 불가능)
        redisTemplate.delete("rt:" + targetUserId);
    }
}
