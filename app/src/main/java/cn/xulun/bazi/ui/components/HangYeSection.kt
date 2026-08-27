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
import cn.xulun.bazi.model.HangYeResult
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed

/**
 * 行业选择（性格断+用神断）展示区域。理论见 .shengke-reference.md 第六四节。
 */
@Composable
fun HangYeSection(result: HangYeResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "行业选择",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "性格信号断行业 + 用神五行定行业（对命主有益者）",
                fontSize = 11.sp,
                color = Color(0xFF9E9E9E),
                modifier = Modifier.padding(top = 4.dp),
            )
            result.items.forEach { item ->
                Text(
                    text = "◆ " + item.trait,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SeedRed,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    text = "宜：" + item.field,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2E7D32),
                )
                Text(
                    text = item.basis,
                    fontSize = 12.sp,
                    color = Color(0xFF616161),
                    lineHeight = 18.sp,
                )
            }
        }
    }
}
