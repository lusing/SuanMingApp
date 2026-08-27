package cn.xulun.bazi

import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 天德贵人与天德合锚点（月支查）。理论见 .shengke-reference.md 第五七节四。
 * 表：寅丁/卯申/辰壬/巳辛/午亥/未甲/申癸/酉寅/戌丙/亥乙/子巳/丑庚；合=干五合支六合。
 */
class TianDeGuiRenTest {

    private fun hasStem(b: cn.xulun.bazi.model.BaziResult, name: String, loc: String, s: HeavenlyStem) =
        ShenShaCalculator.analyze(b).items.any { it.name == name && it.pillarLabel == loc && it.stem == s }

    private fun hasBranch(b: cn.xulun.bazi.model.BaziResult, name: String, loc: String, br: EarthlyBranch) =
        ShenShaCalculator.analyze(b).items.any { it.name == name && it.pillarLabel == loc && it.branch == br }

    @Test fun `正月寅月丁天德壬天德合`() {
        // 壬寅 壬寅 丁卯 壬寅——寅月：时干壬=天德合（丁天德须见丁干——月日干壬非丁）
        val b = chartOf(
            HeavenlyStem.REN, EarthlyBranch.YIN, HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.DING, EarthlyBranch.MAO, HeavenlyStem.REN, EarthlyBranch.YIN)
        assertTrue("寅月壬为天德合", hasStem(b, "天德合", "年干", HeavenlyStem.REN))
    }

    @Test fun `二月卯月申天德巳天德合`() {
        // 甲辰 丁卯 甲申 己巳——卯月：日支申=天德；时支巳=天德合
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.CHEN, HeavenlyStem.DING, EarthlyBranch.MAO,
            HeavenlyStem.JIA, EarthlyBranch.SHEN, HeavenlyStem.JI, EarthlyBranch.SI)
        assertTrue("卯月申为天德", hasBranch(b, "天德贵人", "日支", EarthlyBranch.SHEN))
        assertTrue("卯月巳为天德合", hasBranch(b, "天德合", "时支", EarthlyBranch.SI))
    }

    @Test fun `丑月庚天德乙天德合`() {
        // 乙丑 己丑 甲申 乙丑——丑月：庚天德（无庚干）；年时干乙=天德合
        val b = chartOf(
            HeavenlyStem.YI, EarthlyBranch.CHOU, HeavenlyStem.JI, EarthlyBranch.CHOU,
            HeavenlyStem.JIA, EarthlyBranch.SHEN, HeavenlyStem.YI, EarthlyBranch.CHOU)
        assertTrue("丑月乙为天德合（年干+时干）", hasStem(b, "天德合", "年干", HeavenlyStem.YI) && hasStem(b, "天德合", "时干", HeavenlyStem.YI))
        assertTrue("丑月无庚非天德", ShenShaCalculator.analyze(b).items.none { it.name == "天德贵人" })
    }

    @Test fun `亥月乙天德庚天德合`() {
        // 庚戌 丁亥 甲申 庚午——亥月：乙天德（无乙干）；庚干=天德合（年时双见）
        val b = chartOf(
            HeavenlyStem.GENG, EarthlyBranch.XU, HeavenlyStem.DING, EarthlyBranch.HAI,
            HeavenlyStem.JIA, EarthlyBranch.SHEN, HeavenlyStem.GENG, EarthlyBranch.WU)
        val items = ShenShaCalculator.analyze(b).items.filter { it.name == "天德合" }
        assertTrue("亥月庚为天德合（年干+时干）", items.any { it.pillarLabel == "年干" } && items.any { it.pillarLabel == "时干" })
    }
}
