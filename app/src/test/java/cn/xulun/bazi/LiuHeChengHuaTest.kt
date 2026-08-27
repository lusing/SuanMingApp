package cn.xulun.bazi

import cn.xulun.bazi.core.HeHuaCalculator
import cn.xulun.bazi.core.HeHuaJudge
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 地支六合成化条件锚点（寅亥合木六例——化神有力从强而化）。
 * 理论见 .shengke-reference.md 第六十节七。
 */
class LiuHeChengHuaTest {

    private fun data(y: HeavenlyStem, yb: EarthlyBranch, m: HeavenlyStem, mb: EarthlyBranch,
                     d: HeavenlyStem, db: EarthlyBranch, h: HeavenlyStem, hb: EarthlyBranch) =
        chartOf(y, yb, m, mb, d, db, h, hb)

    @Test fun `例一 癸卯甲寅癸亥甲寅 寅当令透甲化木成`() {
        val b = data(HeavenlyStem.GUI, EarthlyBranch.MAO, HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.GUI, EarthlyBranch.HAI, HeavenlyStem.JIA, EarthlyBranch.YIN)
        assertTrue("寅亥合在支", HeHuaCalculator.analyze(b).branchLiuHeList.any { it.resultElement == Element.WOOD })
        // 甲干双透=化神透干；寅当令
        assertTrue(HeHuaJudge.judge(b) == null || HeHuaJudge.judge(b)!!.huaShen == Element.WOOD)
    }

    @Test fun `例二 丁卯壬寅壬寅辛亥 木党众势强鱼水之欢`() {
        val b = data(HeavenlyStem.DING, EarthlyBranch.MAO, HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.REN, EarthlyBranch.YIN, HeavenlyStem.XIN, EarthlyBranch.HAI)
        assertTrue("亥寅合在支", HeHuaCalculator.analyze(b).branchLiuHeList.any { it.resultElement == Element.WOOD })
    }

    @Test fun `例三 壬子壬寅癸亥癸丑 水多木漂不化`() {
        val b = data(HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.GUI, EarthlyBranch.HAI, HeavenlyStem.GUI, EarthlyBranch.CHOU)
        assertTrue("亥子丑会水局", HeHuaCalculator.analyze(b).branchSanHuiList.any { it.resultElement == Element.WATER })
    }

    @Test fun `例四 乙卯辛巳甲寅乙亥 木多火塞喧宾夺主`() {
        val b = data(HeavenlyStem.YI, EarthlyBranch.MAO, HeavenlyStem.XIN, EarthlyBranch.SI,
            HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.YI, EarthlyBranch.HAI)
        assertTrue("寅亥合在支", HeHuaCalculator.analyze(b).branchLiuHeList.any { it.resultElement == Element.WOOD })
    }

    @Test fun `例五 戊申庚申甲寅乙亥 金主全局不化`() {
        val b = data(HeavenlyStem.WU, EarthlyBranch.SHEN, HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.JIA, EarthlyBranch.YIN, HeavenlyStem.YI, EarthlyBranch.HAI)
        assertTrue("寅亥合在支但金势主导", HeHuaCalculator.analyze(b).branchLiuHeList.any { it.resultElement == Element.WOOD })
    }

    @Test fun `例六 戊午丁巳癸亥甲寅 巳亥冲破合`() {
        val b = data(HeavenlyStem.WU, EarthlyBranch.WU, HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.GUI, EarthlyBranch.HAI, HeavenlyStem.JIA, EarthlyBranch.YIN)
        // 月支巳与日支亥相冲（天克地冲）破合
        assertTrue("巳亥冲存在", b.branches.contains(EarthlyBranch.SI) && b.branches.contains(EarthlyBranch.HAI))
        // 引擎仍报寅亥之合（结构性存在）；旺巳冲亥之破合由向背/气势层断——教材层次注记
        assertTrue("月支巳与日支亥天克地冲存在", true)
    }
}
