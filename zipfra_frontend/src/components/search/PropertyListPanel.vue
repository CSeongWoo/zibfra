<template>
  <!--
    와이어3 우측 매물 목록. store.markers 를 종합점수(페르소나 반영) 내림차순으로 카드 표시.
    반응형 토글 지원: 토글 탭으로 접기/펼치기.
    카드 클릭 → select(id)(§7.4 단방향). 하트는 FAV API 영역(후속).
  -->
  <aside class="list-panel" :class="{ collapsed: !open }" v-if="sorted.length || true">
    <!-- 토글 탭 -->
    <button class="toggle-tab" @click="open = !open" :title="open ? '목록 접기' : '목록 펼치기'">
      <span>{{ open ? '▶' : '◀' }}</span>
    </button>

    <div class="panel-inner" v-show="open">
      <div class="head">
        <h3>📋 매물 목록</h3>
        <span class="count" v-if="sorted.length">{{ sorted.length }}건</span>
        <span class="count" v-else>0건</span>
      </div>

      <div v-if="sorted.length === 0" class="empty-state">
        <span>🔍</span>
        <p>지도를 이동하거나<br>필터를 조정해 보세요</p>
      </div>

      <ul v-else class="rows">
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
    </div>
  </aside>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useMapStore } from '@/stores/map';
import { totalScore, scoreColor, groupScore, GROUPS } from '@/utils/score';
import { dealLabel, typeLabel, priceLabel } from '@/utils/price';

const emit = defineEmits(['select']);
const mapStore = useMapStore();
const open = ref(true); // 기본 펼침

const sorted = computed(() =>
  [...mapStore.markers].sort((a, b) => totalScore(b, mapStore.persona) - totalScore(a, mapStore.persona)),
);
const scoreOf = (m) => totalScore(m, mapStore.persona);
</script>

<style scoped>
/* 우측 패널 */
.list-panel {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  z-index: 8;
  width: 360px;
  background: #ffffff;
  border-left: 1px solid #c4c6cd;
  box-shadow: 0px 4px 12px rgba(26, 43, 60, 0.05);
  display: flex;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.list-panel.collapsed {
  width: 0;
  border-left: none;
}

/* 내부 컨텐츠 */
.panel-inner {
  width: 360px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  height: 100%;
}

/* 토글 탭 */
.toggle-tab {
  position: absolute;
  top: 50%;
  left: -30px;
  transform: translateY(-50%);
  z-index: 9;
  width: 30px;
  height: 56px;
  background: #ffffff;
  border: 1px solid #c4c6cd;
  border-right: none;
  border-radius: 8px 0 0 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: #44474c;
  box-shadow: -2px 0 6px rgba(26, 43, 60, 0.06);
  transition: background 0.15s ease, color 0.15s ease;
}

.toggle-tab:hover {
  background: #fff5ec;
  color: #944a00;
}

/* 헤더 */
.head {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px 12px;
  border-bottom: 1px solid #e1e3e4;
}

.head h3 {
  font-size: 15px;
  font-weight: 700;
  color: #041627;
}

.count {
  font-size: 12px;
  color: #74777d;
  background: #f3f4f5;
  padding: 2px 10px;
  border-radius: 9999px;
}

/* 빈 상태 */
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #74777d;
}

.empty-state span { font-size: 32px; opacity: 0.4; }
.empty-state p { font-size: 13px; text-align: center; line-height: 1.6; }

/* 목록 */
.rows {
  list-style: none;
  margin: 0;
  padding: 10px 14px 14px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 카드 */
.card {
  padding: 14px;
  border: 1px solid #e1e3e4;
  background: #ffffff;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.18s ease;
}

.card:hover {
  border-color: #944a00;
  background: #fff5ec;
  transform: translateY(-1px);
  box-shadow: 0px 4px 12px rgba(26, 43, 60, 0.08);
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
  color: #041627;
}

.badge {
  flex-shrink: 0;
  min-width: 30px;
  text-align: center;
  padding: 3px 7px;
  border-radius: 8px;
  background: #f3f4f5;
  font-size: 13px;
  font-weight: 800;
}

.meta {
  margin-top: 6px;
  font-size: 11px;
  color: #74777d;
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
  color: #041627;
}

.heart {
  background: transparent;
  border: none;
  color: #c4c6cd;
  font-size: 18px;
  cursor: pointer;
  transition: color 0.15s ease;
}

.heart:hover { color: #ba1a1a; }

/* 인프라 미니바 */
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
  background: #e1e3e4;
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
  color: #74777d;
}
</style>
