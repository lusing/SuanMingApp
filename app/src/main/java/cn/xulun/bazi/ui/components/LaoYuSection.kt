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
import cn.xulun.bazi.model.LaoYuResult
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed

/** 牢狱之灾展示区域：原局伏险 + 岁运引发（谨慎参考——预防可减免）。 */
@Composable
fun LaoYuSection(result: LaoYuResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "牢狱之灾早提防",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "原局伏险＋岁运引发——防微杜渐，守法自律可免（谨慎参考）",
                fontSize = 12.sp,
                color = Color(0xFF757575),
                modifier = Modifier.padding(top = 4.dp),
            )
            result.baseRisks.forEach {
                Text("◆ ${it.dim}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = SeedRed, modifier = Modifier.padding(top = 8.dp))
                Text(it.text, fontSize = 11.sp, color = Color(0xFF616161), modifier = Modifier.padding(top = 1.dp))
            }
            result.years.forEach { y ->
                Text("${y.label}——${y.triggers.joinToString("；")}", fontSize = 12.sp, color = Color(0xFF8D6E63), modifier = Modifier.padding(top = 8.dp))
                Text(y.text, fontSize = 11.sp, color = Color(0xFF616161), modifier = Modifier.padding(top = 1.dp))
            }
        }
    }
}
