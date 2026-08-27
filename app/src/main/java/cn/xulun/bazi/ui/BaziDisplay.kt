package cn.xulun.bazi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import cn.xulun.bazi.core.QuYunCalculator
import cn.xulun.bazi.core.YongShenCalculator
import cn.xulun.bazi.core.YunRelationCalculator
import cn.xulun.bazi.core.WuXingFlowCalculator
import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DaYunInfo
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.ui.components.DaYunCard
import cn.xulun.bazi.ui.components.DiZhiRelationSection
import cn.xulun.bazi.ui.components.GeJuBanner
import cn.xulun.bazi.ui.components.HeHuaSection
import cn.xulun.bazi.ui.components.JieChongSection
import cn.xulun.bazi.ui.components.MarriageSection
import cn.xulun.bazi.ui.components.PillarCard
import cn.xulun.bazi.ui.components.QuYunSection
import cn.xulun.bazi.ui.components.ShenShaSection
import cn.xulun.bazi.ui.components.StudySection
import cn.xulun.bazi.ui.components.TiaoHouSection
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
import cn.xulun.bazi.ui.components.YunRelationSection
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
    val kongWang = KongWangCalculator.analyze(result)
    val wealth = WealthCalculator.analyze(result, daYun)
    // 选定的大运/流年；随原局切换自动清空
    var selection by remember(result) { mutableStateOf<YunSelection?>(null) }

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
                            modifier = Modifier.weight(1f),
                        )
                    }
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
        ShenShaSection(result = shenSha)
        YongShenSection(result = yongShen)
        TiaoHouSection(result = tiaoHou)
        if (daYun != null) {
            DaYunSection(
                info = daYun,
                dayMaster = dayMaster,
                selection = selection,
                onSelect = { sel ->
                    selection = if (selection == sel) null else sel
                },
            )
        }
        HeHuaSection(result = heHua)
        DiZhiRelationSection(result = diZhiRelation)
        JieChongSection(result = jieChong)
        MingGongSection(
            result = cn.xulun.bazi.core.MingGongCalculator.analyze(
                result, gender,
                flowYear = (selection as? YunSelection.LiuNian)?.liuNian?.year,
            ),
        )
        WealthSection(result = wealth)
        MarriageSection(result = MarriageCalculator.analyze(result, gender))
        StudySection(result = StudyCalculator.analyze(result, daYun))
        selection?.let { sel ->
            val extras: List<Pair<cn.xulun.bazi.model.Pillar, String>> = when (sel) {
                is YunSelection.DaYun -> listOf(sel.pillar to "大运")
                is YunSelection.LiuNian -> listOf(sel.yun.pillar to "大运", sel.pillar to "流年")
            }
            val (yunPillar, yunLabel) = when (sel) {
                is YunSelection.DaYun -> sel.pillar to "大运"
                is YunSelection.LiuNian -> sel.pillar to "流年"
            }
            QuYunSection(
                result = QuYunCalculator.analyze(result, yongShen, yunPillar),
                yunLabel = yunLabel,
            )
            YunRelationSection(
                result = YunRelationCalculator.analyze(result, extras),
                selection = sel,
            )
        }
        val flowPillars: List<cn.xulun.bazi.model.Pillar> = when (val s = selection) {
            is YunSelection.DaYun -> listOf(s.pillar)
            is YunSelection.LiuNian -> listOf(s.yun.pillar, s.pillar)
            null -> emptyList()
        }
        WuXingFlowSection(result = WuXingFlowCalculator.analyze(result, flowPillars))
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
    selection: YunSelection?,
    onSelect: (YunSelection) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "大运 · ${if (info.forward) "顺行" else "逆行"}（点选大运或流年查看运岁关系）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${info.qiYunText}，${info.startDate.year} 年 ${info.startDate.monthValue} 月 ${info.startDate.dayOfMonth} 日交运",
                fontSize = 12.sp,
                color = androidx.compose.ui.graphics.Color(0xFF757575),
                modifier = Modifier.padding(top = 4.dp),
            )
            LazyRow(
                modifier = Modifier
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(info.yuns, key = { it.startYear }) { yun ->
                    DaYunCard(
                        yun = yun,
                        dayMaster = dayMaster,
                        selection = selection,
                        onSelect = onSelect,
                    )
                }
            }
        }
    }
}
