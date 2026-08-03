package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.CongGeKind
import cn.xulun.bazi.model.CongGeResult
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.GeJuResult
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.TenGod
import cn.xulun.bazi.model.branchHiddenStems
import cn.xulun.bazi.model.tenGodOfBranch
import cn.xulun.bazi.model.tenGodOfStem

/**
 * 格局判定器 — 子平「月令透干法」。
 *
 * 取格顺序：
 * 1. 月令本气为日主比肩 → 建禄格；为劫财 → 阳刃格（阳干）/ 月劫格（阴干）
 * 2. 月支藏干（本气→中气→余气）透出年/月/时干者，以其十神定格（比劫不取格）
 * 3. 藏干皆不透 → 以月支本气定格
 *
 * 注：从格、化格、专旺格等特殊格局需综合旺衰与气势判断，本判定器只覆盖正格。
 */
object GeJuCalculator {

    fun determine(bazi: BaziResult): GeJuResult {
        val day = bazi.dayPillar.stem
        val monthBranch = bazi.monthPillar.branch
        val hidden = branchHiddenStems.getValue(monthBranch)
        val mainGod = tenGodOfStem(day, hidden.first())
        val strength = assessStrength(bazi)

        // 1. 月令比劫：建禄 / 阳刃 / 月劫
        if (mainGod == TenGod.BI_JIAN) {
            return GeJuResult(
                name = "建禄格",
                tenGod = mainGod,
                touGanStem = null,
                basis = "月令${monthBranch.chinese}为日主${day.chinese}之禄地（本气${hidden.first().chinese}为比肩）",
                strength = strength,
            )
        }
        if (mainGod == TenGod.JIE_CAI) {
            val name = if (day.yang) "阳刃格" else "月劫格"
            return GeJuResult(
                name = name,
                tenGod = mainGod,
                touGanStem = null,
                basis = "月令${monthBranch.chinese}为日主${day.chinese}之劫财" +
                    "（本气${hidden.first().chinese}），${if (day.yang) "阳干取阳刃" else "阴干取月劫"}",
                strength = strength,
            )
        }

        // 2. 月令藏干透干取格（本气→中气→余气，比劫不取格）
        val candidates = listOf(
            bazi.yearPillar.stem to "年干",
            bazi.monthPillar.stem to "月干",
            bazi.hourPillar.stem to "时干",
        )
        for (h in hidden) {
            val god = tenGodOfStem(day, h)
            if (god == TenGod.BI_JIAN || god == TenGod.JIE_CAI) continue
            val touAt = candidates.filter { it.first == h }.joinToString("、") { it.second }
            if (touAt.isNotEmpty()) {
                return GeJuResult(
                    name = "${god.chinese}格",
                    tenGod = god,
                    touGanStem = h,
                    basis = "月令${monthBranch.chinese}所藏${h.chinese}（${god.chinese}）透于$touAt",
                    strength = strength,
                )
            }
        }

        // 3. 皆不透 → 以月支本气定格
        return GeJuResult(
            name = "${mainGod.chinese}格",
            tenGod = mainGod,
            touGanStem = null,
            basis = "月令${monthBranch.chinese}藏干俱不透，以本气${hidden.first().chinese}（${mainGod.chinese}）定格",
            strength = strength,
        )
    }

    /**
     * 日主强弱打分法（参考）。
     * 计分：比劫/印星（扶身）记 +w，财官食伤（耗身）记 -w。
     * 权重：月令本气 ×3，其余七字 ×1（日干自身恒 +1）。
     * 总分 ≥2 身强，≤-2 身弱，其间为中和。
     */
    fun assessStrength(bazi: BaziResult): DayStrength {
        val day = bazi.dayPillar.stem
        var support = 1 // 日干自身
        var drain = 0

        fun account(god: TenGod, weight: Int) {
            if (god == TenGod.BI_JIAN || god == TenGod.JIE_CAI ||
                god == TenGod.PIAN_YIN || god == TenGod.ZHENG_YIN
            ) {
                support += weight
            } else {
                drain += weight
            }
        }

        // 月令本气权重 ×3
        account(tenGodOfBranch(day, bazi.monthPillar.branch), 3)

        // 其余柱：天干与地支本气各 ×1（日柱天干已计入）
        // 天干：年柱(0)、月柱(1)、时柱(3)
        for (i in listOf(0, 1, 3)) {
            account(tenGodOfStem(day, bazi.pillars[i].stem), 1)
        }
        // 地支本气：年柱(0)、日柱(2)、时柱(3)
        for (i in listOf(0, 2, 3)) {
            account(tenGodOfBranch(day, bazi.pillars[i].branch), 1)
        }

        val score = support - drain
        return when {
            score >= 2 -> DayStrength.STRONG
            score <= -2 -> DayStrength.WEAK
            else -> DayStrength.BALANCED
        }
    }

    // ── 十二长生根位（《子平真诠》ch3/6/28 根论）—— 逐字移植自 xuanzong geju.js ──
    // 长生（阳顺阴死互换：甲亥 乙午 丙寅 丁酉 戊寅 己酉 庚巳 辛子 壬申 癸卯）
    private val changSheng = arrayOf(
        EarthlyBranch.HAI, EarthlyBranch.WU, EarthlyBranch.YIN, EarthlyBranch.YOU,
        EarthlyBranch.YIN, EarthlyBranch.YOU, EarthlyBranch.SI, EarthlyBranch.ZI,
        EarthlyBranch.SHEN, EarthlyBranch.MAO,
    )
    // 临官=禄（甲寅 乙卯 丙戊巳 丁己午 庚申 辛酉 壬亥 癸子）
    private val lu = arrayOf(
        EarthlyBranch.YIN, EarthlyBranch.MAO, EarthlyBranch.SI, EarthlyBranch.WU,
        EarthlyBranch.SI, EarthlyBranch.WU, EarthlyBranch.SHEN, EarthlyBranch.YOU,
        EarthlyBranch.HAI, EarthlyBranch.ZI,
    )
    // 帝旺=刃（甲卯 乙寅 丙午 丁未 戊午 己未 庚酉 辛申 壬子 癸亥）
    private val diWang = arrayOf(
        EarthlyBranch.MAO, EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.WEI,
        EarthlyBranch.WU, EarthlyBranch.WEI, EarthlyBranch.YOU, EarthlyBranch.SHEN,
        EarthlyBranch.ZI, EarthlyBranch.HAI,
    )

    /** 支对日主的「根」权重（ch6 根之轻重）：长生禄刃=重根，阴长生降档/库余气=轻根，无根=0。 */
    private fun rootWeightOf(day: HeavenlyStem, branch: EarthlyBranch): Double {
        val dayEl = day.element
        if (branch == changSheng[day.ordinal]) return if (day.yang) 1.5 else 1.0
        if (branch == lu[day.ordinal]) return 1.5
        if (branch == diWang[day.ordinal]) return 1.5
        // 库根/余气根：中气余气藏干含日主同五行
        val rest = branchHiddenStems.getValue(branch).drop(1)
        if (rest.any { it.element == dayEl }) return 1.0
        return 0.0
    }

    /**
     * 日主强弱打分（含根论加成）—— 逐字移植自 xuanzong geju.js strengthScore。
     * base（月令本气×3 + 透干×1 + 年日时本气×1，日干恒+1；比劫/印扶身，财官食伤耗身）
     * ＋ 四支根权重加成（通根胜比劫）。供 [detectCongGe] 使用；与 [assessStrength]
     * 的整数档位（≥2/≤-2）互补，本分数版用于从格阈值（≥6/≤-5）。
     */
    fun strengthScore(bazi: BaziResult): Double {
        val day = bazi.dayPillar.stem
        var support = 1.0
        var drain = 0.0
        fun account(god: TenGod, weight: Double) {
            if (god == TenGod.BI_JIAN || god == TenGod.JIE_CAI ||
                god == TenGod.PIAN_YIN || god == TenGod.ZHENG_YIN
            ) {
                support += weight
            } else {
                drain += weight
            }
        }
        account(tenGodOfBranch(day, bazi.monthPillar.branch), 3.0)
        for (i in listOf(0, 1, 3)) account(tenGodOfStem(day, bazi.pillars[i].stem), 1.0)
        for (i in listOf(0, 2, 3)) account(tenGodOfBranch(day, bazi.pillars[i].branch), 1.0)
        for (i in 0..3) support += rootWeightOf(day, bazi.pillars[i].branch)
        return support - drain
    }

    /**
     * 从格检测（粗判）—— 逐字移植自 xuanzong geju.js detectCongGe。
     * - 从强/专旺：strengthScore≥6 且四支皆同我(比劫)或生我(印) → [CongGeKind.CONG_STRONG]
     * - 从弱：strengthScore≤-5 且无本气根(无支本气为同我之行)且天干不透比劫 → [CongGeKind.CONG_WEAK]
     * - 否则 null。只影响「运喜」取向，不改强弱档位。
     */
    fun detectCongGe(bazi: BaziResult): CongGeResult? {
        val day = bazi.dayPillar.stem
        val dayEl = day.element
        val score = strengthScore(bazi)
        val branchEls = bazi.pillars.map { it.branch.element }
        fun selfOrPrint(el: Element) = el == dayEl || el.generates == dayEl // 比劫(同我)或印(生我)
        if (score >= 6 && branchEls.all { selfOrPrint(it) }) {
            return CongGeResult(CongGeKind.CONG_STRONG, "极旺（强弱分${score}）且四支皆比劫/印，顺旺势从强")
        }
        val hasBiJianStem = listOf(0, 1, 3).any { i ->
            val g = tenGodOfStem(day, bazi.pillars[i].stem)
            g == TenGod.BI_JIAN || g == TenGod.JIE_CAI
        }
        if (score <= -5 && branchEls.none { it == dayEl } && !hasBiJianStem) {
            return CongGeResult(CongGeKind.CONG_WEAK, "极弱（强弱分${score}）且无本气根、不透比劫，顺旺神从弱")
        }
        return null
    }
}
