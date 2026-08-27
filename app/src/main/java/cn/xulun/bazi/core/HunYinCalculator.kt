package cn.xulun.bazi.core

import cn.xulun.bazi.core.LiuQinXiJi.category
import cn.xulun.bazi.core.LiuQinXiJi.elementOf
import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.DaYunInfo
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.HeavenlyStem
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
    /** 三会方局：寅卯辰东方木 巳午未南方火 申酉戌西方金 亥子丑北方水。 */
    private val sanHuiGroups = mapOf(
        setOf(EarthlyBranch.YIN, EarthlyBranch.MAO, EarthlyBranch.CHEN) to Element.WOOD,
        setOf(EarthlyBranch.SI, EarthlyBranch.WU, EarthlyBranch.WEI) to Element.FIRE,
        setOf(EarthlyBranch.SHEN, EarthlyBranch.YOU, EarthlyBranch.XU) to Element.METAL,
        setOf(EarthlyBranch.HAI, EarthlyBranch.ZI, EarthlyBranch.CHOU) to Element.WATER,
    )
    /** 六害。 */
    private val liuHaiPairs = setOf(
        setOf(EarthlyBranch.ZI, EarthlyBranch.WEI), setOf(EarthlyBranch.CHOU, EarthlyBranch.WU),
        setOf(EarthlyBranch.YIN, EarthlyBranch.SI), setOf(EarthlyBranch.MAO, EarthlyBranch.CHEN),
        setOf(EarthlyBranch.SHEN, EarthlyBranch.HAI), setOf(EarthlyBranch.YOU, EarthlyBranch.XU),
    )
    private val sanXingGroups = listOf(
        setOf(EarthlyBranch.YIN, EarthlyBranch.SI, EarthlyBranch.SHEN),
        setOf(EarthlyBranch.CHOU, EarthlyBranch.XU, EarthlyBranch.WEI),
        setOf(EarthlyBranch.ZI, EarthlyBranch.MAO), // 子卯互刑
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

        val shiEl = elementOf(GodCategory.SHI, dayEl)
        val shangWangFlag = forces[shiEl]!! >= 3.0
        val pianCaiFlag = bazi.stems.any { tenGodOfStem(day, it) == TenGod.PIAN_CAI } || bazi.branches.any { br ->
            branchHiddenStems.getValue(br).firstOrNull()?.element == spouseEl
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

        // ── 夫妻关系:日柱干支生克比和(以配偶宫衡量,不以星之生克——星必克干) ──
        val dayBrEl = dayBranch.element
        val guanXi = when {
            dayEl.generates == dayBrEl -> LiuQinItem(
                "宫干相生·我愛配偶",
                "日柱干支相生（干生支）——夫妻关系和睦：我喜欢配偶，主动关爱付出。",
            )
            dayBrEl.generates == dayEl -> LiuQinItem(
                "宫干相生·配偶爱我",
                "日柱干支相生（支生干）——夫妻关系和睦：配偶喜欢我，得配偶之关爱体贴（纵生活清苦而情感满足——例四式）。",
            )
            dayEl.controls == dayBrEl -> LiuQinItem(
                "宫干相克·我压制",
                "日柱干支相克（干克支）——夫妻关系不睦：我压制配偶，主导强势。",
            )
            dayBrEl.controls == dayEl -> LiuQinItem(
                "宫干相克·配偶压制",
                "日柱干支相克（支克干）——夫妻关系不睦：配偶压制我，其强势带来心理压力（例三夫人火爆丈夫压抑、例五夫甘做耙耳朵式）。",
            )
            else -> LiuQinItem(
                "宫干比和·朋友之交",
                "日柱干支比和——夫妻关系犹如朋友：平等相处相亲相爱，难如胶似漆；纵离婚亦不生仇恨，然关系非亲密者他异性可乘虚而入（例一二式）。",
            )
        }
        fav += guanXi
        // 女命配偶宫被两重官杀夹克——他男进入配偶宫之象(例一两午夹申)
        if (!male) {
            val keGongEl = Element.entries.first { it.controls == dayBrEl }
            if (keGongEl == spouseEl && bazi.branches.count { it.element == keGongEl } >= 2) {
                unfav += LiuQinItem(
                    "配偶宫被夹克",
                    "配偶宫被两重官杀（${keGongEl.chinese}行）夹克——会有其他男人进入配偶宫占据丈夫之位；宫干比和关系非亲密者，情人乘虚而入。",
                )
            }
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
            fav += LiuQinItem(
                "身弱财旺待时而婚",
                "身弱财旺——逢印比帮扶之年、财星透干之时方有红鸾之喜（例六庚金寅月三会财局式：身变旺能胜财，有妻来到）。",
            )
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
        // ── 异性情缘(第六节) ──
        run {
            val heDuo = HeHuaCalculator.analyze(bazi).branchLiuHeList.size >= 2
            // ④男遍地财/女官杀多——近水楼台(纵身弱)
            val pianCai = bazi.stems.count { tenGodOfStem(day, it) == TenGod.PIAN_CAI } +
                bazi.branches.count { br -> branchHiddenStems.getValue(br).firstOrNull()?.element == spouseEl }
            if (male && forces[spouseEl]!! >= 2.5 && pianCai >= 1) {
                fav += LiuQinItem(
                    "遍地财·异性缘厚",
                    "财星多旺(年月日均有财尤验)——异性缘非常好:自己不找异性也找上门,难以抵挡;年月有财初同床者略长于己(例一二式)",
                )
            }
            if (!male && forces[spouseEl]!! >= 3.0) {
                unfav += LiuQinItem("官杀多·异性缠身", "官杀多而旺——异性主动上门纠缠,纵身弱亦近水楼台;不清则感情纠葛多端。")
            }
            // ①身强星为用——追求执着
            if (spouseCat in d.fav && StrengthEngine.analyze(bazi).verdict == DayStrength.STRONG) {
                fav += LiuQinItem("身强星为用·追求执着", "身强而${if (male) "财" else "官杀"}为用——对异性追求热情执着,情场永不言败。")
            }
            // ②合多——气质吸引
            if (heDuo) {
                fav += LiuQinItem("合多·魅力气质", "柱中合多(尤地支六合成对)——独特魅力气质易得异性青睐,易达心灵沟通。")
            }
            // ③身旺伤官胆大 / ⑤身弱伤官重无财化——异性缘薄
            val shangForce = forces[shiEl]!!
            if (StrengthEngine.analyze(bazi).verdict == DayStrength.STRONG && shangForce >= 2.0) {
                fav += LiuQinItem("身旺伤食·胆大敢追", "身旺而伤官食神有力——胆大富冒险善表达,敢想敢追;伤官食神亦主性欲,异性相吸(例四式,少年即有婚恋)。")
            }
            if (shangForce >= 2.5 && forces[spouseEl]!! <= 0.5) {
                unfav += LiuQinItem(
                    "身弱伤重无财化·异性缘薄",
                    "身弱而伤官泄身过重又无财化泄——有色心无色胆,异性缘薄,有妻难寻(例五式,迟迟未婚配)。",
                )
            }
        }
        // ── 老少夫妻(第七节) ──
        run {
            // 官杀居年柱(父母辈)——嫁老夫;七杀入墓为偏房
            if (!male) {
                val shaNian = bazi.stems.getOrNull(0)?.let { tenGodOfStem(day, it) } == TenGod.QI_SHA ||
                    branchHiddenStems.getValue(bazi.branches[0]).firstOrNull()
                        ?.let { tenGodOfStem(day, it) } == TenGod.QI_SHA
                val guanShaRuMu = bazi.branches.contains(LiuQinXiJi.tombOf[spouseEl]!!) && forces[spouseEl]!! <= 1.5
                if (shaNian) {
                    unfav += LiuQinItem(
                        "官杀居年·招老夫",
                        "七杀/官杀居年柱(父母辈)——丈夫年龄与父母辈不相上下,必嫁老夫" +
                            (if (guanShaRuMu) ";七杀入墓为偏房,官星入墓作填房——不嫁老夫便为偏房填房(例一式)" else "") +
                            "(实况多见大15岁以上)",
                    )
                }
                // 官杀居年支入日时之墓——不招老夫必克夫(例四)
                val riShiMu = bazi.branches.drop(2).contains(LiuQinXiJi.tombOf[spouseEl]!!) &&
                    branchHiddenStems.getValue(bazi.branches[0]).any { it.element == spouseEl }
                if (riShiMu) {
                    unfav += LiuQinItem("年支官杀入日时墓", "官杀居年支而入日时之墓——不招老夫必克夫(例四式,夫大18岁)。")
                }
                // 伤官重重无财化——嫁白头郎或填房
                if (shangWangFlag && forces[spouseEl]!! <= 1.0) {
                    unfav += LiuQinItem("伤重无财化·嫁白头郎", "伤官重重又无财化泄——定嫁白头郎或填房;伤旺克夫者找老夫,否则不死则离(例七式)。")
                }
            }
            // 戊癸合——老少配
            val wuGuiHe = (bazi.stems.contains(HeavenlyStem.WU) && bazi.stems.contains(HeavenlyStem.GUI)) ||
                (day == HeavenlyStem.WU && bazi.stems.contains(HeavenlyStem.GUI)) ||
                (day == HeavenlyStem.GUI && bazi.stems.contains(HeavenlyStem.WU))
            if (wuGuiHe) {
                fav += LiuQinItem("戊癸合·老少之配", "柱逢戊癸之合(无情之合)——老夫配少妇,老妻配少夫,爱人年龄易悬殊。")
            }
            // 男伤食旺透+财从遍地财——小得多的情人
            if (male && forces[shiEl]!! >= 2.5 && forces[spouseEl]!! <= 1.0 && pianCaiFlag) {
                fav += LiuQinItem("伤食旺财从·少情人", "伤官食神旺透而财从遍地是财——胆大富冒险,有比己小得多的情人(例五式)。")
            }
            // 日干旺极弱极——悬殊;中和——差距小;冲战激烈(命硬)——悬殊或欠稳
            val verdict = StrengthEngine.analyze(bazi).verdict
            if (diZhi.chongList.size <= 1) {
                fav += LiuQinItem("四柱平和·年龄相近", "四柱平和无大的冲战,日干中和——爱人年龄差距不大(例八九式,差1-4岁)。")
            }
            if (diZhi.chongList.size >= 2) {
                unfav += LiuQinItem("冲战激烈·命硬", "四柱冲战激烈(命硬)——爱人年龄肯定悬殊,或婚姻欠稳定。")
            }
        }
        // ── 早婚/晚婚倾向(第五节) ──
        run {
            // 早婚①:配偶星现于年月柱,不被合走克破,为喜用
            val starNianYue = (starStems.contains(0) || starStems.contains(1) ||
                (starHidden.any { it.index == 0 || it.index == 1 } && forces[spouseEl]!! >= 1.5))
            val starChongPo = bazi.branches.any { ob ->
                ob != dayBranch && EarthlyBranch.entries[(ob.ordinal + 6) % 12] == dayBranch
            }
            if (starNianYue && spouseCat in d.fav) {
                fav += LiuQinItem(
                    "配偶星得位于年月·早婚",
                    "配偶星现于年/月柱而为喜用，不被合走克破——早婚之人（婚动早；财旺年月均有再逢日干与年干合者尤早——例十一式）。",
                )
            }
            // 早婚④:日支与月支六合/三合
            if (liuHePairs.any { bazi.branches[1] in it && dayBranch in it } ||
                sanHeGroups.any { bazi.branches[1] in it && dayBranch in it }
            ) {
                fav += LiuQinItem("配偶宫与月支相合·早", "日支（配偶宫）与月支相合——结婚时间早。")
            }
            // 早婚⑦:配偶宫得月令相帮而旺
            if (dayBranch.element == bazi.monthPillar.branch.element ||
                bazi.monthPillar.branch.element.generates == dayBranch.element
            ) {
                fav += LiuQinItem("配偶宫得月令·早婚", "配偶宫得月令相帮而旺——当主早婚（例十四式）。")
            }
            // 早婚③:女命身旺杀在月
            if (!male && BingYaoCalculator.analyze(bazi).strength == DayStrength.STRONG &&
                (bazi.stems.getOrNull(1)?.let { tenGodOfStem(day, it) } == TenGod.QI_SHA ||
                    branchHiddenStems.getValue(bazi.branches[1]).firstOrNull()
                        ?.let { tenGodOfStem(day, it) } == TenGod.QI_SHA)
            ) {
                fav += LiuQinItem("身旺杀临月·早婚", "女命身旺而七杀在月上——结婚早或破身早。")
            }
            // 晚婚:星藏支被冲刑
            val zhiChongXing = diZhi.chongList.isNotEmpty() || diZhi.xingList.isNotEmpty()
            if (starStems.isEmpty() && zhiChongXing && starHidden.isNotEmpty()) {
                unfav += LiuQinItem("星藏遭刑冲·晚婚", "配偶星不透干而藏于地支，又被冲刑克害——晚婚之人。")
            }
            // 晚婚:男命星在时柱他处无现
            if (male && starStems.isEmpty() && starHidden.all { it.index == 3 } && starHidden.isNotEmpty()) {
                unfav += LiuQinItem("星居时柱·晚婚", "男命财星仅在时柱，他处无透无藏——结婚时间晚。")
            }
            // 晚婚:男命财弱不从
            if (male && forces[spouseEl]!! <= 1.0 && BingYaoCalculator.analyze(bazi).congGe == null) {
                unfav += LiuQinItem(
                    "财弱不从·妻迟子晚",
                    "男命财弱而不从——晚婚；有食伤者妻迟子晚（例九式，食伤旺正财被合尤甚）。",
                )
            }
            // 晚婚:四柱纯阴(女)
            val allYin = bazi.stems.none { it.yang } && bazi.branches.all {
                it !in listOf(EarthlyBranch.ZI, EarthlyBranch.YIN, EarthlyBranch.CHEN, EarthlyBranch.WU, EarthlyBranch.SHEN, EarthlyBranch.XU)
            }
            if (!male && allYin) {
                unfav += LiuQinItem(
                    "四柱纯阴·不利婚",
                    "四柱纯阴——清灯自守之象，" +
                        (if (TiaoHouCalculator.analyze(bazi).tiaoHouElement == null) "命局偏枯无火调候，" else "") +
                        "不利婚姻，婚缘迟迟（例一式，多次恋爱而难成家）。",
                )
            }
            // 晚婚:印旺身旺+财休囚+宫空亡(例二)
            val yinWang = forces[elementOf(GodCategory.YIN, dayEl)]!! >= 2.0
            // 宫空亡:日柱旬之空亡不可落日支自身,以年柱旬查(例二甲寅日——丁未年旬空寅卯)
            val yearIdx = bazi.yearPillar.cycleIndex
            val xunStart = yearIdx - (yearIdx % 10)
            val nianXuKong = setOf(
                EarthlyBranch.fromIndex((xunStart + 10) % 60 % 12),
                EarthlyBranch.fromIndex((xunStart + 11) % 60 % 12),
            )
            val gongKong = nianXuKong.contains(dayBranch)
            if (yinWang && forces[spouseEl]!! <= 2.0 && gongKong) {
                unfav += LiuQinItem("印旺财囚宫空·难成婚", "印旺身旺而财星休囚，日支（配偶宫）又落空亡——婚姻难成，迟迟未婚（例二式）。")
            }
            // 晚婚:比劫重重+财星受制(例六七八 众劫夺财)
            if (forces[dayEl]!! >= 3.0 && forces[spouseEl]!! <= 1.0) {
                unfav += LiuQinItem(
                    "比劫夺财·晚婚",
                    "比劫重重而财星无力（四柱无财或财从）——青年行财运为众比所夺，财印相战，恋爱难成，晚婚为佳" +
                        "（例六土金成势四柱无财反得财、例八假专旺式；比劫有伤食化泄者，逢财旺岁运婚姻方动——例七式）。",
                )
            }
            // 晚婚:时支合配偶宫+印比旺(例七)
            if ((liuHePairs.any { bazi.branches[3] in it && dayBranch in it }) && yinWang) {
                unfav += LiuQinItem("妻宫时合印旺·晚婚", "配偶宫与时支相合而又印旺夺财——晚婚（例七式）。")
            }
        }
        // ── 离婚隐患:配偶宫被月/时支冲刑合害(第四节) ──
        run {
            val posLabels2 = listOf("年支", "月支", "日支", "时支")
            val gongShang = mutableListOf<String>()
            listOf(0, 1, 3).forEach { i ->
                val ob = bazi.branches[i]
                when {
                    EarthlyBranch.entries[(ob.ordinal + 6) % 12] == dayBranch -> gongShang += "${posLabels2[i]}${ob.chinese}冲配偶宫"
                    sanXingGroups.any { ob in it && dayBranch in it } -> gongShang += "${posLabels2[i]}${ob.chinese}刑配偶宫"
                    liuHePairs.any { ob in it && dayBranch in it } -> gongShang += "${posLabels2[i]}${ob.chinese}合配偶宫"
                    sanHeGroups.any { ob in it && dayBranch in it } -> gongShang += "${posLabels2[i]}${ob.chinese}半合配偶宫"
                    liuHaiPairs.any { ob in it && dayBranch in it } -> gongShang += "${posLabels2[i]}${ob.chinese}害配偶宫"
                }
            }
            if (gongShang.isNotEmpty()) {
                val heYue = gongShang.any { it.startsWith("月支") && it.contains("合") }
                unfav += LiuQinItem(
                    "配偶宫受伤·离婚隐患",
                    "配偶宫被${gongShang.joinToString("、")}——宫之状态即婚姻状态,原局有离婚隐患;" +
                        (if (heYue) "宫与月令合,预示配偶有外遇之兆;日柱干支比和者感情结合不牢,第三者可乘虚而入。" else "") +
                        "再逢刑冲合害配偶宫之大运流年即引发:轻则口角矛盾,重则离异(刑冲尤重)。",
                )
            }
        }
        // ── 婚外情倾向(第八节) ──
        run {
            val gongBeiked = listOf(1, 3).any { i ->
                val ob = bazi.branches[i]
                bazi.branches[i].element.controls == dayBranch.element && ob != dayBranch
            }
            val nanPianCaiDuo = male && bazi.stems.count { tenGodOfStem(day, it) == TenGod.PIAN_CAI } >= 1 &&
                forces[spouseEl]!! >= 2.0
            val nvShaDuo = !male && bazi.stems.count {
                tenGodOfStem(day, it) == TenGod.QI_SHA || tenGodOfStem(day, it) == TenGod.ZHENG_GUAN
            } >= 2
            val shuiWang = forces[Element.WATER]!! >= 3.5
            val gongShangFlag = unfav.any { it.dim == "配偶宫受伤·离婚隐患" }
            val waiQing = mutableListOf<String>()
            if (gongShangFlag) waiQing += "配偶宫被刑冲合害而结构不稳(异性乘虚而入)"
            if (gongBeiked) waiQing += "配偶宫被月/时支所克(受损求补偿)"
            if (nanPianCaiDuo) waiQing += "偏财多旺(树欲静而风不止,异性找上门)"
            if (nvShaDuo) waiQing += "官杀多旺(异性主动纠缠)"
            if (shuiWang || forces[shiEl]!! >= 2.0) waiQing += "${if (shuiWang) "水" else "食伤"}过旺(难控欲望易出轨)"
            val xingRuCGong = sanXingGroups.any { bazi.branches[3] in it && dayBranch in it } ||
                EarthlyBranch.entries[(bazi.branches[3].ordinal + 6) % 12] == dayBranch
            if (male && branchHiddenStems.getValue(bazi.branches[3]).firstOrNull()?.element == spouseEl && xingRuCGong) {
                waiQing += "配偶星居时柱而刑入配偶宫(婚后情人之象)"
            }
            if (waiQing.size >= 2) {
                unfav += LiuQinItem(
                    "婚外情倾向",
                    waiQing.joinToString("、") + "——婚外情概率高:逢流年刑冲合害配偶宫引发(岁运只作环境,流年才是引信)," +
                        "异性乘虚而入;食伤财星官杀当值之岁运尤应。偏财(七杀)透年而宫被合入库者,情人登场(例四癸未式)。",
                )
            }
        }
        // ── 克夫(女)/克妻(男)组合 ──
        if (!male) {
            val yangRenItems = ShenShaCalculator.analyze(bazi).items.filter { it.name == "羊刃" }
            val shangWang = forces[elementOf(GodCategory.SHI, dayEl)]!! >= 3.0
            val starKong = bazi.branches.indices.filter { branchHiddenStems.getValue(bazi.branches[it]).any { h -> h.element == spouseEl } }
                .any { KongWangCalculator.analyze(bazi).isKong(it) }
            val starRuMu = bazi.branches.contains(LiuQinXiJi.tombOf[spouseEl]!!) && forces[spouseEl]!! <= 1.5
            val gongMa = ShenShaCalculator.analyze(bazi).items.any { it.name == "驿马" && it.branch == dayBranch }
            val kb = mutableListOf<String>()
            if (forces[dayEl]!! >= 3.0 || yangRenItems.isNotEmpty()) kb += "比劫${if (yangRenItems.isNotEmpty()) "羊刃" else ""}重重欺夫"
            if (shangWang) kb += "食伤旺透伤夫"
            if (starKong) kb += "夫星落空亡"
            if (starRuMu) kb += "夫星入墓"
            if (gongMa) kb += "夫宫临驿马(夫有跑掉之象)"
            if (kb.size >= 2) {
                unfav += LiuQinItem(
                    "克夫之象",
                    kb.joinToString("、") + "——婚姻极不顺:多婚克夫,不离婚则有死夫之虞;伤官食神旺年攻星、墓门冲开星入墓之年最凶。",
                )
            }
        } else {
            val yueBranch = bazi.monthPillar.branch
            val gongPo = EarthlyBranch.entries[(yueBranch.ordinal + 6) % 12] == dayBranch
            val caiZhiAnHe = bazi.branches.any { ob -> ob != dayBranch && anHePairs.any { ob in it && it.any { x -> x != ob && x == dayBranch } } } ||
                bazi.branches.any { ob -> branchHiddenStems.getValue(ob).firstOrNull()?.element == spouseEl && anHePairs.any { ap -> ob in ap && ap.any { it != ob && it in bazi.branches } } }
            if (gongPo) {
                unfav += LiuQinItem(
                    "克妻之象",
                    "配偶宫被当令之支冲破" + (if (caiZhiAnHe) "+财(妻)星被暗合" else "") +
                        "——婚姻不稳,克妻信息明显:岁运妻星配偶宫遭冲克之年,不生离则死别(例二十四式)。",
                )
            }
        }
        // ── 多婚多妻妾 ──
        run {
            if (!male) {
                val guanSha = bazi.stems.indices.filter {
                    it != 2 && (tenGodOfStem(day, bazi.stems[it]) == TenGod.ZHENG_GUAN || tenGodOfStem(day, bazi.stems[it]) == TenGod.QI_SHA)
                }
                if (guanSha.size >= 2) {
                    unfav += LiuQinItem("官杀混杂·多夫", "年月透官杀而混杂——多夫之象:不嫁老夫便为妾,感情纠葛多婚之命(例十五、十七式)。")
                }
            } else {
                val caiTou = bazi.stems.count { tenGodOfStem(day, it) == TenGod.ZHENG_CAI || tenGodOfStem(day, it) == TenGod.PIAN_CAI }
                val caiCang = bazi.branches.count { branchHiddenStems.getValue(it).any { h -> h.element == spouseEl } }
                if (caiTou >= 1 && caiCang >= 2 && forces[spouseEl]!! >= 2.5) {
                    fav += LiuQinItem(
                        "正偏财透藏·有妻有妾",
                        "正偏财天透地藏(年月时均有财)——老婆多,身财两停者有妻有妾而不离;年月财透初同床者长于己;时支桃花临财者与子女同龄者为妻(例十八式)。",
                    )
                }
            }
        }
        // 夫星弱而无扶（例七：年月日无夫星仅时支余气，身旺欺夫，1988前难成婚）
        if (!male && starStems.isEmpty() && forces[spouseEl]!! in 0.51..1.0 &&
            BingYaoCalculator.analyze(bazi).strength == DayStrength.STRONG) {
            unfav += LiuQinItem(
                "夫星弱而无扶",
                "身旺而夫星弱仅余气微藏——身旺欺夫，早年难成婚；须岁运入星乡（火乡）引动夫星、逢桃花值年方能成婚。",
            )
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
                // 争合解冲：流年支与原局同支重复，且同支与原局他支六合——两巳争合一申式，冲力被合绊
                val gongInJu = (sanHeGroups + sanHuiGroups.keys).any { g ->
                    dayBranch in g && g.count { it in bazi.branches } >= 3 // 居原局成局之三支全齐方可言破局之冲
                }
                val zhengHe = !gongInJu && bazi.branches.contains(yb) &&
                    liuHePairs.any { pr -> yb in pr && pr.any { it != yb && it in bazi.branches } }
                // ① 配偶宫引动
                if (yb == dayBranch) triggers += "流年加临配偶宫${yb.chinese}"
                else {
                    if (EarthlyBranch.entries[(yb.ordinal + 6) % 12] == dayBranch && !zhengHe) {
                        triggers += "${yb.chinese}冲配偶宫${dayBranch.chinese}——婚变动荡"
                    }
                    if (zhengHe) triggers += "${yb.chinese}与原局${yb.chinese}争合——冲力被合绊（难成婚）"
                    val heWithGong = liuHePairs.any { yb in it && dayBranch in it }
                    if (heWithGong) triggers += "${yb.chinese}合动配偶宫${dayBranch.chinese}"
                    if (anHePairs.any { yb in it && dayBranch in it }) triggers += "${yb.chinese}暗合配偶宫${dayBranch.chinese}"
                    // 巳申等对六合三刑并存——合优先于刑（贪合忘刑）
                    if (!heWithGong && sanXingGroups.any { yb in it && dayBranch in it }) triggers += "${yb.chinese}刑配偶宫${dayBranch.chinese}——婚变动荡"
                    if (sanHeGroups.any { yb in it && dayBranch in it }) triggers += "${yb.chinese}与配偶宫${dayBranch.chinese}三合"
                    if (liuHaiPairs.any { yb in it && dayBranch in it }) triggers += "${yb.chinese}害配偶宫${dayBranch.chinese}——婚变引信"
                    if (yb.element == dayBranch.element) triggers += "${yb.chinese}比助配偶宫${dayBranch.chinese}"
                    if (yb.element.generates == dayBranch.element) triggers += "${yb.chinese}生配偶宫${dayBranch.chinese}"
                }
                // ② 配偶星引动
                val yGanGod = tenGodOfStem(day, ln.pillar.stem)
                if (yGanGod.category() == spouseCat) triggers += "${if (male) "财星透" else "官星透"}（${ln.pillar.stem.chinese}${yGanGod.chinese}）"
                if (branchHiddenStems.getValue(yb).firstOrNull()?.element == spouseEl) triggers += "${if (male) "财星当值" else "官星当值"}（${yb.chinese}）"
                // ②b 岁干与日干五合——配偶星合我（例十五丙辛合，财与日干相合）
                val ys = ln.pillar.stem
                val ganHeDay: Boolean = (ys == HeavenlyStem.JIA && day == HeavenlyStem.JI) ||
                    (ys == HeavenlyStem.JI && day == HeavenlyStem.JIA) ||
                    (ys == HeavenlyStem.YI && day == HeavenlyStem.GENG) ||
                    (ys == HeavenlyStem.GENG && day == HeavenlyStem.YI) ||
                    (ys == HeavenlyStem.BING && day == HeavenlyStem.XIN) ||
                    (ys == HeavenlyStem.XIN && day == HeavenlyStem.BING) ||
                    (ys == HeavenlyStem.DING && day == HeavenlyStem.REN) ||
                    (ys == HeavenlyStem.REN && day == HeavenlyStem.DING) ||
                    (ys == HeavenlyStem.WU && day == HeavenlyStem.GUI) ||
                    (ys == HeavenlyStem.GUI && day == HeavenlyStem.WU)
                if (ganHeDay && yGanGod.category() == spouseCat) {
                    triggers += "岁干${ln.pillar.stem.chinese}合日干${day.chinese}（配偶星合我）"
                }
                // ③ 桃花当值（年支起或日支起，任一命中——例一酉自日支申起、例七酉自年支申起）
                val taoYear = taoHuaOf(bazi.branches[0])
                if (yb == taoBranch || yb == taoYear) triggers += "桃花当值（${yb.chinese}）"
                // ④ 三会方局婚变：年支与原局二支成三会，会局行为日主比劫且克配偶星行——身旺欺夫/欺妻，星熄婚变（例七1994申酉戌会金）
                sanHuiGroups.forEach { (group, el) ->
                    if (group.all { it in bazi.branches || it == yb } && el == dayEl && spouseEl.controls == el) {
                        triggers += "${group.joinToString("") { it.chinese }}三会${el.chinese}局——${el.chinese}多${spouseEl.chinese}熄，身旺欺${if (male) "妻" else "夫"}，婚变离异"
                    }
                }
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
                val heDong = triggers.any { it.contains("合动") }
                val anHeOnly = triggers.any { it.contains("暗合配偶宫") } && !xingTou
                val sanHuiBian = triggers.any { it.contains("三会") && it.contains("婚变离异") }
                val xingGong = triggers.any { it.contains("刑配偶宫") }
                val shenFu = (ln.pillar.stem.element == dayEl || ln.pillar.stem.element.generates == dayEl ||
                    yb.element == dayEl || yb.element.generates == dayEl) && age in legalAge..31 // 印比扶身届婚龄（身弱尤验——例九乙亥）
                val waiQingFlag = unfav.any { it.dim == "婚外情倾向" }
                val gongKeGan = dayBrEl.controls == dayEl
                val zhuGong = yb == dayBranch || (gongKeGan && yb.element == dayBranch.element)
                // 离婚引发:原局宫有伤(冲刑合害)而流年再刑冲合害宫——状态再变,婚变之应
                val gongShangJu = unfav.any { it.dim == "配偶宫受伤·离婚隐患" }
                val lihunYin =
                    triggers.any { it.contains("冲配偶宫") || it.contains("刑配偶宫") || it.contains("害配偶宫") } &&
                        (gongShangJu || (xingTou && age >= 25))
                // 亡偶:配偶星入墓之支被流年冲开(墓门开)或星逢流年墓
                val starTomb = LiuQinXiJi.tombOf[spouseEl]!!
                val wangOu = (yb == EarthlyBranch.entries[(starTomb.ordinal + 6) % 12] && bazi.branches.contains(starTomb)) ||
                    (yb == starTomb && forces[spouseEl]!! <= 1.5)
                when {
                    wangOu && age > legalAge -> {
                        kind = HunLianKind.BIAN_DONG
                        text = "配偶星墓门之应——" + (if (yb == EarthlyBranch.entries[(starTomb.ordinal + 6) % 12]) "流年冲开${starTomb.chinese}墓,配偶星入墓" else "流年临配偶星之墓") +
                            "亡偶之厄:不生离则死别(例九夫星入墓离婚、例二十三墓门开夫被误杀式)。"
                    }
                    lihunYin && age > legalAge -> {
                        kind = HunLianKind.BIAN_DONG
                        text = "原局配偶宫带伤,流年再" + triggers.firstOrNull { it.contains("配偶宫") }?.substringBefore("——") +
                            "——宫之状态再变,婚变之应:口角激化乃至离异分居;若为合则防配偶外遇(第三方进入配偶生活——例一1999卯合戌式)。"
                    }
                    zhuGong && gongKeGan && age > legalAge -> {
                        kind = HunLianKind.BIAN_DONG
                        text = "岁支${yb.chinese}助起克身之配偶宫（宫本克干）——夫妻关系恶化之年：口角压抑加剧（例三己巳庚午式）。"
                    }
                    sanHuiBian -> {
                        kind = HunLianKind.BIAN_DONG
                        text = triggers.first { it.contains("三会") }.let { "三会方局成而配偶星熄——婚变之年：身旺欺夫（妻），离异分手多应此（例七甲戌申酉戌会金式）。" }
                    }
                    xingGong && xingTou -> {
                        kind = HunLianKind.TONG_JU
                        text = "刑动配偶宫而配偶星并动——有人来到身边：恋爱同居之期（例十甲戌丑未戌三刑刑动财星宫式）。"
                    }
                    chongXing && age in legalAge..31 -> {
                        kind = HunLianKind.HUN_QING
                        text = "冲动配偶宫且届婚龄——婚庆大事之期：结婚、婚礼、生育多应此年（配偶星并动者尤验）。"
                    }
                    chongXing && age > 31 -> {
                        kind = HunLianKind.BIAN_DONG
                        text = "岁运与婚姻宫反吟（冲）而过适婚之年——婚变之期：配偶星被刑冲者离异难免，防分居失和。"
                    }
                    chongXing -> {
                        kind = HunLianKind.BIAN_DONG
                        text = "配偶宫逢冲刑而未届婚龄——婚恋动荡：聚散离合之变。"
                    }
                    jiaLin -> {
                        kind = HunLianKind.TONG_JU
                        text = "流年加临配偶宫——婚恋质变之年：恋爱同居、共处如夫妻（实质婚姻）；配偶星并动者成家尤验。"
                    }
                    waiQingFlag && triggers.any { it.contains("合动") || it.contains("三合") } && xingTou && age > legalAge -> { // 明合(暗合属暗恋非婚外)
                        kind = HunLianKind.BIAN_DONG
                        text = "配偶宫被合动而配偶星并透,原局带婚外情倾向——婚外情引发之年:第三方进入配偶生活," +
                            (if (male) "偏财(情人)透干尤验" else "官杀(情人)透干尤验") + ";原配退位之象(例四癸未式)。"
                    }
                    ganHeDay && xingTou && age >= legalAge -> {
                        kind = HunLianKind.HUN_QING
                        text = "岁干与日干相合而所合为配偶星——财（官）来合我，婚恋喜期：结婚喝喜酒多应此年（例十五丙辛合、例十二己甲合式）。"
                    }
                    xingTou && gongDong -> {
                        kind = HunLianKind.LIAN_AI
                        text = "配偶星引动且配偶宫被引动——恋爱之年：新恋情萌动，异性缘至。"
                    }
                    zhengHe && heDong -> {
                        kind = HunLianKind.LIAN_AI
                        text = "流年与原局争合（两${yb.chinese}争合）——合被分争：有人介绍而难成，好事不到一月告吹。"
                    }
                    heDong -> {
                        kind = HunLianKind.TONG_JU
                        text = "流年合动配偶宫——恋爱同居之期：关系迅速升温而至共处（桃花并临亦主同居之亲）。"
                    }
                    tao && gongDong -> {
                        kind = HunLianKind.HUN_QING
                        text = "桃花当值且配偶宫引动——婚恋之喜：恋情缔结（无官杀逢财旺/无财逢财旺之年亦应）。"
                    }
                    shenFu -> {
                        kind = HunLianKind.HUN_QING
                        text = "太岁干支生扶身弱之日主且届婚龄——身得帮扶能胜财官，婚成之期（例九乙亥式）。"
                    }
                    anHeOnly -> {
                        kind = HunLianKind.LIAN_AI
                        text = "流年暗合配偶宫——暗中有情之年：有人暗中追求或地下情愫，无配偶星并动者多无果（竹篮打水）。"
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
