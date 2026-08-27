package cn.xulun.bazi

import cn.xulun.bazi.core.BaziCalculator
import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 教材八字反推：在 1700-2050 真实历法中搜索四柱完全一致的出生时刻，
 * 经 [BaziCalculator.calculate] 正规生成（含节气换月柱），
 * 这样 BaziResult 带真实 birthDateTime，后续可叠加分析大运流年。
 * 书上给出的大运流年只用于定位验证，不参与构造。
 *
 * 优化：以 1985-05-20=己未（cycleIndex 55，锚点测试核定）为锚，
 * 先算术筛日柱（每 60 天一遇），命中才调排盘校验年/月柱。
 */
fun chartOf(
    yStem: HeavenlyStem, yBranch: EarthlyBranch,
    mStem: HeavenlyStem, mBranch: EarthlyBranch,
    dStem: HeavenlyStem, dBranch: EarthlyBranch,
    hStem: HeavenlyStem, hBranch: EarthlyBranch,
    fromYear: Int = 1700, toYear: Int = 2050,
): BaziResult {
    val targetDayIdx = (0..59).first { i ->
        HeavenlyStem.fromIndex(i) == dStem && EarthlyBranch.fromIndex(i) == dBranch
    }
    val anchor = LocalDate.of(1985, 5, 20) // 己未
    var date = LocalDate.of(fromYear, 1, 1)
    val end = LocalDate.of(toYear + 1, 1, 1)
    while (date.isBefore(end)) {
        val idx = Math.floorMod(55 + ChronoUnit.DAYS.between(anchor, date).toInt(), 60)
        if (idx == targetDayIdx) {
            val noon = BaziCalculator.calculate(LocalDateTime.of(date, LocalTime.NOON))
            if (noon.yearPillar.stem == yStem && noon.yearPillar.branch == yBranch &&
                noon.monthPillar.stem == mStem && noon.monthPillar.branch == mBranch &&
                noon.dayPillar.stem == dStem && noon.dayPillar.branch == dBranch
            ) {
                for (hour in 0..23) {
                    val b = BaziCalculator.calculate(LocalDateTime.of(date, LocalTime.of(hour, 0)))
                    // 交节当日（如1718-02-04立春在07-12时之间）：正午已过节气而清晨未过——年月柱随时刻而变，须复查
                    if (b.yearPillar.stem == yStem && b.yearPillar.branch == yBranch &&
                        b.monthPillar.stem == mStem && b.monthPillar.branch == mBranch &&
                        b.dayPillar.stem == dStem && b.dayPillar.branch == dBranch &&
                        b.hourPillar.stem == hStem && b.hourPillar.branch == hBranch) return b
                }
            }
        }
        date = date.plusDays(1)
    }
    error("$fromYear-$toYear 未找到四柱：$yStem$yBranch $mStem$mBranch $dStem$dBranch $hStem$hBranch")
}
