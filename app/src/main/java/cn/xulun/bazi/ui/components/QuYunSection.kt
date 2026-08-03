package cn.xulun.bazi.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import cn.xulun.bazi.model.QuYunResult
import cn.xulun.bazi.model.QuYunVerdict
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed

/** 取运（论 X 取运）展示：选中运柱对本命子配置的干支喜忌。 */
@Composable
fun QuYunSection(result: QuYunResult, yunLabel: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "取运（${yunLabel}）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "子配置：${result.ziPeiZhi.familyCh}·${result.ziPeiZhi.subName}",
                fontSize = 13.sp,
                color = Color(0xFF616161),
            )
            VerdictRow(label = "运干", v = result.gan)
            VerdictRow(label = "运支", v = result.zhi)
            Text(
                text = "总评：${result.summary.text}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = SeedRed.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun VerdictRow(label: String, v: cn.xulun.bazi.model.PillarVerdict) {
    val color = when (v.verdict) {
        QuYunVerdict.XI -> Color(0xFF2E7D32)
        QuYunVerdict.JI -> SeedRed
        else -> Color(0xFF616161)
    }
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(text = "$label·${v.godCh}", fontSize = 13.sp, color = Color(0xFF616161))
        Text(
            text = "（${v.verdict.name}）",
            fontSize = 12.sp,
            color = color,
            modifier = Modifier.padding(start = 4.dp),
        )
        Text(text = " ${v.reason}", fontSize = 12.sp, color = Color(0xFF9E9E9E))
    }
}
