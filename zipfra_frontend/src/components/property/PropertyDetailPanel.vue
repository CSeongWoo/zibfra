<template>
  <!--
    매물 상세페이지(#17, 와이어 스크린샷). 풀스크린 오버레이.
    데이터: 매물정보·점수는 props.property(마커 객체)에서, 동 이름은 카카오 coord2Address,
    이미지=카카오 거리뷰(Roadview), 미니맵=카카오 맵, 리뷰=REV-01. 실거래가 추이는 placeholder(데이터 단일월).
    §7.4 단방향: 닫기만 emit, 지도 인스턴스는 안 만짐(여기 자체 kakao 인스턴스만 사용).
  -->
  <section class="detail animate-fade-in">
    <header class="detail-top">
      <button class="back" @click="emit('close')">← 목록으로 돌아가기</button>
    </header>

    <div class="grid">
      <!-- ===== 좌측 ===== -->
      <div class="col-left">
        <!-- 거리뷰(이미지 갤러리 대체) -->
        <div class="card gallery">
          <div ref="roadviewEl" class="roadview"></div>
          <div v-if="!roadviewOk" class="roadview-fallback">
            <span>🏢</span><p>이 위치의 거리뷰가 없습니다</p>
          </div>
        </div>

        <!-- 매물 정보 -->
        <div class="card info">
          <div class="info-head">
            <div>
              <h2 class="bname">{{ p.buildingName || '매물 #' + p.id }}</h2>
              <p class="addr">📍 {{ dong || '—' }}</p>
            </div>
            <div class="price-wrap">
              <span class="deal-tag">{{ dealLabel(p.dealType) }}</span>
              <span class="price">{{ priceLabel(p) }}</span>
            </div>
          </div>
          <div class="info-grid">
            <div><span class="k">면적</span><span class="v">{{ p.exclusiveArea ? p.exclusiveArea + '㎡' : '-' }}</span></div>
            <div><span class="k">층수</span><span class="v">{{ p.floorNo ? p.floorNo + '층' : '-' }}</span></div>
            <div><span class="k">유형</span><span class="v">{{ typeLabel(p.propertyType) }}</span></div>
            <div><span class="k">평균 거래가</span><span class="v">{{ priceLabel(p) }}</span></div>
            <div><span class="k">건축연도</span><span class="v">{{ p.buildYear ? p.buildYear + '년' : '-' }}</span></div>
          </div>
        </div>

        <!-- 실거래가 추이 (월별 평균가, building_price_history) -->
        <div class="card trend">
          <div class="card-title">📈 실거래가 추이 <em v-if="trend.length">· {{ dealLabel(p.dealType) }}</em></div>
          <div v-if="trend.length >= 2" class="trend-chart">
            <div class="chart-body">
              <div class="chart-yaxis">
                <span v-for="(t, i) in yTicks" :key="i" class="ytick" :style="{ top: yPix(t) + 'px' }">{{ formatManwon(t) }}</span>
              </div>
              <div class="chart-plot">
                <svg viewBox="0 0 320 130" class="chart-svg" preserveAspectRatio="none">
                  <line v-for="(t, i) in yTicks" :key="'g' + i" x1="0" :y1="yPix(t)" x2="320" :y2="yPix(t)" stroke="#eef0f4" stroke-width="1" vector-effect="non-scaling-stroke" />
                  <polyline :points="chartLine" fill="none" stroke="#2563eb" stroke-width="2" vector-effect="non-scaling-stroke" />
                  <circle v-for="(pt, i) in chartPts" :key="i" :cx="pt.x" :cy="pt.y" r="3" fill="#2563eb" vector-effect="non-scaling-stroke" />
                </svg>
                <div class="chart-x">
                  <span v-for="(pt, i) in chartPts" :key="i" class="xtick" :style="xLabelStyle(i)">{{ ymLabel(trend[i].dealYm) }}</span>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="trend-placeholder">
            <span>추이 데이터 없음</span>
            <small>이 단지·거래유형의 월별 거래가 아직 적재되지 않았습니다</small>
          </div>
        </div>

        <!-- 리뷰 -->
        <div class="card reviews">
          <div class="card-title row">
            <span>리뷰 ({{ reviewTotal }})</span>
            <button class="write-btn" @click="handleWriteClick">
              {{ showWrite ? '취소' : '리뷰 작성' }}
            </button>
          </div>

          <form v-if="showWrite" class="review-form" @submit.prevent="submitReview">
            <select v-model.number="newReview.rating" class="r-input">
              <option :value="5">★5 아주 좋아요</option>
              <option :value="4">★4 좋아요</option>
              <option :value="3">★3 보통</option>
              <option :value="2">★2 별로</option>
              <option :value="1">★1 최악</option>
            </select>
            <textarea v-model="newReview.content" rows="3" class="r-input" placeholder="리뷰 내용 (10~500자)"></textarea>
            <button type="submit" class="submit-btn" :disabled="submitting">{{ submitting ? '작성 중…' : '등록' }}</button>
          </form>

          <!-- AI 요약 -->
          <div v-if="summaryText" class="ai-summary">
            <div class="ai-head">✨ AI 리뷰 요약</div>
            <p>{{ summaryText }}</p>
          </div>
          <div v-else-if="loadingSummary" class="muted">AI 요약 생성 중…</div>

          <div v-if="loadingReviews" class="muted">불러오는 중…</div>
          <div v-else-if="reviews.length === 0" class="muted">아직 작성된 리뷰가 없습니다.</div>
          <ul v-else class="review-list">
            <li v-for="rv in reviews" :key="rv.id" class="review-item">
              <div class="rv-head">
                <span class="rv-nick">{{ rv.nickname }}</span>
                <span class="rv-rating">⭐ {{ rv.rating }}</span>
              </div>
              <p class="rv-content">{{ rv.content }}</p>
            </li>
          </ul>
        </div>
      </div>

      <!-- ===== 우측 ===== -->
      <div class="col-right">
        <!-- 종합 인프라 점수 도넛 -->
        <div class="card score-card">
          <div class="card-title center">종합 인프라 점수</div>
          <div class="donut" :style="donutStyle">
            <div class="donut-hole">
              <span class="donut-score" :style="{ color: scoreColor(score) }">{{ score }}</span>
              <span class="donut-max">/100</span>
            </div>
          </div>
          <p class="score-cap">페르소나 가중치 적용 점수</p>
        </div>

        <!-- 인프라 세부 분석 -->
        <div class="card breakdown">
          <div class="card-title">인프라 세부 분석</div>
          <div class="bd-row" v-for="g in GROUPS" :key="g.key">
            <div class="bd-top">
              <span class="bd-label">{{ g.icon }} {{ g.long }}</span>
              <span class="bd-val" :style="{ color: g.color }">{{ gScore(g.key) }}</span>
            </div>
            <span class="bd-track"><i :style="{ width: gScore(g.key) + '%', background: g.color }"></i></span>
            <p v-if="infraCaptions[g.key]" class="bd-cap">{{ infraCaptions[g.key] }}</p>
          </div>
        </div>

        <!-- 주변 지도 -->
        <div class="card map-card">
          <div class="card-title">주변 지도</div>
          <div ref="miniMapEl" class="mini-map"></div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useMapStore } from '@/stores/map';
import { useAuthStore } from '@/stores/auth';
import { totalScore, groupScore, scoreColor, GROUPS } from '@/utils/score';
import { dealLabel, typeLabel, priceLabel, formatManwon } from '@/utils/price';
import { getReviews, createReview, getReviewSummary } from '@/api/review';
import { fetchPois } from '@/api/pois';
import { fetchPriceTrend } from '@/api/trend';

const props = defineProps({
  property: { type: Object, required: true },
});
const emit = defineEmits(['close']);

const p = computed(() => props.property);
const mapStore = useMapStore();
const authStore = useAuthStore();
const router = useRouter();

function handleWriteClick() {
  if (!authStore.isAuthenticated) {
    if (confirm('로그인이 필요합니다. 로그인 화면으로 이동하시겠습니까?')) {
      router.push('/login');
    }
    return;
  }
  showWrite.value = !showWrite.value;
}

// ===== 점수(마커/목록과 동일 함수 → 일치) =====
const score = computed(() => totalScore(p.value, mapStore.persona));
const gScore = (key) => groupScore(p.value[key + 'Base'], key);
const donutStyle = computed(() => ({
  background: `conic-gradient(${scoreColor(score.value)} ${score.value * 3.6}deg, #e1e3e4 0)`,
}));

// ===== 인프라 세부 캡션(주변 500m POI 개수, MAP-02 Public) =====
const GROUP_CATS = {
  transit: ['subway', 'bus_stop'],
  education: ['school', 'academy'],
  commerce: ['restaurant', 'cafe', 'cinema', 'mart'],
  convenience: ['convenience_store', 'hospital', 'pharmacy', 'bank'],
};
const CAT_LABEL = {
  subway: '지하철', bus_stop: '버스', school: '학교', academy: '학원',
  restaurant: '식당', cafe: '카페', cinema: '영화관', mart: '마트',
  convenience_store: '편의점', hospital: '병원', pharmacy: '약국', bank: '은행',
};
const infraCaptions = ref({ transit: '', education: '', commerce: '', convenience: '' });

async function loadInfraCounts() {
  if (p.value.lat == null) return;
  const dLat = 0.0045, dLng = 0.0056;
  const bbox = `${p.value.lng - dLng},${p.value.lat - dLat},${p.value.lng + dLng},${p.value.lat + dLat}`;
  try {
    const pois = await fetchPois({ bbox, groups: 'transit,education,commerce,convenience' });
    const cnt = {};
    pois.forEach((poi) => { cnt[poi.category] = (cnt[poi.category] ?? 0) + 1; });
    const caps = {};
    for (const [g, cats] of Object.entries(GROUP_CATS)) {
      const parts = cats.filter((c) => cnt[c]).map((c) => `${CAT_LABEL[c]} ${cnt[c]}`);
      caps[g] = parts.length ? `500m 내 ${parts.join(' · ')}` : '500m 내 시설 없음';
    }
    infraCaptions.value = caps;
  } catch (e) {
    // 캡션은 보조 정보 — 실패해도 무시
  }
}

// ===== 동 이름(카카오 역지오코딩) =====
const dong = ref('');
function loadDong() {
  const { kakao } = window;
  if (!kakao?.maps?.services || p.value.lat == null) return;
  const geocoder = new kakao.maps.services.Geocoder();
  geocoder.coord2Address(p.value.lng, p.value.lat, (res, status) => {
    if (status === kakao.maps.services.Status.OK && res[0]) {
      const a = res[0].address;
      dong.value = `${a.region_2depth_name} ${a.region_3depth_name}`.trim();
    }
  });
}

// ===== 거리뷰 =====
const roadviewEl = ref(null);
const roadviewOk = ref(true);
function initRoadview() {
  const { kakao } = window;
  if (!kakao?.maps || !roadviewEl.value) return;
  const pos = new kakao.maps.LatLng(p.value.lat, p.value.lng);
  const rv = new kakao.maps.Roadview(roadviewEl.value);
  const client = new kakao.maps.RoadviewClient();
  client.getNearestPanoId(pos, 100, (panoId) => {
    if (panoId === null) { roadviewOk.value = false; return; }
    roadviewOk.value = true;
    rv.setPanoId(panoId, pos);
  });
}

// ===== 미니맵 =====
const miniMapEl = ref(null);
function initMiniMap() {
  const { kakao } = window;
  if (!kakao?.maps || !miniMapEl.value) return;
  const pos = new kakao.maps.LatLng(p.value.lat, p.value.lng);
  const map = new kakao.maps.Map(miniMapEl.value, { center: pos, level: 4, draggable: false });
  new kakao.maps.Marker({ position: pos, map });
}

// ===== 리뷰(REV-01) =====
const reviews = ref([]);
const reviewTotal = ref(0);
const loadingReviews = ref(false);
const showWrite = ref(false);
const submitting = ref(false);
const newReview = reactive({ rating: 5, content: '' });

const summaryText = ref(null);
const loadingSummary = ref(false);

async function loadReviewSummary() {
  if (reviewTotal.value < 5) {
    summaryText.value = null;
    return;
  }
  loadingSummary.value = true;
  try {
    const data = await getReviewSummary('BUILDING', String(p.value.id));
    if (data && data.summaryAvailable) {
      summaryText.value = data.summary;
    } else {
      summaryText.value = null;
    }
  } catch (e) {
    summaryText.value = null;
  } finally {
    loadingSummary.value = false;
  }
}

async function loadReviews() {
  loadingReviews.value = true;
  summaryText.value = null;
  try {
    const data = await getReviews('BUILDING', String(p.value.id), 0, 20);
    reviews.value = data.content ?? [];
    reviewTotal.value = data.totalElements ?? reviews.value.length;
    
    if (reviewTotal.value >= 5) {
      loadReviewSummary();
    }
  } catch (e) {
    reviews.value = [];
    reviewTotal.value = 0;
  } finally {
    loadingReviews.value = false;
  }
}

async function submitReview() {
  if (newReview.content.trim().length < 10) { alert('리뷰는 10자 이상 작성해주세요.'); return; }
  submitting.value = true;
  try {
    await createReview('BUILDING', String(p.value.id), newReview.content, newReview.rating);
    newReview.content = '';
    newReview.rating = 5;
    showWrite.value = false;
    await loadReviews();
  } catch (e) {
    alert('리뷰 작성에 실패했습니다.');
  } finally {
    submitting.value = false;
  }
}

// ===== 실거래가 추이(월별 평균가) =====
const trend = ref([]); // [{ dealYm, avgAmount, dealCount }]
async function loadTrend() {
  try {
    trend.value = (await fetchPriceTrend(p.value.id)) ?? [];
  } catch (e) {
    trend.value = [];
  }
}
const CHART_W = 320, CHART_H = 130, PAD = 14;
const trendMin = computed(() => (trend.value.length ? Math.min(...trend.value.map((x) => x.avgAmount)) : 0));
const trendMax = computed(() => (trend.value.length ? Math.max(...trend.value.map((x) => x.avgAmount)) : 0));

// "예쁜" 눈금 산출(4.5억/2.1억 → 5억~2억, 1억 간격)
function niceNum(range, round) {
  const exp = Math.floor(Math.log10(range || 1));
  const frac = (range || 1) / Math.pow(10, exp);
  let nf;
  if (round) nf = frac < 1.5 ? 1 : frac < 3 ? 2 : frac < 7 ? 5 : 10;
  else nf = frac <= 1 ? 1 : frac <= 2 ? 2 : frac <= 5 ? 5 : 10;
  return nf * Math.pow(10, exp);
}
const yScale = computed(() => {
  if (trend.value.length < 2) return { lo: 0, hi: 1, ticks: [] };
  const min = trendMin.value, max = trendMax.value;
  if (max <= min) { const m = min || 1; return { lo: 0, hi: m * 2, ticks: [0, m, m * 2] }; }
  const step = niceNum((max - min) / 3, true);
  const lo = Math.floor(min / step) * step;
  const hi = Math.ceil(max / step) * step;
  const ticks = [];
  for (let v = lo; v <= hi + step * 0.5; v += step) ticks.push(Math.round(v));
  return { lo, hi, ticks };
});
const yTicks = computed(() => yScale.value.ticks);
function yPix(v) {
  const { lo, hi } = yScale.value;
  const range = hi - lo || 1;
  return CHART_H - PAD - ((v - lo) / range) * (CHART_H - 2 * PAD);
}
const chartPts = computed(() => {
  const d = trend.value;
  if (d.length < 2) return [];
  return d.map((x, i) => ({
    x: (i / (d.length - 1)) * CHART_W,
    y: yPix(x.avgAmount),
  }));
});
const chartLine = computed(() => chartPts.value.map((pt) => `${pt.x.toFixed(1)},${pt.y.toFixed(1)}`).join(' '));
const ymLabel = (ym) => `${ym.slice(2, 4)}.${ym.slice(4, 6)}`;
// 각 라벨을 해당 점 x좌표 아래에 정렬(양끝은 안쪽으로)
function xLabelStyle(i) {
  const n = trend.value.length;
  const pct = n > 1 ? (i / (n - 1)) * 100 : 50;
  let transform = 'translateX(-50%)';
  if (i === 0) transform = 'translateX(0)';
  else if (i === n - 1) transform = 'translateX(-100%)';
  return { left: pct + '%', transform };
}

function reloadPropertyData() {
  const { kakao } = window;
  if (kakao?.maps) {
    kakao.maps.load(() => { loadDong(); initRoadview(); initMiniMap(); });
  }
  loadReviews();
  loadInfraCounts();
  loadTrend();
}

watch(() => props.property.id, () => {
  reloadPropertyData();
});

onMounted(() => {
  reloadPropertyData();
});
onUnmounted(() => {/* kakao 인스턴스는 컨테이너와 함께 GC */});
</script>

<style scoped>
/* ── 팔레트 (DESIGN.md) ─────────────────────────────────────
   Primary (Midnight Blue)    : #041627
   Secondary (Terracotta)     : #944a00  accent: #fc8f34
   Background (Soft Off-White): #f8f9fa
   on-surface                 : #191c1d
   on-surface-variant         : #44474c
   outline                    : #74777d
   outline-variant            : #c4c6cd
   surface-container-low      : #f3f4f5
   Level-1 shadow             : 0px 4px 12px rgba(26,43,60,0.05)
   Level-2 shadow             : 0px 12px 32px rgba(26,43,60,0.12)
────────────────────────────────────────────────────────────── */

.detail {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 320px;
  right: 360px;
  z-index: 20;
  background: #f8f9fa;
  overflow-y: auto;
  padding: 20px 32px 40px;
  border-left: 1px solid #c4c6cd;
  border-right: 1px solid #c4c6cd;
}

.detail-top {
  margin-bottom: 16px;
}

.back {
  background: #ffffff;
  border: 1px solid #c4c6cd;
  border-radius: 9999px;
  color: #041627;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  padding: 10px 20px;
  box-shadow: 0 2px 8px rgba(26,43,60,0.06);
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.back:hover { 
  background: #f8f9fa;
  border-color: #944a00;
  color: #944a00;
  box-shadow: 0 4px 12px rgba(26,43,60,0.1);
}

/* 2컬럼 그리드 */
.grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 20px;
  max-width: 1280px;
  margin: 0 auto;
}
.col-left, .col-right { display: flex; flex-direction: column; gap: 20px; }

/* 공통 카드 */
.card {
  background: #ffffff;
  border: 1px solid #c4c6cd;
  border-radius: 16px;
  padding: 18px;
  box-shadow: 0px 4px 12px rgba(26, 43, 60, 0.05);
}
.card-title {
  font-size: 14px;
  font-weight: 700;
  color: #041627;
  margin-bottom: 14px;
}
.card-title.center { text-align: center; }
.card-title.row { display: flex; align-items: center; justify-content: space-between; }

/* 거리뷰 */
.gallery { padding: 0; height: 300px; position: relative; overflow: hidden; }
.roadview { width: 100%; height: 100%; }
.roadview-fallback {
  position: absolute; inset: 0; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 10px;
  color: #74777d; background: #f3f4f5;
}
.roadview-fallback span { font-size: 40px; opacity: 0.4; }
.roadview-fallback p { font-size: 13px; color: #74777d; }

/* 매물 정보 */
.info-head {
  display: flex; align-items: flex-start; justify-content: space-between;
  gap: 16px; margin-bottom: 16px;
}
.bname { font-size: 22px; font-weight: 800; color: #041627; }
.addr { font-size: 13px; color: #44474c; margin-top: 4px; }
.price-wrap { text-align: right; display: flex; flex-direction: column; align-items: flex-end; gap: 4px; flex-shrink: 0; }
.deal-tag {
  font-size: 11px; font-weight: 700; color: #944a00;
  background: rgba(148, 74, 0, 0.08);
  padding: 2px 8px; border-radius: 9999px;
}
.price { font-size: 22px; font-weight: 800; color: #041627; }
.info-grid {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px 0;
  border-top: 1px solid #e1e3e4; padding-top: 14px;
}
.info-grid > div { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.info-grid .k { font-size: 11px; color: #74777d; font-weight: 500; }
.info-grid .v { font-size: 15px; font-weight: 700; color: #041627; }

/* 실거래가 추이 placeholder */
.trend-placeholder {
  height: 160px; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 6px;
  border: 1px dashed #c4c6cd; border-radius: 12px;
  color: #74777d; background: #f8f9fa;
}
.trend-placeholder span { font-size: 14px; font-weight: 600; }
.trend-placeholder small { font-size: 11px; opacity: 0.7; }

/* 실거래가 추이 차트 */
.trend-chart { padding-top: 4px; }
.chart-body { display: flex; gap: 6px; }
.chart-yaxis { position: relative; width: 44px; height: 130px; flex: 0 0 44px; }
.ytick {
  position: absolute; right: 0; transform: translateY(-50%);
  font-size: 10px; font-weight: 600; white-space: nowrap;
  color: var(--text-tertiary, #94a3b8);
}
.chart-plot { position: relative; flex: 1; min-width: 0; }
.chart-svg { width: 100%; height: 130px; display: block; overflow: visible; }
.chart-x { position: relative; height: 13px; margin-top: 4px; }
.xtick {
  position: absolute; top: 0; white-space: nowrap;
  font-size: 10px; color: var(--text-tertiary, #94a3b8);
}

/* 리뷰 섹션 */
.write-btn {
  background: #041627; color: #ffffff;
  border: none; padding: 5px 12px;
  border-radius: 9999px; font-size: 12px; font-weight: 700;
  cursor: pointer; transition: all 0.15s ease;
}
.write-btn:hover { background: #1a2b3c; box-shadow: 0 0 8px rgba(252, 143, 52, 0.4); }

.review-form { display: flex; flex-direction: column; gap: 8px; margin-bottom: 14px; }

.r-input {
  background: #f3f4f5;
  border: 1px solid #c4c6cd;
  border-radius: 8px;
  padding: 8px 10px;
  color: #191c1d;
  font-family: inherit;
  font-size: 13px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
  outline: none;
  width: 100%;
  box-sizing: border-box;
}
.r-input:focus {
  border-color: #944a00;
  box-shadow: 0 0 0 2px rgba(252, 143, 52, 0.2);
  background: #ffffff;
}

.submit-btn {
  align-self: flex-end;
  background: #041627; color: #ffffff;
  border: none; padding: 6px 18px;
  border-radius: 8px; font-weight: 700; font-size: 13px;
  cursor: pointer; transition: all 0.15s ease;
}
.submit-btn:hover:not(:disabled) {
  background: #1a2b3c;
  box-shadow: 0 0 8px rgba(252, 143, 52, 0.4);
}
.submit-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.login-hint { font-size: 12px; color: #74777d; margin-bottom: 12px; }
.muted { font-size: 13px; color: #74777d; padding: 10px 0; }

/* AI 요약 */
.ai-summary {
  background: #fff5ec;
  border: 1px solid #fc8f34;
  border-radius: 10px;
  padding: 14px;
  margin-bottom: 14px;
}
.ai-head {
  font-size: 13px;
  font-weight: 800;
  color: #944a00;
  margin-bottom: 6px;
}
.ai-summary p {
  font-size: 13px;
  line-height: 1.6;
  color: #44474c;
  margin: 0;
}

.review-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 10px; }
.review-item {
  background: #f8f9fa;
  border: 1px solid #e1e3e4;
  border-radius: 10px; padding: 12px;
}
.rv-head { display: flex; gap: 10px; align-items: center; margin-bottom: 4px; }
.rv-nick { font-weight: 700; font-size: 13px; color: #041627; }
.rv-rating { font-size: 12px; color: #944a00; font-weight: 600; }
.rv-content { font-size: 13px; line-height: 1.6; color: #44474c; white-space: pre-wrap; margin: 0; }

/* 종합점수 도넛 */
.score-card { display: flex; flex-direction: column; align-items: center; }
.donut {
  width: 180px; height: 180px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
}
.donut-hole {
  width: 132px; height: 132px; border-radius: 50%;
  background: #ffffff;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  box-shadow: inset 0 2px 8px rgba(26, 43, 60, 0.06);
}
.donut-score { font-size: 44px; font-weight: 800; line-height: 1; }
.donut-max { font-size: 13px; color: #74777d; margin-top: 2px; }
.score-cap { font-size: 12px; color: #74777d; margin-top: 14px; }

/* 세부 분석 바 */
.bd-row { margin-bottom: 16px; }
.bd-row:last-child { margin-bottom: 0; }
.bd-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.bd-label { font-size: 13px; color: #191c1d; font-weight: 600; }
.bd-val { font-size: 14px; font-weight: 800; }
.bd-track {
  display: block; width: 100%; height: 6px;
  background: #e1e3e4; border-radius: 9999px; overflow: hidden;
}
.bd-track i { display: block; height: 100%; border-radius: 9999px; transition: width 0.4s ease; }
.bd-cap { margin-top: 5px; font-size: 11px; color: #74777d; }

/* 미니맵 */
.mini-map { width: 100%; height: 200px; border-radius: 12px; overflow: hidden; }
</style>
