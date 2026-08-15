package com.riichi.mahjong.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riichi.mahjong.core.Tile
import com.riichi.mahjong.ui.theme.ChunRed
import com.riichi.mahjong.ui.theme.HatsuGreen
import com.riichi.mahjong.ui.theme.HonorBlack
import com.riichi.mahjong.ui.theme.Ivory
import com.riichi.mahjong.ui.theme.ManRed
import com.riichi.mahjong.ui.theme.PinBlue
import com.riichi.mahjong.ui.theme.SouGreen
import com.riichi.mahjong.ui.theme.WinGold

private val RANK_CHARS = listOf("一", "二", "三", "四", "五", "六", "七", "八", "九")
private val SUIT_CHARS = listOf("万", "筒", "索")
private val HONOR_CHARS = listOf("东", "南", "西", "北", "白", "发", "中")

/**
 * 麻将牌面。
 *
 * @param selected 选中态（高亮边框）
 * @param winMark 和牌张标记（金色边框 + 顶部"和"角标）
 */
@Composable
fun MahjongTile(
    tile: Tile,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    winMark: Boolean = false,
    small: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val tileSize = if (small) 28.dp else 40.dp
    val corner = if (small) 3.dp else 5.dp
    val shape = RoundedCornerShape(corner)
    Box(
        modifier = modifier
            .size(width = tileSize * 0.78f, height = tileSize)
            .clip(shape)
            .background(Ivory)
            .border(1.5.dp, borderColor(tile, selected, winMark), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val glyphSize = if (small) 13.sp.toPx() else 18.sp.toPx()
            val paint = Paint().apply {
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                this.textSize = glyphSize
            }
            val cx = size.width / 2f
            val cy = size.height / 2f
            if (tile.isHonor) {
                if (tile == Tile.HAKU) {
                    // 白板：留白
                } else {
                    paint.color = when (tile) {
                        Tile.HATSU -> HatsuGreen
                        Tile.CHUN -> ChunRed
                        else -> HonorBlack
                    }.toArgb()
                    drawContext.canvas.nativeCanvas.drawText(HONOR_CHARS[tile.rank - 1], cx, cy + glyphSize * 0.35f, paint)
                }
            } else {
                paint.color = when (tile.suit) {
                    0 -> ManRed
                    1 -> PinBlue
                    else -> SouGreen
                }.toArgb()
                drawContext.canvas.nativeCanvas.drawText(RANK_CHARS[tile.rank - 1], cx, cy + glyphSize * 0.33f, paint)
                // 小花色角标
                val smallPaint = Paint().apply {
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    this.textSize = if (small) 8.sp.toPx() else 10.sp.toPx()
                    this.color = paint.color
                }
                drawContext.canvas.nativeCanvas.drawText(
                    SUIT_CHARS[tile.suit],
                    size.width - smallPaint.textSize * 0.55f,
                    size.height - smallPaint.textSize * 0.25f,
                    smallPaint,
                )
            }
        }
        if (winMark) {
            Canvas(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(if (small) 9.dp else 12.dp)
            ) {
                drawCircle(color = WinGold)
                val p = Paint().apply {
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    this.textSize = (if (small) 7.sp else 9.sp).toPx()
                    this.color = Color.White.toArgb()
                }
                drawContext.canvas.nativeCanvas.drawText("和", size.width / 2f, size.height * 0.72f, p)
            }
        }
    }
}

private fun borderColor(tile: Tile, selected: Boolean, winMark: Boolean): Color = when {
    winMark -> WinGold
    selected -> Color(0xFF1E88E5)
    tile.isHonor -> Color(0xFF8D6E63)
    else -> Color(0xFFB0A894)
}
