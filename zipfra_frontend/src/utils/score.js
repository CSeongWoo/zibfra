/**
 * 매물별 점수 표시 정규화 (AGENTS.md §5.1).
 * 백엔드는 그룹 base(감쇠합산, 0~)만 내려준다. 0~100 표시 점수는 여기서 변환한다.
 * 페르소나 가중치(#6)는 weights 인자로 주입 — 슬라이더 조정 시 실시간 재계산.
 */

// 데모 튜닝 상수: base(≈0~7) → 0~100. base 5.5 ≈ 99점.
const SCALE = 18;

/** 그룹 base → 0~100 (미니바용). */
export function groupScore(base) {
  return Math.min(100, Math.round((base ?? 0) * SCALE));
}

/** 균등 가중치(페르소나 미설정 기본). 가중평균이라 절대값은 무의미(비율만 반영). */
export const EQUAL_WEIGHTS = { transit: 1, education: 1, commerce: 1, convenience: 1 };

/**
 * 페르소나 프리셋(#6, §5 그룹 가중치 4개, 0~100 스케일). 와이어3 좌측 페르소나 버튼.
 * 가중평균이라 스케일(0~100/0~1)은 점수에 무관 — 표시 직관성 위해 0~100.
 */
export const PERSONA_PRESETS = [
  { key: 'commuter', icon: '🚇', label: '출퇴근족', weights: { transit: 100, education: 20, commerce: 50, convenience: 50 } },
  { key: 'parent', icon: '🏫', label: '학부모', weights: { transit: 50, education: 100, commerce: 40, convenience: 70 } },
  { key: 'single', icon: '🏠', label: '1인가구', weights: { transit: 70, education: 10, commerce: 100, convenience: 90 } },
];

/** 페르소나 기본값(중립 균형 50). store 초기값 — 어느 프리셋에도 치우치지 않음. */
export const DEFAULT_PERSONA = { transit: 50, education: 50, commerce: 50, convenience: 50 };

/**
 * 그룹 메타. short=목록 미니바 라벨, long=페르소나/인프라토글 라벨, color=바·슬라이더 색.
 */
export const GROUPS = [
  { key: 'transit', icon: '🚇', short: '교통', long: '지하철 / 버스', color: '#3b82f6' },
  { key: 'education', icon: '🏫', short: '학군', long: '학교 / 학원', color: '#10b981' },
  { key: 'commerce', icon: '🛍', short: '상업', long: '상업시설', color: '#f59e0b' },
  { key: 'convenience', icon: '🏪', short: '편의', long: '편의시설', color: '#8b5cf6' },
];

/**
 * 매물 종합 점수 0~100 = clamp(Σ(base×weight)/Σweight × SCALE).
 * @param {object} m MAP-01 마커(transitBase/educationBase/commerceBase/convenienceBase)
 * @param {object} weights {transit,education,commerce,convenience}
 */
export function totalScore(m, weights = EQUAL_WEIGHTS) {
  const bases = {
    transit: m.transitBase ?? 0,
    education: m.educationBase ?? 0,
    commerce: m.commerceBase ?? 0,
    convenience: m.convenienceBase ?? 0,
  };
  let acc = 0;
  let wsum = 0;
  for (const g of ['transit', 'education', 'commerce', 'convenience']) {
    const w = weights[g] ?? 0;
    acc += bases[g] * w;
    wsum += w;
  }
  const avgBase = wsum > 0 ? acc / wsum : 0;
  return Math.min(100, Math.round(avgBase * SCALE));
}

/** 점수 색상(와이어3 범례: 90+/75-89/60-74/<60). */
export function scoreColor(score) {
  if (score >= 90) return '#10b981'; // emerald
  if (score >= 75) return '#3b82f6'; // blue
  if (score >= 60) return '#f59e0b'; // amber
  return '#ef4444'; // rose
}
