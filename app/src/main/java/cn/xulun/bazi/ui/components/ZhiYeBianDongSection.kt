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
import cn.xulun.bazi.model.ZhiYeBianDongResult
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed

/** 职业变动展示区域：变动流年（刑冲合官印/驿马/官杀克身/食伤流年/地域冲/冲旺财星）。 */
@Composable
fun ZhiYeBianDongSection(result: ZhiYeBianDongResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "职业变动",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "岁运刑冲合动官杀印星 · 临驿马 · 官杀值年克身重 · 食伤流年（升迁见官运）",
                fontSize = 12.sp,
                color = Color(0xFF757575),
                modifier = Modifier.padding(top = 4.dp),
            )
            result.years.forEach { y ->
                Text("${y.pillar}——${y.triggers.joinToString("；")}", fontSize = 12.sp, color = SeedRed, modifier = Modifier.padding(top = 8.dp))
                Text(y.text, fontSize = 11.sp, color = Color(0xFF616161), modifier = Modifier.padding(top = 1.dp))
            }
        }
    }
}
