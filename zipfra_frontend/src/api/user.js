import http from './http';

/**
 * USER-02 본인 정보(닉네임, 비밀번호) 수정
 */
export function updateMyInfo(updateData) {
  return http.patch('/users/me', updateData);
}

/**
 * USER-03 회원 탈퇴
 */
export function deactivateMe() {
  return http.delete('/users/me');
}

/**
 * USER-04 특정 닉네임으로 유저 검색
 */
export function searchUsers(nickname) {
  return http.get('/users/search', { params: { nickname } });
}
