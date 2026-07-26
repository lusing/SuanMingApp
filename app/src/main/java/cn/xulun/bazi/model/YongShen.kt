package cn.xulun.bazi.model

/**
 * 子平真诠（清·沈孝瞻）「月令格局派」用神 / 相神分析结果类型。
 *
 * 与现代扶抑/平衡派不同：子平真诠的**用神取自月令所藏之神**（透干→会支→本气），
 * **相神**为护卫用神、使格局成立的关键字（"凡全局之格，赖此一字而成者，均谓之相也"），
 * 并据《论用神成败救应》判定格局**成败**。
 */

/** 用神取法来源 */
enum class YongShenSource(val chinese: String) {
    /** 月令藏干透于年/月/时干，取透出之干为用 */
    TOU_GAN("月令藏干透干取用"),

    /** 月令藏干俱不透，以月令本气为用（子平真诠：透干→会支→本气） */
    BEN_QI("月令本气（藏干俱不透）"),

    /** 月令为比劫（建禄/月劫），月令无外用神，于他柱另寻财官煞食为用 */
    OTHER("月令比劫，于他柱寻用"),
}

/**
 * 格局成败（子平真诠·论用神成败救应）。
 * [auspicious] 仅用于 UI 配色：true 偏吉（绿），false 偏凶（红）。
 */
enum class GeJuChengBai(val chinese: String, val auspicious: Boolean) {
    CHENG("成格", true),
    CHENG_ZHONG_BAI("成中有败（带忌）", false),
    BAI_ZHONG_CHENG("败中有成（救应）", true),
    BAI("败格", false),
    /** 既无相神成格、又无破神败格，格局欠清纯但不破 */
    PIAN("格偏（无相无破）", false),
}

/** 喜 / 忌 */
enum class XiJi(val chinese: String) {
    XI("喜"),
    JI("忌"),
}

/** 用神 / 相神星：天干 + 十神 + 位置 + 透干/藏支 */
data class YongShenStar(
    val stem: HeavenlyStem,
    val tenGod: TenGod,
    /** 出现位置，如 "月干" / "月支·本气" / "时干" / "年支·中气" */
    val location: String,
    /** false = 透干，true = 藏于地支 */
    val hidden: Boolean,
)

/** 运岁喜忌条目 */
data class XiJiItem(
    val kind: XiJi,
    val tenGod: TenGod,
    val desc: String,
)

/** 子平真诠用神 / 相神分析结果 */
data class YongShenResult(
    /** 用神（建禄/月劫且他柱无财官煞食透干时可为 null） */
    val yongShen: YongShenStar?,
    /** 格局名，如 "正官格" / "七杀格" / "建禄格" */
    val geJuName: String,
    /** 用神取法来源 */
    val source: YongShenSource,
    /** 用神判定依据 */
    val yongShenBasis: String,
    /** 相神（无得力相神时为 null） */
    val xiangShen: YongShenStar?,
    /** 相神作用，如 "财生官" / "食神制杀" / "杀印相生" */
    val xiangShenRole: String,
    /** 相神判定依据 */
    val xiangShenBasis: String,
    /** 格局成败 */
    val chengBai: GeJuChengBai,
    /** 成败依据（命中元素） */
    val chengBaiReason: String,
    /** 运岁喜忌 */
    val xiJi: List<XiJiItem>,
    /** 综合断语 */
    val duanYu: String,
    /** 日主强弱（参考，复用 GeJuCalculator.assessStrength） */
    val strength: DayStrength,
)
