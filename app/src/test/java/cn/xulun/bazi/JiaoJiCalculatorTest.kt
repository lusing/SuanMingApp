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

    @Test
    fun `例二 王先生 壬子辛亥丙辰庚寅 食神藏支内秀+食伤克水打折+水智补弱`() {
        val bazi = chartOf(
            HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.XIN, EarthlyBranch.HAI,
            HeavenlyStem.BING, EarthlyBranch.CHEN,
            HeavenlyStem.GENG, EarthlyBranch.YIN,
            1950, 2000,
        )
        assertEquals("壬子 辛亥 丙辰 庚寅", bazi.fullBazi)
        val dy = DaYunCalculator.compute(bazi, Gender.MALE)
        val r = JiaoJiCalculator.analyze(bazi, dy)
        // 有食神（辰藏支）+水当令党众——交际天赋；食神与水相克打折；水旺聪明灵活——交际仍不错
        assertTrue(r.talent.contains("不错") || r.talent.contains("好"))
        assertTrue(r.waterNote.contains("相克") || r.waterNote.contains("折扣"))
        // 只有食神无伤官且食神在地支——内秀：久处方知，口碑好
        assertTrue(r.style.contains("内秀"))
    }

    @Test
    fun `例三 刘先生 己酉丙子戊午癸亥 伤官与水相生交际不错`() {
        val bazi = chartOf(
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.BING, EarthlyBranch.ZI,
            HeavenlyStem.WU, EarthlyBranch.WU,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            1950, 2000,
        )
        assertEquals("己酉 丙子 戊午 癸亥", bazi.fullBazi)
        val dy = DaYunCalculator.compute(bazi, Gender.MALE)
        val r = JiaoJiCalculator.analyze(bazi, dy)
        // 原局有伤官和水，伤官与水相生——交际天赋不错（聪明能干待人热情善交际）
        assertTrue(r.talent.contains("不错") || r.talent.contains("好"))
        assertTrue(r.waterNote.contains("相生"))
    }
}
