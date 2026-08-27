package cn.xulun.bazi

import cn.xulun.bazi.core.TeShuGeJuCalculator
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.TeShuKind
import cn.xulun.bazi.model.YunVerdict
import cn.xulun.bazi.core.CongGeYunCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * 木火土三气成象格成格锚点。理论见 .shengke-reference.md 第五六节一(一)。
 * 三行顺势相生气势均衡，无破格之金水（行运篇待续）。
 */
class MuHuoTuSanQiTest {

    @Test fun `木火土例一 戊辰丁巳甲寅乙丑 顺势相生`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.CHEN, HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.YI, EarthlyBranch.CHOU))
        assertNotNull("木火土三行顺势化生气势均衡：$r", r)
        assertEquals(TeShuKind.SAN_QI, r!!.kind)
        assertEquals("木火土三气成象", r.name)
    }

    @Test fun `木火土例二 甲寅丁丑丁丑丙午 寒土得暖`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.DING, EarthlyBranch.CHOU,
            HeavenlyStem.DING, EarthlyBranch.CHOU, HeavenlyStem.BING, EarthlyBranch.WU))
        assertNotNull("寒湿丑土得旺火温暖生化有情：$r", r)
        assertEquals(TeShuKind.SAN_QI, r!!.kind)
        assertEquals("木火土三气成象", r.name)
    }

    @Test fun `木火土例三 甲寅丙寅丁丑丁未 一气化生`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.DING, EarthlyBranch.CHOU, HeavenlyStem.DING, EarthlyBranch.WEI))
        assertNotNull("木泄菁英于火火施慈爱于土：$r", r)
        assertEquals(TeShuKind.SAN_QI, r!!.kind)
        assertEquals("木火土三气成象", r.name)
    }

    // ── (二)行运（甲寅 丙寅 己巳 戊辰——木强土厚唯火偏弱） ──

    private val chart = lazy {
        chartOf(
            HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JI, EarthlyBranch.SI, HeavenlyStem.WU, EarthlyBranch.CHEN)
    }

    private fun vy(s: HeavenlyStem, b: EarthlyBranch): YunVerdict? =
        CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))?.verdict

    @Test fun `三气行运1 旺火补弱为用旺木旺土失衡为忌`() {
        for ((s, b) in listOf(
            HeavenlyStem.BING to EarthlyBranch.WU, HeavenlyStem.DING to EarthlyBranch.SI,
        )) assertEquals("(${s.chinese}${b.chinese}) 补弱火——应用神", YunVerdict.YONG, vy(s, b))
        for ((s, b) in listOf(
            HeavenlyStem.JIA to EarthlyBranch.YIN, HeavenlyStem.YI to EarthlyBranch.MAO,
            HeavenlyStem.REN to EarthlyBranch.YIN, HeavenlyStem.GUI to EarthlyBranch.MAO,
            HeavenlyStem.WU to EarthlyBranch.CHEN, HeavenlyStem.WU to EarthlyBranch.XU,
            HeavenlyStem.JI to EarthlyBranch.CHOU, HeavenlyStem.JI to EarthlyBranch.WEI,
        )) assertEquals("(${s.chinese}${b.chinese}) 补有余之行失衡——应忌", YunVerdict.JI, vy(s, b))
    }

    @Test fun `三气行运2 强金转正格取印火`() {
        for ((s, b) in listOf(
            HeavenlyStem.GENG to EarthlyBranch.SHEN, HeavenlyStem.XIN to EarthlyBranch.YOU,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 克泄交加转正格——应转正格", YunVerdict.HUAN_YUAN, r.verdict)
            assertEquals("用神取印绶之火", Element.FIRE, r.newYongShen)
        }
    }

    @Test fun `三气行运3 强水转正格取印火（弱水忌）`() {
        for ((s, b) in listOf(
            HeavenlyStem.REN to EarthlyBranch.ZI, HeavenlyStem.GUI to EarthlyBranch.HAI,
            HeavenlyStem.GENG to EarthlyBranch.ZI, HeavenlyStem.XIN to EarthlyBranch.HAI,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 身弱财官旺转正格——应转正格", YunVerdict.HUAN_YUAN, r.verdict)
            assertEquals(Element.FIRE, r.newYongShen)
        }
    }
}
