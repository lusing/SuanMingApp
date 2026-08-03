package cn.xulun.bazi.model

/** 气候（寒暖燥湿）。 */
enum class Climate(val chinese: String) { HAN("寒"), ZAO("燥"), WEN_HE("温和") }

/** 调候口诀条目。guanStance 为官星在该气候下的立场：喜/忌/无碍/—。 */
data class TiaoHouFormula(val key: String, val name: String, val desc: String, val guanStance: String)

/**
 * 调候分析结果（《子平真诠》第十四章 论用神配气候得失）。
 * 调候为急（urgent=true）时常规得失让位；从格顺势不调候。
 */
data class TiaoHouResult(
    val climate: Climate,
    val climateBasis: String,
    /** 调候用神五行：寒→火，燥→水，温和→null */
    val tiaoHouElement: Element?,
    /** 调候用神是否现于柱中 */
    val tiaoHouPresent: Boolean,
    val tiaoHouNote: String,
    /** 调候为急（寒极无火/燥极无水，且非从格） */
    val urgent: Boolean,
    /** urgent=true 时的说明，否则空串 */
    val urgentDesc: String,
    /** 从格检测结果（null=非从格） */
    val congGe: CongGeResult?,
    val formulas: List<TiaoHouFormula>,
    val deShi: List<String>,
)
