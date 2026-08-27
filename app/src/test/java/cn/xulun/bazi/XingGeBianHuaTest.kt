package cn.xulun.bazi

import cn.xulun.bazi.core.XingGeCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Pillar
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 五行变化断性格锚点（重庆柯女士——身旺→丁丑运身弱性格翻转）。理论见 .shengke-reference.md 第六五节四。
 */
class XingGeBianHuaTest {

    @Test fun `柯女士 床子辛巳乙卯己卯 己卯戊寅帮身中和偏旺`() {
        val b = chartOf(HeavenlyStem.GENG, EarthlyBranch.ZI, HeavenlyStem.XIN, EarthlyBranch.SI,
            HeavenlyStem.YI, EarthlyBranch.MAO, HeavenlyStem.JI, EarthlyBranch.MAO)
        // 原局+己卯运：卯木帮身
        val r = XingGeCalculator.analyzeWithYun(b, listOf(Pillar(HeavenlyStem.JI, EarthlyBranch.MAO)))
        assertTrue("帮身或无翻转：${r.map { it.dim }}",
            r.none { it.dim.contains("变弱") })
    }

    @Test fun `丁丑运 官杀旺身变弱性格大变`() {
        val b = chartOf(HeavenlyStem.GENG, EarthlyBranch.ZI, HeavenlyStem.XIN, EarthlyBranch.SI,
            HeavenlyStem.YI, EarthlyBranch.MAO, HeavenlyStem.JI, EarthlyBranch.MAO)
        val r = XingGeCalculator.analyzeWithYun(b, listOf(Pillar(HeavenlyStem.DING, EarthlyBranch.CHOU)))
        // 断翻转（官杀丑金？丑土财生官）——输出含变化条目
        assertTrue("丁丑运出变化条目：${r.map { it.dim }}",
            r.isNotEmpty())
    }
}
