package com.example.zipfra.service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.zipfra.dto.map.Bbox;
import com.example.zipfra.dto.map.DealType;
import com.example.zipfra.dto.map.MarkerDTO;
import com.example.zipfra.dto.map.MarkerFilter;
import com.example.zipfra.dto.map.MarkerResponse;
import com.example.zipfra.dto.map.PoiMarkerDTO;
import com.example.zipfra.dto.map.PriceTrendPoint;
import com.example.zipfra.dto.map.PropertySearchDTO;
import com.example.zipfra.dto.map.PropertyType;
import com.example.zipfra.dto.map.RegionSummaryDTO;
import com.example.zipfra.mapper.postgis.MarkerMapper;
import com.example.zipfra.mapper.postgis.PoiMapper;
import com.example.zipfra.util.BboxValidator;
import com.example.zipfra.exception.ApiException;
import com.example.zipfra.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link MapService} 구현. PostGIS 읽기 전용(§3: SUPPORTS, spatialTransactionManager
 * 단독).
 */
@Service
public class MapServiceImpl implements MapService {

    static final int ZOOM_MIN = 1;
    static final int ZOOM_MAX = 21;
    static final int ZOOM_THRESHOLD_IN = 15; // >= : DETAIL, 그 미만: SUMMARY
    static final double MAX_DIAGONAL_M = 150_000.0;
    static final int MAX_PAGE_SIZE = 200;
    static final int DEFAULT_SIZE = 100;
    static final int DEFAULT_PAGE = 0;
    static final int SEARCH_MIN_LEN = 2;        // 2자 미만 검색어는 무시(전체 seq-scan 방지)
    static final int SEARCH_DEFAULT_LIMIT = 20;
    static final int SEARCH_MAX_LIMIT = 50;

    private final MarkerMapper markerMapper;
    private final PoiMapper poiMapper;

    public MapServiceImpl(MarkerMapper markerMapper, PoiMapper poiMapper) {
        this.markerMapper = markerMapper;
        this.poiMapper = poiMapper;
    }

    @Override
    @Transactional(transactionManager = "spatialTransactionManager", readOnly = true, propagation = Propagation.SUPPORTS)
    public MarkerResponse getMarkers(String bboxRaw, int zoom, Integer page, Integer size, MarkerFilter filter) {
        if (zoom < ZOOM_MIN || zoom > ZOOM_MAX) {
            throw new ApiException(ErrorCode.ZOOM_OUT_OF_RANGE,
                    "zoom 은 " + ZOOM_MIN + "~" + ZOOM_MAX + " 범위여야 합니다: " + zoom);
        }
        if (size != null && size > MAX_PAGE_SIZE) {
            throw new ApiException(ErrorCode.PAGE_SIZE_EXCEEDED,
                    "size 는 최대 " + MAX_PAGE_SIZE + " 입니다: " + size);
        }
        validateFilter(filter);

        Bbox bbox = BboxValidator.parse(bboxRaw);
        boolean detail = zoom >= ZOOM_THRESHOLD_IN;
        boolean oversized = bbox.diagonalMeters() > MAX_DIAGONAL_M;

        if (detail) {
            if (oversized) {
                throw new ApiException(ErrorCode.BBOX_TOO_LARGE_FOR_DETAIL,
                        "상세 조회 bbox 대각이 150km 를 초과했습니다.");
            }
            return detail(bbox, page, size, filter);
        }
        return summary(bbox, oversized, filter);   // SUMMARY 도 매물 갯수에 필터 반영(기능5, §8.1.1)
    }

    /** 검색 필터의 거래/매물 유형 값(다중)이 enum 에 속하는지 원소별 검증(§8.1). 불량 시 INVALID_PARAM. */
    private void validateFilter(MarkerFilter filter) {
        if (filter == null) {
            return;
        }
        if (filter.getDealTypes() != null) {
            filter.getDealTypes().forEach(v -> requireEnum(v, DealType.class, "dealType"));
        }
        if (filter.getPropertyTypes() != null) {
            filter.getPropertyTypes().forEach(v -> requireEnum(v, PropertyType.class, "propertyType"));
        }
    }

    private <E extends Enum<E>> void requireEnum(String value, Class<E> type, String field) {
        try {
            Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.INVALID_PARAM, field + " 값이 유효하지 않습니다: " + value);
        }
    }

    private MarkerResponse detail(Bbox bbox, Integer pageParam, Integer sizeParam, MarkerFilter filter) {
        int page = (pageParam != null) ? pageParam : DEFAULT_PAGE;
        int size = (sizeParam != null) ? sizeParam : DEFAULT_SIZE;

        long totalCount = markerMapper.countMarkers(bbox, filter);
        List<MarkerDTO> markers = markerMapper.findMarkers(bbox, filter, size, (long) page * size);
        boolean hasNext = (long) (page + 1) * size < totalCount;

        return MarkerResponse.detail(markers, page, size, totalCount, hasNext);
    }

    private MarkerResponse summary(Bbox bbox, boolean oversized, MarkerFilter filter) {
        List<RegionSummaryDTO> regions = markerMapper.findRegionSummaries(bbox, filter);
        return MarkerResponse.summary(regions, oversized);
    }

    @Override
    @Transactional(transactionManager = "spatialTransactionManager", readOnly = true, propagation = Propagation.SUPPORTS)
    public List<PropertySearchDTO> searchProperties(String q, Integer limitParam) {
        if (q == null || q.trim().length() < SEARCH_MIN_LEN) {
            return List.of();   // 2자 미만은 검색하지 않음(전체 스캔·과다결과 방지)
        }
        int limit = (limitParam != null) ? limitParam : SEARCH_DEFAULT_LIMIT;
        if (limit < 1 || limit > SEARCH_MAX_LIMIT) {
            throw new ApiException(ErrorCode.INVALID_PARAM,
                    "limit 는 1~" + SEARCH_MAX_LIMIT + " 입니다: " + limit);
        }
        return markerMapper.searchProperties(q.trim(), limit);
    }

    @Override
    @Transactional(transactionManager = "spatialTransactionManager", readOnly = true, propagation = Propagation.SUPPORTS)
    public List<PriceTrendPoint> getPriceTrend(long propertyId) {
        return markerMapper.findPriceTrend(propertyId);
    }

    @Override
    @Transactional(transactionManager = "spatialTransactionManager", readOnly = true, propagation = Propagation.SUPPORTS)
    public List<PoiMarkerDTO> getPois(String bboxRaw, String groupsCsv) {
        if (groupsCsv == null || groupsCsv.isBlank()) {
            return List.of();
        }
        // groups CSV(소문자 key) → Group enum 집합. 미일치 토큰은 무시.
        Set<Category.Group> groups = Arrays.stream(groupsCsv.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(MapServiceImpl::toGroup).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (groups.isEmpty()) {
            return List.of();
        }
        // 선택 그룹 소속 category(대문자 enum 이름) → poi 조회.
        List<String> categories = Arrays.stream(Category.values())
                .filter(c -> groups.contains(c.group()))
                .map(Enum::name).toList();

        Bbox bbox = BboxValidator.parse(bboxRaw);
        List<PoiMarkerDTO> pois = poiMapper.findInBbox(bbox, categories);
        // DB 대문자 category → 소문자 key + group key 정규화(프론트 색칠용).
        pois.forEach(p -> Category.fromDbCategory(p.getCategory()).ifPresent(c -> {
            p.setCategory(c.key());
            p.setGroup(c.group().key());
        }));
        return pois;
    }

    private static Category.Group toGroup(String key) {
        for (Category.Group g : Category.Group.values()) {
            if (g.key().equalsIgnoreCase(key)) {
                return g;
            }
        }
        return null;
    }
}
