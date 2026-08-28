package cn.xulun.bazi.model

/** 印星（学历主星：正印 / 偏印） */
data class StudyStar(
    val stem: HeavenlyStem,
    val tenGod: TenGod,
    /** 柱位，如 "月干" / "月支·本气" */
    val location: String,
    /** 是否藏于地支藏干（false = 透干） */
    val hidden: Boolean,
)

/** 学历格局 */
enum class StudyPattern(val chinese: String, val summary: String) {
    HIGH("高学历", "印星旺相或透干为用，主学业有成、学历较高"),
    MEDIUM("中等学历", "印星偏弱，学历中等，后天努力可补"),
    TALENT("才艺型", "印星不显而食伤旺，聪明过人，利技艺/理科，正统学业或波折"),
    LOW("学业平平", "印星与食伤俱弱，学历普通"),
}

/** 学业神煞（文昌 / 学堂） */
data class StudyShenSha(
    val name: String,
    val branch: EarthlyBranch,
    /** 在原局的位置 */
    val location: String,
)

/** 利考学的运岁（印 / 官杀运） */
data class StudyFortunePeriod(
    val label: String,
    val pillar: Pillar,
    /** 描述，如 "正印透干" / "七杀藏支" */
    val desc: String,
)

data class StudyYunFortune(
    val dayun: List<StudyFortunePeriod>,
    val liunian: List<StudyFortunePeriod>,
)

/** 文凭断语条目（第十节·一）。 */
data class StudyDiplomaItem(
    /** 断语标题，如 "食伤泄秀为喜用" */
    val title: String,
    /** 断语全文 */
    val text: String,
    /** true=有利学历 / false=不利学历 */
    val favorable: Boolean,
)

/** 文凭推断结果（用神+印星为标志，少运定高低）。 */
data class StudyDiploma(
    /** 级别结论：高学历（硕博之才）/学历较高/学历中等/学历不高 */
    val level: String,
    /** 推断依据链（少运喜忌+格局），附时代提示 */
    val basis: String,
    val favorable: List<StudyDiplomaItem>,
    val unfavorable: List<StudyDiplomaItem>,
)

/** 学业分析总结果 */
data class StudyResult(
    val sealStars: List<StudyStar>,
    val pattern: StudyPattern,
    val patternBasis: String,
    val shenSha: List<StudyShenSha>,
    val yunFortune: StudyYunFortune,
    val diploma: StudyDiploma,
)

/** 文昌（日干食神长生位）：甲巳 乙午 丙戊申 丁己酉 庚亥 辛子 壬寅 癸卯 */
val HeavenlyStem.wenChang: EarthlyBranch
    get() = when (this) {
        HeavenlyStem.JIA -> EarthlyBranch.SI
        HeavenlyStem.YI -> EarthlyBranch.WU
        HeavenlyStem.BING -> EarthlyBranch.SHEN
        HeavenlyStem.DING -> EarthlyBranch.YOU
        HeavenlyStem.WU -> EarthlyBranch.SHEN
        HeavenlyStem.JI -> EarthlyBranch.YOU
        HeavenlyStem.GENG -> EarthlyBranch.HAI
        HeavenlyStem.XIN -> EarthlyBranch.ZI
        HeavenlyStem.REN -> EarthlyBranch.YIN
        HeavenlyStem.GUI -> EarthlyBranch.MAO
    }

/** 学堂（日干长生位） */
val HeavenlyStem.xueTang: EarthlyBranch
    get() = when (this) {
        HeavenlyStem.JIA -> EarthlyBranch.HAI
        HeavenlyStem.YI -> EarthlyBranch.WU
        HeavenlyStem.BING -> EarthlyBranch.YIN
        HeavenlyStem.DING -> EarthlyBranch.YOU
        HeavenlyStem.WU -> EarthlyBranch.YIN
        HeavenlyStem.JI -> EarthlyBranch.YOU
        HeavenlyStem.GENG -> EarthlyBranch.SI
        HeavenlyStem.XIN -> EarthlyBranch.ZI
        HeavenlyStem.REN -> EarthlyBranch.SHEN
        HeavenlyStem.GUI -> EarthlyBranch.MAO
    }
