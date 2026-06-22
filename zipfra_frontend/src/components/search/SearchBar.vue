<template>
  <!--
    상단 검색창 + 현위치(#7). 카카오 services.Places 키워드 검색 → 첫 결과로 지도 이동.
    지도 인스턴스는 직접 만지지 않고 store.requestMove 로 명령만 발신(§7.4 단방향: UI→지도).
    이동 후 KakaoMap 의 idle 이벤트가 MAP-01 을 재조회한다.
  -->
  <div class="search-bar">
    <span class="icon">🔍</span>
    <input
      v-model="query"
      type="text"
      placeholder="지역, 단지명, 주소 검색…"
      @keyup.enter="search"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useMapStore } from '@/stores/map';

const mapStore = useMapStore();
const query = ref('');

const DETAIL_LEVEL = 5; // 카카오 level 5 ≈ 서버 zoom 15(DETAIL, §7.1 MapContainer 변환)

function search() {
  const q = query.value.trim();
  const { kakao } = window;
  if (!q || !kakao?.maps?.services) return;
  const places = new kakao.maps.services.Places();
  places.keywordSearch(q, (data, status) => {
    if (status === kakao.maps.services.Status.OK && data.length) {
      // x=lng, y=lat (카카오 좌표 규약)
      mapStore.requestMove(Number(data[0].y), Number(data[0].x), DETAIL_LEVEL);
    } else {
      alert('검색 결과가 없습니다.');
    }
  });
}

</script>

<style scoped>
.search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 360px;
  max-width: 38vw;
  padding: 8px 16px;
  border-radius: 9999px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid var(--border-color);
}

.search-bar:focus-within {
  border-color: var(--color-info);
}

.icon {
  font-size: 13px;
  flex-shrink: 0;
  opacity: 0.6;
}

.search-bar input {
  flex: 1;
  min-width: 0;
  border: none;
  background: transparent;
  font-size: 13px;
  color: var(--text-primary);
}

.search-bar input::placeholder {
  color: var(--text-tertiary);
}

.search-bar input:focus {
  outline: none;
}
</style>
