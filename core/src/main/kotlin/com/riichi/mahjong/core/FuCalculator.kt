package com.riichi.mahjong.core

/** 符数计算 */
object FuCalculator {

    fun compute(
        h: HandConfig,
        decomp: Decomposition,
        placement: Placement,
        yakuNames: List<String>,
    ): Int {
        var fu = 20
        val menzen = h.menzen()
        if (menzen && !h.isTsumo) fu += 10
        if (h.isTsumo) fu += 2
        // 副露
        for (m in h.melds) {
            if (m.type == MeldType.CHI) continue
            val isHon = m.tiles[0].isTerminalOrHonor
            fu += when (m.type) {
                MeldType.PON -> if (isHon) 4 else 2
                MeldType.KAN_OPEN -> if (isHon) 16 else 8
                MeldType.KAN_CLOSED -> if (isHon) 32 else 16
                MeldType.CHI -> 0
            }
        }
        // 暗刻（和牌张所在的刻子：荣和按明刻）
        decomp.groups.forEachIndexed { idx, g ->
            if (g !is Group.Triplet) return@forEachIndexed
            val isWinGroup = placement.groupIdx == idx && !placement.inPair
            val ronCompleted = isWinGroup && !h.isTsumo
            val isHon = g.tiles[0].isTerminalOrHonor
            fu += if (ronCompleted) (if (isHon) 4 else 2) else (if (isHon) 8 else 4)
        }
        // 雀头
        if (h.isYakuhai(decomp.pair)) {
            fu += if (decomp.pair == h.roundWind && decomp.pair == h.seatWind) 4 else 2
        }
        // 听牌形
        val wt = Decomposer.waitType(decomp, placement)
        if (wt == Decomposer.WaitType.TANKI || wt == Decomposer.WaitType.KANCHAN ||
            wt == Decomposer.WaitType.PENCHAN
        ) {
            fu += 2
        }
        // 平和自摸：不加自摸符
        if (yakuNames.contains("平和") && h.isTsumo) fu -= 2
        if (fu < 20) fu = 20
        return (fu + 9) / 10 * 10
    }
}
