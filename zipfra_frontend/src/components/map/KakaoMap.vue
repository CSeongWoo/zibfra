<template>
  <div ref="mapContainer" class="kakao-map"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue';
import { useMapStore } from '@/stores/map';
import { kakaoBoundsToBbox } from '@/utils/bbox';

// 초기 중심/줌(기본: 강남 삼성동 — 실거래 데이터 밀집 지역). 마커 데이터(MAP-01)는 `markers`로 주입.
const props = defineProps({
  center: { type: Object, default: () => ({ lat: 37.5072, lng: 127.0533 }) },
  level: { type: Number, default: 5 },
  markers: { type: Array, default: () => [] }, // [{ id, lat, lng }]
});

// §7.4: 지도는 상세 로직 없이 이벤트만 발신한다.
const emit = defineEmits(['marker-click', 'viewport-change']);

const mapContainer = ref(null);
const mapStore = useMapStore();

let map = null;
let idleListener = null;
const markerObjects = [];

function syncViewport() {
  if (!map) return;
  const bbox = kakaoBoundsToBbox(map.getBounds());
  const level = map.getLevel(); // 카카오 level = zoom (서버가 권위로 재판정 §7.1)
  mapStore.setViewport({ bbox, zoom: level });
  emit('viewport-change', { bbox, level });
}

function renderMarkers() {
  if (!map) return;
  const { kakao } = window;
  markerObjects.forEach((m) => m.setMap(null));
  markerObjects.length = 0;
  props.markers.forEach((item) => {
    const marker = new kakao.maps.Marker({
      position: new kakao.maps.LatLng(item.lat, item.lng),
      map,
    });
    // §7.4: 모달 등 상세 구현 금지 — propertyId만 발신.
    kakao.maps.event.addListener(marker, 'click', () => emit('marker-click', item.id));
    markerObjects.push(marker);
  });
}

function initMap() {
  const { kakao } = window;
  map = new kakao.maps.Map(mapContainer.value, {
    center: new kakao.maps.LatLng(props.center.lat, props.center.lng),
    level: props.level,
  });
  idleListener = () => syncViewport();
  kakao.maps.event.addListener(map, 'idle', idleListener);
  renderMarkers();
  syncViewport(); // 초기 1회
}

watch(() => props.markers, renderMarkers, { deep: true });

onMounted(() => {
  const { kakao } = window;
  if (!kakao?.maps) {
    console.warn(
      '[KakaoMap] 카카오 맵 SDK 미로드. index.html의 SDK 스크립트와 .env의 VITE_KAKAO_APP_KEY를 확인하세요.',
    );
    return;
  }
  kakao.maps.load(initMap); // index.html에서 autoload=false로 로드했으므로 명시 호출
});

onUnmounted(() => {
  // §7 규약: 리스너·인스턴스 해제
  const { kakao } = window;
  if (map && idleListener && kakao?.maps) {
    kakao.maps.event.removeListener(map, 'idle', idleListener);
  }
  markerObjects.forEach((m) => m.setMap(null));
  markerObjects.length = 0;
  idleListener = null;
  map = null;
});
</script>

<style scoped>
.kakao-map {
  width: 100%;
  height: 100%;
  min-height: 400px;
}
</style>
