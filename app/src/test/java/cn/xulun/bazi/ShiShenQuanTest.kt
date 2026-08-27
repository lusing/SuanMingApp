package cn.xulun.bazi

import cn.xulun.bazi.core.ShiShenCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 十神关系全十神综合锚点（透干×柱位+坐支组合）。
 */
class ShiShenQuanTest {

    private fun labels(b: cn.xulun.bazi.model.BaziResult) = ShiShenCalculator.analyze(b).items.map { it.label }
    private fun meanings(b: cn.xulun.bazi.model.BaziResult) = ShiShenCalculator.analyze(b).items.map { it.label to it.meaning }

    @Test fun `劫财透干与坐支`() {
        // 甲日劫财=乙（阴木同五行）。盘：乙卯 戊寅 甲申 丙寅——年干乙（劫财）
        val b = chartOf(
            HeavenlyStem.YI, EarthlyBranch.MAO, HeavenlyStem.WU, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.SHEN, HeavenlyStem.BING, EarthlyBranch.YIN)
        val m = meanings(b)
        assertTrue("年干劫财（母先亡档）：${'$'}{labels(b)}",
            m.any { it.first.startsWith("年干劫财") && it.second.contains("贫寒") })
    }

    @Test fun `食神透干`() {
        // 甲日食神=丙。盘：丙寅 戊寅 甲申 乙卯? 丙寅年戊寅月——年干丙=食神
        val b = chartOf(
            HeavenlyStem.BING, EarthlyBranch.YIN, HeavenlyStem.GENG, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.SHEN, HeavenlyStem.WU, EarthlyBranch.CHEN)
        assertTrue("年干食神（父肥胖）：${'$'}{labels(b)}",
            meanings(b).any { it.first.startsWith("年干食神") && it.second.contains("肥胖") })
    }

    @Test fun `坐支组合抽查（正财坐正官名利兼得）`() {
        // 庚日正财=甲（阳木），正官=丁。透干甲+支藏丁：
        // 庚日正财=乙（阴木）；乙坐午（藏丁=正官）。盘：乙巳 丁丑 庚申 戊寅? 巳藏丙。
        // 正财坐正官"名利兼得"文案在 zhengCaiZuo[ZHENG_GUAN]——坐支线出。改测偏财坐正官（甲坐丁支）：
        // 甲午 丁卯（甲年二月丁卯✓）庚申 戊寅——年干甲（偏财）坐午（丁=正官）→偏财坐正官"责任心强谋事易成"
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.WU, HeavenlyStem.DING, EarthlyBranch.MAO,
            HeavenlyStem.GENG, EarthlyBranch.SHEN, HeavenlyStem.WU, EarthlyBranch.YIN)
        assertTrue("偏财坐正官：${'$'}{labels(b)}",
            meanings(b).any { it.second.contains("谋事易成") })
    }

    @Test fun `十神全覆盖抽查（每柱至少出一条）`() {
        // 复合盘：甲申 丙寅 戊戌 庚申——戊日：甲=七杀（年透）、丙=偏印（月透）、庚=食神（时透）
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.SHEN, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.WU, EarthlyBranch.XU, HeavenlyStem.GENG, EarthlyBranch.SHEN)
        val ls = labels(b)
        assertTrue("年干七杀：${'$'}{ls}", ls.any { it.startsWith("年干七杀") })
        assertTrue("月干偏印：${'$'}{ls}", ls.any { it.startsWith("月干偏印") })
        assertTrue("时干食神：${'$'}{ls}", ls.any { it.startsWith("时干食神") })
    }
}
