import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import {
  login as apiLogin,
  signup as apiSignup,
  logout as apiLogout,
  getProfile as apiGetProfile,
} from '../api/auth';

/**
 * 인증 상태 스토어 (Phase 2 Dev B).
 *
 * 로그인, 회원가입, 로그아웃, 그리고 RTR을 통한 토큰 갱신 및 프로필 조회를 관리한다.
 */
export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem('access_token') || null);
  const currentUser = ref(null);
  const loading = ref(false);

  // 서버 응답으로 검증되지 않은 로컬 스토리지 데이터에 의존하는 임시 권한 부여(temp_rehydrate) 로직 제거.

  // isAuthenticated는 단순히 브라우저 스토리지의 토큰 존재 여부가 아니라,
  // 실제 백엔드 회원 정보 조회(users/me) 성공 시 할당되는 currentUser 객체의 존재 여부로만 결정합니다.
  const isAuthenticated = computed(() => currentUser.value !== null);

  function setAccessToken(token) {
    accessToken.value = token;
    if (token) {
      localStorage.setItem('access_token', token);
    } else {
      localStorage.removeItem('access_token');
    }
  }

  function clearAllAuth() {
    accessToken.value = null;
    currentUser.value = null;
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
  }

  async function login(email, password) {
    loading.value = true;
    try {
      const response = await apiLogin({ email, password });
      setAccessToken(response.data.accessToken);
      localStorage.setItem('refresh_token', 'true');
      await fetchProfile();
    } finally {
      loading.value = false;
    }
  }

  async function signup(email, password, nickname) {
    loading.value = true;
    try {
      await apiSignup({ email, password, nickname });
    } finally {
      loading.value = false;
    }
  }

  async function logout() {
    loading.value = true;
    try {
      await apiLogout();
    } finally {
      clearAllAuth();
      loading.value = false;
    }
  }

  async function fetchProfile() {
    try {
      const response = await apiGetProfile();
      currentUser.value = response.data;
    } catch (error) {
      clearAllAuth();
      throw error;
    }
  }

  async function checkAuth() {
    if (!accessToken.value) {
      clearAllAuth();
      return;
    }
    try {
      await fetchProfile();
    } catch (error) {
      console.warn('[AuthStore] Session validation/refresh failed via checkAuth:', error);
      throw error;
    }
  }

  return {
    accessToken,
    currentUser,
    loading,
    isAuthenticated,
    setAccessToken,
    clearAllAuth,
    login,
    signup,
    logout,
    fetchProfile,
    checkAuth,
  };
});
