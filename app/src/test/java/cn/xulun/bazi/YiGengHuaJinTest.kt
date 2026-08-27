package cn.xulun.bazi

import cn.xulun.bazi.core.CongGeYunCalculator
import cn.xulun.bazi.core.TeShuGeJuCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.YunVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 乙庚化金格锚点。理论见 .shengke-reference.md 化气格(二)节。
 * 庚日=从革专旺通道（五合克透豁免），乙日=从杀通道（官杀有力不阻）。
 */
class YiGengHuaJinTest {

    // ── 成格三例 ──

    @Test fun `金例一 庚申乙酉庚戌庚辰 三庚合乙从革`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GENG, EarthlyBranch.SHEN, HeavenlyStem.YI, EarthlyBranch.YOU,
            HeavenlyStem.GENG, EarthlyBranch.XU, HeavenlyStem.GENG, EarthlyBranch.CHEN))!!
        assertEquals(cn.xulun.bazi.model.TeShuKind.ZHUAN_WANG, r.kind)
        assertTrue("应从革（乙庚合化金）：${r.name}", r.name.contains("从革"))
    }

    @Test fun `金例二 乙丑辛巳庚申乙酉 巳酉丑合金争合从革`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.YI, EarthlyBranch.CHOU, HeavenlyStem.XIN, EarthlyBranch.SI,
            HeavenlyStem.GENG, EarthlyBranch.SHEN, HeavenlyStem.YI, EarthlyBranch.YOU))!!
        assertEquals(cn.xulun.bazi.model.TeShuKind.ZHUAN_WANG, r.kind)
    }

    @Test fun `金例三 庚申乙酉乙酉庚辰 乙日从杀`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GENG, EarthlyBranch.SHEN, HeavenlyStem.YI, EarthlyBranch.YOU,
            HeavenlyStem.YI, EarthlyBranch.YOU, HeavenlyStem.GENG, EarthlyBranch.CHEN))!!
        assertTrue("乙日化金应从杀：${r.name}", r.name.contains("从杀"))
    }

    // ── 岁运总表（例一 庚日专旺论法 / 例二 乙日从杀论法） ──

    @Test fun `庚日化金 旺金用 土喜 强火转`() {
        val b = chartOf(
            HeavenlyStem.XIN, EarthlyBranch.YOU, HeavenlyStem.WU, EarthlyBranch.XU,
            HeavenlyStem.GENG, EarthlyBranch.SHEN, HeavenlyStem.YI, EarthlyBranch.YOU)
        val ts = TeShuGeJuCalculator.analyze(b)
        assertNotNull("应判从革：$ts", ts)
        assertEquals(YunVerdict.YONG, CongGeYunCalculator.analyze(b, listOf(Pillar(HeavenlyStem.XIN, EarthlyBranch.CHOU)))?.verdict)
        assertEquals(YunVerdict.ZHUAN_GE, CongGeYunCalculator.analyze(b, listOf(Pillar(HeavenlyStem.BING, EarthlyBranch.WU)))?.verdict)
        // 壬子强水：化泄旺金引秀——教材"金水两气取水"，吉档（用神/转格皆通——水在专旺势内为泄秀）
        val rs = CongGeYunCalculator.analyze(b, listOf(Pillar(HeavenlyStem.REN, EarthlyBranch.ZI)))?.verdict
        assertTrue("壬子应吉档：$rs", rs == YunVerdict.YONG || rs == YunVerdict.ZHUAN_GE || rs == YunVerdict.XI)
    }

    @Test fun `乙日化金 旺金用 强木助身抗合化转`() {
        val b = chartOf(
            HeavenlyStem.XIN, EarthlyBranch.YOU, HeavenlyStem.XIN, EarthlyBranch.CHOU,
            HeavenlyStem.YI, EarthlyBranch.YOU, HeavenlyStem.GENG, EarthlyBranch.CHEN)
        val ts = TeShuGeJuCalculator.analyze(b)
        assertNotNull("应判从杀：$ts", ts)
        assertEquals(YunVerdict.YONG, CongGeYunCalculator.analyze(b, listOf(Pillar(HeavenlyStem.GENG, EarthlyBranch.SHEN)))?.verdict)
        // 甲寅强木：助日主抗金之合化 → 金木两气（转格/还原）
        val r = CongGeYunCalculator.analyze(b, listOf(Pillar(HeavenlyStem.JIA, EarthlyBranch.YIN)))
        assertTrue("甲寅应转/还原：${r?.verdict}", r == null || r.verdict == YunVerdict.ZHUAN_GE || r.verdict == YunVerdict.HUAN_YUAN)
    }
}
