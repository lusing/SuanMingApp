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
import cn.xulun.bazi.model.BingYaoResult
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed

/** 病药取用（传统扶抑法：通关/扶抑/顺势/调候）展示区域。 */
@Composable
fun BingYaoSection(result: BingYaoResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "病药取用（扶抑法）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "日主${result.strength.chinese}　法：${result.method.chinese}　" +
                    "用神：${result.yongShen.chinese}（${result.yongShenElement.chinese}）" +
                    (result.yongShenChain.takeIf { it.size >= 2 }?.let { "　链：${it.joinToString("→") { e -> e.chinese }}" } ?: "") +
                    (result.bing?.let { "　病：${it.chinese}" } ?: "　中和无病"),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            val auxiliary = buildList {
                result.xiShen?.let { add("喜 ${it.chinese}") }
                if (result.jiShen.isNotEmpty()) add("忌 ${result.jiShen.joinToString("、") { it.chinese }}")
                if (result.tiaoHou.chinese.isNotEmpty() && result.tiaoHou != cn.xulun.bazi.model.TiaoHouState.NONE)
                    add(result.tiaoHou.chinese)
            }.joinToString("　")
            if (auxiliary.isNotEmpty()) {
                Text(text = auxiliary, fontSize = 13.sp, color = Color(0xFF616161))
            }
            Text(
                text = "干支强弱：${result.strengthRanking}",
                fontSize = 12.sp,
                color = Color(0xFF616161),
            )
            Text(
                text = result.basis,
                fontSize = 12.sp,
                color = if (result.method.chinese == "顺势") SeedRed else Color(0xFF757575),
                lineHeight = 18.sp,
            )
        }
    }
}
