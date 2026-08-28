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
