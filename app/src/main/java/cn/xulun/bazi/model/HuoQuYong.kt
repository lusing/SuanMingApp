package cn.xulun.bazi.model

/** 火命逐月取用的调候态。 */
enum class HuoTiaoHou(val chinese: String) {
    XU_SHUI("需水调候（丙用壬、丁用癸）"),
    JIAN_GU("扶抑调候兼顾（申月余炎）"),
    BU_XU("不需调候（身弱火不烈，金自调候）"),
    SHUI_ZHONG("水太过，不可再以水调候"),
    DONG("冬寒忌金生水，木火相生"),
}

/** 火命取用的特殊标记。 */
enum class HuoSpecial(val chinese: String) {
    NONE(""),
    SUI_YUN("中和，用神宜随运而取"),
    HE_CAI("丙辛合财，反怯可用金"),
    GE_HE_DAI_SHI("根被隔合乏力，一用终生"),
    MU_DU_HUO_ZHI("木多火窒，印重反为忌（生多为克）"),
    BU_HUA_RENG_GEN("合局不化，仍为日主之根"),
}

/**
 * 火之逐月取用结果（丙丁日主专用）。理论见 `.shengke-reference.md` 第四五节。
 */
data class HuoQuYongResult(
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
    val tiaoHou: HuoTiaoHou,
    /** 特殊标记 */
    val special: HuoSpecial,
    /** 推理链 */
    val basis: String,
) {
    override fun toString(): String =
        "火命取${yongShen.chinese}${fuYong?.let { "兼${it.chinese}" } ?: ""}（${strength.chinese}，${tiaoHou.chinese}）"
}
