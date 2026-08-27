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
 * 火土成慈格锚点。理论见 .shengke-reference.md 第五五节二。
 * 成格三例（日主当令分流阈值0.4——火3土7亦成慈）+ 行运总表（戊午 丁巳 戊午 丙辰——火强土弱）。
 */
class HuoTuChengCiTest {

    @Test fun `成慈例一 戊辰丁巳丙午己丑 土化有情`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.CHEN, HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.BING, EarthlyBranch.WU, HeavenlyStem.JI, EarthlyBranch.CHOU))
        assertNotNull(r)
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("火土成慈格", r.name)
    }

    @Test fun `成慈例二 己巳戊辰戊午戊午 土厚当令`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.JI, EarthlyBranch.SI, HeavenlyStem.WU, EarthlyBranch.CHEN,
            HeavenlyStem.WU, EarthlyBranch.WU, HeavenlyStem.WU, EarthlyBranch.WU))
        assertNotNull("火3土7月令辰（日主当令）相互化生——成慈非专旺：$r", r)
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("火土成慈格", r.name)
    }

    @Test fun `成慈例三 戊午丙辰戊午丙辰`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.WU, HeavenlyStem.BING, EarthlyBranch.CHEN,
            HeavenlyStem.WU, EarthlyBranch.WU, HeavenlyStem.BING, EarthlyBranch.CHEN))
        assertNotNull(r)
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("火土成慈格", r.name)
    }

    // ── 行运总表（戊午 丁巳 戊午 丙辰——火强土弱） ──

    private val chart = lazy {
        chartOf(
            HeavenlyStem.WU, EarthlyBranch.WU, HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.WU, EarthlyBranch.WU, HeavenlyStem.BING, EarthlyBranch.CHEN)
    }

    private fun vy(s: HeavenlyStem, b: EarthlyBranch): YunVerdict? =
        CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))?.verdict

    @Test fun `成慈行运1 湿润之土敛炎为用烈火为忌`() {
        for ((s, b) in listOf(
            HeavenlyStem.WU to EarthlyBranch.CHEN, HeavenlyStem.JI to EarthlyBranch.CHOU,
        )) assertEquals("(${s.chinese}${b.chinese}) 润土补弱——应用神", YunVerdict.YONG, vy(s, b))
        for ((s, b) in listOf(
            HeavenlyStem.BING to EarthlyBranch.WU, HeavenlyStem.DING to EarthlyBranch.SI,
        )) assertEquals("(${s.chinese}${b.chinese}) 烈火加燥——应忌", YunVerdict.JI, vy(s, b))
    }

    @Test fun `成慈行运2 强木转三气取润土`() {
        for ((s, b) in listOf(
            HeavenlyStem.JIA to EarthlyBranch.YIN, HeavenlyStem.YI to EarthlyBranch.MAO,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 木火土三气——应转格", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals("润土较弱宜取之为用", Element.EARTH, r.newYongShen)
        }
    }

    @Test fun `成慈行运3 强水转正格滋润为用`() {
        for ((s, b) in listOf(
            HeavenlyStem.REN to EarthlyBranch.ZI, HeavenlyStem.GENG to EarthlyBranch.ZI,
            HeavenlyStem.XIN to EarthlyBranch.HAI,
        )) assertEquals("(${s.chinese}${b.chinese}) 旺水滋润转正格——应转正格", YunVerdict.HUAN_YUAN, vy(s, b))
    }

    @Test fun `成慈行运4 强金转三气取比劫土`() {
        for ((s, b) in listOf(
            HeavenlyStem.GENG to EarthlyBranch.SHEN, HeavenlyStem.XIN to EarthlyBranch.YOU,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 火土金三气——应转格", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals("用神应取比劫", Element.EARTH, r.newYongShen)
        }
    }
}
