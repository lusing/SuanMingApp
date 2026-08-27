package cn.xulun.bazi.model

/**
 * 六亲分析结果容器。理论见 `.liuqin-reference.md`。
 *
 * 六亲以宫位+星曜合断：祖业/外公家族/父母看年月柱（祖上宫/父母宫），后续兄弟/子女等
 * 章节到时扩展本容器。
 */
data class LiuQinResult(
    /** 祖业（第一块） */
    val zuYe: ZuYeResult,
    /** 外公家族（第二块）——母为印星，生印者官杀为外祖父，伤官制之 */
    val waiGong: WaiGongResult,
    /** 父母（第三块）——父星男偏财/女正财，母星男正印/女偏印；父母宫=年柱 */
    val fuMu: FuMuResult,
    /** 兄弟姐妹（第四块）——比劫为手足星，禄刃印从变通计数 */
    val xiongDi: XiongDiResult,
    /** 子女（第五块）——男杀子官女/女伤子食女；子女宫=时柱 */
    val ziNv: ZiNvResult,
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

/** 父母分析结果：有利/不利断语 + 小结。 */
data class FuMuResult(
    val favorable: List<LiuQinItem> = emptyList(),
    val unfavorable: List<LiuQinItem> = emptyList(),
    val summary: String = "",
)

/** 子女分析结果：头胎性别+子女数目+宫位注记+断语。 */
data class ZiNvResult(
    /** 头胎性别推断，如 "头胎生女（时干丁与日干丙阴阳异性）" */
    val headFirst: String = "",
    /** 子女数目近似，如 "4个左右" */
    val count: String = "",
    /** 计数依据 */
    val countBasis: String = "",
    /** 子女宫（时支本气）与日干阴阳注记——同性儿女双全/异性多生女男 */
    val palaceNote: String? = null,
    val favorable: List<LiuQinItem> = emptyList(),
    val unfavorable: List<LiuQinItem> = emptyList(),
    val summary: String = "",
)

/** 兄弟姐妹分析结果：手足数目（左右数）+计数依据+排行+断语。 */
data class XiongDiResult(
    /** 如 "6个左右" */
    val count: String = "",
    /** 计数机制说明（加倍/减半/空亡/墓门/从格/印比合计） */
    val countBasis: String = "",
    /** 排行注记（年月见比劫→非老大），null=老大或不显 */
    val ranking: String? = null,
    val favorable: List<LiuQinItem> = emptyList(),
    val unfavorable: List<LiuQinItem> = emptyList(),
    val summary: String = "",
)
