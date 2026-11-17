package cn.xulun.bazi

import cn.xulun.bazi.core.BaziCalculator
import cn.xulun.bazi.core.DaYunCalculator
import cn.xulun.bazi.core.JinBiaoCalculator
import cn.xulun.bazi.core.WenPingCalculator
import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DaYun
import cn.xulun.bazi.model.DaYunInfo
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.JinBiaoQuYongWay
import cn.xulun.bazi.model.JinBiaoQingZhuo
import cn.xulun.bazi.model.JinBiaoHealth
import cn.xulun.bazi.model.JinBiaoRuoJiType
import cn.xulun.bazi.model.JinBiaoStance
import cn.xulun.bazi.model.JinBiaoVerdict
import cn.xulun.bazi.model.JinBiaoWangJiType
import cn.xulun.bazi.model.Pillar
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 盲派金镖门 · 案例锚定库（.jinbiao-reference.md 第一～三讲）。
 *
 * 双引擎铁律：带有事实的案例必须同时以老引擎分析对照——
 * 金镖门引擎（JinBiaoCalculator）独立断盘，老引擎（WenPing/HunYin 等）锚定事实，
 * 两盘并列展示于 UI（金镖门模块在前，老引擎分区在后）。
 */
class JinBiaoAnLiTest {

    private fun chart(y: Int, m: Int, d: Int, h: Int) =
        BaziCalculator.calculate(LocalDateTime.of(y, m, d, h, 0))

    /** 合成盘：己未 丁未 癸未 戊午（癸水孤立无气，满盘财官）。 */
    private fun syntheticRuoJi(): BaziResult = BaziResult(
        yearPillar = Pillar(HeavenlyStem.JI, EarthlyBranch.WEI),
        monthPillar = Pillar(HeavenlyStem.DING, EarthlyBranch.WEI),
        dayPillar = Pillar(HeavenlyStem.GUI, EarthlyBranch.WEI),
        hourPillar = Pillar(HeavenlyStem.WU, EarthlyBranch.WU),
        birthDateTime = LocalDateTime.of(1982, 7, 1, 12, 0),
    )

    /** 合成盘·旺极：甲木四柱全比劫印枭，无一点克耗泄。[yinZhong] 印重（子×2）/ 印轻（子×1）。 */
    private fun syntheticWangJi(yinZhong: Boolean): BaziResult =
        if (yinZhong) {
            BaziResult(
                yearPillar = Pillar(HeavenlyStem.JIA, EarthlyBranch.ZI),
                monthPillar = Pillar(HeavenlyStem.YI, EarthlyBranch.MAO),
                dayPillar = Pillar(HeavenlyStem.JIA, EarthlyBranch.ZI),
                hourPillar = Pillar(HeavenlyStem.YI, EarthlyBranch.MAO),
                birthDateTime = LocalDateTime.of(1984, 3, 1, 12, 0),
            )
        } else {
            BaziResult(
                yearPillar = Pillar(HeavenlyStem.YI, EarthlyBranch.MAO),
                monthPillar = Pillar(HeavenlyStem.YI, EarthlyBranch.MAO),
                dayPillar = Pillar(HeavenlyStem.JIA, EarthlyBranch.ZI),
                hourPillar = Pillar(HeavenlyStem.YI, EarthlyBranch.MAO),
                birthDateTime = LocalDateTime.of(1984, 3, 1, 12, 0),
            )
        }

    // ── 第一讲 · 立场法：五行五关系 ──

    @Test
    fun `立场法 以用神为中心 五行五关系全覆盖`() {
        // 用神=木：同木帮、水生帮；木生火泄、木克土耗、金克木克——皆损
        assertEquals(JinBiaoStance.BANG to "同用神", JinBiaoCalculator.stanceAction(Element.WOOD, Element.WOOD))
        assertEquals(JinBiaoStance.BANG to "生用神", JinBiaoCalculator.stanceAction(Element.WOOD, Element.WATER))
        assertEquals(JinBiaoStance.SUN to "泄用神", JinBiaoCalculator.stanceAction(Element.WOOD, Element.FIRE))
        assertEquals(JinBiaoStance.SUN to "耗用神", JinBiaoCalculator.stanceAction(Element.WOOD, Element.EARTH))
        assertEquals(JinBiaoStance.SUN to "克用神", JinBiaoCalculator.stanceAction(Element.WOOD, Element.METAL))
    }

    // ── 第三讲 · 弱极从势（权威定论 100% 应验） ──

    @Test
    fun `弱极型一 孤立无气 官旺随官 用神取官杀`() {
        val r = JinBiaoCalculator.analyze(syntheticRuoJi(), null)
        assertTrue("印比全无（一点根都没有，全是克耗泄）——弱极型一", r.ruoJi && r.ruoJiType == JinBiaoRuoJiType.GU_LI)
        assertTrue("官党独大——官旺则要随官", r.quYongWay == JinBiaoQuYongWay.SUI_GUAN)
        assertEquals("用神五行=官杀之土", Element.EARTH, r.yongShenElement)
        assertTrue("己土透年干——七杀为用神代称", r.yongShenGod.chinese == "七杀")
        assertTrue("断语须含弱极从势与随官权威断", r.duanYu.contains("弱极") && r.duanYu.contains("随官"))
    }

    @Test
    fun `弱极从官 行比劫印运必大凶 行财官运必发达`() {
        val daYun = DaYunInfo(
            forward = true,
            qiYunText = "合成",
            startDate = LocalDateTime.of(1982, 7, 1, 0, 0),
            yuns = listOf(
                DaYun(Pillar(HeavenlyStem.GENG, EarthlyBranch.SHEN), 4, 1982, emptyList()),   // 庚申纯印
                DaYun(Pillar(HeavenlyStem.REN, EarthlyBranch.ZI), 14, 1992, emptyList()),     // 壬子纯比劫
                DaYun(Pillar(HeavenlyStem.BING, EarthlyBranch.CHEN), 24, 2002, emptyList()),  // 丙辰财官
            ),
        )
        val r = JinBiaoCalculator.analyze(syntheticRuoJi(), daYun)
        val yinYun = r.yuns[0]
        val biYun = r.yuns[1]
        val caiGuanYun = r.yuns[2]
        assertTrue("庚申印运——弱极行印地必凶无疑", yinYun.verdict == JinBiaoVerdict.XIONG)
        assertTrue("壬子比劫运——弱极行比劫地必凶无疑", biYun.verdict == JinBiaoVerdict.XIONG)
        assertTrue("印/比劫运标注大凶级别（勿看流年）",
            (yinYun.items + biYun.items).all { it.action.contains("必大凶") })
        assertTrue("丙辰财官运——随官之命走财官运必发达", caiGuanYun.verdict == JinBiaoVerdict.JI)
        assertTrue("大凶断语：轻则破大财重则牢狱死亡", r.duanYu.contains("破大财"))
    }

    // ── 第三讲续 · 旺极专旺 ──

    @Test
    fun `旺极 比劫当势随旺神 用神取比劫 好坏取决于大运`() {
        val r = JinBiaoCalculator.analyze(syntheticWangJi(yinZhong = true), null)
        assertTrue("四柱全比劫、无一点克耗泄——旺极", r.wangJi && r.wangJiType == JinBiaoWangJiType.BI_DANG_SHI)
        assertTrue("必然随其旺神——用神取比劫", r.quYongWay == JinBiaoQuYongWay.SUI_WANG)
        assertEquals("用神五行=日主本行之木", Element.WOOD, r.yongShenElement)
        assertTrue("年干甲透——比肩为用神代称", r.yongShenGod.chinese == "比肩")
        assertTrue("断语含旺极运断铁律", r.duanYu.contains("富贵自然显达") && r.duanYu.contains("九死一生"))
    }

    @Test
    fun `旺极运断 比印富贵显达 官杀凶祸即至 财地九死一生`() {
        val daYun = DaYunInfo(
            forward = true,
            qiYunText = "合成",
            startDate = LocalDateTime.of(1988, 3, 1, 0, 0),
            yuns = listOf(
                DaYun(Pillar(HeavenlyStem.REN, EarthlyBranch.ZI), 4, 1988, emptyList()),    // 壬子纯印
                DaYun(Pillar(HeavenlyStem.JIA, EarthlyBranch.YIN), 14, 1998, emptyList()),  // 甲寅比劫
                DaYun(Pillar(HeavenlyStem.GENG, EarthlyBranch.SHEN), 24, 2008, emptyList()), // 庚申官杀
                DaYun(Pillar(HeavenlyStem.WU, EarthlyBranch.CHEN), 34, 2018, emptyList()),   // 戊辰财
            ),
        )
        val r = JinBiaoCalculator.analyze(syntheticWangJi(yinZhong = true), daYun)
        assertTrue("壬子印运——运行比劫印绶富贵自然显达", r.yuns[0].verdict == JinBiaoVerdict.JI)
        assertTrue("甲寅比劫运——同旺神亦吉", r.yuns[1].verdict == JinBiaoVerdict.JI)
        assertTrue("庚申官运——运行官杀犯旺必然大凶", r.yuns[2].verdict == JinBiaoVerdict.XIONG)
        assertTrue("官杀运标注犯旺", r.yuns[2].items.any { it.action.contains("犯旺") })
        assertTrue("戊辰财运——立断九死一生", r.yuns[3].verdict == JinBiaoVerdict.XIONG)
        assertTrue("财运标注九死一生", r.yuns[3].items.all { it.action.contains("九死一生") })
    }

    @Test
    fun `旺极食伤运 印重夺食不可行 印轻顺泄也是好运`() {
        val daYun = DaYunInfo(
            forward = true,
            qiYunText = "合成",
            startDate = LocalDateTime.of(1988, 3, 1, 0, 0),
            yuns = listOf(
                DaYun(Pillar(HeavenlyStem.DING, EarthlyBranch.SI), 4, 1988, emptyList()), // 丁巳纯食伤
            ),
        )
        val yinZhong = JinBiaoCalculator.analyze(syntheticWangJi(yinZhong = true), daYun)
        val yinQing = JinBiaoCalculator.analyze(syntheticWangJi(yinZhong = false), daYun)
        assertTrue("局中印重——走食伤运印夺食，不可行（凶）", yinZhong.yuns[0].verdict == JinBiaoVerdict.XIONG)
        assertTrue("局中印轻——食伤顺泄而行也是好运（吉）", yinQing.yuns[0].verdict == JinBiaoVerdict.JI)
        assertTrue("印轻食伤运标注顺泄可行", yinQing.yuns[0].items.all { it.action.contains("泄") })
    }

    /** 合成盘·强极：[deJu] 印星得局（壬子 癸亥 甲子 壬子——剩下一两个比劫全是印）/ 未得局（甲子 乙卯 甲子 甲子——印比相当）。 */
    private fun syntheticQiangJi(deJu: Boolean): BaziResult =
        if (deJu) {
            BaziResult(
                yearPillar = Pillar(HeavenlyStem.REN, EarthlyBranch.ZI),
                monthPillar = Pillar(HeavenlyStem.GUI, EarthlyBranch.HAI),
                dayPillar = Pillar(HeavenlyStem.JIA, EarthlyBranch.ZI),
                hourPillar = Pillar(HeavenlyStem.REN, EarthlyBranch.ZI),
                birthDateTime = LocalDateTime.of(1984, 3, 1, 12, 0),
            )
        } else {
            BaziResult(
                yearPillar = Pillar(HeavenlyStem.JIA, EarthlyBranch.ZI),
                monthPillar = Pillar(HeavenlyStem.YI, EarthlyBranch.MAO),
                dayPillar = Pillar(HeavenlyStem.JIA, EarthlyBranch.ZI),
                hourPillar = Pillar(HeavenlyStem.JIA, EarthlyBranch.ZI),
                birthDateTime = LocalDateTime.of(1984, 3, 1, 12, 0),
            )
        }

    @Test
    fun `强极 印比同心 印比运定可亨通 印星得局化煞生印 财地触怒强神`() {
        val daYun = DaYunInfo(
            forward = true,
            qiYunText = "合成",
            startDate = LocalDateTime.of(1988, 3, 1, 0, 0),
            yuns = listOf(
                DaYun(Pillar(HeavenlyStem.REN, EarthlyBranch.ZI), 4, 1988, emptyList()),    // 壬子纯印
                DaYun(Pillar(HeavenlyStem.GENG, EarthlyBranch.SHEN), 14, 1998, emptyList()), // 庚申纯官杀
                DaYun(Pillar(HeavenlyStem.WU, EarthlyBranch.CHEN), 24, 2008, emptyList()),   // 戊辰纯财
            ),
        )
        val r = JinBiaoCalculator.analyze(syntheticQiangJi(deJu = true), daYun)
        assertTrue("命局印比叠叠、绝无财官食伤——强极印比同心", r.wangJi && r.wangJiType == JinBiaoWangJiType.YIN_BI_TONG_XIN)
        assertTrue("随其旺神——用神取比劫", r.quYongWay == JinBiaoQuYongWay.SUI_WANG)
        assertTrue("运行印比——人生定可亨通显达", r.yuns[0].verdict == JinBiaoVerdict.JI)
        assertTrue("庚干直克身——犯旺激怒（命一：官干无合化之路，虽得局亦不可触犯）", r.yuns[1].verdict == JinBiaoVerdict.XIONG)
        assertTrue("申支无合局——化煞生印之注仍存", r.yuns[1].items.any { it.action.contains("化煞生印") })
        assertTrue("行财地——财克印犯印之旺，大凶", r.yuns[2].verdict == JinBiaoVerdict.XIONG)
        assertTrue("财运标注犯印之旺出大灾", r.yuns[2].items.all { it.action.contains("犯印之旺") })
    }

    @Test
    fun `强极印未得局 官杀运无印可化仍大凶`() {
        val daYun = DaYunInfo(
            forward = true,
            qiYunText = "合成",
            startDate = LocalDateTime.of(1988, 3, 1, 0, 0),
            yuns = listOf(
                DaYun(Pillar(HeavenlyStem.GENG, EarthlyBranch.SHEN), 4, 1988, emptyList()), // 庚申纯官杀
            ),
        )
        val r = JinBiaoCalculator.analyze(syntheticQiangJi(deJu = false), daYun)
        assertTrue(r.wangJi && r.wangJiType == JinBiaoWangJiType.YIN_BI_TONG_XIN)
        assertTrue("印未得局——官杀运无印可化，仍大凶", r.yuns[0].verdict == JinBiaoVerdict.XIONG)
        assertTrue("断语标注印未得局走官运必大凶", r.duanYu.contains("印未得局") && r.duanYu.contains("必大凶"))
    }

    // ── 案一 · 坤造 壬戌 戊申 丁亥 庚戌（1982-09-01 戌时）——事实：未活过南方火运 ──

    @Test
    fun `案一 坤 壬戌戊申丁亥庚戌 弱极微浮印比 财解围 南方火运必大凶去世`() {
        val bazi = chart(1982, 9, 1, 19)
        assertTrue(bazi.fullBazi.startsWith("壬戌 戊申 丁亥 庚戌"))
        val daYun = DaYunCalculator.compute(bazi, Gender.FEMALE)

        // 大运与师传表一致：逆行 丁未(1990) 丙午 乙巳 甲辰 癸卯 壬寅 辛丑 庚子
        assertTrue(
            "逆行南方运与师传大运表一致",
            daYun.yuns.take(4).map { "${it.pillar.fullName}(${it.startYear})" } ==
                listOf("丁未(1990)", "丙午(2000)", "乙巳(2010)", "甲辰(2020)"),
        )

        // 金镖门：秋金肃杀、戌申西方得局、地支金水无一丝木火——弱极微浮印比（戌中丁借根无用）
        val jb = JinBiaoCalculator.analyze(bazi, daYun)
        assertTrue("弱极·微浮印比自顾不暇", jb.ruoJi && jb.ruoJiType == JinBiaoRuoJiType.WEI_FU)
        assertTrue("判据含藏根叛向（戌中丁火随秋金走）", jb.ruoJiBasis!!.contains("借根无用"))
        assertTrue("财官食并存——必要财运解围，用神取财（金）",
            jb.quYongWay == JinBiaoQuYongWay.CAI_JIE_WEI && jb.yongShenElement == Element.METAL)

        // 事实锚定：上来三步南方火运（丁未丙午乙巳）必凶无疑——命主在南方运中去世，没有活过南方运
        val south = jb.yuns.take(3)
        assertTrue("南方三步火运皆大凶（勿看流年）", south.all { it.verdict == JinBiaoVerdict.XIONG })
        assertTrue("比劫印地标注必大凶", south.flatMap { it.items }.any { it.action.contains("必大凶") })
        assertTrue("断语含大灾级别（破大财/牢狱/死亡）", jb.duanYu.contains("牢狱、死亡"))

        // 老引擎对照（双引擎铁律）：正财格成格、身弱——老引擎按身弱喜扶，金镖门判弱极帮之反害，
        // 事实（南方火运去世）站在金镖门口径——两引擎分歧点即此案教学点。
        val geju = cn.xulun.bazi.core.GeJuCalculator.determine(bazi)
        assertTrue("老引擎定正财格且身弱", geju.name == "正财格" && geju.strength == DayStrength.WEAK)
    }

    // ── 案二 · 乾造 癸卯 丙辰 庚寅 丁亥（1963-04-17 亥时）——事实：卯寅财运中考上大学 ──

    @Test
    fun `案二 乾 癸卯丙辰庚寅丁亥 三会叛局弱极 财解围 卯寅两运好运考上大学 帮身必凶`() {
        val bazi = chart(1963, 4, 17, 21)
        assertTrue(bazi.fullBazi.startsWith("癸卯 丙辰 庚寅 丁亥"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "逆行大运与师传表一致",
            daYun.yuns.take(4).map { "${it.pillar.fullName}(${it.startYear})" } ==
                listOf("乙卯(1967)", "甲寅(1977)", "癸丑(1987)", "壬子(1997)"),
        )

        // 金镖门：春天寅卯辰三会木局——辰印跟着木走等于没有，一点帮扶都没有
        val jb = JinBiaoCalculator.analyze(bazi, daYun)
        assertTrue("弱极·孤立无气（三会叛局）", jb.ruoJi && jb.ruoJiType == JinBiaoRuoJiType.GU_LI)
        assertTrue("判据含三会叛向与财坏印反好", jb.ruoJiBasis!!.contains("三会") && jb.ruoJiBasis.contains("财坏印"))
        assertTrue("财官食并存、伤官制杀——财和解食伤与官杀，用神取财（木）",
            jb.quYongWay == JinBiaoQuYongWay.CAI_JIE_WEI && jb.yongShenElement == Element.WOOD)
        assertTrue("取用依据含通关机理（气静和平）", jb.quYongBasis.contains("气静和平"))

        // 事实锚定：第一步乙卯、第二步甲寅走财运没帮身——自然是好运（那个年代考上大学）
        assertTrue("卯寅两运必好运", jb.yuns[0].verdict == JinBiaoVerdict.JI && jb.yuns[1].verdict == JinBiaoVerdict.JI)

        // 帮之反害：癸丑（丑印地）、庚戌（比劫）皆大凶；辛亥运辛金无根全生亥水——干化于支以食论（案三补丁），中平
        assertTrue("走帮身必大凶（癸丑丑印地、庚戌比劫）", listOf(2, 5).all { jb.yuns[it].verdict == JinBiaoVerdict.XIONG })
        assertTrue("辛亥运干化于支——以食伤论中平", jb.yuns[4].verdict == JinBiaoVerdict.PING &&
            jb.yuns[4].items.any { it.action.contains("干化于支") })

        // 老引擎对照（双引擎铁律）
        val geju = cn.xulun.bazi.core.GeJuCalculator.determine(bazi)
        assertTrue("老引擎定伤官格且身弱", geju.name == "伤官格" && geju.strength == DayStrength.WEAK)
    }

    // ── 案三 · 乾造 丙午 戊戌 戊午 丁巳（1966-10-26 巳时）——事实：金水运贫寒不死、寅运合印局大发 ──

    @Test
    fun `案三 乾 丙午戊戌戊午丁巳 强极印星得局 藏根作废 财地软档不死 寅卯运大发`() {
        val bazi = chart(1966, 10, 26, 9)
        assertTrue(bazi.fullBazi.startsWith("丙午 戊戌 戊午 丁巳"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运与师传表一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } ==
                listOf("己亥(1971)", "庚子(1981)", "辛丑(1991)"),
        )

        // 金镖门：印比之支所藏财官食（戌中辛、巳中庚）跟着走作废——强极；印旺于比——印星得局
        val jb = JinBiaoCalculator.analyze(bazi, daYun)
        assertTrue("强极·印比同心", jb.wangJi && jb.wangJiType == JinBiaoWangJiType.YIN_BI_TONG_XIN)
        assertTrue("判据含藏根作废", jb.wangJiBasis!!.contains("借根无用") && jb.wangJiBasis.contains("同心"))
        assertTrue("得局——化煞生印、合印局则大发", jb.quYongBasis.contains("化煞生印") && jb.quYongBasis.contains("大发"))
        assertTrue("随旺神用神取比劫（土）", jb.quYongWay == JinBiaoQuYongWay.SUI_WANG && jb.yongShenElement == Element.EARTH)

        // 事实锚定：三步北方金水运家境贫寒但不至死亡（通关/盖头顺泄，大难不死必有后福）
        assertTrue(
            "己亥庚子财地软档——贫寒不顺，不至大凶",
            listOf(0, 1).all { jb.yuns[it].verdict == JinBiaoVerdict.XIONG && jb.yuns[it].items.any { a -> a.action.contains("不至大凶") } },
        )

        // 事实锚定：壬寅运壬水无根全生寅木——干化于支以寅论，寅午戌合印局——发达发大财；癸卯卯戌合火——发财很大
        assertTrue(
            "壬寅癸卯官杀化煞合印局大发（寒门贵客）",
            listOf(3, 4).all { jb.yuns[it].verdict == JinBiaoVerdict.JI && jb.yuns[it].items.any { a -> a.action.contains("大发") } },
        )
        assertTrue("壬寅运标注干化于支", jb.yuns[3].items.any { it.action.contains("干化于支") })

        // 老引擎对照（双引擎铁律）：建禄格身强
        val geju = cn.xulun.bazi.core.GeJuCalculator.determine(bazi)
        assertTrue("老引擎定建禄格且身强", geju.name.contains("建禄") && geju.strength == DayStrength.STRONG)
    }

    // ── 案四 · 乾造 癸亥 癸亥 戊辰 甲寅（1983-12-06 寅时）——辰虚土随财杀，财官食皆吉唯帮身贫凶 ──

    @Test
    fun `案四 乾 癸亥癸亥戊辰甲寅 辰虚土寅亥化木弱极随财杀 帮身反害贫凶`() {
        val bazi = chart(1983, 12, 6, 3)
        assertTrue(bazi.fullBazi.startsWith("癸亥 癸亥 戊辰 甲寅"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "逆行大运与师传表一致",
            daYun.yuns.take(4).map { "${it.pillar.fullName}(${it.startYear})" } ==
                listOf("壬戌(1993)", "辛酉(2003)", "庚申(2013)", "己未(2023)"),
        )

        // 金镖门：寅亥合化木满盘财杀——辰为东方虚土（水库随水），敌势滔天辰根作废→帮扶归零
        val jb = JinBiaoCalculator.analyze(bazi, daYun)
        assertTrue("弱极·孤立无气（辰虚土＋寅亥化木）", jb.ruoJi && jb.ruoJiType == JinBiaoRuoJiType.GU_LI)
        assertTrue("判据含寅亥合化木与辰虚土作废",
            jb.ruoJiBasis!!.contains("寅亥合化木") && jb.ruoJiBasis.contains("辰为东方虚土"))
        assertTrue("财旺独大——随财（杀一体），用神取财（水）",
            jb.quYongWay == JinBiaoQuYongWay.SUI_CAI && jb.yongShenElement == Element.WATER)

        // 随势皆吉：辛酉庚申（食生财顺）、乙卯（随财杀走官亦富贵）
        assertTrue(
            "走财走官走食伤皆顺皆吉",
            listOf(1, 2, 7).all { jb.yuns[it].verdict == JinBiaoVerdict.JI },
        )
        assertTrue("官运标注随财杀亦富贵", jb.yuns[7].items.any { it.action.contains("随财杀") })

        // 帮之反害：己未戊午丁巳丙辰（印比帮身）皆凶贫
        assertTrue(
            "走印比帮身运帮之反害，必大凶或贫穷",
            listOf(3, 4, 5, 6).all { jb.yuns[it].verdict == JinBiaoVerdict.XIONG },
        )
        assertTrue("断语含随势而走与帮之反害", jb.duanYu.contains("随势而走") && jb.duanYu.contains("帮之反害"))

        // 老引擎对照（双引擎铁律）：七杀格身弱（相神丙印化杀，成中有败）
        val geju = cn.xulun.bazi.core.GeJuCalculator.determine(bazi)
        assertTrue("老引擎定七杀格且身弱", geju.name == "七杀格" && geju.strength == DayStrength.WEAK)
    }

    // ── 案五 · 乾造 己丑 丙子 丁亥 庚子（2009-12-08 子时）——亥子丑方局弱极随官，南方方局破败 ──

    @Test
    fun `案五 乾 己丑丙子丁亥庚子 丙火自顾不暇方局弱极 财官运高人一等 南方连行破败`() {
        val bazi = chart(2009, 12, 8, 0)
        assertTrue(bazi.fullBazi.startsWith("己丑 丙子 丁亥 庚子"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "逆行大运与师传表一致（起运极早）",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } ==
                listOf("乙亥(2010)", "甲戌(2020)", "癸酉(2030)"),
        )

        // 金镖门：丙火透干无根气＋亥子丑北方方局＋庚金财生杀＋湿土晦火——丙火比劫自顾不暇，弱极
        val jb = JinBiaoCalculator.analyze(bazi, daYun)
        assertTrue("弱极·微浮印比自顾不暇（丙火无根被克）", jb.ruoJi && jb.ruoJiType == JinBiaoRuoJiType.WEI_FU)
        assertTrue("判据含亥子丑方局叛向", jb.ruoJiBasis!!.contains("亥子丑"))
        assertTrue("官旺随官，用神取杀（水）", jb.quYongWay == JinBiaoQuYongWay.SUI_GUAN && jb.yongShenElement == Element.WATER)

        // 事实锚定：癸酉壬申财官运——比任何人都要高人一等、富贵发达
        assertTrue("癸酉壬申财官运必发达", listOf(2, 3).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })

        // 南方巳午未连行方局（辛未庚午己巳）——未午巳皆随火作比劫论，破败
        assertTrue("南方连行方局皆帮身破败", listOf(4, 5, 6).all { jb.yuns[it].verdict == JinBiaoVerdict.XIONG })
        assertTrue("辛未运标注随连行方局论", jb.yuns[4].items.any { it.action.contains("随连行方局") })
        assertTrue("乙亥甲戌印运帮之反害（顶多不死也必贫穷）", listOf(0, 1).all { jb.yuns[it].verdict == JinBiaoVerdict.XIONG })

        // 老引擎对照（双引擎铁律）：七杀格身弱（成格，相神己食神制杀）
        val geju = cn.xulun.bazi.core.GeJuCalculator.determine(bazi)
        assertTrue("老引擎定七杀格且身弱", geju.name == "七杀格" && geju.strength == DayStrength.WEAK)
    }

    // ── 案七 · 乾造 戊午 壬戌 丁卯 癸卯（1978-11-01 卯时）——地支不载官寡去之，坎卦官运无成 ──

    @Test
    fun `案七 乾 戊午壬戌丁卯癸卯 官透两见地支不载 去寡 坎卦官运一事无成 木火好运`() {
        val bazi = chart(1978, 11, 1, 5)
        assertTrue(bazi.fullBazi.startsWith("戊午 壬戌 丁卯 癸卯"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运与师传表一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } ==
                listOf("癸亥(1981)", "甲子(1991)", "乙丑(2001)"),
        )

        // 金镖门：天干一官一杀两透而地支不载——力量主要在地支，无根气＝无用之官，去寡优先于制杀
        val jb = JinBiaoCalculator.analyze(bazi, daYun)
        assertTrue("去寡·用神取伤官（土）", jb.quYongWay == JinBiaoQuYongWay.QU_GUA && jb.yongShenElement == Element.EARTH)
        assertTrue("取用依据含地支主宰与不用之官尽可去", jb.quYongBasis.contains("地支主宰") && jb.quYongBasis.contains("不用之官尽可去"))

        // 坎卦亥子官杀得禄强旺——顺着官杀，一事无成
        assertTrue("癸亥甲子官运皆凶", listOf(0, 1).all { jb.yuns[it].verdict == JinBiaoVerdict.XIONG })
        assertTrue("官运标注一世无成", jb.yuns[0].items.any { it.action.contains("一世无成") })

        // 木火运印旺身旺食伤亦旺——好运（寅卯辰连行方局亦随木作印论）
        assertTrue("丙寅丁卯木火运皆吉", listOf(3, 4).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
        assertTrue("断语含违背官杀好运", jb.duanYu.contains("违背官杀") && jb.duanYu.contains("不可用水激"))

        // 老引擎对照（双引擎铁律）：伤官格——老引擎判身弱，金镖门地支主宰判身旺，强弱分歧即此案教学点
        val geju = cn.xulun.bazi.core.GeJuCalculator.determine(bazi)
        assertTrue("老引擎定伤官格", geju.name == "伤官格")
    }

    // ── 案八 · 乾造 癸丑 壬戌 丙午 庚寅（1973-11-06 寅时）——官有根财生非去寡，身旺用官 ──

    @Test
    fun `案八 乾 癸丑壬戌丙午庚寅 三条件差一点都不对 官有根财生则用官 财官运好制化运差`() {
        val bazi = chart(1973, 11, 6, 3)
        assertTrue(bazi.fullBazi.startsWith("癸丑 壬戌 丙午 庚寅"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "逆行大运与师传表一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } ==
                listOf("辛酉(1983)", "庚申(1993)", "己未(2003)"),
        )

        // 金镖门：寅午戌三合坐刃身旺，但丑中癸＝官有根气、庚财透干来生——条件②破，非去寡
        val jb = JinBiaoCalculator.analyze(bazi, daYun)
        assertTrue("身旺用官（配财），用神取官杀（水）", jb.quYongWay == JinBiaoQuYongWay.PEI_CAI && jb.yongShenElement == Element.WATER)
        assertTrue("取用依据含财滋弱杀与帮官杀好运", jb.quYongBasis.contains("财滋弱杀") && jb.quYongBasis.contains("帮官杀之运"))

        // 事实锚定：辛酉庚申财生官——帮官之运好运；己未之后食制印化比劫皆违官——差运
        assertTrue("辛酉庚申财运皆吉", listOf(0, 1).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
        assertTrue("己未起六步制化比皆凶", (2..7).all { jb.yuns[it].verdict == JinBiaoVerdict.XIONG })
        assertTrue("断语含身旺用官总纲", jb.duanYu.contains("帮助官的运"))

        // 老引擎对照（双引擎铁律）：食神格（成格，相神庚食神生财）——财运两派同吉，官运金镖门独吉
        val geju = cn.xulun.bazi.core.GeJuCalculator.determine(bazi)
        assertTrue("老引擎定食神格", geju.name == "食神格")
    }

    // ── 案九 · 乾造 壬申 戊申 庚辰 甲申（1992-09-01 申时）——有泄路非旺极，用壬泄刚煞，戊土为病 ──

    @Test
    fun `案九 乾 壬申戊申庚辰甲申 非旺极有泄路 用壬泄刚煞 戊土为病 水运发富甲寅去病`() {
        val bazi = chart(1992, 9, 1, 15)
        assertTrue(bazi.fullBazi.startsWith("壬申 戊申 庚辰 甲申"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运与师传表一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } ==
                listOf("己酉(1994)", "庚戌(2004)", "辛亥(2014)"),
        )

        // 金镖门：壬透通根申中壬（秋水通源、透干通根者不废）——有泄路，非旺极不从；无官必用食伤泄刚煞
        val jb = JinBiaoCalculator.analyze(bazi, daYun)
        assertTrue("壬水有劲有泄路——非旺极非弱极", !jb.wangJi && !jb.ruoJi)
        assertTrue("用神取食伤（水）", jb.quYongWay == JinBiaoQuYongWay.SHI_SHENG_CAI && jb.yongShenElement == Element.WATER)
        assertTrue("取用依据含泄刚煞与地支不载", jb.quYongBasis.contains("泄刚煞") && jb.quYongBasis.contains("地支不载"))
        assertTrue("断语含印为格局之病", jb.duanYu.contains("印为格局之病"))

        // 土金运病来皆差；水运帮用神发富；财运去病更佳
        assertTrue("己酉庚戌土金运皆凶", listOf(0, 1).all { jb.yuns[it].verdict == JinBiaoVerdict.XIONG })
        assertTrue("壬子水运帮用神发富", jb.yuns[3].verdict == JinBiaoVerdict.JI)
        assertTrue("甲寅乙卯财运去病更佳", listOf(5, 6).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
        assertTrue("财运标注去病", jb.yuns[5].items.any { it.action.contains("去病") })

        // 老引擎对照（双引擎铁律）：建禄格成格——相神壬食伤生财，两派同取壬水
        val geju = cn.xulun.bazi.core.GeJuCalculator.determine(bazi)
        assertTrue("老引擎定建禄格", geju.name.contains("建禄"))
    }

    // ── 基础模型三命（第五讲口授：权在一人／昆仑之水）──

    @Test
    fun `命一 四庚辰 权在一人 天干可泄地支被夺 官干犯旺激怒 顺者当官丙运死`() {
        val bazi = BaziResult(
            yearPillar = Pillar(HeavenlyStem.GENG, EarthlyBranch.CHEN),
            monthPillar = Pillar(HeavenlyStem.GENG, EarthlyBranch.CHEN),
            dayPillar = Pillar(HeavenlyStem.GENG, EarthlyBranch.CHEN),
            hourPillar = Pillar(HeavenlyStem.GENG, EarthlyBranch.CHEN),
            birthDateTime = LocalDateTime.of(2000, 4, 15, 8, 0),
        )
        val daYun = DaYunInfo(
            forward = true, qiYunText = "合成",
            startDate = LocalDateTime.of(2004, 4, 15, 0, 0),
            yuns = listOf(
                DaYun(Pillar(HeavenlyStem.REN, EarthlyBranch.SHEN), 4, 2004, emptyList()),  // 壬申：食干泄＋比支顺
                DaYun(Pillar(HeavenlyStem.BING, EarthlyBranch.XU), 14, 2014, emptyList()),   // 丙戌：官干犯旺
            ),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun)
        assertTrue("天干全比地支全印无一丝财官——旺强极", jb.wangJi)
        assertTrue("壬申——天干泄秀可行＋西方比地顺：好运（当官）", jb.yuns[0].verdict == JinBiaoVerdict.JI)
        assertTrue("壬干标注天干泄秀", jb.yuns[0].items.any { it.action.contains("天干泄秀") })
        assertTrue("丙戌——官干直克犯旺激怒：大凶（丙运死亡）", jb.yuns[1].verdict == JinBiaoVerdict.XIONG)
        assertTrue("丙干标注犯旺激怒", jb.yuns[1].items.any { it.action.contains("犯旺激怒") })
    }

    @Test
    fun `命二 癸酉甲子庚辰甲申 金水两旺顺金水 昆仑之势 火土逆则刑妻克子贫乏`() {
        val bazi = BaziResult(
            yearPillar = Pillar(HeavenlyStem.GUI, EarthlyBranch.YOU),
            monthPillar = Pillar(HeavenlyStem.JIA, EarthlyBranch.ZI),
            dayPillar = Pillar(HeavenlyStem.GENG, EarthlyBranch.CHEN),
            hourPillar = Pillar(HeavenlyStem.JIA, EarthlyBranch.SHEN),
            birthDateTime = LocalDateTime.of(1993, 12, 15, 15, 0),
        )
        val daYun = DaYunInfo(
            forward = true, qiYunText = "合成",
            startDate = LocalDateTime.of(1997, 12, 15, 0, 0),
            yuns = listOf(
                DaYun(Pillar(HeavenlyStem.GENG, EarthlyBranch.ZI), 4, 1997, emptyList()),  // 庚子：金水顺
                DaYun(Pillar(HeavenlyStem.WU, EarthlyBranch.WU), 14, 2007, emptyList()),    // 戊午：火土逆
            ),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun)
        assertTrue("申子辰三合水局配酉金生源——昆仑之水顺金水", jb.quYongWay == JinBiaoQuYongWay.KUN_LUN && jb.yongShenElement == Element.WATER)
        assertTrue("庚子金水顺——当官好运", jb.yuns[0].verdict == JinBiaoVerdict.JI)
        assertTrue("戊午火土逆——刑妻克子贫乏", jb.yuns[1].verdict == JinBiaoVerdict.XIONG)
    }

    @Test
    fun `命三 壬子辛亥乙亥丙子 地支四水配金昆仑 不以日主论 木顺好运火逆倾家荡产`() {
        val bazi = BaziResult(
            yearPillar = Pillar(HeavenlyStem.REN, EarthlyBranch.ZI),
            monthPillar = Pillar(HeavenlyStem.XIN, EarthlyBranch.HAI),
            dayPillar = Pillar(HeavenlyStem.YI, EarthlyBranch.HAI),
            hourPillar = Pillar(HeavenlyStem.BING, EarthlyBranch.ZI),
            birthDateTime = LocalDateTime.of(1972, 11, 15, 0, 0),
        )
        val daYun = DaYunInfo(
            forward = true, qiYunText = "合成",
            startDate = LocalDateTime.of(1976, 11, 15, 0, 0),
            yuns = listOf(
                DaYun(Pillar(HeavenlyStem.GENG, EarthlyBranch.ZI), 4, 1976, emptyList()),  // 庚子：金水顺
                DaYun(Pillar(HeavenlyStem.JIA, EarthlyBranch.YIN), 14, 1986, emptyList()), // 甲寅：东方木顺泄
                DaYun(Pillar(HeavenlyStem.WU, EarthlyBranch.WU), 24, 1996, emptyList()),   // 戊午：火土逆
            ),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun)
        assertTrue("地支四水配辛金——昆仑之水，围绕水说事", jb.quYongWay == JinBiaoQuYongWay.KUN_LUN && jb.yongShenElement == Element.WATER)
        assertTrue("取用依据含昆仑与不以日主论", jb.quYongBasis.contains("昆仑") && jb.quYongBasis.contains("不一定指日主"))
        assertTrue("庚子金水顺、甲寅东方木顺泄——皆好运", listOf(0, 1).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
        assertTrue("甲寅标注顺泄", jb.yuns[1].items.any { it.action.contains("顺泄") })
        assertTrue("戊午火土逆——倾家荡产", jb.yuns[2].verdict == JinBiaoVerdict.XIONG)
    }

    // ── 案十/案十一 · 清浊章案例（第六讲） ──

    @Test
    fun `案十 癸酉甲子丙寅乙未 弱中变旺用官喜财生 半浊半清 庚申发财己未病运`() {
        val bazi = chart(1993, 12, 11, 13)
        assertTrue(bazi.fullBazi.startsWith("癸酉 甲子 丙寅 乙未"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "逆行大运与师传表一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } ==
                listOf("癸亥(1995)", "壬戌(2005)", "辛酉(2015)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun)
        assertTrue("坐寅贴身印＋甲通根未，官被寅泄未克转衰——弱中变旺", jb.strength == DayStrength.STRONG && jb.strengthBasis.contains("弱中变旺"))
        assertTrue("旺则首选财官——用神取官（子水），喜酉金财生官", jb.quYongWay == JinBiaoQuYongWay.PEI_CAI && jb.yongShenElement == Element.WATER)
        assertTrue("未土克用神＋火克喜神财——双浊源，半浊半清（非清命）",
            jb.qingZhuo == JinBiaoQingZhuo.BAN_QING && jb.qingZhuoBasis.contains("兼病"))

        // 事实锚定：庚申运家业丰裕（申冲去寅、助起官星）发财——金水帮官皆吉
        assertTrue("癸亥辛酉庚申金水运皆吉", listOf(0, 2, 3).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
        // 己未起火土病运皆凶（未克子水用神、丁得根克酉金喜神）
        assertTrue("己未戊午丁巳丙辰火土皆凶", listOf(4, 5, 6, 7).all { jb.yuns[it].verdict == JinBiaoVerdict.XIONG })
        assertTrue("断语含多成多败（跟清命不一样）", jb.duanYu.contains("多成多败"))

        // 老引擎对照：正官格
        val geju = cn.xulun.bazi.core.GeJuCalculator.determine(bazi)
        assertTrue("老引擎定正官格", geju.name == "正官格")
    }

    @Test
    fun `案十一 甲子丙寅己亥辛未 水生木官旺身弱用丙火印 水不透干通关相生 清命南方火运富贵`() {
        // 甲子年丙寅月无己亥日（1984 寅月内无）——教学例，按原文四柱合成盘
        val bazi = BaziResult(
            yearPillar = Pillar(HeavenlyStem.JIA, EarthlyBranch.ZI),
            monthPillar = Pillar(HeavenlyStem.BING, EarthlyBranch.YIN),
            dayPillar = Pillar(HeavenlyStem.JI, EarthlyBranch.HAI),
            hourPillar = Pillar(HeavenlyStem.XIN, EarthlyBranch.WEI),
            birthDateTime = LocalDateTime.of(1984, 2, 20, 13, 0),
        )
        val daYun = DaYunInfo(
            forward = true, qiYunText = "合成（顺行自丙寅）",
            startDate = LocalDateTime.of(1988, 2, 20, 0, 0),
            yuns = listOf(
                DaYun(Pillar(HeavenlyStem.DING, EarthlyBranch.MAO), 4, 1988, emptyList()),   // 丁卯
                DaYun(Pillar(HeavenlyStem.WU, EarthlyBranch.CHEN), 14, 1998, emptyList()),    // 戊辰
                DaYun(Pillar(HeavenlyStem.JI, EarthlyBranch.SI), 24, 2008, emptyList()),      // 己巳
                DaYun(Pillar(HeavenlyStem.GENG, EarthlyBranch.WU), 34, 2018, emptyList()),    // 庚午
                DaYun(Pillar(HeavenlyStem.XIN, EarthlyBranch.WEI), 44, 2028, emptyList()),    // 辛未
                DaYun(Pillar(HeavenlyStem.REN, EarthlyBranch.SHEN), 54, 2038, emptyList()),   // 壬申
            ),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun)
        assertTrue("寅月官旺＋水生木财生官——身弱（未极，未土独根被克而印可用）",
            jb.strength == DayStrength.WEAK && !jb.ruoJi)
        assertTrue("身弱用印——丙火得用为用神", jb.quYongWay == JinBiaoQuYongWay.HUA_SHA && jb.yongShenElement == Element.FIRE)
        assertTrue("病神水不透干、经官木通关转而相生——丙火没被克：清命",
            jb.qingZhuo == JinBiaoQingZhuo.QING && jb.qingZhuoBasis.contains("相生有情"))
        // 走南方火运——印旺化杀、身衰补起：富贵（用印局印比连行方局整运作吉）
        assertTrue("南方巳午未连行火印运皆吉（杀旺印旺身衰补起，富贵）",
            listOf(2, 3, 4).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
    }

    // ── 案十二 · 乙亥 庚辰 戊戌 丁巳（教学合成盘——乙亥年辰月无戊戌日）——三废全浊 ──

    @Test
    fun `案十二 乙亥庚辰戊戌丁巳 官食财三废 全浊用神无着落 一事无成`() {
        val bazi = BaziResult(
            yearPillar = Pillar(HeavenlyStem.YI, EarthlyBranch.HAI),
            monthPillar = Pillar(HeavenlyStem.GENG, EarthlyBranch.CHEN),
            dayPillar = Pillar(HeavenlyStem.WU, EarthlyBranch.XU),
            hourPillar = Pillar(HeavenlyStem.DING, EarthlyBranch.SI),
            birthDateTime = LocalDateTime.of(1995, 4, 20, 9, 0),
        )
        val daYun = DaYunInfo(
            forward = false, qiYunText = "合成（逆行自庚辰）",
            startDate = LocalDateTime.of(1999, 4, 20, 0, 0),
            yuns = listOf(
                DaYun(Pillar(HeavenlyStem.JI, EarthlyBranch.MAO), 4, 1999, emptyList()),
                DaYun(Pillar(HeavenlyStem.WU, EarthlyBranch.YIN), 14, 2009, emptyList()),
                DaYun(Pillar(HeavenlyStem.DING, EarthlyBranch.CHOU), 24, 2019, emptyList()),
                DaYun(Pillar(HeavenlyStem.BING, EarthlyBranch.ZI), 34, 2029, emptyList()),
            ),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun)
        assertTrue("辰月土旺、火随土走比劫旺克财——身旺", jb.strength == DayStrength.STRONG)
        assertTrue("官被庚金克且财源亥被巳冲断——官废不能用（不用之官尽可去）；只能用食神泄我去官",
            jb.quYongWay == JinBiaoQuYongWay.SHI_SHENG_CAI && jb.yongShenElement == Element.METAL)
        assertTrue("官食财三废俱浊——全浊", jb.qingZhuo == JinBiaoQingZhuo.QUAN_ZHUO && jb.qingZhuoBasis.contains("三者俱废"))
        assertTrue("断语一事无成", jb.duanYu.contains("一事无成"))
    }

    // ── 案十三 · 癸亥 己未 丙午 己丑（1983-07-17 丑时）——伤官得局旺中变弱，用木两用，卯寅补拱发财 ──

    @Test
    fun `案十三 乾 癸亥己未丙午己丑 伤官得局旺中变弱 用木疏土生身 乙卯甲寅补拱发财`() {
        val bazi = chart(1983, 7, 17, 1)
        assertTrue(bazi.fullBazi.startsWith("癸亥 己未 丙午 己丑"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "逆行大运与师传表一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } ==
                listOf("戊午", "丁巳", "丙辰").mapIndexed { i, s -> s + "(${daYun.yuns[i].startYear})" }.map { it },
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun)
        assertTrue("未月季土晦火、伤官得局泄过——旺中变弱（身弱）", jb.strength == DayStrength.WEAK && !jb.ruoJi)
        assertTrue("用神取印木——两用：疏土克伤官局＋生身",
            jb.quYongWay == JinBiaoQuYongWay.HUA_SHA && jb.yongShenElement == Element.WOOD && jb.quYongBasis.contains("印两用"))

        // 事实锚定：乙卯甲寅运——亥卯未拱局补成／寅亥合木，用神木到，发财
        val yunNames = jb.yuns.map { it.yunName }
        val yiMao = jb.yuns[yunNames.indexOfFirst { it.startsWith("乙卯") }]
        val jiaYin = jb.yuns[yunNames.indexOfFirst { it.startsWith("甲寅") }]
        assertTrue("乙卯运亥卯未补成印局大发", yiMao.verdict == JinBiaoVerdict.JI && yiMao.items.any { it.action.contains("补成") })
        assertTrue("甲寅运寅亥合木用神到位——发财", jiaYin.verdict == JinBiaoVerdict.JI)
    }

    // ── 案十四 · 丁卯 丁未 庚午 己卯（1987-07-20 卯时）——火炎土焦清枯，好运仅温饱 ──

    @Test
    fun `案十四 乾 丁卯丁未庚午己卯 木火两旺身弱只能用印 火炎土焦清枯 东方运一事无成`() {
        val bazi = chart(1987, 7, 20, 5)
        assertTrue(bazi.fullBazi.startsWith("丁卯 丁未 庚午 己卯"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "逆行大运与师传表一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } ==
                listOf("丙午(1991)", "乙巳(2001)", "甲辰(2011)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun)
        assertTrue("木生火木克土木火两旺、庚无根——身弱", jb.strength == DayStrength.WEAK && !jb.ruoJi)
        assertTrue("只能用印（土）——最起码土帮我扛了一下火",
            jb.quYongWay == JinBiaoQuYongWay.HUA_SHA && jb.yongShenElement == Element.EARTH)
        assertTrue("火炎土焦（燥土难生金）＋木克土——病太大一点救没有：清枯",
            jb.qingZhuo == JinBiaoQingZhuo.QING_KU && jb.qingZhuoBasis.contains("火炎土焦"))
        assertTrue("断语含温饱与一事无成", jb.duanYu.contains("温饱") && jb.duanYu.contains("一事无成"))

        // 好运稍好（丙午官生印——温饱）；东方运（癸卯壬寅木地——干化于支）克用生忌，一事无成
        assertTrue("丙午好运——稍好温饱", jb.yuns[0].verdict == JinBiaoVerdict.JI)
        assertTrue("癸卯壬寅东方木运皆凶", listOf(3, 4).all { jb.yuns[it].verdict == JinBiaoVerdict.XIONG })
    }

    // ── 案十五~案二十一 · 气象调候七例（寒甚要暖有气，暖至要寒有根） ──

    @Test
    fun `案十五 甲申丙子庚辰戊寅 偏寒丙火有气 用丙调候 木火运当官`() {
        val bazi = chart(2004, 12, 27, 3)
        assertTrue(bazi.fullBazi.startsWith("甲申 丙子 庚辰 戊寅"))
        val daYun = DaYunInfo(true, "合成", LocalDateTime.of(2008, 12, 27, 0, 0), listOf(
            DaYun(Pillar(HeavenlyStem.JIA, EarthlyBranch.YIN), 4, 2008, emptyList()),
            DaYun(Pillar(HeavenlyStem.REN, EarthlyBranch.ZI), 14, 2018, emptyList()),
        ))
        val jb = JinBiaoCalculator.analyze(bazi, daYun)
        assertTrue("偏寒而丙有气（甲贴丙＋寅根）——气象调候用火", jb.quYongWay == JinBiaoQuYongWay.TIAO_HOU && jb.yongShenElement == Element.FIRE)
        assertTrue("得气之寒遇暖而发——甲寅木火运当官", jb.yuns[0].verdict == JinBiaoVerdict.JI)
        assertTrue("壬子金水助寒——差", jb.yuns[1].verdict == JinBiaoVerdict.XIONG)
    }

    @Test
    fun `案十六 己酉丙子庚辰甲申 辰化金甲隔位不载 丙无气不调候 火运一事无成`() {
        val bazi = BaziResult(
            Pillar(HeavenlyStem.JI, EarthlyBranch.YOU), Pillar(HeavenlyStem.BING, EarthlyBranch.ZI),
            Pillar(HeavenlyStem.GENG, EarthlyBranch.CHEN), Pillar(HeavenlyStem.JIA, EarthlyBranch.SHEN),
            LocalDateTime.of(1969, 12, 20, 3, 0),
        )
        val daYun = DaYunInfo(false, "合成", LocalDateTime.of(1973, 12, 20, 0, 0), listOf(
            DaYun(Pillar(HeavenlyStem.DING, EarthlyBranch.SI), 4, 1973, emptyList()),
            DaYun(Pillar(HeavenlyStem.REN, EarthlyBranch.SHEN), 14, 1983, emptyList()),
        ))
        val jb = JinBiaoCalculator.analyze(bazi, daYun)
        assertTrue("金局夺辰、甲隔位地支不载——丙无气，不顺调候顺金水", jb.quYongWay == JinBiaoQuYongWay.KUN_LUN && jb.yongShenElement == Element.WATER)
        assertTrue("走丁巳助火调候——一事无成", jb.yuns[0].verdict == JinBiaoVerdict.XIONG && jb.yuns[0].items.any { it.action.contains("不调候") })
        assertTrue("金水顺——好运", jb.yuns[1].verdict == JinBiaoVerdict.JI)
    }

    @Test
    fun `案十七 丁丑丙午丙午壬辰 仲夏燥壬水有根 可调候 金水运当官富贵`() {
        val bazi = chart(1997, 7, 3, 7)
        assertTrue(bazi.fullBazi.startsWith("丁丑 丙午 丙午 壬辰"))
        val daYun = DaYunInfo(true, "合成", LocalDateTime.of(2001, 7, 3, 0, 0), listOf(
            DaYun(Pillar(HeavenlyStem.GENG, EarthlyBranch.SHEN), 4, 2001, emptyList()),
            DaYun(Pillar(HeavenlyStem.BING, EarthlyBranch.WU), 14, 2011, emptyList()),
        ))
        val jb = JinBiaoCalculator.analyze(bazi, daYun)
        assertTrue("燥而壬坐辰库有根——可调候用水", jb.quYongWay == JinBiaoQuYongWay.TIAO_HOU && jb.yongShenElement == Element.WATER)
        assertTrue("庚申金水运——当官富贵（得大气象）", jb.yuns[0].verdict == JinBiaoVerdict.JI)
        assertTrue("丙午火运——凶", jb.yuns[1].verdict == JinBiaoVerdict.XIONG)
    }

    @Test
    fun `案十八 癸未丁巳丙午癸巳 癸水无根不能调 走水运犯火之旺一败如灰`() {
        val bazi = chart(2003, 6, 2, 9)
        assertTrue(bazi.fullBazi.startsWith("癸未 丁巳 丙午 癸巳"))
        val daYun = DaYunInfo(true, "合成", LocalDateTime.of(2007, 6, 2, 0, 0), listOf(
            DaYun(Pillar(HeavenlyStem.REN, EarthlyBranch.ZI), 4, 2007, emptyList()),
        ))
        val jb = JinBiaoCalculator.analyze(bazi, daYun)
        assertTrue("壬子水运——调候神无根强走犯火之旺：一败如灰",
            jb.yuns[0].verdict == JinBiaoVerdict.XIONG && jb.yuns[0].items.any { it.action.contains("犯火之旺") })
    }

    @Test
    fun `案十九 丙辰辛丑庚辰丙子 暖无气 走丙午丁未离婚克子一事无成`() {
        val bazi = chart(1977, 1, 23, 0)
        assertTrue(bazi.fullBazi.startsWith("丙辰 辛丑 庚辰 丙子"))
        val daYun = DaYunInfo(false, "合成", LocalDateTime.of(1981, 1, 23, 0, 0), listOf(
            DaYun(Pillar(HeavenlyStem.BING, EarthlyBranch.WU), 4, 1981, emptyList()),
            DaYun(Pillar(HeavenlyStem.DING, EarthlyBranch.WEI), 14, 1991, emptyList()),
        ))
        val jb = JinBiaoCalculator.analyze(bazi, daYun)
        assertTrue("丙火地支无根无气（隔位无贴生）——强走火运调候反凶",
            listOf(0, 1).all { jb.yuns[it].verdict == JinBiaoVerdict.XIONG } &&
                jb.yuns.all { it.items.any { i -> i.action.contains("不调候") } })
    }

    @Test
    fun `案二十 丁未壬子庚戌丙戌 戌戌火库藏丁 火有根气可以调候`() {
        val bazi = chart(1967, 12, 12, 19)
        assertTrue(bazi.fullBazi.startsWith("丁未 壬子 庚戌 丙戌"))
        val jb = JinBiaoCalculator.analyze(bazi, null)
        assertTrue("子月寒而丁丙透、戌戌未藏丁——火有根气，气象调候用火",
            jb.quYongWay == JinBiaoQuYongWay.TIAO_HOU && jb.yongShenElement == Element.FIRE)
    }

    @Test
    fun `案二十一 癸未丁巳甲午庚午 癸无根不能调 走壬水犯火之旺死`() {
        val bazi = BaziResult(
            Pillar(HeavenlyStem.GUI, EarthlyBranch.WEI), Pillar(HeavenlyStem.DING, EarthlyBranch.SI),
            Pillar(HeavenlyStem.JIA, EarthlyBranch.WU), Pillar(HeavenlyStem.GENG, EarthlyBranch.WU),
            LocalDateTime.of(2003, 5, 21, 11, 0),
        )
        val daYun = DaYunInfo(true, "合成", LocalDateTime.of(2007, 5, 21, 0, 0), listOf(
            DaYun(Pillar(HeavenlyStem.REN, EarthlyBranch.SHEN), 4, 2007, emptyList()),
        ))
        val jb = JinBiaoCalculator.analyze(bazi, daYun)
        assertTrue("巳午未三会火局叛向——甲木无援从儿，顺火之势", jb.quYongWay == JinBiaoQuYongWay.SUI_SHI && jb.yongShenElement == Element.FIRE)
        assertTrue("壬申水运——犯火之旺：死", jb.yuns[0].verdict == JinBiaoVerdict.XIONG && jb.yuns[0].items.any { it.action.contains("犯火之旺") })
    }

    // ── 案廿二 · 己酉 丙寅 庚申 庚辰（1969-02-14 辰时）——财滋弱杀·财被劫大运解病 ──

    @Test
    fun `案廿二 己酉丙寅庚申庚辰 财滋弱杀 财被劫杀弱无妨 北方水运解病一样富贵`() {
        val bazi = chart(1969, 2, 14, 7)
        assertTrue(bazi.fullBazi.startsWith("己酉 丙寅 庚申 庚辰"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "逆行大运与师传表一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } ==
                listOf("乙丑(1972)", "甲子(1982)", "癸亥(1992)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun)
        assertTrue("土金两旺身强、丙杀露而无食伤——有财有官杀优先用杀（财滋弱杀）",
            jb.quYongWay == JinBiaoQuYongWay.PEI_CAI && jb.yongShenElement == Element.FIRE)
        assertTrue("财被劫（寅申冲）杀弱无妨——大运解病一样富贵，勿断贫",
            jb.quYongBasis.contains("勿见杀弱财弱断贫") && jb.quYongBasis.contains("解病"))

        // 北方水运泄金生木通关解病——一样发富发贵；西方金地加重金克木之病——差
        assertTrue("甲子癸亥水运药神到——吉", listOf(1, 2).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
        assertTrue("辛酉庚申金地助病——凶", listOf(4, 5).all { jb.yuns[it].verdict == JinBiaoVerdict.XIONG })
    }

    // ── 案廿三 · 丙申 庚寅 庚申 辛巳（1956-02-23 巳时）——财滋弱杀·火运双用当官 ──

    @Test
    fun `案廿三 丙申庚寅庚申辛巳 巳随金走杀真弱通寅根不去杀 火运双用当官 西方差`() {
        val bazi = chart(1956, 2, 23, 9)
        assertTrue(bazi.fullBazi.startsWith("丙申 庚寅 庚申 辛巳"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运与师传表一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } ==
                listOf("辛卯(1959)", "壬辰(1969)", "癸巳(1979)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun)
        assertTrue("两申贴克寅＋天干三金——身旺；巳随金走不作火根、丙杀真弱而通寅根",
            jb.strength == DayStrength.STRONG && jb.quYongWay == JinBiaoQuYongWay.PEI_CAI && jb.yongShenElement == Element.FIRE)
        assertTrue("杀有根气不能去杀——必须财滋弱杀、禁制杀（制杀贫穷欠债）",
            jb.quYongBasis.contains("巳随") && jb.quYongBasis.contains("禁制杀"))

        // 火运双用（杀通根＋克金护寅）——当官；西方土金助病差
        assertTrue("癸巳甲午火运皆吉", listOf(2, 3).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
        assertTrue("巳午火运标注双用当官", jb.yuns[2].items.any { it.action.contains("当官") } || jb.yuns[3].items.any { it.action.contains("当官") })
        assertTrue("戊戌西方土金凶", jb.yuns[7].verdict == JinBiaoVerdict.XIONG)
    }

    // ── 案廿四 · 癸未 甲子 戊午 癸亥（2003-12-11 亥时，坤）——财党杀攻身·财滋弱杀反例 ──

    @Test
    fun `案廿四 坤 癸未甲子戊午癸亥 身弱财党杀攻身财破印 非贫即夭 丙寅化杀稍安`() {
        val bazi = chart(2003, 12, 11, 21)
        assertTrue(bazi.fullBazi.startsWith("癸未 甲子 戊午 癸亥"))
        val daYun = DaYunCalculator.compute(bazi, Gender.FEMALE)
        assertTrue(
            "顺行大运与师传表一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } ==
                listOf("乙丑(2012)", "丙寅(2022)", "丁卯(2032)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun)
        assertTrue("旺财党杀攻身＋财先破印＋坐下有根非从——财党杀攻身档",
            jb.quYongWay == JinBiaoQuYongWay.GONG_SHEN && jb.yongShenElement == Element.EARTH)
        assertTrue("取用依据含反例框架（身强用杀才行财滋弱杀）", jb.quYongBasis.contains("反例") && jb.quYongBasis.contains("非贫即夭"))
        assertTrue("断语含肾病与非贫即夭", jb.duanYu.contains("非贫即夭") && jb.duanYu.contains("肾病"))

        // 丙寅印透化杀——好运稍安（然身太衰难救本）；壬申财滋杀——最忌
        assertTrue("丙寅运化杀稍安（中平）", jb.yuns[1].verdict == JinBiaoVerdict.PING)
        assertTrue("壬申财地凶", jb.yuns[7].verdict == JinBiaoVerdict.XIONG)
    }

    // ── 案廿五 · 壬戌 乙巳 戊戌 壬戌（1982-05-15 戌时，坤）——财滋弱官同格·女命官断 ──

    @Test
    fun `案廿五 坤 壬戌乙巳戊戌壬戌 财滋弱官同格不区分 女命用官家庭主妇夫收入佳`() {
        val bazi = chart(1982, 5, 15, 19)
        assertTrue(bazi.fullBazi.startsWith("壬戌 乙巳 戊戌 壬戌"))
        val daYun = DaYunCalculator.compute(bazi, Gender.FEMALE)
        assertTrue(
            "逆行大运与师传表一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } ==
                listOf("甲辰(1985)", "癸卯(1995)", "壬寅(2005)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.FEMALE)
        assertTrue("身强＋用官＋财生——财滋弱杀/弱官同格不区分", jb.quYongWay == JinBiaoQuYongWay.PEI_CAI && jb.yongShenElement == Element.WOOD)
        assertTrue("取用依据含同格不区分", jb.quYongBasis.contains("不区分"))
        assertTrue("女命用官——家庭主妇或官方工作断（事实：家庭主妇，夫收入不错）",
            jb.duanYu.contains("家庭主妇") && jb.duanYu.contains("百分之百"))

        // 走帮官杀财之运即好运（调候让位）：癸卯壬寅官杀得地吉；戊戌比劫夺财凶
        assertTrue("癸卯壬寅官杀得地皆吉", listOf(1, 2).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
        assertTrue("戊戌比劫夺财凶", jb.yuns[6].verdict == JinBiaoVerdict.XIONG)
    }

    // ── 案廿六 · 戊子 甲寅 戊午 甲寅（合成盘，乾）——杀重用印·坐下有情 ──

    @Test
    fun `案廿六 乾 戊子甲寅戊午甲寅 杀重用印坐下印有情 子午冲寅隔属友情`() {
        val bazi = BaziResult(
            Pillar(HeavenlyStem.WU, EarthlyBranch.ZI), Pillar(HeavenlyStem.JIA, EarthlyBranch.YIN),
            Pillar(HeavenlyStem.WU, EarthlyBranch.WU), Pillar(HeavenlyStem.JIA, EarthlyBranch.YIN),
            LocalDateTime.of(2008, 2, 15, 3, 0),
        )
        val daYun = DaYunInfo(true, "合成（顺行自甲寅）", LocalDateTime.of(2012, 2, 15, 0, 0), listOf(
            DaYun(Pillar(HeavenlyStem.DING, EarthlyBranch.SI), 4, 2012, emptyList()),  // 丁巳火印帮身
            DaYun(Pillar(HeavenlyStem.GENG, EarthlyBranch.SHEN), 14, 2022, emptyList()), // 庚申金党杀
        ))
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("官杀党力10压倒性、印比3.5——杀重身弱不翻（护栏）", jb.strength == DayStrength.WEAK)
        assertTrue("杀重用印格——用神取坐下印（午火）",
            jb.quYongWay == JinBiaoQuYongWay.HUA_SHA && jb.yongShenElement == Element.FIRE)
        assertTrue("取用依据含化之以德与坐下有情", jb.quYongBasis.contains("化之以德") && jb.quYongBasis.contains("情分"))
        assertTrue("子午之冲有寅卡中属友情——冲化为生不作冲伤",
            jb.health.supports.any { it.contains("情分") } && jb.health.damages.none { it.contains("冲") })
        assertTrue("丁巳火运帮身吉、庚申金党杀凶", jb.yuns[0].verdict == JinBiaoVerdict.JI && jb.yuns[1].verdict == JinBiaoVerdict.XIONG)
    }

    // ── 案廿七 · 戊辰 庚申 甲子 甲子（1988-09-06 早子时，乾）——杀重用印·昆仑让位 ──

    @Test
    fun `案廿七 乾 戊辰庚申甲子甲子 辰贴申生金会局被夺 杀重用印坐下印有情`() {
        val bazi = chart(1988, 9, 6, 0)
        assertTrue(bazi.fullBazi.startsWith("戊辰 庚申 甲子 甲子"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运与师传表一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } ==
                listOf("辛酉(1989)", "壬戌(1999)", "癸亥(2009)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("申月金旺辰生金——金旺杀重、甲无载身弱（干多不如支重）", jb.strength == DayStrength.WEAK)
        assertTrue("杀重用印格（昆仑让位）——用神取坐下印（子水）",
            jb.quYongWay == JinBiaoQuYongWay.HUA_SHA && jb.yongShenElement == Element.WATER)
        assertTrue("取用依据含辰贴申生金与情分与寒木向阳",
            jb.quYongBasis.contains("辰贴申必生金") && jb.quYongBasis.contains("情分") && jb.quYongBasis.contains("寒木向阳"))

        // 金水帮印身皆吉（没火一样当官）；土止水凶
        assertTrue("辛酉癸亥甲子金水运皆吉", listOf(0, 2, 3).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
        assertTrue("壬戌运戌土止水——非吉（干支分论偏凶）", jb.yuns[1].verdict != JinBiaoVerdict.JI)
    }

    // ── 案廿八 · 戊午 丙辰 庚寅 丙戌（合成盘，乾）——杀重用印·湿土晦火·财病化有情 ──

    @Test
    fun `案廿八 乾 戊午丙辰庚寅丙戌 火局克庚不昆仑 杀重用印湿土晦火 寅病化有情当官富贵`() {
        val bazi = BaziResult(
            Pillar(HeavenlyStem.WU, EarthlyBranch.WU), Pillar(HeavenlyStem.BING, EarthlyBranch.CHEN),
            Pillar(HeavenlyStem.GENG, EarthlyBranch.YIN), Pillar(HeavenlyStem.BING, EarthlyBranch.XU),
            LocalDateTime.of(1978, 4, 20, 19, 0),
        )
        val daYun = DaYunInfo(true, "合成（顺行自丙辰）", LocalDateTime.of(1982, 4, 20, 0, 0), listOf(
            DaYun(Pillar(HeavenlyStem.DING, EarthlyBranch.SI), 4, 1982, emptyList()),
            DaYun(Pillar(HeavenlyStem.GENG, EarthlyBranch.SHEN), 34, 2012, emptyList()),
        ))
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("火局克庚——杀旺身弱非昆仑（局克日主不为顺）", jb.quYongWay != JinBiaoQuYongWay.KUN_LUN)
        assertTrue("印辰有气不随杀走——杀重用印格，用神取印（土）",
            jb.quYongWay == JinBiaoQuYongWay.HUA_SHA && jb.yongShenElement == Element.EARTH)
        assertTrue("湿土晦火养金＋寅病经火化有情＋豁免旺衰",
            jb.quYongBasis.contains("湿土") && jb.quYongBasis.contains("不受木的克") && jb.quYongBasis.contains("有情豁免"))
        assertTrue("财不透干经官通关——清（当官富贵）", jb.qingZhuo == JinBiaoQingZhuo.QING)
    }

    // ── 案廿九 · 癸亥 癸亥 丁卯 癸卯（1983-12-05 卯时，乾）——众杀混行一仁可化 ──

    @Test
    fun `案廿九 乾 癸亥癸亥丁卯癸卯 三癸两亥杀旺 坐下卯仁化杀有情 金运伤用运过即复`() {
        val bazi = chart(1983, 12, 5, 5)
        assertTrue(bazi.fullBazi.startsWith("癸亥 癸亥 丁卯 癸卯"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "逆行大运与师传表一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } ==
                listOf("壬戌(1992)", "辛酉(2002)", "庚申(2012)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("杀旺无疑、印化之以德、坐下卯——杀重用印格", jb.quYongWay == JinBiaoQuYongWay.HUA_SHA && jb.yongShenElement == Element.WOOD)
        assertTrue("众杀混行一仁可化＋运过即复入据", jb.quYongBasis.contains("众杀混行，一仁可化") && jb.quYongBasis.contains("运一过功能即复"))
        assertTrue("坐下有情——官杀全往坐下来生拱，外界有情即富贵", jb.quYongBasis.contains("情分"))

        // 庚申辛酉金克木用神被伤——事业很差（一事无成甚至贫夭）
        assertTrue("辛酉庚申金运皆凶", listOf(1, 2).all { jb.yuns[it].verdict == JinBiaoVerdict.XIONG })
    }

    // ── 案三十 · 戊辰 戊午 壬辰 甲辰（1988-06-06 辰时，乾）——食神制杀·日主通根三辰 ──

    @Test
    fun `案三十 乾 戊辰戊午壬辰甲辰 壬通根三辰有根气 食神制杀成格 妙在无金一将当关`() {
        val bazi = chart(1988, 6, 6, 7)
        assertTrue(bazi.fullBazi.startsWith("戊辰 戊午 壬辰 甲辰"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运与师传表一致",
            daYun.yuns.take(3).map { it.pillar.fullName } == listOf("己未", "庚申", "辛酉"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("日主通根三个辰（辰中癸）＝有根气，不弱极——足以食神制杀卫身成格",
            !jb.ruoJi && jb.quYongWay == JinBiaoQuYongWay.ZHI_SHA && jb.yongShenElement == Element.WOOD)
        assertTrue("取用依据含通根三支、妙在无金、一将当关",
            jb.quYongBasis.contains("通根三支") && jb.quYongBasis.contains("妙在无金") && jb.quYongBasis.contains("一将当关"))
        assertTrue("有根气豁免替换贫穷断", !jb.duanYu.contains("贫穷汉"))

        // 妙在无金——金运（庚申辛酉）印夺食反凶；水木帮身食皆吉
        assertTrue("庚申辛酉金运（印夺食）皆凶", listOf(1, 2).all { jb.yuns[it].verdict == JinBiaoVerdict.XIONG })
        assertTrue("癸亥甲子乙丑水木运皆吉", listOf(4, 5, 6).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
    }

    // ── 案卅一 · 庚申 庚辰 甲戌 丙寅（1980-05-01 寅时，乾）——食神制杀·气贯生时 ──

    @Test
    fun `案卅一 乾 庚申庚辰甲戌丙寅 时支寅木气贯生时 辰戌冲砍余气根 食神制杀印运夺食不可`() {
        val bazi = chart(1980, 5, 1, 3)
        assertTrue(bazi.fullBazi.startsWith("庚申 庚辰 甲戌 丙寅"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运与师传表一致",
            daYun.yuns.take(3).map { it.pillar.fullName } == listOf("辛巳", "壬午", "癸未"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("时支寅本气根（气贯生时）＋杀旺＋丙食有力＋印微不夺食——食神制杀格",
            jb.quYongWay == JinBiaoQuYongWay.ZHI_SHA && jb.yongShenElement == Element.FIRE)
        assertTrue("取用依据含气贯生时、辰戌冲砍根、印运夺食",
            jb.quYongBasis.contains("气贯生时") && jb.quYongBasis.contains("藏金暗相砍木") && jb.quYongBasis.contains("印运夺食"))

        // 壬午癸未干头水在南方力减——终究差一点（非大凶）；午地支帮食伤
        assertTrue("壬午癸未非凶（干头印在南方力减）", listOf(1, 2).all { jb.yuns[it].verdict != JinBiaoVerdict.XIONG })
    }

    // ── 案卅二 · 壬子 壬子 丙戌 戊戌（1972-12-21 戌时，乾）——食神制杀·退而求其次 ──

    @Test
    fun `案卅二 乾 壬子壬子丙戌戊戌 杀旺无印 丙两处库根通根 退而求其次食神制杀`() {
        val bazi = chart(1972, 12, 21, 19)
        assertTrue(bazi.fullBazi.startsWith("壬子 壬子 丙戌 戊戌"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运与师传表一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("癸丑(1977)", "甲寅(1987)", "乙卯(1997)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("杀极旺无印食透藏根两处——不弱极，退而求其次食神制杀",
            !jb.ruoJi && jb.quYongWay == JinBiaoQuYongWay.ZHI_SHA && jb.yongShenElement == Element.EARTH)
        assertTrue("取用依据含首先考虑印、退而求其次、没有用印的厉害",
            jb.quYongBasis.contains("首先考虑有没有印") && jb.quYongBasis.contains("退而求其次") && jb.quYongBasis.contains("没有用印的厉害"))

        // 壬子型（不是克就是泄、身无力）——印运反是好运：大运补印化杀帮身（师断「走印运克食神反而好」）
        assertTrue("甲寅乙卯印运化杀帮身皆吉", listOf(1, 2).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
        assertTrue("丙辰之辰随寅卯辰方局作印论——亦吉", jb.yuns[3].verdict == JinBiaoVerdict.JI)
        assertTrue("丁巳戊午火土皆吉", listOf(4, 5).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
    }

    // ── 案卅三 · 壬戌 壬子 丙戌 己丑（1982-12-29 丑时，乾）——退而求其次·印运反好 ──

    @Test
    fun `案卅三 乾 壬戌壬子丙戌己丑 杀旺无印食制退而求其次 印运反好甲寅本科乙卯得财`() {
        val bazi = chart(1982, 12, 29, 1)
        assertTrue(bazi.fullBazi.startsWith("壬戌 壬子 丙戌 己丑"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运与师传表一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("癸丑(1985)", "甲寅(1995)", "乙卯(2005)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("杀旺无印、丙通两根得气——退而求其次食制，不弱极",
            !jb.ruoJi && jb.quYongWay == JinBiaoQuYongWay.ZHI_SHA && jb.yongShenElement == Element.EARTH)
        assertTrue("取用依据含不如印化层次高与印运好运",
            jb.quYongBasis.contains("不如印化层次高") && jb.quYongBasis.contains("印运反是好运"))

        // 事实锚定：甲寅运本科、乙卯运做生意得财——印运化杀帮身好运
        assertTrue("甲寅乙卯印运皆吉（本科/得财）", listOf(1, 2).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
        assertTrue("丁巳戊午火土亦吉", listOf(4, 5).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
    }

    // ── 案卅四 · 壬申 丙午 庚午 丙戌（1992-06-23 戌时，乾）——食神制杀·禄根型做官 ──

    @Test
    fun `案卅四 乾 壬申丙午庚午丙戌 申为庚禄不克泄 禄根型食制 金水俱吉做官`() {
        val bazi = chart(1992, 6, 23, 19)
        assertTrue(bazi.fullBazi.startsWith("壬申 丙午 庚午 丙戌"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运与师传表一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("丁未(1996)", "戊申(2006)", "己酉(2016)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("杀旺食透禄根——禄根型食制（非调候面）",
            jb.quYongWay == JinBiaoQuYongWay.ZHI_SHA && jb.yongShenElement == Element.WATER)
        assertTrue("取用依据含禄根不克泄与做官",
            jb.quYongBasis.contains("禄根不克泄") && jb.quYongBasis.contains("要做官"))

        // 帮食（水）兼帮身（金）俱吉——金水好运做官；火土凶
        assertTrue("庚戌辛亥壬子癸丑金水四步皆吉", listOf(3, 4, 5, 6).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
        assertTrue("丁未火土凶", jb.yuns[0].verdict == JinBiaoVerdict.XIONG)
    }

    // ── 案卅五 · 癸丑 戊午 丙午 壬辰（合成盘，乾）——合官留杀·清格 ──

    @Test
    fun `案卅五 乾 癸丑戊午丙午壬辰 两刃身旺官杀先用 戊癸合官留杀清格 有情豁免旺衰当官`() {
        val bazi = BaziResult(
            Pillar(HeavenlyStem.GUI, EarthlyBranch.CHOU), Pillar(HeavenlyStem.WU, EarthlyBranch.WU),
            Pillar(HeavenlyStem.BING, EarthlyBranch.WU), Pillar(HeavenlyStem.REN, EarthlyBranch.CHEN),
            LocalDateTime.of(1973, 6, 20, 7, 0),
        )
        val daYun = DaYunInfo(true, "合成（顺行自戊午）", LocalDateTime.of(1977, 6, 20, 0, 0), listOf(
            DaYun(Pillar(HeavenlyStem.DING, EarthlyBranch.SI), 4, 1977, emptyList()),
            DaYun(Pillar(HeavenlyStem.BING, EarthlyBranch.CHEN), 14, 1987, emptyList()),
            DaYun(Pillar(HeavenlyStem.YI, EarthlyBranch.MAO), 24, 1997, emptyList()),
            DaYun(Pillar(HeavenlyStem.JIA, EarthlyBranch.YIN), 34, 2007, emptyList()),
            DaYun(Pillar(HeavenlyStem.WU, EarthlyBranch.CHEN), 44, 2017, emptyList()),
        ))
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("两刃身旺、官杀在局先用；癸合戊＝合官留杀清格，留下壬水为用",
            jb.quYongWay == JinBiaoQuYongWay.HE_LIU && jb.yongShenElement == Element.WATER)
        assertTrue("取用依据含有情豁免旺衰与留一即可",
            jb.quYongBasis.contains("合官留杀") && jb.quYongBasis.contains("有情豁免旺衰") && jb.quYongBasis.contains("留一即可"))

        // 乙卯甲寅印运制戊留壬——解病清格好运
        assertTrue("乙卯甲寅制戊留壬皆吉", listOf(2, 3).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
    }

    // ── 案卅六 · 癸巳 戊午 丙午 壬辰（1953-06-24 辰时，乾）——合而化火·无情 ──

    @Test
    fun `案卅六 乾 癸巳戊午丙午壬辰 戊癸化火无情 癸丑运前一事无成多灾多难`() {
        val bazi = chart(1953, 6, 24, 7)
        assertTrue(bazi.fullBazi.startsWith("癸巳 戊午 丙午 壬辰"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "逆行大运与师传表一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("丁巳(1959)", "丙辰(1969)", "乙卯(1979)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("癸根太弱（仅辰一弱根）火炎当令——戊癸化火＝合而化·无情",
            jb.quYongWay == JinBiaoQuYongWay.HUA_WU_QING && jb.yongShenElement == Element.WATER)
        assertTrue("取用依据含化火无情与一事无成",
            jb.quYongBasis.contains("化火") && jb.quYongBasis.contains("无情") && jb.quYongBasis.contains("一事无成"))

        // 癸丑运之前（丁巳丙辰乙卯甲寅）皆凶；癸丑壬子帮官方转
        assertTrue("癸丑前四步皆凶", listOf(0, 1, 2, 3).all { jb.yuns[it].verdict == JinBiaoVerdict.XIONG })
        assertTrue("癸丑壬子帮官方转吉", listOf(4, 5).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
    }

    // ── 案卅七 · 戊申 癸亥 丙午 壬辰（1968-12-02 辰时，乾）——合去混杂·有情 ──

    @Test
    fun `案卅七 乾 戊申癸亥丙午壬辰 身弱官杀混杂 戊合去癸留壬 有情豁免旺衰`() {
        val bazi = chart(1968, 12, 2, 7)
        assertTrue(bazi.fullBazi.startsWith("戊申 癸亥 丙午 壬辰"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运与师传表一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("甲子(1970)", "乙丑(1980)", "丙寅(1990)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("身弱官杀混杂、戊合去癸——合去混杂有情，留壬为用",
            jb.quYongWay == JinBiaoQuYongWay.HE_LIU && jb.yongShenElement == Element.WATER)
        assertTrue("取用依据含混杂两律与豁免",
            jb.quYongBasis.contains("不喜混杂") && jb.quYongBasis.contains("混杂能加力") && jb.quYongBasis.contains("不用考虑"))

        // 食干被官干合住——不能制杀（案卅七戒）
        assertTrue("非食制路径（食在合中）", jb.quYongWay != JinBiaoQuYongWay.ZHI_SHA)
    }

    // ── 案卅八 · 壬申 丁未 丁未 癸卯（1992-07-30 卯时，乾）——比肩合官·不宜合 ──

    @Test
    fun `案卅八 乾 壬申丁未丁未癸卯 身旺通两根 比肩合去壬官不宜合 幸坐申不化金水补`() {
        val bazi = chart(1992, 7, 30, 5)
        assertTrue(bazi.fullBazi.startsWith("壬申 丁未 丁未 癸卯"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运与师传表一致",
            daYun.yuns.take(3).map { it.pillar.fullName } == listOf("戊申", "己酉", "庚戌"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("丁通两未根＋卯印——身旺；身旺官杀先用——财滋弱官（壬癸）",
            jb.strength == DayStrength.STRONG && jb.quYongWay == JinBiaoQuYongWay.PEI_CAI && jb.yongShenElement == Element.WATER)
        assertTrue("取用依据含不宜合与合而不化",
            jb.quYongBasis.contains("不宜合") && jb.quYongBasis.contains("合而不化"))

        // 合而不化——走一路金水运把官杀补起来
        assertTrue("庚戌辛亥壬子癸丑金水四步皆吉", listOf(2, 3, 4, 5).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
    }

    // ── 案卅九 · 戊午 癸亥 丙戌 壬辰（1978-11-20 辰时，乾）——合去混杂·有情有格局 ──

    @Test
    fun `案卅九 乾 戊午癸亥丙戌壬辰 身弱混杂戊去一官 化不化不论有情有格局 帮身运当官`() {
        val bazi = chart(1978, 11, 20, 7)
        assertTrue(bazi.fullBazi.startsWith("戊午 癸亥 丙戌 壬辰"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运与师传表一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("甲子(1984)", "乙丑(1994)", "丙寅(2004)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("身弱官杀混杂、戊去一癸——合去混杂有情",
            jb.quYongWay == JinBiaoQuYongWay.HE_LIU && jb.yongShenElement == Element.WATER)
        assertTrue("化不化不论——有情就是有格局，不在命局旺弱",
            jb.quYongBasis.contains("化不化不论") && jb.quYongBasis.contains("不在于命局旺弱"))

        // 虽仍身弱，八字有格局——走帮身运（印比）也当官
        assertTrue("甲子乙丑印运当官吉", listOf(0, 1).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
        assertTrue("丙寅比运帮身当官吉", jb.yuns[2].verdict == JinBiaoVerdict.JI)
    }

    // ── 案四十 · 甲辰 己巳 戊辰 乙卯（1964-05-19 卯时，乾）——去杀留官·取清 ──

    @Test
    fun `案四十 乾 甲辰己巳戊辰乙卯 官杀通三根混杂重 己合去甲取清 依然当官`() {
        val bazi = chart(1964, 5, 19, 5)
        assertTrue(bazi.fullBazi.startsWith("甲辰 己巳 戊辰 乙卯"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运与师传表一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("庚午(1970)", "辛未(1980)", "壬申(1990)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("官杀通三根（卯辰辰）两透混杂、己劫合去甲杀——去杀留官取清",
            jb.quYongWay == JinBiaoQuYongWay.HE_LIU && jb.yongShenElement == Element.WOOD)
        assertTrue("取用依据含取清与依然当官", jb.quYongBasis.contains("取清") && jb.quYongBasis.contains("依然当官"))
    }

    // ── 案四十一 · 丙辰 辛卯 庚申 丁丑（1976-03-09 丑时，乾）——合去病神·有情 ──

    @Test
    fun `案四十一 乾 丙辰辛卯庚申丁丑 丙辛合去劫解放卯财有情 用丁官喜卯 南方火运双功当官`() {
        val bazi = chart(1976, 3, 9, 1)
        assertTrue(bazi.fullBazi.startsWith("丙辰 辛卯 庚申 丁丑"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运与师传表一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("壬辰(1985)", "癸巳(1995)", "甲午(2005)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("土金两旺身旺、官杀优先——财滋弱官（用丁留丙去）",
            jb.quYongWay == JinBiaoQuYongWay.PEI_CAI && jb.yongShenElement == Element.FIRE)
        assertTrue("取用依据含怕劫护财、合去病神有情、火克金卫护",
            jb.quYongBasis.contains("怕劫克财") && jb.quYongBasis.contains("合去病神") && jb.quYongBasis.contains("卫护"))

        // 南方火运双功：助官＋克金卫卯——当官（甲午乙未火财连吉；癸巳干头伤官不论）
        assertTrue("甲午乙未南方火财皆吉", listOf(2, 3).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
    }

    // ── 案四十二 · 丙辰 辛卯 乙酉 庚辰（1976-04-03 辰时，坤）——旺中变弱·合杀留官 ──

    @Test
    fun `案四十二 坤 丙辰辛卯乙酉庚辰 辰酉化金旺中变弱 丙辛合杀留官有情 地级市领导夫团长`() {
        val bazi = chart(1976, 4, 3, 7)
        assertTrue(bazi.fullBazi.startsWith("丙辰 辛卯 乙酉 庚辰"))
        val daYun = DaYunCalculator.compute(bazi, Gender.FEMALE)
        assertTrue(
            "逆行大运与师传表一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("庚寅(1985)", "己丑(1995)", "戊子(2005)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.FEMALE)
        assertTrue("两辰合酉化金、金旺克木冲木（卯酉冲）——旺中变弱", jb.strength == DayStrength.WEAK)
        assertTrue("丙辛合杀留官——合去混杂有情有格局",
            jb.quYongWay == JinBiaoQuYongWay.HE_LIU && jb.yongShenElement == Element.METAL)
        assertTrue("取用依据含化不化不论", jb.quYongBasis.contains("化不化不论"))
        assertTrue("女命官星双得断（命主地级市领导、夫团长）", jb.duanYu.contains("官星双得"))
    }

    // ── 案四十三 · 丙寅 辛丑 乙亥 庚辰（1987-01-26 辰时，坤）——辰水辩证·合杀有情 ──

    @Test
    fun `案四十三 坤 丙寅辛丑乙亥庚辰 丑辰两土贴亥辰克水印伤 丙合七杀寅亥化木 合掉一个有情`() {
        val bazi = chart(1987, 1, 26, 7)
        assertTrue(bazi.fullBazi.startsWith("丙寅 辛丑 乙亥 庚辰"))
        val daYun = DaYunCalculator.compute(bazi, Gender.FEMALE)
        assertTrue(
            "逆行大运与师传表一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("庚子(1993)", "己亥(2003)", "戊戌(2013)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.FEMALE)
        assertTrue("丑月财当令身弱、官杀混杂——丙合辛杀留庚官，合掉一个有情",
            jb.quYongWay == JinBiaoQuYongWay.HE_LIU && jb.yongShenElement == Element.METAL)
        assertTrue("取用依据含辰水辩证（火土多辰克水）与寅亥化木帮身",
            jb.quYongBasis.contains("辰水辩证") && jb.quYongBasis.contains("必克水") && jb.quYongBasis.contains("寅亥化木"))
    }

    // ── 案四十四 · 壬辰 壬子 丙寅 癸巳（合成盘，乾）——官杀混杂格·杀旺用印 ──

    @Test
    fun `案四十四 乾 壬辰壬子丙寅癸巳 三壬一癸杀旺混杂加力 坐下寅印一仁可化 帮身运当官`() {
        val bazi = BaziResult(
            Pillar(HeavenlyStem.REN, EarthlyBranch.CHEN), Pillar(HeavenlyStem.REN, EarthlyBranch.ZI),
            Pillar(HeavenlyStem.BING, EarthlyBranch.YIN), Pillar(HeavenlyStem.GUI, EarthlyBranch.SI),
            LocalDateTime.of(1952, 12, 20, 9, 0),
        )
        val daYun = DaYunInfo(true, "合成（顺行自壬子）", LocalDateTime.of(1956, 12, 20, 0, 0), listOf(
            DaYun(Pillar(HeavenlyStem.GUI, EarthlyBranch.CHOU), 4, 1956, emptyList()),  // 癸丑
            DaYun(Pillar(HeavenlyStem.JIA, EarthlyBranch.YIN), 14, 1966, emptyList()),  // 甲寅印
            DaYun(Pillar(HeavenlyStem.YI, EarthlyBranch.MAO), 24, 1976, emptyList()),   // 乙卯印
        ))
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("杀旺混杂（更得用印）——杀重用印格，用神取印（寅木）",
            jb.strength == DayStrength.WEAK && jb.quYongWay == JinBiaoQuYongWay.HUA_SHA && jb.yongShenElement == Element.WOOD)
        assertTrue("众杀混行一仁可化＋坐下有情入据",
            jb.quYongBasis.contains("众杀混行，一仁可化") && jb.quYongBasis.contains("情分"))

        // 走一路帮身（印比）——就要当官
        assertTrue("甲寅乙卯印运帮身皆吉", listOf(1, 2).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
    }

    // ── 案四十五 · 甲子 乙亥 己巳 丁卯（1984-12-01 卯时，乾）——亥巳冲毁印·卯半救·寅运绝处逢生 ──

    @Test
    fun `案四十五 乾 甲子乙亥己巳丁卯 官杀混杂必用印 亥巳冲毁印根卯半救 寅运合去冲神绝处逢生`() {
        val bazi = chart(1984, 12, 1, 5)
        assertTrue(bazi.fullBazi.startsWith("甲子 乙亥 己巳 丁卯"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运与师传表一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("丙子(1986)", "丁丑(1996)", "戊寅(2006)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("财杀两旺官杀混杂——必用坐下印化之以德（官杀混杂必要用印绶化之以德）",
            jb.quYongWay == JinBiaoQuYongWay.HUA_SHA && jb.yongShenElement == Element.FIRE)
        assertTrue("亥巳贴身冲无通关——支神只以冲为重毁用神印根",
            jb.health.damages.any { it.contains("支神只以冲为重") })
        assertTrue("卯生巳——家里还有生的把印星补起来（半救）",
            jb.health.supports.any { it.contains("半救") && it.contains("卯") })

        // 戊寅运：寅亥化木合去冲神——巳火绝处逢生，彻底有情（戊干泄火截脚，支神为重）
        assertTrue("寅运合去亥冲神——绝处逢生非凶且注明",
            jb.yuns[2].verdict != JinBiaoVerdict.XIONG && jb.yuns[2].items.any { it.action.contains("绝处逢生") })
    }

    // ── 案四十六 · 辛卯 戊戌 丙辰 壬辰（2011-10-28 辰时，乾）——制杀太过·公务员没当官 ──

    @Test
    fun `案四十六 乾 辛卯戊戌丙辰壬辰 戊通四支制杀太过 喜印克食护杀 杀太弱公务员没当官`() {
        val bazi = chart(2011, 10, 28, 7)
        assertTrue(bazi.fullBazi.startsWith("辛卯 戊戌 丙辰 壬辰"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "逆行大运与师传表一致",
            daYun.yuns.take(3).map { it.pillar.fullName } == listOf("丁酉", "丙申", "乙未"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("戌月土旺食伤制杀、杀仅存余气（壬坐辰藏癸）——用印（卯，木克土）护杀生身",
            jb.quYongWay == JinBiaoQuYongWay.HUA_SHA && jb.yongShenElement == Element.WOOD)
        assertTrue("取用依据含制杀太过与公务员没当官",
            jb.quYongBasis.contains("制杀太过") && jb.quYongBasis.contains("护卫七杀") && jb.quYongBasis.contains("公务员"))
    }

    // ── 案四十七 · 甲寅 戊辰 壬辰 壬寅（1974-04-21 寅时，乾）——制杀太过兼过泄·印运护杀帮身 ──

    @Test
    fun `案四十七 乾 甲寅戊辰壬辰壬寅 春木旺土虚食伤得局过泄 制杀太过 印运护杀帮身`() {
        val bazi = chart(1974, 4, 21, 3)
        assertTrue(bazi.fullBazi.startsWith("甲寅 戊辰 壬辰 壬寅"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运与师传表一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("己巳(1979)", "庚午(1989)", "辛未(1999)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("官杀（土）有力食伤（木）得局——食神制杀格", jb.quYongWay == JinBiaoQuYongWay.ZHI_SHA && jb.yongShenElement == Element.WOOD)
        assertTrue("取用依据含制杀太过兼过泄与印两用（护杀帮身）",
            jb.quYongBasis.contains("制杀太过") && jb.quYongBasis.contains("护杀") && jb.quYongBasis.contains("帮身"))

        // 一定要走印运——庚午辛未壬申癸酉（金）：护杀帮身，非凶
        assertTrue("庚午辛未壬申癸酉金印运非凶", listOf(1, 2, 3, 4).all { jb.yuns[it].verdict != JinBiaoVerdict.XIONG })
    }

    // ── 案四十八 · 丙寅 壬辰 庚辰 壬午（1986-04-06 午时，乾）——身旺食制杀旺官二代 ──

    @Test
    fun `案四十八 乾 丙寅壬辰庚辰壬午 印得局身旺 食伤制杀杀旺可制 官二代`() {
        val bazi = chart(1986, 4, 6, 11)
        assertTrue(bazi.fullBazi.startsWith("丙寅 壬辰 庚辰 壬午"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运与师传表一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("癸巳(1996)", "甲午(2006)", "乙未(2016)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("印得局（两辰＋火生土）身旺——坐下印者不翻弱", jb.strength == DayStrength.STRONG)
        assertTrue("杀旺（丙寅午）＋食有力（壬壬）——食伤制杀，用神取食神（水）",
            jb.quYongWay == JinBiaoQuYongWay.ZHI_SHA && jb.yongShenElement == Element.WATER)
        assertTrue("取用依据含杀旺可制官贵与普通人之别",
            jb.quYongBasis.contains("杀旺可制") && jb.quYongBasis.contains("官贵"))

        // 金支生用神（干火盖头→干支分论非凶）；火运（杀党）非吉
        assertTrue("丙申丁酉金支生用非凶", listOf(3, 4).all { jb.yuns[it].verdict != JinBiaoVerdict.XIONG })
        assertTrue("甲午乙未火运非吉", listOf(1, 2).all { jb.yuns[it].verdict != JinBiaoVerdict.JI })
    }

    // ── 案四十九 · 壬戌 己酉 戊戌 乙卯（1982-09-12 卯时，乾）——土金伤官反喜官 ──

    @Test
    fun `案四十九 乾 壬戌己酉戊戌乙卯 两戌厚土身旺伤官生财被劫 官克比劫用神转官当官显贵`() {
        val bazi = chart(1982, 9, 12, 5)
        assertTrue(bazi.fullBazi.startsWith("壬戌 己酉 戊戌 乙卯"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运与师传表一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("庚戌(1991)", "辛亥(2001)", "壬子(2011)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("酉月两戌厚土＋己透比劫——身旺（月干比透＋坐下比劫本气根补臂）", jb.strength == DayStrength.STRONG)
        assertTrue("伤官生财被劫、乙卯官克比劫救财——用神转官（伤官可以见官）",
            jb.quYongWay == JinBiaoQuYongWay.SHANG_JIAN_GUAN && jb.yongShenElement == Element.WOOD)
        assertTrue("取用依据含破口诀与用神中心论",
            jb.quYongBasis.contains("伤官格可以见官") && jb.quYongBasis.contains("用神转官") && jb.quYongBasis.contains("土金伤官"))

        // 壬子泄金生木（水生木帮用）吉——当官显贵；癸丑丑生酉克卯病大非吉
        assertTrue("壬子水运泄金生木皆吉", jb.yuns[2].verdict == JinBiaoVerdict.JI)
        assertTrue("癸丑丑生酉克用神卯非吉", jb.yuns[3].verdict != JinBiaoVerdict.JI)
    }

    // ── 案五十 · 戊子 辛酉 戊午 丁巳（1948-09-30 巳时，乾）——枭神夺食不可见官 ──

    @Test
    fun `案五十 乾 戊子辛酉戊午丁巳 身旺坐禄刃印贴生 枭神夺食财护为药 官生枭见官祸百端`() {
        val bazi = chart(1948, 9, 30, 9)
        assertTrue(bazi.fullBazi.startsWith("戊子 辛酉 戊午 丁巳"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运与师传表一致",
            daYun.yuns.take(3).map { it.pillar.fullName } == listOf("壬戌", "癸亥", "甲子"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("坐午刃巳禄＋丁巳印贴生——身旺（坐下印克月令食伤补臂）", jb.strength == DayStrength.STRONG)
        assertTrue("身旺无官食伤泄秀——用神取食伤（金）", jb.quYongWay == JinBiaoQuYongWay.SHI_SHENG_CAI && jb.yongShenElement == Element.METAL)
        assertTrue("取用依据含枭神夺食不可见官", jb.quYongBasis.contains("枭神夺食") && jb.quYongBasis.contains("不可见官"))

        // 亥子丑坎卦（水财运克忌神火印）好运；丙寅丁卯官星来凶（官生枭夺食）
        assertTrue("癸亥水运好运", jb.yuns[1].verdict == JinBiaoVerdict.JI)
        assertTrue("丙寅丁卯官运凶", listOf(4, 5).all { jb.yuns[it].verdict == JinBiaoVerdict.XIONG })
        assertTrue("官运标注见官为祸百端", jb.yuns[4].items.any { it.action.contains("祸百端") })
    }

    // ── 案五十一 · 辛酉 丁酉 戊午 辛酉（1981-10-07 酉时，乾）——伤官配印喜见官忌见财 ──

    @Test
    fun `案五十一 乾 辛酉丁酉戊午辛酉 三酉泄体过身弱伤官配印 官生印喜见官 财坏印大忌`() {
        val bazi = chart(1981, 10, 7, 17)
        assertTrue(bazi.fullBazi.startsWith("辛酉 丁酉 戊午 辛酉"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运与师传表一致",
            daYun.yuns.take(3).map { it.pillar.fullName } == listOf("丙申", "乙未", "甲午"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("三酉泄体过——身弱伤官配印（用神丁午火印）", jb.strength == DayStrength.WEAK && jb.quYongWay == JinBiaoQuYongWay.FU_SHEN && jb.yongShenElement == Element.FIRE)
        assertTrue("喜见官不可见财", jb.quYongBasis.contains("喜见官") && jb.quYongBasis.contains("不可见财"))
        // 甲午乙未印运吉；癸巳巳印亦吉；壬辰财坏印非纯吉（干支分论）
        assertTrue("甲午乙未印运皆吉", listOf(1, 2).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
        assertTrue("癸巳巳印吉", jb.yuns[3].verdict == JinBiaoVerdict.JI)
        assertTrue("壬辰财坏印非吉", jb.yuns[4].verdict != JinBiaoVerdict.JI)
    }

    // ── 案五十二 · 癸亥 辛酉 戊申 己未（1983-09-17 未时，乾）——伤官用劫·祸百端正例 ──

    @Test
    fun `案五十二 乾 癸亥辛酉戊申己未 金水旺泄体过无印用劫 比劫夺财是好事 一路印比吉可以做官 乙卯填成亥卯未凶`() {
        val bazi = chart(1983, 9, 17, 14)
        assertTrue(bazi.fullBazi.startsWith("癸亥 辛酉 戊申 己未"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "阴年乾造逆行大运与师传表一致",
            daYun.yuns.take(4).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("庚申(1986)", "己未(1996)", "戊午(2006)", "丁巳(2016)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("酉月申酉辛金泄体过——身弱", jb.strength == DayStrength.WEAK)
        assertTrue("印只藏未中丁不算有印——伤官用劫，用神己未（土比劫）",
            jb.quYongWay == JinBiaoQuYongWay.SHANG_YONG_JIE && jb.yongShenElement == Element.EARTH)
        assertTrue("取用依据含用劫四要：夺财是好事、最怕见官、祸百端、配印之效",
            jb.quYongBasis.contains("比劫夺财是好事") && jb.quYongBasis.contains("最怕见官") &&
                jb.quYongBasis.contains("为祸百端") && jb.quYongBasis.contains("伤官配印之效"))

        // 庚申泄己未——没什么大事（毕竟没有被克）→中平；己未戊午丁巳丙辰一路帮比劫皆吉
        assertTrue("庚申泄用神非克——中平（没什么大事）", jb.yuns[0].verdict == JinBiaoVerdict.PING)
        assertTrue("己未戊午丁巳丙辰四步印比帮用神皆吉（用神得助可承载食伤生财做官）",
            listOf(1, 2, 3, 4).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
        // 乙卯：亥未拱木待卯，运支填成亥卯未——木旺克掉用神己未，出事（伤官见官为祸百端）
        assertTrue("乙卯运填成三合木局克用神——凶", jb.yuns[5].verdict == JinBiaoVerdict.XIONG)
        assertTrue("乙卯运标注三合成局出事", jb.yuns[5].items.any { it.action.contains("三合") && it.action.contains("出事") })
        assertTrue("甲寅官杀克比劫亦非吉", jb.yuns[6].verdict != JinBiaoVerdict.JI)
    }

    // ── 案五十三 · 己丑 辛未 丙寅 己丑（1949-08-04 丑时，乾）——伤官配印·丑未冲三层·去忌神 ──

    @Test
    fun `案五十三 乾 己丑辛未丙寅己丑 丑未冲三层旺中变弱 配印用寅木 干支异途病不贴克 丁卯丙寅去辛金当官`() {
        val bazi = chart(1949, 8, 4, 2)
        assertTrue(bazi.fullBazi.startsWith("己丑 辛未 丙寅 己丑"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "阴年乾造逆行大运与师传表一致",
            daYun.yuns.take(4).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("庚午(1958)", "己巳(1968)", "戊辰(1978)", "丁卯(1988)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("两丑冲未灭木火余气＋湿土晦火——旺中变弱身弱", jb.strength == DayStrength.WEAK)
        assertTrue("伤官配印——用神坐下寅木（印）",
            jb.quYongWay == JinBiaoQuYongWay.FU_SHEN && jb.yongShenElement == Element.WOOD)
        assertTrue("取用依据含丑未冲三层、旺中变弱、帮身、忌神潜伏、去忌神",
            jb.quYongBasis.contains("丑未冲三层") && jb.quYongBasis.contains("财坏印于支内") &&
                jb.quYongBasis.contains("旺中变弱") && jb.quYongBasis.contains("泄体过必须帮身") &&
                jb.quYongBasis.contains("忌神潜伏律") && jb.quYongBasis.contains("去忌神之运最好"))
        assertTrue("辛透干无本气根＋用神坐日支本气根——干支异途病不贴克，有格局行运不违背即好命",
            jb.qingZhuo == JinBiaoQingZhuo.QING && jb.qingZhuoBasis.contains("干支异途"))

        // 庚午己巳：干忌金耗土、支午巳比劫帮身——干支分论；丁卯丙寅去辛金忌神最好当官；甲子癸亥官生印吉
        assertTrue("庚午己巳干凶支帮身——干支分论", listOf(0, 1).all { jb.yuns[it].verdict == JinBiaoVerdict.GAN_ZHI_FEN })
        assertTrue("丁卯丙寅克去透干忌神辛——最好的运",
            listOf(3, 4).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
        assertTrue("丁丙标注去忌神", listOf(3, 4).all { i -> jb.yuns[i].items.any { it.action.contains("去忌神") } })
        assertTrue("甲子癸亥官生印皆吉", listOf(6, 7).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
    }

    // ── 案五十四 · 丙申 戊戌 丁卯 乙巳（1956-10-27 巳时，乾）——身旺食伤生财用财星·寅运死亡 ──

    @Test
    fun `案五十四 乾 丙申戊戌丁卯乙巳 卯戌合火身旺食伤生财 用神申金财 通关化劫无官配置不足 寅运比劫见根死亡`() {
        val bazi = chart(1956, 10, 27, 10)
        assertTrue(bazi.fullBazi.startsWith("丙申 戊戌 丁卯 乙巳"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运起运年与师传年标一致",
            daYun.yuns.take(4).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("己亥(1960)", "庚子(1970)", "辛丑(1980)", "壬寅(1990)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("卯戌六合化火叛向——身强（坐下印归禄帮身）", jb.strength == DayStrength.STRONG)
        assertTrue("财星有力（申本气根戌生金）——用神即财星申金",
            jb.quYongWay == JinBiaoQuYongWay.SHI_CAI_YONG_CAI && jb.yongShenElement == Element.METAL)
        assertTrue("取用依据含用财、通关化劫、官护财配置论",
            jb.quYongBasis.contains("用神肯定就是财星") && jb.quYongBasis.contains("通关化劫") &&
                jb.quYongBasis.contains("有官星就一直富") && jb.quYongBasis.contains("配置不足"))
        assertTrue("通关化病而水官全无——半浊半清（配置不足起伏大）",
            jb.qingZhuo == JinBiaoQingZhuo.BAN_QING && jb.qingZhuoBasis.contains("配置不足"))

        // 亥子平常（泄用）；辛丑丑生申金发财；壬寅寅戌拱火＋丙见根——死亡级大祸
        assertTrue("己亥庚子泄用平常——干支分论", listOf(0, 1).all { jb.yuns[it].verdict == JinBiaoVerdict.GAN_ZHI_FEN })
        assertTrue("辛丑运丑生申金——发财", jb.yuns[2].verdict == JinBiaoVerdict.JI)
        assertTrue("壬寅运比劫见根拱火克用——大凶（死亡级）", jb.yuns[3].verdict == JinBiaoVerdict.XIONG)
        assertTrue("寅运标注丙见根拱火通关被焚死亡级", jb.yuns[3].items.any { it.action.contains("见根") && it.action.contains("通关之神被焚") })
        assertTrue("寅运干壬官护财注记", jb.yuns[3].items.any { it.action.contains("官护财") })
    }

    // ── 案五十五 · 壬申 辛亥 辛酉 庚寅（1992-12-11 寅时·农历月界合成盘，乾）——破格→有情救格 ──

    private fun an55(): cn.xulun.bazi.model.BaziResult = cn.xulun.bazi.model.BaziResult(
        yearPillar = Pillar(HeavenlyStem.REN, EarthlyBranch.SHEN),
        monthPillar = Pillar(HeavenlyStem.XIN, EarthlyBranch.HAI),
        dayPillar = Pillar(HeavenlyStem.XIN, EarthlyBranch.YOU),
        hourPillar = Pillar(HeavenlyStem.GENG, EarthlyBranch.YIN),
        birthDateTime = LocalDateTime.of(1992, 12, 11, 4, 0),
    )

    @Test
    fun `案五十五 乾 壬申辛亥辛酉庚寅 三金两禄身旺用财 财被围克已破格 寅亥化木绝处逢生有情 北方东方水木发大财`() {
        // 排盘口径：节气界壬申年亥月无辛酉日（1992/1932皆无）；1992-12-11农历十月十八仍在亥月——农历月界合成盘
        val bazi = an55()
        assertTrue(bazi.fullBazi == "壬申 辛亥 辛酉 庚寅")
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运与师传表一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("壬子(2001)", "癸丑(2011)", "甲寅(2021)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("三金两禄——弱中变旺身旺", jb.strength == DayStrength.STRONG)
        assertTrue("身旺食伤喜生财——用神财星寅木",
            jb.quYongWay == JinBiaoQuYongWay.SHI_CAI_YONG_CAI && jb.yongShenElement == Element.WOOD)
        assertTrue("取用依据含破格→有情救格与救财两途",
            jb.quYongBasis.contains("已破格") && jb.quYongBasis.contains("绝处逢生") &&
                jb.quYongBasis.contains("食伤能把财彻底救起来也行"))
        assertTrue("寅亥化木补用神入 supports",
            jb.health.supports.any { it.contains("寅亥合化木") && it.contains("化局补用神") })
        assertTrue("破格→有情救格——清（有情就有格局）",
            jb.qingZhuo == JinBiaoQingZhuo.QING && jb.qingZhuoBasis.contains("有情救格"))

        // 北方坎（壬子通关之神到——通关更力反吉）→东方震（甲寅乙卯食伤生财）——一路水木发大财
        assertTrue("壬子通关之神到反吉", jb.yuns[0].verdict == JinBiaoVerdict.JI)
        assertTrue("甲寅乙卯食伤生财发财", listOf(2, 3).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
    }

    // ── 案五十六 · 壬子 戊申 戊戌 辛酉（1972-09-04 酉时，乾）——三会化劫·用神无力 ──

    @Test
    fun `案五十六 乾 壬子戊申戊戌辛酉 申酉戌三会把比劫都化了 身弱用劫用神无力格局不高 金水穷困 甲寅七杀死亡`() {
        val bazi = chart(1972, 9, 4, 18)
        assertTrue(bazi.fullBazi.startsWith("壬子 戊申 戊戌 辛酉"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运起运年与师传年标一致",
            daYun.yuns.take(4).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("己酉(1973)", "庚戌(1983)", "辛亥(1993)", "壬子(2003)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("申酉戌三会金局泄体——身弱", jb.strength == DayStrength.WEAK)
        assertTrue("身弱用神肯定是比劫（土）", jb.quYongWay == JinBiaoQuYongWay.SHANG_YONG_JIE && jb.yongShenElement == Element.EARTH)
        assertTrue("三会化劫入据：把比劫都化了、用神无力、格局不高、财越旺越穷",
            jb.quYongBasis.contains("三会化劫") && jb.quYongBasis.contains("把比劫都化了") &&
                jb.quYongBasis.contains("用神无力") && jb.quYongBasis.contains("财越旺他越穷"))
        assertTrue("戌申随三会局化尽——用神无气受伤",
            jb.health.level == JinBiaoHealth.WU_QI && jb.health.rootNote.contains("随三会局化"))
        assertTrue("用神无力格局不高——浊档（穷困潦倒，唯火土小富）",
            jb.qingZhuo == JinBiaoQingZhuo.MAN_ZHUO && jb.qingZhuoBasis.contains("用神无力") && jb.qingZhuoBasis.contains("小富"))

        // 西北金水穷困潦倒；甲寅乙卯七杀克比劫用神——死亡；丙辰火土帮比劫可小富
        assertTrue("辛亥壬子西北金水——穷困潦倒（泄耗不作中平）", listOf(2, 3).all { jb.yuns[it].verdict == JinBiaoVerdict.XIONG })
        assertTrue("甲寅运七杀死亡", jb.yuns[5].verdict == JinBiaoVerdict.XIONG)
        assertTrue("甲寅运标注七杀死亡", jb.yuns[5].items.any { it.action.contains("死亡") })
        assertTrue("丙辰火土帮比劫——可小富", jb.yuns[7].verdict == JinBiaoVerdict.JI)
    }

    // ── 案五十七 · 庚辰 乙酉 丁丑 乙巳（1940-10-01 巳时，乾）——昆仑让位·财得局坏印清枯 ──

    @Test
    fun `案五十七 乾 庚辰乙酉丁丑乙巳 金局得局然两印贴身有禄不从 用印财得局坏印 财有多旺他就有多穷 印运温饱食伤财运负债`() {
        val bazi = chart(1940, 10, 1, 10)
        assertTrue(bazi.fullBazi.startsWith("庚辰 乙酉 丁丑 乙巳"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运与师传表一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("丙戌(1943)", "丁亥(1953)", "戊子(1963)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("金局得局而两印贴身＋有禄——肯定不是从格：不作昆仑正常分析", jb.quYongWay != JinBiaoQuYongWay.KUN_LUN)
        assertTrue("身弱首要用印——用神乙木（两印贴身）",
            jb.strength == DayStrength.WEAK && jb.quYongWay == JinBiaoQuYongWay.FU_SHEN && jb.yongShenElement == Element.WOOD)
        assertTrue("财得局坏印入据：财有多旺他就有多穷、温饱、身无分文负债",
            jb.quYongBasis.contains("财星得局坏印") && jb.quYongBasis.contains("财有多旺他就有多穷") &&
                jb.quYongBasis.contains("温饱") && jb.quYongBasis.contains("身无分文"))
        assertTrue("清枯第二型（财坏印——印运温饱食伤财运负债）",
            jb.qingZhuo == JinBiaoQingZhuo.QING_KU && jb.qingZhuoBasis.contains("财坏"))

        // 丙戌己丑食伤生财之运——身无分文负债（凶）；庚寅辛卯干财凶支印温饱（分论）
        assertTrue("丙戌己丑食伤生财运负债——凶", listOf(0, 3).all { jb.yuns[it].verdict == JinBiaoVerdict.XIONG })
        assertTrue("庚寅辛卯干财坏印凶、支印温饱——干支分论", listOf(4, 5).all { jb.yuns[it].verdict == JinBiaoVerdict.GAN_ZHI_FEN })
    }

    // ── 案五十八 · 庚申 己卯 癸卯 乙卯（1980-03-31 卯时，乾）——子女章首例·木坚金缺无儿有女 ──

    @Test
    fun `案五十八 乾 庚申己卯癸卯乙卯 三卯食伤得局身太衰用神庚申印 印为儿木坚金缺生女儿 南方火运更无儿`() {
        val bazi = chart(1980, 3, 31, 6)
        assertTrue(bazi.fullBazi.startsWith("庚申 己卯 癸卯 乙卯"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运起运年与师传年标一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("庚辰(1981)", "辛巳(1991)", "壬午(2001)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("三卯木得局食伤过泄＋己杀克——克泄交加身太衰", jb.strength == DayStrength.WEAK)
        assertTrue("印透坐禄有力可依——不作昆仑，肯定要用印星帮身（用神庚申）",
            jb.quYongWay != JinBiaoQuYongWay.KUN_LUN &&
                jb.quYongWay == JinBiaoQuYongWay.HUA_SHA && jb.yongShenElement == Element.METAL)

        // 子女章：身衰印做儿——印星就是我的儿子；木坚金缺剩一点气——没儿子，可生闺女
        val zn = jb.ziNv!!
        assertTrue("无儿有女（四情况之三）", zn.qingKuang == cn.xulun.bazi.model.JinBiaoZiNvQingKuang.WU_ER_YOU_NV)
        assertTrue("子星＝正印庚申（印星就是我的儿子）", zn.ziXing == cn.xulun.bazi.model.TenGod.ZHENG_YIN && zn.ziEl == Element.METAL)
        assertTrue("判据含印为儿、木坚金缺、生闺女",
            zn.basis.contains("印为儿") && zn.basis.contains("木坚金缺") && zn.basis.contains("可生闺女"))
        assertTrue("运注火为子星克神——南方运生不出儿子", zn.basis.contains("辛巳") && zn.basis.contains("壬午"))

        // 庚辰金旺早年出身不错；辛巳巳火克印（学历不高）；壬午午财坏印凶（二十多岁南方运更生不出儿子）
        assertTrue("庚辰金旺帮印——早年吉", jb.yuns[0].verdict == JinBiaoVerdict.JI)
        assertTrue("辛巳支火克印非吉", jb.yuns[1].verdict != JinBiaoVerdict.JI)
        assertTrue("壬午午财坏印——凶", jb.yuns[2].verdict == JinBiaoVerdict.XIONG)
    }

    // ── 案五十九 · 癸亥 乙卯 壬戌 甲辰（1983-04-04 辰时，乾）——子女章次例·凑合生子终损 ──

    @Test
    fun `案五十九 乾 癸亥乙卯壬戌甲辰 克泄交加无印用比劫癸亥 比劫子星被克泄太重 凑合生子终损 生女儿百分之百 财富取决于用神`() {
        val bazi = chart(1983, 4, 4, 8)
        assertTrue(bazi.fullBazi.startsWith("癸亥 乙卯 壬戌 甲辰"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "阴年乾造逆行大运起运年与师传年标一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("甲寅(1992)", "癸丑(2002)", "壬子(2012)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("卯月木旺泄体＋坐两杀克泄交加——百分之百身弱", jb.strength == DayStrength.WEAK)
        assertTrue("印唯戌中辛孤藏不算有印——用神比劫癸亥（财富值取决于比劫之力）",
            jb.quYongWay == JinBiaoQuYongWay.SHANG_YONG_JIE && jb.yongShenElement == Element.WATER)
        assertTrue("取用依据含财富取决于用神论",
            jb.quYongBasis.contains("财富值取决于用神比劫之力") && jb.quYongBasis.contains("用神好财才是真的好"))

        // 子女章：无印比劫当儿——子星被两杀克＋卯泄＋亥卯会泄根，克泄太重：凑合生子终损＋生女儿
        val zn = jb.ziNv!!
        assertTrue("无儿有女（凑合生子终损型）", zn.qingKuang == cn.xulun.bazi.model.JinBiaoZiNvQingKuang.WU_ER_YOU_NV)
        assertTrue("子星＝劫财癸亥", zn.ziXing == cn.xulun.bazi.model.TenGod.JIE_CAI && zn.ziEl == Element.WATER)
        assertTrue("判据含凑合生子必损、墓中送出、生女儿",
            zn.basis.contains("凑合生出来儿子") && zn.basis.contains("损子") &&
                zn.basis.contains("墓中子星要送出") && zn.basis.contains("生女儿"))

        // 壬子运帮身把比劫帮起来（2012壬辰年生子窗口）；甲寅泄子星早年平常
        assertTrue("壬子运帮身——吉（凑合生子之窗）", jb.yuns[2].verdict == JinBiaoVerdict.JI)
        assertTrue("甲寅运泄子星——平（早年无子）", jb.yuns[0].verdict == JinBiaoVerdict.PING)
    }

    // ── 案六十一 · 壬寅 癸丑 戊午 丙辰（1963-01-15 辰时，坤）——子女章四例·土得局夺财生女 ──

    @Test
    fun `案六十一 坤 壬寅癸丑戊午丙辰 土得局身旺喜财坏印用神水财 地支不载没生儿子 戌运火局生闺女 金水运高学历`() {
        val bazi = chart(1963, 1, 15, 8)
        assertTrue(bazi.fullBazi.startsWith("壬寅 癸丑 戊午 丙辰"))
        val daYun = DaYunCalculator.compute(bazi, Gender.FEMALE)
        assertTrue(
            "阳年坤造逆行大运起运年与师传年标一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("壬子(1966)", "辛亥(1976)", "庚戌(1986)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.FEMALE)
        assertTrue("丑月土得局＋午刃丙印——身旺", jb.strength == DayStrength.STRONG)
        assertTrue("官不透而弱不用官——身旺印重喜财坏印，用神壬癸水财（寅木不旺，拿金水取用）",
            jb.quYongWay == JinBiaoQuYongWay.HUAI_YIN && jb.yongShenElement == Element.WATER)

        // 子女章：身旺财为子——土得局夺财地支不载：没生儿子；亥运可生儿；戌运火局生闺女
        val zn = jb.ziNv!!
        assertTrue("无儿有女", zn.qingKuang == cn.xulun.bazi.model.JinBiaoZiNvQingKuang.WU_ER_YOU_NV)
        assertTrue("子星＝偏财水", zn.ziXing == cn.xulun.bazi.model.TenGod.PIAN_CAI && zn.ziEl == Element.WATER)
        assertTrue("判据含地支不载、没生儿子、生女儿、火土局生女",
            zn.basis.contains("地支不载") && zn.basis.contains("没生儿子") &&
                zn.basis.contains("生女儿") && zn.basis.contains("必是闺女"))

        // 壬子辛亥金水旺——高学历（医生）；戌运火土局克用——生女之运非吉
        assertTrue("壬子辛亥金水运皆吉（高学历应期）", listOf(0, 1).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
        assertTrue("庚戌运戌支克用非吉（火局生女之运）", jb.yuns[2].verdict != JinBiaoVerdict.JI)
    }

    // ── 案六十二 · 甲辰 乙亥 丙寅 辛卯（1964-11-13 卯时，乾）——子女章五例·身旺无依 ──

    @Test
    fun `案六十二 乾 甲辰乙亥丙寅辛卯 寅卯辰三会印局身旺无依克耗泄全废 财虚透未被克生育能力在 丑运得子运过损子判前妻`() {
        val bazi = chart(1964, 11, 13, 6)
        assertTrue(bazi.fullBazi.startsWith("甲辰 乙亥 丙寅 辛卯"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运起运年与师传年标一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("丙子(1972)", "丁丑(1982)", "戊寅(1992)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("寅卯辰三会印局＋亥寅化木——身旺", jb.strength == DayStrength.STRONG)
        assertTrue("官被化尽、财不载无根、食伤根随局化——身旺无依（喜用全废一事无成）",
            jb.quYongWay == JinBiaoQuYongWay.SHEN_WANG_WU_YI && jb.yongShenElement == Element.METAL)
        assertTrue("取用依据含身旺无依、一事无成、帮不起来论",
            jb.quYongBasis.contains("身旺无依") && jb.quYongBasis.contains("一事无成") && jb.quYongBasis.contains("帮不起来"))

        // 子女章：财虚透没被比劫克——生育能力在（贫富两轨）；丑运财得根得子；运过损子（离婚判前妻即破解）
        val zn = jb.ziNv!!
        assertTrue("必有儿（生育能力在）", zn.qingKuang == cn.xulun.bazi.model.JinBiaoZiNvQingKuang.BI_YOU_ER)
        assertTrue("子星＝正财辛金", zn.ziXing == cn.xulun.bazi.model.TenGod.ZHENG_CAI && zn.ziEl == Element.METAL)
        assertTrue("判据含贫富两轨、走财得根运得子、离婚判对方破解",
            zn.basis.contains("贫富与子嗣两轨") && zn.basis.contains("不代表穷人就生不了儿子") &&
                zn.basis.contains("走财得根的大运是得儿子的") && zn.basis.contains("丁丑") &&
                zn.basis.contains("离婚判给对方"))
    }

    // ── 案六十三 · 丙午 辛丑 乙亥 丁亥（1967-01-11 亥时，乾）——子女章六例·调候优先 ──

    @Test
    fun `案六十三 乾 丙午辛丑乙亥丁亥 寒局用暖丙午有根 走东南生火不用分析生克 必然有儿子有孙子没有缺陷`() {
        val bazi = chart(1967, 1, 11, 22)
        assertTrue(bazi.fullBazi.startsWith("丙午 辛丑 乙亥 丁亥"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运起运年与师传年标一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("壬寅(1975)", "癸卯(1985)", "甲辰(1995)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("丑月寒冬两亥水——寒局；暖须有气有根：丙午有根——用暖（调候）",
            jb.quYongWay == JinBiaoQuYongWay.TIAO_HOU && jb.yongShenElement == Element.FIRE)

        // 调候优先：走东南暖运就是好——必然有儿子有孙子，没有缺陷
        val zn = jb.ziNv!!
        assertTrue("调候局必有儿（无缺陷有儿有孙）", zn.qingKuang == cn.xulun.bazi.model.JinBiaoZiNvQingKuang.BI_YOU_ER)
        assertTrue("判据含调候优先、不用分析生克、必然有儿子有孙子",
            zn.basis.contains("调候优先") && zn.basis.contains("不用分析生克") && zn.basis.contains("必然有儿子、有孙子"))

        // 东南暖运皆吉（什么都有什么都顺）；辅证：坐印足以食伤制杀——食伤偏弱帮起即好
        assertTrue("甲辰乙巳丙午丁未东南暖运皆吉", listOf(2, 3, 4, 5).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
    }

    // ── 案六十四（＝案四十二盘复看子女）· 丙辰 辛卯 乙酉 庚辰（1976-04-03 辰时，坤）──

    @Test
    fun `案六十四 坤 丙辰辛卯乙酉庚辰复看子女 金贴身冲比劫子星根 没儿子只能生女儿 富贵与子嗣两轨`() {
        val bazi = chart(1976, 4, 3, 7)
        val daYun = DaYunCalculator.compute(bazi, Gender.FEMALE)
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.FEMALE)

        // 官杀欺衰无印——比劫子星之根被官杀贴身冲克（酉贴卯＋辰酉化金力大）——没儿子只能生女儿
        val zn = jb.ziNv!!
        assertTrue("无儿有女", zn.qingKuang == cn.xulun.bazi.model.JinBiaoZiNvQingKuang.WU_ER_YOU_NV)
        assertTrue("子星＝比肩木（身衰印做儿而无印——比劫线）", zn.ziXing == cn.xulun.bazi.model.TenGod.BI_JIAN && zn.ziEl == Element.WOOD)
        assertTrue("判据含贴身冲克、没儿子只能生女儿、没有这个能力和功能、运化忌不救子嗣",
            zn.basis.contains("贴身冲克") && zn.basis.contains("没儿子，只能生女儿") &&
                zn.basis.contains("没有这个能力和功能") && zn.basis.contains("不能说明有儿子"))

        // 己丑金旺助忌——提升不了（中平）；提升在北方运（丁亥泄金生木）
        assertTrue("己丑运生官助忌——提升不了（非吉）", jb.yuns[1].verdict != JinBiaoVerdict.JI)
        assertTrue("丁亥运北方化杀——提升之运吉", jb.yuns[3].verdict == JinBiaoVerdict.JI)
    }

    // ── 案六十五 · 丙寅 戊戌 丙午 庚寅（1986-10-29 寅时，乾）——子女章八例·季节至上 ──

    @Test
    fun `案六十五 乾 丙寅戊戌丙午庚寅 戌月秋金有气季节至上 得气帮得起用财 庚子去刃病 食伤隔劫更可以生儿子`() {
        val bazi = chart(1986, 10, 29, 4)
        assertTrue(bazi.fullBazi.startsWith("丙寅 戊戌 丙午 庚寅"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运起运年与师传年标一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("己亥(1990)", "庚子(2000)", "辛丑(2010)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("寅午戌三合火局然戌月秋金有气——身旺食伤生财用神财庚（得月令之气）",
            jb.quYongWay == JinBiaoQuYongWay.SHI_CAI_YONG_CAI && jb.yongShenElement == Element.METAL)
        assertTrue("取用依据含季节至上（夏雨不冷）、月令季节＞局势",
            jb.quYongBasis.contains("得月令之气") && jb.quYongBasis.contains("季节至上"))

        // 子女：比劫被食伤隔开＋财得月气——更可以生儿子（对勘案六二无气帮不起）
        val zn = jb.ziNv!!
        assertTrue("必有儿", zn.qingKuang == cn.xulun.bazi.model.JinBiaoZiNvQingKuang.BI_YOU_ER)
        assertTrue("判据含隔劫不贴克、更可以生儿子、得气帮得起无气帮不起",
            zn.basis.contains("隔着——不贴克") && zn.basis.contains("更可以生儿子") &&
                zn.basis.contains("得气帮得起，无气帮不起"))

        // 庚子运冲去午刃之病——学习好财运好（吉）；辛丑北方金水帮财吉
        assertTrue("庚子运去羊刃病——吉", jb.yuns[1].verdict == JinBiaoVerdict.JI)
        assertTrue("庚子运标注去刃病", jb.yuns[1].items.any { it.action.contains("去掉这个病") })
        assertTrue("辛丑运帮财——吉", jb.yuns[2].verdict == JinBiaoVerdict.JI)
    }

    // ── 案六十六 · 甲辰 甲戌 庚戌 庚辰（1964-10-28 辰时，乾）——子女章九例·贴克无儿 ──

    @Test
    fun `案六十六 乾 甲辰甲戌庚戌庚辰 四土双冲身旺用财 比劫贴克财纵走三十年财运也生不了儿子 包工头发财`() {
        val bazi = chart(1964, 10, 28, 8)
        assertTrue(bazi.fullBazi.startsWith("甲辰 甲戌 庚戌 庚辰"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运起运年与师传年标一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("乙亥(1968)", "丙子(1978)", "丁丑(1988)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("四土得局双冲而比劫透干——不作昆仑：土金旺身旺足以用财（用神甲木财）",
            jb.quYongWay == JinBiaoQuYongWay.HUAI_YIN && jb.yongShenElement == Element.WOOD)

        // 子女：比劫贴克财（天干贴克＋地支戌冲辰砍伐）——纵走三十年财运发财也生不了儿子
        val zn = jb.ziNv!!
        assertTrue("必无儿（贴克无儿）", zn.qingKuang == cn.xulun.bazi.model.JinBiaoZiNvQingKuang.BI_WU_ER)
        assertTrue("判据含贴着克财、生不了儿子、有钱不一定有儿子",
            zn.basis.contains("贴着克财") && zn.basis.contains("生不了儿子") &&
                zn.basis.contains("有钱不一定有儿子"))

        // 寅卯辰三十年支皆财地（干支分论）——助起财星发财（包工头）；纵发财亦不救子嗣
        assertTrue("戊寅己卯庚辰寅卯辰支皆财地（发财运非凶）", listOf(3, 4, 5).all { jb.yuns[it].verdict != JinBiaoVerdict.XIONG })
    }

    // ── 案六十七 · 丁卯 庚戌 癸丑 己未（1987-10-31 未时，坤）——子女章十例·财贴克印 ──

    @Test
    fun `案六十七 坤 丁卯庚戌癸丑己未 杀重化官生印财贴克印 忌神深重贴克运去病不修复生育程序 生闺女必无子`() {
        val bazi = chart(1987, 10, 31, 14)
        assertTrue(bazi.fullBazi.startsWith("丁卯 庚戌 癸丑 己未"))
        val daYun = DaYunCalculator.compute(bazi, Gender.FEMALE)
        assertTrue(
            "阴年坤造顺行大运起运年与师传年标一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("辛亥(1990)", "壬子(2000)", "癸丑(2010)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.FEMALE)
        assertTrue("丑戌未三刑官杀重——化官生印用神庚金（身弱怕财坏印）",
            jb.strength == DayStrength.WEAK && jb.quYongWay == JinBiaoQuYongWay.HUA_SHA && jb.yongShenElement == Element.METAL)
        assertTrue("取用依据含财贴克印病药论、有病得药加分、财富取决于印",
            jb.quYongBasis.contains("财贴克印") && jb.quYongBasis.contains("有病得药，要加分") &&
                jb.quYongBasis.contains("财富值一定取决于印"))

        // 子女：财贴克印——忌神深重贴克：运去病也不修复生育程序——生闺女、必无子
        val zn = jb.ziNv!!
        assertTrue("无儿有女（解放生女）", zn.qingKuang == cn.xulun.bazi.model.JinBiaoZiNvQingKuang.WU_ER_YOU_NV)
        assertTrue("判据含贴着我的印来克、生不出来儿子、八字程序、生闺女、必无子",
            zn.basis.contains("贴着我的印来克") && zn.basis.contains("生不出来儿子") &&
                zn.basis.contains("八字程序") && zn.basis.contains("能生个闺女") && zn.basis.contains("必无子之命"))

        // 北方壬癸去丁病——好运学习好工作好；东方甲寅乙卯生火助病——破大财凶
        assertTrue("壬子癸丑北方去病运皆吉", listOf(1, 2).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
        assertTrue("甲寅乙卯东方生火助病——凶", listOf(3, 4).all { jb.yuns[it].verdict == JinBiaoVerdict.XIONG })
    }

    // ── 案六十八 · 癸亥 丁巳 丁巳 辛丑（1983-05-29 丑时，坤）——子女章十一例·去火存金 ──

    @Test
    fun `案六十八 坤 癸亥丁巳丁巳辛丑 亥巳冲去火存金巳丑拱金旺中变弱 比劫卡杀有儿 身弱财大负债论 西方运凶`() {
        val bazi = chart(1983, 5, 29, 2)
        assertTrue(bazi.fullBazi.startsWith("癸亥 丁巳 丁巳 辛丑"))
        val daYun = DaYunCalculator.compute(bazi, Gender.FEMALE)
        assertTrue(
            "阴年坤造顺行大运起运年与师传年标一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("戊午(1986)", "己未(1996)", "庚申(2006)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.FEMALE)
        assertTrue("亥巳冲去火存金＋巳丑拱金——金旺财得局：旺中变弱（身弱）", jb.strength == DayStrength.WEAK)
        assertTrue("取用依据含去火存金与身弱财大负债论",
            jb.quYongBasis.contains("去火存金") && jb.quYongBasis.contains("身弱财大＝负债论"))

        // 子女：比劫卡杀（月干丁挡住年杀癸）——身弱怕克被卡＝有儿（功能在）；身旺反向卡＝无儿有侄儿
        val zn = jb.ziNv!!
        assertTrue("必有儿（比劫卡杀）", zn.qingKuang == cn.xulun.bazi.model.JinBiaoZiNvQingKuang.BI_YOU_ER)
        assertTrue("判据含卡住、有这个功能、反向侄儿",
            zn.basis.contains("卡住") && zn.basis.contains("有这个功能") && zn.basis.contains("侄儿"))

        // 戊午己未帮身吉；庚申辛酉西方金运——财生杀攻身在财上出事负债——凶
        assertTrue("戊午己未帮身运吉", listOf(0, 1).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
        assertTrue("庚申辛酉西方运不行（负债）——凶", listOf(2, 3).all { jb.yuns[it].verdict == JinBiaoVerdict.XIONG })
    }

    // ── 案六十九 · 庚午 己卯 乙酉 乙酉（1990-03-21 酉时，坤）——子女章十二例·身体条件论 ──

    @Test
    fun `案六十九 坤 庚午己卯乙酉乙酉 两酉冲一卯日主有力量而敌人更有力 食神制杀以威有格局出身大富 身体不行无儿流产`() {
        val bazi = chart(1990, 3, 21, 18)
        assertTrue(bazi.fullBazi.startsWith("庚午 己卯 乙酉 乙酉"))
        val daYun = DaYunCalculator.compute(bazi, Gender.FEMALE)
        assertTrue(
            "阳年坤造逆行大运起运年与师传年标一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("戊寅(1995)", "丁丑(2005)", "丙子(2015)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.FEMALE)
        assertTrue("两酉冲一卯——比劫禄没有杀旺：旺中变弱", jb.strength == DayStrength.WEAK)
        assertTrue("根强拿得起刀——午一位制杀卫护身体：食神制杀以威有格局（出身大富）",
            jb.quYongWay == JinBiaoQuYongWay.ZHI_SHA && jb.yongShenElement == Element.FIRE)

        // 子女：克泄交加无一印——身体条件不行：八字不具备生儿子功能（怀孕流掉——己亥年）
        val zn = jb.ziNv!!
        assertTrue("必无儿（身体条件档）", zn.qingKuang == cn.xulun.bazi.model.JinBiaoZiNvQingKuang.BI_WU_ER)
        assertTrue("判据含身体条件、不具备功能、流产、没格局两轨",
            zn.basis.contains("身体条件") && zn.basis.contains("不具备生儿子") &&
                zn.basis.contains("流产") && zn.basis.contains("没生出来不代表八字没格局"))
    }

    // ── 案七十 · 丙寅 庚子 癸卯 甲寅（1986-12-25 寅时，坤）——子女章十三例·因果全息 ──

    @Test
    fun `案七十 坤 丙寅庚子癸卯甲寅 子月建禄然三木过泄旺中变弱 财贴克印没有生儿子功能 人一出生就决定 因果全息`() {
        val bazi = chart(1986, 12, 25, 4)
        assertTrue(bazi.fullBazi.startsWith("丙寅 庚子 癸卯 甲寅"))
        val daYun = DaYunCalculator.compute(bazi, Gender.FEMALE)
        assertTrue(
            "阳年坤造逆行大运起运年与师传年标一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("己亥(1992)", "戊戌(2002)", "丁酉(2012)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.FEMALE)
        assertTrue("子月建禄然地支三木过泄食伤混杂——旺中变弱", jb.strength == DayStrength.WEAK)
        assertTrue("身衰印做儿——用神庚金印（调候神丙贴克印——调候让位生克线）",
            jb.quYongWay == JinBiaoQuYongWay.FU_SHEN && jb.yongShenElement == Element.METAL)

        // 子女：丙贴庚财贴克印——没有生儿子的功能（人一出生就决定）；因果全息（祖坟/父风/婆德）
        val zn2 = jb.ziNv!!
        assertTrue("无儿有女（贴克档）", zn2.qingKuang == cn.xulun.bazi.model.JinBiaoZiNvQingKuang.WU_ER_YOU_NV)
        assertTrue("判据含人一出生就决定、因果全息、祖坟",
            zn2.basis.contains("人一出生就决定了") && zn2.basis.contains("因果全息") && zn2.basis.contains("祖坟"))
    }

    // ── 案七十一（＝案四盘复看子女）· 癸亥 癸亥 戊辰 甲寅（1983-12-06 寅时，乾）──

    @Test
    fun `案七十一 乾 癸亥癸亥戊辰甲寅复看子女 弱极从势特殊格局不走正常树 走好运就可以生儿子 辰为震位虚土自顾不暇`() {
        val bazi = chart(1983, 12, 6, 3)
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("弱极孤立无气从势随财（旧锚案四）", jb.ruoJi && jb.quYongWay == JinBiaoQuYongWay.SUI_CAI)

        // 从势局子女另裁：特殊格局——只要走好运（顺势之运）就可以生儿子；最忌帮身
        val zn = jb.ziNv!!
        assertTrue("从势局必有儿（走好运即生）", zn.qingKuang == cn.xulun.bazi.model.JinBiaoZiNvQingKuang.BI_YOU_ER)
        assertTrue("判据含特殊格局不走正常树、走好运生儿、辰为震位虚土、最忌帮身",
            zn.basis.contains("特殊格局") && zn.basis.contains("只要走好运") &&
                zn.basis.contains("震位虚土") && zn.basis.contains("最忌帮身"))

        // 辛酉庚申金顺势吉（事业不错）；戌运比劫帮身逆局——凶（学习不好）
        assertTrue("辛酉庚申金顺势皆吉", listOf(1, 2).all { jb.yuns[it].verdict == JinBiaoVerdict.JI })
        assertTrue("壬戌比劫帮身逆局——凶（学习不好）", jb.yuns[0].verdict == JinBiaoVerdict.XIONG)
    }

    // ── 案七十二（＝案廿五盘复看子女）· 壬戌 乙巳 戊戌 壬戌（1982-05-15 戌时，坤）──

    @Test
    fun `案七十二 坤 壬戌乙巳戊戌壬戌复看子女 明以官杀为子暗仍以财星为子 官虚透无贴克看大运 东方运信号接收生儿子`() {
        val bazi = chart(1982, 5, 15, 19)
        val daYun = DaYunCalculator.compute(bazi, Gender.FEMALE)
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.FEMALE)
        assertTrue("身旺财滋弱官（旧锚案廿五）", jb.quYongWay == JinBiaoQuYongWay.PEI_CAI && jb.yongShenElement == Element.WOOD)

        // 明暗子星：财被三戌劫——官护财克劫——明以官杀为子暗仍以财星为子；官虚透（乙被巳化）无贴克——看大运
        val zn2 = jb.ziNv!!
        assertTrue("明官暗财——东方运信号接收——必有儿", zn2.qingKuang == cn.xulun.bazi.model.JinBiaoZiNvQingKuang.BI_YOU_ER)
        assertTrue("子星＝正官乙木（明）", zn2.ziXing == cn.xulun.bazi.model.TenGod.ZHENG_GUAN && zn2.ziEl == Element.WOOD)
        assertTrue("判据含明以官杀为子暗仍以财星为子、等投胎、接收信号、贴克不看运",
            zn2.basis.contains("明以官杀为子，暗仍以财星为子") && zn2.basis.contains("等投胎") &&
                zn2.basis.contains("接收得到信号") && zn2.basis.contains("若有贴克则不看大运"))
    }

    // ── 案七十三 · 辛酉 癸巳 丁酉 壬寅（1981-05-19 寅时，坤）——子女章十六例·异性轻克 ──

    @Test
    fun `案七十三 坤 辛酉癸巳丁酉壬寅 巳酉化金火根尽化旺中变弱取弱印 酉是小刀寅是树异性轻克 弱代表穷不代表生不了儿子 儿子学习随印运`() {
        val bazi = chart(1981, 5, 19, 4)
        assertTrue(bazi.fullBazi.startsWith("辛酉 癸巳 丁酉 壬寅"))
        val daYun = DaYunCalculator.compute(bazi, Gender.FEMALE)
        assertTrue(
            "阴年坤造顺行大运起运年与师传年标一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("甲午(1987)", "乙未(1997)", "丙申(2007)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.FEMALE)
        assertTrue("巳酉化金×2——巳火不专火根尽化＋官杀混杂加力——旺中变弱", jb.strength == DayStrength.WEAK)
        assertTrue("取这个弱印——用神寅木正印（一切由印星决定）",
            jb.quYongWay == JinBiaoQuYongWay.HUA_SHA && jb.yongShenElement == Element.WOOD)

        // 子女：酉贴寅异性轻克（小刀刻不断树）＋壬坐寅官生印——弱而不废：可以生儿子；印弱＝穷；儿子学习随印运
        val zn3 = jb.ziNv!!
        assertTrue("必有儿（异性轻克档——弱而不废）", zn3.qingKuang == cn.xulun.bazi.model.JinBiaoZiNvQingKuang.BI_YOU_ER)
        assertTrue("判据含小刀刻不断树、弱代表穷不代表生不了、一切由印星决定、儿子学习随印运",
            zn3.basis.contains("小刀刻不断树") && zn3.basis.contains("弱代表她穷，不代表她生不了儿子") &&
                zn3.basis.contains("一切由印星决定") && zn3.basis.contains("儿子学习随印运"))

        // 申运克寅——财富值低（贫穷）；儿子学习差
        assertTrue("丙申运申克印——凶（财富值低）", jb.yuns[2].verdict != JinBiaoVerdict.JI)
    }

    // ── 案七十四 · 丙辰 甲午 丙申 丁酉（1976-06-13 酉时，乾）——子女章十七例·羊刃肆逞 ──

    @Test
    fun `案七十四 乾 丙辰甲午丙申丁酉 羊刃当令带印生飞刃透干肆逞 无官制刃功能废 投胎待不住流产克子 食伤生财享妻家福`() {
        val bazi = chart(1976, 6, 13, 18)
        assertTrue(bazi.fullBazi.startsWith("丙辰 甲午 丙申 丁酉"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运起运年与师传年标一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("乙未(1984)", "丙申(1994)", "丁酉(2004)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("夏天火加成羊刃当令印生刃——身旺用财（申酉）", jb.strength == DayStrength.STRONG &&
            jb.quYongWay == JinBiaoQuYongWay.SHI_CAI_YONG_CAI && jb.yongShenElement == Element.METAL)

        // 子女：羊刃肆逞（带源＋飞刃）贴克财而无官制刃——功能废：必无儿（纵生流产/童子命/早年克子）
        val zn4 = jb.ziNv!!
        assertTrue("必无儿（羊刃肆逞功能废）", zn4.qingKuang == cn.xulun.bazi.model.JinBiaoZiNvQingKuang.BI_WU_ER)
        assertTrue("判据含羊刃肆逞、飞刃、精卵气血、投胎、流产克子",
            zn4.basis.contains("羊刃肆逞") && zn4.basis.contains("飞刃") &&
                zn4.basis.contains("精卵") && zn4.basis.contains("投胎") &&
                zn4.basis.contains("流产") && zn4.basis.contains("克子"))
    }

    // ── 案七十五 · 庚申 乙酉 己丑 乙丑（1980-09-13 丑时，乾）——子女章十八例·虚劫不管事 ──

    @Test
    fun `案七十五 乾 庚申乙酉己丑乙丑 秋金会局王炸大王 比劫虚土不管事必须找印无印没有功能生闺女 用神弱财富低好运积蓄非发富`() {
        val bazi = chart(1980, 9, 13, 2)
        assertTrue(bazi.fullBazi.startsWith("庚申 乙酉 己丑 乙丑"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)
        assertTrue(
            "顺行大运起运年与师传年标一致",
            daYun.yuns.take(3).map { "${it.pillar.fullName}(${it.startYear})" } == listOf("丙戌(1989)", "丁亥(1999)", "戊子(2009)"),
        )
        val jb = JinBiaoCalculator.analyze(bazi, daYun, Gender.MALE)
        assertTrue("秋金会局＋金库＝王炸大王——日主过泄克泄交加（制杀也没用不如不制）——用劫（劫弱财富低）",
            jb.strength == DayStrength.WEAK && jb.quYongWay == JinBiaoQuYongWay.SHANG_YONG_JIE && jb.yongShenElement == Element.EARTH)

        // 子女：比劫都虚了（辰丑虚土古训）不管事——必须找印；无印＝没有功能——不能生儿子生闺女
        val zn5 = jb.ziNv!!
        assertTrue("无儿有女（虚劫不管事档）", zn5.qingKuang == cn.xulun.bazi.model.JinBiaoZiNvQingKuang.WU_ER_YOU_NV)
        assertTrue("判据含虚了不管事、辰丑虚土古训、王炸大王、没有功能、穷人也可以生女儿、用神弱财富低",
            zn5.basis.contains("不管事") && zn5.basis.contains("辰丑之土非彼未戌月之实也") &&
                zn5.basis.contains("王炸大王") && zn5.basis.contains("没有这个功能") &&
                zn5.basis.contains("穷人也可以生女儿") && zn5.basis.contains("用神弱了财富值才低"))
    }

    // ── 双引擎铁律：带有事实的案例同时走老引擎 ──

    @Test
    fun `档五十一双引擎 金镖门独立断盘 老引擎锚定印化七杀硕士`() {
        val bazi = chart(1978, 9, 17, 4)
        assertTrue(bazi.fullBazi.startsWith("戊午 辛酉 壬午 壬寅"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)

        // 金镖门引擎（新口径独立断盘）
        val jb = JinBiaoCalculator.analyze(bazi, daYun)
        assertTrue("金镖门断语一律锚定用神", jb.duanYu.contains("用神"))
        assertTrue("身衰遇官煞枭印乃可化——与老引擎印化七杀同断（五理③）",
            jb.quYongWay == JinBiaoQuYongWay.HUA_SHA && jb.yongShenElement == Element.METAL)
        assertTrue("大运流年逐年评断齐备", jb.yuns.isNotEmpty() && jb.yuns.flatMap { it.years }.any { it.year == 2007 })
        assertTrue("模块注记双引擎对照要求", jb.note.contains("老引擎"))

        // 老引擎（事实锚定：印化七杀——硕士；2007 丁亥晋升见 ShiChuanAnLiTest）
        val wp = WenPingCalculator.analyze(bazi, daYun)
        assertTrue("老引擎事实锚定——印化七杀", wp.favorable.any { it.title.contains("印化七杀") })
    }

    // ── 取用一致性：卷一锚定库全量过盘 ──

    @Test
    fun `卷一锚定库 取用路径与日主强弱自洽`() {
        val cases = listOf(
            "档四十四" to chart(1974, 1, 20, 6),
            "档四十七" to chart(1987, 9, 23, 4),
            "档四十八" to chart(1990, 3, 14, 6),
            "档五十" to chart(1970, 4, 5, 4),
            "档五十一" to chart(1978, 9, 17, 4),
        )
        cases.forEach { (name, bazi) ->
            val r = JinBiaoCalculator.analyze(bazi, null)
            val category = r.daiMing.first { it.element == r.yongShenElement }.category
            if (r.ruoJi) {
                assertTrue("$name 弱极从势——用神必为财官食之一", category in setOf("财星", "官杀", "食伤"))
                assertTrue(
                    "$name 弱极取用必为从势路径",
                    r.quYongWay in setOf(
                        JinBiaoQuYongWay.SUI_GUAN, JinBiaoQuYongWay.SUI_CAI,
                        JinBiaoQuYongWay.CAI_JIE_WEI, JinBiaoQuYongWay.SUI_SHI,
                    ),
                )
            } else {
                when (r.strength) {
                    DayStrength.WEAK ->
                        assertTrue(
                            "$name 身弱取用——印比扶身或枭印化杀",
                            r.quYongWay in setOf(JinBiaoQuYongWay.FU_SHEN, JinBiaoQuYongWay.HUA_SHA, JinBiaoQuYongWay.SHANG_YONG_JIE) &&
                                category in setOf("比劫", "印绶"),
                        )
                    DayStrength.STRONG ->
                        if (r.wangJi) {
                            assertTrue("$name 旺极随旺神——用神必为比劫", r.quYongWay == JinBiaoQuYongWay.SUI_WANG && category == "比劫")
                        } else {
                            assertTrue(
                                "$name 身强取用——五理路径皆财官食",
                                r.quYongWay in setOf(
                                    JinBiaoQuYongWay.YI_SHEN, JinBiaoQuYongWay.ZHI_SHA,
                                    JinBiaoQuYongWay.PEI_CAI, JinBiaoQuYongWay.HUAI_YIN,
                                    JinBiaoQuYongWay.SHI_SHENG_CAI, JinBiaoQuYongWay.QU_GUA,
                                    JinBiaoQuYongWay.SHI_CAI_YONG_CAI, JinBiaoQuYongWay.SHEN_WANG_WU_YI,
                                ) && category in setOf("财星", "官杀", "食伤"),
                            )
                        }
                    DayStrength.BALANCED ->
                        assertTrue("$name 中和取月令", r.quYongWay == JinBiaoQuYongWay.ZHONG_HE)
                }
            }
        }
    }
}
