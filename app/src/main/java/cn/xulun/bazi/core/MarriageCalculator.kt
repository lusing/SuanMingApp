package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.MarriagePattern
import cn.xulun.bazi.model.MarriageResult
import cn.xulun.bazi.model.PillarBranchLabels
import cn.xulun.bazi.model.PillarStemLabels
import cn.xulun.bazi.model.ShenSha
import cn.xulun.bazi.model.SpousePalace
import cn.xulun.bazi.model.SpouseStar
import cn.xulun.bazi.model.TenGod
import cn.xulun.bazi.model.branchHiddenStems
import cn.xulun.bazi.model.hiddenStemGods
import cn.xulun.bazi.model.tenGodOfBranch
import cn.xulun.bazi.model.tenGodOfStem

/**
 * 婚姻分析器：配偶星辨识、夫妻宫（日支）冲合刑、婚姻格局、桃花/孤辰寡宿神煞。
 *
 - 配偶星：男命财星（正/偏财）、女命官杀（正官/七杀）
 - 夫妻宫 = 日支；逢冲/刑主动荡，逢合主和谐
 - 桃花以日支起、孤辰寡宿以年支起
 */
object MarriageCalculator {

    private val hiddenPositions = listOf("本气", "中气", "余气")

    fun analyze(bazi: BaziResult, gender: Gender): MarriageResult {
        val dayMaster = bazi.dayPillar.stem
        val dayBranch = bazi.dayPillar.branch
        val (targets, spouseName) = spouseTargets(gender)
        val stars = findSpouseStars(bazi, dayMaster, targets)
        val palace = findPalace(bazi, dayMaster, dayBranch)
        val (pattern, basis) = determinePattern(stars, palace, spouseName)
        val shenSha = findShenSha(bazi)
        return MarriageResult(
            gender = gender,
            spouseStarName = spouseName,
            spouseStars = stars,
            palace = palace,
            pattern = pattern,
            patternBasis = basis,
            shenSha = shenSha,
        )
    }

    /** 按性别取配偶星十神集合与名称 */
    private fun spouseTargets(gender: Gender): Pair<Set<TenGod>, String> = when (gender) {
        Gender.MALE -> setOf(TenGod.ZHENG_CAI, TenGod.PIAN_CAI) to "财星"
        Gender.FEMALE -> setOf(TenGod.ZHENG_GUAN, TenGod.QI_SHA) to "官杀"
    }

    // ── 配偶星辨识（遍历天干跳日干 + 藏干）──
    private fun findSpouseStars(
        bazi: BaziResult,
        dayMaster: HeavenlyStem,
        targets: Set<TenGod>,
    ): List<SpouseStar> {
        val result = mutableListOf<SpouseStar>()
        bazi.stems.forEachIndexed { i, stem ->
            if (i == 2) return@forEachIndexed // 日干为日主
            val god = tenGodOfStem(dayMaster, stem)
            if (god in targets) {
                result.add(SpouseStar(stem, god, PillarStemLabels[i], hidden = false))
            }
        }
        bazi.branches.forEachIndexed { i, branch ->
            branchHiddenStems.getValue(branch).forEachIndexed { hi, stem ->
                val god = tenGodOfStem(dayMaster, stem)
                if (god in targets) {
                    val pos = hiddenPositions.getOrElse(hi) { "藏" }
                    result.add(SpouseStar(stem, god, "${PillarBranchLabels[i]}·$pos", hidden = true))
                }
            }
        }
        return result
    }

    // ── 夫妻宫（日支）──
    private fun findPalace(bazi: BaziResult, dayMaster: HeavenlyStem, dayBranch: EarthlyBranch): SpousePalace {
        val mainGod = tenGodOfBranch(dayMaster, dayBranch)
        val hiddenGods = hiddenStemGods(dayMaster, dayBranch)
        val diZhi = DiZhiRelationCalculator.analyze(bazi)
        val heHua = HeHuaCalculator.analyze(bazi)
        val chong = diZhi.chongList.firstOrNull { it.location.contains("日支") }
            ?.let { "${it.description}（${it.location}）" }
        val liuHe = heHua.branchLiuHeList.firstOrNull { it.location.contains("日支") }
            ?.let { "${it.description}（${it.location}）" }
        val xing = diZhi.xingList.firstOrNull { it.location.contains("日支") }
            ?.let { "${it.description}（${it.location}）" }
        return SpousePalace(dayBranch, mainGod, hiddenGods, chong, liuHe, xing)
    }

    // ── 婚姻格局 ──
    private fun determinePattern(
        stars: List<SpouseStar>,
        palace: SpousePalace,
        spouseName: String,
    ): Pair<MarriagePattern, String> {
        val count = stars.size
        val revealed = stars.count { !it.hidden }
        val inHour = stars.any { it.location.startsWith("时") }
        return when {
            !palace.stable -> {
                val issue = when {
                    palace.chong != null && palace.xing != null -> "冲刑"
                    palace.chong != null -> "冲"
                    else -> "刑"
                }
                MarriagePattern.UNSTABLE to "日支逢${issue}，夫妻宫受扰，婚姻动荡"
            }
            count >= 3 -> MarriagePattern.COMPLEX to "配偶星${count}见（${spouseName}），多且混杂"
            revealed >= 1 -> MarriagePattern.SMOOTH to "配偶星透干${revealed}见，夫妻宫安稳"
            count == 0 -> MarriagePattern.LATE to "${spouseName}不显，姻缘待运而动"
            inHour -> MarriagePattern.LATE to "配偶星主要现于时柱，姻缘较晚"
            else -> MarriagePattern.PLAIN to "配偶星${count}见（藏支），夫妻宫安稳"
        }
    }

    // ── 神煞：桃花（日支起）、孤辰寡宿（年支起）──
    private fun findShenSha(bazi: BaziResult): List<ShenSha> {
        val branches = bazi.branches
        val result = mutableListOf<ShenSha>()

        val tao = taoHuaOf(branches[2]) // 以日支起
        val taoIdx = branches.indexOf(tao)
        if (taoIdx >= 0) result.add(ShenSha("桃花", tao, PillarBranchLabels[taoIdx]))

        val year = branches[0]
        guChenOf(year)?.let { g ->
            val idx = branches.indexOf(g)
            if (idx >= 0) result.add(ShenSha("孤辰", g, PillarBranchLabels[idx]))
        }
        guaSuOf(year)?.let { g ->
            val idx = branches.indexOf(g)
            if (idx >= 0) result.add(ShenSha("寡宿", g, PillarBranchLabels[idx]))
        }
        return result
    }

    /** 桃花：日支三合局 → 帝旺（桃花）支 */
    private fun taoHuaOf(branch: EarthlyBranch): EarthlyBranch = when (branch) {
        EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN -> EarthlyBranch.YOU
        EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU -> EarthlyBranch.MAO
        EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU -> EarthlyBranch.WU
        EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI -> EarthlyBranch.ZI
    }

    /** 孤辰：年支按四季 → 孤辰支 */
    private fun guChenOf(year: EarthlyBranch): EarthlyBranch? = when (year) {
        EarthlyBranch.YIN, EarthlyBranch.MAO, EarthlyBranch.CHEN -> EarthlyBranch.SI
        EarthlyBranch.SI, EarthlyBranch.WU, EarthlyBranch.WEI -> EarthlyBranch.SHEN
        EarthlyBranch.SHEN, EarthlyBranch.YOU, EarthlyBranch.XU -> EarthlyBranch.HAI
        EarthlyBranch.HAI, EarthlyBranch.ZI, EarthlyBranch.CHOU -> EarthlyBranch.YIN
    }

    /** 寡宿：年支按四季 → 寡宿支 */
    private fun guaSuOf(year: EarthlyBranch): EarthlyBranch? = when (year) {
        EarthlyBranch.YIN, EarthlyBranch.MAO, EarthlyBranch.CHEN -> EarthlyBranch.CHOU
        EarthlyBranch.SI, EarthlyBranch.WU, EarthlyBranch.WEI -> EarthlyBranch.CHEN
        EarthlyBranch.SHEN, EarthlyBranch.YOU, EarthlyBranch.XU -> EarthlyBranch.WEI
        EarthlyBranch.HAI, EarthlyBranch.ZI, EarthlyBranch.CHOU -> EarthlyBranch.XU
    }
}
