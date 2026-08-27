package cn.xulun.bazi

import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 文昌星锚点（年日干查——食神之禄）。理论见 .shengke-reference.md 第五七节六。
 * 甲巳/乙午/丙戊申/丁己酉/庚亥/辛子/壬寅/癸卯。
 */
class WenChangXingTest {

    private fun has(b: cn.xulun.bazi.model.BaziResult, loc: String, br: EarthlyBranch) =
        ShenShaCalculator.analyze(b).items.any { it.name == "文昌" && it.pillarLabel == loc && it.branch == br }

    @Test fun `甲日坐巳为文昌`() {
        val b2 = chartOf(
            HeavenlyStem.GENG, EarthlyBranch.WU, HeavenlyStem.XIN, EarthlyBranch.SI,
            HeavenlyStem.JIA, EarthlyBranch.SHEN, HeavenlyStem.BING, EarthlyBranch.YIN)
        assertTrue("甲日见月支巳（文昌）", has(b2, "月支", EarthlyBranch.SI))
    }

    @Test fun `年干查 乙年干见午`() {
        // 乙丑 壬午 丙申 戊子——年干乙→午：月支午命中（日干丙→申亦中日支）
        val b = chartOf(
            HeavenlyStem.YI, EarthlyBranch.CHOU, HeavenlyStem.REN, EarthlyBranch.WU,
            HeavenlyStem.BING, EarthlyBranch.SHEN, HeavenlyStem.WU, EarthlyBranch.ZI)
        assertTrue("年干乙查月支午", has(b, "月支", EarthlyBranch.WU))
        assertTrue("日干丙查日支申", has(b, "日支", EarthlyBranch.SHEN))
    }

    @Test fun `庚亥辛子壬虎癸兔`() {
        // 庚申 戊子 辛亥 甲午——日干辛→亥：日支亥命中；年干庚→亥亦查
        val b1 = chartOf(
            HeavenlyStem.GENG, EarthlyBranch.SHEN, HeavenlyStem.WU, EarthlyBranch.ZI,
            HeavenlyStem.XIN, EarthlyBranch.HAI, HeavenlyStem.JIA, EarthlyBranch.WU)
        assertTrue("辛日坐亥（年庚同查亥）", has(b1, "日支", EarthlyBranch.HAI))
        // 壬子 壬寅 壬寅 辛丑——壬→寅：月日支双寅
        val b2 = chartOf(
            HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.REN, EarthlyBranch.YIN, HeavenlyStem.XIN, EarthlyBranch.CHOU)
        assertTrue("壬年日干查寅双见", has(b2, "月支", EarthlyBranch.YIN) && has(b2, "日支", EarthlyBranch.YIN))
    }
}
