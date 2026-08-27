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
 * 木土疏通格锚点（相克对）。理论见 .shengke-reference.md 第五五节七。
 * 成格三例 + 行运总表（戊戌 甲寅 甲寅 戊辰——木胜于土）。
 */
class MuTuShuTongTest {

    @Test fun `通例一 戊戌甲寅戊辰甲寅 旺木疏厚土`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.XU, HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.WU, EarthlyBranch.CHEN, HeavenlyStem.JIA, EarthlyBranch.YIN))
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("木土疏通格", r.name)
    }

    @Test fun `通例二 乙未戊寅乙未戊寅`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.YI, EarthlyBranch.WEI, HeavenlyStem.WU, EarthlyBranch.YIN,
            HeavenlyStem.YI, EarthlyBranch.WEI, HeavenlyStem.WU, EarthlyBranch.YIN))
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("木土疏通格", r.name)
    }

    @Test fun `通例三 戊寅乙丑乙丑戊寅 刲羊解牛`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.YIN, HeavenlyStem.YI, EarthlyBranch.CHOU,
            HeavenlyStem.YI, EarthlyBranch.CHOU, HeavenlyStem.WU, EarthlyBranch.YIN))
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("木土疏通格", r.name)
    }

    private val chart = lazy {
        chartOf(
            HeavenlyStem.WU, EarthlyBranch.XU, HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.WU, EarthlyBranch.CHEN)
    }

    private fun vy(s: HeavenlyStem, b: EarthlyBranch): YunVerdict? =
        CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))?.verdict

    @Test fun `通行运1 旺土为用旺木为忌`() {
        for ((s, b) in listOf(
            HeavenlyStem.WU to EarthlyBranch.XU, HeavenlyStem.WU to EarthlyBranch.CHEN,
        )) assertEquals("(${s.chinese}${b.chinese}) 补弱土——应用神", YunVerdict.YONG, vy(s, b))
        for ((s, b) in listOf(
            HeavenlyStem.JIA to EarthlyBranch.YIN, HeavenlyStem.YI to EarthlyBranch.MAO,
        )) assertEquals("(${s.chinese}${b.chinese}) 旺木失衡——应忌", YunVerdict.JI, vy(s, b))
    }

    @Test fun `通行运2 强金与土联合转正格两停`() {
        for ((s, b) in listOf(
            HeavenlyStem.GENG to EarthlyBranch.SHEN, HeavenlyStem.XIN to EarthlyBranch.YOU,
        )) assertEquals("(${s.chinese}${b.chinese}) 旺金与土联合身旺财官两停——应转正格", YunVerdict.HUAN_YUAN, vy(s, b))
    }

    @Test fun `通行运3 强水转正格（书取财克印耗身注记）`() {
        for ((s, b) in listOf(
            HeavenlyStem.REN to EarthlyBranch.ZI, HeavenlyStem.GUI to EarthlyBranch.HAI,
        )) assertEquals("(${s.chinese}${b.chinese}) 旺水滋木转正格——应转正格", YunVerdict.HUAN_YUAN, vy(s, b))
    }

    @Test fun `通行运4 强火通关转三气顺势平衡`() {
        for ((s, b) in listOf(
            HeavenlyStem.BING to EarthlyBranch.WU, HeavenlyStem.DING to EarthlyBranch.SI,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 通木土之关——应转三气", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals("木旺土弱取土", Element.EARTH, r.newYongShen)
        }
    }
}
