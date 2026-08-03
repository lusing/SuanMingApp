package cn.xulun.bazi.ui.components

import androidx.compose.foundation.layout.Arrangement
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
import cn.xulun.bazi.model.JieChongResult
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed

/** 会合解冲刑（《子平真诠》第七章）展示区域。无冲刑则整块隐藏。 */
@Composable
fun JieChongSection(result: JieChongResult, modifier: Modifier = Modifier) {
    if (result.chong.isEmpty() && result.xing.isEmpty()) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "会合解冲刑",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            result.chong.forEach { c ->
                Text(
                    text = if (c.resolved) "${c.description} → 解（${c.resolveBy}）"
                    else "${c.description} → 冲成立（无会合相解）",
                    fontSize = 13.sp,
                    color = if (c.resolved) Color(0xFF2E7D32) else SeedRed,
                )
            }
            result.xing.forEach { x ->
                Text(
                    text = if (x.resolved) "${x.description}（${x.involved}） → 解（${x.resolveBy}）"
                    else "${x.description}（${x.involved}） → 刑成立（无会合相解）",
                    fontSize = 13.sp,
                    color = if (x.resolved) Color(0xFF2E7D32) else SeedRed,
                )
            }
        }
    }
}
