package cn.xulun.bazi

import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 阴注阳受锚点（月支查表·查年日时支）。理论见 .shengke-reference.md 第五七节十五。
 */
class YinZhuYangShouTest {

    private fun items(b: cn.xulun.bazi.model.BaziResult) =
        ShenShaCalculator.analyze(b).items.filter { it.name == "阴注阳受" }

    @Test fun `寅月见子`() {
        // 甲辰 丙寅 甲子 乙丑——寅月阴注阳受=子：日支子命中
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.CHEN, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.ZI, HeavenlyStem.YI, EarthlyBranch.CHOU)
        assertTrue("寅月见子（日支）", items(b).any { it.branch == EarthlyBranch.ZI && it.pillarLabel == "日支" })
    }

    @Test fun `巳月见酉 午月见戌（不规则）`() {
        // 辛巳 癸巳 辛酉 丙申——巳月=酉：日支酉命中
        val b1 = chartOf(
            HeavenlyStem.XIN, EarthlyBranch.SI, HeavenlyStem.GUI, EarthlyBranch.SI,
            HeavenlyStem.XIN, EarthlyBranch.YOU, HeavenlyStem.BING, EarthlyBranch.SHEN)
        assertTrue("巳月见酉", items(b1).any { it.branch == EarthlyBranch.YOU })
        // 戊戌 戊午 戊戌 戊午——午月=戌：年支/日支戌双见
        val b2 = chartOf(
            HeavenlyStem.WU, EarthlyBranch.XU, HeavenlyStem.WU, EarthlyBranch.WU,
            HeavenlyStem.WU, EarthlyBranch.XU, HeavenlyStem.WU, EarthlyBranch.WU)
        assertTrue("午月见戌（年日双见，非逆行之酉）", items(b2).count { it.branch == EarthlyBranch.XU } == 2)
    }

    @Test fun `月支自身不查`() {
        // 乙巳 丁亥 乙卯 甲申——亥月=卯：日支卯命中；月支亥自身非卯
        val b = chartOf(
            HeavenlyStem.YI, EarthlyBranch.SI, HeavenlyStem.DING, EarthlyBranch.HAI,
            HeavenlyStem.YI, EarthlyBranch.MAO, HeavenlyStem.JIA, EarthlyBranch.SHEN)
        assertTrue("亥月见卯（日支）", items(b).any { it.branch == EarthlyBranch.MAO && it.pillarLabel == "日支" })
    }
}
