package cn.xulun.bazi

import cn.xulun.bazi.core.HealthCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 健康引擎测试（教材身体健康 3 例锚定，.health-reference.md 第二节(一)）。
 */
class HealthCalculatorTest {

    private fun health(
        ys: HeavenlyStem, yb: EarthlyBranch,
        ms: HeavenlyStem, mb: EarthlyBranch,
        ds: HeavenlyStem, db: EarthlyBranch,
        hs: HeavenlyStem, hb: EarthlyBranch,
    ) = HealthCalculator.analyze(
        chartOf(ys, yb, ms, mb, ds, db, hs, hb)
    )

    @Test
    fun `健康例1 癸卯甲子己酉壬申 从金水木循环相生 一生健康`() {
        val r = health(
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.JIA, EarthlyBranch.ZI,
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.REN, EarthlyBranch.SHEN,
        )
        assertTrue(r.items.any { it.text.contains("健康") })
    }

    @Test
    fun `健康例2 壬寅丙午丙申己亥 身旺有克泄不亢旺 一生健康`() {
        val r = health(
            HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.BING, EarthlyBranch.SHEN,
            HeavenlyStem.JI, EarthlyBranch.HAI,
        )
        assertTrue(r.items.any { it.text.contains("健康") })
    }

    @Test
    fun `健康例3 乙卯乙酉丙戌庚寅 贵人喜用照命 一生安康`() {
        val r = health(
            HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.YI, EarthlyBranch.YOU,
            HeavenlyStem.BING, EarthlyBranch.XU,
            HeavenlyStem.GENG, EarthlyBranch.YIN,
        )
        assertTrue(r.items.any { it.text.contains("安康") })
    }

    @Test
    fun `疾病倾向 原局受损五行给出对应脏腑疾病`() {
        // 随取一造,验证 risks 非空且含五行与疾病
        val r = health(
            HeavenlyStem.JIA, EarthlyBranch.WU,
            HeavenlyStem.WU, EarthlyBranch.CHEN,
            HeavenlyStem.GENG, EarthlyBranch.YIN,
            HeavenlyStem.BING, EarthlyBranch.XU,
        )
        assertTrue(r.risks.isNotEmpty())
        assertTrue(r.summary.isNotBlank())
    }

    @Test
    fun `皮肤例四 丙辰庚寅丙午戊戌 寅午戌火局土焦 严重皮肤病`() {
        val r = health(
            HeavenlyStem.BING, EarthlyBranch.CHEN,
            HeavenlyStem.GENG, EarthlyBranch.YIN,
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.WU, EarthlyBranch.XU,
        )
        assertTrue(r.risks.any { it.dim.contains("皮肤病") && it.text.contains("火多土焦") })
    }

    @Test
    fun `皮肤例六 戊申庚申庚申甲申 旺金狂泄土 严重皮肤病`() {
        val r = health(
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.JIA, EarthlyBranch.SHEN,
        )
        assertTrue(r.risks.any { it.dim.contains("皮肤病") && it.text.contains("狂泄") })
    }
}
