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

  // 보호 구역 접근 제어
  if (to.meta.requiresAuth) {
    // 최초 1회에 한해 세션 복구 수행 (아직 복구 시도를 안 했고 미인증 상태인 경우만)
    if (!isInitialAuthChecked) {
      isInitialAuthChecked = true;
      if (!authStore.isAuthenticated) {
        const hasRefreshToken = localStorage.getItem('refresh_token');
        if (hasRefreshToken && hasRefreshToken !== 'null' && hasRefreshToken !== 'undefined') {
          try {
            await authStore.checkAuth();
          } catch (e) {
            console.warn('[Router] Initial auth restore failed on protected route:', e);
          }
        }
      }
    }

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
