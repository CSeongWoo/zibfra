<template>
  <!--
    와이어3 좌측 통합 사이드바: 검색 필터(§8.1.1) + 인프라 표시 토글 + 페르소나 가중치(#6).
    필터 → store.filter(MapContainer 가 MAP-01 재조회), 페르소나 → store.persona(점수 실시간 갱신),
    인프라 토글 → store.infraLayers(오버레이 표시, 실제 렌더는 PR C). 모두 단방향(§7.4).
  -->
  <aside class="sidebar glass-panel">
    <!-- ===== 필터 ===== -->
    <h3 class="sec-title">🔎 필터</h3>

    <div class="group">
      <span class="label">거래 유형</span>
      <div class="chips">
        <button
          v-for="o in DEAL_TYPES" :key="o.value"
          class="chip" :class="{ active: filter.dealType === o.value }"
          @click="toggle('dealType', o.value)"
        >{{ o.label }}</button>
      </div>
    </div>

    <div class="group">
      <span class="label">매물 유형</span>
      <div class="chips">
        <button
          v-for="o in PROPERTY_TYPES" :key="o.value"
          class="chip" :class="{ active: filter.propertyType === o.value }"
          @click="toggle('propertyType', o.value)"
        >{{ o.label }}</button>
      </div>
    </div>

    <div class="group">
      <span class="label">
        가격 범위 (억)
        <em v-if="filter.dealType === 'JEONSE' || filter.dealType === 'WOLSE'">· 보증금</em>
        <em v-else-if="!filter.dealType">· 매매가/보증금</em>
      </span>
      <div class="dual">
        <div class="dual-track"></div>
        <div class="dual-fill" :style="fillStyle"></div>
        <input type="range" :min="0" :max="MAX" :step="STEP" :value="minVal" @input="onMinSlider" />
        <input type="range" :min="0" :max="MAX" :step="STEP" :value="maxVal" @input="onMaxSlider" />
      </div>
      <div class="dual-vals">
        <span>{{ formatPrice(minVal) }}</span>
        <span>{{ maxVal >= MAX ? formatPrice(MAX) + '+' : formatPrice(maxVal) }}</span>
      </div>
    </div>

    <div class="group">
      <span class="label">인프라 표시</span>
      <div class="infra-toggles">
        <button
          v-for="g in GROUPS" :key="g.key"
          class="infra" :class="{ off: !infraLayers[g.key] }"
          @click="mapStore.toggleInfra(g.key)"
        >
          <span class="dot" :style="{ background: infraLayers[g.key] ? g.color : 'transparent', borderColor: g.color }"></span>
          {{ g.icon }} {{ g.long }}
        </button>
      </div>
    </div>

    <!-- ===== 페르소나 ===== -->
    <div class="persona-card">
      <h3 class="sec-title">🎚 페르소나 가중치</h3>
      <p class="desc">슬라이더 조정 시 지도 마커와 목록 점수가 실시간 업데이트됩니다.</p>

      <div class="slider" v-for="g in GROUPS" :key="g.key">
        <div class="slider-top">
          <span>{{ g.icon }} {{ g.long }}</span>
          <em :style="{ color: g.color }">{{ persona[g.key] }}</em>
        </div>
        <input
          type="range" min="0" max="100" step="10"
          :value="persona[g.key]"
          :style="{ accentColor: g.color }"
          @input="mapStore.setPersona({ [g.key]: Number($event.target.value) })"
        />
      </div>

      <div class="presets">
        <button
          v-for="p in PERSONA_PRESETS" :key="p.key"
          class="preset" :class="{ active: activeKey === p.key }"
          @click="mapStore.setPersona({ ...p.weights })"
        >{{ p.label }}</button>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue';
import { useMapStore } from '@/stores/map';
import { PERSONA_PRESETS, GROUPS } from '@/utils/score';

const mapStore = useMapStore();
const filter = computed(() => mapStore.filter);
const persona = computed(() => mapStore.persona);
const infraLayers = computed(() => mapStore.infraLayers);

const DEAL_TYPES = [
  { label: '매매', value: 'SALE' },
  { label: '전세', value: 'JEONSE' },
  { label: '월세', value: 'WOLSE' },
];
const PROPERTY_TYPES = [
  { label: '아파트', value: 'APT' },
  { label: '오피스텔', value: 'OFFICETEL' },
  { label: '빌라', value: 'ROW_HOUSE' }, // 원룸 제외(3종 확정, §8.1.1)
];

// 같은 칩 재클릭 → 해제(전체). '전체' 칩 없이 단일 토글(와이어3).
function toggle(key, value) {
  mapStore.setFilter({ [key]: filter.value[key] === value ? null : value });
}

// 가격: 슬라이더(만원 단위)와 입력칸(억 단위)이 같은 filter.priceMin/Max(만원)를 공유.
const MAX = 500000; // 50억(만원)
const STEP = 5000;  // 5천만(만원)
const minVal = computed(() => filter.value.priceMin ?? 0);
const maxVal = computed(() => filter.value.priceMax ?? MAX);
const fillStyle = computed(() => ({
  left: `${(minVal.value / MAX) * 100}%`,
  right: `${100 - (maxVal.value / MAX) * 100}%`,
}));

function onMinSlider(e) {
  const v = Math.min(Number(e.target.value), maxVal.value - STEP);
  mapStore.setFilter({ priceMin: v <= 0 ? null : v });
}
function onMaxSlider(e) {
  const v = Math.max(Number(e.target.value), minVal.value + STEP);
  mapStore.setFilter({ priceMax: v >= MAX ? null : v });
}

// 슬라이더 현재 값 표시(억). 0 은 하한 없음.
function formatPrice(manwon) {
  return manwon <= 0 ? '0' : `${manwon / 10000}억`;
}

const activeKey = computed(() => {
  const p = persona.value;
  const hit = PERSONA_PRESETS.find((preset) => GROUPS.every((g) => preset.weights[g.key] === p[g.key]));
  return hit ? hit.key : null;
});
</script>

<style scoped>
.sidebar {
  position: absolute;
  top: 16px;
  left: 16px;
  bottom: 16px;
  z-index: 6;
  width: 256px;
  padding: 16px 18px;
  border-radius: var(--radius-lg, 16px);
  font-size: 13px;
  overflow-y: auto;
}

.sec-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 14px;
}

.group {
  margin-bottom: 18px;
}

.label {
  display: block;
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.label em {
  font-style: normal;
  color: var(--text-tertiary);
  font-size: 11px;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.chip {
  padding: 6px 13px;
  border: 1px solid var(--border-color);
  background: rgba(255, 255, 255, 0.04);
  color: var(--text-secondary);
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.chip:hover {
  border-color: var(--color-info);
  color: var(--text-primary);
}

.chip.active {
  background: var(--color-info);
  border-color: var(--color-info);
  color: #fff;
}

/* 가격 듀얼 슬라이더 */
.dual {
  position: relative;
  height: 24px;
  margin-bottom: 8px;
}

.dual-track,
.dual-fill {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  height: 4px;
  border-radius: 9999px;
}

.dual-track {
  left: 0;
  right: 0;
  background: rgba(255, 255, 255, 0.1);
}

.dual-fill {
  background: var(--color-info);
}

.dual input[type='range'] {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 24px;
  margin: 0;
  background: transparent;
  pointer-events: none;
  -webkit-appearance: none;
  appearance: none;
}

.dual input[type='range']::-webkit-slider-thumb {
  pointer-events: auto;
  -webkit-appearance: none;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #fff;
  border: 2px solid var(--color-info);
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.4);
}

.dual input[type='range']::-moz-range-thumb {
  pointer-events: auto;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #fff;
  border: 2px solid var(--color-info);
  cursor: pointer;
}

/* 가격 현재 범위 표시 */
.dual-vals {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: var(--text-secondary);
}

/* 인프라 토글 */
.infra-toggles {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.infra {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 4px;
  background: transparent;
  border: none;
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.15s ease;
}

.infra.off {
  opacity: 0.4;
}

.infra .dot {
  width: 14px;
  height: 14px;
  border-radius: 4px;
  border: 2px solid;
  flex-shrink: 0;
}

/* 페르소나 카드 */
.persona-card {
  margin-top: 8px;
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md, 12px);
  background: rgba(255, 255, 255, 0.03);
}

.desc {
  font-size: 11px;
  color: var(--text-tertiary);
  line-height: 1.5;
  margin-bottom: 14px;
}

.slider {
  margin-bottom: 12px;
}

.slider-top {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.slider-top em {
  font-style: normal;
  font-weight: 700;
}

.slider input[type='range'] {
  width: 100%;
  cursor: pointer;
}

.presets {
  display: flex;
  gap: 6px;
  margin-top: 14px;
}

.preset {
  flex: 1;
  padding: 7px 4px;
  border: 1px solid var(--border-color);
  background: rgba(255, 255, 255, 0.04);
  color: var(--text-secondary);
  border-radius: var(--radius-sm, 8px);
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.preset:hover {
  border-color: var(--color-info);
  color: var(--text-primary);
}

.preset.active {
  background: var(--color-info);
  border-color: var(--color-info);
  color: #fff;
}
</style>
