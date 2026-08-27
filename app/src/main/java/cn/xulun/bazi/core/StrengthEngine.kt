package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.GanZhiStrength
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.StrengthResult
import cn.xulun.bazi.model.branchHiddenStems
import kotlin.math.min

/**
 * 干支强弱引擎（八步法）。理论见 `.shengke-reference.md` 第卅五~卅八节。
 *
 * 八步对应：
 * 1 A/B 粗分 → [analyze] 聚合段；2 月令初称 → 旺相休囚死基值（行运时标尺换大运支，第卅二节）；
 * 3 变节修正 → 合化拉走（三会非本行×0.5、被六合化去×0.3）；4 合化记账 → 三会/三合/半合/拱局/六合/天干五合；
 * 5 四得 → 党众+根，孤立无援×0.6；6 紧密度 → 位置权重（日支>月干>时干>月支>时支>年干>年支）；
 * 7 单柱关系 → 双体/覆/载/盖头/截脚 + 贴邻生克修正；
 * 8 终判 → A=Σ(分×紧密度×关系系数)，含印通关转移、日主无根不受生（假生为克）、克我者被食伤牵制。
 *
 * 锚点：教材排序例一（亥子甲未己丑辛丁）、例二含运（寅乙甲午丙丁戊丑辰亥）、十一式判定、病药16例。
 */
object StrengthEngine {

    // ── 基值与系数（锚点调定，出处见注释） ──
    private val SEASON_BASE = doubleArrayOf(1.0, 0.75, 0.45, 0.35, 0.2) // 旺/相/休/囚/死
    private const val DANG_LING_BONUS = 0.5       // 当令支亲秉（标尺本支，亥旺于子）
    private const val COMRADE_BONUS = 0.25        // 同党一支
    private const val ROOT_BONUS = 0.12           // 异行支中藏根一处
    private const val LONELY_K = 0.6              // 孤立无援（无比劫无根）
    private const val HE_ZHU_K = 0.75             // 天干五合绊住耗气（甲被己合绊）
    private const val SAN_HUI_NATIVE_K = 1.1      // 三会局中本行成员增益
    private const val SAN_HUI_DRAG_K = 0.5        // 三会局非本行成员被拖（丑被亥子会）
    private const val LIU_HE_DRAG_K = 0.3         // 被六合化去失去自身作用（亥被寅合化）
    private const val SAN_HE_ZHONG_K = 1.05       // 三合成局中神
    private const val SAN_HE_MEMBER_K = 0.45      // 三合成局非本行成员（移情别恋，重罚）
    private const val BAN_HE_SHENG_K = 0.95       // 生地半合（寅午）
    private const val BAN_HE_MU_K = 0.85          // 墓地半合
    private const val GONG_K = 0.9                // 拱局
    private const val CHONG_K = 0.85              // 六冲动摇根基
    private const val SHUANG_TI_K = 1.05          // 双体（天覆地载）
    private const val FU_ZAI_GAIN = 1.15          // 受覆/受载增益
    private const val FU_ZAI_DRAIN = 0.9          // 施覆/施载泄气
    private const val GAI_TOU_K = 0.8             // 被盖头
    private const val GAI_TOU_DRAIN = 0.95        // 盖头耗力（克弱者耗轻）
    private const val JIE_JIAO_K = 0.8            // 被截脚
    private const val ADJ_CTRL_K = 0.8            // 贴邻被克
    private const val ADJ_DRAIN_K = 0.9           // 贴邻泄于旺者
    private const val ADJ_HAO_K = 0.85            // 贴邻耗于旺者（衰土反被旺水耗）
    private const val ADJ_HELP_K = 1.05           // 贴邻得生
    private const val COLD_GEN_K = 0.5            // 冬月水生木力减（寒水生木作用不大）

    // 关系系数（聚合）
    private const val REL_TONG = 0.95             // 同我
    private const val REL_SHENG = 1.0             // 生我
    private const val REL_KE = 1.15               // 克我（克重于泄）
    private const val REL_XIE = 0.55             // 我生（泄为秀，损力轻）
    private const val REL_HAO = 0.85              // 我克
    private const val TRANSFER_K = 0.8            // 印通关转移（两面撮合，化干戈为玉帛）
    private const val ZHI_HUN_K = 0.65            // 克我者被食伤牵制（水忙御敌无暇克身）
    private const val STRONG_TH = 0.15            // 终判阈值（r=(A-B)/(A+B)）
    private const val WEAK_TH = -0.10

    // ── 合会表 ──
    private val SAN_HUI = listOf(
        setOf(EarthlyBranch.HAI, EarthlyBranch.ZI, EarthlyBranch.CHOU) to Element.WATER,
        setOf(EarthlyBranch.YIN, EarthlyBranch.MAO, EarthlyBranch.CHEN) to Element.WOOD,
        setOf(EarthlyBranch.SI, EarthlyBranch.WU, EarthlyBranch.WEI) to Element.FIRE,
        setOf(EarthlyBranch.SHEN, EarthlyBranch.YOU, EarthlyBranch.XU) to Element.METAL,
        setOf(EarthlyBranch.CHEN, EarthlyBranch.XU, EarthlyBranch.CHOU, EarthlyBranch.WEI) to Element.EARTH,
    )
    private val SAN_HE = listOf(
        setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN) to Element.WATER,
        setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI) to Element.WOOD,
        setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU) to Element.FIRE,
        setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU) to Element.METAL,
    )
    private val ZHONG_SHEN = setOf(EarthlyBranch.ZI, EarthlyBranch.WU, EarthlyBranch.MAO, EarthlyBranch.YOU)
    private val SHENG_DI = setOf(EarthlyBranch.SHEN, EarthlyBranch.HAI, EarthlyBranch.YIN, EarthlyBranch.SI)
    private val LIU_HE = mapOf(
        EarthlyBranch.ZI to EarthlyBranch.CHOU, EarthlyBranch.YIN to EarthlyBranch.HAI,
        EarthlyBranch.MAO to EarthlyBranch.XU, EarthlyBranch.CHEN to EarthlyBranch.YOU,
        EarthlyBranch.SI to EarthlyBranch.SHEN, EarthlyBranch.WU to EarthlyBranch.WEI,
    )
    private val LIU_HE_ELEMENT = mapOf(
        "子丑" to Element.EARTH, "寅亥" to Element.WOOD, "卯戌" to Element.FIRE,
        "辰酉" to Element.METAL, "巳申" to Element.WATER, "午未" to Element.EARTH,
    )
    private val CHONG = setOf(
        setOf(EarthlyBranch.ZI, EarthlyBranch.WU), setOf(EarthlyBranch.CHOU, EarthlyBranch.WEI),
        setOf(EarthlyBranch.YIN, EarthlyBranch.SHEN), setOf(EarthlyBranch.MAO, EarthlyBranch.YOU),
        setOf(EarthlyBranch.CHEN, EarthlyBranch.XU), setOf(EarthlyBranch.SI, EarthlyBranch.HAI),
    )

    /** 单元：一个干或一个支。 */
    private class Unit(
        val label: String,
        val chinese: String,
        val isStem: Boolean,
        val element: Element,
        val pillar: Int,        // 0年 1月 2日 3时 4运 5岁
        val branch: EarthlyBranch?, // 支单元的支
        val stem: HeavenlyStem?,     // 干单元的干
        var score: Double,
        /** 变节后的有效五行（三会/三合/六合拉走向化神），聚合按此计；null=未变节 */
        var effectiveElement: Element? = null,
        val notes: MutableList<String> = mutableListOf(),
    )

    private fun proximity(pillar: Int, isStem: Boolean): Double = when (pillar) {
        2 -> if (isStem) 0.0 else 1.0      // 日支最近（日干自身不计）
        1 -> if (isStem) 0.95 else 0.85    // 月干/月支
        3 -> if (isStem) 0.90 else 0.80    // 时干/时支
        0 -> if (isStem) 0.70 else 0.65    // 年干/年支
        else -> 0.85                        // 运/岁
    }

    fun analyze(bazi: BaziResult, extraPillars: List<Pillar> = emptyList()): StrengthResult {
        val pillars = bazi.pillars + extraPillars
        val stemLabels = listOf("年干", "月干", "日干", "时干") + List(extraPillars.size) { if (it == 0) "运干" else "岁干" }
        val branchLabels = listOf("年支", "月支", "日支", "时支") + List(extraPillars.size) { if (it == 0) "运支" else "岁支" }

        val units = mutableListOf<Unit>()
        pillars.forEachIndexed { i, p ->
            val pi = if (i < 4) i else 4
            units += Unit(stemLabels[i], p.stem.chinese, true, p.stem.element, pi, null, p.stem, 0.0)
            units += Unit(branchLabels[i], p.branch.chinese, false, p.branch.element, pi, p.branch, null, 0.0)
        }

        // ── 第2步 月令初称（标尺：运支 > 月支） ──
        val ruler = extraPillars.firstOrNull()?.branch ?: bazi.monthPillar.branch
        val season = seasonOf(ruler) // element -> 0旺 1相 2休 3囚 4死
        units.forEach { u ->
            u.score = SEASON_BASE[season[u.element]!!]
            u.notes += arrayOf("旺", "相", "休", "囚", "死")[season[u.element]!!]
        }
        // 当令支（仅标尺柱本支；他柱同支不沾）
        val rulerPillarIdx = if (extraPillars.isNotEmpty()) 4 else 1
        units.filter { it.branch != null && it.branch == ruler && it.pillar == rulerPillarIdx }.forEach {
            it.score += DANG_LING_BONUS; it.notes += "当令"
        }

        // ── 第5步 四得：党众与天干一气（藏根延后至合化判定，合局伙伴不喂根） ──
        val stemCountByEl = units.filter { it.isStem }.groupBy { it.element }.mapValues { it.value.size }
        units.forEach { u ->
            val comrades = units.count { it !== u && it.element == u.element }
            u.score += comrades * COMRADE_BONUS
            if (comrades > 0) u.notes += "党众×$comrades"
            if (u.isStem && (stemCountByEl[u.element] ?: 0) >= 3) {
                // 一气须有净根方成势（三金虚浮无用）
                val hasRoot = units.any { o -> o !== u && o.branch != null && o.element != u.element &&
                    branchHiddenStems[o.branch]!!.any { it.element == u.element } } ||
                    (u.branch == null && false)
                if (hasRoot) { u.score += 0.5; u.notes += "天干一气" }
            }
        }

        // ── 第4步 合化记账 ──
        val branchUnits = units.filter { it.branch != null }
        val present = branchUnits.map { it.branch!! }.toSet()
        // 三会（力大优先，参与三会之支不再论六合）
        val sanHuiMembers = mutableSetOf<EarthlyBranch>()
        val committed = mutableMapOf<EarthlyBranch, Element>() // 支所投合局化神
        for ((grp, el) in SAN_HUI) if (grp.all { it in present }) {
            sanHuiMembers += grp
            branchUnits.filter { it.branch in grp }.forEach {
                val k = if (it.element == el) SAN_HUI_NATIVE_K else SAN_HUI_DRAG_K
                it.score *= k
                it.notes += if (it.element == el) "三会${el.chinese}局" else "三会拖向${el.chinese}"
                committed[it.branch!!] = el
                if (it.element != el) { it.effectiveElement = el; it.notes += "变节向${el.chinese}" }
            }
        }
        // 三合 / 半合 / 拱局
        for ((grp, el) in SAN_HE) {
            val have = grp.count { it in present }
            val members = branchUnits.filter { it.branch in grp }
            when {
                have == 3 -> members.forEach {
                    val k = if (it.element == el) SAN_HE_ZHONG_K else SAN_HE_MEMBER_K
                    it.score *= k
                    it.notes += "三合${el.chinese}局" + if (it.branch in ZHONG_SHEN) "(中神)" else ""
                    committed[it.branch!!] = el
                    if (it.element != el) { it.effectiveElement = el; it.notes += "变节向${el.chinese}" }
                }
                have == 2 && grp.count { it in ZHONG_SHEN } == 1 && present.any { it in ZHONG_SHEN && it in grp } -> {
                    // 半合：含中神
                    members.forEach {
                        val isZhong = it.branch in ZHONG_SHEN
                        val isShengDi = it.branch in SHENG_DI
                        val k = when {
                            isZhong -> SAN_HE_ZHONG_K
                            isShengDi -> BAN_HE_SHENG_K
                            else -> BAN_HE_MU_K
                        }
                        it.score *= k
                        it.notes += "半合${el.chinese}局"; committed[it.branch!!] = el
                    }
                }
                have == 2 && grp.none { it in ZHONG_SHEN && it in present } && present.containsAll(grp - ZHONG_SHEN) -> {
                    // 拱局：生地+墓地
                    members.forEach { it.score *= GONG_K; it.notes += "拱${el.chinese}局" }
                }
            }
        }
        // 六合（三会成员跳过）
        val draggedBranches = mutableSetOf<EarthlyBranch>()
        for ((a, b) in LIU_HE) {
            if (a in present && b in present && a !in sanHuiMembers && b !in sanHuiMembers) {
                val key = "${a.chinese}${b.chinese}".let { if (LIU_HE_ELEMENT.containsKey(it)) it else "${b.chinese}${a.chinese}" }
                val el = LIU_HE_ELEMENT[key]!!
                val ua = branchUnits.filter { it.branch == a }
                val ub = branchUnits.filter { it.branch == b }
                val aNative = ua.first().element == el
                val bNative = ub.first().element == el
                if (aNative != bNative) {
                    // 化向本行一方，另一方被合走
                    val (dragged, native) = if (aNative) ub to ua else ua to ub
                    val nativeStrong = native.maxOf { it.score } > dragged.maxOf { it.score }
                    if (nativeStrong) {
                        dragged.forEach { it.score *= LIU_HE_DRAG_K; it.notes += "被${key}合化失去自身"; it.effectiveElement = el; draggedBranches += it.branch!! }
                        native.forEach { it.notes += "合住${key}" }
                    }
                }
            }
        }
        // 三会拖走的支，其藏根亦不可倚（根被拉走）
        branchUnits.forEach { if (it.notes.any { n -> n.startsWith("三会拖向") }) draggedBranches += it.branch!! }
        // 藏根复盘（合化判定后）：拖走支不喂根；所投合局化神同我行之支不喂根（力已付与局）
        fun rootGiver(other: Unit, el: Element): Boolean =
            other.branch != null && other.element != el && other.branch !in draggedBranches &&
                committed[other.branch] != el &&
                branchHiddenStems[other.branch]!!.any { it.element == el }
        units.forEach { u ->
            val roots = units.count { it !== u && rootGiver(it, u.element) }
            if (roots > 0) { u.score += roots * ROOT_BONUS; u.notes += "藏根×$roots" }
            val selfRooted = u.branch != null && u.branch !in draggedBranches && committed[u.branch] != u.element &&
                branchHiddenStems[u.branch]!!.any { it.element == u.element }
            val hasComrade = units.any { it !== u && it.element == u.element }
            if (!hasComrade && roots == 0 && !selfRooted) {
                u.score *= LONELY_K; u.notes += "孤立无援"
            }
        }
        // 六冲
        for (pair in CHONG) {
            val (a, b) = pair.toList()
            if (a in present && b in present) {
                branchUnits.filter { it.branch == a || it.branch == b }.forEach {
                    it.score *= CHONG_K; it.notes += "${a.chinese}${b.chinese}冲"
                }
            }
        }
        // 天干五合（合绊）：干序差 5
        val stemUnits = units.filter { it.isStem }
        for (i in stemUnits.indices) for (j in i + 1 until stemUnits.size) {
            val s1 = stemUnits[i].stem!!
            val s2 = stemUnits[j].stem!!
            if ((s1.ordinal - s2.ordinal + 10) % 5 == 0 && s1.ordinal != s2.ordinal) {
                stemUnits[i].score *= HE_ZHU_K; stemUnits[i].notes += "与${stemUnits[j].chinese}合绊"
                stemUnits[j].score *= HE_ZHU_K; stemUnits[j].notes += "与${stemUnits[i].chinese}合绊"
            }
        }

        // 坐支禄刃根（日支为日主禄/刃位，贴身强根）
        val dayStem = bazi.dayPillar.stem
        val luRen = luRenOf(dayStem)
        val dayBranchUnit = units.firstOrNull { it.pillar == 2 && it.branch != null }
        if (dayBranchUnit != null && dayBranchUnit.branch in luRen) {
            dayBranchUnit.score += 0.4; dayBranchUnit.notes += "坐支禄刃根"
        }

        // ── 第7步 单柱关系（units 按柱成对：[2i]=干 [2i+1]=支） ──
        for (pi in pillars.indices) {
            val s = units[pi * 2]
            val b = units[pi * 2 + 1]
            val (se, be) = s.element to b.element
            when {
                se == be -> { s.score *= SHUANG_TI_K; b.score *= SHUANG_TI_K; s.notes += "双体"; b.notes += "双体" }
                se.generates == be -> { b.score *= FU_ZAI_GAIN; s.score *= FU_ZAI_DRAIN; b.notes += "得覆"; s.notes += "覆而泄" }
                be.generates == se -> { s.score *= FU_ZAI_GAIN; b.score *= FU_ZAI_DRAIN; s.notes += "得载"; b.notes += "载而泄" }
                se.controls == be -> { b.score *= GAI_TOU_K; s.score *= GAI_TOU_DRAIN; b.notes += "被盖头"; s.notes += "盖头耗" }
                else -> { s.score *= JIE_JIAO_K; b.score *= GAI_TOU_DRAIN; s.notes += "被截脚"; b.notes += "截脚耗" }
            }
        }

        // ── 第7步续 贴邻生克（用现值一轮修正） ──
        for (pi in 0 until pillars.size - 1) {
            adjPair(units[pi * 2], units[(pi + 1) * 2])         // 干与干
            adjPair(units[pi * 2 + 1], units[(pi + 1) * 2 + 1]) // 支与支
        }

        // 主宰之柱掌权（第三十二节：一步大运掌本运十年之权；标尺运柱两单元加力）
        val rulerPillar = if (extraPillars.isNotEmpty()) 4 else -1
        units.forEach { if (it.pillar == rulerPillar) { it.score *= 1.5; it.notes += "运柱掌权" } }
        // 旺敌压制：克我之行总力≥3倍自身→重压（风中残烛）
        units.forEach { u ->
            val enemyEl = Element.entries.first { it.controls == u.element }
            val enemyTotal = units.filter { it !== u && it.element == enemyEl }.sumOf { it.score }
            if (enemyTotal >= 3.5 * u.score) { u.score *= 0.75; u.notes += "被旺${enemyEl.chinese}重压" }
        }
        // 排序产出
        val sorted = units.sortedWith(compareByDescending<Unit> { it.score }.thenBy { it.pillar })
        val ranking = sorted.joinToString("") { it.chinese }

        // ── 第1/6/8步 聚合终判 ──
        val dayEl = bazi.dayPillar.stem.element
        val dayRooted = units.any { it.branch != null && branchHiddenStems[it.branch]!!.any { h -> h.element == dayEl } }
        var a = 0.0; var b = 0.0
        val yinUnits = mutableListOf<Unit>(); val guanUnits = mutableListOf<Unit>()
        // 预计算（避免遍历顺序依赖）
        val guanPre = units.filter { it.label != "日干" && (it.effectiveElement ?: it.element).controls == dayEl }
        val guanPreTotal = guanPre.sumOf { it.score }
        val shiEl0 = dayEl.generates
        val shiRealUnits = units.filter { (it.effectiveElement ?: it.element) == shiEl0 && it.label != "日干" }
        val shiHiddenCount = units.filter { it.branch != null }.sumOf { bb -> branchHiddenStems[bb.branch]!!.count { it.element == shiEl0 } }
        val yinEl0 = Element.entries.first { it.generates == dayEl }
        val yinFeed0 = if (shiRealUnits.isNotEmpty())
            units.filter { (it.effectiveElement ?: it.element) == yinEl0 && it.label != "日干" }.sumOf { it.score } * 0.8 else 0.0
        val shiTotalPre = shiRealUnits.sumOf { it.score } + ROOT_BONUS * shiHiddenCount + yinFeed0
        val zhiK = when {
            shiTotalPre >= 0.45 * guanPreTotal -> 0.4
            shiTotalPre >= 0.25 * guanPreTotal -> ZHI_HUN_K
            else -> 1.0
        }
        units.filter { it.label != "日干" }.forEach { u ->
            val ue = u.effectiveElement ?: u.element
            val rel = when {
                ue == dayEl -> REL_TONG
                ue.generates == dayEl -> REL_SHENG
                ue.controls == dayEl -> REL_KE
                dayEl.generates == ue -> REL_XIE
                else -> REL_HAO
            }
            var eff = u.score * proximity(u.pillar, u.isStem) * rel
            if (ue.generates == dayEl) yinUnits += u
            if (ue.controls == dayEl) guanUnits += u
            if (rel == REL_SHENG && !dayRooted) {
                // 日主无根不受生（假生为克，第一节）
                b += eff * 0.8; u.notes += "身无根不受生反埋"
                return@forEach
            }
            if (rel == REL_KE && zhiK < 1.0) {
                // 克我者被食伤牵制（坐收渔利；足制=土得火后盾）
                eff = u.score * proximity(u.pillar, u.isStem) * zhiK
            }
            if (rel == REL_SHENG || rel == REL_TONG) a += eff else b += eff
        }
        // 印通关转移
        val yinTotal = yinUnits.sumOf { it.score }
        val guanTotal = guanUnits.sumOf { it.score }
        if (guanTotal > 0 && yinTotal > 0 && yinRooted(bazi, dayEl)) {
            // 通关用神必须有力（第卅九节）：印透干或两支以上方堪全数转化（两面撮合）；
            // 印单支不透则原局无通关（唯靠岁运），官远强于印有形无实（忌神越关照克）
            val yinEl = Element.entries.first { e -> e.generates == dayEl }
            val yinStrong = units.any { it.isStem && it.element == yinEl } ||
                units.count { (it.effectiveElement ?: it.element) == yinEl } >= 2
            // 印不力（单支不透）：全数转化不给（原局无通关，唯靠岁运）；
            // 官远强于印的有形无实档仍可吸纳一部分（第卅九节通关一例）
            val t = when {
                !yinStrong && guanTotal <= yinTotal * 2.2 -> 0.0
                guanTotal <= yinTotal * 2.2 -> min(guanTotal, yinTotal * 2) * TRANSFER_K
                else -> yinTotal * 0.4
            }
            b -= t; a += t * 0.8
        }
        // 寒暖燥湿折减：寒月（亥子丑）非水日主、燥月（巳午未）非火土日主，扶方×0.9（调候章）
        val m = bazi.monthPillar.branch
        val cold = m == EarthlyBranch.HAI || m == EarthlyBranch.ZI || m == EarthlyBranch.CHOU
        val zao = m == EarthlyBranch.SI || m == EarthlyBranch.WU || m == EarthlyBranch.WEI
        val fireTotal = units.filter { it.element == Element.FIRE && it.label != "日干" }.sumOf { it.score } +
            ROOT_BONUS * units.count { it.branch != null && branchHiddenStems[it.branch]!!.any { h -> h.element == Element.FIRE } }
        val waterTotal = units.filter { it.element == Element.WATER && it.label != "日干" }.sumOf { it.score } +
            ROOT_BONUS * units.count { it.branch != null && branchHiddenStems[it.branch]!!.any { h -> h.element == Element.WATER } }
        val coldLackFire = cold && dayEl != Element.WATER && fireTotal < 1.0
        val zaoLackWater = zao && dayEl != Element.FIRE && dayEl != Element.EARTH && waterTotal < 1.0
        if (coldLackFire || zaoLackWater) a *= 0.84
        val r = if (a + b == 0.0) 0.0 else (a - b) / (a + b)
        val verdict = when {
            r >= STRONG_TH -> DayStrength.STRONG
            r <= WEAK_TH -> DayStrength.WEAK
            else -> DayStrength.BALANCED
        }
        val basis = "标尺${ruler.chinese}（${seasonText(ruler)}）。扶方${fmt(a)}对耗方${fmt(b)}，比值${fmt(r)}。" +
            (if (!dayRooted) "日主虚浮无根，印多不受生（假生为克）。" else "") +
            (if (guanTotal > 0 && yinTotal > 0 && yinRooted(bazi, dayEl)) "印通关转移${fmt(min(guanTotal, yinTotal) * TRANSFER_K)}。" else "")
        return StrengthResult(
            items = sorted.map { GanZhiStrength(it.label, it.chinese, it.element, it.score, it.notes.joinToString("，")) },
            ranking = ranking, aSide = a, bSide = b, verdict = verdict, basis = basis,
        )
    }

    // ── 辅助 ──

    /** 旺相休囚死：当令者旺，令生者相，生令者休，克令者囚，令克者死。 */
    private fun seasonOf(ruler: EarthlyBranch): Map<Element, Int> {
        val l = ruler.element
        return Element.entries.associateWith { el ->
            when {
                el == l -> 0
                l.generates == el -> 1
                el.generates == l -> 2
                el.controls == l -> 3
                else -> 4
            }
        }
    }

    private fun seasonText(ruler: EarthlyBranch): String {
        val s = seasonOf(ruler)
        return Element.entries.joinToString("") { it.chinese + arrayOf("旺", "相", "休", "囚", "死")[s[it]!!] }
    }

    private fun yinRooted(bazi: BaziResult, dayEl: Element): Boolean {
        val yinEl = Element.entries.first { it.generates == dayEl }
        return bazi.pillars.any { p -> branchHiddenStems[p.branch]!!.any { it.element == yinEl } }
    }

    private fun adjPair(x: Unit, y: Unit) {
        // x 对 y 的作用与 y 对 x 的作用（贴邻）
        applyAdj(x, y); applyAdj(y, x)
    }

    private fun applyAdj(from: Unit, to: Unit) {
        val fe = from.element; val te = to.element
        val cold = fe == Element.WATER && te == Element.WOOD && to.pillar == 1 // 冬水生木（寒）
        when {
            fe.controls == te && from.score > to.score * 1.2 -> { to.score *= ADJ_CTRL_K; to.notes += "被${from.chinese}克" }
            te.generates == fe && from.score > to.score -> { to.score *= ADJ_DRAIN_K; to.notes += "泄于${from.chinese}" }
            te.controls == fe && from.score > to.score -> { to.score *= ADJ_HAO_K; to.notes += "耗于${from.chinese}" }
            fe.generates == te && from.score > to.score * 0.5 -> { to.score *= if (cold) 1.02 else ADJ_HELP_K; to.notes += "得${from.chinese}生" }
        }
    }

    /** 日主之禄/刃位（临官/帝旺）。 */
    private fun luRenOf(stem: HeavenlyStem): Set<EarthlyBranch> = when (stem) {
        HeavenlyStem.JIA -> setOf(EarthlyBranch.YIN, EarthlyBranch.MAO)
        HeavenlyStem.YI -> setOf(EarthlyBranch.MAO, EarthlyBranch.YIN)
        HeavenlyStem.BING, HeavenlyStem.WU -> setOf(EarthlyBranch.SI, EarthlyBranch.WU)
        HeavenlyStem.DING, HeavenlyStem.JI -> setOf(EarthlyBranch.WU)   // 阴干禄刃从简（禄刃同宫）
        HeavenlyStem.GENG -> setOf(EarthlyBranch.SHEN, EarthlyBranch.YOU)
        HeavenlyStem.XIN -> setOf(EarthlyBranch.YOU, EarthlyBranch.SHEN)
        HeavenlyStem.REN -> setOf(EarthlyBranch.HAI, EarthlyBranch.ZI)
        HeavenlyStem.GUI -> setOf(EarthlyBranch.ZI, EarthlyBranch.HAI)
    }

    /** 支所在三合局（若有）的化神五行。 */
    private fun sanHeElementOf(b: EarthlyBranch): Element? =
        SAN_HE.firstOrNull { (grp, _) -> b in grp && grp.all { true } }?.second

    private fun fmt(d: Double): String = if (d == d.toLong().toDouble()) d.toLong().toString() else String.format("%.2f", d)
}
