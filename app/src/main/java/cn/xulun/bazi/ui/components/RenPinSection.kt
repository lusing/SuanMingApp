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
import cn.xulun.bazi.model.RenPinResult
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed

/** 人品推断展示区域：总评 · 心性条目 · 岁运改变 · 用人交友建议 */
@Composable
fun RenPinSection(result: RenPinResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "人品推断",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "十神心性与五行特性复合——掀开内心世界看清人品（用人交友之资）",
                fontSize = 12.sp,
                color = Color(0xFF757575),
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(result.verdict, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = SeedRed, modifier = Modifier.padding(top = 10.dp))
            result.items.forEach {
                Text(
                    "◆ ${it.title}" + (if (it.favorable) "" else "（负面）"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (it.favorable) SeedRed else Color(0xFF8D6E63),
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(it.text, fontSize = 11.sp, color = Color(0xFF616161), modifier = Modifier.padding(top = 1.dp))
            }
            if (result.yunNote.isNotEmpty()) {
                Text("岁运改变：${result.yunNote}", fontSize = 12.sp, color = Color(0xFF616161), modifier = Modifier.padding(top = 8.dp))
            }
            Text("用人交友：${result.advice}", fontSize = 12.sp, color = SeedRed.copy(alpha = 0.85f), modifier = Modifier.padding(top = 6.dp))
        }
    }
}
