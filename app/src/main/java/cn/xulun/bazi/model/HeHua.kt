package cn.xulun.bazi.model

/**
 * 天干合化结果（天干五合）。
 * 如 甲己合化土 → stem1=JIA, stem2=JI, resultElement=EARTH
 */
data class StemHeHua(
    val stem1: HeavenlyStem,
    val stem2: HeavenlyStem,
    /** 化气五行 */
    val resultElement: Element,
    /** 合化描述，如 "甲己合化土" */
    val description: String,
    /** 该合化在八字中出现的柱位，如 "年干+时干" */
    val location: String,
)

/**
 * 地支六合结果。
 * 如 子丑合化土 → branch1=ZI, branch2=CHOU, resultElement=EARTH
 */
data class BranchLiuHe(
    val branch1: EarthlyBranch,
    val branch2: EarthlyBranch,
    /** 化气五行 */
    val resultElement: Element,
    /** 合化描述，如 "子丑合化土" */
    val description: String,
    /** 该合化在八字中出现的柱位，如 "年支+时支" */
    val location: String,
)

/**
 * 地支三会方局结果。
 * 如 寅卯辰三会木局 → branches=[YIN, MAO, CHEN], resultElement=WOOD
 */
data class BranchSanHui(
    val branches: List<EarthlyBranch>,
    /** 会化五行 */
    val resultElement: Element,
    /** 会局描述，如 "寅卯辰三会木局" */
    val description: String,
    /** 该会局在八字中出现的柱位，如 "年支+月支+日支" */
    val location: String,
)

/**
 * 八字干支合化分析总结果。
 *
 * 注：地支三合已迁至 [DiZhiRelationResult]（地支关系板块）统一呈现。
 */
data class HeHuaResult(
    /** 天干五合列表 */
    val stemHeHuaList: List<StemHeHua>,
    /** 地支六合列表 */
    val branchLiuHeList: List<BranchLiuHe>,
    /** 地支三会列表 */
    val branchSanHuiList: List<BranchSanHui>,
) {
    /** 是否存在任何合化关系 */
    val hasAny: Boolean
        get() = stemHeHuaList.isNotEmpty() ||
                branchLiuHeList.isNotEmpty() ||
                branchSanHuiList.isNotEmpty()
}
