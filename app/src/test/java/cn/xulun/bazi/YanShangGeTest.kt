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
 * 火专旺（炎上）格锚点。理论见 .shengke-reference.md 第五四节二。
 * 成格三例（普通档）+ 炎上四例（特殊形式）+ 行运喜忌总表（丁巳 丙午 丁未 丙午）。
 */
class YanShangGeTest {

    // ── (一)火专旺格成格（普通档） ──

    @Test fun `普通例一 丁巳丙午丁巳丙午 干支皆火`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.DING, EarthlyBranch.SI, HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.DING, EarthlyBranch.SI, HeavenlyStem.BING, EarthlyBranch.WU))
        assertNotNull(r)
        assertEquals(TeShuKind.ZHUAN_WANG, r!!.kind)
        assertEquals("无会局——普通档", "火专旺格", r.name)
    }

    @Test fun `普通例二 乙巳丙戌丙寅甲午 寅午戌三合月令戌`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.YI, EarthlyBranch.SI, HeavenlyStem.BING, EarthlyBranch.XU,
            HeavenlyStem.BING, EarthlyBranch.YIN, HeavenlyStem.JIA, EarthlyBranch.WU))
        assertNotNull(r)
        assertEquals(TeShuKind.ZHUAN_WANG, r!!.kind)
        assertEquals("月令戌不在巳午未会宫——普通档（木土随火势）", "火专旺格", r.name)
    }

    @Test fun `普通例三 丙午戊戌丙午癸巳 癸合戊乘旺化火不破格`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.BING, EarthlyBranch.WU, HeavenlyStem.WU, EarthlyBranch.XU,
            HeavenlyStem.BING, EarthlyBranch.WU, HeavenlyStem.GUI, EarthlyBranch.SI))
        assertNotNull("时干癸水与月干戊五合化火乘旺，不作破格论：$r", r)
        assertEquals(TeShuKind.ZHUAN_WANG, r!!.kind)
        assertEquals("两午夹戌半局成化——普通档", "火专旺格", r.name)
    }

    // ── (二)炎上格（特殊形式：月令巳午未+三会/三合） ──

    @Test fun `炎上例一 丁巳丙午丁未丙午 支会火局`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.DING, EarthlyBranch.SI, HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.DING, EarthlyBranch.WEI, HeavenlyStem.BING, EarthlyBranch.WU))
        assertEquals("炎上格（月令午+巳午未三会）", "炎上格", r!!.name)
    }

    @Test fun `炎上例二 丙戌甲午丙寅甲午 三合成化多一中神`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.BING, EarthlyBranch.XU, HeavenlyStem.JIA, EarthlyBranch.WU,
            HeavenlyStem.BING, EarthlyBranch.YIN, HeavenlyStem.JIA, EarthlyBranch.WU))
        assertEquals("炎上格（月令午+寅午戌三合）", "炎上格", r!!.name)
    }

    @Test fun `炎上例三 丁巳丁未丁巳丙午 季夏余气会局`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.DING, EarthlyBranch.SI, HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.DING, EarthlyBranch.SI, HeavenlyStem.BING, EarthlyBranch.WU))
        assertEquals("炎上格（月令未+巳午未三会）", "炎上格", r!!.name)
    }

    @Test fun `炎上例四 壬午丙午丁巳丙午 壬合丁贪合忘克`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.REN, EarthlyBranch.WU, HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.DING, EarthlyBranch.SI, HeavenlyStem.BING, EarthlyBranch.WU))
        assertNotNull("壬水与日干丁五合而化神（木）无力——合而不化贪合忘克：$r", r)
        assertEquals(TeShuKind.ZHUAN_WANG, r!!.kind)
        // 书称炎上（一片火海），然无会局——引擎按普通档命名，喜忌同梯（书内从宽之异档）
        assertEquals("火专旺格", r.name)
    }

    // ── (三)行运喜忌总表（丁巳 丙午 丁未 丙午） ──

    private val chart = lazy {
        chartOf(
            HeavenlyStem.DING, EarthlyBranch.SI, HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.DING, EarthlyBranch.WEI, HeavenlyStem.BING, EarthlyBranch.WU)
    }

    private fun vy(s: HeavenlyStem, b: EarthlyBranch): YunVerdict? =
        CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))?.verdict

    @Test fun `火行运1 比劫覆载相生为用神`() {
        for ((s, b) in listOf(
            HeavenlyStem.BING to EarthlyBranch.WU, HeavenlyStem.DING to EarthlyBranch.SI,
            HeavenlyStem.BING to EarthlyBranch.YIN, HeavenlyStem.DING to EarthlyBranch.MAO,
            HeavenlyStem.JIA to EarthlyBranch.WU, HeavenlyStem.YI to EarthlyBranch.SI,
        )) assertEquals("(${s.chinese}${b.chinese}) 应用神", YunVerdict.YONG, vy(s, b))
    }

    @Test fun `火行运2 印绶为喜神`() {
        for ((s, b) in listOf(
            HeavenlyStem.JIA to EarthlyBranch.YIN, HeavenlyStem.YI to EarthlyBranch.MAO,
            HeavenlyStem.REN to EarthlyBranch.YIN, HeavenlyStem.GUI to EarthlyBranch.MAO,
        )) assertEquals("(${s.chinese}${b.chinese}) 应喜神（印扶旺）", YunVerdict.XI, vy(s, b))
    }

    @Test fun `火行运3 官杀强旺转格取财官`() {
        for ((s, b) in listOf(
            HeavenlyStem.REN to EarthlyBranch.ZI, HeavenlyStem.GUI to EarthlyBranch.HAI,
            HeavenlyStem.GENG to EarthlyBranch.ZI, HeavenlyStem.XIN to EarthlyBranch.HAI,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 应转格", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.WATER, r.newYongShen)
        }
    }

    @Test fun `火行运4 官杀力弱为第一忌神`() {
        for ((s, b) in listOf(
            HeavenlyStem.REN to EarthlyBranch.WU, HeavenlyStem.GUI to EarthlyBranch.SI,
            HeavenlyStem.BING to EarthlyBranch.ZI, HeavenlyStem.DING to EarthlyBranch.HAI,
        )) assertEquals("(${s.chinese}${b.chinese}) 应忌（激犯旺气）", YunVerdict.JI, vy(s, b))
    }

    @Test fun `火行运5 财星强旺转格`() {
        for ((s, b) in listOf(
            HeavenlyStem.GENG to EarthlyBranch.SHEN, HeavenlyStem.XIN to EarthlyBranch.YOU,
            HeavenlyStem.WU to EarthlyBranch.SHEN, HeavenlyStem.JI to EarthlyBranch.YOU,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 应转格", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.METAL, r.newYongShen)
        }
    }

    @Test fun `火行运6 财星被合克损为忌（庚戌辛未半合火局）`() {
        for ((s, b) in listOf(
            HeavenlyStem.GENG to EarthlyBranch.XU, HeavenlyStem.XIN to EarthlyBranch.WEI,
        )) assertEquals("(${s.chinese}${b.chinese}) 戌未与原局半合火克损财星——应忌", YunVerdict.JI, vy(s, b))
    }

    @Test fun `火行运7 食伤强旺泄秀`() {
        for ((s, b) in listOf(
            HeavenlyStem.WU to EarthlyBranch.CHEN, HeavenlyStem.JI to EarthlyBranch.CHOU,
            HeavenlyStem.BING to EarthlyBranch.CHEN, HeavenlyStem.DING to EarthlyBranch.CHOU,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 应转格泄秀", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.EARTH, r.newYongShen)
        }
    }

    @Test fun `火行运8 食伤被合火多土焦为忌`() {
        for ((s, b) in listOf(
            HeavenlyStem.WU to EarthlyBranch.XU, HeavenlyStem.JI to EarthlyBranch.WEI,
            HeavenlyStem.BING to EarthlyBranch.XU, HeavenlyStem.DING to EarthlyBranch.WEI,
        )) assertEquals("(${s.chinese}${b.chinese}) 戌未半合火局——火多土焦应忌", YunVerdict.JI, vy(s, b))
    }
}
