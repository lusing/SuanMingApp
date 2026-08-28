package cn.xulun.bazi

import cn.xulun.bazi.core.DaYunCalculator
import cn.xulun.bazi.core.YunNianCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 流年细断引擎测试（大运流年全面铺开，每流年带刑冲克害/三合三会/合化成否/喜忌/格局变化）。
 * 锚定例：林先生 丁未 丙午 戊申 乙卯（.caiyun-reference.md 例八，官运/财运测试共用）。
 */
class YunNianCalculatorTest {

    private fun analyze(
        ys: HeavenlyStem, yb: EarthlyBranch,
        ms: HeavenlyStem, mb: EarthlyBranch,
        ds: HeavenlyStem, db: EarthlyBranch,
        hs: HeavenlyStem, hb: EarthlyBranch,
        female: Boolean = false,
        fromYear: Int = 1950,
        toYear: Int = 1980,
    ): cn.xulun.bazi.model.YunNianResult {
        val bazi = chartOf(ys, yb, ms, mb, ds, db, hs, hb, fromYear, toYear)
        val gender = if (female) Gender.FEMALE else Gender.MALE
        return YunNianCalculator.analyze(bazi, DaYunCalculator.compute(bazi, gender))
    }

    private fun linXianSheng() = analyze(
        HeavenlyStem.DING, EarthlyBranch.WEI,
        HeavenlyStem.BING, EarthlyBranch.WU,
        HeavenlyStem.WU, EarthlyBranch.SHEN,
        HeavenlyStem.YI, EarthlyBranch.MAO,
    )

    private fun allYears(r: cn.xulun.bazi.model.YunNianResult) = r.yuns.flatMap { it.years }

    @Test
    fun `例八 林先生 1993癸酉 酉冲卯冲时支 癸克丙`() {
        val y = allYears(linXianSheng()).first { it.year == 1993 }
        assertEquals("癸酉", y.ganZhi)
        assertTrue(y.relations.any { it.contains("酉冲卯") && it.contains("时支") })
        assertTrue(y.relations.any { it.contains("癸克丙") && it.contains("月干") })
    }

    @Test
    fun `例八 林先生 1993癸酉 身强 岁干支皆喜`() {
        val y = allYears(linXianSheng()).first { it.year == 1993 }
        assertEquals("干喜支喜", y.xiJi)
    }

    @Test
    fun `例八 林先生 1993癸酉 干支十神标注`() {
        val y = allYears(linXianSheng()).first { it.year == 1993 }
        assertTrue(y.tenGods.contains("正财"))
        assertTrue(y.tenGods.contains("伤官"))
    }

    @Test
    fun `例八 林先生 1995乙亥 亥卯未三合木局化木成`() {
        val y = allYears(linXianSheng()).first { it.year == 1995 }
        assertTrue(y.relations.any { it.contains("亥卯未三合木局") && it.contains("化木成") })
    }

    @Test
    fun `例八 林先生 1989己巳 巳午未三会火局`() {
        val y = allYears(linXianSheng()).first { it.year == 1989 }
        assertTrue(y.relations.any { it.contains("巳午未三会火局") })
    }

    @Test
    fun `例八 林先生 1996丙子 子冲提纲午 格局动摇`() {
        val y = allYears(linXianSheng()).first { it.year == 1996 }
        assertTrue(y.relations.any { it.contains("子冲午") && it.contains("月支") })
        assertNotNull(y.geJu)
        assertTrue(y.geJu!!.contains("提纲"))
        assertTrue(y.geJu!!.contains("动摇"))
    }

    @Test
    fun `例八 林先生 1991辛未 未合绊提纲午`() {
        val y = allYears(linXianSheng()).first { it.year == 1991 }
        assertNotNull(y.geJu)
        assertTrue(y.geJu!!.contains("合绊提纲"))
    }

    @Test
    fun `喜忌口径含身强依据`() {
        val r = linXianSheng()
        assertTrue(r.xiJiBasis.contains("身强"))
    }

    @Test
    fun `岁支与原局同支不误报六合 1987丁卯 卯同卯`() {
        // 时支亦卯——卯见卯非六合（卯戌才是），且卯非自刑支，应无关系行
        val y = allYears(linXianSheng()).first { it.year == 1987 }
        assertTrue(y.relations.none { it.contains("卯合卯") })
        assertTrue(y.relations.any { it.contains("卯暗合申") && it.contains("日支") })
    }

    @Test
    fun `岁支与原局同支为自刑支时只报自刑 1990庚午 午同午`() {
        // 月支亦午——午午自刑；午未六合只对未（年支）成立，不得报"午合午"
        val y = allYears(linXianSheng()).first { it.year == 1990 }
        assertTrue(y.relations.any { it.contains("午刑午") && it.contains("自刑") })
        assertTrue(y.relations.none { it.contains("午合午") })
    }
}
