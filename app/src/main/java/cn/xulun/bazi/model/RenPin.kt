package cn.xulun.bazi.model

/** 人品心性条目（十神心性/五行特性——正面或负面）。 */
data class RenPinItem(
    val title: String,
    val text: String,
    /** true=正面心性 / false=负面心性 */
    val favorable: Boolean,
)

/** 人品推断结果（十神心性与五行特性复合）。 */
data class RenPinResult(
    /** 人品总评：优秀（德才兼备）/良好/一般/较差/很差（先天品质很坏） */
    val verdict: String,
    val basis: String,
    val items: List<RenPinItem>,
    /** 岁运改变（官杀旺运原则性增强/食伤运先天本性暴露） */
    val yunNote: String,
    /** 用人交友建议 */
    val advice: String,
)
