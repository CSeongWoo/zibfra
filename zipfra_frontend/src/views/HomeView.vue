<template>
  <!--
    홈: 풀스크린 지도 + 지도 위 모든 오버레이.
    §7.4 단방향: propertyId만 발신 → 여기서 마커 객체를 찾아 상세페이지를 연다.
  -->
  <main class="relative h-screen w-full overflow-hidden animate-fade-in">
    <MapContainer @marker-click="onMarkerClick" />

    <!-- ① Floating Topbar: 검색. 목록 열림 여부에 따라 우측 폭 조정 -->
    <div class="topbar" :class="{ 'list-open': mapStore.listOpen }">
      <SearchBar />
    </div>

    <!-- ② 와이어3 좌측 통합 사이드바 (반응형 토글) -->
    <FilterSidebar />

    <!-- ③ 우측 매물 목록: 평소 숨김 → 클러스터 마커 클릭 시 그 단지 매물 표시(기능4) -->
    <PropertyListPanel
      v-if="mapStore.listOpen"
      :items="mapStore.listItems"
      @select="onListSelect"
      @close="mapStore.closeList()"
    />

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

// 클러스터(동일좌표) 마커 클릭 → 그 단지 매물 배열을 우측 목록에 표시(기능4).
function onMarkerClick(items) {
  mapStore.openList(items);
}

// 목록 카드 클릭 → 상세페이지 진입(§7.4 단방향).
function onListSelect(propertyId) {
  selectedProperty.value = mapStore.listItems.find((m) => m.id === propertyId)
    ?? mapStore.markers.find((m) => m.id === propertyId)
    ?? null;
}
</script>

<style scoped>
/* ── Floating Topbar (검색 전용) ──────────────────────────── */
.topbar {
  position: absolute;
  top: 16px;
  left: 336px;
  right: 24px;
  transition: right 0.3s cubic-bezier(0.4, 0, 0.2, 1);
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

/* 우측 매물 목록(360px) 열리면 검색바를 그 좌측까지로 */
.topbar.list-open {
  right: 392px;
}
</style>
