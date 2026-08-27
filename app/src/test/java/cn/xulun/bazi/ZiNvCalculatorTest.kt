package cn.xulun.bazi

import cn.xulun.bazi.core.LiuQinCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 六亲·子女断语测试（教材第五节实例锚定）。
 *
 * 子女星：男七杀=子/正官=女，女伤官=子/食神=女；子女宫=时柱。
 * 头胎性别：时干与日干阴阳（男同性=男/异性=女，女反是）；
 * 子女宫与日干同性→儿女双全，异性→多生女/男；从象衰而从真→子女有出息。
 */
class ZiNvCalculatorTest {

    private fun ziNv(
        ys: HeavenlyStem, yb: EarthlyBranch,
        ms: HeavenlyStem, mb: EarthlyBranch,
        ds: HeavenlyStem, db: EarthlyBranch,
        hs: HeavenlyStem, hb: EarthlyBranch,
        female: Boolean = false,
    ) = LiuQinCalculator.analyze(
        chartOf(ys, yb, ms, mb, ds, db, hs, hb),
        if (female) Gender.FEMALE else Gender.MALE,
    ).ziNv

    @Test
    fun `享福例一 癸卯甲寅丙午戊戌 女儿星癸从旺 从则吉享女儿福`() {
        val z = ziNv(
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.WU, EarthlyBranch.XU,
        )
        assertTrue(z.favorable.any { it.text.contains("女儿") && it.text.contains("福") })
    }

    @Test
    fun `双全例一 壬午癸卯戊子癸丑 卯官月令旺得水生加倍4女 宫异性多生女`() {
        val z = ziNv(
            HeavenlyStem.REN, EarthlyBranch.WU,
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.WU, EarthlyBranch.ZI,
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
        )
        assertTrue(z.count.contains("4"))
        assertTrue(z.countBasis.contains("加倍"))
        assertTrue(z.palaceNote?.contains("女") == true)
    }

    @Test
    fun `双全例二 乙酉戊子甲子丙寅 子女宫寅与日干同性 儿女双全`() {
        val z = ziNv(
            HeavenlyStem.YI, EarthlyBranch.YOU,
            HeavenlyStem.WU, EarthlyBranch.ZI,
            HeavenlyStem.JIA, EarthlyBranch.ZI,
            HeavenlyStem.BING, EarthlyBranch.YIN,
        )
        assertTrue(z.palaceNote?.contains("双全") == true)
    }

    @Test
    fun `双全例三 己丑甲戌乙未丙子 子女星金无本气从旺土 从则吉子女有出息`() {
        val z = ziNv(
            HeavenlyStem.JI, EarthlyBranch.CHOU,
            HeavenlyStem.JIA, EarthlyBranch.XU,
            HeavenlyStem.YI, EarthlyBranch.WEI,
            HeavenlyStem.BING, EarthlyBranch.ZI,
        )
        assertTrue(z.favorable.any { it.text.contains("出息") })
    }

    @Test
    fun `无子例二 丁未甲辰丁巳甲辰 伤食叠叠旺而忌 难有子息`() {
        val z = ziNv(
            HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.JIA, EarthlyBranch.CHEN,
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.JIA, EarthlyBranch.CHEN,
        )
        assertTrue(z.unfavorable.any { it.text.contains("难有子息") || it.text.contains("无子") })
    }

    @Test
    fun `朱女士 乙未庚辰壬戌辛丑 枭印双透克乙木子星 必主克子`() {
        val z = ziNv(
            HeavenlyStem.YI, EarthlyBranch.WEI,
            HeavenlyStem.GENG, EarthlyBranch.CHEN,
            HeavenlyStem.REN, EarthlyBranch.XU,
            HeavenlyStem.XIN, EarthlyBranch.CHOU,
            female = true,
        )
        assertTrue(z.unfavorable.any { it.text.contains("克子") })
    }

    @Test
    fun `阎女士 丁巳癸丑己卯癸酉 日时支冲且时支空亡 不利子息堕胎流产`() {
        val z = ziNv(
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.JI, EarthlyBranch.MAO,
            HeavenlyStem.GUI, EarthlyBranch.YOU,
            female = true,
        )
        assertTrue(z.unfavorable.any { it.text.contains("堕胎") || it.text.contains("流产") })
    }

    @Test
    fun `远行例一 戊戌丁巳壬寅戊申 宫临驿马逢冲刑 子当远离父母`() {
        val z = ziNv(
            HeavenlyStem.WU, EarthlyBranch.XU,
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.WU, EarthlyBranch.SHEN,
        )
        assertTrue(z.unfavorable.any { it.dim.contains("驿马") && it.text.contains("远离") } ||
            z.favorable.any { it.dim.contains("驿马") && it.text.contains("远") })
    }

    @Test
    fun `头胎性别 乾造三例时干异性均头胎生女`() {
        val charts = listOf(
            listOf("己酉", "癸酉", "丙午", "丁酉"),
            listOf("庚戌", "丁亥", "戊戌", "丁巳"),
            listOf("癸卯", "甲子", "己酉", "壬申"),
        )
        charts.forEach { p ->
            val z = ziNv(
                HeavenlyStem.entries.first { it.chinese == p[0][0].toString() },
                EarthlyBranch.entries.first { it.chinese == p[0][1].toString() },
                HeavenlyStem.entries.first { it.chinese == p[1][0].toString() },
                EarthlyBranch.entries.first { it.chinese == p[1][1].toString() },
                HeavenlyStem.entries.first { it.chinese == p[2][0].toString() },
                EarthlyBranch.entries.first { it.chinese == p[2][1].toString() },
                HeavenlyStem.entries.first { it.chinese == p[3][0].toString() },
                EarthlyBranch.entries.first { it.chinese == p[3][1].toString() },
            )
            assertTrue("${p.joinToString()} 应头胎生女: ${z.headFirst}", z.headFirst.contains("女"))
        }
    }

    @Test
    fun `头胎性别 坤造癸丑庚申戊子乙卯 时干异性头胎为儿`() {
        val z = ziNv(
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.WU, EarthlyBranch.ZI,
            HeavenlyStem.YI, EarthlyBranch.MAO,
            female = true,
        )
        assertTrue(z.headFirst.contains("儿") || z.headFirst.contains("男"))
    }
}
