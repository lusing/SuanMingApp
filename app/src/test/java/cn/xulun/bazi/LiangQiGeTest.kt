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
 * 两气成象格·木火通明锚点。理论见 .shengke-reference.md 第五五节一。
 * 成格三例（日主参与两行均衡——区别于专旺/从格）+ 行运总表（丁卯 丙午 甲寅 丙寅）。
 */
class LiangQiGeTest {

    // ── (一)成格 ──

    @Test fun `通明例一 甲午丙寅丙寅甲午 四火化四木`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.JIA, EarthlyBranch.WU, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.BING, EarthlyBranch.YIN, HeavenlyStem.JIA, EarthlyBranch.WU))
        assertNotNull("木6火4均衡成象非独旺（专旺内分流）：$r", r)
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("木火通明格", r.name)
    }

    @Test fun `通明例二 癸卯戊午甲寅丙寅 戊癸合化去土水`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.MAO, HeavenlyStem.WU, EarthlyBranch.WU,
            HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.BING, EarthlyBranch.YIN))
        assertNotNull("局中只余木火两行各依强势：$r", r)
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("木火通明格", r.name)
    }

    @Test fun `通明例三 甲寅丙寅丙寅甲午 午非印比独立档`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.BING, EarthlyBranch.YIN, HeavenlyStem.JIA, EarthlyBranch.WU))
        assertNotNull("午为食伤不入专旺，两气独立档：$r", r)
        assertEquals(TeShuKind.LIANG_QI, r!!.kind)
        assertEquals("木火通明格", r.name)
    }

    // ── (二)行运喜忌总表（丁卯 丙午 甲寅 丙寅——火旺木弱） ──

    private val chart = lazy {
        chartOf(
            HeavenlyStem.DING, EarthlyBranch.MAO, HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.BING, EarthlyBranch.YIN)
    }

    private fun vy(s: HeavenlyStem, b: EarthlyBranch): YunVerdict? =
        CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))?.verdict

    @Test fun `通明行运1 补弱木为用（丁亥合木归行）`() {
        for ((s, b) in listOf(
            HeavenlyStem.JIA to EarthlyBranch.YIN, HeavenlyStem.YI to EarthlyBranch.MAO,
            HeavenlyStem.DING to EarthlyBranch.HAI,  // 亥与日时寅合化木
            HeavenlyStem.REN to EarthlyBranch.YIN, HeavenlyStem.GUI to EarthlyBranch.MAO,
        )) assertEquals("(${s.chinese}${b.chinese}) 补弱木——应用神", YunVerdict.YONG, vy(s, b))
    }

    @Test fun `通明行运2 强旺火运焚烧弱木为忌（壬午反化火）`() {
        for ((s, b) in listOf(
            HeavenlyStem.BING to EarthlyBranch.WU, HeavenlyStem.DING to EarthlyBranch.SI,
            HeavenlyStem.REN to EarthlyBranch.WU,  // 壬与年干丁反化为火
        )) assertEquals("(${s.chinese}${b.chinese}) 补偏旺火——应忌", YunVerdict.JI, vy(s, b))
    }

    @Test fun `通明行运3 强水转三气取水木`() {
        for ((s, b) in listOf(
            HeavenlyStem.REN to EarthlyBranch.ZI, HeavenlyStem.GUI to EarthlyBranch.HAI,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 水木火三气——应转格", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.WOOD, r.newYongShen) // 原局弱行仍为用（谁弱取谁）
        }
    }

    @Test fun `通明行运4 强金交战转正格取比印`() {
        for ((s, b) in listOf(
            HeavenlyStem.GENG to EarthlyBranch.SHEN, HeavenlyStem.XIN to EarthlyBranch.YOU,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 金木相战——应转正格", YunVerdict.HUAN_YUAN, r.verdict)
        }
    }

    @Test fun `通明行运5 强土转三气取比劫木（戊辰补会书内异断注记）`() {
        val r1 = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(HeavenlyStem.JI, EarthlyBranch.WEI)))!!
        assertEquals("己未木火土三气日主偏弱——应转格取木", YunVerdict.ZHUAN_GE, r1.verdict)
        assertEquals(Element.WOOD, r1.newYongShen)
        // 戊辰补全寅卯辰三会被吞——通明章谓强土/水灵章谓不列旺土（书内异断），引擎按合吞忌档注记
        val v = vy(HeavenlyStem.WU, EarthlyBranch.CHEN)
        org.junit.Assert.assertTrue("戊辰戊土（吉档或合吞注记）：$v", v == YunVerdict.ZHUAN_GE || v == YunVerdict.JI)
    }

    @Test fun `通明行运6 衰弱第三行逆乱为忌`() {
        for ((s, b) in listOf(
            HeavenlyStem.BING to EarthlyBranch.ZI,  // 干火支水互不相生
            HeavenlyStem.WU to EarthlyBranch.ZI,    // 干土支水相克
        )) assertEquals("(${s.chinese}${b.chinese}) 衰弱第三行——应忌", YunVerdict.JI, vy(s, b))
    }
}
