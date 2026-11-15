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
        assertTrue("官杀运标注必然大凶", r.yuns[2].items.all { it.action.contains("必然大凶") })
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
        assertTrue("印轻食伤运标注顺泄好运", yinQing.yuns[0].items.all { it.action.contains("食伤运也是好运") })
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
        assertTrue("印星得局——化煞生印，官杀运不为大凶（中平）", r.yuns[1].verdict == JinBiaoVerdict.PING)
        assertTrue("官杀运标注化煞生印", r.yuns[1].items.all { it.action.contains("化煞生印") })
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

    // ── 双引擎铁律：带有事实的案例同时走老引擎 ──

    @Test
    fun `档五十一双引擎 金镖门独立断盘 老引擎锚定印化七杀硕士`() {
        val bazi = chart(1978, 9, 17, 4)
        assertTrue(bazi.fullBazi.startsWith("戊午 辛酉 壬午 壬寅"))
        val daYun = DaYunCalculator.compute(bazi, Gender.MALE)

        // 金镖门引擎（新口径独立断盘）
        val jb = JinBiaoCalculator.analyze(bazi, daYun)
        assertTrue("金镖门断语一律锚定用神", jb.duanYu.contains("用神"))
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
                        assertTrue("$name 身弱扶身——用神必为印比", r.quYongWay == JinBiaoQuYongWay.FU_SHEN && category in setOf("比劫", "印绶"))
                    DayStrength.STRONG ->
                        if (r.wangJi) {
                            assertTrue("$name 旺极随旺神——用神必为比劫", r.quYongWay == JinBiaoQuYongWay.SUI_WANG && category == "比劫")
                        } else {
                            assertTrue("$name 身强抑泄——用神必为财官食", r.quYongWay == JinBiaoQuYongWay.YI_SHEN && category in setOf("财星", "官杀", "食伤"))
                        }
                    DayStrength.BALANCED ->
                        assertTrue("$name 中和取月令", r.quYongWay == JinBiaoQuYongWay.ZHONG_HE)
                }
            }
        }
    }
}
