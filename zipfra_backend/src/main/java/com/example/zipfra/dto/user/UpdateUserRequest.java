package com.example.zipfra.dto.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class UpdateUserRequest {
    private String nickname;
    private String password;
    private MultipartFile profileImage;
    private Boolean deleteProfileImage;
}
