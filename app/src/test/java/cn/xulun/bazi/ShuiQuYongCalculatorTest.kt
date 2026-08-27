package cn.xulun.bazi

import cn.xulun.bazi.core.DaYunCalculator
import cn.xulun.bazi.core.ShuiQuYongCalculator
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.GodCategory
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.ShuiSpecial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 水之逐月取用锚点测试。13 例出自 .shengke-reference.md 第四八节（春 2 / 夏 3 / 秋 4 / 冬 3），
 * 均真实日期反推；含四处书载大运流年匹配验证（春一/春二/秋二/冬三）。
 * 夏三（庚戌癸未癸丑甲子）为夜子时造，时柱用次日干起子时，现行排盘无此约定，暂缺。
 */
class ShuiQuYongCalculatorTest {

    private fun yunAt(bazi: cn.xulun.bazi.model.BaziResult, gender: Gender, year: Int): String? {
        val info = DaYunCalculator.compute(bazi, gender)
        return info.yuns.firstOrNull { year >= it.startYear && year <= it.endYear }?.pillar?.fullName
    }

    // ── 春水 ──

    @Test fun `春一 丁酉癸卯壬辰己酉 母慈灭子取比劫`() {
        val r = ShuiQuYongCalculator.analyze(chartOf(
            HeavenlyStem.DING, EarthlyBranch.YOU,
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.REN, EarthlyBranch.CHEN,
            HeavenlyStem.JI, EarthlyBranch.YOU,
        ))
        assertEquals(GodCategory.BI, r.yongShen)
        assertTrue("basis 应含母慈灭子：${r.basis}", r.basis.contains("母慈灭子"))
    }

    @Test fun `春一大运 己亥运壬申年 阴男逆排`() {
        val b = chartOf(
            HeavenlyStem.DING, EarthlyBranch.YOU,
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.REN, EarthlyBranch.CHEN,
            HeavenlyStem.JI, EarthlyBranch.YOU,
            fromYear = 1957, toYear = 1958,
        )
        val info = DaYunCalculator.compute(b, Gender.MALE) // 丁年阴男逆排
        assertEquals(false, info.forward)
        assertEquals("己亥", yunAt(b, Gender.MALE, 1992))
    }

    @Test fun `春二 癸丑丙辰癸未戊午 杀重用丑中辛金`() {
        val r = ShuiQuYongCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.BING, EarthlyBranch.CHEN,
            HeavenlyStem.GUI, EarthlyBranch.WEI,
            HeavenlyStem.WU, EarthlyBranch.WU,
        ))
        assertEquals(GodCategory.YIN, r.yongShen)
        assertTrue("basis 应含丑中辛：${r.basis}", r.basis.contains("丑中辛"))
    }

    @Test fun `春二大运 甲寅运乙亥年 阴男逆排`() {
        val b = chartOf(
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.BING, EarthlyBranch.CHEN,
            HeavenlyStem.GUI, EarthlyBranch.WEI,
            HeavenlyStem.WU, EarthlyBranch.WU,
            fromYear = 1973, toYear = 1974,
        )
        val info = DaYunCalculator.compute(b, Gender.MALE) // 癸年阴男逆排
        assertEquals(false, info.forward)
        assertEquals("甲寅", yunAt(b, Gender.MALE, 1995))
    }

    // ── 夏水 ──

    @Test fun `夏一 戊午丁巳壬子甲辰 赖日支敌旺火`() {
        val r = ShuiQuYongCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.WU,
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.JIA, EarthlyBranch.CHEN,
        ))
        assertEquals(GodCategory.BI, r.yongShen)
        assertTrue("basis 应含不以半合：${r.basis}", r.basis.contains("不以半合"))
    }

    @Test fun `夏二 乙卯壬午癸巳癸丑 盖头熏蒸身弱`() {
        val r = ShuiQuYongCalculator.analyze(chartOf(
            HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.REN, EarthlyBranch.WU,
            HeavenlyStem.GUI, EarthlyBranch.SI,
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
        ))
        assertEquals(DayStrength.WEAK, r.strength)
        assertTrue("basis 应含盖头或熏蒸：${r.basis}", r.basis.contains("盖头") || r.basis.contains("熏蒸"))
    }

    @Test fun `夏四 庚戌癸未壬辰辛亥 终生取金水`() {
        val r = ShuiQuYongCalculator.analyze(chartOf(
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.GUI, EarthlyBranch.WEI,
            HeavenlyStem.REN, EarthlyBranch.CHEN,
            HeavenlyStem.XIN, EarthlyBranch.HAI,
        ))
        assertEquals(GodCategory.YIN, r.yongShen)
        assertEquals(GodCategory.BI, r.fuYong)
        assertTrue("basis 应含越金克水：${r.basis}", r.basis.contains("越金克水"))
    }

    // ── 秋水 ──

    @Test fun `秋一 壬子戊申癸亥己未 时柱双体土止水之堤`() {
        val r = ShuiQuYongCalculator.analyze(chartOf(
            HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.JI, EarthlyBranch.WEI,
        ))
        assertEquals(GodCategory.GUAN, r.yongShen)
        assertTrue("basis 应含堤：${r.basis}", r.basis.contains("堤"))
    }

    @Test fun `秋二 庚申乙酉癸巳癸亥 两气成象顺金水`() {
        val r = ShuiQuYongCalculator.analyze(chartOf(
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.YI, EarthlyBranch.YOU,
            HeavenlyStem.GUI, EarthlyBranch.SI,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
        ))
        assertEquals(ShuiSpecial.LIANG_QI, r.special)
    }

    @Test fun `秋二大运 甲申运乙丑年 癸未运丙子年 阳女逆排`() {
        val b = chartOf(
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.YI, EarthlyBranch.YOU,
            HeavenlyStem.GUI, EarthlyBranch.SI,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            fromYear = 1980, toYear = 1981,
        )
        val info = DaYunCalculator.compute(b, Gender.FEMALE) // 庚年阳女逆排
        assertEquals(false, info.forward)
        assertEquals("甲申", yunAt(b, Gender.FEMALE, 1985))
        assertEquals("癸未", yunAt(b, Gender.FEMALE, 1996))
    }

    @Test fun `秋三 戊午辛酉癸巳壬子 身强用财取午火`() {
        val r = ShuiQuYongCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.WU,
            HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.GUI, EarthlyBranch.SI,
            HeavenlyStem.REN, EarthlyBranch.ZI,
        ))
        assertEquals(GodCategory.CAI, r.yongShen)
        assertEquals(GodCategory.SHI, r.fuYong)
    }

    @Test fun `秋四 甲寅甲戌癸亥庚申 强弱随岁运变化`() {
        val r = ShuiQuYongCalculator.analyze(chartOf(
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.XU,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
        ))
        assertEquals(ShuiSpecial.SUI_YUN, r.special)
    }

    // ── 冬水 ──

    @Test fun `冬一 辛丑己亥壬午壬寅 身强午火调候寅午半合`() {
        val r = ShuiQuYongCalculator.analyze(chartOf(
            HeavenlyStem.XIN, EarthlyBranch.CHOU,
            HeavenlyStem.JI, EarthlyBranch.HAI,
            HeavenlyStem.REN, EarthlyBranch.WU,
            HeavenlyStem.REN, EarthlyBranch.YIN,
        ))
        assertEquals(GodCategory.CAI, r.yongShen)
        assertEquals(GodCategory.GUAN, r.fuYong)
    }

    @Test fun `冬二 丁未壬子壬午庚子 金寒水冻火土并用`() {
        val r = ShuiQuYongCalculator.analyze(chartOf(
            HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.REN, EarthlyBranch.WU,
            HeavenlyStem.GENG, EarthlyBranch.ZI,
        ))
        assertEquals(GodCategory.CAI, r.yongShen)
        assertEquals(GodCategory.GUAN, r.fuYong)
        assertTrue("basis 应含夹冲或壬合：${r.basis}", r.basis.contains("夹冲") || r.basis.contains("壬合"))
    }

    @Test fun `冬三 丁未壬子癸丑癸丑 身偏旺随运流转`() {
        val r = ShuiQuYongCalculator.analyze(chartOf(
            HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
        ))
        assertEquals(ShuiSpecial.SUI_YUN, r.special)
    }

    @Test fun `冬三大运 癸丑运癸亥年 阴女顺排`() {
        val b = chartOf(
            HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            fromYear = 1967, toYear = 1968,
        )
        val info = DaYunCalculator.compute(b, Gender.FEMALE) // 丁年阴女顺排
        assertEquals(true, info.forward)
        assertEquals("癸丑", yunAt(b, Gender.FEMALE, 1983))
    }
}
