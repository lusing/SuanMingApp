package cn.xulun.bazi.model

/** 十神大类（不分正偏），病药取用按类论。 */
enum class GodCategory(val chinese: String) {
    BI("比劫"), YIN("印绶"), SHI("食伤"), CAI("财星"), GUAN("官杀");
}

/** 取用方法。 */
enum class YongShenMethod(val chinese: String) {
    FU("扶"),
    YI("抑"),
    TONG_GUAN("通关"),
    SHUN_SHI("顺势"),
    WU_BING("中和无病"),
}

/** 调候与扶抑的关系。 */
enum class TiaoHouState(val chinese: String) {
    NONE("不需调候"),
    ALIGNED("调候扶抑一致"),
    CONFLICT("调候冲突，以扶抑为主"),
    WU_GEN("调候神无根，不宜调候"),
}

/**
 * 病药取用（传统扶抑法）结果。与子平真诠用神（[YongShenCalculator]）是两派并行输出。
 * 理论依据见仓库根 `.shengke-reference.md` 第三十九~四十一节。
 */
data class BingYaoResult(
    /** 日主强弱 */
    val strength: DayStrength,
    /** 从格检测（null=非从格） */
    val congGe: CongGeResult?,
    /** 病（最关键的致病十神大类）；中和/顺势时为 null */
    val bing: GodCategory?,
    /** 用神大类 */
    val yongShen: GodCategory,
    /** 用神五行 */
    val yongShenElement: Element,
    /** 取用方法 */
    val method: YongShenMethod,
    /** 喜神大类（null=未取） */
    val xiShen: GodCategory?,
    /** 忌神大类列表 */
    val jiShen: List<GodCategory>,
    /** 调候状态 */
    val tiaoHou: TiaoHouState,
    /** 推理链说明 */
    val basis: String,
    /** 干支强弱排序（八步法引擎产出），如"亥子甲未己丑辛丁" */
    val strengthRanking: String = "",
) {
    override fun toString(): String =
        "${method.chinese}取${yongShen.chinese}（${yongShenElement.chinese}），病${bing?.chinese ?: "无"}，${strength.chinese}"
}
