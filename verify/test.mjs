// 立直麻将算分引擎测试（Node.js）
import { scoreHand, makeHand, parseTiles } from './engine.mjs';

let passed = 0, failed = 0;
const failures = [];

function check(name, cond, detail) {
  if (cond) { passed++; }
  else { failed++; failures.push(`${name}\n    ${detail}`); }
}

// 构造手牌: tiles=牌串, win=和牌张(字符串), 其余为选项
function h(tiles, win, opt = {}) {
  const ids = parseTiles(tiles);
  return makeHand({ tiles: ids, winId: parseTiles(win)[0], ...opt });
}

const P = (str) => parseTiles(str)[0]; // parse single tile

// ================= 基础点数表 =================
// 子 30符1番 平和ロン → 1000
{
  const r = scoreHand(h('123m456p789p345s55p', '3s'));
  check('平和ロン: 番1 符30 荣和1000', r.error === undefined && r.totalHan === 1 && r.fu === 30 && r.points.kind === 'ron' && r.points.pay === 1000,
    JSON.stringify(r));
  check('平和ロン: 番型含平和', r.error === undefined && r.yaku.some(y => y[0] === '平和'), JSON.stringify(r.yaku));
}
// 子 20符2番 平和ツモ → 400/700
{
  const r = scoreHand(h('123m456p789p345s55p', '3s', { isTsumo: true }));
  check('平和ツモ: 番2 符20 子400/亲700', r.error === undefined && r.totalHan === 2 && r.fu === 20 &&
    r.points.kind === 'tsumo' && r.points.ko === 400 && r.points.oya === 700, JSON.stringify(r));
  check('平和ツモ: 番型含门前清自摸和', r.error === undefined && r.yaku.some(y => y[0] === '门前清自摸和'), JSON.stringify(r.yaku));
}
// 子 25符2番 七对子ロン → 1600
{
  const r = scoreHand(h('1199m1199p1122s33p', '3p'));
  check('七对子ロン: 番2 符25 荣和1600', r.error === undefined && r.totalHan === 2 && r.fu === 25 && r.points.pay === 1600,
    JSON.stringify(r));
}
// 亲 20符2番 平和ツモ → 700 all
{
  const r = scoreHand(h('123m456p789p345s55p', '3s', { isTsumo: true, isDealer: true }));
  check('亲家平和ツモ: 700 all', r.error === undefined && r.points.kind === 'tsumo' && r.points.each === 700, JSON.stringify(r));
}
// 亲 30符1番 断幺九ツモ → 500 all
{
  const r = scoreHand(h('456p678p234s55m', '5m', { isTsumo: true, isDealer: true, melds: [{ type: 'chi', tiles: parseTiles('234p') }] }));
  check('亲家断幺九ツモ: 番1 符30 500 all', r.error === undefined && r.totalHan === 1 && r.fu === 30 &&
    r.points.kind === 'tsumo' && r.points.each === 500, JSON.stringify(r));
}
// 子 30符3番 → 3900
{
  const r = scoreHand(h('123m456p789p345s55p', '3s', { riichi: true, ippatsu: true }));
  check('立直一发平和: 番3 符30 荣和3900', r.error === undefined && r.totalHan === 3 && r.fu === 30 && r.points.pay === 3900, JSON.stringify(r));
}
// 双立直
{
  const r = scoreHand(h('123m456p789p345s55p', '3s', { doubleRiichi: true }));
  check('双立直平和: 番3 符30 荣和3900', r.error === undefined && r.totalHan === 3 && r.points.pay === 3900 &&
    r.yaku.some(y => y[0] === '双立直'), JSON.stringify(r));
}
// 里宝牌
{
  const r = scoreHand(h('123m456p789p345s55p', '3s', { riichi: true, uraDoraIndicators: [P('5p')] }));
  check('立直平和里宝1: 番3 符30 荣和3900', r.error === undefined && r.totalHan === 3 && r.uraDoraHan === 1 && r.points.pay === 3900, JSON.stringify(r));
}
// 子 30符4番 ツモ → 2000/3900
{
  const r = scoreHand(h('234m456m234p234s55m', '5m', { isTsumo: true }));
  check('三色断幺九自摸: 番4 符30 子2000/亲3900', r.error === undefined && r.totalHan === 4 && r.fu === 30 &&
    r.points.kind === 'tsumo' && r.points.ko === 2000 && r.points.oya === 3900, JSON.stringify(r));
}
// 子 40符2番 → 2600
{
  const r = scoreHand(h('123m123p123s456m77m', '7m'));
  check('三色同顺: 番2 符40 荣和2600', r.error === undefined && r.totalHan === 2 && r.fu === 40 && r.points.pay === 2600 &&
    r.yaku.some(y => y[0] === '三色同顺'), JSON.stringify(r));
}
// 一气通贯
{
  const r = scoreHand(h('123456789m234p55p', '5p'));
  check('一气通贯: 番2 符40 荣和2600', r.error === undefined && r.totalHan === 2 && r.fu === 40 && r.points.pay === 2600 &&
    r.yaku.some(y => y[0] === '一气通贯'), JSON.stringify(r));
}
// 一杯口
{
  const r = scoreHand(h('123123m456p789s55p', '5p'));
  check('一杯口: 番1 符40 荣和1300', r.error === undefined && r.totalHan === 1 && r.fu === 40 && r.points.pay === 1300 &&
    r.yaku.some(y => y[0] === '一杯口'), JSON.stringify(r));
}
// 混一色+一杯口 4番40符 → 满贯8000
{
  const r = scoreHand(h('112233m567789mEE', 'E', { roundWind: P('南'), seatWind: P('南') }));
  check('混一色一杯口: 番4 符40 满贯8000', r.error === undefined && r.totalHan === 4 && r.fu === 40 && r.points.pay === 8000 &&
    r.yaku.some(y => y[0] === '混一色') && r.yaku.some(y => y[0] === '一杯口'), JSON.stringify(r));
}
// 清一色 6番 → 跳满 12000
{
  const r = scoreHand(h('111222345678m99m', '9m'));
  check('清一色: 番6 符50 跳满12000', r.error === undefined && r.totalHan === 6 && r.fu === 50 && r.points.pay === 12000 &&
    r.yaku.some(y => y[0] === '清一色'), JSON.stringify(r));
}
// 三暗刻+役牌 3番50符 → 6400
{
  const r = scoreHand(h('111333555m789s白白', '5m', { roundWind: P('南'), seatWind: P('南') }));
  check('三暗刻役牌: 番3 符50 荣和6400', r.error === undefined && r.totalHan === 3 && r.fu === 50 && r.points.pay === 6400 &&
    r.yaku.some(y => y[0] === '三暗刻') && r.yaku.some(y => y[0] === '役牌·白'), JSON.stringify(r));
}
// 断幺九(副露) 1番30符 → 1000
{
  const r = scoreHand(h('456p678p234s55m', '5m', { melds: [{ type: 'chi', tiles: parseTiles('234p') }] }));
  check('断幺九副露: 番1 符30 荣和1000', r.error === undefined && r.totalHan === 1 && r.fu === 30 && r.points.pay === 1000 &&
    r.yaku.some(y => y[0] === '断幺九'), JSON.stringify(r));
}
// 混全带幺九+役牌 3番40符 → 5200
{
  const r = scoreHand(h('123789m123p789sEE', 'E', { roundWind: P('东'), seatWind: P('南') }));
  check('混全带幺九: 番3 符40 荣和5200', r.error === undefined && r.totalHan === 3 && r.fu === 40 && r.points.pay === 5200 &&
    r.yaku.some(y => y[0] === '混全带幺九'), JSON.stringify(r));
}
// 纯全带幺九+平和(ryanmen placement) 4番30符 → 7700
{
  const r = scoreHand(h('123789m123p789s11m', '1m', { roundWind: P('南'), seatWind: P('西') }));
  check('纯全带幺九平和: 番4 符30 荣和7700', r.error === undefined && r.totalHan === 4 && r.fu === 30 && r.points.pay === 7700 &&
    r.yaku.some(y => y[0] === '纯全带幺九') && r.yaku.some(y => y[0] === '平和') &&
    !r.yaku.some(y => y[0] === '混全带幺九'), JSON.stringify(r));
}
// 小三元(副露) 5番 → 满贯
{
  const r = scoreHand(h('发发发中中中白白123m', '白', { melds: [{ type: 'chi', tiles: parseTiles('456p') }] }));
  check('小三元: 番5 符40 满贯8000', r.error === undefined && r.totalHan === 5 && r.fu === 40 && r.points.pay === 8000 &&
    r.yaku.some(y => y[0] === '小三元') && !r.yakuman.includes('大三元'), JSON.stringify(r));
}
// 役牌 双风（亲家） 2番40符 → 3900
{
  const r = scoreHand(h('EEE123m456p789p55p', '5p', { roundWind: P('东'), seatWind: P('东'), isDealer: true }));
  check('连风牌役牌×2(亲家): 番2 符40 荣和3900', r.error === undefined && r.totalHan === 2 && r.fu === 40 && r.points.pay === 3900 &&
    r.yaku.filter(y => y[0] === '役牌·东').length === 2, JSON.stringify(r));
}
// 混老头七对子 4番25符 → 6400
{
  const r = scoreHand(h('1199m1199p1199sEE', 'E', { roundWind: P('南'), seatWind: P('西') }));
  check('混老头七对子: 番4 符25 荣和6400', r.error === undefined && r.totalHan === 4 && r.fu === 25 && r.points.pay === 6400 &&
    r.yaku.some(y => y[0] === '混老头'), JSON.stringify(r));
}
// 二杯口 3番40符 → 5200 (优先于七对子)
{
  const r = scoreHand(h('112233m445566p77s', '7s'));
  check('二杯口: 番3 符40 荣和5200', r.error === undefined && r.totalHan === 3 && r.fu === 40 && r.points.pay === 5200 &&
    r.yaku.some(y => y[0] === '二杯口') && !r.yaku.some(y => y[0] === '七对子'), JSON.stringify(r));
}
// 对对和+三暗刻(荣和完成刻子按明刻算符) 4番50符 → 满贯8000
{
  const r = scoreHand(h('111333555777m99p', '5m'));
  check('对对和三暗刻(荣): 番4 符50 满贯8000', r.error === undefined && r.totalHan === 4 && r.fu === 50 && r.points.pay === 8000 &&
    r.yaku.some(y => y[0] === '对对和') && r.yaku.some(y => y[0] === '三暗刻') && !r.yakuman.includes('四暗刻'), JSON.stringify(r));
}

// ================= 役满 =================
// 国士无双
{
  const r = scoreHand(h('19m19p19sEESWN白发中', '中'));
  check('国士无双: 役满 32000', r.error === undefined && r.yakuman.includes('国士无双') && r.points.pay === 32000, JSON.stringify(r));
}
// 国士十三面
{
  const r = scoreHand(h('19m19p19sEESWN白发中', 'E'));
  check('国士十三面: 双倍役满 64000', r.error === undefined && r.yakuman.filter(y => y === '国士无双十三面').length === 2 && r.points.pay === 64000, JSON.stringify(r));
  const r2 = scoreHand(makeHand({ ...h('19m19p19sEESWN白发中', 'E'), doubleYakuman: false }));
  check('国士十三面(关双倍): 役满 32000', r2.error === undefined && r2.yakuman.includes('国士无双') && r2.points.pay === 32000, JSON.stringify(r2));
}
// 大三元
{
  const r = scoreHand(h('发发发中中中123m55m', '5m', { melds: [{ type: 'pon', tiles: parseTiles('白白白') }] }));
  check('大三元: 役满 32000', r.error === undefined && r.yakuman.includes('大三元') && r.points.pay === 32000, JSON.stringify(r));
}
// 绿一色
{
  const r = scoreHand(h('234234666888s发发', '发'));
  check('绿一色: 役满 32000', r.error === undefined && r.yakuman.includes('绿一色') && r.points.pay === 32000, JSON.stringify(r));
}
// 字一色
{
  const r = scoreHand(h('南南南西西西北北北白白', '白', { melds: [{ type: 'pon', tiles: parseTiles('東東東') }] }));
  check('字一色: 役满 32000', r.error === undefined && r.yakuman.includes('字一色') && r.points.pay === 32000, JSON.stringify(r));
}
// 字一色七对子
{
  const r = scoreHand(h('EESSWWNN白白发发中中', '中', { isTsumo: true }));
  check('字一色七对子: 役满 子8000/亲16000', r.error === undefined && r.yakuman.includes('字一色') &&
    r.points.kind === 'tsumo' && r.points.ko === 8000 && r.points.oya === 16000, JSON.stringify(r));
}
// 四暗刻单骑(自摸)
{
  const r = scoreHand(h('111333555777m99p', '9p', { isTsumo: true }));
  check('四暗刻单骑自摸: 双倍役满 子16000/亲32000', r.error === undefined && r.yakuman.filter(y => y === '四暗刻单骑').length === 2 &&
    r.points.kind === 'tsumo' && r.points.ko === 16000 && r.points.oya === 32000, JSON.stringify(r));
  const r2 = scoreHand(makeHand({ ...h('111333555777m99p', '9p', { isTsumo: true }), doubleYakuman: false }));
  check('四暗刻单骑(关双倍): 役满 子8000/亲16000', r2.error === undefined && r2.yakuman.includes('四暗刻') && !r2.yakuman.includes('四暗刻单骑') &&
    r2.points.ko === 8000 && r2.points.oya === 16000, JSON.stringify(r2));
}
// 九莲宝灯 / 纯正九莲
{
  const r = scoreHand(h('1112345678999m9m', '9m'));
  check('纯正九莲宝灯: 双倍役满 64000', r.error === undefined && r.yakuman.filter(y => y === '纯正九莲宝灯').length === 2 && r.points.pay === 64000, JSON.stringify(r));
  const r2 = scoreHand(h('1112345678999m9m', '5m'));
  check('九莲宝灯: 役满 32000', r2.error === undefined && r2.yakuman.includes('九莲宝灯') && !r2.yakuman.includes('纯正九莲宝灯') && r2.points.pay === 32000, JSON.stringify(r2));
}
// 天和 / 地和
{
  const r = scoreHand(h('123m456p789p345s55p', '3s', { isTsumo: true, isDealer: true, firstDraw: true }));
  check('天和: 役满 亲家16000 all', r.error === undefined && r.yakuman.includes('天和') && r.points.each === 16000, JSON.stringify(r));
  const r2 = scoreHand(h('123m456p789p345s55p', '3s', { isTsumo: true, firstDraw: true }));
  check('地和: 役满 子8000/亲16000', r2.error === undefined && r2.yakuman.includes('地和') && r2.points.ko === 8000 && r2.points.oya === 16000, JSON.stringify(r2));
}
// 亲家 国士
{
  const r = scoreHand(h('19m19p19sEESWN白发中', '中', { isDealer: true }));
  check('亲家国士无双: 48000', r.error === undefined && r.points.pay === 48000, JSON.stringify(r));
}

// ================= 宝牌 / 累计役满 =================
// 表宝牌（含副露牌）
{
  const r = scoreHand(h('456p678p234s55m', '5m', { melds: [{ type: 'chi', tiles: parseTiles('234p') }], doraIndicators: [P('5p')] }));
  check('断幺九+宝牌2: 番3 符30 荣和3900', r.error === undefined && r.totalHan === 3 && r.doraHan === 2 && r.points.pay === 3900, JSON.stringify(r));
}
// 累计役满（立直1 + 宝牌12 = 13番；指示牌 4s×4 与手牌无冲突）
{
  const inds = [P('4s'), P('4s'), P('4s'), P('4s')];
  const r = scoreHand(h('234m456m555s789p99p', '9p', { riichi: true, doraIndicators: inds }));
  check('累计役满(13番): 役满 32000', r.error === undefined && r.totalHan === 13 && r.doraHan === 12 && r.points.pay === 32000, JSON.stringify(r));
  const r2 = scoreHand(makeHand({ ...h('234m456m555s789p99p', '9p', { riichi: true, doraIndicators: inds }), countedYakuman: false }));
  check('累计役满(关): 三倍满 24000', r2.error === undefined && r2.points.pay === 24000, JSON.stringify(r2));
}

// ================= 错误校验 =================
{
  const r = scoreHand(h('111112345678m55p', '5p'));
  check('校验: 超过4张报错', r.error !== undefined && r.error.includes('超过4张'), JSON.stringify(r));
}
{
  const r = scoreHand(h('123m456m789m55s', '5s', { melds: [{ type: 'pon', tiles: parseTiles('555p') }], riichi: true }));
  check('校验: 立直非门前清报错', r.error !== undefined && r.error.includes('立直必须门前清'), JSON.stringify(r));
}
{
  const r = scoreHand(h('11123456789m55p', '5p'));
  check('校验: 手牌数错误报错', r.error !== undefined && r.error.includes('手牌数量'), JSON.stringify(r));
}
{
  // 非和牌型
  const r = scoreHand(h('123m456m789m234p58p', '5p'));
  check('校验: 非和牌型报错', r.error !== undefined && r.error.includes('不是和牌型'), JSON.stringify(r));
}

// ================= 无役 / 输入非法 =================
{
  // 副露 123m + 234m 456p 789s 55p：无任何番型
  const r = scoreHand(h('234m456p789s55p', '5p', { melds: [{ type: 'chi', tiles: parseTiles('123m') }] }));
  check('校验: 无役(副露)报错', r.error !== undefined && r.error.includes('无役'), JSON.stringify(r));
}
{
  // 门前 123m 456p 789s 234m 55p：无任何番型
  const r = scoreHand(h('123m456p789s234m55p', '5p'));
  check('校验: 无役(门前)报错', r.error !== undefined && r.error.includes('无役'), JSON.stringify(r));
}
{
  // 仅有宝牌不算番型
  const r = scoreHand(h('123m456p789s234m55p', '5p', { doraIndicators: [P('5p')] }));
  check('校验: 仅有宝牌仍报无役', r.error !== undefined && r.error.includes('无役'), JSON.stringify(r));
}
{
  // 5 组副露
  const r = scoreHand(h('55p', '5p', { melds: ['123m', '456m', '789m', '123p', '456p'].map(s => ({ type: 'chi', tiles: parseTiles(s) })) }));
  check('校验: 副露过多报错', r.error !== undefined && r.error.includes('副露过多'), JSON.stringify(r));
}

// ================= 特殊和牌方式（枪杠/海底/河底） =================
// 枪杠和：平和 + 枪杠 = 2番30符 2000
{
  const r = scoreHand(h('123m456p789p345s55p', '3s', { chankan: true }));
  check('枪杠和平和: 番2 符30 荣和2000', r.error === undefined && r.totalHan === 2 && r.fu === 30 && r.points.pay === 2000 &&
    r.yaku.some(y => y[0] === '枪杠和'), JSON.stringify(r));
}
// 枪杠合法性：和牌张在手牌中超过 1 张（他人杠已占 4 张）
{
  const r = scoreHand(h('123m456m789m234p55p', '5p', { chankan: true }));
  check('校验: 枪杠和牌张只能1张', r.error !== undefined && r.error.includes('枪杠'), JSON.stringify(r));
}
// 枪杠必须荣和
{
  const r = scoreHand(h('123m456m789m123p55p', '1p', { chankan: true, isTsumo: true }));
  check('校验: 枪杠必须荣和', r.error !== undefined && r.error.includes('荣和'), JSON.stringify(r));
}
// 海底捞月：平和自摸 + 海底 = 3番20符 700/1300
{
  const r = scoreHand(h('123m456p789p345s55p', '3s', { isTsumo: true, haitei: true }));
  check('海底捞月: 番3 符20 子700/亲1300', r.error === undefined && r.totalHan === 3 && r.fu === 20 &&
    r.points.kind === 'tsumo' && r.points.ko === 700 && r.points.oya === 1300 &&
    r.yaku.some(y => y[0] === '海底捞月'), JSON.stringify(r));
}
// 海底必须自摸
{
  const r = scoreHand(h('123m456p789p345s55p', '3s', { haitei: true }));
  check('校验: 海底必须自摸', r.error !== undefined && r.error.includes('自摸'), JSON.stringify(r));
}
// 河底摸鱼：平和 + 河底 = 2番30符 2000
{
  const r = scoreHand(h('123m456p789p345s55p', '3s', { houtei: true }));
  check('河底摸鱼: 番2 符30 荣和2000', r.error === undefined && r.totalHan === 2 && r.fu === 30 && r.points.pay === 2000 &&
    r.yaku.some(y => y[0] === '河底摸鱼'), JSON.stringify(r));
}
// 河底必须荣和
{
  const r = scoreHand(h('123m456p789p345s55p', '3s', { houtei: true, isTsumo: true }));
  check('校验: 河底必须荣和', r.error !== undefined && r.error.includes('荣和'), JSON.stringify(r));
}
// 枪杠与河底互斥
{
  const r = scoreHand(h('123m456m789m123p55p', '1p', { chankan: true, houtei: true }));
  check('校验: 枪杠与河底互斥', r.error !== undefined && r.error.includes('不能同时'), JSON.stringify(r));
}
// 海底与天和/地和矛盾
{
  const r = scoreHand(h('123m456p789p345s55p', '3s', { isTsumo: true, haitei: true, firstDraw: true }));
  check('校验: 海底与天和地和矛盾', r.error !== undefined && r.error.includes('矛盾'), JSON.stringify(r));
}

// ================= 宝牌/里宝牌指示牌与手牌合计 4 张上限 =================
// 暗杠 5555m（4 张，手牌无 5m）+ 表宝牌指示牌 5m：合计 5 张，非法
{
  const r = scoreHand(h('123m456p789p55p', '5p', {
    melds: [{ type: 'kanClosed', tiles: parseTiles('5555m') }],
    doraIndicators: [P('5m')],
  }));
  check('校验: 表宝牌指示牌超限', r.error !== undefined && r.error.includes('合计超过 4 张'), JSON.stringify(r));
}
// 暗杠 5555m（4 张，手牌无 5m）+ 里宝牌指示牌 5m：合计 5 张，非法
{
  const r = scoreHand(h('123m456p789p55p', '5p', {
    melds: [{ type: 'kanClosed', tiles: parseTiles('5555m') }],
    riichi: true,
    uraDoraIndicators: [P('5m')],
  }));
  check('校验: 里宝牌指示牌超限', r.error !== undefined && r.error.includes('合计超过 4 张'), JSON.stringify(r));
}
// 指示牌本身超过 4 张
{
  const r = scoreHand(h('555m234p456p789s55p', '5p', { doraIndicators: [P('5m'), P('5m'), P('5m'), P('5m'), P('5m')] }));
  check('校验: 指示牌自身超限', r.error !== undefined && r.error.includes('合计超过 4 张'), JSON.stringify(r));
}
// 手牌 3 张 5m + 指示牌 1 张 5m = 4 张：合法
{
  const r = scoreHand(h('555m234p456p789s55p', '5p', { riichi: true, doraIndicators: [P('5m')] }));
  check('指示牌合计=4张合法: 番1', r.error === undefined && r.totalHan === 1 && r.yaku.some(y => y[0] === '立直'), JSON.stringify(r));
}

// ================= 庄闲与自风合法性 =================
// 亲家的自风只能是东
{
  const r = scoreHand(h('123m456p789p345s55p', '3s', { isDealer: true, seatWind: P('南') }));
  check('校验: 亲家自风必须为东', r.error !== undefined && r.error.includes('亲家'), JSON.stringify(r));
}
// 子家的自风只能是南/西/北
{
  const r = scoreHand(h('123m456p789p345s55p', '3s', { isDealer: false, seatWind: P('东') }));
  check('校验: 子家自风不能为东', r.error !== undefined && r.error.includes('子家'), JSON.stringify(r));
}
// 亲家 + 自风东：合法
{
  const r = scoreHand(h('123m456p789p345s55p', '3s', { isDealer: true, seatWind: P('东') }));
  check('亲家自风东合法: 番1', r.error === undefined && r.totalHan === 1, JSON.stringify(r));
}

console.log(`\n===== 结果: ${passed} 通过, ${failed} 失败 =====`);
if (failures.length) {
  console.log('\n失败用例:');
  for (const f of failures) console.log('  ✗ ' + f + '\n');
  process.exit(1);
}
