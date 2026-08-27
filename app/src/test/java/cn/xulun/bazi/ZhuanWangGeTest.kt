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
 * 专旺格锚点（第五四节）。成格三例（普通）+ 曲直格三例（特殊形式）+ 行运喜忌总表。
 * 行运例盘：癸卯 乙卯 乙卯 癸未——比劫用/印喜/官财食强转弱忌/己亥特例喜。
 */
class ZhuanWangGeTest {

    // ── (一)木专旺格成格（普通档，命名"木专旺格"） ──

    @Test fun `普通例一 癸卯乙卯甲寅乙亥 寅亥合卯寅会`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.MAO, HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.YI, EarthlyBranch.HAI))
        assertNotNull("应判专旺：$r", r)
        assertEquals(TeShuKind.ZHUAN_WANG, r!!.kind)
        assertEquals("无全局会合只六合半会——普通档木专旺格", "木专旺格", r.name)
    }

    @Test fun `普通例二 乙卯癸未乙卯丁亥 未背土向木丁坐亥截脚`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.YI, EarthlyBranch.MAO, HeavenlyStem.GUI, EarthlyBranch.WEI,
            HeavenlyStem.YI, EarthlyBranch.MAO, HeavenlyStem.DING, EarthlyBranch.HAI))
        assertNotNull("应判专旺（亥卯未三合、丁火坐亥截脚不破格）：$r", r)
        assertEquals(TeShuKind.ZHUAN_WANG, r!!.kind)
        assertEquals("月令未不在寅卯辰会宫——普通档", "木专旺格", r.name)
    }

    @Test fun `普通例三 癸卯乙卯乙卯癸未 三卯合未半合成局`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.MAO, HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.YI, EarthlyBranch.MAO, HeavenlyStem.GUI, EarthlyBranch.WEI))
        assertNotNull("应判专旺（卯未半合变节向木）：$r", r)
        assertEquals(TeShuKind.ZHUAN_WANG, r!!.kind)
        assertEquals("半合非全局——普通档", "木专旺格", r.name)
    }

    // ── (二)曲直格（特殊形式：月令会宫+三合/三会全局） ──

    @Test fun `曲直例一 癸卯甲寅甲辰乙亥 寅卯辰三会`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.MAO, HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.CHEN, HeavenlyStem.YI, EarthlyBranch.HAI))
        assertNotNull("应判专旺：$r", r)
        assertEquals("标准曲直格（月令寅+三会）", "曲直格", r!!.name)
    }

    @Test fun `曲直例二 癸未乙卯甲寅乙亥 亥卯未三合`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.WEI, HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.YI, EarthlyBranch.HAI))
        assertNotNull("应判专旺：$r", r)
        assertEquals("曲直格（月令卯+三合）", "曲直格", r!!.name)
    }

    @Test fun `曲直例三 壬寅甲辰乙卯丁亥 丁壬合木`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.REN, EarthlyBranch.YIN, HeavenlyStem.JIA, EarthlyBranch.CHEN,
            HeavenlyStem.YI, EarthlyBranch.MAO, HeavenlyStem.DING, EarthlyBranch.HAI))
        assertNotNull("应判专旺（丁坐亥截脚+丁壬化木均不破格）：$r", r)
        assertEquals("曲直格真（月令辰+三会）", "曲直格", r!!.name)
    }

    // ── (三)行运喜忌总表（癸卯 乙卯 乙卯 癸未） ──

    private fun yun(vararg ps: Pair<HeavenlyStem, EarthlyBranch>): List<Pillar> = ps.map { Pillar(it.first, it.second) }
    private val chart = lazy {
        chartOf(
            HeavenlyStem.GUI, EarthlyBranch.MAO, HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.YI, EarthlyBranch.MAO, HeavenlyStem.GUI, EarthlyBranch.WEI)
    }

    @Test fun `行运1 比劫覆载相生为用神`() {
        for ((gan, zhi) in listOf(
            HeavenlyStem.JIA to EarthlyBranch.YIN,   // 双体
            HeavenlyStem.YI to EarthlyBranch.MAO,    // 双体
            HeavenlyStem.JIA to EarthlyBranch.ZI,    // 比坐印
            HeavenlyStem.YI to EarthlyBranch.HAI,    // 比坐印
            HeavenlyStem.REN to EarthlyBranch.YIN,   // 印坐比（覆载相生之比劫运）
            HeavenlyStem.GUI to EarthlyBranch.MAO,   // 印坐比
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, yun(gan to zhi))
            assertNotNull("岁运${gan.chinese}${zhi.chinese}应判：$r", r)
            assertEquals("岁运${gan.chinese}${zhi.chinese}比劫运——用神到位：${r!!.basis}", YunVerdict.YONG, r.verdict)
        }
    }

    @Test fun `行运2 印绶双体为喜神`() {
        for ((gan, zhi) in listOf(
            HeavenlyStem.REN to EarthlyBranch.ZI,
            HeavenlyStem.GUI to EarthlyBranch.HAI,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, yun(gan to zhi))
            assertNotNull(r)
            assertEquals("岁运${gan.chinese}${zhi.chinese}印绶——喜神到位：${r!!.basis}", YunVerdict.XI, r.verdict)
        }
    }

    @Test fun `行运3 官杀强旺转身杀两停`() {
        for ((gan, zhi) in listOf(
            HeavenlyStem.GENG to EarthlyBranch.SHEN,  // 双体
            HeavenlyStem.XIN to EarthlyBranch.YOU,    // 双体
            HeavenlyStem.WU to EarthlyBranch.SHEN,    // 财覆载生官
            HeavenlyStem.JI to EarthlyBranch.YOU,
            HeavenlyStem.XIN to EarthlyBranch.CHOU,
            HeavenlyStem.GENG to EarthlyBranch.XU,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, yun(gan to zhi))
            assertNotNull(r)
            assertEquals("岁运${gan.chinese}${zhi.chinese}官杀强——转格取官杀：${r!!.basis}", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.METAL, r.newYongShen)
        }
    }

    @Test fun `行运4 官杀失辅为忌（庚辰辛未支被半会半合拉走）`() {
        for ((gan, zhi) in listOf(
            HeavenlyStem.GENG to EarthlyBranch.CHEN,  // 辰与三卯半会木，金失辰生
            HeavenlyStem.XIN to EarthlyBranch.WEI,    // 未与三卯半合木，金失未生
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, yun(gan to zhi))
            assertNotNull(r)
            assertEquals("岁运${gan.chinese}${zhi.chinese}官杀失辅——忌：${r!!.basis}", YunVerdict.JI, r.verdict)
        }
    }

    @Test fun `行运5 财星强旺转身财两停`() {
        for ((gan, zhi) in listOf(
            HeavenlyStem.WU to EarthlyBranch.XU,      // 双体
            HeavenlyStem.JI to EarthlyBranch.CHOU,    // 双体
            HeavenlyStem.BING to EarthlyBranch.XU,    // 食伤覆载生财
            HeavenlyStem.DING to EarthlyBranch.CHOU,
            HeavenlyStem.WU to EarthlyBranch.WU,
            HeavenlyStem.JI to EarthlyBranch.SI,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, yun(gan to zhi))
            assertNotNull(r)
            assertEquals("岁运${gan.chinese}${zhi.chinese}财星强——转格取财：${r!!.basis}", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.EARTH, r.newYongShen)
        }
    }

    @Test fun `行运6 财星失辅为忌（辰未被合土气减弱）`() {
        for ((gan, zhi) in listOf(
            HeavenlyStem.WU to EarthlyBranch.CHEN,
            HeavenlyStem.JI to EarthlyBranch.WEI,
            HeavenlyStem.BING to EarthlyBranch.CHEN,
            HeavenlyStem.DING to EarthlyBranch.WEI,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, yun(gan to zhi))
            assertNotNull(r)
            assertEquals("岁运${gan.chinese}${zhi.chinese}财星失辅破坏清纯——忌：${r!!.basis}", YunVerdict.JI, r.verdict)
        }
    }

    @Test fun `行运7 食伤覆载相生泄秀`() {
        for ((gan, zhi) in listOf(
            HeavenlyStem.BING to EarthlyBranch.WU,    // 双体
            HeavenlyStem.DING to EarthlyBranch.SI,    // 双体
            HeavenlyStem.BING to EarthlyBranch.YIN,   // 支生干
            HeavenlyStem.DING to EarthlyBranch.MAO,
            HeavenlyStem.JIA to EarthlyBranch.WU,     // 干生支
            HeavenlyStem.YI to EarthlyBranch.SI,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, yun(gan to zhi))
            assertNotNull(r)
            assertEquals("岁运${gan.chinese}${zhi.chinese}木火相辉——泄秀转格：${r!!.basis}", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.FIRE, r.newYongShen)
        }
    }

    @Test fun `行运8 食伤截脚盖头为忌（木多火窒）`() {
        for ((gan, zhi) in listOf(
            HeavenlyStem.BING to EarthlyBranch.SHEN,
            HeavenlyStem.DING to EarthlyBranch.YOU,
            HeavenlyStem.GENG to EarthlyBranch.WU,
            HeavenlyStem.XIN to EarthlyBranch.SI,
        )) {
            val r = CongGeYunCalculator.analyze(chart.value, yun(gan to zhi))
            assertNotNull(r)
            assertEquals("岁运${gan.chinese}${zhi.chinese}食伤力微——木多火窒忌：${r!!.basis}", YunVerdict.JI, r.verdict)
        }
    }

    @Test fun `行运9 己亥特例 支补全三合干土顺从为喜`() {
        val r = CongGeYunCalculator.analyze(chart.value, yun(HeavenlyStem.JI to EarthlyBranch.HAI))
        assertNotNull(r)
        assertEquals("亥补亥卯未三合+己土衰微顺从——喜用：${r!!.basis}", YunVerdict.XI, r.verdict)
    }
}
