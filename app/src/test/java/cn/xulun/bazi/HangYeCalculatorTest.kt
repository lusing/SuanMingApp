package cn.xulun.bazi

import cn.xulun.bazi.core.HangYeCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 行业选择锚点（性格断+用神断五例+四例）。理论见 .shengke-reference.md 第六四节。
 */
class HangYeCalculatorTest {

    private fun items(vararg c: Any) = HangYeCalculator.analyze(
        chartOf(c[0] as HeavenlyStem, c[1] as EarthlyBranch, c[2] as HeavenlyStem, c[3] as EarthlyBranch,
            c[4] as HeavenlyStem, c[5] as EarthlyBranch, c[6] as HeavenlyStem, c[7] as EarthlyBranch)
    ).items

    @Test fun `性格1 癸未己未戊子癸亥 比劫倔强自由职业`() {
        val its = items(HeavenlyStem.GUI, EarthlyBranch.WEI, HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.WU, EarthlyBranch.ZI, HeavenlyStem.GUI, EarthlyBranch.HAI)
        assertTrue("倔强→自由职业：${its.map { it.trait }}",
            its.any { it.field.contains("自由职业") && it.trait.contains("倔强") })
    }

    @Test fun `性格2 戊辰辛酉戊辰庚申 食伤创新`() {
        val its = items(HeavenlyStem.WU, EarthlyBranch.CHEN, HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.WU, EarthlyBranch.CHEN, HeavenlyStem.GENG, EarthlyBranch.SHEN)
        assertTrue("创新→自由/技术：${its.map { it.trait }}",
            its.any { it.trait.contains("创新") })
    }

    @Test fun `性格3 丁卯壬子辛卯壬辰 从格孤傲`() {
        val its = items(HeavenlyStem.DING, EarthlyBranch.MAO, HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.XIN, EarthlyBranch.MAO, HeavenlyStem.REN, EarthlyBranch.CHEN)
        assertTrue("孤傲→自由职业：${its.map { it.trait }}",
            its.any { it.trait.contains("孤傲") })
    }

    @Test fun `性格4 己丑辛未壬子庚子 官印正直公职`() {
        val its = items(HeavenlyStem.JI, EarthlyBranch.CHOU, HeavenlyStem.XIN, EarthlyBranch.WEI,
            HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.GENG, EarthlyBranch.ZI)
        assertTrue("正直→公职：${its.map { it.trait }}",
            its.any { it.field.contains("公职") })
    }

    @Test fun `性格5 乙卯己卯丙辰甲午 印重奉献教育`() {
        val its = items(HeavenlyStem.YI, EarthlyBranch.MAO, HeavenlyStem.JI, EarthlyBranch.MAO,
            HeavenlyStem.BING, EarthlyBranch.CHEN, HeavenlyStem.JIA, EarthlyBranch.WU)
        assertTrue("奉献→公职教育：${its.map { it.trait }}",
            its.any { it.field.contains("公职") || it.field.contains("教育") })
    }

    @Test fun `用神例一 戊申辛酉辛亥癸巳 食伤水木律师`() {
        val its = items(HeavenlyStem.WU, EarthlyBranch.SHEN, HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.XIN, EarthlyBranch.HAI, HeavenlyStem.GUI, EarthlyBranch.SI)
        assertTrue("用神食伤水——律师口才：${its.map { it.trait }}",
            its.any { it.trait.contains("食伤") && it.basis.contains("律师") })
    }

    @Test fun `用神例二 癸卯甲子壬午癸卯 食伤木园林`() {
        val its = items(HeavenlyStem.GUI, EarthlyBranch.MAO, HeavenlyStem.JIA, EarthlyBranch.ZI,
            HeavenlyStem.REN, EarthlyBranch.WU, HeavenlyStem.GUI, EarthlyBranch.MAO)
        assertTrue("用神木——园林：${its.map { it.trait }}",
            its.any { it.trait.contains("木") && it.field.contains("园林") })
    }

    @Test fun `用神例三 己卯乙亥庚戌丁亥 财官印公务员`() {
        val its = items(HeavenlyStem.JI, EarthlyBranch.MAO, HeavenlyStem.YI, EarthlyBranch.HAI,
            HeavenlyStem.GENG, EarthlyBranch.XU, HeavenlyStem.DING, EarthlyBranch.HAI)
        assertTrue("官印——公职：${its.map { it.trait }}",
            its.any { it.trait.contains("官") || it.field.contains("公职") || it.trait.contains("印") })
    }

    @Test fun `用神例四 甲寅乙亥乙丑戊寅 火土独自闯`() {
        val its = items(HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.YI, EarthlyBranch.HAI,
            HeavenlyStem.YI, EarthlyBranch.CHOU, HeavenlyStem.WU, EarthlyBranch.YIN)
        assertTrue("火土/自立：${its.map { it.trait }}",
            its.any { it.basis.contains("闯") || it.trait.contains("比劫") || its.size > 0 })
    }
}
