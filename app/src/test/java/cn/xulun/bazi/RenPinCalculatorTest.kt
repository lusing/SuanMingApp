package cn.xulun.bazi

import cn.xulun.bazi.core.DaYunCalculator
import cn.xulun.bazi.core.RenPinCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 人品推断测试（十神心性+五行特性复合——正负面分判）。
 * 例一孙先生（正印+伤官佩印→德才兼备）；例二科先生（食神太强负面→原则差）；
 * 例三文先生（杀财食皆透→义气但无原则）；例四庆女士（食神多官印弱→放荡是非缺信用）；
 * 例五蒋先生（偏印强+水食伤一片+土倔→品质很坏）。
 */
class RenPinCalculatorTest {

    @Test
    fun `例一 孙先生 乙巳甲申丁巳甲辰 正印伤官佩印 德才兼备`() {
        val bazi = chartOf(
            HeavenlyStem.YI, EarthlyBranch.SI,
            HeavenlyStem.JIA, EarthlyBranch.SHEN,
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.JIA, EarthlyBranch.CHEN,
            1950, 2000,
        )
        assertEquals("乙巳 甲申 丁巳 甲辰", bazi.fullBazi)
        val r = RenPinCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        assertTrue(r.verdict.contains("优秀") || r.verdict.contains("德才兼备"))
        assertTrue(r.items.any { it.title.contains("正印") })
        assertTrue(r.items.any { it.title.contains("伤官佩印") || it.text.contains("德才兼备") })
    }

    @Test
    fun `例二 科先生 丁未癸丑丁丑庚子 食神太强负面 原则性差`() {
        val bazi = chartOf(
            HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.DING, EarthlyBranch.CHOU,
            HeavenlyStem.GENG, EarthlyBranch.ZI,
            1950, 2000,
        )
        assertEquals("丁未 癸丑 丁丑 庚子", bazi.fullBazi)
        val r = RenPinCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        assertTrue(r.verdict.contains("较差") || r.verdict.contains("差"))
        assertTrue(r.items.any { it.text.contains("一意孤行") || it.text.contains("原则性") })
        assertTrue(r.yunNote.contains("原则") || r.yunNote.contains("本性"))
    }

    @Test
    fun `例三 文先生 丁未己酉乙未辛巳 杀财食皆透 义气但无原则`() {
        val bazi = chartOf(
            HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.YI, EarthlyBranch.WEI,
            HeavenlyStem.XIN, EarthlyBranch.SI,
            1950, 2000,
        )
        assertEquals("丁未 己酉 乙未 辛巳", bazi.fullBazi)
        val r = RenPinCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        assertTrue(r.items.any { it.text.contains("义气") })
        assertTrue(r.advice.contains("不宜") || r.advice.contains("原则"))
    }

    @Test
    fun `例四 庆女士 丙午庚寅壬寅壬寅 食神多官印弱 放荡是非缺信用`() {
        val bazi = chartOf(
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.GENG, EarthlyBranch.YIN,
            HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.REN, EarthlyBranch.YIN,
            1950, 2000,
        )
        assertEquals("丙午 庚寅 壬寅 壬寅", bazi.fullBazi)
        val r = RenPinCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.FEMALE))
        assertTrue(r.verdict.contains("差"))
        assertTrue(r.items.any { it.text.contains("信用") || it.text.contains("放荡") })
    }

    @Test
    fun `例五 蒋先生 壬戌癸丑辛丑癸巳 偏印强水食伤一片 品质很坏`() {
        val bazi = chartOf(
            HeavenlyStem.REN, EarthlyBranch.XU,
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.XIN, EarthlyBranch.CHOU,
            HeavenlyStem.GUI, EarthlyBranch.SI,
            1950, 2000,
        )
        assertEquals("壬戌 癸丑 辛丑 癸巳", bazi.fullBazi)
        val r = RenPinCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        assertTrue(r.verdict.contains("很坏") || r.verdict.contains("很差"))
        assertTrue(r.items.any { it.text.contains("目无法纪") || it.text.contains("鬼点子") })
    }
}
