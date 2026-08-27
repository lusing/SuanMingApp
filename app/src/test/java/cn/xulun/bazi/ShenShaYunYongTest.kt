package cn.xulun.bazi

import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 神煞运用举例·华盖太极（玄学缘实证）。理论见 .shengke-reference.md 第五九节一。
 */
class ShenShaYunYongTest {

    @Test fun `例一 戊戌癸亥庚戌乙酉 华盖互见太极天门重重`() {
        val b = chartOf(
            HeavenlyStem.WU, EarthlyBranch.XU, HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.GENG, EarthlyBranch.XU, HeavenlyStem.YI, EarthlyBranch.YOU)
        val items = ShenShaCalculator.analyze(b).items
        // 年日支戌：戌年支（寅午戌→戌）华盖年支自见? 查其余各支——日支戌命中×2；庚日（申酉戌→丑?）华盖=丑无
        val hg = items.filter { it.name == "华盖" }
        assertTrue("年日支华盖互见（日支戌双查）：${hg.map { it.pillarLabel }}", hg.isNotEmpty())
        // 庚日太极=寅亥：亥支月命中；戌亥天门
        assertTrue("太极贵人（庚日寅亥——月支亥）", items.any { it.name == "太极贵人" && it.branch == EarthlyBranch.HAI })
        // 戊年干太极=辰戌丑未：年日支戌再命中
        assertTrue("太极重重（戊年干四库——戌亦中）", items.count { it.name == "太极贵人" } >= 2)
    }

    @Test fun `例二 丁未戊申己未辛未 三重华盖`() {
        val b = chartOf(
            HeavenlyStem.DING, EarthlyBranch.WEI, HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.JI, EarthlyBranch.WEI, HeavenlyStem.XIN, EarthlyBranch.WEI)
        val hg = ShenShaCalculator.analyze(b).items.filter { it.name == "华盖" }
        // 年支未（亥卯未→未）：查月日时——日时支未双命中；日支未同组亦查：三路未
        assertTrue("年日时三重华盖：${hg.map { it.pillarLabel }}", hg.size >= 2)
    }
}
