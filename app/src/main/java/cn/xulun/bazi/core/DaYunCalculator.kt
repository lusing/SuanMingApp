package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DaYun
import cn.xulun.bazi.model.DaYunInfo
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.LiuNian
import cn.xulun.bazi.model.Pillar
import java.time.Duration
import kotlin.math.floor
import kotlin.math.round

/** 大运计算器 */
object DaYunCalculator {

    /** 默认排 8 步大运（覆盖约 80 年） */
    const val DEFAULT_YUN_COUNT = 8

    /**
     * 排大运。
     * - 排向：年干阳 + 男 / 年干阴 + 女 → 顺行；否则逆行
     * - 起运数：顺行数到下一个「节」，逆行数到上一个「节」，相距天数 ÷ 3 为起运年数（3 天折 1 年）
     * - 大运干支：从月柱起，顺行向后 / 逆行向前，每步一柱管十年
     */
    fun compute(bazi: BaziResult, gender: Gender, yunCount: Int = DEFAULT_YUN_COUNT): DaYunInfo {
        val birth = bazi.birthDateTime
        val yangYear = bazi.yearPillar.stem.yang
        val forward = (yangYear && gender == Gender.MALE) || (!yangYear && gender == Gender.FEMALE)

        // 前后两年的 12 个「节」的交节时刻，按时间递增
        val jieMoments: List<java.time.LocalDateTime> = (
            SolarTerm.majorTermIndexes.map { SolarTerm.getSolarTerm(birth.year - 1, it) } +
                SolarTerm.majorTermIndexes.map { SolarTerm.getSolarTerm(birth.year, it) }
            ).sorted()

        // 顺行取下一个节，逆行取上一个节
        val jieMoment = if (forward) {
            jieMoments.first { it.isAfter(birth) }
        } else {
            jieMoments.last { !it.isAfter(birth) }
        }

        val diffSeconds = if (forward) {
            Duration.between(birth, jieMoment).seconds
        } else {
            Duration.between(jieMoment, birth).seconds
        }
        val diffDays = diffSeconds.toDouble() / 86400.0

        // 3 天折 1 年 → 起运年龄（年月日表示）
        val yunYears = diffDays / 3
        val y = floor(yunYears).toInt()
        val monthsFloat = (yunYears - y) * 12
        val m = floor(monthsFloat).toInt()
        val d = round((monthsFloat - m) * 30).toInt()

        val startDate = birth.plusYears(y.toLong()).plusMonths(m.toLong()).plusDays(d.toLong())
        val startYear = startDate.year

        // 从月柱顺/逆推大运干支
        val monthCycle = bazi.monthPillar.cycleIndex
        val yuns = (1..yunCount).map { step ->
            buildDaYun(
                monthCycle = monthCycle,
                step = step,
                forward = forward,
                startYear = startYear + (step - 1) * 10,
                birthYear = birth.year,
            )
        }

        return DaYunInfo(
            forward = forward,
            qiYunText = "出生后 $y 年 $m 个月 $d 天起运",
            startDate = startDate,
            yuns = yuns,
        )
    }

    private fun buildDaYun(monthCycle: Int, step: Int, forward: Boolean, startYear: Int, birthYear: Int): DaYun {
        val cycle = monthCycle + if (forward) step else -step
        return DaYun(
            pillar = Pillar.fromCycleIndex(cycle),
            startAge = startYear - birthYear + 1, // 虚岁
            startYear = startYear,
            liuNian = (startYear until startYear + 10).map { year ->
                LiuNian(year = year, pillar = Pillar.fromCycleIndex(year - 4))
            },
        )
    }
}
