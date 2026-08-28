package cn.xulun.bazi.model

/** 健康分析结果(原局基调+疾病倾向)。 */
data class HealthResult(
    /** 健康基调断语 */
    val items: List<LiuQinItem> = emptyList(),
    /** 原局受损五行→易患疾病倾向(按受损程度降序) */
    val risks: List<LiuQinItem> = emptyList(),
    val summary: String = "",
)
