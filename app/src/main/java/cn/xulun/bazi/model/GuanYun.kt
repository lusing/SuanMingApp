package cn.xulun.bazi.model

import cn.xulun.bazi.model.Pillar

/** 官运流年结论。 */
enum class GuanYunKind(val chinese: String) {
    JIN_SHENG("晋升"),
    BIAN_ZHI("贬职"),
    DIAO_DONG("调动"),
    PING("平顺"),
}

/** 官运流年条目（全量逐年）。 */
data class GuanYunYear(
    val year: Int,
    val label: String,
    val kind: GuanYunKind,
    /** 引动源清单 */
    val triggers: List<String>,
    val text: String,
)

/** 一步大运的官运流年。 */
data class GuanYunYun(
    val pillar: Pillar,
    val label: String,
    val years: List<GuanYunYear>,
)

/** 官运分析结果。 */
data class GuanYunResult(
    /** 官星辨识与格局说明 */
    val officerNote: String,
    /** 贵命断语（原局层） */
    val nobleItems: List<LiuQinItem> = emptyList(),
    val yuns: List<GuanYunYun> = emptyList(),
    val summary: String = "",
)
