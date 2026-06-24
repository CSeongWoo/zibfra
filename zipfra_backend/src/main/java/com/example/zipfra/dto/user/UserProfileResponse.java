package com.example.zipfra.dto.user;

import com.example.zipfra.domain.User;
import lombok.Builder;
import lombok.Getter;

/**
 * USER-01 GET /api/v1/users/me 응답 DTO.
 */
@Getter
@Builder
public class UserProfileResponse {

    private Long id;
    private String email;
    private String nickname;
    private String profileImageUrl;

    public static UserProfileResponse from(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }
}
