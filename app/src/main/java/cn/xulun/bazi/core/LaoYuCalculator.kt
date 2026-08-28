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
    private val liuHePairs = listOf(
        setOf(EarthlyBranch.ZI, EarthlyBranch.CHOU), setOf(EarthlyBranch.YIN, EarthlyBranch.HAI),
        setOf(EarthlyBranch.MAO, EarthlyBranch.XU), setOf(EarthlyBranch.CHEN, EarthlyBranch.YOU),
        setOf(EarthlyBranch.SI, EarthlyBranch.SHEN), setOf(EarthlyBranch.WU, EarthlyBranch.WEI),
    )
    private val ganChong = listOf(
        setOf(HeavenlyStem.JIA, HeavenlyStem.GENG), setOf(HeavenlyStem.YI, HeavenlyStem.XIN),
        setOf(HeavenlyStem.BING, HeavenlyStem.REN), setOf(HeavenlyStem.DING, HeavenlyStem.GUI),
    )

    private fun br2Of(b: EarthlyBranch): EarthlyBranch = EarthlyBranch.entries[(b.ordinal + 6) % 12]

    fun analyze(bazi: BaziResult, daYun: DaYunInfo?): LaoYuResult {
        val day = bazi.dayPillar.stem
        val dayEl = day.element
        val f = BingYaoCalculator.forces(bazi)
        val guanEl = Element.entries.first { it.controls == dayEl }
        val yinEl = Element.entries.first { it.generates == dayEl }
        val shiEl = dayEl.generates
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

        // 枭食同柱有官非信息（rule8）；食多变伤——自恃聪明胆大冒险喜与官斗（例八式）
        val xiaoShiTongZhu = bazi.stems.indices.filter { it != 2 }.any { i ->
            val g = tenGodOfStem(day, bazi.stems[i])
            val zhiShi = branchHiddenStems.getValue(bazi.branches[i]).any { tenGodOfStem(day, it) == TenGod.SHI_SHEN }
            val zhiXiao = branchHiddenStems.getValue(bazi.branches[i]).any { tenGodOfStem(day, it) == TenGod.PIAN_YIN }
            (g == TenGod.PIAN_YIN && zhiShi) || (g == TenGod.SHI_SHEN && zhiXiao)
        }
        if (xiaoShiTongZhu) {
            risks += LiuQinItem(
                "枭食同柱",
                "枭神与食神同柱——有官非信息（rule8，例八式）。",
            )
        }
        if (f[shiEl]!! >= 3.0 && bazi.branches.count { it.element == shiEl } >= 2) {
            risks += LiuQinItem(
                "食多变伤·喜与官斗",
                "食多食旺变为伤官之性而得令，支逢冲则愈冲愈旺——伤官心性自恃聪明、胆大冒险、喜与官斗，易目无法纪，下场不佳（例八式）。",
            )
        }

        // 身旺无制+燥气结党（rule6，例十式）
        if (f[dayEl]!! >= 5.0 && f[guanEl]!! <= 1.0) {
            risks += LiuQinItem(
                "身旺无制",
                "比劫当令结党而身旺无制（${dayEl.chinese}力${f[dayEl]}，官杀力${f[guanEl]}）——不拘礼法、藐视他人，易引发官非（rule6，例十式）。",
            )
        }
        if ((f[Element.EARTH]!! + f[Element.FIRE]!!) >= 5.0 && f[Element.WATER]!! <= 0.5) {
            risks += LiuQinItem(
                "燥气结党",
                "局中一片燥气（火土${f[Element.EARTH]!! + f[Element.FIRE]!!}而无水润）——燥土重重，性格冲动、不计后果；燥土脆金、土多埋金（例十式）。",
            )
        }

        // 两枭夺食（双枭透+食神透）+杀枭同柱（例十五式）
        val xiaoTouCount = bazi.stems.indices.filter { it != 2 }
            .count { tenGodOfStem(day, bazi.stems[it]) == TenGod.PIAN_YIN }
        if (xiaoTouCount >= 2 && tou(TenGod.SHI_SHEN)) {
            risks += LiuQinItem(
                "两枭夺食",
                "双枭挂两头而食神居中——两枭夺食，rule8 有牢狱之虞（例十五式：枭神无根则灾轻）。",
            )
        }
        val shaXiaoTongZhu = bazi.stems.indices.filter { it != 2 }.any { i ->
            tenGodOfStem(day, bazi.stems[i]) == TenGod.PIAN_YIN &&
                branchHiddenStems.getValue(bazi.branches[i]).firstOrNull()?.element == guanEl
        }
        if (shaXiaoTongZhu) {
            risks += LiuQinItem(
                "杀枭同柱",
                "七杀与枭神同柱——不愿干正当生意，喜踏偏门，打政策的擦边球（例十五式）。",
            )
        }


        // 三枭双伤夹日干（rule8 夹日型，例十六式）
        val jiaRi = (bazi.stems.getOrNull(0)?.let { tenGodOfStem(day, it) } == TenGod.PIAN_YIN ||
            bazi.stems.getOrNull(1)?.let { tenGodOfStem(day, it) } == TenGod.PIAN_YIN) &&
            bazi.stems.getOrNull(3)?.let { tenGodOfStem(day, it) } == TenGod.SHANG_GUAN
        val jiaRi2 = (bazi.stems.getOrNull(0)?.let { tenGodOfStem(day, it) } == TenGod.SHANG_GUAN ||
            bazi.stems.getOrNull(1)?.let { tenGodOfStem(day, it) } == TenGod.SHANG_GUAN) &&
            bazi.stems.getOrNull(3)?.let { tenGodOfStem(day, it) } == TenGod.PIAN_YIN
        if ((jiaRi || jiaRi2) && xiaoTouCount + bazi.stems.indices.count {
                it != 2 && tenGodOfStem(day, bazi.stems[it]) == TenGod.PIAN_YIN
            } >= 2) {
            risks += LiuQinItem(
                "枭伤夹日干",
                "枭神与伤官（双枭双伤以上）夹日干——rule8 有牢狱之信息（例十六式）。",
            )
        }

        // 身旺印旺财弱——见财起意见利忘义（例十四式）
        if (f[yinEl]!! >= 3.0 && f[dayEl]!! >= 3.0 && f[dayEl.controls]!! <= 2.0) {
            risks += LiuQinItem(
                "身旺印旺财弱",
                "身旺印旺而财弱——往往见财起意、见利忘义；枭印为忌虎视眈眈——阴险狡诈、精于算计之人（例十四式：多行不义，难逃法网）。",
            )
        }

        // 伤官双体有力+官透+身弱——伤官见官定有官灾（例十三式）；三刑入命提示职务多变+触犯刑法
        val shangShuangTi = shangTouIdx.isNotEmpty() &&
            bazi.branches.any { branchHiddenStems.getValue(it).firstOrNull()?.element == dayEl.generates &&
                bazi.branches.count { it.element == dayEl.generates } >= 1 } &&
            bazi.stems.indices.filter { it != 2 }.any { i ->
                tenGodOfStem(day, bazi.stems[i]) == TenGod.SHANG_GUAN &&
                    bazi.branches[i].element == dayEl.generates
            }
        if (shangShuangTi && guanTouIdx.isNotEmpty() && (weak || f[dayEl]!! <= f[guanEl]!!)) {
            risks += LiuQinItem(
                "伤官见官·身弱定官灾",
                "伤官双体（干支一体）临旺而官星得地透干，身弱——伤官见官定有官灾（例十三式：伤官临旺心性聪明胆大敢冒险；日支临财贪财敛财）。三刑入命者职业职务多变，另有触犯刑法之信息。",
            )
        }

        // 三会官杀方局齐+身轻杀旺无制——受牵连/陷害型官非（例十二式）
        val sanHuiGuan = when (guanEl) {
            Element.WOOD -> bazi.branches.containsAll(listOf(EarthlyBranch.YIN, EarthlyBranch.MAO, EarthlyBranch.CHEN))
            Element.FIRE -> bazi.branches.containsAll(listOf(EarthlyBranch.SI, EarthlyBranch.WU, EarthlyBranch.WEI))
            Element.METAL -> bazi.branches.containsAll(listOf(EarthlyBranch.SHEN, EarthlyBranch.YOU, EarthlyBranch.XU))
            Element.WATER -> bazi.branches.containsAll(listOf(EarthlyBranch.HAI, EarthlyBranch.ZI, EarthlyBranch.CHOU))
            else -> false
        }
        if (sanHuiGuan && f[guanEl]!! >= f[dayEl]!!) {
            risks += LiuQinItem(
                "三会杀局攻身",
                "三会官杀方局齐而身轻杀旺无制——官杀旺攻身为忌：胆小懦弱、易受人摆布缺乏主见；身弱官杀制身——一般不会故意犯罪，**最易受别人牵连**；七杀为忌为小人——**易遭栽赃诬陷而遭官非**（例十二式）。",
            )
        }

        // 众劫侮官（比劫众+官支被冲）+羊刃枭神强旺（例十一式）
        val guanZhiChong = bazi.branches.any { tb ->
            branchHiddenStems.getValue(tb).firstOrNull()?.element == guanEl &&
                bazi.branches.any { it == br2Of(tb) }
        }
        if (f[dayEl]!! >= 3.0 && guanZhiChong) {
            risks += LiuQinItem(
                "众劫侮官",
                "比劫众（${dayEl.chinese}力${f[dayEl]}）而官星之支被冲——众劫侮官，藐视法度，违法乱纪；劫财坐刃为忌——易受朋友牵连（例十一式）。",
            )
        }
        if (f[yinEl]!! >= 2.0 && bazi.branches.count { it.element == dayEl } >= 2) {
            risks += LiuQinItem(
                "羊刃枭神强旺",
                "印枭当值而比劫禄刃齐会——羊刃枭神强旺：做事性格急躁、考虑不周、凭一时感情冲动；枭神七杀入命——阴险狡诈，易入黑道、偷鸡摸狗（例十一式）。",
            )
        }

        // 辰戌罗网交加且为忌（rule9）+双伤夹日柱（例九式）
        if (bazi.branches.containsAll(listOf(EarthlyBranch.CHEN, EarthlyBranch.XU)) &&
            (weak || f[shiEl]!! >= f[dayEl]!!)
        ) {
            risks += LiuQinItem(
                "罗网交加",
                "辰戌罗网交加且为忌神——牢狱信息（rule9，例九式：辰为湿土晦火，泄日干之根基）。",
            )
        }
        if (branchHiddenStems.getValue(bazi.branches[1]).firstOrNull()?.element == shiEl &&
            branchHiddenStems.getValue(bazi.branches[3]).firstOrNull()?.element == shiEl &&
            f[shiEl]!! >= 3.0
        ) {
            risks += LiuQinItem(
                "双伤夹日柱",
                "月时支双伤官夹日柱——${shiEl.chinese}旺${yinEl.chinese}折，损去日干原神；身弱伤官旺——喜欢冒险心存侥幸，易引发犯罪（例九式）。",
            )
        }

        // 天干官杀混杂易有官灾+日干墓库在局身弱入墓信息（例七式）
        val guanTou = tou(TenGod.ZHENG_GUAN)
        val shaTou = tou(TenGod.QI_SHA)
        if (guanTou && shaTou) {
            risks += LiuQinItem(
                "天干官杀混杂",
                "天干官杀混杂——易有官灾（例七式：丙辛合而不化自成体系，官杀并透）。",
            )
        }
        val riGanMu = mapOf(
            Element.METAL to EarthlyBranch.CHOU, Element.WOOD to EarthlyBranch.WEI,
            Element.WATER to EarthlyBranch.CHEN, Element.FIRE to EarthlyBranch.XU,
            Element.EARTH to EarthlyBranch.CHEN,
        )[dayEl]
        if (riGanMu != null && bazi.branches.contains(riGanMu)) {
            risks += LiuQinItem(
                "日干墓库在局",
                "日干（${dayEl.chinese}行）之墓库${riGanMu.chinese}在局——如遇身弱，有日干入墓的信息（rule10 之基）。",
            )
        }

        // 局无财而比劫旺——财星出现则众劫抢财，因财色引发官非（rule13，例七式）
        if (f[dayEl.controls]!! <= 0.5 && f[dayEl]!! >= 3.0) {
            risks += LiuQinItem(
                "局无财而比劫旺",
                "原局不见财而身旺——本有发财之象；但财星一现即引发众劫凶性、不顾一切敛财——因财引发官灾（rule13，例七式）。",
            )
        }

        // 官杀混杂而旺制身+伤官见官——原局牢狱信息（例五式）
        val guanShaZhi = bazi.branches.count {
            branchHiddenStems.getValue(it).firstOrNull()?.element == guanEl
        }
        val shangJianGuan = shangTouIdx.isNotEmpty() && guanTouIdx.isNotEmpty()
        if (guanShaZhi >= 2 && shangJianGuan && f[guanEl]!! >= f[dayEl]!!) {
            risks += LiuQinItem(
                "官杀混杂+伤官见官",
                "月日支官杀混杂，干上伤官见官，且官杀旺而制身——原局有牢狱之灾的信息（例五杨先生式）。",
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

        // ── 岁运应期（交运年前 8 个月仍行前运——有效运=本运+前运）──
        val allYuns = daYun?.yuns.orEmpty()
        val years = allYuns.mapIndexedNotNull { yi, yun ->
            val prevYun = allYuns.getOrNull(yi - 1)
            val effYuns = listOfNotNull(prevYun, yun)
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

                // 岁运干伤官见官+岁运支自刑（辰午酉亥同字）+官杀旺克身过重——入狱（例五庚辰式）
                val yunShangSuiGuan =
                    (tenGodOfStem(day, yun.pillar.stem) == TenGod.SHANG_GUAN && tenGodOfStem(day, ys) in setOf(TenGod.ZHENG_GUAN, TenGod.QI_SHA)) ||
                        (tenGodOfStem(day, ys) == TenGod.SHANG_GUAN && tenGodOfStem(day, yun.pillar.stem) in setOf(TenGod.ZHENG_GUAN, TenGod.QI_SHA))
                val ziXing = yb == yun.pillar.branch && yb in setOf(
                    EarthlyBranch.CHEN, EarthlyBranch.WU, EarthlyBranch.YOU, EarthlyBranch.HAI,
                )
                if (yunShangSuiGuan && ziXing && f[guanEl]!! >= f[dayEl]!!) {
                    triggers += "岁运干伤官见官（${yun.pillar.stem.chinese}${ys.chinese}）+岁运支${yb.chinese}${yun.pillar.branch.chinese}自刑加大官杀混杂而克身过重"
                    notes += "此年入狱（例五庚辰式）"
                    severe = true
                }

                // 众劫抢财引发战局（例七戊寅式）：原局无财+比劫旺+岁支财星值岁+刑冲并现
                if (f[dayEl.controls]!! <= 0.5 && f[dayEl]!! >= 3.0 &&
                    branchHiddenStems.getValue(yb).firstOrNull()?.element == dayEl.controls &&
                    (sanXingGroups.any { g -> g.count { it in all } >= 3 } ||
                        bazi.branches.contains(EarthlyBranch.entries[(yb.ordinal + 6) % 12]))
                ) {
                    triggers += "岁支${yb.chinese}（财星）值岁而原局无财+比劫旺（${f[dayEl]}）——群凶劫财，刑冲并现引发战局"
                    notes += "因财之事招官灾重判（例七戊寅年判无期式）"
                    severe = true
                }

                // 三刑刑开日干墓门+官星透干+伤官心性——无法无天入狱（rule10，例八壬戌式）
                if (riGanMu != null && yb == riGanMu && sanXingGroups.any { g -> g.count { it in all } >= 3 } &&
                    tenGodOfStem(day, ys) in setOf(TenGod.ZHENG_GUAN, TenGod.QI_SHA)
                ) {
                    triggers += "岁支${yb.chinese}（日干之墓）逢三刑刑开墓门+岁干${ys.chinese}官星透干（伤官见官）"
                    notes += "无法无天，因不法行为（抢劫之类）而入狱（例八壬戌判两年式）"
                    severe = true
                }

                // 冲根+刑聚+泄身过重——胆大妄为被捕入狱（例九辛未式）
                val xingDui2 = sanXingGroups.any { g -> g.count { it in all } >= 2 }
                val chongRiGen = br2Of(yun.pillar.branch) == bazi.dayPillar.branch || br2Of(yb) == bazi.dayPillar.branch ||
                    bazi.branches.count { it == br2Of(bazi.dayPillar.branch) } >= 2
                if (xingDui2 && chongRiGen && f[shiEl]!! >= f[dayEl]!!) {
                    triggers += "刑聚（${sanXingGroups.first { g -> g.count { it in all } >= 2 }.joinToString("") { it.chinese }}）+冲去日主之根+泄身过重（${shiEl.chinese}${f[shiEl]}晦${dayEl.chinese}${f[dayEl]}）"
                    notes += "胆大妄为做事不计后果——被捕入狱（例九辛未壬辰月判三年式）"
                    severe = true
                }

                // 官杀混杂（运官+局杀）+身旺欺官+伤官当值+岁支与原局同字双刃争合——因财色犯罪入狱（例十丁巳式）
                val yunGuanSha = effYuns.any { yy ->
                    tenGodOfStem(day, yy.pillar.stem) in setOf(TenGod.ZHENG_GUAN, TenGod.QI_SHA)
                } && shaTou
                val yunShiShang = effYuns.any { yy ->
                    branchHiddenStems.getValue(yy.pillar.branch).firstOrNull()
                        ?.let { tenGodOfStem(day, it) in setOf(TenGod.SHI_SHEN, TenGod.SHANG_GUAN) } == true
                }
                val shuangRen = bazi.branches.contains(yb)
                if (yunGuanSha && f[dayEl]!! >= 5.0 && yunShiShang && shuangRen) {
                    triggers += "运干${effYuns.joinToString("") { it.pillar.stem.chinese }}与原局杀透官杀混杂+身旺（${f[dayEl]}）欺官欺法+运支伤官当值胆大妄为+岁支${yb.chinese}与原局同字（双刃合用争合）"
                    notes += "年纪尚轻却因财色犯罪入狱（例十丁巳式）"
                    severe = true
                }

                // 岁运并临为忌神破格+湿土晦火——牢狱之兆（例十一丁丑式）
                if (yun.pillar.stem == ys && yun.pillar.branch == yb && dayEl == Element.FIRE &&
                    (yb == EarthlyBranch.CHOU || yb == EarthlyBranch.CHEN)
                ) {
                    triggers += "岁运并临${ys.chinese}${yb.chinese}为忌神破格——湿土晦火之光"
                    notes += "名誉有损，有牢狱之兆（例十一丁丑式：伙同抢劫仅三百余元亦被判重刑）"
                    severe = true
                }

                // 岁支冲日主之根引动+财滋杀攻身——受牵连入牢（例十二辛未式）；
                // 岁与日柱天合地合绊住官杀——出狱（例十二甲戌式）
                if (sanHuiGuan) {
                    val genZhi = bazi.branches.filter { it.element == dayEl }
                    val chongGen = genZhi.isNotEmpty() && genZhi.any { br2Of(yb) == it }
                    val yunCai = effYuns.any { yy -> yy.pillar.branch.element == dayEl.controls }
                    if (chongGen && yunCai) {
                        triggers += "岁支${yb.chinese}冲日主之根${genZhi.joinToString("") { it.chinese }}引动木土交战+运支财滋杀——财旺生杀、杀旺攻身"
                        notes += "因受朋友牵连而入牢（例十二辛未式——身弱受牵连型，非故意犯罪）"
                        severe = true
                    }
                    val tianHeZhu = Math.floorMod(ys.ordinal - day.ordinal, 10) == 5
                    val diHeRi = liuHePairs.contains(setOf(yb, bazi.dayPillar.branch))
                    if (tianHeZhu && diHeRi) {
                        triggers += "岁干${ys.chinese}合日干${day.chinese}+岁支${yb.chinese}合日支${bazi.dayPillar.branch.chinese}——与日柱天合地合"
                        notes += "绊住官杀攻身之力大减，身旺能胜财官——借流年之力得以重见天日（出狱，例十二甲戌式）"
                    }
                }

                // 印被冲制+伤官越印攻官——身弱伤官见官牢狱难免（例十三戊寅式）；印还原岁——获救出狱
                if (shangShuangTi && guanTouIdx.isNotEmpty()) {
                    val yinZhiZhi = bazi.branches.filter { it.element == yinEl }
                    val chongYin = yinZhiZhi.isNotEmpty() && (br2Of(yb) in yinZhiZhi || bazi.branches.contains(yb) && br2Of(yb) in yinZhiZhi)
                    if (chongYin) {
                        triggers += "岁支${yb.chinese}冲印${yinZhiZhi.joinToString("") { it.chinese }}——印星被制，伤官越过印星攻击正官"
                        notes += "身弱伤官见官，牢狱灾难免（例十三戊寅式：权重位高一夜沦为阶下囚——提前辞职离官场可免）"
                        severe = true
                    } else if (yb.element == yinEl) {
                        triggers += "岁支${yb.chinese}为印星还原"
                        notes += "印星得力获生机——可获救出狱（例十三甲申乙酉式）"
                    }
                }

                // 禄会之年+财星值岁+财印相战——贪污入狱（例十四己卯式）
                val luZhi = when (day) {
                    HeavenlyStem.JIA -> EarthlyBranch.YIN; HeavenlyStem.YI -> EarthlyBranch.MAO
                    HeavenlyStem.BING -> EarthlyBranch.SI; HeavenlyStem.WU -> EarthlyBranch.SI
                    HeavenlyStem.DING -> EarthlyBranch.WU; HeavenlyStem.JI -> EarthlyBranch.WU
                    HeavenlyStem.GENG -> EarthlyBranch.SHEN; HeavenlyStem.XIN -> EarthlyBranch.YOU
                    HeavenlyStem.REN -> EarthlyBranch.HAI; else -> EarthlyBranch.ZI
                }
                if (yb == luZhi && tenGodOfStem(day, ys) in setOf(TenGod.ZHENG_CAI, TenGod.PIAN_CAI) &&
                    f[yinEl]!! >= 3.0
                ) {
                    triggers += "禄会之年（${yb.chinese}为日主之禄）+财星${ys.chinese}轮值+印旺（${f[yinEl]}）——财印相战"
                    notes += "印旺身旺见财起意——因伙同贪污而入狱（例十四己卯式）"
                    severe = true
                }

                // 两X冲两Y而通关在局——拘留轻灾而释放（例十五乙卯式）
                val shuangChong = (bazi.branches + yb).count { it == yb } >= 2 &&
                    (bazi.branches + yun.pillar.branch).count { it == br2Of(yb) } >= 2
                val tongGuan = Element.entries.firstOrNull { t ->
                    br2Of(yb).element.let { strong -> strong.generates == t && t.generates == yb.element }
                }?.let { bazi.branches.count { b -> b.element == it } >= 1 } == true
                if (shuangChong && tongGuan) {
                    triggers += "两${yb.chinese}冲两${br2Of(yb).chinese}——幸有通关之行化解"
                    notes += "只拘留数日（半月）而释放——灾轻（例十五乙卯式）"
                }

                // 原局刑+岁运刑聚——入狱（例十六丙子式）
                val natXing = sanXingGroups.any { g -> g.count { it in bazi.branches } >= 2 } ||
                    (setOf(EarthlyBranch.ZI, EarthlyBranch.MAO).let { zm -> bazi.branches.count { it in zm } >= 2 })
                val suiYunXing = sanXingGroups.any { g -> setOf(yb, yun.pillar.branch).count { it in g } == 2 } ||
                    setOf(yb, yun.pillar.branch) == setOf(EarthlyBranch.ZI, EarthlyBranch.MAO)
                if ((jiaRi || jiaRi2) && natXing && suiYunXing) {
                    triggers += "原局刑（丑戌/子卯类）+岁运支刑聚"
                    notes += "牢狱信息明显——此年入狱（例十六丙子式）"
                    severe = true
                }

                // 通关运支被岁支刑害而毁——战局重现入狱（例十七丙戌式）
                val haiDui3 = listOf(
                    setOf(EarthlyBranch.ZI, EarthlyBranch.WEI), setOf(EarthlyBranch.CHOU, EarthlyBranch.WU),
                    setOf(EarthlyBranch.YIN, EarthlyBranch.SI), setOf(EarthlyBranch.MAO, EarthlyBranch.CHEN),
                    setOf(EarthlyBranch.SHEN, EarthlyBranch.HAI), setOf(EarthlyBranch.YOU, EarthlyBranch.XU),
                ).any { yb in it && yun.pillar.branch in it }
                if (ganWar && zhiWar && haiDui3 &&
                    sanXingGroups.any { g -> g.count { it in all } >= 2 }
                ) {
                    triggers += "运支${yun.pillar.branch.chinese}（通关之力）被岁支${yb.chinese}刑害而毁+${sanXingGroups.first { g -> g.count { it in all } >= 2 }.joinToString("") { it.chinese }}相刑——土水、水火大战重现"
                    notes += "加之官杀混杂——牢狱之苦难免（例十七丙戌农历九月入狱判四年式）"
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
        }.flatten()

        return LaoYuResult(
            baseRisks = risks,
            years = years,
            summary = "牢狱之灾多在坏运或好运转败运时降临。原局信息：伤官/比劫与官星对抗、财官强身弱、三刑为忌、" +
                "七杀贴身无制、凶神最旺、干支两系统相战无通关。岁运引发：三刑+官杀旺克身（官司牢狱）、" +
                "官杀值岁克身（小人相害）、伤官通根+干冲战（受制裁入狱）、全局混战（续灾）。",
        )
    }
}
