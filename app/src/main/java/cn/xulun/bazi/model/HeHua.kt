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

/** 天干相冲（同性相斥方位对冲）：甲庚、乙辛、丙壬、丁癸；戊己居中不冲。冲比克的力大。 */
data class StemChong(
    val stem1: HeavenlyStem,
    val stem2: HeavenlyStem,
    /** 描述，如 "甲庚相冲" */
    val description: String,
    /** 出现柱位，如 "年干+日干" */
    val location: String,
)

/** 半合类型（三合局缺一支：中神+生地为生地半合，中神+墓地为墓地半合）。 */
enum class BanHeKind(val chinese: String) {
    SHENG_DI("生地半合"),
    MU_DI("墓地半合"),
}

/**
 * 地支半合结果（残缺的三合局，力小于全三合）。
 * 如 寅午生地半合火局；金局例外：酉丑墓地半合之力通常大于巳酉生地半合。
 */
data class BranchBanHe(
    val branch1: EarthlyBranch,
    val branch2: EarthlyBranch,
    val kind: BanHeKind,
    /** 化气五行 */
    val resultElement: Element,
    /** 描述，如 "寅午生地半合火局" */
    val description: String,
    /** 出现柱位 */
    val location: String,
)

/** 地支拱局结果（三合局生地+墓地齐而中神缺，中神到位即成三合）。如 申辰拱子。 */
data class BranchGong(
    val branch1: EarthlyBranch,
    val branch2: EarthlyBranch,
    /** 所拱化神五行 */
    val resultElement: Element,
    /** 描述，如 "申辰拱水局（待子）" */
    val description: String,
    /** 出现柱位 */
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
    /** 天干相冲列表（第八节） */
    val stemChongList: List<StemChong> = emptyList(),
    /** 地支半合列表（第十一节；全三合已成局时不报） */
    val branchBanHeList: List<BranchBanHe> = emptyList(),
    /** 地支拱局列表（第十一节） */
    val branchGongList: List<BranchGong> = emptyList(),
) {
    /** 是否存在任何合化关系 */
    val hasAny: Boolean
        get() = stemHeHuaList.isNotEmpty() ||
                branchLiuHeList.isNotEmpty() ||
                branchSanHuiList.isNotEmpty() ||
                stemChongList.isNotEmpty() ||
                branchBanHeList.isNotEmpty() ||
                branchGongList.isNotEmpty()
}
