package cn.xulun.bazi

import cn.xulun.bazi.core.BaziCalculator
import cn.xulun.bazi.core.DaYunCalculator
import cn.xulun.bazi.core.GuanYunCalculator
import cn.xulun.bazi.core.HunYinCalculator
import cn.xulun.bazi.core.LiuQinCalculator
import cn.xulun.bazi.core.WenPingCalculator
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.GuanYunKind
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * 师传案例锚定·卷一（.ziping-reference.md 档四十四～五十一）。
 *
 * 档四十四 癸丑乙丑辛酉辛卯——辛酉日阴阳差错，妻婚前堕两胎；
 * 档四十七 丁卯己酉乙亥戊寅（坤）——夫星落空亡克夫、财官印依次相生本科；
 * 档四十八 庚午己卯戊寅乙卯——官旺无印文章无彩、2016丙申冲忌婚动机（差一点成婚）；
 * 档五十 庚戌己卯乙卯戊寅——建禄月令不论排行（老大）、同胞三人（刃不双计）；
 * 档五十一 戊午辛酉壬午壬寅——印化七杀硕士、2007丁亥晋升。
 */
class ShiChuanAnLiTest {

    private fun chart(y: Int, m: Int, d: Int, h: Int) =
        BaziCalculator.calculate(LocalDateTime.of(y, m, d, h, 0))

    @Test
    fun `档四十四 乾 癸丑乙丑辛酉辛卯 辛酉日阴阳差错 婚育有损`() {
        val bazi = chart(1974, 1, 20, 6)
        assertTrue(bazi.fullBazi.startsWith("癸丑 乙丑 辛酉 辛卯"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        val hy = HunYinCalculator.analyze(bazi, Gender.MALE, daYun)
        assertTrue("日坐阴阳差错——妻婚前堕两胎之基", hy.baseUnfav.any { it.dim.contains("阴阳差错") })
    }

    @Test
    fun `档四十七 坤 丁卯己酉乙亥戊寅 夫星落空克夫 财官印相生本科`() {
        val bazi = chart(1987, 9, 23, 4)
        assertTrue(bazi.fullBazi.startsWith("丁卯 己酉 乙亥 戊寅"))
        val daYun = DaYunCalculator.compute(bazi, Gender.FEMALE)
        val hy = HunYinCalculator.analyze(bazi, Gender.FEMALE, daYun)
        assertTrue("比劫羊刃+夫星落空亡——克夫之象（未婚无子之基）",
            hy.baseUnfav.any { it.dim.contains("克夫") && it.text.contains("空亡") })
        val wp = WenPingCalculator.analyze(bazi, daYun)
        assertTrue("财官印身依次相生——本科", wp.favorable.any { it.title.contains("依次相生") })
    }

    @Test
    fun `档四十八 乾 庚午己卯戊寅乙卯 官旺无印文章无彩 2016丙申冲忌婚动机`() {
        val bazi = chart(1990, 3, 14, 6)
        assertTrue(bazi.fullBazi.startsWith("庚午 己卯 戊寅 乙卯"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        val wp = WenPingCalculator.analyze(bazi, daYun)
        assertTrue("官旺制身无印通关——学历不高（大专之数）",
            wp.unfavorable.any { it.text.contains("文章无彩") })
        val hy = HunYinCalculator.analyze(bazi, Gender.MALE, daYun)
        val y2016 = hy.yuns.flatMap { it.years }.firstOrNull { it.year == 2016 }
        assertTrue("2016丙申冲妻宫忌神——婚动机之年（差一点成婚）",
            y2016 != null && y2016.kind.chinese.contains("婚变动荡") && y2016.triggers.any { it.contains("冲配偶宫") })
    }

    @Test
    fun `档五十 乾 庚戌己卯乙卯戊寅 建禄月令不论排行 老大 同胞三人`() {
        val bazi = chart(1970, 4, 5, 4)
        assertTrue(bazi.fullBazi.startsWith("庚戌 己卯 乙卯 戊寅"))
        val x = LiuQinCalculator.analyze(bazi, Gender.MALE).xiongDi
        assertTrue("月支卯是乙自己的禄——不论「非老大」（实际排行老大）", x.ranking == null)
        assertTrue("日主1+支本气比劫2（卯寅）——刃寅不双计——同胞三人",
            x.count == "3个左右" && x.countBasis.contains("建禄月令"))
    }

    @Test
    fun `档五十一 乾 戊午辛酉壬午壬寅 印化七杀硕士 2007丁亥晋升`() {
        val bazi = chart(1978, 9, 17, 4)
        assertTrue(bazi.fullBazi.startsWith("戊午 辛酉 壬午 壬寅"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        val wp = WenPingCalculator.analyze(bazi, daYun)
        assertTrue("印化七杀——硕士", wp.favorable.any { it.title.contains("印化七杀") })
        val gy = GuanYunCalculator.analyze(bazi, daYun)
        val y2007 = gy.yuns.flatMap { it.years }.firstOrNull { it.year == 2007 }
        assertTrue("2007丁亥财官印相生——晋升（甲子运一路高升）",
            y2007 != null && y2007.kind == GuanYunKind.JIN_SHENG)
    }
}
