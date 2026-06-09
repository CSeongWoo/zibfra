DROP SCHEMA IF EXISTS zipfra;
CREATE SCHEMA zipfra;
USE zipfra;

-- =========================================
-- MySQL (Main Data) Tables
-- =========================================

-- 1. users 테이블 생성
CREATE TABLE `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `email` VARCHAR(255) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `nickname` VARCHAR(100) DEFAULT NULL,
  `role` VARCHAR(50) DEFAULT 'USER',
  `is_active` TINYINT(1) DEFAULT 1,
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. reviews 테이블 생성
CREATE TABLE `reviews` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT DEFAULT NULL COMMENT 'FK → users.id',
  `target_type` VARCHAR(50) DEFAULT NULL COMMENT 'BUILDING | AREA',
  `target_id` VARCHAR(255) DEFAULT NULL COMMENT '논리참조(PostGIS ID 또는 법정동코드)',
  `content` TEXT DEFAULT NULL COMMENT '마스킹 처리된 평문 리뷰 본문',
  `encrypted_content` VARBINARY(1000) DEFAULT NULL COMMENT 'PII 원본 AES-256 암호문(앱레벨)',
  `rating` TINYINT DEFAULT NULL COMMENT '1~5',
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. ai_summaries 테이블 생성
CREATE TABLE `ai_summaries` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `summary_type` VARCHAR(50) DEFAULT NULL COMMENT 'PROPERTY_INFO | REVIEW',
  `target_type` VARCHAR(50) DEFAULT NULL COMMENT 'PROPERTY | BUILDING | AREA',
  `target_id` VARCHAR(255) DEFAULT NULL COMMENT '논리참조(매물ID 또는 법정동코드)',
  `summary` TEXT DEFAULT NULL COMMENT '요약 본문(Fallback 시 null)',
  `positives` JSON DEFAULT NULL COMMENT '긍정 테마 배열(nullable)',
  `negatives` JSON DEFAULT NULL COMMENT '부정 테마 배열(nullable)',
  `review_count` INT DEFAULT NULL COMMENT '분석된 리뷰 수',
  `model_name` VARCHAR(100) DEFAULT NULL COMMENT '예: gpt-4o-mini',
  `summary_available` TINYINT(1) DEFAULT 1,
  `generated_at` DATETIME DEFAULT NULL,
  `expires_at` DATETIME DEFAULT NULL COMMENT 'TTL 24h 만료 시간',
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =========================================
-- Relationships (MySQL 내 실제 외래키 관계)
-- =========================================

ALTER TABLE `reviews`
  ADD CONSTRAINT `fk_reviews_user_id` 
  FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) -- mysql_users에서 users로 수정
  ON DELETE SET NULL ON UPDATE CASCADE;