package cn.xulun.bazi

import cn.xulun.bazi.core.HeHuaJudge
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 天干反化条件锚点（以力服人——克型/势型）。理论见 .shengke-reference.md 第六十节五。
 */
class HeHuaFanHuaTest {

    @Test fun `例一 己巳辛未丙午甲午 丙辛反化火（克型）`() {
        val b = chartOf(
            HeavenlyStem.JI, EarthlyBranch.SI, HeavenlyStem.XIN, EarthlyBranch.WEI,
            HeavenlyStem.BING, EarthlyBranch.WU, HeavenlyStem.JIA, EarthlyBranch.WU)
        val v = HeHuaJudge.judge(b)!!
        assertTrue("丙辛合应化：${v.basis}", v.cheng)
        assertEquals("辛金无根逢烈火服从——反化火", Element.FIRE, v.huaShen)
    }

    @Test fun `例二 戊申辛酉丙申戊戌 丙从辛化金（势型·从财格）`() {
        val b = chartOf(
            HeavenlyStem.WU, EarthlyBranch.SHEN, HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.BING, EarthlyBranch.SHEN, HeavenlyStem.WU, EarthlyBranch.XU)
        val v = HeHuaJudge.judge(b)!!
        assertTrue("丙辛合应化：${v.basis}", v.cheng)
        assertEquals("丙如火中残烛大势趋金——势服反化金", Element.METAL, v.huaShen)
    }
}
