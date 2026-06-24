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
import com.example.zipfra.dto.user.UpdateUserRequest;
import com.example.zipfra.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final UserService userService;

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

    /**
     * USER-02 회원 정보(닉네임, 비밀번호) 수정
     */
    @PatchMapping("/me")
    public ResponseEntity<Void> updateMyInfo(@RequestBody UpdateUserRequest request,
                                             @AuthenticationPrincipal ZipfraPrincipal principal) {
        if (principal == null) throw new ApiException(ErrorCode.TOKEN_MISSING);
        userService.updateMyInfo(principal.getId(), request);
        return ResponseEntity.noContent().build();
    }

    /**
     * USER-03 회원 탈퇴 (논리적 삭제)
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deactivateMe(HttpServletRequest request,
                                             @AuthenticationPrincipal ZipfraPrincipal principal) {
        if (principal == null) throw new ApiException(ErrorCode.TOKEN_MISSING);
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ApiException(ErrorCode.TOKEN_MISSING);
        }
        String accessToken = authHeader.substring(7);
        
        userService.deactivateMe(principal.getId(), accessToken);
        return ResponseEntity.noContent().build();
    }

    /**
     * USER-04 특정 닉네임으로 유저 검색
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserProfileResponse>> searchUsers(@RequestParam("nickname") String nickname) {
        List<UserProfileResponse> responses = userService.searchUsers(nickname);
        return ResponseEntity.ok(responses);
    }
}
