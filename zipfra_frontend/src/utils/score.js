/**
 * 매물별 점수 표시 정규화 (AGENTS.md §5.1).
 * 백엔드는 그룹 base(감쇠합산, 0~)만 내려준다. 0~100 표시 점수는 여기서 변환한다.
 * 페르소나 가중치(#6)는 weights 인자로 주입 — 슬라이더 조정 시 실시간 재계산.
 *
 * 그룹별 base 스케일이 크게 다르므로(교통 ~40[버스 적재 후], 편의 ~57) **그룹마다 다른
 * 계수(scale)로 0~100 정규화한 뒤 페르소나 가중평균**한다. 단일 SCALE 가중평균은 편의/상업이
 * 지배해 페르소나(교통 강조 등)가 역효과를 내므로 폐기. scale 값은 실데이터 평균(각 그룹
 * 평균 매물이 ~60~75점이 되도록) 기준 튜닝.
 * (2026-06-25) transit 모델 재정비(분석: PROJECT_EXPLAINED §9.5):
 *   ① 버스 적재로 base 폭증 → scale 100→1.5. ② 교통 감쇠 플랫 400m→300m(역세권 엄격).
 *   ③ "지하철 코앞인데 버스 많은 곳에 밀린다" → 백엔드에서 지하철 ×40 가중 + 버스 상한 15 + 버스 중복제거.
 *   이로써 transit_base 최대 55(=40지하철+15버스), median ~20. scale 1.5→1.8 (max 55×1.8=99, 포화 없이 0~99).
 *   강남 역세권 99점 vs 지하철 먼 곳 34점으로 변별.
 */

/** 그룹 메타. short=목록 미니바 라벨, long=페르소나/토글 라벨, color=색, scale=0~100 정규화 계수. */
export const GROUPS = [
  { key: 'transit', icon: '🚇', short: '교통', long: '지하철 / 버스', color: '#3b82f6', scale: 1.8 },
  { key: 'education', icon: '🏫', short: '학군', long: '학교 / 학원', color: '#10b981', scale: 4 },
  { key: 'commerce', icon: '🛍', short: '상업', long: '상업시설', color: '#f59e0b', scale: 2 },
  { key: 'convenience', icon: '🏪', short: '편의', long: '편의시설', color: '#8b5cf6', scale: 1.3 },
];

const SCALE_BY_KEY = Object.fromEntries(GROUPS.map((g) => [g.key, g.scale]));

/** 그룹 base → 0~100 (그룹별 scale 적용, 미니바·종합 공용). */
export function groupScore(base, groupKey) {
  const scale = SCALE_BY_KEY[groupKey] ?? 1;
  return Math.min(100, Math.round((base ?? 0) * scale));
}

/** 균등 가중치(페르소나 미설정 기본). */
export const EQUAL_WEIGHTS = { transit: 1, education: 1, commerce: 1, convenience: 1 };

/**
 * 페르소나 프리셋(#6, §5 그룹 가중치 4개, 0~100 스케일). 와이어3 좌측 페르소나 버튼.
 */
export const PERSONA_PRESETS = [
  { key: 'commuter', icon: '🚇', label: '출퇴근족', weights: { transit: 100, education: 20, commerce: 50, convenience: 50 } },
  { key: 'parent', icon: '🏫', label: '학부모', weights: { transit: 50, education: 100, commerce: 40, convenience: 70 } },
  { key: 'single', icon: '🏠', label: '1인가구', weights: { transit: 70, education: 10, commerce: 100, convenience: 90 } },
];

/** 페르소나 기본값(중립 균형 50). */
export const DEFAULT_PERSONA = { transit: 50, education: 50, commerce: 50, convenience: 50 };

/**
 * 매물 종합 점수 0~100 = 그룹별 0~100 정규화(groupScore) 의 페르소나 가중평균.
 * @param {object} m MAP-01 마커(transitBase/educationBase/commerceBase/convenienceBase)
 * @param {object} weights {transit,education,commerce,convenience}
 */
export function totalScore(m, weights = EQUAL_WEIGHTS) {
  const baseKey = { transit: 'transitBase', education: 'educationBase', commerce: 'commerceBase', convenience: 'convenienceBase' };
  let acc = 0;
  let wsum = 0;
  for (const g of GROUPS) {
    const w = weights[g.key] ?? 0;
    acc += groupScore(m[baseKey[g.key]], g.key) * w;
    wsum += w;
  }
  return wsum > 0 ? Math.round(acc / wsum) : 0;
}

/** 점수 색상(와이어3 범례: 90+/75-89/60-74/<60). */
export function scoreColor(score) {
  if (score >= 90) return '#10b981'; // emerald
  if (score >= 75) return '#3b82f6'; // blue
  if (score >= 60) return '#f59e0b'; // amber
  return '#ef4444'; // rose
}
