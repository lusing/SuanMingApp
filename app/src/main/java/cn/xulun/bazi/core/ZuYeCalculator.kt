package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.GodCategory
import cn.xulun.bazi.model.TenGod
import cn.xulun.bazi.model.ZuYeItem
import cn.xulun.bazi.model.ZuYeResult
import cn.xulun.bazi.model.branchHiddenStems
import cn.xulun.bazi.model.hiddenStemGods
import cn.xulun.bazi.model.tenGodOfStem

/**
 * 祖业断语引擎（.liuqin-reference.md 第一节）。年柱为祖上宫：
 * 以年柱（兼看月柱）十神对日干之喜忌为主判，神煞（贵人/羊刃/禄神/驿马）与
 * 冲破为辅判，给出有利/不利两组断语。
 *
 * 喜忌按教材口径（朴素扶抑）派生，不作从格翻转——与 [BingYaoCalculator] 的
 * 病药/顺势取用是并行两判：
 * - 身弱：喜印比，忌食伤/财/官杀；身强反是（[StrengthEngine] 八步法强弱为准）
 * - 中和：忌无；喜印绶（中和无病之用），并按扶耗两侧细分——扶>耗偏旺喜食伤财官，耗>扶偏弱喜印比
 * - 调候为急：气候级调候神（寒→火/燥→水）之类别转喜
 * - 印重为病：印力≥3 且 ≥2 倍比劫力时，母旺子衰，印反为忌（枭神夺食式——例10 乙卯戊寅丁酉乙巳）
 */
object ZuYeCalculator {

    /** 十神 → 十神大类（正偏不分）。 */
    private fun TenGod.category(): GodCategory = when (this) {
        TenGod.BI_JIAN, TenGod.JIE_CAI -> GodCategory.BI
        TenGod.ZHENG_YIN, TenGod.PIAN_YIN -> GodCategory.YIN
        TenGod.SHI_SHEN, TenGod.SHANG_GUAN -> GodCategory.SHI
        TenGod.ZHENG_CAI, TenGod.PIAN_CAI -> GodCategory.CAI
        TenGod.ZHENG_GUAN, TenGod.QI_SHA -> GodCategory.GUAN
    }

    /** 大类对应的五行（相对日干）。 */
    private fun elementOf(cat: GodCategory, dayEl: Element): Element = when (cat) {
        GodCategory.BI -> dayEl
        GodCategory.YIN -> Element.entries.first { it.generates == dayEl }
        GodCategory.SHI -> dayEl.generates
        GodCategory.CAI -> dayEl.controls
        GodCategory.GUAN -> Element.entries.first { it.controls == dayEl }
    }

    /** 五行 → 大类（相对日干）。 */
    private fun categoryOf(el: Element, dayEl: Element): GodCategory = when {
        el == dayEl -> GodCategory.BI
        el.generates == dayEl -> GodCategory.YIN
        dayEl.generates == el -> GodCategory.SHI
        dayEl.controls == el -> GodCategory.CAI
        else -> GodCategory.GUAN
    }

    /** 教材口径派生喜忌（朴素扶抑 + 调候为急 + 印重为病，不从格翻转）。 */
    private class Derived(val fav: Set<GodCategory>, val unf: Set<GodCategory>, val basis: String)

    private fun deriveXiJi(bazi: BaziResult): Derived {
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

    fun analyze(bazi: BaziResult): ZuYeResult {
        val day = bazi.dayPillar.stem
        val dayEl = day.element
        val eng = StrengthEngine.analyze(bazi)
        val d = deriveXiJi(bazi)
        val forces = BingYaoCalculator.forces(bazi)

        // 年/月柱十神清单（干透 + 支藏）
        val yearStemGod = tenGodOfStem(day, bazi.yearPillar.stem)
        val monthStemGod = tenGodOfStem(day, bazi.monthPillar.stem)
        val yearGods: Set<TenGod> = setOf(yearStemGod) + hiddenStemGods(day, bazi.yearPillar.branch).map { it.second }
        val monthGods: Set<TenGod> = setOf(monthStemGod) + hiddenStemGods(day, bazi.monthPillar.branch).map { it.second }
        val yearCats = yearGods.map { it.category() }.toSet()
        val yearMonthCats = yearCats + monthGods.map { it.category() }

        // 神煞（落年柱）与冲
        val shenShaYear = ShenShaCalculator.analyze(bazi).items.filter { it.pillarLabel.startsWith("年") }
        val chong = DiZhiRelationCalculator.analyze(bazi).chongList
        val yearBranchChonged = chong.any { it.location.contains("年支") }
        val yearMonthChong = chong.any { it.location.contains("年支") && it.location.contains("月支") }

        /** 大类通根年支中气以上（本气/中气——余气根太弱不算旺地）。 */
        fun rootedAtYear(cat: GodCategory): Boolean {
            val el = elementOf(cat, dayEl)
            val hidden = branchHiddenStems.getValue(bazi.yearPillar.branch)
            return hidden.indexOfFirst { it.element == el } in intArrayOf(0, 1)
        }

        val fav = mutableListOf<ZuYeItem>()
        val unfav = mutableListOf<ZuYeItem>()

        // ── 有利祖业 ──

        // 1. 年月柱财官印全且不为忌
        if (setOf(GodCategory.CAI, GodCategory.GUAN, GodCategory.YIN).all { it in yearMonthCats } &&
            listOf(GodCategory.CAI, GodCategory.GUAN, GodCategory.YIN).none { it in d.unf }
        ) {
            fav += ZuYeItem(
                "财官印全·年月柱喜用",
                "年月柱财官印全且不为忌，祖上荣华富贵，命主亦得祖上福荫。",
            )
        }

        // 2. 官杀透年干居旺地，日主身强喜官制
        if (yearStemGod.category() == GodCategory.GUAN && eng.verdict == DayStrength.STRONG &&
            (rootedAtYear(GodCategory.GUAN) || forces[elementOf(GodCategory.GUAN, dayEl)]!! >= 2.0)
        ) {
            fav += ZuYeItem(
                "官杀年柱旺地·身强",
                "官杀居年柱旺地，日主身强喜官制，祖宗风光威仪。",
            )
        }

        // 3. 正印居年柱为喜用（身衰喜印扶者多应）
        if (TenGod.ZHENG_YIN in yearGods && GodCategory.YIN in d.fav) {
            fav += ZuYeItem(
                "正印年柱·喜印扶身",
                "正印居年柱为喜用（身衰喜印扶者尤验），祖上书香门第。",
            )
        }

        // 4. 偏印居年柱为喜用
        if (TenGod.PIAN_YIN in yearGods && GodCategory.YIN in d.fav) {
            fav += ZuYeItem(
                "偏印年柱·喜用",
                "偏印居年柱为喜用，可得祖业，祖上多为技艺之人。",
            )
        }

        // 5. 伤官为喜居年柱
        if (TenGod.SHANG_GUAN in yearGods && GodCategory.SHI in d.fav) {
            fav += ZuYeItem(
                "伤官年柱·喜",
                "伤官为喜居年柱，祖业平平；祖上应是有技艺之人。",
            )
        }

        // 6. 食神为喜用居年柱
        if (TenGod.SHI_SHEN in yearGods && GodCategory.SHI in d.fav) {
            fav += ZuYeItem(
                "食神年柱·喜用",
                "食神为喜用居年柱，祖业丰隆，名高德厚。",
            )
        }

        // 7. 比劫居年柱为喜用
        if (GodCategory.BI in yearCats && GodCategory.BI in d.fav) {
            fav += ZuYeItem(
                "比劫年柱·喜用",
                "比劫居年柱为喜用，祖业虽丰，然非特富。",
            )
        }

        // 8. 财旺居年柱，日主不弱胜财
        if (GodCategory.CAI in yearCats && eng.verdict != DayStrength.WEAK &&
            GodCategory.CAI in d.fav &&
            (forces[elementOf(GodCategory.CAI, dayEl)]!! >= 2.5 || rootedAtYear(GodCategory.CAI) ||
                bazi.monthPillar.branch.element == elementOf(GodCategory.CAI, dayEl))
        ) {
            fav += ZuYeItem(
                "财旺年柱·身强",
                "财旺居年柱，日主身强（或中和偏旺）胜财，祖上万贯家资。",
            )
        }

        // 9. 财星透年月干为喜用且通根有力
        val caiEl = elementOf(GodCategory.CAI, dayEl)
        if ((yearStemGod.category() == GodCategory.CAI || monthStemGod.category() == GodCategory.CAI) &&
            GodCategory.CAI in d.fav &&
            (rootedAtYear(GodCategory.CAI) || bazi.monthPillar.branch.element == caiEl ||
                branchHiddenStems.getValue(bazi.monthPillar.branch).any { it.element == caiEl } ||
                forces[caiEl]!! >= 2.5)
        ) {
            fav += ZuYeItem(
                "财星年月·喜用有力",
                "财食居年月柱，强旺有力不为太过不及，祖上禄裕丰隆。",
            )
        }

        // 10. 年上正官不受克害冲破又不为忌
        if (yearStemGod == TenGod.ZHENG_GUAN && GodCategory.GUAN !in d.unf && !yearBranchChonged &&
            bazi.monthPillar.stem.element.controls != bazi.yearPillar.stem.element
        ) {
            fav += ZuYeItem(
                "正官年柱·不忌无破",
                "年上正官不受克害冲破又不为忌，祖上富贵。",
            )
        }

        // 11. 天乙/天德/月德贵人临年柱为喜用
        val guiRen = shenShaYear.firstOrNull { it.name in setOf("天乙贵人", "天德贵人", "月德贵人") }
        if (guiRen != null && yearStemGod.category() !in d.unf) {
            fav += ZuYeItem(
                "贵人临年·喜用",
                "${guiRen.name}临年柱而为喜用神，祖上不但富贵，而且德高望重。",
            )
        }

        // ── 不利祖业 ──

        // 1. 七杀为忌居年柱
        if (TenGod.QI_SHA in yearGods && GodCategory.GUAN in d.unf) {
            unfav += ZuYeItem("七杀年柱·忌", "七杀为忌居年柱，祖上破财（身弱杀旺，甚则祖上有凶死败财之人）。")
        }

        // 2. 正官为忌居年柱
        if (TenGod.ZHENG_GUAN in yearGods && GodCategory.GUAN in d.unf) {
            unfav += ZuYeItem("正官年柱·忌", "正官为忌居年柱，祖上有名无实。")
        }

        // 3. 枭神为忌居年柱
        if (TenGod.PIAN_YIN in yearGods && GodCategory.YIN in d.unf) {
            unfav += ZuYeItem("枭神年柱·忌", "枭神为忌居年柱，祖上贫寒破落。")
        }

        // 4. 正印为忌居年柱
        if (TenGod.ZHENG_YIN in yearGods && GodCategory.YIN in d.unf) {
            unfav += ZuYeItem("正印年柱·忌", "正印为忌居年柱，祖业浅薄。")
        }

        // 5. 财星为忌居年柱
        if (GodCategory.CAI in yearCats && GodCategory.CAI in d.unf) {
            unfav += ZuYeItem("财星年柱·忌", "财星为忌居年柱，祖上虽有财而德不厚。")
        }

        // 6. 伤官为忌居年柱
        if (TenGod.SHANG_GUAN in yearGods && GodCategory.SHI in d.unf) {
            unfav += ZuYeItem("伤官年柱·忌", "伤官为忌居年柱，祖上没落衰败。")
        }

        // 7. 食神为忌居年柱
        if (TenGod.SHI_SHEN in yearGods && GodCategory.SHI in d.unf) {
            unfav += ZuYeItem("食神年柱·忌", "食神为忌居年柱，祖上贫穷。")
        }

        // 8. 羊刃为忌居年柱
        if (shenShaYear.any { it.name == "羊刃" } && GodCategory.BI in d.unf) {
            unfav += ZuYeItem("羊刃年柱·忌", "羊刃为忌居年柱，祖业寒微破败。")
        }

        // 9. 年柱枭食并见
        if (TenGod.PIAN_YIN in yearGods && TenGod.SHI_SHEN in yearGods) {
            unfav += ZuYeItem("年柱枭食并见", "年柱枭食并见，祖上有讨饭之人。")
        }

        // 10/11. 年柱逢冲
        if (yearMonthChong) {
            unfav += ZuYeItem("年月柱相冲", "年月柱相冲，命主背祖离乡。")
        } else if (yearBranchChonged) {
            unfav += ZuYeItem("年支逢冲", "年柱被其他柱冲动，祖上有迁居漂泊之人。")
        }

        // 12. 年柱印星为喜用遭冲破
        if (GodCategory.YIN in yearCats && GodCategory.YIN in d.fav && yearBranchChonged) {
            unfav += ZuYeItem("年柱印星遭冲破", "年柱印星为喜用，遭他柱冲破，祖业难继。")
        }

        // 13. 年柱禄神为喜遭冲破
        if (shenShaYear.any { it.name == "禄神" } && GodCategory.BI in d.fav && yearBranchChonged) {
            unfav += ZuYeItem("年柱禄神遭冲破", "年柱禄神为喜，遭冲破，祖业破败。")
        }

        // 14. 马星临年柱（分临喜用/忌神；遭冲更甚）
        val maXing = shenShaYear.firstOrNull { it.name == "驿马" }
        if (maXing != null) {
            val maCat = tenGodOfStem(day, branchHiddenStems.getValue(bazi.yearPillar.branch).first()).category()
            when {
                maCat in d.fav -> fav += ZuYeItem(
                    "马星临年柱·用神",
                    "马星临用神居年柱，祖辈外出营谋或多迁移，可有大发展。",
                )
                maCat in d.unf -> unfav += ZuYeItem(
                    "马星临年柱·忌神" + if (yearBranchChonged) "遭冲" else "",
                    "马星临忌神居年柱，祖辈多为飘零不定" +
                        (if (yearBranchChonged) "；马星遭冲，飘零更甚" else "") + "。",
                )
                else -> fav += ZuYeItem(
                    "马星临年柱",
                    "马星临年柱，祖辈外出营谋或多迁移。",
                )
            }
        }

        // 15. 年柱喜用之星遭月柱克泄——先旺后败
        val yearStemLeaked = yearStemGod.category() in d.fav &&
            bazi.yearPillar.stem.element.generates == bazi.monthPillar.stem.element
        val yearBranchKe = hiddenStemGods(day, bazi.yearPillar.branch).first().second.category() in d.fav &&
            bazi.monthPillar.branch.element.controls == bazi.yearPillar.branch.element
        if (yearStemLeaked || yearBranchKe) {
            unfav += ZuYeItem(
                "年柱喜用遭克泄",
                "年柱喜用之星遭月柱化泄重克" +
                    (if (yearStemLeaked) "（年干${bazi.yearPillar.stem.chinese}被月干${bazi.monthPillar.stem.chinese}化泄）" else "") +
                    "，祖上先旺后败。",
            )
        }

        // 16. 贵人临年而年柱五行为忌
        if (guiRen != null && yearStemGod.category() in d.unf) {
            unfav += ZuYeItem("贵人临年·年柱为忌", "贵人虽临年柱，然年柱五行为忌神，祖上家道不显。")
        }

        val tendency = when {
            fav.size > unfav.size -> "祖业偏兴，命主多得祖上福荫。"
            unfav.size > fav.size -> "祖业偏衰，命主多靠自立更生。"
            else -> "祖业平平，得荫与否须兼看岁运。"
        }
        return ZuYeResult(
            favorable = fav,
            unfavorable = unfav,
            summary = "判断祖业，弄清年柱（祖上宫）为日干喜忌非常重要，其次根据十神心性予以推断。" +
                "喜忌口径：${d.basis}。本造有利${fav.size}条、不利${unfav.size}条——$tendency",
        )
    }
}
