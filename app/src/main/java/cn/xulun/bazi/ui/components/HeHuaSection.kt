package cn.xulun.bazi.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import cn.xulun.bazi.model.HeHuaResult
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed
import cn.xulun.bazi.ui.theme.elementColor

/** 合化分析展示区域 */
@Composable
fun HeHuaSection(result: HeHuaResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "干支合化",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            if (!result.hasAny) {
                Text(
                    text = "此八字原局无合化关系",
                    fontSize = 13.sp,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(top = 10.dp),
                )
                return@Column
            }

            // 天干五合
            if (result.stemHeHuaList.isNotEmpty()) {
                SectionLabel("天干合", Modifier.padding(top = 12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    result.stemHeHuaList.forEach { he ->
                        HeHuaChip(
                            text = he.description,
                            element = he.resultElement,
                            subtitle = he.location,
                        )
                    }
                }
            }

            // 地支六合
            if (result.branchLiuHeList.isNotEmpty()) {
                SectionLabel("地支六合", Modifier.padding(top = 12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    result.branchLiuHeList.forEach { he ->
                        HeHuaChip(
                            text = he.description,
                            element = he.resultElement,
                            subtitle = he.location,
                        )
                    }
                }
            }

            // 地支三会
            if (result.branchSanHuiList.isNotEmpty()) {
                SectionLabel("地支三会", Modifier.padding(top = 12.dp))
                result.branchSanHuiList.forEach { he ->
                    HeHuaChip(
                        text = he.description,
                        element = he.resultElement,
                        subtitle = he.location,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
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
private fun HeHuaChip(
    text: String,
    element: Element,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val color = elementColor(element)
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