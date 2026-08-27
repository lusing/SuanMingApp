package cn.xulun.bazi

import cn.xulun.bazi.core.BaziCalculator
import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.ShenShaItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class ShenShaCalculatorTest {
    private fun bazi(y: Int, m: Int, d: Int, h: Int, min: Int = 0) =
        BaziCalculator.calculate(LocalDateTime.of(y, m, d, h, min))

    private fun item(name: String, loc: String, b: EarthlyBranch) = ShenShaItem(name, loc, branch = b)
    private fun itemStem(name: String, loc: String, s: HeavenlyStem) = ShenShaItem(name, loc, stem = s)

    // 锚点 A: 1985-05-20 子时 → 乙丑 辛巳 己未 甲子（日主己）
    @Test fun anchorA_shensha_items_match_xuanzong() {
        val r = ShenShaCalculator.analyze(bazi(1985, 5, 20, 0, 10))
        assertEquals(
            listOf(
                item("天乙贵人", "时支", EarthlyBranch.ZI),
                item("太极贵人", "年支", EarthlyBranch.CHOU),
                item("太极贵人", "日支", EarthlyBranch.WEI),
                item("太极贵人", "时支", EarthlyBranch.ZI),
                item("正学堂", "月支", EarthlyBranch.SI),  // 乙丑海中金，学堂巳，辛巳白蜡金同行=正
                item("金舆", "月支", EarthlyBranch.SI),  // 年干乙→巳
                item("羊刃", "月支", EarthlyBranch.SI),
                item("桃花", "时支", EarthlyBranch.ZI),
                item("驿马", "月支", EarthlyBranch.SI),
                item("华盖", "日支", EarthlyBranch.WEI),
                itemStem("月德合", "年干", HeavenlyStem.YI),
                itemStem("天德贵人", "月干", HeavenlyStem.XIN),
            ),
            r.items,
        )
    }

    // 锚点 D: 2000-03-04 卯时 → 庚辰 戊寅 辛酉 辛卯（日主辛）
    @Test fun anchorD_shensha_items_match_xuanzong() {
        val r = ShenShaCalculator.analyze(bazi(2000, 3, 4, 6, 0))
        assertEquals(
            listOf(
                item("天乙贵人", "月支", EarthlyBranch.YIN),
                item("太极贵人", "月支", EarthlyBranch.YIN),
                item("禄神", "日支", EarthlyBranch.YOU),
                item("驿马", "月支", EarthlyBranch.YIN),  // 年支辰组（申子辰→寅）查月支寅
                item("将星", "日支", EarthlyBranch.YOU),
                item("灾煞", "时支", EarthlyBranch.MAO),  // 日支酉组（巳酉丑→卯）查时支卯
                item("劫煞", "月支", EarthlyBranch.YIN),
                itemStem("月德合", "日干", HeavenlyStem.XIN),
                itemStem("月德合", "时干", HeavenlyStem.XIN),
            ),
            r.items,
        )
    }

    @Test fun groupOf_trigram_groups() {
        assertEquals(0, ShenShaCalculator.groupOf(EarthlyBranch.ZI))   // 申子辰
        assertEquals(0, ShenShaCalculator.groupOf(EarthlyBranch.SHEN))
        assertEquals(1, ShenShaCalculator.groupOf(EarthlyBranch.WEI))  // 亥卯未
        assertEquals(2, ShenShaCalculator.groupOf(EarthlyBranch.YIN))  // 寅午戌
        assertEquals(3, ShenShaCalculator.groupOf(EarthlyBranch.SI))   // 巳酉丑
    }
}
