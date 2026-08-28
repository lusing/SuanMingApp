package cn.xulun.bazi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xulun.bazi.model.DaYun
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.LiuNianDetail
import cn.xulun.bazi.model.tenGodOfStem
import cn.xulun.bazi.ui.theme.CreamBg
import cn.xulun.bazi.ui.theme.PillarBorder
import cn.xulun.bazi.ui.theme.SeedRed

/**
 * 一步大运卡片：干支 + 十神、起止、每流年细断全面铺开——
 * 干支十神/喜忌/刑冲克害/三合三会/六合五合/暗合/合化成否/格局变化。
 */
@Composable
fun DaYunCard(
    yun: DaYun,
    dayMaster: HeavenlyStem,
    years: List<LiuNianDetail> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val god = tenGodOfStem(dayMaster, yun.pillar.stem).chinese
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CreamBg, RoundedCornerShape(10.dp))
            .border(1.dp, PillarBorder, RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "${yun.pillar.fullName} · $god",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "${yun.startAge}岁起 · ${yun.startYear}-${yun.endYear}",
            fontSize = 11.sp,
            color = Color(0xFF757575),
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            years.forEach { y ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                ) {
                    Text(
                        text = "${y.year} ${y.ganZhi}  ${y.tenGods}  ${y.xiJi}",
                        fontSize = 12.sp,
                        color = Color(0xFF333333),
                    )
                    if (y.relations.isNotEmpty()) {
                        Text(
                            text = y.relations.joinToString("；"),
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = Color(0xFF8D6E63),
                        )
                    }
                    if (y.geJu != null) {
                        Text(
                            text = y.geJu,
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = SeedRed,
                        )
                    }
                }
            }
        }
    }
}
