package cn.xulun.bazi

import cn.xulun.bazi.core.LiuQinCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 六亲·手足贵贱测试（教材第四节(五)实例锚定）。
 *
 * 比劫坐禄富而贵；月上官杀喜用无破兄弟必贵；月支空亡异性同胞弱；
 * 比劫干坐下遭克泄贫贱；禄刃落空为忌夭亡；月柱驿马逢冲远行。
 */
class ShouZuGuiJianTest {

    private fun xiongDi(
        ys: HeavenlyStem, yb: EarthlyBranch,
        ms: HeavenlyStem, mb: EarthlyBranch,
        ds: HeavenlyStem, db: EarthlyBranch,
        hs: HeavenlyStem, hb: EarthlyBranch,
        female: Boolean = false,
    ) = LiuQinCalculator.analyze(
        chartOf(ys, yb, ms, mb, ds, db, hs, hb),
        if (female) Gender.FEMALE else Gender.MALE,
    ).xiongDi

    @Test
    fun `坐禄例 甲寅乙亥乙丑戊寅 劫财坐禄 哥姐富贵均有`() {
        val x = xiongDi(
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.YI, EarthlyBranch.HAI,
            HeavenlyStem.YI, EarthlyBranch.CHOU,
            HeavenlyStem.WU, EarthlyBranch.YIN,
        )
        assertTrue(x.favorable.any { it.dim.contains("坐禄") && (it.text.contains("富") || it.text.contains("贵")) })
    }

    @Test
    fun `月官例 丙申丙申庚申壬午 月上杀星喜用通根无破 兄弟必贵`() {
        val x = xiongDi(
            HeavenlyStem.BING, EarthlyBranch.SHEN,
            HeavenlyStem.BING, EarthlyBranch.SHEN,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.REN, EarthlyBranch.WU,
        )
        assertTrue(x.favorable.any { it.dim.contains("官") && it.text.contains("贵") })
    }

    @Test
    fun `月空例 癸卯丙辰壬寅己酉 月支辰空亡 姐妹比自己弱`() {
        val x = xiongDi(
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.BING, EarthlyBranch.CHEN,
            HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.JI, EarthlyBranch.YOU,
        )
        assertTrue(x.unfavorable.any { it.dim.contains("空") && it.text.contains("弱") })
    }

    @Test
    fun `截脚例一 丁未乙巳乙亥丁亥 月干比肩坐巳被泄 哥哥贫贱`() {
        val x = xiongDi(
            HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.YI, EarthlyBranch.SI,
            HeavenlyStem.YI, EarthlyBranch.HAI,
            HeavenlyStem.DING, EarthlyBranch.HAI,
        )
        assertTrue(x.unfavorable.any { it.text.contains("贫") })
    }

    @Test
    fun `截脚例二 庚戌癸未壬辰辛亥 劫财癸坐未被旺土克 克弟兄`() {
        val x = xiongDi(
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.GUI, EarthlyBranch.WEI,
            HeavenlyStem.REN, EarthlyBranch.CHEN,
            HeavenlyStem.XIN, EarthlyBranch.HAI,
        )
        assertTrue(x.unfavorable.any { it.text.contains("克兄") || it.text.contains("病") })
    }

    @Test
    fun `例三 戊子辛酉癸亥甲寅 手足4个 禄落空为忌有夭亡之人`() {
        val x = xiongDi(
            HeavenlyStem.WU, EarthlyBranch.ZI,
            HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
        )
        assertTrue(x.count.contains("4"))
        assertTrue(x.unfavorable.any { it.text.contains("夭") })
    }

    @Test
    fun `例六 己亥己巳己酉壬申 月支驿马逢冲 兄弟中有远行之人`() {
        val x = xiongDi(
            HeavenlyStem.JI, EarthlyBranch.HAI,
            HeavenlyStem.JI, EarthlyBranch.SI,
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.REN, EarthlyBranch.SHEN,
        )
        assertTrue(x.favorable.any { it.dim.contains("驿马") && it.text.contains("远行") } ||
            x.unfavorable.any { it.dim.contains("驿马") && it.text.contains("远行") })
    }
}
