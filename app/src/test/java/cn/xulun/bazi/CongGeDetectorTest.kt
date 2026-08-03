package cn.xulun.bazi

import cn.xulun.bazi.core.BaziCalculator
import cn.xulun.bazi.core.GeJuCalculator
import cn.xulun.bazi.model.CongGeKind
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class CongGeDetectorTest {
    private fun bazi(y: Int, m: Int, d: Int, h: Int, min: Int = 0) =
        BaziCalculator.calculate(LocalDateTime.of(y, m, d, h, min))

    // 锚点 A：正格八字，非从格
    @Test fun anchorA_not_cong() {
        val b = bazi(1985, 5, 20, 0, 10)
        assertEquals("乙丑 辛巳 己未 甲子", b.fullBazi)
        assertNull(GeJuCalculator.detectCongGe(b))
    }

    // 从强：庚子 丁亥 癸亥 壬子（xuanzong strengthScore=14 → CONG_STRONG）
    @Test fun cong_strong_match_xuanzong() {
        val b = bazi(1960, 12, 1, 0, 0)
        assertEquals("庚子 丁亥 癸亥 壬子", b.fullBazi)
        assertEquals(14.0, GeJuCalculator.strengthScore(b), 0.001)
        assertEquals(CongGeKind.CONG_STRONG, GeJuCalculator.detectCongGe(b)?.kind)
    }

    // 从弱：庚子 戊子 丁丑 庚子（xuanzong strengthScore=-8 → CONG_WEAK）
    @Test fun cong_weak_match_xuanzong() {
        val b = bazi(1960, 12, 15, 0, 0)
        assertEquals("庚子 戊子 丁丑 庚子", b.fullBazi)
        assertEquals(-8.0, GeJuCalculator.strengthScore(b), 0.001)
        assertEquals(CongGeKind.CONG_WEAK, GeJuCalculator.detectCongGe(b)?.kind)
    }
}
