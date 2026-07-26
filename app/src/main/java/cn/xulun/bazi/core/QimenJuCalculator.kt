package cn.xulun.bazi.core

import cn.xulun.bazi.model.Dun
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.JuMethod
import cn.xulun.bazi.model.JuResult
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.Yuan
import java.time.LocalDate
import java.time.LocalDateTime
import java.lang.Math.floorMod

/**
 * 奇门遁甲定局：拆补法 与 超神接气（置闰古法）。
 *
 * 核心：两种法的「三元」其实一致——均由日柱符头（最近的甲/己日）地支定
 * （子午卯酉=上、寅申巳亥=中、辰戌丑未=下，等价于全局符头计数 mod 3）。
 * 区别只在「取哪个节气定局」：
 *  - 拆补法：用**实际节气**（按精确交节时刻），跨节气残候自然补回，不置闰。
 *  - 超神接气：用从冬至/夏至起、按符头计数的**名义节气**；大雪/芒种超神≥9天时
 *    插入闰候（重复该节气三元），使冬至/夏至仍起上元。
 *
 * 节气→局口诀表（上/中/下元），编码为 termIndex → [上,中,下]。
 */
object QimenJuCalculator {

    /** termIndex(0立春…21冬至…23大寒) → [上元,中元,下元] 局数 1..9 */
    private val juTable: Map<Int, IntArray> = mapOf(
        0 to intArrayOf(8, 2, 5),    // 立春
        1 to intArrayOf(9, 3, 6),    // 雨水
        2 to intArrayOf(1, 7, 4),    // 惊蛰
        3 to intArrayOf(3, 9, 6),    // 春分
        4 to intArrayOf(4, 1, 7),    // 清明
        5 to intArrayOf(5, 2, 8),    // 谷雨
        6 to intArrayOf(4, 1, 7),    // 立夏
        7 to intArrayOf(5, 2, 8),    // 小满
        8 to intArrayOf(6, 3, 9),    // 芒种
        9 to intArrayOf(9, 3, 6),    // 夏至
        10 to intArrayOf(8, 2, 5),   // 小暑
        11 to intArrayOf(7, 1, 4),   // 大暑
        12 to intArrayOf(2, 5, 8),   // 立秋
        13 to intArrayOf(1, 4, 7),   // 处暑
        14 to intArrayOf(9, 3, 6),   // 白露
        15 to intArrayOf(7, 1, 4),   // 秋分
        16 to intArrayOf(6, 9, 3),   // 寒露
        17 to intArrayOf(5, 8, 2),   // 霜降
        18 to intArrayOf(6, 9, 3),   // 立冬
        19 to intArrayOf(5, 8, 2),   // 小雪
        20 to intArrayOf(4, 7, 1),   // 大雪
        21 to intArrayOf(1, 7, 4),   // 冬至
        22 to intArrayOf(2, 8, 5),   // 小寒
        23 to intArrayOf(3, 9, 6),   // 大寒
    )

    /** 阳遁节气 termIndex（冬至21→夏至9） */
    private val yangTerms = setOf(21, 22, 23, 0, 1, 2, 3, 4, 5, 6, 7, 8)

    private fun isYang(termIndex: Int): Boolean = termIndex in yangTerms
    private fun termName(termIndex: Int): String = SolarTerm.termNames[termIndex]

    /** 取查询时刻所在节气（≤ dt 的最近交节） */
    private fun currentTermIndex(dt: LocalDateTime): Int {
        val tl = termTimeline(dt.year)
        var cur = tl.first()
        for (e in tl) {
            if (!e.second.isAfter(dt)) cur = e else break
        }
        return cur.first
    }

    /** [year-1..year+1] 三年 24 节气按时间排序 */
    private fun termTimeline(year: Int): List<Pair<Int, LocalDateTime>> =
        ((year - 1)..(year + 1)).flatMap { y ->
            val terms = SolarTerm.getYearSolarTerms(y)
            (0 until 24).map { it to terms[it] }
        }.sortedBy { it.second }

    /** 日柱六十甲子序号（复用 BaziCalculator 的 1900-01-01=甲戌 基准） */
    private fun dayCyc(dt: LocalDateTime): Int =
        BaziCalculator.calculate(dt).dayPillar.cycleIndex

    private fun dayCyc(date: LocalDate): Int = dayCyc(date.atStartOfDay())

    /** 符头地支 → 三元 */
    private fun yuanOf(branch: EarthlyBranch): Yuan = when (branch) {
        EarthlyBranch.ZI, EarthlyBranch.WU, EarthlyBranch.MAO, EarthlyBranch.YOU -> Yuan.SHANG
        EarthlyBranch.YIN, EarthlyBranch.SHEN, EarthlyBranch.SI, EarthlyBranch.HAI -> Yuan.ZHONG
        EarthlyBranch.CHEN, EarthlyBranch.XU, EarthlyBranch.CHOU, EarthlyBranch.WEI -> Yuan.XIA
    }

    /** 全局三元（符头计数 mod 3）：0上 1中 2下 */
    private fun globalYuan(dayCyc: Int): Yuan {
        val fuTouCyc = dayCyc - floorMod(dayCyc, 5)
        val ord = (fuTouCyc / 5) % 3
        return Yuan.entries[ord]
    }

    /** [date] 所在候的符头日期（最近的甲/己日，≤ date） */
    private fun fuTouDateOf(date: LocalDate): LocalDate {
        val offset = floorMod(dayCyc(date), 5)
        return date.minusDays(offset.toLong())
    }

    /** 距 [target] 最近的「上元符头」日期（全局三元=上的符头，每 15 天一个） */
    private fun nearestUpperFuTou(target: LocalDate): LocalDate {
        var f = fuTouDateOf(target)
        while (globalYuanOfDay(f) != Yuan.SHANG) f = f.minusDays(5)
        val prev = f
        val next = prev.plusDays(15)
        return if (target.toEpochDay() - prev.toEpochDay() <= next.toEpochDay() - target.toEpochDay()) prev else next
    }

    private fun globalYuanOfDay(date: LocalDate): Yuan = globalYuan(dayCyc(date))

    // ── 对外入口 ──
    fun determineJu(dt: LocalDateTime, method: JuMethod): JuResult {
        val dayPillar = BaziCalculator.calculate(dt).dayPillar
        val fuTouCyc = dayPillar.cycleIndex - floorMod(dayPillar.cycleIndex, 5)
        val fuTouName = Pillar.fromCycleIndex(fuTouCyc).fullName
        val yuan = yuanOf(Pillar.fromCycleIndex(fuTouCyc).branch)

        return when (method) {
            JuMethod.CHAIBU -> determineChaibu(dt, yuan, fuTouName)
            JuMethod.CHAOJIE -> determineChaojie(dt, yuan, fuTouName)
        }
    }

    /** 拆补法：实际节气 + 符头三元 */
    private fun determineChaibu(dt: LocalDateTime, yuan: Yuan, fuTouName: String): JuResult {
        val termIdx = currentTermIndex(dt)
        val dun = if (isYang(termIdx)) Dun.YANG else Dun.YIN
        val ju = juTable.getValue(termIdx)[yuan.ordinal]
        return JuResult(dun, ju, termName(termIdx), termIdx, yuan, JuMethod.CHAIBU, fuTouName, isLeap = false)
    }

    /**
     * 超神接气（置闰古法）：从最近的冬至/夏至（必起上元）起，按符头计数推进名义节气，
     * 每三符头进一节气；大雪(20)/芒种(8)的三元用尽、下一符头超前随后节气≥9天时置闰
     * （重复该节气三元），使冬至/夏至仍起上元。
     */
    private fun determineChaojie(dt: LocalDateTime, yuan: Yuan, fuTouName: String): JuResult {
        val pivot = mostRecentPivot(dt)
        val termSeq = termSequenceFrom(pivot.first)            // 名义节气序列（含下一个冬至/夏至）
        val pivotUpperFuTou = nearestUpperFuTou(pivotSecondDate(pivot)) // 该冬至/夏至的上元符头

        var fuTou = pivotUpperFuTou
        val dtFuTou = fuTouDateOf(dt.toLocalDate())
        var termPos = 0
        var inTerm = 0
        var isLeap = false

        if (!fuTou.isAfter(dtFuTou)) {
            while (fuTou < dtFuTou) {
                fuTou = fuTou.plusDays(5)
                inTerm++
                if (inTerm == 3) {
                    val cur = termSeq[termPos]
                    if (cur == 20 || cur == 8) {
                        val nextPivotTime = nextPivotTimeAfter(dt, cur)
                        if (nextPivotTime != null &&
                            java.time.Duration.between(fuTou.atStartOfDay(), nextPivotTime).toDays() >= 9
                        ) {
                            inTerm = 0
                            isLeap = true
                            continue
                        }
                    }
                    termPos++
                    inTerm = 0
                    isLeap = false
                }
            }
        } else {
            // dt 在冬至/夏至上元符头之前（接气）：仍属该节气上元
            termPos = 0
            isLeap = false
        }

        val nominalTermIdx = termSeq[termPos]
        val dun = if (isYang(nominalTermIdx)) Dun.YANG else Dun.YIN
        val ju = juTable.getValue(nominalTermIdx)[yuan.ordinal]
        return JuResult(dun, ju, termName(nominalTermIdx), nominalTermIdx, yuan, JuMethod.CHAOJIE, fuTouName, isLeap)
    }

    /** 最近一个冬至(21)或夏至(9)（交节时刻 ≤ dt） */
    private fun mostRecentPivot(dt: LocalDateTime): Pair<Int, LocalDateTime> {
        val candidates = ((dt.year - 1)..(dt.year + 1)).flatMap { y ->
            val t = SolarTerm.getYearSolarTerms(y)
            listOf(21 to t[21], 9 to t[9])
        }.filter { !it.second.isAfter(dt) }
        return candidates.maxBy { it.second }
    }

    private fun pivotSecondDate(pivot: Pair<Int, LocalDateTime>): LocalDate = pivot.second.toLocalDate()

    /** 自某节气起的名义节气序列（13 个，到下一个冬至/夏至） */
    private fun termSequenceFrom(startIdx: Int): List<Int> {
        val seq = mutableListOf<Int>()
        var idx = startIdx
        repeat(13) {
            seq.add(idx)
            idx = (idx + 1) % 24
        }
        return seq
    }

    /** [cur] 为大雪(20)时返回随后的冬至；为芒种(8)时返回随后的夏至（> dt） */
    private fun nextPivotTimeAfter(dt: LocalDateTime, cur: Int): LocalDateTime? {
        val targetIdx = if (cur == 20) 21 else 9
        val candidates = ((dt.year)..(dt.year + 1)).map { y ->
            SolarTerm.getYearSolarTerms(y)[targetIdx]
        }.filter { it.isAfter(dt) }
        return candidates.minByOrNull { it }
    }
}
