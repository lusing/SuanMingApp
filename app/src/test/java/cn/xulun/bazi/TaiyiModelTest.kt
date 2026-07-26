package cn.xulun.bazi

import cn.xulun.bazi.core.TaiyiCalculator
import cn.xulun.bazi.model.SixteenGod
import cn.xulun.bazi.model.TaiyiCountType
import cn.xulun.bazi.model.TaiyiFenYe
import cn.xulun.bazi.model.TaiyiPalace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

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

    @Test
    fun `卷八十二分野 宫州与三堂州`() {
        val expect = mapOf(
            1 to "冀", 2 to "荆", 3 to "青", 4 to "徐", 5 to "豫",
            6 to "雍", 7 to "梁", 8 to "兖", 9 to "扬",
        )
        expect.forEach { (no, zhou) -> assertEquals(zhou, TaiyiFenYe.palaceZhou[no]) }
        assertEquals(9, TaiyiFenYe.palaceZhou.size)
        assertEquals("交", TaiyiFenYe.tangZhou["绛宫"])
        assertEquals("益", TaiyiFenYe.tangZhou["明堂"])
        assertEquals("幽", TaiyiFenYe.tangZhou["玉堂"])
        assertEquals(listOf("绛宫", "明堂", "玉堂"), TaiyiFenYe.tangOrder)
    }

    @Test
    fun `markers 2024 四神6宫 天乙明堂 地乙2宫 直符绛宫 仅四神入图`() {
        val p = TaiyiCalculator.build(LocalDateTime.of(2024, 7, 1, 12, 0), TaiyiCountType.YEAR)
        val m = TaiyiFenYe.markers(p)
        assertEquals(listOf("四神"), m["6"]!!.map { it.name })
        assertEquals(listOf("天乙"), m["明堂"]!!.map { it.name })
        assertEquals(listOf("地乙"), m["2"]!!.map { it.name })
        assertEquals(listOf("直符"), m["绛宫"]!!.map { it.name })
        assertEquals(4, m.values.sumOf { it.size }) // 基星/五福/游神不入图
    }
}
