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
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
  // 서버 측 AI p95 < 10초 + Fallback(§6)을 끊지 않도록 여유 있게.
  timeout: 15000,
});

// 요청: 토큰이 있으면 Authorization 헤더 주입 (Protected 엔드포인트용).
http.interceptors.request.use((config) => {
  const token = accessTokenProvider();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 응답: 에러를 공통 에러 계약(§8.3 `{ error, message, timestamp }`)으로 정규화.
http.interceptors.response.use(
  (response) => response,
  (error) => {
    const data = error.response?.data;
    // TODO(Phase 2 §10 RTR): 401 TOKEN_EXPIRED 시 /auth/refresh로 재발급 후
    // 원요청 1회 재시도하는 훅을 여기 추가한다.
    return Promise.reject({
      error: data?.error ?? 'NETWORK_ERROR',
      message: data?.message ?? error.message,
      timestamp: data?.timestamp ?? null,
      status: error.response?.status ?? 0,
    });
  }
);

export default http;
