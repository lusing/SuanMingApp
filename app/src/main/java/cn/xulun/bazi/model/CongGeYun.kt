package cn.xulun.bazi.model

/** 从格岁运判定。 */
enum class YunVerdict(val chinese: String) {
    /** 用神到位（岁运在从势内，助旺格局） */
    YONG("用神到位"),
    /** 喜神到位（食伤生扶旺势） */
    XI("喜神到位"),
    /** 转两气从格（强旺印/官/比劫与势两停，格局变为两气从格，新用神见 [CongGeYunResult.newYongShen]） */
    ZHUAN_GE("转两气从格"),
    /** 忌神（衰弱印/官/比劫，无力转变反杂乱气势/引日主破格伤身） */
    JI("忌神"),
    /** 还原正格（岁运为日主之根，刑动/帮身破格，从格还原身弱正格） */
    HUAN_YUAN("还原正格"),
}

/**
 * 从格岁运喜忌结果。理论见 `.shengke-reference.md` 第五二节（从财格行运喜忌，五格通用）。
 */
data class CongGeYunResult(
    /** 原局从格名 */
    val geName: String,
    /** 从势链 */
    val shi: List<Element>,
    /** 判定 */
    val verdict: YunVerdict,
    /** 转两气从格时的新用神（其余为 null） */
    val newYongShen: Element?,
    /** 推理链 */
    val basis: String,
) {
    override fun toString(): String = "$geName·${verdict.chinese}${newYongShen?.let { "（取${it.chinese}）" } ?: ""}"
}
