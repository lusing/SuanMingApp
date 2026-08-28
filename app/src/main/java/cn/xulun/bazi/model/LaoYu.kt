package cn.xulun.bazi.model

/** 牢狱/官非流年应期条目。 */
data class LaoYuYear(
    val year: Int,
    /** 干支标签，如 1997 丁丑 */
    val label: String,
    /** 引动源清单 */
    val triggers: List<String>,
    val text: String,
)

/** 牢狱之灾分析结果（原局伏险 + 岁运引发）。 */
data class LaoYuResult(
    /** 原局牢狱信息标志（15 条之可实现集） */
    val baseRisks: List<LiuQinItem>,
    val years: List<LaoYuYear>,
    val summary: String,
)
