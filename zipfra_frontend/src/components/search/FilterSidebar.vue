<template>
  <!--
    좌측 도킹 통합 사이드바: 검색(#4) + 검색 필터(§8.1) + 인프라 토글 + 페르소나 가중치(#6).
    반응형 토글 지원: 토글 탭으로 접기/펼치기.
    필터 → store.filter(MapContainer 가 MAP-01 재조회), 페르소나 → store.persona(점수 실시간 갱신).
  -->
  <aside class="sidebar" :class="{ collapsed: !open }">
    <!-- 펼침/접기 탭 -->
    <button class="toggle-tab" @click="open = !open" :title="open ? '사이드바 접기' : '사이드바 펼치기'">
      <span>{{ open ? '◀' : '▶' }}</span>
    </button>

    <div class="sidebar-inner" v-show="open">

      <!-- 로고 + 인증 헤더 -->
      <div class="sidebar-header">
        <router-link to="/" class="logo-link">
          <span class="logo-icon">🏠</span>
          <span class="logo-text">Zibfra</span>
        </router-link>

        <div class="auth-area">
          <template v-if="authStore.isAuthenticated">
            <span class="greeting">{{ authStore.currentUser?.nickname }}님</span>
            <button class="auth-btn secondary" @click="handleLogout" :disabled="authStore.loading">로그아웃</button>
          </template>
          <template v-else>
            <button class="auth-btn primary" @click="goToLogin">로그인</button>
          </template>
        </div>
      </div>

      <div class="sidebar-body">

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
    </div>
  </aside>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useMapStore } from '@/stores/map';
import { useAuthStore } from '@/stores/auth';
import { PERSONA_PRESETS, GROUPS } from '@/utils/score';

const mapStore = useMapStore();
const authStore = useAuthStore();
const router = useRouter();
const filter = computed(() => mapStore.filter);
const persona = computed(() => mapStore.persona);
const infraLayers = computed(() => mapStore.infraLayers);

const open = ref(true);

function goToLogin() {
  router.push('/login');
}

async function handleLogout() {
  try {
    await authStore.logout();
  } catch (e) {
    console.error('Logout failed:', e);
  }
}

const DEAL_TYPES = [
  { label: '매매', value: 'SALE' },
  { label: '전세', value: 'JEONSE' },
  { label: '월세', value: 'WOLSE' },
];
const PROPERTY_TYPES = [
  { label: '아파트', value: 'APT' },
  { label: '오피스텔', value: 'OFFICETEL' },
  { label: '빌라', value: 'ROW_HOUSE' },
];

const depositOnly = computed(() => {
  const d = filter.value.dealTypes;
  return d.length > 0 && d.every((v) => v === 'JEONSE' || v === 'WOLSE');
});

const MAX = 500000;
const STEP = 5000;
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

const lockedPreset = ref(null);
function onPreset(p) {
  if (lockedPreset.value === p.key) {
    lockedPreset.value = null;
  } else {
    mapStore.setPersona({ ...p.weights });
    lockedPreset.value = p.key;
  }
}
</script>

<style scoped>
/* ── 팔레트 (DESIGN.md) ─────────────────────────────────
   Primary  : #041627   Secondary : #944a00  accent: #fc8f34
   Surface  : #f8f9fa   On-surface: #191c1d
   Variant  : #44474c   Outline   : #74777d  #c4c6cd
────────────────────────────────────────────────────── */

/* 사이드바 전체 */
.sidebar {
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 8;
  width: 320px;
  background: #ffffff;
  border-right: 1px solid #c4c6cd;
  box-shadow: 0px 4px 12px rgba(26, 43, 60, 0.05);
  display: flex;
  font-size: 13px;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.sidebar.collapsed {
  width: 0;
  border-right: none;
}

/* 내부 컨텐츠 (펼침 상태에서만) */
.sidebar-inner {
  width: 320px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  height: 100%;
}

/* 로고 + 인증 헤더 */
.sidebar-header {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 12px;
  border-bottom: 1px solid #c4c6cd;
  background: #ffffff;
}

.logo-link {
  display: flex;
  align-items: center;
  gap: 6px;
  text-decoration: none;
  cursor: pointer;
}

.logo-icon { font-size: 18px; }

.logo-text {
  font-size: 17px;
  font-weight: 800;
  color: #041627;
  letter-spacing: -0.5px;
}

.auth-area {
  display: flex;
  align-items: center;
  gap: 6px;
}

.greeting {
  font-size: 12px;
  font-weight: 600;
  color: #44474c;
  white-space: nowrap;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.auth-btn {
  padding: 5px 12px;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  border: none;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.auth-btn.primary {
  background: #041627;
  color: #ffffff;
}

.auth-btn.primary:hover {
  background: #1a2b3c;
  box-shadow: 0 0 8px rgba(252, 143, 52, 0.4);
}

.auth-btn.secondary {
  background: transparent;
  color: #44474c;
  border: 1px solid #c4c6cd;
}

.auth-btn.secondary:hover {
  border-color: #944a00;
  color: #944a00;
  background: #fff5ec;
}

.auth-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 토글 탭 (항상 보임) */
.toggle-tab {
  position: absolute;
  top: 50%;
  right: -30px;
  transform: translateY(-50%);
  z-index: 9;
  width: 30px;
  height: 56px;
  background: #ffffff;
  border: 1px solid #c4c6cd;
  border-left: none;
  border-radius: 0 8px 8px 0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: #44474c;
  box-shadow: 2px 0 6px rgba(26, 43, 60, 0.06);
  transition: background 0.15s ease, color 0.15s ease;
}

.toggle-tab:hover {
  background: #fff5ec;
  color: #944a00;
}

/* 검색창 상단 고정 */
.sidebar-search {
  flex: none;
  padding: 14px 16px 10px;
  border-bottom: 1px solid #c4c6cd;
}

/* 스크롤 본문 */
.sidebar-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 18px 18px;
  scrollbar-width: none;
}

.sidebar-body::-webkit-scrollbar {
  display: none;
}

.sec-title {
  font-size: 13px;
  font-weight: 700;
  color: #041627;
  margin-bottom: 12px;
  margin-top: 8px;
}

.group {
  margin-bottom: 18px;
}

.label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: #44474c;
  margin-bottom: 8px;
  letter-spacing: 0.04em;
}

.label em {
  font-style: normal;
  color: #74777d;
  font-size: 11px;
  font-weight: 400;
}

/* 칩 */
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.chip {
  padding: 5px 12px;
  border: 1px solid #c4c6cd;
  background: #ffffff;
  color: #44474c;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.chip:hover {
  border-color: #944a00;
  color: #041627;
  background: #fff5ec;
}

.chip.active {
  background: #041627;
  border-color: #041627;
  color: #ffffff;
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

.dual-track { left: 0; right: 0; background: #e1e3e4; }
.dual-fill { background: #fc8f34; }

.dual input[type='range'] {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 24px;
  margin: 0; background: transparent;
  pointer-events: none;
  -webkit-appearance: none; appearance: none;
}

.dual input[type='range']::-webkit-slider-thumb {
  pointer-events: auto;
  -webkit-appearance: none;
  width: 14px; height: 14px;
  border-radius: 50%;
  background: #ffffff; border: 2px solid #944a00;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(26, 43, 60, 0.2);
}

.dual input[type='range']::-moz-range-thumb {
  pointer-events: auto;
  width: 14px; height: 14px;
  border-radius: 50%;
  background: #ffffff; border: 2px solid #944a00;
  cursor: pointer;
}

.dual-vals {
  display: flex; justify-content: space-between;
  font-size: 11px; color: #74777d;
}

/* 인프라 토글 */
.infra-toggles { display: flex; flex-direction: column; gap: 4px; }

.infra {
  display: flex; align-items: center; gap: 8px;
  padding: 5px 4px;
  background: transparent; border: none;
  color: #191c1d; font-size: 12px; font-weight: 500;
  cursor: pointer;
  transition: opacity 0.15s ease;
  text-align: left;
}

.infra.off { opacity: 0.35; }

.infra .dot {
  width: 14px; height: 14px;
  border-radius: 4px; border: 2px solid;
  flex-shrink: 0;
}

/* 페르소나 카드 */
.persona-card {
  margin-top: 4px; padding: 14px;
  border: 1px solid #c4c6cd;
  border-radius: 12px; background: #f8f9fa;
}

.desc {
  font-size: 11px; color: #74777d;
  line-height: 1.5; margin-bottom: 14px; margin-top: -4px;
}

.slider { margin-bottom: 12px; }
.slider.locked { opacity: 0.45; }

.slider-top {
  display: flex; justify-content: space-between;
  font-size: 12px; color: #44474c; margin-bottom: 4px;
}

.slider-top em { font-style: normal; font-weight: 700; }

.slider input[type='range'] {
  width: 100%; cursor: pointer; accent-color: #fc8f34;
}

.slider input[type='range']:disabled { cursor: not-allowed; }

/* 프리셋 */
.presets { display: flex; gap: 6px; margin-top: 14px; }

.preset {
  flex: 1; padding: 7px 4px;
  border: 1px solid #c4c6cd; background: #ffffff;
  color: #44474c; border-radius: 8px;
  font-size: 11px; font-weight: 600;
  cursor: pointer; transition: all 0.15s ease;
}

.preset:hover { border-color: #944a00; color: #041627; background: #fff5ec; }

.preset.active { background: #041627; border-color: #041627; color: #ffffff; }
</style>
