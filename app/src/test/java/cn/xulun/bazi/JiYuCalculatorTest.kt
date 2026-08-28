package cn.xulun.bazi

import cn.xulun.bazi.core.CaiYunCalculator
import cn.xulun.bazi.core.DaYunCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 机遇推断测试（教材六节例一二锚定, .caiyun-reference.md）。
 * 机遇=全命局和局之年;种类=流年十神;燥土不生金;天克地冲制忌化战为和亦吉。
 */
class JiYuCalculatorTest {

    private fun analyze(
        ys: HeavenlyStem, yb: EarthlyBranch,
        ms: HeavenlyStem, mb: EarthlyBranch,
        ds: HeavenlyStem, db: EarthlyBranch,
        hs: HeavenlyStem, hb: EarthlyBranch,
    ): cn.xulun.bazi.model.CaiYunResult {
        val bazi = chartOf(ys, yb, ms, mb, ds, db, hs, hb, 1950, 1980)
        return CaiYunCalculator.analyze(bazi, Gender.MALE, DaYunCalculator.compute(bazi, Gender.MALE))
    }

    private fun chenXianSheng() = analyze(
        HeavenlyStem.YI, EarthlyBranch.SI,
        HeavenlyStem.GUI, EarthlyBranch.WEI,
        HeavenlyStem.BING, EarthlyBranch.XU,
        HeavenlyStem.DING, EarthlyBranch.YOU,
    )

    private fun tangXianSheng() = analyze(
        HeavenlyStem.REN, EarthlyBranch.ZI,
        HeavenlyStem.GENG, EarthlyBranch.XU,
        HeavenlyStem.DING, EarthlyBranch.HAI,
        HeavenlyStem.WU, EarthlyBranch.SHEN,
    )

    private fun years(r: cn.xulun.bazi.model.CaiYunResult, yun: String) =
        r.yuns.first { it.pillar.fullName == yun }.years

    @Test
    fun `例一 陈先生 庚辰运1994甲戌 发财机遇`() {
        val n = years(chenXianSheng(), "庚辰").firstOrNull { it.year == 1994 }
        assertNotNull(n)
        assertTrue(n!!.kind.chinese.contains("发"))
    }

    @Test
    fun `例一 陈先生 己卯运2004甲申2005乙酉 发财机遇`() {
        val ys = years(chenXianSheng(), "己卯")
        listOf(2004, 2005).forEach { y ->
            val n = ys.firstOrNull { it.year == y }
            assertNotNull("缺 $y", n)
            assertTrue("$y 应为机遇年:${n!!.kind.chinese}/${n.text.take(60)}", n.kind.chinese.contains("发"))
        }
    }

    @Test
    fun `例一 陈先生 2006丙戌 2007丁亥 破财`() {
        val ys = years(chenXianSheng(), "己卯")
        listOf(2006, 2007).forEach { y ->
            val n = ys.firstOrNull { it.year == y }
            assertNotNull("缺 $y", n)
            assertTrue("$y 应为破/谨慎:${n!!.kind.chinese}", n.kind.chinese.contains("破") || n.kind.chinese.contains("谨慎"))
        }
    }

    @Test
    fun `例二 唐先生 癸丑运2004甲申2005乙酉 金旺通关 发财机遇`() {
        val ys = years(tangXianSheng(), "癸丑")
        listOf(2004, 2005).forEach { y ->
            val n = ys.firstOrNull { it.year == y }
            assertNotNull("缺 $y", n)
            assertTrue("$y 应为机遇年:${n!!.kind.chinese}/${n.text.take(60)}", n.kind.chinese.contains("发"))
        }
    }
}
