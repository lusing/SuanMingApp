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
import cn.xulun.bazi.model.HealthResult
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed

/**
 * 健康展示区域：健康基调 + 疾病倾向（受损五行→脏腑）。理论见 .health-reference.md。
 */
@Composable
fun HealthSection(result: HealthResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "健康·疾病批断",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "五行对应脏腑——原局定基调，受损之行定易患之疾，岁运引发定发病之期",
                fontSize = 11.sp,
                color = Color(0xFF9E9E9E),
                modifier = Modifier.padding(top = 4.dp),
            )
            result.items.forEach { item ->
                Text(
                    text = "◆ " + item.dim,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(text = item.text, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            }
            result.risks.forEach { item ->
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
                text = result.summary,
                fontSize = 12.sp,
                color = Color(0xFF6D4C41),
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}
