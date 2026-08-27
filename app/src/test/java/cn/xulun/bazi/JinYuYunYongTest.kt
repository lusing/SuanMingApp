package cn.xulun.bazi

import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 神煞运用举例·金舆（车缘逢冲主车祸实证）。理论见 .shengke-reference.md 第五九节二。
 */
class JinYuYunYongTest {
    @Test fun `己酉庚午庚辰丙戌 庚日金舆戌时支`() {
        val b = chartOf(
            HeavenlyStem.JI, EarthlyBranch.YOU, HeavenlyStem.GENG, EarthlyBranch.WU,
            HeavenlyStem.GENG, EarthlyBranch.CHEN, HeavenlyStem.BING, EarthlyBranch.XU)
        val items = ShenShaCalculator.analyze(b).items
        assertTrue("庚日金舆戌（时支）——1988三辰冲戌车祸之验",
            items.any { it.name == "金舆" && it.branch == EarthlyBranch.XU && it.pillarLabel == "时支" })
    }
}
