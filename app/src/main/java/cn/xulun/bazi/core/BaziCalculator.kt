package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Pillar
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.lang.Math.floorMod

/** 生辰八字计算引擎 */
object BaziCalculator {

    /** 十二时辰对应的时间范围（按地支顺序 子丑寅卯辰巳午未申酉戌亥） */
    private val hourRanges: List<HourRange> = listOf(
        HourRange(EarthlyBranch.ZI, 23, 0, "子时 23:00-00:59"),
        HourRange(EarthlyBranch.CHOU, 1, 2, "丑时 01:00-02:59"),
        HourRange(EarthlyBranch.YIN, 3, 4, "寅时 03:00-04:59"),
        HourRange(EarthlyBranch.MAO, 5, 6, "卯时 05:00-06:59"),
        HourRange(EarthlyBranch.CHEN, 7, 8, "辰时 07:00-08:59"),
        HourRange(EarthlyBranch.SI, 9, 10, "巳时 09:00-10:59"),
        HourRange(EarthlyBranch.WU, 11, 12, "午时 11:00-12:59"),
        HourRange(EarthlyBranch.WEI, 13, 14, "未时 13:00-14:59"),
        HourRange(EarthlyBranch.SHEN, 15, 16, "申时 15:00-16:59"),
        HourRange(EarthlyBranch.YOU, 17, 18, "酉时 17:00-18:59"),
        HourRange(EarthlyBranch.XU, 19, 20, "戌时 19:00-20:59"),
        HourRange(EarthlyBranch.HAI, 21, 22, "亥时 21:00-22:59"),
    )

    /** 五虎遁：年干分组 → 寅月起始天干（丙戊庚壬甲） */
    private val yearStemToMonthStemStart = intArrayOf(2, 4, 6, 8, 0)

    /** 五鼠遁：日干分组 → 子时起始天干（甲丙戊庚壬） */
    private val dayStemToHourStemStart = intArrayOf(0, 2, 4, 6, 8)

    /** 12 节(立春/惊蛰/...)在节气列表中的序号 → 对应月支索引 */
    private val termToBranch: List<Pair<Int, Int>> = listOf(
        0 to 2, 2 to 3, 4 to 4, 6 to 5, 8 to 6, 10 to 7,
        12 to 8, 14 to 9, 16 to 10, 18 to 11, 20 to 0, 22 to 1,
    )

    /** 时辰标签列表（用于 UI） */
    fun getHourLabels(): List<String> = hourRanges.map { it.label }

    /** 根据小时数获取对应的时辰地支 */
    fun getHourBranch(hour: Int): EarthlyBranch =
        hourRanges.firstOrNull { it.matches(hour) }?.branch ?: hourRanges[0].branch

    /**
     * 计算生辰八字。[birthDate] 为北京时间（UTC+8）的出生时刻。
     * 年柱以立春精确交节时刻为界，月柱以 12 个「节」的精确交节时刻为界。
     */
    fun calculate(birthDate: LocalDateTime): BaziResult {
        val yearPillar = calculateYearPillar(birthDate)
        val monthPillar = calculateMonthPillar(birthDate, yearPillar.stem)
        val dayPillar = calculateDayPillar(birthDate)
        // 夜子时（23:00-24:00）：日柱不进，时干按次日干起子时——壬日夜子=壬子（水专旺篇两例）
        val hourPillar = if (birthDate.hour == 23) {
            val nextStem = HeavenlyStem.fromIndex(floorMod(dayPillar.stem.ordinal + 1, 10))
            calculateHourPillar(0, nextStem)
        } else {
            calculateHourPillar(birthDate.hour, dayPillar.stem)
        }
        return BaziResult(
            yearPillar = yearPillar,
            monthPillar = monthPillar,
            dayPillar = dayPillar,
            hourPillar = hourPillar,
            birthDateTime = birthDate,
        )
    }

    /** 计算年柱（以立春精确交节时刻为界，立春前用前一年干支） */
    private fun calculateYearPillar(date: LocalDateTime): Pillar {
        val lichunThisYear = SolarTerm.getSolarTerm(date.year, 0)
        val effectiveYear = if (date.isBefore(lichunThisYear)) date.year - 1 else date.year
        val stemIndex = floorMod(effectiveYear - 4, 10)
        val branchIndex = floorMod(effectiveYear - 4, 12)
        return Pillar(
            stem = HeavenlyStem.fromIndex(stemIndex),
            branch = EarthlyBranch.fromIndex(branchIndex),
        )
    }

    /** 计算月柱 */
    private fun calculateMonthPillar(date: LocalDateTime, yearStem: HeavenlyStem): Pillar {
        val branchIndex = getMonthBranchIndex(date)
        val branch = EarthlyBranch.fromIndex(branchIndex)

        // 五虎遁：寅月(月支 index=2)起始天干
        val group = yearStem.ordinal % 5
        val yinMonthStemStart = yearStemToMonthStemStart[group]
        val monthOffset = floorMod(branchIndex - 2, 12)
        val stemIndex = floorMod(yinMonthStemStart + monthOffset, 10)

        return Pillar(HeavenlyStem.fromIndex(stemIndex), branch)
    }

    /**
     * 根据出生时刻确定八字月份（地支索引，寅=2 为八字第一个月）。
     * 以 12 个「节」的精确交节时刻为界，取最后一个交节时刻 <= 出生时刻的边界。
     */
    private fun getMonthBranchIndex(date: LocalDateTime): Int {
        val prevYearTerms = SolarTerm.getYearSolarTerms(date.year - 1)
        val thisYearTerms = SolarTerm.getYearSolarTerms(date.year)

        val boundaries: List<Pair<LocalDateTime, Int>> = buildList {
            termToBranch.forEach { (termIndex, branchIndex) -> add(prevYearTerms[termIndex] to branchIndex) }
            termToBranch.forEach { (termIndex, branchIndex) -> add(thisYearTerms[termIndex] to branchIndex) }
        }

        var branchIndex = boundaries.first().second
        for ((moment, branch) in boundaries) {
            if (date.isBefore(moment)) break
            branchIndex = branch
        }
        return branchIndex
    }

    /** 计算日柱：基准日 1900-01-01 = 甲戌（六十甲子序号 10） */
    private fun calculateDayPillar(date: LocalDateTime): Pillar {
        val referenceDate = LocalDate.of(1900, 1, 1)
        val daysDiff = ChronoUnit.DAYS.between(referenceDate, LocalDate.of(date.year, date.monthValue, date.dayOfMonth))
        val cycleIndex = floorMod(floorMod(daysDiff.toInt(), 60) + 10, 60)
        val stemIndex = cycleIndex % 10
        val branchIndex = cycleIndex % 12
        return Pillar(HeavenlyStem.fromIndex(stemIndex), EarthlyBranch.fromIndex(branchIndex))
    }

    /** 计算时柱 */
    private fun calculateHourPillar(hour: Int, dayStem: HeavenlyStem): Pillar {
        val branch = getHourBranch(hour)
        val group = dayStem.ordinal % 5
        val ziHourStemStart = dayStemToHourStemStart[group]
        val stemIndex = floorMod(ziHourStemStart + branch.ordinal, 10)
        return Pillar(HeavenlyStem.fromIndex(stemIndex), branch)
    }

    /** 时辰范围 */
    private class HourRange(val branch: EarthlyBranch, val startHour: Int, val endHour: Int, val label: String) {
        fun matches(hour: Int): Boolean = if (startHour == 23) {
            hour == 23 || hour == 0
        } else {
            hour in startHour..endHour
        }
    }
}
