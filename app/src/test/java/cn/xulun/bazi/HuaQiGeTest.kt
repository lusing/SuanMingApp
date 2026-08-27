package cn.xulun.bazi

import cn.xulun.bazi.core.CongGeYunCalculator
import cn.xulun.bazi.core.TeShuGeJuCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Element
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 化气格锚点（甲己化土代表）。理论见 .shengke-reference.md 第五三节。
 * 引擎覆盖验证：甲日化土=从财 / 己日化土=稼穑专旺 / 遥隔亦化 / 争合。
 */
class HuaQiGeTest {

    // ── 成格三例 ──

    @Test fun `化土例一 戊辰己未甲戌戊辰 甲从财化土`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.CHEN, HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.JIA, EarthlyBranch.XU, HeavenlyStem.WU, EarthlyBranch.CHEN))
        assertNotNull("应判化气从财：$r", r)
        assertTrue("应从土（甲己合化土）：${r!!.yongShen.joinToString("") { it.chinese }}", Element.EARTH in r.yongShen)
    }

    @Test fun `化土例二 己未甲戌己丑戊辰 争合两己从真`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.JI, EarthlyBranch.WEI, HeavenlyStem.JIA, EarthlyBranch.XU,
            HeavenlyStem.JI, EarthlyBranch.CHOU, HeavenlyStem.WU, EarthlyBranch.CHEN))
        assertNotNull("应判稼穑/从：$r", r)
        assertTrue("应土势（两己争合甲化土）：${r!!.yongShen.joinToString("") { it.chinese }}", Element.EARTH in r.yongShen)
    }

    @Test fun `化土例三 甲戌戊辰己未戊辰 遥隔亦化`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.JIA, EarthlyBranch.XU, HeavenlyStem.WU, EarthlyBranch.CHEN,
            HeavenlyStem.JI, EarthlyBranch.WEI, HeavenlyStem.WU, EarthlyBranch.CHEN))
        assertNotNull("甲己遥隔仍应化（化神强劲）：$r", r)
        assertTrue("应土势：${r!!.yongShen.joinToString("") { it.chinese }}", Element.EARTH in r.yongShen)
    }

    // ── 甲日化土岁运（例一总表：戊辰己未甲辰戊辰，从财论法） ──

    private val jiaDay = chartOf(
        HeavenlyStem.WU, EarthlyBranch.CHEN, HeavenlyStem.JI, EarthlyBranch.WEI,
        HeavenlyStem.JIA, EarthlyBranch.CHEN, HeavenlyStem.WU, EarthlyBranch.CHEN)

    private fun vj(p: cn.xulun.bazi.model.Pillar) = CongGeYunCalculator.analyze(jiaDay, listOf(p))?.verdict

    @Test fun `甲日化土 旺土用神 火喜神`() {
        assertEquals(cn.xulun.bazi.model.YunVerdict.YONG, vj(cn.xulun.bazi.model.Pillar(HeavenlyStem.JI, EarthlyBranch.CHOU)))
        assertEquals(cn.xulun.bazi.model.YunVerdict.YONG, vj(cn.xulun.bazi.model.Pillar(HeavenlyStem.WU, EarthlyBranch.WU)))
        val xi = vj(cn.xulun.bazi.model.Pillar(HeavenlyStem.BING, EarthlyBranch.WU))
        assertTrue("丙午应喜（火生土）：$xi", xi == cn.xulun.bazi.model.YunVerdict.XI || xi == cn.xulun.bazi.model.YunVerdict.YONG)
    }

    @Test fun `甲日化土 强水转两气 强金转两气 强木转两气`() {
        assertEquals(cn.xulun.bazi.model.YunVerdict.ZHUAN_GE, vj(cn.xulun.bazi.model.Pillar(HeavenlyStem.REN, EarthlyBranch.ZI)))
        assertEquals(cn.xulun.bazi.model.YunVerdict.ZHUAN_GE, vj(cn.xulun.bazi.model.Pillar(HeavenlyStem.GENG, EarthlyBranch.SHEN)))
        assertEquals(cn.xulun.bazi.model.YunVerdict.ZHUAN_GE, vj(cn.xulun.bazi.model.Pillar(HeavenlyStem.JIA, EarthlyBranch.YIN)))
    }
}
