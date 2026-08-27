package cn.xulun.bazi

import cn.xulun.bazi.core.CongGeYunCalculator
import cn.xulun.bazi.core.TeShuGeJuCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.TeShuKind
import cn.xulun.bazi.model.YunVerdict
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 水木火三气成象格锚点（五格收官）。理论见 .shengke-reference.md 第五六节五。
 * 成格三例 + 行运总表（壬子 壬寅 甲寅 丙寅——木最强水次之火最弱）。
 */
class ShuiMuHuoSanQiTest {

    @Test fun `水木火例一 壬子癸卯丙午甲午 清纯不杂`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.BING, EarthlyBranch.WU, HeavenlyStem.JIA, EarthlyBranch.WU))
        assertEquals(TeShuKind.SAN_QI, r!!.kind)
        assertEquals("水木火三气成象", r.name)
    }

    @Test fun `水木火例二 癸卯丁巳甲子乙亥 木通关`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.MAO, HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.JIA, EarthlyBranch.ZI, HeavenlyStem.YI, EarthlyBranch.HAI))
        assertEquals(TeShuKind.SAN_QI, r!!.kind)
        assertEquals("水木火三气成象", r.name)
    }

    @Test fun `水木火例三 壬午壬寅甲子丙寅 拥木成象`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.REN, EarthlyBranch.WU, HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.ZI, HeavenlyStem.BING, EarthlyBranch.YIN))
        assertEquals(TeShuKind.SAN_QI, r!!.kind)
        assertEquals("水木火三气成象", r.name)
    }

    private val chart = lazy {
        chartOf(
            HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.BING, EarthlyBranch.YIN)
    }

    private fun vy(s: HeavenlyStem, b: EarthlyBranch): YunVerdict? =
        CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))?.verdict

    @Test fun `水木火行运1 旺火补弱泄木为用`() {
        for ((s, b) in listOf(
            HeavenlyStem.BING to EarthlyBranch.WU, HeavenlyStem.DING to EarthlyBranch.SI,
        )) assertEquals("(${s.chinese}${b.chinese}) 补弱火泄旺木——应用神", YunVerdict.YONG, vy(s, b))
    }

    @Test fun `水木火行运2 强土联合火转正格身财两停（火土联合最佳）`() {
        for ((s, b) in listOf(
            HeavenlyStem.WU to EarthlyBranch.CHEN, HeavenlyStem.WU to EarthlyBranch.XU,
            HeavenlyStem.JI to EarthlyBranch.CHOU, HeavenlyStem.JI to EarthlyBranch.WEI,
            HeavenlyStem.WU to EarthlyBranch.WU, HeavenlyStem.JI to EarthlyBranch.SI,
            HeavenlyStem.BING to EarthlyBranch.XU, HeavenlyStem.BING to EarthlyBranch.CHEN,
            HeavenlyStem.DING to EarthlyBranch.CHOU, HeavenlyStem.DING to EarthlyBranch.WEI,
        )) {
            val v = vy(s, b)
            org.junit.Assert.assertTrue("(${s.chinese}${b.chinese}) 旺土/火土联合转正格——非忌档：$v",
                v == YunVerdict.HUAN_YUAN || v == YunVerdict.ZHUAN_GE || v == YunVerdict.YONG)
        }
    }

    @Test fun `水木火行运3 强金联合火克泄旺木转正格流通`() {
        for ((s, b) in listOf(
            HeavenlyStem.GENG to EarthlyBranch.SHEN, HeavenlyStem.XIN to EarthlyBranch.YOU,
        )) assertEquals("(${s.chinese}${b.chinese}) 官印身伤流通正格——应转正格", YunVerdict.HUAN_YUAN, vy(s, b))
    }
}
