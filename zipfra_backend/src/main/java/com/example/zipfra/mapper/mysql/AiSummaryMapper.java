package com.example.zipfra.mapper.mysql;

import com.example.zipfra.domain.AiSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * ai_summaries 캐싱 테이블 전담 Mapper.
 *
 * <p>Read-Through 캐시 흐름 (§6.3):
 * <ol>
 *   <li>요청 도착 → {@link #findValid} 조회: 유효(expires_at > NOW())한 최신 캐시 반환</li>
 *   <li>캐시 MISS → LLM 호출 → 성공 시 {@link #insert} 로 새 캐시 이력 추가</li>
 *   <li>리뷰 이벤트(작성/삭제) → {@link #invalidateByTargetId} 이전 캐시들 논리 만료</li>
 * </ol>
 *
 * <p><b>GIGO 방지 원칙</b>: summaryAvailable=false인 Fallback 결과는 DB에 저장하지 않는다.
 */
@Mapper
public interface AiSummaryMapper {

    /**
     * 유효한 캐시 단건 조회.
     * WHERE summary_type=:summaryType AND target_id=:targetId
     *   AND (target_type=:targetType OR target_type IS NULL)
     *   AND expires_at > NOW()
     * ORDER BY created_at DESC LIMIT 1
     *
     * @param summaryType  PROPERTY_INFO | REVIEW
     * @param targetId     String (propertyId 또는 targetId)
     * @param targetType   PROPERTY | BUILDING | AREA
     * @return 유효한 캐시 row, 없으면 null (캐시 MISS)
     */
    AiSummary findValid(@Param("summaryType") String summaryType,
                        @Param("targetId") String targetId,
                        @Param("targetType") String targetType);

    /**
     * 캐시 Insert (테이블에 Unique 제약조건이 없으므로 UPSERT 대신 이력 INSERT 형태로 저장).
     *
     * @param aiSummary 저장할 객체
     */
    void insert(AiSummary aiSummary);

    /**
     * 특정 targetId의 캐시 논리 만료 (expires_at = NOW()).
     * REV-02(작성) / REV-04(삭제) 이벤트 후 호출 (AGENTS.md §6.8).
     *
     * @param targetId 만료시킬 REVIEW 캐시의 target_id
     */
    void invalidateByTargetId(@Param("targetId") String targetId);
}
