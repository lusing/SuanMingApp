package cn.xulun.bazi

import cn.xulun.bazi.core.BaziCalculator
import cn.xulun.bazi.core.WuXingFlowCalculator
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class WuXingFlowCalculatorTest {
    private fun bazi(y: Int, m: Int, d: Int, h: Int, min: Int = 0) =
        BaziCalculator.calculate(LocalDateTime.of(y, m, d, h, min))

    // 锚点 A：链1，无战，利（身强），无断语
    @Test fun anchorA_chain1_no_war() {
        val r = WuXingFlowCalculator.analyze(bazi(1985, 5, 20, 0, 10))
        assertEquals(1, r.chain)
        assertArrayEquals(intArrayOf(2, 1, 3, 1, 1), r.elems)
        assertFalse(r.war.jinMu)
        assertFalse(r.war.shuiHuo)
        assertTrue(r.favorable)
        assertTrue(r.duanYu.isEmpty())
    }

    // 锚点 B：水火交战，不利
    @Test fun anchorB_shuihuo_war_unfavor() {
        val r = WuXingFlowCalculator.analyze(bazi(1990, 12, 15, 12, 0))
        assertEquals(2, r.chain)
        assertTrue(r.war.shuiHuo)
        assertFalse(r.war.jinMu)
        assertFalse(r.favorable)
        assertEquals("shui_huo_zhan", r.duanYu.first().id)
    }

    // 锚点 D：金木交战，利（印旺）
    @Test fun anchorD_jinmu_war() {
        val r = WuXingFlowCalculator.analyze(bazi(2000, 3, 4, 6, 0))
        assertEquals(2, r.chain)
        assertTrue(r.war.jinMu)
        assertFalse(r.war.shuiHuo)
        assertTrue(r.favorable)
        assertEquals("jin_mu_zhan", r.duanYu.first().id)
    }
}
