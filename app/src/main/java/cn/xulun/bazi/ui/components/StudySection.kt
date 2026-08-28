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
import cn.xulun.bazi.model.StudyResult
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed
import cn.xulun.bazi.ui.theme.elementColor

/** 学业/学历分析展示区域：印星 · 学历格局 · 学业神煞 · 考学岁运 */
@Composable
fun StudySection(result: StudyResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "学业分析",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "以印星（正印/偏印）为学历主星，参看食伤、官杀与日主旺衰",
                fontSize = 12.sp,
                color = Color(0xFF757575),
                modifier = Modifier.padding(top = 4.dp),
            )

            // 印星
            SectionLabel("印星（学历主星）", Modifier.padding(top = 14.dp))
            if (result.sealStars.isEmpty()) {
                HintText("印星不显（天干与藏干俱无正/偏印）")
            } else {
                ChipFlow {
                    result.sealStars.forEach {
                        RelationChip(
                            text = "${it.stem.chinese} ${it.tenGod.chinese}",
                            subtitle = "${it.location}·${if (it.hidden) "藏" else "透"}",
                            color = elementColor(it.stem.element),
                        )
                    }
                }
            }

            // 学历格局
            SectionLabel("学历格局", Modifier.padding(top = 14.dp))
            Text(result.pattern.chinese, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = SeedRed)
            Text(result.pattern.summary, fontSize = 12.sp, color = Color(0xFF616161), modifier = Modifier.padding(top = 2.dp))
            Text("依据：${result.patternBasis}", fontSize = 11.sp, color = Color(0xFF9E9E9E), modifier = Modifier.padding(top = 2.dp))

            // 文凭推断（用神+印星为标志，少运定高低）
            SectionLabel("文凭推断", Modifier.padding(top = 14.dp))
            val dip = result.diploma
            Text(dip.level, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = SeedRed)
            Text("依据：${dip.basis}", fontSize = 11.sp, color = Color(0xFF9E9E9E), modifier = Modifier.padding(top = 2.dp))
            if (dip.favorable.isNotEmpty()) {
                Text("有利文凭", fontSize = 12.sp, color = Color(0xFF757575), modifier = Modifier.padding(top = 8.dp))
                dip.favorable.forEach {
                    Text("◆ ${it.title}", fontSize = 12.sp, color = SeedRed, modifier = Modifier.padding(top = 3.dp))
                    Text(it.text, fontSize = 11.sp, color = Color(0xFF616161), modifier = Modifier.padding(top = 1.dp))
                }
            }
            if (dip.unfavorable.isNotEmpty()) {
                Text("不利文凭", fontSize = 12.sp, color = Color(0xFF757575), modifier = Modifier.padding(top = 8.dp))
                dip.unfavorable.forEach {
                    Text("◆ ${it.title}", fontSize = 12.sp, color = Color(0xFF8D6E63), modifier = Modifier.padding(top = 3.dp))
                    Text(it.text, fontSize = 11.sp, color = Color(0xFF616161), modifier = Modifier.padding(top = 1.dp))
                }
            }
            // 读书天赋（食伤=智商/文昌为食神，印=文凭学校，水旺主智）
            SectionLabel("读书天赋", Modifier.padding(top = 14.dp))
            Text(dip.talent, fontSize = 12.sp, color = Color(0xFF616161))
            if (dip.talentYears.isNotEmpty()) {
                Text("学龄流年心性", fontSize = 12.sp, color = Color(0xFF757575), modifier = Modifier.padding(top = 6.dp))
                dip.talentYears.forEach {
                    Text("${it.label}——${it.text}", fontSize = 11.sp, color = Color(0xFF616161), modifier = Modifier.padding(top = 2.dp))
                }
            }

            if (dip.goodYears.isNotEmpty()) {
                Text("利文途流年（干支和局——高考必中式）", fontSize = 12.sp, color = Color(0xFF757575), modifier = Modifier.padding(top = 8.dp))
                dip.goodYears.forEach {
                    Text("${it.label}——${it.text}", fontSize = 11.sp, color = SeedRed, modifier = Modifier.padding(top = 2.dp))
                }
            }
            if (dip.badYears.isNotEmpty()) {
                Text("不利文途流年", fontSize = 12.sp, color = Color(0xFF757575), modifier = Modifier.padding(top = 8.dp))
                dip.badYears.forEach {
                    Text("${it.label}——${it.text}", fontSize = 11.sp, color = Color(0xFF8D6E63), modifier = Modifier.padding(top = 2.dp))
                }
            }

            // 学业神煞
            SectionLabel("学业神煞", Modifier.padding(top = 14.dp))
            if (result.shenSha.isEmpty()) {
                HintText("原局无文昌、学堂")
            } else {
                ChipFlow {
                    result.shenSha.forEach {
                        RelationChip(text = "${it.name} ${it.branch.chinese}", subtitle = it.location, color = SeedRed)
                    }
                }
            }

            // 考学岁运
            SectionLabel("考学岁运", Modifier.padding(top = 14.dp))
            val yf = result.yunFortune
            if (yf.dayun.isEmpty() && yf.liunian.isEmpty()) {
                HintText("未排大运")
            } else {
                if (yf.dayun.isNotEmpty()) {
                    Text("利考学的大运", fontSize = 12.sp, color = Color(0xFF757575))
                    yf.dayun.forEach {
                        Text("${it.label} · ${it.desc}", fontSize = 12.sp, color = SeedRed, modifier = Modifier.padding(top = 2.dp))
                    }
                }
                if (yf.liunian.isNotEmpty()) {
                    Text("利考学的流年", fontSize = 12.sp, color = Color(0xFF757575), modifier = Modifier.padding(top = 8.dp))
                    ChipFlow {
                        yf.liunian.forEach {
                            RelationChip(text = it.label, subtitle = it.desc, color = SeedRed)
                        }
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
