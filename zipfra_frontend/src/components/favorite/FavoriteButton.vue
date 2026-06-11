<template>
  <button 
    class="favorite-btn" 
    :class="{ active: isFavorite }" 
    @click.stop="handleToggle" 
    :disabled="loading"
    title="즐겨찾기"
  >
    <span class="icon">{{ isFavorite ? '❤️' : '🤍' }}</span>
  </button>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { toggleFavorite, getFavorites } from '@/api/favorite';
import { useAuthStore } from '@/stores/auth';
import { useRouter } from 'vue-router';

const props = defineProps({
  propertyId: {
    type: [Number, String],
    required: true
  }
});

const authStore = useAuthStore();
const router = useRouter();

const isFavorite = ref(false);
const loading = ref(false);

onMounted(async () => {
  if (!authStore.isAuthenticated) return;
  try {
    const favorites = await getFavorites();
    isFavorite.value = favorites.some(f => String(f.propertyId) === String(props.propertyId));
  } catch (e) {
    console.error('Failed to load favorites', e);
  }
});

async function handleToggle() {
  if (!authStore.isAuthenticated) {
    alert('로그인이 필요한 기능입니다.');
    router.push('/login');
    return;
  }
  
  if (loading.value) return;
  loading.value = true;
  
  try {
    const result = await toggleFavorite(props.propertyId);
    isFavorite.value = result.isFavorite;
  } catch (error) {
    console.error('Favorite toggle failed', error);
    alert('즐겨찾기 처리에 실패했습니다.');
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.favorite-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  transition: transform 0.2s ease;
  padding: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.favorite-btn:hover:not(:disabled) {
  transform: scale(1.1);
}
.favorite-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
