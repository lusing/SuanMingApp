package cn.xulun.bazi

import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 华盖锚点（三合墓库——年支日支并查）。理论见 .shengke-reference.md 第五七节九。
 */
class HuaGaiTest {

    private fun items(b: cn.xulun.bazi.model.BaziResult) =
        ShenShaCalculator.analyze(b).items.filter { it.name == "华盖" }

    @Test fun `年支寅组查戌`() {
        // 甲寅 丙寅 甲戌 乙丑——年支寅（寅午戌→戌）：日支戌命中（日支同组亦查戌）
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.XU, HeavenlyStem.YI, EarthlyBranch.CHOU)
        assertTrue("寅组华盖戌（日支）", items(b).any { it.branch == EarthlyBranch.XU })
    }

    @Test fun `年日异组并查 子组辰与亥组未`() {
        // 甲辰（年支辰·申子辰→辰） 壬申 癸未（日支未? 日柱癸未——日支未·亥卯未→未）：时支??
        // 此盘年支辰组查辰（年支自身不在其余支——查月日时）：无辰；日支未组查未：日支自身不查——改盘：
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.CHEN, HeavenlyStem.REN, EarthlyBranch.SHEN,
            HeavenlyStem.GUI, EarthlyBranch.WEI, HeavenlyStem.JI, EarthlyBranch.WEI)
        // 年支辰（申子辰→辰）查月日时无辰；日支未（亥卯未→未）查月日时：时支未命中
        assertTrue("日支未组华盖未（时支）", items(b).any { it.branch == EarthlyBranch.WEI && it.pillarLabel == "时支" })
    }

    @Test fun `申组查辰 巳组查丑`() {
        // 壬申 壬子 壬辰 丁未——年日支申子（申子辰→辰）：日支辰命中
        val b = chartOf(
            HeavenlyStem.REN, EarthlyBranch.SHEN, HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.REN, EarthlyBranch.CHEN, HeavenlyStem.DING, EarthlyBranch.WEI)
        assertTrue("申子辰组华盖辰（日支）", items(b).any { it.branch == EarthlyBranch.CHEN })
        // 乙丑（年支丑·巳酉丑→丑） 辛巳 乙酉 丁丑——查月日时丑：时支丑
        val b2 = chartOf(
            HeavenlyStem.YI, EarthlyBranch.CHOU, HeavenlyStem.XIN, EarthlyBranch.SI,
            HeavenlyStem.YI, EarthlyBranch.YOU, HeavenlyStem.DING, EarthlyBranch.CHOU)
        assertTrue("巳酉丑组华盖丑（时支）", items(b2).any { it.branch == EarthlyBranch.CHOU && it.pillarLabel == "时支" })
    }
}
