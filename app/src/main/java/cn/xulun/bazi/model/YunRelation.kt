package cn.xulun.bazi.model

/**
 * 运岁（大运/流年）柱并入原局后的关系汇总。
 *
 * 选大运时为原局四柱 + 大运（五柱）；选流年时为原局四柱 + 大运 + 流年（六柱）。
 * 复用原局分析的关系类型；运岁柱的柱位在各条 [StemHeHua.location] / [BranchLiuHe.location]
 * 等字段中以「大运」或「流年」标识，便于区分哪些关系由运岁引动。
 */
data class YunRelationResult(
    /** 天干五合 */
    val stemHeHuaList: List<StemHeHua>,
    /** 地支六合 */
    val branchLiuHeList: List<BranchLiuHe>,
    /** 地支三合 */
    val branchSanHeList: List<BranchSanHe>,
    /** 地支三会 */
    val branchSanHuiList: List<BranchSanHui>,
    /** 地支六冲 */
    val branchChongList: List<BranchChong>,
    /** 地支相刑 */
    val branchXingList: List<BranchXing>,
) {
    /** 是否存在任何关系 */
    val hasAny: Boolean
        get() = stemHeHuaList.isNotEmpty() ||
                branchLiuHeList.isNotEmpty() ||
                branchSanHeList.isNotEmpty() ||
                branchSanHuiList.isNotEmpty() ||
                branchChongList.isNotEmpty() ||
                branchXingList.isNotEmpty()
}
