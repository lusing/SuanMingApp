package cn.xulun.bazi

import cn.xulun.bazi.core.CongGeYunCalculator
import cn.xulun.bazi.core.TeShuGeJuCalculator
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.TeShuKind
import cn.xulun.bazi.model.YunVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * 火土金三气成象格锚点。理论见 .shengke-reference.md 第五六节二。
 * 成格三例（甲己合化加旺土势）+ 行运总表（己丑 己巳 丁巳 己酉——火土两强金略偏弱）。
 */
class HuoTuJinSanQiTest {

    @Test fun `火土金例一 庚申己丑丙午丙申 调暖冻土`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GENG, EarthlyBranch.SHEN, HeavenlyStem.JI, EarthlyBranch.CHOU,
            HeavenlyStem.BING, EarthlyBranch.WU, HeavenlyStem.BING, EarthlyBranch.SHEN))
        assertEquals(TeShuKind.SAN_QI, r!!.kind)
        assertEquals("火土金三气成象", r.name)
    }

    @Test fun `火土金例二 丁酉戊申己丑庚午 天干不驳`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.DING, EarthlyBranch.YOU, HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.JI, EarthlyBranch.CHOU, HeavenlyStem.GENG, EarthlyBranch.WU))
        assertEquals(TeShuKind.SAN_QI, r!!.kind)
        assertEquals("火土金三气成象", r.name)
    }

    @Test fun `火土金例三 甲辰己巳戊午庚申 甲己合化`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.JIA, EarthlyBranch.CHEN, HeavenlyStem.JI, EarthlyBranch.SI,
            HeavenlyStem.WU, EarthlyBranch.WU, HeavenlyStem.GENG, EarthlyBranch.SHEN))
        assertNotNull("甲己合化加旺土势：$r", r)
        assertEquals(TeShuKind.SAN_QI, r!!.kind)
        assertEquals("火土金三气成象", r.name)
    }

    private val chart = lazy {
        chartOf(
            HeavenlyStem.JI, EarthlyBranch.CHOU, HeavenlyStem.JI, EarthlyBranch.SI,
            HeavenlyStem.DING, EarthlyBranch.SI, HeavenlyStem.JI, EarthlyBranch.YOU)
    }

    private fun vy(s: HeavenlyStem, b: EarthlyBranch): YunVerdict? =
        CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))?.verdict

    @Test fun `火土金行运1 旺金补弱为用`() {
        for ((s, b) in listOf(
            HeavenlyStem.GENG to EarthlyBranch.SHEN, HeavenlyStem.XIN to EarthlyBranch.YOU,
        )) assertEquals("(${s.chinese}${b.chinese}) 补弱金损火土有余——应用神", YunVerdict.YONG, vy(s, b))
    }

    @Test fun `火土金行运2 强水转正格取比劫火`() {
        for ((s, b) in listOf(
            HeavenlyStem.GUI to EarthlyBranch.HAI, HeavenlyStem.XIN to EarthlyBranch.HAI,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 身弱食伤官杀旺——应转正格", YunVerdict.HUAN_YUAN, r.verdict)
            assertEquals("用神取比劫之火", Element.FIRE, r.newYongShen)
        }
    }

    @Test fun `火土金行运3 强木转正格取财损印`() {
        for ((s, b) in listOf(
            HeavenlyStem.JIA to EarthlyBranch.YIN, HeavenlyStem.YI to EarthlyBranch.MAO,
            HeavenlyStem.REN to EarthlyBranch.YIN, HeavenlyStem.GUI to EarthlyBranch.MAO,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 身旺印旺——应转正格", YunVerdict.HUAN_YUAN, r.verdict)
            assertEquals("取财星损印耗身", Element.METAL, r.newYongShen)
        }
    }
}
