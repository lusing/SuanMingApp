package cn.xulun.bazi

import cn.xulun.bazi.core.BaziCalculator
import cn.xulun.bazi.core.CaiYunCalculator
import cn.xulun.bazi.core.DaYunCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * 财运完整岁运测试（教材三例锚定，.caiyun-reference.md）。
 *
 * 三层：原局基调 / 大运和战评级 / 流年关键年（财星引动→全命局和局=发、战局=破）。
 * 通关有力化战为和；三合组化敌为友；比劫冲原局财星支=冲战；三刑组齐=刑战。
 */
class CaiYunCalculatorTest {

    private fun analyze(
        ys: HeavenlyStem, yb: EarthlyBranch,
        ms: HeavenlyStem, mb: EarthlyBranch,
        ds: HeavenlyStem, db: EarthlyBranch,
        hs: HeavenlyStem, hb: EarthlyBranch,
        female: Boolean = false,
        fromYear: Int = 1950,
        toYear: Int = 1980,
    ): cn.xulun.bazi.model.CaiYunResult {
        val bazi = chartOf(ys, yb, ms, mb, ds, db, hs, hb, fromYear, toYear)
        val gender = if (female) Gender.FEMALE else Gender.MALE
        return CaiYunCalculator.analyze(bazi, gender, DaYunCalculator.compute(bazi, gender))
    }

    private fun cn.xulun.bazi.model.CaiYunResult.yun(name: String) =
        yuns.firstOrNull { it.pillar.stem.chinese + it.pillar.branch.chinese == name }

    // ── 例一 重庆徐先生 乾 丁酉己酉甲申癸酉 ──

    @Test
    fun `徐先生 丙午运支系统火金交战无通关 运级战局不顺`() {
        val r = analyze(
            HeavenlyStem.DING, EarthlyBranch.YOU,
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.JIA, EarthlyBranch.SHEN,
            HeavenlyStem.GUI, EarthlyBranch.YOU,
        )
        val ym = r.yun("丙午")
        assertNotNull(ym)
        assertTrue(ym!!.heZhan.contains("战"))
    }

    @Test
    fun `徐先生 乙巳运丁丑1997 巳酉丑合化金和局 发财`() {
        val r = analyze(
            HeavenlyStem.DING, EarthlyBranch.YOU,
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.JIA, EarthlyBranch.SHEN,
            HeavenlyStem.GUI, EarthlyBranch.YOU,
        )
        val n = r.yun("乙巳")!!.years.firstOrNull { it.year == 1997 }
        assertNotNull(n)
        assertTrue(n!!.kind.chinese.contains("发"))
    }

    @Test
    fun `徐先生 甲辰运戊寅1998己卯1999 金木交战 谨慎防破`() {
        val r = analyze(
            HeavenlyStem.DING, EarthlyBranch.YOU,
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.JIA, EarthlyBranch.SHEN,
            HeavenlyStem.GUI, EarthlyBranch.YOU,
        )
        val years = r.yun("甲辰")!!.years
        val y98 = years.firstOrNull { it.year == 1998 }
        val y99 = years.firstOrNull { it.year == 1999 }
        assertTrue(y98 != null && y98.kind.chinese != "发" && (y98.text.contains("谨慎") || y98.text.contains("破")))
        assertTrue(y99 == null || (y99.kind.chinese != "发" && (y99.text.contains("谨慎") || y99.text.contains("破"))))
    }

    @Test
    fun `徐先生 甲辰运2000至2003 辰通关和局 财运亨通`() {
        val r = analyze(
            HeavenlyStem.DING, EarthlyBranch.YOU,
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.JIA, EarthlyBranch.SHEN,
            HeavenlyStem.GUI, EarthlyBranch.YOU,
        )
        val years = r.yun("甲辰")!!.years
        listOf(2000, 2001, 2002, 2003).forEach { y ->
            val n = years.firstOrNull { it.year == y }
            assertNotNull("缺关键年 $y", n)
            assertTrue("$y 应为发财年:${n!!.kind}/${n.text}", n.kind.chinese.contains("发"))
        }
    }

    // ── 例二 山东程先生 乾 甲辰丁卯庚申戊寅 ──

    @Test
    fun `程先生 己巳运寅巳申三刑 财星遭刑求财难如愿`() {
        val r = analyze(
            HeavenlyStem.JIA, EarthlyBranch.CHEN,
            HeavenlyStem.DING, EarthlyBranch.MAO,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.WU, EarthlyBranch.YIN,
        )
        val ys = r.yun("己巳")
        assertNotNull(ys)
        assertTrue(ys!!.heZhan.contains("刑") || ys.heZhan.contains("战") || ys.text.contains("难如愿"))
    }

    @Test
    fun `程先生 庚午运午通关木土 和局好运`() {
        val r = analyze(
            HeavenlyStem.JIA, EarthlyBranch.CHEN,
            HeavenlyStem.DING, EarthlyBranch.MAO,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.WU, EarthlyBranch.YIN,
        )
        val yw = r.yun("庚午")
        assertNotNull(yw)
        assertTrue(yw!!.heZhan.contains("和"))
    }

    @Test
    fun `程先生 庚午运壬申1992癸酉1993 比劫冲财支 破财年`() {
        val r = analyze(
            HeavenlyStem.JIA, EarthlyBranch.CHEN,
            HeavenlyStem.DING, EarthlyBranch.MAO,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.WU, EarthlyBranch.YIN,
        )
        val years = r.yun("庚午")!!.years
        listOf(1992, 1993).forEach { y ->
            val n = years.firstOrNull { it.year == y }
            assertNotNull(n)
            assertTrue("$y 应为破财年:${n!!.kind}", n.kind.chinese.contains("破"))
        }
    }

    @Test
    fun `程先生 庚午运戊寅1998己卯1999 财星当值和局 求财顺利`() {
        val r = analyze(
            HeavenlyStem.JIA, EarthlyBranch.CHEN,
            HeavenlyStem.DING, EarthlyBranch.MAO,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.WU, EarthlyBranch.YIN,
        )
        val years = r.yun("庚午")!!.years
        listOf(1998, 1999).forEach { y ->
            val n = years.firstOrNull { it.year == y }
            assertNotNull(n)
            assertTrue("$y 应为发财年:${n!!.kind}/${n.text}", n.kind.chinese.contains("发"))
        }
    }

    // ── 例四 重庆谢先生 乾 癸卯甲子癸丑乙卯 ──

    @Test
    fun `谢先生 原局无财星但伤官食伤有力 食伤生财非穷命`() {
        val r = analyze(
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.JIA, EarthlyBranch.ZI,
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.YI, EarthlyBranch.MAO,
        )
        assertTrue(r.baseItems.any { it.text.contains("食伤生财") })
    }

    @Test
    fun `谢先生 壬戌运干支基本成就和局`() {
        val r = analyze(
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.JIA, EarthlyBranch.ZI,
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.YI, EarthlyBranch.MAO,
        )
        val yw = r.yun("壬戌")
        assertNotNull(yw)
        assertTrue(yw!!.heZhan.contains("和"))
    }

    @Test
    fun `谢先生 壬戌运丙寅1986己巳1989庚午1990 木火年成就和局 发财`() {
        val r = analyze(
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.JIA, EarthlyBranch.ZI,
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.YI, EarthlyBranch.MAO,
        )
        val years = r.yun("壬戌")!!.years
        listOf(1986, 1989, 1990).forEach { y ->
            val n = years.firstOrNull { it.year == y }
            assertNotNull("缺关键年 $y", n)
            assertTrue("$y 应为发财年:${n!!.kind}/${n.text}", n.kind.chinese.contains("发"))
        }
    }

    // ── 例三 上海郎女士 坤 丁未丁未壬午辛亥 ──

    @Test
    fun `郎女士 原局正财双透宜稳 偏财贴日支有投机冲动`() {
        val r = analyze(
            HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.REN, EarthlyBranch.WU,
            HeavenlyStem.XIN, EarthlyBranch.HAI,
            female = true,
        )
        assertTrue(r.baseItems.any { it.text.contains("工资") || it.text.contains("稳妥") })
        assertTrue(r.baseItems.any { it.text.contains("投机") || it.text.contains("冒险") })
    }

    @Test
    fun `郎女士 己酉运正印平稳 财运无大起伏`() {
        val r = analyze(
            HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.REN, EarthlyBranch.WU,
            HeavenlyStem.XIN, EarthlyBranch.HAI,
            female = true,
        )
        val yy = r.yun("己酉")
        assertNotNull(yy)
        assertTrue(yy!!.text.contains("平稳") || yy.text.contains("平稳").not() && yy.text.contains("无大起伏"))
    }

    @Test
    fun `郎女士 庚戌运乙亥1995水火水土相战 丙子1996比劫冲偏财 破财年`() {
        val r = analyze(
            HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.REN, EarthlyBranch.WU,
            HeavenlyStem.XIN, EarthlyBranch.HAI,
            female = true,
        )
        val years = r.yun("庚戌")!!.years
        listOf(1995, 1996).forEach { y ->
            val n = years.firstOrNull { it.year == y }
            assertNotNull(n)
            assertTrue("$y 应为破财年:${n!!.kind}/${n.text}", n.kind.chinese.contains("破"))
        }
    }

    @Test
    fun `郎女士 辛巳2001壬午2002癸未2003 连年战局 偏财不能合化 亏损`() {
        val r = analyze(
            HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.REN, EarthlyBranch.WU,
            HeavenlyStem.XIN, EarthlyBranch.HAI,
            female = true,
        )
        val years = r.yun("庚戌")!!.years
        val inWar = years.filter { it.year in 2001..2003 }
        assertTrue("2001-2003 不应有发财年", inWar.none { it.kind.chinese.contains("发") })
        assertTrue("2001-2003 应至少一年破财年", inWar.any { it.kind.chinese.contains("破") })
    }

    // ── 例五 成都林女士 坤 庚戌丁亥己酉癸酉 ──

    @Test
    fun `林女士 甲申运己卯1999 卯戌合解卯酉冲 和局创业成功`() {
        val r = analyze(
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.DING, EarthlyBranch.HAI,
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.GUI, EarthlyBranch.YOU,
            female = true,
        )
        val n = r.yun("甲申")!!.years.firstOrNull { it.year == 1999 }
        assertNotNull(n)
        assertTrue(n!!.kind.chinese.contains("发"))
    }

    @Test
    fun `林女士 甲申运壬午2002癸未2003 财星当值 财运较好`() {
        val r = analyze(
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.DING, EarthlyBranch.HAI,
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.GUI, EarthlyBranch.YOU,
            female = true,
        )
        val years = r.yun("甲申")!!.years
        listOf(2002, 2003).forEach { y ->
            val n = years.firstOrNull { it.year == y }
            assertNotNull(n)
            assertTrue("$y 应为发财年:${n!!.kind}/${n.text}", n.kind.chinese.contains("发"))
        }
    }

    @Test
    fun `林女士 甲申运甲申2004 岁运并临甲庚冲 伤官官星冲战损失`() {
        val r = analyze(
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.DING, EarthlyBranch.HAI,
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.GUI, EarthlyBranch.YOU,
            female = true,
        )
        val n = r.yun("甲申")!!.years.firstOrNull { it.year == 2004 }
        assertNotNull(n)
        assertTrue(n!!.kind.chinese.contains("破"))
    }

    // ── 例六 浙江程先生 乾 庚戌己卯戊子壬戌 ──

    @Test
    fun `浙程 壬午运庚辰2000 比肩忌神 谨慎防破`() {
        val r = analyze(
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.JI, EarthlyBranch.MAO,
            HeavenlyStem.WU, EarthlyBranch.ZI,
            HeavenlyStem.REN, EarthlyBranch.XU,
        )
        val n = r.yun("壬午")!!.years.firstOrNull { it.year == 2000 }
        assertNotNull(n)
        assertTrue(n!!.kind.chinese != "发" && (n.text.contains("谨慎") || n.text.contains("破")))
    }

    @Test
    fun `浙程 壬午运壬午2002甲申2004 财星当值食伤引动 发财`() {
        val r = analyze(
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.JI, EarthlyBranch.MAO,
            HeavenlyStem.WU, EarthlyBranch.ZI,
            HeavenlyStem.REN, EarthlyBranch.XU,
        )
        val years = r.yun("壬午")!!.years
        listOf(2002, 2004).forEach { y ->
            val n = years.firstOrNull { it.year == y }
            assertNotNull(n)
            assertTrue("$y 应为发财年:${n!!.kind}/${n.text}", n.kind.chinese.contains("发"))
        }
    }

    // ── 例七 成都余先生 乾 丙午戊戌壬寅甲辰 ──

    @Test
    fun `余先生 辛丑运木火食伤生财连年构成和局 发财`() {
        val r = analyze(
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.WU, EarthlyBranch.XU,
            HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.CHEN,
        )
        val years = r.yun("辛丑")!!.years
        listOf(1998, 1999, 2002, 2005).forEach { y ->
            val n = years.firstOrNull { it.year == y }
            assertNotNull("缺关键年 $y", n)
            assertTrue("$y 应为发财年:${n!!.kind}/${n.text}", n.kind.chinese.contains("发"))
        }
    }

    // ── 例八 成都郑女士 坤 丙辰己亥辛未戊戌 ──

    @Test
    fun `郑女士 原局财无本气不透偏财无伤官 不宜独立创业`() {
        val r = analyze(
            HeavenlyStem.BING, EarthlyBranch.CHEN,
            HeavenlyStem.JI, EarthlyBranch.HAI,
            HeavenlyStem.XIN, EarthlyBranch.WEI,
            HeavenlyStem.WU, EarthlyBranch.XU,
            female = true,
        )
        assertTrue(r.baseItems.any { it.text.contains("单位") })
    }

    @Test
    fun `郑女士 丙申运2001辛巳2002壬午 非财星流年无关键财运信号`() {
        val r = analyze(
            HeavenlyStem.BING, EarthlyBranch.CHEN,
            HeavenlyStem.JI, EarthlyBranch.HAI,
            HeavenlyStem.XIN, EarthlyBranch.WEI,
            HeavenlyStem.WU, EarthlyBranch.XU,
            female = true,
        )
        val years = r.yun("丙申")!!.years
        val inRange = years.filter { it.year == 2001 || it.year == 2002 }
        assertTrue("2001/2002 不应有发财年: ${inRange.joinToString { "${it.year}[${it.kind.chinese}]" }}",
            inRange.none { it.kind.chinese.contains("发") })
    }

    // ── 例九 乾 癸卯丁巳癸亥甲寅 ──

    @Test
    fun `例九 财贴身当令时柱食伤为财源 商人特征`() {
        val r = analyze(
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
        )
        assertTrue(r.baseItems.any { it.text.contains("商人") })
    }

    @Test
    fun `例九 甲寅运壬申1992癸酉1993 岁干支全喜用 财运尚可`() {
        val r = analyze(
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
        )
        val years = r.yun("甲寅")!!.years
        listOf(1992, 1993).forEach { y ->
            val n = years.firstOrNull { it.year == y }
            assertNotNull(n)
            assertTrue("$y 应为发财年:${n!!.kind}/${n.text}", n.kind.chinese.contains("发"))
        }
    }

    @Test
    fun `例九 甲戌1994丁丑1997 官杀当值克身 损财`() {
        val r = analyze(
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
        )
        val y1994 = r.yun("甲寅")!!.years.firstOrNull { it.year == 1994 }
        val y1997 = r.yun("癸丑")!!.years.firstOrNull { it.year == 1997 }
        assertNotNull(y1994)
        assertNotNull(y1997)
        assertTrue("1994 应为破财年:${y1994!!.kind}", y1994.kind.chinese.contains("破"))
        assertTrue("1997 应为破财年:${y1997!!.kind}", y1997.kind.chinese.contains("破"))
    }

    @Test
    fun `例九 丙子1996 比劫支当值全局相生 生意兴隆`() {
        val r = analyze(
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
        )
        val n = r.yun("甲寅")!!.years.firstOrNull { it.year == 1996 }
        assertNotNull(n)
        assertTrue(n!!.kind.chinese.contains("发"))
    }

    // ── 例十 乾 乙巳庚辰己亥丙寅 ──

    @Test
    fun `例十 丁丑运壬申1992癸酉1993 伤官财星得地 发财`() {
        val r = analyze(
            HeavenlyStem.YI, EarthlyBranch.SI,
            HeavenlyStem.GENG, EarthlyBranch.CHEN,
            HeavenlyStem.JI, EarthlyBranch.HAI,
            HeavenlyStem.BING, EarthlyBranch.YIN,
        )
        val years = r.yun("丁丑")!!.years
        listOf(1992, 1993).forEach { y ->
            val n = years.firstOrNull { it.year == y }
            assertNotNull(n)
            assertTrue("$y 应为发财年:${n!!.kind}/${n.text}", n.kind.chinese.contains("发"))
        }
    }

    // ── 例十二/十三/十四 ──

    @Test
    fun `例十二 乙丑运丙寅1986丁卯1987 财星透出食伤旺 发财`() {
        val r = analyze(
            HeavenlyStem.WU, EarthlyBranch.ZI,
            HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            fromYear = 1940, toYear = 1955,
        )
        val years = r.yun("乙丑")!!.years
        listOf(1986, 1987).forEach { y ->
            val n = years.firstOrNull { it.year == y }
            assertNotNull(n)
            assertTrue("$y 应为发财年:${n!!.kind}/${n.text}", n.kind.chinese.contains("发"))
        }
    }

    @Test
    fun `例十三 庚寅运甲戌1994 官制劫从势 发财`() {
        val r = analyze(
            HeavenlyStem.GENG, EarthlyBranch.YIN,
            HeavenlyStem.BING, EarthlyBranch.XU,
            HeavenlyStem.JI, EarthlyBranch.CHOU,
            HeavenlyStem.GUI, EarthlyBranch.YOU,
        )
        val n = r.yun("庚寅")!!.years.firstOrNull { it.year == 1994 }
        assertNotNull(n)
        assertTrue("1994 应为发财年:${n!!.kind}/${n.text}", n.kind.chinese.contains("发"))
    }

    @Test
    fun `例十四 乙丑运丙子 财星当值有力 发大财`() {
        val r = analyze(
            HeavenlyStem.JI, EarthlyBranch.MAO,
            HeavenlyStem.XIN, EarthlyBranch.WEI,
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.GENG, EarthlyBranch.WU,
            fromYear = 1930, toYear = 1945,
        )
        val n = r.yun("乙丑")!!.years.firstOrNull { it.year == 1996 }
        assertNotNull(n)
        assertTrue(n!!.kind.chinese.contains("发"))
    }
}
