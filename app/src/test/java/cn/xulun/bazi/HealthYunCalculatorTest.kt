package cn.xulun.bazi

import cn.xulun.bazi.core.DaYunCalculator
import cn.xulun.bazi.core.HealthYunCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 健康·岁运应期测试（教材肝胆/脾胃/心脏/车祸诸例锚定）。
 */
class HealthYunCalculatorTest {

    private fun analyze(
        ys: HeavenlyStem, yb: EarthlyBranch,
        ms: HeavenlyStem, mb: EarthlyBranch,
        ds: HeavenlyStem, db: EarthlyBranch,
        hs: HeavenlyStem, hb: EarthlyBranch,
        female: Boolean = false,
        fromYear: Int = 1950,
        toYear: Int = 1980,
    ): cn.xulun.bazi.model.HealthYunResult {
        val bazi = chartOf(ys, yb, ms, mb, ds, db, hs, hb, fromYear, toYear)
        val gender = if (female) Gender.FEMALE else Gender.MALE
        return HealthYunCalculator.analyze(bazi, DaYunCalculator.compute(bazi, gender))
    }

    private fun allYears(r: cn.xulun.bazi.model.HealthYunResult) = r.yuns.flatMap { it.years }

    @Test
    fun `肝胆例一 林先生 甲辰运1985乙丑引动乙卯 肝病发`() {
        val r = analyze(
            HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.YI, EarthlyBranch.MAO,
        )
        val n = allYears(r).firstOrNull { it.year == 1985 }
        assertNotNull(n)
        assertTrue(n!!.triggers.any { it.contains("引动") })
        assertTrue(n.kind.chinese.contains("病发"))
    }

    @Test
    fun `肝胆例一 林先生 1983癸亥水生木 病情缓解`() {
        val r = analyze(
            HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.YI, EarthlyBranch.MAO,
        )
        val n = allYears(r).firstOrNull { it.year == 1983 }
        assertNotNull(n)
        assertTrue(n!!.kind.chinese.contains("缓解"))
    }

    @Test
    fun `脾胃例一 张女士 庚寅运1998戊寅木克土 胃病发`() {
        val r = analyze(
            HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.DING, EarthlyBranch.HAI,
            HeavenlyStem.GUI, EarthlyBranch.WEI,
            HeavenlyStem.WU, EarthlyBranch.WU,
            female = true,
        )
        val n = allYears(r).firstOrNull { it.year == 1998 }
        assertNotNull(n)
        assertTrue(n!!.kind.chinese.contains("病发"))
    }

    @Test
    fun `脾胃例一 张女士 2001辛巳火通关 胃病缓解`() {
        val r = analyze(
            HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.DING, EarthlyBranch.HAI,
            HeavenlyStem.GUI, EarthlyBranch.WEI,
            HeavenlyStem.WU, EarthlyBranch.WU,
            female = true,
        )
        val n = allYears(r).firstOrNull { it.year == 2001 }
        assertNotNull(n)
        assertTrue(n!!.kind.chinese.contains("缓解"))
    }

    @Test
    fun `心例一 胡先生 壬子运2002壬午火根被冲 心脏病发`() {
        val r = analyze(
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.JI, EarthlyBranch.WEI,
        )
        val n = allYears(r).firstOrNull { it.year == 2002 }
        assertNotNull(n)
        assertTrue(n!!.kind.chinese.contains("病发"))
    }

    @Test
    fun `伤灾例三 柏女士 戊子运2002壬午 车祸`() {
        val r = analyze(
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.REN, EarthlyBranch.CHEN,
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.DING, EarthlyBranch.CHOU,
            female = true,
        )
        val n = allYears(r).firstOrNull { it.year == 2002 }
        assertNotNull(n)
        assertTrue(n!!.kind.chinese.contains("车祸"))
    }

    @Test
    fun `伤灾例四 周先生 癸亥运1996丙子 车祸`() {
        val r = analyze(
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.REN, EarthlyBranch.WU,
            HeavenlyStem.BING, EarthlyBranch.WU,
        )
        val n = allYears(r).firstOrNull { it.year == 1996 }
        assertNotNull(n)
        assertTrue(n!!.kind.chinese.contains("车祸"))
    }

    @Test
    fun `伤灾例二 边先生 癸亥运1996丙子天干壬丙冲 车祸`() {
        val r = analyze(
            HeavenlyStem.WU, EarthlyBranch.XU,
            HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.GENG, EarthlyBranch.ZI,
        )
        val n = allYears(r).firstOrNull { it.year == 1996 }
        assertNotNull(n)
        assertTrue(n!!.kind.chinese.contains("车祸"))
    }
}
