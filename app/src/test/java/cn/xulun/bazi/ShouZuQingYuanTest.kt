package cn.xulun.bazi

import cn.xulun.bazi.core.LiuQinCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 六亲·手足情缘测试（教材第四节(四)实例锚定）。
 *
 * 比劫喜忌为总纲；月令刑冲→天各一方；年杀月伤为忌→兄弟不睦(出不孝之人)；
 * 财弱比劫旺→群比夺财兄弟反目；专旺财从→兄弟融洽。
 */
class ShouZuQingYuanTest {

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
    fun `刑冲例一 戊申壬戌丁丑乙巳 丑戌刑兄弟宫 兄弟难聚首`() {
        val x = xiongDi(
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.REN, EarthlyBranch.XU,
            HeavenlyStem.DING, EarthlyBranch.CHOU,
            HeavenlyStem.YI, EarthlyBranch.SI,
        )
        assertTrue(x.unfavorable.any { it.dim.contains("月柱") && (it.text.contains("聚首") || it.text.contains("一方")) })
    }

    @Test
    fun `刑冲例二 甲寅戊辰丙戌乙未 辰戌冲月令 兄弟天各一方`() {
        val x = xiongDi(
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.WU, EarthlyBranch.CHEN,
            HeavenlyStem.BING, EarthlyBranch.XU,
            HeavenlyStem.YI, EarthlyBranch.WEI,
        )
        assertTrue(x.unfavorable.any { it.dim.contains("月柱") && it.text.contains("一方") })
    }

    @Test
    fun `年杀月伤例 壬辰己酉丙寅乙未 年杀月伤为忌 兄弟不睦出不孝之人`() {
        val x = xiongDi(
            HeavenlyStem.REN, EarthlyBranch.CHEN,
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.YI, EarthlyBranch.WEI,
        )
        assertTrue(x.unfavorable.any { it.dim.contains("杀") && (it.text.contains("不睦") || it.text.contains("反目") || it.text.contains("不孝")) })
    }

    @Test
    fun `财弱比劫旺例一 癸卯乙卯乙卯癸未 曲直专旺财从 兄弟关系融洽`() {
        val x = xiongDi(
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.GUI, EarthlyBranch.WEI,
        )
        assertTrue(x.favorable.any { it.text.contains("融洽") })
    }

    @Test
    fun `财弱比劫旺例二 甲寅乙亥乙丑戊寅 群比夺财 兄弟反目`() {
        val x = xiongDi(
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.YI, EarthlyBranch.HAI,
            HeavenlyStem.YI, EarthlyBranch.CHOU,
            HeavenlyStem.WU, EarthlyBranch.YIN,
        )
        assertTrue(x.unfavorable.any { it.dim.contains("夺财") || it.text.contains("反目") })
    }

    @Test
    fun `比劫喜用例 戊申庚申戊辰壬戌 食神泄身略过比劫喜用 多得兄弟之助`() {
        val x = xiongDi(
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.WU, EarthlyBranch.CHEN,
            HeavenlyStem.REN, EarthlyBranch.XU,
        )
        assertTrue(x.favorable.any { it.dim.contains("喜用") && it.text.contains("助") })
    }
}
