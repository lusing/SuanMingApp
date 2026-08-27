package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.GeJuResult
import cn.xulun.bazi.model.TenGod
import cn.xulun.bazi.model.branchHiddenStems
import cn.xulun.bazi.model.tenGodOfStem

/**
 * 八格定格法（本书取格规则）。理论见 `.shengke-reference.md` 第五一节。
 *
 * 与 [GeJuCalculator]（子平真诠：透干→会支→本气）**两派并行**——关键差异在并透与皆不透时的**力量比较**：
 * 1. 月令本气透干 → 以本气十神取格；
 * 2. 本气不透而支中其他藏干透出 → 以所透干取格；
 * 3. 两藏干并透 → **较二者力量强弱，以较强者取格**；
 * 4. 藏干皆不透 → **力量最强之藏干**取格；月支单藏一本气不透 → 直接以本气取格。
 *
 * 力力度量：该藏干五行之力（干 1 支本气 1 月令 +2，[BingYaoCalculator.forces]）＋ 三合/三会化神加成（+3，胜月令本气）。
 * 比劫藏干透出不取格（八格无比劫）；月支本气比劫 → 建禄格（禄）/阳刃格（刃，阴干作月劫）。
 */
object BaGeCalculator {

    fun determine(bazi: BaziResult): GeJuResult {
        val day = bazi.dayPillar.stem
        val monthBranch = bazi.monthPillar.branch
        val hidden = branchHiddenStems.getValue(monthBranch)
        val strength = GeJuCalculator.assessStrength(bazi)
        val mainGod = tenGodOfStem(day, hidden.first())

        // 1. 月令比劫：建禄 / 阳刃（月劫）
        if (mainGod == TenGod.BI_JIAN) {
            return GeJuResult(
                name = "建禄格", tenGod = mainGod, touGanStem = null,
                basis = "月令${monthBranch.chinese}为日主${day.chinese}之禄地（本气${hidden.first().chinese}比肩），建禄格不入八格",
                strength = strength,
            )
        }
        if (mainGod == TenGod.JIE_CAI) {
            val name = if (day.yang) "阳刃格" else "月劫格"
            return GeJuResult(
                name = name, tenGod = mainGod, touGanStem = null,
                basis = "月令${monthBranch.chinese}本气${hidden.first().chinese}为劫财，${if (day.yang) "阳干取阳刃格" else "阴干取月劫格"}，不入八格",
                strength = strength,
            )
        }

        val candidates = listOf(
            bazi.yearPillar.stem to "年干",
            bazi.monthPillar.stem to "月干",
            bazi.hourPillar.stem to "时干",
        )

        // 藏干之力：该行 forces + 合局化神加成
        val f = BingYaoCalculator.forces(bazi)
        val sanHe = DiZhiRelationCalculator.analyze(bazi).sanHeList
        val sanHui = HeHuaCalculator.analyze(bazi).branchSanHuiList
        fun forceOf(h: cn.xulun.bazi.model.HeavenlyStem): Double {
            var v = f[h.element]!!
            val heEls = sanHe.map { it.resultElement } + sanHui.map { it.resultElement }
            if (h.element in heEls) v += 3.0 // 三合/三会化神之力大增（胜月令本气之+2）
            return v
        }

        // 透出之非比劫藏干
        val tou = hidden.filter { h -> h !in listOf(bazi.dayPillar.stem) }
            .filter { tenGodOfStem(day, it) !in listOf(TenGod.BI_JIAN, TenGod.JIE_CAI) }
            .filter { h -> candidates.any { it.first == h } }

        val pick: cn.xulun.bazi.model.HeavenlyStem
        val basis: String
        when {
            // 法一：本气透（本气非比劫已在上方排除）
            hidden.first() in tou -> {
                pick = hidden.first()
                basis = "月令本气${pick.chinese}透干，以${tenGodOfStem(day, pick).chinese}取格"
            }
            // 法二/三：其他藏干透出——单透直取，并透较力
            tou.size == 1 -> {
                pick = tou.first()
                basis = "本气${hidden.first().chinese}不透，${pick.chinese}透出，以${tenGodOfStem(day, pick).chinese}取格"
            }
            tou.size >= 2 -> {
                pick = tou.maxBy { forceOf(it) }
                val cmp = tou.joinToString("、") { "${it.chinese}${forceOf(it)}" }
                basis = "本气${hidden.first().chinese}不透，${tou.joinToString("、") { it.chinese }}并透，较力（$cmp）以较强之${pick.chinese}取格"
            }
            // 法四：皆不透——最强藏干；单藏直接本气
            else -> {
                pick = if (hidden.size == 1) hidden.first()
                else hidden.filter { tenGodOfStem(day, it) !in listOf(TenGod.BI_JIAN, TenGod.JIE_CAI) }
                    .maxBy { forceOf(it) }
                basis = if (hidden.size == 1)
                    "月令${monthBranch.chinese}单藏${pick.chinese}不透，直接以${tenGodOfStem(day, pick).chinese}取格"
                else "月令${monthBranch.chinese}藏干俱不透，以力量最强之${pick.chinese}（${tenGodOfStem(day, pick).chinese}，力${forceOf(pick)}）取格"
            }
        }

        return GeJuResult(
            name = "${tenGodOfStem(day, pick).chinese}格",
            tenGod = tenGodOfStem(day, pick),
            touGanStem = if (pick in tou) pick else null,
            basis = basis,
            strength = strength,
        )
    }
}
