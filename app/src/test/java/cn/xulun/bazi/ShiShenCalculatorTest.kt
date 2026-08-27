package cn.xulun.bazi

import cn.xulun.bazi.core.ShiShenCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 十神关系（柱位寓意·正印）锚点。理论见 .shengke-reference.md 第六三节。
 */
class ShiShenCalculatorTest {

    @Test fun `年干正印透出查表`() {
        // 乙巳 戊子 丙午 庚寅——丙日年干乙（阴木生阳火异性）=正印
        val b = chartOf(
            HeavenlyStem.YI, EarthlyBranch.SI, HeavenlyStem.WU, EarthlyBranch.ZI,
            HeavenlyStem.BING, EarthlyBranch.WU, HeavenlyStem.GENG, EarthlyBranch.YIN)
        val items = ShiShenCalculator.analyze(b).items
        assertTrue("年干正印应出寓意：${items.map { it.label }}",
            items.any { it.label.startsWith("年干正印") && it.meaning.contains("书香") })
    }

    @Test fun `月干正印透出`() {
        // 甲子 丙寅 甲戌 乙丑? 月丙=食神。用：戊辰 乙卯 丙午 戊子——月乙对丙=正印
        val b = chartOf(
            HeavenlyStem.WU, EarthlyBranch.CHEN, HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.BING, EarthlyBranch.WU, HeavenlyStem.WU, EarthlyBranch.ZI)
        val items = ShiShenCalculator.analyze(b).items
        assertTrue("月干正印：${items.map { it.label }}",
            items.any { it.label.startsWith("月干正印") && it.meaning.contains("学业") })
    }

    @Test fun `正印坐支组合`() {
        // 癸酉 甲寅 丙午 戊子——日支午藏丁=劫财（丙日）；时干戊=食神。
        // 用盘：甲子 丙寅 甲戌 乙丑? 甲日：戌藏戊=偏财。年甲=比肩。
        // 正印坐支需透干为正印的柱：戊辰 乙卯 丙午 戊子——月干乙（正印）坐卯（乙=正印）→正印坐正印
        val b = chartOf(
            HeavenlyStem.WU, EarthlyBranch.CHEN, HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.BING, EarthlyBranch.WU, HeavenlyStem.WU, EarthlyBranch.ZI)
        val items = ShiShenCalculator.analyze(b).items
        assertTrue("月干正印坐月支卯（正印坐正印）：${items.map { it.label }}",
            items.any { it.label.contains("月支坐正印") && it.meaning.contains("过于自尊") })
    }

    @Test fun `时干正印透出`() {
        // 丙子 庚寅 戊辰 乙卯? 时乙对戊=正官。用：甲子 丙寅 戊午 丁巳——时丁对戊=正印
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.ZI, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.WU, EarthlyBranch.WU, HeavenlyStem.DING, EarthlyBranch.SI)
        val items = ShiShenCalculator.analyze(b).items
        assertTrue("时干正印：${items.map { it.label }}",
            items.any { it.label.startsWith("时干正印") && it.meaning.contains("子嗣") })
    }

    @Test fun `喜忌注记非空`() {
        val b = chartOf(
            HeavenlyStem.GUI, EarthlyBranch.YOU, HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.BING, EarthlyBranch.WU, HeavenlyStem.WU, EarthlyBranch.ZI)
        val items = ShiShenCalculator.analyze(b).items
        assertTrue("每条寓意带喜忌注记", items.all { it.xiJi in listOf("喜用", "忌神", "中性") })
    }
}
