package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DaYunInfo
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.PillarBranchLabels
import cn.xulun.bazi.model.PillarStemLabels
import cn.xulun.bazi.model.TenGod
import cn.xulun.bazi.model.WealthPattern
import cn.xulun.bazi.model.WealthResult
import cn.xulun.bazi.model.WealthStar
import cn.xulun.bazi.model.WealthStorage
import cn.xulun.bazi.model.branchHiddenStems
import cn.xulun.bazi.model.tenGodOfBranch
import cn.xulun.bazi.model.tenGodOfStem
import cn.xulun.bazi.model.wealthStorage

/**
 * 财运分析器：原局财星辨识、身财旺衰·富格、财库与开库、岁运财运。
 *
 - 财五行 = 日主所克（`Element.controls`）
 - 财星 = 十神为正财/偏财者（透干 + 地支藏干）
 - 财库 = 财五行之墓库；墓库逢对宫冲（辰戌/丑未）则开
 - 富格 = 身强弱（`GeJuCalculator.assessStrength`）× 财星数 × 月令财
 - 岁运财运 = 大运/流年柱带财星的时段
 */
object WealthCalculator {

    /** 藏干位次名 */
    private val hiddenPositions = listOf("本气", "中气", "余气")

    fun analyze(bazi: BaziResult, daYun: DaYunInfo?): WealthResult {
        val dayMaster = bazi.dayPillar.stem
        val wealthElement = dayMaster.element.controls
        val stars = findStars(bazi, dayMaster)
        val storage = findStorage(bazi, wealthElement)
        val (pattern, basis) = determinePattern(bazi, dayMaster, stars)
        val yunFortune = findYunFortune(daYun, dayMaster)
        return WealthResult(
            wealthElement = wealthElement,
            stars = stars,
            storage = storage,
            pattern = pattern,
            patternBasis = basis,
            yunFortune = yunFortune,
        )
    }

    // ── 财星辨识 ──
    private fun findStars(bazi: BaziResult, dayMaster: cn.xulun.bazi.model.HeavenlyStem): List<WealthStar> {
        val result = mutableListOf<WealthStar>()
        // 天干（跳过日柱 index 2 = 日主本身）
        bazi.stems.forEachIndexed { i, stem ->
            if (i == 2) return@forEachIndexed
            val god = tenGodOfStem(dayMaster, stem)
            if (god == TenGod.ZHENG_CAI || god == TenGod.PIAN_CAI) {
                result.add(WealthStar(stem, god, PillarStemLabels[i], hidden = false))
            }
        }
        // 地支藏干
        bazi.branches.forEachIndexed { i, branch ->
            val stems = branchHiddenStems.getValue(branch)
            stems.forEachIndexed { hi, stem ->
                val god = tenGodOfStem(dayMaster, stem)
                if (god == TenGod.ZHENG_CAI || god == TenGod.PIAN_CAI) {
                    val pos = hiddenPositions.getOrElse(hi) { "藏" }
                    result.add(WealthStar(stem, god, "${PillarBranchLabels[i]}·$pos", hidden = true))
                }
            }
        }
        return result
    }

    // ── 财库与开库 ──
    private fun findStorage(bazi: BaziResult, wealthElement: cn.xulun.bazi.model.Element): WealthStorage {
        val storageBranch = wealthElement.wealthStorage
        val idx = bazi.branches.indexOf(storageBranch)
        val present = idx >= 0
        val location = if (present) PillarBranchLabels[idx] else ""
        // 墓库逢对宫冲（辰戌冲 / 丑未冲）则开
        val opener = DiZhiRelationCalculator.analyze(bazi).chongList
            .firstOrNull { it.branch1 == storageBranch || it.branch2 == storageBranch }
        return WealthStorage(
            branch = storageBranch,
            present = present,
            location = location,
            opened = opener != null,
            openBy = opener?.description,
        )
    }

    // ── 富格判定 ──
    private fun determinePattern(
        bazi: BaziResult,
        dayMaster: cn.xulun.bazi.model.HeavenlyStem,
        stars: List<WealthStar>,
    ): Pair<WealthPattern, String> {
        val strength = GeJuCalculator.assessStrength(bazi)
        val monthGod = tenGodOfBranch(dayMaster, bazi.monthPillar.branch)
        val monthIsWealth = monthGod == TenGod.ZHENG_CAI || monthGod == TenGod.PIAN_CAI
        val count = stars.size
        val monthNote = if (monthIsWealth) "，月令为财" else ""
        return when {
            count == 0 -> {
                val p = if (strength == DayStrength.STRONG) WealthPattern.NO_WEALTH_STRONG
                else WealthPattern.NO_WEALTH_WEAK
                p to "原局财星不显，日主${strength.chinese}"
            }
            strength == DayStrength.STRONG && (count >= 2 || monthIsWealth) ->
                WealthPattern.STRONG_WEALTH to "日主身强，财星${count}见$monthNote，身能任财"
            strength == DayStrength.STRONG ->
                WealthPattern.STRONG_LIGHT_WEALTH to "日主身强，财星仅${count}见且月令非财，财薄待运"
            strength == DayStrength.WEAK && (count >= 3 || (monthIsWealth && count >= 2)) ->
                WealthPattern.WEAK_HEAVY_WEALTH to "日主身弱，财星${count}见$monthNote，财多难任"
            strength == DayStrength.WEAK ->
                WealthPattern.WEAK_LIGHT_WEALTH to "日主身弱，财星${count}见，身财俱弱"
            else -> WealthPattern.BALANCED to "日主中和，财星${count}见，身财两停"
        }
    }

    // ── 岁运财运 ──
    private fun findYunFortune(
        daYun: DaYunInfo?,
        dayMaster: cn.xulun.bazi.model.HeavenlyStem,
    ): cn.xulun.bazi.model.YunFortune {
        if (daYun == null) return cn.xulun.bazi.model.YunFortune(emptyList(), emptyList())
        val dayunList = mutableListOf<cn.xulun.bazi.model.FortunePeriod>()
        val liunianList = mutableListOf<cn.xulun.bazi.model.FortunePeriod>()
        for (yun in daYun.yuns) {
            wealthDescOfPillar(yun.pillar, dayMaster)?.let { desc ->
                dayunList.add(cn.xulun.bazi.model.FortunePeriod("${yun.pillar.fullName}大运", yun.pillar, desc))
            }
            for (ln in yun.liuNian) {
                wealthDescOfPillar(ln.pillar, dayMaster)?.let { desc ->
                    liunianList.add(cn.xulun.bazi.model.FortunePeriod("${ln.year} ${ln.pillar.fullName}", ln.pillar, desc))
                }
            }
        }
        return cn.xulun.bazi.model.YunFortune(dayunList, liunianList)
    }

    /** 运岁柱的财星描述（透干优先），无财返回 null */
    private fun wealthDescOfPillar(
        pillar: Pillar,
        dayMaster: cn.xulun.bazi.model.HeavenlyStem,
    ): String? {
        val stemGod = tenGodOfStem(dayMaster, pillar.stem)
        if (stemGod == TenGod.ZHENG_CAI) return "正财透干"
        if (stemGod == TenGod.PIAN_CAI) return "偏财透干"
        val hiddenWealth = branchHiddenStems.getValue(pillar.branch).firstOrNull { stem ->
            val g = tenGodOfStem(dayMaster, stem)
            g == TenGod.ZHENG_CAI || g == TenGod.PIAN_CAI
        }
        if (hiddenWealth != null) {
            val g = tenGodOfStem(dayMaster, hiddenWealth)
            return "${g.chinese}藏支"
        }
        return null
    }
}
