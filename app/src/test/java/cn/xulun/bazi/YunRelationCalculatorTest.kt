package cn.xulun.bazi

import cn.xulun.bazi.core.BaziCalculator
import cn.xulun.bazi.core.YunRelationCalculator
import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Pillar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * 运岁关系分析器测试：选定大运/流年柱并入原局后，五柱之间的全部关系。
 *
 * 验证两点：(1) 原局已有的关系仍保留（「全部」语义，非过滤）；
 * (2) 运岁柱引动的新关系，其 location 含「大运」/「流年」标签。
 */
class YunRelationCalculatorTest {

    @Test
    fun `1985-05-20 原局加流年庚午 保留原局关系并引动乙庚合子午冲`() {
        val bazi = BaziCalculator.calculate(LocalDateTime.of(1985, 5, 20, 0, 10))
        assertEquals("乙丑 辛巳 己未 甲子", bazi.fullBazi)
        val result = YunRelationCalculator.analyze(
            bazi,
            Pillar(HeavenlyStem.GENG, EarthlyBranch.WU),
            "流年",
        )

        // 原局关系保留
        assertTrue(result.stemHeHuaList.any { it.description == "甲己合化土" })
        assertTrue(result.branchLiuHeList.any { it.description == "子丑合化土" })
        assertTrue(result.branchChongList.any { it.description == "丑未相冲" })

        // 运岁（流年庚午）引动：乙(月干)+庚 → 乙庚合化金；子(时支)+午 → 子午冲
        val yueGeng = result.stemHeHuaList.first { it.description == "乙庚合化金" }
        assertTrue(yueGeng.location.contains("流年"))
        val ziWu = result.branchChongList.first { it.description == "子午相冲" }
        assertTrue(ziWu.location.contains("流年"))
        assertTrue(result.hasAny)
    }

    @Test
    fun `大运申并入 引动申子辰三合与寅申冲`() {
        // 原局地支：子(年) 辰(月) 寅(日) 亥(时)
        val bazi = baziOf(
            EarthlyBranch.ZI, EarthlyBranch.CHEN, EarthlyBranch.YIN, EarthlyBranch.HAI,
        )
        val result = YunRelationCalculator.analyze(
            bazi,
            Pillar(HeavenlyStem.JIA, EarthlyBranch.SHEN),
            "大运",
        )

        // 申(大运)+子(年)+辰(月) → 申子辰三合水局
        val sanHe = result.branchSanHeList.first { it.description == "申子辰三合水局" }
        assertTrue(sanHe.location.contains("大运"))

        // 寅(日)+申(大运) → 寅申冲
        val yinShen = result.branchChongList.first { it.description == "寅申相冲" }
        assertTrue(yinShen.location.contains("大运"))
    }

    @Test
    fun `运岁柱与原局无新关系时 原局关系仍全部保留`() {
        // 1985-05-20 原局（甲己合、子丑合、丑未冲）+ 一根不引动任何新关系的柱（如丙寅）
        val bazi = BaziCalculator.calculate(LocalDateTime.of(1985, 5, 20, 0, 10))
        val result = YunRelationCalculator.analyze(
            bazi,
            Pillar(HeavenlyStem.BING, EarthlyBranch.YIN),
            "流年",
        )
        // 原局三类关系仍在
        assertTrue(result.stemHeHuaList.any { it.description == "甲己合化土" })
        assertTrue(result.branchLiuHeList.any { it.description == "子丑合化土" })
        assertTrue(result.branchChongList.any { it.description == "丑未相冲" })
    }

    @Test
    fun `六柱 原局加大运流年 跨柱申子辰三合`() {
        val bazi = baziOf(EarthlyBranch.SHEN, EarthlyBranch.XU, EarthlyBranch.WEI, EarthlyBranch.HAI)
        val result = YunRelationCalculator.analyze(
            bazi,
            listOf(
                Pillar(HeavenlyStem.JIA, EarthlyBranch.ZI) to "大运",
                Pillar(HeavenlyStem.JIA, EarthlyBranch.CHEN) to "流年",
            ),
        )
        // 申(年支)+子(大运)+辰(流年) 跨三柱凑成申子辰三合水局
        val sanHe = result.branchSanHeList.first { it.description == "申子辰三合水局" }
        assertEquals("年支+大运+流年", sanHe.location)
    }

    @Test
    fun `六柱 跨柱寅卯辰三会`() {
        val bazi = baziOf(EarthlyBranch.YIN, EarthlyBranch.XU, EarthlyBranch.WEI, EarthlyBranch.HAI)
        val result = YunRelationCalculator.analyze(
            bazi,
            listOf(
                Pillar(HeavenlyStem.JIA, EarthlyBranch.MAO) to "大运",
                Pillar(HeavenlyStem.JIA, EarthlyBranch.CHEN) to "流年",
            ),
        )
        // 寅(年支)+卯(大运)+辰(流年) 跨三柱凑成寅卯辰三会木局
        val sanHui = result.branchSanHuiList.first { it.description == "寅卯辰三会木局" }
        assertEquals("年支+大运+流年", sanHui.location)
    }

    @Test
    fun `六柱 跨柱寅巳申三刑`() {
        val bazi = baziOf(EarthlyBranch.YIN, EarthlyBranch.XU, EarthlyBranch.WEI, EarthlyBranch.HAI)
        val result = YunRelationCalculator.analyze(
            bazi,
            listOf(
                Pillar(HeavenlyStem.JIA, EarthlyBranch.SI) to "大运",
                Pillar(HeavenlyStem.JIA, EarthlyBranch.SHEN) to "流年",
            ),
        )
        // 寅(年支)+巳(大运)+申(流年) 跨三柱凑成寅巳申三刑
        val xing = result.branchXingList.first { it.description.startsWith("寅巳申三刑") }
        assertEquals("年支+大运+流年", xing.location)
    }

    /** 以四地支构造 BaziResult（天干统一取甲，仅用于地支关系测试，不保证干支合法配对） */
    private fun baziOf(vararg branches: EarthlyBranch): BaziResult {
        require(branches.size == 4) { "需提供四柱地支" }
        val (y, m, d, h) = branches.map { Pillar(HeavenlyStem.JIA, it) }
        return BaziResult(
            yearPillar = y,
            monthPillar = m,
            dayPillar = d,
            hourPillar = h,
            birthDateTime = LocalDateTime.of(2000, 1, 1, 12, 0),
        )
    }
}
