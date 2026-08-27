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
import cn.xulun.bazi.model.CaiYunKind
import cn.xulun.bazi.model.CaiYunResult
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed

/**
 * 财运·岁运批断展示区域——**全面铺开**：原局基调 + 每步大运和战评级 +
 * 运内**全部十年流年**逐年显示十神、喜忌、与原局/大运的刑冲会合害、
 * 合化成功与否、财运判定（发/破/谨慎/平稳）与断语。大运流年是八字的主要部分，能算尽算。
 * 理论见 .caiyun-reference.md——和局发财、战局破财。
 */
@Composable
fun CaiYunSection(result: CaiYunResult, modifier: Modifier = Modifier) {
    fun kindColor(k: CaiYunKind): Color = when (k) {
        CaiYunKind.FA -> Color(0xFF2E7D32)
        CaiYunKind.PO -> SeedRed
        CaiYunKind.JIN -> Color(0xFF8D6E00)
        CaiYunKind.PING -> Color(0xFF6D7576)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "财运·岁运批断",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "和局发财·战局破财——大运定基础，流年逐年批：十神/喜忌/刑冲会合害/合化成否/财运",
                fontSize = 11.sp,
                color = Color(0xFF9E9E9E),
                modifier = Modifier.padding(top = 4.dp),
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
            result.yuns.forEach { yun ->
                Text(
                    text = "▍" + yun.label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SeedRed,
                    modifier = Modifier.padding(top = 14.dp),
                )
                Text(
                    text = yun.heZhan + "。" + yun.text,
                    fontSize = 12.sp,
                    color = Color(0xFF6D4C41),
                    modifier = Modifier.padding(top = 2.dp),
                )
                yun.years.forEach { y ->
                    val color = kindColor(y.kind)
                    Text(
                        text = "${y.kind.chinese} ${y.label}（${y.ganZhiGods}｜${y.xiJi}）",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    if (y.relations.isNotEmpty()) {
                        Text(
                            text = "作用：" + y.relations.joinToString("、"),
                            fontSize = 12.sp,
                            color = Color(0xFF00695C),
                            modifier = Modifier.padding(top = 1.dp),
                        )
                    }
                    Text(
                        text = y.text,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 1.dp),
                    )
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
