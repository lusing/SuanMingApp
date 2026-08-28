package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DaYunInfo
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.PillarBranchLabels
import cn.xulun.bazi.model.PillarStemLabels
import cn.xulun.bazi.model.StudyFortunePeriod
import cn.xulun.bazi.model.StudyPattern
import cn.xulun.bazi.model.StudyResult
import cn.xulun.bazi.model.StudyShenSha
import cn.xulun.bazi.model.StudyStar
import cn.xulun.bazi.model.StudyYunFortune
import cn.xulun.bazi.model.TenGod
import cn.xulun.bazi.model.branchHiddenStems
import cn.xulun.bazi.model.tenGodOfBranch
import cn.xulun.bazi.model.tenGodOfStem

/**
 * 学业/学历分析器：印星辨识、学历格局、文昌/学堂神煞、考学岁运（印/官运）。
 *
 - 印星（正印/偏印）为学历主星
 - 文昌、学堂以日干起
 - 考学岁运看大运/流年的印星、官杀
 */
object StudyCalculator {

    private val hiddenPositions = listOf("本气", "中气", "余气")
    private val sealTargets = setOf(TenGod.ZHENG_YIN, TenGod.PIAN_YIN)
    private val foodTargets = setOf(TenGod.SHI_SHEN, TenGod.SHANG_GUAN)
    private val officerTargets = setOf(TenGod.ZHENG_GUAN, TenGod.QI_SHA)

    fun analyze(bazi: BaziResult, daYun: DaYunInfo?): StudyResult {
        val dayMaster = bazi.dayPillar.stem
        val seals = findStars(bazi, dayMaster, sealTargets)
        val foods = findStars(bazi, dayMaster, foodTargets)
        val officers = findStars(bazi, dayMaster, officerTargets)
        val (pattern, basis) = determinePattern(bazi, dayMaster, seals, foods, officers)
        val shenSha = findShenSha(bazi)
        val yunFortune = findYunFortune(daYun, dayMaster)
        val diploma = WenPingCalculator.analyze(bazi, daYun)
        return StudyResult(seals, pattern, basis, shenSha, yunFortune, diploma)
    }

    /** 按目标十神集合辨识星（天干跳日干 + 藏干） */
    private fun findStars(
        bazi: BaziResult,
        dayMaster: HeavenlyStem,
        targets: Set<TenGod>,
    ): List<StudyStar> {
        val result = mutableListOf<StudyStar>()
        bazi.stems.forEachIndexed { i, stem ->
            if (i == 2) return@forEachIndexed // 日干为日主
            val god = tenGodOfStem(dayMaster, stem)
            if (god in targets) {
                result.add(StudyStar(stem, god, PillarStemLabels[i], hidden = false))
            }
        }
        bazi.branches.forEachIndexed { i, branch ->
            branchHiddenStems.getValue(branch).forEachIndexed { hi, stem ->
                val god = tenGodOfStem(dayMaster, stem)
                if (god in targets) {
                    val pos = hiddenPositions.getOrElse(hi) { "藏" }
                    result.add(StudyStar(stem, god, "${PillarBranchLabels[i]}·$pos", hidden = true))
                }
            }
        }
        return result
    }

    // ── 学历格局 ──
    private fun determinePattern(
        bazi: BaziResult,
        dayMaster: HeavenlyStem,
        seals: List<StudyStar>,
        foods: List<StudyStar>,
        officers: List<StudyStar>,
    ): Pair<StudyPattern, String> {
        val strength = GeJuCalculator.assessStrength(bazi)
        val monthGod = tenGodOfBranch(dayMaster, bazi.monthPillar.branch)
        val monthIsSeal = monthGod in sealTargets
        val sealCount = seals.size
        val sealRevealed = seals.count { !it.hidden }
        val foodCount = foods.size
        val officerRevealed = officers.any { !it.hidden }

        val (pattern, mainNote) = when {
            sealCount == 0 && foodCount >= 2 ->
                StudyPattern.TALENT to "印星不显而食伤${foodCount}见，聪明多才艺"
            sealCount == 0 ->
                StudyPattern.LOW to "印星与食伤俱弱，无学历主星"
            monthIsSeal || sealCount >= 2 ->
                StudyPattern.HIGH to "印星旺相" + (if (monthIsSeal) "（月令为印）" else "（${sealCount}见）")
            sealRevealed >= 1 ->
                StudyPattern.HIGH to "印星透干为用"
            else ->
                StudyPattern.MEDIUM to "印星偏弱（仅藏支）"
        }
        val parts = mutableListOf(mainNote, "日主${strength.chinese}")
        if (officerRevealed && sealRevealed >= 1) parts += "官印相生，利考学升学"
        parts += when {
            pattern == StudyPattern.HIGH -> "偏文/理论"
            foodCount >= 2 -> "偏理/才艺"
            else -> "文理均衡"
        }
        return pattern to parts.joinToString("；")
    }

    // ── 学业神煞：文昌、学堂（取自通用神煞层，日干起）──
    private fun findShenSha(bazi: BaziResult): List<StudyShenSha> {
        val result = mutableListOf<StudyShenSha>()
        val all = ShenShaCalculator.analyze(bazi).items
        all.firstOrNull { it.name == "文昌" }?.let {
            result.add(StudyShenSha("文昌", it.branch!!, it.pillarLabel))
        }
        all.firstOrNull { it.name == "学堂" }?.let {
            result.add(StudyShenSha("学堂", it.branch!!, it.pillarLabel))
        }
        return result
    }

    // ── 考学岁运：印星 / 官杀运 ──
    private fun findYunFortune(daYun: DaYunInfo?, dayMaster: HeavenlyStem): StudyYunFortune {
        if (daYun == null) return StudyYunFortune(emptyList(), emptyList())
        val dayun = mutableListOf<StudyFortunePeriod>()
        val liunian = mutableListOf<StudyFortunePeriod>()
        for (yun in daYun.yuns) {
            studyDescOfPillar(yun.pillar, dayMaster)?.let { desc ->
                dayun.add(StudyFortunePeriod("${yun.pillar.fullName}大运", yun.pillar, desc))
            }
            for (ln in yun.liuNian) {
                studyDescOfPillar(ln.pillar, dayMaster)?.let { desc ->
                    liunian.add(StudyFortunePeriod("${ln.year} ${ln.pillar.fullName}", ln.pillar, desc))
                }
            }
        }
        return StudyYunFortune(dayun, liunian)
    }

    /** 运岁柱的印/官描述（透干优先），无则 null */
    private fun studyDescOfPillar(pillar: Pillar, dayMaster: HeavenlyStem): String? {
        val stemGod = tenGodOfStem(dayMaster, pillar.stem)
        if (stemGod in sealTargets || stemGod in officerTargets) return "${stemGod.chinese}透干"
        val hit = branchHiddenStems.getValue(pillar.branch).firstOrNull { stem ->
            val g = tenGodOfStem(dayMaster, stem)
            g in sealTargets || g in officerTargets
        }
        if (hit != null) {
            val g = tenGodOfStem(dayMaster, hit)
            return "${g.chinese}藏支"
        }
        return null
    }
}
