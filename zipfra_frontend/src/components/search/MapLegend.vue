<template>
  <!-- 점수 색상 범례(좌하단). 필터 사이드바 접고 펼 때 사이드바 폭만큼 좌우 이동. score.js scoreColor 임계와 일치. -->
  <div class="legend" :class="{ 'sidebar-collapsed': !mapStore.sidebarOpen }">
    <span class="title">인프라 점수</span>
    <span class="item" v-for="t in TIERS" :key="t.label">
      <i :style="{ background: t.color }"></i>{{ t.label }}
    </span>
  </div>
</template>

<script setup>
import { useMapStore } from '@/stores/map';

const mapStore = useMapStore();
const TIERS = [
  { color: '#10b981', label: '90+' },
  { color: '#3b82f6', label: '75–89' },
  { color: '#f59e0b', label: '60–74' },
  { color: '#ef4444', label: '< 60' },
];
</script>

<style scoped>
.legend {
  position: absolute;
  left: 336px; /* 사이드바(320px) 펼침 시: 그 우측 */
  bottom: 24px;
  transition: left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 5;
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding: 12px 14px;
  border-radius: 12px;
  font-size: 11px;
  background: #ffffff;
  border: 1px solid #c4c6cd;
  box-shadow: 0px 4px 12px rgba(26, 43, 60, 0.08);
}

/* 사이드바 접힘 시: 화면 좌측 끝으로(토글 탭 30px 비켜) */
.legend.sidebar-collapsed {
  left: 40px;
}

.title {
  font-weight: 700;
  color: #041627;
  margin-bottom: 2px;
  font-size: 12px;
}

.item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #44474c;
}

.item i {
  width: 10px;
  height: 10px;
  border-radius: 9999px;
  display: inline-block;
  flex-shrink: 0;
}
</style>
