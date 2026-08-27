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
 * 金水木三气成象格锚点。理论见 .shengke-reference.md 第五六节四。
 * 成格三例（申子半合解寅申冲）+ 行运总表（癸亥 辛酉 甲寅 癸酉——金水迭旺木略偏弱）。
 */
class JinShuiMuSanQiTest {

    @Test fun `金水木例一 癸亥辛酉甲寅甲子 停均不杂`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.HAI, HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.JIA, EarthlyBranch.ZI))
        assertEquals(TeShuKind.SAN_QI, r!!.kind)
        assertEquals("金水木三气成象", r.name)
    }

    @Test fun `金水木例二 庚申甲申壬子壬寅 申子解冲`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GENG, EarthlyBranch.SHEN, HeavenlyStem.JIA, EarthlyBranch.SHEN,
            HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.REN, EarthlyBranch.YIN))
        assertEquals(TeShuKind.SAN_QI, r!!.kind)
        assertEquals("金水木三气成象", r.name)
    }

    @Test fun `金水木例三 壬申壬子甲寅癸酉 流通不杂`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.REN, EarthlyBranch.SHEN, HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.GUI, EarthlyBranch.YOU))
        assertEquals(TeShuKind.SAN_QI, r!!.kind)
        assertEquals("金水木三气成象", r.name)
    }

    private val chart = lazy {
        chartOf(
            HeavenlyStem.GUI, EarthlyBranch.HAI, HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.GUI, EarthlyBranch.YOU)
    }

    private fun vy(s: HeavenlyStem, b: EarthlyBranch): YunVerdict? =
        CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))?.verdict

    @Test fun `金水木行运1 旺木补弱为用金水失衡为忌`() {
        for ((s, b) in listOf(
            HeavenlyStem.JIA to EarthlyBranch.YIN, HeavenlyStem.YI to EarthlyBranch.MAO,
            HeavenlyStem.REN to EarthlyBranch.YIN, HeavenlyStem.GUI to EarthlyBranch.MAO,
        )) assertEquals("(${s.chinese}${b.chinese}) 补弱木——应用神", YunVerdict.YONG, vy(s, b))
        for ((s, b) in listOf(
            HeavenlyStem.GENG to EarthlyBranch.SHEN, HeavenlyStem.XIN to EarthlyBranch.YOU,
            HeavenlyStem.REN to EarthlyBranch.ZI, HeavenlyStem.GUI to EarthlyBranch.HAI,
            HeavenlyStem.REN to EarthlyBranch.SHEN, HeavenlyStem.GUI to EarthlyBranch.YOU,
            HeavenlyStem.GENG to EarthlyBranch.ZI, HeavenlyStem.XIN to EarthlyBranch.HAI,
        )) assertEquals("(${s.chinese}${b.chinese}) 补有余金水——应忌", YunVerdict.JI, vy(s, b))
    }

    @Test fun `金水木行运2 强火转正格取印绶`() {
        for ((s, b) in listOf(
            HeavenlyStem.BING to EarthlyBranch.WU, HeavenlyStem.JIA to EarthlyBranch.WU,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 身弱食伤官杀旺——应转正格", YunVerdict.HUAN_YUAN, r.verdict)
            assertEquals("制食伤化官生身", Element.WATER, r.newYongShen)
        }
    }

    @Test fun `金水木行运3 强土转正格取印绶`() {
        for ((s, b) in listOf(
            HeavenlyStem.JI to EarthlyBranch.WEI, HeavenlyStem.DING to EarthlyBranch.WEI,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 身弱财官旺——应转正格", YunVerdict.HUAN_YUAN, r.verdict)
            assertEquals("化官生身耗财", Element.WATER, r.newYongShen)
        }
    }
}
