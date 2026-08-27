package cn.xulun.bazi.core

import cn.xulun.bazi.core.LiuQinXiJi.category
import cn.xulun.bazi.core.LiuQinXiJi.elementOf
import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.DaYunInfo
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.GodCategory
import cn.xulun.bazi.model.HunLianKind
import cn.xulun.bazi.model.HunYinResult
import cn.xulun.bazi.model.HunYinYear
import cn.xulun.bazi.model.HunYinYun
import cn.xulun.bazi.model.LiuQinItem
import cn.xulun.bazi.model.TenGod
import cn.xulun.bazi.model.branchHiddenStems
import cn.xulun.bazi.model.tenGodOfStem

/**
 * 婚姻完整引擎（.hunyin-reference.md）。男以财星为配偶星，女以官杀为配偶星；
 * 日支为配偶宫。
 *
 * 两层：
 * 1. 原局断语——日支（配偶宫）十神喜忌定配偶性情相貌（日坐X为喜/忌系列）、
 *    日支四正四生四库、禄桃花临宫、官杀混杂/伤官克夫/羊刃克妻/财衰墓绝、
 *    日时刑冲、四柱纯阳等（男/女分表）。
 * 2. 婚恋流年（全量铺开）——配偶宫被引动（冲/合/刑/暗合/**当值加临**/生助比和）
 *    或配偶星被引动（透干/当值）即婚恋信息年；桃花当值主婚庆；宫逢冲刑主动荡。
 *    性质倾向：星透+宫动→恋爱；宫加临→实质婚同居；桃花+动→婚庆；冲刑→婚变。
 *
 * 婚龄口径：恋爱多发 16-30，法定婚龄男 22 女 20，适婚重点女 20-31/男 22-31。
 * 喜忌由 [LiuQinXiJi] 教材口径派生。
 */
object HunYinCalculator {

    /** 日支起桃花（咸池）：申子辰→酉 寅午戌→卯 巳酉丑→午 亥卯未→子。 */
    private fun taoHuaOf(b: EarthlyBranch): EarthlyBranch = when (b) {
        EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN -> EarthlyBranch.YOU
        EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU -> EarthlyBranch.MAO
        EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU -> EarthlyBranch.WU
        else -> EarthlyBranch.ZI // 亥卯未
    }

    private val liuHePairs = setOf(
        setOf(EarthlyBranch.ZI, EarthlyBranch.CHOU), setOf(EarthlyBranch.YIN, EarthlyBranch.HAI),
        setOf(EarthlyBranch.MAO, EarthlyBranch.XU), setOf(EarthlyBranch.CHEN, EarthlyBranch.YOU),
        setOf(EarthlyBranch.SI, EarthlyBranch.SHEN), setOf(EarthlyBranch.WU, EarthlyBranch.WEI),
    )
    private val anHePairs = setOf(
        setOf(EarthlyBranch.YIN, EarthlyBranch.CHOU), setOf(EarthlyBranch.WU, EarthlyBranch.HAI),
        setOf(EarthlyBranch.ZI, EarthlyBranch.SI), setOf(EarthlyBranch.MAO, EarthlyBranch.SHEN),
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

    fun analyze(bazi: BaziResult, gender: Gender, daYun: DaYunInfo): HunYinResult {
        val day = bazi.dayPillar.stem
        val dayEl = day.element
        val dayBranch = bazi.dayPillar.branch
        val male = gender == Gender.MALE
        val d = LiuQinXiJi.derive(bazi)
        val forces = BingYaoCalculator.forces(bazi)
        val spouseCat = if (male) GodCategory.CAI else GodCategory.GUAN
        val spouseEl = elementOf(spouseCat, dayEl)

        // ── 配偶星辨识 ──
        val starStems = bazi.stems.indices.filter {
            it != 2 && tenGodOfStem(day, bazi.stems[it]).category() == spouseCat
        }
        val starHidden = bazi.branches.withIndex().filter { (i, br) ->
            i != 2 && branchHiddenStems.getValue(br).any { it.element == spouseEl }
        }
        val spouseStarNote = buildString {
            append("配偶星（${if (male) "财星" else "官杀"}·${spouseEl.chinese}行）：")
            append(
                if (starStems.isNotEmpty()) "透干" + starStems.joinToString("、") { "${bazi.stems[it].chinese}(${listOf("年干", "月干", "日干", "时干")[it]})" } +
                    (if (starHidden.isNotEmpty()) "，兼藏支" + starHidden.joinToString("、") { "${it.value.chinese}(${listOf("年支", "月支", "日支", "时支")[it.index]})" } else "")
                else if (starHidden.isNotEmpty()) "藏于" + starHidden.joinToString("、") { "${it.value.chinese}(${listOf("年支", "月支", "日支", "时支")[it.index]})" }
                else "命中不现——姻缘待运而动"
            )
        }

        // ── 配偶宫（日支） ──
        val palaceGod = tenGodOfStem(day, branchHiddenStems.getValue(dayBranch).first())
        val diZhi = DiZhiRelationCalculator.analyze(bazi)
        val palaceChong = diZhi.chongList.firstOrNull { it.location.contains("日支") }?.description
        val palaceHe = HeHuaCalculator.analyze(bazi).branchLiuHeList.firstOrNull { it.location.contains("日支") }?.description
        val palaceXing = diZhi.xingList.firstOrNull { it.location.contains("日支") }?.description
        val palaceNote = buildString {
            append("配偶宫（日支${dayBranch.chinese}·${palaceGod.chinese}）")
            if (palaceChong != null) append("；逢${palaceChong}")
            if (palaceHe != null) append("；${palaceHe}")
            if (palaceXing != null) append("；逢${palaceXing}")
        }

        // ── 原局断语 ──
        val fav = mutableListOf<LiuQinItem>()
        val unfav = mutableListOf<LiuQinItem>()
        val palaceCat = palaceGod.category()
        val palaceXi = palaceCat in d.fav
        val palaceJi = palaceCat in d.unf

        // 日坐X系列（喜/忌双面，男女各自文案）
        if (palaceXi) {
            val t = if (male) "日坐${palaceGod.chinese}为喜用" else "日坐${palaceGod.chinese}为喜用"
            val body = when (palaceGod) {
                TenGod.ZHENG_CAI, TenGod.PIAN_CAI ->
                    if (male) "妻持家内助，或因妻而富。" else "日支偏财旺为喜用——夫必富。"
                TenGod.ZHENG_YIN -> if (male) "妻贤惠敦厚。" else "日支官印旺而为喜用——夫必贵。"
                TenGod.PIAN_YIN -> if (male) "妻精明能干。" else "日支偏印为喜用——夫多谋略。"
                TenGod.ZHENG_GUAN -> if (male) "妻端庄矜持。" else "日坐正官为喜用——夫端正有责。"
                TenGod.QI_SHA -> if (male) "妻刚直伶俐。" else "日坐七杀为喜用——夫刚毅果决。"
                TenGod.SHANG_GUAN -> if (male) "妻聪明爽朗。" else "日坐伤官为喜用——夫多才而性傲。"
                TenGod.SHI_SHEN ->
                    if (male) "食神为喜用——食偏旺妻体略胖性情随和喜歌舞；偏弱妻秀气端庄稳重含蓄。" else "日支食旺为喜用——夫诚信有德。"
                TenGod.BI_JIAN, TenGod.JIE_CAI ->
                    if (male) "妻精于算计，生财有方。" else "日支比劫为喜用——夫为自立自强之人。"
            }
            fav += LiuQinItem(t, body)
            if (palaceCat == spouseCat) {
                fav += LiuQinItem(
                    "配偶星坐配偶宫",
                    if (male) "日坐财星为喜用——妻持家内助，因妻而富；日支临用神，得妻内助。" else "用神临日支——必嫁良夫。",
                )
            }
        }
        if (palaceJi) {
            val body = when (palaceGod) {
                TenGod.QI_SHA -> if (male) "妻性刚暴躁。" else "日坐七杀为忌——夫性暴而婚多磨。"
                TenGod.BI_JIAN, TenGod.JIE_CAI -> if (male) "身坐比劫为忌——几度新郎之象。" else "日坐比劫为忌——争夫之象。"
                TenGod.PIAN_YIN -> if (male) "日支偏印为忌——妻多疑难处。" else "日坐枭神为忌——夫妻情薄。"
                else -> if (male) "配偶宫之神为忌——婚姻难得其助。" else "配偶宫之神为忌——婚姻难得其助。"
            }
            unfav += LiuQinItem("日坐${palaceGod.chinese}为忌", body)
        }

        // 日支四正/四生/四库——配偶形貌（男命看妻貌，须结合财星）
        val siZheng = setOf(EarthlyBranch.ZI, EarthlyBranch.WU, EarthlyBranch.MAO, EarthlyBranch.YOU)
        val siKu = setOf(EarthlyBranch.CHEN, EarthlyBranch.XU, EarthlyBranch.CHOU, EarthlyBranch.WEI)
        val maoNote = when {
            dayBranch in siZheng -> "日支临四正（子午卯酉）——配偶多貌美秀气。"
            dayBranch in siKu -> "日支临四库（辰戌丑未）——配偶貌平常而重实际（须结合配偶星看）。"
            else -> "日支临四生（寅申巳亥）——配偶稳重敦厚。"
        }
        fav += LiuQinItem("日支形貌", maoNote)

        // 禄临妻宫 / 桃花临宫
        if (palaceXi && ShenShaCalculator.analyze(bazi).items.any { it.name == "禄神" && it.branch == dayBranch }) {
            fav += LiuQinItem("禄临配偶宫", "禄临妻宫为喜用——${if (male) "主娶富贵人家之女" else "嫁妆丰厚之家"}。")
        }
        val taoBranch = taoHuaOf(dayBranch)
        if (dayBranch == taoBranch || bazi.branches.count { it == taoBranch } > 0) {
            if (bazi.branches.contains(taoBranch)) {
                val idx = bazi.branches.indexOf(taoBranch)
                val label = listOf("年支", "月支", "日支", "时支")[idx]
                val item = LiuQinItem(
                    "桃花临$label",
                    if (spouseCat == GodCategory.CAI) "财旺临桃花——配偶${if (male) "妻多情貌美" else ""}；桃花旺主异性缘厚。" else "官带桃花——${if (!male) "夫俊美多艺" else "配偶貌美"}。",
                )
                if (palaceCat in d.fav || spouseCat in d.fav) fav += item else unfav += LiuQinItem("桃花临$label", "桃花临命——多情浪漫，防因色生是非。")
            }
        }
        // 身旺财旺（男）/ 官旺（女）
        if (male && forces[spouseEl]!! >= 3.0 && spouseCat in d.fav) {
            fav += LiuQinItem("财旺为喜", "身强财旺为喜用——妻貌美，或异性缘厚；运逢财旺三合六合之年多有花烛之喜。")
        }
        if (!male && forces[spouseEl]!! >= 3.0 && spouseCat in d.fav) {
            fav += LiuQinItem("官旺为喜", "官杀旺为喜用——夫贵；运逢财官生旺三合六合之年多有红鸾之庆。")
        }
        // 财多身弱（男）依从妻言
        if (male && forces[spouseEl]!! >= 3.0 && spouseCat in d.unf) {
            unfav += LiuQinItem("财多身弱", "财多身弱——依从妻言，妻执家政；身弱财旺妻夺夫权。")
        }
        // 一官清透（女）
        if (!male) {
            val guanStems = bazi.stems.indices.filter {
                it != 2 && (tenGodOfStem(day, bazi.stems[it]) == TenGod.ZHENG_GUAN || tenGodOfStem(day, bazi.stems[it]) == TenGod.QI_SHA)
            }
            if (guanStems.size == 1 && spouseCat in d.fav) {
                fav += LiuQinItem("官星清透", "一${tenGodOfStem(day, bazi.stems[guanStems.first()]).chinese}清透为喜用而无混杂——必嫁良夫，可享荣华富贵。")
            }
            if (guanStems.size >= 2 && spouseCat in d.unf) {
                unfav += LiuQinItem("官杀混杂", "官杀混杂旺而为忌——多夫之象，婚路波折。")
            }
            // 伤官旺忌克夫
            if (forces[elementOf(GodCategory.SHI, dayEl)]!! >= 3.0 && GodCategory.SHI in d.unf) {
                unfav += LiuQinItem("伤官旺忌", "伤官旺而为忌——克夫；无财引化而官遭克陷者尤甚。日坐伤官者好骂夫。")
            }
            // 比劫争夫
            if (forces[dayEl]!! >= 3.0 && spouseCat in d.unf && guanStems.isEmpty()) {
                unfav += LiuQinItem("比劫争夫", "比劫旺透而官星衰弱——争夫之象。")
            }
        }
        // 男:财衰墓绝/空亡/比劫旺
        if (male) {
            if (forces[spouseEl]!! <= 1.0 && forces[dayEl]!! >= 3.0) {
                unfav += LiuQinItem("财衰比劫旺", "财衰而比劫旺——一妻难白头；身旺比劫旺为忌克妻耗财。")
            }
            val kong = KongWangCalculator.analyze(bazi)
            val starPillars = bazi.branches.indices.filter { branchHiddenStems.getValue(bazi.branches[it]).any { h -> h.element == spouseEl } }
            if (forces[spouseEl]!! <= 1.5 && starPillars.any { kong.isKong(it) }) {
                unfav += LiuQinItem("配偶星空亡", "配偶星休囚又落空亡——夫妇难以白头偕老。")
            }
            // 身旺羊刃忌克妻
            if (forces[dayEl]!! >= 3.0 && ShenShaCalculator.analyze(bazi).items.any { it.name == "羊刃" } && GodCategory.BI in d.unf) {
                unfav += LiuQinItem("身旺羊刃", "身旺坐羊刃为忌——克妻之命；柱逢羊刃为忌者再婚之人。")
            }
        }
        // 日时刑冲
        val riShiChong = diZhi.chongList.any { it.location.contains("日支") && it.location.contains("时支") }
        val riShiXing = diZhi.xingList.any { it.location.contains("日支") && it.location.contains("时支") }
        if (riShiChong || riShiXing) {
            unfav += LiuQinItem("日时刑冲", "日时两柱刑冲——${if (male) "婚多不顺" else "克夫克子"}，家庭多波澜。")
        }
        // 喜用临日支遭刑冲
        if (palaceXi && (palaceChong != null || palaceXing != null)) {
            unfav += LiuQinItem("喜用临宫遭刑冲", "喜用神临日支遭刑冲——${if (male) "妻虽好但难以偕老" else "夫虽好但难以偕老"}。")
        }
        // 四柱纯阳
        val allYang = bazi.stems.all { it.yang } && bazi.branches.all { listOf(EarthlyBranch.ZI, EarthlyBranch.YIN, EarthlyBranch.CHEN, EarthlyBranch.WU, EarthlyBranch.SHEN, EarthlyBranch.XU).contains(it) }
        if (allYang) {
            unfav += LiuQinItem("四柱纯阳", "四柱纯阳——婚姻难如意，${if (male) "婚姻不称心" else "性刚而婚多磨"}。")
        }
        // 配偶星不现——晚婚；女命又透伤官者再婚之命（例二）
        if (starStems.isEmpty() && forces[spouseEl]!! <= 0.5) {
            unfav += LiuQinItem(
                "配偶星不现",
                "配偶星（${if (male) "财星" else "官杀"}）命中不现——姻缘薄而多晚婚，婚期须待岁运星透或加临配偶宫之年。" +
                    (if (!male && bazi.stems.any { tenGodOfStem(day, it) == TenGod.SHANG_GUAN }) "女命星不现又伤官透出——晚婚之命，且原组合不利婚姻者为再婚之命。" else ""),
            )
        }
        // 官星衰入墓（女）
        if (!male && forces[spouseEl]!! <= 1.0 && bazi.branches.contains(LiuQinXiJi.tombOf[spouseEl]!!)) {
            unfav += LiuQinItem("官星衰入墓", "官星衰而入墓——克夫，夫缘薄弱。")
        }
        // 官坐长生不遭刑冲（女）/财坐长生（男）——配偶寿长
        if (spouseCat in d.fav) {
            val changSheng = when (spouseEl) {
                Element.WOOD -> EarthlyBranch.HAI
                Element.FIRE -> EarthlyBranch.YIN
                Element.METAL -> EarthlyBranch.SI
                Element.WATER -> EarthlyBranch.SHEN
                else -> EarthlyBranch.SHEN
            }
            if (bazi.branches.contains(changSheng) && palaceChong == null) {
                fav += LiuQinItem("配偶星坐长生", "${if (male) "财" else "官"}星坐长生不遭刑冲——${if (male) "妻长寿" else "夫身健寿长"}。")
            }
        }

        // ── 婚恋流年（全量铺开） ──
        val birthYear = bazi.birthDateTime.year
        val legalAge = if (male) 22 else 20
        val yuns = daYun.yuns.filter { it.startAge >= 14 }.map { yun ->
            val years = yun.liuNian.map { ln ->
                val yb = ln.pillar.branch
                val triggers = mutableListOf<String>()
                // ① 配偶宫引动
                if (yb == dayBranch) triggers += "流年加临配偶宫${yb.chinese}"
                else {
                    if (EarthlyBranch.entries[(yb.ordinal + 6) % 12] == dayBranch) triggers += "${yb.chinese}冲配偶宫${dayBranch.chinese}——婚变动荡"
                    val heWithGong = liuHePairs.any { yb in it && dayBranch in it }
                    if (heWithGong) triggers += "${yb.chinese}合动配偶宫${dayBranch.chinese}"
                    if (anHePairs.any { yb in it && dayBranch in it }) triggers += "${yb.chinese}暗合配偶宫${dayBranch.chinese}"
                    // 巳申等对六合三刑并存——合优先于刑（贪合忘刑）
                    if (!heWithGong && sanXingGroups.any { yb in it && dayBranch in it }) triggers += "${yb.chinese}刑配偶宫${dayBranch.chinese}——婚变动荡"
                    if (sanHeGroups.any { yb in it && dayBranch in it }) triggers += "${yb.chinese}与配偶宫${dayBranch.chinese}三合"
                    if (yb.element == dayBranch.element) triggers += "${yb.chinese}比助配偶宫${dayBranch.chinese}"
                    if (yb.element.generates == dayBranch.element) triggers += "${yb.chinese}生配偶宫${dayBranch.chinese}"
                }
                // ② 配偶星引动
                val yGanGod = tenGodOfStem(day, ln.pillar.stem)
                if (yGanGod.category() == spouseCat) triggers += "${if (male) "财星透" else "官星透"}（${ln.pillar.stem.chinese}${yGanGod.chinese}）"
                if (branchHiddenStems.getValue(yb).firstOrNull()?.element == spouseEl) triggers += "${if (male) "财星当值" else "官星当值"}（${yb.chinese}）"
                // ③ 桃花当值（日支起）
                if (yb == taoBranch) triggers += "桃花当值（${yb.chinese}）"
                // 性质判定
                val age = ln.year - birthYear
                val ageNote = when {
                    age < 16 -> "少年（非婚恋重点段）"
                    age < legalAge -> "恋爱段（未达法定婚龄$legalAge）"
                    age <= 31 -> "适婚段"
                    else -> "晚婚段"
                }
                val kind: HunLianKind
                val text: String
                val gongDong = triggers.any { it.contains("配偶宫") }
                val chongXing = triggers.any { it.contains("婚变动荡") }
                val xingTou = triggers.any { it.contains("星透") || it.contains("星当值") }
                val jiaLin = triggers.any { it.contains("加临") }
                val tao = triggers.any { it.contains("桃花当值") }
                val heDong = triggers.any { it.contains("合动") || it.contains("暗合") }
                when {
                    chongXing && age >= legalAge -> {
                        kind = HunLianKind.HUN_QING
                        text = "冲动配偶宫且届婚龄——婚庆大事之期：结婚、婚礼、生育多应此年（配偶星并动者尤验）。"
                    }
                    chongXing -> {
                        kind = HunLianKind.BIAN_DONG
                        text = "配偶宫逢冲刑而未届婚龄——婚恋动荡：聚散离合之变。"
                    }
                    jiaLin -> {
                        kind = HunLianKind.TONG_JU
                        text = "流年加临配偶宫——婚恋质变之年：恋爱同居、共处如夫妻（实质婚姻）；配偶星并动者成家尤验。"
                    }
                    xingTou && gongDong -> {
                        kind = HunLianKind.LIAN_AI
                        text = "配偶星引动且配偶宫被引动——恋爱之年：新恋情萌动，异性缘至。"
                    }
                    heDong -> {
                        kind = HunLianKind.TONG_JU
                        text = "流年合动配偶宫——恋爱同居之期：关系迅速升温而至共处（桃花并临亦主同居之亲）。"
                    }
                    tao && gongDong -> {
                        kind = HunLianKind.HUN_QING
                        text = "桃花当值且配偶宫引动——婚庆嫁娶之期：恋情公开、缔结婚约（再婚之礼亦应此）。"
                    }
                    xingTou || gongDong -> {
                        kind = HunLianKind.LIAN_AI
                        text = "配偶${if (xingTou) "星" else "宫"}被引动——婚恋信息之年：婚恋状态有变化之机。"
                    }
                    else -> {
                        kind = HunLianKind.WU
                        text = "配偶宫星两无引动——婚恋平稳无信号之年。"
                    }
                }
                HunYinYear(ln.year, "${ln.year} ${ln.pillar.stem.chinese}${yb.chinese}", kind, triggers, text, "周岁$age·$ageNote")
            }
            HunYinYun(yun.pillar, "${yun.pillar.fullName} ${yun.startYear}-${yun.endYear}", years)
        }

        val summary = "男以财星、女以官杀为配偶星，日支为配偶宫。婚恋流年=宫被引动（冲合刑暗合当值生助）"
            "或星被引动（透干当值）；恋爱多应16-30岁，法定婚龄男22女20。实质婚姻（同居共处）亦为命理之婚。"

        return HunYinResult(
            spouseStarNote = spouseStarNote,
            palaceNote = palaceNote,
            baseItems = fav,
            baseUnfav = unfav,
            yuns = yuns,
            summary = summary,
        )
    }
}
