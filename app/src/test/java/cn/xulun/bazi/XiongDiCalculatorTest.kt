package cn.xulun.bazi

import cn.xulun.bazi.core.LiuQinCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 六亲·兄弟姐妹断语测试（教材实例锚定，.liuqin-reference.md 第四节）。
 *
 * 比劫为手足星（禄为比肩、刃为劫财、身弱无比劫时印星代之）；男比肩=兄弟/劫财=姐妹，女反是。
 * 计数口径：每处比劫 1 数（遭本气克之藏干不计），禄支额外 +1；
 * 身旺加倍 / 身弱加印 / 不当令众而相战减半 / 空亡墓门减数 / 从格按从势合并单元计数。
 */
class XiongDiCalculatorTest {

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
    fun `加倍例二 丙午庚寅丁酉庚子 旺相加倍6个 年月劫财非老大 比劫临桃花`() {
        val x = xiongDi(
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.GENG, EarthlyBranch.YIN,
            HeavenlyStem.DING, EarthlyBranch.YOU,
            HeavenlyStem.GENG, EarthlyBranch.ZI,
        )
        assertTrue(x.count.contains("6"))
        assertTrue(x.countBasis.contains("加倍"))
        assertTrue(x.ranking?.contains("老大") == true)
        assertTrue(x.favorable.any { it.dim.contains("桃花") })
    }

    @Test
    fun `减半例二 甲辰甲戌甲寅丁卯 不当令与旺土相战减半3个`() {
        val x = xiongDi(
            HeavenlyStem.JIA, EarthlyBranch.CHEN,
            HeavenlyStem.JIA, EarthlyBranch.XU,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.DING, EarthlyBranch.MAO,
        )
        assertTrue(x.count.contains("3"))
        assertTrue(x.countBasis.contains("减半"))
    }

    @Test
    fun `例七 癸亥己未甲辰乙亥 比劫喜用 手足关系好能照顾呵护弟妹`() {
        val x = xiongDi(
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.JIA, EarthlyBranch.CHEN,
            HeavenlyStem.YI, EarthlyBranch.HAI,
        )
        assertTrue(x.favorable.any { it.dim.contains("喜用") && it.text.contains("呵护") })
    }

    @Test
    fun `例八 乙丑癸未丁巳丁未 月令藏比肩 自己不是老大`() {
        val x = xiongDi(
            HeavenlyStem.YI, EarthlyBranch.CHOU,
            HeavenlyStem.GUI, EarthlyBranch.WEI,
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.DING, EarthlyBranch.WEI,
        )
        assertTrue(x.ranking?.contains("老大") == true)
    }

    @Test
    fun `加倍例一 乙巳己卯庚辰甲申 坤 身弱印比合计4个`() {
        val x = xiongDi(
            HeavenlyStem.YI, EarthlyBranch.SI,
            HeavenlyStem.JI, EarthlyBranch.MAO,
            HeavenlyStem.GENG, EarthlyBranch.CHEN,
            HeavenlyStem.JIA, EarthlyBranch.SHEN,
            female = true,
        )
        assertTrue(x.count.contains("4"))
    }

    @Test
    fun `从格例一 丁巳壬寅辛亥戊子 坤 从水木火按合并单元计数4个`() {
        val x = xiongDi(
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.XIN, EarthlyBranch.HAI,
            HeavenlyStem.WU, EarthlyBranch.ZI,
            female = true,
        )
        assertTrue(x.count.contains("4"))
        assertTrue(x.countBasis.contains("从"))
    }
}
