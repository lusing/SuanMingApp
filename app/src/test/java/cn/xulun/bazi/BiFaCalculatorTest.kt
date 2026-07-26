package cn.xulun.bazi

import cn.xulun.bazi.core.BiFaCalculator
import cn.xulun.bazi.core.LiuRenCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class BiFaCalculatorTest {

    @Test
    fun `analyze 真实日期不抛异常且非空`() {
        val plate = LiuRenCalculator.buildPlate(LocalDateTime.of(2026, 7, 1, 12, 0))
        val hits = BiFaCalculator.analyze(plate)
        assertTrue("应产出命中条目：$hits", hits.isNotEmpty())
        // 法96（旬空）与 93/97/98（警示提示）恒出
        assertTrue(hits.any { it.no == 96 })
        assertTrue(hits.any { it.no == 93 && it.note })
    }

    @Test
    fun `法7 旺禄临身 乙日干上卯为禄`() {
        // 乙寄辰，月将=子、占时=丑 → offset=11，干上神=heavenAt(辰)=卯=乙禄
        val plate = LiuRenCalculator.compute(
            HeavenlyStem.YI, EarthlyBranch.CHOU, EarthlyBranch.CHOU, EarthlyBranch.ZI, hourCycleIndex = 0,
        )
        val ganShang = plate.lesson(1).top
        assertTrue("干上应为卯：$ganShang", ganShang == EarthlyBranch.MAO)
        val hits = BiFaCalculator.analyze(plate)
        assertTrue("应命中法7旺禄临身：${hits.filter { it.no == 7 }}", hits.any { it.no == 7 })
    }

    @Test
    fun `法21 交车相合 可构造命中`() {
        // 干上神合支、支上神合干。遍历六十甲子日柱 × 12 月将（占时取子，覆盖各 offset）找一个命中例
        val found = (0..59).flatMap { cyc -> (0..11).map { cyc to it } }.any { (cyc, yj) ->
            val pillar = cn.xulun.bazi.model.Pillar.fromCycleIndex(cyc)
            val plate = LiuRenCalculator.compute(
                pillar.stem, pillar.branch, EarthlyBranch.ZI, EarthlyBranch.entries[yj], hourCycleIndex = 0,
            )
            val i = cn.xulun.bazi.core.BiFaInput(plate)
            cn.xulun.bazi.core.LiuRenTables.liuHe(i.ganShang, i.dayBranch) &&
                cn.xulun.bazi.core.LiuRenTables.liuHe(i.zhiShang, i.ganJi)
        }
        assertTrue("应能构造出交车相合之盘", found)
    }

    @Test
    fun `命中条目带依据与断语`() {
        val plate = LiuRenCalculator.buildPlate(LocalDateTime.of(2026, 3, 25, 9, 0))
        val hits = BiFaCalculator.analyze(plate)
        hits.forEach {
            assertTrue("法${it.no}依据非空", it.basis.isNotBlank())
            assertTrue("法${it.no}断语非空", it.duanYu.isNotBlank())
        }
    }
}
