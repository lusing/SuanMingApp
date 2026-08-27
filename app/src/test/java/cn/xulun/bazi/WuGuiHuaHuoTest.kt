package cn.xulun.bazi

import cn.xulun.bazi.core.CongGeYunCalculator
import cn.xulun.bazi.core.TeShuGeJuCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.YunVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 戊癸化火格锚点（化气格收官）。理论见 .shengke-reference.md 化气格(五)节。
 * 戊日=从印通道，癸日=从财通道。
 */
class WuGuiHuaHuoTest {

    // ── 成格三例 ──

    @Test fun `火例一 丙午癸巳戊午丁巳 戊日从印`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.BING, EarthlyBranch.WU, HeavenlyStem.GUI, EarthlyBranch.SI,
            HeavenlyStem.WU, EarthlyBranch.WU, HeavenlyStem.DING, EarthlyBranch.SI))!!
        assertTrue("戊日化火应从印：${r.name}", r.name.contains("从印"))
    }

    @Test fun `火例二 戊午戊午癸未丁巳 两戊合癸癸日从财`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.WU, HeavenlyStem.WU, EarthlyBranch.WU,
            HeavenlyStem.GUI, EarthlyBranch.WEI, HeavenlyStem.DING, EarthlyBranch.SI))!!
        assertTrue("癸日化火应从财：${r.name}", r.name.contains("从财"))
    }

    @Test fun `火例三 癸巳戊午癸未戊午 两组戊癸癸日从财`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.SI, HeavenlyStem.WU, EarthlyBranch.WU,
            HeavenlyStem.GUI, EarthlyBranch.WEI, HeavenlyStem.WU, EarthlyBranch.WU))!!
        assertTrue("癸日化火应从财：${r.name}", r.name.contains("从财"))
    }

    // ── 岁运（例一 戊日从印论法） ──

    private val wuRi = chartOf(
        HeavenlyStem.BING, EarthlyBranch.WU, HeavenlyStem.GUI, EarthlyBranch.SI,
        HeavenlyStem.WU, EarthlyBranch.WU, HeavenlyStem.DING, EarthlyBranch.SI)

    private fun v(p: Pillar): YunVerdict? = CongGeYunCalculator.analyze(wuRi, listOf(p))?.verdict

    @Test fun `戊日化火 旺火用 木喜`() {
        assertEquals(YunVerdict.YONG, v(Pillar(HeavenlyStem.BING, EarthlyBranch.WU)))
        assertEquals(YunVerdict.XI, v(Pillar(HeavenlyStem.JIA, EarthlyBranch.YIN)))
    }

    @Test fun `戊日化火 强水强金强湿土转格`() {
        assertEquals(YunVerdict.ZHUAN_GE, v(Pillar(HeavenlyStem.REN, EarthlyBranch.ZI)))
        assertEquals(YunVerdict.ZHUAN_GE, v(Pillar(HeavenlyStem.GENG, EarthlyBranch.SHEN)))
        assertEquals(YunVerdict.ZHUAN_GE, v(Pillar(HeavenlyStem.WU, EarthlyBranch.CHEN)))
    }
}
