package com.example.zipfra.mapper.mysql;

import com.example.zipfra.domain.AiSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * mysql_ai_summaries 캐싱 테이블 전담 Mapper (AGENTS.md §8.3 mysql.AiSummaryMapper).
 *
 * <p>AGENTS.md §3: primarySqlSessionFactory(@MapperScan)가 이 패키지를 스캔하므로
 * 별도 DataSource 지정 없이 MySQL primaryDataSource를 자동으로 사용한다.
 *
 * <p>Read-Through 캐시 흐름 (§6.3):
 * <ol>
 *   <li>요청 도착 → {@link #findValid} 조회: 유효(expires_at > NOW())한 캐시 반환</li>
 *   <li>캐시 MISS → Spring AI Planner 구동 → LLM 응답 성공 시 {@link #upsert}</li>
 *   <li>리뷰 이벤트(작성/삭제) → {@link #invalidateByTargetId} 즉시 만료</li>
 * </ol>
 *
 * <p><b>GIGO 방지 원칙</b>: summaryAvailable=false인 Fallback 결과는 DB에 저장하지 않는다.
 * 일시적 LLM 장애가 24시간 고착화되는 현상 방지 (§6.3·§6.9).
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
     * @param summaryType  PROPERTY | REVIEW
     * @param targetId     propertyId (PROPERTY) 또는 targetId (REVIEW)
     * @param targetType   PROPERTY 시 null, REVIEW 시 BUILDING|AREA
     * @return 유효한 캐시 row, 없으면 null (캐시 MISS)
     */
    AiSummary findValid(@Param("summaryType") String summaryType,
                        @Param("targetId") Long targetId,
                        @Param("targetType") String targetType);

    /**
     * 캐시 Upsert (생성 또는 갱신).
     * ON DUPLICATE KEY UPDATE summary_json, summary_available, created_at, expires_at.
     * UNIQUE KEY: (summary_type, target_id, target_type).
     *
     * <p><b>주의</b>: summaryAvailable=false인 Fallback 응답은 이 메서드를 호출하지 않는다.
     *
     * @param summaryType     PROPERTY | REVIEW
     * @param targetId        propertyId 또는 targetId
     * @param targetType      null (PROPERTY) | BUILDING | AREA (REVIEW)
     * @param summaryJson     직렬화된 {@link com.example.zipfra.dto.ai.AiSummaryResponse} JSON
     * @param ttlHours        캐시 유효 시간 (시간 단위, 보통 24)
     */
    void upsert(@Param("summaryType") String summaryType,
                @Param("targetId") Long targetId,
                @Param("targetType") String targetType,
                @Param("summaryJson") String summaryJson,
                @Param("ttlHours") int ttlHours);

    /**
     * 특정 targetId의 캐시 즉시 만료 (expires_at = NOW()).
     * REV-02(작성) / REV-04(삭제) 이벤트 후 호출 (AGENTS.md §6.8).
     *
     * @param targetId 만료시킬 REVIEW 캐시의 target_id
     */
    void invalidateByTargetId(@Param("targetId") Long targetId);
}
