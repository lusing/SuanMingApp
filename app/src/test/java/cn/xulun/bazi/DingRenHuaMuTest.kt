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
 * 丁壬化木格锚点。理论见 .shengke-reference.md 化气格(四)节。
 * 丁日=从印通道，壬日=从儿通道。
 */
class DingRenHuaMuTest {

    // ── 成格三例 ──

    @Test fun `木例一 丁卯壬寅丁卯壬寅 两丁合两壬从印`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.DING, EarthlyBranch.MAO, HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.DING, EarthlyBranch.MAO, HeavenlyStem.REN, EarthlyBranch.YIN))!!
        assertTrue("丁日化木应从印：${r.name}", r.name.contains("从印"))
    }

    @Test fun `木例二 癸亥乙卯壬寅丁未 壬日从儿`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.HAI, HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.REN, EarthlyBranch.YIN, HeavenlyStem.DING, EarthlyBranch.WEI))!!
        assertTrue("壬日化木应从儿：${r.name}", r.name.contains("从儿"))
    }

    @Test fun `木例三 壬寅甲辰丁卯癸卯 遥隔会局从印`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.REN, EarthlyBranch.YIN, HeavenlyStem.JIA, EarthlyBranch.CHEN,
            HeavenlyStem.DING, EarthlyBranch.MAO, HeavenlyStem.GUI, EarthlyBranch.MAO))!!
        assertTrue("丁日遥隔化木应从印：${r.name}", r.name.contains("从印"))
    }

    // ── 岁运（例一 丁日从印论法） ──

    private val dingRi = chartOf(
        HeavenlyStem.REN, EarthlyBranch.YIN, HeavenlyStem.JIA, EarthlyBranch.CHEN,
        HeavenlyStem.DING, EarthlyBranch.MAO, HeavenlyStem.REN, EarthlyBranch.YIN)

    private fun v(p: Pillar): YunVerdict? = CongGeYunCalculator.analyze(dingRi, listOf(p))?.verdict

    @Test fun `丁日化木 旺木用 水喜`() {
        assertEquals(YunVerdict.YONG, v(Pillar(HeavenlyStem.JIA, EarthlyBranch.YIN)))
        assertEquals(YunVerdict.XI, v(Pillar(HeavenlyStem.REN, EarthlyBranch.ZI)))
    }

    @Test fun `丁日化木 强金强土强火转格`() {
        assertEquals(YunVerdict.ZHUAN_GE, v(Pillar(HeavenlyStem.GENG, EarthlyBranch.SHEN)))
        assertEquals(YunVerdict.ZHUAN_GE, v(Pillar(HeavenlyStem.WU, EarthlyBranch.XU)))
        // 丙午强火：助日主抵制壬之合化——教材"木火两气取火"；与多从例3（强土还原正格）同构异断（第九处），转格/还原皆通
        val rf = v(Pillar(HeavenlyStem.BING, EarthlyBranch.WU))
        assertTrue("丙午应转格/还原：$rf", rf == YunVerdict.ZHUAN_GE || rf == YunVerdict.HUAN_YUAN)
    }
}
