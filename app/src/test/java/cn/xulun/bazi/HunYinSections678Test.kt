package cn.xulun.bazi

import cn.xulun.bazi.core.DaYunCalculator
import cn.xulun.bazi.core.HunYinCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 婚姻六七八节测试:异性情缘/老少夫妻/婚外情。
 */
class HunYinSections678Test {

    private fun analyze(
        ys: HeavenlyStem, yb: EarthlyBranch,
        ms: HeavenlyStem, mb: EarthlyBranch,
        ds: HeavenlyStem, db: EarthlyBranch,
        hs: HeavenlyStem, hb: EarthlyBranch,
        female: Boolean = false,
        fromYear: Int = 1950,
        toYear: Int = 1980,
    ): cn.xulun.bazi.model.HunYinResult {
        val bazi = chartOf(ys, yb, ms, mb, ds, db, hs, hb, fromYear, toYear)
        val gender = if (female) Gender.FEMALE else Gender.MALE
        return HunYinCalculator.analyze(bazi, gender, DaYunCalculator.compute(bazi, gender))
    }

    // ── 六、异性情缘 ──

    @Test
    fun `异性缘例一 黄先生 年月日均有财 异性缘非常好`() {
        val r = analyze(
            HeavenlyStem.BING, EarthlyBranch.SHEN,
            HeavenlyStem.DING, EarthlyBranch.YOU,
            HeavenlyStem.GUI, EarthlyBranch.SI,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
        )
        assertTrue(r.baseItems.any { it.dim.contains("遍地财") && it.text.contains("异性缘") })
    }

    @Test
    fun `异性缘例五 某先生 身弱伤重无财化 有色心无色胆`() {
        val r = analyze(
            HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.JIA, EarthlyBranch.CHEN,
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.JIA, EarthlyBranch.CHEN,
        )
        assertTrue(r.baseUnfav.any { it.dim.contains("异性缘薄") })
    }

    // ── 七、老少夫妻 ──

    @Test
    fun `老少例一 李小姐 七杀居年入墓 必嫁老夫`() {
        val r = analyze(
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.GUI, EarthlyBranch.WEI,
            HeavenlyStem.REN, EarthlyBranch.CHEN,
            HeavenlyStem.XIN, EarthlyBranch.HAI,
            female = true,
        )
        assertTrue(r.baseUnfav.any { it.dim.contains("招老夫") })
    }

    @Test
    fun `老少例七 某女士 伤旺克夫无财化 找老夫`() {
        val r = analyze(
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.GUI, EarthlyBranch.SI,
            HeavenlyStem.JIA, EarthlyBranch.WU,
            HeavenlyStem.DING, EarthlyBranch.MAO,
            female = true,
        )
        assertTrue(r.baseUnfav.any { it.text.contains("找老夫") || it.dim.contains("白头郎") })
    }

    @Test
    fun `老少例八 某女士 官星临近日干 年龄相近`() {
        val r = analyze(
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.WU, EarthlyBranch.ZI,
            HeavenlyStem.YI, EarthlyBranch.MAO,
            female = true,
        )
        assertTrue(r.baseItems.any { it.dim.contains("年龄相近") })
    }

    // ── 八、婚外情 ──

    @Test
    fun `婚外例二 陈先生 宫月刑时害食伤旺 婚外情倾向`() {
        val r = analyze(
            HeavenlyStem.YI, EarthlyBranch.SI,
            HeavenlyStem.GUI, EarthlyBranch.WEI,
            HeavenlyStem.BING, EarthlyBranch.XU,
            HeavenlyStem.DING, EarthlyBranch.YOU,
        )
        assertTrue(r.baseUnfav.any { it.dim.contains("婚外情倾向") })
    }

    @Test
    fun `婚外例四 梁先生 宫三伤财星刑入宫 婚外情倾向`() {
        val r = analyze(
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JI, EarthlyBranch.MAO,
            HeavenlyStem.JIA, EarthlyBranch.ZI,
        )
        assertTrue(r.baseUnfav.any { it.dim.contains("婚外情倾向") })
    }

    @Test
    fun `婚外例四 2003癸未 偏财透宫合入库 情人登场`() {
        val r = analyze(
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JI, EarthlyBranch.MAO,
            HeavenlyStem.JIA, EarthlyBranch.ZI,
        )
        val n = r.yuns.flatMap { it.years }.firstOrNull { it.year == 2003 }
        assertNotNull(n)
        assertTrue(n!!.triggers.any { it.contains("合动") || it.contains("暗合") || it.contains("三合") })
    }
}
