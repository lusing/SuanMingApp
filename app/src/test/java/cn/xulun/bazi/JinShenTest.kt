package cn.xulun.bazi

import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 金神锚点（时柱三座：乙丑/己巳/癸酉）。理论见 .shengke-reference.md 第五七节十七。
 */
class JinShenTest {

    private fun has(b: cn.xulun.bazi.model.BaziResult) =
        ShenShaCalculator.analyze(b).items.any { it.name == "金神" }

    @Test fun `乙丑时金神`() {
        // 甲辰 丙寅 甲戌 乙丑——时柱乙丑
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.CHEN, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.XU, HeavenlyStem.YI, EarthlyBranch.CHOU)
        assertTrue("乙丑时金神", has(b))
    }

    @Test fun `己巳癸酉时金神`() {
        // 甲子 丙寅 甲戌 己巳——甲日巳时=己巳
        val b1 = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.ZI, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.XU, HeavenlyStem.JI, EarthlyBranch.SI)
        assertTrue("己巳时金神", has(b1))
        // 己巳 戊辰 甲戌 癸酉——甲日酉时=癸酉
        val b2 = chartOf(
            HeavenlyStem.JI, EarthlyBranch.SI, HeavenlyStem.WU, EarthlyBranch.CHEN,
            HeavenlyStem.JIA, EarthlyBranch.XU, HeavenlyStem.GUI, EarthlyBranch.YOU)
        assertTrue("癸酉时金神", has(b2))
    }

    @Test fun `日柱乙丑非时柱不算`() {
        // 甲子 丙寅 乙丑 甲申——乙丑在日柱非时柱
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.ZI, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.YI, EarthlyBranch.CHOU, HeavenlyStem.JIA, EarthlyBranch.SHEN)
        assertTrue("日柱乙丑非金神", !has(b))
    }
}
