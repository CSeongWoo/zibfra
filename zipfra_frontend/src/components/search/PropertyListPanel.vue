<template>
  <!--
    와이어3 우측 매물 목록. store.markers 를 종합점수(페르소나 반영) 내림차순으로 카드 표시.
    종합 배지·정렬 = 페르소나 반영, 그룹 미니바 = 객관 그룹 점수. 카드 클릭 → select(id)(§7.4 단방향).
    하트(즐겨찾기)는 FAV API=Dev B 영역 → 자리만(클릭 전파만 차단, 동작은 후속).
  -->
  <aside class="list-panel glass-panel" v-if="sorted.length">
    <div class="head">
      <h3>📋 매물 목록</h3>
      <span class="count">{{ sorted.length }}건</span>
    </div>

    <ul class="rows">
      <li v-for="m in sorted" :key="m.id" class="card" @click="emit('select', m.id)">
        <div class="card-head">
          <div class="title-wrap">
            <span class="name">{{ m.buildingName || '매물 #' + m.id }}</span>
          </div>
          <span class="badge" :style="{ color: scoreColor(scoreOf(m)) }">{{ scoreOf(m) }}</span>
        </div>

        <div class="meta">
          {{ dealLabel(m.dealType) }} · {{ m.exclusiveArea ? m.exclusiveArea + '㎡' : '-' }}
          <template v-if="m.floorNo"> · {{ m.floorNo }}층</template>
          · {{ typeLabel(m.propertyType) }}
        </div>

        <div class="price-row">
          <span class="price">{{ priceLabel(m) }}</span>
          <button class="heart" title="즐겨찾기(준비중)" @click.stop>♡</button>
        </div>

        <div class="bars">
          <div class="bar" v-for="g in GROUPS" :key="g.key">
            <span class="bar-track">
              <i :style="{ width: groupScore(m[g.key + 'Base'], g.key) + '%', background: g.color }"></i>
            </span>
            <span class="bar-label">{{ g.short }}</span>
          </div>
        </div>
      </li>
    </ul>
  </aside>
</template>

<script setup>
import { computed } from 'vue';
import { useMapStore } from '@/stores/map';
import { totalScore, scoreColor, groupScore, GROUPS } from '@/utils/score';
import { dealLabel, typeLabel, priceLabel } from '@/utils/price';

const emit = defineEmits(['select']);
const mapStore = useMapStore();

const sorted = computed(() =>
  [...mapStore.markers].sort((a, b) => totalScore(b, mapStore.persona) - totalScore(a, mapStore.persona)),
);
const scoreOf = (m) => totalScore(m, mapStore.persona);
</script>

<style scoped>
.list-panel {
  position: absolute;
  top: 16px;
  right: 16px;
  bottom: 16px;
  z-index: 6;
  width: 320px;
  padding: 16px 14px;
  border-radius: var(--radius-lg, 16px);
  display: flex;
  flex-direction: column;
}

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 4px 12px;
}

.head h3 {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.count {
  font-size: 12px;
  color: var(--text-secondary);
  background: rgba(255, 255, 255, 0.06);
  padding: 2px 10px;
  border-radius: 9999px;
}

.rows {
  list-style: none;
  margin: 0;
  padding: 0 2px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.card {
  padding: 14px;
  border: 1px solid var(--border-color);
  background: rgba(255, 255, 255, 0.03);
  border-radius: var(--radius-md, 12px);
  cursor: pointer;
  transition: all 0.18s ease;
}

.card:hover {
  border-color: var(--color-info);
  background: rgba(59, 130, 246, 0.06);
  transform: translateY(-1px);
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.name {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}

.badge {
  flex-shrink: 0;
  min-width: 30px;
  text-align: center;
  padding: 3px 7px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.08);
  font-size: 13px;
  font-weight: 800;
}

.meta {
  margin-top: 6px;
  font-size: 11px;
  color: var(--text-secondary);
}

.price-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.price {
  font-size: 17px;
  font-weight: 800;
  color: var(--text-primary);
}

.heart {
  background: transparent;
  border: none;
  color: var(--text-tertiary);
  font-size: 18px;
  cursor: pointer;
  transition: color 0.15s ease;
}

.heart:hover {
  color: var(--color-danger);
}

.bars {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-top: 12px;
}

.bar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.bar-track {
  width: 100%;
  height: 4px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 9999px;
  overflow: hidden;
}

.bar-track i {
  display: block;
  height: 100%;
  border-radius: 9999px;
  transition: width 0.3s ease;
}

.bar-label {
  font-size: 10px;
  color: var(--text-tertiary);
}
</style>
