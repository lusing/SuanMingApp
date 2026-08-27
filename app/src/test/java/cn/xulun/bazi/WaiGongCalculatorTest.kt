package cn.xulun.bazi

import cn.xulun.bazi.core.LiuQinCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 六亲·外公家族断语测试（教材 3 例锚定，.liuqin-reference.md 第二节）。
 *
 * 理论链：母亲=印星，生于外祖父家；生印者官杀为外祖父；制官杀者伤官之力最大。
 * 外祖父宫位在年月柱——伤官（伤食）居年月柱且旺为忌，外祖父家败绝。
 */
class WaiGongCalculatorTest {

    private fun waiGong(
        ys: HeavenlyStem, yb: EarthlyBranch,
        ms: HeavenlyStem, mb: EarthlyBranch,
        ds: HeavenlyStem, db: EarthlyBranch,
        hs: HeavenlyStem, hb: EarthlyBranch,
    ) = LiuQinCalculator.analyze(
        chartOf(ys, yb, ms, mb, ds, db, hs, hb),
        Gender.MALE,
    ).waiGong

    @Test
    fun `例一 丙辰丙申乙卯戊寅 年月伤官旺透忌且寅卯生之 外公家或舅家绝房残疾`() {
        val w = waiGong(
            HeavenlyStem.BING, EarthlyBranch.CHEN,
            HeavenlyStem.BING, EarthlyBranch.SHEN,
            HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.WU, EarthlyBranch.YIN,
        )
        assertTrue(w.items.any { it.dim.contains("伤官") && it.text.contains("外祖父家败绝") })
        assertTrue(w.items.any { it.text.contains("绝房") })
        assertTrue(w.summary.isNotBlank())
    }

    @Test
    fun `例二 癸巳壬戌庚子丁丑 年月伤食旺透得子根 不利外祖父家绝嗣`() {
        val w = waiGong(
            HeavenlyStem.GUI, EarthlyBranch.SI,
            HeavenlyStem.REN, EarthlyBranch.XU,
            HeavenlyStem.GENG, EarthlyBranch.ZI,
            HeavenlyStem.DING, EarthlyBranch.CHOU,
        )
        assertTrue(w.items.any { it.dim.contains("伤") && it.text.contains("外祖父家败绝") })
        assertTrue(w.items.any { it.text.contains("绝嗣") })
    }

    @Test
    fun `例三 丁未甲辰丁巳甲辰 伤官重重当令且印星坐墓 舅姨早亡`() {
        val w = waiGong(
            HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.JIA, EarthlyBranch.CHEN,
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.JIA, EarthlyBranch.CHEN,
        )
        assertTrue(w.items.any { it.dim.contains("伤官") && it.text.contains("外祖父家败绝") })
        assertTrue(w.items.any { it.dim.contains("墓") && it.text.contains("早亡") })
    }
}
