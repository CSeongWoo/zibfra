<template>
  <main class="workspace animate-fade-in">
    <MapContainer @marker-click="onMarkerClick" />

    <!-- 와이어3 좌측 통합 사이드바: 필터 + 인프라 토글 + 페르소나 -->
    <FilterSidebar />

    <!-- 와이어3 우측: 매물 목록(점수배지 + 4미니바). 카드 클릭 = 마커 클릭과 동일 -->
    <PropertyListPanel @select="onMarkerClick" />

    <!-- 와이어3 지도 컨트롤: 범례(좌하단) · 현위치(하단중앙) · 줌(우하단) -->
    <MapLegend />
    <LocateButton />
    <ZoomButtons />

    <!-- 매물 선택 시 상세페이지(#17). 리뷰는 상세페이지 내부에 임베드 -->
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

const mapStore = useMapStore();
const selectedProperty = ref(null);

// §7.4: 지도/목록은 propertyId만 발신 → 여기서 마커 객체를 찾아 상세페이지를 연다.
function onMarkerClick(propertyId) {
  selectedProperty.value = mapStore.markers.find((m) => m.id === propertyId) ?? null;
}
</script>

<style scoped>
.workspace {
  flex: 1;
  position: relative;
  height: calc(100vh - 64px);
  width: 100%;
  overflow: hidden;
}
</style>
