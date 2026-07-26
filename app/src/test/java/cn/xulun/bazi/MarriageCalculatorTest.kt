package cn.xulun.bazi

import cn.xulun.bazi.core.BaziCalculator
import cn.xulun.bazi.core.MarriageCalculator
import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.MarriagePattern
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.TenGod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * 婚姻分析器测试：配偶星（性别区分）、夫妻宫冲合刑、婚姻格局、神煞。
 */
class MarriageCalculatorTest {

    @Test
    fun `1985-05-20 男 配偶星财 夫妻宫丑未冲动荡 桃花子`() {
        val bazi = BaziCalculator.calculate(LocalDateTime.of(1985, 5, 20, 0, 10))
        assertEquals("乙丑 辛巳 己未 甲子", bazi.fullBazi)
        val result = MarriageCalculator.analyze(bazi, Gender.MALE)

        // 男命配偶星 = 财星；丑藏癸、子癸（偏财）
        assertEquals("财星", result.spouseStarName)
        assertEquals(2, result.spouseStars.size)
        assertTrue(result.spouseStars.all { it.tenGod == TenGod.PIAN_CAI })

        // 夫妻宫未，逢丑未冲 → 动荡
        assertEquals(EarthlyBranch.WEI, result.palace.branch)
        assertTrue(result.palace.chong?.contains("丑未相冲") == true)
        assertFalse(result.palace.stable)
        assertEquals(MarriagePattern.UNSTABLE, result.pattern)

        // 桃花：日支未 → 亥卯未 → 子；原局时支子
        assertTrue(result.shenSha.any { it.name == "桃花" && it.branch == EarthlyBranch.ZI })
    }

    @Test
    fun `1990-01-01 女 配偶星官杀 夫妻宫安稳`() {
        val bazi = BaziCalculator.calculate(LocalDateTime.of(1990, 1, 1, 12, 0))
        assertEquals("己巳 丙子 丙寅 甲午", bazi.fullBazi)
        val result = MarriageCalculator.analyze(bazi, Gender.FEMALE)

        // 女命配偶星 = 官杀；子藏癸（正官）
        assertEquals("官杀", result.spouseStarName)
        assertTrue(result.spouseStars.any { it.tenGod == TenGod.ZHENG_GUAN })

        // 夫妻宫寅，无冲无刑 → 安稳
        assertEquals(EarthlyBranch.YIN, result.palace.branch)
        assertTrue(result.palace.stable)
    }

    @Test
    fun `年支寅春 孤辰在巳`() {
        // 年支寅(春) → 孤辰巳；原局时支巳 → 命带孤辰
        val bazi = baziOf(EarthlyBranch.YIN, EarthlyBranch.ZI, EarthlyBranch.CHEN, EarthlyBranch.SI)
        val result = MarriageCalculator.analyze(bazi, Gender.MALE)
        assertTrue(result.shenSha.any { it.name == "孤辰" && it.branch == EarthlyBranch.SI })
    }

    /** 以四地支构造 BaziResult（天干统一取甲，仅用于婚姻逻辑测试，不保证干支合法配对） */
    private fun baziOf(vararg branches: EarthlyBranch): BaziResult {
        require(branches.size == 4) { "需提供四柱地支" }
        val (y, m, d, h) = branches.map { Pillar(HeavenlyStem.JIA, it) }
        return BaziResult(
            yearPillar = y,
            monthPillar = m,
            dayPillar = d,
            hourPillar = h,
            birthDateTime = LocalDateTime.of(2000, 1, 1, 12, 0),
        )
    }
}
