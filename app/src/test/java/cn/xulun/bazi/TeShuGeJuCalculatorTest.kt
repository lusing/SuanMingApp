package cn.xulun.bazi

import cn.xulun.bazi.core.DaYunCalculator
import cn.xulun.bazi.core.TeShuGeJuCalculator
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.TeShuKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 特殊格局取用锚点测试。9 例出自 .shengke-reference.md 第四九节（单从2/多从1/战局1/杂从1/专旺1/成象2+岁运还原），
 * 均真实日期反推（缪先生按 1952 窗口以合书载丙午运壬申年）。
 */
class TeShuGeJuCalculatorTest {

    // ── 单从 ──

    @Test fun `假从杀 吴小姐 戊寅乙卯己卯甲子 从木`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.YIN,
            HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.JI, EarthlyBranch.MAO,
            HeavenlyStem.JIA, EarthlyBranch.ZI,
        ))!!
        assertEquals(TeShuKind.JIA_CONG, r.kind)
        assertTrue("应标假从：${r.basis}", r.fake)
        assertEquals(listOf(Element.WOOD), r.yongShen)
        assertTrue("名应含从杀：${r.name}", r.name.contains("从杀"))
    }

    @Test fun `从格已正 李先生深圳 庚戌乙酉丙申己丑 保持从财食伤`() {
        // 教材：乙被庚合化金、戌根被金局抑制——从得较纯（非假）
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.YI, EarthlyBranch.YOU,
            HeavenlyStem.BING, EarthlyBranch.SHEN,
            HeavenlyStem.JI, EarthlyBranch.CHOU,
        ))!!
        assertTrue("kind 应为从类：${r.kind}", r.kind == TeShuKind.JIA_CONG || r.kind == TeShuKind.DUO_CONG)
        assertTrue("用神应含金：${r.yongShen}", Element.METAL in r.yongShen)
    }

    // ── 多从 ──

    @Test fun `多从链 李先生湖南 乙巳己卯壬戌辛丑 从木火土`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.YI, EarthlyBranch.SI,
            HeavenlyStem.JI, EarthlyBranch.MAO,
            HeavenlyStem.REN, EarthlyBranch.XU,
            HeavenlyStem.XIN, EarthlyBranch.CHOU,
        ))!!
        assertTrue("kind 应为从类：${r.kind}", r.kind == TeShuKind.DUO_CONG || r.kind == TeShuKind.JIA_CONG)
        assertEquals(listOf(Element.WOOD, Element.FIRE, Element.EARTH), r.yongShen)
    }

    // ── 战局 ──

    @Test fun `战局通关 王先生万州 丙寅辛卯壬申己酉 金木交战取水`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.XIN, EarthlyBranch.MAO,
            HeavenlyStem.REN, EarthlyBranch.SHEN,
            HeavenlyStem.JI, EarthlyBranch.YOU,
        ))!!
        assertEquals(TeShuKind.ZHAN_JU, r.kind)
        assertEquals(listOf(Element.WATER), r.yongShen)
        assertTrue("basis 应含通关：${r.basis}", r.basis.contains("通关"))
    }

    // ── 杂从+还原 ──

    @Test fun `杂从 缪先生广西 壬辰壬寅庚子辛巳 从水木火`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.REN, EarthlyBranch.CHEN,
            HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.GENG, EarthlyBranch.ZI,
            HeavenlyStem.XIN, EarthlyBranch.SI,
            fromYear = 1952, toYear = 1953,
        ))!!
        assertEquals(TeShuKind.JIA_CONG, r.kind)
        assertTrue(r.fake)
        assertEquals(listOf(Element.WATER, Element.WOOD, Element.FIRE), r.yongShen)
    }

    @Test fun `杂从还原 缪先生 丙午运壬申年 比劫得禄还原正格`() {
        // 书载：丙午大运壬申流年，日主得禄还原为身弱正格
        val b = chartOf(
            HeavenlyStem.REN, EarthlyBranch.CHEN,
            HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.GENG, EarthlyBranch.ZI,
            HeavenlyStem.XIN, EarthlyBranch.SI,
            fromYear = 1952, toYear = 1953,
        )
        val info = DaYunCalculator.compute(b, Gender.MALE) // 壬年阳男顺排
        assertEquals(true, info.forward)
        val wuYun = info.yuns.firstOrNull { 1992 >= it.startYear && 1992 <= it.endYear }
        assertNotNull("1992 应在丙午运：${info.yuns.joinToString { it.pillar.fullName + it.startYear }}", wuYun)
        assertEquals("丙午", wuYun!!.pillar.fullName)
        val restored = TeShuGeJuCalculator.analyze(b, listOf(wuYun.pillar, Pillar(HeavenlyStem.REN, EarthlyBranch.SHEN)))
        assertTrue("岁运比劫得禄应还原正格（返回 null 落回病药）：restored=$restored", restored == null)
    }

    // ── 专旺 ──

    @Test fun `专旺变节 黄先生四川 戊申辛酉辛巳己丑 从革`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.XIN, EarthlyBranch.SI,
            HeavenlyStem.JI, EarthlyBranch.CHOU,
        ))!!
        assertEquals(TeShuKind.ZHUAN_WANG, r.kind)
        assertTrue("名应含从革：${r.name}", r.name.contains("从革"))
        // 用神首选比印：金土
        assertEquals(Element.METAL, r.yongShen.first())
        assertTrue(Element.EARTH in r.yongShen)
    }

    // ── 成象 ──

    @Test fun `三气成象 陈小姐云南 戊午丙辰丙寅甲午 木火土补木`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.WU,
            HeavenlyStem.BING, EarthlyBranch.CHEN,
            HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.WU,
        ))!!
        assertEquals(TeShuKind.SAN_QI, r.kind)
        assertEquals(setOf(Element.WOOD, Element.FIRE, Element.EARTH), r.yongShen.toSet())
        assertEquals(Element.WOOD, r.yongShen.first()) // 补弱者=木
    }

    @Test fun `三气成象 李先生潮州 壬子庚戌辛巳壬辰 土金水补弱`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.XIN, EarthlyBranch.SI,
            HeavenlyStem.REN, EarthlyBranch.CHEN,
        ))!!
        assertEquals(TeShuKind.SAN_QI, r.kind)
        assertEquals(setOf(Element.EARTH, Element.METAL, Element.WATER), r.yongShen.toSet())
        assertTrue("首选应为金或水（弱者）：${r.yongShen}", r.yongShen.first() == Element.METAL || r.yongShen.first() == Element.WATER)
    }

    // ── 引擎三规则回归锚（正格侧受益） ──

    @Test fun `水浊 王先生原局 引擎应判弱`() {
        val e = cn.xulun.bazi.core.StrengthEngine.analyze(chartOf(
            HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.XIN, EarthlyBranch.MAO,
            HeavenlyStem.REN, EarthlyBranch.SHEN,
            HeavenlyStem.JI, EarthlyBranch.YOU,
        ))
        assertEquals(DayStrength.WEAK, e.verdict)
    }
}
