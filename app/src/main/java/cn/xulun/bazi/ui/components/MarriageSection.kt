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
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.MarriageResult
import cn.xulun.bazi.ui.theme.ChongColor
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed
import cn.xulun.bazi.ui.theme.elementColor

/** 婚姻分析展示区域：配偶星 · 夫妻宫 · 婚姻格局 · 神煞 */
@Composable
fun MarriageSection(result: MarriageResult, modifier: Modifier = Modifier) {
    val starHint = if (result.gender == Gender.MALE)
        "男命以财星（正财/偏财）为妻" else "女命以官杀（正官/七杀）为夫"
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "婚姻分析",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(starHint, fontSize = 12.sp, color = Color(0xFF757575), modifier = Modifier.padding(top = 4.dp))

            // 配偶星
            SectionLabel("配偶星（${result.spouseStarName}）", Modifier.padding(top = 14.dp))
            if (result.spouseStars.isEmpty()) {
                HintText("配偶星不显（天干与藏干俱无）")
            } else {
                ChipFlow {
                    result.spouseStars.forEach {
                        RelationChip(
                            text = "${it.stem.chinese} ${it.tenGod.chinese}",
                            subtitle = "${it.location}·${if (it.hidden) "藏" else "透"}",
                            color = elementColor(it.stem.element),
                        )
                    }
                }
            }

            // 夫妻宫
            SectionLabel("夫妻宫（日支）", Modifier.padding(top = 14.dp))
            val p = result.palace
            Text("日支${p.branch.chinese} · 本气${p.mainGod.chinese}", fontSize = 13.sp)
            Text(
                text = "藏干：" + p.hiddenGods.joinToString(" ") { "${it.first.chinese}(${it.second.chinese})" },
                fontSize = 11.sp,
                color = Color(0xFF757575),
                modifier = Modifier.padding(top = 2.dp),
            )
            when {
                p.chong != null -> Text("逢冲：${p.chong}", fontSize = 13.sp, color = ChongColor, modifier = Modifier.padding(top = 2.dp))
                p.xing != null -> Text("逢刑：${p.xing}", fontSize = 13.sp, color = ChongColor, modifier = Modifier.padding(top = 2.dp))
                else -> Text("夫妻宫安稳（无冲无刑）", fontSize = 13.sp, color = Color(0xFF616161), modifier = Modifier.padding(top = 2.dp))
            }
            if (p.liuHe != null) {
                Text("逢合：${p.liuHe}", fontSize = 13.sp, color = elementColor(Element.WOOD), modifier = Modifier.padding(top = 2.dp))
            }

            // 婚姻格局
            SectionLabel("婚姻格局", Modifier.padding(top = 14.dp))
            Text(result.pattern.chinese, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = SeedRed)
            Text(result.pattern.summary, fontSize = 12.sp, color = Color(0xFF616161), modifier = Modifier.padding(top = 2.dp))
            Text("依据：${result.patternBasis}", fontSize = 11.sp, color = Color(0xFF9E9E9E), modifier = Modifier.padding(top = 2.dp))

            // 神煞
            SectionLabel("神煞", Modifier.padding(top = 14.dp))
            if (result.shenSha.isEmpty()) {
                HintText("原局无桃花、孤辰、寡宿")
            } else {
                ChipFlow {
                    result.shenSha.forEach {
                        val color = if (it.name == "桃花") SeedRed else Color(0xFF9E9E9E)
                        RelationChip(text = "${it.name} ${it.branch.chinese}", subtitle = it.location, color = color)
                    }
                }
            }
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
private fun RelationChip(text: String, subtitle: String, color: Color) {
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
