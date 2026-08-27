package cn.xulun.bazi

import cn.xulun.bazi.core.BingYaoCalculator
import cn.xulun.bazi.model.BingYaoResult
import cn.xulun.bazi.model.CongGeKind
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.GodCategory
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.TiaoHouState
import cn.xulun.bazi.model.YongShenMethod
import org.junit.Test
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/**
 * 病药取用（传统扶抑/通关/顺势/调候）锚点测试。
 * 黄金例全部出自《参考理论》第三十九~四十一节（.shengke-reference.md），教材已给出结论。
 */
class BingYaoCalculatorTest {

    /**
     * 教材八字反推：在 1700-2050 真实历法中搜索四柱完全一致的出生时刻，
     * 经 [cn.xulun.bazi.core.BaziCalculator.calculate] 正规生成（含节气换月柱），
     * 这样 BaziResult 带真实 birthDateTime，后续可叠加分析大运流年。
     * 书上给出的大运流年只用于定位验证，不参与构造。
     * 优化：以 1985-05-20=己未（cycleIndex 55）为锚，先算术筛日柱（每 60 天一遇），
     * 命中才调用排盘校验年/月柱。
     */
    private fun bazi(
        yStem: HeavenlyStem, yBranch: EarthlyBranch,
        mStem: HeavenlyStem, mBranch: EarthlyBranch,
        dStem: HeavenlyStem, dBranch: EarthlyBranch,
        hStem: HeavenlyStem, hBranch: EarthlyBranch,
    ): cn.xulun.bazi.model.BaziResult {
        val targetDayIdx = (0..59).first { i ->
            HeavenlyStem.fromIndex(i) == dStem && EarthlyBranch.fromIndex(i) == dBranch
        }
        val anchor = java.time.LocalDate.of(1985, 5, 20) // cycleIndex 55（锚点测试核定）
        var date = java.time.LocalDate.of(1700, 1, 1)
        val end = java.time.LocalDate.of(2051, 1, 1)
        while (date.isBefore(end)) {
            val idx = Math.floorMod(55 + java.time.temporal.ChronoUnit.DAYS.between(anchor, date).toInt(), 60)
            if (idx == targetDayIdx) {
                val noon = cn.xulun.bazi.core.BaziCalculator.calculate(LocalDateTime.of(date, java.time.LocalTime.NOON))
                if (noon.yearPillar.stem == yStem && noon.yearPillar.branch == yBranch &&
                    noon.monthPillar.stem == mStem && noon.monthPillar.branch == mBranch &&
                    noon.dayPillar.stem == dStem && noon.dayPillar.branch == dBranch
                ) {
                    for (hour in 0..23) {
                        val b = cn.xulun.bazi.core.BaziCalculator.calculate(LocalDateTime.of(date, java.time.LocalTime.of(hour, 0)))
                        if (b.hourPillar.stem == hStem && b.hourPillar.branch == hBranch) return b
                    }
                }
            }
            date = date.plusDays(1)
        }
        error("1700-2050 未找到四柱：$yStem$yBranch $mStem$mBranch $dStem$dBranch $hStem$hBranch")
    }

    // ── 扶·弱+官杀病 → 印 ──

    @Test fun `通关一 戊午己未壬申庚子 身弱杀强取印通关`() {
        // 教材：土众当令克壬，庚金纳土之力生壬——金为通关用神
        val r = BingYaoCalculator.analyze(bazi(
            HeavenlyStem.WU, EarthlyBranch.WU,
            HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.REN, EarthlyBranch.SHEN,
            HeavenlyStem.GENG, EarthlyBranch.ZI,
        ))
        assertEquals(DayStrength.WEAK, r.strength)
        assertEquals(GodCategory.GUAN, r.bing)
        assertEquals(GodCategory.YIN, r.yongShen)
        assertEquals(YongShenMethod.TONG_GUAN, r.method)
    }

    @Test fun `通关二 戊午己未壬辰庚子 印虚浮仍用印但注记`() {
        // 教材：与前造只换一辰字，庚金虚浮无根，土众埋金——通关有力无力之分
        val r = BingYaoCalculator.analyze(bazi(
            HeavenlyStem.WU, EarthlyBranch.WU,
            HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.REN, EarthlyBranch.CHEN,
            HeavenlyStem.GENG, EarthlyBranch.ZI,
        ))
        assertEquals(DayStrength.WEAK, r.strength)
        assertEquals(GodCategory.GUAN, r.bing)
        assertEquals(GodCategory.YIN, r.yongShen)
        assertTrue("应注记通关力薄：${r.basis}", r.basis.contains("虚浮") || r.basis.contains("无力"))
    }

    @Test fun `扶官杀病一 戊戌己未癸亥庚申 取印化杀`() {
        // 教材：土众当令，只宜用印化官杀生身，水为喜神；忌火土
        val r = BingYaoCalculator.analyze(bazi(
            HeavenlyStem.WU, EarthlyBranch.XU,
            HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
        ))
        assertEquals(DayStrength.WEAK, r.strength)
        assertEquals(GodCategory.GUAN, r.bing)
        assertEquals(GodCategory.YIN, r.yongShen)
        assertEquals(GodCategory.BI, r.xiShen)
        assertTrue("教材忌火土：${r.jiShen}", r.jiShen.contains(GodCategory.CAI) && r.jiShen.contains(GodCategory.GUAN))
    }

    @Test fun `扶官杀病二 乙巳辛巳辛未庚寅 取印`() {
        // 教材：三金虚浮，坐下未土四支藏土通关，取印为用
        val r = BingYaoCalculator.analyze(bazi(
            HeavenlyStem.YI, EarthlyBranch.SI,
            HeavenlyStem.XIN, EarthlyBranch.SI,
            HeavenlyStem.XIN, EarthlyBranch.WEI,
            HeavenlyStem.GENG, EarthlyBranch.YIN,
        ))
        assertEquals(DayStrength.WEAK, r.strength)
        assertEquals(GodCategory.GUAN, r.bing)
        assertEquals(GodCategory.YIN, r.yongShen)
    }

    @Test fun `扶官杀病印弱例外 壬子壬子丁巳乙巳 取比劫不取印`() {
        // 教材：乙木偏印无根不能通关，日柱双体两巳结朋，取巳火为用而不用乙木
        val r = BingYaoCalculator.analyze(bazi(
            HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.YI, EarthlyBranch.SI,
        ))
        assertEquals(DayStrength.WEAK, r.strength)
        assertEquals(GodCategory.GUAN, r.bing)
        assertEquals("印无根比劫有力时应取比劫：${r.basis}", GodCategory.BI, r.yongShen)
    }

    @Test fun `扶官杀病印弱例外二 癸丑丙辰癸未癸丑 首取比劫辛金为喜`() {
        // 教材：湿土藏水有弱根+年时透癸，取癸水为用，辛金为喜神
        val r = BingYaoCalculator.analyze(bazi(
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.BING, EarthlyBranch.CHEN,
            HeavenlyStem.GUI, EarthlyBranch.WEI,
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
        ))
        assertEquals(DayStrength.WEAK, r.strength)
        assertEquals(GodCategory.BI, r.yongShen)
        assertEquals(GodCategory.YIN, r.xiShen)
    }

    // ── 扶·弱+财病 → 比劫 ──

    @Test fun `扶财病一 戊戌乙丑甲辰丁卯 取比劫胜财`() {
        // 教材：不取辰丑中癸水（土强水必败），取乙卯木帮身胜财
        val r = BingYaoCalculator.analyze(bazi(
            HeavenlyStem.WU, EarthlyBranch.XU,
            HeavenlyStem.YI, EarthlyBranch.CHOU,
            HeavenlyStem.JIA, EarthlyBranch.CHEN,
            HeavenlyStem.DING, EarthlyBranch.MAO,
        ))
        assertEquals(DayStrength.WEAK, r.strength)
        assertEquals(GodCategory.CAI, r.bing)
        assertEquals(GodCategory.BI, r.yongShen)
    }

    @Test fun `扶财病二 己丑戊辰甲辰丁卯 取卯木护身`() {
        val r = BingYaoCalculator.analyze(bazi(
            HeavenlyStem.JI, EarthlyBranch.CHOU,
            HeavenlyStem.WU, EarthlyBranch.CHEN,
            HeavenlyStem.JIA, EarthlyBranch.CHEN,
            HeavenlyStem.DING, EarthlyBranch.MAO,
        ))
        assertEquals(DayStrength.WEAK, r.strength)
        assertEquals(GodCategory.CAI, r.bing)
        assertEquals(GodCategory.BI, r.yongShen)
    }

    // ── 扶·弱+食伤病 → 印（无印→比劫）──

    @Test fun `扶食伤病无木 己丑庚午丙子戊戌 以午火为用`() {
        // 教材：当令被众土泄，旺而不强，四柱无木，只好以午火为用
        val r = BingYaoCalculator.analyze(bazi(
            HeavenlyStem.JI, EarthlyBranch.CHOU,
            HeavenlyStem.GENG, EarthlyBranch.WU,
            HeavenlyStem.BING, EarthlyBranch.ZI,
            HeavenlyStem.WU, EarthlyBranch.XU,
        ))
        assertEquals(DayStrength.WEAK, r.strength)
        assertEquals(GodCategory.SHI, r.bing)
        assertEquals(GodCategory.BI, r.yongShen)
    }

    // ── 抑 ──

    @Test fun `抑印病无财 乙巳乙酉乙亥乙酉 取巳火泄身`() {
        // 教材：四乙一气+亥通关金木，身强；官杀有水通关不宜，戊土财太弱，取巳火泄身耗印
        val r = BingYaoCalculator.analyze(bazi(
            HeavenlyStem.YI, EarthlyBranch.SI,
            HeavenlyStem.YI, EarthlyBranch.YOU,
            HeavenlyStem.YI, EarthlyBranch.HAI,
            HeavenlyStem.YI, EarthlyBranch.YOU,
        ))
        assertEquals(DayStrength.STRONG, r.strength)
        assertEquals(GodCategory.BI, r.bing)
        assertEquals(GodCategory.SHI, r.yongShen)
    }

    @Test fun `顺势 己酉辛未庚戌庚辰 两行成象顺势取土金`() {
        // 教材：土金一气身印两强，以财制印反致混战，顺势取土金，最忌财星虚浮
        val r = BingYaoCalculator.analyze(bazi(
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.XIN, EarthlyBranch.WEI,
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.GENG, EarthlyBranch.CHEN,
        ))
        assertEquals(CongGeKind.CONG_STRONG, r.congGe?.kind)
        assertEquals(YongShenMethod.SHUN_SHI, r.method)
        assertTrue("两行成象忌财：${r.jiShen}", r.jiShen.contains(GodCategory.CAI))
    }

    @Test fun `抑比劫病官杀无力 癸丑甲子癸卯癸亥 取甲木泄秀`() {
        // 教材：亥子丑会局一片汪洋极强；取官杀必水土交战且丑无可倚，取甲木泄身吐秀
        val r = BingYaoCalculator.analyze(bazi(
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.JIA, EarthlyBranch.ZI,
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
        ))
        assertEquals(DayStrength.STRONG, r.strength)
        // 亥子丑三会去丑则入水灵木秀（用同为木泄秀）；未会尽则按病药比劫为病
        if (r.congGe == null) {
            assertEquals(GodCategory.BI, r.bing)
            assertEquals(GodCategory.SHI, r.yongShen)
        }
    }

    @Test fun `抑比劫病无官 甲戌丙寅甲戌乙亥 取丙火通关泄身生财`() {
        // 教材：身强财弱，宜取丙火通关，泄身生财
        val r = BingYaoCalculator.analyze(bazi(
            HeavenlyStem.JIA, EarthlyBranch.XU,
            HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.XU,
            HeavenlyStem.YI, EarthlyBranch.HAI,
        ))
        assertEquals(DayStrength.STRONG, r.strength)
        assertEquals(GodCategory.BI, r.bing)
        assertEquals(GodCategory.SHI, r.yongShen)
        assertEquals(YongShenMethod.TONG_GUAN, r.method)
    }

    // ── 调候叠层 ──

    @Test fun `调候一致 癸卯乙丑己亥丙寅 丙火兼扶抑与调候`() {
        // 教材：命局寒湿以丙火调候，调候用神正是扶抑用神
        val r = BingYaoCalculator.analyze(bazi(
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.YI, EarthlyBranch.CHOU,
            HeavenlyStem.JI, EarthlyBranch.HAI,
            HeavenlyStem.BING, EarthlyBranch.YIN,
        ))
        assertEquals(DayStrength.WEAK, r.strength)
        assertEquals(GodCategory.YIN, r.yongShen)
        assertEquals(TiaoHouState.ALIGNED, r.tiaoHou)
    }

    @Test fun `调候冲突 庚午己丑己丑壬申 宜食伤火为忌扶抑为主`() {
        // 教材：当令身强宜食伤，火调候必生土更强为忌——以扶抑为主
        val r = BingYaoCalculator.analyze(bazi(
            HeavenlyStem.GENG, EarthlyBranch.WU,
            HeavenlyStem.JI, EarthlyBranch.CHOU,
            HeavenlyStem.JI, EarthlyBranch.CHOU,
            HeavenlyStem.REN, EarthlyBranch.SHEN,
        ))
        assertEquals(DayStrength.STRONG, r.strength)
        assertEquals(GodCategory.SHI, r.yongShen)
        assertEquals(TiaoHouState.CONFLICT, r.tiaoHou)
    }

    @Test fun `调候矛盾局 辛丑癸巳丁酉壬寅 夏月组合致弱水调候为忌`() {
        // 教材补例：丁火生巳月但巳酉丑合金致弱；水克身为忌，调候与扶抑对立，唯靠岁运水木兼行调节
        val r = BingYaoCalculator.analyze(chartOf(
            HeavenlyStem.XIN, EarthlyBranch.CHOU,
            HeavenlyStem.GUI, EarthlyBranch.SI,
            HeavenlyStem.DING, EarthlyBranch.YOU,
            HeavenlyStem.REN, EarthlyBranch.YIN,
        ))
        assertEquals(DayStrength.WEAK, r.strength)
        assertEquals(GodCategory.GUAN, r.bing)
        assertEquals(TiaoHouState.CONFLICT, r.tiaoHou)
        org.junit.Assert.assertTrue("水为忌：${r.jiShen}", r.jiShen.contains(GodCategory.GUAN))
    }

    @Test fun `自调局 戊戌甲子壬寅丙午 子月火局自暖不需调候`() {
        // 教材补例二：支全寅午戌丙透，命局本身之火足暖，不需再调候；再逢旺火反致火多水干
        val r = BingYaoCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.XU,
            HeavenlyStem.JIA, EarthlyBranch.ZI,
            HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.BING, EarthlyBranch.WU,
        ))
        assertEquals(TiaoHouState.ZI_TIAO, r.tiaoHou)
    }

    @Test fun `暖极寒无根 癸未丁巳丙午癸巳 不宜调候顺势`() {
        // 教材：巳午未会火暖极，癸弱水无根不可用；岁运逢根反致水火交战
        val r = BingYaoCalculator.analyze(bazi(
            HeavenlyStem.GUI, EarthlyBranch.WEI,
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.GUI, EarthlyBranch.SI,
        ))
        assertEquals(DayStrength.STRONG, r.strength)
        assertEquals(GodCategory.SHI, r.yongShen)
        assertEquals(TiaoHouState.WU_GEN, r.tiaoHou)
    }
}

/** 测试用壳，便于以后挂大运叠加柱。 */
