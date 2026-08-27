package cn.xulun.bazi.ui.components

import androidx.compose.foundation.background
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
import cn.xulun.bazi.model.CaiYunKind
import cn.xulun.bazi.model.CaiYunResult
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.SeedRed
import cn.xulun.bazi.ui.YunSelection

/**
 * 财运·岁运批断展示区域（原局基调 + 各步大运和战评级 + 运内关键流年）。
 * 理论见 .caiyun-reference.md——和局发财、战局破财；前略后细，关键年份为要。
 *
 * 联动：选中的大运整卡高亮；选中的流年在其运内标"◀"，并置顶"本次选中流年批断"
 * （非关键年亦给出"财星未被引动"与该运和战基础的结论）。
 */
@Composable
fun CaiYunSection(result: CaiYunResult, selection: YunSelection?, modifier: Modifier = Modifier) {
    val selectedYunPillar = selection?.let { sel ->
        when (sel) {
            is YunSelection.DaYun -> sel.pillar
            is YunSelection.LiuNian -> sel.yun.pillar
        }
    }
    val selectedYear = (selection as? YunSelection.LiuNian)?.liuNian?.year

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "财运·岁运批断",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "和局发财·战局破财——原局定基调，大运定基础，流年定关键",
                fontSize = 11.sp,
                color = Color(0xFF9E9E9E),
                modifier = Modifier.padding(top = 4.dp),
            )

            // ── 选中流年置顶批断 ──
            if (selectedYear != null) {
                // 定位所属大运（按流年落入选中运的十年区间）
                val selYun = result.yuns.firstOrNull { y ->
                    val yr = y.label.substringAfterLast(" ")
                    yr.contains("-") && selectedYear in (yr.substringBefore("-").toIntOrNull() ?: 0)..(yr.substringAfter("-").toIntOrNull() ?: 0)
                }
                val yearItem = selYun?.years?.firstOrNull { it.year == selectedYear }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(Color(0x14FF5722))
                        .padding(10.dp),
                ) {
                    Text(
                        text = "本次选中流年批断",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SeedRed,
                    )
                    if (yearItem != null) {
                        val color = when (yearItem.kind) {
                            CaiYunKind.FA -> Color(0xFF2E7D32)
                            CaiYunKind.PO -> SeedRed
                            CaiYunKind.JIN -> Color(0xFF8D6E00)
                        }
                        Text(
                            text = "${yearItem.kind.chinese} ${yearItem.label}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(text = yearItem.text, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                    } else {
                        Text(
                            text = "$selectedYear 年",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            text = "该年财星未被引动——非财运关键年，无大起大落之信号；" +
                                (selYun?.let { "所行${it.pillar.fullName}运：${it.heZhan}。${it.text}" } ?: "大运信息待选。"),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }

            result.baseItems.forEach { item ->
                Text(
                    text = "◆ " + item.dim,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(text = item.text, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            }
            result.yuns.forEach { yun ->
                val isSel = selectedYunPillar != null && yun.pillar == selectedYunPillar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .background(if (isSel) Color(0x1FFF9800) else Color.Transparent)
                        .padding(if (isSel) 8.dp else 0.dp),
                ) {
                    Text(
                        text = (if (isSel) "▶ " else "▍") + yun.label + (if (isSel) "（当前选中）" else ""),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSel) Color(0xFFE65100) else SeedRed,
                    )
                    Text(
                        text = yun.heZhan + "。" + yun.text,
                        fontSize = 12.sp,
                        color = Color(0xFF6D4C41),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    yun.years.forEach { y ->
                        val color = when (y.kind) {
                            CaiYunKind.FA -> Color(0xFF2E7D32)
                            CaiYunKind.PO -> SeedRed
                            CaiYunKind.JIN -> Color(0xFF8D6E00)
                        }
                        val yearSel = y.year == selectedYear
                        Text(
                            text = "${y.kind.chinese} ${y.label}" + (if (yearSel) " ◀" else ""),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (yearSel) Color(0xFFE65100) else color,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .background(if (yearSel) Color(0x1FFF9800) else Color.Transparent)
                                .padding(horizontal = if (yearSel) 4.dp else 0.dp),
                        )
                        Text(text = y.text, fontSize = 12.sp, modifier = Modifier.padding(top = 1.dp))
                    }
                }
            }
            Text(
                text = result.summary,
                fontSize = 12.sp,
                color = Color(0xFF6D4C41),
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}
