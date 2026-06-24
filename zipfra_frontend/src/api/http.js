import axios from 'axios';

/**
 * 공통 Axios 인스턴스 (Phase 1 Dev B — Axios 공통 인터셉터).
 *
 * - baseURL `/api/v1`: Vite dev 프록시(→ :8080)를 경유한다 (vite.config.js).
 * - 인증 토큰은 PR-2 auth 스토어가 채운다. 아직 없는 스토어에 하드 의존하지
 *   않도록 "토큰 공급자 주입" 방식으로 디커플링한다.
 */

// PR-2(auth 스토어)가 setAccessTokenProvider로 주입. 기본은 토큰 없음.
let accessTokenProvider = () => null;

/** auth 스토어 등에서 토큰을 읽어올 함수를 등록한다. */
export function setAccessTokenProvider(fn) {
  accessTokenProvider = fn;
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
  // 서버 측 AI p95 < 10초 + Fallback(§6)을 끊지 않도록 여유 있게.
  timeout: 15000,
});

// 요청: 토큰이 있으면 Authorization 헤더 주입 (Protected 엔드포인트용).
http.interceptors.request.use((config) => {
  const token = accessTokenProvider();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  // FormData 전송 시, 기본 JSON Content-Type 제거 (브라우저가 multipart/form-data 및 boundary 자동 설정)
  if (config.data instanceof FormData) {
    delete config.headers['Content-Type'];
  }
  return config;
});

// 토큰 갱신 대기열 및 진행 상태 관리
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

// 응답: 에러를 공통 에러 계약(§8.3 `{ error, message, timestamp }`)으로 정규화.
http.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const data = error.response?.data;
    const status = error.response?.status;

    // 401 Unauthorized 에러 수신 시 RTR 재발급 시도 (재발급 요청 자체인 경우는 예외 처리하여 무한 루프 방지)
    if (status === 401 && !originalRequest._retry && !originalRequest.url?.includes('/auth/refresh')) {
      const hasRefreshToken = localStorage.getItem('refresh_token');
      if (!hasRefreshToken || hasRefreshToken === 'null' || hasRefreshToken === 'undefined') {
        const { useAuthStore } = await import('../stores/auth');
        const authStore = useAuthStore();
        authStore.clearAllAuth();
        const { default: router } = await import('../router');
        router.push('/login');
        return Promise.reject({
          error: 'REFRESH_TOKEN_MISSING',
          message: 'Refresh Token이 없어 세션을 복구할 수 없습니다. 다시 로그인해주세요.',
          timestamp: null,
          status: 401,
        });
      }

      if (isRefreshing) {
        // 이미 갱신 중인 경우 대기열에 추가하고 대기
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return http(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const { refresh } = await import('./auth');
        const { useAuthStore } = await import('../stores/auth');
        const authStore = useAuthStore();

        // 갱신 API 호출 (HttpOnly 쿠키에 실린 Refresh Token 이용)
        const refreshResponse = await refresh();
        const newAccessToken = refreshResponse.data.accessToken;

        // 스토어 업데이트 및 대기열 처리
        authStore.setAccessToken(newAccessToken);
        processQueue(null, newAccessToken);
        isRefreshing = false;

        // 원본 요청 수정 후 재실행
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return http(originalRequest);
      } catch (refreshError) {
        // 갱신 실패 시 대기열의 요청들 전부 실패 처리하고 로그아웃 처리
        processQueue(refreshError, null);
        isRefreshing = false;
        
        const { useAuthStore } = await import('../stores/auth');
        const authStore = useAuthStore();
        authStore.clearAllAuth();

        const { default: router } = await import('../router');
        router.push('/login');

        return Promise.reject({
          error: refreshError.error ?? 'REFRESH_TOKEN_INVALID',
          message: refreshError.message ?? '로그인 세션이 만료되었습니다. 다시 로그인해주세요.',
          timestamp: refreshError.timestamp ?? null,
          status: refreshError.status ?? 401,
        });
      }
    }

    return Promise.reject({
      error: data?.error ?? 'NETWORK_ERROR',
      message: data?.message ?? error.message,
      timestamp: data?.timestamp ?? null,
      status: status ?? 0,
    });
  }
);

export default http;
