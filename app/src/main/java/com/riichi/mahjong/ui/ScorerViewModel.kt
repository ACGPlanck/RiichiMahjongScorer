package com.riichi.mahjong.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.riichi.mahjong.core.HandConfig
import com.riichi.mahjong.core.Meld
import com.riichi.mahjong.core.RiichiScorer
import com.riichi.mahjong.core.ScoreOutcome
import com.riichi.mahjong.core.Tile

class ScorerViewModel : ViewModel() {

    // ===== 和牌条件 =====
    var isDealer by mutableStateOf(false)
    var isTsumo by mutableStateOf(false)
    var roundWind by mutableStateOf(Tile.EAST)
    var seatWind by mutableStateOf(Tile.SOUTH) // 默认子家，自风为南（子家自风只能是南/西/北）
    var riichi by mutableStateOf(false)
    var doubleRiichi by mutableStateOf(false)
    var ippatsu by mutableStateOf(false)
    var chankan by mutableStateOf(false) // 枪杠和牌（仅荣和）
    var haitei by mutableStateOf(false)  // 海底捞月（仅自摸）
    var houtei by mutableStateOf(false)  // 河底摸鱼（仅荣和）
    var firstDraw by mutableStateOf(false)
    var countedYakuman by mutableStateOf(true)
    var doubleYakuman by mutableStateOf(true)

    /** 设置庄闲；亲家必为东家，子家自风只能是南/西/北 */
    fun updateDealer(v: Boolean) {
        isDealer = v
        if (v) {
            seatWind = Tile.EAST
        } else if (seatWind == Tile.EAST) {
            seatWind = Tile.SOUTH
        }
    }

    /** 设置和牌方式，自动清除冲突的特殊和牌标记 */
    fun updateTsumo(v: Boolean) {
        isTsumo = v
        if (v) { chankan = false; houtei = false } else { haitei = false }
    }

    /** 设置天和/地和，与海底捞月互斥 */
    fun updateFirstDraw(v: Boolean) {
        firstDraw = v
        if (v) haitei = false
    }

    /** 设置立直；立直关闭且无双立直时清空里宝牌 */
    fun updateRiichi(v: Boolean) {
        riichi = v
        if (v) doubleRiichi = false
        if (!riichi && !doubleRiichi) uraDoraIndicators = emptyList()
    }

    /** 设置双立直；双立直关闭且无立直时清空里宝牌 */
    fun updateDoubleRiichi(v: Boolean) {
        doubleRiichi = v
        if (v) riichi = false
        if (!riichi && !doubleRiichi) uraDoraIndicators = emptyList()
    }

    // ===== 手牌 =====
    var handTiles by mutableStateOf<List<Tile>>(emptyList())
    var melds by mutableStateOf<List<Meld>>(emptyList())
    var winIndex by mutableStateOf<Int?>(null)
    var doraIndicators by mutableStateOf<List<Tile>>(emptyList())
    var uraDoraIndicators by mutableStateOf<List<Tile>>(emptyList())

    // ===== 结果 =====
    var result by mutableStateOf<ScoreOutcome?>(null)

    /** 各牌在"门前牌 + 副露"中的总数 */
    private fun totalCount(t: Tile): Int =
        handTiles.count { it == t } + melds.sumOf { m -> m.tiles.count { it == t } }

    fun totalCountOf(t: Tile): Int = totalCount(t)

    fun addTile(t: Tile) {
        if (totalCount(t) < 4) {
            handTiles = handTiles + t
            if (winIndex == null && handTiles.size > 1) winIndex = handTiles.size - 1
        }
    }

    fun removeTileAt(index: Int) {
        if (index !in handTiles.indices) return
        handTiles = handTiles.filterIndexed { i, _ -> i != index }
        winIndex = winIndex?.let { w ->
            when {
                w == index -> null
                w > index -> w - 1
                else -> w
            }
        }
        if (handTiles.size == 1) winIndex = 0
    }

    fun clearHand() {
        handTiles = emptyList()
        winIndex = null
    }

    fun markWin(index: Int) {
        if (index in handTiles.indices) winIndex = index
    }

    fun autoWinIndex() {
        if (winIndex == null && handTiles.isNotEmpty()) winIndex = handTiles.size - 1
    }

    fun addMeld(m: Meld) {
        // 校验：副露张数与手牌不重复超 4 张
        for (t in m.tiles.distinct()) {
            if (totalCount(t) + m.tiles.count { it == t } > 4) return
        }
        melds = melds + m
    }

    fun removeMeld(index: Int) {
        if (index in melds.indices) melds = melds.filterIndexed { i, _ -> i != index }
    }

    /** 某牌在"手牌+副露+表宝牌+里宝牌"中的合计张数 */
    private fun combinedCount(t: Tile): Int =
        totalCount(t) +
            doraIndicators.count { it == t } +
            uraDoraIndicators.count { it == t }

    fun canAddDora(t: Tile): Boolean = combinedCount(t) < 4

    fun canAddUraDora(t: Tile): Boolean =
        (riichi || doubleRiichi) && combinedCount(t) < 4

    fun addDora(t: Tile) {
        if (canAddDora(t)) doraIndicators = doraIndicators + t
    }

    fun removeDora(index: Int) {
        doraIndicators = doraIndicators.filterIndexed { i, _ -> i != index }
    }

    fun addUraDora(t: Tile) {
        // 里宝牌仅在立直/双立直时允许添加，且与手牌/表宝牌合计不超过 4 张
        if (canAddUraDora(t)) {
            uraDoraIndicators = uraDoraIndicators + t
        }
    }

    fun removeUraDora(index: Int) {
        uraDoraIndicators = uraDoraIndicators.filterIndexed { i, _ -> i != index }
    }

    fun calculate() {
        autoWinIndex()
        val winId = winIndex?.let { handTiles.getOrNull(it) }
        if (winId == null) {
            result = ScoreOutcome.Error("请先输入手牌（含和牌张，点击手牌可标记）")
            return
        }
        result = RiichiScorer.score(
            HandConfig(
                tiles = handTiles,
                melds = melds,
                winId = winId,
                isTsumo = isTsumo,
                isDealer = isDealer,
                roundWind = roundWind,
                seatWind = seatWind,
                riichi = riichi,
                doubleRiichi = doubleRiichi,
                ippatsu = ippatsu,
                chankan = chankan,
                haitei = haitei,
                houtei = houtei,
                doraIndicators = doraIndicators,
                uraDoraIndicators = uraDoraIndicators,
                firstDraw = firstDraw,
                countedYakuman = countedYakuman,
                doubleYakuman = doubleYakuman,
            )
        )
    }

    fun reset() {
        handTiles = emptyList()
        melds = emptyList()
        winIndex = null
        doraIndicators = emptyList()
        uraDoraIndicators = emptyList()
        chankan = false
        haitei = false
        houtei = false
        result = null
        riichi = false
        doubleRiichi = false
        ippatsu = false
        firstDraw = false
    }
}
