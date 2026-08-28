package cn.xulun.bazi

import cn.xulun.bazi.core.DaYunCalculator
import cn.xulun.bazi.core.GuanYunCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.GuanYunKind
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 官运引擎测试（教材七章实例锚定，.caiyun-reference.md）。
 * 官星为用力足=晋升；为忌力足=贬职；冲刑马星伤透=调动。
 */
class GuanYunCalculatorTest {

    private fun analyze(
        ys: HeavenlyStem, yb: EarthlyBranch,
        ms: HeavenlyStem, mb: EarthlyBranch,
        ds: HeavenlyStem, db: EarthlyBranch,
        hs: HeavenlyStem, hb: EarthlyBranch,
        female: Boolean = false,
        fromYear: Int = 1950,
        toYear: Int = 1980,
    ): cn.xulun.bazi.model.GuanYunResult {
        val bazi = chartOf(ys, yb, ms, mb, ds, db, hs, hb, fromYear, toYear)
        val gender = if (female) Gender.FEMALE else Gender.MALE
        return GuanYunCalculator.analyze(bazi, DaYunCalculator.compute(bazi, gender))
    }

    private fun allYears(r: cn.xulun.bazi.model.GuanYunResult) = r.yuns.flatMap { it.years }

    @Test
    fun `例八 林先生 癸卯运1993癸酉官冲引动 晋升`() {
        val r = analyze(
            HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.YI, EarthlyBranch.MAO,
        )
        val n = allYears(r).firstOrNull { it.year == 1993 }
        assertNotNull(n)
        assertTrue(n!!.kind == GuanYunKind.JIN_SHENG)
    }

    @Test
    fun `例八 林先生 乙酉2005官冲引动 晋升`() {
        val r = analyze(
            HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.YI, EarthlyBranch.MAO,
        )
        val n = allYears(r).firstOrNull { it.year == 2005 }
        assertNotNull(n)
        assertTrue(n!!.kind == GuanYunKind.JIN_SHENG)
    }

    @Test
    fun `例七 方先生 丙寅运甲戌1994官星强旺 升迁`() {
        val r = analyze(
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.JI, EarthlyBranch.SI,
            HeavenlyStem.GUI, EarthlyBranch.SI,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
        )
        val n = allYears(r).firstOrNull { it.year == 1994 }
        assertNotNull(n)
        assertTrue(n!!.kind == GuanYunKind.JIN_SHENG)
    }

    @Test
    fun `例六 王先生 1992壬申天克地冲三刑 免职`() {
        val r = analyze(
            HeavenlyStem.YI, EarthlyBranch.WEI,
            HeavenlyStem.XIN, EarthlyBranch.SI,
            HeavenlyStem.JIA, EarthlyBranch.WU,
            HeavenlyStem.WU, EarthlyBranch.CHEN,
        )
        val n = allYears(r).firstOrNull { it.year == 1992 }
        assertNotNull(n)
        assertTrue(n!!.kind == GuanYunKind.BIAN_ZHI)
    }

    @Test
    fun `例二十二 某先生 1996丙子官星当值身旺 升官`() {
        val r = analyze(
            HeavenlyStem.GENG, EarthlyBranch.ZI,
            HeavenlyStem.XIN, EarthlyBranch.SI,
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.JI, EarthlyBranch.YOU,
        )
        val n = allYears(r).firstOrNull { it.year == 1996 }
        assertNotNull(n)
        assertTrue(n!!.kind == GuanYunKind.JIN_SHENG)
    }

    @Test
    fun `例九 范女士 丁酉运乙亥1995伤官佩印 晋升`() {
        val r = analyze(
            HeavenlyStem.BING, EarthlyBranch.SHEN,
            HeavenlyStem.XIN, EarthlyBranch.CHOU,
            HeavenlyStem.GENG, EarthlyBranch.CHEN,
            HeavenlyStem.YI, EarthlyBranch.YOU,
            female = true,
        )
        val n = allYears(r).firstOrNull { it.year == 1995 }
        assertNotNull(n)
        assertTrue(n!!.kind == GuanYunKind.JIN_SHENG)
    }
}
