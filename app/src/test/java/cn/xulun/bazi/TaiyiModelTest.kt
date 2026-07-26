package cn.xulun.bazi

import cn.xulun.bazi.model.SixteenGod
import cn.xulun.bazi.model.TaiyiPalace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaiyiModelTest {
    @Test
    fun `太乙九宫 差一宫 乾1离2艮3震4兑6坤7坎8巽9`() {
        assertEquals("乾", TaiyiPalace.of(1).trigram)
        assertEquals("离", TaiyiPalace.of(2).trigram)
        assertEquals("艮", TaiyiPalace.of(3).trigram)
        assertEquals("震", TaiyiPalace.of(4).trigram)
        assertEquals("兑", TaiyiPalace.of(6).trigram)
        assertEquals("坤", TaiyiPalace.of(7).trigram)
        assertEquals("坎", TaiyiPalace.of(8).trigram)
        assertEquals("巽", TaiyiPalace.of(9).trigram)
    }

    @Test
    fun `gridOrder 上南下北 与 ring 行宫序`() {
        assertEquals(listOf(9, 2, 7), TaiyiPalace.gridOrder[0])
        assertEquals(listOf(4, 5, 6), TaiyiPalace.gridOrder[1])
        assertEquals(listOf(3, 8, 1), TaiyiPalace.gridOrder[2])
        assertEquals(listOf(1, 2, 3, 4, 6, 7, 8, 9), TaiyiPalace.ring)
    }

    @Test
    fun `对宫冲 与 地内助主 天外助客`() {
        assertEquals(9, TaiyiPalace.opposite(1))   // 乾↔巽
        assertEquals(1, TaiyiPalace.opposite(9))
        assertEquals(8, TaiyiPalace.opposite(2))   // 离↔坎
        assertEquals(2, TaiyiPalace.opposite(8))
        assertEquals(7, TaiyiPalace.opposite(3))   // 艮↔坤
        assertEquals(6, TaiyiPalace.opposite(4))   // 震↔兑
        assertTrue(TaiyiPalace.isInner(1) && TaiyiPalace.isInner(8) && TaiyiPalace.isInner(3) && TaiyiPalace.isInner(4))
        assertTrue(!TaiyiPalace.isInner(2) && !TaiyiPalace.isInner(9) && !TaiyiPalace.isInner(6) && !TaiyiPalace.isInner(7))
    }

    @Test
    fun `十六神 正神带宫数 间辰无`() {
        // 正神宫数（差一宫）
        assertEquals(8, SixteenGod.at(0).palaceNo)   // 子=坎8
        assertEquals(3, SixteenGod.at(2).palaceNo)   // 艮=3
        assertEquals(4, SixteenGod.at(4).palaceNo)   // 卯=震4
        assertEquals(9, SixteenGod.at(6).palaceNo)   // 巽=9
        assertEquals(2, SixteenGod.at(8).palaceNo)   // 午=离2
        assertEquals(7, SixteenGod.at(10).palaceNo)  // 坤=7
        assertEquals(6, SixteenGod.at(12).palaceNo)  // 酉=兑6
        assertEquals(1, SixteenGod.at(14).palaceNo)  // 乾=1
        // 间辰无宫数
        assertNull(SixteenGod.at(15).palaceNo)       // 亥=间辰
        assertEquals("大义", SixteenGod.at(15).title)
        assertTrue(SixteenGod.at(0).isZheng)
        assertTrue(!SixteenGod.at(15).isZheng)
    }
}
