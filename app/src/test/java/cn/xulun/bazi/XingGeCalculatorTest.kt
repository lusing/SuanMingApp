package cn.xulun.bazi

import cn.xulun.bazi.core.XingGeCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 性格分析锚点（五行气势五例）。理论见 .shengke-reference.md 第六五节一。
 */
class XingGeCalculatorTest {

    private fun its(y: HeavenlyStem, yb: EarthlyBranch, m: HeavenlyStem, mb: EarthlyBranch,
                    d: HeavenlyStem, db: EarthlyBranch, h: HeavenlyStem, hb: EarthlyBranch) =
        XingGeCalculator.analyze(chartOf(y, yb, m, mb, d, db, h, hb)).items

    @Test fun `例一 辛亥辛卯甲寅辛未 木主固执独断`() {
        val r = its(HeavenlyStem.XIN, EarthlyBranch.HAI, HeavenlyStem.XIN, EarthlyBranch.MAO,
            HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.XIN, EarthlyBranch.WEI)
        assertTrue("木气势+甲干：${r.map { it.dim }}",
            r.any { it.dim.contains("木") && it.text.contains("固执独断") })
    }

    @Test fun `例二 甲寅乙亥丁巳丙午 木火刚强易冲动`() {
        val r = its(HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.YI, EarthlyBranch.HAI,
            HeavenlyStem.DING, EarthlyBranch.SI, HeavenlyStem.BING, EarthlyBranch.WU)
        assertTrue("丁干心性：${r.map { it.dim }}",
            r.any { it.dim.contains("丁") && it.text.contains("半途而废") })
    }

    @Test fun `例三 庚戌丙戌戊辰戊午 两戌冲辰倔强急躁`() {
        val r = its(HeavenlyStem.GENG, EarthlyBranch.XU, HeavenlyStem.BING, EarthlyBranch.XU,
            HeavenlyStem.WU, EarthlyBranch.CHEN, HeavenlyStem.WU, EarthlyBranch.WU)
        assertTrue("土旺+冲辰：${r.map { it.dim }}",
            r.any { (it.dim.contains("土") || it.dim.contains("戌")) && it.text.contains("倔强") })
    }

    @Test fun `例四 辛酉辛卯庚子甲申 金木交战杀伐`() {
        val r = its(HeavenlyStem.XIN, EarthlyBranch.YOU, HeavenlyStem.XIN, EarthlyBranch.MAO,
            HeavenlyStem.GENG, EarthlyBranch.ZI, HeavenlyStem.JIA, EarthlyBranch.SHEN)
        assertTrue("金木交战：${r.map { it.dim }}", r.any { it.dim.contains("交战") })
    }

    @Test fun `例五 壬子癸丑壬子戊申 土水混杂侥幸`() {
        val r = its(HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.WU, EarthlyBranch.SHEN)
        assertTrue("土水混杂：${r.map { it.dim }}", r.any { it.dim.contains("混杂") || it.text.contains("侥幸") })
    }
}
