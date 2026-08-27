package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.MingGongResult
import cn.xulun.bazi.model.Pillar
import java.time.LocalDateTime

/**
 * 命宫 / 胎元 / 小限 / 小运计算器。理论见 `.shengke-reference.md` 第廿二~廿四节。
 *
 * - 命宫：26−（月数+时数），余数>12 再减 12；支数以寅=1…丑=12 计；
 *   **生日过本月中气，月数加 1（按下月计）**（中气=节气表奇数索引，[SolarTerm] 精确到分）；
 *   天干以年上起月法（五虎遁）推。命宫可视为第五柱，其干支五行参与命局喜忌。
 * - 胎元：月干顺推一位 + 月支顺推三位。
 * - 小限：命宫为出发点，一年一换，60 甲子序逐年 −1（交接随太岁，以立春为准）。
 * - 小运：以时柱为出发点，一年一换（满一周岁交脱），**一岁即行首步**（非时柱本身）；
 *   阳男阴女顺排，阴男阳女逆排（同大运顺逆）。
 */
object MingGongCalculator {

    /** 十二宫星曜（命宫支→星名+断语）。 */
    private val STARS = mapOf(
        EarthlyBranch.ZI to ("天贵星" to "志气宏大，富裕高贵"),
        EarthlyBranch.CHOU to ("天厄星" to "离祖劳心，先难后吉"),
        EarthlyBranch.YIN to ("天权星" to "聪明有为，中年有权"),
        EarthlyBranch.MAO to ("天赦星" to "仗义疏财，清高傲物"),
        EarthlyBranch.CHEN to ("天如星" to "事多反复，足智多谋"),
        EarthlyBranch.SI to ("天文星" to "文途光明，女获良缘"),
        EarthlyBranch.WU to ("天福星" to "荣华富贵，清闲安乐"),
        EarthlyBranch.WEI to ("天驿星" to "劳碌奔波，离祖创业"),
        EarthlyBranch.SHEN to ("天孤星" to "孤独寂寞，女命妨夫"),
        EarthlyBranch.YOU to ("天秘星" to "性情刚直，易招是非"),
        EarthlyBranch.XU to ("天艺星" to "多才多艺，平易随和"),
        EarthlyBranch.HAI to ("天寿星" to "心慈性灵，助人为乐"),
    )

    fun starNameOf(branch: EarthlyBranch): String = STARS[branch]!!.first

    /** 支数：寅=1、卯=2 … 子=11、丑=12。 */
    private fun branchNum(b: EarthlyBranch): Int = (b.ordinal - 2 + 12) % 12 + 1

    /** 支数→支。 */
    private fun branchOfNum(n: Int): EarthlyBranch = EarthlyBranch.fromIndex(n + 1)

    /**
     * 命宫推算（纯函数）。
     * @param pastZhongQi 生日是否已过本月中气（过则月数+1，按下月计）
     */
    fun mingGongOf(monthBranch: EarthlyBranch, hourBranch: EarthlyBranch, pastZhongQi: Boolean, yearStem: HeavenlyStem): Pillar {
        var monthNum = branchNum(monthBranch)
        if (pastZhongQi) monthNum = monthNum % 12 + 1
        var n = 26 - (monthNum + branchNum(hourBranch))
        if (n > 12) n -= 12
        val branch = branchOfNum(n)
        // 年上起月法（五虎遁）：寅月干 = (年干%5)*2+2；命宫干自寅月起顺数至命宫支
        val yinStem = (yearStem.ordinal % 5) * 2 + 2
        val stem = HeavenlyStem.fromIndex(yinStem + branchNum(branch) - 1)
        return Pillar(stem, branch)
    }

    /** 小限：命宫起逐年逆行（yearsAfter = 流年 − 出生年）。 */
    fun xiaoXianOf(mingGong: Pillar, yearsAfter: Int): Pillar =
        Pillar.fromCycleIndex(mingGong.cycleIndex - yearsAfter)

    /** 小运：时柱起一年一换，一岁即行首步（男顺 1 岁=时柱+1，女逆 1 岁=时柱−1）；age 为虚岁。 */
    fun xiaoYunOf(hourPillar: Pillar, forward: Boolean, age: Int): Pillar =
        Pillar.fromCycleIndex(hourPillar.cycleIndex + (if (forward) 1 else -1) * age)

    /** 出生之「立春年」（用于虚岁与小限的年差）。 */
    fun birthYearForSui(birth: LocalDateTime): Int =
        if (!birth.isBefore(SolarTerm.getSolarTerm(birth.year, 0))) birth.year else birth.year - 1

    /** 生日是否已过本月中气（中气=节气表奇数索引：寅月雨水…丑月大寒）。 */
    private fun pastZhongQi(bazi: BaziResult): Boolean {
        val birth = bazi.birthDateTime
        val m = bazi.monthPillar.branch
        val monthIdx = (m.ordinal - 2 + 12) % 12 // 寅=0…丑=11
        // 丑月（小寒/大寒）落在上一节气年的列表里（index 22/23 = 次年1月）
        val termYear = if (m == EarthlyBranch.CHOU) birth.year - 1 else birth.year
        val zhongQi = SolarTerm.getSolarTerm(termYear, 2 * monthIdx + 1)
        return !birth.isBefore(zhongQi)
    }

    /**
     * 全量分析。
     * @param flowYear 小限/小运所示流年（null = 出生年，即 1 岁）
     */
    fun analyze(bazi: BaziResult, gender: Gender, flowYear: Int? = null): MingGongResult {
        val past = pastZhongQi(bazi)
        val mingGong = mingGongOf(bazi.monthPillar.branch, bazi.hourPillar.branch, past, bazi.yearPillar.stem)
        val (star, text) = STARS[mingGong.branch]!!

        // 胎元：月干+1、月支+3
        val taiYuan = Pillar(
            HeavenlyStem.fromIndex(bazi.monthPillar.stem.ordinal + 1),
            EarthlyBranch.fromIndex(bazi.monthPillar.branch.ordinal + 3),
        )

        val birthYear = birthYearForSui(bazi.birthDateTime)
        val year = flowYear ?: birthYear
        val xiaoXian = xiaoXianOf(mingGong, year - birthYear)
        val forward = (bazi.yearPillar.stem.yang && gender == Gender.MALE) ||
            (!bazi.yearPillar.stem.yang && gender == Gender.FEMALE)
        val age = year - birthYear + 1
        val xiaoYun = xiaoYunOf(bazi.hourPillar, forward, age)

        val basis = "26−(${branchNum(bazi.monthPillar.branch)}${if (past) "（过中气+1）" else ""}+" +
            "${branchNum(bazi.hourPillar.branch)})，${bazi.yearPillar.stem.chinese}年五虎遁配干" +
            (if (past) "（生日过本月中气，月数按下月计）" else "")

        return MingGongResult(
            mingGong = mingGong,
            starName = star,
            starText = text,
            mingGongBasis = basis,
            taiYuan = taiYuan,
            xiaoXianYear = year,
            xiaoXian = xiaoXian,
            xiaoYunAge = age,
            xiaoYun = xiaoYun,
            xiaoYunForward = forward,
        )
    }
}
