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
import cn.xulun.bazi.model.LiuQinResult
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed

/**
 * 六亲展示区域（容器）。第一块：祖业——年柱（祖上宫）十神喜忌/神煞/冲合断语；
 * 第二块：外公家族——伤官居年月柱而旺制官杀（外祖父星）。
 * 理论见 .liuqin-reference.md。后续父母/兄弟/子女等章节在此容器内追加子块。
 */
@Composable
fun LiuQinSection(result: LiuQinResult, modifier: Modifier = Modifier) {
    val zuYe = result.zuYe
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "六亲",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "宫位+星曜合断——祖业看年柱（祖上宫）",
                fontSize = 11.sp,
                color = Color(0xFF9E9E9E),
                modifier = Modifier.padding(top = 4.dp),
            )

            // ── 祖业 ──
            Text(
                text = "—— 祖业 ——",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SeedRed,
                modifier = Modifier.padding(top = 10.dp),
            )
            if (zuYe.favorable.isEmpty() && zuYe.unfavorable.isEmpty()) {
                Text(
                    text = "年柱无特殊喜忌信号，祖业平平。",
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            zuYe.favorable.forEach { item ->
                Text(
                    text = "◆ " + item.dim,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(text = item.text, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            }
            zuYe.unfavorable.forEach { item ->
                Text(
                    text = "◇ " + item.dim,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SeedRed,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(text = item.text, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Text(
                text = zuYe.summary,
                fontSize = 12.sp,
                color = Color(0xFF6D4C41),
                modifier = Modifier.padding(top = 14.dp),
            )

            // ── 外公家族 ──
            Text(
                text = "—— 外公家族 ——",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SeedRed,
                modifier = Modifier.padding(top = 18.dp),
            )
            if (result.waiGong.items.isEmpty()) {
                Text(
                    text = result.waiGong.summary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            result.waiGong.items.forEach { item ->
                Text(
                    text = "◇ " + item.dim,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SeedRed,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(text = item.text, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            }
            if (result.waiGong.items.isNotEmpty()) {
                Text(
                    text = result.waiGong.summary,
                    fontSize = 12.sp,
                    color = Color(0xFF6D4C41),
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
        }
    }
}
