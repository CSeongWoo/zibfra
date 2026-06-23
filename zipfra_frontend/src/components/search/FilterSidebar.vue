<template>
  <!--
    좌측 도킹 통합 사이드바: 검색(#4) + 검색 필터(§8.1) + 인프라 토글 + 페르소나 가중치(#6).
    구조: 상단 검색(고정) + 스크롤 본문(스크롤바 숨김 #2). 모두 단방향(§7.4).
    필터 → store.filter(MapContainer 가 MAP-01 재조회), 페르소나 → store.persona(점수 실시간 갱신).
  -->
  <aside class="sidebar glass-panel">
    <!-- 검색창(필터바 상단 고정) + 검색결과 토글 드롭다운 -->
    <div class="sidebar-search">
      <SearchBar />
    </div>

    <div class="sidebar-body">
      <!-- ===== 필터 ===== -->
      <h3 class="sec-title">🔎 필터</h3>

      <div class="group">
        <span class="label">거래 유형 <em>· 중복 선택</em></span>
        <div class="chips">
          <button
            v-for="o in DEAL_TYPES" :key="o.value"
            class="chip" :class="{ active: filter.dealTypes.includes(o.value) }"
            @click="mapStore.toggleFilterValue('dealTypes', o.value)"
          >{{ o.label }}</button>
        </div>
      </div>

      <div class="group">
        <span class="label">매물 유형 <em>· 중복 선택</em></span>
        <div class="chips">
          <button
            v-for="o in PROPERTY_TYPES" :key="o.value"
            class="chip" :class="{ active: filter.propertyTypes.includes(o.value) }"
            @click="mapStore.toggleFilterValue('propertyTypes', o.value)"
          >{{ o.label }}</button>
        </div>
      </div>

      <div class="group">
        <span class="label">
          가격 범위 (억)
          <em v-if="depositOnly">· 보증금</em>
          <em v-else>· 매매가/보증금</em>
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
        <p class="desc">
          <template v-if="lockedPreset">프리셋 적용 중 — 같은 버튼을 다시 누르면 직접 조절할 수 있습니다.</template>
          <template v-else>슬라이더 조정 시 지도 마커와 목록 점수가 실시간 업데이트됩니다.</template>
        </p>

        <div class="slider" :class="{ locked: lockedPreset }" v-for="g in GROUPS" :key="g.key">
          <div class="slider-top">
            <span>{{ g.icon }} {{ g.long }}</span>
            <em :style="{ color: g.color }">{{ persona[g.key] }}</em>
          </div>
          <input
            type="range" min="0" max="100" step="10"
            :value="persona[g.key]"
            :disabled="lockedPreset !== null"
            :style="{ accentColor: g.color }"
            @input="mapStore.setPersona({ [g.key]: Number($event.target.value) })"
          />
        </div>

        <div class="presets">
          <button
            v-for="p in PERSONA_PRESETS" :key="p.key"
            class="preset" :class="{ active: lockedPreset === p.key }"
            @click="onPreset(p)"
          >{{ p.label }}</button>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useMapStore } from '@/stores/map';
import { PERSONA_PRESETS, GROUPS } from '@/utils/score';
import SearchBar from '@/components/search/SearchBar.vue';

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
  { label: '빌라', value: 'ROW_HOUSE' }, // 원룸 제외(3종 확정, §8.1)
];

// 선택한 거래유형이 전부 전·월세면 가격 라벨을 '보증금'으로(매매가 없을 때).
const depositOnly = computed(() => {
  const d = filter.value.dealTypes;
  return d.length > 0 && d.every((v) => v === 'JEONSE' || v === 'WOLSE');
});

// 가격: 슬라이더(만원 단위)와 표시(억 단위)가 같은 filter.priceMin/Max(만원)를 공유.
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

function formatPrice(manwon) {
  return manwon <= 0 ? '0' : `${manwon / 10000}억`;
}

// #5 페르소나 프리셋 잠금: 프리셋 적용 시 슬라이더 비활성, 같은 프리셋 재클릭 시 해제.
const lockedPreset = ref(null);
function onPreset(p) {
  if (lockedPreset.value === p.key) {
    lockedPreset.value = null; // 재클릭 → 잠금 해제(슬라이더 다시 활성, 가중치는 유지)
  } else {
    mapStore.setPersona({ ...p.weights }); // 적용 + 잠금
    lockedPreset.value = p.key;
  }
}
</script>

<style scoped>
/* #1 좌측 가장자리 도킹: 떠있던 16px 여백·라운딩 제거, 헤더 아래 full-height */
.sidebar {
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 6;
  width: 280px;
  border-radius: 0;
  border-top: none;
  border-left: none;
  border-bottom: none;
  font-size: 13px;
  display: flex;
  flex-direction: column;
}

.sidebar-search {
  flex: none;
  padding: 14px 16px 10px;
}

/* #2 스크롤바 숨김(스크롤 기능은 유지) */
.sidebar-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 4px 18px 18px;
  scrollbar-width: none;
}

.sidebar-body::-webkit-scrollbar {
  display: none;
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

/* #5 프리셋 잠금 시 슬라이더 흐리게(비활성) */
.slider.locked {
  opacity: 0.45;
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

.slider input[type='range']:disabled {
  cursor: not-allowed;
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
