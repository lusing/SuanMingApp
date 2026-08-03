package cn.xulun.bazi.model

/** 冲被会合所解的结果条目。 */
data class ChongJie(
    val branch1: EarthlyBranch,
    val branch2: EarthlyBranch,
    val description: String,
    /** 是否被六合/三合所解 */
    val resolved: Boolean,
    /** 解去之神，如 "丑六合（子）" / "子三合"；未解为 null */
    val resolveBy: String?,
    val note: String,
)

/** 刑被会合所解的结果条目。 */
data class XingJie(
    val description: String,
    val kind: XingKind,
    /** 涉刑支拼接，如 "午午" */
    val involved: String,
    val resolved: Boolean,
    val resolveBy: String?,
    val note: String,
)

/** 会合解冲刑分析总结果（《子平真诠》第七章）。 */
data class JieChongResult(
    val chong: List<ChongJie>,
    val xing: List<XingJie>,
    val hasChong: Boolean,
    /** 是否存在未解之冲（供成败判定「未解才败」） */
    val hasUnresolvedChong: Boolean,
    val hasUnresolvedXing: Boolean,
    val liuHeCount: Int,
    val sanHeCount: Int,
)
