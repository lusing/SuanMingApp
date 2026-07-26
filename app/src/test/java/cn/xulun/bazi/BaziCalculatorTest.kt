package cn.xulun.bazi

import cn.xulun.bazi.core.BaziCalculator
import cn.xulun.bazi.core.DaYunCalculator
import cn.xulun.bazi.core.GeJuCalculator
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.Gender
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

/**
 * 锚点测试：期望值由参考 Dart 应用（F:/code/bazi）对相同出生时刻计算得出，作为移植行为基准。
 */
class BaziCalculatorTest {

    @Test
    fun anchor_1985_05_20_zi_male() {
        val bazi = BaziCalculator.calculate(LocalDateTime.of(1985, 5, 20, 0, 10))
        val geju = GeJuCalculator.determine(bazi)
        val dayun = DaYunCalculator.compute(bazi, Gender.MALE)

        assertEquals("乙丑 辛巳 己未 甲子", bazi.fullBazi)
        assertEquals(55, bazi.dayPillar.cycleIndex)
        assertEquals("正印格", geju.name)
        assertEquals(DayStrength.STRONG, geju.strength)

        assertEquals(false, dayun.forward)
        assertEquals("出生后 4 年 8 个月 12 天起运", dayun.qiYunText)
        assertEquals(1990, dayun.startDate.year)
        assertEquals(2, dayun.startDate.monthValue)
        assertEquals(1, dayun.startDate.dayOfMonth)

        assertEquals(8, dayun.yuns.size)
        val first = dayun.yuns.first()
        assertEquals("庚辰", first.pillar.fullName)
        assertEquals(6, first.startAge)
        assertEquals(1990, first.startYear)
        assertEquals(1999, first.endYear)
        assertEquals("庚午", first.liuNian.first().pillar.fullName)
        assertEquals(1990, first.liuNian.first().year)
    }

    @Test
    fun anchor_1985_02_03_preLichun_male() {
        // 立春前：年柱用前一年（1984 甲子，而非 1985 乙丑）
        val bazi = BaziCalculator.calculate(LocalDateTime.of(1985, 2, 3, 0, 10))
        val geju = GeJuCalculator.determine(bazi)
        val dayun = DaYunCalculator.compute(bazi, Gender.MALE)

        assertEquals("甲子", bazi.yearPillar.fullName)
        assertEquals("甲子 丁丑 癸酉 壬子", bazi.fullBazi)
        assertEquals("七杀格", geju.name)
        assertEquals(DayStrength.BALANCED, geju.strength)
        assertEquals(true, dayun.forward)
    }

    @Test
    fun anchor_1990_01_01_female() {
        val bazi = BaziCalculator.calculate(LocalDateTime.of(1990, 1, 1, 12, 0))
        val geju = GeJuCalculator.determine(bazi)
        val dayun = DaYunCalculator.compute(bazi, Gender.FEMALE)

        assertEquals("己巳 丙子 丙寅 甲午", bazi.fullBazi)
        assertEquals(2, bazi.dayPillar.cycleIndex)
        assertEquals("正官格", geju.name)
        assertEquals(DayStrength.STRONG, geju.strength)
        assertEquals(true, dayun.forward)
        assertEquals(1991, dayun.startDate.year)
    }

    @Test
    fun anchor_1900_01_01_referenceConstant() {
        // 参考基准常量：1900-01-01 = 甲戌日（六十甲子序号 10）
        val bazi = BaziCalculator.calculate(LocalDateTime.of(1900, 1, 1, 12, 0))
        val geju = GeJuCalculator.determine(bazi)
        val dayun = DaYunCalculator.compute(bazi, Gender.MALE)

        assertEquals("甲戌", bazi.dayPillar.fullName)
        assertEquals(10, bazi.dayPillar.cycleIndex)
        assertEquals("己亥 丙子 甲戌 庚午", bazi.fullBazi)
        assertEquals("正印格", geju.name)
        assertEquals(DayStrength.BALANCED, geju.strength)
        assertEquals(false, dayun.forward)
        assertEquals(1908, dayun.startDate.year)
    }

    @Test
    fun anchor_2000_08_15_female() {
        val bazi = BaziCalculator.calculate(LocalDateTime.of(2000, 8, 15, 9, 30))
        val geju = GeJuCalculator.determine(bazi)
        val dayun = DaYunCalculator.compute(bazi, Gender.FEMALE)

        assertEquals("庚辰 甲申 乙巳 辛巳", bazi.fullBazi)
        assertEquals(41, bazi.dayPillar.cycleIndex)
        assertEquals("正官格", geju.name)
        assertEquals(DayStrength.WEAK, geju.strength)
        assertEquals(false, dayun.forward)
        assertEquals(2003, dayun.startDate.year)
    }
}
