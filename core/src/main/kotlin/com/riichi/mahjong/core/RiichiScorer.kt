package com.riichi.mahjong.core

/**
 * 总控：校验输入 -> 枚举读法 -> 判定番型/符数/番数 -> 计算点数 -> 取最优读法。
 */
object RiichiScorer {

    fun score(h: HandConfig): ScoreOutcome {
        validate(h)?.let { return ScoreOutcome.Error(it) }

        val counts = countsOf(h.tiles)
        val groupsNeeded = 4 - h.melds.size
        val readings = mutableListOf<Reading>()

        // 特殊手牌（国士 / 七对子）
        if (h.melds.isEmpty()) {
            val kokushiDup = YakuDetector.detectKokushi(h)
            if (kokushiDup != null) readings.add(Reading(kokushiDup = kokushiDup))
            if (YakuDetector.detectSevenPairs(h)) readings.add(Reading(sevenPairs = true))
        }
        // 普通分解
        for (decomp in Decomposer.decompose(counts, groupsNeeded)) {
            for (placement in Decomposer.placementsOf(decomp, h.winId)) {
                readings.add(Reading(decomp = decomp, placement = placement))
            }
        }

        // 宝牌 / 里宝牌
        val ac = countsOf(h.allTiles())
        var doraHan = 0
        for (ind in h.doraIndicators) doraHan += ac[ind.dora().id]
        var uraDoraHan = 0
        if (h.riichi || h.doubleRiichi) {
            for (ind in h.uraDoraIndicators) uraDoraHan += ac[ind.dora().id]
        }

        var best: ScoreResult? = null
        for (r in readings) {
            val sr = scoreReading(h, r, doraHan, uraDoraHan)
            if (best == null || sr.totalPoints > best.totalPoints) best = sr
        }
        if (best == null) return ScoreOutcome.Error("不是和牌型")
        // 无役不能和牌：番型列表为空（宝牌/里宝牌不计作番型）
        if (best.yaku.isEmpty() && best.yakuman.isEmpty()) {
            return ScoreOutcome.Error("无役，不能和牌（需至少一个番型，宝牌不计作番型）")
        }
        return ScoreOutcome.Success(best)
    }

    private data class Reading(
        val kokushiDup: Tile? = null,
        val sevenPairs: Boolean = false,
        val decomp: Decomposition? = null,
        val placement: Placement? = null,
    )

    private fun scoreReading(h: HandConfig, r: Reading, doraHan: Int, uraDoraHan: Int): ScoreResult {
        val det = when {
            r.kokushiDup != null -> YakuDetector.detect(h, null, null, false, r.kokushiDup)
            r.sevenPairs -> YakuDetector.detect(h, null, null, true, null)
            else -> YakuDetector.detect(h, r.decomp, r.placement, false, null)
        }
        val fu = det.fu ?: FuCalculator.compute(h, r.decomp!!, r.placement!!, det.yaku.map { it.first })
        var totalHan = 0
        for ((_, han) in det.yaku) totalHan += han
        val yakumanCount = det.yakuman.size
        val hanWithDora = if (yakumanCount > 0) 13 * yakumanCount else totalHan + doraHan + uraDoraHan
        val points = PointCalculator.compute(hanWithDora, fu, h, yakumanCount)
        val totalPoints = when (points) {
            is Points.Ron -> points.pay
            is Points.Tsumo -> if (h.isDealer) points.each * 3 else points.ko * 2 + points.oya
        }
        return ScoreResult(
            yaku = det.yaku,
            yakuman = det.yakuman,
            doraHan = doraHan,
            uraDoraHan = uraDoraHan,
            totalHan = hanWithDora,
            fu = fu,
            points = points,
            totalPoints = totalPoints,
        )
    }

    private fun validate(h: HandConfig): String? {
        if (h.melds.size > 4) return "副露过多（最多 4 组）"
        val kanCount = h.melds.count { it.isKan }
        val nonKanCount = h.melds.size - kanCount
        val expected = 14 - 3 * (kanCount + nonKanCount)
        if (h.tiles.size != expected) {
            return "手牌数量不正确：应有 $expected 张（含和牌张），实际 ${h.tiles.size} 张"
        }
        if (!h.tiles.contains(h.winId)) return "和牌张不在手牌中"
        val c = countsOf(h.allTiles())
        for (i in 0 until 34) {
            if (c[i] > 4) return "「${Tile(i).display()}」超过 4 张"
        }
        // 宝牌/里宝牌指示牌与手牌出自同一副牌，合计同样不能超过每种 4 张
        val ind = countsOf(h.doraIndicators + h.uraDoraIndicators)
        for (i in 0 until 34) {
            if (c[i] + ind[i] > 4) {
                return "「${Tile(i).display()}」在宝牌/里宝牌指示牌与手牌中合计超过 4 张"
            }
        }
        if (!h.menzen() && (h.riichi || h.doubleRiichi)) return "立直必须门前清"
        if (h.ippatsu && !(h.riichi || h.doubleRiichi)) return "一发需要立直"
        // 庄闲与自风：亲家必为东家，子家自风只能是南/西/北
        if (h.seatWind != null) {
            if (h.isDealer && h.seatWind != Tile.EAST) return "亲家（庄家）的自风必须为东"
            if (!h.isDealer && h.seatWind == Tile.EAST) return "子家的自风不能为东"
        }
        // 特殊和牌方式合法性
        if (h.chankan && h.isTsumo) return "枪杠和牌必须是荣和"
        if (h.haitei && !h.isTsumo) return "海底捞月必须是自摸"
        if (h.houtei && h.isTsumo) return "河底摸鱼必须是荣和"
        if (h.chankan && h.houtei) return "枪杠与河底摸鱼不能同时成立"
        if (h.haitei && h.firstDraw) return "海底捞月与天和/地和矛盾"
        if (h.chankan && c[h.winId.id] != 1) return "枪杠和牌时和牌张在手牌中只能有 1 张（其余 3 张在他人杠中）"
        return null
    }
}
