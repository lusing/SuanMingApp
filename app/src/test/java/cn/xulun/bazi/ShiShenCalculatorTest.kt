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

    @Test fun `年干偏印透出（月干非印与生母缘薄）`() {
        // 壬子 壬寅 甲戌 乙亥? 年壬对甲=偏印（阳生阳）
        val b = chartOf(
            HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.XU, HeavenlyStem.YI, EarthlyBranch.HAI)
        val items = ShiShenCalculator.analyze(b).items
        assertTrue("年干偏印：${'$'}{items.map { it.label }}",
            items.any { it.label.startsWith("年干偏印") && it.meaning.contains("继母") })
    }

    @Test fun `偏印坐支组合（枭神夺食）`() {
        // 庚戌 戊子 甲申 丙寅——月干戊对甲=偏财；时丙=食神。
        // 偏印坐食神盘：壬辰 壬子 庚戌 戊寅? 壬对庚=食神。
        // 用：甲寅 丙寅 戊戌 甲寅? 丙对戊=偏印坐寅（甲=七杀）
        // 偏印坐食神：透干偏印+支藏本气食神。戊日偏印=丙，食神=庚。
        // 盘：丙辰 庚寅 戊申 庚申——年干丙（偏印）坐辰（戊=比肩）→偏印坐比肩
        val b = chartOf(
            HeavenlyStem.BING, EarthlyBranch.CHEN, HeavenlyStem.GENG, EarthlyBranch.YIN,
            HeavenlyStem.WU, EarthlyBranch.SHEN, HeavenlyStem.GENG, EarthlyBranch.SHEN)
        val items = ShiShenCalculator.analyze(b).items
        assertTrue("偏印坐支：${'$'}{items.map { it.label }}",
            items.any { it.label.contains("偏印坐") || it.meaning.contains("枭神") })
    }

    @Test fun `年干正官透出（祖上显贵）`() {
        // 庚寅 戊子 乙巳 丁丑? 庚对乙=正官（阳克阴）
        val b = chartOf(
            HeavenlyStem.GENG, EarthlyBranch.YIN, HeavenlyStem.WU, EarthlyBranch.ZI,
            HeavenlyStem.YI, EarthlyBranch.SI, HeavenlyStem.DING, EarthlyBranch.CHOU)
        val items = ShiShenCalculator.analyze(b).items
        assertTrue("年干正官：${'$'}{items.map { it.label }}",
            items.any { it.label.startsWith("年干正官") && it.meaning.contains("显贵") })
    }

    @Test fun `月干正官（身官两停大贵）`() {
        // 戊子 乙卯 庚申 丁丑? 乙对庚=正财非官。丁对庚=正官——庚日正官=丁。
        // 盘：丁卯 壬寅 庚申 丙子——月干壬=食神。用：戊辰 丁巳 庚申 丙子——月丁=正官
        val b = chartOf(
            HeavenlyStem.WU, EarthlyBranch.CHEN, HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.GENG, EarthlyBranch.SHEN, HeavenlyStem.BING, EarthlyBranch.ZI)
        val items = ShiShenCalculator.analyze(b).items
        assertTrue("月干正官：${'$'}{items.map { it.label }}",
            items.any { it.label.startsWith("月干正官") && it.meaning.contains("大贵") })
    }

    @Test fun `日支本气正官（婚姻美满）`() {
        // 甲子 丙寅 乙巳 戊寅——日支巳藏丙=伤官非官。乙日正官=庚。用：甲子 丙寅 乙酉 戊寅——酉藏辛=七杀。
        // 用：壬子 壬寅 丁亥 庚子——日支亥藏壬=正官（壬对丁阳克阴）
        val b = chartOf(
            HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.DING, EarthlyBranch.HAI, HeavenlyStem.GENG, EarthlyBranch.ZI)
        val items = ShiShenCalculator.analyze(b).items
        assertTrue("日支本气正官：${'$'}{items.map { it.label }}",
            items.any { it.label.startsWith("日支本气正官") && it.meaning.contains("婚姻美满") })
    }

    @Test fun `正官坐支组合（坐正印功名显达）`() {
        // 丁卯 壬寅 庚申 丙子? 月壬坐寅（甲=偏财）。正官坐正印：透干正官+支藏正印。
        // 庚日正官=丁，正印=己。盘：丁巳 己酉 庚申 丙子——年干丁（正官）坐巳（丙=七杀）
        // 盘：戊辰 乙卯 丁巳 庚子——月干乙（偏印）坐卯（乙=偏印）。
        // 用：甲辰 己巳 庚申 丁丑——月干己（正印）坐巳（丙=七杀）。
        // 正官坐正印需透干丁+支藏己：戊辰 丁未 庚申 己卯——月干丁（正官）坐未（己=正印）✓
        val b = chartOf(
            HeavenlyStem.REN, EarthlyBranch.CHEN, HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.GENG, EarthlyBranch.SHEN, HeavenlyStem.JI, EarthlyBranch.MAO)
        val items = ShiShenCalculator.analyze(b).items
        assertTrue("正官坐正印：${'$'}{items.map { it.label }}",
            items.any { it.meaning.contains("官印相生") })
    }
}
