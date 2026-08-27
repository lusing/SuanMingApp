package cn.xulun.bazi

import cn.xulun.bazi.core.CongGeYunCalculator
import cn.xulun.bazi.core.TeShuGeJuCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.YunVerdict
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 化气格实例解析锚点（陈女士——乙庚化金，忌比劫还原）。
 */
class HuaQiShiLiTest {

    @Test fun `陈女士 己酉甲戌乙酉庚辰 乙庚化金 乙亥年忌 旺金吉`() {
        val b = chartOf(
            HeavenlyStem.JI, EarthlyBranch.YOU, HeavenlyStem.JIA, EarthlyBranch.XU,
            HeavenlyStem.YI, EarthlyBranch.YOU, HeavenlyStem.GENG, EarthlyBranch.CHEN)
        val ts = TeShuGeJuCalculator.analyze(b)
        assertNotNull("应判化气（乙庚化金）：$ts", ts)
        assertTrue("乙日化金应从杀：${ts!!.name}", ts.name.contains("从杀"))
        // 丙子运乙亥年：岁干乙木比劫为忌（还原/忌档——水厄几丧命）
        val r1 = CongGeYunCalculator.analyze(b, listOf(
            Pillar(HeavenlyStem.BING, EarthlyBranch.ZI), Pillar(HeavenlyStem.YI, EarthlyBranch.HAI)))
        assertTrue("乙亥年应凶档（忌/还原）：${r1?.verdict}", r1 == null || r1.verdict == YunVerdict.JI || r1.verdict == YunVerdict.HUAN_YUAN)
        // 顺金之势岁运——吉档（旺金助格局）
        val r2 = CongGeYunCalculator.analyze(b, listOf(Pillar(HeavenlyStem.GENG, EarthlyBranch.SHEN)))
        assertTrue("庚申旺金应吉档：${r2?.verdict}", r2 != null && (r2.verdict == YunVerdict.YONG || r2.verdict == YunVerdict.XI))
    }
}
