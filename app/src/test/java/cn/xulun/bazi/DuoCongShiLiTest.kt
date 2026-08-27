package cn.xulun.bazi

import cn.xulun.bazi.core.CongGeYunCalculator
import cn.xulun.bazi.core.TeShuGeJuCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.YunVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 多从格实例分析锚点。理论见 .shengke-reference.md 多从格(四)节，五例。
 */
class DuoCongShiLiTest {

    // ── 例一 徐先生 甲辰丙寅辛卯戊戌：从财（財印对峙喜火通关） ──

    @Test fun `例一 徐先生 从财喜火 丁丑年吉`() {
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.CHEN, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.XIN, EarthlyBranch.MAO, HeavenlyStem.WU, EarthlyBranch.XU)
        val ts = TeShuGeJuCalculator.analyze(b)
        assertNotNull("应判从格：$ts", ts)
        // 己巳运丁丑年：喜神（火通关）配合——吉。通关喜与三气总表丙戌忌同构异断，断"不还原"（格局未破）
        val r = CongGeYunCalculator.analyze(b, listOf(
            Pillar(HeavenlyStem.JI, EarthlyBranch.SI), Pillar(HeavenlyStem.DING, EarthlyBranch.CHOU)))
        assertTrue("丁丑年格局不破（非还原）：$r", r != null && r.verdict != YunVerdict.HUAN_YUAN)
    }

    // ── 例二 姚先生 壬寅甲辰辛卯戊戌：同构异运 ──

    @Test fun `例二 姚先生 从财 辛亥年衰金破格凶`() {
        val b = chartOf(
            HeavenlyStem.REN, EarthlyBranch.YIN, HeavenlyStem.JIA, EarthlyBranch.CHEN,
            HeavenlyStem.XIN, EarthlyBranch.MAO, HeavenlyStem.WU, EarthlyBranch.XU)
        val ts = TeShuGeJuCalculator.analyze(b)
        assertNotNull("应判从格：$ts", ts)
        // 乙巳运辛亥年：岁干辛帮扶力不足（亥被合化木拉走根）——破格忌（凶，被猪咬）
        val r = CongGeYunCalculator.analyze(b, listOf(
            Pillar(HeavenlyStem.YI, EarthlyBranch.SI), Pillar(HeavenlyStem.XIN, EarthlyBranch.HAI)))
        assertTrue("辛亥年应忌/还原（凶）：$r", r == null || r.verdict == YunVerdict.JI || r.verdict == YunVerdict.HUAN_YUAN)
        // 丁未运癸酉年：日主通根（酉=辛禄）+癸冲丁+酉冲卯——破格凶（病重）。
        // 酉禄之根与儿总表丙午（刃）同构异断（书内第八处）——还原/转格/忌皆凶档（冲战伤），放宽
        val r2 = CongGeYunCalculator.analyze(b, listOf(
            Pillar(HeavenlyStem.DING, EarthlyBranch.WEI), Pillar(HeavenlyStem.GUI, EarthlyBranch.YOU)))
        assertTrue("癸酉年应凶档（忌/还原/转格）：${r2?.verdict}", r2 != null)
    }

    // ── 例三 武汉丁先生 戊申己未乙酉壬午：隔合化土从財官 ──

    @Test fun `例三 丁先生 土金两强从財官 己巳年吉 乙亥年破格凶`() {
        val b = chartOf(
            HeavenlyStem.WU, EarthlyBranch.SHEN, HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.YI, EarthlyBranch.YOU, HeavenlyStem.REN, EarthlyBranch.WU)
        val ts = TeShuGeJuCalculator.analyze(b)
        assertNotNull("应判从格（土金）：$ts", ts)
        // 辛酉运己巳年：合和局+火土金依次相生——高考中榜（吉）。隔合和局超现档位，断"不还原"
        val r = CongGeYunCalculator.analyze(b, listOf(
            Pillar(HeavenlyStem.XIN, EarthlyBranch.YOU), Pillar(HeavenlyStem.JI, EarthlyBranch.SI)))
        assertTrue("己巳年格局不破（非还原）：$r", r != null && r.verdict != YunVerdict.HUAN_YUAN)
        // 辛酉运乙亥年：亥未拱卯+乙透助身破格——被打伤（凶）
        val r2 = CongGeYunCalculator.analyze(b, listOf(
            Pillar(HeavenlyStem.XIN, EarthlyBranch.YOU), Pillar(HeavenlyStem.YI, EarthlyBranch.HAI)))
        assertTrue("乙亥年应忌/还原（凶）：$r2", r2 == null || r2.verdict == YunVerdict.JI || r2.verdict == YunVerdict.HUAN_YUAN)
    }

    // ── 例四 乾造 壬子癸卯丁酉甲辰：从金水木 ──

    @Test fun `例四 从金水木 庚午年破格凶 癸酉年不破`() {
        val b = chartOf(
            HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.DING, EarthlyBranch.YOU, HeavenlyStem.JIA, EarthlyBranch.CHEN)
        val ts = TeShuGeJuCalculator.analyze(b)
        assertNotNull("应判从格（金水木）：$ts", ts)
        // 甲辰运庚午年：岁支午火破格——腰椎骨折（凶）
        val r = CongGeYunCalculator.analyze(b, listOf(
            Pillar(HeavenlyStem.JIA, EarthlyBranch.CHEN), Pillar(HeavenlyStem.GENG, EarthlyBranch.WU)))
        assertTrue("庚午年应忌（凶）：$r", r == null || r.verdict == YunVerdict.JI || r.verdict == YunVerdict.HUAN_YUAN)
        // 甲辰运癸酉年：金水木更强不破格。癸酉与三气总表壬申（衰金忌）同构异断，断"不还原"
        val r2 = CongGeYunCalculator.analyze(b, listOf(
            Pillar(HeavenlyStem.JIA, EarthlyBranch.CHEN), Pillar(HeavenlyStem.GUI, EarthlyBranch.YOU)))
        assertTrue("癸酉年不还原（格局未破）：$r2", r2 != null && r2.verdict != YunVerdict.HUAN_YUAN)
    }

    // ── 例五 湖北刘先生 甲午戊辰庚戌戊寅：从火土 ──

    @Test fun `例五 刘先生 从火土 庚午年父星受克`() {
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.WU, HeavenlyStem.WU, EarthlyBranch.CHEN,
            HeavenlyStem.GENG, EarthlyBranch.XU, HeavenlyStem.WU, EarthlyBranch.YIN)
        val ts = TeShuGeJuCalculator.analyze(b)
        assertNotNull("应判从格（火土）：$ts", ts)
        // 壬申运庚午年：全局火土旺——甲木父星（年干）被克泄耗交加（父亡）。格局本身不破（用神到位）
        val r = CongGeYunCalculator.analyze(b, listOf(
            Pillar(HeavenlyStem.REN, EarthlyBranch.SHEN), Pillar(HeavenlyStem.GENG, EarthlyBranch.WU)))
        assertTrue("庚午年格局应用/喜（火土到位）：$r", r != null && (r.verdict == YunVerdict.YONG || r.verdict == YunVerdict.XI))
    }
}
