package cn.xulun.bazi

import cn.xulun.bazi.core.CaiYunCalculator
import cn.xulun.bazi.core.DaYunCalculator
import cn.xulun.bazi.model.CaiYunKind
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 财运岁运测试续（教材例十六至二十九批量锚定，.caiyun-reference.md 第五节）。
 *
 * 参数化:四柱+大运名+年份+期望(发/破)。逐例断言关键流年方向。
 */
class CaiYunBatch2Test {

    private data class Case(
        val ganzhi: String,
        val female: Boolean,
        val from: Int,
        val to: Int,
        val yun: String,
        val year: Int,
        val expectFa: Boolean, // true=发财 false=破财/谨慎
    )

    private fun hs(c: Char) = HeavenlyStem.entries.first { it.chinese == c.toString() }
    private fun ez(c: Char) = EarthlyBranch.entries.first { it.chinese == c.toString() }

    private fun run(c: Case): cn.xulun.bazi.model.CaiYunYear? {
        val g = c.ganzhi
        val bazi = chartOf(hs(g[0]), ez(g[1]), hs(g[2]), ez(g[3]), hs(g[4]), ez(g[5]), hs(g[6]), ez(g[7]), c.from, c.to)
        val gender = if (c.female) Gender.FEMALE else Gender.MALE
        val r = CaiYunCalculator.analyze(bazi, gender, DaYunCalculator.compute(bazi, gender))
        val y = r.yuns.firstOrNull { it.pillar.stem.chinese + it.pillar.branch.chinese == c.yun }
        assertNotNull("缺大运 ${c.yun}: ${r.yuns.joinToString { it.pillar.fullName }}", y)
        return y!!.years.firstOrNull { it.year == c.year }
    }

    private fun assertCase(c: Case) {
        val n = run(c)
        assertNotNull("缺关键年 ${c.year}(${c.ganzhi}/${c.yun}运)", n)
        val ok = if (c.expectFa) n!!.kind == CaiYunKind.FA else n!!.kind != CaiYunKind.FA
        assertTrue("${c.ganzhi} ${c.yun}运 ${c.year} 期望${if (c.expectFa) "发" else "破/谨慎"}实际${n.kind.chinese}: ${n.text.take(80)}", ok)
    }

    @Test
    fun `例十六 戊寅1998 官星引动制劫 发大财`() = assertCase(
        Case("癸未己未戊子癸亥", false, 1940, 1950, "甲寅", 1998, true)
    )

    @Test
    fun `例十七 乙亥1995丙子1996 财星得生从象真 发财`() {
        assertCase(Case("癸卯甲子庚寅戊寅", true, 1960, 1970, "丁卯", 1995, true))
        assertCase(Case("癸卯甲子庚寅戊寅", true, 1960, 1970, "丁卯", 1996, true))
    }

    @Test
    fun `例十八 甲申乙酉食伤生财 丁亥引动财星 发财`() {
        assertCase(Case("庚子壬午戊辰丁巳", true, 1955, 1965, "丁丑", 2004, true))
        assertCase(Case("庚子壬午戊辰丁巳", true, 1955, 1965, "丁丑", 2007, true))
    }

    @Test
    fun `例十九 丙戌己丑庚寅 和而不战 财运昌隆`() {
        assertCase(Case("戊午丁巳丁丑辛丑", false, 1975, 1985, "庚申", 2006, true))
        assertCase(Case("戊午丁巳丁丑辛丑", false, 1975, 1985, "庚申", 2010, true))
    }

    @Test
    fun `例二十 庚寅2010 偏财当值 发财数百万`() = assertCase(
        Case("戊午己未庚子癸未", false, 1975, 1985, "癸亥", 2010, true)
    )

    @Test
    fun `例二十一 丙戌2006忌神破财 丁亥2007发财`() {
        assertCase(Case("癸丑壬戌辛卯乙未", true, 1970, 1980, "乙丑", 2006, false))
        assertCase(Case("癸丑壬戌辛卯乙未", true, 1970, 1980, "乙丑", 2007, true))
    }

    @Test
    fun `例二十二 壬申1992发 乙亥1995发 丁丑1997破 己卯1999破`() {
        assertCase(Case("己亥壬申戊辰丙辰", true, 1955, 1965, "乙亥", 1992, true))
        assertCase(Case("己亥壬申戊辰丙辰", true, 1955, 1965, "乙亥", 1995, true))
        assertCase(Case("己亥壬申戊辰丙辰", true, 1955, 1965, "乙亥", 1997, false))
        assertCase(Case("己亥壬申戊辰丙辰", true, 1955, 1965, "丙子", 1999, false)) // 引擎起运分界:1999属丙子运
    }

    @Test
    fun `例二十八 甲戌1994 刑冲引动木土交战 破财`() = assertCase(
        Case("甲辰乙亥丁丑丙午", false, 1960, 1970, "戊寅", 1994, false)
    )

    @Test
    fun `例二十九 丙子1996 财透而弱被克泄 破财`() = assertCase(
        Case("庚寅乙酉癸亥己未", true, 1945, 1955, "庚辰", 1996, false)
    )
}
