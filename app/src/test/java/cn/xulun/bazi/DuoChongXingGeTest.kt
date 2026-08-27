package cn.xulun.bazi

import cn.xulun.bazi.core.XingGeCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 多重十神断性格锚点（两例四组合）。理论见 .shengke-reference.md 第六五节五。
 */
class DuoChongXingGeTest {

    private fun its(y: HeavenlyStem, yb: EarthlyBranch, m: HeavenlyStem, mb: EarthlyBranch,
                    d: HeavenlyStem, db: EarthlyBranch, h: HeavenlyStem, hb: EarthlyBranch) =
        XingGeCalculator.analyze(chartOf(y, yb, m, mb, d, db, h, hb)).items

    @Test fun `湖南李先生 乙巳己卯壬戌辛丑 伤官见官幸有化泄`() {
        val r = its(HeavenlyStem.YI, EarthlyBranch.SI, HeavenlyStem.JI, EarthlyBranch.MAO,
            HeavenlyStem.REN, EarthlyBranch.XU, HeavenlyStem.XIN, EarthlyBranch.CHOU)
        assertTrue("伤官见官多重：${r.map { it.dim }}",
            r.any { it.dim.contains("伤官见官") && it.text.contains("化泄") })
    }

    @Test fun `广东柯先生 庚戌甲申壬申庚子 枭神夺食`() {
        val r = its(HeavenlyStem.GENG, EarthlyBranch.XU, HeavenlyStem.JIA, EarthlyBranch.SHEN,
            HeavenlyStem.REN, EarthlyBranch.SHEN, HeavenlyStem.GENG, EarthlyBranch.ZI)
        assertTrue("枭神夺食：${r.map { it.dim }}",
            r.any { it.dim.contains("枭神夺食") || it.text.contains("金多水浊") })
    }
}
