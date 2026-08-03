package cn.xulun.bazi.model

/**
 * 通用神煞项（13 种之一）。仅当目标干/支命中原局某柱时才产出。
 * branch/stem 二者有其一：地支类神煞填 branch，天干类填 stem。
 */
data class ShenShaItem(
    val name: String,
    /** 命中柱位，如 "时支"/"月干" */
    val pillarLabel: String,
    val stem: HeavenlyStem? = null,
    val branch: EarthlyBranch? = null,
)

/** 八字神煞分析总结果。 */
data class ShenShaResult(val items: List<ShenShaItem>) {
    val hasAny: Boolean get() = items.isNotEmpty()
}
