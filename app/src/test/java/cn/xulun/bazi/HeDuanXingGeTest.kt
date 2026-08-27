package cn.xulun.bazi

import cn.xulun.bazi.core.XingGeCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 十神心性与五行特性合断性格锚点（第六五节六 八步法三例）。
 * 天干=明显性格/地支=相对隐蔽（阳支显现阴支潜伏）；被合绊之干折力；
 * 透干最强十神主行为方略——行为决定命运。
 */
class HeDuanXingGeTest {

    private fun its(y: HeavenlyStem, yb: EarthlyBranch, m: HeavenlyStem, mb: EarthlyBranch,
                    d: HeavenlyStem, db: EarthlyBranch, h: HeavenlyStem, hb: EarthlyBranch) =
        XingGeCalculator.analyzeComposite(chartOf(y, yb, m, mb, d, db, h, hb)).items

    @Test fun `例一 西藏鸿先生 戊申壬戌壬申辛丑 七杀主事丑土潜伏`() {
        val r = its(HeavenlyStem.WU, EarthlyBranch.SHEN, HeavenlyStem.REN, EarthlyBranch.XU,
            HeavenlyStem.REN, EarthlyBranch.SHEN, HeavenlyStem.XIN, EarthlyBranch.CHOU)
        val gan = r.first { it.dim.contains("天干线") }
        assertTrue("天干线含戊七杀：${gan.text}", gan.text.contains("戊") && gan.text.contains("七杀"))
        assertTrue("潜伏性格含丑：${r.map { it.dim }}", r.any { it.dim.contains("潜伏") && it.dim.contains("丑") })
        assertTrue("丑土负面特性（怀疑猜忌）：${r.map { it.dim }}",
            r.any { it.dim.contains("潜伏") && it.dim.contains("怀疑猜忌") })
        val fang = r.first { it.dim.contains("方略") }
        assertTrue("七杀主事强硬手段：${fang.dim}${fang.text}",
            fang.dim.contains("七杀") && fang.text.contains("强硬"))
    }

    @Test fun `例三 湖南元先生 癸卯戊午丙午癸巳 癸被合绊食神主事实干技术`() {
        val r = its(HeavenlyStem.GUI, EarthlyBranch.MAO, HeavenlyStem.WU, EarthlyBranch.WU,
            HeavenlyStem.BING, EarthlyBranch.WU, HeavenlyStem.GUI, EarthlyBranch.SI)
        val gan = r.first { it.dim.contains("天干线") }
        assertTrue("癸被合绊：${gan.text}", gan.text.contains("癸（被合绊）"))
        assertTrue("天干线含戊食神：${gan.text}", gan.text.contains("戊") && gan.text.contains("食神"))
        assertTrue("卯阴支潜伏藏正印：${r.map { it.dim }}",
            r.any { it.dim.contains("潜伏") && it.dim.contains("卯") })
        val fang = r.first { it.dim.contains("方略") }
        assertTrue("食神主事实干技术：${fang.dim}${fang.text}",
            fang.dim.contains("食神") && fang.text.contains("技术"))
    }

    @Test fun `例四 坤造 戊申辛酉戊戌甲寅 伤官太强主事`() {
        val r = its(HeavenlyStem.WU, EarthlyBranch.SHEN, HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.WU, EarthlyBranch.XU, HeavenlyStem.JIA, EarthlyBranch.YIN)
        val gan = r.first { it.dim.contains("天干线") }
        assertTrue("天干线含辛伤官：${gan.text}", gan.text.contains("辛") && gan.text.contains("伤官"))
        val fang = r.first { it.dim.contains("方略") }
        assertTrue("伤官主事：${fang.dim}", fang.dim.contains("伤官"))
    }
}
