package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.TeShuGeJuResult
import cn.xulun.bazi.model.TeShuKind
import cn.xulun.bazi.model.branchHiddenStems

/**
 * 特殊格局取用：专旺 / 假从（单从·杂从）/ 多从 / 战局通关 / 三气成象。
 * 理论见 `.shengke-reference.md` 第四九节（一）~（六）。
 *
 * 检测序：专旺（四支皆同我生我，含三合/三会变节支——从革炎上润下曲直稼穑）→
 * 从格（无有效根+日行力≤四成，透劫印被克泄殆尽者为假从；从势=有力诸行，
 * 相生者连成多从链，相战两强取通关）→ 三气成象（日主有力参与三行连环相生，
 * 弱者补之）。岁运还原：从格遇日主得禄之支 / 专旺成象遇破势之行——回正格（返回 null）。
 */
object TeShuGeJuCalculator {

    private val GRID_NAME = mapOf(
        Element.METAL to "从革格", Element.FIRE to "炎上格", Element.WATER to "润下格",
        Element.WOOD to "曲直格", Element.EARTH to "稼穑格",
    )

    /** 五行相生序中 x 与 y 之间的通关神（x 生关，关生 y）。 */
    private fun tongGuan(x: Element, y: Element): Element? = Element.entries.firstOrNull { m ->
        x.generates == m && m.generates == y
    }

    fun analyze(bazi: BaziResult, extraPillars: List<Pillar> = emptyList()): TeShuGeJuResult? {
        val dayEl = bazi.dayPillar.stem.element
        val yinEl = Element.entries.first { it.generates == dayEl }
        val f = BingYaoCalculator.forces(bazi)
        val fc = { el: Element -> f[el]!! }
        val branches = bazi.branches
        val total = Element.entries.sumOf { f[it]!! }.coerceAtLeast(0.001)

        // ── 1. 专旺：四支皆同我/生我（含变节），干无克耗 ──
        val sanHe = DiZhiRelationCalculator.analyze(bazi).sanHeList
        val sanHui = HeHuaCalculator.analyze(bazi).branchSanHuiList
        fun allyBranch(b: EarthlyBranch): Boolean {
            if (b.element == dayEl || b.element == yinEl) return true
            // 变节向同我/生我之合局成员
            val heJu = sanHe.map { it.branches to it.resultElement } + sanHui.map { it.branches to it.resultElement }
            return heJu.any { (bs, el) -> b in bs && (el == dayEl || el == yinEl) }
        }
        val allAlly = branches.all { allyBranch(it) }
        val chouKeTou = bazi.stems.any { (it.element == dayEl.controls || it.element.controls == dayEl) && it !== bazi.dayPillar.stem }
        // 专旺须日主自身有力（≥2.5）——日主力微而印众者为从印非专旺（张先生/印总表例）
        if (allAlly && !chouKeTou && fc(dayEl) >= 2.5 && fc(dayEl) + fc(yinEl) >= 0.6 * total) {
            val usedBianJie = branches.any { it.element != dayEl && it.element != yinEl } // 变节支使格不纯
            // 岁运破格：克势之行（双体或透+根）→ 还原正格
            val poEl = dayEl.controls // 我克者泄我；克我者伤势——取克我行为破格先锋
            val keEl = Element.entries.first { it.controls == dayEl }
            val broken = extraPillars.any { p ->
                (p.stem.element == keEl && p.branch.element == keEl) || (p.stem.element == poEl && p.branch.element == poEl)
            }
            if (broken) return null
            val yong = listOf(dayEl, yinEl, dayEl.generates) // 比印首选，有力食伤次之
            return TeShuGeJuResult(
                kind = TeShuKind.ZHUAN_WANG, name = GRID_NAME[dayEl]!!,
                yongShen = yong, xi = listOf(dayEl.generates), ji = listOf(keEl, poEl),
                fake = usedBianJie, restoreHint = "岁运${keEl.chinese}${poEl.chinese}破势则还原正格取用",
                basis = "四柱干支与日主一气${if (usedBianJie) "（有变节支，格不太标准）" else ""}，" +
                    "顺我者昌逆我者亡。用神首选比印（${dayEl.chinese}${yinEl.chinese}），其次有力之食伤（${dayEl.generates.chinese}）。" +
                    "岁运食伤有力则取泄秀，泄神被合绊则回专旺，木火破势则变正格。",
            )
        }

        // ── 2. 从格（单从/杂从/多从/战局） ──
        // 有效根：本气支（非刑支、非合走），藏干根须本气不克
        val xingBranches = mutableSetOf<EarthlyBranch>()
        for (grp in listOf(
            setOf(EarthlyBranch.YIN, EarthlyBranch.SI, EarthlyBranch.SHEN),
            setOf(EarthlyBranch.CHOU, EarthlyBranch.XU, EarthlyBranch.WEI),
        )) if (grp.count { it in branches } >= 2) xingBranches += grp.filter { it in branches }
        // 本气根：本气支非刑支，且非合局变节成员（辰在申子辰水局内不算土根——总表例）
        val heJu = sanHe.map { it.branches to it.resultElement } + sanHui.map { it.branches to it.resultElement }
        val bianJieBranches = heJu.flatMap { (bs, _) -> bs }
        val hasBenQiRoot = branches.any { it.element == dayEl && it !in xingBranches &&
            (it !in bianJieBranches || heJu.any { (bs, el) -> it in bs && el == dayEl }) }
        if (!hasBenQiRoot) {
            // 变节力转移：合局成员本气之力与月令加成随化神计（丑在亥子丑水局内之力归水）
            val fAdj = f.toMutableMap()
            for ((bs, el) in heJu) for (b in bs) {
                if (b.element != el && b in branches) { fAdj[b.element] = fAdj[b.element]!! - 1; fAdj[el] = fAdj[el]!! + 1 }
            }
            val rulerB = bazi.monthPillar.branch
            heJu.firstOrNull { (bs, el) -> rulerB in bs && el != rulerB.element }?.let { (bs, el) ->
                fAdj[rulerB.element] = fAdj[rulerB.element]!! - 2; fAdj[el] = fAdj[el]!! + 2
            }
            val fcAdj = { el: Element -> fAdj[el]!! }
            val maxOther = Element.entries.filter { it != dayEl }.maxOf { fAdj[it]!! }
            // 透干比劫/印：被克泄殆尽或坐下截脚者无力（假从之"虚助"）
            // 印为势首时印透不算虚助（从印格之戊己——印是用神非帮扶）
            val yinIsShi = fAdj[yinEl]!! >= 2.5 && fAdj[yinEl]!! >= maxOther
            val helperStems = listOf(bazi.yearPillar.stem, bazi.monthPillar.stem, bazi.hourPillar.stem)
                .filter { it.element == dayEl || (it.element == yinEl && !yinIsShi) }
            val weakHelpers = helperStems.filter { s ->
                val self = fAdj[s.element]!!
                val foes = Element.entries.filter { e -> e.controls == s.element || s.element.controls == e }.sumOf { fAdj[it]!! }
                foes >= 2.5 * self
            }
            // 截脚按实际柱位（枚举同值防首配错柱）；湿土（丑辰）不截水根——藏水之根（A5 教材例）
            val jieJiaoStems = helperStems.filter { s ->
                listOf(bazi.yearPillar, bazi.monthPillar, bazi.hourPillar).any { pl ->
                    pl.stem === s && pl.branch.element.controls == s.element &&
                        !(s.element == Element.WATER && (pl.branch == EarthlyBranch.CHOU || pl.branch == EarthlyBranch.CHEN))
                }
            }
            // 合化入势从真：透劫/印与另一透干五合且化神在从势内（癸合戊化火/乙合庚化金）——不以虚助论
            val heHuaRuShi = helperStems.filter { s ->
                bazi.stems.any { o ->
                    o !== bazi.dayPillar.stem && o !== s &&
                        (s.ordinal - o.ordinal + 10) % 5 == 0 && s.ordinal != o.ordinal
                }
            }
            val weakSet = (jieJiaoStems + weakHelpers).toSet() - heHuaRuShi.toSet()
            val fake = weakSet.isNotEmpty()
            // 日行有效力：透干虚助被殆尽/截脚者折尽
            val effDayLine = (fcAdj(dayEl) - weakSet.size).coerceAtLeast(0.0)
            if (effDayLine <= 0.4 * maxOther) {
                // 从势：有力诸行（≥2.5），相生连链；连接行力可低
                val others = Element.entries.filter { it != dayEl }
                val strong = others.filter { fAdj[it]!! >= 2.5 }
                if (strong.isNotEmpty()) {
                    // 链组装：自最强行向前向后延（生我者/我生者中在 strong 或为连接行）
                    val maxEl = strong.maxBy { fAdj[it]!! }
                    val chain = linkedSetOf(maxEl)
                    var grew = true
                    while (grew) {
                        grew = false
                        for (e in others) {
                            if (e in chain) continue
                            val tou = bazi.stems.any { it.element == e && it !== bazi.dayPillar.stem }
                            // 强行直入；弱端只顺下游（链生它，势之所趋）——上游饲喂者被吸尽不入；
                            // 印行不入从链，唯力≥2 且透干者例外（火土金之印金——三气从格例二）
                            val downstream = chain.any { c -> c.generates == e }
                            val yinOk = e != yinEl || (fAdj[e]!! >= 2 && tou)
                            if (e in strong || (fAdj[e]!! >= 1 && downstream && yinOk)) {
                                chain += e; grew = true
                            }
                        }
                    }
                    // 上游饲喂者入链：力≥2 且透干（火生土式——杨先生例）；各倚旺势：链≥2行时力≥2且透干之非印行不拘生克（水木土——例三）；多趟回扫
                    var grew2 = true
                    while (grew2) {
                        grew2 = false
                        for (e in others) {
                            if (e in chain) continue
                            val tou = bazi.stems.any { it.element == e && it !== bazi.dayPillar.stem }
                            if (fAdj[e]!! >= 2 && tou && chain.any { c -> e.generates == c }) { chain += e; grew2 = true }
                            else if (chain.size >= 2 && fAdj[e]!! >= 2 && tou && e != yinEl) { chain += e; grew2 = true }
                        }
                    }
                    // 相生拓扑序：先生它者为链首
                    val chainList = mutableListOf<Element>()
                    val rest = chain.toMutableSet()
                    while (rest.isNotEmpty()) {
                        val head = rest.firstOrNull { e -> rest.none { c -> c.generates == e } } ?: rest.first()
                        chainList += head; rest.remove(head)
                    }
                    // 战局：链内/力前两行相克力差小 → 通关
                    val top2 = others.sortedByDescending { fAdj[it]!! }.take(2)
                    if (top2.size == 2) {
                        val (a, b) = top2
                        val xKeY = a.controls == b; val yKeX = b.controls == a
                        if ((xKeY || yKeX) && kotlin.math.abs(fAdj[a]!! - fAdj[b]!!) <= 1.5) {
                            val (winner, loser) = if (xKeY) a to b else b to a
                            val guan = tongGuan(winner, loser)
                            // 战局仅当：通关神即日主（弃命取己通关——王先生式）；
                            // 通关神非日主者（火金器皿相战式）入两气从格——多从格例三
                            if (guan != null && guan == dayEl) {
                                // 岁运还原：日主得禄之支
                                if (extraPillars.any { it.branch.element == dayEl }) return null
                                return TeShuGeJuResult(
                                    kind = TeShuKind.ZHAN_JU, name = "${a.chinese}${b.chinese}交战",
                                    yongShen = listOf(guan), xi = emptyList(), ji = listOf(a, b),
                                    fake = fake, restoreHint = "岁运${guan.chinese}到位解战；日主得根则还原正格",
                                    basis = "日主弱极无依，气势在${a.chinese}${b.chinese}而两强相战势均力敌——依附谁都不理想，" +
                                        "首取通关之${guan.chinese}以解战局（战者和为贵，合平衡流通原则）。宜北方水地江边居住等趋吉避凶。",
                                )
                            }
                        }
                    }
                    // 官杀双透且官力不足（非强势成员）且未被彻底制住 → 阻从（调候矛盾局壬癸双透例）；
                    // 官被克官之行彻底制住（≥2.5×官力——丙戌庚寅甲午庚午例火6制金2）不阻
                    val guanEl0 = Element.entries.first { it.controls == dayEl }
                    val guanTouCount = listOf(bazi.yearPillar.stem, bazi.monthPillar.stem, bazi.hourPillar.stem)
                        .count { it.element == guanEl0 }
                    val zhiGuanEl = Element.entries.first { it.controls == guanEl0 }
                    val guanCrushed = fAdj[zhiGuanEl]!! >= 2.5 * fAdj[guanEl0]!!
                    if (fAdj[guanEl0]!! < 2.5 && guanTouCount >= 2 && !guanCrushed) {
                        // 落入三气成象/正格
                    } else {
                    // 岁运还原：日主得禄之支（如庚禄在申）或印得本气根（如壬通根子生日干——朱先生丙子年）→ 回正格身弱
                    if (extraPillars.any { it.branch.element == dayEl || it.branch.element == yinEl }) return null
                    val kind = if (chainList.size == 1) TeShuKind.JIA_CONG
                    else if (fake) TeShuKind.JIA_CONG else TeShuKind.DUO_CONG
                    // 命名按势首（最强成员）与日主之关系：財→从财、官→从杀、食伤→从儿、印→从印
                    // （杨先生火財5:4土官→从财；丁巳例火食6:2土財→从儿——同构异名，关系定之）
                    val shiShou = chainList.maxBy { fAdj[it]!! }
                    val relName = when {
                        shiShou == dayEl.controls -> "从财"
                        shiShou.controls == dayEl -> "从杀"
                        shiShou == dayEl.generates -> "从儿"
                        shiShou == yinEl -> "从印"
                        else -> "从"
                    }
                    val name = (if (fake) "假" else "") + relName + "格"
                    val rootDesc = if (fake) "日主无有效根（透干之助被克泄殆尽或截脚无力，假从），" else "日主无有效根，弱极无依，"
                    val chainText = chainList.joinToString("") { it.chinese }
                    val ji = listOf(yinEl, dayEl).filter { it !in chainList }
                    return TeShuGeJuResult(
                        kind = kind, name = name,
                        yongShen = chainList, xi = chainList.drop(1), ji = ji,
                        fake = fake,
                        restoreHint = "贴身比劫伺机而动，岁运${dayEl.chinese}得禄（如${luZhiOf(dayEl)}）则还原正格身弱论",
                        basis = rootDesc + "弃命从" + chainText + "——" + chainText +
                            (if (chainList.size >= 3) "连续相生" else "相生为势") +
                            "，取" + chainText + "为喜用，忌印比逆势。" + (if (fake) "逢流年亦可还原，切不可一味按从格断。" else ""),
                    )
                    }
                }
            }
        }

        // ── 3. 三气成象：日主有力参与三行连环相生，弱者补之 ──
        val engine = StrengthEngine.analyze(bazi)
        if (engine.verdict != DayStrength.WEAK) {
            for (start in Element.entries) {
                val mid = start.generates
                val end = mid.generates
                if (start == end) continue
                val lines = listOf(start, mid, end)
                if (dayEl !in lines) continue
                if (lines.all { f[it]!! >= 2 } && Element.entries.filter { it !in lines }.all { f[it]!! <= 1.5 }) {
                    val sorted = lines.sortedBy { f[it]!! } // 弱者在前
                    // 破格之行值运有力 → 正格取用
                    val poLines = Element.entries.filter { it !in lines }
                    if (extraPillars.any { p -> p.stem.element in poLines && p.branch.element in poLines }) return null
                    return TeShuGeJuResult(
                        kind = TeShuKind.SAN_QI,
                        name = lines.joinToString("") { it.chinese } + "三气成象",
                        yongShen = sorted, xi = sorted.drop(1), ji = poLines,
                        fake = false, restoreHint = "破格之行（${poLines.joinToString("") { it.chinese }}）值运有力则按正格取用",
                        basis = "日主有力，${lines.joinToString("→") { it.chinese }}三气成象秀气流通。" +
                            "三行中弱者补之：首补${sorted.first().chinese}（原局${sorted.joinToString("") { "${it.chinese}${fmtI(f[it]!!)}" }}）。" +
                            "岁运用神到位（补弱之行）则兴旺；破格之行值运有力则按正格取用。",
                    )
                }
            }
        }
        return null
    }

    private fun branchElementOf(bazi: BaziResult, s: cn.xulun.bazi.model.HeavenlyStem): Element? =
        pillarOfStem(bazi, s)?.branch?.element

    private fun pillarOfStem(bazi: BaziResult, s: cn.xulun.bazi.model.HeavenlyStem): Pillar? =
        bazi.pillars.firstOrNull { it.stem == s }

    /** 日主禄位支名（用于还原提示）。 */
    private fun luZhiOf(el: Element): String = when (el) {
        Element.WOOD -> "寅卯"; Element.FIRE -> "巳午"; Element.EARTH -> "午辰戌丑未"
        Element.METAL -> "申酉"; Element.WATER -> "亥子"
    }

    private fun fmtI(d: Double): String = if (d == d.toLong().toDouble()) d.toLong().toString() else String.format("%.1f", d)
}
