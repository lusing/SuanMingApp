package cn.xulun.bazi

import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 太极贵人锚点（年日干查）。理论见 .shengke-reference.md 第五七节二。
 */
class TaiJiGuiRenTest {

    private fun has(b: cn.xulun.bazi.model.BaziResult, br: EarthlyBranch): Boolean =
        ShenShaCalculator.analyze(b).items.any { it.name == "太极贵人" && it.branch == br }

    @Test fun `甲乙子午`() {
        // 甲子 丙寅 甲午 乙丑——甲日年支子日支午双太极
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.ZI, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.WU, HeavenlyStem.YI, EarthlyBranch.CHOU)
        assertTrue(has(b, EarthlyBranch.ZI) && has(b, EarthlyBranch.WU))
    }

    @Test fun `戊己四季与年干查`() {
        // 戊辰 乙卯 己未 甲子——日干己四库（年支辰/日支未）；年干戊同查
        val b = chartOf(
            HeavenlyStem.WU, EarthlyBranch.CHEN, HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.JI, EarthlyBranch.WEI, HeavenlyStem.JIA, EarthlyBranch.ZI)
        assertTrue(has(b, EarthlyBranch.CHEN) && has(b, EarthlyBranch.WEI))
    }

    @Test fun `庚辛寅亥 壬癸巳申`() {
        // 庚寅 戊子 辛亥 丙申——年干庚查寅（年支）；日干辛查亥（日支）
        val b1 = chartOf(
            HeavenlyStem.GENG, EarthlyBranch.YIN, HeavenlyStem.WU, EarthlyBranch.ZI,
            HeavenlyStem.XIN, EarthlyBranch.HAI, HeavenlyStem.BING, EarthlyBranch.SHEN)
        assertTrue(has(b1, EarthlyBranch.YIN) && has(b1, EarthlyBranch.HAI))
        // 壬申 壬子 癸巳 甲寅——年干壬查申；日干癸查巳
        val b2 = chartOf(
            HeavenlyStem.REN, EarthlyBranch.SHEN, HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.GUI, EarthlyBranch.SI, HeavenlyStem.JIA, EarthlyBranch.YIN)
        assertTrue(has(b2, EarthlyBranch.SHEN) && has(b2, EarthlyBranch.SI))
    }

    @Test fun `丙丁卯酉`() {
        // 丙子 庚寅 丁卯 辛丑——日干丁查卯；丙年干同
        val b = chartOf(
            HeavenlyStem.BING, EarthlyBranch.ZI, HeavenlyStem.GENG, EarthlyBranch.YIN,
            HeavenlyStem.DING, EarthlyBranch.MAO, HeavenlyStem.XIN, EarthlyBranch.CHOU)
        assertTrue(has(b, EarthlyBranch.MAO))
    }
}
