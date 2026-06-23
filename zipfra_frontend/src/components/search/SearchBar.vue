<template>
  <!--
    필터바 상단 검색창 + 검색결과 토글 드롭다운(#4, 와이어 스크린샷).
    검색을 2원화한다(§8.1 MAP-03):
      · 지역(시·구·동)  = 카카오 Geocoder.addressSearch — 주소 전용
      · 단지(아파트명)  = 우리 백엔드 GET /map/search
    항목 클릭 시 store.requestMove 로 지도 이동 명령만 발신(§7.4 단방향).
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
      <button class="search-btn" @click="runSearch">검색</button>
    </div>

    <!-- 검색결과 드롭다운 -->
    <div v-if="open" class="results">
      <div class="results-head">검색결과</div>

      <template v-if="regions.length">
        <div class="sec-label">지역</div>
        <ul class="results-list">
          <li v-for="(r, i) in regions" :key="'r' + i" class="result" @click="pickRegion(r)">
            <div class="r-name">📍 {{ r.address_name }}</div>
          </li>
        </ul>
      </template>

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
const regions = ref([]);
const properties = ref([]);
const open = ref(false);

const REGION_LEVEL = 6;
const DETAIL_LEVEL = 5;

let timer = null;

function runSearch() {
  const q = query.value.trim();
  if (q.length < 2) { close(); return; }
  open.value = true;
  searchRegions(q);
  searchBuildings(q);
}

function searchRegions(q) {
  const { kakao } = window;
  if (!kakao?.maps?.services) { regions.value = []; return; }
  const geocoder = new kakao.maps.services.Geocoder();
  geocoder.addressSearch(q, (data, status) => {
    regions.value = status === kakao.maps.services.Status.OK ? data.slice(0, 5) : [];
  });
}

async function searchBuildings(q) {
  try {
    properties.value = await searchProperties(q, 20);
  } catch (e) {
    console.error('[MAP-03] 단지 검색 실패:', e.error, e.message);
    properties.value = [];
  }
}

watch(query, () => {
  clearTimeout(timer);
  timer = setTimeout(runSearch, 250);
});
onUnmounted(() => clearTimeout(timer));

function pickRegion(r) {
  mapStore.requestMove(Number(r.y), Number(r.x), REGION_LEVEL);
  close();
}

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
  width: 100%;
  display: flex;
  align-items: center;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 0 4px;
  background: transparent;
  border: none;
}

.icon {
  font-size: 14px;
  flex-shrink: 0;
  opacity: 0.45;
}

.search-bar input {
  flex: 1;
  min-width: 0;
  border: none;
  background: transparent;
  font-size: 14px;
  color: #191c1d;
  font-family: inherit;
  outline: none;
}

.search-bar input::placeholder {
  color: #74777d;
}

/* X 지우기 버튼 */
.clear {
  flex-shrink: 0;
  border: none;
  background: transparent;
  color: #74777d;
  font-size: 13px;
  cursor: pointer;
  padding: 2px 4px;
  transition: color 0.15s ease;
}

.clear:hover {
  color: #041627;
}

/* 검색 버튼 */
.search-btn {
  flex-shrink: 0;
  padding: 6px 16px;
  background: #041627;
  color: #ffffff;
  border: none;
  border-radius: 9999px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.search-btn:hover {
  background: #1a2b3c;
  box-shadow: 0 0 8px rgba(252, 143, 52, 0.4);
}

/* 검색결과 드롭다운 */
.results {
  position: absolute;
  top: calc(100% + 20px);
  left: 0;
  right: 0;
  z-index: 20;
  max-height: 64vh;
  overflow-y: auto;
  border-radius: 12px;
  padding: 6px;
  background: #ffffff;
  border: 1px solid #c4c6cd;
  box-shadow: 0px 8px 24px rgba(26, 43, 60, 0.12);
  scrollbar-width: none;
}

.results::-webkit-scrollbar {
  display: none;
}

.results-head {
  font-size: 11px;
  font-weight: 700;
  color: #74777d;
  padding: 6px 8px 2px;
}

.sec-label {
  font-size: 10px;
  font-weight: 700;
  color: #944a00;
  letter-spacing: 0.5px;
  padding: 8px 8px 4px;
  text-transform: uppercase;
}

.results-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.result {
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.12s ease;
}

.result:hover {
  background: #fff5ec;
}

.r-name {
  font-size: 13px;
  font-weight: 600;
  color: #191c1d;
}

.r-sub {
  font-size: 11px;
  color: #74777d;
  margin-top: 2px;
}

.results-empty {
  padding: 14px 8px;
  font-size: 12px;
  color: #74777d;
  text-align: center;
}
</style>
