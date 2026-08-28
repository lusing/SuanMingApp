package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DaYunInfo
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.TenGod
import cn.xulun.bazi.model.TianFuItem
import cn.xulun.bazi.model.TianFuResult
import cn.xulun.bazi.model.branchHiddenStems
import cn.xulun.bazi.model.tenGodOfStem
import cn.xulun.bazi.core.LiuQinXiJi.category

/**
 * 天赋推断与职业选择引擎（十神含义+五行特性→天赋→职业；行运定取舍）。
 *
 * - 伤官主口才交际+水主智 → 与人打交道/销售（例一刘先生）
 * - 财星最旺+伤官生财 → 理财能力 → 销售/经营管理/财务管理（例一）
 * - 七杀贴身透干+偏印强旺 → 自律精明自尊强 → 行政/管理（例二黎先生）
 * - 七杀两透或三合杀局根牢 → 军警司法武职（例三小清先生；例四清禄先生可从政）
 * - 偏财藏支+偏印 → 重财精明内秀——暗中副业隐蔽不影响本职（例三）
 * - 行运取舍：食伤生财大运（≥2 步）→ 从商挣钱优先，官星受比难升迁（例四）；
 *   官杀连运而从 → 官场方向（例二/例三）；官杀运中财被泄 → 不宜辞职下海（例三）
 */
object TianFuCalculator {

    fun analyze(bazi: BaziResult, daYun: DaYunInfo?): TianFuResult {
        val day = bazi.dayPillar.stem
        val dayEl = day.element
        val f = BingYaoCalculator.forces(bazi)
        val shiEl = dayEl.generates
        val caiEl = dayEl.controls
        val guanEl = Element.entries.first { it.controls == dayEl }
        val yinEl = Element.entries.first { it.generates == dayEl }

        fun godCount(target: TenGod): Int =
            bazi.stems.indices.filter { it != 2 }.count { tenGodOfStem(day, bazi.stems[it]) == target } +
                bazi.branches.count { branchHiddenStems.getValue(it).firstOrNull()?.let { tenGodOfStem(day, it) == target } == true }

        val shangTou = godCount(TenGod.SHANG_GUAN) > 0 && bazi.stems.indices.filter { it != 2 }
            .any { tenGodOfStem(day, bazi.stems[it]) == TenGod.SHANG_GUAN }
        val shangZhi = bazi.branches.any { branchHiddenStems.getValue(it).firstOrNull()?.let { tenGodOfStem(day, it) == TenGod.SHANG_GUAN } == true }
        val shaTouCount = bazi.stems.indices.filter { it != 2 }
            .count { tenGodOfStem(day, bazi.stems[it]) == TenGod.QI_SHA }
        val pianYinLi = run {
            var v = 0.0
            bazi.stems.forEachIndexed { i, s -> if (i != 2 && tenGodOfStem(day, s) == TenGod.PIAN_YIN) v += 1.0 }
            bazi.branches.forEach { br -> branchHiddenStems.getValue(br).forEach { h -> if (tenGodOfStem(day, h) == TenGod.PIAN_YIN) v += 0.5 } }
            v
        }
        val caiBenQiZhi = bazi.branches.any { branchHiddenStems.getValue(it).firstOrNull()?.element == caiEl }
        val caiTou = bazi.stems.indices.filter { it != 2 }
            .any { tenGodOfStem(day, bazi.stems[it]) in setOf(TenGod.ZHENG_CAI, TenGod.PIAN_CAI) }
        val pianCaiZhi = caiBenQiZhi && !caiTou
        val yinTou = bazi.stems.indices.filter { it != 2 }
            .any { tenGodOfStem(day, bazi.stems[it]) in setOf(TenGod.ZHENG_YIN, TenGod.PIAN_YIN) }
        val caiMax = f[caiEl]!! == f.values.max()

        val items = mutableListOf<TianFuItem>()
        val careers = mutableListOf<String>()

        if ((shangTou || shangZhi) && f[Element.WATER]!! >= 3.0) {
            items += TianFuItem(
                "伤官口才+水智",
                "伤官主口才与交际，水主聪明灵活——具备与人打交道的天赋，适合交际、销售类工作（例一刘先生式）。",
            )
            careers += "销售/与人打交道的工作"
        }
        if (caiMax && f[shiEl]!! >= 1.0) {
            items += TianFuItem(
                "伤官生财·理财",
                "${caiEl.chinese}（财星）为命局最旺之行，食伤生财——具有很好的理财能力，工作宜围绕「赚钱」二字（销售/经营管理/财务管理）。",
            )
            careers += "销售/经营管理/财务管理（赚钱为核心）"
        }
        val shaSanHe = bazi.branches.toSet().containsAll(listOf(
            when (guanEl) {
                Element.FIRE -> cn.xulun.bazi.model.EarthlyBranch.YIN
                Element.WATER -> cn.xulun.bazi.model.EarthlyBranch.SHEN
                Element.METAL -> cn.xulun.bazi.model.EarthlyBranch.SI
                else -> cn.xulun.bazi.model.EarthlyBranch.HAI
            },
            when (guanEl) {
                Element.FIRE -> cn.xulun.bazi.model.EarthlyBranch.WU
                Element.WATER -> cn.xulun.bazi.model.EarthlyBranch.ZI
                Element.METAL -> cn.xulun.bazi.model.EarthlyBranch.YOU
                else -> cn.xulun.bazi.model.EarthlyBranch.MAO
            },
            when (guanEl) {
                Element.FIRE -> cn.xulun.bazi.model.EarthlyBranch.XU
                Element.WATER -> cn.xulun.bazi.model.EarthlyBranch.CHEN
                Element.METAL -> cn.xulun.bazi.model.EarthlyBranch.CHOU
                else -> cn.xulun.bazi.model.EarthlyBranch.WEI
            },
        ))
        if (((shaTouCount >= 2 && !yinTou) || (shaTouCount >= 1 && shaSanHe))) {
            items += TianFuItem(
                "七杀有力·武职",
                "七杀" + (if (shaTouCount >= 2) "${shaTouCount}透" else "透干") +
                    (if (shaSanHe) "且支成三合杀局" else "") + "有生无克根基牢固——七杀代表司法军警之类武职，能充分发挥作用（例三小清先生式）。",
            )
            careers += "司法/军警之武职"
        } else if (shaTouCount >= 1 && (pianYinLi >= 3.0 || yinTou)) {
            items += TianFuItem(
                "杀印相生·自律精明",
                "天干杀印相生，应为贵格，适于从政（例五方先生式）。" +
                    (if (pianYinLi >= 3.0) "七杀与日干紧贴——自律能力强；偏印当令强旺——想法与众不同精明过人；杀印结合——自尊心面子心强，为出人头地而积极上进（例二黎先生式）。" else ""),
            )
            careers += "行政/管理（从政）"
        }
        if (pianCaiZhi) {
            items += TianFuItem(
                "偏财藏支·精明内秀",
                "财星居于地支（不透干）——会在暗地里捞到外快且不为人所知（例五方先生式）" +
                    (if (pianYinLi >= 2.0) "；偏印精明而内秀——为挣钱会暗中从事第二职业，做得隐蔽不影响本职（例三式）" else ""),
            )
        }

        // 从财从官格：工资渠道型（无偏财不自创——依赖性）
        val teShu = TeShuGeJuCalculator.analyze(bazi)
        if (teShu != null && f[caiEl]!! >= 3.0 && godCount(TenGod.PIAN_CAI) == 0 &&
            teShu.yongShen.any { it == caiEl || it.controls == dayEl }
        ) {
            items += TianFuItem(
                "从财从官·工资型",
                "从格从财又从官，财官相生——挣钱渠道为工资，不会自行创业，也难搞好第二职业；正财旺贪图物质满足，对丈夫具有依赖性——只能干点简单的手工活，复杂工作干不了（例六顺容女士式）。",
            )
            careers += "简单手工活/安定领薪之职（不宜创业）"
        }

        // 行运取舍
        val yuns = daYun?.yuns.orEmpty()
        // 食伤生财运：支本气食伤而干为食伤或财（例一支食伤干财——癸酉壬申；例四干支皆食伤——辛酉庚申；
        // 支非食伤之混杂运不算——例三壬辰癸巳）
        val shiShangYuns = yuns.filter { y ->
            val g = tenGodOfStem(day, y.pillar.stem)
            val z = branchHiddenStems.getValue(y.pillar.branch).firstOrNull()?.let { tenGodOfStem(day, it) }
            (z == TenGod.SHI_SHEN || z == TenGod.SHANG_GUAN) &&
                (g == TenGod.SHI_SHEN || g == TenGod.SHANG_GUAN || g.category() == cn.xulun.bazi.model.GodCategory.CAI)
        }
        val guanYuns = yuns.filter { y ->
            tenGodOfStem(day, y.pillar.stem).category() == cn.xulun.bazi.model.GodCategory.GUAN ||
                branchHiddenStems.getValue(y.pillar.branch).firstOrNull()
                    ?.let { tenGodOfStem(day, it).category() == cn.xulun.bazi.model.GodCategory.GUAN } == true
        }
        val yunNote = buildString {
            if (shiShangYuns.size >= 2) {
                append(
                    "${shiShangYuns.first().startYear}-${shiShangYuns.last().endYear}行食伤大运${shiShangYuns.joinToString("") { it.pillar.fullName }}，食伤生财——挣钱好运：" +
                        "此运官杀不如食伤财星强旺，从政虽不丢官也难有很好升迁，从商更好（例四清禄先生式）；",
                )
            }
            if (guanYuns.size >= 2 && shiShangYuns.size < 2) {
                append(
                    "${guanYuns.first().startYear}-${guanYuns.last().endYear}连续官杀运，命局从官杀则官杀为用——宜朝官场/武职方向努力，在领导岗位会有所政绩（例二/例三式）；",
                )
                // 财星本气支尽入三合杀局——财化杀力弱（例三寅木入寅午戌火局）
                val caiZhis = bazi.branches.filter { branchHiddenStems.getValue(it).firstOrNull()?.element == caiEl }
                val caiHuaSha = shaSanHe && caiZhis.isNotEmpty() && guanEl.let { ge ->
                    val sanHeSet = when (ge) {
                        Element.FIRE -> setOf(cn.xulun.bazi.model.EarthlyBranch.YIN, cn.xulun.bazi.model.EarthlyBranch.WU, cn.xulun.bazi.model.EarthlyBranch.XU)
                        Element.WATER -> setOf(cn.xulun.bazi.model.EarthlyBranch.SHEN, cn.xulun.bazi.model.EarthlyBranch.ZI, cn.xulun.bazi.model.EarthlyBranch.CHEN)
                        Element.METAL -> setOf(cn.xulun.bazi.model.EarthlyBranch.SI, cn.xulun.bazi.model.EarthlyBranch.YOU, cn.xulun.bazi.model.EarthlyBranch.CHOU)
                        else -> setOf(cn.xulun.bazi.model.EarthlyBranch.HAI, cn.xulun.bazi.model.EarthlyBranch.MAO, cn.xulun.bazi.model.EarthlyBranch.WEI)
                    }
                    caiZhis.all { it in sanHeSet }
                }
                if (f[caiEl]!! <= 1.5 || caiHuaSha) {
                    append("此间财星力弱被官杀所泄——不宜辞职下海经商（不在官场则钱被官方所得），从事本职之余适当搞点副业补贴家用（例三式）；")
                }
            }
        }

        return TianFuResult(
            items = items,
            careers = careers.distinct(),
            yunNote = yunNote,
            summary = "十神含义与五行特性反映天赋信息：伤官口才+水智宜交际销售；财星最旺伤官生财宜赚钱之业；" +
                "七杀有力宜武职，杀印相生宜行政；偏财藏支偏印精明可暗中副业。职业取舍配合行运——食伤生财运从商，官杀连运从政。",
        )
    }
}
