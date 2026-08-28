package cn.xulun.bazi

import cn.xulun.bazi.core.DaYunCalculator
import cn.xulun.bazi.core.LaoYuCalculator
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun assertEquals2(e: String, a: String) = assertEquals(e, a)

/**
 * 牢狱之灾引擎测试（教材第十四节，15 条信息+岁运应期）。
 * 例一边先生：干支皆水土相战无通关——丁丑三刑+官杀旺克身牢狱、甲戌小人、戊寅混战续灾。
 * 例二陈先生：伤官与正官紧贴相战无印制——黑道；壬午年伤官通根+壬丙冲战入狱。
 */
class LaoYuCalculatorTest {

    @Test
    fun `例一 边先生 戊戌己未壬子庚子 水土相战+丁丑三刑牢狱+甲戌小人+戊寅混战`() {
        val bazi = chartOf(
            HeavenlyStem.WU, EarthlyBranch.XU,
            HeavenlyStem.JI, EarthlyBranch.WEI,
            HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.GENG, EarthlyBranch.ZI,
            1900, 1970,
        )
        assertEquals("戊戌 己未 壬子 庚子", bazi.fullBazi)
        val r = LaoYuCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        assertTrue(r.baseRisks.any { it.dim.contains("相战") || it.dim.contains("战局") })
        val y97 = r.years.firstOrNull { it.year == 1997 }
        assertNotNull(y97)
        assertTrue(y97!!.text.contains("牢狱") || y97.text.contains("官司"))
        val y94 = r.years.firstOrNull { it.year == 1994 }
        assertNotNull(y94)
        assertTrue(y94!!.text.contains("小人") || y94.text.contains("陷害"))
        val y98 = r.years.firstOrNull { it.year == 1998 }
        assertNotNull(y98)
        assertTrue(y98!!.text.contains("混战") || y98.text.contains("灾"))
    }

    @Test
    fun `例十七 黄先生 丁巳壬子癸丑癸丑 通关酉被丙戌刑害战局重现入狱`() {
        val bazi = chartOf(
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.REN, EarthlyBranch.ZI,
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            1950, 2000,
        )
        assertEquals2("丁巳 壬子 癸丑 癸丑", bazi.fullBazi)
        val r = LaoYuCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        // 原局水火土水交战+七杀无制——官非牢狱信息
        assertTrue(r.baseRisks.any { it.dim.contains("相战") })
        // 己酉运丙戌年：酉戌害+丑戌刑——通关之金被毁，战局重现——入狱判4年
        val y06 = r.years.firstOrNull { it.year == 2006 }
        assertNotNull(y06)
        assertTrue(y06!!.text.contains("牢狱") || y06.text.contains("入狱"))
    }

    @Test
    fun `例十五 乾造 丁酉辛亥己亥丁卯 两枭夺食杀枭同柱+乙卯通关拘留轻灾`() {
        val bazi = chartOf(
            HeavenlyStem.DING, EarthlyBranch.YOU,
            HeavenlyStem.XIN, EarthlyBranch.HAI,
            HeavenlyStem.JI, EarthlyBranch.HAI,
            HeavenlyStem.DING, EarthlyBranch.MAO,
            1950, 2000,
        )
        assertEquals2("丁酉 辛亥 己亥 丁卯", bazi.fullBazi)
        val r = LaoYuCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        // 年时双枭挂两头+月上食神——两枭夺食；杀枭同柱——喜踏偏门打擦边球
        assertTrue(r.baseRisks.any { it.dim.contains("两枭夺食") || it.dim.contains("杀枭同柱") })
        // 1975乙卯：两卯冲两酉幸有亥水通关——只拘留15天而释放
        val y75 = r.years.firstOrNull { it.year == 1975 }
        assertNotNull(y75)
        assertTrue(y75!!.text.contains("拘留") || y75.text.contains("释放"))
    }

    @Test
    fun `例十六 乾造 甲申甲戌丙寅己丑 三枭双伤夹日+丑戌刑子卯刑聚入狱`() {
        val bazi = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.SHEN,
            HeavenlyStem.JIA, EarthlyBranch.XU,
            HeavenlyStem.BING, EarthlyBranch.YIN,
            HeavenlyStem.JI, EarthlyBranch.CHOU,
            1900, 1950,
        )
        assertEquals2("甲申 甲戌 丙寅 己丑", bazi.fullBazi)
        val r = LaoYuCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        // 三枭双伤夹日干——牢狱信息明显
        assertTrue(r.baseRisks.any { it.dim.contains("夹日") })
        // 己卯运丙子年：丑戌刑+子卯刑——刑聚入狱
        val y96 = r.years.firstOrNull { it.year == 1996 }
        assertNotNull(y96)
        assertTrue(y96!!.text.contains("入狱"))
    }

    @Test
    fun `例十四 乾造 壬辰癸卯乙丑丙子 身旺印旺财弱+己卯禄会贪污入狱`() {
        val bazi = chartOf(
            HeavenlyStem.REN, EarthlyBranch.CHEN,
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.YI, EarthlyBranch.CHOU,
            HeavenlyStem.BING, EarthlyBranch.ZI,
            1950, 2000,
        )
        assertEquals2("壬辰 癸卯 乙丑 丙子", bazi.fullBazi)
        val r = LaoYuCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        // 身旺印旺财弱——见财起意见利忘义；枭印为忌阴险狡诈
        assertTrue(r.baseRisks.any { it.dim.contains("身旺印旺财弱") })
        // 1999己卯：禄会之年财星轮值财印相战——因伙同贪污而入狱
        val y99 = r.years.firstOrNull { it.year == 1999 }
        assertNotNull(y99)
        assertTrue(y99!!.text.contains("贪污") || y99.text.contains("入狱"))
    }

    @Test
    fun `例十三 乾造 壬辰戊申癸巳甲寅 伤官越印攻官入狱+印还原获救`() {
        val bazi = chartOf(
            HeavenlyStem.REN, EarthlyBranch.CHEN,
            HeavenlyStem.WU, EarthlyBranch.SHEN,
            HeavenlyStem.GUI, EarthlyBranch.SI,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            1950, 2000,
        )
        assertEquals2("壬辰 戊申 癸巳 甲寅", bazi.fullBazi)
        val r = LaoYuCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        // 寅巳申三刑入命+伤官双体官透身弱——伤官见官定有官灾
        assertTrue(r.baseRisks.any { it.dim.contains("伤官见官·身弱") || it.dim.contains("官灾") })
        // 1998戊寅：两寅冲申印被制，伤官越印攻官——牢狱灾难免
        val y98 = r.years.firstOrNull { it.year == 1998 }
        assertNotNull(y98)
        assertTrue(y98!!.text.contains("牢狱"))
        // 2004甲申：印星还原——可获救出狱
        val y04 = r.years.firstOrNull { it.year == 2004 }
        assertNotNull(y04)
        assertTrue(y04!!.text.contains("获救") || y04.text.contains("出狱"))
    }

    @Test
    fun `例十二 乾造 癸丑甲寅己卯戊辰 三会杀局受牵连入牢+甲戌天合地合出狱`() {
        val bazi = chartOf(
            HeavenlyStem.GUI, EarthlyBranch.CHOU,
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.JI, EarthlyBranch.MAO,
            HeavenlyStem.WU, EarthlyBranch.CHEN,
            1950, 2000,
        )
        assertEquals2("癸丑 甲寅 己卯 戊辰", bazi.fullBazi)
        val r = LaoYuCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        // 三会杀局攻身——不会故意犯罪，最易受牵连/栽赃诬陷
        assertTrue(r.baseRisks.any { it.dim.contains("杀局") && it.text.contains("牵连") })
        // 1991辛未：冲丑根引动+财滋杀攻身——受朋友牵连入牢
        val y91 = r.years.firstOrNull { it.year == 1991 }
        assertNotNull(y91)
        assertTrue(y91!!.text.contains("牵连") || y91.text.contains("入牢"))
        // 1994甲戌：与日柱天合地合绊住卯木——出狱重见天日
        val y94 = r.years.firstOrNull { it.year == 1994 }
        assertNotNull(y94)
        assertTrue(y94!!.text.contains("出狱") || y94.text.contains("重见天日"))
    }

    @Test
    fun `例十一 乾造 甲寅乙亥丁巳丙午 众劫侮官+丁丑岁运并临湿土晦火入狱`() {
        val bazi = chartOf(
            HeavenlyStem.JIA, EarthlyBranch.YIN,
            HeavenlyStem.YI, EarthlyBranch.HAI,
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.BING, EarthlyBranch.WU,
            1950, 2000,
        )
        assertEquals2("甲寅 乙亥 丁巳 丙午", bazi.fullBazi)
        val r = LaoYuCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        // 巳亥冲+众劫侮官——藐视法度；羊刃枭神强旺——急躁+阴险易入黑道
        assertTrue(r.baseRisks.any { it.dim.contains("侮官") || it.dim.contains("羊刃枭神") })
        // 丁丑运丁丑年：岁运并临为忌破格+湿土晦火——伙同抢劫三百余元判12年
        val y97 = r.years.firstOrNull { it.year == 1997 }
        assertNotNull(y97)
        assertTrue(y97!!.text.contains("牢狱"))
    }

    @Test
    fun `例十 李先生 乙未丙戌己酉己巳 身旺无制+甲申运丁巳双刃财色入狱`() {
        val bazi = chartOf(
            HeavenlyStem.YI, EarthlyBranch.WEI,
            HeavenlyStem.BING, EarthlyBranch.XU,
            HeavenlyStem.JI, EarthlyBranch.YOU,
            HeavenlyStem.JI, EarthlyBranch.SI,
            1950, 2000,
        )
        assertEquals2("乙未 丙戌 己酉 己巳", bazi.fullBazi)
        val r = LaoYuCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        // 比劫结党身旺无制+燥气结党脆金——牢狱之基
        assertTrue(r.baseRisks.any { it.dim.contains("身旺无制") || it.dim.contains("燥气") })
        // 甲申运丁巳：官杀混杂身旺欺官+伤官当值+双巳双刃争合——因财色犯罪入狱
        val y77 = r.years.firstOrNull { it.year == 1977 }
        assertNotNull(y77)
        assertTrue(y77!!.text.contains("财色") || y77.text.contains("入狱"))
    }

    @Test
    fun `例九 乾造 丁亥甲辰丁巳庚戌 罗网交加双伤夹日+辛未冲根刑聚被捕`() {
        val bazi = chartOf(
            HeavenlyStem.DING, EarthlyBranch.HAI,
            HeavenlyStem.JIA, EarthlyBranch.CHEN,
            HeavenlyStem.DING, EarthlyBranch.SI,
            HeavenlyStem.GENG, EarthlyBranch.XU,
            1900, 1950,
        )
        assertEquals2("丁亥 甲辰 丁巳 庚戌", bazi.fullBazi)
        val r = LaoYuCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        // 辰戌罗网交加且为忌+双伤夹日柱土旺木折——牢狱信息明显
        assertTrue(r.baseRisks.any { it.dim.contains("罗网") || it.dim.contains("双伤") })
        // 己亥运1991辛未：戌未相刑+两亥冲巳冲去日主之根+泄身过重——壬辰月被捕判3年
        val y91 = r.years.firstOrNull { it.year == 1991 }
        assertNotNull(y91)
        assertTrue(y91!!.text.contains("被捕") || y91.text.contains("入狱"))
    }

    @Test
    fun `例八 乾造 丙午乙未丁丑辛亥 枭食同柱+壬戌刑开墓门抢劫入狱`() {
        val bazi = chartOf(
            HeavenlyStem.BING, EarthlyBranch.WU,
            HeavenlyStem.YI, EarthlyBranch.WEI,
            HeavenlyStem.DING, EarthlyBranch.CHOU,
            HeavenlyStem.XIN, EarthlyBranch.HAI,
            1950, 2000,
        )
        assertEquals2("丙午 乙未 丁丑 辛亥", bazi.fullBazi)
        val r = LaoYuCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        // 枭食同柱有官非信息；食多变伤得令——自恃聪明胆大冒险喜与官斗
        assertTrue(r.baseRisks.any { it.dim.contains("枭食") || it.dim.contains("官非") })
        // 1982壬戌：官星透干+丑未戌三刑刑开日干火墓之门+伤官见官——因抢劫入狱判两年
        val y82 = r.years.firstOrNull { it.year == 1982 }
        assertNotNull(y82)
        assertTrue(y82!!.text.contains("入狱"))
    }

    @Test
    fun `例七 乾造 辛巳丙申庚戌丁丑 官杀混杂+局无财众劫抢财戊寅判无期`() {
        val bazi = chartOf(
            HeavenlyStem.XIN, EarthlyBranch.SI,
            HeavenlyStem.BING, EarthlyBranch.SHEN,
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.DING, EarthlyBranch.CHOU,
            1900, 1950,
        )
        assertEquals2("辛巳 丙申 庚戌 丁丑", bazi.fullBazi)
        val r = LaoYuCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        // 天干官杀混杂易有官灾+日干墓库在局+局无财比劫旺——财现众劫抢财
        assertTrue(r.baseRisks.any { it.dim.contains("官杀混杂") })
        assertTrue(r.baseRisks.any { it.dim.contains("无财") || it.dim.contains("众劫") || it.dim.contains("墓库") })
        // 辛卯运戊寅年：岁支寅财值岁+寅巳申三刑——群凶劫财引发战局，因财判无期
        val y98 = r.years.firstOrNull { it.year == 1998 }
        assertNotNull(y98)
        assertTrue(y98!!.text.contains("官灾") || y98.text.contains("重判"))
    }

    @Test
    fun `例五 杨先生 癸卯庚申乙酉丙子 官杀混杂伤官见官+庚辰岁运自刑入狱`() {
        val bazi = chartOf(
            HeavenlyStem.GUI, EarthlyBranch.MAO,
            HeavenlyStem.GENG, EarthlyBranch.SHEN,
            HeavenlyStem.YI, EarthlyBranch.YOU,
            HeavenlyStem.BING, EarthlyBranch.ZI,
            1950, 2000,
        )
        assertEquals2("癸卯 庚申 乙酉 丙子", bazi.fullBazi)
        val r = LaoYuCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        // 月日支官杀混杂+月时干伤官见官+官杀旺制身——原局牢狱信息
        assertTrue(r.baseRisks.any { it.dim.contains("混杂") || it.dim.contains("伤官见官") })
        // 丙辰运庚辰年：岁运干伤官见官+辰辰自刑加大官杀混杂克身过重——入狱
        val y00 = r.years.firstOrNull { it.year == 2000 }
        assertNotNull(y00)
        assertTrue(y00!!.text.contains("入狱") || y00.text.contains("牢狱"))
    }

    @Test
    fun `例二 陈先生 庚戌丙戌乙酉丁亥 伤官见官黑道+壬午冲战入狱`() {
        val bazi = chartOf(
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.BING, EarthlyBranch.XU,
            HeavenlyStem.YI, EarthlyBranch.YOU,
            HeavenlyStem.DING, EarthlyBranch.HAI,
            1900, 1970,
        )
        assertEquals("庚戌 丙戌 乙酉 丁亥", bazi.fullBazi)
        val r = LaoYuCalculator.analyze(bazi, DaYunCalculator.compute(bazi, Gender.MALE))
        assertTrue(r.baseRisks.any { it.text.contains("无视法纪") || it.text.contains("黑道") })

        // 例三 杜先生 庚戌丙戌甲申丙寅：日支七杀贴身——庚寅运甲申乙酉金年引发杀旺克身，2004/2005连续官司
        val bazi3 = chartOf(
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.BING, EarthlyBranch.XU,
            HeavenlyStem.JIA, EarthlyBranch.SHEN,
            HeavenlyStem.BING, EarthlyBranch.YIN,
            1900, 1970,
        )
        assertEquals2("庚戌 丙戌 甲申 丙寅", bazi3.fullBazi)
        val r3 = LaoYuCalculator.analyze(bazi3, DaYunCalculator.compute(bazi3, Gender.MALE))
        val y04 = r3.years.firstOrNull { it.year == 2004 }
        assertNotNull(y04)
        assertTrue(y04!!.text.contains("官司") || y04.text.contains("麻烦") || y04.text.contains("小人"))

        // 例四 程先生 庚戌己卯戊子壬戌：偏财贴身+子卯刑+透食神——2000庚辰食神生偏财+辰冲戌+
        // 卯辰害+比劫亢旺克财——盗用商标坐牢
        val bazi4 = chartOf(
            HeavenlyStem.GENG, EarthlyBranch.XU,
            HeavenlyStem.JI, EarthlyBranch.MAO,
            HeavenlyStem.WU, EarthlyBranch.ZI,
            HeavenlyStem.REN, EarthlyBranch.XU,
            1950, 2000,
        )
        assertEquals2("庚戌 己卯 戊子 壬戌", bazi4.fullBazi)
        val r4 = LaoYuCalculator.analyze(bazi4, DaYunCalculator.compute(bazi4, Gender.MALE))
        assertTrue(r4.baseRisks.any { it.text.contains("铤而走险") })
        val y00 = r4.years.firstOrNull { it.year == 2000 }
        assertNotNull(y00)
        assertTrue(y00!!.text.contains("牢狱"))
        val y02 = r.years.firstOrNull { it.year == 2002 }
        assertNotNull(y02)
        assertTrue(y02!!.text.contains("牢狱") || y02.text.contains("制裁"))
    }
}
