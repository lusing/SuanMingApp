package cn.xulun.bazi

import cn.xulun.bazi.core.CongGeYunCalculator
import cn.xulun.bazi.core.TeShuGeJuCalculator
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
 * 专旺格实例解析锚点。理论见 .shengke-reference.md 第五四节六。
 * 两行/三行成专旺、争合仍化、官杀合化向金、假专旺（孤弱财官隐患）与岁运助寡交战凶。
 */
class ZhuanWangShiLiTest {

    // ── 典型与多行成专旺 ──

    @Test fun `典型 癸亥四柱 润下`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.HAI, HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.GUI, EarthlyBranch.HAI, HeavenlyStem.GUI, EarthlyBranch.HAI))
        assertEquals(TeShuKind.ZHUAN_WANG, r!!.kind)
        // 书称润下（干支纯水）——无会局，引擎按普通档命名（与炎上例四一片火海同例）
        assertEquals("水专旺格", r.name)
    }

    @Test fun `两行成专旺 庚申庚辰庚申庚辰 金众土寡`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GENG, EarthlyBranch.SHEN, HeavenlyStem.GENG, EarthlyBranch.CHEN,
            HeavenlyStem.GENG, EarthlyBranch.SHEN, HeavenlyStem.GENG, EarthlyBranch.CHEN))
        assertEquals(TeShuKind.ZHUAN_WANG, r!!.kind)
    }

    @Test fun `三行成专旺 乙酉庚辰庚申庚辰 乙从庚化`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.YI, EarthlyBranch.YOU, HeavenlyStem.GENG, EarthlyBranch.CHEN,
            HeavenlyStem.GENG, EarthlyBranch.SHEN, HeavenlyStem.GENG, EarthlyBranch.CHEN))
        assertEquals("乙木从庚而化无损从革气势——仍专旺", TeShuKind.ZHUAN_WANG, r!!.kind)
        assertEquals("从革格", r.name)
    }

    @Test fun `三行成专旺 乙酉庚辰庚申乙酉`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.YI, EarthlyBranch.YOU, HeavenlyStem.GENG, EarthlyBranch.CHEN,
            HeavenlyStem.GENG, EarthlyBranch.SHEN, HeavenlyStem.YI, EarthlyBranch.YOU))
        assertEquals(TeShuKind.ZHUAN_WANG, r!!.kind)
    }

    @Test fun `争合仍化 乙酉乙酉庚辰乙酉 三乙合一庚`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.YI, EarthlyBranch.YOU, HeavenlyStem.YI, EarthlyBranch.YOU,
            HeavenlyStem.GENG, EarthlyBranch.CHEN, HeavenlyStem.YI, EarthlyBranch.YOU))
        assertNotNull("乙木全被截脚金势不可抗，争合仍化不破专旺：$r", r)
        assertEquals(TeShuKind.ZHUAN_WANG, r!!.kind)
        assertEquals("从革格", r.name)
    }

    @Test fun `官杀合化 庚申辛巳辛酉己丑 巳酉丑化金`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GENG, EarthlyBranch.SHEN, HeavenlyStem.XIN, EarthlyBranch.SI,
            HeavenlyStem.XIN, EarthlyBranch.YOU, HeavenlyStem.JI, EarthlyBranch.CHOU))
        assertNotNull("巳中丙火已与酉丑合化金反加强金势；己丑双体丑亦向金：$r", r)
        assertEquals(TeShuKind.ZHUAN_WANG, r!!.kind)
        assertEquals("月令巳不在申酉戌——普通档", "金专旺格", r.name)
    }

    // ── 假专旺（孤弱财官隐患） ──

    @Test fun `假专旺 候先生 壬子壬子壬午庚子 午火隐患`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.REN, EarthlyBranch.WU, HeavenlyStem.GENG, EarthlyBranch.ZI,
            fromYear = 1971, toYear = 1973))
        assertNotNull("水旺极火弱极，强众敌寡仍专旺：$r", r)
        assertEquals(TeShuKind.ZHUAN_WANG, r!!.kind)
        assertTrue("假专旺留隐患（fake）：${r.basis}", r.fake)
    }

    @Test fun `假专旺 戊申庚申庚戌丁丑 丁官无根隐患`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.SHEN, HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.GENG, EarthlyBranch.XU, HeavenlyStem.DING, EarthlyBranch.CHOU))
        assertNotNull("丁火官星极弱不能损日主——假专旺：$r", r)
        assertEquals(TeShuKind.ZHUAN_WANG, r!!.kind)
        assertTrue("官透无根隐患（fake）：${r.basis}", r.fake)
    }

    @Test fun `假专旺 赵先生 庚戌甲申庚申辛巳 甲巳皆弱`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GENG, EarthlyBranch.XU, HeavenlyStem.JIA, EarthlyBranch.SHEN,
            HeavenlyStem.GENG, EarthlyBranch.SHEN, HeavenlyStem.XIN, EarthlyBranch.SI,
            fromYear = 1969, toYear = 1971))
        assertNotNull("甲无根临绝被围克、巳失令遭申合克力甚微——假专旺：$r", r)
        assertEquals(TeShuKind.ZHUAN_WANG, r!!.kind)
        assertTrue("木火隐患（fake）：${r.basis}", r.fake)
    }

    // ── 岁运助寡交战凶 ──

    @Test fun `候先生 壬子运1978戊午 助午交战忌（药物中毒）`() {
        val b = chartOf(
            HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.REN, EarthlyBranch.WU, HeavenlyStem.GENG, EarthlyBranch.ZI,
            fromYear = 1971, toYear = 1973)
        // 比劫岁运顺旺为用
        assertEquals(YunVerdict.YONG, CongGeYunCalculator.analyze(b, listOf(Pillar(HeavenlyStem.REN, EarthlyBranch.ZI)))!!.verdict)
        // 原局午火得助由弱变强与金水交战——凶（火为财主饮食，肠道之厄）
        val r = CongGeYunCalculator.analyze(b, listOf(Pillar(HeavenlyStem.REN, EarthlyBranch.ZI), Pillar(HeavenlyStem.WU, EarthlyBranch.WU)))
        assertNotNull(r)
        assertEquals("戊午年助午火隐患与旺水交战——凶：${r!!.basis}", YunVerdict.JI, r.verdict)
    }

    @Test fun `丁丑例 岁运忌木火喜土金水`() {
        val b = chartOf(
            HeavenlyStem.WU, EarthlyBranch.SHEN, HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.GENG, EarthlyBranch.XU, HeavenlyStem.DING, EarthlyBranch.CHOU)
        assertEquals("庚申比劫——吉", YunVerdict.YONG,
            CongGeYunCalculator.analyze(b, listOf(Pillar(HeavenlyStem.GENG, EarthlyBranch.SHEN)))!!.verdict)
        assertEquals("丁巳助丁官隐患交战——忌", YunVerdict.JI,
            CongGeYunCalculator.analyze(b, listOf(Pillar(HeavenlyStem.DING, EarthlyBranch.SI)))!!.verdict)
    }

    @Test fun `赵先生 丙戌运甲戌年 透甲助隐患忌（丧父）`() {
        val b = chartOf(
            HeavenlyStem.GENG, EarthlyBranch.XU, HeavenlyStem.JIA, EarthlyBranch.SHEN,
            HeavenlyStem.GENG, EarthlyBranch.SHEN, HeavenlyStem.XIN, EarthlyBranch.SI,
            fromYear = 1969, toYear = 1971)
        // 丙有辛合不为大忌
        val yun = CongGeYunCalculator.analyze(b, listOf(Pillar(HeavenlyStem.BING, EarthlyBranch.XU)))
        assertNotNull(yun)
        assertTrue("丙戌运丙辛合绊——不为大忌：${yun!!.basis}", yun.verdict == YunVerdict.XI || yun.verdict == YunVerdict.YONG)
        // 岁干甲透帮扶月干甲，木得势叛金与旺金战——凶（甲偏财居父位，丧父）
        val r = CongGeYunCalculator.analyze(b, listOf(Pillar(HeavenlyStem.BING, EarthlyBranch.XU), Pillar(HeavenlyStem.JIA, EarthlyBranch.XU)))
        assertNotNull(r)
        assertEquals("甲戌年透甲助隐患——凶：${r!!.basis}", YunVerdict.JI, r.verdict)
    }
}
