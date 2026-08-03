package cn.xulun.bazi

import cn.xulun.bazi.core.BaziCalculator
import cn.xulun.bazi.core.JieChongCalculator
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class JieChongCalculatorTest {
    private fun bazi(y: Int, m: Int, d: Int, h: Int, min: Int = 0) =
        BaziCalculator.calculate(LocalDateTime.of(y, m, d, h, min))

    // 锚点 A：丑未冲，丑合子解；1 六合，无未解
    @Test fun anchorA_chong_resolved_by_liuhe() {
        val r = JieChongCalculator.analyze(bazi(1985, 5, 20, 0, 10))
        assertEquals(1, r.chong.size)
        assertTrue(r.chong.first().resolved)
        assertEquals("丑六合（子）", r.chong.first().resolveBy)
        assertFalse(r.hasUnresolvedChong)
        assertEquals(1, r.liuHeCount)
        assertEquals(0, r.sanHeCount)
    }

    // 锚点 B：子午冲 + 午自刑，均无解
    @Test fun anchorB_chong_xing_unresolved() {
        val r = JieChongCalculator.analyze(bazi(1990, 12, 15, 12, 0))
        assertTrue(r.hasUnresolvedChong)
        assertTrue(r.hasUnresolvedXing)
        assertFalse(r.xing.first().resolved)
    }

    // 锚点 C：无冲刑
    @Test fun anchorC_no_relations() {
        val r = JieChongCalculator.analyze(bazi(1988, 7, 20, 10, 0))
        assertTrue(r.chong.isEmpty())
        assertTrue(r.xing.isEmpty())
        assertFalse(r.hasUnresolvedChong)
        assertFalse(r.hasUnresolvedXing)
    }

    // 锚点 D：卯酉冲，酉合辰解
    @Test fun anchorD_chong_resolved() {
        val r = JieChongCalculator.analyze(bazi(2000, 3, 4, 6, 0))
        assertEquals(1, r.chong.size)
        assertTrue(r.chong.first().resolved)
        assertEquals("酉六合（辰）", r.chong.first().resolveBy)
    }
}
