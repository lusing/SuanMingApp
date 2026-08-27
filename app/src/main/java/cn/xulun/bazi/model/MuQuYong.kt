package cn.xulun.bazi.model

/** 木命逐月取用的调候态。 */
enum class MuTiaoHou(val chinese: String) {
    XU_HUO("需火调候"),
    YONG_SHUI("以水调候，水火既济"),
    BU_XU("不需调候（已自调）"),
    BU_JIN("调候不为紧要"),
    HAN_JI("寒极无火，岁运忌虚浮之火"),
}

/** 木命取用的特殊标记。 */
enum class MuSpecial(val chinese: String) {
    NONE(""),
    SUI_YUN("中和，用神宜随运而取"),
    HE_ZHI("用神被合，两失其用"),
    DAI_SUI_YUN("通关之神力不足，有待岁运弥补"),
}

/**
 * 木之逐月取用结果（甲乙日主专用）。理论见 `.shengke-reference.md` 第四四节。
 */
data class MuQuYongResult(
    /** 月令取用总纲（如"寅月犹有余寒，首取丙火"） */
    val monthRule: String,
    /** 日主强弱（八步法引擎） */
    val strength: DayStrength,
    /** 用神大类 */
    val yongShen: GodCategory,
    /** 辅用大类（null=无） */
    val fuYong: GodCategory?,
    /** 用神细注（如"丙火泄秀调候，一石二鸟"） */
    val detail: String,
    /** 调候态 */
    val tiaoHou: MuTiaoHou,
    /** 特殊标记 */
    val special: MuSpecial,
    /** 推理链 */
    val basis: String,
) {
    override fun toString(): String =
        "木命取${yongShen.chinese}${fuYong?.let { "兼${it.chinese}" } ?: ""}（${strength.chinese}，${tiaoHou.chinese}）"
}
