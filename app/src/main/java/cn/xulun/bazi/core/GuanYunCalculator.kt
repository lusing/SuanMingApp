package cn.xulun.bazi.core

import cn.xulun.bazi.core.LiuQinXiJi.categoryOf
import cn.xulun.bazi.core.LiuQinXiJi.elementOf
import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DaYunInfo
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.GodCategory
import cn.xulun.bazi.model.GuanYunKind
import cn.xulun.bazi.model.GuanYunResult
import cn.xulun.bazi.model.GuanYunYear
import cn.xulun.bazi.model.GuanYunYun
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.LiuQinItem
import cn.xulun.bazi.model.TenGod
import cn.xulun.bazi.model.branchHiddenStems
import cn.xulun.bazi.model.tenGodOfStem

/**
 * 官运引擎（.caiyun-reference.md 第七章）。以官星代表官运——喜忌与力量为升贬依据：
 * 官星为用神且力量足够之流年=晋升机遇；为忌且力足=贬职丢官。
 *
 * 原局层：官星辨识（透干/藏支/不现）+ 贵命断语（杀印相生/食神制杀/伤官伤尽/伤官佩印/
 * 财官印全/从财官/无官做官三格——此等格局最忌官星出现，出现之时往往贬职罢官）。
 *
 * 流年层：
 * - **晋升**：①官星引动（六式：冲/合/三合/刑/同类/当值）+全命局和局+官为用；
 *   ②流年财官印依次相生（财生官官生身）；③印星用神格逢伤官年构成伤官佩印；
 *   ④身弱官杀重逢比劫帮身年胜官杀。
 * - **贬职**：①岁运与日/年柱天克地冲+三刑入命（身弱根伤尤甚）；②从格官星还原遭克
 *   （官从火势反有官当，官值年欲叛遭重克——例六王先生式）；③官星为忌力足（身弱杀重又逢）。
 * - **调动**：冲/刑/马星/伤官透——五行力量重大改变主「动」（潘女士式工作变动）。
 */
object GuanYunCalculator {

    private val liuHePairs = setOf(
        setOf(EarthlyBranch.ZI, EarthlyBranch.CHOU), setOf(EarthlyBranch.YIN, EarthlyBranch.HAI),
        setOf(EarthlyBranch.MAO, EarthlyBranch.XU), setOf(EarthlyBranch.CHEN, EarthlyBranch.YOU),
        setOf(EarthlyBranch.SI, EarthlyBranch.SHEN), setOf(EarthlyBranch.WU, EarthlyBranch.WEI),
    )
    private val sanHeGroups = listOf(
        setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU),
        setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU),
        setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN),
        setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI),
    )
    private val sanXingGroups = listOf(
        setOf(EarthlyBranch.YIN, EarthlyBranch.SI, EarthlyBranch.SHEN),
        setOf(EarthlyBranch.CHOU, EarthlyBranch.XU, EarthlyBranch.WEI),
    )
    private val taoHuaOf = mapOf(
        EarthlyBranch.SHEN to EarthlyBranch.YOU, EarthlyBranch.ZI to EarthlyBranch.YOU,
        EarthlyBranch.CHEN to EarthlyBranch.YOU, EarthlyBranch.YIN to EarthlyBranch.MAO,
        EarthlyBranch.WU to EarthlyBranch.MAO, EarthlyBranch.XU to EarthlyBranch.MAO,
        EarthlyBranch.SI to EarthlyBranch.WU, EarthlyBranch.YOU to EarthlyBranch.WU,
        EarthlyBranch.CHOU to EarthlyBranch.WU, EarthlyBranch.HAI to EarthlyBranch.ZI,
        EarthlyBranch.MAO to EarthlyBranch.ZI, EarthlyBranch.WEI to EarthlyBranch.ZI,
    )
    private val ganChong = listOf(
        setOf(HeavenlyStem.JIA, HeavenlyStem.GENG), setOf(HeavenlyStem.YI, HeavenlyStem.XIN),
        setOf(HeavenlyStem.BING, HeavenlyStem.REN), setOf(HeavenlyStem.DING, HeavenlyStem.GUI),
    )

    private fun br2(b: EarthlyBranch): EarthlyBranch = EarthlyBranch.entries[(b.ordinal + 6) % 12]

    fun analyze(bazi: BaziResult, daYun: DaYunInfo): GuanYunResult {
        val day = bazi.dayPillar.stem
        val dayEl = day.element
        val d = LiuQinXiJi.derive(bazi)
        val forces = BingYaoCalculator.forces(bazi)
        val engVerdict = StrengthEngine.analyze(bazi).verdict
        val guanEl = elementOf(GodCategory.GUAN, dayEl)
        val guanForce = forces[guanEl]!!

        // ── 官星辨识 ──
        val guanStems = bazi.stems.indices.filter {
            it != 2 && (tenGodOfStem(day, bazi.stems[it]) == TenGod.ZHENG_GUAN || tenGodOfStem(day, bazi.stems[it]) == TenGod.QI_SHA)
        }
        val guanHidden = bazi.branches.withIndex().filter { (_, br) ->
            branchHiddenStems.getValue(br).any { it.element == guanEl }
        }
        val officerNote = buildString {
            append("官星（官杀·${guanEl.chinese}行力${guanForce}）：")
            append(
                if (guanStems.isNotEmpty()) "透干" + guanStems.joinToString("、") { "${bazi.stems[it].chinese}(${listOf("年干", "月干", "日干", "时干")[it]})" } +
                    (if (guanHidden.isNotEmpty()) "兼藏支" else "")
                else if (guanHidden.isNotEmpty()) "藏于" + guanHidden.joinToString("、") { "${it.value.chinese}" }
                else "命中不现——秀气流通生化有情者亦可得官（最忌官星出现之时）"
            )
            append("。${if (GodCategory.GUAN in d.fav) "官为喜用——仕途可为。" else if (GodCategory.GUAN in d.unf) "官为忌——升沉随岁运组合。" else "官为闲神——仕途平淡。"}")
        }

        // ── 贵命断语（原局层）──
        val noble = mutableListOf<LiuQinItem>()
        val shiForce = forces[elementOf(GodCategory.SHI, dayEl)]!!
        val yinForce = forces[elementOf(GodCategory.YIN, dayEl)]!!
        if (guanStems.isNotEmpty() && yinForce >= 2.0 && GodCategory.GUAN in d.fav) {
            noble += LiuQinItem("杀印相生", "官杀透而有印化（杀印相生）为喜用——有贵之命，武职文职皆宜（例七方先生式）。")
        }
        if (engVerdict == DayStrength.STRONG && shiForce >= 2.5 && guanStems.isEmpty()) {
            noble += LiuQinItem(
                "伤官伤尽",
                "身旺伤官有力而不见官星（伤官伤尽）——以出众能力得信获官，握权柄；**最忌官星出现，出现之时往往贬职罢官**。",
            )
        }
        if (shiForce >= 2.0 && yinForce >= 1.5) {
            noble += LiuQinItem(
                "伤官佩印",
                "伤官（智慧能力）配印（原则仁慈）——具备做官素质，为贵格；机遇在食伤得力之年（能力展现——例三孙/例四卢式）。",
            )
        }
        if (setOf(GodCategory.CAI, GodCategory.GUAN, GodCategory.YIN).all { forces[elementOf(it, dayEl)]!! >= 1.0 }) {
            noble += LiuQinItem("财官印全", "财官印全——清纯显贵，国家柱石之才。")
        }
        if (guanStems.isEmpty() && guanHidden.isEmpty() && bingCongGe(bazi)) {
            noble += LiuQinItem(
                "从财官",
                "命从财官（格局清纯）——贵格（例十三厅长式）；运降原局层次则不可大贵（例十五式）。",
            )
        }
        if (forces[dayEl]!! >= 3.0 && shiForce >= 2.5 && guanForce >= 1.5) {
            noble += LiuQinItem("身杀两停·食神制杀", "身杀两停而食神制杀——英雄独压万人，掌权之命。")
        }

        val yuns = daYun.yuns.filter { it.startAge >= 16 }.map { yun ->
            val years = yun.liuNian.map { ln ->
                val ys = ln.pillar.stem
                val yb = ln.pillar.branch
                val age = ln.year - bazi.birthDateTime.year
                val triggers = mutableListOf<String>()

                // 官星支与流年支作用（六式引动）
                val guanBranches = bazi.branches.filter { branchHiddenStems.getValue(it).any { h -> h.element == guanEl }
                }
                val yinDong = mutableListOf<String>()
                guanBranches.forEach { gb ->
                    when {
                        br2(yb) == gb -> yinDong += "${yb.chinese}冲${gb.chinese}——官星被冲引动"
                        // 同字非合（亥合亥/辰合辰乃自合谬误——同支再现为「当值」另判）
                        yb != gb && liuHePairs.any { yb in it && gb in it } -> yinDong += "${yb.chinese}合${gb.chinese}——官星被合引动"
                        yb != gb && sanHeGroups.any { yb in it && gb in it } -> yinDong += "${yb.chinese}与${gb.chinese}三合——官局成引"
                        yb != gb && sanXingGroups.any { yb in it && gb in it } -> yinDong += "${yb.chinese}刑${gb.chinese}——官星被刑引动"
                        yb.element == guanEl && gb.element == guanEl -> yinDong += "${yb.chinese}同类引动${gb.chinese}"
                    }
                }
                if (branchHiddenStems.getValue(yb).firstOrNull()?.element == guanEl) yinDong += "${yb.chinese}官星当值"
                if (tenGodOfStem(day, ys) == TenGod.ZHENG_GUAN || tenGodOfStem(day, ys) == TenGod.QI_SHA) yinDong += "${ys.chinese}官星透干"

                val guanYinDong = yinDong.isNotEmpty()
                val guanXi = GodCategory.GUAN in d.fav

                // 官杀混杂无合解（例八1994甲戌式——原局透官而岁透异性杀/反之，两不相绊则仕途滞塞；
                // 一方被合绊则去留取清——甲己合绊杀而岁官透反清纯，不入此档）
                val guanShaZa = run {
                    val yuanTou = bazi.stems.filterIndexed { i, s ->
                        i != 2 && (tenGodOfStem(day, s) == TenGod.ZHENG_GUAN || tenGodOfStem(day, s) == TenGod.QI_SHA)
                    }
                    val suiGuan = tenGodOfStem(day, ys)
                    val suiTouGuan = suiGuan == TenGod.ZHENG_GUAN || suiGuan == TenGod.QI_SHA
                    if (yuanTou.isEmpty() || !suiTouGuan) return@run false
                    val yangSet = yuanTou.map { it.yang }.toSet()
                    val hun = yangSet.contains(!ys.yang)
                    if (!hun) return@run false
                    // 干被五合绊住（他柱之干或运干为五合对；日干不算——官合日主反为有情）
                    fun heBan(s: HeavenlyStem): Boolean =
                        listOf(bazi.stems[0], bazi.stems[1], bazi.stems[3], yun.pillar.stem).any { t ->
                            t != s && (t.ordinal - s.ordinal + 10) % 10 == 5
                        }
                    !heBan(ys) && yuanTou.none { heBan(it) }
                }

                val kind: GuanYunKind
                val text: String
                when {
                    // 贬职:天克地冲日柱或年柱+三刑入命(身弱尤甚)
                    ganChong.any { p -> ys in p && day in p } &&
                        (br2(yb) == bazi.branches[2] || br2(yb) == bazi.branches[0]) -> {
                        triggers += "${ys.chinese}${day.chinese}天干相克+${yb.chinese}冲${if (br2(yb) == bazi.branches[2]) "日支" else "年支"}——天克地冲"
                        kind = GuanYunKind.BIAN_ZHI
                        text = "岁与${if (br2(yb) == bazi.branches[2]) "日柱" else "年柱"}天克地冲" +
                            (if (sanXingGroups.any { g -> bazi.branches.count { it in g } + (if (yb in g) 1 else 0) >= 3 }) "而三刑入命" else "") +
                            "——根气动摇${if (engVerdict == DayStrength.WEAK) "（身弱尤甚）" else ""}：贬职罢官之应，防官非（例六王先生壬申年免职式）。"
                    }
                    // 贬职:从格官星还原遭克(官星微极而从,官值年欲叛遭重克)
                    guanYinDong && guanForce <= 1.0 && GodCategory.GUAN !in d.fav &&
                        Element.entries.first { it.controls == guanEl }.let { ke -> forces[ke]!! >= 2.5 } -> {
                        triggers += yinDong
                        kind = GuanYunKind.BIAN_ZHI
                        text = "原局官星微极而从势（从则无克伐之忧，顺势者昌反有官当），今官星值岁运欲还原叛势——力弱触发旺神之怒反遭重克：贬职罢官之年（例六辛官从火、申年通根叛火被免职式）。"
                    }
                    // 滞塞:官杀混杂无合解（例八1994甲戌式——甲透无合解混杂，与领导不融洽）
                    guanShaZa -> {
                        triggers += "${ys.chinese}透与原局官杀混杂而无合解"
                        kind = GuanYunKind.PING
                        text = "岁干${ys.chinese}（官杀）透而与原局官杀阴阳并见、无合绊取清——官杀混杂之年：" +
                            "仕途滞塞，防与领导不睦、同僚倾轧，宜低调度过（例八1994甲戌式）。"
                    }
                    // 晋升:官星引动+官为用+和局(流年官星支当值/被引动,全命局干支相生或对峙不战)
                    guanYinDong && guanXi && heJu(bazi, yun, ln) -> {
                        triggers += yinDong
                        kind = GuanYunKind.JIN_SHENG
                        text = "官星被引动（${yinDong.joinToString("；")}）而全命局构成和局——官星引动之年：" +
                            "晋升机遇，加官晋级（例八林先生癸酉酉冲卯对峙成和式；机遇之年宜把握）。"
                    }
                    // 晋升:财官印依次相生(流年财官印岁运组合)
                    shengCheng(bazi, dayEl, ln) -> {
                        triggers += "流年参与财官印依次相生"
                        kind = GuanYunKind.JIN_SHENG
                        text = "流年参与构成财生官、官生印、印生身之依次相生——生化有情：升迁之喜" +
                            "（例十七丁丑癸酉运财官印相生升副处、例二十癸酉申酉戌合金式）。"
                    }
                    // 晋升:印星用神格逢伤官年——伤官佩印
                    GodCategory.YIN in d.fav &&
                        (tenGodOfStem(day, ys) == TenGod.SHANG_GUAN || tenGodOfStem(day, ys) == TenGod.SHI_SHEN) && yinForce >= 1.5 -> {
                        triggers += "${ys.chinese}伤食透年+印星用神"
                        kind = GuanYunKind.JIN_SHENG
                        text = "印星为用之神而逢伤官（食神）流年——构成伤官佩印之格：以人品表现赢得提拔，晋升机遇" +
                            "（例九范女士乙亥年式；官星不现之格尤为应验）。"
                    }
                    // 贬职:官为忌力足(身弱杀重又逢官杀年)
                    GodCategory.GUAN in d.unf && guanYinDong && engVerdict == DayStrength.WEAK -> {
                        triggers += yinDong
                        kind = GuanYunKind.BIAN_ZHI
                        text = "身弱而官杀为忌力足，岁运又逢官星引动——克身过重：贬职降级之应，亦防官灾。"
                    }
                    // 调动:冲/刑/马星/伤官透——五行力量重大改变主动
                    (br2(yb).let { cb -> bazi.branches.contains(cb) } ||
                        sanXingGroups.any { g -> bazi.branches.count { it in g } + (if (yb in g) 1 else 0) >= 3 } ||
                        taoHuaOf[bazi.branches[0]] == yb || taoHuaOf[bazi.branches[2]] == yb) &&
                        (tenGodOfStem(day, ys) == TenGod.SHANG_GUAN || tenGodOfStem(day, ys) == TenGod.SHI_SHEN ||
                            br2(yb).let { cb -> bazi.branches.contains(cb) }) -> {
                        val dongYin = mutableListOf<String>()
                        if (br2(yb).let { cb -> bazi.branches.contains(cb) }) dongYin += "${yb.chinese}冲原局${br2(yb).chinese}"
                        if (sanXingGroups.any { g -> bazi.branches.count { it in g } + (if (yb in g) 1 else 0) >= 3 }) dongYin += "${yb.chinese}参与三刑"
                        if (taoHuaOf[bazi.branches[0]] == yb || taoHuaOf[bazi.branches[2]] == yb) dongYin += "${yb.chinese}马星当值"
                        if (tenGodOfStem(day, ys) == TenGod.SHANG_GUAN || tenGodOfStem(day, ys) == TenGod.SHI_SHEN) dongYin += "${ys.chinese}伤食透干"
                        triggers += dongYin
                        kind = GuanYunKind.DIAO_DONG
                        text = "动之信息（${dongYin.joinToString("、")}）——五行力量重大改变：工作变动/职务调整/迁移出国" +
                            "（潘女士式——动非独冲马，刑与伤透皆主变动）。"
                    }
                    else -> {
                        kind = GuanYunKind.PING
                        text = "官星无显著引动与和战之变——仕途平顺之年。"
                    }
                }

                GuanYunYear(ln.year, "${ln.year} ${ys.chinese}${yb.chinese}", kind, triggers, text)
            }
            GuanYunYun(yun.pillar, "${yun.pillar.fullName} ${yun.startYear}-${yun.endYear}", years)
        }

        val summary = "官星为用且力足之流年=晋升机遇（官星六式引动+和局/财官印相生/伤官佩印年）；" +
            "为忌力足=贬职（天克地冲三刑/从格官星还原遭克——例六式）；冲刑马星伤透主调动之动。"
        return GuanYunResult(officerNote = officerNote, nobleItems = noble, yuns = yuns, summary = summary)
    }

    /** 全命局和局：流年介入后前二大行相生或通关或对峙不战（简判——克战对得通关之行力足）。 */
    private fun heJu(bazi: BaziResult, yun: cn.xulun.bazi.model.DaYun, ln: cn.xulun.bazi.model.LiuNian): Boolean {
        val f = BingYaoCalculator.forces(bazi).toMutableMap()
        f[yun.pillar.stem.element] = f[yun.pillar.stem.element]!! + 1
        f[yun.pillar.branch.element] = f[yun.pillar.branch.element]!! + 1
        f[ln.pillar.stem.element] = f[ln.pillar.stem.element]!! + 1
        f[ln.pillar.branch.element] = f[ln.pillar.branch.element]!! + 1
        val sorted = Element.entries.filter { f[it]!! >= 1.0 }.sortedByDescending { f[it]!! }
        if (sorted.size < 2) return true
        val a = sorted[0]; val b2 = sorted[1]
        if (a.generates == b2 || b2.generates == a) return true
        val strong = if (a.controls == b2) a else b2
        val weak = if (strong == a) b2 else a
        val tong = Element.entries.firstOrNull { strong.generates == it && it.generates == weak }
        return tong != null && f[tong]!! >= 1.5
    }

    /** 流年参与财官印依次相生（财生官官生身）。 */
    private fun shengCheng(bazi: BaziResult, dayEl: Element, ln: cn.xulun.bazi.model.LiuNian): Boolean {
        val caiEl = dayEl.controls
        val guanEl = Element.entries.first { it.controls == dayEl }
        val yEls = setOf(ln.pillar.stem.element, ln.pillar.branch.element)
        val f = BingYaoCalculator.forces(bazi).toMutableMap()
        f[ln.pillar.stem.element] = f[ln.pillar.stem.element]!! + 1
        f[ln.pillar.branch.element] = f[ln.pillar.branch.element]!! + 1
        // 财官印有源且流年补其一
        return f[caiEl]!! >= 1.0 && f[guanEl]!! >= 0.5 && yEls.any { it == caiEl || it == guanEl } && heJuSimple(f)
    }

    private fun heJuSimple(f: Map<Element, Double>): Boolean {
        val sorted = Element.entries.filter { f[it]!! >= 1.0 }.sortedByDescending { f[it]!! }
        if (sorted.size < 2) return true
        val a = sorted[0]; val b2 = sorted[1]
        if (a.generates == b2 || b2.generates == a) return true
        val strong = if (a.controls == b2) a else b2
        val weak = if (strong == a) b2 else a
        val tong = Element.entries.firstOrNull { strong.generates == it && it.generates == weak }
        return tong != null && f[tong]!! >= 1.0
    }

    private fun bingCongGe(bazi: BaziResult): Boolean = BingYaoCalculator.analyze(bazi).congGe != null
}
