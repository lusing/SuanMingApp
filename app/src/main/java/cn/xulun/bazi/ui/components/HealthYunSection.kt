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
import cn.xulun.bazi.model.HealthYearKind
import cn.xulun.bazi.model.HealthYunResult
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed

/**
 * 健康·岁运批断展示区域——全面铺开：原局病根 + 各步大运**全部流年**逐年
 * 病发/缓解/车祸/平安与引发源。理论见 .health-reference.md。
 */
@Composable
fun HealthYunSection(result: HealthYunResult, modifier: Modifier = Modifier) {
    fun kindColor(k: HealthYearKind): Color = when (k) {
        HealthYearKind.BING -> SeedRed
        HealthYearKind.CHE_HUO -> Color(0xFFB71C1C)
        HealthYearKind.HUAJIE -> Color(0xFF2E7D32)
        HealthYearKind.PING -> Color(0xFF6D7576)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "健康·岁运批断",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "原局伏病根→大运成形→流年引发——病发/缓解/车祸逐年批",
                fontSize = 11.sp,
                color = Color(0xFF9E9E9E),
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = result.rootNote,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp),
            )
            result.yuns.forEach { yun ->
                Text(
                    text = "▍" + yun.label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SeedRed,
                    modifier = Modifier.padding(top = 14.dp),
                )
                yun.years.forEach { y ->
                    val hasSignal = y.kind != HealthYearKind.PING
                    Text(
                        text = "${y.kind.chinese} ${y.label}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = kindColor(y.kind),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    if (y.triggers.isNotEmpty()) {
                        Text(
                            text = "引发：" + y.triggers.joinToString("、"),
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
