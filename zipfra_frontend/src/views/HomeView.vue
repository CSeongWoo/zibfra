<template>
  <main class="workspace animate-fade-in">
    <KakaoMap @viewport-change="onViewport" @marker-click="onMarkerClick" />

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
    </div>
  </main>
</template>

<script setup>
import { ref } from 'vue';
import KakaoMap from '@/components/map/KakaoMap.vue';
import { useMapStore } from '@/stores/map';

const mapStore = useMapStore();
const lastPropertyId = ref(null);

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
</style>
