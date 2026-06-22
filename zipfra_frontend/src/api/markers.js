import http from './http';

/**
 * MAP-01 지도 마커 조회 (GET /api/v1/map/markers, Public).
 *
 * 서버가 zoom 으로 전략을 재판정(§7.1)해 응답 형태가 갈린다:
 * - DETAIL: { strategy, markers:[{id,lat,lng,...}], page, size, totalCount, hasNext, bboxOversized }
 * - SUMMARY: { strategy, regions:[{regionCd,regionName,lat,lng,...}], bboxOversized }
 *
 * @param {{ bbox: string, zoom: number, page?: number, size?: number,
 *           dealType?: string[], propertyType?: string[], priceMin?: number, priceMax?: number }} params
 *   bbox = "minLng,minLat,maxLng,maxLat" (EPSG:4326, §8 공통 규약)
 *   검색 필터(§8.1, DETAIL 한정): dealType/propertyType 는 다중선택 배열(콤마 join 전송),
 *   priceMin/priceMax. null·빈배열·빈값은 전송 생략(서버 INVALID_PARAM 회피).
 * @returns {Promise<object>} 위 응답 객체
 */
export async function fetchMarkers({ bbox, zoom, page, size, dealType, propertyType, priceMin, priceMax }) {
  // 다중선택 배열 → 콤마 문자열(빈 배열은 undefined 로 떨궈 전송 생략).
  const csv = (v) => (Array.isArray(v) ? (v.length ? v.join(',') : undefined) : v);
  const params = {
    bbox, zoom, page, size,
    dealType: csv(dealType), propertyType: csv(propertyType), priceMin, priceMax,
  };
  // null/undefined/'' 은 쿼리스트링에서 제외(서버 INVALID_PARAM 회피)
  Object.keys(params).forEach((k) => {
    if (params[k] === null || params[k] === undefined || params[k] === '') delete params[k];
  });
  const res = await http.get('/map/markers', { params });
  return res.data;
}
