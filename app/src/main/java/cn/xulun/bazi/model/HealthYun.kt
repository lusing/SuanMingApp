package cn.xulun.bazi.model

import cn.xulun.bazi.model.Pillar

/** 健康岁运流年结论。 */
enum class HealthYearKind(val chinese: String) {
    BING("病发"),
    HUAJIE("缓解"),
    CHE_HUO("车祸"),
    PING("平安"),
}

/** 健康流年条目（全量逐年）。 */
data class HealthYear(
    val year: Int,
    /** 如 "1985 乙丑" */
    val label: String,
    val kind: HealthYearKind,
    /** 引发源清单，如 ["丑冲未(日支)——木根拔", "乙引动乙卯"] */
    val triggers: List<String>,
    val text: String,
)

/** 一步大运的健康流年。 */
data class HealthYun(
    val pillar: Pillar,
    val label: String,
    val years: List<HealthYear>,
)

/** 健康·岁运批断结果。 */
data class HealthYunResult(
    /** 原局病根元素说明 */
    val rootNote: String,
    val yuns: List<HealthYun> = emptyList(),
    val summary: String = "",
)
