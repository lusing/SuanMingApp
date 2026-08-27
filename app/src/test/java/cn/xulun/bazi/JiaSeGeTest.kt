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
 * 土专旺（稼穑）格锚点。理论见 .shengke-reference.md 第五四节三。
 * 成格三例（普通档，刑冲激旺/午未论合）+ 稼穑四例（三刑局或四库全）+ 行运总表（戊戌 己未 己丑 戊辰）。
 */
class JiaSeGeTest {

    // ── (一)土专旺格成格（普通档） ──

    @Test fun `普通例一 己丑己巳戊戌己未 丑未戌三刑月令巳`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.JI, EarthlyBranch.CHOU, HeavenlyStem.JI, EarthlyBranch.SI,
            HeavenlyStem.WU, EarthlyBranch.XU, HeavenlyStem.JI, EarthlyBranch.WEI))
        assertNotNull(r)
        assertEquals(TeShuKind.ZHUAN_WANG, r!!.kind)
        assertEquals("三刑激旺但月令巳不在辰戌丑未——普通档", "土专旺格", r.name)
    }

    @Test fun `普通例二 戊辰戊午己未戊辰 午未论合不论会`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.CHEN, HeavenlyStem.WU, EarthlyBranch.WU,
            HeavenlyStem.JI, EarthlyBranch.WEI, HeavenlyStem.WU, EarthlyBranch.CHEN))
        assertNotNull(r)
        assertEquals(TeShuKind.ZHUAN_WANG, r!!.kind)
        assertEquals("众土晦火午未合化土——普通档", "土专旺格", r.name)
    }

    @Test fun `普通例三 己丑甲戌戊戌丙辰 甲己合化土`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.JI, EarthlyBranch.CHOU, HeavenlyStem.JIA, EarthlyBranch.XU,
            HeavenlyStem.WU, EarthlyBranch.XU, HeavenlyStem.BING, EarthlyBranch.CHEN))
        assertNotNull("甲与年干己五合化土乘旺，不作破格论：$r", r)
        assertEquals(TeShuKind.ZHUAN_WANG, r!!.kind)
        assertEquals("丑戌刑辰戌冲但缺未非三刑非四库全——普通档", "土专旺格", r.name)
    }

    // ── (二)稼穑格（特殊形式：月令辰戌丑未+三刑局或四库全） ──

    @Test fun `稼穑例一 己丑甲戌戊辰己未 四库全`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.JI, EarthlyBranch.CHOU, HeavenlyStem.JIA, EarthlyBranch.XU,
            HeavenlyStem.WU, EarthlyBranch.CHEN, HeavenlyStem.JI, EarthlyBranch.WEI))
        assertEquals("稼穑格（月令戌+辰戌丑未土方局）", "稼穑格", r!!.name)
    }

    @Test fun `稼穑例二 戊戌己未己丑己巳 丑未戌三刑`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.XU, HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.JI, EarthlyBranch.CHOU, HeavenlyStem.JI, EarthlyBranch.SI))
        assertEquals("稼穑格（月令未+三刑局）", "稼穑格", r!!.name)
    }

    @Test fun `稼穑例三 戊戌己未己丑戊辰 支全四库`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.XU, HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.JI, EarthlyBranch.CHOU, HeavenlyStem.WU, EarthlyBranch.CHEN))
        assertEquals("稼穑格（干支皆土+四库全）", "稼穑格", r!!.name)
    }

    @Test fun `稼穑例四 己丑甲戌己未甲戌 两甲反化为土`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.JI, EarthlyBranch.CHOU, HeavenlyStem.JIA, EarthlyBranch.XU,
            HeavenlyStem.JI, EarthlyBranch.WEI, HeavenlyStem.JIA, EarthlyBranch.XU))
        assertNotNull("两甲与己五合化土贪合忘克：$r", r)
        assertEquals(TeShuKind.ZHUAN_WANG, r!!.kind)
        assertEquals("稼穑格（月令戌+丑戌未三刑）", "稼穑格", r.name)
    }

    // ── (三)行运喜忌总表（戊戌 己未 己丑 戊辰） ──

    private val chart = lazy {
        chartOf(
            HeavenlyStem.WU, EarthlyBranch.XU, HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.JI, EarthlyBranch.CHOU, HeavenlyStem.WU, EarthlyBranch.CHEN)
    }

    private fun vy(s: HeavenlyStem, b: EarthlyBranch): YunVerdict? =
        CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))?.verdict

    @Test fun `土行运1 比劫为用神`() {
        for ((s, b) in listOf(
            HeavenlyStem.WU to EarthlyBranch.CHEN, HeavenlyStem.WU to EarthlyBranch.XU,
            HeavenlyStem.JI to EarthlyBranch.CHOU, HeavenlyStem.JI to EarthlyBranch.WEI,
        )) assertEquals("(${s.chinese}${b.chinese}) 应用神", YunVerdict.YONG, vy(s, b))
    }

    @Test fun `土行运2 印绶为喜神（双体印严格喜，印比覆载喜用通称）`() {
        for ((s, b) in listOf(
            HeavenlyStem.BING to EarthlyBranch.WU, HeavenlyStem.DING to EarthlyBranch.SI,
        )) assertEquals("(${s.chinese}${b.chinese}) 双体印应喜", YunVerdict.XI, vy(s, b))
        for ((s, b) in listOf(
            HeavenlyStem.BING to EarthlyBranch.CHEN, HeavenlyStem.BING to EarthlyBranch.XU,
            HeavenlyStem.DING to EarthlyBranch.CHOU, HeavenlyStem.DING to EarthlyBranch.WEI,
            HeavenlyStem.WU to EarthlyBranch.WU, HeavenlyStem.JI to EarthlyBranch.SI,
        )) {
            val v = vy(s, b)
            assertNotNull(v)
            assertTrue2("(${s.chinese}${b.chinese}) 印比覆载应吉档（喜用通称）", v == YunVerdict.XI || v == YunVerdict.YONG)
        }
    }

    @Test fun `土行运3 官杀强旺转格`() {
        for ((s, b) in listOf(
            HeavenlyStem.JIA to EarthlyBranch.YIN, HeavenlyStem.YI to EarthlyBranch.MAO,
            HeavenlyStem.REN to EarthlyBranch.YIN, HeavenlyStem.GUI to EarthlyBranch.MAO,
            HeavenlyStem.YI to EarthlyBranch.HAI,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 应转格", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.WOOD, r.newYongShen)
        }
    }

    @Test fun `土行运4 甲子天地合格局未变仍取土`() {
        val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(HeavenlyStem.JIA, EarthlyBranch.ZI)))
        assertNotNull(r)
        assertEquals("甲子与日柱己丑天地合化土——仍取土为用：${r!!.basis}", YunVerdict.YONG, r.verdict)
    }

    @Test fun `土行运5 甲辰甲戌合化不以忌论`() {
        for ((s, b) in listOf(
            HeavenlyStem.JIA to EarthlyBranch.CHEN, HeavenlyStem.JIA to EarthlyBranch.XU,
        )) {
            val v = vy(s, b)
            assertNotNull(v)
            assertTrue2("(${s.chinese}${b.chinese}) 甲合己化土应吉档不以忌论", v == YunVerdict.YONG || v == YunVerdict.XI)
        }
    }

    @Test fun `土行运6 官杀力弱为忌`() {
        for ((s, b) in listOf(
            HeavenlyStem.WU to EarthlyBranch.YIN, HeavenlyStem.YI to EarthlyBranch.CHOU,
            HeavenlyStem.YI to EarthlyBranch.WEI,
        )) assertEquals("(${s.chinese}${b.chinese}) 力量不足应忌", YunVerdict.JI, vy(s, b))
    }

    @Test fun `土行运7 财星强旺转格`() {
        for ((s, b) in listOf(
            HeavenlyStem.GUI to EarthlyBranch.HAI, HeavenlyStem.XIN to EarthlyBranch.HAI,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 应转格", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.WATER, r.newYongShen)
        }
    }

    @Test fun `土行运8 壬子庚子子丑合住财减为忌`() {
        for ((s, b) in listOf(
            HeavenlyStem.REN to EarthlyBranch.ZI, HeavenlyStem.GENG to EarthlyBranch.ZI,
        )) assertEquals("(${s.chinese}${b.chinese}) 子水与原局丑合化财减——应忌", YunVerdict.JI, vy(s, b))
    }

    @Test fun `土行运9 食伤强旺泄秀`() {
        for ((s, b) in listOf(
            HeavenlyStem.GENG to EarthlyBranch.SHEN, HeavenlyStem.XIN to EarthlyBranch.YOU,
            HeavenlyStem.WU to EarthlyBranch.SHEN, HeavenlyStem.JI to EarthlyBranch.YOU,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 应转格泄秀", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.METAL, r.newYongShen)
        }
    }

    private fun assertTrue2(msg: String, cond: Boolean) = org.junit.Assert.assertTrue(msg, cond)
}
