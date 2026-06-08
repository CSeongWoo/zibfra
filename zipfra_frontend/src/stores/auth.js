import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import {
  login as apiLogin,
  signup as apiSignup,
  logout as apiLogout,
  refresh as apiRefresh,
  getProfile as apiGetProfile,
} from '../api/auth';

/**
 * 인증 상태 스토어 (Phase 2 Dev B).
 *
 * 로그인, 회원가입, 로그아웃, 그리고 RTR을 통한 토큰 갱신 및 프로필 조회를 관리한다.
 */
export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(null);
  const currentUser = ref(null);
  const loading = ref(false);

  const isAuthenticated = computed(() => accessToken.value !== null);

  function setAccessToken(token) {
    accessToken.value = token;
  }

  function clearAccessToken() {
    accessToken.value = null;
    currentUser.value = null;
    localStorage.removeItem('refresh_token');
  }

  async function login(email, password) {
    loading.value = true;
    try {
      const response = await apiLogin({ email, password });
      accessToken.value = response.data.accessToken;
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
      clearAccessToken();
      loading.value = false;
    }
  }

  async function fetchProfile() {
    try {
      const response = await apiGetProfile();
      currentUser.value = response.data;
    } catch (error) {
      clearAccessToken();
      throw error;
    }
  }

  async function checkAuth() {
    try {
      const response = await apiRefresh();
      accessToken.value = response.data.accessToken;
      localStorage.setItem('refresh_token', 'true');
      await fetchProfile();
    } catch (error) {
      clearAccessToken();
    }
  }

  return {
    accessToken,
    currentUser,
    loading,
    isAuthenticated,
    setAccessToken,
    clearAccessToken,
    login,
    signup,
    logout,
    fetchProfile,
    checkAuth,
  };
});
