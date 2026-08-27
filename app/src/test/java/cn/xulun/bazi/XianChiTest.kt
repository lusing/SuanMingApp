package cn.xulun.bazi

import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 咸池/桃花锚点（三合沐浴地，年支日支并查）。理论见 .shengke-reference.md 第五八节七。
 */
class XianChiTest {

    private fun items(b: cn.xulun.bazi.model.BaziResult) =
        ShenShaCalculator.analyze(b).items.filter { it.name == "桃花" }

    @Test fun `申子辰在酉（水沐浴酉）`() {
        // 壬申 壬子 壬午 己酉——年日支申子（申子辰→酉）：时支酉命中
        val b = chartOf(
            HeavenlyStem.REN, EarthlyBranch.SHEN, HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.REN, EarthlyBranch.WU, HeavenlyStem.JI, EarthlyBranch.YOU)
        assertTrue("申子辰咸池酉（时支=墙外桃花）", items(b).any { it.branch == EarthlyBranch.YOU && it.pillarLabel == "时支" })
    }

    @Test fun `年支异组 巳酉丑在午`() {
        // 乙丑 辛巳 乙酉 壬午——年支丑+日支酉（巳酉丑→午）：时支午命中
        val b = chartOf(
            HeavenlyStem.YI, EarthlyBranch.CHOU, HeavenlyStem.XIN, EarthlyBranch.SI,
            HeavenlyStem.YI, EarthlyBranch.YOU, HeavenlyStem.REN, EarthlyBranch.WU)
        assertTrue("巳酉丑咸池午（时支）", items(b).any { it.branch == EarthlyBranch.WU && it.pillarLabel == "时支" })
    }

    @Test fun `寅午戌在卯 亥卯未在子`() {
        // 甲寅 丙寅 己卯 乙亥——寅午组（寅午戌→卯）：日支卯命中（己日亥时乙亥）
        val b1 = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JI, EarthlyBranch.MAO, HeavenlyStem.YI, EarthlyBranch.HAI)
        assertTrue("寅午戌咸池卯（日支）", items(b1).any { it.branch == EarthlyBranch.MAO && it.pillarLabel == "日支" })
        // 乙卯 己卯 乙亥 丙子——年日支卯（亥卯未→子）：时支子命中
        val b2 = chartOf(
            HeavenlyStem.YI, EarthlyBranch.MAO, HeavenlyStem.JI, EarthlyBranch.MAO,
            HeavenlyStem.YI, EarthlyBranch.HAI, HeavenlyStem.BING, EarthlyBranch.ZI)
        assertTrue("亥卯未咸池子（时支）", items(b2).any { it.branch == EarthlyBranch.ZI && it.pillarLabel == "时支" })
    }
}
