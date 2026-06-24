import http from './http';

/**
 * AUTH-01 회원가입
 */
export function signup(signupData) {
  return http.post('/auth/signup', signupData);
}

/**
 * AUTH-02 로그인
 */
export function login(loginData) {
  return http.post('/auth/login', loginData);
}

/**
 * AUTH-03 Refresh Token 재발급 (RTR)
 */
export function refresh() {
  return http.post('/auth/refresh');
}

/**
 * AUTH-04 로그아웃
 */
export function logout() {
  return http.post('/auth/logout');
}

/**
 * USER-01 사용자 프로필 조회
 */
export function getProfile() {
  return http.get('/users/me');
}

/**
 * AUTH-05 비밀번호 찾기 (임시 비밀번호 발급)
 */
export function forgotPassword(forgotData) {
  return http.post('/auth/forgot-password', forgotData);
}
