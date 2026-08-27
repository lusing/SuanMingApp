package cn.xulun.bazi

import cn.xulun.bazi.core.DaYunCalculator
import cn.xulun.bazi.core.HunYinCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.HunLianKind
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 婚姻·离婚/多婚/亡偶测试（教材第四节 24 例中静态可锚定者）。
 */
class LiHunTest {

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

    private fun allYears(r: cn.xulun.bazi.model.HunYinResult) = r.yuns.flatMap { it.years }

    @Test
    fun `例一 黄女士 宫被月未刑离婚隐患 2003未刑宫离婚 1999卯合宫外遇`() {
        val r = analyze(
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.XIN, EarthlyBranch.WEI,
            HeavenlyStem.WU, EarthlyBranch.XU,
            HeavenlyStem.DING, EarthlyBranch.SI,
            female = true,
        )
        assertTrue(r.baseUnfav.any { it.dim.contains("离婚隐患") })
        val y03 = allYears(r).firstOrNull { it.year == 2003 }
        assertNotNull(y03)
        assertTrue(y03!!.kind == HunLianKind.BIAN_DONG)
        val y99 = allYears(r).firstOrNull { it.year == 1999 }
        assertNotNull(y99)
        assertTrue(y99!!.triggers.any { it.contains("合动配偶宫") || it.contains("合配偶宫") })
    }

    @Test
    fun `例二 黎先生 宫巳半合争合 1994戌害宫离婚`() {
        val r = analyze(
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.JI, EarthlyBranch.SI,
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.JI, EarthlyBranch.SI,
        )
        val y94 = allYears(r).firstOrNull { it.year == 1994 }
        assertNotNull(y94)
        assertTrue(y94!!.triggers.any { it.contains("害配偶宫") })
        assertTrue(y94.kind == HunLianKind.BIAN_DONG)
    }

    @Test
    fun `例三 徐先生 1998戊寅冲申宫离婚`() {
        val r = analyze(
            HeavenlyStem.DING, EarthlyBranch.YOU,
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.JIA, EarthlyBranch.SHEN,
            HeavenlyStem.GUI, EarthlyBranch.YOU,
        )
        val y98 = allYears(r).firstOrNull { it.year == 1998 }
        assertNotNull(y98)
        assertTrue(y98!!.triggers.any { it.contains("冲配偶宫") })
        assertTrue(y98.kind == HunLianKind.BIAN_DONG)
    }

    @Test
    fun `例五 张先生 宫午被两子夹冲 1996三子冲午离婚`() {
        val r = analyze(
            HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.JIA, EarthlyBranch.WU,
            HeavenlyStem.JIA, EarthlyBranch.ZI,
        )
        assertTrue(r.baseUnfav.any { it.dim.contains("离婚隐患") })
        val y96 = allYears(r).firstOrNull { it.year == 1996 }
        assertNotNull(y96)
        assertTrue(y96!!.kind == HunLianKind.BIAN_DONG)
    }

    @Test
    fun `例十 姜小姐 1994天克地冲夫宫婚变`() {
        val r = analyze(
            HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.GENG, EarthlyBranch.CHEN,
            HeavenlyStem.BING, EarthlyBranch.XU,
            female = true,
        )
        val y94 = allYears(r).firstOrNull { it.year == 1994 }
        assertNotNull(y94)
        assertTrue(y94!!.triggers.any { it.contains("冲配偶宫") })
        assertTrue(y94.kind == HunLianKind.BIAN_DONG)
    }

    @Test
    fun `例十三 陈先生 辛卯两卯冲宫离婚 丁亥解冲结婚`() {
        val r = analyze(
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.YI, EarthlyBranch.YOU,
            HeavenlyStem.XIN, EarthlyBranch.SI,
        )
        val yMao = allYears(r).firstOrNull { it.year == 2011 }
        assertNotNull(yMao)
        assertTrue(yMao!!.triggers.any { it.contains("冲配偶宫") })
        assertTrue(yMao.kind == HunLianKind.BIAN_DONG)
    }

    @Test
    fun `例九 某女士 1994夫星入墓离婚`() {
        val r = analyze(
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.XIN, EarthlyBranch.HAI,
            HeavenlyStem.GUI, EarthlyBranch.SI,
            female = true,
        )
        val y94 = allYears(r).firstOrNull { it.year == 1994 }
        assertNotNull(y94)
        assertTrue(y94!!.kind == HunLianKind.BIAN_DONG)
    }

    @Test
    fun `例二十四 杨先生 克妻标志 1999宫星遭冲克妻亡`() {
        val r = analyze(
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.DING, EarthlyBranch.YOU,
            HeavenlyStem.JIA, EarthlyBranch.CHEN,
        )
        assertTrue(r.baseUnfav.any { it.dim.contains("克妻") })
        val y99 = allYears(r).firstOrNull { it.year == 1999 }
        assertNotNull(y99)
        assertTrue(y99!!.kind == HunLianKind.BIAN_DONG)
    }

    @Test
    fun `例十八 某先生 正偏财透藏有妻有妾`() {
        val r = analyze(
            HeavenlyStem.GENG, EarthlyBranch.ZI,
            HeavenlyStem.XIN, EarthlyBranch.SI,
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.JI, EarthlyBranch.YOU,
            fromYear = 1960, toYear = 1970,
        )
        assertTrue(r.baseItems.any { it.dim.contains("有妻有妾") })
    }

    @Test
    fun `例十五 王女士 官杀混杂多夫`() {
        val r = analyze(
            HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            female = true,
        )
        assertTrue(r.baseUnfav.any { it.dim.contains("多夫") })
    }
}
