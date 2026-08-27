package cn.xulun.bazi.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.WealthResult
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed
import cn.xulun.bazi.ui.theme.elementColor

/** 财运分析展示区域：财星 · 富格 · 财库（岁运逐年批断见财运·岁运批断区块） */
@Composable
fun WealthSection(result: WealthResult, modifier: Modifier = Modifier) {
    val wealthColor = elementColor(result.wealthElement)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "财运分析",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "日主所克为${result.wealthElement.chinese}（财），财星即十神之正财/偏财",
                fontSize = 12.sp,
                color = Color(0xFF757575),
                modifier = Modifier.padding(top = 4.dp),
            )

            // 财星
            SectionLabel("财星", Modifier.padding(top = 14.dp))
            if (result.stars.isEmpty()) {
                HintText("原局财星不显（天干与藏干俱无正/偏财）")
            } else {
                ChipFlow {
                    result.stars.forEach {
                        WealthChip(
                            text = "${it.stem.chinese} ${it.tenGod.chinese}",
                            subtitle = "${it.location}·${if (it.hidden) "藏" else "透"}",
                            color = wealthColor,
                        )
                    }
                }
            }

            // 身财·富格
            SectionLabel("身财 · 富格", Modifier.padding(top = 14.dp))
            Text(
                text = result.pattern.chinese,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = SeedRed,
            )
            Text(
                text = result.pattern.summary,
                fontSize = 12.sp,
                color = Color(0xFF616161),
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = "依据：${result.patternBasis}",
                fontSize = 11.sp,
                color = Color(0xFF9E9E9E),
                modifier = Modifier.padding(top = 2.dp),
            )

            // 财库
            SectionLabel("财库", Modifier.padding(top = 14.dp))
            val s = result.storage
            val earthNote = if (result.wealthElement == Element.EARTH) "（土寄水库，历来有争议）" else ""
            val presence = if (s.present) "原局见（${s.location}）" else "原局无"
            val openState = when {
                !s.present -> ""
                s.opened -> "，逢${s.openBy}开库，财动"
                else -> "，闭合未开"
            }
            Text(
                text = "财库${s.branch.chinese}$earthNote · $presence$openState",
                fontSize = 13.sp,
                color = if (s.opened) SeedRed else Color(0xFF333333),
            )

        }
    }
}

@Composable
private fun ChipFlow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        content()
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = SeedRed.copy(alpha = 0.8f),
        modifier = modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun HintText(text: String) {
    Text(text = text, fontSize = 13.sp, color = Color(0xFF9E9E9E))
}

@Composable
private fun WealthChip(text: String, subtitle: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = color)
        Text(
            text = "（$subtitle）",
            fontSize = 11.sp,
            color = Color(0xFF9E9E9E),
            modifier = Modifier.padding(start = 2.dp),
        )
    }
}
