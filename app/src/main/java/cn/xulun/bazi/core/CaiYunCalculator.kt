package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.CaiYunKind
import cn.xulun.bazi.model.CaiYunResult
import cn.xulun.bazi.model.CaiYunYear
import cn.xulun.bazi.model.CaiYunYun
import cn.xulun.bazi.model.DaYunInfo
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.LiuQinItem
import cn.xulun.bazi.model.TenGod
import cn.xulun.bazi.model.branchHiddenStems
import cn.xulun.bazi.core.LiuQinXiJi.category
import cn.xulun.bazi.model.tenGodOfStem

/**
 * 财运完整岁运引擎（.caiyun-reference.md）。三层：原局基调 / 大运和战评级 / 流年关键年。
 *
 * 核心（滴天髓·财气通门户）：全命局（原局+大运+流年）**和局→发财，战局→破财**；
 * 财星被流年引动（年干支为财、冲动/合动原局财星、伤官透干）才列入关键年——"前略后细"。
 *
 * 和战判定（干支系统分判）：
 * - 支系统：三刑组齐（寅巳申/丑戌未）=刑战；比劫之年支冲原局财本气支（未被合解）=冲战；
 *   六合对≥2=合化解战（谢先生壬戌运卯戌+子丑）；三合组化敌为友（巳酉丑合金——徐先生丁丑年）；
 *   前二大行相生=和；相克而通关之行力≥1=和（辰土通火金之关），否则交战
 * - 干系统：干对冲（甲庚乙辛丙壬丁癸）=战；前二大行相生=和；相克通关≥1=和，否则战
 * - 发/破分级：和局=发财；冲战/刑战/干对冲/干支双战=破财；单交战=谨慎
 *
 * 无财星而有食伤≠无财——食伤（伤官尤重）为创造财富之手段，逢财星流年即用武之地（谢先生式）。
 */
object CaiYunCalculator {

    private val sanXingGroups = listOf(
        setOf(EarthlyBranch.YIN, EarthlyBranch.SI, EarthlyBranch.SHEN),
        setOf(EarthlyBranch.CHOU, EarthlyBranch.XU, EarthlyBranch.WEI),
    )
    private val sanHeGroups = mapOf(
        setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU) to Element.FIRE,
        setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU) to Element.METAL,
        setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN) to Element.WATER,
        setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI) to Element.WOOD,
    )
    private val liuHePairs = setOf(
        setOf(EarthlyBranch.ZI, EarthlyBranch.CHOU), setOf(EarthlyBranch.YIN, EarthlyBranch.HAI),
        setOf(EarthlyBranch.MAO, EarthlyBranch.XU), setOf(EarthlyBranch.CHEN, EarthlyBranch.YOU),
        setOf(EarthlyBranch.SI, EarthlyBranch.SHEN), setOf(EarthlyBranch.WU, EarthlyBranch.WEI),
    )
    private val ganChong = setOf(
        setOf(HeavenlyStem.JIA, HeavenlyStem.GENG), setOf(HeavenlyStem.YI, HeavenlyStem.XIN),
        setOf(HeavenlyStem.BING, HeavenlyStem.REN), setOf(HeavenlyStem.DING, HeavenlyStem.GUI),
    )

    /** 一层系统（干或支）的和战结论。 */
    private class HeZhan(val he: Boolean, val desc: String)

    fun analyze(bazi: BaziResult, gender: Gender, daYun: DaYunInfo): CaiYunResult {
        val day = bazi.dayPillar.stem
        val dayEl = day.element
        val caiEl = dayEl.controls
        val biEl = dayEl
        val d = LiuQinXiJi.derive(bazi)
        val forces = BingYaoCalculator.forces(bazi)

        // ── 原局财星信息 ──
        val caiStems = bazi.stems.indices.filter {
            it != 2 && tenGodOfStem(day, bazi.stems[it]).let { g -> g == TenGod.ZHENG_CAI || g == TenGod.PIAN_CAI }
        }
        val zhengTou = caiStems.any { tenGodOfStem(day, bazi.stems[it]) == TenGod.ZHENG_CAI }
        val pianTou = caiStems.any { tenGodOfStem(day, bazi.stems[it]) == TenGod.PIAN_CAI }
        val caiBenQiPillars = bazi.branches.indices.filter {
            branchHiddenStems.getValue(bazi.branches[it]).firstOrNull()?.element == caiEl
        }
        val caiAnyHidden = bazi.branches.any { br -> branchHiddenStems.getValue(br).any { it.element == caiEl } }
        val shiEl = dayEl.generates
        val shangTou = bazi.stems.any { tenGodOfStem(day, it) == TenGod.SHANG_GUAN }

        val baseItems = mutableListOf<LiuQinItem>()
        if (zhengTou && !pianTou) {
            baseItems += LiuQinItem("正财透干", "正财透干——宜稳妥方式求财，最好找份好工作稳稳当当拿工资；做生意勿经营风险较大的项目。")
        }
        if (pianTou) {
            baseItems += LiuQinItem("偏财透干", "偏财透干——不甘于平淡拿工资，有冒险投机求财的强烈冲动；逢偏财被引动生助之流年，念头即变行动。")
        }
        val dayBranchCaiPian = branchHiddenStems.getValue(bazi.dayPillar.branch).firstOrNull()?.element == caiEl
        if (dayBranchCaiPian) {
            baseItems += LiuQinItem("偏财贴身", "偏财（日支本气）紧贴日支——内心有投机求财冲动，逢引动之年难免冒险。")
        }
        if (caiStems.isEmpty() && !caiAnyHidden && forces[shiEl]!! >= 2.0) {
            baseItems += LiuQinItem(
                "无财有食伤",
                "命局无财星而有食伤——食伤生财：财富由人创造，伤官（技术才华行为力）+食神（想象创新）即创造财富的手段，找到用武之地便非穷命，逢财星流年如虎添翼。",
            )
        }
        if (forces[caiEl]!! >= 3.0 && zhengTou != pianTou && (zhengTou || pianTou)) {
            baseItems += LiuQinItem("财星有力", "财星有力（${caiEl.chinese}行力${forces[caiEl]}）——正偏财并旺者工资外水左右逢源。")
        }
        // 原局财支被冲——损失伏笔
        val origChongLocs = DiZhiRelationCalculator.analyze(bazi).chongList
        val caiChonged = caiBenQiPillars.any { p ->
            origChongLocs.any { c ->
                (c.branch1 == bazi.branches[p] || c.branch2 == bazi.branches[p])
            }
        }
        if (caiChonged) {
            baseItems += LiuQinItem("财星遭冲", "原局财星本气支遭冲克——命中埋下钱财被浪费损失的伏笔。")
        }
        // 商人特征：财贴日柱当令有力+时柱食伤为财源（例九）
        val caiDangLing = bazi.monthPillar.branch.element == caiEl
        val hourShiShang = bazi.stems[3].element == shiEl ||
            branchHiddenStems.getValue(bazi.branches[3]).firstOrNull()?.element == shiEl
        if ((caiDangLing || caiStems.isNotEmpty()) && hourShiShang) {
            baseItems += LiuQinItem("商人特征", "财星贴身当令而旺，时柱食伤为财源——有经济头脑，具商人特征，宜商途。")
        }
        // 无偏财潜质：财无本气+不透偏财+无伤官（例八）
        if (caiBenQiPillars.isEmpty() && !pianTou && !shangTou) {
            baseItems += LiuQinItem("不宜独立创业", "正偏财无本气、不透偏财又无伤官——没有偏财行业的先天潜质，不宜离开单位独立创业，依靠单位稳当求财为宜。")
        }

        // ── 和战判定 ──
        val bing = BingYaoCalculator.analyze(bazi)
        val isCong = bing.congGe != null
        val engVerdict = StrengthEngine.analyze(bazi).verdict // 原始强弱（不从格翻转——喜用覆盖用）

        fun mergeBranchHeZhan(extraBranches: List<EarthlyBranch>, yearStemEl: Element? = null): HeZhan {
            val all = bazi.branches + extraBranches
            val f = Element.entries.associateWith { 0.0 }.toMutableMap()
            bazi.branches.forEach { f[it.element] = f[it.element]!! + 1.0 }
            f[bazi.monthPillar.branch.element] = f[bazi.monthPillar.branch.element]!! + 2.0
            extraBranches.forEach { f[it.element] = f[it.element]!! + 1.0 }
            // 三刑组齐
            sanXingGroups.forEach { g ->
                if (g.count { it in all } >= 3) return HeZhan(false, "三刑组齐（${g.joinToString("") { it.chinese }}）刑战")
            }
            // 六合对数（合化解战）
            var hePairs = 0
            liuHePairs.forEach { p -> if (p.count { it in all } >= 2) hePairs++ }
            fun chongSolved(a: EarthlyBranch, b: EarthlyBranch): Boolean =
                liuHePairs.any { p -> (a in p && p.any { it != a && it in all && it != b }) || (b in p && p.any { it != b && it in all && it != a }) }
            // 年支冲原局支：比劫冲财=夺财战（年干通关可解——例九乙亥乙木通水火之关）；
            // 财冲比劫——财力胜则去劫（吉），力不敌亦战
            extraBranches.forEach { eb ->
                val cb = br2(eb)
                val clashPillar = bazi.branches.indices.firstOrNull { bazi.branches[it] == cb }
                if (clashPillar != null) {
                    if (eb.element == biEl && cb.element == caiEl) {
                        return HeZhan(false, "比劫${eb.chinese}冲原局财星${cb.chinese}——冲战夺财")
                    }
                    if (eb.element == caiEl && cb.element == biEl && forces[caiEl]!! <= forces[biEl]!! &&
                        !chongSolved(eb, cb)
                    ) {
                        return HeZhan(false, "财星${eb.chinese}冲${cb.chinese}而力不敌比劫——冲战")
                    }
                }
            }
            // 前二大行
            val sorted = Element.entries.filter { f[it]!! >= 1.0 }.sortedByDescending { f[it]!! }
            if (sorted.size < 2) return HeZhan(true, "五行单气和顺")
            val a = sorted[0]; val b2 = sorted[1]
            if (hePairs >= 2) return HeZhan(true, "六合对${hePairs}组合化解战")
            if (a.generates == b2 || b2.generates == a) return HeZhan(true, "${a.chinese}${b2.chinese}相生")
            // 相克：三合组化敌（弱方之支参与强方三合组）
            val strong = if (a.controls == b2) a else b2
            val weak = if (strong == a) b2 else a
            sanHeGroups.forEach { (group, el) ->
                if (el == strong && group.any { it in all && it.element == weak } &&
                    group.count { it in all } >= 2
                ) return HeZhan(true, "${group.joinToString("") { it.chinese }}合化${strong.chinese}——化敌为友")
            }
            // 通关（含岁干通关——流年天干为通关之行，如丙寅年丙火通木土之关；
            // 及月干通关——通关之行透于月干且贴身有力（例十三丙火通木土），从格不取）
            val tong = Element.entries.firstOrNull { strong.generates == it && it.generates == weak }
            val yueGanTong = tong == bazi.monthPillar.stem.element && !isCong
            if (tong != null && (f[tong]!! >= 1.0 || tong == yearStemEl || yueGanTong)) {
                return HeZhan(true, "${strong.chinese}克${weak.chinese}得${tong.chinese}通关" +
                    (if (tong == yearStemEl) "（岁干通关）" else if (yueGanTong) "（月干通关）" else ""))
            }
            return HeZhan(false, "${strong.chinese}${weak.chinese}交战无通关")
        }

        fun mergeStemHeZhan(extraStems: List<HeavenlyStem>): HeZhan {
            val all = bazi.stems + extraStems
            // 岁运并临而天干对冲——力增不可解（林2004甲申并临甲庚冲），从格亦战
            if (extraStems.size == 2 && extraStems[0] == extraStems[1] &&
                extraStems[0].element != caiEl && // 岁干为财星当值不以战论（浙程2002壬午并临——财星当值而发）
                ganChong.any { g -> extraStems[0] in g && bazi.stems.any { it in g } }
            ) {
                return HeZhan(false, "天干岁运并临对冲——力增战局")
            }
            // 干对冲（例外：从格从则不战；岁干为财星当值不判；冲方力≥3方冲得动）
            if (!isCong && extraStems.isNotEmpty() && extraStems.last().element != caiEl) {
                val gf = Element.entries.associateWith { 0.0 }.toMutableMap()
                all.forEach { gf[it.element] = gf[it.element]!! + 1.0 }
                ganChong.forEach { p ->
                    if (extraStems.any { it in p } && all.count { it in p } >= 2) {
                        val e1 = p.first().element; val e2 = p.last().element
                        val bingLinYear = extraStems.size == 2 && extraStems[0] == extraStems[1]
                        if (bingLinYear || maxOf(gf[e1]!!, gf[e2]!!) >= 3.0) {
                            return HeZhan(false, "天干${p.joinToString("") { it.chinese }}对冲" + (if (bingLinYear) "（岁运并临力增不可解）" else ""))
                        }
                    }
                }
            }
            val f = Element.entries.associateWith { 0.0 }.toMutableMap()
            all.forEach { f[it.element] = f[it.element]!! + 1.0 }
            val sorted = Element.entries.filter { f[it]!! >= 1.0 }.sortedByDescending { f[it]!! }
            if (sorted.size < 2) return HeZhan(true, "干气单线和顺")
            val a = sorted[0]; val b2 = sorted[1]
            if (a.generates == b2 || b2.generates == a) return HeZhan(true, "干${a.chinese}${b2.chinese}相生")
            val strong = if (a.controls == b2) a else b2
            val weak = if (strong == a) b2 else a
            val tong = Element.entries.firstOrNull { strong.generates == it && it.generates == weak }
            if (tong != null && f[tong]!! >= 1.0) return HeZhan(true, "干${strong.chinese}克${weak.chinese}得${tong.chinese}通关")
            return HeZhan(false, "干${strong.chinese}${weak.chinese}交战")
        }

        /** 年支六合原局财星/比劫支（合动——林1999卯合劫支戌）。 */
        fun heDong(eb: EarthlyBranch): Boolean =
            liuHePairs.any { p ->
                eb in p && p.any { other -> other != eb && other in bazi.branches &&
                    (other.element == caiEl || other.element == biEl) }
            }

        fun chongOf(eb: EarthlyBranch): EarthlyBranch? = br2OrNull(eb)

        val yuns = mutableListOf<CaiYunYun>()
        daYun.yuns.filter { it.startAge >= 16 }.forEach { yun ->
            val yunBranchHe = mergeBranchHeZhan(listOf(yun.pillar.branch))
            val yunStemHe = mergeStemHeZhan(listOf(yun.pillar.stem))
            val yunHe = yunBranchHe.he && yunStemHe.he
            val yunGanGod = tenGodOfStem(day, yun.pillar.stem)
            val yunZhiGod = tenGodOfStem(day, branchHiddenStems.getValue(yun.pillar.branch).first())
            val caiDangZhi = yunGanGod.let { it == TenGod.ZHENG_CAI || it == TenGod.PIAN_CAI } ||
                yunZhiGod.let { it == TenGod.ZHENG_CAI || it == TenGod.PIAN_CAI }
            val sb = StringBuilder()
            sb.append(if (yunHe) "干支系统构成和局——财运畅通的基础" else "支${if (yunBranchHe.he) "和" else "战"}干${if (yunStemHe.he) "和" else "战"}——${yunBranchHe.desc}，求财难如愿，宜守不宜攻")
            if (caiDangZhi) sb.append("；财星当值——挣钱机会多而起伏大，吉凶随流年而定")
            if (yunZhiGod.category() == cn.xulun.bazi.model.GodCategory.YIN || yunGanGod.category() == cn.xulun.bazi.model.GodCategory.YIN) {
                sb.append("；印运守传统，寻平稳工作，财运无大起伏")
            }
            val years = mutableListOf<CaiYunYear>()
            yun.liuNian.forEach { ln ->
                val yGanGod = tenGodOfStem(day, ln.pillar.stem)
                val yZhiHasCai = branchHiddenStems.getValue(ln.pillar.branch).any { it.element == caiEl }
                val yZhiBenQiEl = branchHiddenStems.getValue(ln.pillar.branch).first().element
                val rawFav: Set<cn.xulun.bazi.model.GodCategory> = when (engVerdict) {
                    DayStrength.WEAK -> setOf(cn.xulun.bazi.model.GodCategory.BI, cn.xulun.bazi.model.GodCategory.YIN)
                    DayStrength.STRONG -> setOf(cn.xulun.bazi.model.GodCategory.SHI, cn.xulun.bazi.model.GodCategory.CAI, cn.xulun.bazi.model.GodCategory.GUAN)
                    DayStrength.BALANCED -> setOf(cn.xulun.bazi.model.GodCategory.YIN)
                }
                val yinDong = yGanGod == TenGod.ZHENG_CAI || yGanGod == TenGod.PIAN_CAI ||
                    // 比劫支当值（郎1995亥——胜财或败财之变年）
                    branchHiddenStems.getValue(ln.pillar.branch).firstOrNull()?.element == biEl ||
                    // 岁干支全喜用（例九1993癸酉）
                    (LiuQinXiJi.categoryOf(ln.pillar.stem.element, dayEl) in rawFav &&
                        LiuQinXiJi.categoryOf(branchHiddenStems.getValue(ln.pillar.branch).first().element, dayEl) in rawFav) ||
                    yZhiHasCai || ((yGanGod == TenGod.SHANG_GUAN || yGanGod == TenGod.SHI_SHEN) && run {
                        val keShi = Element.entries.first { it.controls == shiEl }
                        forces[shiEl]!! >= 2.0 && forces[shiEl]!! >= forces[keShi]!! // 食伤受克过重则不引（郑女士亥水遭土克）
                    }) || heDong(ln.pillar.branch) ||
                    (chongOf(ln.pillar.branch)?.let { cb -> caiBenQiPillars.any { bazi.branches[it] == cb } } == true) ||
                    // 食伤年支引动财星（例六甲申乙酉）
                    yZhiBenQiEl == shiEl ||
                    // 比劫年支为忌夺财（例六庚辰——比劫行力≥3）
                    (yZhiBenQiEl == biEl && forces[biEl]!! >= 3.0)
                if (!yinDong) return@forEach
                // 覆盖层：岁干支全喜用（按原始扶抑喜忌，不含调候翻转）；官杀支忌值年而年干不帮身；比劫支忌而力强
                val ganCatFav = LiuQinXiJi.categoryOf(ln.pillar.stem.element, dayEl) in rawFav
                val zhiCatFav = LiuQinXiJi.categoryOf(yZhiBenQiEl, dayEl) in rawFav
                val allXiYong = ganCatFav && zhiCatFav // 岁干支皆喜用（例九壬申癸酉）
                val guanElX = Element.entries.first { it.controls == dayEl }
                val guanZhiDang = yZhiBenQiEl == guanElX && cn.xulun.bazi.model.GodCategory.GUAN in d.unf &&
                    ln.pillar.stem.element != biEl // 年干帮身则官受制不克身（林1999己比肩）
                val biZhiDang = yZhiBenQiEl == biEl
                val biJie = biZhiDang && forces[biEl]!! >= 3.0 &&
                    (cn.xulun.bazi.model.GodCategory.BI in d.unf || cn.xulun.bazi.model.GodCategory.CAI in d.fav) && // 教材忌/财旺争财
                    ln.pillar.stem.element.controls != biEl // 年干官杀制劫则劫不夺财（例十三甲戌——甲木制土劫，从势而发）
                val brHe = mergeBranchHeZhan(listOf(yun.pillar.branch, ln.pillar.branch), ln.pillar.stem.element)
                val stHe = if ((yGanGod == TenGod.ZHENG_CAI || yGanGod == TenGod.PIAN_CAI) &&
                    ln.pillar.branch.element.generates == ln.pillar.stem.element
                ) {
                    // 年干财星而年支本气生之——食伤生财，干气和顺
                    HeZhan(true, "岁干财星得年支${ln.pillar.branch.chinese}本气相生（食伤生财）")
                } else mergeStemHeZhan(listOf(yun.pillar.stem, ln.pillar.stem))
                // 比劫强克财之干战——群劫争财（严格强才算）
                val jieCai = !stHe.he && stHe.desc.contains("交战") && run {
                    val fs = Element.entries.associateWith { 0.0 }.toMutableMap()
                    (bazi.stems + listOf(yun.pillar.stem, ln.pillar.stem)).forEach { fs[it.element] = fs[it.element]!! + 1.0 }
                    val hasBi = fs[biEl]!! >= 1.0
                    val hasCai = fs[caiEl]!! >= 1.0
                    // 比劫与财星两行居于干交战双方（郎2003癸未水火争财式）
                    hasBi && hasCai && (stHe.desc.contains("${biEl.chinese}${caiEl.chinese}") || stHe.desc.contains("${caiEl.chinese}${biEl.chinese}"))
                }
                val dangZhi = yGanGod == TenGod.ZHENG_CAI || yGanGod == TenGod.PIAN_CAI ||
                    branchHiddenStems.getValue(ln.pillar.branch).firstOrNull()?.element == caiEl
                val kind: CaiYunKind
                val text: String
                val chongNote = chongOf(ln.pillar.branch)?.let { cb ->
                    bazi.branches.filter { it == cb }.firstOrNull()?.let { "惟年支${ln.pillar.branch.chinese}冲${cb.chinese}，防有不愉快之事。" }
                } ?: ""
                if (allXiYong) {
                    kind = CaiYunKind.FA
                    text = "岁干支皆喜用（${ln.pillar.stem.chinese}${ln.pillar.branch.chinese}）——日干得生扶能胜财，有求财之举，财运尚可；得流年相助宜乘势而为。"
                } else if (guanZhiDang) {
                    kind = CaiYunKind.PO
                    text = "官杀${ln.pillar.branch.chinese}当值而克身（官杀为忌）——因财生灾，官司口舌而损财，此年切忌违法冒险求财。"
                } else if (biJie) {
                    kind = CaiYunKind.PO
                    text = "比劫${ln.pillar.branch.chinese}当值而比劫力强（${forces[biEl]}）——克财之忌神值年，行事务必小心谨慎，否则破财难免；破财之年的想法往往是错误的，勿动歪脑筋。"
                } else if (brHe.he && stHe.he) {
                    kind = CaiYunKind.FA
                    text = "全命局和局（${brHe.desc}；${stHe.desc}）——财星引动而全局和顺，发财之年" +
                        (if (dangZhi) "；财星当值，求财非常顺利" else "") + "，宜抓住机遇。" + chongNote
                } else if ((!brHe.he && !stHe.he) || jieCai) {
                    kind = CaiYunKind.PO
                    text = "干支双战（${brHe.desc}；${stHe.desc}）" + (if (jieCai) "天干群劫争财——" else "") + "——破财之年，防重大损失，切勿冒险投机。"
                } else {
                    val war = if (!brHe.he) brHe else stHe
                    val chongXing = war.desc.contains("冲战") || war.desc.contains("刑战") || war.desc.contains("对冲")
                    kind = if (chongXing) CaiYunKind.PO else CaiYunKind.JIN
                    text = if (chongXing) {
                        "战局（${war.desc}）——破财之年，防损失伤灾。"
                    } else {
                        "交战未解（${war.desc}）——宜谨慎小心，切勿冒险求财，图个安稳即可。"
                    }
                }
                years += CaiYunYear(ln.year, "${ln.year} ${ln.pillar.stem.chinese}${ln.pillar.branch.chinese}", kind, text)
            }
            yuns += CaiYunYun(
                pillar = yun.pillar,
                label = "${yun.pillar.fullName} ${yun.startYear}-${yun.endYear}",
                heZhan = (if (yunBranchHe.he) "支和（${yunBranchHe.desc}）" else "支战（${yunBranchHe.desc}）") +
                    (if (yunStemHe.he) "干和（${yunStemHe.desc}）" else "干战（${yunStemHe.desc}）"),
                text = sb.toString(),
                years = years,
            )
        }

        return CaiYunResult(
            baseItems = baseItems,
            yuns = yuns,
            summary = "财运三看：原局财星为用（力${forces[caiEl]}）→大运和战定基础→流年财星引动定关键年（和局发/战局破）。" +
                "财星当值与食伤之运起伏最大；具体发财破财年份以各运关键年为准，宜按规律见机行事。",
        )
    }

    /** 地支六冲对宫。 */
    private fun br2(b: EarthlyBranch): EarthlyBranch = EarthlyBranch.entries[(b.ordinal + 6) % 12]
    private fun br2OrNull(b: EarthlyBranch): EarthlyBranch = br2(b)
}
