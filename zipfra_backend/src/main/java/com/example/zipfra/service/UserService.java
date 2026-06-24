package com.example.zipfra.service;

import com.example.zipfra.domain.User;
import com.example.zipfra.dto.user.UpdateUserRequest;
import com.example.zipfra.dto.user.UserProfileResponse;
import com.example.zipfra.exception.ApiException;
import com.example.zipfra.exception.ErrorCode;
import com.example.zipfra.mapper.mysql.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final FileService fileService;

    /**
     * USER-02 본인 정보(닉네임, 비밀번호) 수정
     */
    @Transactional(transactionManager = "primaryTransactionManager")
    public void updateMyInfo(Long userId, UpdateUserRequest request) {
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        String encodedPassword = user.getPassword();
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            encodedPassword = passwordEncoder.encode(request.getPassword());
        }

        String nickname = user.getNickname();
        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            nickname = request.getNickname();
        }

        String profileImageUrl = user.getProfileImageUrl();
        if (Boolean.TRUE.equals(request.getDeleteProfileImage())) {
            profileImageUrl = null;
        } else if (request.getProfileImage() != null && !request.getProfileImage().isEmpty()) {
            profileImageUrl = fileService.storeProfileImage(request.getProfileImage());
        }

        User updatedUser = User.builder()
                .id(user.getId())
                .nickname(nickname)
                .password(encodedPassword)
                .profileImageUrl(profileImageUrl)
                .build();

        userMapper.updateUser(updatedUser);
    }

    /**
     * USER-03 회원 탈퇴 (논리적 삭제)
     */
    @Transactional(transactionManager = "primaryTransactionManager")
    public void deactivateMe(Long userId, String accessToken) {
        userMapper.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        userMapper.deactivateUser(userId);

        // 탈퇴 후 토큰 블랙리스트 처리 및 강제 로그아웃
        authService.logout(accessToken, userId);
    }

    /**
     * USER-04 닉네임으로 유저 검색
     */
    public List<UserProfileResponse> searchUsers(String nickname) {
        return userMapper.searchByNickname(nickname).stream()
                .map(UserProfileResponse::from)
                .collect(Collectors.toList());
    }
}
