package cn.xulun.bazi.model

import cn.xulun.bazi.model.Pillar

/** 婚恋流年性质。 */
enum class HunLianKind(val chinese: String) {
    LIAN_AI("恋爱信息"),
    TONG_JU("同居·实质婚"),
    HUN_QING("婚庆嫁娶"),
    BIAN_DONG("婚变动荡"),
    WU("无信号"),
}

/** 婚恋流年条目（全量逐年）。 */
data class HunYinYear(
    val year: Int,
    /** 如 "1987 丁卯" */
    val label: String,
    val kind: HunLianKind,
    /** 引动源清单，如 ["官星透（丁正官）", "卯暗合配偶宫申"] */
    val triggers: List<String>,
    /** 断语 */
    val text: String,
    /** 该年虚岁/周岁提示（法定婚龄男22女20） */
    val ageNote: String = "",
)

/** 一步大运的婚恋流年。 */
data class HunYinYun(
    val pillar: Pillar,
    val label: String,
    val years: List<HunYinYear>,
)

/** 婚姻完整分析结果。 */
data class HunYinResult(
    /** 配偶星（男财/女官杀）现于何处的描述 */
    val spouseStarNote: String,
    /** 配偶宫（日支）描述（十神/冲合刑） */
    val palaceNote: String,
    /** 原局有利断语 */
    val baseItems: List<LiuQinItem> = emptyList(),
    /** 原局不利断语 */
    val baseUnfav: List<LiuQinItem> = emptyList(),
    val yuns: List<HunYinYun> = emptyList(),
    val summary: String = "",
)
