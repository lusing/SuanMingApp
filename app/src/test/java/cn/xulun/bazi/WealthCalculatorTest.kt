package cn.xulun.bazi

import cn.xulun.bazi.core.BaziCalculator
import cn.xulun.bazi.core.DaYunCalculator
import cn.xulun.bazi.core.WealthCalculator
import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.TenGod
import cn.xulun.bazi.model.WealthPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * 财运分析器测试：财星辨识、富格、财库与开库、岁运财运。
 */
class WealthCalculatorTest {

    @Test
    fun `1985-05-20 己土日 财属水 身旺财旺 偏财藏支`() {
        val bazi = BaziCalculator.calculate(LocalDateTime.of(1985, 5, 20, 0, 10))
        assertEquals("乙丑 辛巳 己未 甲子", bazi.fullBazi)
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        val result = WealthCalculator.analyze(bazi, daYun)

        // 财五行：己土所克 = 水
        assertEquals(Element.WATER, result.wealthElement)

        // 财星：天干乙辛甲俱非财；藏干 丑(癸偏财)、子(癸偏财)
        assertEquals(2, result.stars.size)
        assertTrue(result.stars.all { it.tenGod == TenGod.PIAN_CAI })
        assertTrue(result.stars.any { it.location == "年支·中气" })  // 丑藏癸
        assertTrue(result.stars.any { it.location == "时支·本气" })  // 子本气癸
        assertTrue(result.stars.all { it.hidden })

        // 身强 + 财 2 见 → 身旺财旺
        assertEquals(WealthPattern.STRONG_WEALTH, result.pattern)

        // 财库辰，原局无辰
        assertEquals(EarthlyBranch.CHEN, result.storage.branch)
        assertFalse(result.storage.present)

        // 第一步大运庚辰带财（辰藏癸偏财）
        assertTrue(result.yunFortune.dayun.any { it.label == "庚辰大运" && it.desc == "偏财藏支" })
    }

    @Test
    fun `四土局 甲木日 财多身弱 财库辰逢辰戌冲开`() {
        // 年辰 月戌 日丑 时未；日主甲木，财属土
        val bazi = baziOf(EarthlyBranch.CHEN, EarthlyBranch.XU, EarthlyBranch.CHOU, EarthlyBranch.WEI)
        val result = WealthCalculator.analyze(bazi, null)

        assertEquals(Element.EARTH, result.wealthElement)

        // 藏干财星：辰戊(偏财)、戌戊(偏财)、丑己(正财)、未己(正财)
        assertEquals(4, result.stars.size)
        assertTrue(result.stars.any { it.tenGod == TenGod.PIAN_CAI && it.location == "年支·本气" })
        assertTrue(result.stars.any { it.tenGod == TenGod.ZHENG_CAI && it.location == "日支·本气" })

        // 身弱 + 财 4 见（月令戌亦为财）→ 财多身弱
        assertEquals(WealthPattern.WEAK_HEAVY_WEALTH, result.pattern)

        // 财库辰：原局年支辰，逢辰戌冲开
        assertEquals(EarthlyBranch.CHEN, result.storage.branch)
        assertTrue(result.storage.present)
        assertEquals("年支", result.storage.location)
        assertTrue(result.storage.opened)
        assertEquals("辰戌相冲", result.storage.openBy)

        // daYun=null → 岁运财运为空
        assertTrue(result.yunFortune.dayun.isEmpty())
        assertTrue(result.yunFortune.liunian.isEmpty())
    }

    /** 以四地支构造 BaziResult（天干统一取甲，仅用于财运逻辑测试，不保证干支合法配对） */
    private fun baziOf(vararg branches: EarthlyBranch): BaziResult {
        require(branches.size == 4) { "需提供四柱地支" }
        val (y, m, d, h) = branches.map { Pillar(HeavenlyStem.JIA, it) }
        return BaziResult(
            yearPillar = y,
            monthPillar = m,
            dayPillar = d,
            hourPillar = h,
            birthDateTime = LocalDateTime.of(2000, 1, 1, 12, 0),
        )
    }
}
