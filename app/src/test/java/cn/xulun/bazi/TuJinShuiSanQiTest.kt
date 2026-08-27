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
 * 土金水三气成象格锚点。理论见 .shengke-reference.md 第五六节三。
 * 成格三例 + 行运总表（辛丑 戊戌 壬子 己酉——土金两强水略偏弱）。
 */
class TuJinShuiSanQiTest {

    @Test fun `土金水例一 戊戌庚申壬子戊申 起年收日`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.XU, HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.WU, EarthlyBranch.SHEN))
        assertEquals(TeShuKind.SAN_QI, r!!.kind)
        assertEquals("土金水三气成象", r.name)
    }

    @Test fun `土金水例二 辛丑辛丑戊申壬子 顺化清纯`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.XIN, EarthlyBranch.CHOU, HeavenlyStem.XIN, EarthlyBranch.CHOU,
            HeavenlyStem.WU, EarthlyBranch.SHEN, HeavenlyStem.REN, EarthlyBranch.ZI))
        assertEquals(TeShuKind.SAN_QI, r!!.kind)
        assertEquals("土金水三气成象", r.name)
    }

    @Test fun `土金水例三 癸亥庚申己未戊辰 土厚水清`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.HAI, HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.JI, EarthlyBranch.WEI, HeavenlyStem.WU, EarthlyBranch.CHEN))
        assertEquals(TeShuKind.SAN_QI, r!!.kind)
        assertEquals("土金水三气成象", r.name)
    }

    private val chart = lazy {
        chartOf(
            HeavenlyStem.XIN, EarthlyBranch.CHOU, HeavenlyStem.WU, EarthlyBranch.XU,
            HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.JI, EarthlyBranch.YOU)
    }

    private fun vy(s: HeavenlyStem, b: EarthlyBranch): YunVerdict? =
        CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))?.verdict

    @Test fun `土金水行运1 旺水补弱为用土金失衡为忌`() {
        for ((s, b) in listOf(
            HeavenlyStem.REN to EarthlyBranch.ZI, HeavenlyStem.GUI to EarthlyBranch.HAI,
            HeavenlyStem.GENG to EarthlyBranch.ZI, HeavenlyStem.XIN to EarthlyBranch.HAI,
        )) assertEquals("(${s.chinese}${b.chinese}) 补弱水——应用神", YunVerdict.YONG, vy(s, b))
        for ((s, b) in listOf(
            HeavenlyStem.WU to EarthlyBranch.XU, HeavenlyStem.WU to EarthlyBranch.CHEN,
            HeavenlyStem.JI to EarthlyBranch.WEI, HeavenlyStem.JI to EarthlyBranch.CHOU,
            HeavenlyStem.GENG to EarthlyBranch.SHEN, HeavenlyStem.XIN to EarthlyBranch.YOU,
            HeavenlyStem.WU to EarthlyBranch.SHEN, HeavenlyStem.JI to EarthlyBranch.YOU,
        )) assertEquals("(${s.chinese}${b.chinese}) 补有余土金——应忌", YunVerdict.JI, vy(s, b))
    }

    @Test fun `土金水行运2 强木转正格取印绶`() {
        for ((s, b) in listOf(
            HeavenlyStem.JIA to EarthlyBranch.YIN, HeavenlyStem.YI to EarthlyBranch.MAO,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 身弱官杀食伤旺——应转正格", YunVerdict.HUAN_YUAN, r.verdict)
            assertEquals("化官制食伤生身", Element.METAL, r.newYongShen)
        }
    }

    @Test fun `土金水行运3 强火转正格取印绶`() {
        for ((s, b) in listOf(
            HeavenlyStem.BING to EarthlyBranch.WU, HeavenlyStem.JIA to EarthlyBranch.WU,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 身弱财官旺——应转正格", YunVerdict.HUAN_YUAN, r.verdict)
            assertEquals("化官杀生身纠偏", Element.METAL, r.newYongShen)
        }
    }
}
