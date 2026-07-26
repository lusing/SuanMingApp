package cn.xulun.bazi.core

import java.time.LocalDateTime
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.sin

/**
 * 节气计算器。
 *
 * 数据来源（两级）：
 * 1. 查表（1899-2100）：[kSolarTermTable]，sxtwl 寿星天文历精密星历，精确到分钟。
 * 2. 天文算法（表外年份回退）：Jean Meeus《Astronomical Algorithms》第 25 章太阳视黄经公式，精度约 ±15 分钟。
 *
 * 约定：对外返回的 [LocalDateTime] 均为**北京时间（UTC+8）墙钟时间**；
 * 内部儒略日计算使用世界时（UT），在 JD 转换处统一处理 +8h 偏移（用固定 [beijingOffset]，绝不使用 Asia/Shanghai 历史时区）。
 */
object SolarTerm {

    /** 太阳视黄经计算常量（J2000.0 历元） */
    private const val J2000 = 2451545.0

    /** 北京时间相对世界时的偏移（小时） */
    private const val BEIJING_OFFSET_HOURS = 8.0

    /** 24 节气名称（从立春开始，每 15° 一个） */
    val termNames: List<String> = listOf(
        "立春", "雨水", "惊蛰", "春分", "清明", "谷雨",
        "立夏", "小满", "芒种", "夏至", "小暑", "大暑",
        "立秋", "处暑", "白露", "秋分", "寒露", "霜降",
        "立冬", "小雪", "大雪", "冬至", "小寒", "大寒",
    )

    /** 12 个「节」（八字换月分界节气）在 termNames 中的序号 */
    val majorTermIndexes: List<Int> = listOf(0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22)

    /** 缓存：年份 → 24 个节气的精确交节时刻（北京时间，按时间顺序）。
     * 列表从该年立春开始：index 0 = 立春(约2月4日)，index 22 = 小寒(次年1月)，index 23 = 大寒(次年1月)。 */
    private val cache = HashMap<Int, List<LocalDateTime>>()

    /** 获取以 [year] 年立春为首的 24 节气精确时刻（北京时间，按时间顺序） */
    fun getYearSolarTerms(year: Int): List<LocalDateTime> =
        cache.getOrPut(year) { loadYearSolarTerms(year) }

    private fun loadYearSolarTerms(year: Int): List<LocalDateTime> {
        val row = kSolarTermTable[year]
        return if (row != null) decodeTableRow(year, row) else computeWithMeeus(year)
    }

    /** 解包查表数据（MMDDHHMM → LocalDateTime）。小寒/大寒(index 22/23)落在次年 1 月。 */
    private fun decodeTableRow(year: Int, row: IntArray): List<LocalDateTime> =
        (0 until 24).map { i ->
            val v = row[i]
            LocalDateTime.of(
                if (i >= 22) year + 1 else year,
                v / 1_000_000,
                v / 10_000 % 100,
                v / 100 % 100,
                v % 100,
            )
        }

    /** 获取某节气序号的精确交节时刻（北京时间）。termIndex: 0=立春 ... 23=大寒 */
    fun getSolarTerm(year: Int, termIndex: Int): LocalDateTime = getYearSolarTerms(year)[termIndex]

    /** 用 Meeus 天文算法计算指定年份全部 24 节气的精确时刻（表外年份回退） */
    fun computeWithMeeus(year: Int): List<LocalDateTime> =
        (0 until 24).map { i ->
            val longitude = (315.0 + i * 15.0) % 360.0
            val jd = findSolarTermJD(year, i, longitude)
            jdToBeijingDateTime(jd)
        }

    /** 牛顿迭代求太阳视黄经达到 [targetLongitude] 时的儒略日（UT） */
    private fun findSolarTermJD(year: Int, termIndex: Int, targetLongitude: Double): Double {
        // 立春约在 2 月 4 日（年内约第 34 天），之后每节气平均间隔 365.2422/24 天
        val approxDayOfYear = 34.0 + termIndex * (365.2422 / 24)
        var jd = beijingDateTimeToJD(LocalDateTime.of(year, 1, 1, 0, 0)) + approxDayOfYear
        var iter = 0
        while (iter < 50) {
            iter++
            val currentLon = sunApparentLongitude(jd)
            var diff = currentLon - targetLongitude
            if (diff > 180) diff -= 360.0
            if (diff < -180) diff += 360.0
            // 收敛判据：0.0001° ≈ 8.8 秒
            if (kotlin.math.abs(diff) < 0.0001) break
            // 太阳每日约运行 0.9856°，用黄经差反推时间修正量，限制 ±10 天防跳节气
            val adjustment = (-diff / 0.9856).coerceIn(-10.0, 10.0)
            jd += adjustment
        }
        return jd
    }

    /** 计算太阳视黄经（度，0~360°），基于 Meeus 算法第 25 章，精度约 0.01° */
    private fun sunApparentLongitude(jd: Double): Double {
        val t = (jd - J2000) / 36525.0

        // 太阳几何平黄经（度）
        val l0 = normalize360(280.46646 + 36000.76983 * t + 0.0003032 * t * t)

        // 太阳平近点角（度）
        val m = normalize360(357.52911 + 35999.05029 * t - 0.0001537 * t * t)
        val mRad = toRadians(m)

        // 中心差（度）
        val c = (1.914602 - 0.004817 * t - 0.0000142 * t * t) * sin(mRad) +
            (0.019993 - 0.000101 * t) * sin(2 * mRad) +
            0.000289 * sin(3 * mRad)

        // 太阳真黄经
        var l = l0 + c

        // 视黄经 = 真黄经 + 黄经章动 - 光行差（Meeus 25.9）
        val omega = toRadians(125.04 - 1934.136 * t)
        l += -0.00478 * sin(omega) - 0.00569
        return normalize360(l)
    }

    /** 北京时间 LocalDateTime → 儒略日（UT） */
    private fun beijingDateTimeToJD(dt: LocalDateTime): Double {
        var year = dt.year
        var month = dt.monthValue
        if (month <= 2) {
            year -= 1
            month += 12
        }
        val a = year / 100
        val b = 2 - a + a / 4

        // 转为世界时的小数日（减去 +8h）
        val hourUT = dt.hour + dt.minute / 60.0 + dt.second / 3600.0 - BEIJING_OFFSET_HOURS
        val dayWithFraction = dt.dayOfMonth + hourUT / 24.0

        return floor(365.25 * (year + 4716)) +
            floor(30.6001 * (month + 1)) +
            dayWithFraction + b - 1524.5
    }

    /** 儒略日（UT）→ 北京时间 LocalDateTime */
    private fun jdToBeijingDateTime(jd: Double): LocalDateTime {
        // 先平移到北京时区再做历法分解
        val jdBeijing = jd + BEIJING_OFFSET_HOURS / 24.0

        val jdInt = jdBeijing + 0.5
        val z = floor(jdInt).toInt()
        val f = jdInt - z

        var a = z
        if (z >= 2299161) {
            val alpha = floor((z - 1867216.25) / 36524.25).toInt()
            a = z + 1 + alpha - alpha / 4
        }

        val b = a + 1524
        val c = floor((b - 122.1) / 365.25).toInt()
        val d = floor(365.25 * c).toInt()
        val e = floor((b - d) / 30.6001).toInt()

        val day = b - d - floor(30.6001 * e) + f
        var month = e - 1
        if (month > 12) month -= 12
        // 1/2 月属于上一历法年的年末，年份要 +1 回当前公历年
        var year = c - 4716
        if (month <= 2) year += 1

        val intDay = floor(day).toInt()
        val fracDay = day - intDay
        val intHour = floor(fracDay * 24).toInt()
        val intMinute = floor((fracDay * 24 - intHour) * 60).toInt()
        val intSecond = round(((fracDay * 24 - intHour) * 60 - intMinute) * 60).toInt()

        // 用 plusSeconds 归一化：round 可能产生 60 秒，java.time 的 of() 会抛异常，
        // 而 Dart 的 DateTime 构造器会自动进位（plusSeconds 与之一致）。
        return LocalDateTime.of(year, month, intDay, intHour, intMinute, 0)
            .plusSeconds(intSecond.toLong())
    }

    private fun normalize360(angle: Double): Double {
        var a = angle % 360
        if (a < 0) a += 360.0
        return a
    }

    private fun toRadians(degrees: Double): Double = degrees * PI / 180.0
}
