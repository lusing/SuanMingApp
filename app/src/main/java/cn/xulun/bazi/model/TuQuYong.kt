package cn.xulun.bazi.model

/** 土命逐月取用的调候态。 */
enum class TuTiaoHou(val chinese: String) {
    XU_HUO("寒土需火暖（兼化杀生身）"),
    SHUI_JIN("水金并用（水须有金源头）"),
    BU_XU("不需调候（寒暖适中）"),
    ZAO("过燥需润（扶抑调候兼顾）"),
    DONG("冬寒忌金水（木为火原神适量）"),
}

/** 土命取用的特殊标记。 */
enum class TuSpecial(val chinese: String) {
    NONE(""),
    SUI_YUN("中和，用神宜随运而取"),
    ZAO_JI("炎燥气势格局，岁运金水弱则破格、强则流通不破"),
    XU_TU("春土虚而不实（藏干有木培根）"),
}

/**
 * 土之逐月取用结果（戊己日主专用）。理论见 `.shengke-reference.md` 第四六节。
 */
data class TuQuYongResult(
    /** 月令取用总纲 */
    val monthRule: String,
    /** 日主强弱（八步法引擎） */
    val strength: DayStrength,
    /** 用神大类 */
    val yongShen: GodCategory,
    /** 辅用大类（null=无） */
    val fuYong: GodCategory?,
    /** 用神细注 */
    val detail: String,
    /** 调候态 */
    val tiaoHou: TuTiaoHou,
    /** 特殊标记 */
    val special: TuSpecial,
    /** 推理链 */
    val basis: String,
) {
    override fun toString(): String =
        "土命取${yongShen.chinese}${fuYong?.let { "兼${it.chinese}" } ?: ""}（${strength.chinese}，${tiaoHou.chinese}）"
}
