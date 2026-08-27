package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.BranchAnHe
import cn.xulun.bazi.model.BranchChong
import cn.xulun.bazi.model.BranchHai
import cn.xulun.bazi.model.BranchSanHe
import cn.xulun.bazi.model.BranchXing
import cn.xulun.bazi.model.DiZhiRelationResult
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.PillarBranchLabels
import cn.xulun.bazi.model.XingKind

/**
 * 地支关系分析器 — 三合 · 六冲 · 相刑 · 六害 · 暗合。
 *
 * - 三合局：申子辰水、亥卯未木、寅午戌火、巳酉丑金（三支齐备则成局）
 * - 六冲：子午、丑未、寅申、卯酉、辰戌、巳亥（对宫相冲，两支 ordinal 差为 6）
 * - 相刑：寅巳申三刑（恃势之刑）、丑戌未三刑（无恩之刑）、子卯互刑（无礼之刑）、辰午酉亥自刑
 * - 六害：子未、丑午、寅巳、卯辰、申亥、酉戌
 * - 暗合：卯申（教材第十六节用例；寅丑/午亥他派有说不取）
 *
 * 通过 [analyze] 的列表重载可分析任意地支组合（如原局四支 + 选定运岁支）。
 * 相刑取「齐备即论」，不细分方向；自刑取同支两见及以上。
 */
object DiZhiRelationCalculator {

    // ── 三合局定义（每组三支 ordinal 模 4 同余）──
    private data class SanHeDef(
        val branches: Set<EarthlyBranch>,
        val element: Element,
        val description: String,
    )

    private val sanHeDefs = listOf(
        SanHeDef(setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN), Element.WATER, "申子辰三合水局"),
        SanHeDef(setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI), Element.WOOD, "亥卯未三合木局"),
        SanHeDef(setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU), Element.FIRE, "寅午戌三合火局"),
        SanHeDef(setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU), Element.METAL, "巳酉丑三合金局"),
    )

    // ── 六冲：两支 ordinal 差为 6 ──
    private val chongDesc: Map<Int, String> = listOf(
        EarthlyBranch.ZI to EarthlyBranch.WU,    // 子午
        EarthlyBranch.CHOU to EarthlyBranch.WEI, // 丑未
        EarthlyBranch.YIN to EarthlyBranch.SHEN, // 寅申
        EarthlyBranch.MAO to EarthlyBranch.YOU,  // 卯酉
        EarthlyBranch.CHEN to EarthlyBranch.XU,  // 辰戌
        EarthlyBranch.SI to EarthlyBranch.HAI,   // 巳亥
    ).associate { (a, b) -> makeKey(a, b) to "${a.chinese}${b.chinese}相冲" }

    // ── 三刑（三支齐备）──
    private data class SanXingDef(
        val branches: Set<EarthlyBranch>,
        val description: String,
    )

    private val sanXingDefs = listOf(
        SanXingDef(setOf(EarthlyBranch.YIN, EarthlyBranch.SI, EarthlyBranch.SHEN), "寅巳申三刑（恃势之刑）"),
        SanXingDef(setOf(EarthlyBranch.CHOU, EarthlyBranch.XU, EarthlyBranch.WEI), "丑戌未三刑（无恩之刑）"),
    )

    // ── 六害 ──
    private val haiDesc: Map<Int, String> = listOf(
        EarthlyBranch.ZI to EarthlyBranch.WEI,    // 子未
        EarthlyBranch.CHOU to EarthlyBranch.WU,   // 丑午
        EarthlyBranch.YIN to EarthlyBranch.SI,    // 寅巳
        EarthlyBranch.MAO to EarthlyBranch.CHEN,  // 卯辰
        EarthlyBranch.SHEN to EarthlyBranch.HAI,  // 申亥
        EarthlyBranch.YOU to EarthlyBranch.XU,    // 酉戌
    ).associate { (a, b) -> makeKey(a, b) to a.chinese + b.chinese + "相害" }

    // ── 暗合（卯申）──
    private val anHeDesc: Map<Int, String> = listOf(
        EarthlyBranch.MAO to EarthlyBranch.SHEN,
    ).associate { (a, b) -> makeKey(a, b) to a.chinese + b.chinese + "暗合" }

    // ── 自刑支 ──
    private val ziXingBranches = setOf(
        EarthlyBranch.CHEN, EarthlyBranch.WU, EarthlyBranch.YOU, EarthlyBranch.HAI,
    )

    /** 分析八字原局四支的地支关系 */
    fun analyze(bazi: BaziResult): DiZhiRelationResult =
        analyze(bazi.branches, PillarBranchLabels)

    /**
     * 分析任意地支组合的关系（供原局 + 运岁支等场景复用）。
     * [labels] 为各地支柱位名，长度需与 [branches] 一致。
     */
    fun analyze(branches: List<EarthlyBranch>, labels: List<String>): DiZhiRelationResult =
        DiZhiRelationResult(
            sanHeList = findSanHe(branches, labels),
            chongList = findChong(branches, labels),
            xingList = findXing(branches, labels),
            haiList = findHai(branches, labels),
            anHeList = findAnHe(branches, labels),
        )

    // ── 三合检测 ──
    private fun findSanHe(branches: List<EarthlyBranch>, labels: List<String>): List<BranchSanHe> {
        // 不同地支不足 3 个，不可能有三合
        if (branches.toSet().size < 3) return emptyList()

        val result = mutableListOf<BranchSanHe>()
        for (def in sanHeDefs) {
            if (def.branches.intersect(branches.toSet()).size >= 3) {
                result.add(
                    BranchSanHe(
                        branches = branches.filter { it in def.branches },
                        resultElement = def.element,
                        description = def.description,
                        location = locationOf(branches, def.branches, labels),
                    ),
                )
            }
        }
        return result
    }

    // ── 六冲检测（遍历所有地支两两组合，与六合一致）──
    private fun findChong(branches: List<EarthlyBranch>, labels: List<String>): List<BranchChong> {
        val result = mutableListOf<BranchChong>()
        for (i in 0 until branches.size) {
            for (j in i + 1 until branches.size) {
                val a = branches[i]
                val b = branches[j]
                val desc = chongDesc[makeKey(a, b)] ?: continue
                // 命名惯例：ordinal 小者（阳支）在前
                val (first, second) = if (a.ordinal < b.ordinal) a to b else b to a
                result.add(
                    BranchChong(
                        branch1 = first,
                        branch2 = second,
                        description = desc,
                        location = "${labels[i]}+${labels[j]}",
                    ),
                )
            }
        }
        return result
    }

    // ── 相刑检测 ──
    private fun findXing(branches: List<EarthlyBranch>, labels: List<String>): List<BranchXing> {
        val branchSet = branches.toSet()
        val result = mutableListOf<BranchXing>()

        // 1. 三刑（三支齐备）
        if (branchSet.size >= 3) {
            for (def in sanXingDefs) {
                if (def.branches.intersect(branchSet).size >= 3) {
                    result.add(
                        BranchXing(
                            branches = branches.filter { it in def.branches },
                            kind = XingKind.SAN_XING,
                            description = def.description,
                            location = locationOf(branches, def.branches, labels),
                        ),
                    )
                }
            }
        }

        // 2. 子卯互刑（两支齐备，列出所有 子-卯 配对）
        if (EarthlyBranch.ZI in branchSet && EarthlyBranch.MAO in branchSet) {
            for (i in 0 until branches.size) {
                for (j in i + 1 until branches.size) {
                    val a = branches[i]
                    val b = branches[j]
                    val isZiMao = (a == EarthlyBranch.ZI && b == EarthlyBranch.MAO) ||
                        (a == EarthlyBranch.MAO && b == EarthlyBranch.ZI)
                    if (isZiMao) {
                        result.add(
                            BranchXing(
                                branches = listOf(a, b),
                                kind = XingKind.HU_XING,
                                description = "子卯相刑（无礼之刑）",
                                location = "${labels[i]}+${labels[j]}",
                            ),
                        )
                    }
                }
            }
        }

        // 3. 自刑（辰午酉亥 同支重复 ≥2 次）
        for (branch in ziXingBranches) {
            val positions = branches.mapIndexed { i, b -> if (b == branch) i else -1 }.filter { it >= 0 }
            if (positions.size >= 2) {
                result.add(
                    BranchXing(
                        branches = List(positions.size) { branch },
                        kind = XingKind.ZI_XING,
                        description = "${branch.chinese}自刑（${positions.size}见）",
                        location = positions.joinToString("+") { labels[it] },
                    ),
                )
            }
        }

        return result
    }

    // ── 六害检测 ──
    private fun findHai(branches: List<EarthlyBranch>, labels: List<String>): List<BranchHai> {
        val result = mutableListOf<BranchHai>()
        for (i in 0 until branches.size) {
            for (j in i + 1 until branches.size) {
                haiDesc[makeKey(branches[i], branches[j])]?.let { desc ->
                    result.add(
                        BranchHai(
                            branch1 = branches[i],
                            branch2 = branches[j],
                            description = desc,
                            location = labels[i] + "+" + labels[j],
                        ),
                    )
                }
            }
        }
        return result
    }

    // ── 暗合检测（卯申） ──
    private fun findAnHe(branches: List<EarthlyBranch>, labels: List<String>): List<BranchAnHe> {
        val result = mutableListOf<BranchAnHe>()
        for (i in 0 until branches.size) {
            for (j in i + 1 until branches.size) {
                anHeDesc[makeKey(branches[i], branches[j])]?.let { desc ->
                    result.add(
                        BranchAnHe(
                            branch1 = branches[i],
                            branch2 = branches[j],
                            description = desc,
                            location = labels[i] + "+" + labels[j],
                        ),
                    )
                }
            }
        }
        return result
    }

    /** 列出 [selected] 中地支在列表里出现的全部柱位 */
    private fun locationOf(branches: List<EarthlyBranch>, selected: Set<EarthlyBranch>, labels: List<String>): String =
        branches.mapIndexed { i, b -> if (b in selected) labels[i] else null }
            .filterNotNull()
            .joinToString("+")

    /** 为两个地支生成无序的查找键 */
    private fun makeKey(a: EarthlyBranch, b: EarthlyBranch): Int {
        val lo = minOf(a.ordinal, b.ordinal)
        val hi = maxOf(a.ordinal, b.ordinal)
        return lo * 100 + hi
    }
}
