package cn.xulun.bazi

import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 亡神锚点（三合禄地，年支日支并查）。理论见 .shengke-reference.md 第五八节五。
 */
class WangShenTest {

    private fun items(b: cn.xulun.bazi.model.BaziResult) =
        ShenShaCalculator.analyze(b).items.filter { it.name == "亡神" }

    @Test fun `申子辰见亥（水局禄亥）`() {
        // 壬申 壬子 壬午辛亥? 亥非壬日亥时——辛亥时=壬日亥时辛亥✓：壬申 壬子 壬午 辛亥——年日申子组见亥：时支亥
        val b = chartOf(
            HeavenlyStem.REN, EarthlyBranch.SHEN, HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.REN, EarthlyBranch.WU, HeavenlyStem.XIN, EarthlyBranch.HAI)
        assertTrue("申子辰亡神亥（时支）", items(b).any { it.branch == EarthlyBranch.HAI && it.pillarLabel == "时支" })
    }

    @Test fun `年支异组 寅午戌见巳`() {
        // 甲戌 丙寅 甲午 乙丑——年支戌组（寅午戌→巳）：无巳；日支午组同——改盘：
        // 丙戌 庚寅 甲午 己巳——年支戌+日支午（寅午戌→巳）：时支巳命中
        val b = chartOf(
            HeavenlyStem.BING, EarthlyBranch.XU, HeavenlyStem.GENG, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.WU, HeavenlyStem.JI, EarthlyBranch.SI)
        assertTrue("寅午戌亡神巳（时支）", items(b).any { it.branch == EarthlyBranch.SI && it.pillarLabel == "时支" })
    }

    @Test fun `亥卯未见寅（木局禄寅）`() {
        // 乙卯 己卯 乙未 戊寅——年日支卯（亥卯未→寅）：时支寅命中
        val b = chartOf(
            HeavenlyStem.YI, EarthlyBranch.MAO, HeavenlyStem.JI, EarthlyBranch.MAO,
            HeavenlyStem.YI, EarthlyBranch.WEI, HeavenlyStem.WU, EarthlyBranch.YIN)
        assertTrue("亥卯未亡神寅（时支）", items(b).any { it.branch == EarthlyBranch.YIN && it.pillarLabel == "时支" })
    }
}
