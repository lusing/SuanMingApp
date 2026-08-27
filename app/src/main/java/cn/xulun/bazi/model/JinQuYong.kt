package cn.xulun.bazi.model

/** 金命逐月取用的调候态。 */
enum class JinTiaoHou(val chinese: String) {
    HUO_TU("火土并见（火暖土生，火通木土之关）"),
    BU_XU("不需调候（阳气渐升）"),
    SHUI_ZHI("水制火存金（儿能救母）"),
    DING_DUAN("丁火煅炼（钟鼎之材）"),
    HUO_NUAN("冬寒火暖，燥土为良"),
}

/** 金命取用的特殊标记。 */
enum class JinSpecial(val chinese: String) {
    NONE(""),
    SUI_YUN("金木力差不大，随岁运而变"),
    SHEN_SI_HE_JIAN("申巳相合，克力有减"),
    WU_WEI_HE_ZAO("午未合增燥，印如杀"),
    YIN_RU_SHA("未土炎燥脆金，其印如杀"),
    CENG_CI("用神有层次，病重药轻"),
}

/**
 * 金之逐月取用结果（庚辛日主专用）。理论见 `.shengke-reference.md` 第四七节。
 */
data class JinQuYongResult(
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
    val tiaoHou: JinTiaoHou,
    /** 特殊标记 */
    val special: JinSpecial,
    /** 推理链 */
    val basis: String,
) {
    override fun toString(): String =
        "金命取${yongShen.chinese}${fuYong?.let { "兼${it.chinese}" } ?: ""}（${strength.chinese}，${tiaoHou.chinese}）"
}
