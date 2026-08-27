package cn.xulun.bazi

import cn.xulun.bazi.core.CongGeYunCalculator
import cn.xulun.bazi.core.TeShuGeJuCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.YunVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 丙辛化水格锚点。理论见 .shengke-reference.md 化气格(三)节。
 * 丙日=从杀通道，辛日=从儿通道；巳申合化水不破格。
 */
class BingXinHuaShuiTest {

    // ── 成格三例 ──

    @Test fun `水例一 壬申辛亥丙子壬辰 丙日从杀`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.REN, EarthlyBranch.SHEN, HeavenlyStem.XIN, EarthlyBranch.HAI,
            HeavenlyStem.BING, EarthlyBranch.ZI, HeavenlyStem.REN, EarthlyBranch.CHEN))!!
        assertTrue("丙日化水应从杀：${r.name}", r.name.contains("从杀"))
    }

    @Test fun `水例二 壬子辛亥丙申癸巳 巳申合化不破格`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.XIN, EarthlyBranch.HAI,
            HeavenlyStem.BING, EarthlyBranch.SHEN, HeavenlyStem.GUI, EarthlyBranch.SI))!!
        assertTrue("丙日化水应从杀（巳合申化水）：${r.name}", r.name.contains("从杀"))
    }

    @Test fun `水例三 壬辰壬子辛亥丙申 辛日从儿`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.REN, EarthlyBranch.CHEN, HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.XIN, EarthlyBranch.HAI, HeavenlyStem.BING, EarthlyBranch.SHEN))!!
        assertTrue("辛日化水应从儿：${r.name}", r.name.contains("从儿"))
    }

    // ── 岁运（例一 丙日从杀论法） ──

    private val bingRi = chartOf(
        HeavenlyStem.REN, EarthlyBranch.SHEN, HeavenlyStem.XIN, EarthlyBranch.HAI,
        HeavenlyStem.BING, EarthlyBranch.ZI, HeavenlyStem.REN, EarthlyBranch.CHEN)

    private fun v(p: Pillar): YunVerdict? = CongGeYunCalculator.analyze(bingRi, listOf(p))?.verdict

    @Test fun `丙日化水 旺水用 金喜`() {
        assertEquals(YunVerdict.YONG, v(Pillar(HeavenlyStem.GUI, EarthlyBranch.HAI)))
        assertEquals(YunVerdict.XI, v(Pillar(HeavenlyStem.GENG, EarthlyBranch.SHEN)))
    }

    @Test fun `丙日化水 强土强木强火转格`() {
        assertEquals(YunVerdict.ZHUAN_GE, v(Pillar(HeavenlyStem.WU, EarthlyBranch.XU)))
        assertEquals(YunVerdict.ZHUAN_GE, v(Pillar(HeavenlyStem.JIA, EarthlyBranch.YIN)))
        assertEquals(YunVerdict.ZHUAN_GE, v(Pillar(HeavenlyStem.BING, EarthlyBranch.WU)))
    }
}
