package com.riichi.mahjong.core

/**
 * 麻将牌（34 种）：万子 0..8、筒子 9..17、索子 18..26、字牌 27..33（东南西北白发中）
 */
@JvmInline
value class Tile(val id: Int) {

    val suit: Int get() = id / 9
    val rank: Int get() = id % 9 + 1

    val isHonor: Boolean get() = suit == SUIT_HONOR
    val isTerminal: Boolean get() = suit < SUIT_HONOR && (rank == 1 || rank == 9)
    val isTerminalOrHonor: Boolean get() = isTerminal || isHonor
    val isSimple: Boolean get() = !isHonor && rank in 2..8

    /** 宝牌指示牌 -> 宝牌 */
    fun dora(): Tile {
        if (isHonor) {
            return when (rank) {
                1 -> Tile.of(SUIT_HONOR, 2)
                2 -> Tile.of(SUIT_HONOR, 3)
                3 -> Tile.of(SUIT_HONOR, 4)
                4 -> Tile.of(SUIT_HONOR, 1)
                5 -> Tile.of(SUIT_HONOR, 6)
                6 -> Tile.of(SUIT_HONOR, 7)
                else -> Tile.of(SUIT_HONOR, 5)
            }
        }
        return Tile.of(suit, if (rank == 9) 1 else rank + 1)
    }

    fun display(): String = displayOf(this)

    companion object {
        const val SUIT_MAN = 0
        const val SUIT_PIN = 1
        const val SUIT_SOU = 2
        const val SUIT_HONOR = 3

        val HAKU = Tile(31)
        val HATSU = Tile(32)
        val CHUN = Tile(33)
        val EAST = Tile(27)
        val SOUTH = Tile(28)
        val WEST = Tile(29)
        val NORTH = Tile(30)
        val DRAGONS = listOf(HAKU, HATSU, CHUN)

        fun of(suit: Int, rank: Int): Tile = Tile(suit * 9 + rank - 1)

        /** 全部 34 种牌 */
        fun all(): List<Tile> = (0..33).map { Tile(it) }

        private val RANK_CHARS = listOf("一", "二", "三", "四", "五", "六", "七", "八", "九")
        private val HONOR_CHARS = listOf("东", "南", "西", "北", "白", "发", "中")
        private val SUIT_CHARS = listOf("万", "筒", "索")

        fun displayOf(t: Tile): String =
            if (t.isHonor) HONOR_CHARS[t.rank - 1]
            else RANK_CHARS[t.rank - 1] + SUIT_CHARS[t.suit]
    }
}

/** 统计牌数（34 长度） */
fun countsOf(tiles: List<Tile>): IntArray {
    val c = IntArray(34)
    for (t in tiles) c[t.id]++
    return c
}
