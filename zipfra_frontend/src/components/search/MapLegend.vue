<template>
  <!-- 점수 색상 범례(와이어3 좌하단). score.js scoreColor 임계와 일치. -->
  <div class="legend glass-panel">
    <span class="title">인프라 점수</span>
    <span class="item" v-for="t in TIERS" :key="t.label">
      <i :style="{ background: t.color }"></i>{{ t.label }}
    </span>
  </div>
</template>

<script setup>
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
  left: 288px; /* 좌측 사이드바(256+여백) 우측 */
  bottom: 24px;
  z-index: 5;
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding: 12px 14px;
  border-radius: var(--radius-md, 12px);
  font-size: 11px;
  color: var(--text-secondary);
}

.title {
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 2px;
}

.item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.item i {
  width: 10px;
  height: 10px;
  border-radius: 9999px;
  display: inline-block;
}
</style>
