package cn.xulun.bazi

import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Nayin
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.TenGod
import cn.xulun.bazi.model.branchHiddenStems
import cn.xulun.bazi.model.tenGodOfStem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 六十甲子数学（CRT）、纳音、藏干表、十神规则 */
class PillarMathTest {

    @Test
    fun cycleIndex_roundTrips_forAll60() {
        for (n in 0..59) {
            assertEquals(n, Pillar.fromCycleIndex(n).cycleIndex)
        }
    }

    @Test
    fun fromCycleIndex_knownNames() {
        assertEquals("甲子", Pillar.fromCycleIndex(0).fullName)
        assertEquals("甲戌", Pillar.fromCycleIndex(10).fullName)
        assertEquals("癸亥", Pillar.fromCycleIndex(59).fullName)
    }

    @Test
    fun cycleIndex_fromStemBranch() {
        assertEquals(0, Pillar(HeavenlyStem.JIA, EarthlyBranch.ZI).cycleIndex)
        assertEquals(10, Pillar(HeavenlyStem.JIA, EarthlyBranch.XU).cycleIndex)
        // 己未 = stem5 branch7 → 55
        assertEquals(55, Pillar(HeavenlyStem.JI, EarthlyBranch.WEI).cycleIndex)
    }

    @Test
    fun nayin_lookups() {
        assertEquals(30, Nayin.entries.size)
        assertEquals("海中金", Nayin.fromCycleIndex(0).chinese)
        assertEquals("海中金", Nayin.fromCycleIndex(1).chinese)
        assertEquals("大海水", Nayin.fromCycleIndex(58).chinese)
        assertEquals("大海水", Nayin.fromCycleIndex(59).chinese)
        // 庚午(6)/辛未(7) → 路旁土
        assertEquals("路旁土", Pillar.fromCycleIndex(6).nayin.chinese)
    }

    @Test
    fun branchHiddenStems_sizes() {
        assertEquals(1, branchHiddenStems.getValue(EarthlyBranch.ZI).size)
        assertEquals(3, branchHiddenStems.getValue(EarthlyBranch.CHOU).size)
        assertEquals(1, branchHiddenStems.getValue(EarthlyBranch.YOU).size)
        assertEquals(2, branchHiddenStems.getValue(EarthlyBranch.HAI).size)
        // 丑藏 己癸辛
        assertEquals(HeavenlyStem.JI, branchHiddenStems.getValue(EarthlyBranch.CHOU)[0])
    }

    @Test
    fun tenGod_rules() {
        val dm = HeavenlyStem.JI // 阴土
        // 甲阳木克己土，异阴 → 正官
        assertEquals(TenGod.ZHENG_GUAN, tenGodOfStem(dm, HeavenlyStem.JIA))
        // 乙阴木克己土，同阴 → 七杀
        assertEquals(TenGod.QI_SHA, tenGodOfStem(dm, HeavenlyStem.YI))
        // 辛阴金，土生金，同阴 → 食神
        assertEquals(TenGod.SHI_SHEN, tenGodOfStem(dm, HeavenlyStem.XIN))
        // 丙阳火生土，异 → 正印
        assertEquals(TenGod.ZHENG_YIN, tenGodOfStem(dm, HeavenlyStem.BING))
    }

    @Test
    fun element_generatesAndControls() {
        // 木生火、木克土
        assertEquals(cn.xulun.bazi.model.Element.FIRE, cn.xulun.bazi.model.Element.WOOD.generates)
        assertEquals(cn.xulun.bazi.model.Element.EARTH, cn.xulun.bazi.model.Element.WOOD.controls)
        assertTrue(true)
    }
}
