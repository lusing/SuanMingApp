package cn.xulun.bazi.model

/** 水命逐月取用的调候态。 */
enum class ShuiTiaoHou(val chinese: String) {
    JI_HUO("初春寒流，尚需火以既济"),
    ZHI_LIU("散漫无收，土止其流"),
    JIN_SHUI("金水并用（岁运并行缺一不美）"),
    HUO_XU("秋强用土喜火"),
    DONG_HAN("冬水无火为缺陷，火金并用方生弱水"),
}

/** 水命取用的特殊标记。 */
enum class ShuiSpecial(val chinese: String) {
    NONE(""),
    SUI_YUN("中和，用神宜随运而取"),
    MU_CI("母慈灭子（印重身轻/木弱不受水生）"),
    LIANG_QI("两气成象，顺金水之势，忌木火"),
}

/**
 * 水之逐月取用结果（壬癸日主专用）。理论见 `.shengke-reference.md` 第四八节。
 */
data class ShuiQuYongResult(
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
    val tiaoHou: ShuiTiaoHou,
    /** 特殊标记 */
    val special: ShuiSpecial,
    /** 推理链 */
    val basis: String,
) {
    override fun toString(): String =
        "水命取${yongShen.chinese}${fuYong?.let { "兼${it.chinese}" } ?: ""}（${strength.chinese}，${tiaoHou.chinese}）"
}
