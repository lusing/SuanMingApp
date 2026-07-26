package cn.xulun.bazi.model

/** 财星（正财 / 偏财） */
data class WealthStar(
    val stem: HeavenlyStem,
    /** 正财或偏财 */
    val tenGod: TenGod,
    /** 柱位，如 "年干" / "年支·中气" */
    val location: String,
    /** 是否藏于地支藏干（false = 透干） */
    val hidden: Boolean,
)

/** 财库情况 */
data class WealthStorage(
    /** 财五行对应的墓库地支 */
    val branch: EarthlyBranch,
    /** 原局是否含此墓库 */
    val present: Boolean,
    /** 在原局的位置，如 "年支"；原局无则为空串 */
    val location: String,
    /** 是否被对宫冲开 */
    val opened: Boolean,
    /** 被何冲开，如 "辰戌相冲"；未开为 null */
    val openBy: String?,
)

/** 富格类型 */
enum class WealthPattern(val chinese: String, val summary: String) {
    STRONG_WEALTH("身旺财旺", "身强能任财，财源丰厚，主富"),
    WEAK_HEAVY_WEALTH("财多身弱", "财虽多而身弱难任，富屋贫人"),
    STRONG_LIGHT_WEALTH("身旺财弱", "身强而财薄，待运而发"),
    WEAK_LIGHT_WEALTH("身弱财弱", "身财俱弱，财运平淡"),
    NO_WEALTH_STRONG("身旺无财", "比劫旺而无财星，求财多竞争"),
    NO_WEALTH_WEAK("身弱无财", "财星不显，财运平淡"),
    BALANCED("身财两停", "身财均衡，财运平稳"),
}

/** 一段带财星的大运 / 流年 */
data class FortunePeriod(
    /** 标签，如 "庚辰大运" / "2000 庚辰" */
    val label: String,
    val pillar: Pillar,
    /** 财星描述，如 "偏财透干" / "正财藏支" */
    val desc: String,
)

/** 岁运财运 */
data class YunFortune(
    /** 带财的大运 */
    val dayun: List<FortunePeriod>,
    /** 带财的流年 */
    val liunian: List<FortunePeriod>,
)

/** 财运分析总结果 */
data class WealthResult(
    /** 财五行（日主所克） */
    val wealthElement: Element,
    /** 原局财星 */
    val stars: List<WealthStar>,
    /** 财库 */
    val storage: WealthStorage,
    /** 富格 */
    val pattern: WealthPattern,
    /** 富格判定依据文案 */
    val patternBasis: String,
    /** 岁运财运 */
    val yunFortune: YunFortune,
)

/**
 * 财五行 → 墓库地支。
 * 木→未、火→戌、金→丑、水→辰；土寄水库（取辰，土库历来有争议）。
 */
val Element.wealthStorage: EarthlyBranch
    get() = when (this) {
        Element.WOOD -> EarthlyBranch.WEI
        Element.FIRE -> EarthlyBranch.XU
        Element.METAL -> EarthlyBranch.CHOU
        Element.WATER -> EarthlyBranch.CHEN
        Element.EARTH -> EarthlyBranch.CHEN
    }
