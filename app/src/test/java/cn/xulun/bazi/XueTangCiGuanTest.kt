package cn.xulun.bazi

import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 学堂词馆（年命纳音查）与将星（年支日支并查）锚点。
 * 理论见 .shengke-reference.md 第五七节七、八。
 */
class XueTangCiGuanTest {

    private fun items(b: cn.xulun.bazi.model.BaziResult, name: String) =
        ShenShaCalculator.analyze(b).items.filter { it.name == name }

    @Test fun `正学堂 乙丑海中金见辛巳白蜡金`() {
        // 乙丑 辛巳 己未 甲子——年海中金，学堂巳（月柱辛巳白蜡金同行=正学堂）
        val b = chartOf(
            HeavenlyStem.YI, EarthlyBranch.CHOU, HeavenlyStem.XIN, EarthlyBranch.SI,
            HeavenlyStem.JI, EarthlyBranch.WEI, HeavenlyStem.JIA, EarthlyBranch.ZI)
        assertTrue("乙丑金命正学堂（月支巳）", items(b, "正学堂").any { it.pillarLabel == "月支" && it.branch == EarthlyBranch.SI })
    }

    @Test fun `学堂非正 丙午天河水见壬申剑锋金`() {
        // 甲寅 壬申 甲子 乙丑——年大溪水，学堂申（月柱壬申剑锋金≠水=学堂非正）
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.REN, EarthlyBranch.SHEN,
            HeavenlyStem.JIA, EarthlyBranch.ZI, HeavenlyStem.YI, EarthlyBranch.CHOU)
        assertTrue("水命学堂申（非正）", items(b, "学堂").any { it.pillarLabel == "月支" && it.branch == EarthlyBranch.SHEN })
        assertTrue("非正学堂", items(b, "正学堂").isEmpty())
    }

    @Test fun `正词馆 戊辰大林木见庚寅松柏木`() {
        // 辛卯 庚寅 甲申 丙寅——年松柏木，词馆寅（月柱庚寅松柏木同行=正词馆）
        val b = chartOf(
            HeavenlyStem.XIN, EarthlyBranch.MAO, HeavenlyStem.GENG, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.SHEN, HeavenlyStem.BING, EarthlyBranch.YIN)
        assertTrue("木命正词馆（月支寅）", items(b, "正词馆").any { it.pillarLabel == "月支" && it.branch == EarthlyBranch.YIN })
    }

    @Test fun `将星年支日支并查`() {
        // 寅年支（寅午戌→午）+ 子日支（申子辰→子）：寅午戌组查午、申子辰组查子
        val b = chartOf(
            HeavenlyStem.BING, EarthlyBranch.YIN, HeavenlyStem.BING, EarthlyBranch.SHEN,
            HeavenlyStem.JIA, EarthlyBranch.ZI, HeavenlyStem.BING, EarthlyBranch.YIN)
        val jx = items(b, "将星")
        assertTrue("年支寅组中神午（无午支）+日支子组中神子（日支子命中）：${jx}",
            jx.any { it.pillarLabel == "日支" && it.branch == EarthlyBranch.ZI })
    }
}
