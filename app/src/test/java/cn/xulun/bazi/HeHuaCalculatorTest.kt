package cn.xulun.bazi

import cn.xulun.bazi.core.BaziCalculator
import cn.xulun.bazi.core.HeHuaCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * 合化检测锚点测试，与现有 BaziCalculatorTest 使用相同八字。
 */
class HeHuaCalculatorTest {

    @Test
    fun `1985-05-20 甲己合土 子丑合土`() {
        val bazi = BaziCalculator.calculate(LocalDateTime.of(1985, 5, 20, 0, 10))
        assertEquals("乙丑 辛巳 己未 甲子", bazi.fullBazi)
        val result = HeHuaCalculator.analyze(bazi)

        // 天干：甲(时干) + 己(日干) → 甲己合化土
        assertEquals(1, result.stemHeHuaList.size)
        assertEquals("甲己合化土", result.stemHeHuaList[0].description)
        assertEquals("日干+时干", result.stemHeHuaList[0].location)

        // 地支：子(时支) + 丑(年支) → 子丑合化土
        assertEquals(1, result.branchLiuHeList.size)
        assertEquals("子丑合化土", result.branchLiuHeList[0].description)

        assertTrue(result.branchSanHuiList.isEmpty())
        assertTrue(result.hasAny)
    }

    @Test
    fun `1990-01-01 甲己合土 无地支合`() {
        val bazi = BaziCalculator.calculate(LocalDateTime.of(1990, 1, 1, 12, 0))
        assertEquals("己巳 丙子 丙寅 甲午", bazi.fullBazi)
        val result = HeHuaCalculator.analyze(bazi)

        // 天干：甲(时干) + 己(年干) → 甲己合化土
        assertEquals(1, result.stemHeHuaList.size)
        assertEquals("甲己合化土", result.stemHeHuaList[0].description)
        assertEquals("年干+时干", result.stemHeHuaList[0].location)

        // 地支：巳、子、寅、午 — 无六合
        assertTrue(result.branchLiuHeList.isEmpty())
        assertTrue(result.branchSanHuiList.isEmpty())
        assertTrue(result.hasAny)
    }

    @Test
    fun `2000-08-15 乙庚合金 巳申合水`() {
        val bazi = BaziCalculator.calculate(LocalDateTime.of(2000, 8, 15, 9, 30))
        assertEquals("庚辰 甲申 乙巳 辛巳", bazi.fullBazi)
        val result = HeHuaCalculator.analyze(bazi)

        // 天干：乙(日干) + 庚(年干) → 乙庚合化金
        assertEquals(1, result.stemHeHuaList.size)
        assertEquals("乙庚合化金", result.stemHeHuaList[0].description)
        assertEquals("年干+日干", result.stemHeHuaList[0].location)

        // 地支：巳(日支) + 申(月支) → 巳申合化水
        // 注意：时支也是巳，所以会有两个巳申合
        assertEquals(2, result.branchLiuHeList.size)
        assertTrue(result.branchLiuHeList.any { it.description == "巳申合化水" })
        assertTrue(result.branchLiuHeList.any { it.location.contains("月支") })

        assertTrue(result.branchSanHuiList.isEmpty())
        assertTrue(result.hasAny)
    }

    @Test
    fun `1900-01-01 甲己合土`() {
        val bazi = BaziCalculator.calculate(LocalDateTime.of(1900, 1, 1, 12, 0))
        assertEquals("己亥 丙子 甲戌 庚午", bazi.fullBazi)
        val result = HeHuaCalculator.analyze(bazi)

        // 天干：甲(日干) + 己(年干) → 甲己合化土
        assertEquals(1, result.stemHeHuaList.size)
        assertEquals("甲己合化土", result.stemHeHuaList[0].description)
        assertEquals("年干+日干", result.stemHeHuaList[0].location)

        // 地支：亥、子、戌、午 — 无合化
        assertTrue(result.branchLiuHeList.isEmpty())
        assertTrue(result.branchSanHuiList.isEmpty())
        assertTrue(result.hasAny)
    }

    @Test
    fun `1985-02-03 立春前 丁壬合木 子丑合土`() {
        val bazi = BaziCalculator.calculate(LocalDateTime.of(1985, 2, 3, 0, 10))
        assertEquals("甲子 丁丑 癸酉 壬子", bazi.fullBazi)
        val result = HeHuaCalculator.analyze(bazi)

        // 天干：甲(0), 丁(3), 癸(9), 壬(8) — 丁壬合化木 (月干+时干)
        assertEquals(1, result.stemHeHuaList.size)
        assertEquals("丁壬合化木", result.stemHeHuaList[0].description)

        // 地支：子(0), 丑(1), 酉(9), 子(0) — 子丑合（年支+月支，月支+时支）
        assertEquals(2, result.branchLiuHeList.size)
        assertTrue(result.branchLiuHeList.all { it.description == "子丑合化土" })

        assertTrue(result.branchSanHuiList.isEmpty())
    }

    @Test
    fun `hasAny and isEmpty work correctly`() {
        // 验证无合化八字也能正确返回空结果
        val bazi = BaziCalculator.calculate(LocalDateTime.of(2024, 2, 10, 12, 0))
        val result = HeHuaCalculator.analyze(bazi)
        // 方法不崩溃，返回有效结果
        assertFalse(result.toString().isEmpty())
    }
}