package cn.xulun.bazi

import cn.xulun.bazi.core.CongGeYunCalculator
import cn.xulun.bazi.core.TeShuGeJuCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.YunVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 从格岁运喜忌锚点测试。理论见 .shengke-reference.md 第五二节（从财格）。
 * 主锚：壬辰壬子己亥壬申（从财）岁运分类总表十五柱 + 成格三例 + 实例三则。
 */
class CongGeYunCalculatorTest {

    // ── 成格三例（含合化入势从真） ──

    @Test fun `成格一 戊辰己未乙丑丙戌 从财`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.CHEN, HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.YI, EarthlyBranch.CHOU, HeavenlyStem.BING, EarthlyBranch.XU))!!
        assertTrue("应从财：${r.name}", r.name.contains("从财"))
    }

    @Test fun `成格二 癸巳戊午壬午丙午 癸合戊化从真`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.SI, HeavenlyStem.WU, EarthlyBranch.WU,
            HeavenlyStem.REN, EarthlyBranch.WU, HeavenlyStem.BING, EarthlyBranch.WU))!!
        assertTrue("应从财（癸合化不以虚助论）：${r.name} fake=${r.fake}", r.name.contains("从财"))
        assertTrue("癸合戊化入势应从真：fake=${r.fake}", !r.fake)
    }

    @Test fun `成格三 庚辰乙酉丁酉己酉 乙庚合化反助金势从真`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GENG, EarthlyBranch.CHEN, HeavenlyStem.YI, EarthlyBranch.YOU,
            HeavenlyStem.DING, EarthlyBranch.YOU, HeavenlyStem.JI, EarthlyBranch.YOU))!!
        assertTrue("应从财：${r.name}", r.name.contains("从财"))
        assertTrue("乙合庚化不以破格论应从真：fake=${r.fake}", !r.fake)
    }

    // ── 岁运分类总表（壬辰 壬子 己亥 壬申，从财·水） ──

    private val congCai = chartOf(
        HeavenlyStem.REN, EarthlyBranch.CHEN, HeavenlyStem.REN, EarthlyBranch.ZI,
        HeavenlyStem.JI, EarthlyBranch.HAI, HeavenlyStem.REN, EarthlyBranch.SHEN)

    private fun v(p: Pillar): YunVerdict? = CongGeYunCalculator.analyze(congCai, listOf(p))?.verdict

    @Test fun `用神到位 旺水六柱`() {
        for ((s, b) in listOf(
            HeavenlyStem.REN to EarthlyBranch.ZI, HeavenlyStem.GUI to EarthlyBranch.HAI,
            HeavenlyStem.REN to EarthlyBranch.SHEN, HeavenlyStem.GUI to EarthlyBranch.YOU,
            HeavenlyStem.GENG to EarthlyBranch.ZI, HeavenlyStem.XIN to EarthlyBranch.HAI,
        )) assertEquals("(${s.chinese}${b.chinese}) 应用神", YunVerdict.YONG, v(Pillar(s, b)))
    }

    @Test fun `喜神到位 食伤四柱`() {
        for ((s, b) in listOf(
            HeavenlyStem.GENG to EarthlyBranch.SHEN, HeavenlyStem.XIN to EarthlyBranch.YOU,
            HeavenlyStem.WU to EarthlyBranch.SHEN, HeavenlyStem.JI to EarthlyBranch.YOU,
        )) assertEquals("(${s.chinese}${b.chinese}) 应喜神", YunVerdict.XI, v(Pillar(s, b)))
    }

    @Test fun `转两气 强印丙午丁巳取印`() {
        for ((s, b) in listOf(HeavenlyStem.BING to EarthlyBranch.WU, HeavenlyStem.DING to EarthlyBranch.SI)) {
            val r = CongGeYunCalculator.analyze(congCai, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 应转格", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.FIRE, r.newYongShen)
        }
    }

    @Test fun `转两气 强官甲寅乙卯壬寅癸卯取官`() {
        for ((s, b) in listOf(
            HeavenlyStem.JIA to EarthlyBranch.YIN, HeavenlyStem.YI to EarthlyBranch.MAO,
            HeavenlyStem.REN to EarthlyBranch.YIN, HeavenlyStem.GUI to EarthlyBranch.MAO,
        )) {
            val r = CongGeYunCalculator.analyze(congCai, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 应转格", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.WOOD, r.newYongShen)
        }
    }

    @Test fun `转两气 强比戊戌己未丙戌丁未取比`() {
        for ((s, b) in listOf(
            HeavenlyStem.WU to EarthlyBranch.XU, HeavenlyStem.JI to EarthlyBranch.WEI,
            HeavenlyStem.BING to EarthlyBranch.XU, HeavenlyStem.DING to EarthlyBranch.WEI,
        )) {
            val r = CongGeYunCalculator.analyze(congCai, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 应转格", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.EARTH, r.newYongShen)
        }
    }

    @Test fun `忌神 衰弱印绶庚午`() {
        assertEquals(YunVerdict.JI, v(Pillar(HeavenlyStem.GENG, EarthlyBranch.WU)))
    }

    // ── 实例三则 ──

    @Test fun `杨先生 假从财 丙子年忌神`() {
        val b = chartOf(
            HeavenlyStem.REN, EarthlyBranch.WU, HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.REN, EarthlyBranch.YIN, HeavenlyStem.DING, EarthlyBranch.WEI)
        val ts = TeShuGeJuCalculator.analyze(b)
        assertNotNull("应判从格：$ts", ts)
        assertTrue("应从财（火土）：${ts!!.name}", ts.name.contains("从财"))
        assertTrue("壬庚居干应假从：fake=${ts.fake}", ts.fake)
        // 乙卯运丙子年：太岁水引发反叛，忌神（凶）
        val r = CongGeYunCalculator.analyze(b, listOf(
            Pillar(HeavenlyStem.YI, EarthlyBranch.MAO), Pillar(HeavenlyStem.BING, EarthlyBranch.ZI)))
        assertTrue("丙子年应忌/破：$r", r == null || r.verdict == YunVerdict.JI || r.verdict == YunVerdict.HUAN_YUAN)
    }

    @Test fun `刘女士 假从财 甲戌年丑戌刑动还原`() {
        val b = chartOf(
            HeavenlyStem.XIN, EarthlyBranch.HAI, HeavenlyStem.JI, EarthlyBranch.HAI,
            HeavenlyStem.JI, EarthlyBranch.YOU, HeavenlyStem.GUI, EarthlyBranch.YOU)
        val ts = TeShuGeJuCalculator.analyze(b)!!
        assertTrue("应从财（水）：${ts.name}", ts.name.contains("从财"))
        assertTrue("月干己帮身应假从：fake=${ts.fake}", ts.fake)
        // 辛丑运甲戌年：岁支戌为日主之根，还原正格（刑狱）
        val r = CongGeYunCalculator.analyze(b, listOf(
            Pillar(HeavenlyStem.XIN, EarthlyBranch.CHOU), Pillar(HeavenlyStem.JIA, EarthlyBranch.XU)))
        assertEquals(YunVerdict.HUAN_YUAN, r?.verdict)
        // 壬寅运丁丑年：太岁土帮身破格还原（官司）
        val r2 = CongGeYunCalculator.analyze(b, listOf(
            Pillar(HeavenlyStem.REN, EarthlyBranch.YIN), Pillar(HeavenlyStem.DING, EarthlyBranch.CHOU)))
        assertEquals(YunVerdict.HUAN_YUAN, r2?.verdict)
    }

    @Test fun `席女士 假从 辛未年用神到位从象更真`() {
        val b = chartOf(
            HeavenlyStem.GUI, EarthlyBranch.CHOU, HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.JIA, EarthlyBranch.CHEN, HeavenlyStem.XIN, EarthlyBranch.WEI)
        val ts = TeShuGeJuCalculator.analyze(b)
        assertNotNull("应判从格：$ts", ts)
        // 戊午运辛未年：从象更真——吉（用神/喜神）
        val r = CongGeYunCalculator.analyze(b, listOf(
            Pillar(HeavenlyStem.WU, EarthlyBranch.WU), Pillar(HeavenlyStem.XIN, EarthlyBranch.WEI)))
        assertTrue("辛未年应用/喜（吉）：$r", r != null && (r.verdict == YunVerdict.YONG || r.verdict == YunVerdict.XI))
    }
}
