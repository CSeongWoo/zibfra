<template>
  <div class="app-container animate-fade-in">
    <!-- Header -->
    <header class="app-header glass-panel">
      <div class="logo">
        <span class="logo-icon">🏙️</span>
        <h1>Zipfra</h1>
        <span class="badge badge-info">Map Base</span>
      </div>

      <div class="status-indicator">
        <span class="pulse-dot"></span>
        <span class="status-text">
          Zoom <strong>{{ mapStore.zoom ?? '–' }}</strong> · {{ mapStore.strategy ?? 'IDLE' }}
        </span>
      </div>
    </header>

    <!-- Map workspace -->
    <main class="workspace">
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
  </div>
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

<style>
.app-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100vw;
  background-color: var(--bg-primary);
}

.app-header {
  height: 64px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  border-bottom: 1px solid var(--border-color);
  z-index: 10;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  font-size: 24px;
}

.logo h1 {
  font-size: 22px;
  font-family: var(--font-heading);
  letter-spacing: -0.5px;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-secondary);
}

.pulse-dot {
  width: 8px;
  height: 8px;
  background-color: var(--color-success);
  border-radius: 50%;
  box-shadow: 0 0 8px var(--color-success);
  animation: pulse-glow 2s infinite;
}

@keyframes pulse-glow {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7); }
  70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(16, 185, 129, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
}

.workspace {
  flex: 1;
  position: relative;
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
