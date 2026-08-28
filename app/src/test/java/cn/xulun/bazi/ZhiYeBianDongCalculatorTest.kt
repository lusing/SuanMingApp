package cn.xulun.bazi

import cn.xulun.bazi.core.DaYunCalculator
import cn.xulun.bazi.core.ZhiYeBianDongCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 职业变动引擎测试。四标志：岁运刑冲合日柱官杀印星；临驿马；官杀值年克身重；
 * 食伤流年。卯酉冲为地域冲；岁运并临喜神+驿马重重→下海；冲旺财星财旺身弱→变动或病灾。
 */
class ZhiYeBianDongCalculatorTest {

    @Test
    fun `例一 周先生 戊子辛酉癸亥甲寅 1986丙寅岁运并临驿马 下海`() {
        val bazi = chartOf(
            HeavenlyStem.WU, EarthlyBranch.ZI,
            HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            1900, 1970,
        )
        assertEquals("戊子 辛酉 癸亥 甲寅", bazi.fullBazi)
        val r = ZhiYeBianDongCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        val y = r.years.firstOrNull { it.year == 1986 }
        assertNotNull(y)
        assertTrue(y!!.text.contains("下海") || y.triggers.any { t -> t.contains("驿马") })
    }

    @Test
    fun `例二 乾造 乙酉丁亥丁亥己酉 癸卯年卯酉地域冲 环境变化`() {
        val bazi = chartOf(
            HeavenlyStem.YI, EarthlyBranch.YOU,
            HeavenlyStem.DING, EarthlyBranch.HAI,
            HeavenlyStem.DING, EarthlyBranch.HAI,
            HeavenlyStem.JI, EarthlyBranch.YOU,
            1900, 1970,
        )
        assertEquals("乙酉 丁亥 丁亥 己酉", bazi.fullBazi)
        val r = ZhiYeBianDongCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        val y = r.years.firstOrNull { it.pillar.startsWith("癸卯") || it.year == 1963 }
        assertNotNull(y)
        assertTrue(y!!.text.contains("地域") || y.text.contains("环境"))
    }

    @Test
    fun `例六 乾造 丁酉癸卯丁亥辛丑 1982壬戌伤官迁居+1985乙丑天合地合`() {
        val bazi = chartOf(
            HeavenlyStem.DING, EarthlyBranch.YOU,
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.DING, EarthlyBranch.HAI,
            HeavenlyStem.XIN, EarthlyBranch.CHOU,
            1900, 1970,
        )
        assertEquals("丁酉 癸卯 丁亥 辛丑", bazi.fullBazi)
        val r = ZhiYeBianDongCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        // 庚子运亥子丑会水，水主流动；1982壬戌戌土伤官——迁居走动（移居香港）
        val y82 = r.years.firstOrNull { it.year == 1982 }
        assertNotNull(y82)
        assertTrue(y82!!.triggers.any { it.contains("食伤") || it.contains("伤官") })
        // 1985乙丑与庚子运天合地合——合也主动（驻京代表）
        val y85 = r.years.firstOrNull { it.year == 1985 }
        assertNotNull(y85)
        assertTrue(y85!!.triggers.any { it.contains("天合地合") })
    }

    @Test
    fun `例八 赵女士 己未甲戌戊辰己未 1998戊寅官杀得生临驿马 参加工作`() {
        val bazi = chartOf(
            HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.JIA, EarthlyBranch.XU,
            HeavenlyStem.WU, EarthlyBranch.CHEN,
            HeavenlyStem.JI, EarthlyBranch.WEI,
            1950, 2000,
        )
        assertEquals("己未 甲戌 戊辰 己未", bazi.fullBazi)
        val r = ZhiYeBianDongCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.FEMALE))
        // 丙子运戊寅年：岁支寅（官杀）得子水生，日支辰起驿马在寅——环境变化（毕业参加工作）
        val y = r.years.firstOrNull { it.year == 1998 }
        assertNotNull(y)
        assertTrue(y!!.triggers.any { it.contains("驿马") || it.contains("官杀") })
    }

    @Test
    fun `例七 乾造 戊申戊午丙辰丁酉 1986丙寅驿马值年 环境变化`() {
        val bazi = chartOf(
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.WU, EarthlyBranch.WU,
            HeavenlyStem.BING, EarthlyBranch.CHEN,
            HeavenlyStem.DING, EarthlyBranch.YOU,
            1950, 2000,
        )
        assertEquals("戊申 戊午 丙辰 丁酉", bazi.fullBazi)
        val r = ZhiYeBianDongCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        // 年支申起驿马在寅——1986丙寅驿马值年，环境变化（离开学校）
        val y = r.years.firstOrNull { it.year == 1986 }
        assertNotNull(y)
        assertTrue(y!!.triggers.any { it.contains("驿马") })
    }

    @Test
    fun `例五 乾造 癸卯丙辰壬寅己酉 戊寅年岁运临金舆 工作调动`() {
        val bazi = chartOf(
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.BING, EarthlyBranch.CHEN,
            HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.JI, EarthlyBranch.YOU,
            1900, 1970,
        )
        assertEquals("癸卯 丙辰 壬寅 己酉", bazi.fullBazi)
        val r = ZhiYeBianDongCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        // 癸丑运（丑为壬之金舆）流年戊寅（寅丑暗合引动金舆）——工作方面的调动
        val y = r.years.firstOrNull { it.year == 1998 }
        assertNotNull(y)
        assertTrue(y!!.triggers.any { it.contains("金舆") })
    }

    @Test
    fun `例四 乾造 甲申丁丑庚寅丙戌 1983癸亥伤官岁运逢冲 环境变化迁居`() {
        val bazi = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.SHEN,
            HeavenlyStem.DING, EarthlyBranch.CHOU,
            HeavenlyStem.GENG, EarthlyBranch.YIN,
            HeavenlyStem.BING, EarthlyBranch.XU,
            1900, 1970,
        )
        assertEquals("甲申 丁丑 庚寅 丙戌", bazi.fullBazi)
        val r = ZhiYeBianDongCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        // 辛巳运寅巳申三刑刑也主动；癸亥年伤官流年+岁运逢冲（亥冲巳）——工作环境变化（1983分房迁新居）
        val y = r.years.firstOrNull { it.year == 1983 }
        assertNotNull(y)
        assertTrue(y!!.text.contains("环境") || y.text.contains("变动"))
        assertTrue(y.triggers.any { it.contains("岁运逢冲") || it.contains("伤官") || it.contains("食伤") })
    }

    @Test
    fun `例三 乾造 甲辰甲戌乙卯丙戌 戊辰年冲旺财星 调走或病灾`() {
        val bazi = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.CHEN,
            HeavenlyStem.JIA, EarthlyBranch.XU,
            HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.BING, EarthlyBranch.XU,
            1900, 1970,
        )
        assertEquals("甲辰 甲戌 乙卯 丙戌", bazi.fullBazi)
        val r = ZhiYeBianDongCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        val y = r.years.firstOrNull { it.year == 1988 }
        assertNotNull(y)
        assertTrue(y!!.text.contains("财旺身弱") || y.text.contains("病灾"))
    }
}
