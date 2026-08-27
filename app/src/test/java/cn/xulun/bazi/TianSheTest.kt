package cn.xulun.bazi

import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 天赦锚点（四季查日柱）。理论见 .shengke-reference.md 第五七节十四。
 */
class TianSheTest {

    private fun has(b: cn.xulun.bazi.model.BaziResult) =
        ShenShaCalculator.analyze(b).items.any { it.name == "天赦" }

    @Test fun `春戊寅`() {
        // 甲辰 丙寅 戊寅 甲寅——春月（寅）日柱戊寅
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.CHEN, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.WU, EarthlyBranch.YIN, HeavenlyStem.JIA, EarthlyBranch.YIN)
        assertTrue("春戊寅天赦", has(b))
    }

    @Test fun `夏甲午`() {
        // 庚午 壬午 甲午 庚午——夏月（午）日柱甲午
        val b = chartOf(
            HeavenlyStem.GENG, EarthlyBranch.WU, HeavenlyStem.REN, EarthlyBranch.WU,
            HeavenlyStem.JIA, EarthlyBranch.WU, HeavenlyStem.GENG, EarthlyBranch.WU)
        assertTrue("夏甲午天赦", has(b))
    }

    @Test fun `秋戊申`() {
        // 甲申 壬申 戊申 庚申——秋月（申）日柱戊申
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.SHEN, HeavenlyStem.REN, EarthlyBranch.SHEN,
            HeavenlyStem.WU, EarthlyBranch.SHEN, HeavenlyStem.GENG, EarthlyBranch.SHEN)
        assertTrue("秋戊申天赦", has(b))
    }

    @Test fun `非天赦 春月非戊寅日`() {
        // 甲辰 丙寅 甲午 乙丑——春月但日柱甲午非戊寅
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.CHEN, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.WU, HeavenlyStem.YI, EarthlyBranch.CHOU)
        assertTrue("春月甲午日非天赦", !has(b))
    }
}
