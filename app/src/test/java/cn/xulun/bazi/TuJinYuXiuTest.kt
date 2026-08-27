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
 * 土金毓秀格锚点。理论见 .shengke-reference.md 第五五节三。
 * 成格三例（丑戌刑废根由比劫两透自立；弱旺去月令计）+ 行运总表（辛酉 辛丑 戊申 己未——金乘旺土略偏弱）。
 */
class TuJinYuXiuTest {

    @Test fun `毓秀例一 庚申己丑戊戌庚申 均衡`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GENG, EarthlyBranch.SHEN, HeavenlyStem.JI, EarthlyBranch.CHOU,
            HeavenlyStem.WU, EarthlyBranch.XU, HeavenlyStem.GENG, EarthlyBranch.SHEN))
        assertNotNull("戌被丑刑而戊己两透自立：$r", r)
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("土金毓秀格", r.name)
    }

    @Test fun `毓秀例二 辛酉辛丑辛丑戊戌 三辛泄厚土`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.XIN, EarthlyBranch.YOU, HeavenlyStem.XIN, EarthlyBranch.CHOU,
            HeavenlyStem.XIN, EarthlyBranch.CHOU, HeavenlyStem.WU, EarthlyBranch.XU))
        assertNotNull(r)
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("土金毓秀格", r.name)
    }

    @Test fun `毓秀例三 戊戌庚申戊辰庚申 纯和均衡`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.XU, HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.WU, EarthlyBranch.CHEN, HeavenlyStem.GENG, EarthlyBranch.SHEN))
        assertNotNull(r)
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("土金毓秀格", r.name)
    }

    // ── 行运总表（辛酉 辛丑 戊申 己未——金乘旺，土略偏弱，气候偏寒喜燥土） ──

    private val chart = lazy {
        chartOf(
            HeavenlyStem.XIN, EarthlyBranch.YOU, HeavenlyStem.XIN, EarthlyBranch.CHOU,
            HeavenlyStem.WU, EarthlyBranch.SHEN, HeavenlyStem.JI, EarthlyBranch.WEI)
    }

    private fun vy(s: HeavenlyStem, b: EarthlyBranch): YunVerdict? =
        CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))?.verdict

    @Test fun `毓秀行运1 厚重燥土为用旺金为忌`() {
        for ((s, b) in listOf(
            HeavenlyStem.WU to EarthlyBranch.XU, HeavenlyStem.JI to EarthlyBranch.WEI,
        )) assertEquals("(${s.chinese}${b.chinese}) 燥土补弱取暖——应用神", YunVerdict.YONG, vy(s, b))
        for ((s, b) in listOf(
            HeavenlyStem.GENG to EarthlyBranch.SHEN, HeavenlyStem.XIN to EarthlyBranch.YOU,
        )) assertEquals("(${s.chinese}${b.chinese}) 泄虚土气失衡——应忌", YunVerdict.JI, vy(s, b))
    }

    @Test fun `毓秀行运2 丙午强火转三气扶土调候`() {
        val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(HeavenlyStem.BING, EarthlyBranch.WU)))
        assertNotNull(r)
        assertEquals("丙午火土金三气，旺火扶土抑金调候——应转格", YunVerdict.ZHUAN_GE, r!!.verdict)
        assertEquals(Element.EARTH, r.newYongShen)
    }

    @Test fun `毓秀行运3 甲寅乙卯强木转正格取比劫土`() {
        for ((s, b) in listOf(
            HeavenlyStem.JIA to EarthlyBranch.YIN, HeavenlyStem.YI to EarthlyBranch.MAO,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 木战转正格——应转正格", YunVerdict.HUAN_YUAN, r.verdict)
            assertEquals("金木较强比劫土略偏弱——取比劫", Element.EARTH, r.newYongShen)
        }
    }

    @Test fun `毓秀行运4 强水泄秀转三气取比劫土（丁亥弱水忌）`() {
        for ((s, b) in listOf(
            HeavenlyStem.REN to EarthlyBranch.ZI, HeavenlyStem.GUI to EarthlyBranch.HAI,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 土金水三气——应转格", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals("金水偏旺土略偏弱——取比劫土", Element.EARTH, r.newYongShen)
        }
        assertEquals("丁亥弱水碍格局纯势——应忌", YunVerdict.JI, vy(HeavenlyStem.DING, EarthlyBranch.HAI))
    }
}
