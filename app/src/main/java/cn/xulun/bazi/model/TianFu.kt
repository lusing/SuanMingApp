package cn.xulun.bazi.model

/** 天赋条目（十神含义+五行特性→天赋信息）。 */
data class TianFuItem(
    val title: String,
    val text: String,
)

/** 天赋推断与职业选择结果。 */
data class TianFuResult(
    val items: List<TianFuItem>,
    /** 适合的职业方向（按匹配度排序） */
    val careers: List<String>,
    /** 行运配合说明（何运宜何业/不宜何业） */
    val yunNote: String,
    val summary: String,
)
