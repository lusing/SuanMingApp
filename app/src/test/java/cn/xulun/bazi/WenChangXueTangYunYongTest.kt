package cn.xulun.bazi

import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 神煞运用举例·文昌学堂（文星临用神利文途实证）。理论见 .shengke-reference.md 第五九节四。
 */
class WenChangXueTangYunYongTest {
    @Test fun `丁巳庚戌戊申丁巳 文昌学堂并临日支申用神`() {
        val b = chartOf(
            HeavenlyStem.DING, EarthlyBranch.SI, HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.WU, EarthlyBranch.SHEN, HeavenlyStem.DING, EarthlyBranch.SI)
        val items = ShenShaCalculator.analyze(b).items
        // 戊日文昌=申（日支命中）；年命丁巳沙中土→学堂=长生申（日柱戊申纳音大驿土同行=正学堂）
        assertTrue("文昌申（日支）", items.any { it.name == "文昌" && it.branch == EarthlyBranch.SHEN && it.pillarLabel == "日支" })
        assertTrue("学堂/正学堂申（日支）", items.any { (it.name == "学堂" || it.name == "正学堂") && it.branch == EarthlyBranch.SHEN && it.pillarLabel == "日支" })
        // 词馆=土命临官亥（原局无亥——1995乙亥太岁值词馆）
        assertTrue("原局无词馆亥（值流年）——不应出现", items.none { it.name.startsWith("词馆") })
    }
}
