package com.example.zipfra.web;

import com.example.zipfra.domain.User;
import com.example.zipfra.dto.user.UserProfileResponse;
import com.example.zipfra.exception.ApiException;
import com.example.zipfra.exception.ErrorCode;
import com.example.zipfra.mapper.mysql.UserMapper;
import com.example.zipfra.security.ZipfraPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;

    /**
     * USER-01 현재 로그인한 사용자 프로필 조회
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(@AuthenticationPrincipal ZipfraPrincipal principal) {
        if (principal == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }

        User user = userMapper.findById(principal.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        return ResponseEntity.ok(UserProfileResponse.from(user));
    }
}
