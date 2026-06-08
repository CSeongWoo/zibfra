import http from './http';

/**
 * MAP-01 지도 마커 조회 (GET /api/v1/map/markers, Public).
 *
 * 서버가 zoom 으로 전략을 재판정(§7.1)해 응답 형태가 갈린다:
 * - DETAIL: { strategy, markers:[{id,lat,lng,...}], page, size, totalCount, hasNext, bboxOversized }
 * - SUMMARY: { strategy, regions:[{regionCd,regionName,lat,lng,...}], bboxOversized }
 *
 * @param {{ bbox: string, zoom: number, page?: number, size?: number }} params
 *   bbox = "minLng,minLat,maxLng,maxLat" (EPSG:4326, §8 공통 규약)
 * @returns {Promise<object>} 위 응답 객체
 */
export async function fetchMarkers({ bbox, zoom, page, size }) {
  const res = await http.get('/map/markers', { params: { bbox, zoom, page, size } });
  return res.data;
}
