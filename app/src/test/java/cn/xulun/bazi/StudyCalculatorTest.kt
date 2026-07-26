package cn.xulun.bazi

import cn.xulun.bazi.core.BaziCalculator
import cn.xulun.bazi.core.DaYunCalculator
import cn.xulun.bazi.core.StudyCalculator
import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.StudyPattern
import cn.xulun.bazi.model.TenGod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * 学业/学历分析器测试：印星辨识、学历格局、文昌起法。
 */
class StudyCalculatorTest {

    @Test
    fun `1985-05-20 己土 月令印 高学历`() {
        val bazi = BaziCalculator.calculate(LocalDateTime.of(1985, 5, 20, 0, 10))
        assertEquals("乙丑 辛巳 己未 甲子", bazi.fullBazi)
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        val result = StudyCalculator.analyze(bazi, daYun)

        // 印星：巳藏丙(正印·月支本气)、未藏丁(偏印·日支中气)
        assertEquals(2, result.sealStars.size)
        assertTrue(result.sealStars.any { it.tenGod == TenGod.ZHENG_YIN })
        assertTrue(result.sealStars.any { it.tenGod == TenGod.PIAN_YIN })

        // 月令为印 → 高学历
        assertEquals(StudyPattern.HIGH, result.pattern)

        // 己日文昌酉，原局无酉 → 无文昌
        assertTrue(result.shenSha.none { it.name == "文昌" })

        // 排了大运 → 考学岁运非空（必有印/官运）
        assertTrue(result.yunFortune.dayun.isNotEmpty() || result.yunFortune.liunian.isNotEmpty())
    }

    @Test
    fun `无印食伤旺 才艺型`() {
        // 甲木日，地支巳午未戌（火土），无水印；食伤（火）多见
        val bazi = baziOf(EarthlyBranch.SI, EarthlyBranch.WU, EarthlyBranch.WEI, EarthlyBranch.XU)
        val result = StudyCalculator.analyze(bazi, null)

        assertTrue(result.sealStars.isEmpty())
        assertEquals(StudyPattern.TALENT, result.pattern)
    }

    @Test
    fun `无印无食伤 学业平平`() {
        // 甲木日，地支酉卯（金木），无水印无火食伤
        val bazi = baziOf(EarthlyBranch.YOU, EarthlyBranch.MAO, EarthlyBranch.YOU, EarthlyBranch.MAO)
        val result = StudyCalculator.analyze(bazi, null)

        assertTrue(result.sealStars.isEmpty())
        assertEquals(StudyPattern.LOW, result.pattern)
    }

    @Test
    fun `甲日年支巳 文昌在巳`() {
        // 甲日 → 文昌巳；年支巳 → 命带文昌
        val bazi = baziOf(EarthlyBranch.SI, EarthlyBranch.ZI, EarthlyBranch.CHEN, EarthlyBranch.XU)
        val result = StudyCalculator.analyze(bazi, null)
        assertTrue(result.shenSha.any { it.name == "文昌" && it.branch == EarthlyBranch.SI })
    }

    /** 以四地支构造 BaziResult（天干统一取甲，仅用于学业逻辑测试，不保证干支合法配对） */
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
