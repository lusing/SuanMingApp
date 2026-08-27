package cn.xulun.bazi

import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 三奇贵人锚点（顺布紧连）。理论见 .shengke-reference.md 第五七节三。
 */
class SanQiGuiRenTest {

    private fun names(b: cn.xulun.bazi.model.BaziResult) =
        ShenShaCalculator.analyze(b).items.filter { it.name.startsWith("三奇贵人") }.map { it.name to it.pillarLabel }

    @Test fun `天上三奇 甲年戊月庚日紧连`() {
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.ZI, HeavenlyStem.WU, EarthlyBranch.CHEN,
            HeavenlyStem.GENG, EarthlyBranch.SHEN, HeavenlyStem.BING, EarthlyBranch.ZI)
        assertTrue("年月日干连：${names(b)}", names(b).contains("三奇贵人（天上）" to "年月日干连"))
    }

    @Test fun `天上三奇 甲月戊日庚时紧连`() {
        val b = chartOf(
            HeavenlyStem.BING, EarthlyBranch.YIN, HeavenlyStem.JIA, EarthlyBranch.WU,
            HeavenlyStem.WU, EarthlyBranch.CHEN, HeavenlyStem.GENG, EarthlyBranch.SHEN)
        assertTrue("月日时干连：${names(b)}", names(b).contains("三奇贵人（天上）" to "月日时干连"))
    }

    @Test fun `间隔不连非三奇 甲年戊月X日庚时`() {
        // 甲子 戊辰 壬申 庚子——甲戊庚分居年月时，相隔不连
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.ZI, HeavenlyStem.WU, EarthlyBranch.CHEN,
            HeavenlyStem.REN, EarthlyBranch.SHEN, HeavenlyStem.GENG, EarthlyBranch.ZI)
        assertTrue("间隔不为三奇：${names(b)}", names(b).isEmpty())
    }

    @Test fun `地下三奇乙丙丁 人中三奇壬癸辛`() {
        val b1 = chartOf(
            HeavenlyStem.YI, EarthlyBranch.WEI, HeavenlyStem.BING, EarthlyBranch.XU,
            HeavenlyStem.DING, EarthlyBranch.MAO, HeavenlyStem.REN, EarthlyBranch.YIN)
        assertTrue("地下乙丙丁年月日连：${names(b1)}", names(b1).contains("三奇贵人（地下）" to "年月日干连"))
        val b2 = chartOf(
            HeavenlyStem.GENG, EarthlyBranch.YIN, HeavenlyStem.REN, EarthlyBranch.WU,
            HeavenlyStem.GUI, EarthlyBranch.WEI, HeavenlyStem.XIN, EarthlyBranch.YOU)
        assertTrue("人中壬癸辛月日时连：${names(b2)}", names(b2).contains("三奇贵人（人中）" to "月日时干连"))
    }

    @Test fun `逆乱不取 庚戊甲倒序`() {
        // 庚午 戊子 甲寅 丙寅——庚戊甲逆序非三奇
        val b = chartOf(
            HeavenlyStem.GENG, EarthlyBranch.WU, HeavenlyStem.WU, EarthlyBranch.ZI,
            HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.BING, EarthlyBranch.YIN)
        assertTrue("逆乱不为三奇：${names(b)}", names(b).isEmpty())
    }
}
