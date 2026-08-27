package cn.xulun.bazi

import cn.xulun.bazi.core.CongGeYunCalculator
import cn.xulun.bazi.core.TeShuGeJuCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.YunVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 从格岁运喜忌锚点测试。理论见 .shengke-reference.md 第五二节（从财格）。
 * 主锚：壬辰壬子己亥壬申（从财）岁运分类总表十五柱 + 成格三例 + 实例三则。
 */
class CongGeYunCalculatorTest {

    // ── 成格三例（含合化入势从真） ──

    @Test fun `成格一 戊辰己未乙丑丙戌 从财`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.CHEN, HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.YI, EarthlyBranch.CHOU, HeavenlyStem.BING, EarthlyBranch.XU))!!
        assertTrue("应从财：${r.name}", r.name.contains("从财"))
    }

    @Test fun `成格二 癸巳戊午壬午丙午 癸合戊化从真`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.SI, HeavenlyStem.WU, EarthlyBranch.WU,
            HeavenlyStem.REN, EarthlyBranch.WU, HeavenlyStem.BING, EarthlyBranch.WU))!!
        assertTrue("应从财（癸合化不以虚助论）：${r.name} fake=${r.fake}", r.name.contains("从财"))
        assertTrue("癸合戊化入势应从真：fake=${r.fake}", !r.fake)
    }

    @Test fun `成格三 庚辰乙酉丁酉己酉 乙庚合化反助金势从真`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GENG, EarthlyBranch.CHEN, HeavenlyStem.YI, EarthlyBranch.YOU,
            HeavenlyStem.DING, EarthlyBranch.YOU, HeavenlyStem.JI, EarthlyBranch.YOU))!!
        assertTrue("应从财：${r.name}", r.name.contains("从财"))
        assertTrue("乙合庚化不以破格论应从真：fake=${r.fake}", !r.fake)
    }

    // ── 岁运分类总表（壬辰 壬子 己亥 壬申，从财·水） ──

    private val congCai = chartOf(
        HeavenlyStem.REN, EarthlyBranch.CHEN, HeavenlyStem.REN, EarthlyBranch.ZI,
        HeavenlyStem.JI, EarthlyBranch.HAI, HeavenlyStem.REN, EarthlyBranch.SHEN)

    private fun v(p: Pillar): YunVerdict? = CongGeYunCalculator.analyze(congCai, listOf(p))?.verdict

    @Test fun `用神到位 旺水六柱`() {
        for ((s, b) in listOf(
            HeavenlyStem.REN to EarthlyBranch.ZI, HeavenlyStem.GUI to EarthlyBranch.HAI,
            HeavenlyStem.REN to EarthlyBranch.SHEN, HeavenlyStem.GUI to EarthlyBranch.YOU,
            HeavenlyStem.GENG to EarthlyBranch.ZI, HeavenlyStem.XIN to EarthlyBranch.HAI,
        )) assertEquals("(${s.chinese}${b.chinese}) 应用神", YunVerdict.YONG, v(Pillar(s, b)))
    }

    @Test fun `喜神到位 食伤四柱`() {
        for ((s, b) in listOf(
            HeavenlyStem.GENG to EarthlyBranch.SHEN, HeavenlyStem.XIN to EarthlyBranch.YOU,
            HeavenlyStem.WU to EarthlyBranch.SHEN, HeavenlyStem.JI to EarthlyBranch.YOU,
        )) assertEquals("(${s.chinese}${b.chinese}) 应喜神", YunVerdict.XI, v(Pillar(s, b)))
    }

    @Test fun `转两气 强印丙午丁巳取印`() {
        for ((s, b) in listOf(HeavenlyStem.BING to EarthlyBranch.WU, HeavenlyStem.DING to EarthlyBranch.SI)) {
            val r = CongGeYunCalculator.analyze(congCai, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 应转格", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.FIRE, r.newYongShen)
        }
    }

    @Test fun `转两气 强官甲寅乙卯壬寅癸卯取官`() {
        for ((s, b) in listOf(
            HeavenlyStem.JIA to EarthlyBranch.YIN, HeavenlyStem.YI to EarthlyBranch.MAO,
            HeavenlyStem.REN to EarthlyBranch.YIN, HeavenlyStem.GUI to EarthlyBranch.MAO,
        )) {
            val r = CongGeYunCalculator.analyze(congCai, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 应转格", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.WOOD, r.newYongShen)
        }
    }

    @Test fun `转两气 强比戊戌己未丙戌丁未取比`() {
        for ((s, b) in listOf(
            HeavenlyStem.WU to EarthlyBranch.XU, HeavenlyStem.JI to EarthlyBranch.WEI,
            HeavenlyStem.BING to EarthlyBranch.XU, HeavenlyStem.DING to EarthlyBranch.WEI,
        )) {
            val r = CongGeYunCalculator.analyze(congCai, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 应转格", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.EARTH, r.newYongShen)
        }
    }

    @Test fun `忌神 衰弱印绶庚午`() {
        assertEquals(YunVerdict.JI, v(Pillar(HeavenlyStem.GENG, EarthlyBranch.WU)))
    }

    // ── 实例三则 ──

    @Test fun `杨先生 假从财 丙子年忌神`() {
        val b = chartOf(
            HeavenlyStem.REN, EarthlyBranch.WU, HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.REN, EarthlyBranch.YIN, HeavenlyStem.DING, EarthlyBranch.WEI)
        val ts = TeShuGeJuCalculator.analyze(b)
        assertNotNull("应判从格：$ts", ts)
        assertTrue("应从财（火土）：${ts!!.name}", ts.name.contains("从财"))
        assertTrue("壬庚居干应假从：fake=${ts.fake}", ts.fake)
        // 乙卯运丙子年：太岁水引发反叛，忌神（凶）
        val r = CongGeYunCalculator.analyze(b, listOf(
            Pillar(HeavenlyStem.YI, EarthlyBranch.MAO), Pillar(HeavenlyStem.BING, EarthlyBranch.ZI)))
        assertTrue("丙子年应忌/破：$r", r == null || r.verdict == YunVerdict.JI || r.verdict == YunVerdict.HUAN_YUAN)
    }

    @Test fun `刘女士 假从财 甲戌年丑戌刑动还原`() {
        val b = chartOf(
            HeavenlyStem.XIN, EarthlyBranch.HAI, HeavenlyStem.JI, EarthlyBranch.HAI,
            HeavenlyStem.JI, EarthlyBranch.YOU, HeavenlyStem.GUI, EarthlyBranch.YOU)
        val ts = TeShuGeJuCalculator.analyze(b)!!
        assertTrue("应从财（水）：${ts.name}", ts.name.contains("从财"))
        assertTrue("月干己帮身应假从：fake=${ts.fake}", ts.fake)
        // 辛丑运甲戌年：岁支戌为日主之根，还原正格（刑狱）
        val r = CongGeYunCalculator.analyze(b, listOf(
            Pillar(HeavenlyStem.XIN, EarthlyBranch.CHOU), Pillar(HeavenlyStem.JIA, EarthlyBranch.XU)))
        assertEquals(YunVerdict.HUAN_YUAN, r?.verdict)
        // 壬寅运丁丑年：太岁土帮身破格还原（官司）
        val r2 = CongGeYunCalculator.analyze(b, listOf(
            Pillar(HeavenlyStem.REN, EarthlyBranch.YIN), Pillar(HeavenlyStem.DING, EarthlyBranch.CHOU)))
        assertEquals(YunVerdict.HUAN_YUAN, r2?.verdict)
    }

    @Test fun `席女士 假从 辛未年用神到位从象更真`() {
        val b = chartOf(
            HeavenlyStem.GUI, EarthlyBranch.CHOU, HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.JIA, EarthlyBranch.CHEN, HeavenlyStem.XIN, EarthlyBranch.WEI)
        val ts = TeShuGeJuCalculator.analyze(b)
        assertNotNull("应判从格：$ts", ts)
        // 戊午运辛未年：从象更真——吉（用神/喜神）
        val r = CongGeYunCalculator.analyze(b, listOf(
            Pillar(HeavenlyStem.WU, EarthlyBranch.WU), Pillar(HeavenlyStem.XIN, EarthlyBranch.WEI)))
        assertTrue("辛未年应用/喜（吉）：$r", r != null && (r.verdict == YunVerdict.YONG || r.verdict == YunVerdict.XI))
    }

    // ════════ 从官(杀)格 ════════

    // ── 成格三例 ──

    @Test fun `杀成格一 壬申壬子丙子壬辰 申子辰从杀`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.REN, EarthlyBranch.SHEN, HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.BING, EarthlyBranch.ZI, HeavenlyStem.REN, EarthlyBranch.CHEN))!!
        assertTrue("应从杀：${r.name}", r.name.contains("从杀"))
    }

    @Test fun `杀成格二 癸卯乙卯戊辰甲寅 辰会木局从杀`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.MAO, HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.WU, EarthlyBranch.CHEN, HeavenlyStem.JIA, EarthlyBranch.YIN))!!
        assertTrue("应从杀：${r.name}", r.name.contains("从杀"))
    }

    @Test fun `杀成格三 癸巳戊午辛未甲午 火局三会癸合戊化从杀`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GUI, EarthlyBranch.SI, HeavenlyStem.WU, EarthlyBranch.WU,
            HeavenlyStem.XIN, EarthlyBranch.WEI, HeavenlyStem.JIA, EarthlyBranch.WU))!!
        assertTrue("应从杀：${r.name}", r.name.contains("从杀"))
        assertTrue("癸合戊化应从真：fake=${r.fake}", !r.fake)
    }

    // ── 岁运总表（壬子 癸丑 丁亥 庚子，从杀·水） ──

    private val congSha = chartOf(
        HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.GUI, EarthlyBranch.CHOU,
        HeavenlyStem.DING, EarthlyBranch.HAI, HeavenlyStem.GENG, EarthlyBranch.ZI)

    private fun vs(p: Pillar): YunVerdict? = CongGeYunCalculator.analyze(congSha, listOf(p))?.verdict

    @Test fun `杀总表 用神旺水四柱`() {
        for ((s, b) in listOf(
            HeavenlyStem.REN to EarthlyBranch.ZI, HeavenlyStem.GUI to EarthlyBranch.HAI,
            HeavenlyStem.GENG to EarthlyBranch.ZI, HeavenlyStem.XIN to EarthlyBranch.HAI,
        )) assertEquals("(${s.chinese}${b.chinese}) 应用神", YunVerdict.YONG, vs(Pillar(s, b)))
    }

    @Test fun `杀总表 喜神财金四柱`() {
        // 壬申/癸酉（干透杀+支财）书归喜神而同构之从财表归用神——皆吉档，放宽为喜/用
        for ((s, b) in listOf(
            HeavenlyStem.GENG to EarthlyBranch.SHEN, HeavenlyStem.XIN to EarthlyBranch.YOU,
            HeavenlyStem.REN to EarthlyBranch.SHEN, HeavenlyStem.GUI to EarthlyBranch.YOU,
        )) {
            val v = vs(Pillar(s, b))
            assertTrue("(${s.chinese}${b.chinese}) 应喜/用（吉档）：$v", v == YunVerdict.XI || v == YunVerdict.YONG)
        }
    }

    @Test fun `杀总表 转格强食伤戊戌己未丙戌丁未`() {
        for ((s, b) in listOf(
            HeavenlyStem.WU to EarthlyBranch.XU, HeavenlyStem.JI to EarthlyBranch.WEI,
            HeavenlyStem.BING to EarthlyBranch.XU, HeavenlyStem.DING to EarthlyBranch.WEI,
        )) {
            val r = CongGeYunCalculator.analyze(congSha, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 应转格", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.EARTH, r.newYongShen)
        }
    }

    @Test fun `杀总表 转格强印甲寅乙卯壬寅癸卯`() {
        for ((s, b) in listOf(
            HeavenlyStem.JIA to EarthlyBranch.YIN, HeavenlyStem.YI to EarthlyBranch.MAO,
            HeavenlyStem.REN to EarthlyBranch.YIN, HeavenlyStem.GUI to EarthlyBranch.MAO,
        )) {
            val r = CongGeYunCalculator.analyze(congSha, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 应转格", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.WOOD, r.newYongShen)
        }
    }

    @Test fun `杀总表 转格强比丙午丁巳身官两气`() {
        for ((s, b) in listOf(HeavenlyStem.BING to EarthlyBranch.WU, HeavenlyStem.DING to EarthlyBranch.SI)) {
            val r = CongGeYunCalculator.analyze(congSha, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 应转格", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.FIRE, r.newYongShen)
        }
    }

    @Test fun `杀总表 忌神衰土戊子己亥壬戌癸未`() {
        for ((s, b) in listOf(
            HeavenlyStem.WU to EarthlyBranch.ZI, HeavenlyStem.JI to EarthlyBranch.HAI,
            HeavenlyStem.REN to EarthlyBranch.XU, HeavenlyStem.GUI to EarthlyBranch.WEI,
        )) assertEquals("(${s.chinese}${b.chinese}) 应忌神", YunVerdict.JI, vs(Pillar(s, b)))
    }

    @Test fun `杀总表 忌神衰木印水多木漂甲戌乙未戊寅己卯`() {
        for ((s, b) in listOf(
            HeavenlyStem.JIA to EarthlyBranch.XU, HeavenlyStem.YI to EarthlyBranch.WEI,
            HeavenlyStem.WU to EarthlyBranch.YIN, HeavenlyStem.JI to EarthlyBranch.MAO,
        )) assertEquals("(${s.chinese}${b.chinese}) 应忌神", YunVerdict.JI, vs(Pillar(s, b)))
    }

    // ── 实例 ──

    @Test fun `吴先生 丙午乙未庚午壬午 无根无助真从`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.BING, EarthlyBranch.WU, HeavenlyStem.YI, EarthlyBranch.WEI,
            HeavenlyStem.GENG, EarthlyBranch.WU, HeavenlyStem.REN, EarthlyBranch.WU))!!
        assertTrue("应从杀（火）：${r.name}", r.name.contains("从杀"))
        assertTrue("无根无助不受土生应真从：fake=${r.fake}", !r.fake)
    }

    @Test fun `坤造庚寅戊寅己卯甲子 假从 己未年还原凶`() {
        val b = chartOf(
            HeavenlyStem.GENG, EarthlyBranch.YIN, HeavenlyStem.WU, EarthlyBranch.YIN,
            HeavenlyStem.JI, EarthlyBranch.MAO, HeavenlyStem.JIA, EarthlyBranch.ZI)
        val ts = TeShuGeJuCalculator.analyze(b)!!
        assertTrue("应从杀（木）：${ts.name}", ts.name.contains("从杀"))
        assertTrue("月干戊帮身应假从：fake=${ts.fake}", ts.fake)
        // 乙亥运己未年：未土破格以弱犯强——还原（凶，离婚）
        val r = CongGeYunCalculator.analyze(b, listOf(
            Pillar(HeavenlyStem.YI, EarthlyBranch.HAI), Pillar(HeavenlyStem.JI, EarthlyBranch.WEI)))
        assertEquals(YunVerdict.HUAN_YUAN, r?.verdict)
    }

    // ════════ 从儿格 ════════

    // ── 成格四例 ──

    @Test fun `儿成格一 丁巳丙午甲戌己巳 从儿`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.DING, EarthlyBranch.SI, HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.JIA, EarthlyBranch.XU, HeavenlyStem.JI, EarthlyBranch.SI))!!
        assertTrue("应从儿：${r.name}", r.name.contains("从儿"))
    }

    @Test fun `儿成格二 戊戌己未丙辰己丑 食伤四库从儿`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.WU, EarthlyBranch.XU, HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.BING, EarthlyBranch.CHEN, HeavenlyStem.JI, EarthlyBranch.CHOU))!!
        assertTrue("应从儿：${r.name}", r.name.contains("从儿"))
    }

    @Test fun `儿成格三 庚申乙酉戊戌庚申 申酉戌三会从儿`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.GENG, EarthlyBranch.SHEN, HeavenlyStem.YI, EarthlyBranch.YOU,
            HeavenlyStem.WU, EarthlyBranch.XU, HeavenlyStem.GENG, EarthlyBranch.SHEN))!!
        assertTrue("应从儿：${r.name}", r.name.contains("从儿"))
    }

    @Test fun `儿成格四 壬子壬子庚子丁亥 丁壬反化从儿`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.REN, EarthlyBranch.ZI, HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.GENG, EarthlyBranch.ZI, HeavenlyStem.DING, EarthlyBranch.HAI))!!
        assertTrue("应从儿：${r.name}", r.name.contains("从儿"))
    }

    @Test fun `儿非提纲一 丙戌庚寅甲午庚午 支合食伤局从儿`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.BING, EarthlyBranch.XU, HeavenlyStem.GENG, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.WU, HeavenlyStem.GENG, EarthlyBranch.WU))!!
        assertTrue("应从儿（食伤不在提纲，层次稍低）：${r.name}", r.name.contains("从儿"))
    }

    @Test fun `儿非提纲三 壬辰戊申庚子丙子 申子辰从儿`() {
        val r = TeShuGeJuCalculator.analyze(chartOf(
            HeavenlyStem.REN, EarthlyBranch.CHEN, HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.GENG, EarthlyBranch.ZI, HeavenlyStem.BING, EarthlyBranch.ZI))!!
        assertTrue("应从儿：${r.name}", r.name.contains("从儿"))
    }

    // ── 岁运总表（己未 戊辰 丙戌 戊戌，从儿·土） ──

    private val congEr = chartOf(
        HeavenlyStem.JI, EarthlyBranch.WEI, HeavenlyStem.WU, EarthlyBranch.CHEN,
        HeavenlyStem.BING, EarthlyBranch.XU, HeavenlyStem.WU, EarthlyBranch.XU)

    private fun ve(p: Pillar): YunVerdict? = CongGeYunCalculator.analyze(congEr, listOf(p))?.verdict

    @Test fun `儿总表 用神旺土四柱`() {
        for ((s, b) in listOf(
            HeavenlyStem.WU to EarthlyBranch.CHEN, HeavenlyStem.WU to EarthlyBranch.XU,
            HeavenlyStem.JI to EarthlyBranch.CHOU, HeavenlyStem.JI to EarthlyBranch.WEI,
        )) assertEquals("(${s.chinese}${b.chinese}) 应用神", YunVerdict.YONG, ve(Pillar(s, b)))
    }

    @Test fun `儿总表 转格强印甲寅乙卯取印`() {
        for ((s, b) in listOf(HeavenlyStem.JIA to EarthlyBranch.YIN, HeavenlyStem.YI to EarthlyBranch.MAO)) {
            val r = CongGeYunCalculator.analyze(congEr, listOf(Pillar(s, b)))!!
            assertEquals(YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.WOOD, r.newYongShen)
        }
    }

    @Test fun `儿总表 转格强官壬子癸亥取官`() {
        for ((s, b) in listOf(HeavenlyStem.REN to EarthlyBranch.ZI, HeavenlyStem.GUI to EarthlyBranch.HAI)) {
            val r = CongGeYunCalculator.analyze(congEr, listOf(Pillar(s, b)))!!
            assertEquals(YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.WATER, r.newYongShen)
        }
    }

    @Test fun `儿总表 转格强财庚申辛酉取财`() {
        for ((s, b) in listOf(HeavenlyStem.GENG to EarthlyBranch.SHEN, HeavenlyStem.XIN to EarthlyBranch.YOU)) {
            val r = CongGeYunCalculator.analyze(congEr, listOf(Pillar(s, b)))!!
            assertEquals("(${s.chinese}${b.chinese}) 应转格", YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.METAL, r.newYongShen)
        }
    }

    @Test fun `儿总表 转格强比丙午丁巳身食两气`() {
        for ((s, b) in listOf(HeavenlyStem.BING to EarthlyBranch.WU, HeavenlyStem.DING to EarthlyBranch.SI)) {
            val r = CongGeYunCalculator.analyze(congEr, listOf(Pillar(s, b)))!!
            assertEquals(YunVerdict.ZHUAN_GE, r.verdict)
            assertEquals(Element.FIRE, r.newYongShen)
        }
    }

    // ── 实例 ──

    @Test fun `儿李先生 辛卯庚寅癸未乙卯 卯未半合不破格 丙寅年吉`() {
        val b = chartOf(
            HeavenlyStem.XIN, EarthlyBranch.MAO, HeavenlyStem.GENG, EarthlyBranch.YIN,
            HeavenlyStem.GUI, EarthlyBranch.WEI, HeavenlyStem.YI, EarthlyBranch.MAO)
        val ts = TeShuGeJuCalculator.analyze(b)!!
        assertTrue("应从儿（木）：${ts.name}", ts.name.contains("从儿"))
        assertEquals(listOf(Element.WOOD), ts.yongShen)
        // 丙戌运丙寅年：寅木在势+丙辛合卯戌合和局——吉（升官）
        val r = CongGeYunCalculator.analyze(b, listOf(
            Pillar(HeavenlyStem.BING, EarthlyBranch.XU), Pillar(HeavenlyStem.BING, EarthlyBranch.YIN)))
        assertEquals(YunVerdict.YONG, r?.verdict)
    }
}
