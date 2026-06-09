import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';
import LoginView from '../views/LoginView.vue';
import { useAuthStore } from '../stores/auth';

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomeView,
    meta: { requiresAuth: false },
  },
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { requiresAuth: false },
  },
  {
    path: '/review-write',
    name: 'review-write',
    component: () => import('../views/ReviewWriteView.vue'),
    meta: { requiresAuth: true },
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// 최초 랜딩 시 세션 갱신 검증 여부 플래그
let isInitialAuthChecked = false;

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore();

  // 최초 1회에 한해 세션 복구 수행 (스토리지에 기존 토큰 흔적이 있고 아직 검사하지 않은 경우)
  if (!isInitialAuthChecked) {
    isInitialAuthChecked = true;
    const hasRefreshToken = localStorage.getItem('refresh_token');
    const hasAccessToken = localStorage.getItem('access_token');
    if ((hasRefreshToken && hasRefreshToken !== 'null' && hasRefreshToken !== 'undefined') || hasAccessToken) {
      try {
        await authStore.checkAuth();
      } catch (e) {
        console.warn('[Router] Initial session restore failed:', e);
        authStore.clearAllAuth();
      }
    }
  }

  // 보호 구역 접근 제어
  if (to.meta.requiresAuth) {
    if (!authStore.isAuthenticated) {
      console.warn(`[Router] Protected route ${to.path} requires login. Redirecting to /login.`);
      return next('/login');
    }
  }

  // 이미 로그인된 상태에서 로그인 페이지 접근 시 홈으로 리다이렉트
  if (to.path === '/login' && authStore.isAuthenticated) {
    return next('/');
  }

  next();
});

export default router;
