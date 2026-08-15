package com.riichi.mahjong.core

/** 面子（一组 3 张牌） */
sealed class Group {
    abstract val tiles: List<Tile>

    /** 顺子 */
    class Sequence(override val tiles: List<Tile>) : Group()

    /** 刻子（含杠的 3 张） */
    class Triplet(override val tiles: List<Tile>) : Group()
}

/** 一次分解：若干面子 + 雀头 */
data class Decomposition(val groups: List<Group>, val pair: Tile)

/** 和牌张在分解中的位置 */
data class Placement(val inPair: Boolean, val groupIdx: Int, val pos: Int)

/**
 * 手牌分解：把门前牌拆成 groupsNeeded 个面子 + 1 个雀头。
 */
object Decomposer {

    fun decompose(counts: IntArray, groupsNeeded: Int): List<Decomposition> {
        val results = mutableListOf<Decomposition>()
        val groups = mutableListOf<Group>()
        val c = counts.copyOf()
        fun rec(remainingGroups: Int, remainingPairs: Int, pair: Tile?) {
            var i = -1
            for (k in 0 until 34) if (c[k] > 0) { i = k; break }
            if (i == -1) {
                if (remainingGroups == 0 && remainingPairs == 0) {
                    results.add(Decomposition(groups.toList(), pair!!))
                }
                return
            }
            val tile = Tile(i)
            // 雀头
            if (remainingPairs > 0 && c[i] >= 2) {
                c[i] -= 2
                rec(remainingGroups, remainingPairs - 1, tile)
                c[i] += 2
            }
            // 刻子
            if (remainingGroups > 0 && c[i] >= 3) {
                c[i] -= 3
                groups.add(Group.Triplet(listOf(tile, tile, tile)))
                rec(remainingGroups - 1, remainingPairs, pair)
                groups.removeAt(groups.size - 1)
                c[i] += 3
            }
            // 顺子
            if (remainingGroups > 0 && tile.suit < Tile.SUIT_HONOR && tile.rank <= 7 &&
                c[i + 1] > 0 && c[i + 2] > 0
            ) {
                c[i]--; c[i + 1]--; c[i + 2]--
                groups.add(Group.Sequence(listOf(Tile(i), Tile(i + 1), Tile(i + 2))))
                rec(remainingGroups - 1, remainingPairs, pair)
                groups.removeAt(groups.size - 1)
                c[i]++; c[i + 1]++; c[i + 2]++
            }
        }
        rec(groupsNeeded, 1, null)
        return results
    }

    /** 该分解中，和牌张的所有可能位置 */
    fun placementsOf(decomp: Decomposition, winId: Tile): List<Placement> {
        val out = mutableListOf<Placement>()
        if (decomp.pair == winId) out.add(Placement(inPair = true, groupIdx = -1, pos = -1))
        decomp.groups.forEachIndexed { idx, g ->
            g.tiles.forEachIndexed { pos, t ->
                if (t == winId) out.add(Placement(inPair = false, groupIdx = idx, pos = pos))
            }
        }
        return out
    }

    /** 听牌形 */
    enum class WaitType { RYANMEN, KANCHAN, PENCHAN, TANKI, SHANPON }

    fun waitType(decomp: Decomposition, placement: Placement): WaitType {
        if (placement.inPair) return WaitType.TANKI
        val g = decomp.groups[placement.groupIdx]
        if (g is Group.Triplet) return WaitType.SHANPON
        val others = g.tiles.filterIndexed { i, _ -> i != placement.pos }.sortedBy { it.id }
        val a = others[0]
        val b = others[1]
        if (b.id == a.id + 1) {
            return if (a.rank == 1 || a.rank == 8) WaitType.PENCHAN else WaitType.RYANMEN
        }
        return WaitType.KANCHAN
    }
}
