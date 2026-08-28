package cn.xulun.bazi

import cn.xulun.bazi.core.DaYunCalculator
import cn.xulun.bazi.core.StudyCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 文凭推断引擎测试（.xueye-reference.md 第十节·一）。锚定例一（博士）与林先生命。
 * 学历依据=用神+印星：格局清纯/少运印用神/印忌行食伤运→高；印用少行财运→不高；
 * 例一：原局无印大运生之，身旺食伤（文昌）泄秀，循环相生→博士。
 */
class WenPingCalculatorTest {

    @Test
    fun `例一 某先生 癸卯壬戌壬子壬寅 少运印补身旺泄秀 高学历`() {
        val bazi = chartOf(
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.REN, EarthlyBranch.XU,
            HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.REN, EarthlyBranch.YIN,
            1900, 1970,
        )
        assertEquals2("癸卯 壬戌 壬子 壬寅", bazi.fullBazi)
        val r = StudyCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        val dip = r.diploma
        assertNotNull(dip)
        assertTrue(dip.level, dip.level.contains("高学历"))
        // 身旺食伤泄秀为喜用（壬日食伤=木：卯寅；文昌=寅）
        assertTrue(dip.favorable.any { it.title.contains("食伤泄秀") })
        assertTrue(dip.favorable.first { it.title.contains("食伤泄秀") }.text.contains("文昌"))
        // 原局印弱+少运印运（辛酉庚申）→ 大运生之登金榜
        assertTrue(dip.basis.contains("大运") || dip.basis.contains("印"))
    }

    @Test
    fun `例一 少运辛酉庚申为印运 首运含印`() {
        val bazi = chartOf(
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.REN, EarthlyBranch.XU,
            HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.REN, EarthlyBranch.YIN,
            1900, 1970,
        )
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(daYun.yuns.take(2).any { it.pillar.stem == HeavenlyStem.XIN || it.pillar.stem == HeavenlyStem.GENG })
    }

    @Test
    fun `林先生 丁未丙午戊申乙卯 身强印旺比劫重重为忌+身旺官杀为喜用`() {
        val bazi = chartOf(
            HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.YI, EarthlyBranch.MAO,
            1950, 1980,
        )
        val r = StudyCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        assertTrue(r.diploma.unfavorable.any { it.title.contains("身强印旺") && it.title.contains("比劫重重") })
        assertTrue(r.diploma.favorable.any { it.title.contains("身旺官杀为喜用") })
    }

    @Test
    fun `例二 某先生 癸丑乙卯丙午庚寅 身旺印旺癸丑运伤官泄秀 高学历`() {
        val bazi = chartOf(
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.GENG, EarthlyBranch.YIN,
            1900, 1980,
        )
        assertEquals2("癸丑 乙卯 丙午 庚寅", bazi.fullBazi)
        val r = StudyCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        val dip = r.diploma
        assertTrue(dip.level, dip.level.contains("高学历"))
        // 运支丑伤官当值泄秀，伤官佩印——名登金榜
        assertTrue(dip.favorable.any { it.title.contains("伤官佩印") })
        assertTrue(dip.basis.contains("伤官") || dip.favorable.any { it.title.contains("伤官") })
    }

    @Test
    fun `例三 某先生 丁巳辛亥乙酉戊寅 杀印相生 研究生`() {
        val bazi = chartOf(
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.XIN, EarthlyBranch.HAI,
            HeavenlyStem.YI, EarthlyBranch.YOU,
            HeavenlyStem.WU, EarthlyBranch.YIN,
            1950, 2000,
        )
        assertEquals2("丁巳 辛亥 乙酉 戊寅", bazi.fullBazi)
        val r = StudyCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        val dip = r.diploma
        assertTrue(dip.level, dip.level.contains("高学历"))
        // 四柱杀旺、印星当令、化杀生身——印化七杀定登科第
        assertTrue(dip.favorable.any { it.title.contains("印化七杀") })
        // 少行财运而财生杀、杀生印、印生身——循环相生，杀印相生功名显达
        assertTrue(dip.basis.contains("杀印相生"))
    }

    @Test
    fun `例四 某先生 丁巳丙午丙辰辛卯 身强辰土泄秀甲辰运 研究生`() {
        val bazi = chartOf(
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.BING, EarthlyBranch.CHEN,
            HeavenlyStem.XIN, EarthlyBranch.MAO,
            1950, 2000,
        )
        assertEquals2("丁巳 丙午 丙辰 辛卯", bazi.fullBazi)
        val r = StudyCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        val dip = r.diploma
        assertTrue(dip.level, dip.level.contains("高学历"))
        // 身强以辰土（湿土泄秀/教材谓文昌）为用，17-27岁甲辰运用神到位——顺利攻读研究生
        assertTrue(dip.favorable.any { it.title.contains("食伤泄秀") })
        assertTrue(dip.basis.contains("印星为忌") || dip.basis.contains("泄秀"))
    }

    @Test
    fun `例五 某先生 癸卯甲子壬寅壬寅 身旺食伤旺秀气流通 研究生`() {
        val bazi = chartOf(
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.JIA, EarthlyBranch.ZI,
            HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.REN, EarthlyBranch.YIN,
            1900, 2000,
        )
        assertEquals2("癸卯 甲子 壬寅 壬寅", bazi.fullBazi)
        val r = StudyCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        val dip = r.diploma
        assertTrue(dip.level, dip.level.contains("高学历"))
        val xieXiu = dip.favorable.first { it.title.contains("食伤泄秀") }
        assertTrue(xieXiu.text.contains("秀气流通"))
    }

    @Test
    fun `例六 某先生 癸卯丙辰壬寅己酉 从木火土文昌照命 本科`() {
        val bazi = chartOf(
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.BING, EarthlyBranch.CHEN,
            HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.JI, EarthlyBranch.YOU,
            1900, 2000,
        )
        assertEquals2("癸卯 丙辰 壬寅 己酉", bazi.fullBazi)
        val r = StudyCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        val dip = r.diploma
        assertTrue(dip.level, dip.level.contains("高学历"))
        assertTrue(dip.basis.contains("从"))
    }

    @Test
    fun `例七 某女士 己酉丙子壬申辛丑 土金水顺生贵格 轻易本科`() {
        val bazi = chartOf(
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.BING, EarthlyBranch.ZI,
            HeavenlyStem.REN, EarthlyBranch.SHEN,
            HeavenlyStem.XIN, EarthlyBranch.CHOU,
            1900, 2000,
        )
        assertEquals2("己酉 丙子 壬申 辛丑", bazi.fullBazi)
        val r = StudyCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.FEMALE))
        val dip = r.diploma
        assertTrue(dip.level, dip.level.contains("高学历"))
        assertTrue(dip.favorable.any { it.title.contains("依次相生") })
    }

    @Test
    fun `例八 某先生 乙巳己丑庚午丙戌 从火土两行清纯 本科`() {
        val bazi = chartOf(
            HeavenlyStem.YI, EarthlyBranch.SI,
            HeavenlyStem.JI, EarthlyBranch.CHOU,
            HeavenlyStem.GENG, EarthlyBranch.WU,
            HeavenlyStem.BING, EarthlyBranch.XU,
            1900, 2000,
        )
        assertEquals2("乙巳 己丑 庚午 丙戌", bazi.fullBazi)
        val r = StudyCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        val dip = r.diploma
        assertTrue(dip.level, dip.level.contains("高学历"))
        assertTrue(dip.basis.contains("从神"))
    }

    @Test
    fun `例九 某先生 戊申乙卯壬寅戊申 三行相战丁巳运通关和局 重点本科`() {
        val bazi = chartOf(
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            1900, 2000,
        )
        assertEquals2("戊申 乙卯 壬寅 戊申", bazi.fullBazi)
        val r = StudyCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        val dip = r.diploma
        assertTrue(dip.level, dip.level.contains("高学历"))
        assertTrue(dip.basis.contains("从神") || dip.basis.contains("通关"))
    }

    @Test
    fun `例十 黄先生 戊午乙卯己巳乙亥 身弱杀旺巳午印化杀 登榜`() {
        val bazi = chartOf(
            HeavenlyStem.WU, EarthlyBranch.WU,
            HeavenlyStem.YI, EarthlyBranch.MAO,
            HeavenlyStem.JI, EarthlyBranch.SI,
            HeavenlyStem.YI, EarthlyBranch.HAI,
            1950, 2000,
        )
        assertEquals2("戊午 乙卯 己巳 乙亥", bazi.fullBazi)
        val r = StudyCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        val dip = r.diploma
        assertTrue(dip.level, dip.level.contains("高学历"))
        assertTrue(dip.basis.contains("杀印相生") || dip.basis.contains("化杀"))
    }

    @Test
    fun `例十一 方小姐 己未甲戌辛酉癸巳 身旺食伤文昌当值 保送本科`() {
        val bazi = chartOf(
            HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.JIA, EarthlyBranch.XU,
            HeavenlyStem.XIN, EarthlyBranch.YOU,
            HeavenlyStem.GUI, EarthlyBranch.SI,
            1900, 2000,
        )
        assertEquals2("己未 甲戌 辛酉 癸巳", bazi.fullBazi)
        val r = StudyCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.FEMALE))
        val dip = r.diploma
        assertTrue(dip.level, dip.level.contains("高学历"))
        val xieXiu = dip.favorable.first { it.title.contains("食伤泄秀") }
        assertTrue(xieXiu.text.contains("文昌"))
    }

    @Test
    fun `例十二 某先生 壬寅癸卯甲子戊辰 原局食伤绝无运泄为救 专科`() {
        val bazi = chartOf(
            HeavenlyStem.REN, EarthlyBranch.YIN,
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.JIA, EarthlyBranch.ZI,
            HeavenlyStem.WU, EarthlyBranch.CHEN,
            1900, 2000,
        )
        assertEquals2("壬寅 癸卯 甲子 戊辰", bazi.fullBazi)
        val r = StudyCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        val dip = r.diploma
        assertTrue(dip.level, dip.level.contains("学历较高"))
        assertTrue(dip.basis.contains("为救") || dip.basis.contains("有救"))
    }

    @Test
    fun `例十三 某先生 己酉丙子甲戌丁卯 身弱用印逢财坏印 专科`() {
        val bazi = chartOf(
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.BING, EarthlyBranch.ZI,
            HeavenlyStem.JIA, EarthlyBranch.XU,
            HeavenlyStem.DING, EarthlyBranch.MAO,
            1900, 2000,
        )
        assertEquals2("己酉 丙子 甲戌 丁卯", bazi.fullBazi)
        val r = StudyCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        val dip = r.diploma
        // 印星为用本应较高，15岁甲戌运财星当值破印耗身——只考取专科
        assertTrue(dip.level, dip.level.contains("学历较高"))
        assertTrue(dip.basis.contains("损印") || dip.basis.contains("坏印") || dip.basis.contains("财"))
    }

    @Test
    fun `例十四 某先生 戊申壬戌庚申己卯 身强泄耗欠力亢旺 高中`() {
        val bazi = chartOf(
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.REN, EarthlyBranch.XU,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.JI, EarthlyBranch.MAO,
            1950, 2000,
        )
        assertEquals2("戊申 壬戌 庚申 己卯", bazi.fullBazi)
        val r = StudyCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        val dip = r.diploma
        // 原局身强而水木泄耗欠力，形成亢旺——高中时肝炎病后无心读书
        assertTrue(dip.level, dip.level.contains("学历不高"))
        assertTrue(dip.basis.contains("亢旺"))
    }

    @Test
    fun `例十五 某先生 甲寅甲戌甲午辛未 身弱用印运支被冲 初中肄业`() {
        val bazi = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.XU,
            HeavenlyStem.JIA, EarthlyBranch.WU,
            HeavenlyStem.XIN, EarthlyBranch.WEI,
            1900, 2000,
        )
        assertEquals2("甲寅 甲戌 甲午 辛未", bazi.fullBazi)
        val r = StudyCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        val dip = r.diploma
        // 寅午戌合火木生火火生土当令——身弱喜印忌财；丙子运子水无源无助又被午冲——用神不力，初中肄业
        assertTrue(dip.level, dip.level.contains("学历不高"))
        assertTrue(dip.basis.contains("冲克") || dip.basis.contains("用神不力"))
    }

    @Test
    fun `例十六 袁女士 戊申乙丑乙巳壬午 1988戊辰双体土克水 退学`() {
        val bazi = chartOf(
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.YI, EarthlyBranch.CHOU,
            HeavenlyStem.YI, EarthlyBranch.SI,
            HeavenlyStem.REN, EarthlyBranch.WU,
            1900, 2000,
        )
        assertEquals2("戊申 乙丑 乙巳 壬午", bazi.fullBazi)
        val r = StudyCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.FEMALE))
        val dip = r.diploma
        // 水主智慧，1988戊辰流年双体之土克水——岁运水被土杂，不利文途而退学
        assertTrue(dip.level, dip.level.contains("学历不高"))
        val bad = dip.badYears.firstOrNull { it.year == 1988 }
        assertNotNull(bad)
        assertTrue(bad!!.text.contains("不利文途") || bad.text.contains("退学"))
    }

    @Test
    fun `例十七 苏先生 癸丑丙辰癸未戊午 甲寅运1993癸酉和局 高考必中`() {
        val bazi = chartOf(
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.BING, EarthlyBranch.CHEN,
            HeavenlyStem.GUI, EarthlyBranch.WEI,
            HeavenlyStem.WU, EarthlyBranch.WU,
            1950, 2000,
        )
        assertEquals2("癸丑 丙辰 癸未 戊午", bazi.fullBazi)
        val r = StudyCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        val dip = r.diploma
        val good = dip.goodYears.firstOrNull { it.year == 1993 }
        assertNotNull(good)
        assertTrue(good!!.text.contains("高考") || good.text.contains("和局"))
    }

    @Test
    fun `例十八 张女士 甲午己巳己卯癸酉 丁卯运1973癸丑墓库收敛化战 保送`() {
        val bazi = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.WU,
            HeavenlyStem.JI, EarthlyBranch.SI,
            HeavenlyStem.JI, EarthlyBranch.MAO,
            HeavenlyStem.GUI, EarthlyBranch.YOU,
            1950, 2000,
        )
        assertEquals2("甲午 己巳 己卯 癸酉", bazi.fullBazi)
        val r = StudyCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.FEMALE))
        val dip = r.diploma
        // 卯酉冲战局，岁支丑为酉金墓库收敛化战为和——干有甲木泄化水，前途之喜
        val good = dip.goodYears.firstOrNull { it.year == 1973 }
        assertNotNull(good)
        assertTrue(good!!.text.contains("和局") || good.text.contains("化战"))
    }

    @Test
    fun `天赋例一 黄女士 己酉辛未戊戌丁巳 伤官佩印天赋+1987丁卯印年录取`() {
        val bazi = chartOf(
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.XIN, EarthlyBranch.WEI,
            HeavenlyStem.WU, EarthlyBranch.XU,
            HeavenlyStem.DING, EarthlyBranch.SI,
            1950, 2000,
        )
        assertEquals2("己酉 辛未 戊戌 丁巳", bazi.fullBazi)
        val r = StudyCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.FEMALE))
        val dip = r.diploma
        // 原局伤官佩印——读书天赋好；食伤为智商（文昌=食神），印为文凭/学校
        assertTrue(dip.talent.contains("伤官佩印"))
        // 1987丁卯：印星流年而原局伤官配合——印主学校，升学易录取好学校（本科）
        val y = dip.talentYears.firstOrNull { it.year == 1987 }
        assertNotNull(y)
        assertTrue(y!!.text.contains("录取") || y.text.contains("学校"))
    }

    @Test
    fun `天赋例二 王女士 戊戌癸亥甲寅丁卯 伤官无根不佩印+水旺智+1983癸亥利高考`() {
        val bazi = chartOf(
            HeavenlyStem.WU, EarthlyBranch.XU,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.DING, EarthlyBranch.MAO,
            1950, 2000,
        )
        assertEquals2("戊戌 癸亥 甲寅 丁卯", bazi.fullBazi)
        val r = StudyCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.FEMALE))
        val dip = r.diploma
        // 印星当令而伤官处死地无本气——不构成伤官佩印；水旺主智，智商不错
        assertTrue(dip.talent.contains("不构成伤官佩印"))
        assertTrue(dip.talent.contains("水旺") || dip.talent.contains("水主智"))
        // 辛酉运1983癸亥：水旺正印之年引动原局癸印——利高考（考上大学）
        val good = dip.goodYears.firstOrNull { it.year == 1983 }
        assertNotNull(good)
    }

    @Test
    fun `天赋例三 肖先生 壬子庚戌丙戌丙申 食神强旺+1991辛未食神心性主导`() {
        val bazi = chartOf(
            HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.BING, EarthlyBranch.XU,
            HeavenlyStem.BING, EarthlyBranch.SHEN,
            1950, 2000,
        )
        assertEquals2("壬子 庚戌 丙戌 丙申", bazi.fullBazi)
        val r = StudyCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        val dip = r.diploma
        // 原局食神特别强旺+文昌申在局——读书天赋相当不错
        assertTrue(dip.talent.contains("食神"))
        // 1991辛未：岁支伤官而原局食神强旺（戌2未1食神强于伤官）——食神心性主导，高考发挥优异（清华式）
        val y = dip.talentYears.firstOrNull { it.year == 1991 }
        assertNotNull(y)
        assertTrue(y!!.text.contains("食神心性") || y.text.contains("充分发挥"))
    }

    @Test
    fun `天赋例四 孙先生 乙巳甲申丁巳甲辰 时柱佩印+1983癸亥岁运生印佩印之年`() {
        val bazi = chartOf(
            HeavenlyStem.YI, EarthlyBranch.SI,
            HeavenlyStem.JIA, EarthlyBranch.SHEN,
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.JIA, EarthlyBranch.CHEN,
            1950, 2000,
        )
        assertEquals2("乙巳 甲申 丁巳 甲辰", bazi.fullBazi)
        val r = StudyCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        val dip = r.diploma
        // 时柱伤官佩印（辰伤官本气+甲印透）——读书天赋
        assertTrue(dip.talent.contains("伤官佩印"))
        // 1983癸亥（壬午运）：岁运壬癸水生甲木印而伤官可用——伤官佩印之年，考试发挥+被好学校录取（本科）
        val y = dip.talentYears.firstOrNull { it.year == 1983 }
        assertNotNull(y)
        assertTrue(y!!.text.contains("伤官佩印") || y.text.contains("录取"))
    }

    @Test
    fun `天赋例五 马先生 甲寅甲戌癸巳癸亥 水旺伤官众天资聪慧+1993癸酉印年通关录取`() {
        val bazi = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.JIA, EarthlyBranch.XU,
            HeavenlyStem.GUI, EarthlyBranch.SI,
            HeavenlyStem.GUI, EarthlyBranch.HAI,
            1950, 2000,
        )
        assertEquals2("甲寅 甲戌 癸巳 癸亥", bazi.fullBazi)
        val r = StudyCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        val dip = r.diploma
        // 水和伤官众多——天资聪慧，读书好料
        assertTrue(dip.talent.contains("天资聪慧") || dip.talent.contains("伤官强旺"))
        assertTrue(dip.talent.contains("水旺") || dip.talent.contains("水主智"))
        // 1993癸酉（丙子运）：印星值年而水通关金不克木，伤官得用——被大学录取
        val y = dip.talentYears.firstOrNull { it.year == 1993 }
        assertNotNull(y)
        assertTrue(y!!.text.contains("录取"))
    }

    @Test
    fun `天赋例六 王女士 辛亥丙申己丑戊辰 食神透干+1991辛未引动食伤最佳+1988比劫不利`() {
        val bazi = chartOf(
            HeavenlyStem.XIN, EarthlyBranch.HAI,
            HeavenlyStem.BING, EarthlyBranch.SHEN,
            HeavenlyStem.JI, EarthlyBranch.CHOU,
            HeavenlyStem.WU, EarthlyBranch.CHEN,
            1950, 2000,
        )
        assertEquals2("辛亥 丙申 己丑 戊辰", bazi.fullBazi)
        val r = StudyCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.FEMALE))
        val dip = r.diploma
        // 食神透干强旺，月干丙火弱合不住辛金——食神可发挥作用
        assertTrue(dip.talent.contains("食神透干"))
        // 1991辛未：流年干金引动原局食伤力量大增——考试最佳之年，有望考上大学（保送）
        val y91 = dip.talentYears.firstOrNull { it.year == 1991 }
        assertNotNull(y91)
        assertTrue(y91!!.text.contains("考上大学") || y91.text.contains("最佳"))
        // 1988戊辰：比劫过强缺乏灵活，对考试不利
        val y88 = dip.talentYears.firstOrNull { it.year == 1988 }
        assertNotNull(y88)
        assertTrue(y88!!.text.contains("缺乏灵活"))
    }

    @Test
    fun `天赋例七 段先生 戊申甲子戊午戊午 乙丑运合住子水又申入墓 小学毕业`() {
        val bazi = chartOf(
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.JIA, EarthlyBranch.ZI,
            HeavenlyStem.WU, EarthlyBranch.WU,
            HeavenlyStem.WU, EarthlyBranch.WU,
            1950, 2000,
        )
        assertEquals2("戊申 甲子 戊午 戊午", bazi.fullBazi)
        val r = StudyCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        val dip = r.diploma
        // 水当令有申金源头+申为食神——先天智商不错；乙丑运丑合住子水、又为申之墓——食神与智慧俱废
        assertTrue(dip.talent.contains("水旺") || dip.talent.contains("水主智") || dip.talent.contains("食神"))
        assertTrue(dip.level, dip.level.contains("学历不高"))
        assertTrue(dip.basis.contains("合住") || dip.basis.contains("入墓"))
    }

    @Test
    fun `文凭级别附时代提示`() {
        val bazi = chartOf(
            HeavenlyStem.DING, EarthlyBranch.WEI,
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.YI, EarthlyBranch.MAO,
            1950, 1980,
        )
        val r = StudyCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        assertTrue(r.diploma.basis.contains("时代"))
    }

    private fun assertEquals2(exp: String, act: String) = org.junit.Assert.assertEquals(exp, act)
}
