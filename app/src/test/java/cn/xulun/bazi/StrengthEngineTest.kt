package cn.xulun.bazi

import cn.xulun.bazi.core.StrengthEngine
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Pillar
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 强弱引擎（八步法）锚点测试。
 * 排序例=教材第十九/二十节亲手推演的全干支强弱序；十一式=第三十七节强弱分类黄金例。
 */
class StrengthEngineTest {

    // ── 排序例一（第十九节：原局静态）──
    // 丁未 辛亥 己丑 甲子 → 亥>子>甲>未>己>丑>辛>丁
    @Test fun `排序例一 丁未辛亥己丑甲子 全干支序`() {
        val r = StrengthEngine.analyze(chartOf(
            HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.XIN, EarthlyBranch.HAI,
            HeavenlyStem.JI, EarthlyBranch.CHOU,
            HeavenlyStem.JIA, EarthlyBranch.ZI,
        ))
        assertEquals("亥子甲未己丑辛丁", r.ranking)
    }

    // ── 排序例二（第二十节：大运介入，标尺换运支寅）──
    // 甲辰 乙亥 丁丑 丙午 + 运戊寅 → 寅>乙>甲>午>丙>丁>戊>丑>辰>亥
    @Test fun `排序例二 甲辰乙亥丁丑丙午加运戊寅 全干支序`() {
        val r = StrengthEngine.analyze(
            chartOf(
                HeavenlyStem.JIA, EarthlyBranch.CHEN,
                HeavenlyStem.YI, EarthlyBranch.HAI,
                HeavenlyStem.DING, EarthlyBranch.CHOU,
                HeavenlyStem.BING, EarthlyBranch.WU,
            ),
            extraPillars = listOf(Pillar(HeavenlyStem.WU, EarthlyBranch.YIN)),
        )
        assertEquals("寅乙甲午丙丁戊丑辰亥", r.ranking)
    }

    // ── 十一式强弱判定（第三十七节）──

    @Test fun `强旺一 当令势众 甲寅丙寅甲午丁卯`() {
        assertEquals(DayStrength.STRONG, StrengthEngine.analyze(chartOf(
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.WU,
            HeavenlyStem.DING, EarthlyBranch.MAO,
        )).verdict)
    }

    @Test fun `强旺二 势众得令生 甲子丙寅丙午己丑`() {
        assertEquals(DayStrength.STRONG, StrengthEngine.analyze(chartOf(
            HeavenlyStem.JIA, EarthlyBranch.ZI,
            HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.JI, EarthlyBranch.CHOU,
        )).verdict)
    }

    @Test fun `强一 结党势众 庚戌戊子己未己巳`() {
        assertEquals(DayStrength.STRONG, StrengthEngine.analyze(chartOf(
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.WU, EarthlyBranch.ZI,
            HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.JI, EarthlyBranch.SI,
        )).verdict)
    }

    @Test fun `强二 变节向日 丙午戊戌丁卯壬寅`() {
        assertEquals(DayStrength.STRONG, StrengthEngine.analyze(chartOf(
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.WU, EarthlyBranch.XU,
            HeavenlyStem.DING, EarthlyBranch.MAO,
            HeavenlyStem.REN, EarthlyBranch.YIN,
        )).verdict)
    }

    @Test fun `强三 化敌为友通关 庚寅癸未甲寅壬申`() {
        assertEquals(DayStrength.STRONG, StrengthEngine.analyze(chartOf(
            HeavenlyStem.GENG, EarthlyBranch.YIN,
            HeavenlyStem.GUI, EarthlyBranch.WEI,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.REN, EarthlyBranch.SHEN,
        )).verdict)
    }

    @Test fun `强四 坐收渔利 戊午癸亥丙午庚寅`() {
        assertEquals(DayStrength.STRONG, StrengthEngine.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.WU,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.GENG, EarthlyBranch.YIN,
        )).verdict)
    }

    @Test fun `衰弱一 失令孤立从格 壬申壬子丙子己亥`() {
        assertEquals(DayStrength.WEAK, StrengthEngine.analyze(chartOf(
            HeavenlyStem.REN, EarthlyBranch.SHEN,
            HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.BING, EarthlyBranch.ZI,
            HeavenlyStem.JI, EarthlyBranch.HAI,
        )).verdict)
    }

    @Test fun `衰弱二 衰不敌旺 癸亥甲子己丑己巳`() {
        assertEquals(DayStrength.WEAK, StrengthEngine.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.JIA, EarthlyBranch.ZI,
            HeavenlyStem.JI, EarthlyBranch.CHOU,
            HeavenlyStem.JI, EarthlyBranch.SI,
        )).verdict)
    }

    @Test fun `弱一 反主为客 庚申壬午丙子戊子`() {
        assertEquals(DayStrength.WEAK, StrengthEngine.analyze(chartOf(
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.REN, EarthlyBranch.WU,
            HeavenlyStem.BING, EarthlyBranch.ZI,
            HeavenlyStem.WU, EarthlyBranch.ZI,
        )).verdict)
    }

    @Test fun `弱一又例 群鼠食猫 甲午丙子壬午丙午`() {
        assertEquals(DayStrength.WEAK, StrengthEngine.analyze(chartOf(
            HeavenlyStem.JIA, EarthlyBranch.WU,
            HeavenlyStem.BING, EarthlyBranch.ZI,
            HeavenlyStem.REN, EarthlyBranch.WU,
            HeavenlyStem.BING, EarthlyBranch.WU,
        )).verdict)
    }

    @Test fun `弱二 移情别恋 丁丑乙巳丁酉辛丑`() {
        assertEquals(DayStrength.WEAK, StrengthEngine.analyze(chartOf(
            HeavenlyStem.DING, EarthlyBranch.CHOU,
            HeavenlyStem.YI, EarthlyBranch.SI,
            HeavenlyStem.DING, EarthlyBranch.YOU,
            HeavenlyStem.XIN, EarthlyBranch.CHOU,
        )).verdict)
    }

    @Test fun `弱三 生多反弱从印 己未辛未庚辰癸未`() {
        assertEquals(DayStrength.WEAK, StrengthEngine.analyze(chartOf(
            HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.XIN, EarthlyBranch.WEI,
            HeavenlyStem.GENG, EarthlyBranch.CHEN,
            HeavenlyStem.GUI, EarthlyBranch.WEI,
        )).verdict)
    }
}
