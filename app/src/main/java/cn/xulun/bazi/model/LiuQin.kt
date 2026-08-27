package cn.xulun.bazi.model

/**
 * 六亲分析结果容器。理论见 `.liuqin-reference.md`。
 *
 * 六亲以宫位+星曜合断：祖业/外公家族看年月柱（祖上宫），后续父母/兄弟/子女等
 * 章节到时扩展本容器。
 */
data class LiuQinResult(
    /** 祖业（第一块） */
    val zuYe: ZuYeResult,
    /** 外公家族（第二块）——母为印星，生印者官杀为外祖父，伤官制之 */
    val waiGong: WaiGongResult,
)

/** 六亲单项断语（各亲属块通用）。 */
data class LiuQinItem(
    /** 判据，如 "正印年柱·喜印扶身" / "七杀年柱·忌" */
    val dim: String,
    /** 断语 */
    val text: String,
)

/** 祖业分析结果：有利/不利断语 + 小结。 */
data class ZuYeResult(
    val favorable: List<LiuQinItem> = emptyList(),
    val unfavorable: List<LiuQinItem> = emptyList(),
    val summary: String = "",
)

/** 外公家族分析结果：断语条目 + 小结（无信号时 items 为空）。 */
data class WaiGongResult(
    val items: List<LiuQinItem> = emptyList(),
    val summary: String = "",
)
