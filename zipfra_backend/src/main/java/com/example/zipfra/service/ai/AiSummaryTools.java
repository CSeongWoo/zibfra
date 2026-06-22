package com.example.zipfra.service.ai;

import com.example.zipfra.dto.ai.PropertyDetailDTO;
import com.example.zipfra.dto.location.PoiDistanceDTO;
import com.example.zipfra.dto.location.ScoreResponse;
import com.example.zipfra.dto.review.ReviewResponse;
import com.example.zipfra.exception.ApiException;
import com.example.zipfra.exception.ErrorCode;
import com.example.zipfra.mapper.mysql.ReviewMapper;
import com.example.zipfra.mapper.postgis.PoiMapper;
import com.example.zipfra.mapper.postgis.PropertyDetailMapper;
import com.example.zipfra.service.LocationScoreCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Spring AI @Tool 컴포넌트 (AGENTS.md §6.4.1).
 *
 * <p>LLM이 Function Calling 기법으로 직접 호출하는 3개의 데이터 공급 인터페이스:
 * <ol>
 *   <li>{@link #getPropertyDetail(Long)} — real_estate_sales 거래 데이터</li>
 *   <li>{@link #getLocationBreakdown(Long, Integer)} — 입지 점수 breakdown</li>
 *   <li>{@link #getReviews(String, String, Integer)} — PII 마스킹된 리뷰 목록</li>
 * </ol>
 *
 * <p><b>GIGO 방지 원칙</b> (§6.9 말미):
 * Tool이 반환하는 Resource 품질이 LLM 요약 품질을 결정한다.
 * null 데이터·비마스킹 텍스트가 LLM에 전달되지 않도록 각 Tool에서 검증한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiSummaryTools {

    private static final int RADIUS_DEFAULT = 1500;       // §5 기본 반경 1,500m
    private static final int MAX_TOKEN_ESTIMATE = 1500;   // 입력 토큰 상한 (§6.4.2)
    private static final double AVG_TOKENS_PER_KO_CHAR = 2.5; // 한글 평균 토큰

    // PII 마스킹 패턴 (전화번호·이메일 — §6.8)
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("(\\d{2,3})[\\-\\s]?(\\d{3,4})[\\-\\s]?(\\d{4})");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");
    private static final Pattern SSN_PATTERN =
            Pattern.compile("(\\d{6})-[1-4]\\d{6}");

    // 리뷰 인젝션 방지 구분자 (§6.8)
    private static final String REVIEW_START = "---USER REVIEW START---";
    private static final String REVIEW_END   = "---USER REVIEW END---";

    private final PropertyDetailMapper propertyDetailMapper;
    private final PoiMapper            poiMapper;
    private final LocationScoreCalculator calculator;
    private final ReviewMapper         reviewMapper;

    // ═══════════════════════════════════════════════════════════════════════════
    // Tool 1: getPropertyDetail
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * [AI-01 Tool] 매물 상세 정보 조회 (AGENTS.md §6.4.1 getPropertyDetail).
     *
     * <p>PostGIS real_estate_sales에서 propertyId 기반으로 거래 데이터를 가져와
     * LLM이 이해하기 쉬운 텍스트 형태로 변환한다.
     *
     * @param propertyId real_estate_sales.id
     * @return 매물 상세 컨텍스트 텍스트 (LLM Resource)
     * @throws ApiException PROPERTY_NOT_FOUND — 존재하지 않는 propertyId
     */
    @Tool(name = "getPropertyDetail",
          description = "주어진 propertyId로 부동산 매물의 상세 거래 정보(건물명, 거래금액, 면적, 층, 법정동, 거래유형)를 조회합니다.")
    @Transactional(transactionManager = "spatialTransactionManager",
                   readOnly = true,
                   propagation = Propagation.SUPPORTS)
    public String getPropertyDetail(Long propertyId) {
        log.debug("[AiTool][getPropertyDetail] propertyId={}", propertyId);

        PropertyDetailDTO detail = propertyDetailMapper.findById(propertyId);
        if (detail == null) {
            log.warn("[AiTool][getPropertyDetail] 매물 없음 propertyId={}", propertyId);
            throw new ApiException(ErrorCode.PROPERTY_NOT_FOUND,
                    "propertyId=" + propertyId + " 에 해당하는 매물이 없습니다.");
        }

        String context = detail.toPromptContext();
        log.debug("[AiTool][getPropertyDetail] 조회 완료 propertyId={}, context=\n{}", propertyId, context);
        return context;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Tool 2: getLocationBreakdown
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * [AI-01 Tool] 입지 점수 breakdown 조회 (AGENTS.md §6.4.1 getLocationBreakdown).
     *
     * <p>해당 매물 좌표 주변 radiusMeters 내 POI를 PostGIS에서 조회하고,
     * §5 알고리즘(거리 감쇠·그룹 합산)으로 입지 점수를 계산해 반환한다.
     * 가중치는 모두 1.0으로 설정하여 그룹 base를 그대로 노출한다.
     *
     * @param propertyId    real_estate_sales.id (좌표 조회용)
     * @param radiusMeters  검색 반경(m), null이면 기본값 1500m 적용
     * @return 입지 점수 breakdown 텍스트 (LLM Resource)
     */
    @Tool(name = "getLocationBreakdown",
          description = "주어진 propertyId 매물의 주변 교통·교육·상업·편의 인프라 점수 breakdown을 조회합니다.")
    @Transactional(transactionManager = "spatialTransactionManager",
                   readOnly = true,
                   propagation = Propagation.SUPPORTS)
    public String getLocationBreakdown(Long propertyId, Integer radiusMeters) {
        int radius = (radiusMeters != null) ? radiusMeters : RADIUS_DEFAULT;
        log.debug("[AiTool][getLocationBreakdown] propertyId={}, radius={}m", propertyId, radius);

        // 매물 좌표 조회 (이미 getPropertyDetail에서 호출했을 수도 있지만 Tool은 독립적으로 동작)
        PropertyDetailDTO detail = propertyDetailMapper.findById(propertyId);
        if (detail == null || detail.getLon() == null || detail.getLat() == null) {
            log.warn("[AiTool][getLocationBreakdown] 좌표 없음 propertyId={}", propertyId);
            return "입지 점수 정보를 조회할 수 없습니다. (좌표 누락)";
        }

        List<PoiDistanceDTO> pois = poiMapper.findWithin(detail.getLon(), detail.getLat(), radius);
        log.debug("[AiTool][getLocationBreakdown] POI {}건 조회 완료", pois.size());

        // 가중치 1.0으로 전 그룹 base 노출 (AI 요약용, 페르소나 가중치와 무관)
        Map<String, Double> allOnesWeight = Map.of(
                "transit", 1.0, "education", 1.0, "commerce", 1.0, "convenience", 1.0
        );
        LocationScoreCalculator.CalcResult result = calculator.calculate(pois, allOnesWeight);

        return formatBreakdown(result.breakdown(), radius);
    }

    /** breakdown 맵을 LLM이 이해하기 쉬운 텍스트로 변환. */
    private String formatBreakdown(Map<String, ScoreResponse.GroupResult> breakdown, int radius) {
        StringBuilder sb = new StringBuilder();
        sb.append("반경 ").append(radius).append("m 입지 점수 분석:\n");
        breakdown.forEach((group, result) -> {
            sb.append(String.format("  [%s] base=%.3f%n", group, result.base()));
            result.categories().forEach((cat, score) -> {
                String nearest = score.nearestMeters() != null
                        ? score.nearestMeters() + "m" : "없음";
                sb.append(String.format("    - %s: %d개, 최근접=%s, base=%.4f%n",
                        cat, score.count(), nearest, score.base()));
            });
        });
        return sb.toString();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Tool 3: getReviews
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * [AI-02 Tool] PII 마스킹된 리뷰 목록 조회 (AGENTS.md §6.4.1 getReviews).
     *
     * <p>mysql_reviews에서 최신순으로 리뷰를 조회하고:
     * <ol>
     *   <li>전화번호·이메일·주민번호를 [REDACTED]로 PII 마스킹 (§6.8)</li>
     *   <li>인젝션 방지 구분자 삽입 (§6.8)</li>
     *   <li>총 토큰 추정치가 1,500 초과 시 최신 리뷰부터 절단 (§6.8)</li>
     * </ol>
     *
     * @param targetType  BUILDING | AREA
     * @param targetId    건물 또는 법정동 ID
     * @param maxReviews  최대 조회 건수 (1~50, null이면 30)
     * @return 마스킹·구분자 처리된 리뷰 텍스트 (LLM Resource)
     */
    @Tool(name = "getReviews",
          description = "지정된 건물(BUILDING) 또는 지역(AREA)의 최신 리뷰 목록을 PII 마스킹 처리 후 반환합니다.")
    @Transactional(transactionManager = "primaryTransactionManager",
                   readOnly = true,
                   propagation = Propagation.SUPPORTS)
    public String getReviews(String targetType, String targetId, Integer maxReviews) {
        int limit = (maxReviews != null) ? Math.min(maxReviews, 50) : 30;
        log.debug("[AiTool][getReviews] targetType={}, targetId={}, limit={}", targetType, targetId, limit);

        List<ReviewResponse> reviews = reviewMapper.findByTarget(targetType, targetId, 0, limit);
        long totalCount = reviewMapper.countByTarget(targetType, targetId);

        log.debug("[AiTool][getReviews] 조회된 리뷰 {}건 (전체 {}건)", reviews.size(), totalCount);

        if (reviews.isEmpty()) {
            return "리뷰 건수: 0건\n리뷰 데이터가 없습니다.";
        }

        // PII 마스킹 및 구분자 삽입, 토큰 절단
        List<String> masked = reviews.stream()
                .map(r -> maskPii(r.getContent()))
                .collect(Collectors.toList());

        List<String> truncated = truncateToTokenLimit(masked);

        StringBuilder sb = new StringBuilder();
        sb.append("리뷰 건수: ").append(reviews.size()).append("건 (전체 ").append(totalCount).append("건)\n\n");
        for (int i = 0; i < truncated.size(); i++) {
            sb.append(REVIEW_START).append("\n");
            sb.append("[리뷰 ").append(i + 1).append("] ").append(truncated.get(i)).append("\n");
            sb.append(REVIEW_END).append("\n\n");
        }

        String result = sb.toString();
        log.debug("[AiTool][getReviews] 최종 리뷰 텍스트 {}자", result.length());
        return result;
    }

    // ─── PII 마스킹 ─────────────────────────────────────────────────────────

    /**
     * 전화번호, 이메일, 주민번호를 [REDACTED]로 치환 (AGENTS.md §6.8 PII 마스킹).
     * CryptoUtils.maskPii()와 동일한 정규식 집합을 사용하되 이메일도 추가.
     */
    private String maskPii(String content) {
        if (content == null) return "";
        return SSN_PATTERN.matcher(
                EMAIL_PATTERN.matcher(
                        PHONE_PATTERN.matcher(content)
                                .replaceAll("[REDACTED]")
                ).replaceAll("[REDACTED]")
        ).replaceAll("[REDACTED]");
    }

    /**
     * 총 토큰 추정치가 MAX_TOKEN_ESTIMATE를 초과하면 뒤쪽 리뷰부터 제거.
     * 한글 1자 ≈ 2.5 tokens 기준 (AGENTS.md §6.8).
     */
    private List<String> truncateToTokenLimit(List<String> reviews) {
        double totalTokens = 0;
        int cutAt = 0;
        for (String review : reviews) {
            double tokens = review.length() * AVG_TOKENS_PER_KO_CHAR;
            if (totalTokens + tokens > MAX_TOKEN_ESTIMATE) {
                log.debug("[AiTool][getReviews] 토큰 초과로 {}번째 이후 리뷰 절단", cutAt);
                break;
            }
            totalTokens += tokens;
            cutAt++;
        }
        return cutAt == reviews.size() ? reviews : reviews.subList(0, cutAt);
    }
}
