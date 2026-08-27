package cn.xulun.bazi.model

/**
 * 六亲分析结果容器。理论见 `.liuqin-reference.md`。
 *
 * 六亲以宫位+星曜合断：祖业看年柱（祖上宫），后续父母/兄弟/子女等章节到时扩展本容器。
 */
data class LiuQinResult(
    /** 祖业（第一块） */
    val zuYe: ZuYeResult,
)

/** 祖业单项断语。 */
data class ZuYeItem(
    /** 判据，如 "正印年柱·身衰喜印" / "七杀年柱·忌" */
    val dim: String,
    /** 断语 */
    val text: String,
)

/** 祖业分析结果：有利/不利断语 + 小结。 */
data class ZuYeResult(
    val favorable: List<ZuYeItem> = emptyList(),
    val unfavorable: List<ZuYeItem> = emptyList(),
    val summary: String = "",
)
