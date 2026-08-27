package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.GodCategory
import cn.xulun.bazi.model.TenGod

/**
 * 六亲喜忌派生（教材口径，各亲属块共用）。理论见 `.liuqin-reference.md`。
 *
 * 朴素扶抑，不作从格翻转——与 [BingYaoCalculator] 的病药/顺势取用是并行两判：
 * - 身弱：喜印比，忌食伤/财/官杀；身强反是（[StrengthEngine] 八步法强弱为准）
 * - 中和：忌无；喜印绶（中和无病之用），并按扶耗两侧细分——扶>耗偏旺喜食伤财官，耗>扶偏弱喜印比
 * - 调候为急：气候级调候神（寒→火/燥→水）之类别转喜
 * - 印重为病：印力≥3 且 ≥2 倍比劫力时，母旺子衰，印反为忌（例：乙卯戊寅丁酉乙巳，木重火塞）
 */
internal object LiuQinXiJi {

    /** 十神 → 十神大类（正偏不分）。 */
    fun TenGod.category(): GodCategory = when (this) {
        TenGod.BI_JIAN, TenGod.JIE_CAI -> GodCategory.BI
        TenGod.ZHENG_YIN, TenGod.PIAN_YIN -> GodCategory.YIN
        TenGod.SHI_SHEN, TenGod.SHANG_GUAN -> GodCategory.SHI
        TenGod.ZHENG_CAI, TenGod.PIAN_CAI -> GodCategory.CAI
        TenGod.ZHENG_GUAN, TenGod.QI_SHA -> GodCategory.GUAN
    }

    /** 大类对应的五行（相对日干）。 */
    fun elementOf(cat: GodCategory, dayEl: Element): Element = when (cat) {
        GodCategory.BI -> dayEl
        GodCategory.YIN -> Element.entries.first { it.generates == dayEl }
        GodCategory.SHI -> dayEl.generates
        GodCategory.CAI -> dayEl.controls
        GodCategory.GUAN -> Element.entries.first { it.controls == dayEl }
    }

    /** 五行 → 大类（相对日干）。 */
    fun categoryOf(el: Element, dayEl: Element): GodCategory = when {
        el == dayEl -> GodCategory.BI
        el.generates == dayEl -> GodCategory.YIN
        dayEl.generates == el -> GodCategory.SHI
        dayEl.controls == el -> GodCategory.CAI
        else -> GodCategory.GUAN
    }

    /** 派生喜忌结论。 */
    class Derived(val fav: Set<GodCategory>, val unf: Set<GodCategory>, val basis: String)

    fun derive(bazi: BaziResult): Derived {
        val dayEl = bazi.dayPillar.stem.element
        val eng = StrengthEngine.analyze(bazi)
        val f = BingYaoCalculator.forces(bazi)
        val fav = mutableSetOf<GodCategory>()
        val unf = mutableSetOf<GodCategory>()
        val notes = mutableListOf<String>()
        when (eng.verdict) {
            DayStrength.WEAK -> {
                fav += listOf(GodCategory.BI, GodCategory.YIN)
                unf += listOf(GodCategory.SHI, GodCategory.CAI, GodCategory.GUAN)
                notes += "身弱喜印比、忌克泄耗"
            }
            DayStrength.STRONG -> {
                fav += listOf(GodCategory.SHI, GodCategory.CAI, GodCategory.GUAN)
                unf += listOf(GodCategory.BI, GodCategory.YIN)
                notes += "身强喜克泄耗、忌印比"
            }
            DayStrength.BALANCED -> {
                fav += GodCategory.YIN // 中和无病取印绶
                when {
                    eng.aSide > eng.bSide -> {
                        fav += listOf(GodCategory.SHI, GodCategory.CAI, GodCategory.GUAN)
                        notes += "中和偏旺，兼喜食伤财官"
                    }
                    eng.aSide < eng.bSide -> {
                        fav += listOf(GodCategory.BI, GodCategory.YIN)
                        notes += "中和偏弱，兼喜印比"
                    }
                    else -> notes += "中和无病，喜印绶"
                }
            }
        }
        // 调候为急：气候级调候神转喜
        val thEl = TiaoHouCalculator.analyze(bazi).tiaoHouElement
        if (thEl != null) {
            val cat = categoryOf(thEl, dayEl)
            fav += cat; unf -= cat
            notes += "调候${thEl.chinese}为急，${cat.chinese}转喜"
        }
        // 印重为病：母旺子衰，印反为忌
        val yinEl = elementOf(GodCategory.YIN, dayEl)
        val biEl = elementOf(GodCategory.BI, dayEl)
        if (eng.verdict != DayStrength.STRONG && f[yinEl]!! >= 3.0 && f[yinEl]!! >= 2.0 * f[biEl]!!) {
            fav -= GodCategory.YIN; unf += GodCategory.YIN
            notes += "印重为病（${yinEl.chinese}力${f[yinEl]}≥2倍比劫），母旺子衰印反为忌"
        }
        return Derived(fav.toSet(), unf.toSet(), notes.joinToString("；"))
    }
}
