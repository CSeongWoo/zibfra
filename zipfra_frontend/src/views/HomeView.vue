<template>
  <!--
    홈: 풀스크린 지도 + 지도 위 모든 오버레이.
    §7.4 단방향: propertyId만 발신 → 여기서 마커 객체를 찾아 상세페이지를 연다.
  -->
  <main class="relative h-screen w-full overflow-hidden animate-fade-in">
    <MapContainer @marker-click="onMarkerClick" />

    <!-- ① Floating Topbar: 검색 + 검색 버튼만 -->
    <div class="topbar">
      <SearchBar />
    </div>

    <!-- ② 와이어3 좌측 통합 사이드바 (반응형 토글) -->
    <FilterSidebar />

    <!-- ③ 와이어3 우측 매물 목록 (반응형 토글) -->
    <PropertyListPanel @select="onMarkerClick" />

    <!-- ④ 지도 컨트롤: 범례(좌하단) · 현위치(하단중앙) · 줌(우하단) -->
    <MapLegend />
    <LocateButton />
    <ZoomButtons />

    <!-- ⑤ 매물 상세페이지(#17). 리뷰는 상세페이지 내부에 임베드 -->
    <PropertyDetailPanel
      v-if="selectedProperty"
      :property="selectedProperty"
      @close="selectedProperty = null"
    />
  </main>
</template>

<script setup>
import { ref } from 'vue';
import { useMapStore } from '@/stores/map';
import MapContainer from '@/components/map/MapContainer.vue';
import FilterSidebar from '@/components/search/FilterSidebar.vue';
import PropertyListPanel from '@/components/search/PropertyListPanel.vue';
import MapLegend from '@/components/search/MapLegend.vue';
import LocateButton from '@/components/search/LocateButton.vue';
import ZoomButtons from '@/components/search/ZoomButtons.vue';
import PropertyDetailPanel from '@/components/property/PropertyDetailPanel.vue';
import SearchBar from '@/components/search/SearchBar.vue';

const mapStore = useMapStore();
const selectedProperty = ref(null);

function onMarkerClick(propertyId) {
  selectedProperty.value = mapStore.markers.find((m) => m.id === propertyId) ?? null;
}
</script>

<style scoped>
/* ── Floating Topbar (검색 전용) ──────────────────────────── */
.topbar {
  position: absolute;
  top: 16px;
  left: 336px;
  right: 392px;
  z-index: 10;
  height: 52px;
  min-width: 240px;
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid #c4c6cd;
  border-radius: 9999px;
  padding: 0 8px 0 16px;
  box-shadow: 0px 4px 12px rgba(26, 43, 60, 0.08);
  overflow: visible;
}
</style>
