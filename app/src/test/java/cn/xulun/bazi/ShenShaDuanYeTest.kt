package cn.xulun.bazi

import cn.xulun.bazi.core.HangYeCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 神煞断行业锚点（玄学缘/将星军警/桃花娱乐三例）。理论见 .shengke-reference.md 第六四节四。
 */
class ShenShaDuanYeTest {

    private fun its(y: HeavenlyStem, yb: EarthlyBranch, m: HeavenlyStem, mb: EarthlyBranch,
                    d: HeavenlyStem, db: EarthlyBranch, h: HeavenlyStem, hb: EarthlyBranch) =
        HangYeCalculator.analyze(chartOf(y, yb, m, mb, d, db, h, hb)).items

    @Test fun `例一 己亥丁卯庚戌甲申 天门太极玄学缘（看相为业）`() {
        val r = its(HeavenlyStem.JI, EarthlyBranch.HAI, HeavenlyStem.DING, EarthlyBranch.MAO,
            HeavenlyStem.GENG, EarthlyBranch.XU, HeavenlyStem.JIA, EarthlyBranch.SHEN)
        assertTrue("玄学缘：${r.map { it.field }}", r.any { it.field.contains("玄学") || it.field.contains("五术") })
    }

    @Test fun `例二 庚戌丁亥戊戌丁巳 天门华盖太极（业余周易）`() {
        val r = its(HeavenlyStem.GENG, EarthlyBranch.XU, HeavenlyStem.DING, EarthlyBranch.HAI,
            HeavenlyStem.WU, EarthlyBranch.XU, HeavenlyStem.DING, EarthlyBranch.SI)
        assertTrue("玄学缘：${r.map { it.field }}", r.any { it.field.contains("玄学") || it.field.contains("五术") })
    }

    @Test fun `例三 乙巳甲申庚申乙酉 将星临刃从军（团长）`() {
        val r = its(HeavenlyStem.YI, EarthlyBranch.SI, HeavenlyStem.JIA, EarthlyBranch.SHEN,
            HeavenlyStem.GENG, EarthlyBranch.SHEN, HeavenlyStem.YI, EarthlyBranch.YOU)
        assertTrue("将星刃→军警：${r.map { it.field }}", r.any { it.field.contains("从军") || it.field.contains("公检法") })
    }

    @Test fun `例四 辛亥辛卯庚子己卯 桃花水旺娱乐`() {
        val r = its(HeavenlyStem.XIN, EarthlyBranch.HAI, HeavenlyStem.XIN, EarthlyBranch.MAO,
            HeavenlyStem.GENG, EarthlyBranch.ZI, HeavenlyStem.JI, EarthlyBranch.MAO)
        assertTrue("桃花水→娱乐：${r.map { it.field }}", r.any { it.field.contains("娱乐") })
    }
}
