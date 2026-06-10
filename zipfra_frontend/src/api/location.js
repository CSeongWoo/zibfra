import http from './http';

/**
 * LOC-01 입지 점수 (POST /api/v1/location/score, Protected).
 * http 인스턴스가 Bearer 토큰을 자동 첨부한다(미로그인 시 401).
 *
 * @param {{ lon:number, lat:number, radiusMeters?:number, weights?:Record<string,number> }} body
 * @returns {Promise<object>} { lon, lat, radiusMeters, finalScore, breakdown }
 */
export async function fetchLocationScore(body) {
  const res = await http.post('/location/score', body);
  return res.data;
}
