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

    <!-- 매물 선택 시 리뷰 모달 직결(Dev B 컴포넌트, §7.4 단방향) -->
    <ReviewModal
      :isOpen="isReviewModalOpen"
      targetType="BUILDING"
      :targetId="String(selectedPropertyId)"
      @close="isReviewModalOpen = false"
    />
  </main>
</template>

<script setup>
import { ref } from 'vue';
import MapContainer from '@/components/map/MapContainer.vue';
import FilterSidebar from '@/components/search/FilterSidebar.vue';
import PropertyListPanel from '@/components/search/PropertyListPanel.vue';
import MapLegend from '@/components/search/MapLegend.vue';
import LocateButton from '@/components/search/LocateButton.vue';
import ZoomButtons from '@/components/search/ZoomButtons.vue';
import ReviewModal from '@/components/review/ReviewModal.vue';

const selectedPropertyId = ref(null);
const isReviewModalOpen = ref(false);

// §7.4: 지도/목록은 propertyId만 발신. 여기서 모달을 연다(상세 페이지는 후속 PR B).
function onMarkerClick(propertyId) {
  selectedPropertyId.value = propertyId;
  isReviewModalOpen.value = true;
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
