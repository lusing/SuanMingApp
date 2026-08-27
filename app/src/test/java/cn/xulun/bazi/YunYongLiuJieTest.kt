package cn.xulun.bazi

import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 神煞运用举例·七~十二节（羊刃/桃花/罗网/阴阳差错/空亡/灾煞）。理论见 .shengke-reference.md 第五九节七~十二。
 */
class YunYongLiuJieTest {

    private fun items(b: cn.xulun.bazi.model.BaziResult) = ShenShaCalculator.analyze(b).items

    @Test fun `七 甲辰癸酉庚午乙酉 阳刃重重月时双酉`() {
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.CHEN, HeavenlyStem.GUI, EarthlyBranch.YOU,
            HeavenlyStem.GENG, EarthlyBranch.WU, HeavenlyStem.YI, EarthlyBranch.YOU)
        val yr = items(b).filter { it.name == "羊刃" }
        assertTrue("庚日刃酉月时双见（1981辛酉三酉自刑刀伤骨折之验）",
            yr.any { it.pillarLabel == "月支" } && yr.any { it.pillarLabel == "时支" })
    }

    @Test fun `八 两造桃花皆值流年（原局无桃花支）`() {
        val b1 = chartOf(
            HeavenlyStem.WU, EarthlyBranch.SHEN, HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.GENG, EarthlyBranch.ZI)
        assertTrue("戊申甲寅壬子庚子原局无酉（癸酉年桃花值岁结婚）", items(b1).none { it.name == "桃花" })
        val b2 = chartOf(
            HeavenlyStem.YI, EarthlyBranch.WEI, HeavenlyStem.GENG, EarthlyBranch.CHEN,
            HeavenlyStem.REN, EarthlyBranch.XU, HeavenlyStem.XIN, EarthlyBranch.CHOU)
        assertTrue("乙未庚辰壬戌辛丑原局无子（丙子年桃花+申子辰水局合旺结婚）", items(b2).none { it.name == "桃花" })
    }

    @Test fun `九 丁亥甲辰丁巳庚戌 天罗地网全见（判刑三年）`() {
        val b = chartOf(
            HeavenlyStem.DING, EarthlyBranch.HAI, HeavenlyStem.JIA, EarthlyBranch.CHEN,
            HeavenlyStem.DING, EarthlyBranch.SI, HeavenlyStem.GENG, EarthlyBranch.XU)
        assertTrue("辰戌巳亥全——牢狱标志（1991辛未罗网重见官讼败诉）",
            items(b).any { it.name == "天罗地网" })
    }

    @Test fun `十 庚戌甲申癸亥戊午 阴阳差错+妻宫羊刃`() {
        val b1 = chartOf(
            HeavenlyStem.GENG, EarthlyBranch.XU, HeavenlyStem.JIA, EarthlyBranch.SHEN,
            HeavenlyStem.GUI, EarthlyBranch.HAI, HeavenlyStem.WU, EarthlyBranch.WU)
        assertTrue("癸亥日阴阳差错", items(b1).any { it.name == "阴阳差错" })
        assertTrue("癸日羊刃亥居妻宫（两度新郎）", items(b1).any { it.name == "羊刃" && it.branch == EarthlyBranch.HAI && it.pillarLabel == "日支" })
        val b2 = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.CHEN, HeavenlyStem.YI, EarthlyBranch.HAI,
            HeavenlyStem.REN, EarthlyBranch.XU, HeavenlyStem.JI, EarthlyBranch.YOU)
        assertTrue("壬戌日阴阳差错（离婚二次）", items(b2).any { it.name == "阴阳差错" })
    }

    @Test fun `十一 辛卯丙申丙午己丑 年日互换空亡+日坐阴阳差错`() {
        val b1 = chartOf(
            HeavenlyStem.XIN, EarthlyBranch.MAO, HeavenlyStem.BING, EarthlyBranch.SHEN,
            HeavenlyStem.BING, EarthlyBranch.WU, HeavenlyStem.JI, EarthlyBranch.CHOU)
        val k = items(b1).filter { it.name == "空亡" }
        assertTrue("丙午日甲辰旬空寅卯→年支卯；辛卯年甲申旬空午未→日支午（互换空亡）",
            k.any { it.branch == EarthlyBranch.MAO && it.pillarLabel == "年支" } && k.any { it.branch == EarthlyBranch.WU && it.pillarLabel == "日支" })
        assertTrue("丙午日又坐阴阳差错（两婚不睦）", items(b1).any { it.name == "阴阳差错" })
        val b2 = chartOf(
            HeavenlyStem.BING, EarthlyBranch.XU, HeavenlyStem.JI, EarthlyBranch.HAI,
            HeavenlyStem.YI, EarthlyBranch.MAO, HeavenlyStem.REN, EarthlyBranch.WU)
        assertTrue("丙戌年甲申旬空午未→时支午（时柱空亡不利子息：三年才得一子）",
            items(b2).any { it.name == "空亡" && it.branch == EarthlyBranch.WU && it.pillarLabel == "时支" })
    }

    @Test fun `十二 庚戌己卯戊子壬戌 日支灾煞逢刑（贪污拘禁）；丁未造无灾煞`() {
        val b1 = chartOf(
            HeavenlyStem.GENG, EarthlyBranch.XU, HeavenlyStem.JI, EarthlyBranch.MAO,
            HeavenlyStem.WU, EarthlyBranch.ZI, HeavenlyStem.REN, EarthlyBranch.XU)
        assertTrue("戌年组灾煞子居日支+子卯刑（1996丙子灾煞当值入狱）",
            items(b1).any { it.name == "灾煞" && it.branch == EarthlyBranch.ZI && it.pillarLabel == "日支" })
        val b2 = chartOf(
            HeavenlyStem.DING, EarthlyBranch.WEI, HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.JIA, EarthlyBranch.ZI, HeavenlyStem.DING, EarthlyBranch.MAO)
        assertTrue("丁未戊申甲子丁卯确无灾煞（未组灾煞酉——1996两子刑卯坐牢乃凶局非神煞）",
            items(b2).none { it.name == "灾煞" })
    }
}
