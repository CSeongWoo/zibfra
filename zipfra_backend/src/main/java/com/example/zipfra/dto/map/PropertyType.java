package com.example.zipfra.dto.map;

/** MAP-01 매물유형 필터 값 (AGENTS.md §8.1.1, 아파트/오피스텔/빌라 3종 — 원룸 제외). */
public enum PropertyType {
    APT,         // 아파트
    OFFICETEL,   // 오피스텔
    ROW_HOUSE    // 빌라(연립·다세대)
}
