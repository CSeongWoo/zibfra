package com.example.zipfra.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.List;

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

    @Mock
    private MarkerMapper markerMapper;

    private MapServiceImpl service() {
        return new MapServiceImpl(markerMapper);
    }

    @Test
    @DisplayName("T-2: zoom=14 → SUMMARY")
    void zoom14_summary() {
        when(markerMapper.findRegionSummaries(any())).thenReturn(List.of());
        MarkerResponse res = service().getMarkers(SMALL_BBOX, 14, null, null);
        assertThat(res.getStrategy()).isEqualTo("SUMMARY");
    }

    @Test
    @DisplayName("T-2: zoom=15 → DETAIL")
    void zoom15_detail() {
        when(markerMapper.countMarkers(any())).thenReturn(0L);
        when(markerMapper.findMarkers(any(), anyInt(), anyLong())).thenReturn(List.of());
        MarkerResponse res = service().getMarkers(SMALL_BBOX, 15, null, null);
        assertThat(res.getStrategy()).isEqualTo("DETAIL");
        assertThat(res.getSize()).isEqualTo(100);
        assertThat(res.getPage()).isZero();
    }

    @Test
    @DisplayName("zoom 0/22 → ZOOM_OUT_OF_RANGE")
    void zoomOutOfRange() {
        assertThatThrownBy(() -> service().getMarkers(SMALL_BBOX, 0, null, null))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ZOOM_OUT_OF_RANGE);
        assertThatThrownBy(() -> service().getMarkers(SMALL_BBOX, 22, null, null))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ZOOM_OUT_OF_RANGE);
    }

    @Test
    @DisplayName("T-1/T-4: 대각>150km + zoom=15 → BBOX_TOO_LARGE_FOR_DETAIL")
    void hugeBbox_detail_rejected() {
        assertThatThrownBy(() -> service().getMarkers(HUGE_BBOX, 15, null, null))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BBOX_TOO_LARGE_FOR_DETAIL);
    }

    @Test
    @DisplayName("T-1/T-4: 대각>150km + zoom=10 → SUMMARY + bboxOversized")
    void hugeBbox_summary_downgraded() {
        when(markerMapper.findRegionSummaries(any())).thenReturn(List.of());
        MarkerResponse res = service().getMarkers(HUGE_BBOX, 10, null, null);
        assertThat(res.getStrategy()).isEqualTo("SUMMARY");
        assertThat(res.isBboxOversized()).isTrue();
    }

    @Test
    @DisplayName("T-4: size=201 → PAGE_SIZE_EXCEEDED")
    void pageSizeExceeded() {
        assertThatThrownBy(() -> service().getMarkers(SMALL_BBOX, 15, 0, 201))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PAGE_SIZE_EXCEEDED);
    }

    @Test
    @DisplayName("T-4: totalCount=342, size=100, page=0 → hasNext=true")
    void hasNext_true() {
        when(markerMapper.countMarkers(any())).thenReturn(342L);
        when(markerMapper.findMarkers(any(), anyInt(), anyLong())).thenReturn(List.of());
        MarkerResponse res = service().getMarkers(SMALL_BBOX, 15, 0, 100);
        assertThat(res.getHasNext()).isTrue();
        assertThat(res.getTotalCount()).isEqualTo(342L);
    }

    @Test
    @DisplayName("T-4: totalCount=342, size=100, page=3 → hasNext=false")
    void hasNext_false() {
        when(markerMapper.countMarkers(any())).thenReturn(342L);
        when(markerMapper.findMarkers(any(), anyInt(), anyLong())).thenReturn(List.of());
        MarkerResponse res = service().getMarkers(SMALL_BBOX, 15, 3, 100);
        assertThat(res.getHasNext()).isFalse();
    }
}
