package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DaYunInfo
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.GodCategory
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.LiuNianDetail
import cn.xulun.bazi.model.YunNianResult
import cn.xulun.bazi.model.YunNianYun
import cn.xulun.bazi.model.branchHiddenStems
import cn.xulun.bazi.core.LiuQinXiJi.categoryOf
import cn.xulun.bazi.model.tenGodOfStem

/**
 * 流年细断引擎（大运流年全面铺开）。每步大运每个流年给出：
 * - **刑冲克害**：岁支对原局四支+运支（六冲/相刑/六害/暗合/自刑）；岁干对年月时干+运干（冲/五合/克）
 * - **三合三会**：岁支与全局凑成完整三合局/半合局/三会方局
 * - **合化成否**：六合/五合/三合所化之行在全命局（原局+大运+流年）力≥2 为化成（与财运引擎同口径）
 * - **喜忌**：LiuQinXiJi 共享派生（朴素扶抑+调候为急+印重为病），干支分判
 * - **格局变化**：岁支冲提纲=动摇；合绊提纲；三合三会成局向化神倾斜
 */
object YunNianCalculator {

    private val sanHeGroups = mapOf(
        setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU) to Element.FIRE,
        setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU) to Element.METAL,
        setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN) to Element.WATER,
        setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI) to Element.WOOD,
    )
    private val sanHuiDefs = mapOf(
        setOf(EarthlyBranch.YIN, EarthlyBranch.MAO, EarthlyBranch.CHEN) to Element.WOOD,
        setOf(EarthlyBranch.SI, EarthlyBranch.WU, EarthlyBranch.WEI) to Element.FIRE,
        setOf(EarthlyBranch.SHEN, EarthlyBranch.YOU, EarthlyBranch.XU) to Element.METAL,
        setOf(EarthlyBranch.HAI, EarthlyBranch.ZI, EarthlyBranch.CHOU) to Element.WATER,
        setOf(EarthlyBranch.CHEN, EarthlyBranch.XU, EarthlyBranch.CHOU, EarthlyBranch.WEI) to Element.EARTH,
    )
    private val sanXingGroups = listOf(
        setOf(EarthlyBranch.YIN, EarthlyBranch.SI, EarthlyBranch.SHEN),
        setOf(EarthlyBranch.CHOU, EarthlyBranch.XU, EarthlyBranch.WEI),
    )
    private val ziMaoXing = setOf(EarthlyBranch.ZI, EarthlyBranch.MAO)
    private val selfXing = setOf(EarthlyBranch.CHEN, EarthlyBranch.WU, EarthlyBranch.YOU, EarthlyBranch.HAI)
    private val liuHeHua = mapOf(
        setOf(EarthlyBranch.ZI, EarthlyBranch.CHOU) to Element.EARTH,
        setOf(EarthlyBranch.YIN, EarthlyBranch.HAI) to Element.WOOD,
        setOf(EarthlyBranch.MAO, EarthlyBranch.XU) to Element.FIRE,
        setOf(EarthlyBranch.CHEN, EarthlyBranch.YOU) to Element.METAL,
        setOf(EarthlyBranch.SI, EarthlyBranch.SHEN) to Element.WATER,
        setOf(EarthlyBranch.WU, EarthlyBranch.WEI) to Element.EARTH,
    )
    private val liuHaiPairs = setOf(
        setOf(EarthlyBranch.ZI, EarthlyBranch.WEI), setOf(EarthlyBranch.CHOU, EarthlyBranch.WU),
        setOf(EarthlyBranch.YIN, EarthlyBranch.SI), setOf(EarthlyBranch.MAO, EarthlyBranch.CHEN),
        setOf(EarthlyBranch.SHEN, EarthlyBranch.HAI), setOf(EarthlyBranch.YOU, EarthlyBranch.XU),
    )
    private val anHePairs = setOf(
        setOf(EarthlyBranch.YIN, EarthlyBranch.CHOU), setOf(EarthlyBranch.WU, EarthlyBranch.HAI),
        setOf(EarthlyBranch.ZI, EarthlyBranch.SI), setOf(EarthlyBranch.MAO, EarthlyBranch.SHEN),
    )
    private val ganHeHua = mapOf(
        setOf(HeavenlyStem.JIA, HeavenlyStem.JI) to Element.EARTH,
        setOf(HeavenlyStem.YI, HeavenlyStem.GENG) to Element.METAL,
        setOf(HeavenlyStem.BING, HeavenlyStem.XIN) to Element.WATER,
        setOf(HeavenlyStem.DING, HeavenlyStem.REN) to Element.WOOD,
        setOf(HeavenlyStem.WU, HeavenlyStem.GUI) to Element.FIRE,
    )
    private val ganChong = setOf(
        setOf(HeavenlyStem.JIA, HeavenlyStem.GENG), setOf(HeavenlyStem.YI, HeavenlyStem.XIN),
        setOf(HeavenlyStem.BING, HeavenlyStem.REN), setOf(HeavenlyStem.DING, HeavenlyStem.GUI),
    )

    private fun br2(b: EarthlyBranch): EarthlyBranch = EarthlyBranch.entries[(b.ordinal + 6) % 12]

    fun analyze(bazi: BaziResult, daYun: DaYunInfo): YunNianResult {
        val day = bazi.dayPillar.stem
        val dayEl = day.element
        val d = LiuQinXiJi.derive(bazi)
        val yueZhi = bazi.monthPillar.branch

        fun catXiJi(el: Element): String {
            val c = categoryOf(el, dayEl)
            return if (c in d.fav) "喜" else if (c in d.unf) "忌" else "闲"
        }

        val yuns = daYun.yuns.map { yun ->
            val years = yun.liuNian.map { ln ->
                val yb = ln.pillar.branch
                val ys = ln.pillar.stem
                val allBranches = bazi.branches + yun.pillar.branch + yb
                val allStems = bazi.stems + yun.pillar.stem + ys

                // 合化成否：所化之行在全命局力≥2 为成（与财运引擎同口径）
                fun huaCheng(huaEl: Element): String {
                    val f = Element.entries.associateWith { 0.0 }.toMutableMap()
                    allStems.forEach { f[it.element] = f[it.element]!! + 1.0 }
                    allBranches.forEach { f[it.element] = f[it.element]!! + 1.0 }
                    return if (f[huaEl]!! >= 2.0) "化${huaEl.chinese}成" else "化${huaEl.chinese}不成"
                }

                // ── 岁支 vs 原局四支 + 运支：冲/六合/刑/害/暗合/自刑 ──
                val relations = mutableListOf<String>()
                val zhiTargets = bazi.branches.mapIndexed { i, br -> br to listOf("年支", "月支", "日支", "时支")[i] } +
                    listOf(yun.pillar.branch to "运支")
                zhiTargets.forEach { (tb, pos) ->
                    // 岁支与该支同字：仅自刑支（辰午酉亥）报自刑，其余不构成关系
                    if (tb == yb) {
                        if (tb in selfXing) relations += "${yb.chinese}刑${tb.chinese}($pos·自刑)"
                        return@forEach
                    }
                    when {
                        br2(yb) == tb -> relations += "${yb.chinese}冲${tb.chinese}($pos)"
                        liuHeHua.keys.any { yb in it && tb in it } -> {
                            val el = liuHeHua.entries.first { yb in it.key && tb in it.key }.value
                            relations += "${yb.chinese}合${tb.chinese}($pos)·" + huaCheng(el)
                        }
                        sanXingGroups.any { yb in it && tb in it } -> relations += "${yb.chinese}刑${tb.chinese}($pos)"
                        ziMaoXing.containsAll(setOf(yb, tb)) -> relations += "${yb.chinese}刑${tb.chinese}($pos·无礼之刑)"
                        liuHaiPairs.any { yb in it && tb in it } -> relations += "${yb.chinese}害${tb.chinese}($pos)"
                        anHePairs.any { yb in it && tb in it } -> relations += "${yb.chinese}暗合${tb.chinese}($pos)"
                    }
                }

                // ── 三合局（全/半）与三会方局 ──
                sanHeGroups.forEach { (group, el) ->
                    if (yb !in group) return@forEach
                    val full = group.count { it in allBranches } >= 3
                    if (full) {
                        relations += "${group.joinToString("") { it.chinese }}三合${el.chinese}局·" + huaCheng(el)
                        return@forEach
                    }
                    val zhong = when (el) {
                        Element.WATER -> EarthlyBranch.ZI; Element.WOOD -> EarthlyBranch.MAO
                        Element.FIRE -> EarthlyBranch.WU; else -> EarthlyBranch.YOU
                    }
                    val others = group - zhong
                    val banHe = (zhong in allBranches && others.any { it in allBranches })
                    if (banHe) {
                        val pair = listOf(zhong) + others.filter { it in allBranches }
                        relations += "${pair.joinToString("") { it.chinese }}半合${el.chinese}局·" + huaCheng(el)
                    }
                }
                sanHuiDefs.forEach { (def, el) ->
                    if (yb in def && def.count { it in allBranches } >= 3) {
                        relations += "${def.joinToString("") { it.chinese }}三会${el.chinese}局·" + huaCheng(el)
                    }
                }

                // ── 岁干 vs 年月时干 + 运干：冲/五合/克 ──
                val ganTargets = listOfNotNull(
                    bazi.stems.getOrNull(0)?.takeIf { it != day }?.let { it to "年干" },
                    bazi.stems.getOrNull(1)?.takeIf { it != day }?.let { it to "月干" },
                    bazi.stems.getOrNull(3)?.takeIf { it != day }?.let { it to "时干" },
                    yun.pillar.stem.takeIf { it != day }?.let { it to "运干" },
                )
                ganTargets.forEach { (ts, pos) ->
                    if (ts == ys) return@forEach
                    when {
                        ganChong.any { ys in it && ts in it } -> relations += "${ys.chinese}冲${ts.chinese}($pos)"
                        ganHeHua.entries.any { ys in it.key && ts in it.key } -> {
                            val el = ganHeHua.entries.first { ys in it.key && ts in it.key }.value
                            relations += "${ys.chinese}合${ts.chinese}($pos)·" + huaCheng(el)
                        }
                        ys.element.controls == ts.element -> relations += "${ys.chinese}克${ts.chinese}($pos)"
                        ts.element.controls == ys.element -> relations += "${ts.chinese}($pos)克${ys.chinese}"
                    }
                }

                // ── 十神与喜忌 ──
                val ganGod = tenGodOfStem(day, ys)
                val zhiGod = tenGodOfStem(day, branchHiddenStems.getValue(yb).first())
                val tenGods = "${ys.chinese}${ganGod.chinese}·${yb.chinese}${zhiGod.chinese}"
                val xiJi = "干${catXiJi(ys.element)}支${catXiJi(branchHiddenStems.getValue(yb).first().element)}"

                // ── 格局变化：冲提纲 > 合绊提纲 > 三合成局 > 三会方局 ──
                val geJu: String? = when {
                    br2(yb) == yueZhi -> "岁支${yb.chinese}冲提纲${yueZhi.chinese}——格局根基动摇，此年多变动"
                    liuHeHua.keys.any { yb in it && yueZhi in it } ->
                        "岁支${yb.chinese}合绊提纲${yueZhi.chinese}——月令贪合，格局之力受牵绊"
                    anHePairs.any { yb in it && yueZhi in it } ->
                        "岁支${yb.chinese}暗合提纲${yueZhi.chinese}——月令暗合，格局之力受牵绊"
                    sanHeGroups.any { (group, el) ->
                        yb in group && group.count { it in allBranches } >= 3 &&
                            Element.entries.associateWith { e ->
                                allStems.count { it.element == e } + allBranches.count { it.element == e }
                            }[el]!! >= 2.0
                    } -> {
                        val (group, el) = sanHeGroups.entries.first { (g, e) ->
                            yb in g && g.count { it in allBranches } >= 3
                        }
                        "岁支凑${group.joinToString("") { it.chinese }}三合${el.chinese}局而化${el.chinese}成——格局向${el.chinese}行倾斜"
                    }
                    sanHuiDefs.any { (def, el) ->
                        yb in def && def.count { it in allBranches } >= 3
                    } -> {
                        val (def, el) = sanHuiDefs.entries.first { (df, e) -> yb in df && df.count { it in allBranches } >= 3 }
                        "岁支凑${def.joinToString("") { it.chinese }}三会${el.chinese}局——格局向${el.chinese}行倾斜"
                    }
                    else -> null
                }

                LiuNianDetail(ln.year, "${ys.chinese}${yb.chinese}", tenGods, xiJi, relations, geJu)
            }
            YunNianYun(yun.pillar, "${yun.pillar.fullName} ${yun.startYear}-${yun.endYear}", years)
        }

        return YunNianResult(xiJiBasis = d.basis, yuns = yuns)
    }
}
