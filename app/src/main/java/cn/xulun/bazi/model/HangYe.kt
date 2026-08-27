package cn.xulun.bazi.model

/**
 * 行业选择（以性格断行业）结果。理论见 `.shengke-reference.md` 第六四节起。
 *
 * 性格信号 → 行业倾向（自由职业/公职/技术/创作/流动/玄学等），附命中理由。
 */
data class HangYeResult(
    val items: List<HangYeItem>,
) {
    data class HangYeItem(
        /** 性格标签（倔强不服管/创新开拓/孤傲清高…） */
        val trait: String,
        /** 行业倾向（自由职业/公职…） */
        val field: String,
        /** 命中理由（引擎信号描述） */
        val basis: String,
    )
}
