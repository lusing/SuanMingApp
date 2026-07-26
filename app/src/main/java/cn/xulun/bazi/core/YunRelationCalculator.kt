package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.PillarBranchLabels
import cn.xulun.bazi.model.PillarStemLabels
import cn.xulun.bazi.model.YunRelationResult

/**
 * 运岁关系分析器：把选定的大运/流年柱并入原局四柱，计算这些柱之间全部的
 * 天干五合、地支六合、三合、三会、六冲、相刑。
 *
 - 选大运：原局 + 大运（五柱）
 - 选流年：原局 + 大运 + 流年（六柱）——流年必在某步大运内，故其大运背景一并纳入
 *
 * 各运岁柱的柱位名（「大运」/「流年」）会出现在关系条目的 location 中，
 * 标识哪些关系由运岁引动（含跨原局/大运/流年三者凑成的三合三会三刑）。
 * 委托 [HeHuaCalculator]（天干合/六合/三会）与 [DiZhiRelationCalculator]（三合/六冲/相刑）的列表重载。
 */
object YunRelationCalculator {

    /**
     * 多柱重载：把 [extras] 中的若干柱（大运、流年）并入原局分析。
     * [extras] 每项为 (柱, 柱位名)，柱位名会出现在 location 中。
     */
    fun analyze(bazi: BaziResult, extras: List<Pair<Pillar, String>>): YunRelationResult {
        val stems = bazi.stems + extras.map { it.first.stem }
        val branches = bazi.branches + extras.map { it.first.branch }
        val stemLabels = PillarStemLabels + extras.map { it.second }
        val branchLabels = PillarBranchLabels + extras.map { it.second }

        val heHua = HeHuaCalculator.analyze(stems, branches, stemLabels, branchLabels)
        val diZhi = DiZhiRelationCalculator.analyze(branches, branchLabels)

        return YunRelationResult(
            stemHeHuaList = heHua.stemHeHuaList,
            branchLiuHeList = heHua.branchLiuHeList,
            branchSanHeList = diZhi.sanHeList,
            branchSanHuiList = heHua.branchSanHuiList,
            branchChongList = diZhi.chongList,
            branchXingList = diZhi.xingList,
        )
    }

    /** 单柱重载（向后兼容）：并入一个运岁柱。 */
    fun analyze(bazi: BaziResult, extra: Pillar, extraLabel: String): YunRelationResult =
        analyze(bazi, listOf(extra to extraLabel))
}
