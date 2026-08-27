package cn.xulun.bazi.model

/**
 * 地支三合局结果。
 * 如 申子辰三合水局 → branches=[SHEN, ZI, CHEN], resultElement=WATER
 */
data class BranchSanHe(
    val branches: List<EarthlyBranch>,
    /** 化气五行 */
    val resultElement: Element,
    /** 合局描述，如 "申子辰三合水局" */
    val description: String,
    /** 该合局在八字中出现的柱位，如 "年支+月支+日支" */
    val location: String,
)

/**
 * 地支六冲结果。
 * 子午冲、丑未冲、寅申冲、卯酉冲、辰戌冲、巳亥冲（对宫相冲，ordinal 差为 6）。
 */
data class BranchChong(
    val branch1: EarthlyBranch,
    val branch2: EarthlyBranch,
    /** 描述，如 "子午相冲" */
    val description: String,
    /** 出现柱位，如 "年支+日支" */
    val location: String,
)

/** 相刑类型 */
enum class XingKind(val chinese: String) {
    /** 寅巳申 / 丑戌未 三支齐备之刑 */
    SAN_XING("三刑"),
    /** 子卯 两支互刑 */
    HU_XING("互刑"),
    /** 辰午酉亥 同支重复之自刑 */
    ZI_XING("自刑"),
}

/**
 * 地支相刑结果。
 * 寅巳申三刑、丑戌未三刑、子卯相刑、辰午酉亥自刑。
 */
data class BranchXing(
    val branches: List<EarthlyBranch>,
    val kind: XingKind,
    /** 描述，如 "寅巳申三刑" / "子卯相刑" / "辰自刑（2见）" */
    val description: String,
    /** 出现柱位 */
    val location: String,
)

/**
 * 地支六害结果（穿害）：子未、丑午、寅巳、卯辰、申亥、酉戌。
 */
data class BranchHai(
    val branch1: EarthlyBranch,
    val branch2: EarthlyBranch,
    /** 描述，如 "子未相害" */
    val description: String,
    /** 出现柱位 */
    val location: String,
)

/**
 * 地支暗合结果（参考理论第十六节用例：卯申暗合，可化解冲财）。
 * 寅丑/午亥暗合他派有说、教材未及，不取。
 */
data class BranchAnHe(
    val branch1: EarthlyBranch,
    val branch2: EarthlyBranch,
    /** 描述，如 "卯申暗合" */
    val description: String,
    /** 出现柱位 */
    val location: String,
)

/**
 * 地支关系分析总结果（三合 · 六冲 · 相刑 · 六害 · 暗合）。
 */
data class DiZhiRelationResult(
    /** 地支三合局 */
    val sanHeList: List<BranchSanHe>,
    /** 地支六冲 */
    val chongList: List<BranchChong>,
    /** 地支相刑 */
    val xingList: List<BranchXing>,
    /** 地支六害 */
    val haiList: List<BranchHai> = emptyList(),
    /** 地支暗合（卯申） */
    val anHeList: List<BranchAnHe> = emptyList(),
) {
    /** 是否存在任何地支关系 */
    val hasAny: Boolean
        get() = sanHeList.isNotEmpty() ||
                chongList.isNotEmpty() ||
                xingList.isNotEmpty() ||
                haiList.isNotEmpty() ||
                anHeList.isNotEmpty()
}
