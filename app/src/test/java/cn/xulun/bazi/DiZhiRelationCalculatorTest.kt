package cn.xulun.bazi

import cn.xulun.bazi.core.BaziCalculator
import cn.xulun.bazi.core.DiZhiRelationCalculator
import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.XingKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * 地支关系（三合 · 六冲 · 相刑）检测测试。
 *
 * 真实八字锚点与 HeHuaCalculatorTest 一致；另以 [baziOf] 构造特定地支组合覆盖
 * 三合、三刑、互刑、自刑等边界（干干统一取甲，仅用于地支逻辑测试，不保证干支合法配对）。
 */
class DiZhiRelationCalculatorTest {

    @Test
    fun `1985-05-20 丑未相冲`() {
        val bazi = BaziCalculator.calculate(LocalDateTime.of(1985, 5, 20, 0, 10))
        assertEquals("乙丑 辛巳 己未 甲子", bazi.fullBazi)
        val result = DiZhiRelationCalculator.analyze(bazi)

        assertEquals(1, result.chongList.size)
        assertEquals("丑未相冲", result.chongList[0].description)
        assertEquals("年支+日支", result.chongList[0].location)

        assertTrue(result.sanHeList.isEmpty())
        assertTrue(result.xingList.isEmpty())
        assertTrue(result.hasAny)
    }

    @Test
    fun `1990-01-01 子午相冲`() {
        val bazi = BaziCalculator.calculate(LocalDateTime.of(1990, 1, 1, 12, 0))
        assertEquals("己巳 丙子 丙寅 甲午", bazi.fullBazi)
        val result = DiZhiRelationCalculator.analyze(bazi)

        assertEquals(1, result.chongList.size)
        assertEquals("子午相冲", result.chongList[0].description)
        assertEquals("月支+时支", result.chongList[0].location)
        assertTrue(result.sanHeList.isEmpty())
        assertTrue(result.xingList.isEmpty())
    }

    @Test
    fun `1900-01-01 子午相冲 无三合相刑`() {
        val bazi = BaziCalculator.calculate(LocalDateTime.of(1900, 1, 1, 12, 0))
        assertEquals("己亥 丙子 甲戌 庚午", bazi.fullBazi)
        val result = DiZhiRelationCalculator.analyze(bazi)

        assertEquals(1, result.chongList.size)
        assertEquals("子午相冲", result.chongList[0].description)
        assertTrue(result.sanHeList.isEmpty())
        assertTrue(result.xingList.isEmpty())
    }

    @Test
    fun `2000-08-15 无地支关系`() {
        val bazi = BaziCalculator.calculate(LocalDateTime.of(2000, 8, 15, 9, 30))
        assertEquals("庚辰 甲申 乙巳 辛巳", bazi.fullBazi)
        val result = DiZhiRelationCalculator.analyze(bazi)

        assertTrue(result.chongList.isEmpty())
        assertTrue(result.sanHeList.isEmpty())
        assertTrue(result.xingList.isEmpty())
        assertFalse(result.hasAny)
    }

    @Test
    fun `申子辰三合水局`() {
        val result = DiZhiRelationCalculator.analyze(
            baziOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN, EarthlyBranch.HAI),
        )
        assertEquals(1, result.sanHeList.size)
        val sanHe = result.sanHeList[0]
        assertEquals("申子辰三合水局", sanHe.description)
        assertEquals(Element.WATER, sanHe.resultElement)
        assertEquals("年支+月支+日支", sanHe.location)

        assertTrue(result.chongList.isEmpty())
        assertTrue(result.xingList.isEmpty())
    }

    @Test
    fun `申子辰三合与辰戌六冲共存`() {
        val result = DiZhiRelationCalculator.analyze(
            baziOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN, EarthlyBranch.XU),
        )
        assertEquals(1, result.sanHeList.size)
        assertEquals("申子辰三合水局", result.sanHeList[0].description)

        assertEquals(1, result.chongList.size)
        assertEquals("辰戌相冲", result.chongList[0].description)
        assertEquals("日支+时支", result.chongList[0].location)
    }

    @Test
    fun `寅巳申三刑伴随寅申冲`() {
        val result = DiZhiRelationCalculator.analyze(
            baziOf(EarthlyBranch.YIN, EarthlyBranch.SI, EarthlyBranch.SHEN, EarthlyBranch.YOU),
        )
        assertEquals(1, result.xingList.size)
        val xing = result.xingList[0]
        assertEquals(XingKind.SAN_XING, xing.kind)
        assertEquals("寅巳申三刑（恃势之刑）", xing.description)
        assertEquals("年支+月支+日支", xing.location)

        // 寅申同在，故另成六冲
        assertTrue(result.chongList.any { it.description == "寅申相冲" })
    }

    @Test
    fun `丑戌未三刑伴随丑未冲`() {
        val result = DiZhiRelationCalculator.analyze(
            baziOf(EarthlyBranch.CHOU, EarthlyBranch.XU, EarthlyBranch.WEI, EarthlyBranch.ZI),
        )
        assertEquals(1, result.xingList.size)
        assertEquals("丑戌未三刑（无恩之刑）", result.xingList[0].description)
        assertEquals(XingKind.SAN_XING, result.xingList[0].kind)
        assertTrue(result.chongList.any { it.description == "丑未相冲" })
    }

    @Test
    fun `子卯互刑伴随子午冲`() {
        val result = DiZhiRelationCalculator.analyze(
            baziOf(EarthlyBranch.ZI, EarthlyBranch.MAO, EarthlyBranch.CHEN, EarthlyBranch.WU),
        )
        assertEquals(1, result.xingList.size)
        val xing = result.xingList[0]
        assertEquals(XingKind.HU_XING, xing.kind)
        assertEquals("子卯相刑（无礼之刑）", xing.description)
        assertEquals("年支+月支", xing.location)

        assertTrue(result.chongList.any { it.description == "子午相冲" })
    }

    @Test
    fun `辰自刑两见`() {
        val result = DiZhiRelationCalculator.analyze(
            baziOf(EarthlyBranch.CHEN, EarthlyBranch.CHEN, EarthlyBranch.WU, EarthlyBranch.YOU),
        )
        assertEquals(1, result.xingList.size)
        val xing = result.xingList[0]
        assertEquals(XingKind.ZI_XING, xing.kind)
        assertEquals("辰自刑（2见）", xing.description)
        assertEquals("年支+月支", xing.location)
        assertTrue(result.chongList.isEmpty())
        assertTrue(result.sanHeList.isEmpty())
    }

    /** 以四地支构造 BaziResult（天干统一取甲，仅用于地支关系测试，不保证干支合法配对） */
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
