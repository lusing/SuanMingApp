package cn.xulun.bazi

import cn.xulun.bazi.core.CongGeYunCalculator
import cn.xulun.bazi.core.TeShuGeJuCalculator
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.TeShuKind
import cn.xulun.bazi.model.YunVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 金专旺（从革）格锚点。理论见 .shengke-reference.md 第五四节四。
 * 成格三例（普通档）+ 从革四例 + 行运总表（戊申 辛酉 庚戌 庚辰）——支归局/干合绊/化势转格诸细则。
 */
class CongGeGeTest {

    // ── (一)金专旺格成格（普通档） ──

    @Test fun `普通例一 戊申辛酉庚辰甲申 辰酉党强`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.SHEN, HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.GENG, EarthlyBranch.CHEN, HeavenlyStem.JIA, EarthlyBranch.SHEN))
        assertNotNull("甲木坐申截脚不破格：$r", r)
        assertEquals(TeShuKind.ZHUAN_WANG, r!!.kind)
        assertEquals("无会局——普通档", "金专旺格", r.name)
    }

    @Test fun `普通例二 庚申庚辰庚申庚辰 尽泄土气`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GENG, EarthlyBranch.SHEN, HeavenlyStem.GENG, EarthlyBranch.CHEN,
            HeavenlyStem.GENG, EarthlyBranch.SHEN, HeavenlyStem.GENG, EarthlyBranch.CHEN))
        assertEquals(TeShuKind.ZHUAN_WANG, r!!.kind)
        assertEquals("普通档", "金专旺格", r.name)
    }

    @Test fun `普通例三 辛酉辛丑庚申辛巳 巳三合向金`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.XIN, EarthlyBranch.YOU, HeavenlyStem.XIN, EarthlyBranch.CHOU,
            HeavenlyStem.GENG, EarthlyBranch.SHEN, HeavenlyStem.XIN, EarthlyBranch.SI))
        assertNotNull("巳火与年月三合情义向金：$r", r)
        assertEquals(TeShuKind.ZHUAN_WANG, r!!.kind)
        assertEquals("月令丑不在申酉戌——普通档", "金专旺格", r.name)
    }

    // ── (二)从革格 ──

    @Test fun `从革例一 戊申辛酉庚戌庚辰 申酉戌会辰酉合`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.SHEN, HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.GENG, EarthlyBranch.XU, HeavenlyStem.GENG, EarthlyBranch.CHEN))
        assertEquals("从革格（月令酉+申酉戌三会）", "从革格", r!!.name)
    }

    @Test fun `从革例二 庚辰乙酉辛巳己丑 三局同化`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GENG, EarthlyBranch.CHEN, HeavenlyStem.YI, EarthlyBranch.YOU,
            HeavenlyStem.XIN, EarthlyBranch.SI, HeavenlyStem.JI, EarthlyBranch.CHOU))
        assertNotNull("乙庚合化金不破格：$r", r)
        assertEquals("从革格（月令酉+巳酉丑三合）", "从革格", r!!.name)
    }

    @Test fun `从革例三 辛酉戊戌庚申乙酉 三会强金化土`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.XIN, EarthlyBranch.YOU, HeavenlyStem.WU, EarthlyBranch.XU,
            HeavenlyStem.GENG, EarthlyBranch.SHEN, HeavenlyStem.YI, EarthlyBranch.YOU))
        assertEquals("从革格（月令戌+申酉戌三会）", "从革格", r!!.name)
    }

    @Test fun `从革例四 庚申乙酉庚辰乙酉 乙庚辰酉皆合金`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GENG, EarthlyBranch.SHEN, HeavenlyStem.YI, EarthlyBranch.YOU,
            HeavenlyStem.GENG, EarthlyBranch.CHEN, HeavenlyStem.YI, EarthlyBranch.YOU))
        assertNotNull("两乙与日干庚五合化金贪合忘克：$r", r)
        assertEquals(TeShuKind.ZHUAN_WANG, r!!.kind)
        assertEquals("无会局而化气成格——从革格", "从革格", r.name)
    }

    // ── (三)行运喜忌总表（戊申 辛酉 庚戌 庚辰） ──

    private val chart = lazy {
        chartOf(
            HeavenlyStem.WU, EarthlyBranch.SHEN, HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.GENG, EarthlyBranch.XU, HeavenlyStem.GENG, EarthlyBranch.CHEN)
    }

    private fun vy(s: HeavenlyStem, b: EarthlyBranch): YunVerdict? =
        CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))?.verdict

    @Test fun `金行运1 比劫为用神（辛巳巳酉合金归局）`() {
        for ((s, b) in listOf(
            HeavenlyStem.GENG to EarthlyBranch.SHEN, HeavenlyStem.XIN to EarthlyBranch.YOU,
            HeavenlyStem.GENG to EarthlyBranch.CHEN, HeavenlyStem.GENG to EarthlyBranch.XU,
            HeavenlyStem.XIN to EarthlyBranch.CHOU, HeavenlyStem.XIN to EarthlyBranch.WEI,
            HeavenlyStem.XIN to EarthlyBranch.SI,
        )) assertEquals("(${s.chinese}${b.chinese}) 应用神", YunVerdict.YONG, vy(s, b))
    }

    @Test fun `金行运2 印绶为喜神（双体印严格喜，印比覆载通称）`() {
        for ((s, b) in listOf(
            HeavenlyStem.WU to EarthlyBranch.CHEN, HeavenlyStem.WU to EarthlyBranch.XU,
            HeavenlyStem.JI to EarthlyBranch.CHOU, HeavenlyStem.JI to EarthlyBranch.WEI,
        )) assertEquals("(${s.chinese}${b.chinese}) 双体印应喜", YunVerdict.XI, vy(s, b))
        for ((s, b) in listOf(
            HeavenlyStem.WU to EarthlyBranch.SHEN, HeavenlyStem.JI to EarthlyBranch.YOU,
        )) {
            val v = vy(s, b)
            assertNotNull(v)
            assertTrue("(${s.chinese}${b.chinese}) 印比覆载应吉档", v == YunVerdict.XI || v == YunVerdict.YONG)
        }
    }

    @Test fun `金行运3 官杀强旺转格`() {
        for ((s, b) in listOf(
            HeavenlyStem.BING to EarthlyBranch.WU, HeavenlyStem.DING to EarthlyBranch.SI,
            HeavenlyStem.JIA to EarthlyBranch.WU,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 应转格", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.FIRE, r.newYongShen)
        }
    }

    @Test fun `金行运4 乙巳化金归局不以官杀论`() {
        val v = vy(HeavenlyStem.YI, EarthlyBranch.SI)
        assertNotNull(v)
        assertTrue("乙巳：乙庚化金+巳酉合金应吉档（${v}）", v == YunVerdict.YONG || v == YunVerdict.XI)
    }

    @Test fun `金行运5 财星强旺转格`() {
        for ((s, b) in listOf(
            HeavenlyStem.JIA to EarthlyBranch.YIN, HeavenlyStem.YI to EarthlyBranch.MAO,
            HeavenlyStem.REN to EarthlyBranch.YIN, HeavenlyStem.GUI to EarthlyBranch.MAO,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 应转格", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.WOOD, r.newYongShen)
        }
    }

    @Test fun `金行运6 衰弱财星为忌`() {
        for ((s, b) in listOf(
            HeavenlyStem.WU to EarthlyBranch.YIN, HeavenlyStem.JI to EarthlyBranch.MAO,
        )) assertEquals("(${s.chinese}${b.chinese}) 应忌", YunVerdict.JI, vy(s, b))
    }

    @Test fun `金行运7 甲戌甲辰乙丑乙未助局不以忌论`() {
        for ((s, b) in listOf(
            HeavenlyStem.JIA to EarthlyBranch.XU, HeavenlyStem.JIA to EarthlyBranch.CHEN,
            HeavenlyStem.YI to EarthlyBranch.CHOU, HeavenlyStem.YI to EarthlyBranch.WEI,
        )) {
            val v = vy(s, b)
            assertNotNull(v)
            assertTrue("(${s.chinese}${b.chinese}) 合化助局应吉档（${v}）", v == YunVerdict.YONG || v == YunVerdict.XI)
        }
    }

    @Test fun `金行运8 食伤强旺泄秀`() {
        for ((s, b) in listOf(
            HeavenlyStem.REN to EarthlyBranch.ZI, HeavenlyStem.GUI to EarthlyBranch.HAI,
            HeavenlyStem.GENG to EarthlyBranch.ZI, HeavenlyStem.XIN to EarthlyBranch.HAI,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 应转格泄秀", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.WATER, r.newYongShen)
        }
    }

    @Test fun `金行运9 丙子化势转格泄秀为用`() {
        val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(HeavenlyStem.BING, EarthlyBranch.ZI)))
        assertNotNull(r)
        assertEquals("丙子：丙辛合水+子水泄秀——化势转格：${r!!.basis}", YunVerdict.ZHUAN_GE, r.verdict)
        assertEquals(Element.WATER, r.newYongShen)
    }

    @Test fun `金行运10 衰弱食伤为忌（癸巳除外）`() {
        for ((s, b) in listOf(
            HeavenlyStem.REN to EarthlyBranch.WU, HeavenlyStem.DING to EarthlyBranch.HAI,
        )) assertEquals("(${s.chinese}${b.chinese}) 应忌", YunVerdict.JI, vy(s, b))
        val v = vy(HeavenlyStem.GUI, EarthlyBranch.SI)
        assertNotNull(v)
        assertTrue("癸巳：戊癸合+巳酉合不以忌论（${v}）", v == YunVerdict.YONG || v == YunVerdict.XI)
    }
}
