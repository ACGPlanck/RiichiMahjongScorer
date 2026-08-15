package com.riichi.mahjong.core

/** 一次读法的番型检测结果 */
data class YakuDetection(
    val yaku: List<Pair<String, Int>>, // 番型名 -> 番数
    val yakuman: List<String>,         // 役满名（双倍役满会重复出现两次）
    val fu: Int?                       // 固定符数（七对子 25 / 役满 0），null 表示需按常规计算
)

/**
 * 番型判定。
 */
object YakuDetector {

    fun detect(
        h: HandConfig,
        decomp: Decomposition?,
        placement: Placement?,
        sevenPairs: Boolean,
        kokushiDup: Tile?,
    ): YakuDetection {
        val yaku = mutableListOf<Pair<String, Int>>()
        val yakuman = mutableListOf<String>()
        val menzen = h.menzen()
        val all = h.allTiles()
        val allCounts = countsOf(all)
        val concealedCounts = countsOf(h.tiles)
        val kanCount = h.melds.count { it.isKan }
        val isGreen = { t: Tile -> t.id == Tile.of(2, 2).id || t.id == Tile.of(2, 3).id || t.id == Tile.of(2, 4).id ||
            t.id == Tile.of(2, 6).id || t.id == Tile.of(2, 8).id || t == Tile.HATSU }

        // ===== 役满（不依赖分解）=====
        if (kokushiDup != null) {
            if (h.doubleYakuman && h.winId == kokushiDup) {
                yakuman.add("国士无双十三面"); yakuman.add("国士无双十三面")
            } else {
                yakuman.add("国士无双")
            }
            return YakuDetection(yaku, yakuman, 0)
        }
        if (sevenPairs) {
            if (all.all { it.isHonor }) yakuman.add("字一色")
            if (all.all { it.isTerminal }) yakuman.add("清老头")
            if (all.all(isGreen) && allCounts[Tile.HATSU.id] > 0) yakuman.add("绿一色")
            if (yakuman.isNotEmpty()) return YakuDetection(yaku, yakuman, 0)
            yaku.add("七对子" to 2)
            if (all.all { it.isSimple }) yaku.add("断幺九" to 1)
            if (all.all { it.isTerminalOrHonor }) yaku.add("混老头" to 2)
            val suits = all.map { it.suit }.toSet()
            if (suits.size == 1 && !suits.contains(Tile.SUIT_HONOR)) yaku.add("清一色" to (if (menzen) 6 else 5))
            else if (suits.size == 2 && suits.contains(Tile.SUIT_HONOR)) yaku.add("混一色" to (if (menzen) 3 else 2))
            if (menzen && h.isTsumo) yaku.add("门前清自摸和" to 1)
            return YakuDetection(yaku, yakuman, 25)
        }

        val d = decomp!!
        val pl = placement!!
        val concealedTriCount = d.groups.count { it is Group.Triplet } +
            h.melds.count { it.type == MeldType.KAN_CLOSED }
        val dragonTriCount = Tile.DRAGONS.count { allCounts[it.id] >= 3 }
        val groupsAll: List<Group> = d.groups + h.melds.map { m ->
            if (m.type == MeldType.CHI) Group.Sequence(m.tiles) else Group.Triplet(m.tiles.take(3))
        }

        // 九莲宝灯（严格型 1112345678999+X）
        val nineGates = detectNineGates(concealedCounts, h)
        if (nineGates != null) {
            if (nineGates && h.doubleYakuman) {
                yakuman.add("纯正九莲宝灯"); yakuman.add("纯正九莲宝灯")
            } else {
                yakuman.add("九莲宝灯")
            }
            return YakuDetection(yaku, yakuman, 0)
        }

        // ===== 役满 =====
        if (kanCount == 4) { yakuman.add("四杠子"); return YakuDetection(yaku, yakuman, 0) }
        if (concealedTriCount == 4 && (h.isTsumo || pl.inPair)) {
            if (h.doubleYakuman && pl.inPair) {
                yakuman.add("四暗刻单骑"); yakuman.add("四暗刻单骑")
            } else {
                yakuman.add("四暗刻")
            }
            return YakuDetection(yaku, yakuman, 0)
        }
        if (dragonTriCount == 3) { yakuman.add("大三元"); return YakuDetection(yaku, yakuman, 0) }
        if (all.all { it.isHonor }) { yakuman.add("字一色"); return YakuDetection(yaku, yakuman, 0) }
        if (all.all(isGreen) && allCounts[Tile.HATSU.id] > 0) {
            yakuman.add("绿一色"); return YakuDetection(yaku, yakuman, 0)
        }
        if (all.all { it.isTerminal }) { yakuman.add("清老头"); return YakuDetection(yaku, yakuman, 0) }

        // ===== 通常役 =====
        if (menzen) {
            if (h.doubleRiichi) yaku.add("双立直" to 2)
            else if (h.riichi) yaku.add("立直" to 1)
            if (h.ippatsu && (h.riichi || h.doubleRiichi)) yaku.add("一发" to 1)
            if (h.firstDraw) {
                if (h.isDealer && h.isTsumo) yakuman.add("天和")
                else if (!h.isDealer && h.isTsumo) yakuman.add("地和")
                if (yakuman.isNotEmpty()) return YakuDetection(emptyList(), yakuman, 0)
            }
            if (h.isTsumo) yaku.add("门前清自摸和" to 1)
        }

        // 平和
        val allSeq = d.groups.all { it is Group.Sequence } && h.melds.isEmpty()
        if (allSeq && !h.isYakuhai(d.pair) &&
            Decomposer.waitType(d, pl) == Decomposer.WaitType.RYANMEN
        ) {
            yaku.add("平和" to 1)
        }

        // 断幺九
        if (all.all { it.isSimple }) yaku.add("断幺九" to 1)

        // 特殊和牌方式（需用户输入的牌局状况）
        if (h.chankan && !h.isTsumo) yaku.add("枪杠和" to 1)
        if (h.haitei && h.isTsumo) yaku.add("海底捞月" to 1)
        if (h.houtei && !h.isTsumo) yaku.add("河底摸鱼" to 1)

        // 役牌（场风/自风/三元 的刻子或雀头，各 1 番）
        val yakuhaiCandidates = mutableListOf<Tile>()
        val round = h.roundWind
        val seat = h.seatWind
        if (round != null && (allCounts[round.id] >= 3 || d.pair == round)) yakuhaiCandidates.add(round)
        if (seat != null && (allCounts[seat.id] >= 3 || d.pair == seat)) yakuhaiCandidates.add(seat)
        for (dd in Tile.DRAGONS) if (allCounts[dd.id] >= 3 || d.pair == dd) yakuhaiCandidates.add(dd)
        for (t in yakuhaiCandidates) yaku.add("役牌·${t.display()}" to 1)

        // 一杯口 / 二杯口（门前）
        if (menzen && h.melds.isEmpty()) {
            val seen = HashMap<String, Int>()
            for (g in d.groups.filterIsInstance<Group.Sequence>()) {
                val key = g.tiles.joinToString(",") { it.id.toString() }
                seen[key] = (seen[key] ?: 0) + 1
            }
            val pairsOfSeq = seen.values.count { it >= 2 }
            if (pairsOfSeq == 2) yaku.add("二杯口" to 3)
            else if (pairsOfSeq == 1) yaku.add("一杯口" to 1)
        }

        // 三色同顺
        for (r in 1..7) {
            val has = (0..2).all { s ->
                groupsAll.any { g -> g is Group.Sequence && g.tiles[0] == Tile.of(s, r) }
            }
            if (has) { yaku.add("三色同顺" to (if (menzen) 2 else 1)); break }
        }
        // 一气通贯
        for (s in 0..2) {
            if (listOf(1, 4, 7).all { r ->
                    groupsAll.any { g -> g is Group.Sequence && g.tiles[0] == Tile.of(s, r) }
                }
            ) {
                yaku.add("一气通贯" to (if (menzen) 2 else 1)); break
            }
        }
        // 混全带幺九 / 纯全带幺九（纯全优先）
        val junchan = groupsAll.all { g -> g.tiles.any { it.isTerminal } } && d.pair.isTerminal
        if (junchan) {
            yaku.add("纯全带幺九" to (if (menzen) 3 else 2))
        } else if (groupsAll.all { g -> g.tiles.any { it.isTerminalOrHonor } } && d.pair.isTerminalOrHonor) {
            yaku.add("混全带幺九" to (if (menzen) 2 else 1))
        }
        // 对对和
        if (groupsAll.all { it is Group.Triplet }) yaku.add("对对和" to 2)
        // 三暗刻
        if (concealedTriCount >= 3) yaku.add("三暗刻" to 2)
        // 三杠子
        if (kanCount == 3) yaku.add("三杠子" to 2)
        // 小三元
        if (dragonTriCount == 2 && Tile.DRAGONS.any { it == d.pair }) yaku.add("小三元" to 2)
        // 混老头（对对形）
        if (all.all { it.isTerminalOrHonor } && groupsAll.all { it is Group.Triplet }) yaku.add("混老头" to 2)
        // 清一色 / 混一色
        val suits = all.map { it.suit }.toSet()
        if (suits.size == 1 && !suits.contains(Tile.SUIT_HONOR)) {
            yaku.add("清一色" to (if (menzen) 6 else 5))
        } else if (suits.size == 2 && suits.contains(Tile.SUIT_HONOR)) {
            yaku.add("混一色" to (if (menzen) 3 else 2))
        }

        return YakuDetection(yaku, yakuman, null)
    }

    /** 国士无双：返回重复的那张牌，或 null */
    fun detectKokushi(h: HandConfig): Tile? {
        if (h.melds.isNotEmpty()) return null
        val c = countsOf(h.tiles)
        val orphans = listOf(
            Tile.of(0, 1), Tile.of(0, 9), Tile.of(1, 1), Tile.of(1, 9),
            Tile.of(2, 1), Tile.of(2, 9), Tile.EAST, Tile.SOUTH, Tile.WEST, Tile.NORTH,
            Tile.HAKU, Tile.HATSU, Tile.CHUN,
        )
        var dup: Tile? = null
        for (o in orphans) {
            when (c[o.id]) {
                0 -> return null
                2 -> if (dup != null) return null else dup = o
                else -> if (c[o.id] > 2) return null
            }
        }
        return dup
    }

    /** 七对子：7 个不同对子 */
    fun detectSevenPairs(h: HandConfig): Boolean {
        if (h.melds.isNotEmpty()) return false
        val c = countsOf(h.tiles)
        var pairs = 0
        for (i in 0 until 34) {
            if (c[i] != 0 && c[i] != 2) return false
            if (c[i] == 2) pairs++
        }
        return pairs == 7
    }

    /** 九莲宝灯（严格型）：返回是否纯正 */
    fun detectNineGates(counts: IntArray, h: HandConfig): Boolean? {
        if (h.melds.isNotEmpty()) return null
        // 仅数牌花色可能构成九莲宝灯（字牌 27..33 不做检查）
        for (s in 0..2) {
            val start = s * 9
            val inSuit = (start until start + 9).sumOf { counts[it] }
            if (inSuit == 14) {
                val base = intArrayOf(3, 1, 1, 1, 1, 1, 1, 1, 3)
                var extra = -1
                for (r in 1..9) {
                    val c = counts[Tile.of(s, r).id]
                    val b = base[r - 1]
                    if (c == b) continue
                    if (c == b + 1 && extra == -1) { extra = r; continue }
                    return null
                }
                if (extra == -1) return null
                // 纯正：和牌张是"额外"那张（和牌前为 1112345678999 九面听）
                val pure = counts[h.winId.id] == base[h.winId.rank - 1] + 1
                return pure
            }
        }
        return null
    }
}
