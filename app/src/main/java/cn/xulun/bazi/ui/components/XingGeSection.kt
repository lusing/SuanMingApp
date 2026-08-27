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
import cn.xulun.bazi.model.XingGeResult
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed

/**
 * 性格分析展示区域。理论见 .shengke-reference.md 第六五节（含六 合断八步法）。
 */
@Composable
fun XingGeSection(result: XingGeResult, composite: XingGeResult? = null, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "性格分析",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "五行气势/十神/神煞/多重 + 合断八步（干支复合·主流与潜伏）",
                fontSize = 11.sp,
                color = Color(0xFF9E9E9E),
                modifier = Modifier.padding(top = 4.dp),
            )
            if (composite != null) {
                Text(
                    text = "—— 合断（十神心性×五行特性复合）——",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SeedRed,
                    modifier = Modifier.padding(top = 10.dp),
                )
                composite.items.forEach { item ->
                    Text(
                        text = "◆ " + item.dim,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SeedRed,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(
                        text = item.text,
                        fontSize = 13.sp,
                        color = Color(0xFF424242),
                        lineHeight = 20.sp,
                    )
                }
                Text(
                    text = "—— 分线细断 ——",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SeedRed,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
            result.items.forEach { item ->
                Text(
                    text = "◆ " + item.dim,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SeedRed,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    text = item.text,
                    fontSize = 13.sp,
                    color = Color(0xFF424242),
                    lineHeight = 20.sp,
                )
            }
        }
    }
}
