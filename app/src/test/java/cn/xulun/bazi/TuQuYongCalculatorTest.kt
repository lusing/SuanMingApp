package cn.xulun.bazi

import cn.xulun.bazi.core.TuQuYongCalculator
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.GodCategory
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.TuSpecial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 土之逐月取用锚点测试。10 例出自 .shengke-reference.md 第四六节（春 3 / 夏 2 / 秋 1 / 冬 4），
 * 均真实日期反推。
 */
class TuQuYongCalculatorTest {

    // ── 春土 ──

    @Test fun `春一 己亥丙寅己未乙亥 丙火通关调候一物两用`() {
        val r = TuQuYongCalculator.analyze(chartOf(
            HeavenlyStem.JI, EarthlyBranch.HAI,
            HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.YI, EarthlyBranch.HAI,
        ))
        assertEquals(GodCategory.YIN, r.yongShen)
        assertEquals(GodCategory.BI, r.fuYong)
        assertTrue("basis 应含一物两用：${r.basis}", r.basis.contains("一物两用"))
    }

    @Test fun `春二 庚辰己卯戊辰癸亥 木实土虚身弱`() {
        val r = TuQuYongCalculator.analyze(chartOf(
            HeavenlyStem.GENG, EarthlyBranch.CHEN,
            HeavenlyStem.JI, EarthlyBranch.MAO,
            HeavenlyStem.WU, EarthlyBranch.CHEN,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
        ))
        assertEquals(DayStrength.WEAK, r.strength)
        assertTrue("basis 应含虚：${r.basis}", r.basis.contains("虚"))
    }

    @Test fun `春三 己亥戊辰戊辰癸亥 身旺财弱取金水`() {
        val r = TuQuYongCalculator.analyze(chartOf(
            HeavenlyStem.JI, EarthlyBranch.HAI,
            HeavenlyStem.WU, EarthlyBranch.CHEN,
            HeavenlyStem.WU, EarthlyBranch.CHEN,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
        ))
        assertEquals(DayStrength.STRONG, r.strength)
        assertEquals(GodCategory.SHI, r.yongShen)
        assertEquals(GodCategory.CAI, r.fuYong)
    }

    // ── 夏土 ──

    @Test fun `夏一 癸巳丁巳己卯丁卯 燥极水调候为喜`() {
        val r = TuQuYongCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.SI,
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.JI, EarthlyBranch.MAO,
            HeavenlyStem.DING, EarthlyBranch.MAO,
        ))
        assertEquals(GodCategory.CAI, r.yongShen)
        assertEquals(TuSpecial.ZAO_JI, r.special)
    }

    @Test fun `夏二 丙辰乙未戊子庚申 申子辰难化身强财弱`() {
        val r = TuQuYongCalculator.analyze(chartOf(
            HeavenlyStem.BING, EarthlyBranch.CHEN,
            HeavenlyStem.YI, EarthlyBranch.WEI,
            HeavenlyStem.WU, EarthlyBranch.ZI,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
        ))
        assertEquals(DayStrength.STRONG, r.strength)
        assertTrue("basis 应含难化：${r.basis}", r.basis.contains("难化"))
    }

    // ── 秋土 ──

    @Test fun `秋 壬辰戊申己丑丁卯 湿土中和随运`() {
        val r = TuQuYongCalculator.analyze(chartOf(
            HeavenlyStem.REN, EarthlyBranch.CHEN,
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.JI, EarthlyBranch.CHOU,
            HeavenlyStem.DING, EarthlyBranch.MAO,
        ))
        assertEquals(TuSpecial.SUI_YUN, r.special)
        assertTrue("basis 应含湿：${r.basis}", r.basis.contains("湿"))
    }

    // ── 冬土 ──

    @Test fun `冬一 戊戌癸亥己未丙寅 身强反取水木`() {
        val r = TuQuYongCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.XU,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.BING, EarthlyBranch.YIN,
        ))
        assertEquals(GodCategory.GUAN, r.yongShen)
        assertEquals(GodCategory.CAI, r.fuYong)
    }

    @Test fun `冬二 丁巳壬子戊戌甲寅 取巳中丙火戌戊帮身`() {
        val r = TuQuYongCalculator.analyze(chartOf(
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.WU, EarthlyBranch.XU,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
        ))
        assertEquals(GodCategory.YIN, r.yongShen)
        assertEquals(GodCategory.BI, r.fuYong)
        assertTrue("basis 应含巳中丙：${r.basis}", r.basis.contains("巳中丙"))
    }

    @Test fun `冬三 甲辰丁丑戊寅癸亥 中和随运`() {
        val r = TuQuYongCalculator.analyze(chartOf(
            HeavenlyStem.JIA, EarthlyBranch.CHEN,
            HeavenlyStem.DING, EarthlyBranch.CHOU,
            HeavenlyStem.WU, EarthlyBranch.YIN,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
        ))
        assertEquals(TuSpecial.SUI_YUN, r.special)
    }

    @Test fun `冬四 丁巳癸丑己卯癸酉 合而难化中和偏强`() {
        val r = TuQuYongCalculator.analyze(chartOf(
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.JI, EarthlyBranch.MAO,
            HeavenlyStem.GUI, EarthlyBranch.YOU,
        ))
        assertEquals(TuSpecial.SUI_YUN, r.special)
        assertTrue("basis 应含难化：${r.basis}", r.basis.contains("难化"))
    }
}
