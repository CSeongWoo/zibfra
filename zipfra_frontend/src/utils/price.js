// 매물 가격·거래유형 포맷 공용 유틸. 지도 마커(KakaoMap)와 목록 패널(PropertyListPanel)이 공유한다.
// 금액 단위는 만원(서버 §8.1 계약). 예: 85000 → "8억 5,000".

export const DEAL = { SALE: '매매', JEONSE: '전세', WOLSE: '월세' };
export const TYPE = { APT: '아파트', OFFICETEL: '오피스텔', ROW_HOUSE: '빌라' };

export const dealLabel = (v) => DEAL[v] || v || '-';
export const typeLabel = (v) => TYPE[v] || v || '-';

// 만원 단위 금액 → 1억 이상은 "N.N억"(소수1자리, 정수면 "N억"), 1억 미만은 "M,MMM만".
// 예: 85000→"8.5억", 32000→"3.2억", 80000→"8억", 2000→"2,000만".
export function formatManwon(amount) {
  if (amount == null) return '-';
  if (amount >= 10000) {
    const eok = Math.round((amount / 10000) * 10) / 10; // 소수 1자리 반올림
    return `${Number.isInteger(eok) ? eok : eok.toFixed(1)}억`;
  }
  return `${amount.toLocaleString()}만`;
}

// 거래유형별 대표 가격 문자열. 매매=거래금액, 전세=보증금, 월세=보증금/월세.
export function priceLabel(m) {
  if (m.dealType === 'SALE') return formatManwon(m.dealAmount);
  if (m.dealType === 'JEONSE') return formatManwon(m.deposit);
  if (m.dealType === 'WOLSE') return `${formatManwon(m.deposit)} / 월 ${m.monthlyRent ?? '-'}`;
  return formatManwon(m.dealAmount ?? m.deposit);
}

// 마커용 짧은 가격: 매매는 가격만, 그 외는 유형 접두(전세 5억) — 동일 숫자라도 거래유형을 구분.
export function markerPriceLabel(m) {
  if (m.dealType === 'SALE' || m.dealType == null) return priceLabel(m);
  return `${dealLabel(m.dealType)} ${priceLabel(m)}`;
}
