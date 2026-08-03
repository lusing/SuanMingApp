package cn.xulun.bazi

import cn.xulun.bazi.core.BaziCalculator
import cn.xulun.bazi.core.TiaoHouCalculator
import cn.xulun.bazi.model.Climate
import cn.xulun.bazi.model.Element
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class TiaoHouCalculatorTest {
    private fun bazi(y: Int, m: Int, d: Int, h: Int, min: Int = 0) =
        BaziCalculator.calculate(LocalDateTime.of(y, m, d, h, min))

    // 锚点 A：巳月夏 → 燥，需水，柱中亥子支得调候，不急
    @Test fun anchorA_summer_zao_needs_water_present() {
        val r = TiaoHouCalculator.analyze(bazi(1985, 5, 20, 0, 10))
        assertEquals(Climate.ZAO, r.climate)
        assertEquals("生于夏月（巳）火当令、火炎土燥", r.climateBasis)
        assertEquals(Element.WATER, r.tiaoHouElement)
        assertTrue(r.tiaoHouPresent)
        assertFalse(r.urgent)
        assertEquals("燥需水调候，柱中亥子支得调候之神，气候得宜", r.tiaoHouNote)
    }

    // 锚点 B：子月冬 → 寒，需火（午支在柱），冬木透官难贵得失注
    @Test fun anchorB_winter_han_needs_fire_with_deshi() {
        val r = TiaoHouCalculator.analyze(bazi(1990, 12, 15, 12, 0))
        assertEquals(Climate.HAN, r.climate)
        assertEquals(Element.FIRE, r.tiaoHouElement)
        assertTrue(r.tiaoHouPresent)
        assertEquals(1, r.deShi.size)
        assertTrue(r.deShi.first().contains("冬木透官难贵"))
    }

    // 锚点 D：寅月春 → 温和，无调候
    @Test fun anchorD_spring_wenhe_no_tiaohou() {
        val r = TiaoHouCalculator.analyze(bazi(2000, 3, 4, 6, 0))
        assertEquals(Climate.WEN_HE, r.climate)
        assertNull(r.tiaoHouElement)
        assertFalse(r.tiaoHouPresent)
    }
}
