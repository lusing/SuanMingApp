package cn.xulun.bazi

import cn.xulun.bazi.core.MingGongCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Pillar
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 命宫/胎元/小限/小运锚点测试。理论见 .shengke-reference.md 第廿二~廿四节，例均出自教材。
 */
class MingGongCalculatorTest {

    // ── 命宫：教材两例（第二十二节） ──

    @Test fun `命宫例一 甲子年壬申月乙亥时得甲戌`() {
        // 26-(申7+亥10)=9→戌；甲年五虎遁（寅起丙）至戌月得甲
        val mg = MingGongCalculator.mingGongOf(
            monthBranch = EarthlyBranch.SHEN, hourBranch = EarthlyBranch.HAI,
            pastZhongQi = false, yearStem = HeavenlyStem.JIA,
        )
        assertEquals(Pillar(HeavenlyStem.JIA, EarthlyBranch.XU), mg)
    }

    @Test fun `命宫例二 甲子年寅月卯时得丙子`() {
        // 26-(1+2)=23>12→-12=11→子；甲年遁至子月得丙
        val mg = MingGongCalculator.mingGongOf(
            monthBranch = EarthlyBranch.YIN, hourBranch = EarthlyBranch.MAO,
            pastZhongQi = false, yearStem = HeavenlyStem.JIA,
        )
        assertEquals(Pillar(HeavenlyStem.BING, EarthlyBranch.ZI), mg)
    }

    // ── 胎元：月干进一位、月支进三位 ──

    @Test fun `胎元 己未月得庚戌`() {
        // 通关一例：戊午 己未 壬申 庚子——月柱己未→胎元庚戌
        val r = MingGongCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.WU,
            HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.REN, EarthlyBranch.SHEN,
            HeavenlyStem.GENG, EarthlyBranch.ZI,
        ), Gender.MALE)
        assertEquals(Pillar(HeavenlyStem.GENG, EarthlyBranch.XU), r.taiYuan)
    }

    // ── 小限：命宫起逐年逆行（教材例：戊午年命宫丁巳） ──

    @Test fun `小限 丁巳命宫逐年丙辰乙卯甲寅`() {
        val dingSi = Pillar(HeavenlyStem.DING, EarthlyBranch.SI)
        assertEquals(Pillar(HeavenlyStem.BING, EarthlyBranch.CHEN), MingGongCalculator.xiaoXianOf(dingSi, 1))
        assertEquals(Pillar(HeavenlyStem.YI, EarthlyBranch.MAO), MingGongCalculator.xiaoXianOf(dingSi, 2))
        assertEquals(Pillar(HeavenlyStem.JIA, EarthlyBranch.YIN), MingGongCalculator.xiaoXianOf(dingSi, 3))
    }

    @Test fun `小限 出生年即命宫`() {
        val r = MingGongCalculator.analyze(chartOf(
            HeavenlyStem.GENG, EarthlyBranch.YIN,
            HeavenlyStem.GUI, EarthlyBranch.WEI,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.REN, EarthlyBranch.SHEN,
        ), Gender.MALE, flowYear = null)
        assertEquals(r.mingGong, r.xiaoXian)
    }

    // ── 小运：时柱起，阳男阴女顺、阴男阳女逆（教材例：庚戌乾造时柱甲申） ──

    @Test fun `小运 阳年男顺排 乙酉丙戌丁亥`() {
        // 庚戌年（阳）乾造，时柱甲申：1岁乙酉、2岁丙戌、3岁丁亥（教材明言"其一岁为乙酉"）
        val jiaShen = Pillar(HeavenlyStem.JIA, EarthlyBranch.SHEN)
        assertEquals(Pillar(HeavenlyStem.YI, EarthlyBranch.YOU), MingGongCalculator.xiaoYunOf(jiaShen, true, 1))
        assertEquals(Pillar(HeavenlyStem.BING, EarthlyBranch.XU), MingGongCalculator.xiaoYunOf(jiaShen, true, 2))
        assertEquals(Pillar(HeavenlyStem.DING, EarthlyBranch.HAI), MingGongCalculator.xiaoYunOf(jiaShen, true, 3))
    }

    @Test fun `小运 阳年女逆排 癸未壬午辛巳庚辰`() {
        // 同八字坤造（阳年女逆）：1岁癸未、2岁壬午、3岁辛巳、4岁庚辰
        val jiaShen = Pillar(HeavenlyStem.JIA, EarthlyBranch.SHEN)
        assertEquals(Pillar(HeavenlyStem.GUI, EarthlyBranch.WEI), MingGongCalculator.xiaoYunOf(jiaShen, false, 1))
        assertEquals(Pillar(HeavenlyStem.REN, EarthlyBranch.WU), MingGongCalculator.xiaoYunOf(jiaShen, false, 2))
        assertEquals(Pillar(HeavenlyStem.XIN, EarthlyBranch.SI), MingGongCalculator.xiaoYunOf(jiaShen, false, 3))
        assertEquals(Pillar(HeavenlyStem.GENG, EarthlyBranch.CHEN), MingGongCalculator.xiaoYunOf(jiaShen, false, 4))
    }

    @Test fun `小运 集成 庚寅阳年男时柱壬申`() {
        // 强三例：庚寅（阳年）男，时柱壬申——2岁（次年）癸酉
        val r = MingGongCalculator.analyze(chartOf(
            HeavenlyStem.GENG, EarthlyBranch.YIN,
            HeavenlyStem.GUI, EarthlyBranch.WEI,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.REN, EarthlyBranch.SHEN,
        ), Gender.MALE, flowYear = r2())
        assertEquals(Pillar(HeavenlyStem.JIA, EarthlyBranch.XU), r.xiaoYun)
        assertEquals(2, r.xiaoYunAge)
    }

    @Test fun `小运 集成 同造女命逆行辛未`() {
        val r = MingGongCalculator.analyze(chartOf(
            HeavenlyStem.GENG, EarthlyBranch.YIN,
            HeavenlyStem.GUI, EarthlyBranch.WEI,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.REN, EarthlyBranch.SHEN,
        ), Gender.FEMALE, flowYear = r2())
        assertEquals(Pillar(HeavenlyStem.GENG, EarthlyBranch.WU), r.xiaoYun)
    }

    // ── 命宫星曜抽查 ──

    @Test fun `命宫星 戌宫天艺星`() {
        val r = MingGongCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.CHEN,
            HeavenlyStem.REN, EarthlyBranch.XU,
            HeavenlyStem.YI, EarthlyBranch.CHOU,
            HeavenlyStem.GUI, EarthlyBranch.WEI,
        ), Gender.MALE)
        // 四库全例：戊辰 壬戌 乙丑 癸未——只验星名与命宫支一致
        assertEquals(r.mingGong.branch.let { MingGongCalculator.starNameOf(it) }, r.starName)
    }

    private fun r2(): Int {
        // 强三例的真实出生年（反推所得）+1
        return MingGongCalculator.birthYearForSui(chartOf(
            HeavenlyStem.GENG, EarthlyBranch.YIN,
            HeavenlyStem.GUI, EarthlyBranch.WEI,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.REN, EarthlyBranch.SHEN,
        ).birthDateTime) + 1
    }
}
