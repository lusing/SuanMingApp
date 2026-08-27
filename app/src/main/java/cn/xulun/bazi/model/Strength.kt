package cn.xulun.bazi.model

/** 单个干支的强弱评估项。 */
data class GanZhiStrength(
    /** 柱位标签：年干/年支/月干/月支/日干/日支/时干/时支/运干/运支 */
    val label: String,
    /** 汉字（如"亥"） */
    val chinese: String,
    val element: Element,
    /** 强弱分（越高越强） */
    val score: Double,
    /** 评估说明（旺衰/党众/合化/同柱/贴邻等修正） */
    val note: String,
)

/**
 * 强弱引擎（八步法）结果。理论见 `.shengke-reference.md` 第卅五~卅八节。
 *
 * ranking 为全干支按分降序的汉字串（教材两例的验收形式）。
 */
data class StrengthResult(
    /** 各干支强弱（已按分降序） */
    val items: List<GanZhiStrength>,
    /** 降序汉字串，如"亥子甲未己丑辛丁" */
    val ranking: String,
    /** 生助方合力（A：同我+生我，紧密度×关系系数加权） */
    val aSide: Double,
    /** 克损方合力（B：克我+我克+我生） */
    val bSide: Double,
    /** 日主强弱终判 */
    val verdict: DayStrength,
    /** 推理链 */
    val basis: String,
) {
    override fun toString(): String = "$ranking（日主${verdict.chinese}，扶$aSide 对 耗$bSide）"
}
