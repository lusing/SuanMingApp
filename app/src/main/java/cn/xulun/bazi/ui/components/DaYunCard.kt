package cn.xulun.bazi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xulun.bazi.model.DaYun
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.tenGodOfStem
import cn.xulun.bazi.ui.YunSelection
import cn.xulun.bazi.ui.theme.CreamBg
import cn.xulun.bazi.ui.theme.PillarBorder
import cn.xulun.bazi.ui.theme.SeedRed

/** 一步大运卡片：干支 + 十神、起止、10 个流年。点头部选大运，点流年行选流年。 */
@Composable
fun DaYunCard(
    yun: DaYun,
    dayMaster: HeavenlyStem,
    selection: YunSelection?,
    onSelect: (YunSelection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val god = tenGodOfStem(dayMaster, yun.pillar.stem).chinese
    val isYunSelected = selection is YunSelection.DaYun && selection.yun == yun
    Column(
        modifier = modifier
            .width(132.dp)
            .background(
                if (isYunSelected) SeedRed.copy(alpha = 0.10f) else CreamBg,
                RoundedCornerShape(10.dp),
            )
            .border(
                width = if (isYunSelected) 2.dp else 1.dp,
                color = if (isYunSelected) SeedRed else PillarBorder,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable { onSelect(YunSelection.DaYun(yun)) }
            .padding(8.dp),
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
            yun.liuNian.forEach { ln ->
                val isLnSelected = selection is YunSelection.LiuNian && selection.liuNian == ln
                Text(
                    text = "${ln.year} ${ln.pillar.fullName} ${tenGodOfStem(dayMaster, ln.pillar.stem).chinese}",
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    color = if (isLnSelected) SeedRed else Color(0xFF333333),
                    fontWeight = if (isLnSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isLnSelected) SeedRed.copy(alpha = 0.15f) else Color.Transparent,
                            RoundedCornerShape(4.dp),
                        )
                        .clickable { onSelect(YunSelection.LiuNian(yun, ln)) }
                        .padding(vertical = 1.dp),
                )
            }
        }
    }
}
