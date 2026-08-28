package cn.xulun.bazi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import cn.xulun.bazi.model.tenGodOfStem
import cn.xulun.bazi.ui.theme.CreamBg
import cn.xulun.bazi.ui.theme.PillarBorder

/** 一步大运卡片：干支 + 十神、起止、10 个流年（纯展示，全面铺开不可点选）。 */
@Composable
fun DaYunCard(
    yun: DaYun,
    dayMaster: HeavenlyStem,
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
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            yun.liuNian.forEach { ln ->
                Text(
                    text = "${ln.year} ${ln.pillar.fullName} ${tenGodOfStem(dayMaster, ln.pillar.stem).chinese}",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFF333333),
                )
            }
        }
    }
}
