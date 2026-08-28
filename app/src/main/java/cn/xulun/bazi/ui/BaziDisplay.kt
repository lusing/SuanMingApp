package cn.xulun.bazi.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.xulun.bazi.core.DiZhiRelationCalculator
import cn.xulun.bazi.core.GeJuCalculator
import cn.xulun.bazi.core.HeHuaCalculator
import cn.xulun.bazi.core.JieChongCalculator
import cn.xulun.bazi.core.KongWangCalculator
import cn.xulun.bazi.core.MarriageCalculator
import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.core.StudyCalculator
import cn.xulun.bazi.core.TiaoHouCalculator
import cn.xulun.bazi.core.WealthCalculator
import cn.xulun.bazi.core.YongShenCalculator
import cn.xulun.bazi.core.WuXingFlowCalculator
import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DaYunInfo
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.ui.components.DaYunCard
import cn.xulun.bazi.ui.components.DiZhiRelationSection
import cn.xulun.bazi.ui.components.GeJuBanner
import cn.xulun.bazi.ui.components.HeHuaSection
import cn.xulun.bazi.ui.components.JieChongSection
import cn.xulun.bazi.ui.components.LiuQinSection
import cn.xulun.bazi.ui.components.HealthSection
import cn.xulun.bazi.ui.components.GuanYunSection
import cn.xulun.bazi.ui.components.HealthYunSection
import cn.xulun.bazi.ui.components.HunYinSection
import cn.xulun.bazi.ui.components.MarriageSection
import cn.xulun.bazi.ui.components.PillarCard
import cn.xulun.bazi.ui.components.ShenShaSection
import cn.xulun.bazi.ui.components.StudySection
import cn.xulun.bazi.ui.components.TiaoHouSection
import cn.xulun.bazi.ui.components.CaiYunSection
import cn.xulun.bazi.ui.components.WealthSection
import cn.xulun.bazi.ui.components.BingYaoSection
import cn.xulun.bazi.ui.components.MingGongSection
import cn.xulun.bazi.ui.components.HuoQuYongSection
import cn.xulun.bazi.ui.components.JinQuYongSection
import cn.xulun.bazi.ui.components.MuQuYongSection
import cn.xulun.bazi.ui.components.ShuiQuYongSection
import cn.xulun.bazi.ui.components.TuQuYongSection
import cn.xulun.bazi.ui.components.WuXingFlowSection
import cn.xulun.bazi.ui.components.YongShenSection
import cn.xulun.bazi.ui.theme.CreamSurface
import cn.xulun.bazi.ui.theme.elementColor

private val PillarLabels = listOf("年柱", "月柱", "日柱", "时柱")

/** 八字结果展示（原局 + 格局 + 大运流年） */
@Composable
fun BaziDisplay(result: BaziResult, daYun: DaYunInfo?, gender: Gender, modifier: Modifier = Modifier) {
    val dayMaster = result.dayPillar.stem
    val geju = GeJuCalculator.determine(result)
    val yongShen = YongShenCalculator.analyze(result)
    val tiaoHou = TiaoHouCalculator.analyze(result)
    val heHua = HeHuaCalculator.analyze(result)
    val diZhiRelation = DiZhiRelationCalculator.analyze(result)
    val jieChong = JieChongCalculator.analyze(result)
    val shenSha = ShenShaCalculator.analyze(result)
    val shiShen = cn.xulun.bazi.core.ShiShenCalculator.analyze(result)
    val kongWang = KongWangCalculator.analyze(result)
    val wealth = WealthCalculator.analyze(result)
    // 神煞按柱分组（入柱卡展示，不单列区块）
    val shenShaByPillar: List<List<String>> = listOf("年", "月", "日", "时").map { zi ->
        shenSha.items.filter { it.pillarLabel.startsWith(zi) }.map { it.name }
    }

    Column(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = CreamSurface),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = result.fullBazi,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                )
                GeJuBanner(
                    geju = geju,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    result.pillars.forEachIndexed { i, pillar ->
                        PillarCard(
                            label = PillarLabels[i],
                            pillar = pillar,
                            dayMaster = dayMaster,
                            isDayPillar = i == 2,
                            kong = kongWang.isKong(i),
                            shenSha = shenShaByPillar[i],
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                // 命宫·胎元（紧贴排盘）
                val mingGong = cn.xulun.bazi.core.MingGongCalculator.analyze(result, gender)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "命宫 ${mingGong.mingGong.fullName}·${mingGong.starName}    胎元 ${mingGong.taiYuan.fullName}",
                        fontSize = 12.sp,
                        color = Color(0xFF6D4C41),
                    )
                }
                // 纳音
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    result.pillars.forEachIndexed { i, pillar ->
                        NayinChip(label = PillarLabels[i], pillar = pillar)
                    }
                }
            }
        }
        cn.xulun.bazi.ui.components.ShiShenSection(result = shiShen)
        cn.xulun.bazi.ui.components.HangYeSection(result = cn.xulun.bazi.core.HangYeCalculator.analyze(result))
        cn.xulun.bazi.ui.components.XingGeSection(result = cn.xulun.bazi.core.XingGeCalculator.analyze(result), composite = cn.xulun.bazi.core.XingGeCalculator.analyzeComposite(result))
        YongShenSection(result = yongShen)
        TiaoHouSection(result = tiaoHou)
        if (daYun != null) {
            DaYunSection(
                info = daYun,
                dayMaster = dayMaster,
                yunNian = cn.xulun.bazi.core.YunNianCalculator.analyze(result, daYun),
            )
        }
        HeHuaSection(result = heHua)
        DiZhiRelationSection(result = diZhiRelation)
        JieChongSection(result = jieChong)
        WealthSection(result = wealth)
        daYun?.let { dy ->
            CaiYunSection(result = cn.xulun.bazi.core.CaiYunCalculator.analyze(result, gender, dy))
            GuanYunSection(result = cn.xulun.bazi.core.GuanYunCalculator.analyze(result, dy))
        }
        MarriageSection(result = MarriageCalculator.analyze(result, gender))
        HealthSection(result = cn.xulun.bazi.core.HealthCalculator.analyze(result))
        daYun?.let { dy ->
            HealthYunSection(result = cn.xulun.bazi.core.HealthYunCalculator.analyze(result, dy))
        }
        daYun?.let { dy ->
            HunYinSection(result = cn.xulun.bazi.core.HunYinCalculator.analyze(result, gender, dy))
        }
        LiuQinSection(result = cn.xulun.bazi.core.LiuQinCalculator.analyze(result, gender))
        StudySection(result = StudyCalculator.analyze(result, daYun))
        cn.xulun.bazi.ui.components.JiaoJiSection(result = cn.xulun.bazi.core.JiaoJiCalculator.analyze(result, daYun))
        cn.xulun.bazi.ui.components.TianFuSection(result = cn.xulun.bazi.core.TianFuCalculator.analyze(result, daYun))
        WuXingFlowSection(result = WuXingFlowCalculator.analyze(result, emptyList()))
        BingYaoSection(result = cn.xulun.bazi.core.BingYaoCalculator.analyze(result))
        when (dayMaster.element) {
            cn.xulun.bazi.model.Element.WOOD -> MuQuYongSection(result = cn.xulun.bazi.core.MuQuYongCalculator.analyze(result))
            cn.xulun.bazi.model.Element.FIRE -> HuoQuYongSection(result = cn.xulun.bazi.core.HuoQuYongCalculator.analyze(result))
            cn.xulun.bazi.model.Element.EARTH -> TuQuYongSection(result = cn.xulun.bazi.core.TuQuYongCalculator.analyze(result))
            cn.xulun.bazi.model.Element.METAL -> JinQuYongSection(result = cn.xulun.bazi.core.JinQuYongCalculator.analyze(result))
            cn.xulun.bazi.model.Element.WATER -> ShuiQuYongSection(result = cn.xulun.bazi.core.ShuiQuYongCalculator.analyze(result))
            else -> {}
        }
    }
}

@Composable
private fun NayinChip(label: String, pillar: cn.xulun.bazi.model.Pillar) {
    val color = elementColor(pillar.nayin.element)
    Row(
        modifier = Modifier
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${label}: ${pillar.nayin.chinese}",
            fontSize = 12.sp,
            color = color,
        )
    }
}

@Composable
private fun DaYunSection(
    info: DaYunInfo,
    dayMaster: cn.xulun.bazi.model.HeavenlyStem,
    yunNian: cn.xulun.bazi.model.YunNianResult,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "大运 · ${if (info.forward) "顺行" else "逆行"}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${info.qiYunText}，${info.startDate.year} 年 ${info.startDate.monthValue} 月 ${info.startDate.dayOfMonth} 日交运",
                fontSize = 12.sp,
                color = androidx.compose.ui.graphics.Color(0xFF757575),
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = "喜忌口径：${yunNian.xiJiBasis}",
                fontSize = 11.sp,
                color = androidx.compose.ui.graphics.Color(0xFF6D4C41),
                modifier = Modifier.padding(top = 2.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                info.yuns.forEachIndexed { i, yun ->
                    DaYunCard(
                        yun = yun,
                        dayMaster = dayMaster,
                        years = yunNian.yuns.getOrNull(i)?.years ?: emptyList(),
                    )
                }
            }
        }
    }
}
