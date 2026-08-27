package cn.xulun.bazi

import cn.xulun.bazi.core.DaYunCalculator
import cn.xulun.bazi.core.HunYinCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 婚姻完整引擎测试（教材例一锚定，.hunyin-reference.md）。
 *
 * 男财星/女官杀为配偶星，日支为配偶宫；婚恋流年=宫被引动（冲合刑暗合当值助）
 * 或星被引动（透干/当值）；桃花当值主婚庆。
 */
class HunYinCalculatorTest {

    private fun analyze(
        ys: HeavenlyStem, yb: EarthlyBranch,
        ms: HeavenlyStem, mb: EarthlyBranch,
        ds: HeavenlyStem, db: EarthlyBranch,
        hs: HeavenlyStem, hb: EarthlyBranch,
        female: Boolean = false,
        fromYear: Int = 1950,
        toYear: Int = 1980,
    ): cn.xulun.bazi.model.HunYinResult {
        val bazi = chartOf(ys, yb, ms, mb, ds, db, hs, hb, fromYear, toYear)
        val gender = if (female) Gender.FEMALE else Gender.MALE
        return HunYinCalculator.analyze(bazi, gender, DaYunCalculator.compute(bazi, gender))
    }

    private fun yuNvShi() = analyze(
        HeavenlyStem.JI, EarthlyBranch.YOU,
        HeavenlyStem.GENG, EarthlyBranch.WU,
        HeavenlyStem.GENG, EarthlyBranch.SHEN,
        HeavenlyStem.REN, EarthlyBranch.WU,
        female = true,
    )

    @Test
    fun `例一 1987丁卯 官星透加卯申暗合宫 恋爱年`() {
        val r = yuNvShi()
        val yun = r.yuns.firstOrNull { it.pillar.fullName == "壬申" }
        assertNotNull(yun)
        val n = yun!!.years.firstOrNull { it.year == 1987 }
        assertNotNull(n)
        assertTrue(n!!.triggers.any { it.contains("官星透") })
        assertTrue(n.triggers.any { it.contains("暗合") && it.contains("配偶宫") })
        assertTrue(n.kind.chinese.contains("恋爱"))
    }

    @Test
    fun `例一 1989己巳 巳合动配偶宫 恋爱年`() {
        val r = yuNvShi()
        val n = r.yuns.first { it.pillar.fullName == "壬申" }.years.firstOrNull { it.year == 1989 }
        assertNotNull(n)
        assertTrue(n!!.triggers.any { it.contains("合动") && it.contains("配偶宫") })
        assertTrue(n.kind.chinese.contains("恋爱"))
    }

    @Test
    fun `例一 1992壬申 流年加临配偶宫 实质婚同居`() {
        val r = yuNvShi()
        val n = r.yuns.first { it.pillar.fullName == "壬申" }.years.firstOrNull { it.year == 1992 }
        assertNotNull(n)
        assertTrue(n!!.triggers.any { it.contains("加临") && it.contains("配偶宫") })
        assertTrue(n.kind.chinese.contains("实质婚") || n.kind.chinese.contains("同居"))
    }

    @Test
    fun `例一 1993癸酉 桃花当值引动配偶宫 婚庆年`() {
        val r = yuNvShi()
        val n = r.yuns.first { it.pillar.fullName == "壬申" }.years.firstOrNull { it.year == 1993 }
        assertNotNull(n)
        assertTrue(n!!.triggers.any { it.contains("桃花") })
        assertTrue(n.kind.chinese.contains("婚"))
    }

    @Test
    fun `原局断语 女命日支比肩透官有婚可断 有基调节语`() {
        val r = yuNvShi()
        assertTrue(r.summary.isNotBlank())
        assertTrue(r.baseItems.isNotEmpty() || r.baseUnfav.isNotEmpty())
    }

    private fun allYears(r: cn.xulun.bazi.model.HunYinResult) = r.yuns.flatMap { it.years }

    @Test
    fun `例二 王女士 配偶星不现又伤官透 晚婚再婚之命`() {
        val r = analyze(
            HeavenlyStem.WU, EarthlyBranch.XU,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.DING, EarthlyBranch.MAO,
            female = true,
        )
        assertTrue(r.baseUnfav.any { it.text.contains("晚婚") })
    }

    @Test
    fun `例二 1986与1998寅加临配偶宫 实质婚姻`() {
        val r = analyze(
            HeavenlyStem.WU, EarthlyBranch.XU,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.DING, EarthlyBranch.MAO,
            female = true,
        )
        listOf(1986, 1998).forEach { y ->
            val n = allYears(r).firstOrNull { it.year == y }
            assertNotNull("缺 $y", n)
            assertTrue("$y 应为实质婚:${n!!.kind.chinese}", n.kind.chinese.contains("同居"))
        }
    }

    @Test
    fun `例二 1999己卯 桃花当值比助配偶宫 再婚婚礼`() {
        val r = analyze(
            HeavenlyStem.WU, EarthlyBranch.XU,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.DING, EarthlyBranch.MAO,
            female = true,
        )
        val n = allYears(r).firstOrNull { it.year == 1999 }
        assertNotNull(n)
        assertTrue(n!!.triggers.any { it.contains("桃花") })
        assertTrue(n.kind.chinese.contains("婚"))
    }

    @Test
    fun `例三 程先生 1986丙寅 财星当值冲动配偶宫届婚龄 结婚`() {
        val r = analyze(
            HeavenlyStem.JIA, EarthlyBranch.CHEN,
            HeavenlyStem.DING, EarthlyBranch.MAO,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.WU, EarthlyBranch.YIN,
        )
        val n = allYears(r).firstOrNull { it.year == 1986 }
        assertNotNull(n)
        assertTrue(n!!.triggers.any { it.contains("冲配偶宫") })
        assertTrue(n.triggers.any { it.contains("财星当值") })
        assertTrue(n.kind.chinese.contains("婚"))
    }

    @Test
    fun `例四 陈先生 1987卯合宫同居 1988辰冲宫婚礼`() {
        val r = analyze(
            HeavenlyStem.YI, EarthlyBranch.SI,
            HeavenlyStem.GUI, EarthlyBranch.WEI,
            HeavenlyStem.BING, EarthlyBranch.XU,
            HeavenlyStem.DING, EarthlyBranch.YOU,
        )
        val y87 = allYears(r).firstOrNull { it.year == 1987 }
        val y88 = allYears(r).firstOrNull { it.year == 1988 }
        assertNotNull(y87)
        assertNotNull(y88)
        assertTrue(y87!!.kind.chinese.contains("同居"))
        assertTrue(y88!!.kind.chinese.contains("婚"))
    }

    @Test
    fun `例五 石女士 1993癸酉 酉合动配偶宫辰 结婚`() {
        val r = analyze(
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.GENG, EarthlyBranch.CHEN,
            HeavenlyStem.GENG, EarthlyBranch.CHEN,
            female = true,
        )
        val n = allYears(r).firstOrNull { it.year == 1993 }
        assertNotNull(n)
        assertTrue(n!!.triggers.any { it.contains("合动") && it.contains("配偶宫") })
        assertTrue(n.kind.chinese.contains("同居") || n.kind.chinese.contains("婚"))
    }
}
