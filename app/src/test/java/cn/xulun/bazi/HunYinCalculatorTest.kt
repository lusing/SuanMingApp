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

    @Test
    fun `例六 罗先生 1994甲戌 财透冲动配偶宫 红鸾同居`() {
        val r = analyze(
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.WU, EarthlyBranch.YIN,
            HeavenlyStem.GENG, EarthlyBranch.CHEN,
            HeavenlyStem.JI, EarthlyBranch.MAO,
        )
        val n = allYears(r).firstOrNull { it.year == 1994 }
        assertNotNull(n)
        assertTrue(n!!.triggers.any { it.contains("财星透") })
        assertTrue(n.triggers.any { it.contains("冲配偶宫") })
        assertTrue(n.kind.chinese.contains("婚"))
    }

    @Test
    fun `例七 某女士 夫星弱仅余气身旺欺夫 早年难成婚`() {
        val r = analyze(
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.XIN, EarthlyBranch.HAI,
            HeavenlyStem.GUI, EarthlyBranch.SI,
            female = true,
        )
        assertTrue(r.baseUnfav.any { it.text.contains("难成婚") })
    }

    @Test
    fun `例七 1993癸酉 桃花值年(年支申起)生配偶宫 喜事来临`() {
        val r = analyze(
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.XIN, EarthlyBranch.HAI,
            HeavenlyStem.GUI, EarthlyBranch.SI,
            female = true,
        )
        val n = allYears(r).firstOrNull { it.year == 1993 }
        assertNotNull(n)
        assertTrue(n!!.triggers.any { it.contains("桃花当值") })
        assertTrue(n.kind.chinese.contains("婚"))
    }

    @Test
    fun `例七 1994甲戌 申酉戌三会金局金多火熄身旺欺夫 婚变离异`() {
        val r = analyze(
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.XIN, EarthlyBranch.HAI,
            HeavenlyStem.GUI, EarthlyBranch.SI,
            female = true,
        )
        val n = allYears(r).firstOrNull { it.year == 1994 }
        assertNotNull(n)
        assertTrue(n!!.triggers.any { it.contains("三会") && it.contains("婚变离异") })
        assertTrue(n.kind.chinese.contains("动荡"))
    }

    @Test
    fun `例七 1990庚午 午亥暗合无星动 暗中追求竹篮打水`() {
        val r = analyze(
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.XIN, EarthlyBranch.HAI,
            HeavenlyStem.GUI, EarthlyBranch.SI,
            female = true,
        )
        val n = allYears(r).firstOrNull { it.year == 1990 }
        assertNotNull(n)
        assertTrue(n!!.triggers.any { it.contains("暗合") })
        assertTrue(n.kind.chinese.contains("恋爱"))
    }

    @Test
    fun `例八 周先生 1974与1975 婚恋信号`() {
        val r = analyze(
            HeavenlyStem.WU, EarthlyBranch.ZI,
            HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            fromYear = 1945, toYear = 1955,
        )
        listOf(1974, 1975).forEach { y ->
            val n = allYears(r).firstOrNull { it.year == y }
            assertNotNull("缺 $y", n)
            assertTrue("$y 应有婚恋信号:${n!!.kind.chinese}", n!!.kind != cn.xulun.bazi.model.HunLianKind.WU)
        }
    }

    @Test
    fun `例八 1977丁巳 财星当值冲动配偶宫 结婚之喜`() {
        val r = analyze(
            HeavenlyStem.WU, EarthlyBranch.ZI,
            HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            fromYear = 1945, toYear = 1955,
        )
        val n = allYears(r).firstOrNull { it.year == 1977 }
        assertNotNull(n)
        assertTrue(n!!.triggers.any { it.contains("财星当值") })
        assertTrue(n.kind.chinese.contains("婚"))
    }

    @Test
    fun `例八 1989己巳 冲宫过31岁反吟 离婚之忧`() {
        val r = analyze(
            HeavenlyStem.WU, EarthlyBranch.ZI,
            HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            fromYear = 1945, toYear = 1955,
        )
        val n = allYears(r).firstOrNull { it.year == 1989 }
        assertNotNull(n)
        assertTrue(n!!.kind.chinese.contains("动荡"))
    }

    @Test
    fun `例九 某先生 1987丁卯两卯争合两戌 介绍而难成`() {
        val r = analyze(
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.BING, EarthlyBranch.ZI,
            HeavenlyStem.JIA, EarthlyBranch.XU,
            HeavenlyStem.DING, EarthlyBranch.MAO,
        )
        val n = allYears(r).firstOrNull { it.year == 1987 }
        assertNotNull(n)
        assertTrue(n!!.kind.chinese.contains("恋爱"))
    }

    @Test
    fun `例九 1995乙亥 太岁生扶身弱 婚成`() {
        val r = analyze(
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.BING, EarthlyBranch.ZI,
            HeavenlyStem.JIA, EarthlyBranch.XU,
            HeavenlyStem.DING, EarthlyBranch.MAO,
        )
        val n = allYears(r).firstOrNull { it.year == 1995 }
        assertNotNull(n)
        assertTrue(n!!.kind.chinese.contains("婚"))
    }

    @Test
    fun `例十 某先生 1990庚午岁运桃花合宫 1994甲戌刑动财星宫 同居`() {
        val r = analyze(
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.YI, EarthlyBranch.WEI,
            HeavenlyStem.XIN, EarthlyBranch.SI,
        )
        val y90 = allYears(r).firstOrNull { it.year == 1990 }
        val y94 = allYears(r).firstOrNull { it.year == 1994 }
        assertNotNull(y90)
        assertNotNull(y94)
        assertTrue(y90!!.kind.chinese.contains("同居"))
        assertTrue(y94!!.kind.chinese.contains("同居"))
    }

    @Test
    fun `例十 2001辛巳 三巳争合一申 三角恋爱`() {
        val r = analyze(
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.YI, EarthlyBranch.WEI,
            HeavenlyStem.XIN, EarthlyBranch.SI,
        )
        val n = allYears(r).firstOrNull { it.year == 2001 }
        assertNotNull(n)
        assertTrue(n!!.triggers.any { it.contains("争合") })
    }

    @Test
    fun `例十一 赵女士 1996丙子桃花重逢财旺 婚恋之喜`() {
        val r = analyze(
            HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.JIA, EarthlyBranch.XU,
            HeavenlyStem.WU, EarthlyBranch.CHEN,
            HeavenlyStem.JI, EarthlyBranch.WEI,
            female = true,
            fromYear = 1975, toYear = 1985,
        )
        val n = allYears(r).firstOrNull { it.year == 1996 }
        assertNotNull(n)
        assertTrue(n!!.triggers.any { it.contains("桃花") })
        assertTrue(n.kind.chinese.contains("婚"))
    }

    @Test
    fun `例十二 吴先生 1999己卯 桃花值年三合生财星 喜事结婚`() {
        val r = analyze(
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.DING, EarthlyBranch.HAI,
            HeavenlyStem.JIA, EarthlyBranch.WU,
            HeavenlyStem.BING, EarthlyBranch.ZI,
        )
        val n = allYears(r).firstOrNull { it.year == 1999 }
        assertNotNull(n)
        assertTrue(n!!.triggers.any { it.contains("桃花") })
        assertTrue(n.kind.chinese.contains("婚"))
    }

    @Test
    fun `例十三 李先生 1981辛酉 妻宫值年 婚恋之喜`() {
        val r = analyze(
            HeavenlyStem.YI, EarthlyBranch.WEI,
            HeavenlyStem.BING, EarthlyBranch.XU,
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.JI, EarthlyBranch.SI,
            fromYear = 1950, toYear = 1960,
        )
        val n = allYears(r).firstOrNull { it.year == 1981 }
        assertNotNull(n)
        assertTrue(n!!.triggers.any { it.contains("加临") })
        assertTrue(n.kind.chinese.contains("婚"))
    }

    @Test
    fun `例十四 金先生 1989己巳 财星当值生配偶宫 婚恋`() {
        val r = analyze(
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.GUI, EarthlyBranch.WEI,
            HeavenlyStem.YI, EarthlyBranch.MAO,
        )
        val n = allYears(r).firstOrNull { it.year == 1989 }
        assertNotNull(n)
        assertTrue(n!!.triggers.any { it.contains("财星当值") })
        assertTrue(n.kind != cn.xulun.bazi.model.HunLianKind.WU)
    }

    @Test
    fun `例十五 付先生 2001辛巳 丙辛合财合日干 婚喜`() {
        val r = analyze(
            HeavenlyStem.WU, EarthlyBranch.WU,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.BING, EarthlyBranch.XU,
            HeavenlyStem.XIN, EarthlyBranch.MAO,
        )
        val n = allYears(r).firstOrNull { it.year == 2001 }
        assertNotNull(n)
        assertTrue(n!!.triggers.any { it.contains("合我") })
        assertTrue(n.kind.chinese.contains("婚"))
    }

    @Test
    fun `关系例一 余女士 宫干比和朋友 两杀夹克宫他男进入`() {
        val r = yuNvShi()
        assertTrue(r.baseItems.any { it.text.contains("犹如朋友") })
        assertTrue(r.baseUnfav.any { it.text.contains("乘虚而入") || it.text.contains("其他男人") })
    }

    @Test
    fun `关系例二 王女士 宫干比和 宫逢合离婚之基`() {
        val r = analyze(
            HeavenlyStem.WU, EarthlyBranch.XU,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.DING, EarthlyBranch.MAO,
            female = true,
        )
        assertTrue(r.baseItems.any { it.text.contains("犹如朋友") })
        assertTrue(r.palaceNote.contains("合"))
    }

    @Test
    fun `关系例三 黎先生 支克干配偶压制 1983冲宫结婚 1989-90恶化`() {
        val r = analyze(
            HeavenlyStem.JIA, EarthlyBranch.WU,
            HeavenlyStem.DING, EarthlyBranch.CHOU,
            HeavenlyStem.XIN, EarthlyBranch.SI,
            HeavenlyStem.JI, EarthlyBranch.CHOU,
            fromYear = 1950, toYear = 1960,
        )
        assertTrue(r.baseItems.any { it.text.contains("配偶压制我") })
        val y83 = allYears(r).firstOrNull { it.year == 1983 }
        assertNotNull(y83)
        assertTrue(y83!!.kind.chinese.contains("婚"))
        listOf(1989, 1990).forEach { y ->
            val n = allYears(r).firstOrNull { it.year == y }
            assertNotNull(n)
            assertTrue("$y 应关系恶化:${n!!.kind.chinese}", n.kind == cn.xulun.bazi.model.HunLianKind.BIAN_DONG)
        }
    }

    @Test
    fun `关系例四 科女士 支生干得丈夫之爱`() {
        val r = analyze(
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.BING, EarthlyBranch.ZI,
            HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.GUI, EarthlyBranch.SI,
            female = true,
        )
        assertTrue(r.baseItems.any { it.text.contains("配偶喜欢我") })
    }

    @Test
    fun `关系例五 夫支克干耙耳朵 妻支生干享夫福`() {
        val husband = analyze(
            HeavenlyStem.YI, EarthlyBranch.SI,
            HeavenlyStem.JI, EarthlyBranch.MAO,
            HeavenlyStem.JI, EarthlyBranch.MAO,
            HeavenlyStem.BING, EarthlyBranch.YIN,
        )
        assertTrue(husband.baseItems.any { it.text.contains("配偶压制我") })
        val wife = analyze(
            HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.XIN, EarthlyBranch.HAI,
            HeavenlyStem.XIN, EarthlyBranch.CHOU,
            HeavenlyStem.GUI, EarthlyBranch.SI,
            female = true,
        )
        assertTrue(wife.baseItems.any { it.text.contains("配偶喜欢我") })
    }
}
