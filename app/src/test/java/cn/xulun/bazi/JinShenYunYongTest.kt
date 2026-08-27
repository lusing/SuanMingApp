package cn.xulun.bazi

import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 神煞运用举例·金神（入火乡发富——喜忌为准实证）。理论见 .shengke-reference.md 第五九节六。
 */
class JinShenYunYongTest {
    @Test fun `壬辰辛亥己未癸酉 时带金神入火乡发富`() {
        val b = chartOf(
            HeavenlyStem.REN, EarthlyBranch.CHEN, HeavenlyStem.XIN, EarthlyBranch.HAI,
            HeavenlyStem.JI, EarthlyBranch.WEI, HeavenlyStem.GUI, EarthlyBranch.YOU)
        val items = ShenShaCalculator.analyze(b).items
        assertTrue("时柱癸酉金神——身弱用火，运入丁未丙午+南方发展成百万之富",
            items.any { it.name == "金神" && it.branch == EarthlyBranch.YOU && it.pillarLabel == "时柱" })
    }
}
