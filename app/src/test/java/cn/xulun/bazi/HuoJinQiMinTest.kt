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
import org.junit.Test

/**
 * 火金器皿格锚点（相克对收官）。理论见 .shengke-reference.md 第五五节十。
 * 成格四例（甲印单透可容/戊癸合化掉）+ 行运总表（辛酉 丁酉 丁酉 丙午——金强于火）。
 */
class HuoJinQiMinTest {

    @Test fun `皿例一 辛酉丙申丙午丙申`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.XIN, EarthlyBranch.YOU, HeavenlyStem.BING, EarthlyBranch.SHEN,
            HeavenlyStem.BING, EarthlyBranch.WU, HeavenlyStem.BING, EarthlyBranch.SHEN))
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("火金器皿格", r.name)
    }

    @Test fun `皿例二 癸巳戊午辛酉丁酉 戊癸合化火`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.SI, HeavenlyStem.WU, EarthlyBranch.WU,
            HeavenlyStem.XIN, EarthlyBranch.YOU, HeavenlyStem.DING, EarthlyBranch.YOU))
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("火金器皿格", r.name)
    }

    @Test fun `皿例三 辛巳丁酉丙申甲午 甲印单透`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.XIN, EarthlyBranch.SI, HeavenlyStem.DING, EarthlyBranch.YOU,
            HeavenlyStem.BING, EarthlyBranch.SHEN, HeavenlyStem.JIA, EarthlyBranch.WU))
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("火金器皿格", r.name)
    }

    @Test fun `皿例四 丁酉丙午丙申丙申 对峙不战`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.DING, EarthlyBranch.YOU, HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.BING, EarthlyBranch.SHEN, HeavenlyStem.BING, EarthlyBranch.SHEN))
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("火金器皿格", r.name)
    }

    private val chart = lazy {
        chartOf(
            HeavenlyStem.XIN, EarthlyBranch.YOU, HeavenlyStem.DING, EarthlyBranch.YOU,
            HeavenlyStem.DING, EarthlyBranch.YOU, HeavenlyStem.BING, EarthlyBranch.WU)
    }

    private fun vy(s: HeavenlyStem, b: EarthlyBranch): YunVerdict? =
        CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))?.verdict

    @Test fun `皿行运1 旺火为用旺金为忌`() {
        for ((s, b) in listOf(
            HeavenlyStem.BING to EarthlyBranch.WU, HeavenlyStem.DING to EarthlyBranch.SI,
            HeavenlyStem.JIA to EarthlyBranch.WU,
        )) assertEquals("(${s.chinese}${b.chinese}) 补弱火——应用神", YunVerdict.YONG, vy(s, b))
        for ((s, b) in listOf(
            HeavenlyStem.GENG to EarthlyBranch.SHEN, HeavenlyStem.XIN to EarthlyBranch.YOU,
        )) assertEquals("(${s.chinese}${b.chinese}) 旺金失衡——应忌", YunVerdict.JI, vy(s, b))
    }

    @Test fun `皿行运2 强水转正格取比劫火`() {
        for ((s, b) in listOf(
            HeavenlyStem.REN to EarthlyBranch.ZI, HeavenlyStem.GUI to EarthlyBranch.HAI,
            HeavenlyStem.GENG to EarthlyBranch.ZI, HeavenlyStem.XIN to EarthlyBranch.HAI,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 身弱财官旺转正格——应转正格", YunVerdict.HUAN_YUAN, r.verdict)
            assertEquals("取比劫克财耗官", Element.FIRE, r.newYongShen)
        }
    }

    @Test fun `皿行运3 强木生火转正格旺木到位`() {
        for ((s, b) in listOf(
            HeavenlyStem.JIA to EarthlyBranch.YIN, HeavenlyStem.YI to EarthlyBranch.MAO,
            HeavenlyStem.REN to EarthlyBranch.YIN, HeavenlyStem.GUI to EarthlyBranch.MAO,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 身旺印旺财旺转正格——应转正格", YunVerdict.HUAN_YUAN, r.verdict)
        }
    }

    @Test fun `皿行运4 强土通关转三气取火`() {
        for ((s, b) in listOf(
            HeavenlyStem.JI to EarthlyBranch.WEI, HeavenlyStem.DING to EarthlyBranch.WEI,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 通火金之关——应转三气", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals("土金偏旺火偏弱——取火", Element.FIRE, r.newYongShen)
        }
    }
}
