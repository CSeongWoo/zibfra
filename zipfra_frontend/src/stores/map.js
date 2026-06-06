import { defineStore } from 'pinia';
import { ref } from 'vue';

/**
 * 지도 뷰포트 상태 스토어 골격 (Phase 1 Dev B).
 *
 * 카카오 맵(PR-3)이 viewport를 갱신하고, MAP-01 호출(Phase 2 Dev A)이
 * 이 상태를 읽어 bbox/zoom 파라미터로 사용한다. 줌→전략 판정은 서버 권위(§7.1)이며
 * `strategy`는 서버 응답('DETAIL' | 'SUMMARY')을 그대로 보관한다.
 */
export const useMapStore = defineStore('map', () => {
  const bbox = ref(null);     // "minLng,minLat,maxLng,maxLat" (EPSG:4326, §8 공통 규약)
  const zoom = ref(null);     // 카카오 level (서버가 임계값으로 재판정)
  const strategy = ref(null); // 서버 응답 전략: 'DETAIL' | 'SUMMARY'

  function setViewport({ bbox: nextBbox, zoom: nextZoom } = {}) {
    if (nextBbox !== undefined) bbox.value = nextBbox;
    if (nextZoom !== undefined) zoom.value = nextZoom;
  }

  function setStrategy(next) {
    strategy.value = next;
  }

  return { bbox, zoom, strategy, setViewport, setStrategy };
});
