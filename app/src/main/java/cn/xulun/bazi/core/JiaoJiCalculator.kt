package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DaYunInfo
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.JiaoJiResult
import cn.xulun.bazi.model.TenGod
import cn.xulun.bazi.model.branchHiddenStems
import cn.xulun.bazi.model.tenGodOfStem

/**
 * 交际能力推断引擎。交际以语言（口语/眼神/肢体）实现——思想情绪之表达，命理以**食伤**为代表；
 * 交际需应变智慧——**水主智**，水强者交际手段好。
 *
 * - 原局食伤不弱且未被制住/合住 → 交际天赋好
 * - 原局无食伤/太弱（尤无本气）→ 不善交际（木讷封闭）；土多而强 → 固执己见不善沟通（例一）
 * - 原局食伤弱而行食伤大运 → 该运起交际转折（一改木讷变得善交际，例一壬子运）
 * - 伤官强——语言直接爽快；食神强——含蓄委婉
 * - 水多水强无土克 → 交际天赋好；水生食伤 → 加强；食伤与水相克 → 打折
 */
object JiaoJiCalculator {

    fun analyze(bazi: BaziResult, daYun: DaYunInfo?): JiaoJiResult {
        val day = bazi.dayPillar.stem
        val dayEl = day.element
        val f = BingYaoCalculator.forces(bazi)
        val shiEl = dayEl.generates
        val yinEl = Element.entries.first { it.generates == dayEl }

        val shiShenTou = bazi.stems.indices.filter { it != 2 }.any { tenGodOfStem(day, bazi.stems[it]) == TenGod.SHI_SHEN }
        val shangTou = bazi.stems.indices.filter { it != 2 }.any { tenGodOfStem(day, bazi.stems[it]) == TenGod.SHANG_GUAN }
        val shiBenQi = bazi.branches.count { branchHiddenStems.getValue(it).firstOrNull()?.element == shiEl }
        val shiLi = f[shiEl]!!

        // 制住：印星力明显压过食伤（印克食伤）；印所生之行（比劫）通关则贪生忘克不制（例三——火印2比土1通关）
        val zhiZhu = f[yinEl]!! >= shiLi + 1.0 && f[yinEl.generates]!! < 1.0
        // 合住：食伤透干与原局他干五合（贪合忘泄）
        val heZhu = (shiShenTou || shangTou) && run {
            val shiShangGans = bazi.stems.indices.filter { it != 2 }
                .filter { tenGodOfStem(day, bazi.stems[it]) in setOf(TenGod.SHI_SHEN, TenGod.SHANG_GUAN) }
                .map { bazi.stems[it] }
            shiShangGans.any { s -> bazi.stems.any { it != s && it != day && Math.floorMod(s.ordinal - it.ordinal, 10) == 5 } }
        }

        val parts = mutableListOf<String>()
        val talent = when {
            shiLi <= 0.5 && shiBenQi == 0 && !shiShenTou && !shangTou -> {
                parts += "原局无本气之食伤（表达之星不现）——不善交际，性格木讷封闭" +
                    (if (f[Element.EARTH]!! >= 3.0) "；土多而强——固执己见，不善与人沟通（例一石女士式）" else "")
                "交际天赋差（不善交际）"
            }
            zhiZhu -> {
                parts += "食伤（力${shiLi}）被印星（力${f[yinEl]}）制住——表达受抑，交际放不开"
                "交际天赋受限（表达被抑）"
            }
            heZhu -> {
                parts += "食伤透干而被合住（贪合忘泄）——表达之心被牵绊"
                "交际天赋受限（表达被绊）"
            }
            shiLi >= 2.0 && !zhiZhu && !heZhu -> {
                parts += "原局食伤不弱（${shiEl.chinese}行力${shiLi}）且未被制住合住——交际天赋较好，能力往往也不错"
                "交际天赋好"
            }
            f[Element.WATER]!! >= 4.0 -> {
                parts += "食伤偏弱（力${shiLi}）而水智强旺（水力${f[Element.WATER]}）——非常聪明灵活，补表达之弱——交际还是不错的（例二王先生式）"
                "交际天赋不错（水智补弱）"
            }
            else -> {
                parts += "食伤力${shiLi}偏弱——交际天赋一般，后天锻炼可补"
                "交际天赋一般"
            }
        }

        // 风格
        val shangLi = run {
            var v = 0.0
            bazi.stems.forEachIndexed { i, s -> if (i != 2 && tenGodOfStem(day, s) == TenGod.SHANG_GUAN) v += 1.0 }
            bazi.branches.forEach { br -> branchHiddenStems.getValue(br).forEach { h -> if (tenGodOfStem(day, h) == TenGod.SHANG_GUAN) v += 0.5 } }
            v
        }
        val shiShenLi = run {
            var v = 0.0
            bazi.stems.forEachIndexed { i, s -> if (i != 2 && tenGodOfStem(day, s) == TenGod.SHI_SHEN) v += 1.0 }
            bazi.branches.forEach { br -> branchHiddenStems.getValue(br).forEach { h -> if (tenGodOfStem(day, h) == TenGod.SHI_SHEN) v += 0.5 } }
            v
        }
        val style = when {
            shangTou && shangLi >= shiShenLi -> "伤官较强——语言直接爽快，一针见血"
            shiShenTou -> "食神透干——含蓄委婉，言语得体"
            shiBenQi > 0 && shangLi < 0.5 -> "食神藏支而无伤官——内秀型：含蓄委婉，初识不觉、久处方知，口碑渐好（例二王先生式）"
            shiBenQi > 0 || shiShenLi > 0 -> "食神较弱——含蓄委婉"
            else -> "食伤不显——少言寡语"
        }

        // 水智与生克（多注并列——可同时有水智强与食伤克水打折）
        val waterNotes = mutableListOf<String>()
        if (f[Element.WATER]!! >= 3.0) {
            waterNotes += if (f[Element.EARTH]!! < f[Element.WATER]!!) {
                "命局水多水强（力${f[Element.WATER]}）而无土克——水主智，交际手段和能力较好"
            } else {
                "水虽多（力${f[Element.WATER]}）而被土克——交际智慧打折"
            }
        }
        if (Element.WATER.generates == shiEl && f[Element.WATER]!! >= 2.0) {
            waterNotes += "水生食伤（水力${f[Element.WATER]}）——智慧生表达，交际能力得到加强"
        }
        if (shiEl.generates == Element.WATER && shiLi >= 1.0) {
            waterNotes += "食伤（${shiEl.chinese}）与水相生——表达与智慧相济，交际天赋不错（例三刘先生式）"
        }
        if (shiEl.controls == Element.WATER && shiBenQi >= 1) {
            waterNotes += "食伤（${shiEl.chinese}本气支）与水相克——交际天赋打折扣；惟水多而强旺——聪明灵活，交际仍然不错（例二式）"
        }
        if (Element.WATER.controls == shiEl && f[Element.WATER]!! >= 3.0) {
            waterNotes += "水克食伤（${shiEl.chinese}）——智慧压过表达，想说说不出口"
        }
        val waterNote = waterNotes.joinToString("；")

        // 食伤大运转折
        val yunTurning = if (shiLi <= 1.0 && daYun != null) {
            val y = daYun.yuns.firstOrNull { yun ->
                yun.pillar.stem.element == shiEl ||
                    branchHiddenStems.getValue(yun.pillar.branch).firstOrNull()?.element == shiEl
            }
            if (y != null) "${y.pillar.fullName}运（${y.startYear}-${y.endYear}）起为交际转折点——一改过去木讷封闭的性格而变得喜欢交际甚至善于交际" else ""
        } else ""

        return JiaoJiResult(
            talent = talent,
            basis = parts.joinToString("；"),
            style = style,
            waterNote = waterNote,
            yunTurning = yunTurning,
            summary = "交际以语言（口语/眼神/肢体）为载体——思想情绪之表达，命理以食伤为代表；水主智，水强者应变好。" +
                "食伤不弱未被制合则天赋好；原局弱而行食伤运则该运转折。",
        )
    }
}
