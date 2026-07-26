package cn.xulun.bazi.core

import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Pillar
import java.lang.Math.floorMod

/** 日干与地支（以其五行论）的五类关系：比/印/食伤/财/官鬼 */
enum class GanZhiRel { BI, YIN, SHI_SHANG, CAI, GUI }

/**
 * 大六壬查表（纯数据 + 纯函数）。规则固化见 .liuren-reference.md。
 *
 * 旬空/旬丁/旬首尾由六十甲子序号 [cycleIndex] 推导（公式法，避免硬编码笔误）；
 * 伏吟/返吟三传用《六壬大全》固定查表（含杜传、井栏射等边界，直接硬编码最稳）。
 */
object LiuRenTables {

    // ── 干寄宫（甲课寅兮乙课辰…）──
    val jiGong: Map<HeavenlyStem, EarthlyBranch> = mapOf(
        HeavenlyStem.JIA to EarthlyBranch.YIN,
        HeavenlyStem.YI to EarthlyBranch.CHEN,
        HeavenlyStem.BING to EarthlyBranch.SI,
        HeavenlyStem.DING to EarthlyBranch.WEI,
        HeavenlyStem.WU to EarthlyBranch.SI,
        HeavenlyStem.JI to EarthlyBranch.WEI,
        HeavenlyStem.GENG to EarthlyBranch.SHEN,
        HeavenlyStem.XIN to EarthlyBranch.XU,
        HeavenlyStem.REN to EarthlyBranch.HAI,
        HeavenlyStem.GUI to EarthlyBranch.CHOU,
    )

    /** 地支所寄之干（反向；子午卯酉无寄干） */
    val ganOfBranch: Map<EarthlyBranch, List<HeavenlyStem>> =
        jiGong.entries.groupBy({ it.value }, { it.key }).mapValues { it.value }

    // ── 禄 / 墓 / 长生（按日干）──
    val lu: Map<HeavenlyStem, EarthlyBranch> = mapOf(
        HeavenlyStem.JIA to EarthlyBranch.YIN, HeavenlyStem.YI to EarthlyBranch.MAO,
        HeavenlyStem.BING to EarthlyBranch.SI, HeavenlyStem.DING to EarthlyBranch.WU,
        HeavenlyStem.WU to EarthlyBranch.SI, HeavenlyStem.JI to EarthlyBranch.WU,
        HeavenlyStem.GENG to EarthlyBranch.SHEN, HeavenlyStem.XIN to EarthlyBranch.YOU,
        HeavenlyStem.REN to EarthlyBranch.HAI, HeavenlyStem.GUI to EarthlyBranch.ZI,
    )

    val mu: Map<HeavenlyStem, EarthlyBranch> = mapOf(
        HeavenlyStem.JIA to EarthlyBranch.WEI, HeavenlyStem.YI to EarthlyBranch.WEI,
        HeavenlyStem.BING to EarthlyBranch.XU, HeavenlyStem.DING to EarthlyBranch.XU,
        HeavenlyStem.WU to EarthlyBranch.XU, HeavenlyStem.JI to EarthlyBranch.XU,
        HeavenlyStem.GENG to EarthlyBranch.CHOU, HeavenlyStem.XIN to EarthlyBranch.CHOU,
        HeavenlyStem.REN to EarthlyBranch.CHEN, HeavenlyStem.GUI to EarthlyBranch.CHEN,
    )

    val changSheng: Map<HeavenlyStem, EarthlyBranch> = mapOf(
        HeavenlyStem.JIA to EarthlyBranch.HAI, HeavenlyStem.YI to EarthlyBranch.WU,
        HeavenlyStem.BING to EarthlyBranch.YIN, HeavenlyStem.DING to EarthlyBranch.YOU,
        HeavenlyStem.WU to EarthlyBranch.YIN, HeavenlyStem.JI to EarthlyBranch.YOU,
        HeavenlyStem.GENG to EarthlyBranch.SI, HeavenlyStem.XIN to EarthlyBranch.ZI,
        HeavenlyStem.REN to EarthlyBranch.SHEN, HeavenlyStem.GUI to EarthlyBranch.MAO,
    )

    // ── 地支六合 / 六害 / 六冲（无序对）──
    private val liuHePairs: Set<Set<EarthlyBranch>> = setOf(
        setOf(EarthlyBranch.ZI, EarthlyBranch.CHOU),
        setOf(EarthlyBranch.YIN, EarthlyBranch.HAI),
        setOf(EarthlyBranch.MAO, EarthlyBranch.XU),
        setOf(EarthlyBranch.CHEN, EarthlyBranch.YOU),
        setOf(EarthlyBranch.SI, EarthlyBranch.SHEN),
        setOf(EarthlyBranch.WU, EarthlyBranch.WEI),
    )

    private val liuHaiPairs: Set<Set<EarthlyBranch>> = setOf(
        setOf(EarthlyBranch.ZI, EarthlyBranch.WEI),
        setOf(EarthlyBranch.CHOU, EarthlyBranch.WU),
        setOf(EarthlyBranch.YIN, EarthlyBranch.SI),
        setOf(EarthlyBranch.MAO, EarthlyBranch.CHEN),
        setOf(EarthlyBranch.SHEN, EarthlyBranch.HAI),
        setOf(EarthlyBranch.YOU, EarthlyBranch.XU),
    )

    fun liuHe(a: EarthlyBranch, b: EarthlyBranch): Boolean = setOf(a, b) in liuHePairs
    fun liuHai(a: EarthlyBranch, b: EarthlyBranch): Boolean = setOf(a, b) in liuHaiPairs
    fun chong(a: EarthlyBranch, b: EarthlyBranch): Boolean = floorMod(a.ordinal - b.ordinal, 12) == 6

    // ── 三合局 ──
    private val sanHeGroups: List<Set<EarthlyBranch>> = listOf(
        setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN),
        setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI),
        setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU),
        setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU),
    )

    /** 日支所在三合局（顺序：长生/帝旺/墓，如申子辰） */
    fun sanHeGroupOf(branch: EarthlyBranch): List<EarthlyBranch> =
        sanHeGroups.first { branch in it }.let { g -> listOf(g.first(), g.toList()[1], g.toList()[2]) }

    // ── 相刑（自刑：辰午酉亥）──
    val xingOf: Map<EarthlyBranch, EarthlyBranch> = mapOf(
        EarthlyBranch.YIN to EarthlyBranch.SI, EarthlyBranch.SI to EarthlyBranch.SHEN, EarthlyBranch.SHEN to EarthlyBranch.YIN,
        EarthlyBranch.CHOU to EarthlyBranch.XU, EarthlyBranch.XU to EarthlyBranch.WEI, EarthlyBranch.WEI to EarthlyBranch.CHOU,
        EarthlyBranch.ZI to EarthlyBranch.MAO, EarthlyBranch.MAO to EarthlyBranch.ZI,
        EarthlyBranch.CHEN to EarthlyBranch.CHEN, EarthlyBranch.WU to EarthlyBranch.WU,
        EarthlyBranch.YOU to EarthlyBranch.YOU, EarthlyBranch.HAI to EarthlyBranch.HAI,
    )

    val ziXing: Set<EarthlyBranch> = setOf(EarthlyBranch.CHEN, EarthlyBranch.WU, EarthlyBranch.YOU, EarthlyBranch.HAI)

    /** 冲 */
    fun chongOf(b: EarthlyBranch): EarthlyBranch = EarthlyBranch.fromIndex(floorMod(b.ordinal + 6, 12))

    // ── 华盖 / 桃花 / 驿马（按三合局）──
    private val sanHeDerived: Map<Set<EarthlyBranch>, Triple<EarthlyBranch, EarthlyBranch, EarthlyBranch>> = mapOf(
        setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN) to Triple(EarthlyBranch.CHEN, EarthlyBranch.YOU, EarthlyBranch.YIN), // 华盖辰 桃花酉 驿马寅
        setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI) to Triple(EarthlyBranch.WEI, EarthlyBranch.ZI, EarthlyBranch.SI),
        setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU) to Triple(EarthlyBranch.XU, EarthlyBranch.MAO, EarthlyBranch.SHEN),
        setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU) to Triple(EarthlyBranch.CHOU, EarthlyBranch.WU, EarthlyBranch.HAI),
    )

    fun huaGai(branch: EarthlyBranch): EarthlyBranch = sanHeDerived.entries.first { branch in it.key }.value.first
    fun taoHua(branch: EarthlyBranch): EarthlyBranch = sanHeDerived.entries.first { branch in it.key }.value.second
    fun yiMa(branch: EarthlyBranch): EarthlyBranch = sanHeDerived.entries.first { branch in it.key }.value.third

    /** 天马：正月起午顺行六阳位（午申戌子寅辰）。monthIndex 1..12（寅月=1） */
    private val tianMaSeq: List<EarthlyBranch> = listOf(
        EarthlyBranch.WU, EarthlyBranch.SHEN, EarthlyBranch.XU, EarthlyBranch.ZI,
        EarthlyBranch.YIN, EarthlyBranch.CHEN, // 六阳循环
    )

    fun tianMa(monthBranch: EarthlyBranch): EarthlyBranch {
        // 月支序（寅=1）：寅2→0
        val m = floorMod(monthBranch.ordinal - 2, 12)
        return tianMaSeq[m % 6]
    }

    // ── 贵人歌（正法版，非清颠倒）日干 → (昼贵, 夜贵) ──
    val guiRen: Map<HeavenlyStem, Pair<EarthlyBranch, EarthlyBranch>> = mapOf(
        HeavenlyStem.JIA to (EarthlyBranch.CHOU to EarthlyBranch.WEI),
        HeavenlyStem.WU to (EarthlyBranch.CHOU to EarthlyBranch.WEI),
        HeavenlyStem.GENG to (EarthlyBranch.CHOU to EarthlyBranch.WEI),
        HeavenlyStem.YI to (EarthlyBranch.ZI to EarthlyBranch.SHEN),
        HeavenlyStem.JI to (EarthlyBranch.ZI to EarthlyBranch.SHEN),
        HeavenlyStem.BING to (EarthlyBranch.HAI to EarthlyBranch.YOU),
        HeavenlyStem.DING to (EarthlyBranch.HAI to EarthlyBranch.YOU),
        HeavenlyStem.REN to (EarthlyBranch.SI to EarthlyBranch.MAO),
        HeavenlyStem.GUI to (EarthlyBranch.SI to EarthlyBranch.MAO),
        HeavenlyStem.XIN to (EarthlyBranch.WU to EarthlyBranch.YIN),
    )

    // ── 四孟 / 四仲 / 四季 ──
    val siMeng: Set<EarthlyBranch> = setOf(EarthlyBranch.YIN, EarthlyBranch.SHEN, EarthlyBranch.SI, EarthlyBranch.HAI)
    val siZhong: Set<EarthlyBranch> = setOf(EarthlyBranch.ZI, EarthlyBranch.WU, EarthlyBranch.MAO, EarthlyBranch.YOU)
    val siJi: Set<EarthlyBranch> = setOf(EarthlyBranch.CHEN, EarthlyBranch.XU, EarthlyBranch.CHOU, EarthlyBranch.WEI)

    // ── 旬法（由六十甲子序号推导）──
    /** 旬首支（甲子旬→子、甲戌→戌…） */
    fun xunShouBranch(cycleIndex: Int): EarthlyBranch = Pillar.fromCycleIndex(cycleIndex / 10 * 10).branch

    /** 旬空两支 = 旬首前两位 */
    fun xunKong(cycleIndex: Int): Set<EarthlyBranch> {
        val s = xunShouBranch(cycleIndex).ordinal
        return setOf(EarthlyBranch.fromIndex(floorMod(s - 1, 12)), EarthlyBranch.fromIndex(floorMod(s - 2, 12)))
    }

    /** 旬尾支（旬首前一支） */
    fun xunWeiBranch(cycleIndex: Int): EarthlyBranch =
        EarthlyBranch.fromIndex(floorMod(xunShouBranch(cycleIndex).ordinal - 1, 12))

    /** 旬丁支（该旬遁干为丁之地支）= 旬首 + 3 */
    fun xunDingBranch(cycleIndex: Int): EarthlyBranch =
        Pillar.fromCycleIndex(cycleIndex / 10 * 10 + 3).branch

    /** 地支是否落旬空 */
    fun isKong(branch: EarthlyBranch, cycleIndex: Int): Boolean = branch in xunKong(cycleIndex)

    // ── 十恶大败日（禄空）──
    val shiEBai: Set<String> = setOf(
        "甲辰", "乙巳", "壬申", "丙申", "丁亥", "庚辰", "戊戌", "癸亥", "辛巳", "己丑",
    )

    // ── 伏吟 / 返吟 固定三传表（key=日柱fullName，value=三传支串如"寅巳申"）──
    // 取自《六壬大全》；含杜传(乙/壬自刑)、井栏射(返吟无克6日)等边界。

    /** 伏吟三传查表 */
    val fuYin: Map<String, String> = buildMap {
        // 六甲(自任格) → 寅巳申
        listOf("甲子", "甲戌", "甲申", "甲午", "甲辰", "甲寅").forEach { put(it, "寅巳申") }
        // 六丙(自任格) → 巳申寅
        listOf("丙子", "丙戌", "丙申", "丙午", "丙辰", "丙寅").forEach { put(it, "巳申寅") }
        // 六戊(戊寄巳，同丙) → 巳申寅
        listOf("戊辰", "戊寅", "戊子", "戊戌", "戊申", "戊午").forEach { put(it, "巳申寅") }
        // 六庚(自任格) → 申寅巳
        listOf("庚午", "庚辰", "庚寅", "庚子", "庚戌", "庚申").forEach { put(it, "申寅巳") }
        // 六癸(伏吟有克) → 丑戌未
        listOf("癸酉", "癸未", "癸巳", "癸卯", "癸丑", "癸亥").forEach { put(it, "丑戌未") }
        // 六乙(杜传格，乙寄辰为自刑)
        put("乙丑", "辰丑戌"); put("乙卯", "辰卯子"); put("乙巳", "辰巳申")
        put("乙未", "辰未丑"); put("乙酉", "辰酉卯"); put("乙亥", "辰亥巳")
        // 六壬(杜传格，壬寄亥为自刑)
        put("壬子", "亥子卯"); put("壬寅", "亥寅巳"); put("壬辰", "亥辰戌")
        put("壬午", "亥午子"); put("壬申", "亥申寅"); put("壬戌", "亥戌未")
        // 六丁(丁寄未)
        put("丁丑", "丑戌未"); put("丁卯", "卯子午"); put("丁巳", "巳申寅")
        put("丁未", "未丑戌"); put("丁酉", "酉未丑"); put("丁亥", "亥未丑")
        // 六己(己寄未，同丁)
        put("己丑", "丑戌未"); put("己卯", "卯子午"); put("己巳", "巳申寅")
        put("己未", "未丑戌"); put("己酉", "酉未丑"); put("己亥", "亥未丑")
        // 六辛(辛寄戌)
        put("辛丑", "丑戌未"); put("辛卯", "卯子午"); put("辛巳", "巳申寅")
        put("辛未", "未丑戌"); put("辛酉", "酉戌未"); put("辛亥", "亥戌未")
    }

    /** 返吟三传查表 */
    val fanYin: Map<String, String> = buildMap {
        // 有克(无依格)：初→冲→初
        listOf("甲子", "甲戌", "甲申", "甲午", "甲辰", "甲寅").forEach { put(it, "寅申寅") }
        listOf("庚午", "庚辰", "庚寅", "庚子", "庚戌", "庚申").forEach { put(it, "寅申寅") }
        listOf("丙子", "丙午", "戊子", "戊午", "壬子", "壬午").forEach { put(it, "午子午") }
        listOf("丙寅", "丙申", "戊寅", "戊申", "壬寅", "壬申").forEach { put(it, "寅申寅") }
        listOf("丙辰", "丙戌", "戊辰", "戊戌", "壬辰", "壬戌").forEach { put(it, "巳亥巳") }
        // 六乙
        listOf("乙丑", "乙未").forEach { put(it, "戌辰戌") }
        listOf("乙卯", "乙酉").forEach { put(it, "卯酉卯") }
        listOf("乙巳", "乙亥").forEach { put(it, "巳亥巳") }
        // 六癸
        listOf("癸丑", "癸未").forEach { put(it, "未丑未") }
        listOf("癸卯", "癸酉").forEach { put(it, "卯酉卯") }
        listOf("癸巳", "癸亥").forEach { put(it, "巳亥巳") }
        // 六丁/己/辛(有克8日)
        listOf("丁卯", "丁酉", "己卯", "己酉", "辛卯", "辛酉").forEach { put(it, "卯酉卯") }
        listOf("丁巳", "丁亥", "己巳", "己亥", "辛巳", "辛亥").forEach { put(it, "巳亥巳") }
        // 无克(井栏射/无亲格6日)：初=驿马, 中=支上神, 末=干上神
        put("丁丑", "亥未丑"); put("己丑", "亥未丑"); put("辛丑", "亥未辰")
        put("丁未", "巳丑丑"); put("己未", "巳丑丑"); put("辛未", "巳丑辰")
    }

    /** 把三传支串（如"寅巳申"）解析为三支 */
    fun parseChuan(s: String): List<EarthlyBranch> = s.toList().map { ch ->
        EarthlyBranch.entries.first { it.chinese == ch.toString() }
    }

    /** 地支（以其五行）相对日干的五类关系 */
    fun relOf(dayStem: HeavenlyStem, branch: EarthlyBranch): GanZhiRel {
        val me = dayStem.element
        val it = branch.element
        return when {
            me == it -> GanZhiRel.BI
            it.generates == me -> GanZhiRel.YIN
            me.generates == it -> GanZhiRel.SHI_SHANG
            me.controls == it -> GanZhiRel.CAI
            else -> GanZhiRel.GUI
        }
    }
}
