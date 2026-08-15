@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.riichi.mahjong.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.riichi.mahjong.core.Meld
import com.riichi.mahjong.core.MeldType
import com.riichi.mahjong.core.Tile

private val WIND_TILES = listOf(Tile.EAST, Tile.SOUTH, Tile.WEST, Tile.NORTH)

@Composable
fun InputScreen(vm: ScorerViewModel, onCalculate: () -> Unit) {
    var showMeldDialog by remember { mutableStateOf(false) }
    var showDoraDialog by remember { mutableStateOf(false) }
    var showUraDialog by remember { mutableStateOf(false) }

    val expected = 14 - 3 * vm.melds.size

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ===== 和牌条件 =====
        SectionCard("和牌条件") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label("庄家", Modifier.weight(0.28f))
                SingleChoiceSegmentedButtonRow(Modifier.weight(0.72f)) {
                    SegmentedButton(
                        selected = vm.isDealer,
                        onClick = { vm.updateDealer(true) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                    ) { Text("亲家") }
                    SegmentedButton(
                        selected = !vm.isDealer,
                        onClick = { vm.updateDealer(false) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                    ) { Text("子家") }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label("和牌", Modifier.weight(0.28f))
                SingleChoiceSegmentedButtonRow(Modifier.weight(0.72f)) {
                    SegmentedButton(
                        selected = vm.isTsumo,
                        onClick = { vm.updateTsumo(true) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                    ) { Text("自摸") }
                    SegmentedButton(
                        selected = !vm.isTsumo,
                        onClick = { vm.updateTsumo(false) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                    ) { Text("荣和") }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label("场风", Modifier.weight(0.28f))
                FlowRow(Modifier.weight(0.72f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WIND_TILES.forEach { w ->
                        WindChip(w, w == vm.roundWind) { vm.roundWind = w }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label("自风", Modifier.weight(0.28f))
                FlowRow(Modifier.weight(0.72f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WIND_TILES.forEach { w ->
                        // 亲家必为东家；子家自风只能是南/西/北
                        val enabled = if (vm.isDealer) w == Tile.EAST else w != Tile.EAST
                        WindChip(w, w == vm.seatWind, enabled = enabled) { vm.seatWind = w }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = vm.riichi,
                    onClick = { vm.updateRiichi(!vm.riichi) },
                    label = { Text("立直") },
                    colors = FilterChipDefaults.filterChipColors(),
                )
                FilterChip(
                    selected = vm.doubleRiichi,
                    onClick = { vm.updateDoubleRiichi(!vm.doubleRiichi) },
                    label = { Text("双立直") },
                )
                FilterChip(
                    selected = vm.ippatsu,
                    onClick = { vm.ippatsu = !vm.ippatsu },
                    enabled = vm.riichi || vm.doubleRiichi,
                    label = { Text("一发") },
                )
                FilterChip(
                    selected = vm.firstDraw,
                    onClick = { vm.updateFirstDraw(!vm.firstDraw) },
                    enabled = vm.isTsumo,
                    label = { Text(if (vm.isDealer) "天和" else "地和") },
                )
            }
            Spacer(Modifier.height(10.dp))
            // 特殊和牌方式（仅凭牌型无法判断，需用户指定牌局状况）
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = vm.chankan,
                    onClick = { vm.chankan = !vm.chankan },
                    enabled = !vm.isTsumo,
                    label = { Text("枪杠和") },
                )
                FilterChip(
                    selected = vm.haitei,
                    onClick = { vm.haitei = !vm.haitei; if (vm.haitei) vm.firstDraw = false },
                    enabled = vm.isTsumo,
                    label = { Text("海底捞月") },
                )
                FilterChip(
                    selected = vm.houtei,
                    onClick = { vm.houtei = !vm.houtei },
                    enabled = !vm.isTsumo,
                    label = { Text("河底摸鱼") },
                )
            }
        }

        // ===== 手牌 =====
        SectionCard("手牌（${vm.handTiles.size}/$expected）") {
            if (vm.melds.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    vm.melds.forEachIndexed { idx, meld ->
                        MeldRow(meld) { vm.removeMeld(idx) }
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }
            OutlinedButton(onClick = { showMeldDialog = true }) {
                Text("＋ 添加副露（顺子/刻子/杠）")
            }
            Spacer(Modifier.height(10.dp))
            if (vm.handTiles.isEmpty()) {
                Text("请从下方牌山点击添加 $expected 张手牌（含和牌张，点击手牌可设为和牌张）", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    vm.handTiles.forEachIndexed { idx, t ->
                        MahjongTile(
                            tile = t,
                            selected = vm.winIndex == idx,
                            winMark = vm.winIndex == idx,
                            onClick = { vm.markWin(idx) },
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "点击手牌可标记/更换和牌张（金色）",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { vm.clearHand() }) { Text("清空") }
                }
            }
            Spacer(Modifier.height(6.dp))
            // 牌山
            TilePicker(vm)
        }

        // ===== 宝牌 =====
        SectionCard("宝牌 / 里宝牌") {
            IndicatorRow("表宝牌", vm.doraIndicators, onRemove = { vm.removeDora(it) }) {
                showDoraDialog = true
            }
            Spacer(Modifier.height(8.dp))
            IndicatorRow(
                "里宝牌",
                vm.uraDoraIndicators,
                onRemove = { vm.removeUraDora(it) },
                enabled = vm.riichi || vm.doubleRiichi,
            ) {
                showUraDialog = true
            }
            Text(
                if (vm.riichi || vm.doubleRiichi) "里宝牌仅在立直/双立直时计入"
                else "里宝牌仅在立直/双立直时可添加并计入（当前未立直）",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ===== 规则设置 =====
        SectionCard("规则设置") {
            SwitchRow("累计役满（13番以上按役满）", vm.countedYakuman) { vm.countedYakuman = it }
            Spacer(Modifier.height(6.dp))
            SwitchRow("双倍役满（四暗刻单骑/国士十三面/纯正九莲）", vm.doubleYakuman) { vm.doubleYakuman = it }
        }

        Button(
            onClick = onCalculate,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("计算点数", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
    }

    if (showMeldDialog) {
        MeldDialog(
            vm = vm,
            onDismiss = { showMeldDialog = false },
        )
    }
    if (showDoraDialog) {
        IndicatorPickerDialog(
            title = "选择表宝牌指示牌",
            canAdd = { vm.canAddDora(it) },
            onAdd = { vm.addDora(it) },
            onDismiss = { showDoraDialog = false },
        )
    }
    if (showUraDialog) {
        IndicatorPickerDialog(
            title = "选择里宝牌指示牌",
            canAdd = { vm.canAddUraDora(it) },
            onAdd = { vm.addUraDora(it) },
            onDismiss = { showUraDialog = false },
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun Label(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = modifier)
}

@Composable
private fun WindChip(wind: Tile, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(wind.display()) },
    )
}

@Composable
private fun MeldRow(meld: Meld, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        val typeName = when (meld.type) {
            MeldType.CHI -> "顺子"
            MeldType.PON -> "刻子"
            MeldType.KAN_OPEN -> "明杠"
            MeldType.KAN_CLOSED -> "暗杠"
        }
        Text(
            typeName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(40.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            meld.tiles.forEach { MahjongTile(it, small = true) }
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onRemove) { Text("删除") }
    }
}

@Composable
private fun TilePicker(vm: ScorerViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(0 to "万", 1 to "筒", 2 to "索").forEach { (suit, name) ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(22.dp))
                (1..9).forEach { r ->
                    val t = Tile.of(suit, r)
                    PickerTile(t, vm.totalCountOf(t)) { vm.addTile(t) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("字", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(22.dp))
            (27..33).forEach { id ->
                val t = Tile(id)
                PickerTile(t, vm.totalCountOf(t)) { vm.addTile(t) }
            }
        }
    }
}

@Composable
private fun PickerTile(t: Tile, used: Int, onClick: () -> Unit) {
    val enabled = used < 4
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(38.dp)) {
        MahjongTile(t, small = true, onClick = if (enabled) onClick else null)
        Text(
            if (enabled) "" else "满",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun IndicatorRow(
    title: String,
    list: List<Tile>,
    onRemove: (Int) -> Unit,
    enabled: Boolean = true,
    onAdd: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Label(title, Modifier.width(64.dp))
        if (list.isEmpty()) {
            Text(
                if (enabled) "无" else "需立直",
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
                modifier = Modifier.weight(1f),
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.weight(1f),
            ) {
                list.forEachIndexed { idx, t ->
                    MahjongTile(t, small = true, onClick = { onRemove(idx) })
                }
            }
        }
        OutlinedButton(onClick = onAdd, enabled = enabled, modifier = Modifier.height(34.dp)) { Text("＋") }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

// ================= 副露对话框 =================

@Composable
private fun MeldDialog(vm: ScorerViewModel, onDismiss: () -> Unit) {
    var type by remember { mutableStateOf(MeldType.CHI) }
    var selected by remember { mutableStateOf<List<Tile>>(emptyList()) }

    val isValid = when (type) {
        MeldType.CHI -> selected.size == 3 && isSequence(selected)
        MeldType.PON, MeldType.KAN_OPEN, MeldType.KAN_CLOSED -> selected.size == 1
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("添加副露", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(MeldType.CHI to "顺子", MeldType.PON to "刻子", MeldType.KAN_OPEN to "明杠", MeldType.KAN_CLOSED to "暗杠")
                        .forEach { (t, name) ->
                            FilterChip(selected = type == t, onClick = { type = t; selected = emptyList() }, label = { Text(name) })
                        }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    when (type) {
                        MeldType.CHI -> "依次点击 3 张连续的顺子（如 2-3-4 万）"
                        MeldType.PON -> "点击一张牌组成刻子"
                        else -> "点击一张牌组成杠子（手牌与副露合计不超过 4 张）"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                if (selected.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        if (type == MeldType.CHI) selected.sortedBy { it.id }.forEach {
                            MahjongTile(it, small = true)
                        } else {
                            val count = if (type == MeldType.CHI) 3 else if (type == MeldType.PON) 3 else 4
                            repeat(count) { MahjongTile(selected[0], small = true) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                TileGridForMeld(selected) { t ->
                    selected = when (type) {
                        MeldType.CHI -> {
                            if (selected.contains(t)) selected.filter { it != t }
                            else if (selected.size < 3) selected + t else selected
                        }
                        else -> if (selected.contains(t)) emptyList() else listOf(t)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("取消") }
                    Button(
                        onClick = {
                            when (type) {
                                MeldType.CHI -> vm.addMeld(Meld(MeldType.CHI, selected.sortedBy { it.id }))
                                MeldType.PON -> vm.addMeld(Meld(MeldType.PON, List(3) { selected[0] }))
                                MeldType.KAN_OPEN -> vm.addMeld(Meld(MeldType.KAN_OPEN, List(4) { selected[0] }))
                                MeldType.KAN_CLOSED -> vm.addMeld(Meld(MeldType.KAN_CLOSED, List(4) { selected[0] }))
                            }
                            onDismiss()
                        },
                        enabled = isValid,
                        modifier = Modifier.weight(1f),
                    ) { Text("确认添加") }
                }
            }
        }
    }
}

private fun isSequence(tiles: List<Tile>): Boolean {
    if (tiles.size != 3) return false
    val s = tiles.sortedBy { it.id }
    val suit = s[0].suit
    return suit < 3 && s.all { it.suit == suit } && s[1].id == s[0].id + 1 && s[2].id == s[1].id + 1
}

@Composable
private fun TileGridForMeld(selected: List<Tile>, onTile: (Tile) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(0 to "万", 1 to "筒", 2 to "索").forEach { (suit, name) ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(22.dp))
                (1..9).forEach { r ->
                    val t = Tile.of(suit, r)
                    MahjongTile(t, small = true, selected = selected.contains(t), onClick = { onTile(t) })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("字", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(22.dp))
            (27..33).forEach { id ->
                val t = Tile(id)
                MahjongTile(t, small = true, selected = selected.contains(t), onClick = { onTile(t) })
            }
        }
    }
}

// ================= 宝牌指示牌对话框 =================

@Composable
private fun IndicatorPickerDialog(
    title: String,
    canAdd: (Tile) -> Boolean,
    onAdd: (Tile) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text("点击牌面添加指示牌（与手牌合计每种最多 4 张）", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0 to "万", 1 to "筒", 2 to "索").forEach { (suit, name) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(name, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(22.dp))
                            (1..9).forEach { r ->
                                val t = Tile.of(suit, r)
                                MahjongTile(t, small = true, onClick = if (canAdd(t)) { { onAdd(t) } } else null)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("字", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(22.dp))
                        (27..33).forEach { id ->
                            val t = Tile(id)
                            MahjongTile(t, small = true, onClick = if (canAdd(t)) { { onAdd(t) } } else null)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("完成") }
            }
        }
    }
}
