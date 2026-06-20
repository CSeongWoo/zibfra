<template>
  <div ref="mapContainer" class="kakao-map"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue';
import { useMapStore } from '@/stores/map';
import { kakaoBoundsToBbox } from '@/utils/bbox';
import { totalScore, scoreColor } from '@/utils/score';

// 초기 중심/줌(기본: 강남 삼성동 — 실거래 데이터 밀집 지역). 마커 데이터(MAP-01)는 `markers`로 주입.
const props = defineProps({
  center: { type: Object, default: () => ({ lat: 37.5072, lng: 127.0533 }) },
  level: { type: Number, default: 5 },
  markers: { type: Array, default: () => [] }, // [{ id, lat, lng }]
});

// §7.4: 지도는 상세 로직 없이 이벤트만 발신한다.
const emit = defineEmits(['marker-click', 'viewport-change', 'map-click']);

const mapContainer = ref(null);
const mapStore = useMapStore();

let map = null;
let idleListener = null;
let clickListener = null;
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
    // §5.1: 매물 종합 점수(base×페르소나 가중치 정규화)를 핀 마커로 표시(#6 실시간 반영).
    const score = totalScore(item, mapStore.persona);
    const color = scoreColor(score);
    const pin = document.createElement('div');
    pin.style.cssText = 'position:relative;width:36px;height:36px;cursor:pointer;';
    const circle = document.createElement('div');
    circle.textContent = score;
    circle.style.cssText =
      `width:36px;height:36px;border-radius:50%;background:${color};color:#fff;`
      + 'display:flex;align-items:center;justify-content:center;font-weight:700;font-size:13px;'
      + 'border:2px solid #fff;box-shadow:0 2px 8px rgba(0,0,0,.4);';
    const tail = document.createElement('div');
    tail.style.cssText =
      `position:absolute;left:50%;bottom:-6px;transform:translateX(-50%);width:0;height:0;`
      + `border-left:5px solid transparent;border-right:5px solid transparent;border-top:8px solid ${color};`;
    pin.appendChild(circle);
    pin.appendChild(tail);
    // §7.4: 모달 등 상세 구현 금지 — propertyId만 발신.
    pin.addEventListener('click', () => emit('marker-click', item.id));
    const overlay = new kakao.maps.CustomOverlay({
      position: new kakao.maps.LatLng(item.lat, item.lng),
      content: pin,
      yAnchor: 1.0, // 핀 꼬리 끝이 좌표에 닿도록
      clickable: true,
    });
    overlay.setMap(map);
    markerObjects.push(overlay);
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
  // LOC-01: 지도 클릭 좌표만 발신(상세 계산은 상위에서). §7.4 단방향.
  clickListener = (mouseEvent) => {
    const latlng = mouseEvent.latLng;
    emit('map-click', { lat: latlng.getLat(), lng: latlng.getLng() });
  };
  kakao.maps.event.addListener(map, 'click', clickListener);
  renderMarkers();
  syncViewport(); // 초기 1회
}

watch(() => props.markers, renderMarkers, { deep: true });
// #6: 페르소나 가중치 변경 시 배지 점수·색상 실시간 갱신.
watch(() => mapStore.persona, renderMarkers, { deep: true });
// #7: 검색·현위치 이동 명령 → 지도 중심/레벨 변경(이후 idle 이벤트가 markers 재조회).
watch(() => mapStore.moveTarget, (t) => {
  if (!map || !t) return;
  const { kakao } = window;
  map.setCenter(new kakao.maps.LatLng(t.lat, t.lng));
  if (t.level != null) map.setLevel(t.level);
});
// #7: 줌 +/- 버튼 → 레벨 변경(delta +1=줌인이므로 level 감소).
watch(() => mapStore.zoomReq, (z) => {
  if (!map || !z) return;
  map.setLevel(map.getLevel() - z.delta);
});

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
  if (map && kakao?.maps) {
    if (idleListener) kakao.maps.event.removeListener(map, 'idle', idleListener);
    if (clickListener) kakao.maps.event.removeListener(map, 'click', clickListener);
  }
  markerObjects.forEach((m) => m.setMap(null));
  markerObjects.length = 0;
  idleListener = null;
  clickListener = null;
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
