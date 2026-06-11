<template>
  <main class="workspace animate-fade-in">
    <MapContainer @viewport-change="onViewport" @marker-click="onMarkerClick" @map-click="onMapClick" />

    <div class="map-overlay glass-panel">
      <h3>실시간 뷰포트 (§7 · §8)</h3>
      <dl>
        <dt>bbox</dt>
        <dd>{{ mapStore.bbox ?? '–' }}</dd>
        <dt>zoom(level)</dt>
        <dd>{{ mapStore.zoom ?? '–' }}</dd>
        <dt>last propertyId</dt>
        <dd>{{ lastPropertyId ?? '–' }}</dd>
      </dl>
      <p class="hint">지도를 움직이면 bbox가 갱신됩니다. 마커는 Phase 2(MAP-01)에서 주입됩니다.</p>

      <!-- LOC-01: 지도 클릭 지점 입지 점수 (가중치 전부 1.0 고정) -->
      <hr />
      <h3>입지 점수 (LOC-01)</h3>
      <p v-if="!score && !scoreError" class="hint">지도를 클릭하면 그 지점의 입지 점수를 계산합니다.</p>
      <p v-if="scoreLoading" class="hint">계산 중…</p>
      <p v-if="scoreError" class="hint" style="color: var(--color-danger, #ef4444);">{{ scoreError }}</p>

      <template v-if="score">
        <dl>
          <dt>좌표</dt>
          <dd>{{ score.lon.toFixed(5) }}, {{ score.lat.toFixed(5) }}</dd>
          <dt>반경</dt>
          <dd>{{ score.radiusMeters }}m</dd>
          <dt>finalScore</dt>
          <dd><strong>{{ score.finalScore }}</strong></dd>
        </dl>
        <dl>
          <template v-for="(grp, key) in score.breakdown" :key="key">
            <dt>{{ key }}</dt>
            <dd>
              <span v-if="grp.applicable === false">제외(서울 외)</span>
              <span v-else>{{ grp.score }}</span>
            </dd>
            <template v-for="(cat, cname) in grp.categories" :key="cname">
              <dt style="padding-left: 12px; color: var(--text-tertiary);">· {{ cname }}</dt>
              <dd>{{ cat.contribution }} <span style="color: var(--text-tertiary);">({{ cat.count }}개)</span></dd>
            </template>
          </template>
        </dl>
      </template>
    </div>

    <!-- Phase 2 Demo Actions -->
    <div class="demo-actions glass-panel" v-if="lastPropertyId">
      <h4>선택된 매물 (ID: {{ lastPropertyId }})</h4>
      <div class="actions-row">
        <FavoriteButton :propertyId="lastPropertyId" />
        <button class="review-btn" @click="isReviewModalOpen = true">리뷰 보기</button>
      </div>
    </div>

    <ReviewModal 
      :isOpen="isReviewModalOpen"
      targetType="BUILDING"
      :targetId="String(lastPropertyId)"
      @close="isReviewModalOpen = false"
    />
  </main>
</template>

<script setup>
import { ref } from 'vue';
import MapContainer from '@/components/map/MapContainer.vue';
import KakaoMap from '@/components/map/KakaoMap.vue';
import FavoriteButton from '@/components/favorite/FavoriteButton.vue';
import ReviewModal from '@/components/review/ReviewModal.vue';
import { useMapStore } from '@/stores/map';
import { fetchLocationScore } from '@/api/location';

const mapStore = useMapStore();
const lastPropertyId = ref(null);
const isReviewModalOpen = ref(false);

// LOC-01 데모: 가중치는 전 카테고리 1.0 고정(점수가 보이도록).
const DEMO_WEIGHTS = {
  pharmacy: 1, mart: 1, bank: 1,
  restaurant: 1, cafe: 1, cinema: 1,
  noise: 1, waste: 1,
};

const score = ref(null);
const scoreError = ref(null);
const scoreLoading = ref(false);

async function onMapClick({ lat, lng }) {
  scoreLoading.value = true;
  scoreError.value = null;
  try {
    score.value = await fetchLocationScore({ lon: lng, lat, weights: DEMO_WEIGHTS });
  } catch (e) {
    score.value = null;
    scoreError.value = e.status === 401
      ? '로그인이 필요합니다 (Protected).'
      : `${e.error ?? '오류'}: ${e.message ?? ''}`;
  } finally {
    scoreLoading.value = false;
  }
}

function onViewport({ bbox, level }) {
  // mapStore는 KakaoMap 내부에서 이미 갱신함. 여기선 데모용 로그.
  console.log('[viewport-change]', bbox, 'level', level);
}

// §7.4: 지도는 propertyId만 발신. 모달 등 상세는 부모/Dev B(Phase 2)가 처리.
function onMarkerClick(propertyId) {
  lastPropertyId.value = propertyId;
  console.log('[marker-click] propertyId =', propertyId);
}
</script>

<style scoped>
.workspace {
  flex: 1;
  position: relative;
  height: calc(100vh - 64px);
  width: 100%;
}

.map-overlay {
  position: absolute;
  top: 16px;
  left: 16px;
  z-index: 5;
  max-width: 340px;
  padding: 16px 18px;
  border-radius: var(--radius-md);
  font-size: 12px;
}

.map-overlay h3 {
  font-size: 12px;
  color: var(--accent-color);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 10px;
}

.map-overlay dl {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 4px 12px;
}

.map-overlay dt {
  color: var(--text-secondary);
}

.map-overlay dd {
  color: var(--text-primary);
  font-family: monospace;
  word-break: break-all;
}

.map-overlay .hint {
  margin-top: 10px;
  color: var(--text-tertiary);
  font-size: 11px;
  line-height: 1.5;
}

.demo-actions {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 5;
  padding: 16px;
  border-radius: var(--radius-md);
  min-width: 250px;
}

.demo-actions h4 {
  font-size: 13px;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.actions-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.review-btn {
  padding: 8px 16px;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  transition: all 0.2s;
}

.review-btn:hover {
  background: var(--bg-secondary);
  border-color: var(--accent-color);
}
</style>
