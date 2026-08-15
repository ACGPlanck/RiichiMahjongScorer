package com.riichi.mahjong.core

/** 点数的支付方式 */
sealed class Points {
    /** 荣和 */
    data class Ron(val pay: Int) : Points()

    /** 自摸：子家各付 ko，亲家付 oya；亲家自摸时 ko == oya */
    data class Tsumo(val ko: Int, val oya: Int, val isDealer: Boolean) : Points() {
        val each: Int get() = if (isDealer) ko else oya
    }
}

/** 最终结果 */
data class ScoreResult(
    val yaku: List<Pair<String, Int>>,  // 番型与番数（不含宝牌）
    val yakuman: List<String>,          // 役满名（双倍时重复）
    val doraHan: Int,                   // 宝牌番数
    val uraDoraHan: Int,                // 里宝牌番数
    val totalHan: Int,                  // 合计番数（役满按 13×倍数）
    val fu: Int,                        // 符数（役满为 0，显示为 —）
    val points: Points,
    val totalPoints: Int,               // 合计收付点数
) {
    val isYakuman: Boolean get() = yakuman.isNotEmpty()
    val yakumanCount: Int get() = yakuman.size

    /** 手牌名称（用于结果标题，如 平和 / 七对子 / 国士无双 / 满贯 等） */
    val handName: String
        get() {
            if (yakuman.isNotEmpty()) return yakuman.first()
            val max = yaku.maxByOrNull { it.second } ?: return "和牌"
            return max.first
        }
}

sealed class ScoreOutcome {
    data class Success(val result: ScoreResult) : ScoreOutcome()
    data class Error(val message: String) : ScoreOutcome()
}

/** 点数计算 */
object PointCalculator {

    /** 计算基本点 */
    fun basePoints(han: Int, fu: Int, h: HandConfig, yakumanCount: Int): Int {
        if (yakumanCount > 0) return 8000 * yakumanCount
        return when {
            han >= 13 -> if (h.countedYakuman) 8000 else 6000
            han >= 11 -> 6000
            han >= 8 -> 4000
            han >= 6 -> 3000
            han >= 5 -> 2000
            else -> {
                val b = fu * (1 shl (2 + han))
                if (b >= 2000) 2000 else b
            }
        }
    }

    fun compute(han: Int, fu: Int, h: HandConfig, yakumanCount: Int): Points {
        val base = basePoints(han, fu, h, yakumanCount)
        fun r100(v: Int): Int = (v + 99) / 100 * 100
        return if (h.isDealer) {
            if (h.isTsumo) Points.Tsumo(ko = r100(base * 2), oya = r100(base * 2), isDealer = true)
            else Points.Ron(pay = r100(base * 6))
        } else {
            if (h.isTsumo) Points.Tsumo(ko = r100(base), oya = r100(base * 2), isDealer = false)
            else Points.Ron(pay = r100(base * 4))
        }
    }
}
