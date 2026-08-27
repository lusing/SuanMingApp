package cn.xulun.bazi.model

import cn.xulun.bazi.model.Pillar

/** 财运流年关键年结论。 */
enum class CaiYunKind(val chinese: String) {
    FA("发财"),
    PO("破财"),
    JIN("谨慎"),
    PING("平稳"),
}

/** 流年财运条目（全量十年——关键年发/破/谨慎，未引动年平稳）。 */
data class CaiYunYear(
    val year: Int,
    /** 如 "1997 丁丑" */
    val label: String,
    val kind: CaiYunKind,
    /** 和局/战局依据 + 断语 */
    val text: String,
    /** 岁干十神/岁支本气十神，如 "丙食神·申七杀" */
    val ganZhiGods: String = "",
    /** 喜忌，如 "干闲支忌" */
    val xiJi: String = "",
    /** 与原局/大运的刑冲会合害，如 listOf("申冲寅(日支)", "巳申合·化水不成") */
    val relations: List<String> = emptyList(),
)

/** 一步大运的财运评估。 */
data class CaiYunYun(
    val pillar: Pillar,
    /** 如 "甲辰大运 1998-2007" */
    val label: String,
    /** 干支系统和战结论，如 "支系统战局·金木交战" / "干支皆和局" */
    val heZhan: String,
    /** 运级财运断语 */
    val text: String,
    /** 运内关键流年 */
    val years: List<CaiYunYear>,
)

/** 财运完整分析结果（原局基调+大运+流年三层）。 */
data class CaiYunResult(
    /** 原局财运基调断语 */
    val baseItems: List<LiuQinItem> = emptyList(),
    /** 各步大运（起运年龄≥16）财运评估 */
    val yuns: List<CaiYunYun> = emptyList(),
    val summary: String = "",
)
