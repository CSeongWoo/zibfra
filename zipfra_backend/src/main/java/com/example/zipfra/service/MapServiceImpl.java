package com.example.zipfra.service;

import java.util.List;

import com.example.zipfra.dto.map.Bbox;
import com.example.zipfra.dto.map.MarkerDTO;
import com.example.zipfra.dto.map.MarkerResponse;
import com.example.zipfra.dto.map.RegionSummaryDTO;
import com.example.zipfra.mapper.postgis.MarkerMapper;
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

    private final MarkerMapper markerMapper;

    public MapServiceImpl(MarkerMapper markerMapper) {
        this.markerMapper = markerMapper;
    }

    @Override
    @Transactional(transactionManager = "spatialTransactionManager", readOnly = true, propagation = Propagation.SUPPORTS)
    public MarkerResponse getMarkers(String bboxRaw, int zoom, Integer page, Integer size) {
        if (zoom < ZOOM_MIN || zoom > ZOOM_MAX) {
            throw new ApiException(ErrorCode.ZOOM_OUT_OF_RANGE,
                    "zoom 은 " + ZOOM_MIN + "~" + ZOOM_MAX + " 범위여야 합니다: " + zoom);
        }
        if (size != null && size > MAX_PAGE_SIZE) {
            throw new ApiException(ErrorCode.PAGE_SIZE_EXCEEDED,
                    "size 는 최대 " + MAX_PAGE_SIZE + " 입니다: " + size);
        }

        Bbox bbox = BboxValidator.parse(bboxRaw);
        boolean detail = zoom >= ZOOM_THRESHOLD_IN;
        boolean oversized = bbox.diagonalMeters() > MAX_DIAGONAL_M;

        if (detail) {
            if (oversized) {
                throw new ApiException(ErrorCode.BBOX_TOO_LARGE_FOR_DETAIL,
                        "상세 조회 bbox 대각이 150km 를 초과했습니다.");
            }
            return detail(bbox, page, size);
        }
        return summary(bbox, oversized);
    }

    private MarkerResponse detail(Bbox bbox, Integer pageParam, Integer sizeParam) {
        int page = (pageParam != null) ? pageParam : DEFAULT_PAGE;
        int size = (sizeParam != null) ? sizeParam : DEFAULT_SIZE;

        long totalCount = markerMapper.countMarkers(bbox);
        List<MarkerDTO> markers = markerMapper.findMarkers(bbox, size, (long) page * size);
        boolean hasNext = (long) (page + 1) * size < totalCount;

        return MarkerResponse.detail(markers, page, size, totalCount, hasNext);
    }

    private MarkerResponse summary(Bbox bbox, boolean oversized) {
        List<RegionSummaryDTO> regions = markerMapper.findRegionSummaries(bbox);
        return MarkerResponse.summary(regions, oversized);
    }
}
