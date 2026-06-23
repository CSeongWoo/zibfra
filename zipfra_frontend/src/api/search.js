import http from './http';

/**
 * MAP-03 단지 검색 (GET /api/v1/map/search, Public).
 * 우리 DB(real_estate_sales)의 단지명 부분일치 → 단지 단위 집계 목록.
 * 지역(시·구·동) 검색은 카카오 addressSearch 가 담당(이 함수 아님).
 *
 * @param {string} q 검색어(2자 미만이면 서버가 빈 배열)
 * @param {number} [limit=20] 결과 상한(max 50)
 * @returns {Promise<Array<{id,buildingName,lat,lng,propertyType,dealCount,minArea,maxArea,buildYear}>>}
 */
export async function searchProperties(q, limit = 20) {
  const res = await http.get('/map/search', { params: { q, limit } });
  return res.data;
}
