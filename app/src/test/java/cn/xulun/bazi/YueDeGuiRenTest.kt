package cn.xulun.bazi

import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 月德贵人与月德合锚点（月支查干）。理论见 .shengke-reference.md 第五七节五。
 * 寅午戌丙/申子辰壬/亥卯未甲/巳酉丑庚；合=五合干（辛/丁/己/乙）。
 */
class YueDeGuiRenTest {

    private fun has(b: cn.xulun.bazi.model.BaziResult, name: String, loc: String, s: HeavenlyStem) =
        ShenShaCalculator.analyze(b).items.any { it.name == name && it.pillarLabel == loc && it.stem == s }

    @Test fun `寅月丙月德辛月德合`() {
        // 辛丑 庚寅 甲午 丙寅——寅月：时干丙=月德；年干辛=月德合
        val b = chartOf(
            HeavenlyStem.XIN, EarthlyBranch.CHOU, HeavenlyStem.GENG, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.WU, HeavenlyStem.BING, EarthlyBranch.YIN)
        assertTrue("寅月丙为月德", has(b, "月德贵人", "时干", HeavenlyStem.BING))
        assertTrue("寅月辛为月德合", has(b, "月德合", "年干", HeavenlyStem.XIN))
    }

    @Test fun `子月壬月德丁月德合`() {
        // 丁卯 壬子 甲申 丁卯——子月：月干壬=月德；年时干丁=月德合
        val b = chartOf(
            HeavenlyStem.DING, EarthlyBranch.MAO, HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.JIA, EarthlyBranch.SHEN, HeavenlyStem.DING, EarthlyBranch.MAO)
        assertTrue("子月壬为月德", has(b, "月德贵人", "月干", HeavenlyStem.REN))
        assertTrue("子月丁为月德合（年干+时干）", has(b, "月德合", "年干", HeavenlyStem.DING) && has(b, "月德合", "时干", HeavenlyStem.DING))
    }

    @Test fun `酉月庚月德乙月德合`() {
        // 乙巳 乙酉 庚辰 乙酉——酉月：庚无透干（日干庚=月德！）；乙干=月德合
        val b = chartOf(
            HeavenlyStem.YI, EarthlyBranch.SI, HeavenlyStem.YI, EarthlyBranch.YOU,
            HeavenlyStem.GENG, EarthlyBranch.CHEN, HeavenlyStem.YI, EarthlyBranch.YOU)
        assertTrue("酉月日干庚为月德", has(b, "月德贵人", "日干", HeavenlyStem.GENG))
        val he = ShenShaCalculator.analyze(b).items.filter { it.name == "月德合" }
        assertTrue("酉月乙为月德合（三干）", he.size >= 3)
    }
}
