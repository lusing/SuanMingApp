package cn.xulun.bazi

import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 灾煞锚点（冲将星·白虎煞，年支日支并查）。理论见 .shengke-reference.md 第五八节二。
 */
class ZaiShaTest {

    private fun items(b: cn.xulun.bazi.model.BaziResult) =
        ShenShaCalculator.analyze(b).items.filter { it.name == "灾煞" }

    @Test fun `寅午戌见子`() {
        // 甲寅 丙寅 甲午 甲子——年日支寅午（寅午戌→子）：时支子命中
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.WU, HeavenlyStem.JIA, EarthlyBranch.ZI)
        assertTrue("寅午戌灾煞子（时支）", items(b).any { it.branch == EarthlyBranch.ZI && it.pillarLabel == "时支" })
    }

    @Test fun `申子辰见午 巳酉丑见卯`() {
        // 壬申 壬子 壬午 丁未——年日支申子（申子辰→午）：日支午命中
        val b1 = chartOf(
            HeavenlyStem.REN, EarthlyBranch.SHEN, HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.REN, EarthlyBranch.WU, HeavenlyStem.DING, EarthlyBranch.WEI)
        assertTrue("申子辰灾煞午（日支）", items(b1).any { it.branch == EarthlyBranch.WU })
        // 乙丑 辛巳 乙卯 丁丑——年支丑组（巳酉丑→卯）：日支卯命中
        val b2 = chartOf(
            HeavenlyStem.YI, EarthlyBranch.CHOU, HeavenlyStem.XIN, EarthlyBranch.SI,
            HeavenlyStem.YI, EarthlyBranch.MAO, HeavenlyStem.DING, EarthlyBranch.CHOU)
        assertTrue("巳酉丑灾煞卯（日支，年支组）", items(b2).any { it.branch == EarthlyBranch.MAO && it.pillarLabel == "日支" })
    }

    @Test fun `亥卯未见酉`() {
        // 乙卯 己卯 乙酉 甲申——年日支卯（亥卯未→酉）：日支酉命中
        val b = chartOf(
            HeavenlyStem.YI, EarthlyBranch.MAO, HeavenlyStem.JI, EarthlyBranch.MAO,
            HeavenlyStem.YI, EarthlyBranch.YOU, HeavenlyStem.JIA, EarthlyBranch.SHEN)
        assertTrue("亥卯未灾煞酉（日支）", items(b).any { it.branch == EarthlyBranch.YOU })
    }
}
