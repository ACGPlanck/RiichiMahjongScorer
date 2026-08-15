// ============================================================================
// 立直麻将 算分引擎 —— Node.js 镜像实现（用于验证算法，与 Kotlin 引擎一一对应）
// Riichi Mahjong scoring engine (JS mirror, for verification only)
// ============================================================================
'use strict';

// ---------- 牌 Tile ----------
export const SUIT_MAN = 0, SUIT_PIN = 1, SUIT_SOU = 2, SUIT_HONOR = 3;

export function tileId(suit, rank) { return suit * 9 + (rank - 1); }
export function suitOf(id) { return Math.floor(id / 9); }
export function rankOf(id) { return (id % 9) + 1; }
export function isHonor(id) { return suitOf(id) === SUIT_HONOR; }
export function isTerminal(id) { const s = suitOf(id); return s < 3 && (rankOf(id) === 1 || rankOf(id) === 9); }
export function isTerminalOrHonor(id) { return isTerminal(id) || isHonor(id); }
export function isSimple(id) { return !isHonor(id) && rankOf(id) >= 2 && rankOf(id) <= 8; }

// 宝牌指示牌 -> 宝牌 (dora indicator -> dora tile)
export function doraOf(id) {
  const s = suitOf(id), r = rankOf(id);
  if (s === SUIT_HONOR) {
    if (r === 1) return tileId(3, 2);
    if (r === 2) return tileId(3, 3);
    if (r === 3) return tileId(3, 4);
    if (r === 4) return tileId(3, 1);
    if (r === 5) return tileId(3, 6);
    if (r === 6) return tileId(3, 7);
    return tileId(3, 5);
  }
  return tileId(s, r === 9 ? 1 : r + 1);
}

export function countsOf(tiles) {
  const c = new Array(34).fill(0);
  for (const id of tiles) c[id]++;
  return c;
}

// ---------- 手牌配置 Hand ----------
// meld type: 'chi' | 'pon' | 'kanOpen' | 'kanClosed'
// hand.tiles: 门前(暗)牌，包含和牌张；总张数 = 14 - 3*(非杠副露数) - 3*杠数
export function makeHand(opt) {
  return {
    tiles: opt.tiles,          // concealed tiles incl. winning tile
    melds: opt.melds || [],    // [{type, tiles:[4 ids for kan, 3 otherwise]}]
    winId: opt.winId,
    isTsumo: !!opt.isTsumo,
    isDealer: !!opt.isDealer,
    roundWind: opt.roundWind,  // id
    seatWind: opt.seatWind,    // id
    riichi: !!opt.riichi,
    doubleRiichi: !!opt.doubleRiichi,
    ippatsu: !!opt.ippatsu,
    chankan: !!opt.chankan,      // 枪杠和牌（荣和）
    haitei: !!opt.haitei,        // 海底捞月（自摸最后一张）
    houtei: !!opt.houtei,        // 河底摸鱼（荣和最后一张弃牌）
    doraIndicators: opt.doraIndicators || [],
    uraDoraIndicators: opt.uraDoraIndicators || [],
    firstDraw: !!opt.firstDraw,
    countedYakuman: opt.countedYakuman !== false,
    doubleYakuman: opt.doubleYakuman !== false,
  };
}

export function menzenOf(h) {
  return h.melds.every(m => m.type === 'kanClosed');
}

export function allTilesOf(h) {
  const out = h.tiles.slice();
  for (const m of h.melds) for (const id of m.tiles) out.push(id);
  return out;
}

export function isYakuhai(id, h) {
  return id === h.roundWind || id === h.seatWind || id === tileId(3, 5) || id === tileId(3, 6) || id === tileId(3, 7);
}

// ---------- 手牌分解 Decomposer ----------
export function decomposeHand(counts, groupsNeeded) {
  const results = [];
  const rec = (c, remainingGroups, remainingPairs, groups, pair) => {
    let i = -1;
    for (let k = 0; k < 34; k++) { if (c[k] > 0) { i = k; break; } }
    if (i === -1) {
      if (remainingGroups === 0 && remainingPairs === 0) results.push({ groups: groups.slice(), pair });
      return;
    }
    if (remainingPairs > 0 && c[i] >= 2) {
      c[i] -= 2;
      rec(c, remainingGroups, remainingPairs - 1, groups, i);
      c[i] += 2;
    }
    if (remainingGroups > 0 && c[i] >= 3) {
      c[i] -= 3;
      groups.push({ kind: 'tri', tiles: [i, i, i] });
      rec(c, remainingGroups - 1, remainingPairs, groups, pair);
      groups.pop();
      c[i] += 3;
    }
    if (remainingGroups > 0 && suitOf(i) < 3 && rankOf(i) <= 7 && c[i + 1] > 0 && c[i + 2] > 0) {
      c[i]--; c[i + 1]--; c[i + 2]--;
      groups.push({ kind: 'seq', tiles: [i, i + 1, i + 2] });
      rec(c, remainingGroups - 1, remainingPairs, groups, pair);
      groups.pop();
      c[i]++; c[i + 1]++; c[i + 2]++;
    }
  };
  rec(counts.slice(), groupsNeeded, 1, [], -1);
  return results;
}

// 该分解中，和牌张的所有可能位置
function placementsOf(decomp, winId) {
  const out = [];
  if (decomp.pair === winId) out.push({ inPair: true, groupIdx: -1, pos: -1 });
  decomp.groups.forEach((g, idx) => {
    g.tiles.forEach((t, pos) => { if (t === winId) out.push({ inPair: false, groupIdx: idx, pos }); });
  });
  return out;
}

// 等待类型: 'ryanmen'(0符) | 'kanchan'(2符) | 'penchan'(2符) | 'tanki'(2符) | 'shanpon'(0符)
export function waitType(h, decomp, placement) {
  if (placement.inPair) return 'tanki';
  const g = decomp.groups[placement.groupIdx];
  if (g.kind === 'tri') return 'shanpon';
  const other = g.tiles.filter((x, i) => i !== placement.pos).sort((a, b) => a - b);
  const [a, b] = other;
  if (b === a + 1) {
    const r = rankOf(a);
    return (r === 1 || r === 8) ? 'penchan' : 'ryanmen';
  }
  return 'kanchan';
}

// ---------- 番型判定 Yaku ----------
const HAKU = tileId(3, 5), HATSU = tileId(3, 6), CHUN = tileId(3, 7);
const DRAGONS = [HAKU, HATSU, CHUN];

export function detectYaku(h, decomp, placement, sevenPairs, kokushi) {
  const yaku = [];       // [name, han]
  const yakuman = [];    // names
  const menzen = menzenOf(h);
  const all = allTilesOf(h);
  const allCounts = countsOf(all);
  const concealedCounts = countsOf(h.tiles);
  const kanCount = h.melds.filter(m => m.type === 'kanOpen' || m.type === 'kanClosed').length;

  // ===== 役满（不依赖分解）=====
  if (kokushi) {
    if (h.doubleYakuman && h.winId === kokushi.dup) yakuman.push('国士无双十三面', '国士无双十三面');
    else yakuman.push('国士无双');
    return { yaku, yakuman, fu: 0 };
  }
  if (sevenPairs) {
    // 字一色 / 清老头 / 绿一色 七对子
    if (all.every(isHonor)) yakuman.push('字一色');
    if (all.every(isTerminal)) yakuman.push('清老头');
    if (all.every(id => id === tileId(2, 2) || id === tileId(2, 3) || id === tileId(2, 4) ||
      id === tileId(2, 6) || id === tileId(2, 8) || id === HATSU) && allCounts[HATSU] > 0) yakuman.push('绿一色');
    if (yakuman.length > 0) return { yaku, yakuman, fu: 0 };
    yaku.push(['七对子', 2]);
    // 附加役（七对子不计算役牌/一杯口/对对和等）
    if (all.every(isSimple)) yaku.push(['断幺九', 1]);
    if (all.every(isTerminalOrHonor)) yaku.push(['混老头', 2]);
    const suits = new Set(all.map(suitOf));
    if (suits.size === 1 && !suits.has(SUIT_HONOR)) yaku.push(['清一色', menzen ? 6 : 5]);
    else if (suits.size <= 2 && suits.has(SUIT_HONOR) && suits.size === 2) yaku.push(['混一色', menzen ? 3 : 2]);
    if (menzen && h.isTsumo) yaku.push(['门前清自摸和', 1]);
    return { yaku, yakuman, fu: 25 };
  }

  // 九莲宝灯（严格型）
  const concealedTriCount = decomp.groups.filter(g => g.kind === 'tri').length +
    h.melds.filter(m => m.type === 'kanClosed').length;
  const dragonTriCount = DRAGONS.filter(d => allCounts[d] >= 3).length;
  const groupsAll = decomp.groups.concat(h.melds.map(m => ({ kind: m.type === 'chi' ? 'seq' : 'tri', tiles: m.tiles.slice(0, 3) })));

  const nineGates = detectNineGates(concealedCounts, h);
  if (nineGates) {
    if (nineGates.pure && h.doubleYakuman) yakuman.push('纯正九莲宝灯', '纯正九莲宝灯');
    else yakuman.push('九莲宝灯');
    return { yaku, yakuman, fu: 0 };
  }

  // ===== 四杠子 =====
  if (kanCount === 4) { yakuman.push('四杠子'); return { yaku, yakuman, fu: 0 }; }
  // 四暗刻（含暗杠）：自摸可，荣和仅限单骑
  if (concealedTriCount === 4 && (h.isTsumo || placement.inPair)) {
    if (h.doubleYakuman && placement.inPair) yakuman.push('四暗刻单骑', '四暗刻单骑');
    else yakuman.push('四暗刻');
    return { yaku, yakuman, fu: 0 };
  }
  // 大三元
  if (dragonTriCount === 3) { yakuman.push('大三元'); return { yaku, yakuman, fu: 0 }; }
  // 字一色（对对形）
  if (all.every(isHonor)) { yakuman.push('字一色'); return { yaku, yakuman, fu: 0 }; }
  // 绿一色
  const isGreen = id => id === tileId(2, 2) || id === tileId(2, 3) || id === tileId(2, 4) ||
    id === tileId(2, 6) || id === tileId(2, 8) || id === HATSU;
  if (all.every(isGreen) && allCounts[HATSU] > 0) { yakuman.push('绿一色'); return { yaku, yakuman, fu: 0 }; }
  // 清老头
  if (all.every(isTerminal)) { yakuman.push('清老头'); return { yaku, yakuman, fu: 0 }; }

  // ===== 通常役 =====
  // 立直系
  if (menzen) {
    if (h.doubleRiichi) yaku.push(['双立直', 2]);
    else if (h.riichi) yaku.push(['立直', 1]);
    if (h.ippatsu && (h.riichi || h.doubleRiichi)) yaku.push(['一发', 1]);
    if (h.firstDraw) {
      if (h.isDealer && h.isTsumo) yakuman.push('天和');
      else if (!h.isDealer && h.isTsumo) yakuman.push('地和');
      if (yakuman.length > 0) return { yaku: [], yakuman, fu: 0 };
    }
    if (h.isTsumo) yaku.push(['门前清自摸和', 1]);
  }

  // 平和
  const allSeq = decomp.groups.every(g => g.kind === 'seq') && h.melds.length === 0;
  if (allSeq && !isYakuhai(decomp.pair, h) && waitType(h, decomp, placement) === 'ryanmen') {
    yaku.push(['平和', 1]);
  }

  // 断幺九
  if (all.every(isSimple)) yaku.push(['断幺九', 1]);

  // 特殊和牌方式（需用户输入的牌局状况）
  if (h.chankan && !h.isTsumo) yaku.push(['枪杠和', 1]);
  if (h.haitei && h.isTsumo) yaku.push(['海底捞月', 1]);
  if (h.houtei && !h.isTsumo) yaku.push(['河底摸鱼', 1]);

  // 役牌（场风/自风/三元 的刻子或雀头，各1番）
  const yakuhaiCandidates = [];
  if (allCounts[h.roundWind] >= 3 || (decomp.pair === h.roundWind)) yakuhaiCandidates.push(h.roundWind);
  if (allCounts[h.seatWind] >= 3 || (decomp.pair === h.seatWind)) yakuhaiCandidates.push(h.seatWind);
  for (const d of DRAGONS) if (allCounts[d] >= 3 || decomp.pair === d) yakuhaiCandidates.push(d);
  for (const t of yakuhaiCandidates) yaku.push([`役牌·${nameOf(t)}`, 1]);

  // 一杯口 / 二杯口（门前）
  if (menzen && h.melds.length === 0) {
    const seqKeys = decomp.groups.filter(g => g.kind === 'seq').map(g => g.tiles.join(','));
    const dupCount = seqKeys.filter((k, i) => seqKeys.indexOf(k) !== i).length / 1;
    // 统计重复的顺子组
    const seen = {};
    for (const k of seqKeys) seen[k] = (seen[k] || 0) + 1;
    const pairsOfSeq = Object.values(seen).filter(v => v >= 2).length;
    if (pairsOfSeq === 2) yaku.push(['二杯口', 3]);
    else if (pairsOfSeq === 1) yaku.push(['一杯口', 1]);
  }

  // 三色同顺
  for (let r = 1; r <= 7; r++) {
    const has = [0, 1, 2].every(s => groupsAll.some(g => g.kind === 'seq' && g.tiles[0] === tileId(s, r)));
    if (has) { yaku.push(['三色同顺', menzen ? 2 : 1]); break; }
  }
  // 一气通贯
  for (let s = 0; s < 3; s++) {
    if ([1, 4, 7].every(r => groupsAll.some(g => g.kind === 'seq' && g.tiles[0] === tileId(s, r)))) {
      yaku.push(['一气通贯', menzen ? 2 : 1]);
      break;
    }
  }
  // 混全带幺九 / 纯全带幺九（纯全优先，不重复计数）
  const junchan = groupsAll.every(g => g.tiles.some(isTerminal)) && isTerminal(decomp.pair);
  if (junchan) yaku.push(['纯全带幺九', menzen ? 3 : 2]);
  else if (groupsAll.every(g => g.tiles.some(isTerminalOrHonor)) && isTerminalOrHonor(decomp.pair)) {
    yaku.push(['混全带幺九', menzen ? 2 : 1]);
  }
  // 对对和
  if (groupsAll.every(g => g.kind === 'tri')) yaku.push(['对对和', 2]);
  // 三暗刻
  if (concealedTriCount >= 3) yaku.push(['三暗刻', 2]);
  // 三杠子
  if (kanCount === 3) yaku.push(['三杠子', 2]);
  // 小三元
  if (dragonTriCount === 2 && DRAGONS.some(d => d === decomp.pair)) yaku.push(['小三元', 2]);
  // 混老头（对对形）
  if (all.every(isTerminalOrHonor) && groupsAll.every(g => g.kind === 'tri')) yaku.push(['混老头', 2]);
  // 混一色
  const suits = new Set(all.map(suitOf));
  if (suits.size === 1 && !suits.has(SUIT_HONOR)) yaku.push(['清一色', menzen ? 6 : 5]);
  else if (suits.size === 2 && suits.has(SUIT_HONOR)) yaku.push(['混一色', menzen ? 3 : 2]);

  return { yaku, yakuman, fu: null };
}

// 国士无双 检测: 返回 null 或 { dup }
export function detectKokushi(h) {
  if (h.melds.length > 0) return null;
  const c = countsOf(h.tiles);
  const orphans = [tileId(0,1), tileId(0,9), tileId(1,1), tileId(1,9), tileId(2,1), tileId(2,9), 27, 28, 29, 30, 31, 32, 33];
  let dup = -1;
  for (const o of orphans) {
    if (c[o] === 0) return null;
    if (c[o] === 2) { if (dup !== -1) return null; dup = o; }
    if (c[o] > 2) return null;
  }
  return { dup };
}

// 七对子 检测
export function detectSevenPairs(h) {
  if (h.melds.length > 0) return null;
  const c = countsOf(h.tiles);
  let pairs = 0;
  for (let i = 0; i < 34; i++) {
    if (c[i] !== 0 && c[i] !== 2) return null;
    if (c[i] === 2) pairs++;
  }
  return pairs === 7 ? {} : null;
}

// 九莲宝灯（严格型 1112345678999+X）: 返回 null 或 { pure }
export function detectNineGates(counts, h) {
  if (h.melds.length > 0) return null;
  // 必须为单一数牌花色（无字牌）
  for (let s = 0; s < 4; s++) {
    const start = s * 9;
    const inSuit = counts.slice(start, start + 9).reduce((a, b) => a + b, 0);
    if (inSuit > 0 && s === SUIT_HONOR) return null;
    if (inSuit === 14) {
      const base = [3, 1, 1, 1, 1, 1, 1, 1, 3];
      let extra = -1;
      for (let r = 1; r <= 9; r++) {
        const c = counts[tileId(s, r)], b = base[r - 1];
        if (c === b) continue;
        if (c === b + 1 && extra === -1) { extra = r; continue; }
        return null;
      }
      if (extra === -1) return null;
      const winRank = rankOf(h.winId);
      const pure = counts[h.winId] === base[winRank - 1] + 1;
      return { pure };
    }
  }
  return null;
}

// ---------- 符数计算 Fu ----------
export function computeFu(h, decomp, placement, yakuNames) {
  let fu = 20;
  const menzen = menzenOf(h);
  if (menzen && !h.isTsumo) fu += 10;
  if (h.isTsumo) fu += 2;
  // 副露
  for (const m of h.melds) {
    if (m.type === 'chi') continue;
    const isHon = isTerminalOrHonor(m.tiles[0]);
    if (m.type === 'pon') fu += isHon ? 4 : 2;
    else if (m.type === 'kanOpen') fu += isHon ? 16 : 8;
    else if (m.type === 'kanClosed') fu += isHon ? 32 : 16;
  }
  // 暗刻（和牌张所在的刻子：荣和按明刻）
  decomp.groups.forEach((g, idx) => {
    if (g.kind !== 'tri') return;
    const isWinGroup = placement.groupIdx === idx && !placement.inPair;
    const ronCompleted = isWinGroup && !h.isTsumo;
    const isHon = isTerminalOrHonor(g.tiles[0]);
    fu += ronCompleted ? (isHon ? 4 : 2) : (isHon ? 8 : 4);
  });
  // 雀头
  if (isYakuhai(decomp.pair, h)) fu += (decomp.pair === h.roundWind && decomp.pair === h.seatWind) ? 4 : 2;
  // 听牌形
  const wt = waitType(h, decomp, placement);
  if (wt === 'tanki' || wt === 'kanchan' || wt === 'penchan') fu += 2;
  // 平和自摸：不加自摸符
  if (yakuNames.includes('平和') && h.isTsumo) fu -= 2;
  if (fu < 20) fu = 20;
  return Math.ceil(fu / 10) * 10;
}

// ---------- 点数计算 Points ----------
export function computePoints(han, fu, h, yakumanCount) {
  let base;
  if (yakumanCount > 0) {
    base = 8000 * yakumanCount;
  } else {
    if (han >= 13) base = h.countedYakuman ? 8000 : 6000;
    else if (han >= 11) base = 6000;
    else if (han >= 8) base = 4000;
    else if (han >= 6) base = 3000;
    else if (han >= 5) base = 2000;
    else {
      base = fu * Math.pow(2, 2 + han);
      if (base >= 2000) base = 2000;
    }
  }
  const r100 = v => Math.ceil(v / 100) * 100;
  if (h.isDealer) {
    return h.isTsumo ? { kind: 'tsumo', each: r100(base * 2) } : { kind: 'ron', pay: r100(base * 6) };
  }
  return h.isTsumo ? { kind: 'tsumo', ko: r100(base), oya: r100(base * 2) } : { kind: 'ron', pay: r100(base * 4) };
}

// ---------- 总控 Score ----------
export function scoreHand(h) {
  const err = validate(h);
  if (err) return { error: err };

  const menzen = menzenOf(h);
  const counts = countsOf(h.tiles);
  const groupsNeeded = 4 - h.melds.length;

  const readings = [];
  // 特殊手牌
  if (h.melds.length === 0) {
    const kokushi = detectKokushi(h);
    if (kokushi) readings.push(buildReading(h, kokushi, null, true, false));
    const seven = detectSevenPairs(h);
    if (seven) readings.push(buildReading(h, null, null, false, true));
  }
  // 普通分解
  for (const decomp of decomposeHand(counts, groupsNeeded)) {
    for (const placement of placementsOf(decomp, h.winId)) {
      readings.push(buildReading(h, null, { decomp, placement }, false, false));
    }
  }

  // 宝牌 / 里宝牌
  const all = allTilesOf(h);
  const ac = countsOf(all);
  let doraHan = 0;
  for (const ind of h.doraIndicators) doraHan += ac[doraOf(ind)];
  let uraDoraHan = 0;
  if (h.riichi || h.doubleRiichi) {
    for (const ind of h.uraDoraIndicators) uraDoraHan += ac[doraOf(ind)];
  }

  // 选择最高点数的读法
  let best = null;
  for (const r of readings) {
    const pts = scoreReading(h, r, doraHan, uraDoraHan);
    if (!best || pts.totalPoints > best.totalPoints) best = pts;
  }
  if (!best) return { error: '不是和牌型' };
  // 无役不能和牌：番型列表为空（宝牌/里宝牌不计作番型）
  if (best.yaku.length === 0 && best.yakuman.length === 0) {
    return { error: '无役，不能和牌（需至少一个番型，宝牌不计作番型）' };
  }
  return best;
}

function buildReading(h, kokushi, normal, isKokushi, isSevenPairs) {
  if (isKokushi) return { kokushi, sevenPairs: false, decomp: null, placement: null };
  if (isSevenPairs) return { kokushi: null, sevenPairs: true, decomp: null, placement: null };
  return { kokushi: null, sevenPairs: false, decomp: normal.decomp, placement: normal.placement };
}

function scoreReading(h, reading, doraHan, uraDoraHan) {
  let det;
  if (reading.kokushi) det = detectYaku(h, null, null, false, reading.kokushi);
  else if (reading.sevenPairs) det = detectYaku(h, null, null, true, null);
  else det = detectYaku(h, reading.decomp, reading.placement, false, null);

  let fu = det.fu;
  if (fu === null) {
    const yakuNames = det.yaku.map(y => y[0]);
    fu = computeFu(h, reading.decomp, reading.placement, yakuNames);
  }
  let totalHan = 0;
  for (const [, han] of det.yaku) totalHan += han;
  const yakumanCount = det.yakuman.length;
  let hanWithDora;
  if (yakumanCount > 0) hanWithDora = 13 * yakumanCount;
  else hanWithDora = totalHan + doraHan + uraDoraHan;
  const pts = computePoints(hanWithDora, fu, h, yakumanCount);
  let totalPoints;
  if (pts.kind === 'ron') totalPoints = pts.pay;
  else if (h.isDealer) totalPoints = pts.each * 3;
  else totalPoints = pts.ko * 2 + pts.oya;

  return {
    yaku: det.yaku,
    yakuman: det.yakuman,
    fu,
    totalHan: hanWithDora,
    doraHan,
    uraDoraHan,
    yakumanCount,
    points: pts,
    totalPoints,
  };
}

function validate(h) {
  if (h.melds.length > 4) return '副露过多（最多 4 组）';
  const kanCount = h.melds.filter(m => m.type === 'kanOpen' || m.type === 'kanClosed').length;
  const nonKanCount = h.melds.length - kanCount;
  const expected = 14 - 3 * (kanCount + nonKanCount);
  if (h.tiles.length !== expected) return `手牌数量不正确：应有 ${expected} 张（含和牌张），实际 ${h.tiles.length} 张`;
  if (!h.tiles.includes(h.winId)) return '和牌张不在手牌中';
  const all = allTilesOf(h);
  const c = countsOf(all);
  for (let i = 0; i < 34; i++) if (c[i] > 4) return `「${nameOf(i)}」超过4张`;
  // 宝牌/里宝牌指示牌与手牌出自同一副牌，合计同样不能超过每种 4 张
  const ind = countsOf(h.doraIndicators.concat(h.uraDoraIndicators));
  for (let i = 0; i < 34; i++) {
    if (c[i] + ind[i] > 4) return `「${nameOf(i)}」在宝牌/里宝牌指示牌与手牌中合计超过 4 张`;
  }
  if (!menzenOf(h) && (h.riichi || h.doubleRiichi)) return '立直必须门前清';
  if (h.ippatsu && !(h.riichi || h.doubleRiichi)) return '一发需要立直';
  // 庄闲与自风：亲家必为东家，子家自风只能是南/西/北
  if (h.seatWind != null) {
    if (h.isDealer && h.seatWind !== 27) return '亲家（庄家）的自风必须为东';
    if (!h.isDealer && h.seatWind === 27) return '子家的自风不能为东';
  }
  // 特殊和牌方式合法性
  if (h.chankan && h.isTsumo) return '枪杠和牌必须是荣和';
  if (h.haitei && !h.isTsumo) return '海底捞月必须是自摸';
  if (h.houtei && h.isTsumo) return '河底摸鱼必须是荣和';
  if (h.chankan && h.houtei) return '枪杠与河底摸鱼不能同时成立';
  if (h.haitei && h.firstDraw) return '海底捞月与天和/地和矛盾';
  if (h.chankan && c[h.winId] !== 1) return '枪杠和牌时和牌张在手牌中只能有 1 张（其余 3 张在他人杠中）';
  return null;
}

// ---------- 显示 ----------
const RANK_CHARS = ['一', '二', '三', '四', '五', '六', '七', '八', '九'];
const HONOR_CHARS = ['东', '南', '西', '北', '白', '发', '中'];
export function nameOf(id) {
  const s = suitOf(id), r = rankOf(id);
  if (s === SUIT_HONOR) return HONOR_CHARS[r - 1];
  const suitChar = ['万', '筒', '索'][s];
  return RANK_CHARS[r - 1] + suitChar;
}

// 平局/对局字符串表示（测试用）
export function parseTiles(str) {
  // e.g. "123m456p789sESWN白發中" → ids
  const ids = [];
  const re = /(\d{1,9})([mps])|(.)/g;
  let i = 0;
  while (i < str.length) {
    const ch = str[i];
    if (ch >= '1' && ch <= '9') {
      let j = i;
      while (j < str.length && str[j] >= '0' && str[j] <= '9') j++;
      const ranks = str.slice(i, j);
      const suitChar = str[j];
      const suit = suitChar === 'm' ? 0 : suitChar === 'p' ? 1 : 2;
      for (const r of ranks) ids.push(tileId(suit, parseInt(r)));
      i = j + 1;
    } else {
      const map = { E: 27, S: 28, W: 29, N: 30, 东: 27, 東: 27, 南: 28, 西: 29, 北: 30, '白': 31, 发: 32, 發: 32, F: 32, '中': 33 };
      if (map[ch] !== undefined) ids.push(map[ch]);
      i++;
    }
  }
  return ids;
}
