package cn.xulun.bazi.model

/** 职业变动流年条目。 */
data class BianDongYear(
    val year: Int,
    /** 干支标签，如 1986 丙寅 */
    val pillar: String,
    /** 引动源清单 */
    val triggers: List<String>,
    val text: String,
)

/** 职业变动分析结果（升迁见官运引擎，此处主工作环境变动/调动/走动）。 */
data class ZhiYeBianDongResult(
    val years: List<BianDongYear>,
    val summary: String,
)
