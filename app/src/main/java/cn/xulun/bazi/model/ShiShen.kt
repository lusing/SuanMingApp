package cn.xulun.bazi.model

/**
 * 十神关系（柱位寓意）结果。理论见 `.shengke-reference.md` 第六三节起。
 *
 * 每柱两条：透干十神（天干对日主）与坐支十神（支藏本气对日主）；
 * 寓意文案按（十神 × 柱位 / 十神 × 坐十神）查表，喜忌注记由引擎附加。
 */
data class ShiShenResult(
    val items: List<ShiShenItem>,
) {
    data class ShiShenItem(
        /** 如 "年干正印" / "月支坐七杀" */
        val label: String,
        /** 对日主之十神 */
        val god: TenGod,
        /** 寓意正文（多条以；分隔） */
        val meaning: String,
        /** 该十神行之喜忌注记（喜用/忌神/中性——按 BingYao 用神推断） */
        val xiJi: String,
    )
}
