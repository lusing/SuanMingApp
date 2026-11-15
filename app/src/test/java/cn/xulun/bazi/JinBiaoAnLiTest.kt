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
                            r.quYongWay in setOf(JinBiaoQuYongWay.FU_SHEN, JinBiaoQuYongWay.HUA_SHA) &&
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
