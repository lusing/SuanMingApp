package cn.xulun.bazi

import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 元辰锚点（阳男阴女冲前一位/阴男阳女冲后一位——年支查）。理论见 .shengke-reference.md 第五八节六。
 */
class YuanChenTest {

    private fun has(b: cn.xulun.bazi.model.BaziResult, male: Boolean?, br: EarthlyBranch) =
        male == null || ShenShaCalculator.analyze(b, male).items.any { it.name == "元辰" && it.branch == br }

    @Test fun `甲子年阳男冲午前未`() {
        // 甲子 丙寅 甲申 乙丑——阳年男：子冲午，午前未：柱中未无；换丑未皆有盘：甲子 丙寅 甲申 乙丑无未。
        // 用：甲子 丙寅 甲午 乙丑——日支午非元辰位（元辰=未）——柱无未。换：甲子 丙寅 甲申 辛未——未时
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.ZI, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.SHEN, HeavenlyStem.XIN, EarthlyBranch.WEI)
        assertTrue("甲子阳男元辰未（时支）", has(b, true, EarthlyBranch.WEI))
        assertTrue("甲子阴女元辰巳（柱无巳不算）", !has(b, false, EarthlyBranch.SI))
    }

    @Test fun `丙申年阴女冲寅后丑`() {
        // 丙申 庚寅 甲申 乙丑——阴年女：申冲寅，寅后丑：时支丑命中
        val b = chartOf(
            HeavenlyStem.BING, EarthlyBranch.SHEN, HeavenlyStem.GENG, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.SHEN, HeavenlyStem.YI, EarthlyBranch.CHOU)
        assertTrue("丙申阴女元辰丑（时支）", has(b, false, EarthlyBranch.CHOU))
        assertTrue("丙申阳男元辰卯（柱无卯不算）", !has(b, true, EarthlyBranch.MAO))
    }

    @Test fun `不带性别不出元辰`() {
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.ZI, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.SHEN, HeavenlyStem.XIN, EarthlyBranch.WEI)
        assertTrue(ShenShaCalculator.analyze(b).items.none { it.name == "元辰" })
    }
}
