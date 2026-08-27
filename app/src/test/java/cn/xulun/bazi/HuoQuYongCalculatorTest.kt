package cn.xulun.bazi

import cn.xulun.bazi.core.HuoQuYongCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.GodCategory
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.HuoSpecial
import cn.xulun.bazi.model.HuoTiaoHou
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 火之逐月取用锚点测试。13 例出自 .shengke-reference.md 第四五节（春 3 / 夏 3 / 秋 5 / 冬 2），
 * 均真实日期反推。
 */
class HuoQuYongCalculatorTest {

    // ── 春火 ──

    @Test fun `春一 甲子丙寅丙午己丑 己土泄秀为用`() {
        val r = HuoQuYongCalculator.analyze(chartOf(
            HeavenlyStem.JIA, EarthlyBranch.ZI,
            HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.JI, EarthlyBranch.CHOU,
        ))
        assertEquals(GodCategory.SHI, r.yongShen)
        assertTrue("basis 应含泄秀：${r.basis}", r.basis.contains("泄秀"))
    }

    @Test fun `春三 己丑戊辰丙寅甲午 湿土晦火取木疏土`() {
        val r = HuoQuYongCalculator.analyze(chartOf(
            HeavenlyStem.JI, EarthlyBranch.CHOU,
            HeavenlyStem.WU, EarthlyBranch.CHEN,
            HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.WU,
        ))
        assertEquals(GodCategory.YIN, r.yongShen)
        assertTrue("basis 应含湿土：${r.basis}", r.basis.contains("湿土"))
    }

    // ── 夏火 ──

    @Test fun `夏一 乙巳辛巳丙寅辛卯 丙辛合反怯可用金`() {
        val r = HuoQuYongCalculator.analyze(chartOf(
            HeavenlyStem.YI, EarthlyBranch.SI,
            HeavenlyStem.XIN, EarthlyBranch.SI,
            HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.XIN, EarthlyBranch.MAO,
        ))
        assertEquals(GodCategory.CAI, r.yongShen)
        assertTrue("basis 应含丙辛合：${r.basis}", r.basis.contains("丙辛合"))
    }

    @Test fun `夏二 壬辰乙巳丁丑壬寅 湿土泄秀喜金`() {
        val r = HuoQuYongCalculator.analyze(chartOf(
            HeavenlyStem.REN, EarthlyBranch.CHEN,
            HeavenlyStem.YI, EarthlyBranch.SI,
            HeavenlyStem.DING, EarthlyBranch.CHOU,
            HeavenlyStem.REN, EarthlyBranch.YIN,
        ))
        assertEquals(GodCategory.SHI, r.yongShen)
        assertEquals(GodCategory.CAI, r.fuYong)
    }

    @Test fun `夏三 己丑庚午丙子戊戌 土众晦火用午火不需调候`() {
        val r = HuoQuYongCalculator.analyze(chartOf(
            HeavenlyStem.JI, EarthlyBranch.CHOU,
            HeavenlyStem.GENG, EarthlyBranch.WU,
            HeavenlyStem.BING, EarthlyBranch.ZI,
            HeavenlyStem.WU, EarthlyBranch.XU,
        ))
        assertEquals(GodCategory.BI, r.yongShen)
        assertEquals(HuoTiaoHou.BU_XU, r.tiaoHou)
    }

    // ── 秋火 ──

    @Test fun `秋一 癸亥庚申丙午庚寅 首取寅木兼午火`() {
        val r = HuoQuYongCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.GENG, EarthlyBranch.YIN,
        ))
        assertEquals(GodCategory.YIN, r.yongShen)
        assertEquals(GodCategory.BI, r.fuYong)
        assertTrue("basis 应含寅木：${r.basis}", r.basis.contains("寅木"))
    }

    @Test fun `秋二 戊午庚申丙寅甲午 失时不弱用庚金喜申壬`() {
        val r = HuoQuYongCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.WU,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.WU,
        ))
        assertEquals(GodCategory.CAI, r.yongShen)
        assertTrue("basis 应含申：${r.basis}", r.basis.contains("申"))
    }

    @Test fun `秋三 戊申辛酉丙戌甲午 取火为用`() {
        val r = HuoQuYongCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.BING, EarthlyBranch.XU,
            HeavenlyStem.JIA, EarthlyBranch.WU,
        ))
        assertEquals(GodCategory.BI, r.yongShen)
    }

    @Test fun `秋四 丁亥戊申丙子癸巳 巳被申合一用终生木火`() {
        val r = HuoQuYongCalculator.analyze(chartOf(
            HeavenlyStem.DING, EarthlyBranch.HAI,
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.BING, EarthlyBranch.ZI,
            HeavenlyStem.GUI, EarthlyBranch.SI,
        ))
        assertEquals(GodCategory.YIN, r.yongShen)
        assertEquals(GodCategory.BI, r.fuYong)
        assertEquals(HuoSpecial.GE_HE_DAI_SHI, r.special)
    }

    @Test fun `秋五 甲寅甲戌丙寅乙未 木多火窒火金并用`() {
        val r = HuoQuYongCalculator.analyze(chartOf(
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.XU,
            HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.YI, EarthlyBranch.WEI,
        ))
        assertEquals(GodCategory.BI, r.yongShen)
        assertEquals(GodCategory.CAI, r.fuYong)
        assertEquals(HuoSpecial.MU_DU_HUO_ZHI, r.special)
    }

    // ── 冬火 ──

    @Test fun `冬一 庚申戊子丙辰癸巳 水局身弱赖巳根`() {
        val r = HuoQuYongCalculator.analyze(chartOf(
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.WU, EarthlyBranch.ZI,
            HeavenlyStem.BING, EarthlyBranch.CHEN,
            HeavenlyStem.GUI, EarthlyBranch.SI,
        ))
        assertEquals(cn.xulun.bazi.model.DayStrength.WEAK, r.strength)
        assertTrue("basis 应含巳：${r.basis}", r.basis.contains("巳"))
    }

    @Test fun `冬二 己酉丁丑丁亥乙巳 合局不化巳仍为根取乙木`() {
        val r = HuoQuYongCalculator.analyze(chartOf(
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.DING, EarthlyBranch.CHOU,
            HeavenlyStem.DING, EarthlyBranch.HAI,
            HeavenlyStem.YI, EarthlyBranch.SI,
        ))
        assertEquals(GodCategory.YIN, r.yongShen)
        assertEquals(GodCategory.BI, r.fuYong)
        assertTrue("basis 应含不化：${r.basis}", r.basis.contains("不化"))
    }
}
