import { defineStore } from 'pinia';
import { ref } from 'vue';
import { DEFAULT_PERSONA } from '@/utils/score';

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

  // MAP-01 검색 필터(§8.1, PR A). 빈 배열 = 전체. MAP-01 DETAIL 에서만 적용.
  // dealTypes/propertyTypes 는 다중선택(§8.1 다중선택 계약) — markers.js 가 콤마 join.
  const filter = ref({
    dealTypes: [],       // (SALE | JEONSE | WOLSE)[]
    propertyTypes: [],   // (APT | OFFICETEL | ROW_HOUSE)[]
    priceMin: null,      // 만원
    priceMax: null,      // 만원
  });

  /** 다중선택 칩 토글: 배열에 있으면 제거, 없으면 추가. */
  function toggleFilterValue(key, value) {
    const cur = filter.value[key] ?? [];
    const next = cur.includes(value) ? cur.filter((v) => v !== value) : [...cur, value];
    filter.value = { ...filter.value, [key]: next };
  }

  // 현재 DETAIL 마커 목록(MAP-01 응답). 지도와 우측 목록 패널이 공유(§7.4 단방향).
  const markers = ref([]);

  function setMarkers(next) {
    markers.value = Array.isArray(next) ? next : [];
  }

  // 현재 SUMMARY 시·군·구 집계 목록(MAP-01 줌아웃 응답, §7.1).
  const regions = ref([]);

  function setRegions(next) {
    regions.value = Array.isArray(next) ? next : [];
  }

  // 지도 중심 이동 명령(#7 검색·현위치). KakaoMap 이 watch 해 setCenter/setLevel(§7.4 단방향: UI→지도).
  const moveTarget = ref(null); // { lat, lng, level? }

  function requestMove(lat, lng, level) {
    moveTarget.value = { lat, lng, level };
  }

  // 줌 +/- 버튼(#7). delta +1=줌인(카카오 level 감소), -1=줌아웃. 매번 새 객체로 watch 트리거.
  const zoomReq = ref(null);

  function requestZoom(delta) {
    zoomReq.value = { delta };
  }

  // 인프라 표시 토글(와이어3 좌측). 그룹별 POI 오버레이 on/off (MAP-02).
  const infraLayers = ref({ transit: true, education: true, commerce: true, convenience: true });

  function toggleInfra(key) {
    infraLayers.value = { ...infraLayers.value, [key]: !infraLayers.value[key] };
  }

  // MAP-02 POI 오버레이 마커(켜진 그룹의 POI). DETAIL 에서만 채움.
  const pois = ref([]);

  function setPois(next) {
    pois.value = Array.isArray(next) ? next : [];
  }

  // 페르소나 그룹 가중치(#6, §5.1 표시 단계). base 사전계산값에 실시간 곱 → 마커/목록 점수 갱신.
  const persona = ref({ ...DEFAULT_PERSONA });

  /** 페르소나 가중치 부분 갱신(슬라이더 한 축) 또는 프리셋 전체 교체. */
  function setPersona(partial) {
    persona.value = { ...persona.value, ...partial };
  }

  function setViewport({ bbox: nextBbox, zoom: nextZoom } = {}) {
    if (nextBbox !== undefined) bbox.value = nextBbox;
    if (nextZoom !== undefined) zoom.value = nextZoom;
  }

  function setStrategy(next) {
    strategy.value = next;
  }

  /** 필터 부분 갱신(나머지 키는 유지). MapContainer 가 deep watch 로 재조회. */
  function setFilter(partial) {
    filter.value = { ...filter.value, ...partial };
  }

  return {
    bbox, zoom, strategy, filter, persona, markers, regions, moveTarget, infraLayers, zoomReq, pois,
    setViewport, setStrategy, setFilter, toggleFilterValue, setPersona, setMarkers, setRegions,
    requestMove, toggleInfra, requestZoom, setPois,
  };
});
