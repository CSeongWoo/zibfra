DROP DATABASE zipfra;

-- 0. database: db 없을시 생성
CREATE DATABASE IF NOT EXISTS `zipfra`;

-- 1. users: 회원 테이블
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    profile_image_url VARCHAR(255) DEFAULT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. reviews: 리뷰 테이블 (MySQL 내 실제 FK 유지)
CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL CHECK (target_type IN ('BUILDING', 'AREA')),
    target_id VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    encrypted_content BLOB NOT NULL, -- PII 원본(AES-256)
    rating TINYINT CHECK (rating BETWEEN 1 AND 5),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_reviews_target_created (target_type, target_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. ai_summaries: AI 요약 캐시
CREATE TABLE ai_summaries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    summary_type VARCHAR(20) NOT NULL CHECK (summary_type IN ('PROPERTY_INFO', 'REVIEW')),
    target_type VARCHAR(20) NOT NULL CHECK (target_type IN ('PROPERTY', 'BUILDING', 'AREA')),
    target_id VARCHAR(20) NOT NULL,
    summary TEXT,
    positives JSON,
    negatives JSON,
    review_count INT,
    model_name VARCHAR(50) NOT NULL,
    summary_available BOOLEAN NOT NULL DEFAULT true,
    generated_at DATETIME,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_ai_summary_lookup (summary_type, target_type, target_id),
    INDEX idx_ai_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. favorites: 즐겨찾기 테이블 (MySQL 내 실제 FK 유지 및 복합 유니크 제약 조건 추가)
CREATE TABLE favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    property_id BIGINT NOT NULL, -- 논리참조(PostGIS pg_real_estate_sales.id)
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE INDEX uk_favorites_user_property (user_id, property_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Event Scheduler: 24시간 TTL 만료 캐시 자동 삭제
SET GLOBAL event_scheduler = ON;
CREATE EVENT ev_cleanup_ai_summaries
ON SCHEDULE EVERY 1 DAY
STARTS (CURRENT_DATE + INTERVAL 1 DAY + INTERVAL 3 HOUR) -- 새벽 3시 실행
DO
DELETE FROM ai_summaries WHERE expires_at < NOW();