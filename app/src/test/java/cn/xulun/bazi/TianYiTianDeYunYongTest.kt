package cn.xulun.bazi

import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 神煞运用举例·天乙天德（并临喜用逢凶化吉实证）。理论见 .shengke-reference.md 第五九节三。
 */
class TianYiTianDeYunYongTest {

    @Test fun `例一 乙酉壬午丙午己亥 年天乙酉时支天乙天德亥并临`() {
        val b = chartOf(
            HeavenlyStem.YI, EarthlyBranch.YOU, HeavenlyStem.REN, EarthlyBranch.WU,
            HeavenlyStem.BING, EarthlyBranch.WU, HeavenlyStem.JI, EarthlyBranch.HAI)
        val items = ShenShaCalculator.analyze(b).items
        assertTrue("丙日天乙酉亥——年支酉命中", items.any { it.name == "天乙贵人" && it.branch == EarthlyBranch.YOU && it.pillarLabel == "年支" })
        assertTrue("时支亥天乙", items.any { it.name == "天乙贵人" && it.branch == EarthlyBranch.HAI && it.pillarLabel == "时支" })
        assertTrue("午月天德亥——时支天乙天德并临", items.any { it.name == "天德贵人" && it.branch == EarthlyBranch.HAI && it.pillarLabel == "时支" })
    }

    @Test fun `例二 丙午庚子癸丑丁巳 癸日天乙巳临火用神`() {
        val b = chartOf(
            HeavenlyStem.BING, EarthlyBranch.WU, HeavenlyStem.GENG, EarthlyBranch.ZI,
            HeavenlyStem.GUI, EarthlyBranch.CHOU, HeavenlyStem.DING, EarthlyBranch.SI)
        val items = ShenShaCalculator.analyze(b).items
        assertTrue("癸日天乙卯巳——时支巳（火用神）命中：1989己巳荣升之验",
            items.any { it.name == "天乙贵人" && it.branch == EarthlyBranch.SI && it.pillarLabel == "时支" })
    }
}
