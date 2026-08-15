package com.riichi.mahjong.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riichi.mahjong.core.Points
import com.riichi.mahjong.core.ScoreOutcome
import com.riichi.mahjong.core.ScoreResult

@Composable
fun ResultScreen(outcome: ScoreOutcome, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when (outcome) {
            is ScoreOutcome.Error -> ErrorCard(outcome.message)
            is ScoreOutcome.Success -> ResultContent(outcome.result)
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("返回修改", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("无法计算", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ResultContent(r: ScoreResult) {
    // 顶部：手牌名 + 等级
    val tierName = tierNameOf(r)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(r.handName, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            if (tierName != null) {
                Box(
                    Modifier
                        .background(tierColorOf(r), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(tierName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }

    // 番数 / 符数 / 基本点
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatBox("番数", if (r.isYakuman) "役满" else "${r.totalHan}番", Modifier.weight(1f))
        StatBox("符数", if (r.isYakuman && r.fu == 0) "—" else "${r.fu}符", Modifier.weight(1f))
    }

    // 点数
    PointsCard(r)

    // 番型明细
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("番型明细", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            if (r.yakuman.isNotEmpty()) {
                val grouped = r.yakuman.groupingBy { it }.eachCount()
                grouped.forEach { (name, count) ->
                    YakuRow(name, if (count > 1) "役满×$count" else "役满")
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
            }
            r.yaku.forEach { (name, han) -> YakuRow(name, "${han}番") }
            if (r.doraHan > 0) YakuRow("宝牌", "+${r.doraHan}番", accent = true)
            if (r.uraDoraHan > 0) YakuRow("里宝牌", "+${r.uraDoraHan}番", accent = true)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            YakuRow("合计", if (r.isYakuman) "役满" else "${r.totalHan}番", total = true)
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(
            Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun PointsCard(r: ScoreResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (val p = r.points) {
                is Points.Ron -> {
                    Text("荣和 · 放铳者支付", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("${p.pay} 点", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                }
                is Points.Tsumo -> {
                    Text(
                        if (p.isDealer) "亲家自摸 · 各家支付" else "子家自摸 · 亲家/子家支付",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (p.isDealer) "每家 ${p.each} 点" else "子家 ${p.ko} · 亲家 ${p.oya}",
                        color = Color.White,
                        fontSize = if (p.isDealer) 34.sp else 30.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun YakuRow(name: String, value: String, accent: Boolean = false, total: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (total || accent) FontWeight.Bold else FontWeight.Normal,
            color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (total) FontWeight.Bold else FontWeight.Medium,
            color = if (total) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun tierNameOf(r: ScoreResult): String? {
    if (r.isYakuman) return if (r.yakumanCount > 1) "双倍役满" else "役满"
    return when {
        r.totalHan >= 13 -> "累计役满"
        r.totalHan >= 11 -> "三倍满"
        r.totalHan >= 8 -> "倍满"
        r.totalHan >= 6 -> "跳满"
        r.totalHan >= 5 -> "满贯"
        else -> null
    }
}

private fun tierColorOf(r: ScoreResult): Color = when {
    r.isYakuman -> Color(0xFFB71C1C)
    r.totalHan >= 13 -> Color(0xFFB71C1C)
    r.totalHan >= 11 -> Color(0xFFE65100)
    r.totalHan >= 8 -> Color(0xFFAD1457)
    r.totalHan >= 6 -> Color(0xFF6A1B9A)
    r.totalHan >= 5 -> Color(0xFFF57F17)
    else -> Color(0xFF37474F)
}
