package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DaYunInfo
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.BianDongYear
import cn.xulun.bazi.model.ZhiYeBianDongResult
import cn.xulun.bazi.model.branchHiddenStems
import cn.xulun.bazi.model.tenGodOfStem

/**
 * 职业变动引擎（教材·职业变动节）。升迁见官运引擎，此处主工作环境变动/调动/走动。
 *
 * 四标志：
 * 1. 岁运与四柱构成刑冲合，刑冲**动日柱官杀印星**（或合旺官杀印星）——工作环境变动
 * 2. 岁运临**驿马**（年支起）——工作调动
 * 3. **官杀值年克身较重**（身弱）——日干不堪重负有走动
 * 4. **伤官食神流年**——多工作变动
 *
 * 特殊式：卯酉冲为**地域冲**（环境变化）；岁运并临喜神+驿马重重——停薪留职下海（例一）；
 * 岁支冲旺财星而财旺身弱——当年无变动难逃病灾（被迫调走，例三）。
 */
object ZhiYeBianDongCalculator {

    private val sanXingGroups = listOf(
        setOf(EarthlyBranch.YIN, EarthlyBranch.SI, EarthlyBranch.SHEN),
        setOf(EarthlyBranch.CHOU, EarthlyBranch.XU, EarthlyBranch.WEI),
    )
    private val liuHePairs = listOf(
        setOf(EarthlyBranch.ZI, EarthlyBranch.CHOU), setOf(EarthlyBranch.YIN, EarthlyBranch.HAI),
        setOf(EarthlyBranch.MAO, EarthlyBranch.XU), setOf(EarthlyBranch.CHEN, EarthlyBranch.YOU),
        setOf(EarthlyBranch.SI, EarthlyBranch.SHEN), setOf(EarthlyBranch.WU, EarthlyBranch.WEI),
    )
    /** 年支起驿马：申子辰马在寅，寅午戌马在申，巳酉丑马在亥，亥卯未马在巳。 */
    private val yiMa = mapOf(
        EarthlyBranch.SHEN to EarthlyBranch.YIN, EarthlyBranch.ZI to EarthlyBranch.YIN, EarthlyBranch.CHEN to EarthlyBranch.YIN,
        EarthlyBranch.YIN to EarthlyBranch.SHEN, EarthlyBranch.WU to EarthlyBranch.SHEN, EarthlyBranch.XU to EarthlyBranch.SHEN,
        EarthlyBranch.SI to EarthlyBranch.HAI, EarthlyBranch.YOU to EarthlyBranch.HAI, EarthlyBranch.CHOU to EarthlyBranch.HAI,
        EarthlyBranch.HAI to EarthlyBranch.SI, EarthlyBranch.MAO to EarthlyBranch.SI, EarthlyBranch.WEI to EarthlyBranch.SI,
    )

    private fun br2(b: EarthlyBranch): EarthlyBranch = EarthlyBranch.entries[(b.ordinal + 6) % 12]

    fun analyze(bazi: BaziResult, daYun: DaYunInfo?): ZhiYeBianDongResult {
        val day = bazi.dayPillar.stem
        val dayEl = day.element
        val guanEl = Element.entries.first { it.controls == dayEl }
        val yinEl = Element.entries.first { it.generates == dayEl }
        val caiEl = dayEl.controls
        val shiEl = dayEl.generates
        val weak = StrengthEngine.analyze(bazi).verdict == DayStrength.WEAK
        val posLabels = listOf("年支", "月支", "日支", "时支")

        fun benQi(b: EarthlyBranch) = branchHiddenStems.getValue(b).firstOrNull()?.element

        val years = daYun?.yuns.orEmpty().flatMap { yun ->
            yun.liuNian.mapNotNull { ln ->
                val yb = ln.pillar.branch
                val ys = ln.pillar.stem
                val triggers = mutableListOf<String>()
                val notes = mutableListOf<String>()

                // ① 岁支刑冲原局官杀印星支（或合动官印支）
                bazi.branches.forEachIndexed { i, tb ->
                    if (tb == yb) return@forEachIndexed
                    val el = benQi(tb)
                    val pos = posLabels[i]
                    when {
                        br2(yb) == tb && (el == guanEl || el == yinEl) ->
                            triggers += "${yb.chinese}冲${tb.chinese}($pos·${if (el == guanEl) "官杀" else "印星"}之根)"
                        sanXingGroups.any { yb in it && tb in it } && (el == guanEl || el == yinEl) ->
                            triggers += "${yb.chinese}刑${tb.chinese}($pos·${if (el == guanEl) "官杀" else "印星"}之根)"
                        liuHePairs.any { yb in it && tb in it } && (el == guanEl || el == yinEl) ->
                            triggers += "${yb.chinese}合${tb.chinese}($pos·${if (el == guanEl) "官杀" else "印星"})——合动"
                    }
                }
                if (triggers.isNotEmpty()) notes += "刑冲合动官杀印星——工作环境变动之应"

                // ② 驿马（年支起，岁支/运支临马）
                val ma = yiMa[bazi.yearPillar.branch]
                if (ma != null && (yb == ma || yun.pillar.branch == ma)) {
                    val zhong = yb == ma && yun.pillar.branch == ma
                    triggers += "驿马${ma.chinese}入命" + (if (zhong) "（岁运并临——驿马重重）" else "")
                    notes += "驿马主奔波走动——工作调动之应"
                }

                // ③ 官杀值年克身较重（身弱）
                if (weak && (ys.element == guanEl || benQi(yb) == guanEl)) {
                    triggers += "官杀值年而身弱——克身较重"
                    notes += "日干不堪重负——有走动之应"
                }

                // ④ 伤官食神流年
                if (tenGodOfStem(day, ys).let { it == cn.xulun.bazi.model.TenGod.SHANG_GUAN || it == cn.xulun.bazi.model.TenGod.SHI_SHEN } ||
                    benQi(yb) == shiEl
                ) {
                    triggers += "食伤流年（${ys.chinese}${yb.chinese}）"
                    notes += "食伤之年多工作变动之应"
                }

                // 特殊：卯酉冲——地域冲（环境变化，例二式）
                if ((yb == EarthlyBranch.MAO && bazi.branches.contains(EarthlyBranch.YOU)) ||
                    (yb == EarthlyBranch.YOU && bazi.branches.contains(EarthlyBranch.MAO))
                ) {
                    triggers += "${yb.chinese}酉冲为地域冲"
                    notes += "卯酉相冲主地域之变——环境变化（例二上中专式）"
                }

                // 特殊：冲旺财星而财旺身弱——变动或病灾（例三式）
                val chongCai = bazi.branches.count { it == br2(yb) && benQi(it) == caiEl }
                if (chongCai >= 1 && (weak || chongCai >= 2)) {
                    triggers += "${yb.chinese}冲${br2(yb).chinese}（财星本气）×${chongCai}——冲旺财星"
                    notes += "财旺身弱——当年若无变动，当难逃病灾（因受排挤而调走，例三式）"
                }

                if (triggers.isEmpty()) return@mapNotNull null

                // 岁运并临喜神（干支皆同）——突发奇想之变（例一停薪留职下海式）
                val bingLin = yun.pillar.stem == ys && yun.pillar.branch == yb
                val text = buildString {
                    append(notes.joinToString("；"))
                    if (bingLin) append("；岁运并临${ys.chinese}${yb.chinese}——思维突发奇想，宜乘势而为（停薪留职下海经商，例一周先生式）")
                    append("。")
                }
                BianDongYear(ln.year, "${ln.year} ${ln.pillar.fullName}", triggers, text)
            }
        }

        return ZhiYeBianDongResult(
            years = years,
            summary = "职业变动四标志：岁运刑冲合动日柱官杀印星；临驿马；官杀值年克身重（身弱）；食伤流年。" +
                "卯酉冲为地域冲；岁运并临喜神+驿马重重主下海；冲旺财星而财旺身弱——无变动则病灾。",
        )
    }
}
