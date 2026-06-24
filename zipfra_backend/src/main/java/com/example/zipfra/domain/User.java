package com.example.zipfra.domain;

import lombok.Builder;
import lombok.Getter;

/**
 * users 테이블 매핑 도메인 VO.
 * MyBatis resultMap 대상 — JPA 엔티티가 아니므로 @Entity 없음.
 */
@Getter
@Builder
public class User {

    private Long id;
    private String email;
    private String password;  // BCrypt 해시
    private String nickname;
    private String role;
    private Boolean isActive;
}
