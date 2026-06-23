<template>
  <!-- 현재 위치(와이어3 하단 중앙). geolocation → store.requestMove(§7.4 단방향). -->
  <button class="locate" @click="locate">
    <span class="ico">📍</span> 현재 위치
  </button>
</template>

<script setup>
import { useMapStore } from '@/stores/map';

const mapStore = useMapStore();
const DETAIL_LEVEL = 5;

function locate() {
  if (!navigator.geolocation) {
    alert('이 브라우저는 위치 정보를 지원하지 않습니다.');
    return;
  }
  navigator.geolocation.getCurrentPosition(
    (pos) => mapStore.requestMove(pos.coords.latitude, pos.coords.longitude, DETAIL_LEVEL),
    () => alert('현재 위치를 가져올 수 없습니다.'),
  );
}
</script>

<style scoped>
.locate {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 5;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 9px 18px;
  border-radius: 9999px;
  font-size: 13px;
  font-weight: 600;
  color: #041627;
  background: #ffffff;
  border: 1px solid #c4c6cd;
  box-shadow: 0px 4px 12px rgba(26, 43, 60, 0.08);
  cursor: pointer;
  transition: all 0.15s ease;
}

.locate:hover {
  border-color: #944a00;
  color: #944a00;
  background: #fff5ec;
  box-shadow: 0 0 8px rgba(252, 143, 52, 0.4);
}

.ico {
  font-size: 13px;
}
</style>
