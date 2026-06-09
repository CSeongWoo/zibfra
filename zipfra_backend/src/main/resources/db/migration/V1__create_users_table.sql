-- =================================================================
-- Zipfra — users 테이블 DDL (MySQL 8.x, primaryDataSource)
-- §4: 쓰기=MySQL, 읽기·공간 분석=PostGIS
-- §8.2: AUTH-01(회원가입), AUTH-02(로그인) 에 사용
-- =================================================================

CREATE TABLE IF NOT EXISTS users
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE COMMENT '로그인 이메일 (RFC5322, ≤255)',
    password   VARCHAR(255) NOT NULL COMMENT 'BCrypt 해시 비밀번호',
    nickname   VARCHAR(20)  NOT NULL COMMENT '사용자 닉네임 (2~20자)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
