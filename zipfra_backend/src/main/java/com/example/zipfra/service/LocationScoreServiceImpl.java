package com.example.zipfra.service;

import com.example.zipfra.dto.location.PoiDistanceDTO;
import com.example.zipfra.dto.location.ScoreRequest;
import com.example.zipfra.dto.location.ScoreResponse;
import com.example.zipfra.mapper.postgis.PoiMapper;
import com.example.zipfra.exception.ApiException;
import com.example.zipfra.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * LOC-01 입지 점수 서비스 구현 (AGENTS.md §5).
 * PostGIS 단일 read-only 트랜잭션(spatialTransactionManager, SUPPORTS)에서 POI 를 추출하고,
 * 감쇠·합산은 메모리 계산기(LocationScoreCalculator)에 위임한다.
 */
@Slf4j
@Service
public class LocationScoreServiceImpl implements LocationScoreService {

    private static final double LON_MIN = -180.0;
    private static final double LON_MAX = 180.0;
    private static final double LAT_MIN = -90.0;
    private static final double LAT_MAX = 90.0;
    private static final int RADIUS_MIN = 100;
    private static final int RADIUS_MAX = 3000;
    private static final int RADIUS_DEFAULT = 1500;

    private final PoiMapper poiMapper;
    private final LocationScoreCalculator calculator;

    public LocationScoreServiceImpl(PoiMapper poiMapper, LocationScoreCalculator calculator) {
        this.poiMapper = poiMapper;
        this.calculator = calculator;
    }

    @Override
    @Transactional(
            transactionManager = "spatialTransactionManager",
            readOnly = true,
            propagation = Propagation.SUPPORTS
    )
    public ScoreResponse score(ScoreRequest request) {
        double lon = requireCoord(request.getLon(), "lon");
        double lat = requireCoord(request.getLat(), "lat");
        validateCoordRange(lon, lat);
        int radiusMeters = resolveRadius(request.getRadiusMeters());
        validateWeights(request.weightsOrEmpty());

        boolean inSeoul = poiMapper.isInSeoul(lon, lat);
        List<PoiDistanceDTO> pois = poiMapper.findWithin(lon, lat, radiusMeters);

        LocationScoreCalculator.CalcResult result =
                calculator.calculate(pois, request.weightsOrEmpty(), inSeoul);

        return new ScoreResponse(lon, lat, radiusMeters, result.finalScore(), result.breakdown());
    }

    private double requireCoord(Double value, String field) {
        if (value == null) {
            throw new ApiException(ErrorCode.INVALID_PARAM, field + " is required");
        }
        return value;
    }

    private void validateCoordRange(double lon, double lat) {
        if (lon < LON_MIN || lon > LON_MAX || lat < LAT_MIN || lat > LAT_MAX) {
            throw new ApiException(ErrorCode.COORD_OUT_OF_RANGE,
                    "lon must be in [-180,180] and lat in [-90,90]");
        }
    }

    private int resolveRadius(Integer radiusMeters) {
        if (radiusMeters == null) {
            return RADIUS_DEFAULT;
        }
        if (radiusMeters < RADIUS_MIN || radiusMeters > RADIUS_MAX) {
            throw new ApiException(ErrorCode.RADIUS_OUT_OF_RANGE,
                    "radiusMeters must be in [" + RADIUS_MIN + ", " + RADIUS_MAX + "]");
        }
        return radiusMeters;
    }

    private void validateWeights(Map<String, Double> weights) {
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            Double value = entry.getValue();
            if (value == null || value < 0.0 || value > 1.0) {
                throw new ApiException(ErrorCode.WEIGHT_OUT_OF_RANGE,
                        "weight '" + entry.getKey() + "' must be in [0.0, 1.0]");
            }
        }
    }
}
