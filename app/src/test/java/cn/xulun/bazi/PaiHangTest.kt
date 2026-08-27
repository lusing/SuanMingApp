package cn.xulun.bazi

import cn.xulun.bazi.core.LiuQinCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 六亲·手足排行测试（教材第四节(六)实例锚定）。
 *
 * 年月本气为劫比→非老大；亥水当令之比劫数已实不加倍（5个正好）；
 * 比劫为忌——不宜和兄弟姐妹合伙投资经营。
 */
class PaiHangTest {

    private fun xiongDi(
        ys: HeavenlyStem, yb: EarthlyBranch,
        ms: HeavenlyStem, mb: EarthlyBranch,
        ds: HeavenlyStem, db: EarthlyBranch,
        hs: HeavenlyStem, hb: EarthlyBranch,
    ) = LiuQinCalculator.analyze(
        chartOf(ys, yb, ms, mb, ds, db, hs, hb),
        Gender.MALE,
    ).xiongDi

    @Test
    fun `例一 江苏郝先生 辛卯庚寅甲申甲戌 年月本气劫比 自己非老大`() {
        val x = xiongDi(
            HeavenlyStem.XIN, EarthlyBranch.MAO,
            HeavenlyStem.GENG, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.SHEN,
            HeavenlyStem.JIA, EarthlyBranch.XU,
        )
        assertTrue(x.ranking?.contains("老大") == true)
    }

    @Test
    fun `例二 庚戌丁亥癸丑癸亥 比劫5个正好 月柱藏劫非老大 忌合伙`() {
        val x = xiongDi(
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.DING, EarthlyBranch.HAI,
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
        )
        assertTrue(x.count.contains("5"))
        assertTrue(x.ranking?.contains("老大") == true)
        assertTrue(x.unfavorable.any { it.text.contains("合伙") })
    }
}
