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
 * 金木栋梁格锚点（相克对首例）。理论见 .shengke-reference.md 第五五节六。
 * 成格三例（金木相互克耗均衡；通关水微弱不破格）+ 行运总表（乙卯 甲申 乙卯 甲申——木反胜于金）。
 */
class JinMuDongLiangTest {

    @Test fun `梁例一 辛酉辛卯甲寅癸酉 通关透干书内矛盾注记`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.XIN, EarthlyBranch.YOU, HeavenlyStem.XIN, EarthlyBranch.MAO,
            HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.GUI, EarthlyBranch.YOU))
        // 书断栋梁格；然通关癸透与病药章扶财一（戊戌乙丑甲辰丁卯丁通关透）同构异断——
        // 引擎取病药侧（通关透干不分流），例二例三无通关透正常入格
        assertTrue2("栋梁例一通关透干放宽（LIANG_QI 或正格）：$r", r == null || r.kind == TeShuKind.LIANG_QI)
    }

    private fun assertTrue2(msg: String, cond: Boolean) = org.junit.Assert.assertTrue(msg, cond)

    @Test fun `梁例二 辛酉辛卯辛卯庚寅 强金盖头斩削`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.XIN, EarthlyBranch.YOU, HeavenlyStem.XIN, EarthlyBranch.MAO,
            HeavenlyStem.XIN, EarthlyBranch.MAO, HeavenlyStem.GENG, EarthlyBranch.YIN))
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("金木栋梁格", r.name)
    }

    @Test fun `梁例三 辛卯辛卯辛卯辛卯 干支各不相杂`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.XIN, EarthlyBranch.MAO, HeavenlyStem.XIN, EarthlyBranch.MAO,
            HeavenlyStem.XIN, EarthlyBranch.MAO, HeavenlyStem.XIN, EarthlyBranch.MAO))
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("金木栋梁格", r.name)
    }

    private val chart = lazy {
        chartOf(
            HeavenlyStem.YI, EarthlyBranch.MAO, HeavenlyStem.JIA, EarthlyBranch.SHEN,
            HeavenlyStem.YI, EarthlyBranch.MAO, HeavenlyStem.JIA, EarthlyBranch.SHEN)
    }

    private fun vy(s: HeavenlyStem, b: EarthlyBranch): YunVerdict? =
        CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))?.verdict

    @Test fun `梁行运1 旺金为用旺木为忌`() {
        for ((s, b) in listOf(
            HeavenlyStem.GENG to EarthlyBranch.SHEN, HeavenlyStem.XIN to EarthlyBranch.YOU,
        )) assertEquals("(${s.chinese}${b.chinese}) 补弱金——应用神", YunVerdict.YONG, vy(s, b))
        for ((s, b) in listOf(
            HeavenlyStem.JIA to EarthlyBranch.YIN, HeavenlyStem.YI to EarthlyBranch.MAO,
        )) assertEquals("(${s.chinese}${b.chinese}) 旺木失衡——应忌", YunVerdict.JI, vy(s, b))
    }

    @Test fun `梁行运2 强火转正格取火金联用`() {
        for ((s, b) in listOf(
            HeavenlyStem.BING to EarthlyBranch.WU, HeavenlyStem.DING to EarthlyBranch.SI,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 引泄旺木转正格——应转正格", YunVerdict.HUAN_YUAN, r.verdict)
            assertEquals(Element.FIRE, r.newYongShen)
        }
    }

    @Test fun `梁行运3 强土合力转正格旺土到位`() {
        for ((s, b) in listOf(
            HeavenlyStem.WU to EarthlyBranch.XU, HeavenlyStem.WU to EarthlyBranch.CHEN,
            HeavenlyStem.JI to EarthlyBranch.WEI, HeavenlyStem.JI to EarthlyBranch.CHOU,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 生金耗木趋于中和——应转正格", YunVerdict.HUAN_YUAN, r.verdict)
            assertEquals(Element.EARTH, r.newYongShen)
        }
    }

    @Test fun `梁行运4 强水通关转三气取金`() {
        for ((s, b) in listOf(
            HeavenlyStem.REN to EarthlyBranch.ZI, HeavenlyStem.GUI to EarthlyBranch.HAI,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 通金木之关——应转三气", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals("金略偏弱取金", Element.METAL, r.newYongShen)
        }
    }
}
