package com.riichi.mahjong.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringTest {

    // ---------- 测试辅助 ----------

    private fun parseTiles(str: String): List<Tile> {
        val ids = mutableListOf<Tile>()
        var i = 0
        while (i < str.length) {
            val ch = str[i]
            if (ch in '1'..'9') {
                var j = i
                while (j < str.length && str[j] in '0'..'9') j++
                val ranks = str.substring(i, j)
                val suit = when (str[j]) {
                    'm' -> 0
                    'p' -> 1
                    else -> 2
                }
                for (r in ranks) ids.add(Tile.of(suit, r - '0'))
                i = j + 1
            } else {
                val map = mapOf(
                    'E' to 27, 'S' to 28, 'W' to 29, 'N' to 30,
                    '东' to 27, '東' to 27, '南' to 28, '西' to 29, '北' to 30,
                    '白' to 31, '发' to 32, '發' to 32, 'F' to 32, '中' to 33,
                )
                map[ch]?.let { ids.add(Tile(it)) }
                i++
            }
        }
        return ids
    }

    private fun P(str: String): Tile = parseTiles(str).first()

    private fun hand(
        tiles: String,
        win: String,
        isTsumo: Boolean = false,
        isDealer: Boolean = false,
        roundWind: Tile? = null,
        seatWind: Tile? = null,
        riichi: Boolean = false,
        doubleRiichi: Boolean = false,
        ippatsu: Boolean = false,
        chankan: Boolean = false,
        haitei: Boolean = false,
        houtei: Boolean = false,
        doraIndicators: List<Tile> = emptyList(),
        uraDoraIndicators: List<Tile> = emptyList(),
        firstDraw: Boolean = false,
        countedYakuman: Boolean = true,
        doubleYakuman: Boolean = true,
        melds: List<Meld> = emptyList(),
    ) = HandConfig(
        tiles = parseTiles(tiles),
        melds = melds,
        winId = parseTiles(win).first(),
        isTsumo = isTsumo, isDealer = isDealer, roundWind = roundWind, seatWind = seatWind,
        riichi = riichi, doubleRiichi = doubleRiichi, ippatsu = ippatsu,
        chankan = chankan, haitei = haitei, houtei = houtei,
        doraIndicators = doraIndicators, uraDoraIndicators = uraDoraIndicators,
        firstDraw = firstDraw, countedYakuman = countedYakuman, doubleYakuman = doubleYakuman,
    )

    private fun success(h: HandConfig): ScoreResult {
        val r = RiichiScorer.score(h)
        assertTrue("应为成功，实际错误: ${(r as? ScoreOutcome.Error)?.message}", r is ScoreOutcome.Success)
        return (r as ScoreOutcome.Success).result
    }

    private fun chi(tiles: String) = Meld(MeldType.CHI, parseTiles(tiles))
    private fun pon(tiles: String) = Meld(MeldType.PON, parseTiles(tiles))

    private fun assertYaku(res: ScoreResult, names: List<String>) {
        for (n in names) {
            assertTrue("缺少番型: $n (实际 ${res.yaku})", res.yaku.any { it.first == n })
        }
    }

    // ================= 基础点数 =================

    @Test
    fun pinfuRon() {
        val r = success(hand("123m456p789p345s55p", "3s"))
        assertEquals(1, r.totalHan); assertEquals(30, r.fu)
        assertEquals(1000, (r.points as Points.Ron).pay)
        assertYaku(r, listOf("平和"))
    }

    @Test
    fun pinfuTsumo() {
        val r = success(hand("123m456p789p345s55p", "3s", isTsumo = true))
        assertEquals(2, r.totalHan); assertEquals(20, r.fu)
        val t = r.points as Points.Tsumo
        assertEquals(400, t.ko); assertEquals(700, t.oya)
        assertYaku(r, listOf("门前清自摸和"))
    }

    @Test
    fun sevenPairsRon() {
        val r = success(hand("1199m1199p1122s33p", "3p"))
        assertEquals(2, r.totalHan); assertEquals(25, r.fu)
        assertEquals(1600, (r.points as Points.Ron).pay)
    }

    @Test
    fun dealerPinfuTsumo() {
        val r = success(hand("123m456p789p345s55p", "3s", isTsumo = true, isDealer = true))
        assertEquals(700, (r.points as Points.Tsumo).each)
    }

    @Test
    fun dealerTanyaoTsumo() {
        val r = success(hand("456p678p234s55m", "5m", isTsumo = true, isDealer = true, melds = listOf(chi("234p"))))
        assertEquals(1, r.totalHan); assertEquals(30, r.fu)
        assertEquals(500, (r.points as Points.Tsumo).each)
    }

    @Test
    fun riichiIppatsuPinfu() {
        val r = success(hand("123m456p789p345s55p", "3s", riichi = true, ippatsu = true))
        assertEquals(3, r.totalHan); assertEquals(30, r.fu)
        assertEquals(3900, (r.points as Points.Ron).pay)
    }

    @Test
    fun doubleRiichiPinfu() {
        val r = success(hand("123m456p789p345s55p", "3s", doubleRiichi = true))
        assertEquals(3, r.totalHan)
        assertEquals(3900, (r.points as Points.Ron).pay)
        assertYaku(r, listOf("双立直"))
    }

    @Test
    fun uraDora() {
        val r = success(hand("123m456p789p345s55p", "3s", riichi = true, uraDoraIndicators = listOf(P("5p"))))
        assertEquals(3, r.totalHan); assertEquals(1, r.uraDoraHan)
        assertEquals(3900, (r.points as Points.Ron).pay)
    }

    @Test
    fun sanshokuTanyaoTsumo_30fu4han() {
        val r = success(hand("234m456m234p234s55m", "5m", isTsumo = true))
        assertEquals(4, r.totalHan); assertEquals(30, r.fu)
        val t = r.points as Points.Tsumo
        assertEquals(2000, t.ko); assertEquals(3900, t.oya)
    }

    @Test
    fun sanshokuRon() {
        val r = success(hand("123m123p123s456m77m", "7m"))
        assertEquals(2, r.totalHan); assertEquals(40, r.fu)
        assertEquals(2600, (r.points as Points.Ron).pay)
        assertYaku(r, listOf("三色同顺"))
    }

    @Test
    fun ittsu() {
        val r = success(hand("123456789m234p55p", "5p"))
        assertEquals(2, r.totalHan); assertEquals(40, r.fu)
        assertEquals(2600, (r.points as Points.Ron).pay)
        assertYaku(r, listOf("一气通贯"))
    }

    @Test
    fun iipeiko() {
        val r = success(hand("123123m456p789s55p", "5p"))
        assertEquals(1, r.totalHan); assertEquals(40, r.fu)
        assertEquals(1300, (r.points as Points.Ron).pay)
        assertYaku(r, listOf("一杯口"))
    }

    @Test
    fun honitsuIipeiko() {
        val r = success(hand("112233m567789mEE", "E", roundWind = P("南"), seatWind = P("南")))
        assertEquals(4, r.totalHan); assertEquals(40, r.fu)
        assertEquals(8000, (r.points as Points.Ron).pay)
        assertYaku(r, listOf("混一色", "一杯口"))
    }

    @Test
    fun chinitsu() {
        val r = success(hand("111222345678m99m", "9m"))
        assertEquals(6, r.totalHan); assertEquals(50, r.fu)
        assertEquals(12000, (r.points as Points.Ron).pay)
        assertYaku(r, listOf("清一色"))
    }

    @Test
    fun sanankoTanyao() {
        val r = success(hand("111333555m789s白白", "5m", roundWind = P("南"), seatWind = P("南")))
        assertEquals(3, r.totalHan); assertEquals(50, r.fu)
        assertEquals(6400, (r.points as Points.Ron).pay)
        assertYaku(r, listOf("三暗刻", "役牌·白"))
    }

    @Test
    fun tanyaoOpen() {
        val r = success(hand("456p678p234s55m", "5m", melds = listOf(chi("234p"))))
        assertEquals(1, r.totalHan); assertEquals(30, r.fu)
        assertEquals(1000, (r.points as Points.Ron).pay)
        assertYaku(r, listOf("断幺九"))
    }

    @Test
    fun chantaYakuhai() {
        val r = success(hand("123789m123p789sEE", "E", roundWind = P("东"), seatWind = P("南")))
        assertEquals(3, r.totalHan); assertEquals(40, r.fu)
        assertEquals(5200, (r.points as Points.Ron).pay)
        assertYaku(r, listOf("混全带幺九", "役牌·东"))
    }

    @Test
    fun junchanPinfu() {
        val r = success(hand("123789m123p789s11m", "1m", roundWind = P("南"), seatWind = P("西")))
        assertEquals(4, r.totalHan); assertEquals(30, r.fu)
        assertEquals(7700, (r.points as Points.Ron).pay)
        assertYaku(r, listOf("纯全带幺九", "平和"))
        assertTrue("不应同时计算混全带幺九", r.yaku.none { it.first == "混全带幺九" })
    }

    @Test
    fun shousangen() {
        val r = success(hand("发发发中中中白白123m", "白", melds = listOf(chi("456p"))))
        assertEquals(5, r.totalHan); assertEquals(40, r.fu)
        assertEquals(8000, (r.points as Points.Ron).pay)
        assertYaku(r, listOf("小三元"))
    }

    @Test
    fun doubleWindYakuhai() {
        // 亲家（自风=东）+ 场风=东：连风牌 役牌×2，2番40符 亲家荣和 3900
        val r = success(hand("EEE123m456p789p55p", "5p", roundWind = P("东"), seatWind = P("东"), isDealer = true))
        assertEquals(2, r.totalHan); assertEquals(40, r.fu)
        assertEquals(3900, (r.points as Points.Ron).pay)
        assertEquals(2, r.yaku.count { it.first == "役牌·东" })
    }

    @Test
    fun honrotoSevenPairs() {
        val r = success(hand("1199m1199p1199sEE", "E", roundWind = P("南"), seatWind = P("西")))
        assertEquals(4, r.totalHan); assertEquals(25, r.fu)
        assertEquals(6400, (r.points as Points.Ron).pay)
        assertYaku(r, listOf("混老头"))
    }

    @Test
    fun ryanpeikoBeatsSevenPairs() {
        val r = success(hand("112233m445566p77s", "7s"))
        assertEquals(3, r.totalHan); assertEquals(40, r.fu)
        assertEquals(5200, (r.points as Points.Ron).pay)
        assertYaku(r, listOf("二杯口"))
        assertTrue("不应选七对子读法", r.yaku.none { it.first == "七对子" })
    }

    @Test
    fun toitoiSanankoRon() {
        val r = success(hand("111333555777m99p", "5m"))
        assertEquals(4, r.totalHan); assertEquals(50, r.fu)
        assertEquals(8000, (r.points as Points.Ron).pay)
        assertYaku(r, listOf("对对和", "三暗刻"))
        assertTrue("不应为四暗刻", r.yakuman.none { it == "四暗刻" })
    }

    // ================= 役满 =================

    @Test
    fun kokushi() {
        val r = success(hand("19m19p19sEESWN白发中", "中"))
        assertTrue(r.yakuman.contains("国士无双"))
        assertEquals(32000, (r.points as Points.Ron).pay)
    }

    @Test
    fun kokushiThirteenSided() {
        val r = success(hand("19m19p19sEESWN白发中", "E"))
        assertEquals(2, r.yakuman.count { it == "国士无双十三面" })
        assertEquals(64000, (r.points as Points.Ron).pay)

        val r2 = success(hand("19m19p19sEESWN白发中", "E", doubleYakuman = false))
        assertTrue(r2.yakuman.contains("国士无双"))
        assertEquals(32000, (r2.points as Points.Ron).pay)
    }

    @Test
    fun daisangen() {
        val r = success(hand("发发发中中中123m55m", "5m", melds = listOf(pon("白白白"))))
        assertTrue(r.yakuman.contains("大三元"))
        assertEquals(32000, (r.points as Points.Ron).pay)
    }

    @Test
    fun ryuisou() {
        val r = success(hand("234234666888s发发", "发"))
        assertTrue(r.yakuman.contains("绿一色"))
        assertEquals(32000, (r.points as Points.Ron).pay)
    }

    @Test
    fun tsuisou() {
        val r = success(hand("南南南西西西北北北白白", "白", melds = listOf(pon("東東東"))))
        assertTrue(r.yakuman.contains("字一色"))
        assertEquals(32000, (r.points as Points.Ron).pay)
    }

    @Test
    fun tsuisouSevenPairs() {
        val r = success(hand("EESSWWNN白白发发中中", "中", isTsumo = true))
        assertTrue(r.yakuman.contains("字一色"))
        val t = r.points as Points.Tsumo
        assertEquals(8000, t.ko); assertEquals(16000, t.oya)
    }

    @Test
    fun suuankoTanki() {
        val r = success(hand("111333555777m99p", "9p", isTsumo = true))
        assertEquals(2, r.yakuman.count { it == "四暗刻单骑" })
        val t = r.points as Points.Tsumo
        assertEquals(16000, t.ko); assertEquals(32000, t.oya)

        val r2 = success(hand("111333555777m99p", "9p", isTsumo = true, doubleYakuman = false))
        assertTrue(r2.yakuman.contains("四暗刻"))
        val t2 = r2.points as Points.Tsumo
        assertEquals(8000, t2.ko); assertEquals(16000, t2.oya)
    }

    @Test
    fun churen() {
        val r = success(hand("1112345678999m9m", "9m"))
        assertEquals(2, r.yakuman.count { it == "纯正九莲宝灯" })
        assertEquals(64000, (r.points as Points.Ron).pay)

        val r2 = success(hand("1112345678999m9m", "5m"))
        assertTrue(r2.yakuman.contains("九莲宝灯"))
        assertTrue(r2.yakuman.none { it == "纯正九莲宝灯" })
        assertEquals(32000, (r2.points as Points.Ron).pay)
    }

    @Test
    fun tenhoChiho() {
        val r = success(hand("123m456p789p345s55p", "3s", isTsumo = true, isDealer = true, firstDraw = true))
        assertTrue(r.yakuman.contains("天和"))
        assertEquals(16000, (r.points as Points.Tsumo).each)

        val r2 = success(hand("123m456p789p345s55p", "3s", isTsumo = true, firstDraw = true))
        assertTrue(r2.yakuman.contains("地和"))
        val t = r2.points as Points.Tsumo
        assertEquals(8000, t.ko); assertEquals(16000, t.oya)
    }

    @Test
    fun dealerKokushi() {
        val r = success(hand("19m19p19sEESWN白发中", "中", isDealer = true))
        assertEquals(48000, (r.points as Points.Ron).pay)
    }

    // ================= 宝牌 / 累计役满 =================

    @Test
    fun doraInMelds() {
        val r = success(hand("456p678p234s55m", "5m", melds = listOf(chi("234p")), doraIndicators = listOf(P("5p"))))
        assertEquals(3, r.totalHan); assertEquals(2, r.doraHan)
        assertEquals(3900, (r.points as Points.Ron).pay)
    }

    @Test
    fun countedYakuman() {
        // 立直1 + 宝牌12 = 13番；指示牌 4s×4 与手牌（无 4s）无冲突
        val inds = listOf(P("4s"), P("4s"), P("4s"), P("4s"))
        val r = success(hand("234m456m555s789p99p", "9p", riichi = true, doraIndicators = inds))
        assertEquals(13, r.totalHan); assertEquals(12, r.doraHan)
        assertEquals(32000, (r.points as Points.Ron).pay)

        val r2 = success(hand("234m456m555s789p99p", "9p", riichi = true, doraIndicators = inds, countedYakuman = false))
        assertEquals(24000, (r2.points as Points.Ron).pay)
    }

    // ================= 错误校验 =================

    @Test
    fun validationTooManyTiles() {
        val r = RiichiScorer.score(hand("111112345678m55p", "5p"))
        val msg = (r as ScoreOutcome.Error).message
        assertTrue("应报超过4张: $msg", msg.contains("超过 4 张"))
    }

    @Test
    fun validationRiichiOpen() {
        val r = RiichiScorer.score(hand("123m456m789m55s", "5s", melds = listOf(pon("555p")), riichi = true))
        val msg = (r as ScoreOutcome.Error).message
        assertTrue("应报立直门前清: $msg", msg.contains("立直必须门前清"))
    }

    @Test
    fun validationTileCount() {
        val r = RiichiScorer.score(hand("11123456789m55p", "5p"))
        val msg = (r as ScoreOutcome.Error).message
        assertTrue("应报手牌数量: $msg", msg.contains("手牌数量"))
    }

    @Test
    fun validationNotWinning() {
        val r = RiichiScorer.score(hand("123m456m789m234p58p", "5p"))
        val msg = (r as ScoreOutcome.Error).message
        assertTrue("应报不是和牌型: $msg", msg.contains("不是和牌型"))
    }

    @Test
    fun validationWinTileNotInHand() {
        val r = RiichiScorer.score(hand("123m456m789m345s55p", "6s"))
        val msg = (r as ScoreOutcome.Error).message
        assertTrue("应报和牌张不在手牌: $msg", msg.contains("和牌张不在手牌"))
    }

    // ================= 无役 / 输入非法 =================

    @Test
    fun validationNoYakuOpen() {
        // 副露 123m + 234m 456p 789s 55p：无任何番型
        val r = RiichiScorer.score(hand("234m456p789s55p", "5p", melds = listOf(chi("123m"))))
        val msg = (r as ScoreOutcome.Error).message
        assertTrue("无役应报错: $msg", msg.contains("无役"))
    }

    @Test
    fun validationNoYakuMenzen() {
        // 门前 123m 456p 789s 234m 55p：无任何番型
        val r = RiichiScorer.score(hand("123m456p789s234m55p", "5p"))
        val msg = (r as ScoreOutcome.Error).message
        assertTrue("无役应报错: $msg", msg.contains("无役"))
    }

    @Test
    fun validationNoYakuWithDoraOnly() {
        // 仅有宝牌（1番）不算番型，仍是无役
        val r = RiichiScorer.score(hand("123m456p789s234m55p", "5p", doraIndicators = listOf(P("5p"))))
        val msg = (r as ScoreOutcome.Error).message
        assertTrue("仅有宝牌仍应报无役: $msg", msg.contains("无役"))
    }

    @Test
    fun validationTooManyMelds() {
        val r = RiichiScorer.score(
            hand(
                "55p", "5p",
                melds = listOf(chi("123m"), chi("456m"), chi("789m"), chi("123p"), chi("456p")),
            )
        )
        val msg = (r as ScoreOutcome.Error).message
        assertTrue("副露过多应报错: $msg", msg.contains("副露过多"))
    }

    // ================= 特殊和牌方式（枪杠/海底/河底） =================

    @Test
    fun chankan() {
        val r = success(hand("123m456p789p345s55p", "3s", chankan = true))
        assertEquals(2, r.totalHan); assertEquals(30, r.fu)
        assertEquals(2000, (r.points as Points.Ron).pay)
        assertYaku(r, listOf("枪杠和", "平和"))
    }

    @Test
    fun chankanWinTileMustBeSingle() {
        // 他人杠中已持 4 张，和牌张在手牌中只能有 1 张
        val r = RiichiScorer.score(hand("123m456m789m234p55p", "5p", chankan = true))
        val msg = (r as ScoreOutcome.Error).message
        assertTrue("枪杠4张校验应报错: $msg", msg.contains("枪杠"))
    }

    @Test
    fun chankanMustBeRon() {
        val r = RiichiScorer.score(hand("123m456p789p345s55p", "3s", chankan = true, isTsumo = true))
        val msg = (r as ScoreOutcome.Error).message
        assertTrue("枪杠必须荣和: $msg", msg.contains("荣和"))
    }

    @Test
    fun haitei() {
        val r = success(hand("123m456p789p345s55p", "3s", isTsumo = true, haitei = true))
        assertEquals(3, r.totalHan); assertEquals(20, r.fu)
        val t = r.points as Points.Tsumo
        assertEquals(700, t.ko); assertEquals(1300, t.oya)
        assertYaku(r, listOf("海底捞月"))
    }

    @Test
    fun haiteiMustBeTsumo() {
        val r = RiichiScorer.score(hand("123m456p789p345s55p", "3s", haitei = true))
        val msg = (r as ScoreOutcome.Error).message
        assertTrue("海底必须自摸: $msg", msg.contains("自摸"))
    }

    @Test
    fun houtei() {
        val r = success(hand("123m456p789p345s55p", "3s", houtei = true))
        assertEquals(2, r.totalHan); assertEquals(30, r.fu)
        assertEquals(2000, (r.points as Points.Ron).pay)
        assertYaku(r, listOf("河底摸鱼"))
    }

    @Test
    fun houteiMustBeRon() {
        val r = RiichiScorer.score(hand("123m456p789p345s55p", "3s", houtei = true, isTsumo = true))
        val msg = (r as ScoreOutcome.Error).message
        assertTrue("河底必须荣和: $msg", msg.contains("荣和"))
    }

    @Test
    fun chankanHouteiExclusive() {
        val r = RiichiScorer.score(hand("123m456p789p345s55p", "3s", chankan = true, houtei = true))
        val msg = (r as ScoreOutcome.Error).message
        assertTrue("枪杠与河底互斥: $msg", msg.contains("不能同时"))
    }

    @Test
    fun haiteiFirstDrawConflict() {
        val r = RiichiScorer.score(hand("123m456p789p345s55p", "3s", isTsumo = true, haitei = true, firstDraw = true))
        val msg = (r as ScoreOutcome.Error).message
        assertTrue("海底与天和地和矛盾: $msg", msg.contains("矛盾"))
    }

    // ================= 宝牌/里宝牌指示牌与手牌合计 4 张上限 =================

    @Test
    fun validationDoraIndicatorOverLimit() {
        // 暗杠 5555m（4 张，手牌无 5m）+ 表宝牌指示牌 5m：合计 5 张，非法
        val r = RiichiScorer.score(
            hand(
                "123m456p789p55p", "5p",
                melds = listOf(Meld(MeldType.KAN_CLOSED, parseTiles("5555m"))),
                doraIndicators = listOf(P("5m")),
            )
        )
        val msg = (r as ScoreOutcome.Error).message
        assertTrue("指示牌超限应报错: $msg", msg.contains("合计超过 4 张"))
    }

    @Test
    fun validationUraDoraIndicatorOverLimit() {
        // 暗杠 5555m（4 张，手牌无 5m）+ 里宝牌指示牌 5m：合计 5 张，非法
        val r = RiichiScorer.score(
            hand(
                "123m456p789p55p", "5p",
                melds = listOf(Meld(MeldType.KAN_CLOSED, parseTiles("5555m"))),
                riichi = true,
                uraDoraIndicators = listOf(P("5m")),
            )
        )
        val msg = (r as ScoreOutcome.Error).message
        assertTrue("里宝牌指示牌超限应报错: $msg", msg.contains("合计超过 4 张"))
    }

    @Test
    fun validationDoraIndicatorItselfOverLimit() {
        // 指示牌本身超过 4 张（引擎层防御，UI 已限制）
        val r = RiichiScorer.score(
            hand(
                "555m234p456p789s55p", "5p",
                doraIndicators = listOf(P("5m"), P("5m"), P("5m"), P("5m"), P("5m")),
            )
        )
        val msg = (r as ScoreOutcome.Error).message
        assertTrue("指示牌自身超限应报错: $msg", msg.contains("合计超过 4 张"))
    }

    @Test
    fun validationIndicatorWithinLimit() {
        // 手牌 3 张 5m + 指示牌 1 张 5m = 4 张：合法，正常计算
        val r = success(hand("555m234p456p789s55p", "5p", riichi = true, doraIndicators = listOf(P("5m"))))
        assertEquals(1, r.totalHan)
        assertYaku(r, listOf("立直"))
    }

    // ================= 庄闲与自风合法性 =================

    @Test
    fun validationDealerSeatWindMustBeEast() {
        // 亲家的自风只能是东
        val r = RiichiScorer.score(hand("123m456p789p345s55p", "3s", isDealer = true, seatWind = P("南")))
        val msg = (r as ScoreOutcome.Error).message
        assertTrue("亲家自风非东应报错: $msg", msg.contains("亲家"))
    }

    @Test
    fun validationNonDealerSeatWindCannotBeEast() {
        // 子家的自风只能是南/西/北
        val r = RiichiScorer.score(hand("123m456p789p345s55p", "3s", isDealer = false, seatWind = P("东")))
        val msg = (r as ScoreOutcome.Error).message
        assertTrue("子家自风为东应报错: $msg", msg.contains("子家"))
    }

    @Test
    fun dealerSeatWindEastValid() {
        // 亲家 + 自风东：合法
        val r = success(hand("123m456p789p345s55p", "3s", isDealer = true, seatWind = P("东")))
        assertEquals(1, r.totalHan)
    }
}
