package cn.xulun.bazi.model

/**
 * 性格分析结果（五行+十神+神煞多线）。理论见 `.shengke-reference.md` 第六五节起。
 */
data class XingGeResult(
    val items: List<XingGeItem>,
) {
    data class XingGeItem(
        /** 维度标签（气势五行/十神心性/神煞性情） */
        val dim: String,
        /** 性格描述正文 */
        val text: String,
    )
}
