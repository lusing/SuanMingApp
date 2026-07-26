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
import cn.xulun.bazi.model.DiZhiRelationResult
import cn.xulun.bazi.ui.theme.ChongColor
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed
import cn.xulun.bazi.ui.theme.XingColor
import cn.xulun.bazi.ui.theme.elementColor

/** 地支关系（三合 · 六冲 · 相刑）展示区域 */
@Composable
fun DiZhiRelationSection(result: DiZhiRelationResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "地支关系",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            if (!result.hasAny) {
                Text(
                    text = "此八字原局地支无刑冲合之象",
                    fontSize = 13.sp,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(top = 10.dp),
                )
                return@Column
            }

            // 地支三合
            if (result.sanHeList.isNotEmpty()) {
                SectionLabel("地支三合", Modifier.padding(top = 12.dp))
                result.sanHeList.forEach { he ->
                    RelationChip(
                        text = he.description,
                        color = elementColor(he.resultElement),
                        subtitle = he.location,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }

            // 地支六冲
            if (result.chongList.isNotEmpty()) {
                SectionLabel("地支六冲", Modifier.padding(top = 12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    result.chongList.forEach { chong ->
                        RelationChip(
                            text = chong.description,
                            color = ChongColor,
                            subtitle = chong.location,
                        )
                    }
                }
            }

            // 地支相刑
            if (result.xingList.isNotEmpty()) {
                SectionLabel("地支相刑", Modifier.padding(top = 12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    result.xingList.forEach { xing ->
                        RelationChip(
                            text = xing.description,
                            color = XingColor,
                            subtitle = xing.location,
                        )
                    }
                }
            }
        }
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
