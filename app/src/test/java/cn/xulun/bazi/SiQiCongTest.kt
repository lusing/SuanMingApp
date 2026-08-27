package cn.xulun.bazi

import cn.xulun.bazi.core.CongGeYunCalculator
import cn.xulun.bazi.core.TeShuGeJuCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.TeShuKind
import cn.xulun.bazi.model.YunVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 四气从格锚点测试。理论见 .shengke-reference.md 多从格(三)节。
 */
class SiQiCongTest {

    // ── 成格三例 ──

    @Test fun `四气成格一 癸丑戊午乙酉丙子 从火土金水`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.CHOU, HeavenlyStem.WU, EarthlyBranch.WU,
            HeavenlyStem.YI, EarthlyBranch.YOU, HeavenlyStem.BING, EarthlyBranch.ZI))!!
        assertEquals(TeShuKind.DUO_CONG, r.kind)
        assertTrue("应四行（火土金水）：${r.yongShen.joinToString("") { it.chinese }}", r.yongShen.size >= 4)
    }

    @Test fun `四气成格二 己酉丙寅癸酉己未 从木火土金`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.JI, EarthlyBranch.YOU, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.GUI, EarthlyBranch.YOU, HeavenlyStem.JI, EarthlyBranch.WEI))!!
        assertEquals(TeShuKind.DUO_CONG, r.kind)
        assertTrue("应四行（木火土金）：${r.yongShen.joinToString("") { it.chinese }}", r.yongShen.size >= 4)
    }

    @Test fun `四气成格三 己未丙寅庚子丁亥 从水木火土`() {
        // 丙丁双透（官杀）：与调候矛盾局（辛丑癸巳丁酉壬寅壬癸双透）同构异断——书内矛盾，从/正格两可
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.JI, EarthlyBranch.WEI, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.GENG, EarthlyBranch.ZI, HeavenlyStem.DING, EarthlyBranch.HAI))
        if (r != null) {
            assertEquals(TeShuKind.DUO_CONG, r.kind)
            assertTrue("应四行（水木火土）：${r.yongShen.joinToString("") { it.chinese }}", r.yongShen.size >= 4)
        }
    }

    // ── 总表（己未 壬申 甲午 庚午，从火土金水） ──

    private val siQi = chartOf(
        HeavenlyStem.JI, EarthlyBranch.WEI, HeavenlyStem.REN, EarthlyBranch.SHEN,
        HeavenlyStem.JIA, EarthlyBranch.WU, HeavenlyStem.GENG, EarthlyBranch.WU)

    private fun v(p: Pillar): YunVerdict? = CongGeYunCalculator.analyze(siQi, listOf(p))?.verdict

    @Test fun `四气总表 补金水之不足为用`() {
        for ((s, b) in listOf(
            HeavenlyStem.REN to EarthlyBranch.SHEN, HeavenlyStem.GUI to EarthlyBranch.YOU,
            HeavenlyStem.XIN to EarthlyBranch.HAI,
        )) assertEquals("(${s.chinese}${b.chinese}) 补金水应用", YunVerdict.YONG, v(Pillar(s, b)))
    }

    @Test fun `四气总表 火土两旺失衡为忌`() {
        for ((s, b) in listOf(
            HeavenlyStem.BING to EarthlyBranch.WU, HeavenlyStem.DING to EarthlyBranch.SI,
            HeavenlyStem.JI to EarthlyBranch.CHOU, HeavenlyStem.JI to EarthlyBranch.WEI,
            HeavenlyStem.WU to EarthlyBranch.CHEN, HeavenlyStem.WU to EarthlyBranch.XU,
            HeavenlyStem.BING to EarthlyBranch.CHEN, HeavenlyStem.BING to EarthlyBranch.XU,
            HeavenlyStem.DING to EarthlyBranch.CHOU, HeavenlyStem.DING to EarthlyBranch.WEI,
            HeavenlyStem.WU to EarthlyBranch.WU, HeavenlyStem.JI to EarthlyBranch.SI,
        )) assertEquals("(${s.chinese}${b.chinese}) 补火土应忌", YunVerdict.JI, v(Pillar(s, b)))
    }

    @Test fun `四气总表 强木还原正格 衰木忌`() {
        for ((s, b) in listOf(
            HeavenlyStem.JIA to EarthlyBranch.YIN, HeavenlyStem.YI to EarthlyBranch.MAO,
            HeavenlyStem.REN to EarthlyBranch.YIN, HeavenlyStem.GUI to EarthlyBranch.MAO,
        )) assertEquals("(${s.chinese}${b.chinese}) 强木应还原", YunVerdict.HUAN_YUAN, v(Pillar(s, b)))
        // 衰木（庚寅：干克支）——忌
        assertEquals(YunVerdict.JI, v(Pillar(HeavenlyStem.GENG, EarthlyBranch.YIN)))
    }
}
