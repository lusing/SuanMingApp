package cn.xulun.bazi

import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 驿马锚点（三合长生冲位——年支日支并查）。理论见 .shengke-reference.md 第五七节十。
 */
class YiMaTest {

    private fun items(b: cn.xulun.bazi.model.BaziResult) =
        ShenShaCalculator.analyze(b).items.filter { it.name == "驿马" }

    @Test fun `年支申组查寅`() {
        // 壬申 壬寅 壬子 辛丑——年日支申子（申子辰→寅）：月支寅命中
        val b = chartOf(
            HeavenlyStem.REN, EarthlyBranch.SHEN, HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.XIN, EarthlyBranch.CHOU)
        assertTrue("申子辰马在寅（月支）", items(b).any { it.branch == EarthlyBranch.YIN && it.pillarLabel == "月支" })
    }

    @Test fun `年日异组 巳组亥与亥组巳`() {
        // 乙巳（巳酉丑→亥） 丁亥 丙寅 戊子——年支巳组查月日时亥：月支亥命中；日支亥组（亥卯未→巳）查巳：无
        val b = chartOf(
            HeavenlyStem.YI, EarthlyBranch.SI, HeavenlyStem.DING, EarthlyBranch.HAI,
            HeavenlyStem.BING, EarthlyBranch.YIN, HeavenlyStem.WU, EarthlyBranch.ZI)
        assertTrue("巳酉丑马在亥（月支，年支组）", items(b).any { it.branch == EarthlyBranch.HAI && it.pillarLabel == "月支" })
    }

    @Test fun `马头带剑 壬申剑锋金`() {
        // 庚子 戊子 甲申 壬申——年支子组（申子辰→寅）无寅；日支申组（申子辰→寅）无寅——改：
        // 壬申日坐驿马之支须寅盘。用：戊申 壬戌 壬申 壬寅——日支申组查寅：时支寅；日柱壬申=剑锋金非马头（马在寅）
        val b = chartOf(
            HeavenlyStem.WU, EarthlyBranch.SHEN, HeavenlyStem.REN, EarthlyBranch.XU,
            HeavenlyStem.REN, EarthlyBranch.SHEN, HeavenlyStem.REN, EarthlyBranch.YIN)
        assertTrue("申组马在寅（时支）", items(b).any { it.branch == EarthlyBranch.YIN && it.pillarLabel == "时支" })
    }
}
