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
import cn.xulun.bazi.model.YunRelationResult
import cn.xulun.bazi.ui.YunSelection
import cn.xulun.bazi.ui.theme.ChongColor
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed
import cn.xulun.bazi.ui.theme.XingColor
import cn.xulun.bazi.ui.theme.elementColor

/**
 * 运岁关系展示区域：选大运时为原局+大运（五柱）；选流年时为原局+大运+流年（六柱）。
 * 展示天干五合、地支六合、三合、三会、六冲、相刑。
 */
@Composable
fun YunRelationSection(
    result: YunRelationResult,
    selection: YunSelection,
    modifier: Modifier = Modifier,
) {
    val (title, hint, emptyText) = when (selection) {
        is YunSelection.DaYun -> Triple(
            "运岁关系（大运 ${selection.pillar.fullName} ↔ 原局）",
            "并入大运后五柱之间的全部刑冲合；含「大运」者为运岁引动",
            "并入大运后五柱无刑冲合之象",
        )
        is YunSelection.LiuNian -> Triple(
            "运岁关系（流年 ${selection.pillar.fullName} · 大运 ${selection.yun.pillar.fullName} ↔ 原局）",
            "并入流年与其大运后六柱之间的全部刑冲合；含「大运/流年」者为运岁引动，含跨原局·大运·流年三者凑成的三合三会三刑",
            "并入流年与大运后六柱无刑冲合之象",
        )
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = hint,
                fontSize = 11.sp,
                color = Color(0xFF9E9E9E),
                modifier = Modifier.padding(top = 4.dp),
            )

            if (!result.hasAny) {
                Text(
                    text = emptyText,
                    fontSize = 13.sp,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(top = 10.dp),
                )
                return@Column
            }

            // 天干五合
            if (result.stemHeHuaList.isNotEmpty()) {
                SectionLabel("天干五合", Modifier.padding(top = 12.dp))
                ChipFlow {
                    result.stemHeHuaList.forEach {
                        RelationChip(it.description, elementColor(it.resultElement), it.location)
                    }
                }
            }

            // 地支六合
            if (result.branchLiuHeList.isNotEmpty()) {
                SectionLabel("地支六合", Modifier.padding(top = 12.dp))
                ChipFlow {
                    result.branchLiuHeList.forEach {
                        RelationChip(it.description, elementColor(it.resultElement), it.location)
                    }
                }
            }

            // 地支三合
            if (result.branchSanHeList.isNotEmpty()) {
                SectionLabel("地支三合", Modifier.padding(top = 12.dp))
                result.branchSanHeList.forEach {
                    RelationChip(
                        text = it.description,
                        color = elementColor(it.resultElement),
                        subtitle = it.location,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }

            // 地支三会
            if (result.branchSanHuiList.isNotEmpty()) {
                SectionLabel("地支三会", Modifier.padding(top = 12.dp))
                result.branchSanHuiList.forEach {
                    RelationChip(
                        text = it.description,
                        color = elementColor(it.resultElement),
                        subtitle = it.location,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }

            // 地支六冲
            if (result.branchChongList.isNotEmpty()) {
                SectionLabel("地支六冲", Modifier.padding(top = 12.dp))
                ChipFlow {
                    result.branchChongList.forEach {
                        RelationChip(it.description, ChongColor, it.location)
                    }
                }
            }

            // 地支相刑
            if (result.branchXingList.isNotEmpty()) {
                SectionLabel("地支相刑", Modifier.padding(top = 12.dp))
                ChipFlow {
                    result.branchXingList.forEach {
                        RelationChip(it.description, XingColor, it.location)
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
private fun RelationChip(
    text: String,
    color: Color,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = color,
        )
        Text(
            text = "（$subtitle）",
            fontSize = 11.sp,
            color = Color(0xFF9E9E9E),
            modifier = Modifier.padding(start = 2.dp),
        )
    }
}
