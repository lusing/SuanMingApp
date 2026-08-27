package cn.xulun.bazi.core

import cn.xulun.bazi.model.BanHeKind
import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.BranchBanHe
import cn.xulun.bazi.model.BranchGong
import cn.xulun.bazi.model.BranchLiuHe
import cn.xulun.bazi.model.BranchSanHui
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.HeHuaResult
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.PillarBranchLabels
import cn.xulun.bazi.model.PillarStemLabels
import cn.xulun.bazi.model.StemChong
import cn.xulun.bazi.model.StemHeHua

/**
 * 干支合化分析器。
 *
 * 分析一组干支中的：
 * - 天干五合（甲己合土、乙庚合金、丙辛合水、丁壬合木、戊癸合火）
 * - 天干相冲（甲庚、乙辛、丙壬、丁癸；戊己居中不冲——同性相斥方位对冲，冲比克的力大）
 * - 地支六合（子丑合土、寅亥合木、卯戌合火、辰酉合金、巳申合水、午未合土）
 * - 地支三会（寅卯辰木、巳午未火、申酉戌金、亥子丑水、辰戌丑未会四方土局）
 * - 地支半合（生地/墓地半合，残缺三合；全三合已成局时不报；金局酉丑墓地半合＞巳酉生地半合）
 * - 地支拱局（三合生地+墓地齐而中神缺，如申辰拱子）
 *
 * 通过 [analyze] 的列表重载可分析任意干支组合（如原局四柱 + 选定运岁柱）。
 */
object HeHuaCalculator {

    // ── 天干五合映射 ──
    // 合化对：stem.ordinal 之差为 5（甲0↔己5, 乙1↔庚6, 丙2↔辛7, 丁3↔壬8, 戊4↔癸9）
    private val stemHeElements = arrayOf(
        Element.EARTH,  // 甲己 → 土
        Element.METAL,  // 乙庚 → 金
        Element.WATER,  // 丙辛 → 水
        Element.WOOD,   // 丁壬 → 木
        Element.FIRE,   // 戊癸 → 火
    )

    // ── 地支六合映射 ──
    // key = min(branch1.ordinal, branch2.ordinal) * 100 + max(...)
    private val branchLiuHeMap: Map<Int, Pair<String, Element>> = mapOf(
        EarthlyBranch.ZI to EarthlyBranch.CHOU to ("子丑合化土" to Element.EARTH),
        EarthlyBranch.YIN to EarthlyBranch.HAI to ("寅亥合化木" to Element.WOOD),
        EarthlyBranch.MAO to EarthlyBranch.XU to ("卯戌合化火" to Element.FIRE),
        EarthlyBranch.CHEN to EarthlyBranch.YOU to ("辰酉合化金" to Element.METAL),
        EarthlyBranch.SI to EarthlyBranch.SHEN to ("巳申合化水" to Element.WATER),
        EarthlyBranch.WU to EarthlyBranch.WEI to ("午未合化土" to Element.EARTH),
    ).mapKeys { (k, _) -> makeKey(k.first, k.second) }

    // ── 地支三会局定义 ──
    private data class SanHuiDef(
        val branches: Set<EarthlyBranch>,
        val element: Element,
        val description: String,
    )

    private val sanHuiDefs = listOf(
        SanHuiDef(
            setOf(EarthlyBranch.YIN, EarthlyBranch.MAO, EarthlyBranch.CHEN),
            Element.WOOD, "寅卯辰三会木局",
        ),
        SanHuiDef(
            setOf(EarthlyBranch.SI, EarthlyBranch.WU, EarthlyBranch.WEI),
            Element.FIRE, "巳午未三会火局",
        ),
        SanHuiDef(
            setOf(EarthlyBranch.SHEN, EarthlyBranch.YOU, EarthlyBranch.XU),
            Element.METAL, "申酉戌三会金局",
        ),
        SanHuiDef(
            setOf(EarthlyBranch.HAI, EarthlyBranch.ZI, EarthlyBranch.CHOU),
            Element.WATER, "亥子丑三会水局",
        ),
        SanHuiDef(
            setOf(EarthlyBranch.CHEN, EarthlyBranch.XU, EarthlyBranch.CHOU, EarthlyBranch.WEI),
            Element.EARTH, "辰戌丑未会四方土局",
        ),
    )

    // ── 三合局分组（供半合/拱局判定）：中神/生地/墓地 ──
    private data class SanHeGroup(
        val zhong: EarthlyBranch,
        val shengDi: EarthlyBranch,
        val muDi: EarthlyBranch,
        val element: Element,
    )

    private val sanHeGroups = listOf(
        SanHeGroup(EarthlyBranch.ZI, EarthlyBranch.SHEN, EarthlyBranch.CHEN, Element.WATER),
        SanHeGroup(EarthlyBranch.MAO, EarthlyBranch.HAI, EarthlyBranch.WEI, Element.WOOD),
        SanHeGroup(EarthlyBranch.WU, EarthlyBranch.YIN, EarthlyBranch.XU, Element.FIRE),
        SanHeGroup(EarthlyBranch.YOU, EarthlyBranch.SI, EarthlyBranch.CHOU, Element.METAL),
    )

    // ── 天干相冲：同性方位对冲 ──
    private val stemChongDesc: Map<Int, String> = listOf(
        HeavenlyStem.JIA to HeavenlyStem.GENG,
        HeavenlyStem.YI to HeavenlyStem.XIN,
        HeavenlyStem.BING to HeavenlyStem.REN,
        HeavenlyStem.DING to HeavenlyStem.GUI,
    ).associate { (a, b) -> a.ordinal * 100 + b.ordinal to a.chinese + b.chinese + "相冲" }

    /** 分析八字原局四柱的合化关系 */
    fun analyze(bazi: BaziResult): HeHuaResult =
        analyze(bazi.stems, bazi.branches, PillarStemLabels, PillarBranchLabels)

    /**
     * 分析任意干支组合的合化关系（供原局 + 运岁柱等场景复用）。
     * [stemLabels]/[branchLabels] 为各柱柱位名，长度需与 [stems]/[branches] 一致。
     */
    fun analyze(
        stems: List<HeavenlyStem>,
        branches: List<EarthlyBranch>,
        stemLabels: List<String>,
        branchLabels: List<String>,
    ): HeHuaResult = HeHuaResult(
        stemHeHuaList = findStemHeHua(stems, stemLabels),
        branchLiuHeList = findBranchLiuHe(branches, branchLabels),
        branchSanHuiList = findBranchSanHui(branches, branchLabels),
        stemChongList = findStemChong(stems, stemLabels),
        branchBanHeList = findBranchBanHe(branches, branchLabels),
        branchGongList = findBranchGong(branches, branchLabels),
    )

    // ── 天干五合检测 ──
    private fun findStemHeHua(stems: List<HeavenlyStem>, labels: List<String>): List<StemHeHua> {
        val result = mutableListOf<StemHeHua>()
        for (i in 0 until stems.size) {
            for (j in i + 1 until stems.size) {
                val a = stems[i]
                val b = stems[j]
                if (isStemHePair(a, b)) {
                    val group = minOf(a.ordinal, b.ordinal)
                    val element = stemHeElements[group]
                    // 天干五合命名惯例：yang 干（小序号）在前
                    val (first, second) = if (a.ordinal < b.ordinal) a to b else b to a
                    result.add(
                        StemHeHua(
                            stem1 = first,
                            stem2 = second,
                            resultElement = element,
                            description = "${first.chinese}${second.chinese}合化${element.chinese}",
                            location = "${labels[i]}+${labels[j]}",
                        ),
                    )
                }
            }
        }
        return result
    }

    /** 两天干是否构成五合（ordinal 差为 5） */
    private fun isStemHePair(a: HeavenlyStem, b: HeavenlyStem): Boolean {
        return java.lang.Math.floorMod(a.ordinal - b.ordinal, 10) == 5
    }

    // ── 地支六合检测 ──
    private fun findBranchLiuHe(branches: List<EarthlyBranch>, labels: List<String>): List<BranchLiuHe> {
        val result = mutableListOf<BranchLiuHe>()
        for (i in 0 until branches.size) {
            for (j in i + 1 until branches.size) {
                val a = branches[i]
                val b = branches[j]
                val key = makeKey(a, b)
                branchLiuHeMap[key]?.let { (desc, element) ->
                    result.add(
                        BranchLiuHe(
                            branch1 = a,
                            branch2 = b,
                            resultElement = element,
                            description = desc,
                            location = "${labels[i]}+${labels[j]}",
                        ),
                    )
                }
            }
        }
        return result
    }

    // ── 地支三会检测 ──
    private fun findBranchSanHui(branches: List<EarthlyBranch>, labels: List<String>): List<BranchSanHui> {
        val branchSet = branches.toSet()
        if (branchSet.size < 3) return emptyList()

        val result = mutableListOf<BranchSanHui>()
        for (def in sanHuiDefs) {
            val matched = def.branches.intersect(branchSet)
            if (matched.size >= 3) {
                val matchedList = branches.filter { it in matched }
                result.add(
                    BranchSanHui(
                        branches = matchedList,
                        resultElement = def.element,
                        description = def.description,
                        location = matchedList.joinToString("+") { b ->
                            labels[branches.indexOf(b)]
                        },
                    ),
                )
            }
        }
        return result
    }

    // ── 天干相冲检测 ──
    private fun findStemChong(stems: List<HeavenlyStem>, labels: List<String>): List<StemChong> {
        val result = mutableListOf<StemChong>()
        for (i in 0 until stems.size) {
            for (j in i + 1 until stems.size) {
                val desc = stemChongDesc[stems[i].ordinal * 100 + stems[j].ordinal]
                    ?: stemChongDesc[stems[j].ordinal * 100 + stems[i].ordinal]
                    ?: continue
                val (first, second) = if (stems[i].ordinal < stems[j].ordinal) stems[i] to stems[j] else stems[j] to stems[i]
                result.add(
                    StemChong(
                        stem1 = first,
                        stem2 = second,
                        description = desc,
                        location = labels[i] + "+" + labels[j],
                    ),
                )
            }
        }
        return result
    }

    // ── 半合检测（全三合已成局时不报） ──
    private fun findBranchBanHe(branches: List<EarthlyBranch>, labels: List<String>): List<BranchBanHe> {
        val present = branches.toSet()
        val result = mutableListOf<BranchBanHe>()
        for (g in sanHeGroups) {
            if (present.containsAll(setOf(g.zhong, g.shengDi, g.muDi))) continue
            for ((other, kind) in listOf(g.shengDi to BanHeKind.SHENG_DI, g.muDi to BanHeKind.MU_DI)) {
                if (g.zhong in present && other in present) {
                    val jinException = g.element == Element.METAL && kind == BanHeKind.MU_DI
                    val note = if (jinException) "（金局例外：酉丑墓地半合＞巳酉生地半合）" else ""
                    // 命名惯例：生地半合=生地+中神（巳酉），墓地半合=中神+墓地（酉丑）
                    val (first, second) = if (kind == BanHeKind.SHENG_DI) other to g.zhong else g.zhong to other
                    result.add(
                        BranchBanHe(
                            branch1 = first,
                            branch2 = second,
                            kind = kind,
                            resultElement = g.element,
                            description = first.chinese + second.chinese + kind.chinese + g.element.chinese + "局" + note,
                            location = locationPairOf(branches, g.zhong, other, labels),
                        ),
                    )
                }
            }
        }
        return result
    }

    // ── 拱局检测（生地+墓地齐而中神缺） ──
    private fun findBranchGong(branches: List<EarthlyBranch>, labels: List<String>): List<BranchGong> {
        val present = branches.toSet()
        val result = mutableListOf<BranchGong>()
        for (g in sanHeGroups) {
            if (g.zhong !in present && g.shengDi in present && g.muDi in present) {
                // 命名惯例：拱局=生地+墓地（申辰拱子）
                val (first, second) = g.shengDi to g.muDi
                result.add(
                    BranchGong(
                        branch1 = first,
                        branch2 = second,
                        resultElement = g.element,
                        description = first.chinese + second.chinese + "拱" + g.element.chinese + "局（待" + g.zhong.chinese + "）",
                        location = locationPairOf(branches, g.shengDi, g.muDi, labels),
                    ),
                )
            }
        }
        return result
    }

    /** 两支首次出现的柱位 */
    private fun locationPairOf(branches: List<EarthlyBranch>, a: EarthlyBranch, b: EarthlyBranch, labels: List<String>): String {
        val ia = branches.indexOf(a)
        val ib = branches.indexOf(b)
        return labels[minOf(ia, ib)] + "+" + labels[maxOf(ia, ib)]
    }

    /** 为两个地支生成无序的查找键 */
    private fun makeKey(a: EarthlyBranch, b: EarthlyBranch): Int {
        val lo = minOf(a.ordinal, b.ordinal)
        val hi = maxOf(a.ordinal, b.ordinal)
        return lo * 100 + hi
    }
}
