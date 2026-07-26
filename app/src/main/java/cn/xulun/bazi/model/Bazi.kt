package cn.xulun.bazi.model

import java.time.LocalDateTime
import java.lang.Math.floorMod

/** 地支藏干（本气在前，中气/余气在后） */
val branchHiddenStems: Map<EarthlyBranch, List<HeavenlyStem>> = mapOf(
    EarthlyBranch.ZI to listOf(HeavenlyStem.GUI),
    EarthlyBranch.CHOU to listOf(HeavenlyStem.JI, HeavenlyStem.GUI, HeavenlyStem.XIN),
    EarthlyBranch.YIN to listOf(HeavenlyStem.JIA, HeavenlyStem.BING, HeavenlyStem.WU),
    EarthlyBranch.MAO to listOf(HeavenlyStem.YI),
    EarthlyBranch.CHEN to listOf(HeavenlyStem.WU, HeavenlyStem.YI, HeavenlyStem.GUI),
    EarthlyBranch.SI to listOf(HeavenlyStem.BING, HeavenlyStem.WU, HeavenlyStem.GENG),
    EarthlyBranch.WU to listOf(HeavenlyStem.DING, HeavenlyStem.JI),
    EarthlyBranch.WEI to listOf(HeavenlyStem.JI, HeavenlyStem.DING, HeavenlyStem.YI),
    EarthlyBranch.SHEN to listOf(HeavenlyStem.GENG, HeavenlyStem.REN, HeavenlyStem.WU),
    EarthlyBranch.YOU to listOf(HeavenlyStem.XIN),
    EarthlyBranch.XU to listOf(HeavenlyStem.WU, HeavenlyStem.XIN, HeavenlyStem.DING),
    EarthlyBranch.HAI to listOf(HeavenlyStem.REN, HeavenlyStem.JIA),
)

/**
 * 计算 [other] 相对日主 [dayMaster] 的十神。
 * 同我比劫、我生食伤、我克财、克我官杀、生我印；阴阳同为偏，相异为正。
 */
fun tenGodOfStem(dayMaster: HeavenlyStem, other: HeavenlyStem): TenGod {
    val samePolarity = dayMaster.yang == other.yang
    val me = dayMaster.element
    val it = other.element
    return when {
        it == me -> if (samePolarity) TenGod.BI_JIAN else TenGod.JIE_CAI
        me.generates == it -> if (samePolarity) TenGod.SHI_SHEN else TenGod.SHANG_GUAN
        me.controls == it -> if (samePolarity) TenGod.PIAN_CAI else TenGod.ZHENG_CAI
        it.controls == me -> if (samePolarity) TenGod.QI_SHA else TenGod.ZHENG_GUAN
        else -> if (samePolarity) TenGod.PIAN_YIN else TenGod.ZHENG_YIN
    }
}

/** 地支本气（藏干第一位）相对日主的十神 */
fun tenGodOfBranch(dayMaster: HeavenlyStem, branch: EarthlyBranch): TenGod =
    tenGodOfStem(dayMaster, branchHiddenStems.getValue(branch).first())

/** 地支全部藏干及其十神（本气在前） */
fun hiddenStemGods(dayMaster: HeavenlyStem, branch: EarthlyBranch): List<Pair<HeavenlyStem, TenGod>> =
    branchHiddenStems.getValue(branch).map { it to tenGodOfStem(dayMaster, it) }

/**
 * 单柱（天干 + 地支）。
 * cycleIndex 由中国剩余定理求得：序号 n 同时满足 n ≡ stem(mod10)、n ≡ branch(mod12)。
 */
data class Pillar(val stem: HeavenlyStem, val branch: EarthlyBranch) {

    /** 六十甲子序号 0..59 */
    val cycleIndex: Int
        get() {
            val diff = floorMod(branch.ordinal - stem.ordinal, 12)
            val k = (5 * (diff / 2)) % 6
            return stem.ordinal + 10 * k
        }

    /** 纳音五行 */
    val nayin: Nayin get() = Nayin.fromCycleIndex(cycleIndex)

    /** 完整干支字符串，如 "甲子" */
    val fullName: String get() = "${stem.chinese}${branch.chinese}"

    override fun toString(): String = fullName

    companion object {
        /** 由六十甲子序号构造（自动取模到 0..59） */
        fun fromCycleIndex(cycleIndex: Int): Pillar {
            val i = floorMod(cycleIndex, 60)
            return Pillar(
                stem = HeavenlyStem.fromIndex(i % 10),
                branch = EarthlyBranch.fromIndex(i % 12),
            )
        }
    }
}

/** 生辰八字结果 */
data class BaziResult(
    val yearPillar: Pillar,
    val monthPillar: Pillar,
    val dayPillar: Pillar,
    val hourPillar: Pillar,
    val birthDateTime: LocalDateTime,
) {
    /** 四柱列表：年、月、日、时 */
    val pillars: List<Pillar> get() = listOf(yearPillar, monthPillar, dayPillar, hourPillar)

    /** 八字字符串，如 "甲辰 辛未 己丑 庚午" */
    val fullBazi: String
        get() = pillars.joinToString(" ") { it.fullName }

    /** 天干列表 */
    val stems: List<HeavenlyStem>
        get() = pillars.map { it.stem }

    /** 地支列表 */
    val branches: List<EarthlyBranch>
        get() = pillars.map { it.branch }

    override fun toString(): String = fullBazi
}

/** 四柱天干标签（年/月/日/时干），供关系分析器复用 */
val PillarStemLabels = listOf("年干", "月干", "日干", "时干")

/** 四柱地支标签（年/月/日/时支），供关系分析器复用 */
val PillarBranchLabels = listOf("年支", "月支", "日支", "时支")
