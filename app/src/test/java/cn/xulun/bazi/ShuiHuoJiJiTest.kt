package cn.xulun.bazi

import cn.xulun.bazi.core.CongGeYunCalculator
import cn.xulun.bazi.core.TeShuGeJuCalculator
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.TeShuKind
import cn.xulun.bazi.model.YunVerdict
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 水火既济格锚点（相克对）。理论见 .shengke-reference.md 第五五节九。
 * 成格三例 + 行运总表（壬子 壬子 癸巳 丁巳——水当令火弱势）。
 */
class ShuiHuoJiJiTest {

    @Test fun `济例一 壬子壬子丁巳丙午 停均`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.DING, EarthlyBranch.SI, HeavenlyStem.BING, EarthlyBranch.WU))
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("水火既济格", r.name)
    }

    @Test fun `济例二 癸亥丁巳癸巳壬子 盛夏得水`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.HAI, HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.GUI, EarthlyBranch.SI, HeavenlyStem.REN, EarthlyBranch.ZI))
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("水火既济格", r.name)
    }

    @Test fun `济例三 丙午癸巳癸亥癸亥 当令对党众`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.BING, EarthlyBranch.WU, HeavenlyStem.GUI, EarthlyBranch.SI,
            HeavenlyStem.GUI, EarthlyBranch.HAI, HeavenlyStem.GUI, EarthlyBranch.HAI))
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("水火既济格", r.name)
    }

    private val chart = lazy {
        chartOf(
            HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.GUI, EarthlyBranch.SI, HeavenlyStem.DING, EarthlyBranch.SI)
    }

    private fun vy(s: HeavenlyStem, b: EarthlyBranch): YunVerdict? =
        CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))?.verdict

    @Test fun `济行运1 旺火为用旺水为忌`() {
        for ((s, b) in listOf(
            HeavenlyStem.BING to EarthlyBranch.WU, HeavenlyStem.DING to EarthlyBranch.SI,
            HeavenlyStem.JIA to EarthlyBranch.WU, HeavenlyStem.YI to EarthlyBranch.SI,
        )) assertEquals("(${s.chinese}${b.chinese}) 补弱火——应用神", YunVerdict.YONG, vy(s, b))
        for ((s, b) in listOf(
            HeavenlyStem.REN to EarthlyBranch.ZI, HeavenlyStem.GUI to EarthlyBranch.HAI,
            HeavenlyStem.GENG to EarthlyBranch.ZI, HeavenlyStem.XIN to EarthlyBranch.HAI,
        )) assertEquals("(${s.chinese}${b.chinese}) 旺水失衡——应忌", YunVerdict.JI, vy(s, b))
    }

    @Test fun `济行运2 强土联合火转正格两停`() {
        for ((s, b) in listOf(
            HeavenlyStem.WU to EarthlyBranch.XU, HeavenlyStem.JI to EarthlyBranch.WEI,
        )) assertEquals("(${s.chinese}${b.chinese}) 与原局火联合两停——应转正格", YunVerdict.HUAN_YUAN, vy(s, b))
    }

    @Test fun `济行运3 强金转正格取财克印耗身`() {
        for ((s, b) in listOf(
            HeavenlyStem.GENG to EarthlyBranch.SHEN, HeavenlyStem.XIN to EarthlyBranch.YOU,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 身旺印旺财弱正格——应转正格", YunVerdict.HUAN_YUAN, r.verdict)
        }
    }

    @Test fun `济行运4 强木通关转三气旺木到位`() {
        for ((s, b) in listOf(
            HeavenlyStem.JIA to EarthlyBranch.YIN, HeavenlyStem.YI to EarthlyBranch.MAO,
            HeavenlyStem.REN to EarthlyBranch.YIN, HeavenlyStem.GUI to EarthlyBranch.MAO,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 通水火之关——应转三气", YunVerdict.ZHUAN_GE, r.verdict)
        }
    }
}
