package cn.xulun.bazi

import cn.xulun.bazi.core.DaYunCalculator
import cn.xulun.bazi.core.JinQuYongCalculator
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.GodCategory
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 金之逐月取用锚点测试。13 例出自 .shengke-reference.md 第四七节（春 4 / 夏 5 / 秋 3 / 冬 2），
 * 均真实日期反推；含书载大运流年匹配验证（例三/例四/冬二）。
 */
class JinQuYongCalculatorTest {

    /** 书载岁运匹配：某流年所在大运柱。 */
    private fun yunAt(bazi: cn.xulun.bazi.model.BaziResult, gender: Gender, year: Int): String? {
        val info = DaYunCalculator.compute(bazi, gender)
        return info.yuns.firstOrNull { year >= it.startYear && year <= it.endYear }?.pillar?.fullName
    }

    // ── 春金 ──

    @Test fun `春一 癸卯甲寅庚戌甲申 财重取比劫`() {
        val r = JinQuYongCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.JIA, EarthlyBranch.SHEN,
        ))
        assertEquals(GodCategory.BI, r.yongShen)
        assertTrue("basis 应含比劫：${r.basis}", r.basis.contains("比劫"))
    }

    @Test fun `春二 辛丑庚寅辛未戊戌 失时不弱首选财`() {
        val r = JinQuYongCalculator.analyze(chartOf(
            HeavenlyStem.XIN, EarthlyBranch.CHOU,
            HeavenlyStem.GENG, EarthlyBranch.YIN,
            HeavenlyStem.XIN, EarthlyBranch.WEI,
            HeavenlyStem.WU, EarthlyBranch.XU,
        ))
        assertEquals(GodCategory.CAI, r.yongShen)
        assertEquals(GodCategory.SHI, r.fuYong)
    }

    @Test fun `春三 壬申癸卯辛巳庚寅 取庚金帮身随岁运变`() {
        val r = JinQuYongCalculator.analyze(chartOf(
            HeavenlyStem.REN, EarthlyBranch.SHEN,
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.XIN, EarthlyBranch.SI,
            HeavenlyStem.GENG, EarthlyBranch.YIN,
            fromYear = 1932, toYear = 1933,
        ))
        assertEquals(GodCategory.BI, r.yongShen)
        assertEquals(cn.xulun.bazi.model.JinSpecial.SUI_YUN, r.special)
    }

    @Test fun `春三大运 1935未起运 1958庚子 1972己亥 阳女逆排`() {
        // 书载岁运验证：1935 乙亥"大运尚在月柱"；1958 戊戌行庚子运；1972 壬子行己亥运
        val b = chartOf(
            HeavenlyStem.REN, EarthlyBranch.SHEN,
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.XIN, EarthlyBranch.SI,
            HeavenlyStem.GENG, EarthlyBranch.YIN,
            fromYear = 1932, toYear = 1933,
        )
        val info = DaYunCalculator.compute(b, Gender.FEMALE) // 壬年阳女逆排
        assertEquals(false, info.forward)
        assertTrue("1935 应未起运（起运 ${info.qiYunText}，首运 ${info.yuns.first().startYear} 年）",
            1935 < info.yuns.first().startYear)
        assertEquals("庚子", yunAt(b, Gender.FEMALE, 1958))
        assertEquals("己亥", yunAt(b, Gender.FEMALE, 1972))
    }

    @Test fun `春四 乙巳庚辰辛丑庚寅 身印两强取木用水为喜`() {
        val r = JinQuYongCalculator.analyze(chartOf(
            HeavenlyStem.YI, EarthlyBranch.SI,
            HeavenlyStem.GENG, EarthlyBranch.CHEN,
            HeavenlyStem.XIN, EarthlyBranch.CHOU,
            HeavenlyStem.GENG, EarthlyBranch.YIN,
        ))
        assertEquals(GodCategory.CAI, r.yongShen)
        assertEquals(GodCategory.SHI, r.fuYong)
    }

    @Test fun `春四大运 戊寅运癸亥年 阴男逆排`() {
        // 书载：戊寅运癸亥年考上大学（乙巳年 = 1965，癸亥 = 1983）
        val b = chartOf(
            HeavenlyStem.YI, EarthlyBranch.SI,
            HeavenlyStem.GENG, EarthlyBranch.CHEN,
            HeavenlyStem.XIN, EarthlyBranch.CHOU,
            HeavenlyStem.GENG, EarthlyBranch.YIN,
            fromYear = 1965, toYear = 1966,
        )
        val info = DaYunCalculator.compute(b, Gender.MALE) // 乙年阴男逆排
        assertEquals(false, info.forward)
        assertEquals("戊寅", yunAt(b, Gender.MALE, 1983))
    }

    // ── 夏金 ──

    @Test fun `夏一 戊申丁巳辛亥甲午 水制火存金庚辅`() {
        val r = JinQuYongCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.XIN, EarthlyBranch.HAI,
            HeavenlyStem.JIA, EarthlyBranch.WU,
        ))
        assertEquals(GodCategory.SHI, r.yongShen)
        assertEquals(GodCategory.BI, r.fuYong)
    }

    @Test fun `夏二 癸卯丁巳庚申丁丑 用神在丑`() {
        val r = JinQuYongCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.DING, EarthlyBranch.CHOU,
        ))
        assertEquals(GodCategory.YIN, r.yongShen)
        assertTrue("basis 应含申巳合与丑：${r.basis}", r.basis.contains("申巳") || r.basis.contains("丑"))
    }

    @Test fun `夏三 己酉庚午辛未癸巳 湿土通关金水为喜`() {
        val r = JinQuYongCalculator.analyze(chartOf(
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.GENG, EarthlyBranch.WU,
            HeavenlyStem.XIN, EarthlyBranch.WEI,
            HeavenlyStem.GUI, EarthlyBranch.SI,
        ))
        assertEquals(GodCategory.YIN, r.yongShen)
        assertTrue("basis 应含湿土：${r.basis}", r.basis.contains("湿土"))
    }

    @Test fun `夏四 戊申己未庚午辛巳 午未合增燥身弱`() {
        val r = JinQuYongCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.GENG, EarthlyBranch.WU,
            HeavenlyStem.XIN, EarthlyBranch.SI,
        ))
        assertEquals(DayStrength.WEAK, r.strength)
        assertTrue("basis 应含燥：${r.basis}", r.basis.contains("燥"))
    }

    @Test fun `夏五 戊申己未庚辰庚辰 辰润金强`() {
        val r = JinQuYongCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.GENG, EarthlyBranch.CHEN,
            HeavenlyStem.GENG, EarthlyBranch.CHEN,
        ))
        assertEquals(DayStrength.STRONG, r.strength)
        assertTrue("basis 应含润：${r.basis}", r.basis.contains("润"))
    }

    // ── 秋金 ──

    @Test fun `秋一 壬申戊申庚午丙戌 用午中丁火`() {
        val r = JinQuYongCalculator.analyze(chartOf(
            HeavenlyStem.REN, EarthlyBranch.SHEN,
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.GENG, EarthlyBranch.WU,
            HeavenlyStem.BING, EarthlyBranch.XU,
        ))
        assertEquals(GodCategory.GUAN, r.yongShen)
        assertTrue("basis 应含丁火：${r.basis}", r.basis.contains("丁火"))
    }

    @Test fun `秋三 丙申戊戌辛丑壬辰 病在土众木疏有层次`() {
        val r = JinQuYongCalculator.analyze(chartOf(
            HeavenlyStem.BING, EarthlyBranch.SHEN,
            HeavenlyStem.WU, EarthlyBranch.XU,
            HeavenlyStem.XIN, EarthlyBranch.CHOU,
            HeavenlyStem.REN, EarthlyBranch.CHEN,
        ))
        assertEquals(GodCategory.CAI, r.yongShen)
        assertEquals(cn.xulun.bazi.model.JinSpecial.CENG_CI, r.special)
    }

    // ── 冬金 ──

    @Test fun `冬一 乙亥丁亥庚戌庚辰 火土并用燥土为良`() {
        val r = JinQuYongCalculator.analyze(chartOf(
            HeavenlyStem.YI, EarthlyBranch.HAI,
            HeavenlyStem.DING, EarthlyBranch.HAI,
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.GENG, EarthlyBranch.CHEN,
        ))
        assertEquals(GodCategory.YIN, r.yongShen)
        assertTrue("basis 应含燥土：${r.basis}", r.basis.contains("燥土"))
    }

    @Test fun `冬二 辛丑辛丑庚申丁亥 身强取亥水泄秀`() {
        val r = JinQuYongCalculator.analyze(chartOf(
            HeavenlyStem.XIN, EarthlyBranch.CHOU,
            HeavenlyStem.XIN, EarthlyBranch.CHOU,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.DING, EarthlyBranch.HAI,
        ))
        assertEquals(GodCategory.SHI, r.yongShen)
    }

    @Test fun `冬二大运 戊戌运戊辰年 阴男逆排`() {
        // 书载：戊戌运戊辰年厚土克亥水患精神病（辛丑年生，戊辰 = +27 年）
        val b = chartOf(
            HeavenlyStem.XIN, EarthlyBranch.CHOU,
            HeavenlyStem.XIN, EarthlyBranch.CHOU,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.DING, EarthlyBranch.HAI,
        )
        val birthYear = b.birthDateTime.year
        val info = DaYunCalculator.compute(b, Gender.MALE) // 辛年阴男逆排
        assertEquals(false, info.forward)
        assertEquals("戊戌", yunAt(b, Gender.MALE, birthYear + 27))
    }
}
