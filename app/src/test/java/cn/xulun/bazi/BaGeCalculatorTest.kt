package cn.xulun.bazi

import cn.xulun.bazi.core.BaGeCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 八格定格法锚点测试。十二例出自 .shengke-reference.md 第五一节（四法各三例），
 * 均真实日期反推；另附建禄/阳刃两例（月令比劫不入八格）。
 */
class BaGeCalculatorTest {

    private fun nameOf(vararg p: Any): String {
        val b = chartOf(p[0] as HeavenlyStem, p[1] as EarthlyBranch, p[2] as HeavenlyStem, p[3] as EarthlyBranch,
            p[4] as HeavenlyStem, p[5] as EarthlyBranch, p[6] as HeavenlyStem, p[7] as EarthlyBranch)
        return BaGeCalculator.determine(b).name
    }

    // ── 法一：月令本气透干 ──

    @Test fun `法一例一 戊辰甲寅己丑丁卯 正官格`() {
        assertEquals("正官格", nameOf(
            HeavenlyStem.WU, EarthlyBranch.CHEN, HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.JI, EarthlyBranch.CHOU, HeavenlyStem.DING, EarthlyBranch.MAO))
    }

    @Test fun `法一例二 丁酉乙巳己未丙寅 正印格`() {
        assertEquals("正印格", nameOf(
            HeavenlyStem.DING, EarthlyBranch.YOU, HeavenlyStem.YI, EarthlyBranch.SI,
            HeavenlyStem.JI, EarthlyBranch.WEI, HeavenlyStem.BING, EarthlyBranch.YIN))
    }

    @Test fun `法一例三 丁丑丙午甲辰乙亥 伤官格`() {
        assertEquals("伤官格", nameOf(
            HeavenlyStem.DING, EarthlyBranch.CHOU, HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.JIA, EarthlyBranch.CHEN, HeavenlyStem.YI, EarthlyBranch.HAI))
    }

    // ── 法二：本气不透，支中他藏干透出 ──

    @Test fun `法二例一 庚辰戊寅丙辰癸巳 食神格`() {
        assertEquals("食神格", nameOf(
            HeavenlyStem.GENG, EarthlyBranch.CHEN, HeavenlyStem.WU, EarthlyBranch.YIN,
            HeavenlyStem.BING, EarthlyBranch.CHEN, HeavenlyStem.GUI, EarthlyBranch.SI))
    }

    @Test fun `法二例二 丙申辛丑丁巳乙巳 偏财格`() {
        assertEquals("偏财格", nameOf(
            HeavenlyStem.BING, EarthlyBranch.SHEN, HeavenlyStem.XIN, EarthlyBranch.CHOU,
            HeavenlyStem.DING, EarthlyBranch.SI, HeavenlyStem.YI, EarthlyBranch.SI))
    }

    @Test fun `法二例三 丁卯辛亥戊辰甲寅 七杀格`() {
        assertEquals("七杀格", nameOf(
            HeavenlyStem.DING, EarthlyBranch.MAO, HeavenlyStem.XIN, EarthlyBranch.HAI,
            HeavenlyStem.WU, EarthlyBranch.CHEN, HeavenlyStem.JIA, EarthlyBranch.YIN))
    }

    // ── 法三：两藏干并透，较力量强弱 ──

    @Test fun `法三例一 丁酉庚戌甲寅辛未 辛强丁微取正官格`() {
        assertEquals("正官格", nameOf(
            HeavenlyStem.DING, EarthlyBranch.YOU, HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.XIN, EarthlyBranch.WEI))
    }

    @Test fun `法三例二 癸亥丙辰庚午乙酉 癸旺于乙取伤官格`() {
        assertEquals("伤官格", nameOf(
            HeavenlyStem.GUI, EarthlyBranch.HAI, HeavenlyStem.BING, EarthlyBranch.CHEN,
            HeavenlyStem.GENG, EarthlyBranch.WU, HeavenlyStem.YI, EarthlyBranch.YOU))
    }

    @Test fun `法三例三 甲午壬申丙寅戊子 壬强戊弱取七杀格`() {
        assertEquals("七杀格", nameOf(
            HeavenlyStem.JIA, EarthlyBranch.WU, HeavenlyStem.REN, EarthlyBranch.SHEN,
            HeavenlyStem.BING, EarthlyBranch.YIN, HeavenlyStem.WU, EarthlyBranch.ZI))
    }

    // ── 法四：藏干皆不透，取最强；单藏直接本气 ──

    @Test fun `法四例一 戊戌乙丑丙寅戊子 己土最强取伤官格`() {
        assertEquals("伤官格", nameOf(
            HeavenlyStem.WU, EarthlyBranch.XU, HeavenlyStem.YI, EarthlyBranch.CHOU,
            HeavenlyStem.BING, EarthlyBranch.YIN, HeavenlyStem.WU, EarthlyBranch.ZI))
    }

    @Test fun `法四例二 丙辰丙申甲子丁卯 三合水局壬增取偏印格`() {
        assertEquals("偏印格", nameOf(
            HeavenlyStem.BING, EarthlyBranch.CHEN, HeavenlyStem.BING, EarthlyBranch.SHEN,
            HeavenlyStem.JIA, EarthlyBranch.ZI, HeavenlyStem.DING, EarthlyBranch.MAO))
    }

    @Test fun `法四例三 乙丑乙酉丙午己亥 酉单藏辛取正财格`() {
        assertEquals("正财格", nameOf(
            HeavenlyStem.YI, EarthlyBranch.CHOU, HeavenlyStem.YI, EarthlyBranch.YOU,
            HeavenlyStem.BING, EarthlyBranch.WU, HeavenlyStem.JI, EarthlyBranch.HAI))
    }

    // ── 建禄 / 阳刃（月令比劫不入八格） ──

    @Test fun `甲生寅月为建禄格`() {
        // 甲辰丙寅甲午丁卯——甲日寅月本气甲比肩
        assertEquals("建禄格", nameOf(
            HeavenlyStem.JIA, EarthlyBranch.CHEN, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.WU, HeavenlyStem.DING, EarthlyBranch.MAO))
    }

    @Test fun `甲生卯月为阳刃格`() {
        // 乙卯己卯甲子丁卯——甲日卯月本气乙劫财（阳刃）
        assertEquals("阳刃格", nameOf(
            HeavenlyStem.YI, EarthlyBranch.MAO, HeavenlyStem.JI, EarthlyBranch.MAO,
            HeavenlyStem.JIA, EarthlyBranch.ZI, HeavenlyStem.DING, EarthlyBranch.MAO))
    }
}
