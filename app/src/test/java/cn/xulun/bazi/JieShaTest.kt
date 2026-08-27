package cn.xulun.bazi

import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 劫煞锚点（三合绝地，年支日支并查）。理论见 .shengke-reference.md 第五八节四。
 */
class JieShaTest {

    private fun items(b: cn.xulun.bazi.model.BaziResult) =
        ShenShaCalculator.analyze(b).items.filter { it.name == "劫煞" }

    @Test fun `申子辰见巳（水绝在巳）`() {
        // 壬申 壬子 壬午 乙巳——年日支申子（申子辰→巳）：时支巳命中
        val b = chartOf(
            HeavenlyStem.REN, EarthlyBranch.SHEN, HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.REN, EarthlyBranch.WU, HeavenlyStem.YI, EarthlyBranch.SI)
        assertTrue("申子辰劫煞巳（时支）", items(b).any { it.branch == EarthlyBranch.SI && it.pillarLabel == "时支" })
    }

    @Test fun `寅午戌见亥（火绝在亥）`() {
        // 甲寅 丙寅 甲午 乙亥——年日支寅午（寅午戌→亥）：时支亥命中
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.WU, HeavenlyStem.YI, EarthlyBranch.HAI)
        assertTrue("寅午戌劫煞亥（时支）", items(b).any { it.branch == EarthlyBranch.HAI && it.pillarLabel == "时支" })
    }

    @Test fun `年支异组并查 巳酉丑见寅`() {
        // 乙丑 辛巳 乙酉 戊寅——年支丑组（巳酉丑→寅）：时支寅命中（日支酉同组亦查寅）
        val b = chartOf(
            HeavenlyStem.YI, EarthlyBranch.CHOU, HeavenlyStem.XIN, EarthlyBranch.SI,
            HeavenlyStem.YI, EarthlyBranch.YOU, HeavenlyStem.WU, EarthlyBranch.YIN)
        assertTrue("巳酉丑劫煞寅（时支）", items(b).any { it.branch == EarthlyBranch.YIN && it.pillarLabel == "时支" })
    }
}
