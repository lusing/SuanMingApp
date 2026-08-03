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
import cn.xulun.bazi.model.TiaoHouResult
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed

/** 调候（寒暖燥湿）展示区域。 */
@Composable
fun TiaoHouSection(result: TiaoHouResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "调候",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "气候：${result.climate.chinese}　${result.climateBasis}",
                fontSize = 13.sp,
                color = Color(0xFF616161),
            )
            val elCh = result.tiaoHouElement?.chinese ?: "无"
            Text(
                text = "调候用神：$elCh　${result.tiaoHouNote}",
                fontSize = 13.sp,
                color = Color(0xFF616161),
            )
            if (result.urgent) {
                Text(
                    text = "⚠ ${result.urgentDesc}",
                    fontSize = 12.sp,
                    color = SeedRed,
                )
            }
            if (result.formulas.isNotEmpty()) {
                Text(
                    text = "调候口诀",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SeedRed.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp),
                )
                result.formulas.forEach { f ->
                    Text(
                        text = "【${f.name}】${f.desc}（官星：${f.guanStance}）",
                        fontSize = 12.sp,
                        color = Color(0xFF616161),
                    )
                }
            }
            if (result.deShi.isNotEmpty()) {
                Text(
                    text = "调候得失",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SeedRed.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp),
                )
                result.deShi.forEach {
                    Text(text = "· $it", fontSize = 12.sp, color = Color(0xFF616161))
                }
            }
        }
    }
}
