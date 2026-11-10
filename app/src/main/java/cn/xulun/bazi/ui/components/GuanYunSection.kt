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
import cn.xulun.bazi.model.GuanYunKind
import cn.xulun.bazi.model.GuanYunResult
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed

/**
 * 官运·仕途批断展示区域——全面铺开：官星辨识+贵命断语+每运十年逐年晋升/贬职/调动。
 * 理论见 .caiyun-reference.md 第七章。
 */
@Composable
fun GuanYunSection(result: GuanYunResult, modifier: Modifier = Modifier) {
    fun kindColor(k: GuanYunKind): Color = when (k) {
        GuanYunKind.JIN_SHENG -> Color(0xFF2E7D32)
        GuanYunKind.BIAN_ZHI -> SeedRed
        GuanYunKind.DIAO_DONG -> Color(0xFFE65100)
        GuanYunKind.PING -> Color(0xFF6D7576)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "官运·仕途批断",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "官星喜忌与力量——为用力足之流年晋升，为忌力足贬职；冲刑马星伤透主调动",
                fontSize = 11.sp,
                color = Color(0xFF9E9E9E),
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = result.officerNote,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp),
            )
            result.nobleItems.forEach { item ->
                Text(
                    text = "◆ " + item.dim,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
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
                    val hasSignal = y.kind != GuanYunKind.PING
                    Text(
                        text = "${y.kind.chinese} ${y.label}",
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
