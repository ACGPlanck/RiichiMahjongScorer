package com.riichi.mahjong.core

/** 副露种类 */
enum class MeldType { CHI, PON, KAN_OPEN, KAN_CLOSED }

/** 副露（顺子/刻子/杠子） */
data class Meld(val type: MeldType, val tiles: List<Tile>) {
    init {
        require(tiles.size == if (isKan) 4 else 3) { "副露张数不正确" }
        require(tiles.distinct().size == if (type == MeldType.CHI) 3 else 1) { "副露牌型不正确" }
        if (type == MeldType.CHI) {
            val s = tiles.sortedBy { it.id }
            require(s[0].suit < Tile.SUIT_HONOR && s[1].id == s[0].id + 1 && s[2].id == s[1].id + 1) {
                "顺子必须为同一花色连续三张"
            }
        }
    }

    val isKan: Boolean get() = type == MeldType.KAN_OPEN || type == MeldType.KAN_CLOSED
    val isTriplet: Boolean get() = type != MeldType.CHI
}
