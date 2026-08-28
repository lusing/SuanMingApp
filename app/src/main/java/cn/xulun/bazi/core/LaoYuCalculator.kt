package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DaYunInfo
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.LaoYuResult
import cn.xulun.bazi.model.LaoYuYear
import cn.xulun.bazi.model.LiuQinItem
import cn.xulun.bazi.model.TenGod
import cn.xulun.bazi.model.branchHiddenStems
import cn.xulun.bazi.model.tenGodOfStem

/**
 * 牢狱之灾引擎（教材第十四节）。原局伏险（15 条信息之可实现集）+ 岁运引发：
 * - **三刑+官杀旺克身年**——官司牢狱之苦（例一丁丑）
 * - **官杀值岁旺而克身**——被小人相害（例一甲戌）
 * - **干支两系统皆战（混战）**——继续承受上年之灾（例一戊寅）
 * - **伤官通根而旺+岁干与原局伤官干冲战**——行为猖獗受制裁，牢狱之灾（例二壬午）
 */
object LaoYuCalculator {

    private val sanXingGroups = listOf(
        setOf(EarthlyBranch.YIN, EarthlyBranch.SI, EarthlyBranch.SHEN),
        setOf(EarthlyBranch.CHOU, EarthlyBranch.XU, EarthlyBranch.WEI),
    )
    private val ganChong = listOf(
        setOf(HeavenlyStem.JIA, HeavenlyStem.GENG), setOf(HeavenlyStem.YI, HeavenlyStem.XIN),
        setOf(HeavenlyStem.BING, HeavenlyStem.REN), setOf(HeavenlyStem.DING, HeavenlyStem.GUI),
    )

    fun analyze(bazi: BaziResult, daYun: DaYunInfo?): LaoYuResult {
        val day = bazi.dayPillar.stem
        val dayEl = day.element
        val f = BingYaoCalculator.forces(bazi)
        val guanEl = Element.entries.first { it.controls == dayEl }
        val yinEl = Element.entries.first { it.generates == dayEl }
        val weak = StrengthEngine.analyze(bazi).verdict == DayStrength.WEAK

        fun tou(target: TenGod) = bazi.stems.indices.filter { it != 2 }.any { tenGodOfStem(day, bazi.stems[it]) == target }
        val shangTouIdx = bazi.stems.indices.filter { it != 2 }.filter { tenGodOfStem(day, bazi.stems[it]) == TenGod.SHANG_GUAN }
        val guanTouIdx = bazi.stems.indices.filter { it != 2 }
            .filter { tenGodOfStem(day, bazi.stems[it]) in setOf(TenGod.ZHENG_GUAN, TenGod.QI_SHA) }

        val risks = mutableListOf<LiuQinItem>()

        // 干支两系统皆战（存在力均之克对且通关无力）——五行结构糟糕（例一水土相战式；
        // 天干通关须≥2 双透，地支通关≥1；非前二大行而按最强克对判定）
        fun sysWar(stems: List<HeavenlyStem>, branches: List<EarthlyBranch>, yueBonus: Boolean, tongXu: Double): Boolean {
            val ff = Element.entries.associateWith { 0.0 }.toMutableMap()
            stems.forEach { ff[it.element] = ff[it.element]!! + 1.0 }
            branches.forEach { ff[it.element] = ff[it.element]!! + 1.0 }
            if (yueBonus) ff[bazi.monthPillar.branch.element] = ff[bazi.monthPillar.branch.element]!! + 2.0
            return Element.entries.any { a -> Element.entries.any { b ->
                a.controls == b && ff[a]!! >= 1.0 && ff[b]!! >= 1.0 && run {
                    val tong = Element.entries.firstOrNull { a.generates == it && it.generates == b }
                    tong == null || ff[tong]!! < tongXu
                }
            } }
        }

        val ganWar = sysWar(bazi.stems, emptyList(), false, 2.0)
        val zhiWar = sysWar(emptyList(), bazi.branches, true, 1.0)
        if (ganWar && zhiWar) {
            risks += LiuQinItem(
                "干支两系统皆相战",
                "天干与地支系统皆前二大行相战且通关无力——五行结构很糟糕（例一边先生水土相战式）。水被强土过分克制则水土混杂——聪明不足糊涂有余；土太强则倔强不听劝告容易做蠢事；官杀强而克身——一生易被人欺侮。一旦触犯法律，官司牢狱难免。",
            )
        }

        // 伤官与官星紧贴相战而无正印制伤官——无视法纪胆大妄为（例二式）
        val shangGuanTie = shangTouIdx.any { si -> guanTouIdx.any { gi -> Math.abs(si - gi) == 1 } }
        if (shangTouIdx.isNotEmpty() && shangGuanTie && !tou(TenGod.ZHENG_YIN)) {
            risks += LiuQinItem(
                "伤官与官星对抗",
                "伤官透干与官星紧贴相战，天干又无正印克制伤官——无视法纪，胆大妄为；官杀又代表法律——一旦触犯法纪就会遭到惩罚。伤官恃才傲物、性格急躁，此种组合容易走上黑道并成为团伙骨干（例二陈先生式）。",
            )
        }

        // 七杀贴身无制——心狠手辣，犯之必罚（例二酉杀贴日干）
        val shaTieRi = bazi.branches[2].let { branchHiddenStems.getValue(it).firstOrNull()?.let { g -> tenGodOfStem(day, g) } }
            ?.let { it == TenGod.QI_SHA } == true
        val shaZhiWuZhi = shaTieRi && bazi.branches.none {
            branchHiddenStems.getValue(it).firstOrNull()?.element == dayEl.generates
        }
        if (shaZhiWuZhi) {
            risks += LiuQinItem(
                "七杀贴身无制",
                "七杀与日干紧贴而无食伤制约——心狠手辣；七杀代表法律，与日干紧贴相克——一旦触犯法纪即遭惩罚（牢狱信息标志，例二式）。",
            )
        }

        // 偏财贴身与日干相战+财支与官支相刑+透食神——为钱财违犯法律铤而走险（例四式）
        val pianCaiGan = bazi.stems.indices.filter { it != 2 }
            .firstOrNull { tenGodOfStem(day, bazi.stems[it]) == TenGod.PIAN_CAI }
        val caiZhi = bazi.branches.firstOrNull { branchHiddenStems.getValue(it).firstOrNull()?.element == dayEl.controls }
        val caiXingGuan = caiZhi != null && (
            sanXingGroups.any { g -> caiZhi in g && bazi.branches.any { it in g && it != caiZhi } } ||
                (setOf(EarthlyBranch.ZI, EarthlyBranch.MAO).contains(caiZhi) &&
                    bazi.branches.any { it in setOf(EarthlyBranch.ZI, EarthlyBranch.MAO) && it != caiZhi })
            )
        if (pianCaiGan != null && Math.abs(pianCaiGan - 2) == 1 && caiZhi != null && Math.abs(bazi.branches.indexOf(caiZhi) - 2) <= 1 &&
            caiXingGuan && tou(TenGod.SHI_SHEN)
        ) {
            risks += LiuQinItem(
                "财星贴身相战为财铤而走险",
                "偏财在干支皆与日干紧贴相战，财星支又与官星支相刑，天干透食神——为钱财会违犯法律铤而走险（例四程先生式：盗用他人商标坐牢并高额罚款）。",
            )
        }

        // 财官强而日干弱——贪污受贿犯法（rule3）
        if (weak && f[dayEl.controls]!! >= 3.0 && f[guanEl]!! >= 3.0) {
            risks += LiuQinItem("财官强而身弱", "财官强而日干弱——易因贪污受贿而犯法（rule3）。")
        }

        // 命局三刑为忌（rule4）
        if (sanXingGroups.any { g -> g.count { it in bazi.branches } >= 3 }) {
            risks += LiuQinItem("命局三刑", "命局三刑组齐为忌——牢狱之虞（rule4）。")
        }

        // 凶神气势最旺（rule11：伤官/枭/劫/杀）
        val xiongShen = listOf(
            bazi.stems.count { it != day && tenGodOfStem(day, it) == TenGod.SHANG_GUAN } + bazi.branches.count {
                branchHiddenStems.getValue(it).firstOrNull()?.let { tenGodOfStem(day, it) == TenGod.SHANG_GUAN } == true
            },
            f[yinEl]!!.toInt(),
            (f[dayEl]!! - bazi.stems.count { it == day }).toInt(),
            f[guanEl]!!.toInt(),
        )
        val total = f.values.sum()
        if (xiongShen.sum() >= total * 0.6 && total > 0) {
            risks += LiuQinItem("凶神气势最旺", "伤官、枭神、劫财、七杀等凶神气势最旺——易犯官灾（rule11）。")
        }

        // ── 岁运应期 ──
        val years = daYun?.yuns.orEmpty().flatMap { yun ->
            yun.liuNian.mapNotNull { ln ->
                val yb = ln.pillar.branch
                val ys = ln.pillar.stem
                val all = bazi.branches + listOf(yun.pillar.branch, yb)
                val triggers = mutableListOf<String>()
                val notes = mutableListOf<String>()
                var severe = false

                // 三刑组齐+官杀旺克身——官司牢狱（例一丁丑式）
                val xingQi = sanXingGroups.any { g -> g.count { it in all } >= 3 }
                val guanDang = ys.element == guanEl || branchHiddenStems.getValue(yb).firstOrNull()?.element == guanEl
                if (xingQi && (guanDang || f[guanEl]!! >= 4.0) && (weak || f[guanEl]!! >= 2.0 * f[dayEl]!!)) {
                    triggers += "三刑组齐（${sanXingGroups.first { g -> g.count { it in all } >= 3 }.joinToString("") { it.chinese }}）+官杀旺而克身"
                    notes += "遭遇官司牢狱之苦（例一丁丑式）"
                    severe = true
                }

                // 官杀值岁旺而克身——小人相害（例一甲戌式）
                if (!severe && guanDang && (weak || f[guanEl]!! >= 2.0 * f[dayEl]!!)) {
                    triggers += "官杀值岁（${ys.chinese}${yb.chinese}）旺而克身"
                    notes += "被小人相害/遭遇官司压力——身心交瘁（例一甲戌/例三甲申乙酉式）"
                }

                // 伤官通根而旺+岁干与原局伤官干冲战——制裁入狱（例二壬午式）
                val shangGen = branchHiddenStems.getValue(yb).firstOrNull()?.element == dayEl.generates &&
                    shangTouIdx.isNotEmpty()
                val chongShangGan = shangTouIdx.any { si -> ganChong.any { p -> ys in p && bazi.stems[si] in p } }
                if (shangGen && chongShangGan) {
                    triggers += "岁支${yb.chinese}通伤官本气之根而旺（行为过分猖獗）+天干${ys.chinese}与伤官${bazi.stems[shangTouIdx.first()].chinese}冲战"
                    notes += "非法行为受到制裁——牢狱之灾（例二壬午式）"
                    severe = true
                }

                // 铤而走险求财招灾（例四庚辰式）：食伤生偏财+岁支冲动+刑害聚+比劫亢旺克财
                val shiShengPianCai = (tenGodOfStem(day, ys) == TenGod.SHI_SHEN || tenGodOfStem(day, ys) == TenGod.SHANG_GUAN) &&
                    pianCaiGan != null && ys.element.generates == bazi.stems[pianCaiGan].element
                val chongZhi = EarthlyBranch.entries[(yb.ordinal + 6) % 12]
                val suiChong = bazi.branches.contains(chongZhi)
                val haiDui = listOf(
                    setOf(EarthlyBranch.ZI, EarthlyBranch.WEI), setOf(EarthlyBranch.CHOU, EarthlyBranch.WU),
                    setOf(EarthlyBranch.YIN, EarthlyBranch.SI), setOf(EarthlyBranch.MAO, EarthlyBranch.CHEN),
                    setOf(EarthlyBranch.SHEN, EarthlyBranch.HAI), setOf(EarthlyBranch.YOU, EarthlyBranch.XU),
                ).any { p -> yb in p && bazi.branches.any { it in p } }
                val xingDui = sanXingGroups.any { g -> g.count { it in all } >= 2 } || caiXingGuan
                if (shiShengPianCai && suiChong && (haiDui || xingDui) && f[dayEl]!! >= f[dayEl.controls]!! + 1.0) {
                    triggers += "食伤生偏财+岁支${yb.chinese}冲动+刑害聚+比劫亢旺（${f[dayEl]}）克财（${f[dayEl.controls]}）"
                    notes += "冒险求财意识突然加强——不法行为惹火烧身，招致牢狱之灾而失去人身自由（例四庚辰盗用商标式）"
                    severe = true
                }

                // 干支两系统皆战（混战）——续灾（例一戊寅式）
                val mGan = sysWar(bazi.stems + listOf(yun.pillar.stem, ys), emptyList(), false, 2.0)
                val mZhi = sysWar(emptyList(), bazi.branches + listOf(yun.pillar.branch, yb), true, 1.0)
                if (mGan && mZhi) {
                    triggers += "全命局干支两系统混战"
                    notes += "继续承受上年之灾（例一戊寅式）"
                }

                if (triggers.isEmpty()) return@mapNotNull null
                LaoYuYear(ln.year, "${ln.year} ${ln.pillar.fullName}", triggers, notes.joinToString("；") + "。")
            }
        }

        return LaoYuResult(
            baseRisks = risks,
            years = years,
            summary = "牢狱之灾多在坏运或好运转败运时降临。原局信息：伤官/比劫与官星对抗、财官强身弱、三刑为忌、" +
                "七杀贴身无制、凶神最旺、干支两系统相战无通关。岁运引发：三刑+官杀旺克身（官司牢狱）、" +
                "官杀值岁克身（小人相害）、伤官通根+干冲战（受制裁入狱）、全局混战（续灾）。",
        )
    }
}
