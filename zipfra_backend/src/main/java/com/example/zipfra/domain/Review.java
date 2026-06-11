package com.example.zipfra.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Review {
    private Long id;
    private Long userId;
    private String targetType;
    private String targetId;
    private String content;
    private byte[] encryptedContent;
    private Integer rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
