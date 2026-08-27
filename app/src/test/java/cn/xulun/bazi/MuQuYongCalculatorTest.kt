package cn.xulun.bazi

import cn.xulun.bazi.core.MuQuYongCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.GodCategory
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.MuSpecial
import cn.xulun.bazi.model.MuTiaoHou
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 木之逐月取用锚点测试。16 例全部出自 .shengke-reference.md 第四四节（春 7 / 夏 3 / 秋 3 / 冬 3），
 * 均真实日期反推。
 */
class MuQuYongCalculatorTest {

    // ── 春木 ──

    @Test fun `春一 癸亥甲寅甲辰丙寅 丙火泄秀调候一石二鸟`() {
        val r = MuQuYongCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.CHEN,
            HeavenlyStem.BING, EarthlyBranch.YIN,
        ))
        assertEquals(GodCategory.SHI, r.yongShen)
        assertEquals(MuTiaoHou.XU_HUO, r.tiaoHou)
        assertTrue("basis 应含丙火泄秀：${r.basis}", r.basis.contains("丙") && r.basis.contains("泄秀"))
    }

    @Test fun `春二 甲戌丙寅甲午乙亥 火局自调取亥水`() {
        val r = MuQuYongCalculator.analyze(chartOf(
            HeavenlyStem.JIA, EarthlyBranch.XU,
            HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.WU,
            HeavenlyStem.YI, EarthlyBranch.HAI,
        ))
        assertEquals(GodCategory.YIN, r.yongShen)
        assertEquals(MuTiaoHou.BU_XU, r.tiaoHou)
        assertTrue("basis 应含不可再用火：${r.basis}", r.basis.contains("不可再用火"))
    }

    @Test fun `春三 丁丑壬寅乙酉辛巳 丁壬合两失其用`() {
        val r = MuQuYongCalculator.analyze(chartOf(
            HeavenlyStem.DING, EarthlyBranch.CHOU,
            HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.YI, EarthlyBranch.YOU,
            HeavenlyStem.XIN, EarthlyBranch.SI,
        ))
        assertEquals(MuSpecial.HE_ZHI, r.special)
    }

    @Test fun `春四 乙酉庚辰甲寅丁卯 中和随运而取`() {
        val r = MuQuYongCalculator.analyze(chartOf(
            HeavenlyStem.YI, EarthlyBranch.YOU,
            HeavenlyStem.GENG, EarthlyBranch.CHEN,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.DING, EarthlyBranch.MAO,
        ))
        assertEquals(MuSpecial.SUI_YUN, r.special)
    }

    @Test fun `春五 乙酉庚辰甲寅壬申 金重水通关力不足待岁运`() {
        val r = MuQuYongCalculator.analyze(chartOf(
            HeavenlyStem.YI, EarthlyBranch.YOU,
            HeavenlyStem.GENG, EarthlyBranch.CHEN,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.REN, EarthlyBranch.SHEN,
        ))
        assertEquals(GodCategory.YIN, r.yongShen)
        assertEquals(MuSpecial.DAI_SUI_YUN, r.special)
    }

    @Test fun `春六 甲寅戊辰甲戌戊辰 土厚甲坐禄疏土`() {
        val r = MuQuYongCalculator.analyze(chartOf(
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.WU, EarthlyBranch.CHEN,
            HeavenlyStem.JIA, EarthlyBranch.XU,
            HeavenlyStem.WU, EarthlyBranch.CHEN,
        ))
        assertEquals(GodCategory.BI, r.yongShen)
    }

    @Test fun `春七 丙午壬辰甲午丙寅 火众取壬水制火`() {
        val r = MuQuYongCalculator.analyze(chartOf(
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.REN, EarthlyBranch.CHEN,
            HeavenlyStem.JIA, EarthlyBranch.WU,
            HeavenlyStem.BING, EarthlyBranch.YIN,
        ))
        assertEquals(GodCategory.YIN, r.yongShen)
    }

    // ── 夏木 ──

    @Test fun `夏一 癸亥丁巳甲寅丁卯 水火既济中和随运`() {
        val r = MuQuYongCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.DING, EarthlyBranch.MAO,
        ))
        assertEquals(MuSpecial.SUI_YUN, r.special)
    }

    @Test fun `夏二 庚子辛巳乙卯己卯 中和偏弱随运`() {
        val r = MuQuYongCalculator.analyze(chartOf(
            HeavenlyStem.GENG, EarthlyBranch.ZI,
            HeavenlyStem.XIN, EarthlyBranch.SI,
            HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.JI, EarthlyBranch.MAO,
        ))
        assertTrue("basis 应含随运：${r.basis}", r.basis.contains("随运"))
    }

    @Test fun `夏三 壬子丙午乙卯辛巳 壬子之水为用`() {
        val r = MuQuYongCalculator.analyze(chartOf(
            HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.XIN, EarthlyBranch.SI,
        ))
        assertEquals(GodCategory.YIN, r.yongShen)
        assertTrue("basis 应含壬子：${r.basis}", r.basis.contains("壬子"))
    }

    // ── 秋木 ──

    @Test fun `秋一 戊寅庚申甲寅戊辰 水藏取寅甲帮身调候不紧`() {
        val r = MuQuYongCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.YIN,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.WU, EarthlyBranch.CHEN,
        ))
        assertEquals(GodCategory.BI, r.yongShen)
        assertEquals(MuTiaoHou.BU_JIN, r.tiaoHou)
    }

    @Test fun `秋二 乙卯乙酉乙亥壬午 失时不弱金成栋梁`() {
        val r = MuQuYongCalculator.analyze(chartOf(
            HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.YI, EarthlyBranch.YOU,
            HeavenlyStem.YI, EarthlyBranch.HAI,
            HeavenlyStem.REN, EarthlyBranch.WU,
        ))
        assertEquals(GodCategory.GUAN, r.yongShen)
        assertEquals(GodCategory.CAI, r.fuYong)
    }

    @Test fun `秋三 辛酉戊戌甲寅戊辰 拱午自调用木疏土`() {
        val r = MuQuYongCalculator.analyze(chartOf(
            HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.WU, EarthlyBranch.XU,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.WU, EarthlyBranch.CHEN,
        ))
        assertEquals(GodCategory.BI, r.yongShen)
        assertEquals(MuTiaoHou.BU_XU, r.tiaoHou)
    }

    // ── 冬木 ──

    @Test fun `冬一 乙卯丁亥甲寅己巳 寒木用火一物两用`() {
        val r = MuQuYongCalculator.analyze(chartOf(
            HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.DING, EarthlyBranch.HAI,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.JI, EarthlyBranch.SI,
        ))
        assertEquals(GodCategory.SHI, r.yongShen)
        assertEquals(MuTiaoHou.XU_HUO, r.tiaoHou)
    }

    @Test fun `冬二 丁未壬子乙卯丙子 火土两用`() {
        val r = MuQuYongCalculator.analyze(chartOf(
            HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.BING, EarthlyBranch.ZI,
        ))
        assertEquals(GodCategory.SHI, r.yongShen)
        assertEquals(GodCategory.CAI, r.fuYong)
    }

    @Test fun `冬三 癸卯乙丑甲子戊辰 寒极无火忌虚浮之火`() {
        val r = MuQuYongCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.YI, EarthlyBranch.CHOU,
            HeavenlyStem.JIA, EarthlyBranch.ZI,
            HeavenlyStem.WU, EarthlyBranch.CHEN,
        ))
        assertEquals(MuTiaoHou.HAN_JI, r.tiaoHou)
        assertTrue("basis 应含虚浮：${r.basis}", r.basis.contains("虚浮"))
    }
}
