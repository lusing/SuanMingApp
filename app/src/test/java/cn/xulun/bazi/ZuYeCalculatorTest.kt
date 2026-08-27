package cn.xulun.bazi

import cn.xulun.bazi.core.LiuQinCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 六亲·祖业断语测试（教材 13 例锚定，.liuqin-reference.md 第一节）。
 *
 * 喜忌由 BingYaoCalculator（扶抑派）判定；断语按年柱（祖上宫）十神喜忌 + 神煞 + 冲合给出。
 */
class ZuYeCalculatorTest {

    private fun zuYe(
        ys: HeavenlyStem, yb: EarthlyBranch,
        ms: HeavenlyStem, mb: EarthlyBranch,
        ds: HeavenlyStem, db: EarthlyBranch,
        hs: HeavenlyStem, hb: EarthlyBranch,
        female: Boolean = false,
    ) = LiuQinCalculator.analyze(
        chartOf(ys, yb, ms, mb, ds, db, hs, hb),
        if (female) Gender.FEMALE else Gender.MALE,
    ).zuYe

    // ── 1. 祖上望族 ──

    @Test
    fun `例1 乙丑丁亥乙丑丁丑 财官印全喜用 祖上荣华得福荫`() {
        val z = zuYe(
            HeavenlyStem.YI, EarthlyBranch.CHOU,
            HeavenlyStem.DING, EarthlyBranch.HAI,
            HeavenlyStem.YI, EarthlyBranch.CHOU,
            HeavenlyStem.DING, EarthlyBranch.CHOU,
        )
        assertTrue(z.favorable.any { it.dim.contains("财官印全") && it.text.contains("荣华") })
        assertTrue(z.summary.isNotBlank())
    }

    @Test
    fun `例2 癸丑乙卯丙午庚寅 官杀年柱旺地身强 风光威仪且先旺后败`() {
        val z = zuYe(
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.GENG, EarthlyBranch.YIN,
        )
        assertTrue(z.favorable.any { it.dim.contains("官杀") && it.text.contains("威仪") })
        assertTrue(z.unfavorable.any { it.text.contains("先旺后败") })
    }

    @Test
    fun `例3 乙卯己丑丙寅丙申 正印年柱身衰喜印 书香门第`() {
        val z = zuYe(
            HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.JI, EarthlyBranch.CHOU,
            HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.BING, EarthlyBranch.SHEN,
            female = true,
        )
        assertTrue(z.favorable.any { it.dim.contains("正印") && it.text.contains("书香") })
    }

    // ── 2. 祖上富翁 ──

    @Test
    fun `例4 丙午癸巳壬申壬子 财居年月有力喜用 禄裕丰隆`() {
        val z = zuYe(
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.GUI, EarthlyBranch.SI,
            HeavenlyStem.REN, EarthlyBranch.SHEN,
            HeavenlyStem.REN, EarthlyBranch.ZI,
        )
        assertTrue(z.favorable.any { it.dim.contains("财") && it.text.contains("禄裕丰隆") })
    }

    @Test
    fun `例5 庚子丁亥己未戊辰 财旺年柱身强 万贯家资`() {
        val z = zuYe(
            HeavenlyStem.GENG, EarthlyBranch.ZI,
            HeavenlyStem.DING, EarthlyBranch.HAI,
            HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.WU, EarthlyBranch.CHEN,
        )
        assertTrue(z.favorable.any { it.dim.contains("财旺") && it.text.contains("万贯家资") })
    }

    @Test
    fun `例6 辛丑庚子甲申戊辰 年上正官不忌无冲 祖上富贵`() {
        val z = zuYe(
            HeavenlyStem.XIN, EarthlyBranch.CHOU,
            HeavenlyStem.GENG, EarthlyBranch.ZI,
            HeavenlyStem.JIA, EarthlyBranch.SHEN,
            HeavenlyStem.WU, EarthlyBranch.CHEN,
        )
        assertTrue(z.favorable.any { it.dim.contains("正官") && it.text.contains("富贵") })
    }

    // ── 3. 祖上艺人 ──

    @Test
    fun `例7 丁酉癸丑己丑丁卯 年上偏印为用 祖上技艺`() {
        val z = zuYe(
            HeavenlyStem.DING, EarthlyBranch.YOU,
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.JI, EarthlyBranch.CHOU,
            HeavenlyStem.DING, EarthlyBranch.MAO,
        )
        assertTrue(z.favorable.any { it.dim.contains("偏印") && it.text.contains("技艺") })
    }

    @Test
    fun `例8 乙巳戊子壬寅戊申 年上伤官为用 祖上技艺`() {
        val z = zuYe(
            HeavenlyStem.YI, EarthlyBranch.SI,
            HeavenlyStem.WU, EarthlyBranch.ZI,
            HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.WU, EarthlyBranch.SHEN,
        )
        assertTrue(z.favorable.any { it.dim.contains("伤官") && it.text.contains("技艺") })
    }

    // ── 4. 祖上贫贱 ──

    @Test
    fun `例9 己未己巳癸卯癸亥 七杀忌居年 祖上破财`() {
        val z = zuYe(
            HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.JI, EarthlyBranch.SI,
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
        )
        assertTrue(z.unfavorable.any { it.dim.contains("七杀") && it.text.contains("破财") })
    }

    @Test
    fun `例10 乙卯戊寅丁酉乙巳 枭神忌居年 祖上贫寒破落`() {
        val z = zuYe(
            HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.WU, EarthlyBranch.YIN,
            HeavenlyStem.DING, EarthlyBranch.YOU,
            HeavenlyStem.YI, EarthlyBranch.SI,
        )
        assertTrue(z.unfavorable.any { it.dim.contains("枭") && it.text.contains("贫寒") })
    }

    @Test
    fun `例11 乙巳戊子壬寅乙巳 伤官忌居年 祖上没落`() {
        val z = zuYe(
            HeavenlyStem.YI, EarthlyBranch.SI,
            HeavenlyStem.WU, EarthlyBranch.ZI,
            HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.YI, EarthlyBranch.SI,
        )
        assertTrue(z.unfavorable.any { it.dim.contains("伤官") && it.text.contains("没落") })
    }

    @Test
    fun `例12 癸丑庚申乙未辛巳 年支丑被未冲 祖上漂泊迁居`() {
        val z = zuYe(
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.YI, EarthlyBranch.WEI,
            HeavenlyStem.XIN, EarthlyBranch.SI,
        )
        assertTrue(z.unfavorable.any { it.dim.contains("年") && it.dim.contains("冲") && it.text.contains("漂泊") })
    }

    @Test
    fun `例13 甲寅丙寅甲申乙丑 马星临年柱遭冲 祖辈飘零`() {
        val z = zuYe(
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.SHEN,
            HeavenlyStem.YI, EarthlyBranch.CHOU,
        )
        assertTrue(z.unfavorable.any { it.dim.contains("马星") && it.text.contains("飘零") })
    }
}
