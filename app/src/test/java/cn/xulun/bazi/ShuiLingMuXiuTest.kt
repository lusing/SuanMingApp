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
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * 水灵木秀格锚点。理论见 .shengke-reference.md 第五五节五。
 * 成格三例（含夜子时）+ 行运总表（癸亥 乙卯 壬子 癸卯——旺木当令水略偏弱）。
 */
class ShuiLingMuXiuTest {

    @Test fun `秀例一 癸亥甲寅癸亥甲子 夜子时`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.HAI, HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.GUI, EarthlyBranch.HAI, HeavenlyStem.JIA, EarthlyBranch.ZI))
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("水灵木秀格", r.name)
    }

    @Test fun `秀例二 癸未癸亥癸卯癸亥 木局三合未不破格`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.WEI, HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.GUI, EarthlyBranch.MAO, HeavenlyStem.GUI, EarthlyBranch.HAI))
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("水灵木秀格", r.name)
    }

    @Test fun `秀例三 癸卯甲子甲子乙亥`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.MAO, HeavenlyStem.JIA, EarthlyBranch.ZI,
            HeavenlyStem.JIA, EarthlyBranch.ZI, HeavenlyStem.YI, EarthlyBranch.HAI))
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("水灵木秀格", r.name)
    }

    private val chart = lazy {
        chartOf(
            HeavenlyStem.GUI, EarthlyBranch.HAI, HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.GUI, EarthlyBranch.MAO)
    }

    private fun vy(s: HeavenlyStem, b: EarthlyBranch): YunVerdict? =
        CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))?.verdict

    @Test fun `秀行运1 旺水为用旺木为忌`() {
        for ((s, b) in listOf(
            HeavenlyStem.REN to EarthlyBranch.ZI, HeavenlyStem.GUI to EarthlyBranch.HAI,
            HeavenlyStem.GENG to EarthlyBranch.ZI, HeavenlyStem.XIN to EarthlyBranch.HAI,
        )) assertEquals("(${s.chinese}${b.chinese}) 补弱水——应用神", YunVerdict.YONG, vy(s, b))
        for ((s, b) in listOf(
            HeavenlyStem.JIA to EarthlyBranch.YIN, HeavenlyStem.YI to EarthlyBranch.MAO,
        )) assertEquals("(${s.chinese}${b.chinese}) 旺木失衡——应忌", YunVerdict.JI, vy(s, b))
    }

    @Test fun `秀行运2 强金转三气（书取金为用注记）`() {
        for ((s, b) in listOf(
            HeavenlyStem.GENG to EarthlyBranch.SHEN, HeavenlyStem.XIN to EarthlyBranch.YOU,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 金水木三气——应转格", YunVerdict.ZHUAN_GE, r.verdict)
        }
    }

    @Test fun `秀行运3 戊戌强土转正格取比劫（辰未丑被合不列旺土）`() {
        val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(HeavenlyStem.WU, EarthlyBranch.XU)))
        assertNotNull(r)
        assertEquals("戊戌强土转正格——应转正格", YunVerdict.HUAN_YUAN, r!!.verdict)
        assertEquals(Element.WATER, r.newYongShen)
        for ((s, b) in listOf(
            HeavenlyStem.JI to EarthlyBranch.WEI, HeavenlyStem.JI to EarthlyBranch.CHOU,
        )) assertEquals("(${s.chinese}${b.chinese}) 支与原局合会不在旺土列——应忌", YunVerdict.JI, vy(s, b))
        // 戊辰子辰半合向弱水——书注不列旺土（与通明章戊辰强土书内异断），引擎按转正格凶档论
        val v = vy(HeavenlyStem.WU, EarthlyBranch.CHEN)
        org.junit.Assert.assertTrue("戊辰不在旺土列（非吉档）：$v", v == YunVerdict.JI || v == YunVerdict.HUAN_YUAN)
    }

    @Test fun `秀行运4 强火引泄转三气取比劫水`() {
        for ((s, b) in listOf(
            HeavenlyStem.BING to EarthlyBranch.WU, HeavenlyStem.DING to EarthlyBranch.SI,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 水木火三气——应转格", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.WATER, r.newYongShen)
        }
    }
}
