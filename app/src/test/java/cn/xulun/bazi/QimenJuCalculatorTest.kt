package cn.xulun.bazi

import cn.xulun.bazi.core.QimenJuCalculator
import cn.xulun.bazi.model.Dun
import cn.xulun.bazi.model.JuMethod
import cn.xulun.bazi.model.Yuan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * 奇门定局测试。基准日 1900-01-01（基准日柱甲戌）：
 * 处于冬至(1899-12-22)与下年小寒(1900-01-06)之间 → 节气=冬至；
 * 符头甲戌（地支戌→下元）→ 冬至下元 = 阳遁四局。
 */
class QimenJuCalculatorTest {

    @Test
    fun `1900-01-01 甲戌日 冬至下元 阳遁四局 拆补`() {
        // 1900-01-01 = 甲戌（基准日）；冬至下元 = 1,7,4 → 4
        val ju = QimenJuCalculator.determineJu(LocalDateTime.of(1900, 1, 1, 12, 0), JuMethod.CHAIBU)
        assertEquals(Dun.YANG, ju.dun)
        assertEquals(4, ju.ju)
        assertEquals("冬至", ju.termName)
        assertEquals(Yuan.XIA, ju.yuan)
        assertEquals("甲戌", ju.fuTouName)
    }

    @Test
    fun `拆补 局数始终 1到9 阴阳遁与节气一致`() {
        // 随机抽样若干日期，校验结构正确性
        val dates = listOf(
            LocalDateTime.of(2024, 6, 21, 12, 0),  // 夏至附近 → 阴遁
            LocalDateTime.of(2024, 12, 22, 12, 0), // 冬至附近 → 阳遁
            LocalDateTime.of(2024, 3, 21, 12, 0),  // 春分 → 阳遁
            LocalDateTime.of(2024, 9, 23, 12, 0),  // 秋分 → 阴遁
        )
        dates.forEach { dt ->
            val ju = QimenJuCalculator.determineJu(dt, JuMethod.CHAIBU)
            assertTrue("局数范围 ${ju.ju}", ju.ju in 1..9)
            // 夏至(9)→冬至(21)区间为阴遁，冬至→夏至为阳遁
            val expectedDun = if (ju.termIndex in setOf(21, 22, 23, 0, 1, 2, 3, 4, 5, 6, 7, 8)) Dun.YANG else Dun.YIN
            assertEquals("节气 ${ju.termName} 阴阳遁", expectedDun, ju.dun)
        }
    }

    @Test
    fun `超神接气 结构正确且局数范围合法`() {
        val ju = QimenJuCalculator.determineJu(LocalDateTime.of(1900, 1, 1, 12, 0), JuMethod.CHAOJIE)
        assertTrue(ju.ju in 1..9)
        // 1900-01-01 处于冬至节气中段（非置闰窗口），超神接气亦应为阳遁
        assertEquals(Dun.YANG, ju.dun)
    }

    @Test
    fun `两法在节气中段结果一致`() {
        // 1900-01-01 远离大雪/芒种置闰窗口，拆补与超神接气应给出同一局
        val a = QimenJuCalculator.determineJu(LocalDateTime.of(1900, 1, 1, 12, 0), JuMethod.CHAIBU)
        val b = QimenJuCalculator.determineJu(LocalDateTime.of(1900, 1, 1, 12, 0), JuMethod.CHAOJIE)
        assertEquals(a.dun, b.dun)
        assertEquals(a.ju, b.ju)
    }
}
