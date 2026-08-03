package cn.xulun.bazi.model

/** 五行断语倾向。 */
enum class FlowType { FAVOR, UNFAVOR }

/** 一条五行流通/战局断语。 */
data class FlowItem(val type: FlowType, val id: String, val text: String)

/** 原局交战标记（六冲中的金木对、水火对）。 */
data class WarFlags(val jinMu: Boolean, val shuiHuo: Boolean)

/**
 * 五行流通 / 战局 / 混杂分析结果（试验）。
 * elems 按 [Element.ordinal] 索引：[木, 火, 土, 金, 水]。
 */
data class WuXingFlowResult(
    val duanYu: List<FlowItem>,
    /** 最长相生链长度 0..5 */
    val chain: Int,
    val elems: IntArray,
    val war: WarFlags,
    /** 流通是否顺势生扶日主（身强或印旺） */
    val favorable: Boolean,
)
