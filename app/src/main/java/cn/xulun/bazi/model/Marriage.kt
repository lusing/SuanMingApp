package cn.xulun.bazi.model

/** 配偶星（男命=财星，女命=官杀） */
data class SpouseStar(
    val stem: HeavenlyStem,
    val tenGod: TenGod,
    /** 柱位，如 "月干" / "年支·中气" */
    val location: String,
    /** 是否藏于地支藏干（false = 透干） */
    val hidden: Boolean,
)

/** 夫妻宫（日支）情况 */
data class SpousePalace(
    val branch: EarthlyBranch,
    /** 日支本气相对日主的十神 */
    val mainGod: TenGod,
    /** 日支藏干及其十神（本气在前） */
    val hiddenGods: List<Pair<HeavenlyStem, TenGod>>,
    /** 逢冲描述，null=无，如 "丑未相冲（年支+日支）" */
    val chong: String?,
    /** 逢合描述，null=无 */
    val liuHe: String?,
    /** 逢刑描述，null=无 */
    val xing: String?,
) {
    /** 夫妻宫安稳（无冲无刑） */
    val stable: Boolean get() = chong == null && xing == null
}

/** 婚姻格局 */
enum class MarriagePattern(val chinese: String, val summary: String) {
    UNSTABLE("婚姻动荡", "夫妻宫逢冲或逢刑，感情多变，宜晚婚或用心经营"),
    COMPLEX("感情复杂", "配偶星多且混杂，情缘纷繁"),
    SMOOTH("婚姻顺遂", "配偶星明现且夫妻宫安稳，姻缘和顺"),
    LATE("姻缘较晚", "配偶星弱或不显，待运而动"),
    PLAIN("婚姻平稳", "星宫中和，平淡稳定"),
}

/** 神煞（桃花 / 孤辰 / 寡宿） */
data class ShenSha(
    val name: String,
    val branch: EarthlyBranch,
    /** 在原局的位置，如 "时支" */
    val location: String,
)

/** 婚姻分析总结果 */
data class MarriageResult(
    val gender: Gender,
    /** 配偶星名："财星"（男）/ "官杀"（女） */
    val spouseStarName: String,
    val spouseStars: List<SpouseStar>,
    val palace: SpousePalace,
    val pattern: MarriagePattern,
    /** 格局判定依据文案 */
    val patternBasis: String,
    val shenSha: List<ShenSha>,
)
