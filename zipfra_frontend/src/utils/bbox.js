/**
 * BBox 좌표 직렬화 유틸 (Phase 1 Dev B — BBox 좌표계 파라미터 연동).
 *
 * MAP-01 등 공간 API는 bbox를 EPSG:4326, 순서 고정
 * `"minLng,minLat,maxLng,maxLat"`(lon=경도, lat=위도)로 받는다(§8 공통 규약).
 * lat/lng 자리는 절대 뒤바꾸지 않는다.
 */

const round6 = (n) => Math.round(n * 1e6) / 1e6;

/** 순수 함수: 네 좌표 → bbox 문자열. */
export function formatBbox({ minLng, minLat, maxLng, maxLat }) {
  return [minLng, minLat, maxLng, maxLat].map(round6).join(',');
}

/** 카카오 LatLngBounds(getSouthWest/getNorthEast) → bbox 문자열. */
export function kakaoBoundsToBbox(bounds) {
  const sw = bounds.getSouthWest();
  const ne = bounds.getNorthEast();
  return formatBbox({
    minLng: sw.getLng(),
    minLat: sw.getLat(),
    maxLng: ne.getLng(),
    maxLat: ne.getLat(),
  });
}
