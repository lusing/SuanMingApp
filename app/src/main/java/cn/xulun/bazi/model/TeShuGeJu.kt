package cn.xulun.bazi.model

/** 特殊格局种类。 */
enum class TeShuKind(val chinese: String) {
    JIA_CONG("假从"),
    DUO_CONG("多从"),
    ZHAN_JU("战局"),
    ZHUAN_WANG("专旺"),
    LIANG_QI("两气成象"),
    SAN_QI("三气成象"),
}

/**
 * 特殊格局取用结果。理论见 `.shengke-reference.md` 第四九节（单从/多从/战局/杂从/专旺/成象）。
 *
 * [yongShen] 为用神五行链（首选在前）：
 * - 单从/杂从：从势之行（假从标 [fake]）；
 * - 多从：整条相生链（如木火土）；
 * - 战局：通关之五行；
 * - 专旺：比印首选、有力食伤次之；
 * - 三气成象：三行中弱者补之（弱者在前）。
 */
data class TeShuGeJuResult(
    val kind: TeShuKind,
    /** 格局名，如"假从杀格"/"从革格"/"木火土三气成象"/"金木交战" */
    val name: String,
    /** 用神五行链（首选在前） */
    val yongShen: List<Element>,
    /** 喜神五行 */
    val xi: List<Element>,
    /** 忌神五行 */
    val ji: List<Element>,
    /** 假从/不太标准（有透干虚助或变节支） */
    val fake: Boolean,
    /** 还原神描述（岁运逢之则还原正格） */
    val restoreHint: String,
    /** 推理链 */
    val basis: String,
) {
    override fun toString(): String = "$name，取${yongShen.joinToString("→") { it.chinese }}"
}
