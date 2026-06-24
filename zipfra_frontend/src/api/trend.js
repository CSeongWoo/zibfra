import http from './http';

/**
 * 실거래가 추이 (GET /api/v1/map/property/{id}/trend, Public).
 * 그 매물 단지·거래유형의 월별 평균가 시계열.
 * @returns {Promise<Array<{dealYm:string, avgAmount:number, dealCount:number}>>}
 */
export async function fetchPriceTrend(propertyId) {
  const res = await http.get(`/map/property/${propertyId}/trend`);
  return res.data;
}
