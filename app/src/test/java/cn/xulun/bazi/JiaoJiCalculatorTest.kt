package cn.xulun.bazi

import cn.xulun.bazi.core.DaYunCalculator
import cn.xulun.bazi.core.JiaoJiCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 交际能力推断测试（交际=语言/眼神/肢体表达——食伤为代表；水主智——水强交际手段好）。
 * 锚定例一：湖北石女士 戊申丁巳庚辰庚辰——原局无本气食伤不善交际+土多固执；42岁后壬子食伤运交际转折。
 */
class JiaoJiCalculatorTest {

    @Test
    fun `例一 石女士 戊申丁巳庚辰庚辰 无本气食伤不善交际+土多固执+壬子运转折`() {
        val bazi = chartOf(
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.GENG, EarthlyBranch.CHEN,
            HeavenlyStem.GENG, EarthlyBranch.CHEN,
            1950, 2000,
        )
        assertEquals("戊申 丁巳 庚辰 庚辰", bazi.fullBazi)
        val dy = DaYunCalculator.compute(bazi, Gender.FEMALE)
        val r = JiaoJiCalculator.analyze(bazi, dy)
        // 原局无本气之食伤——不善交际；土多而强——固执己见，不善与人沟通
        assertTrue(r.talent.contains("不善交际") || r.talent.contains("木讷"))
        assertTrue(r.talent.contains("固执") || r.basis.contains("固执"))
        // 42岁后行壬子、辛亥食伤运——交际转折点
        assertTrue(r.yunTurning.contains("壬子") || r.yunTurning.contains("转折"))
    }
}
