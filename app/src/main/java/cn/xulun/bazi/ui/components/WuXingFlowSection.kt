package cn.xulun.bazi.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import cn.xulun.bazi.model.FlowType
import cn.xulun.bazi.model.WuXingFlowResult
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed

/** 五行流通 / 战局 / 混杂（试验）展示区域。 */
@Composable
fun WuXingFlowSection(result: WuXingFlowResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "运岁五行（试验）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            val names = listOf("木", "火", "土", "金", "水")
            Text(
                text = "五行计数：${names.mapIndexed { i, n -> "$n${result.elems[i]}" }.joinToString("  ")}　相生链长 ${result.chain}",
                fontSize = 13.sp,
                color = Color(0xFF616161),
            )
            if (result.war.jinMu || result.war.shuiHuo) {
                val wars = buildList {
                    if (result.war.jinMu) add("金木交战")
                    if (result.war.shuiHuo) add("水火交战")
                }.joinToString("、")
                Text(text = "战局：$wars", fontSize = 12.sp, color = SeedRed)
            }
            if (result.duanYu.isEmpty()) {
                Text(text = "无明显流通/战局之象", fontSize = 12.sp, color = Color(0xFF9E9E9E))
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    result.duanYu.forEach { d ->
                        Text(
                            text = d.text,
                            fontSize = 12.sp,
                            color = if (d.type == FlowType.FAVOR) Color(0xFF2E7D32) else SeedRed,
                        )
                    }
                }
            }
        }
    }
}
