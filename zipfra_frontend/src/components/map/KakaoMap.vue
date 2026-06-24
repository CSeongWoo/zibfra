<template>
  <div ref="mapContainer" class="kakao-map"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue';
import { useMapStore } from '@/stores/map';
import { kakaoBoundsToBbox } from '@/utils/bbox';
import { totalScore, scoreColor } from '@/utils/score';
import { clusterPriceLabel } from '@/utils/price';

// 초기 중심/줌(기본: 강남 삼성동 — 실거래 데이터 밀집 지역). 마커 데이터(MAP-01)는 `markers`로 주입.
const props = defineProps({
  center: { type: Object, default: () => ({ lat: 37.5072, lng: 127.0533 }) },
  level: { type: Number, default: 5 },
  markers: { type: Array, default: () => [] }, // [{ id, lat, lng }]
});

// §7.4: 지도는 상세 로직 없이 이벤트만 발신한다. marker-click 은 클러스터 매물 배열(MarkerDTO[]).
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

// §7.1 전략 분기: DETAIL=개별 매물 핀, SUMMARY=시·군·구 집계 풍선.
function render() {
  if (!map) return;
  markerObjects.forEach((m) => m.setMap(null));
  markerObjects.length = 0;
  if (mapStore.strategy === 'SUMMARY') {
    renderRegions();
  } else {
    renderDetailMarkers();
  }
}

// SUMMARY: 시·군·구 중심에 집계 풍선(지역명 + 매물 갯수). 클릭 시 그 지역으로 줌인.
function renderRegions() {
  const { kakao } = window;
  mapStore.regions.forEach((r) => {
    const bubble = document.createElement('div');
    bubble.style.cssText =
      'display:flex;flex-direction:column;align-items:center;justify-content:center;'
      + 'width:62px;height:62px;border-radius:50%;background:rgba(37,99,235,0.85);'
      + 'color:#fff;border:2px solid rgba(255,255,255,.45);box-shadow:0 4px 12px rgba(0,0,0,.45);'
      + 'cursor:pointer;text-align:center;line-height:1.2;';
    bubble.innerHTML =
      `<span style="font-size:10px;opacity:.9;">${r.regionName ?? ''}</span>`
      + `<span style="font-size:15px;font-weight:800;">${r.propertyCount ?? 0}</span>`;
    // §7.4: 풍선 클릭 → 해당 지역으로 줌인(DETAIL 전환). 지도 제어만, 상세 모달 X.
    bubble.addEventListener('click', () => {
      map.setCenter(new kakao.maps.LatLng(r.lat, r.lng));
      map.setLevel(5); // ≈ DETAIL
    });
    const overlay = new kakao.maps.CustomOverlay({
      position: new kakao.maps.LatLng(r.lat, r.lng),
      content: bubble,
      yAnchor: 0.5,
      clickable: true,
    });
    overlay.setMap(map);
    markerObjects.push(overlay);
  });
}

// DETAIL: 동일좌표(=같은 단지, 층만 다른 매물) 묶어 핀 1개. 평균 점수·평균가 + N건 뱃지.
function renderDetailMarkers() {
  const { kakao } = window;
  // 좌표 5자리 키로 그룹핑. 단지당 지오코딩 1회라 같은 단지 매물은 좌표가 동일(설계 §4).
  const groups = new Map();
  props.markers.forEach((item) => {
    const key = `${item.lat.toFixed(5)},${item.lng.toFixed(5)}`;
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key).push(item);
  });

  groups.forEach((items) => {
    const rep = items[0];
    // §5.1: 그룹 평균 종합 점수(base×페르소나 가중치 정규화, #6 실시간). 동일좌표라 사실상 동일값.
    const score = Math.round(
      items.reduce((s, it) => s + totalScore(it, mapStore.persona), 0) / items.length,
    );
    const color = scoreColor(score);
    const price = clusterPriceLabel(items); // 단건=실거래가, 다건=평균 대표가(~)
    const count = items.length;
    const pin = document.createElement('div');
    pin.style.cssText = 'position:relative;cursor:pointer;';
    // 점수 배지 + 가격 알약(Glassmorphism §7.5). 배지 색은 점수 색.
    const pill = document.createElement('div');
    pill.style.cssText =
      'display:flex;align-items:center;gap:5px;padding:3px 9px 3px 3px;border-radius:9999px;'
      + 'background:rgba(255,255,255,.92);backdrop-filter:blur(4px);'
      + 'border:1px solid rgba(255,255,255,.6);box-shadow:0 2px 8px rgba(0,0,0,.28);white-space:nowrap;';
    const circle = document.createElement('div');
    circle.textContent = score;
    circle.style.cssText =
      `width:26px;height:26px;border-radius:50%;background:${color};color:#fff;`
      + 'display:flex;align-items:center;justify-content:center;font-weight:700;font-size:12px;flex:none;';
    const priceEl = document.createElement('span');
    priceEl.textContent = price;
    priceEl.style.cssText = 'font-size:12px;font-weight:700;color:#0f172a;';
    pill.appendChild(circle);
    pill.appendChild(priceEl);
    // 동일좌표 매물 N건 뱃지(2건 이상일 때만).
    if (count > 1) {
      const badge = document.createElement('span');
      badge.textContent = `${count}건`;
      badge.style.cssText =
        'font-size:11px;font-weight:700;color:#fff;background:#944a00;'
        + 'border-radius:9999px;padding:1px 7px;flex:none;';
      pill.appendChild(badge);
    }
    const tail = document.createElement('div');
    tail.style.cssText =
      `position:absolute;left:50%;bottom:-6px;transform:translateX(-50%);width:0;height:0;`
      + `border-left:5px solid transparent;border-right:5px solid transparent;border-top:8px solid ${color};`;
    pin.appendChild(pill);
    pin.appendChild(tail);
    // §7.4: 모달 등 상세 구현 금지 — 클러스터 매물 배열만 발신(상위가 우측 목록을 연다).
    pin.addEventListener('click', () => emit('marker-click', items));
    const overlay = new kakao.maps.CustomOverlay({
      position: new kakao.maps.LatLng(rep.lat, rep.lng),
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
  render();
  syncViewport(); // 초기 1회
}

watch(() => props.markers, render, { deep: true });
// SUMMARY 집계 풍선 갱신.
watch(() => mapStore.regions, render, { deep: true });
// 전략(DETAIL↔SUMMARY) 전환 시 즉시 재렌더.
watch(() => mapStore.strategy, render);
// #6: 페르소나 가중치 변경 시 배지 점수·색상 실시간 갱신.
watch(() => mapStore.persona, render, { deep: true });
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
