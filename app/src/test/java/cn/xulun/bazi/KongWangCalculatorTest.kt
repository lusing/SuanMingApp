package cn.xulun.bazi

import cn.xulun.bazi.core.BaziCalculator
import cn.xulun.bazi.core.KongWangCalculator
import cn.xulun.bazi.model.EarthlyBranch
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class KongWangCalculatorTest {
    private fun bazi(y: Int, m: Int, d: Int, h: Int, min: Int = 0) =
        BaziCalculator.calculate(LocalDateTime.of(y, m, d, h, min))

    // 锚点 A: 1985-05-20 子时，日柱己未 cycleIndex=55 → 旬空 子丑；年支丑、时支子落空
    @Test fun anchorA_xunkong_and_marks() {
        val b = bazi(1985, 5, 20, 0, 10)
        assertEquals(EarthlyBranch.ZI to EarthlyBranch.CHOU, KongWangCalculator.xunKongBranches(b.dayPillar.cycleIndex))
        val r = KongWangCalculator.analyze(b)
        assertEquals(setOf(0, 3), r.marks.map { it.pillarIdx }.toSet()) // 年柱、时柱
        assertTrue(r.isKong(0))
        assertFalse(r.isKong(1))
        assertFalse(r.isKong(2))
        assertTrue(r.isKong(3))
    }

    // 锚点 C: 1988-07-20 巳时，日柱 cycleIndex=12 → 旬空 申酉；四支辰寅子巳均不落空
    @Test fun anchorC_no_marks() {
        val b = bazi(1988, 7, 20, 10, 0)
        assertEquals(EarthlyBranch.SHEN to EarthlyBranch.YOU, KongWangCalculator.xunKongBranches(b.dayPillar.cycleIndex))
        assertTrue(KongWangCalculator.analyze(b).marks.isEmpty())
    }
}
