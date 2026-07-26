package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.GeJuResult
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
}
