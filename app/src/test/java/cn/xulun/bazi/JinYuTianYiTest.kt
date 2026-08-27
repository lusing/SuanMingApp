package cn.xulun.bazi

import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 金舆（禄前二位·年日干查）与天医（月支前一位·查年日时）锚点。
 * 理论见 .shengke-reference.md 第五七节十一、十二。
 */
class JinYuTianYiTest {

    private fun items(b: cn.xulun.bazi.model.BaziResult, name: String) =
        ShenShaCalculator.analyze(b).items.filter { it.name == name }

    @Test fun `金舆 甲日见辰`() {
        // 甲辰 丙寅 甲申 丙寅——甲日年支辰=金舆（年干甲并查辰）
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.CHEN, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.SHEN, HeavenlyStem.BING, EarthlyBranch.YIN)
        assertTrue("甲日见辰金舆", items(b, "金舆").any { it.branch == EarthlyBranch.CHEN })
    }

    @Test fun `金舆年干查 癸年干见寅`() {
        // 癸卯 甲寅 庚申 丙子——年干癸→寅：月支寅命中；日干庚→戌：无
        val b = chartOf(
            HeavenlyStem.GUI, EarthlyBranch.MAO, HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.GENG, EarthlyBranch.SHEN, HeavenlyStem.BING, EarthlyBranch.ZI)
        assertTrue("年干癸查月支寅金舆", items(b, "金舆").any { it.branch == EarthlyBranch.YIN && it.pillarLabel == "月支" })
    }

    @Test fun `天医 卯月见寅`() {
        // 癸卯 甲寅 庚申 丙子——卯月天医=寅：年?（查年日时）日支申非寅；月支寅不查——改盘：
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.CHEN, HeavenlyStem.DING, EarthlyBranch.MAO,
            HeavenlyStem.GENG, EarthlyBranch.YIN, HeavenlyStem.BING, EarthlyBranch.ZI)
        assertTrue("卯月天医寅（日支）", items(b, "天医").any { it.branch == EarthlyBranch.YIN })
    }

    @Test fun `天医 子月见亥`() {
        // 甲子 丙子 甲申 乙亥——子月天医=亥：时支亥命中
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.ZI, HeavenlyStem.BING, EarthlyBranch.ZI,
            HeavenlyStem.JIA, EarthlyBranch.SHEN, HeavenlyStem.YI, EarthlyBranch.HAI)
        assertTrue("子月天医亥（时支）", items(b, "天医").any { it.branch == EarthlyBranch.HAI && it.pillarLabel == "时支" })
    }
}
