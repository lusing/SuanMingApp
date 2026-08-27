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
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 水专旺（润下）格锚点。理论见 .shengke-reference.md 第五四节五。
 * 含夜子时两例（BaziCalculator 夜子正法：23-24 点日柱不变、时干按次日干推）。
 */
class RunXiaGeTest {

    // ── (一)水专旺格成格（普通档） ──

    @Test fun `普通例一 癸亥四柱纯水`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.HAI, HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.GUI, EarthlyBranch.HAI, HeavenlyStem.GUI, EarthlyBranch.HAI))
        assertEquals(TeShuKind.ZHUAN_WANG, r!!.kind)
        assertEquals("无会局——普通档", "水专旺格", r.name)
    }

    @Test fun `普通例二 壬子四柱夜子时`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.REN, EarthlyBranch.ZI))
        assertEquals(TeShuKind.ZHUAN_WANG, r!!.kind)
        assertEquals("干支纯水——普通档", "水专旺格", r.name)
    }

    @Test fun `普通例三 辛亥壬辰壬申壬子 申子辰三合金随水势`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.XIN, EarthlyBranch.HAI, HeavenlyStem.REN, EarthlyBranch.CHEN,
            HeavenlyStem.REN, EarthlyBranch.SHEN, HeavenlyStem.REN, EarthlyBranch.ZI))
        assertNotNull("夜子时例：$r", r)
        assertEquals(TeShuKind.ZHUAN_WANG, r!!.kind)
        assertEquals("月令辰不在亥子丑——普通档", "水专旺格", r.name)
    }

    // ── (二)润下格 ──

    @Test fun `润下例一 癸亥癸亥癸丑壬子 亥子丑三会`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.HAI, HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.GUI, EarthlyBranch.CHOU, HeavenlyStem.REN, EarthlyBranch.ZI))
        assertEquals("润下格（月令亥+亥子丑三会）", "润下格", r!!.name)
    }

    @Test fun `润下例二 壬申壬子壬辰辛亥 申子辰三合`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.REN, EarthlyBranch.SHEN, HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.REN, EarthlyBranch.CHEN, HeavenlyStem.XIN, EarthlyBranch.HAI))
        assertEquals("润下格（月令子+申子辰三合）", "润下格", r!!.name)
    }

    @Test fun `润下例三 壬子癸丑癸亥壬子 丑月三会`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.GUI, EarthlyBranch.HAI, HeavenlyStem.REN, EarthlyBranch.ZI))
        assertEquals("润下格（月令丑+亥子丑三会）", "润下格", r!!.name)
    }

    // ── (三)行运喜忌总表（壬子 壬子 壬子 壬子 夜子时） ──

    private val chart = lazy {
        chartOf(
            HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.REN, EarthlyBranch.ZI)
    }

    private fun vy(s: HeavenlyStem, b: EarthlyBranch): YunVerdict? =
        CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))?.verdict

    @Test fun `水行运1 比劫为用神`() {
        for ((s, b) in listOf(
            HeavenlyStem.REN to EarthlyBranch.ZI, HeavenlyStem.GUI to EarthlyBranch.HAI,
            HeavenlyStem.GENG to EarthlyBranch.ZI, HeavenlyStem.XIN to EarthlyBranch.HAI,
        )) assertEquals("(${s.chinese}${b.chinese}) 应用神", YunVerdict.YONG, vy(s, b))
    }

    @Test fun `水行运2 印绶为喜神（双体印严格喜，印比覆载通称）`() {
        for ((s, b) in listOf(
            HeavenlyStem.GENG to EarthlyBranch.SHEN, HeavenlyStem.XIN to EarthlyBranch.YOU,
        )) assertEquals("(${s.chinese}${b.chinese}) 双体印应喜", YunVerdict.XI, vy(s, b))
        for ((s, b) in listOf(
            HeavenlyStem.REN to EarthlyBranch.SHEN, HeavenlyStem.GUI to EarthlyBranch.YOU,
        )) {
            val v = vy(s, b)
            assertNotNull(v)
            assertTrue("(${s.chinese}${b.chinese}) 印比覆载应吉档", v == YunVerdict.XI || v == YunVerdict.YONG)
        }
    }

    @Test fun `水行运3 官杀强旺转格`() {
        for ((s, b) in listOf(
            HeavenlyStem.WU to EarthlyBranch.XU, HeavenlyStem.JI to EarthlyBranch.WEI,
            HeavenlyStem.BING to EarthlyBranch.XU, HeavenlyStem.DING to EarthlyBranch.WEI,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 应转格", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.EARTH, r.newYongShen)
        }
    }

    @Test fun `水行运4 辰丑被合官杀力弱为忌`() {
        for ((s, b) in listOf(
            HeavenlyStem.WU to EarthlyBranch.CHEN, HeavenlyStem.JI to EarthlyBranch.CHOU,
            HeavenlyStem.BING to EarthlyBranch.CHEN, HeavenlyStem.DING to EarthlyBranch.CHOU,
        )) assertEquals("(${s.chinese}${b.chinese}) 辰丑半合水局官杀弱——应忌", YunVerdict.JI, vy(s, b))
    }

    @Test fun `水行运5 财星强旺转格（丁酉丙申不以忌论）`() {
        for ((s, b) in listOf(
            HeavenlyStem.BING to EarthlyBranch.WU, HeavenlyStem.DING to EarthlyBranch.SI,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 应转格", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.FIRE, r.newYongShen)
        }
        for ((s, b) in listOf(
            HeavenlyStem.DING to EarthlyBranch.YOU, HeavenlyStem.BING to EarthlyBranch.SHEN,
        )) {
            val v = vy(s, b)
            assertNotNull(v)
            assertTrue("(${s.chinese}${b.chinese}) 丁壬反化/申子合水——不以忌论（${v}）",
                v == YunVerdict.YONG || v == YunVerdict.XI)
        }
    }

    @Test fun `水行运6 食伤强旺泄秀`() {
        for ((s, b) in listOf(
            HeavenlyStem.JIA to EarthlyBranch.YIN, HeavenlyStem.YI to EarthlyBranch.MAO,
            HeavenlyStem.REN to EarthlyBranch.YIN, HeavenlyStem.GUI to EarthlyBranch.MAO,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 应转格泄秀", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.WOOD, r.newYongShen)
        }
    }
}
