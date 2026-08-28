package cn.xulun.bazi

import cn.xulun.bazi.core.DaYunCalculator
import cn.xulun.bazi.core.TianFuCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 天赋推断与职业选择测试（十神含义+五行特性→天赋→职业；行运配合）。
 * 例一刘先生（伤官+水智+伤官生财→销售管理）；例二黎先生（杀贴身+偏印强→行政管理）；
 * 例三小清先生（两杀透+三合杀局→武职司法；官杀运不宜下海）。
 */
class TianFuCalculatorTest {

    @Test
    fun `例一 刘先生 己酉丙子戊午癸亥 伤官水智伤官生财 销售管理`() {
        val bazi = chartOf(
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.BING, EarthlyBranch.ZI,
            HeavenlyStem.WU, EarthlyBranch.WU,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            1950, 2000,
        )
        assertEquals("己酉 丙子 戊午 癸亥", bazi.fullBazi)
        val r = TianFuCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        assertTrue(r.items.any { it.title.contains("口才") || it.title.contains("交际") })
        assertTrue(r.careers.any { it.contains("销售") })
        assertTrue(r.yunNote.contains("食伤生财") || r.yunNote.contains("销售"))
    }

    @Test
    fun `例二 黎先生 甲午丁丑辛巳己丑 杀贴身偏印强 行政管理`() {
        val bazi = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.WU,
            HeavenlyStem.DING, EarthlyBranch.CHOU,
            HeavenlyStem.XIN, EarthlyBranch.SI,
            HeavenlyStem.JI, EarthlyBranch.CHOU,
            1950, 2000,
        )
        assertEquals("甲午 丁丑 辛巳 己丑", bazi.fullBazi)
        val r = TianFuCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        assertTrue(r.items.any { it.title.contains("自律") || it.title.contains("偏印") })
        assertTrue(r.careers.any { it.contains("行政") || it.contains("管理") })
    }

    @Test
    fun `例五 方先生 己酉己巳癸巳庚申 干上杀印相生贵格从政+暗财`() {
        val bazi = chartOf(
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.JI, EarthlyBranch.SI,
            HeavenlyStem.GUI, EarthlyBranch.SI,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            1950, 2000,
        )
        assertEquals("己酉 己巳 癸巳 庚申", bazi.fullBazi)
        val r = TianFuCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        assertTrue(r.items.any { it.title.contains("杀印相生") })
        assertTrue(r.careers.any { it.contains("从政") || it.contains("行政") })
        assertTrue(r.items.any { it.text.contains("外快") || it.text.contains("暗地") })
    }

    @Test
    fun `例六 顺容女士 丙午庚寅辛亥甲午 从财从官工资型简单活`() {
        val bazi = chartOf(
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.GENG, EarthlyBranch.YIN,
            HeavenlyStem.XIN, EarthlyBranch.HAI,
            HeavenlyStem.JIA, EarthlyBranch.WU,
            1950, 2000,
        )
        assertEquals("丙午 庚寅 辛亥 甲午", bazi.fullBazi)
        val r = TianFuCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.FEMALE))
        assertTrue(r.items.any { it.title.contains("从财从官") || it.text.contains("依赖") })
        assertTrue(r.careers.any { it.contains("简单") || it.contains("不宜创业") })
    }

    @Test
    fun `例三 小清先生 丙午庚寅庚戌丙戌 两杀透三合杀局 武职司法`() {
        val bazi = chartOf(
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.GENG, EarthlyBranch.YIN,
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.BING, EarthlyBranch.XU,
            1950, 2000,
        )
        assertEquals("丙午 庚寅 庚戌 丙戌", bazi.fullBazi)
        val r = TianFuCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        assertTrue(r.items.any { it.title.contains("七杀") || it.title.contains("武职") })
        assertTrue(r.careers.any { it.contains("军警") || it.contains("司法") || it.contains("武职") })
        assertTrue(r.yunNote.contains("不宜") || r.yunNote.contains("副业"))
    }
}
