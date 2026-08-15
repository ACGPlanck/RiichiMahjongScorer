package com.riichi.mahjong.core

/**
 * 和牌输入配置。
 *
 * @param tiles 门前（暗）牌，包含和牌张。张数 = 14 - 3×(副露数+杠数)
 * @param melds 副露（含杠子）
 * @param winId 和牌张
 */
data class HandConfig(
    val tiles: List<Tile>,
    val melds: List<Meld> = emptyList(),
    val winId: Tile,
    val isTsumo: Boolean = false,
    val isDealer: Boolean = false,
    val roundWind: Tile? = null,   // 场风
    val seatWind: Tile? = null,    // 自风
    val riichi: Boolean = false,
    val doubleRiichi: Boolean = false,
    val ippatsu: Boolean = false,
    val chankan: Boolean = false, // 枪杠和牌（荣和他人加杠）
    val haitei: Boolean = false,  // 海底捞月（自摸最后一张）
    val houtei: Boolean = false,  // 河底摸鱼（荣和最后一张弃牌）
    val doraIndicators: List<Tile> = emptyList(),
    val uraDoraIndicators: List<Tile> = emptyList(),
    val firstDraw: Boolean = false, // 第一巡自摸（天和/地和）
    val countedYakuman: Boolean = true,
    val doubleYakuman: Boolean = true,
) {
    /** 门前清（允许暗杠） */
    fun menzen(): Boolean = melds.all { it.type == MeldType.KAN_CLOSED }

    /** 全部牌（含副露） */
    fun allTiles(): List<Tile> = tiles + melds.flatMap { it.tiles }

    fun isYakuhai(t: Tile): Boolean =
        t == roundWind || t == seatWind || t == Tile.HAKU || t == Tile.HATSU || t == Tile.CHUN
}
