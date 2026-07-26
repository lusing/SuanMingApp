package cn.xulun.bazi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.hiddenStemGods
import cn.xulun.bazi.model.tenGodOfStem
import cn.xulun.bazi.ui.theme.SeedRed
import cn.xulun.bazi.ui.theme.elementColor

/** 单柱卡片：标签、天干十神、天干、地支、藏干及十神、生肖 */
@Composable
fun PillarCard(
    label: String,
    pillar: Pillar,
    dayMaster: HeavenlyStem,
    isDayPillar: Boolean,
    modifier: Modifier = Modifier,
) {
    val stemGod = if (isDayPillar) "日主" else tenGodOfStem(dayMaster, pillar.stem).chinese
    val hiddenGods = hiddenStemGods(dayMaster, pillar.branch)
    val stemColor = elementColor(pillar.stem.element)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF9E9E9E),
        )
        Text(
            text = stemGod,
            fontSize = 11.sp,
            fontWeight = if (isDayPillar) FontWeight.Bold else FontWeight.Normal,
            color = if (isDayPillar) SeedRed else Color(0xFF616161),
            modifier = Modifier.padding(top = 4.dp),
        )
        // 天干
        CharBox(
            char = pillar.stem.chinese,
            color = stemColor,
            modifier = Modifier.padding(top = 4.dp),
        )
        // 地支
        CharBox(
            char = pillar.branch.chinese,
            color = stemColor,
            fillAlpha = 0.16f,
            modifier = Modifier.padding(top = 4.dp),
        )
        // 藏干及其十神
        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            hiddenGods.forEach { (stem, god) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stem.chinese,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = elementColor(stem.element),
                    )
                    Text(
                        text = god.chinese,
                        fontSize = 9.sp,
                        color = Color(0xFF616161),
                    )
                }
            }
        }
        Text(
            text = pillar.branch.animal,
            fontSize = 11.sp,
            color = Color(0xFF9E9E9E),
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun CharBox(
    char: String,
    color: Color,
    fillAlpha: Float = 0.12f,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .background(color.copy(alpha = fillAlpha), RoundedCornerShape(12.dp))
            .border(1.5.dp, color.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = char,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}
