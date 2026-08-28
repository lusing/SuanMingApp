package cn.xulun.bazi

import cn.xulun.bazi.core.DaYunCalculator
import cn.xulun.bazi.core.LaoYuCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun assertEquals2(e: String, a: String) = assertEquals(e, a)

/**
 * 牢狱之灾引擎测试（教材第十四节，15 条信息+岁运应期）。
 * 例一边先生：干支皆水土相战无通关——丁丑三刑+官杀旺克身牢狱、甲戌小人、戊寅混战续灾。
 * 例二陈先生：伤官与正官紧贴相战无印制——黑道；壬午年伤官通根+壬丙冲战入狱。
 */
class LaoYuCalculatorTest {

    @Test
    fun `例一 边先生 戊戌己未壬子庚子 水土相战+丁丑三刑牢狱+甲戌小人+戊寅混战`() {
        val bazi = chartOf(
            HeavenlyStem.WU, EarthlyBranch.XU,
            HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.GENG, EarthlyBranch.ZI,
            1900, 1970,
        )
        assertEquals("戊戌 己未 壬子 庚子", bazi.fullBazi)
        val r = LaoYuCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        assertTrue(r.baseRisks.any { it.dim.contains("相战") || it.dim.contains("战局") })
        val y97 = r.years.firstOrNull { it.year == 1997 }
        assertNotNull(y97)
        assertTrue(y97!!.text.contains("牢狱") || y97.text.contains("官司"))
        val y94 = r.years.firstOrNull { it.year == 1994 }
        assertNotNull(y94)
        assertTrue(y94!!.text.contains("小人") || y94.text.contains("陷害"))
        val y98 = r.years.firstOrNull { it.year == 1998 }
        assertNotNull(y98)
        assertTrue(y98!!.text.contains("混战") || y98.text.contains("灾"))
    }

    @Test
    fun `例二 陈先生 庚戌丙戌乙酉丁亥 伤官见官黑道+壬午冲战入狱`() {
        val bazi = chartOf(
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.BING, EarthlyBranch.XU,
            HeavenlyStem.YI, EarthlyBranch.YOU,
            HeavenlyStem.DING, EarthlyBranch.HAI,
            1900, 1970,
        )
        assertEquals("庚戌 丙戌 乙酉 丁亥", bazi.fullBazi)
        val r = LaoYuCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        assertTrue(r.baseRisks.any { it.text.contains("无视法纪") || it.text.contains("黑道") })

        // 例三 杜先生 庚戌丙戌甲申丙寅：日支七杀贴身——庚寅运甲申乙酉金年引发杀旺克身，2004/2005连续官司
        val bazi3 = chartOf(
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.BING, EarthlyBranch.XU,
            HeavenlyStem.JIA, EarthlyBranch.SHEN,
            HeavenlyStem.BING, EarthlyBranch.YIN,
            1900, 1970,
        )
        assertEquals2("庚戌 丙戌 甲申 丙寅", bazi3.fullBazi)
        val r3 = LaoYuCalculator.analyze(bazi3, DaYunCalculator.compute(bazi3, Gender.MALE))
        val y04 = r3.years.firstOrNull { it.year == 2004 }
        assertNotNull(y04)
        assertTrue(y04!!.text.contains("官司") || y04.text.contains("麻烦") || y04.text.contains("小人"))

        // 例四 程先生 庚戌己卯戊子壬戌：偏财贴身+子卯刑+透食神——2000庚辰食神生偏财+辰冲戌+
        // 卯辰害+比劫亢旺克财——盗用商标坐牢
        val bazi4 = chartOf(
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.JI, EarthlyBranch.MAO,
            HeavenlyStem.WU, EarthlyBranch.ZI,
            HeavenlyStem.REN, EarthlyBranch.XU,
            1950, 2000,
        )
        assertEquals2("庚戌 己卯 戊子 壬戌", bazi4.fullBazi)
        val r4 = LaoYuCalculator.analyze(bazi4, DaYunCalculator.compute(bazi4, Gender.MALE))
        assertTrue(r4.baseRisks.any { it.text.contains("铤而走险") })
        val y00 = r4.years.firstOrNull { it.year == 2000 }
        assertNotNull(y00)
        assertTrue(y00!!.text.contains("牢狱"))
        val y02 = r.years.firstOrNull { it.year == 2002 }
        assertNotNull(y02)
        assertTrue(y02!!.text.contains("牢狱") || y02.text.contains("制裁"))
    }
}
