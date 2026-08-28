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
import cn.xulun.bazi.model.TianFuResult
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed

/** 天赋与职业选择展示区域：天赋条目 · 职业方向 · 行运取舍 */
@Composable
fun TianFuSection(result: TianFuResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "天赋与职业",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "十神含义与五行特性反映天赋信息，职业取舍配合行运",
                fontSize = 12.sp,
                color = Color(0xFF757575),
                modifier = Modifier.padding(top = 4.dp),
            )
            result.items.forEach {
                Text("◆ ${it.title}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = SeedRed, modifier = Modifier.padding(top = 8.dp))
                Text(it.text, fontSize = 11.sp, color = Color(0xFF616161), modifier = Modifier.padding(top = 1.dp))
            }
            if (result.careers.isNotEmpty()) {
                Text(
                    text = "适合职业：" + result.careers.joinToString("；"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = SeedRed,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            if (result.yunNote.isNotEmpty()) {
                Text("行运取舍：${result.yunNote}", fontSize = 12.sp, color = Color(0xFF616161), modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}
