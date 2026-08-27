package cn.xulun.bazi

import cn.xulun.bazi.core.ShenShaCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 勾绞煞（性别·年支前三后三）、孤辰寡宿（年支方位组）、六厄（三合死地·年支独查）锚点。
 * 理论见 .shengke-reference.md 第五八节八、九、十。
 */
class GouJiaoGuChenTest {

    @Test fun `勾煞 甲子年阳男前三辰卯为勾`() {
        // 甲子 丙寅 甲申 丁卯——阳年男：子前三=卯：时支卯（丁卯=甲日卯时✓）命中
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.ZI, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.SHEN, HeavenlyStem.DING, EarthlyBranch.MAO)
        assertTrue("甲子阳男勾在卯（时支）",
            ShenShaCalculator.analyze(b, true).items.any { it.name == "勾煞" && it.branch == EarthlyBranch.MAO })
    }

    @Test fun `孤辰寡宿 寅卯辰见巳孤丑寡`() {
        // 甲辰 丙寅 甲申 乙丑——年支辰（寅卯辰→孤巳寡丑）：时支丑=寡宿（巳无）
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.CHEN, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.SHEN, HeavenlyStem.YI, EarthlyBranch.CHOU)
        val items = ShenShaCalculator.analyze(b).items
        assertTrue("辰年寡宿丑（时支）", items.any { it.name == "寡宿" && it.branch == EarthlyBranch.CHOU && it.pillarLabel == "时支" })
    }

    @Test fun `孤辰 亥子丑见寅`() {
        // 甲子 丙寅 甲申 乙丑——年支子（亥子丑→孤寅）：月支寅=孤辰
        val b = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.ZI, HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.SHEN, HeavenlyStem.YI, EarthlyBranch.CHOU)
        assertTrue("子年孤辰寅（月支）",
            ShenShaCalculator.analyze(b).items.any { it.name == "孤辰" && it.branch == EarthlyBranch.YIN && it.pillarLabel == "月支" })
    }

    @Test fun `六厄 申子辰见卯（水死卯）`() {
        // 壬子 壬寅 壬申 辛丑——年支子（申子辰→卯）：柱无卯；换：壬子 壬寅 壬卯 辛丑? 卯日壬寅月——
        // 用：壬子 壬寅 癸卯 甲寅——年支子组→卯：日支卯命中
        val b = chartOf(
            HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.GUI, EarthlyBranch.MAO, HeavenlyStem.JIA, EarthlyBranch.YIN)
        assertTrue("子年六厄卯（日支）",
            ShenShaCalculator.analyze(b).items.any { it.name == "六厄" && it.branch == EarthlyBranch.MAO && it.pillarLabel == "日支" })
    }
}
