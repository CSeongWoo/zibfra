<template>
  <!-- 줌 +/-(와이어3 우하단). store.requestZoom → KakaoMap 이 setLevel(§7.4 단방향). -->
  <div class="zoom glass-panel">
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
  right: 352px; /* 우측 매물 목록(320+여백) 좌측 */
  z-index: 5;
  display: flex;
  flex-direction: column;
  align-items: center;
  border-radius: var(--radius-md, 12px);
  overflow: hidden;
}

.zoom button {
  width: 40px;
  height: 40px;
  background: transparent;
  border: none;
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s ease;
}

.zoom button:hover {
  background: rgba(255, 255, 255, 0.08);
}

.divider {
  width: 60%;
  height: 1px;
  background: var(--border-color);
}
</style>
