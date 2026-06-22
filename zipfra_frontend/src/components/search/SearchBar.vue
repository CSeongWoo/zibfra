<template>
  <!--
    필터바 상단 검색창 + 검색결과 토글 드롭다운(#4, 와이어 스크린샷).
    검색을 2원화한다(§8.1 MAP-03):
      · 지역(시·구·동)  = 카카오 Geocoder.addressSearch — 주소 전용(편의점·다이소 등 POI 배제)
      · 단지(아파트명)  = 우리 백엔드 GET /map/search — real_estate_sales 단지명 검색
    항목 클릭 시 store.requestMove 로 지도 이동 명령만 발신(§7.4 단방향). 지도 인스턴스는 안 만짐.
  -->
  <div class="search-wrap">
    <div class="search-bar">
      <span class="icon">🔍</span>
      <input
        v-model="query"
        type="text"
        placeholder="지역·단지명 검색…"
        @keyup.enter="runSearch"
        @keyup.esc="close"
      />
      <button v-if="query" class="clear" title="지우기" @click="clear">✕</button>
    </div>

    <!-- 검색결과 토글창 -->
    <div v-if="open" class="results glass-panel">
      <div class="results-head">검색결과</div>

      <!-- 지역 섹션 -->
      <template v-if="regions.length">
        <div class="sec-label">지역</div>
        <ul class="results-list">
          <li v-for="(r, i) in regions" :key="'r' + i" class="result" @click="pickRegion(r)">
            <div class="r-name">📍 {{ r.address_name }}</div>
          </li>
        </ul>
      </template>

      <!-- 단지 섹션 -->
      <template v-if="properties.length">
        <div class="sec-label">단지</div>
        <ul class="results-list">
          <li v-for="p in properties" :key="'p' + p.id" class="result" @click="pickProperty(p)">
            <div class="r-name">🏢 {{ p.buildingName }}</div>
            <div class="r-sub">{{ subtitle(p) }}</div>
          </li>
        </ul>
      </template>

      <div v-if="!regions.length && !properties.length" class="results-empty">
        검색 결과가 없습니다.
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue';
import { useMapStore } from '@/stores/map';
import { searchProperties } from '@/api/search';
import { typeLabel } from '@/utils/price';

const mapStore = useMapStore();
const query = ref('');
const regions = ref([]);     // 카카오 addressSearch 결과(지역)
const properties = ref([]);  // 백엔드 단지 검색 결과
const open = ref(false);

const REGION_LEVEL = 6; // 지역은 약간 넓게(≈SUMMARY 경계)
const DETAIL_LEVEL = 5; // 단지는 DETAIL(개별 마커)

let timer = null;

function runSearch() {
  const q = query.value.trim();
  if (q.length < 2) { close(); return; }
  open.value = true;
  searchRegions(q);
  searchBuildings(q);
}

// 지역: 카카오 주소 검색(POI 제외). 상위 5건만.
function searchRegions(q) {
  const { kakao } = window;
  if (!kakao?.maps?.services) { regions.value = []; return; }
  const geocoder = new kakao.maps.services.Geocoder();
  geocoder.addressSearch(q, (data, status) => {
    regions.value = status === kakao.maps.services.Status.OK ? data.slice(0, 5) : [];
  });
}

// 단지: 우리 백엔드(real_estate_sales) 검색.
async function searchBuildings(q) {
  try {
    properties.value = await searchProperties(q, 20);
  } catch (e) {
    console.error('[MAP-03] 단지 검색 실패:', e.error, e.message);
    properties.value = [];
  }
}

// 입력 디바운스(250ms) — 타이핑하며 자동 검색.
watch(query, () => {
  clearTimeout(timer);
  timer = setTimeout(runSearch, 250);
});
onUnmounted(() => clearTimeout(timer));

// 지역 선택 → 해당 좌표로 이동(x=lng, y=lat 카카오 규약).
function pickRegion(r) {
  mapStore.requestMove(Number(r.y), Number(r.x), REGION_LEVEL);
  close();
}

// 단지 선택 → 단지 대표 좌표로 DETAIL 이동.
function pickProperty(p) {
  mapStore.requestMove(p.lat, p.lng, DETAIL_LEVEL);
  close();
}

function clear() {
  query.value = '';
  close();
}

function close() {
  open.value = false;
  regions.value = [];
  properties.value = [];
}

// "아파트 · 42건 · 59~168㎡ · 2009년"
function subtitle(p) {
  const parts = [typeLabel(p.propertyType), `${p.dealCount}건`];
  if (p.minArea != null && p.maxArea != null) {
    parts.push(`${Math.round(p.minArea)}~${Math.round(p.maxArea)}㎡`);
  }
  if (p.buildYear) parts.push(`${p.buildYear}년`);
  return parts.join(' · ');
}
</script>

<style scoped>
.search-wrap {
  position: relative;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 14px;
  border-radius: 9999px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid var(--border-color);
}

.search-bar:focus-within {
  border-color: var(--color-info);
}

.icon {
  font-size: 13px;
  flex-shrink: 0;
  opacity: 0.6;
}

.search-bar input {
  flex: 1;
  min-width: 0;
  border: none;
  background: transparent;
  font-size: 13px;
  color: var(--text-primary);
}

.search-bar input::placeholder {
  color: var(--text-tertiary);
}

.search-bar input:focus {
  outline: none;
}

.clear {
  flex-shrink: 0;
  border: none;
  background: transparent;
  color: var(--text-tertiary);
  font-size: 12px;
  cursor: pointer;
  padding: 2px;
}

.clear:hover {
  color: var(--text-primary);
}

/* 검색결과 토글 드롭다운 — 검색창 아래로 떠서 필터 위를 덮음 */
.results {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  right: 0;
  z-index: 20;
  max-height: 64vh;
  overflow-y: auto;
  border-radius: var(--radius-md, 12px);
  padding: 6px;
  scrollbar-width: none;
}

.results::-webkit-scrollbar {
  display: none;
}

.results-head {
  font-size: 11px;
  font-weight: 700;
  color: var(--text-secondary);
  padding: 6px 8px 2px;
}

.sec-label {
  font-size: 10px;
  font-weight: 700;
  color: var(--color-info);
  letter-spacing: 0.4px;
  padding: 8px 8px 4px;
}

.results-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.result {
  padding: 8px;
  border-radius: var(--radius-sm, 8px);
  cursor: pointer;
  transition: background 0.12s ease;
}

.result:hover {
  background: rgba(59, 130, 246, 0.12);
}

.r-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.r-sub {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 2px;
}

.results-empty {
  padding: 14px 8px;
  font-size: 12px;
  color: var(--text-tertiary);
  text-align: center;
}
</style>
