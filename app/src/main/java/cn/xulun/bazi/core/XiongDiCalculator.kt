package cn.xulun.bazi.core

import cn.xulun.bazi.core.LiuQinXiJi.category
import cn.xulun.bazi.core.LiuQinXiJi.elementOf
import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.GodCategory
import cn.xulun.bazi.model.LiuQinItem
import cn.xulun.bazi.model.TenGod
import cn.xulun.bazi.model.XiongDiResult
import cn.xulun.bazi.model.branchHiddenStems
import cn.xulun.bazi.model.tenGodOfStem

/**
 * 兄弟姐妹断语引擎（.liuqin-reference.md 第四节）。比劫为手足星：
 * 男命比肩=兄弟、劫财=姐妹（女命反是）；禄为比肩、刃为劫财；身弱无比劫生扶时印星代之。
 *
 * 手足数目口径（教材实例反推的统一约定）：
 * - 基数 = 日主 1 数 + 天干比劫每处 1 数 + 地支**本气**比劫每处 1 数
 * - 身弱：加印星数（天干印 + 支藏印全计）——"印比合计"
 * - 身旺：加倍——"旺相加倍"
 * - 减半：比劫不当令（月支本气与比劫行非生/同）且基数≥5 且克泄耗之行力≥比劫行力——"众而相战减半"
 * - 空亡：比劫本气支落旬空每支减 1；墓门开：比劫行墓支藏比劫且被冲减 1
 * - 从格：日干从什么就以什么为手足——克泄之行按合并单元计（六合对/干五合对各 1 数，日主与印行之单元不计）
 *
 * 喜忌由 [LiuQinXiJi] 教材口径派生；少运（第一步大运）影响留待岁运扩展。
 */
object XiongDiCalculator {

    fun analyze(bazi: BaziResult, gender: Gender): XiongDiResult {
        val day = bazi.dayPillar.stem
        val dayEl = day.element
        val d = LiuQinXiJi.derive(bazi)
        val forces = BingYaoCalculator.forces(bazi)
        val bing = BingYaoCalculator.analyze(bazi)

        val biEl = elementOf(GodCategory.BI, dayEl)
        val biForce = forces[biEl]!!
        // 克泄耗之行力（克我+我生+我克）
        val keXieHao = Element.entries.filter { it != biEl && it != elementOf(GodCategory.YIN, dayEl) }
            .sumOf { forces[it]!! }

        /** 天干比劫处数（跳日干）。 */
        val biStems = bazi.stems.indices.count { it != 2 && bazi.stems[it].element == biEl }
        /** 支本气比劫柱位。 */
        fun benQiBiPillars(): List<Int> = bazi.branches.indices.filter {
            branchHiddenStems.getValue(bazi.branches[it]).firstOrNull()?.element == biEl
        }
        /** 支藏比劫（任意藏干，含中余气——排行/墓门用）。 */
        val biHiddenAnywhere = bazi.branches.any { br ->
            branchHiddenStems.getValue(br).any { it.element == biEl }
        }
        /** 天干印数 + 支本气印数（与比劫计数对称——干印+本气印）。 */
        val yinEl = elementOf(GodCategory.YIN, dayEl)
        val yinStems = bazi.stems.indices.count { it != 2 && bazi.stems[it].element == yinEl }
        val yinHidden = bazi.branches.count { br ->
            branchHiddenStems.getValue(br).firstOrNull()?.element == yinEl
        }

        // ── 手足数目 ──
        val count: Int
        val basis = StringBuilder()
        val biBenQi = benQiBiPillars()
        if (bing.congGe != null && biBenQi.isEmpty()) { // 日干从=比劫无本气根（有禄根不作从论）
            // 从格：日干从什么就以什么为手足——克泄之行按合并单元计
            // （支六合对各 1、单支各 1，日支计入；干五合对各 1，日主与印行之干不计）
            var units = 0
            val usedBranch = mutableSetOf<Int>()
            HeHuaCalculator.analyze(bazi).branchLiuHeList.forEach { he ->
                val i1 = bazi.branches.indexOfFirst { it == he.branch1 }
                val i2 = bazi.branches.indexOfFirst { it == he.branch2 }
                if (i1 >= 0 && i2 >= 0 && i1 != i2 && i1 !in usedBranch && i2 !in usedBranch) {
                    units += 1; usedBranch += i1; usedBranch += i2
                }
            }
            bazi.branches.indices.forEach { i -> if (i !in usedBranch) units += 1 }
            val stemChars = bazi.stems.map { it.chinese.first() }
            val countedStems = bazi.stems.indices.filter {
                it != 2 && bazi.stems[it].element != biEl && bazi.stems[it].element != yinEl
            }
            units += countedStems.size
            val wuHe = listOf(
                setOf('甲', '己'), setOf('乙', '庚'), setOf('丙', '辛'),
                setOf('丁', '壬'), setOf('戊', '癸'),
            )
            val paired = mutableSetOf<Int>()
            wuHe.forEach { pair ->
                val i1 = countedStems.firstOrNull { stemChars[it] in pair } ?: -1
                val i2 = countedStems.lastOrNull { stemChars[it] in pair } ?: -1
                if (i1 >= 0 && i2 >= 0 && i1 != i2 && i1 !in paired && i2 !in paired) {
                    units -= 1 // 两干合化为一单元（先前各计 1，合并减 1）
                    paired += i1; paired += i2
                }
            }
            count = units
            basis.append("从格——日干从什么就以什么为手足，按从势合并单元计数")
                .append("（六合/干五合之对合并为1数，日主与印行之干不计）得${units}数")
        } else {
            var n = 1 /*日主*/ + biStems + biBenQi.size
            basis.append("日主1+干比劫${biStems}+支本气比劫${biBenQi.size}")
            when (bing.strength) {
                DayStrength.WEAK -> {
                    val yin = yinStems + yinHidden
                    if (yin > 0) {
                        n += yin
                        basis.append("；身弱印比合计加印${yin}（干${yinStems}+藏${yinHidden}）")
                    }
                }
                else -> {}
            }
            // 加倍：比劫旺相（当令或得月令之生）
            val monthEl = bazi.monthPillar.branch.element
            val dangLing = monthEl == biEl || monthEl.generates == biEl
            if (dangLing) {
                n *= 2
                basis.append("；比劫旺相（${monthEl.chinese}月${if (monthEl == biEl) "当令" else "相地"}）加倍→${n}")
            }
            // 减半：不当令+数众+克泄耗行力≥比劫行力
            if (!dangLing && n >= 5 && keXieHao >= biForce) {
                n = Math.round(n / 2.0).toInt()
                basis.append("；比劫不当令（${monthEl.chinese}月）数众而与克泄耗之行相战（${keXieHao}对${biForce}）减半→${n}")
            }
            // 空亡：比劫本气支落旬空每支减1（禄支为实根"铁哥们"，落空亦以实计）
            val kong = KongWangCalculator.analyze(bazi)
            val luBranch = ShenShaCalculator.analyze(bazi).items
                .firstOrNull { it.name == "禄神" }?.branch
            val kongBi = benQiBiPillars().count { kong.isKong(it) && bazi.branches[it] != luBranch }
            if (kongBi > 0) {
                n -= kongBi
                basis.append("；比劫支旬空减${kongBi}→${n}")
            }
            // 墓门开：比劫行墓支藏比劫且被冲减1
            val tomb = LiuQinXiJi.tombOf[biEl]!!
            val chongLocs = DiZhiRelationCalculator.analyze(bazi).chongList.map { it.location }
            val tombIdx = bazi.branches.indices.firstOrNull { bazi.branches[it] == tomb }
            if (tombIdx != null && branchHiddenStems.getValue(tomb).any { it.element == biEl } &&
                chongLocs.any { loc -> loc.contains(listOf("年支", "月支", "日支", "时支")[tombIdx]) }
            ) {
                n -= 1
                basis.append("；${tomb.chinese}为比劫之墓而藏比劫、墓门被冲开减1→${n}")
            }
            count = n.coerceAtLeast(0)
        }

        // ── 排行：年/月柱见比劫（含月支藏比肩）→ 非老大 ──
        val ranking: String? = run {
            val yearBi = biStemsAt(bazi, 0, biEl) || branchHiddenStems.getValue(bazi.branches[0]).firstOrNull()?.element == biEl
            val monthBi = biStemsAt(bazi, 1, biEl) ||
                branchHiddenStems.getValue(bazi.branches[1]).any { it.element == biEl }
            when {
                yearBi -> "年柱见比劫——自己不是老大"
                monthBi -> "月柱（月令）藏比劫——自己不是老大"
                else -> null
            }
        }

        // ── 断语 ──
        val fav = mutableListOf<LiuQinItem>()
        val unfav = mutableListOf<LiuQinItem>()
        val ssItems = ShenShaCalculator.analyze(bazi).items
        val biPillarBranches = benQiBiPillars()
        fun ssOnBiBranch(name: String): Boolean =
            ssItems.any { ss -> ss.name == name && biPillarBranches.any { bazi.branches[it] == ss.branch } }

        if (biForce >= 3.0 || count >= 5) fav += LiuQinItem("比劫多而旺", "四柱比劫多而旺——兄弟姐妹多。")
        if (bing.strength == DayStrength.WEAK && forces[yinEl]!! >= 2.5) {
            fav += LiuQinItem("身弱印旺", "身弱印旺——兄弟姐妹亦多（印比同气）。")
        }
        if (biForce >= 2.5 && bing.strength != DayStrength.WEAK) {
            fav += LiuQinItem("比劫生旺", "四柱比劫生旺——兄弟姐妹身健寿高。")
        }
        if (GodCategory.BI in d.fav) {
            fav += LiuQinItem("比劫喜用", "比劫为喜用——多得兄弟姐妹之助，能照顾呵护弟妹。")
        }
        if (GodCategory.BI in d.unf) {
            unfav += LiuQinItem(
                "比劫为忌",
                "比劫为忌——兄弟不宜合伙做事，否则不利；朋友对事业前途无实质帮助，反为其拖累，宜独立创业。",
            )
        }
        // 旺相度与财从专旺（计数块外亦需判定）
        val monthElX = bazi.monthPillar.branch.element
        val dangLingX = monthElX == biEl || monthElX.generates == biEl
        val caiElX = elementOf(GodCategory.CAI, dayEl)
        val caiForceX = forces[caiElX]!!
        val zhuanWang = bing.congGe?.kind == cn.xulun.bazi.model.CongGeKind.CONG_STRONG ||
            (biForce >= 5.0 && caiForceX <= 1.5 && dangLingX)
        val caiTou = bazi.stems.indices.any { it != 2 && bazi.stems[it].element == caiElX }
        if (zhuanWang) {
            fav += LiuQinItem("众比成林·财从", "比劫成林（专旺）而财从——父母家无多遗产可争，兄弟各自发奋，关系融洽，同甘共苦。")
        } else if (biForce >= 3.0 && caiTou && caiForceX <= biForce / 2 && GodCategory.BI in d.unf) {
            unfav += LiuQinItem(
                "群比夺财",
                "财弱而透、比劫旺（${biForce}对${caiForceX}）——群比夺财，兄弟吃喝玩乐耗费家财，反目失和；遗产不多反成争夺之源。",
            )
        }
        if (ssOnBiBranch("桃花")) fav += LiuQinItem("比劫临桃花", "比劫临桃花——兄弟姐妹中有重情风流之人。")
        if (ssOnBiBranch("天乙贵人") || ssOnBiBranch("天德贵人") || ssOnBiBranch("月德贵人")) {
            fav += LiuQinItem("比劫临贵人", "比劫临天乙/天德/月德——兄弟姐妹仁慈。")
        }
        if (ssOnBiBranch("将星") && biForce >= 2.5) {
            fav += LiuQinItem("比劫临将星·生旺", "比劫临将星而生旺——兄弟姐妹多贵。")
        }
        if (ssOnBiBranch("驿马")) {
            fav += LiuQinItem("比劫临驿马", "比劫临驿马——兄弟姐妹多迁移走动，逢冲更甚。")
        }
        // 比劫坐禄（禄支=本气比劫支）
        if (biPillarBranches.isNotEmpty()) {
            fav += LiuQinItem("比劫坐禄", "比劫坐禄（地支本气比劫）——兄弟姐妹有力富裕之象。")
        }
        // 月柱官印/食财为喜用
        val monthStemCat = tenGodOfStem(day, bazi.monthPillar.stem).category()
        val monthBenCat = tenGodOfStem(day, branchHiddenStems.getValue(bazi.monthPillar.branch).first()).category()
        if ((monthStemCat == GodCategory.GUAN || monthStemCat == GodCategory.YIN ||
                monthBenCat == GodCategory.GUAN || monthBenCat == GodCategory.YIN) &&
            (monthStemCat in d.fav || monthBenCat in d.fav)
        ) {
            fav += LiuQinItem("月柱临官印·喜用", "月柱临官印为喜用——兄弟姐妹多贵。")
        }
        if ((monthStemCat == GodCategory.SHI || monthStemCat == GodCategory.CAI ||
                monthBenCat == GodCategory.SHI || monthBenCat == GodCategory.CAI) &&
            (monthStemCat in d.fav || monthBenCat in d.fav)
        ) {
            fav += LiuQinItem("月柱临食财·喜用", "月柱临食财为喜用——兄弟姐妹多富。")
        }
        // 月日柱相生 + 比劫喜用
        val monthBrEl = bazi.monthPillar.branch.element
        val dayBrEl = bazi.dayPillar.branch.element
        if ((monthBrEl.generates == dayBrEl || dayBrEl.generates == monthBrEl) && GodCategory.BI in d.fav) {
            fav += LiuQinItem("月日相生·比劫喜用", "月日柱相生且比劫为喜用——兄弟姐妹感情融洽，互有助益。")
        }

        // ── 不利 ──
        if (forces[elementOf(GodCategory.GUAN, dayEl)]!! >= 3.0 && GodCategory.GUAN in d.unf) {
            unfav += LiuQinItem("官杀旺为忌", "官杀旺而为忌——不利兄弟姐妹。")
        }
        val yearStemGod = tenGodOfStem(day, bazi.stems[0])
        val yearBenGod = tenGodOfStem(day, branchHiddenStems.getValue(bazi.branches[0]).first())
        if ((yearStemGod == TenGod.SHANG_GUAN || yearBenGod == TenGod.SHANG_GUAN) && GodCategory.SHI in d.unf) {
            unfav += LiuQinItem("年柱伤官为忌", "年柱伤官为忌——兄弟姐妹有损。")
        }
        if (count in 1..2 && biForce <= 1.5) {
            unfav += LiuQinItem("比劫衰而少", "四柱比劫衰而少——兄弟姐妹少。")
        }
        if (ssItems.any { it.name == "华盖" } && count <= 2) {
            unfav += LiuQinItem("华盖而比劫少", "四柱带华盖而比劫少——兄弟姐妹少。")
        }
        if (benQiBiPillars().any { KongWangCalculator.analyze(bazi).isKong(it) } && biForce <= 2.0) {
            unfav += LiuQinItem("比劫休囚旬空", "比劫休囚处旬空——兄弟姐妹少。")
        }
        // 克泄耗忌：身弱，或中和偏弱（扶耗两侧耗>扶）
        val engX = StrengthEngine.analyze(bazi)
        val keXieHaoJi = bing.strength == DayStrength.WEAK ||
            (bing.strength == DayStrength.BALANCED && engX.aSide < engX.bSide)
        if (yearStemGod == TenGod.QI_SHA &&
            (tenGodOfStem(day, bazi.monthPillar.stem) == TenGod.SHANG_GUAN) &&
            keXieHaoJi
        ) {
            unfav += LiuQinItem("年杀月伤为忌", "年逢七杀、月逢伤官为忌（伤官制杀显无礼凶暴）——兄弟不睦，兄弟中易出不孝之人。")
        }
        val diZhiRel = DiZhiRelationCalculator.analyze(bazi)
        // 三刑组两支亦成刑（丑戌/戌未/未丑、寅巳/巳申/寅申——丑戌刑兄弟宫式，全齐三刑之外）
        val sanXingGroups = listOf(
            setOf(cn.xulun.bazi.model.EarthlyBranch.CHOU, cn.xulun.bazi.model.EarthlyBranch.XU, cn.xulun.bazi.model.EarthlyBranch.WEI),
            setOf(cn.xulun.bazi.model.EarthlyBranch.YIN, cn.xulun.bazi.model.EarthlyBranch.SI, cn.xulun.bazi.model.EarthlyBranch.SHEN),
        )
        val monthBranch = bazi.monthPillar.branch
        val monthXingPair = sanXingGroups.any { g -> monthBranch in g && bazi.branches.count { it in g } >= 2 }
        val monthChongXing = diZhiRel.chongList.any { it.location.contains("月支") } ||
            diZhiRel.xingList.any { it.location.contains("月支") } || monthXingPair
        if (monthChongXing) {
            unfav += LiuQinItem("月柱逢刑冲", "月令遭刑冲——兄弟宫被动，命主与兄弟背井离乡外出创业，天各一方，难聚首。")
        }
        if (biForce >= 4.0 && bing.strength == DayStrength.STRONG) {
            unfav += LiuQinItem("日主旺比劫重叠", "日主旺而比劫重叠——兄弟姐妹不和或败家。")
        }
        if (bing.strength == DayStrength.STRONG && ssItems.any { it.name == "羊刃" }) {
            unfav += LiuQinItem("日主旺逢羊刃", "日主旺而逢羊刃——兄弟姐妹不和或败家。")
        }
        // 比劫冲损用神
        val favBranchElements = d.fav.map { elementOf(it, dayEl) }.toSet()
        val chongLocs2 = diZhiRel.chongList
        if (chongLocs2.any { ch ->
                val b1God = branchHiddenStems.getValue(ch.branch1).firstOrNull()?.let { it.element }
                val b2God = branchHiddenStems.getValue(ch.branch2).firstOrNull()?.let { it.element }
                (b1God == biEl && ch.branch2.element in favBranchElements) ||
                    (b2God == biEl && ch.branch1.element in favBranchElements)
            }
        ) {
            unfav += LiuQinItem("比劫冲损用神", "比劫冲损用神——易为兄弟姐妹所累。")
        }
        // 比劫衰弱被重克或入墓
        val keEl = Element.entries.first { it.controls == biEl }
        val tombBi = LiuQinXiJi.tombOf[biEl]!!
        if (biForce <= 1.5 && (forces[keEl]!! >= 2.0 * biForce || bazi.branches.any { it == tombBi && biHiddenAnywhere })) {
            unfav += LiuQinItem("比劫衰遭重克入墓", "比劫衰弱被重克或入墓——兄弟姐妹中有伤残或夭折之人。")
        }

        val genderNote = if (gender == Gender.MALE) "比肩为兄弟、劫财为姐妹" else "比肩为姐妹、劫财为兄弟"
        return XiongDiResult(
            count = "${count}个左右",
            countBasis = basis.toString(),
            ranking = ranking,
            favorable = fav,
            unfavorable = unfav,
            summary = "手足星比劫（${biEl.chinese}），${genderNote}；禄为比肩、刃为劫财，身弱印比同气。" +
                "计数：${basis}。实际数目尚须结合少运（首步大运帮比劫则增、克泄则减）与国运综合判断。",
        )
    }

    private fun biStemsAt(bazi: BaziResult, idx: Int, biEl: Element): Boolean =
        bazi.stems[idx].element == biEl
}
