package cn.xulun.bazi

import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 神煞运用举例·驿马（流动性职业+逢冲调动实证）。理论见 .shengke-reference.md 第五九节五。
 */
class YiMaYunYongTest {

    private fun ma(b: cn.xulun.bazi.model.BaziResult) =
        ShenShaCalculator.analyze(b).items.filter { it.name == "驿马" }

    @Test fun `A 辛亥乙未癸丑丁巳 马星巳居时支（推销员）`() {
        val b = chartOf(
            HeavenlyStem.XIN, EarthlyBranch.HAI, HeavenlyStem.YI, EarthlyBranch.WEI,
            HeavenlyStem.GUI, EarthlyBranch.CHOU, HeavenlyStem.DING, EarthlyBranch.SI)
        assertTrue("亥年组马在巳（时支）", ma(b).any { it.branch == EarthlyBranch.SI && it.pillarLabel == "时支" })
    }

    @Test fun `B 己酉辛未己亥丁卯 马星亥居日支（记者）`() {
        val b = chartOf(
            HeavenlyStem.JI, EarthlyBranch.YOU, HeavenlyStem.XIN, EarthlyBranch.WEI,
            HeavenlyStem.JI, EarthlyBranch.HAI, HeavenlyStem.DING, EarthlyBranch.MAO)
        assertTrue("酉年组马在亥（日支）", ma(b).any { it.branch == EarthlyBranch.HAI && it.pillarLabel == "日支" })
    }

    @Test fun `C 壬子己酉甲寅丙寅 马星寅日时双见（乘务员）`() {
        val b = chartOf(
            HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.BING, EarthlyBranch.YIN)
        val m = ma(b)
        assertTrue("子年组马在寅（日支+时支）", m.any { it.branch == EarthlyBranch.YIN && it.pillarLabel == "日支" } &&
            m.any { it.branch == EarthlyBranch.YIN && it.pillarLabel == "时支" })
    }

    @Test fun `D 戊戌庚申丙寅庚寅 月柱驿马申逢寅冲（调动两验）`() {
        val b = chartOf(
            HeavenlyStem.WU, EarthlyBranch.XU, HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.BING, EarthlyBranch.YIN, HeavenlyStem.GENG, EarthlyBranch.YIN)
        assertTrue("戌年组马在申（月支）——日时寅冲（1986丙寅冲马/1992壬申马值）",
            ma(b).any { it.branch == EarthlyBranch.SHEN && it.pillarLabel == "月支" })
    }
}
