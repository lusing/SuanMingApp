package cn.xulun.bazi

import cn.xulun.bazi.core.LiuQinCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 六亲·父母断语测试（教材 12 例锚定，.liuqin-reference.md 第三节）。
 *
 * 父母星为主、父母宫（年柱）为辅：父星男偏财/女正财，母星男正印/女偏印。
 * 关键技法：真从（从象不伤反主有地位）、旬空、坐墓、衰不敌众、财印强弱比较谁先亡。
 */
class FuMuCalculatorTest {

    private fun fuMu(
        ys: HeavenlyStem, yb: EarthlyBranch,
        ms: HeavenlyStem, mb: EarthlyBranch,
        ds: HeavenlyStem, db: EarthlyBranch,
        hs: HeavenlyStem, hb: EarthlyBranch,
        female: Boolean = false,
    ) = LiuQinCalculator.analyze(
        chartOf(ys, yb, ms, mb, ds, db, hs, hb),
        if (female) Gender.FEMALE else Gender.MALE,
    ).fuMu

    @Test
    fun `例2 乙丑癸未丁巳丁未 父星真从火土 父有名望富有`() {
        val f = fuMu(
            HeavenlyStem.YI, EarthlyBranch.CHOU,
            HeavenlyStem.GUI, EarthlyBranch.WEI,
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.DING, EarthlyBranch.WEI,
        )
        assertTrue(f.favorable.any { it.dim.contains("父星") && it.dim.contains("从") && it.text.contains("名望") })
    }

    @Test
    fun `例4 癸丑壬戌癸巳辛酉 坤 父星弱于母星 父先逝倾向`() {
        val f = fuMu(
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.REN, EarthlyBranch.XU,
            HeavenlyStem.GUI, EarthlyBranch.SI,
            HeavenlyStem.XIN, EarthlyBranch.YOU,
            female = true,
        )
        assertTrue(f.unfavorable.any { it.dim.contains("父") && it.text.contains("先") })
    }

    @Test
    fun `例6 丙午庚寅丁酉庚子 父星临天乙有名望 年柱为父星官星有地位`() {
        val f = fuMu(
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.GENG, EarthlyBranch.YIN,
            HeavenlyStem.DING, EarthlyBranch.YOU,
            HeavenlyStem.GENG, EarthlyBranch.ZI,
        )
        assertTrue(f.favorable.any { it.dim.contains("天乙") && it.text.contains("名望") })
        assertTrue(f.favorable.any { it.dim.contains("官星") && it.text.contains("地位") })
    }

    @Test
    fun `例7 壬辰庚戌甲寅丙寅 财旺 父长寿`() {
        val f = fuMu(
            HeavenlyStem.REN, EarthlyBranch.CHEN,
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.BING, EarthlyBranch.YIN,
        )
        assertTrue(f.favorable.any { it.dim.contains("财旺") && it.text.contains("父") && it.text.contains("长寿") })
    }

    @Test
    fun `例8 甲寅乙亥丁巳丙午 印旺 母寿长`() {
        val f = fuMu(
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.YI, EarthlyBranch.HAI,
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.BING, EarthlyBranch.WU,
        )
        assertTrue(f.favorable.any { it.dim.contains("印旺") && it.text.contains("母") && it.text.contains("寿") })
    }

    @Test
    fun `例9 戊子甲子己丑甲子 年月天乙贵人 双亲行善积德`() {
        val f = fuMu(
            HeavenlyStem.WU, EarthlyBranch.ZI,
            HeavenlyStem.JIA, EarthlyBranch.ZI,
            HeavenlyStem.JI, EarthlyBranch.CHOU,
            HeavenlyStem.JIA, EarthlyBranch.ZI,
        )
        assertTrue(f.favorable.any { it.dim.contains("贵人") && it.text.contains("行善") })
    }

    @Test
    fun `例11 甲寅丁丑丙辰丙申 坤 年柱生合日干喜用 父母疼爱`() {
        val f = fuMu(
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.DING, EarthlyBranch.CHOU,
            HeavenlyStem.BING, EarthlyBranch.CHEN,
            HeavenlyStem.BING, EarthlyBranch.SHEN,
            female = true,
        )
        assertTrue(f.favorable.any { it.text.contains("疼爱") })
    }

    @Test
    fun `例12 庚午甲申辛丑己丑 父星坐绝遭克尽从旺不伤 印旺母寿`() {
        val f = fuMu(
            HeavenlyStem.GENG, EarthlyBranch.WU,
            HeavenlyStem.JIA, EarthlyBranch.SHEN,
            HeavenlyStem.XIN, EarthlyBranch.CHOU,
            HeavenlyStem.JI, EarthlyBranch.CHOU,
        )
        assertTrue(f.favorable.any { it.dim.contains("父星") && it.dim.contains("从") && it.text.contains("不伤") })
        assertTrue(f.favorable.any { it.dim.contains("印旺") && it.text.contains("母") })
    }

    @Test
    fun `例13 戊戌辛酉戊戌己未 财印均真从 父母高寿不克父`() {
        val f = fuMu(
            HeavenlyStem.WU, EarthlyBranch.XU,
            HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.WU, EarthlyBranch.XU,
            HeavenlyStem.JI, EarthlyBranch.WEI,
        )
        assertTrue(f.favorable.any { it.dim.contains("真从") && it.text.contains("高寿") })
        assertTrue(f.unfavorable.none { it.text.contains("亡父") })
    }

    @Test
    fun `例14 辛卯戊戌辛亥戊子 父星旬空遭克合 早岁克父`() {
        val f = fuMu(
            HeavenlyStem.XIN, EarthlyBranch.MAO,
            HeavenlyStem.WU, EarthlyBranch.XU,
            HeavenlyStem.XIN, EarthlyBranch.HAI,
            HeavenlyStem.WU, EarthlyBranch.ZI,
        )
        assertTrue(f.unfavorable.any { it.dim.contains("旬空") && it.text.contains("克父") })
    }

    @Test
    fun `例19 乙未己丑丙戌辛卯 母星透干坐墓 克母`() {
        val f = fuMu(
            HeavenlyStem.YI, EarthlyBranch.WEI,
            HeavenlyStem.JI, EarthlyBranch.CHOU,
            HeavenlyStem.BING, EarthlyBranch.XU,
            HeavenlyStem.XIN, EarthlyBranch.MAO,
        )
        assertTrue(f.unfavorable.any { it.dim.contains("墓") && it.text.contains("克母") })
    }

    @Test
    fun `例23 甲午庚午甲子戊辰 财强印弱遭冲 父寿长于母`() {
        val f = fuMu(
            HeavenlyStem.JIA, EarthlyBranch.WU,
            HeavenlyStem.GENG, EarthlyBranch.WU,
            HeavenlyStem.JIA, EarthlyBranch.ZI,
            HeavenlyStem.WU, EarthlyBranch.CHEN,
        )
        assertTrue(f.unfavorable.any { it.text.contains("父寿长于母") })
    }
@Test
    fun `例24 甲戌辛未戊午戊午 坤 母星从得不真为忌受母虐 父母离异随父`() {
        val f = fuMu(
            HeavenlyStem.JIA, EarthlyBranch.XU,
            HeavenlyStem.XIN, EarthlyBranch.WEI,
            HeavenlyStem.WU, EarthlyBranch.WU,
            HeavenlyStem.WU, EarthlyBranch.WU,
            female = true,
        )
        // 母星丙丁俱不透而火行有力(午午2.0)——从得不真+忌神：母缘薄之极受虐待
        assertTrue(f.unfavorable.any { it.dim.contains("从得不真") && it.text.contains("虐待") })
        // 从得不真则吉断不取（不再断"母星真从——父母康健高寿"）
        assertTrue(f.favorable.none { it.dim == "母星真从" })
        // 年柱甲戌相克+父星水从绝+印忌旺——父母离异随父
        assertTrue(f.unfavorable.any { it.dim.contains("离异") && it.dim.contains("随父") && it.text.contains("随父") })
    }
}