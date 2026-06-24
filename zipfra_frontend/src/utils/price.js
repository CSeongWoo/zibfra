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

// 동일좌표 클러스터(같은 단지, 층만 다른 매물)의 가격 범위 라벨(최소~최대). 단건이면 markerPriceLabel 그대로.
// 대표가격 = COALESCE(매매가, 보증금). 최소·최대가 같으면 단일 표기. 예: "25억~26억".
export function clusterPriceLabel(items) {
  if (!items || items.length === 0) return '-';
  if (items.length === 1) return markerPriceLabel(items[0]);
  const prices = items.map((it) => it.dealAmount ?? it.deposit ?? 0);
  const min = Math.min(...prices);
  const max = Math.max(...prices);
  return min === max ? formatManwon(min) : `${formatManwon(min)}~${formatManwon(max)}`;
}

// ── 가격 필터 비균등 슬라이더(기능2) ──────────────────────────
// 만원 단위. 오른쪽으로 갈수록 점프가 커지는 스텝 배열. 슬라이더는 '인덱스'를 다룬다(눈금 균등, 값 점프).
// 전부 천만원의 배수라 라벨에 0.5억 같은 소수 억이 안 나온다.
export const PRICE_STOPS = [
  0, 1000, 2000, 3000, 5000, 7000, 10000, 15000, 20000, 30000,
  50000, 70000, 100000, 200000, 300000, 500000, 1000000,
];
export const PRICE_MAX_IDX = PRICE_STOPS.length - 1;

// 슬라이더 인덱스 → 가격(만원).
export function idxToPrice(idx) {
  return PRICE_STOPS[Math.max(0, Math.min(PRICE_MAX_IDX, idx))];
}

// 가격(만원) → 가장 가까운(이하 최대) 스텝 인덱스. null → 0.
export function priceToIdx(value) {
  if (value == null) return 0;
  let best = 0;
  for (let i = 0; i < PRICE_STOPS.length; i += 1) {
    if (PRICE_STOPS[i] <= value) best = i;
    else break;
  }
  return best;
}

// 천만 단위 라벨. 0 → '0'. 예: 5000→"5천만", 15000→"1억5천", 100000→"10억".
export function formatPriceStop(manwon) {
  if (manwon == null || manwon <= 0) return '0';
  const eok = Math.floor(manwon / 10000);
  const cheonman = (manwon % 10000) / 1000; // 1,000만 단위
  if (eok === 0) return `${cheonman}천만`;
  if (cheonman === 0) return `${eok}억`;
  return `${eok}억${cheonman}천`;
}
