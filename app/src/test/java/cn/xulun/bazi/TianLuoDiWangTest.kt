package cn.xulun.bazi

import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 天罗地网锚点（戌亥互见/辰巳互见——年支或日支查其余三支）。理论见 .shengke-reference.md 第五八节三。
 */
class TianLuoDiWangTest {

    private fun names(b: cn.xulun.bazi.model.BaziResult) =
        ShenShaCalculator.analyze(b).items.map { it.name to it.pillarLabel }

    @Test fun `戌人见亥为天罗`() {
        // 甲戌 丙寅 甲午 乙亥——年支戌（日支午非罗网支）：时支亥=天罗
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.XU, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.WU, HeavenlyStem.YI, EarthlyBranch.HAI)
        assertTrue("戌见亥天罗（时支）：${names(b)}", names(b).contains("天罗" to "时支"))
    }

    @Test fun `辰人见巳为地网`() {
        // 甲辰 丙寅 甲午 乙丑——辰无巳；换：甲辰 丙寅 甲申 丁卯？巳无。用：戊辰 丙辰 甲戌 己巳
        // 年支辰组：时支巳=地网；日支戌组：亥无。但辰戌巳有+亥无——非齐见
        val b = chartOf(
            HeavenlyStem.WU, EarthlyBranch.CHEN, HeavenlyStem.BING, EarthlyBranch.CHEN,
            HeavenlyStem.JIA, EarthlyBranch.XU, HeavenlyStem.JI, EarthlyBranch.SI)
        assertTrue("辰见巳地网（时支）：${names(b)}", names(b).contains("地网" to "时支"))
    }

    @Test fun `辰戌巳亥全为天罗地网齐见`() {
        // 甲辰 丙寅 甲戌 乙亥——辰戌亥+巳无——非全。需四支全：戊辰 丙巳 甲戌 乙亥
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.CHEN, HeavenlyStem.JI, EarthlyBranch.SI,
            HeavenlyStem.JIA, EarthlyBranch.XU, HeavenlyStem.YI, EarthlyBranch.HAI)
        assertTrue("辰戌巳亥齐见：${names(b)}", names(b).any { it.first == "天罗地网" })
    }

    @Test fun `无罗网支不算`() {
        // 甲子 丙寅 甲午 乙丑——子寅午丑无辰戌巳亥
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.ZI, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.WU, HeavenlyStem.YI, EarthlyBranch.CHOU)
        assertTrue("无罗网：${names(b)}", names(b).none { it.first in listOf("天罗", "地网", "天罗地网") })
    }
}
