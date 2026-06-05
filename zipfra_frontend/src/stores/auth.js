import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

/**
 * 인증 상태 스토어 골격 (Phase 1 Dev B).
 *
 * Phase 1에서는 Access Token 보관·주입 지점만 마련한다.
 * 로그인/RTR 재발급/Blacklist 등 실제 흐름은 Phase 2 Dev B(§10)에서 채운다.
 */
export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(null);

  const isAuthenticated = computed(() => accessToken.value !== null);

  function setAccessToken(token) {
    accessToken.value = token;
  }

  function clearAccessToken() {
    accessToken.value = null;
  }

  return { accessToken, isAuthenticated, setAccessToken, clearAccessToken };
});
