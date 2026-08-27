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
 * 土水池沼格锚点（相克对）。理论见 .shengke-reference.md 第五五节八。
 * 成格三例（甲随己化不破格）+ 行运总表（癸未 壬戌 癸亥 癸亥——水胜于土）。
 */
class TuShuiChiZhaoTest {

    @Test fun `沼例一 癸亥丙辰戊辰癸亥 旺水得厚土之制`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.HAI, HeavenlyStem.BING, EarthlyBranch.CHEN,
            HeavenlyStem.WU, EarthlyBranch.CHEN, HeavenlyStem.GUI, EarthlyBranch.HAI))
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("土水池沼格", r.name)
    }

    @Test fun `沼例二 戊戌癸亥癸亥己未 厚土围水`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.XU, HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.GUI, EarthlyBranch.HAI, HeavenlyStem.JI, EarthlyBranch.WEI))
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("土水池沼格", r.name)
    }

    @Test fun `沼例三 己未甲戌癸亥壬子 甲随己化`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.JI, EarthlyBranch.WEI, HeavenlyStem.JIA, EarthlyBranch.XU,
            HeavenlyStem.GUI, EarthlyBranch.HAI, HeavenlyStem.REN, EarthlyBranch.ZI))
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("土水池沼格", r.name)
    }

    private val chart = lazy {
        chartOf(
            HeavenlyStem.GUI, EarthlyBranch.WEI, HeavenlyStem.REN, EarthlyBranch.XU,
            HeavenlyStem.GUI, EarthlyBranch.HAI, HeavenlyStem.GUI, EarthlyBranch.HAI)
    }

    private fun vy(s: HeavenlyStem, b: EarthlyBranch): YunVerdict? =
        CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))?.verdict

    @Test fun `沼行运1 旺土为用（丙戌丁丑火生土亦旺土）旺水为忌`() {
        for ((s, b) in listOf(
            HeavenlyStem.WU to EarthlyBranch.XU, HeavenlyStem.WU to EarthlyBranch.CHEN,
            HeavenlyStem.JI to EarthlyBranch.CHOU, HeavenlyStem.JI to EarthlyBranch.WEI,
            HeavenlyStem.BING to EarthlyBranch.XU, HeavenlyStem.DING to EarthlyBranch.CHOU,
        )) assertEquals("(${s.chinese}${b.chinese}) 补弱土——应用神", YunVerdict.YONG, vy(s, b))
        for ((s, b) in listOf(
            HeavenlyStem.REN to EarthlyBranch.ZI, HeavenlyStem.GUI to EarthlyBranch.HAI,
            HeavenlyStem.GENG to EarthlyBranch.ZI, HeavenlyStem.XIN to EarthlyBranch.HAI,
        )) assertEquals("(${s.chinese}${b.chinese}) 旺水失衡——应忌", YunVerdict.JI, vy(s, b))
    }

    @Test fun `沼行运2 强木转正格（弱木水多木漂忌）`() {
        for ((s, b) in listOf(
            HeavenlyStem.JIA to EarthlyBranch.YIN, HeavenlyStem.YI to EarthlyBranch.MAO,
        )) assertEquals("(${s.chinese}${b.chinese}) 身旺食伤官杀弱转正格——应转正格", YunVerdict.HUAN_YUAN, vy(s, b))
        for ((s, b) in listOf(
            HeavenlyStem.JIA to EarthlyBranch.CHEN, HeavenlyStem.JIA to EarthlyBranch.XU,
            HeavenlyStem.YI to EarthlyBranch.WEI, HeavenlyStem.YI to EarthlyBranch.CHOU,
        )) assertEquals("(${s.chinese}${b.chinese}) 水多木漂——应忌", YunVerdict.JI, vy(s, b))
    }

    @Test fun `沼行运3 强火联合弱土转正格两停`() {
        for ((s, b) in listOf(
            HeavenlyStem.BING to EarthlyBranch.WU, HeavenlyStem.DING to EarthlyBranch.SI,
        )) assertEquals("(${s.chinese}${b.chinese}) 生扶弱土对抗日主两停——应转正格", YunVerdict.HUAN_YUAN, vy(s, b))
    }

    @Test fun `沼行运4 强金通关转三气取土金`() {
        for ((s, b) in listOf(
            HeavenlyStem.GENG to EarthlyBranch.SHEN, HeavenlyStem.XIN to EarthlyBranch.YOU,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 通土水之关——应转三气", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals("土金偏弱取土（弱行）", Element.EARTH, r.newYongShen)
        }
    }
}
