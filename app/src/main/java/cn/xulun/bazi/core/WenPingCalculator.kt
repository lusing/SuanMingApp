package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DaYunInfo
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.GodCategory
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.StudyDiploma
import cn.xulun.bazi.model.StudyDiplomaItem
import cn.xulun.bazi.model.TenGod
import cn.xulun.bazi.model.TeShuKind
import cn.xulun.bazi.model.branchHiddenStems
import cn.xulun.bazi.model.tenGodOfStem
import cn.xulun.bazi.model.xueTang
import cn.xulun.bazi.model.wenChang
import cn.xulun.bazi.core.LiuQinXiJi.category
import cn.xulun.bazi.core.LiuQinXiJi.categoryOf

/**
 * 文凭推断引擎（.xueye-reference.md 第十节·一）。学历以 **用神和印星** 为标志，少运定高低：
 * - 格局清纯，少运印星为用神；或印为忌而行食伤运——学历高（例二癸丑运伤官泄秀）
 * - 印星为用神，少行财运/损印晦印之运——学历不高
 * - 印星为忌而少年走用神运——也能高学历
 * - 原局印星力弱，大运印星生之，全局循环相生——仍登金榜（例一博士）
 * - 其他学历标志：七杀、羊刃、华盖及特殊组合
 * - 学历高低须结合时代（四五十年代高中即高学历，21 世纪大专不算高）
 *
 * 断语分有利/不利两类，全部以"为喜用/为忌"为前提（教材口径）。
 */
object WenPingCalculator {

    /** 阳干羊刃（帝旺支）。 */
    private val yangRen = mapOf(
        HeavenlyStem.JIA to EarthlyBranch.MAO, HeavenlyStem.BING to EarthlyBranch.WU,
        HeavenlyStem.WU to EarthlyBranch.WU, HeavenlyStem.GENG to EarthlyBranch.YOU,
        HeavenlyStem.REN to EarthlyBranch.ZI,
    )
    /** 三合组库支（华盖）。 */
    private val huaGai = mapOf(
        EarthlyBranch.SHEN to EarthlyBranch.CHEN, EarthlyBranch.ZI to EarthlyBranch.CHEN,
        EarthlyBranch.CHEN to EarthlyBranch.CHEN,
        EarthlyBranch.HAI to EarthlyBranch.WEI, EarthlyBranch.MAO to EarthlyBranch.WEI,
        EarthlyBranch.WEI to EarthlyBranch.WEI,
        EarthlyBranch.YIN to EarthlyBranch.XU, EarthlyBranch.WU to EarthlyBranch.XU,
        EarthlyBranch.XU to EarthlyBranch.XU,
        EarthlyBranch.SI to EarthlyBranch.CHOU, EarthlyBranch.YOU to EarthlyBranch.CHOU,
        EarthlyBranch.CHOU to EarthlyBranch.CHOU,
    )

    fun analyze(bazi: BaziResult, daYun: DaYunInfo?): StudyDiploma {
        val day = bazi.dayPillar.stem
        val dayEl = day.element
        val f = BingYaoCalculator.forces(bazi)
        val d = LiuQinXiJi.derive(bazi)
        val eng = StrengthEngine.analyze(bazi)
        val strong = eng.verdict == DayStrength.STRONG
        val weak = eng.verdict == DayStrength.WEAK

        val yinEl = Element.entries.first { it.generates == dayEl }
        val shiEl = dayEl.generates
        val caiEl = dayEl.controls
        val guanEl = Element.entries.first { it.controls == dayEl }
        val biEl = dayEl

        fun godOf(stem: HeavenlyStem) = tenGodOfStem(day, stem)
        val sealRevealed = bazi.stems.indices.filter { it != 2 }.map { godOf(bazi.stems[it]) }
            .any { it.category() == GodCategory.YIN }
        val shangRevealed = bazi.stems.indices.filter { it != 2 }.map { godOf(bazi.stems[it]) }
            .any { it == TenGod.SHANG_GUAN }
        val shiRevealed = bazi.stems.indices.filter { it != 2 }.map { godOf(bazi.stems[it]) }
            .any { it == TenGod.SHI_SHEN }
        val shaRevealed = bazi.stems.indices.filter { it != 2 }.map { godOf(bazi.stems[it]) }
            .any { it == TenGod.QI_SHA }
        val guanRevealed = bazi.stems.indices.filter { it != 2 }.map { godOf(bazi.stems[it]) }
            .any { it == TenGod.ZHENG_GUAN }
        val caiRevealed = bazi.stems.indices.filter { it != 2 }.map { godOf(bazi.stems[it]) }
            .any { it.category() == GodCategory.CAI }
        fun godCount(cat: GodCategory): Int =
            (bazi.stems.indices.filter { it != 2 }.count { godOf(bazi.stems[it]).category() == cat } +
                bazi.branches.count { br -> branchHiddenStems.getValue(br).firstOrNull()?.let { godOf(it).category() == cat } == true })
        val zhengYinCount = (bazi.stems.indices.filter { it != 2 }.count { godOf(bazi.stems[it]) == TenGod.ZHENG_YIN } +
            bazi.branches.count { br -> branchHiddenStems.getValue(br).firstOrNull() == yinElStemOf(day) })
        val pianYinCount = godCount(GodCategory.YIN) - zhengYinCount
        val yangRenCount = yangRen[day]?.let { ren -> bazi.branches.count { it == ren } } ?: 0
        val wenChangIn = bazi.branches.contains(day.wenChang)
        val xueTangIn = bazi.branches.contains(day.xueTang)
        val huaGaiBranch = huaGai[bazi.dayPillar.branch]?.takeIf { it in bazi.branches }
        val yueEl = bazi.monthPillar.branch.element

        // ── 少运（25 岁前的大运）：运星 = 干/支本气十神类别集 ──
        val youngYuns = daYun?.yuns?.filter { it.startAge < 26 }.orEmpty().take(3)
        val wenChangYunZhi = youngYuns.any { it.pillar.branch == day.wenChang }
        fun yunCats(pillar: cn.xulun.bazi.model.Pillar): Set<GodCategory> {
            val cats = mutableSetOf(categoryOf(pillar.stem.element, dayEl))
            branchHiddenStems.getValue(pillar.branch).firstOrNull()?.let {
                cats += categoryOf(it.element, dayEl)
            }
            return cats
        }
        val youngCats = youngYuns.flatMap { yunCats(it.pillar) }.toSet()

        // ── 少运喜忌链（总则；特殊格局除外——从格顺势论不作扶抑喜忌）──
        val basisParts = mutableListOf<String>()
        var highSignal = false
        var lowSignal = false
        var mediumHigh = false
        val yinDangLing = bazi.monthPillar.branch.element == yinEl
        // 杀印相生结构（局级——化杀生身，不随喜忌分支走；财运行杀印循环不作损印论，例三/例十式）
        val shaYin = shaRevealed && (yinDangLing || f[yinEl]!! >= 2.0) && f[guanEl]!! >= 2.0
        if (shaYin) {
            highSignal = true
            basisParts += "杀旺而印星${if (yinDangLing) "当令" else "有力（${f[yinEl]}）"}，化杀生身，用神有力——杀印相生，功名显达" +
                (if (GodCategory.CAI in youngCats) "；少行财运而财生杀、杀生印、印生身——循环相生" else "")
        }
        // 印星当令中和有力（3≤力<5）而少行财运——财星坏印（破印耗身）不利考学（例十三专科式）；
        // 印重≥5则财运制重印反喜（例十一土印5身旺，财运不坏印）；杀印相生结构例外（例三财走杀印循环）
        val yinDangLingCaiHuai = !shaYin && yinDangLing && GodCategory.CAI in youngCats &&
            f[yinEl]!! >= 3.0 && f[yinEl]!! < 5.0
        if (yinDangLingCaiHuai) {
            lowSignal = true
            mediumHigh = true
            basisParts += "印星当令为用而少行财运，财星坏印（破印耗身）——不利考学（例十三专科式）"
        }
        val teShu = TeShuGeJuCalculator.analyze(bazi)
        if (teShu != null) {
            // ── 从格/战局：少行从神/喜神/通关之运——顺势求学 ──
            // 从儿喜财（儿再生儿）——从神集并入从儿之财；印比之势（身旺象）另断
            val congEls = (teShu.yongShen + teShu.xi +
                (if (teShu.name.contains("从儿")) listOf(teShu.yongShen.first().generates) else emptyList())
                ).distinct()
            val congCats = congEls.map { categoryOf(it, dayEl) }.toSet()
            val yinBiShi = congCats.isNotEmpty() && congCats.all { it == GodCategory.BI || it == GodCategory.YIN }
            when {
                youngCats.any { it in congCats } && !yinBiShi -> {
                    highSignal = true
                    val zhanJu = teShu.kind == TeShuKind.ZHAN_JU
                    basisParts += "命成${teShu.name}，少行${if (zhanJu) "通关" else "从神/喜神"}" +
                        "（${congEls.joinToString("") { it.chinese }}）之运——" +
                        (if (zhanJu) "战者和为贵，形成顺势相生之势，印为喜（例九式）" else "顺势求学有成（例六/例八/例九式）") +
                        (if (!zhanJu && teShu.ji.firstOrNull()?.let { f[it]!! >= 1.0 } == true) {
                            "；惟忌神${teShu.ji.first().chinese}在局有力（金木交战式）——学历上限受限，难再升高位"
                        } else "")
                }
                // 印比之势成象（身旺象）而原局食伤绝无——运泄为救，有利文凭而非大旺（例十二专科式）
                yinBiShi && GodCategory.SHI in youngCats && f[shiEl]!! < 1.0 -> {
                    mediumHigh = true
                    basisParts += "命成${teShu.name}（印比之势——身旺象），原局食伤绝无，逢伤食运化泄为有救——有利文凭，通过努力可考（例十二专科式）"
                }
            }
        } else if (GodCategory.YIN in d.fav) {
            if (GodCategory.YIN in youngCats) {
                highSignal = true
                basisParts += "印星为用而少行印运——科甲可期"
            }
            if (GodCategory.CAI in youngCats && !shaYin) {
                lowSignal = true
                basisParts += "印星为用而少行财运损印——学历受阻"
            }
        } else if (GodCategory.YIN in d.unf) {
            if (GodCategory.SHI in youngCats && f[shiEl]!! >= 1.0) {
                highSignal = true
                basisParts += "印星为忌而少行食伤运，泄秀反利登科（例二/例四/例十一式）"
            }
            if (GodCategory.SHI in youngCats && f[shiEl]!! < 1.0) {
                // 原局食伤绝无，仅赖运化泄为救——有利文凭而非大旺（例十二专科式）
                mediumHigh = true
                basisParts += "身旺印为忌，原局食伤绝无，逢伤食运化泄为有救——有利文凭，通过努力可考（例十二专科式）"
            }
            if (youngCats.any { it in d.fav } && !yinDangLingCaiHuai) {
                highSignal = true
                basisParts += "印星为忌而少年走用神运——亦取高学历"
            }
            if (f[yinEl]!! <= 1.0 && youngCats.contains(GodCategory.YIN) && strong && f[shiEl]!! >= 2.0) {
                highSignal = true
                basisParts += "原局印星力弱（${f[yinEl]}），大运印星生之，" +
                    "${yinEl.chinese}生${biEl.chinese}生${shiEl.chinese}循环相生流通有情——利文途（例一博士式）"
            }
        }

        // ── 格局清纯：食伤旺而秀气流通（例五式）──
        if (!highSignal && GodCategory.SHI in d.fav && f[shiEl]!! >= 3.0) {
            highSignal = true
            basisParts += "食伤旺（${shiEl.chinese}力${f[shiEl]}）而秀气流通，格局清纯——一举成名（例五式）"
        }
        if (!highSignal && !lowSignal) {
            basisParts += "少运无印食财之显著取向，以原局断语参断"
        }

        // ── 断语扫描 ──
        val fav = mutableListOf<StudyDiplomaItem>()
        val unf = mutableListOf<StudyDiplomaItem>()

        if (eng.verdict == DayStrength.BALANCED && caiRevealed && guanRevealed && sealRevealed) {
            fav += item("五行中和财官印全", "四柱五行中和平衡，财官印全——最利登科成名。")
        }
        if (weak && shangRevealed && f[yinEl]!! >= 2.0 && GodCategory.YIN in d.fav) {
            fav += item("身弱伤官有印制", "身弱伤官有印制——利考学。")
        }
        if (shaRevealed && (sealRevealed || yinDangLing || f[yinEl]!! >= 2.5)) {
            fav += item(
                "印化七杀为用",
                "四柱杀旺，印星" + (if (yinDangLing) "当令" else if (sealRevealed) "透干" else "有力") +
                    "，化杀生身——定登科第（例三：杀印相生，学历学位到研究生）。",
            )
        }
        if (yangRenCount >= 2 && (shaRevealed || guanRevealed)) {
            fav += item("羊刃重重逢杀制", "羊刃重重（${yangRenCount}见）逢杀制——黄榜标名。")
        }
        if (strong && (dayEl == Element.WOOD || dayEl == Element.FIRE) &&
            f[Element.WOOD]!! + f[Element.FIRE]!! >= 5.0
        ) {
            fav += item("木火通明", "木火通明日主强——胸中珠玑万斛状元才。")
        }
        if (f[Element.METAL]!! + f[Element.WATER]!! >= 5.0 && f[Element.FIRE]!! >= 1.0) {
            fav += item("金水相涵有火暖", "金水相涵有火暖——聪明机智，文章振发。")
        }
        val shangBenQi = bazi.branches.any { br -> branchHiddenStems.getValue(br).firstOrNull()?.let { godOf(it) == TenGod.SHANG_GUAN } == true }
        val youngShangDangZhi = youngYuns.any { y ->
            branchHiddenStems.getValue(y.pillar.branch).firstOrNull()?.let { godOf(it) == TenGod.SHANG_GUAN } == true
        }
        val shangYouLi = shangRevealed || shangBenQi || youngShangDangZhi
        if (shangYouLi && f[yinEl]!! >= 3.0 && GodCategory.SHI in d.fav) {
            fav += item(
                "伤官佩印为喜用",
                "伤官泄秀而印星旺（印力${f[yinEl]}）制衡有度，伤官佩印——文章锦秀，声名显达" +
                    (if (youngShangDangZhi) "（例二：癸丑运伤官当值泄秀名登金榜式）" else "") + "。",
            )
        }
        if (huaGaiBranch != null && huaGaiBranch.element == yueEl) {
            fav += item("华盖得令为喜用", "华盖${huaGaiBranch.chinese}得令——翰林之才。")
        }
        if (!weak && GodCategory.SHI in d.fav &&
            (f[shiEl]!! >= 2.0 || youngCats.contains(GodCategory.SHI) || wenChangIn)
        ) {
            fav += item(
                "食伤泄秀为喜用",
                "身旺食伤（${shiEl.chinese}行力${f[shiEl]}）泄秀为喜用——聪明多智，文章振发" +
                    (if (f[shiEl]!! >= 3.0) "；食伤旺而秀气流通，格局清纯，一举成名（例五式）" else "") +
                    (if (youngCats.contains(GodCategory.SHI)) "；少运食伤用神到位" else "") +
                    (if (wenChangIn) "；食伤得文昌尤秀（文昌${day.wenChang.chinese}在局）" else "") +
                    (if (wenChangYunZhi) "；文昌${day.wenChang.chinese}值少运——文昌当值泄秀，必利文途（例十一式）" else "") + "。",
            )
        }
        if (shaRevealed && (shiRevealed || f[shiEl]!! >= f[guanEl]!! && f[shiEl]!! >= 2.0)) {
            fav += item("七杀有制化为权", "七杀有制化为权——功名显达自少年。")
        }
        if (strong && (shaRevealed || guanRevealed) && GodCategory.GUAN in d.fav) {
            fav += item("身旺官杀为喜用", "身旺官杀为喜用——有官有名。")
        }
        if (weak && sealRevealed && GodCategory.YIN in d.fav) {
            fav += item("身弱印绶为喜用", "身弱印绶为喜用——因文扬名。")
        }
        if (sealRevealed && GodCategory.YIN in d.fav && (wenChangIn || xueTangIn)) {
            fav += item(
                "印临文昌学堂",
                "印星为喜而" + (listOfNotNull(
                    if (wenChangIn) "文昌${day.wenChang.chinese}" else null,
                    if (xueTangIn) "学堂${day.xueTang.chinese}" else null,
                ).joinToString("、")) + "在局——才高八斗，文盖天下。",
            )
        }
        if (bazi.branches[3].let { branchHiddenStems.getValue(it).firstOrNull() }?.let { godOf(it) } == TenGod.QI_SHA &&
            godCount(GodCategory.GUAN) <= 1 && GodCategory.GUAN in d.fav
        ) {
            fav += item("时上独杀为喜用", "时上独杀为喜用——官高名显。")
        }
        if (weak && GodCategory.BI in d.fav && f[biEl]!! >= 2.0) {
            fav += item("比劫扶身为喜用", "身弱比劫扶身为喜用——艺技显名。")
        }
        if (f[caiEl]!! >= 1.0 && f[guanEl]!! >= 1.0 && f[yinEl]!! >= 1.0 &&
            (teShu == null || teShu.kind == TeShuKind.SAN_QI)
        ) {
            fav += item(
                "财官印身依次相生",
                "财生官、官生印、印生身（${caiEl.chinese}→${guanEl.chinese}→${yinEl.chinese}→${biEl.chinese}）依次相生——清纯秀丽之贵格，轻易登科（例七式）。",
            )
        }

        // 不利类
        if (strong && f[yinEl]!! >= 3.0 && f[biEl]!! >= 2.0 &&
            (GodCategory.YIN in d.unf || GodCategory.BI in d.unf)
        ) {
            unf += item(
                "身强印旺比劫重重为忌",
                "身强印旺（${yinEl.chinese}力${f[yinEl]}），比劫重重（${biEl.chinese}力${f[biEl]}）为忌——不利考试。",
            )
        }
        if (weak && f[guanEl]!! >= 3.0 && f[yinEl]!! <= 0.5) {
            unf += item("身弱官旺印星死绝", "身弱官旺（${guanEl.chinese}力${f[guanEl]}），印星处死绝——读书少成。")
        }
        if (weak && f[caiEl]!! >= 3.0 && GodCategory.YIN in d.unf) {
            unf += item("身弱财旺损印为忌", "身弱财旺（${caiEl.chinese}力${f[caiEl]}）损印为忌——科甲难登。")
        }
        if (zhengYinCount == 0 && pianYinCount >= 2 && GodCategory.YIN in d.unf) {
            unf += item("柱无正印偏印旺为忌", "柱无正印而偏印旺（${pianYinCount}见）为忌——不爱正业爱偏业，多学少成。")
        }
        if (f[guanEl]!! <= 0.5 * f[shiEl]!! && f[shiEl]!! >= 2.0 && f[guanEl]!! >= 1.0) {
            unf += item("偏官制伏太过", "偏官（杀）力${f[guanEl]}被食伤（力${f[shiEl]}）制伏太过——贫懦之命。")
        }
        if (f[guanEl]!! >= 3.0 && f[yinEl]!! <= 1.0 && weak) {
            unf += item("官旺制身太过无印通关", "官旺（${guanEl.chinese}力${f[guanEl]}）制身太过，无印通关——文章无彩。")
        }
        if (f[Element.WATER]!! >= 3.0 && f[Element.EARTH]!! >= 3.0 && f[Element.WOOD]!! <= 1.0) {
            unf += item("水土混杂", "水土混杂（水${f[Element.WATER]}土${f[Element.EARTH]}）而无木通关——思维易混沌，读书须加倍努力。")
        }
        if (f[caiEl]!! >= 4.0 && godCount(GodCategory.YIN) == 0) {
            unf += item("财多财旺不见印星", "财多财旺（${caiEl.chinese}力${f[caiEl]}）而不见印星——兴趣在财不在学。")
        }
        if (weak && f[shiEl]!! >= 3.0 && f[yinEl]!! <= 1.0) {
            unf += item("食伤泄身太过无印制", "食伤泄身太过（${shiEl.chinese}力${f[shiEl]}）而无印制——聪明反被聪明误。")
        }

        // ── 级别 ──
        val level = when {
            highSignal && !lowSignal -> "高学历（硕博之才）"
            highSignal && lowSignal -> "学历较高（少运有起伏）"
            mediumHigh -> "学历较高（专科/普通本科式）"
            !highSignal && lowSignal -> "学历不高"
            fav.size >= 3 -> "学历较高"
            unf.isNotEmpty() && fav.isEmpty() -> "学历平平"
            else -> "学历中等"
        }
        basisParts += "学历高低须结合时代——四五十年代高中即为高学历，21世纪大专已不算高"
        return StudyDiploma(
            level = level,
            basis = basisParts.joinToString("；"),
            favorable = fav,
            unfavorable = unf,
        )
    }

    private fun item(title: String, text: String) = StudyDiplomaItem(title, text, true)

    /** 日主之正印天干。 */
    private fun yinElStemOf(day: HeavenlyStem): HeavenlyStem {
        val yinEl = Element.entries.first { it.generates == day.element }
        return HeavenlyStem.entries.first { it.element == yinEl && it.isYang == !day.isYang }
    }

    private val HeavenlyStem.isYang: Boolean get() = ordinal % 2 == 0
}
