package cn.xulun.bazi.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xulun.bazi.model.HunLianKind
import cn.xulun.bazi.model.HunYinResult
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed

/**
 * 婚姻展示区域——全面铺开：配偶星/配偶宫辨识 + 原局断语（有利◆/不利◇）
 * + 各步大运**全部流年**逐年显示婚恋性质（恋爱/同居/婚庆/动荡/无信号）、
 * 引动源（宫冲合刑暗合加临生助/星透当值/桃花）与年龄提示（法定婚龄男22女20）。
 * 理论见 .hunyin-reference.md。
 */
@Composable
fun HunYinSection(result: HunYinResult, modifier: Modifier = Modifier) {
    fun kindColor(k: HunLianKind): Color = when (k) {
        HunLianKind.LIAN_AI -> Color(0xFF2E7D32)
        HunLianKind.TONG_JU -> Color(0xFFE65100)
        HunLianKind.HUN_QING -> Color(0xFFAD1457)
        HunLianKind.BIAN_DONG -> SeedRed
        HunLianKind.WU -> Color(0xFF6D7576)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "婚姻·婚恋批断",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "配偶星（男财女官）+配偶宫（日支）——宫星引动定婚恋之年，逐年批：引动/性质/年龄",
                fontSize = 11.sp,
                color = Color(0xFF9E9E9E),
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = result.spouseStarNote,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                text = result.palaceNote,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp),
            )
            result.baseItems.forEach { item ->
                Text(
                    text = "◆ " + item.dim,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(text = item.text, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            }
            result.baseUnfav.forEach { item ->
                Text(
                    text = "◇ " + item.dim,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SeedRed,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(text = item.text, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            }
            result.yuns.forEach { yun ->
                Text(
                    text = "▍" + yun.label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SeedRed,
                    modifier = Modifier.padding(top = 14.dp),
                )
                yun.years.forEach { y ->
                    val hasSignal = y.kind != HunLianKind.WU
                    Text(
                        text = "${y.kind.chinese} ${y.label}（${y.ageNote}）",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = kindColor(y.kind),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    if (y.triggers.isNotEmpty()) {
                        Text(
                            text = "引动：" + y.triggers.joinToString("、"),
                            fontSize = 12.sp,
                            color = Color(0xFF00695C),
                            modifier = Modifier.padding(top = 1.dp),
                        )
                    }
                    if (hasSignal) {
                        Text(text = y.text, fontSize = 12.sp, modifier = Modifier.padding(top = 1.dp))
                    }
                }
            }
            Text(
                text = result.summary,
                fontSize = 12.sp,
                color = Color(0xFF6D4C41),
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}
