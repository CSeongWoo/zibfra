import http from './http';

/**
 * MAP-02 POI 오버레이 조회 (GET /api/v1/map/pois, Public).
 * @param {{ bbox: string, groups: string }} p groups=transit,commerce 형태 CSV
 * @returns {Promise<Array<{lat:number,lng:number,category:string,group:string,name:string}>>}
 */
export async function fetchPois({ bbox, groups }) {
  const res = await http.get('/map/pois', { params: { bbox, groups } });
  return res.data;
}
