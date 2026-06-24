<template>
  <!-- 줌 +/-(우하단). store.requestZoom → KakaoMap 이 setLevel(§7.4 단방향).
       목록(360px) 열리면 그 좌측으로 이동(기능4). -->
  <div class="zoom" :class="{ shifted: mapStore.listOpen }">
    <button @click="mapStore.requestZoom(1)" title="확대">＋</button>
    <span class="divider"></span>
    <button @click="mapStore.requestZoom(-1)" title="축소">－</button>
  </div>
</template>

<script setup>
import { useMapStore } from '@/stores/map';

const mapStore = useMapStore();
</script>

<style scoped>
.zoom {
  position: absolute;
  bottom: 24px;
  right: 24px; /* 평소: 화면 우측 끝 */
  transition: right 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 5;
  display: flex;
  flex-direction: column;
  align-items: center;
  border-radius: 12px;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid #c4c6cd;
  box-shadow: 0px 4px 12px rgba(26, 43, 60, 0.08);
}

/* 매물 목록(360px) 열리면 그 좌측으로 */
.zoom.shifted {
  right: 376px;
}

.zoom button {
  width: 40px;
  height: 40px;
  background: transparent;
  border: none;
  color: #041627;
  font-size: 18px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s ease;
}

.zoom button:hover {
  background: #f3f4f5;
  color: #944a00;
}

.divider {
  width: 60%;
  height: 1px;
  background: #c4c6cd;
}
</style>
