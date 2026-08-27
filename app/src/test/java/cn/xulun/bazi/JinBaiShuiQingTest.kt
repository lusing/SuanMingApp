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
 * 金白水清格锚点。理论见 .shengke-reference.md 第五五节四。
 * 成格三例（丙辛合化水不破格）+ 行运总表（壬申 庚戌 癸酉 癸亥——强金会局水略偏弱）。
 */
class JinBaiShuiQingTest {

    @Test fun `清例一 癸亥庚申癸亥庚申`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.HAI, HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.GUI, EarthlyBranch.HAI, HeavenlyStem.GENG, EarthlyBranch.SHEN))
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("金白水清格", r.name)
    }

    @Test fun `清例二 辛亥丙申壬申庚子 丙辛合化水`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.XIN, EarthlyBranch.HAI, HeavenlyStem.BING, EarthlyBranch.SHEN,
            HeavenlyStem.REN, EarthlyBranch.SHEN, HeavenlyStem.GENG, EarthlyBranch.ZI))
        assertNotNull("丙与辛合而化水反助命局之势：$r", r)
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("金白水清格", r.name)
    }

    @Test fun `清例三 壬申庚戌癸酉癸亥 申酉戌三会`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.REN, EarthlyBranch.SHEN, HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.GUI, EarthlyBranch.YOU, HeavenlyStem.GUI, EarthlyBranch.HAI))
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("金白水清格", r.name)
    }

    private val chart = lazy {
        chartOf(
            HeavenlyStem.REN, EarthlyBranch.SHEN, HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.GUI, EarthlyBranch.YOU, HeavenlyStem.GUI, EarthlyBranch.HAI)
    }

    private fun vy(s: HeavenlyStem, b: EarthlyBranch): YunVerdict? =
        CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))?.verdict

    @Test fun `清行运1 旺水为用旺金为忌`() {
        for ((s, b) in listOf(
            HeavenlyStem.REN to EarthlyBranch.ZI, HeavenlyStem.GUI to EarthlyBranch.HAI,
        )) assertEquals("(${s.chinese}${b.chinese}) 补弱水——应用神", YunVerdict.YONG, vy(s, b))
        for ((s, b) in listOf(
            HeavenlyStem.GENG to EarthlyBranch.SHEN, HeavenlyStem.XIN to EarthlyBranch.YOU,
        )) assertEquals("(${s.chinese}${b.chinese}) 旺金失衡——应忌", YunVerdict.JI, vy(s, b))
    }

    @Test fun `清行运2 强土通关转三气水土联用（弱土忌）`() {
        for ((s, b) in listOf(
            HeavenlyStem.WU to EarthlyBranch.CHEN, HeavenlyStem.WU to EarthlyBranch.XU,
            HeavenlyStem.JI to EarthlyBranch.CHOU, HeavenlyStem.JI to EarthlyBranch.WEI,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 旺金通关土金水三气——应转格", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals("取土水联用（水为弱行）", Element.WATER, r.newYongShen)
        }
        for ((s, b) in listOf(
            HeavenlyStem.WU to EarthlyBranch.ZI, HeavenlyStem.JI to EarthlyBranch.HAI,
            HeavenlyStem.GUI to EarthlyBranch.WEI,
        )) assertEquals("(${s.chinese}${b.chinese}) 衰弱土运金多土弱——应忌", YunVerdict.JI, vy(s, b))
    }

    @Test fun `清行运3 强火转正格取火`() {
        for ((s, b) in listOf(
            HeavenlyStem.BING to EarthlyBranch.WU, HeavenlyStem.JIA to EarthlyBranch.WU,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 财星抗金耗水转正格——应转正格", YunVerdict.HUAN_YUAN, r.verdict)
            assertEquals(Element.FIRE, r.newYongShen)
        }
    }

    @Test fun `清行运4 强木泄秀转三气取水木`() {
        for ((s, b) in listOf(
            HeavenlyStem.JIA to EarthlyBranch.YIN, HeavenlyStem.YI to EarthlyBranch.MAO,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 金水木三气——应转格", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.WATER, r.newYongShen)
        }
    }
}
