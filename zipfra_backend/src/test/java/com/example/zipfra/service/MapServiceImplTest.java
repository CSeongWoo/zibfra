package com.example.zipfra.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.List;

import com.example.zipfra.dto.map.MarkerFilter;
import com.example.zipfra.dto.map.MarkerResponse;
import com.example.zipfra.mapper.postgis.MarkerMapper;
import com.example.zipfra.exception.ApiException;
import com.example.zipfra.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MapServiceImplTest {

    private static final String SMALL_BBOX = "126.9,37.4,127.1,37.6";   // 대각 < 150km
    private static final String HUGE_BBOX = "120.0,33.0,130.0,39.0";    // 대각 > 150km
    private static final MarkerFilter NO_FILTER = new MarkerFilter(null, null, null, null);

    @Mock
    private MarkerMapper markerMapper;

    private MapServiceImpl service() {
        return new MapServiceImpl(markerMapper);
    }

    @Test
    @DisplayName("T-2: zoom=14 → SUMMARY")
    void zoom14_summary() {
        when(markerMapper.findRegionSummaries(any())).thenReturn(List.of());
        MarkerResponse res = service().getMarkers(SMALL_BBOX, 14, null, null, NO_FILTER);
        assertThat(res.getStrategy()).isEqualTo("SUMMARY");
    }

    @Test
    @DisplayName("T-2: zoom=15 → DETAIL")
    void zoom15_detail() {
        when(markerMapper.countMarkers(any(), any())).thenReturn(0L);
        when(markerMapper.findMarkers(any(), any(), anyInt(), anyLong())).thenReturn(List.of());
        MarkerResponse res = service().getMarkers(SMALL_BBOX, 15, null, null, NO_FILTER);
        assertThat(res.getStrategy()).isEqualTo("DETAIL");
        assertThat(res.getSize()).isEqualTo(100);
        assertThat(res.getPage()).isZero();
    }

    @Test
    @DisplayName("zoom 0/22 → ZOOM_OUT_OF_RANGE")
    void zoomOutOfRange() {
        assertThatThrownBy(() -> service().getMarkers(SMALL_BBOX, 0, null, null, NO_FILTER))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ZOOM_OUT_OF_RANGE);
        assertThatThrownBy(() -> service().getMarkers(SMALL_BBOX, 22, null, null, NO_FILTER))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ZOOM_OUT_OF_RANGE);
    }

    @Test
    @DisplayName("T-1/T-4: 대각>150km + zoom=15 → BBOX_TOO_LARGE_FOR_DETAIL")
    void hugeBbox_detail_rejected() {
        assertThatThrownBy(() -> service().getMarkers(HUGE_BBOX, 15, null, null, NO_FILTER))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BBOX_TOO_LARGE_FOR_DETAIL);
    }

    @Test
    @DisplayName("T-1/T-4: 대각>150km + zoom=10 → SUMMARY + bboxOversized")
    void hugeBbox_summary_downgraded() {
        when(markerMapper.findRegionSummaries(any())).thenReturn(List.of());
        MarkerResponse res = service().getMarkers(HUGE_BBOX, 10, null, null, NO_FILTER);
        assertThat(res.getStrategy()).isEqualTo("SUMMARY");
        assertThat(res.isBboxOversized()).isTrue();
    }

    @Test
    @DisplayName("T-4: size=201 → PAGE_SIZE_EXCEEDED")
    void pageSizeExceeded() {
        assertThatThrownBy(() -> service().getMarkers(SMALL_BBOX, 15, 0, 201, NO_FILTER))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PAGE_SIZE_EXCEEDED);
    }

    @Test
    @DisplayName("T-4: totalCount=342, size=100, page=0 → hasNext=true")
    void hasNext_true() {
        when(markerMapper.countMarkers(any(), any())).thenReturn(342L);
        when(markerMapper.findMarkers(any(), any(), anyInt(), anyLong())).thenReturn(List.of());
        MarkerResponse res = service().getMarkers(SMALL_BBOX, 15, 0, 100, NO_FILTER);
        assertThat(res.getHasNext()).isTrue();
        assertThat(res.getTotalCount()).isEqualTo(342L);
    }

    @Test
    @DisplayName("T-4: totalCount=342, size=100, page=3 → hasNext=false")
    void hasNext_false() {
        when(markerMapper.countMarkers(any(), any())).thenReturn(342L);
        when(markerMapper.findMarkers(any(), any(), anyInt(), anyLong())).thenReturn(List.of());
        MarkerResponse res = service().getMarkers(SMALL_BBOX, 15, 3, 100, NO_FILTER);
        assertThat(res.getHasNext()).isFalse();
    }

    @Test
    @DisplayName("T-10: dealType 값 불량 → INVALID_PARAM (mapper 호출 전 거부)")
    void invalidDealType_rejected() {
        MarkerFilter bad = new MarkerFilter("INVALID", null, null, null);
        assertThatThrownBy(() -> service().getMarkers(SMALL_BBOX, 15, null, null, bad))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_PARAM);
    }

    @Test
    @DisplayName("T-10: propertyType 값 불량 → INVALID_PARAM")
    void invalidPropertyType_rejected() {
        MarkerFilter bad = new MarkerFilter(null, "VILLA_TYPO", null, null);
        assertThatThrownBy(() -> service().getMarkers(SMALL_BBOX, 15, null, null, bad))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_PARAM);
    }

    @Test
    @DisplayName("T-10: 유효 필터(SALE/APT)는 통과해 DETAIL 반환")
    void validFilter_passes() {
        when(markerMapper.countMarkers(any(), any())).thenReturn(0L);
        when(markerMapper.findMarkers(any(), any(), anyInt(), anyLong())).thenReturn(List.of());
        MarkerFilter ok = new MarkerFilter("SALE", "APT", 10000, 50000);
        MarkerResponse res = service().getMarkers(SMALL_BBOX, 15, null, null, ok);
        assertThat(res.getStrategy()).isEqualTo("DETAIL");
    }
}
