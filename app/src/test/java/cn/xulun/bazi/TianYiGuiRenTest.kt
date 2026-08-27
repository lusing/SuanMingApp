package cn.xulun.bazi

import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 天乙贵人锚点（神峰通考版——甲戊兼牛羊、庚辛逢虎马；年干日干均可查）。
 * 理论见 .shengke-reference.md 第五七节一。
 */
class TianYiGuiRenTest {

    private fun has(b: cn.xulun.bazi.model.BaziResult, loc: String, br: EarthlyBranch): Boolean =
        ShenShaCalculator.analyze(b).items.any { it.name == "天乙贵人" && it.pillarLabel == loc && it.branch == br }

    @Test fun `庚日寅午为贵非丑未(神峰版)`() {
        // 庚戌 己丑 庚午 丁丑——庚日见午（渊海版庚取丑未，神峰版庚取寅午）
        val b = chartOf(
            HeavenlyStem.GENG, EarthlyBranch.XU, HeavenlyStem.JI, EarthlyBranch.CHOU,
            HeavenlyStem.GENG, EarthlyBranch.WU, HeavenlyStem.DING, EarthlyBranch.CHOU)
        assertTrue("庚日见午应为天乙（寅午）", has(b, "日支", EarthlyBranch.WU))
        assertTrue("庚日丑非天乙（神峰版）", !has(b, "月支", EarthlyBranch.CHOU) && !has(b, "时支", EarthlyBranch.CHOU))
    }

    @Test fun `甲日丑未为贵`() {
        // 甲子 丙寅 甲辰 乙丑——甲日见时支丑
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.ZI, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.CHEN, HeavenlyStem.YI, EarthlyBranch.CHOU)
        assertTrue("甲日见丑为天乙", has(b, "时支", EarthlyBranch.CHOU))
    }

    @Test fun `年干查贵人(年干甲日干庚并集)`() {
        // 甲午 辛未 庚辰 丙子——年干甲→丑未（无）；日干庚→寅午（年支午命中！）
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.WU, HeavenlyStem.XIN, EarthlyBranch.WEI,
            HeavenlyStem.GENG, EarthlyBranch.CHEN, HeavenlyStem.BING, EarthlyBranch.ZI)
        assertTrue("年干庚查午？非也——日干庚查年支午为天乙", has(b, "年支", EarthlyBranch.WU))
    }

    @Test fun `丙丁猪鸡位 壬癸兔蛇藏 乙己鼠猴乡`() {
        // 丙子 庚寅 丙寅 癸巳——丙日见时支巳？丙取酉亥非巳；年干丙同——巳非贵
        val b1 = chartOf(
            HeavenlyStem.BING, EarthlyBranch.ZI, HeavenlyStem.GENG, EarthlyBranch.YIN,
            HeavenlyStem.BING, EarthlyBranch.YIN, HeavenlyStem.GUI, EarthlyBranch.SI)
        assertTrue("丙日巳非天乙", ShenShaCalculator.analyze(b1).items.none { it.name == "天乙贵人" && it.branch == EarthlyBranch.SI })
        // 壬寅 壬子 壬申 辛亥——壬日见亥？壬取卯巳——亥非；年干壬同
        val b2 = chartOf(
            HeavenlyStem.REN, EarthlyBranch.YIN, HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.REN, EarthlyBranch.SHEN, HeavenlyStem.XIN, EarthlyBranch.HAI)
        assertTrue("壬日亥非天乙", ShenShaCalculator.analyze(b2).items.none { it.name == "天乙贵人" && it.branch == EarthlyBranch.HAI })
    }

    @Test fun `日贵四日 丁酉丁亥癸巳癸卯`() {
        // 丁酉 丁未 丁酉 庚子——丁日坐酉=日贵
        val b = chartOf(
            HeavenlyStem.DING, EarthlyBranch.YOU, HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.DING, EarthlyBranch.YOU, HeavenlyStem.GENG, EarthlyBranch.ZI)
        assertTrue("丁日坐酉（日贵）", has(b, "日支", EarthlyBranch.YOU))
        // 年支酉+日支酉两柱皆贵（丁年干同查酉亥）
        assertEquals(2, ShenShaCalculator.analyze(b).items.count { it.name == "天乙贵人" && it.branch == EarthlyBranch.YOU })
    }
}
