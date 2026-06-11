<template>
  <!--
    MAP-01 연동 컨테이너 (§7.4 단방향 흐름).
    지도(KakaoMap)는 뷰포트 이벤트만 발신 → 여기서 MAP-01 호출 → markers 되주입.
    마커 클릭(propertyId)은 그대로 상위로 중계(모달 등은 Dev B 영역).
  -->
  <KakaoMap
    :markers="markers"
    @viewport-change="onViewport"
    @marker-click="(propertyId) => emit('marker-click', propertyId)"
    @map-click="(coord) => emit('map-click', coord)"
  />
</template>

<script setup>
import { ref } from 'vue';
import KakaoMap from '@/components/map/KakaoMap.vue';
import { useMapStore } from '@/stores/map';
import { fetchMarkers } from '@/api/markers';

const emit = defineEmits(['marker-click', 'viewport-change', 'map-click']);

const mapStore = useMapStore();
const markers = ref([]); // KakaoMap 이 그릴 점마커 [{ id, lat, lng }]

// §7.1 협의 결과(프론트 한정 변환): 카카오 level(1~14, 작을수록 줌인)을
// 서버 zoom 척도(1~21, ≥15=DETAIL)로 변환한다. 서버는 여전히 임계값으로 재판정(권위).
// 예: level 5→zoom 15(DETAIL), level 6→zoom 14(SUMMARY).
function kakaoLevelToZoom(level) {
  return Math.min(21, Math.max(1, 20 - level));
}

async function onViewport({ bbox, level }) {
  emit('viewport-change', { bbox, level });
  if (!bbox) return;

  try {
    const data = await fetchMarkers({ bbox, zoom: kakaoLevelToZoom(level) });
    mapStore.setStrategy(data.strategy); // 서버 권위 전략 보관(§7.1)
    // B안: DETAIL 점마커만 렌더. SUMMARY 집계 풍선은 후속 PR에서 시각화.
    markers.value = data.strategy === 'DETAIL' ? data.markers : [];
  } catch (e) {
    // 공통 에러 계약(§8.3): { error, message, timestamp, status }
    console.error('[MAP-01] markers 조회 실패:', e.error, e.message);
    markers.value = [];
  }
}
</script>
