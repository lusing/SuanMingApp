package cn.xulun.bazi

import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 魁罡锚点（日柱四座神）。理论见 .shengke-reference.md 第五七节十六。
 */
class KuiGangTest {

    private fun has(b: cn.xulun.bazi.model.BaziResult) =
        ShenShaCalculator.analyze(b).items.any { it.name == "魁罡" }

    @Test fun `庚辰日魁罡`() {
        // 甲辰 丙寅 庚辰 丙子——日柱庚辰
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.CHEN, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.GENG, EarthlyBranch.CHEN, HeavenlyStem.BING, EarthlyBranch.ZI)
        assertTrue("庚辰日魁罡", has(b))
    }

    @Test fun `壬辰庚戌戊戌三日`() {
        // 甲辰 丙寅 壬辰 壬寅——壬辰日
        val b1 = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.CHEN, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.REN, EarthlyBranch.CHEN, HeavenlyStem.REN, EarthlyBranch.YIN)
        assertTrue("壬辰日魁罡", has(b1))
        // 甲戌 丙寅 庚戌 丙子——庚戌日
        val b2 = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.XU, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.GENG, EarthlyBranch.XU, HeavenlyStem.BING, EarthlyBranch.ZI)
        assertTrue("庚戌日魁罡", has(b2))
        // 甲戌 丙寅 戊戌 甲寅——戊戌日
        val b3 = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.XU, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.WU, EarthlyBranch.XU, HeavenlyStem.JIA, EarthlyBranch.YIN)
        assertTrue("戊戌日魁罡", has(b3))
    }

    @Test fun `年柱魁罡非日柱不算`() {
        // 甲辰年 丙寅 庚戌 甲申——庚戌在日柱? 非此意——用甲日使庚辰仅在年柱：甲辰 丙寅 甲戌 乙丑
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.CHEN, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.XU, HeavenlyStem.YI, EarthlyBranch.CHOU)
        assertTrue("年柱庚辰非魁罡", !has(b))
    }
}
