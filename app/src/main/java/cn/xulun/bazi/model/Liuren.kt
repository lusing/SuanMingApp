package cn.xulun.bazi.model

import java.lang.Math.floorMod

/**
 * 大六壬数据模型。
 *
 * 起课链路：四柱 → 月将(中气换将) → 天盘(月将加占时旋转) → 四课 → 三传(九宗门) → 天将(贵人歌+昼夜+顺逆)。
 * 规则固化见 G:\code\zhouyi\bazia\.liuren-reference.md。
 */

/** 月将（十二将），中气换将。[branch] 为所对应地支，[title] 为专名（登明…神后）。 */
enum class YueJiang(val branch: EarthlyBranch, val title: String) {
    DENG_MING(EarthlyBranch.HAI, "登明"),
    HE_KUI(EarthlyBranch.XU, "河魁"),
    CONG_KUI(EarthlyBranch.YOU, "从魁"),
    CHUAN_SONG(EarthlyBranch.SHEN, "传送"),
    XIAO_JI(EarthlyBranch.WEI, "小吉"),
    SHENG_GUANG(EarthlyBranch.WU, "胜光"),
    TAI_YI(EarthlyBranch.SI, "太乙"),
    TIAN_GANG(EarthlyBranch.CHEN, "天罡"),
    TAI_CHONG(EarthlyBranch.MAO, "太冲"),
    GONG_CAO(EarthlyBranch.YIN, "功曹"),
    DA_JI(EarthlyBranch.CHOU, "大吉"),
    SHEN_HOU(EarthlyBranch.ZI, "神后");

    companion object {
        fun of(branch: EarthlyBranch): YueJiang = entries.first { it.branch == branch }
    }
}

/** 十二天将（顺序敏感：自贵人起，顺布或逆布）。吉凶为通行归类。 */
enum class TianJiang(val chinese: String, val auspicious: Boolean) {
    GUI_REN("贵人", true),
    TENG_SHE("腾蛇", false),
    ZHU_QUE("朱雀", false),
    LIU_HE("六合", true),
    GOU_CHEN("勾陈", false),
    QING_LONG("青龙", true),
    TIAN_KONG("天空", false),
    BAI_HU("白虎", false),
    TAI_CHANG("太常", true),
    XUAN_WU("玄武", false),
    TAI_YIN("太阴", true),
    TIAN_HOU("天后", true);
}

/** 九宗门（定初传之法）+ 伏吟/返吟。 */
enum class ZongMen(val chinese: String) {
    YUAN_SHOU("元首"),   // 上克下，恰一课
    ZHONG_SHEN("重审"),  // 下贼上，恰一课
    BI_YONG("比用"),     // 多克贼，与日干阴阳不全相比
    SHE_HAI("涉害"),     // 俱比或俱不比，涉害深浅+孟仲季
    YAO_KE("遥克"),      // 四课无克，取遥克
    MAO_XING("昴星"),    // 无克无遥，四课全备
    BIE_ZE("别责"),      // 四课仅三备
    BA_ZHUAN("八专"),    // 干支同宫（干寄宫==日支）
    FU_YIN("伏吟"),
    FAN_YIN("返吟"),
}

/**
 * 一课。[top] 为上神；[bottomBranch] 为下神地盘位（用于定位与显示）；
 * 干课(一课)的 [bottomStem] 记日干——克贼五行取日干而非寄宫支（如乙寄辰，仍以乙木论）。
 */
data class Lesson(
    val index: Int,                       // 1..4
    val top: EarthlyBranch,               // 上神
    val bottomBranch: EarthlyBranch,      // 下神地盘位
    val bottomStem: HeavenlyStem? = null, // 干课一课记日干
) {
    /** 下神五行（干课取日干，支课取下神支） */
    val bottomElement: Element
        get() = bottomStem?.element ?: bottomBranch.element

    /** 下神显示文字（干课显干，支课显支） */
    val bottomText: String get() = bottomStem?.chinese ?: bottomBranch.chinese

    /** 是否上克下（上神五行克下神五行） */
    val isShangKeXia: Boolean get() = top.element.controls == bottomElement

    /** 是否下贼上（下神五行克上神五行） */
    val isXiaZeiShang: Boolean get() = bottomElement.controls == top.element

    /** 本课是否存在克贼 */
    val hasKeZei: Boolean get() = isShangKeXia || isXiaZeiShang
}

/** 三传：初/中/末 + 所用宗门 + 课名（显示用，如「元首」「涉害」「蒿矢」「伏吟·自任」「返吟·无依」）。 */
data class ThreeChuan(
    val initial: EarthlyBranch,
    val middle: EarthlyBranch,
    val last: EarthlyBranch,
    val zongMen: ZongMen,
    val keMing: String,
)

/** 天将落宫：天将 + 所临天盘支（所乘）+ 所落地盘支。天将骑天盘。 */
data class TianJiangOnPlate(
    val jiang: TianJiang,
    val heavenBranch: EarthlyBranch,  // 天将所乘之天盘支
    val earthBranch: EarthlyBranch,   // 所落地盘支
)

/** 年命：行年（性别+虚岁推）与本命支（可选）。 */
data class NianMing(
    val xingNian: EarthlyBranch?,
    val benMing: EarthlyBranch?,
)

/** 大六壬课盘。 */
data class LiuRenPlate(
    val yueJiang: YueJiang,
    val hourBranch: EarthlyBranch,            // 占时
    val dayStem: HeavenlyStem,                // 日干
    val dayBranch: EarthlyBranch,             // 日支
    val hourCycleIndex: Int,                  // 时柱六十甲子序号（推旬空/旬丁）
    val yearBranch: EarthlyBranch,            // 占年（太岁，神煞用）
    val monthBranch: EarthlyBranch,           // 占月（神煞用）
    val isDay: Boolean,                       // 昼占
    val lessons: List<Lesson>,                // 四课（顺序 1..4）
    val threeChuan: ThreeChuan,
    val tianJiang: List<TianJiangOnPlate>,    // 十二天将落宫
    val nianMing: NianMing,
    val tianPanOffset: Int,                   // 天盘相对地盘旋转步数 = (月将 - 占时) mod 12
) {
    /** 天盘某地盘位之上的天盘支 */
    fun heavenAt(earthBranch: EarthlyBranch): EarthlyBranch =
        EarthlyBranch.fromIndex(floorMod(earthBranch.ordinal + tianPanOffset, 12))

    /** 四课索引访问 */
    fun lesson(i: Int): Lesson = lessons[i - 1]
}

/** 毕法赋命中条目。 */
data class BiFaHit(
    val no: Int,            // 法号 1..100
    val name: String,       // 法名
    val auspicious: Boolean,
    val basis: String,      // 命中依据（盘面要素）
    val duanYu: String,     // 断语
    val note: Boolean = false, // 警示类（法 93/97/98），非吉凶判定
)

/** 课体（非百法的特殊课，如斩关/游子/三交/乱首/龙战）。 */
data class KeTi(
    val name: String,
    val auspicious: Boolean,
    val basis: String,
)
