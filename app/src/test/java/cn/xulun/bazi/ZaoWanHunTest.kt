package cn.xulun.bazi

import cn.xulun.bazi.core.DaYunCalculator
import cn.xulun.bazi.core.HunYinCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 婚姻·早婚晚婚测试（教材第五节 14 例中静态可锚定者）。
 */
class ZaoWanHunTest {

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

    @Test
    fun `例一 刘女士 四柱纯阴无火调候 不利婚`() {
        val r = analyze(
            HeavenlyStem.XIN, EarthlyBranch.HAI,
            HeavenlyStem.JI, EarthlyBranch.HAI,
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.GUI, EarthlyBranch.YOU,
            female = true,
        )
        assertTrue(r.baseUnfav.any { it.dim.contains("纯阴") })
    }

    @Test
    fun `例二 某先生 印旺财囚宫空亡 难成婚`() {
        val r = analyze(
            HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.XU,
        )
        assertTrue(r.baseUnfav.any { it.dim.contains("难成婚") })
    }

    @Test
    fun `例六 谬先生 土金成势无财 比劫夺财晚婚`() {
        val r = analyze(
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.REN, EarthlyBranch.XU,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.GENG, EarthlyBranch.CHEN,
        )
        assertTrue(r.baseUnfav.any { it.dim.contains("夺财") || it.dim.contains("晚婚") })
    }

    @Test
    fun `例七 周先生 妻宫时合印旺 晚婚`() {
        val r = analyze(
            HeavenlyStem.WU, EarthlyBranch.ZI,
            HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            fromYear = 1945, toYear = 1955,
        )
        assertTrue(r.baseUnfav.any { it.dim.contains("时合") })
    }

    @Test
    fun `例八 李先生 假专旺众劫夺财 恋爱难成`() {
        val r = analyze(
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.YI, EarthlyBranch.CHOU,
            HeavenlyStem.WU, EarthlyBranch.CHEN,
            HeavenlyStem.BING, EarthlyBranch.CHEN,
        )
        assertTrue(r.baseUnfav.any { it.dim.contains("夺财") || it.dim.contains("晚婚") })
    }

    @Test
    fun `例九 王先生 财弱有食伤 妻迟子晚`() {
        val r = analyze(
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.YI, EarthlyBranch.HAI,
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.BING, EarthlyBranch.ZI,
        )
        assertTrue(r.baseUnfav.any { it.text.contains("妻迟子晚") })
    }

    @Test
    fun `例十一 殷先生 财旺年月 早婚`() {
        val r = analyze(
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.BING, EarthlyBranch.XU,
            HeavenlyStem.YI, EarthlyBranch.YOU,
            HeavenlyStem.WU, EarthlyBranch.YIN,
        )
        assertTrue(r.baseItems.any { it.dim.contains("早婚") })
    }

    @Test
    fun `例十三 罗先生 财旺透月干年支有财 婚早`() {
        val r = analyze(
            HeavenlyStem.JI, EarthlyBranch.CHOU,
            HeavenlyStem.REN, EarthlyBranch.SHEN,
            HeavenlyStem.WU, EarthlyBranch.ZI,
            HeavenlyStem.REN, EarthlyBranch.XU,
            fromYear = 1945, toYear = 1955,
        )
        assertTrue(r.baseItems.any { it.dim.contains("早婚") })
    }

    @Test
    fun `例十四 某女士 夫宫得月令而旺 早婚`() {
        val r = analyze(
            HeavenlyStem.XIN, EarthlyBranch.MAO,
            HeavenlyStem.XIN, EarthlyBranch.MAO,
            HeavenlyStem.DING, EarthlyBranch.MAO,
            HeavenlyStem.REN, EarthlyBranch.YIN,
            female = true,
        )
        assertTrue(r.baseItems.any { it.dim.contains("得月令") })
    }
}
