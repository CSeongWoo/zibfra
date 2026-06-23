<template>
  <!--
    MAP-01 연동 컨테이너 (§7.4 단방향 흐름).
    지도(KakaoMap)는 뷰포트 이벤트만 발신 → 여기서 MAP-01 호출 → markers 되주입.
    마커 클릭(propertyId)은 그대로 상위로 중계(모달 등은 Dev B 영역).
  -->
  <KakaoMap
    :markers="mapStore.markers"
    @viewport-change="onViewport"
    @marker-click="(propertyId) => emit('marker-click', propertyId)"
    @map-click="(coord) => emit('map-click', coord)"
  />
</template>

<script setup>
import { ref, watch } from 'vue';
import KakaoMap from '@/components/map/KakaoMap.vue';
import { useMapStore } from '@/stores/map';
import { fetchMarkers } from '@/api/markers';
import { fetchPois } from '@/api/pois';

const emit = defineEmits(['marker-click', 'viewport-change', 'map-click']);

const mapStore = useMapStore();

// 마지막 뷰포트 보관 — 필터 변경 시 같은 화면 범위로 재조회하기 위함.
const lastBbox = ref(null);
const lastLevel = ref(null);

// §7.1 협의 결과(프론트 한정 변환): 카카오 level(1~14, 작을수록 줌인)을
// 서버 zoom 척도(1~21, ≥15=DETAIL)로 변환한다. 서버는 여전히 임계값으로 재판정(권위).
// 예: level 5→zoom 15(DETAIL), level 6→zoom 14(SUMMARY).
function kakaoLevelToZoom(level) {
  return Math.min(21, Math.max(1, 20 - level));
}

async function loadMarkers() {
  if (!lastBbox.value) return;
  try {
    const f = mapStore.filter; // §8.1 검색 필터(빈 배열·null 은 markers.js 가 전송 생략)
    const data = await fetchMarkers({
      bbox: lastBbox.value,
      zoom: kakaoLevelToZoom(lastLevel.value),
      dealType: f.dealTypes,        // 다중선택 배열 → markers.js 가 콤마 join
      propertyType: f.propertyTypes,
      priceMin: f.priceMin,
      priceMax: f.priceMax,
    });
    mapStore.setStrategy(data.strategy); // 서버 권위 전략 보관(§7.1)
    // 전략별 분기: DETAIL=개별 매물 마커, SUMMARY=시·군·구 집계 풍선(§7.1)
    if (data.strategy === 'DETAIL') {
      mapStore.setMarkers(data.markers ?? []);
      mapStore.setRegions([]);
    } else {
      mapStore.setRegions(data.regions ?? []);
      mapStore.setMarkers([]);
    }
  } catch (e) {
    // 공통 에러 계약(§8.3): { error, message, timestamp, status }
    console.error('[MAP-01] markers 조회 실패:', e.error, e.message);
    mapStore.setMarkers([]);
    mapStore.setRegions([]);
  }
  loadPois(); // MAP-01 전략 확정 후 POI 오버레이 갱신
}

// MAP-02: DETAIL + 켜진 인프라 그룹의 POI 오버레이 조회. SUMMARY·토글 전무면 비움.
async function loadPois() {
  if (!lastBbox.value || mapStore.strategy !== 'DETAIL') {
    mapStore.setPois([]);
    return;
  }
  const on = Object.entries(mapStore.infraLayers).filter(([, v]) => v).map(([k]) => k);
  if (!on.length) {
    mapStore.setPois([]);
    return;
  }
  try {
    const data = await fetchPois({ bbox: lastBbox.value, groups: on.join(',') });
    mapStore.setPois(data);
  } catch (e) {
    console.error('[MAP-02] pois 조회 실패:', e.error, e.message);
    mapStore.setPois([]);
  }
}

function onViewport({ bbox, level }) {
  emit('viewport-change', { bbox, level });
  if (!bbox) return;
  lastBbox.value = bbox;
  lastLevel.value = level;
  loadMarkers();
}

// 필터 변경 → 현재 뷰포트로 재조회(§8.1.1). 필터는 DETAIL 에서만 서버가 적용.
watch(() => mapStore.filter, loadMarkers, { deep: true });
// 인프라 토글 변경 → POI 오버레이만 재조회(MAP-02). 마커 재조회는 불필요.
watch(() => mapStore.infraLayers, loadPois, { deep: true });
</script>
