package cn.xulun.bazi.core

import cn.xulun.bazi.core.LiuQinXiJi.category
import cn.xulun.bazi.core.LiuQinXiJi.elementOf
import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.FuMuResult
import cn.xulun.bazi.model.Gender
import cn.xulun.bazi.model.GodCategory
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.LiuQinItem
import cn.xulun.bazi.model.Nayin
import cn.xulun.bazi.model.TenGod
import cn.xulun.bazi.model.branchHiddenStems
import cn.xulun.bazi.model.tenGodOfStem

/**
 * 父母断语引擎（.liuqin-reference.md 第三节）。父母星为主、父母宫（年柱）为辅：
 * 父星男命偏财、女命正财；母星男命正印、女命偏印。
 *
 * 关键技法：
 * - **真从**——星无透干无本气根（仅藏余/中气或全无）即从：从象不伤父母，反主有地位能力（例二/三/十二/十三）
 * - **旬空**——星本气根支落旬空：父空则无，不死即远离（例十四 卯木旬空）
 * - **坐墓**——星透干坐本行墓支：先天克亲信息（例十九 母星乙坐未墓）
 * - **众寡不敌**——星有根但力≤1.5 且克泄耗之行合计≥2.5 倍：土旺木折式克亲（例十四/十八）
 * - **财印强弱比较**——弱者之亲先逝倾向（例四 父先、例二十三 母先）
 *
 * 岁运应期（断语表 23~26 条）属动态批断，不在本引擎范围。
 * 喜忌由 [LiuQinXiJi] 教材口径派生。
 */
object FuMuCalculator {

    fun analyze(bazi: BaziResult, gender: Gender): FuMuResult {
        val day = bazi.dayPillar.stem
        val dayEl = day.element
        val d = LiuQinXiJi.derive(bazi)
        val forces = BingYaoCalculator.forces(bazi)

        // ── 父母星 ──
        val fatherGod = if (gender == Gender.MALE) TenGod.PIAN_CAI else TenGod.ZHENG_CAI
        val motherGod = if (gender == Gender.MALE) TenGod.ZHENG_YIN else TenGod.PIAN_YIN
        val caiEl = elementOf(GodCategory.CAI, dayEl)
        val yinEl = elementOf(GodCategory.YIN, dayEl)

        /** 星透干柱位（跳日干）。 */
        fun stemPillars(god: TenGod): List<Int> =
            bazi.stems.indices.filter { it != 2 && tenGodOfStem(day, bazi.stems[it]) == god }

        /** 星本气根柱位。 */
        fun benQiPillars(god: TenGod): List<Int> =
            bazi.branches.indices.filter { pillarIdx ->
                val hs = branchHiddenStems.getValue(bazi.branches[pillarIdx])
                hs.isNotEmpty() && tenGodOfStem(day, hs.first()) == god
            }

        /** 星现于柱中任意位置（含藏干）。 */
        fun anywhere(god: TenGod): Boolean =
            stemPillars(god).isNotEmpty() || benQiPillars(god).isNotEmpty() ||
                bazi.branches.any { br -> branchHiddenStems.getValue(br).any { tenGodOfStem(day, it) == god } }

        /** 真从：正偏星行俱不透干且无本气根（仅藏余/中气或全无）；或星透干但坐绝地被克尽（例十二 甲坐申、甲庚冲尽）。 */
        fun zhenCong(god: TenGod, starEl: Element): Boolean {
            if (benQiPillars(god).isNotEmpty()) return false
            val catStems = bazi.stems.indices.filter { it != 2 && bazi.stems[it].element == starEl }
            if (catStems.isEmpty()) return true
            // 透干坐绝且克星之行力≥2倍——彻底冲尽，从旺反不伤
            val jue = LiuQinXiJi.jueOf[starEl]!!
            val keEl = Element.entries.first { it.controls == starEl }
            return catStems.isNotEmpty() && catStems.all { bazi.branches[it] == jue } &&
                forces[keEl]!! >= 2.0 * maxOf(forces[starEl]!!, 0.5)
        }

        val fatherCong = zhenCong(fatherGod, caiEl)
        val motherCong = zhenCong(motherGod, yinEl)
        val caiForce = forces[caiEl]!!
        val yinForce = forces[yinEl]!!

        // 神煞（年/月柱）与冲刑合
        val ssItems = ShenShaCalculator.analyze(bazi).items
        fun ssAt(names: Set<String>, pillars: Set<String>) =
            ssItems.filter { it.name in names && pillars.any { p -> it.pillarLabel.startsWith(p) } }
        val chong = DiZhiRelationCalculator.analyze(bazi)
        val chongLoc = chong.chongList.map { it.location } + chong.xingList.map { it.location }
        fun branchChonged(idx: Int): Boolean {
            val label = listOf("年支", "月支", "日支", "时支")[idx]
            return chongLoc.any { it.contains(label) }
        }
        val kong = KongWangCalculator.analyze(bazi)
        val liuHe = HeHuaCalculator.analyze(bazi).branchLiuHeList
        val pillarsLabel = listOf("年", "月", "日", "时")

        val fav = mutableListOf<LiuQinItem>()
        val unfav = mutableListOf<LiuQinItem>()

        // ── 寿长主线 ──
        val caiWang = caiForce >= 3.0
        val yinWang = yinForce >= 3.0
        if (caiWang && yinWang) {
            fav += LiuQinItem("财印两旺·双亲高寿", "柱中财印两旺，父母均高寿。")
        } else {
            if (caiWang) fav += LiuQinItem("财旺·父寿长", "柱中财旺，父长寿。")
            if (yinWang) fav += LiuQinItem("印旺·母寿长", "柱中印旺，母长寿。")
        }

        // ── 真从：从象不伤，反主有地位 ──
        // 从得不真（例十五式引申）：星缺位（正偏俱不透无本气根）而其行有力≥2——行在星虚，
        // 从得不真即克该亲；为忌则缘薄之极（虐待档：坤造甲戌辛未戊午戊午 母星火缺位而午午有力且为忌——幼年受母虐待实锚）
        val yinBuTou = bazi.stems.indices.none {
            it != 2 && (tenGodOfStem(day, bazi.stems[it]) == TenGod.ZHENG_YIN || tenGodOfStem(day, bazi.stems[it]) == TenGod.PIAN_YIN)
        }
        val motherDeBuZhen = motherCong && yinForce >= 2.0 && yinBuTou
        val caiBuTou = bazi.stems.indices.none {
            it != 2 && (tenGodOfStem(day, bazi.stems[it]) == TenGod.ZHENG_CAI || tenGodOfStem(day, bazi.stems[it]) == TenGod.PIAN_CAI)
        }
        val fatherDeBuZhen = fatherCong && caiForce >= 2.0 && caiBuTou
        if (fatherCong && !fatherDeBuZhen) {
            fav += LiuQinItem(
                "父星真从",
                "父星（${caiEl.chinese}）无透干无本气根，从得真——从象不伤父母，" +
                    "反主父为有名望、有能力、有地位之人，父母康健高寿之象（财从反不伤财）。",
            )
        }
        if (motherCong && !motherDeBuZhen) {
            fav += LiuQinItem(
                "母星真从",
                "母星（${yinEl.chinese}）无透干无本气根，从得真——从象不伤，" +
                    "反主母为有地位、有能力之人，父母康健高寿之象。",
            )
        }
        if (motherDeBuZhen) {
            unfav += LiuQinItem(
                "母星从得不真" + (if (GodCategory.YIN in d.unf) "·为忌" else ""),
                "母星（${yinEl.chinese}）正偏皆不透干、无本气根，而印行之力达${yinForce}——星缺位而行有力，从得不真" +
                    "（例十五式：从得不真即克该亲）" +
                    (if (GodCategory.YIN in d.unf)
                        "；印又为忌神——母缘薄之极：生而不慈、幼年难得母爱，甚者受母虐待之应。"
                    else "——母缘偏薄，母女聚少离多。"),
            )
        }
        if (fatherDeBuZhen) {
            unfav += LiuQinItem(
                "父星从得不真" + (if (GodCategory.CAI in d.unf) "·为忌" else ""),
                "父星（${caiEl.chinese}）正偏皆不透干、无本气根，而财行之力达${caiForce}——星缺位而行有力，从得不真" +
                    (if (GodCategory.CAI in d.unf)
                        "；财又为忌神——父缘薄之极，早年与父缘尽之应。"
                    else "——父缘偏薄，父子聚少离多。"),
            )
        }

        // ── 父母星居父母宫且旺相 ──
        val fatherInYear = stemPillars(fatherGod).contains(0) || benQiPillars(fatherGod).contains(0)
        val motherInYear = stemPillars(motherGod).contains(0) || benQiPillars(motherGod).contains(0)
        if (fatherInYear && caiForce >= 2.5) {
            fav += LiuQinItem("父星居年柱·旺相", "父星居于父母宫（年柱）且旺相——父为有能力、有名望与地位之人。")
        }
        if (motherInYear && yinForce >= 2.5) {
            fav += LiuQinItem("母星居年柱·旺相", "母星居于父母宫（年柱）且旺相——母为有能力、有名望与地位之人。")
        }

        // ── 父母星旺相不遭冲克 ──
        if (caiForce >= 2.5 && benQiPillars(fatherGod).all { !branchChonged(it) }) {
            fav += LiuQinItem("父星旺相不遭冲", "父星旺相而不遭冲克——父为健康长寿之人。")
        }
        if (yinForce >= 2.5 && benQiPillars(motherGod).all { !branchChonged(it) }) {
            fav += LiuQinItem("母星旺相不遭冲", "母星旺相而不遭冲克——母为健康长寿之人。")
        }

        // ── 年月柱贵人 ──
        val guiRen = ssAt(setOf("天乙贵人", "天德贵人", "月德贵人"), setOf("年", "月"))
        if (guiRen.isNotEmpty()) {
            fav += LiuQinItem(
                "年月贵人",
                "年月柱见${guiRen.distinctBy { it.name }.joinToString("、") { it.name }}——" +
                    "多主双亲貌秀文雅，心地善良，为行善积德之人。",
            )
        }

        // ── 父星临天乙贵人 ──
        val tianYi = ssItems.filter { it.name == "天乙贵人" }
        val fatherBranches = benQiPillars(fatherGod) +
            bazi.branches.indices.filter { i -> branchHiddenStems.getValue(bazi.branches[i]).any { tenGodOfStem(day, it) == fatherGod } }
        if (tianYi.any { ty -> fatherBranches.any { bazi.branches[it] == ty.branch } } && !fatherCong) {
            fav += LiuQinItem("父星临天乙", "父星临天乙贵人——父亲为有名望之人。")
        }

        // ── 年柱旺相为父星之官星 ──
        val yearEl = bazi.yearPillar.stem.element
        if (yearEl.controls == caiEl && fatherBranches.isNotEmpty() && !fatherCong &&
            forces[yearEl]!! >= 2.0
        ) {
            fav += LiuQinItem(
                "年柱为父星之官星",
                "年柱旺相（${yearEl.chinese}），为财（父）星之官星——父辈中有权力和地位之人。",
            )
        }

        // ── 年柱生合日干为喜用 ──
        val yearStemGod = tenGodOfStem(day, bazi.yearPillar.stem)
        if ((bazi.yearPillar.stem.element.generates == dayEl || bazi.yearPillar.branch.element.generates == dayEl) &&
            yearStemGod.category() in d.fav
        ) {
            fav += LiuQinItem(
                "年柱生合日干·喜用",
                "年柱生合日干而为喜用——得父母疼爱有加，父母省吃俭用亦不愿委屈命主。",
            )
        } else if (dayEl.generates == bazi.yearPillar.stem.element) {
            fav += LiuQinItem("日元生年柱", "日元生年柱——命主孝顺父母。")
        }

        // ── 月支为喜用 ──
        val monthBenQiGod = tenGodOfStem(day, branchHiddenStems.getValue(bazi.monthPillar.branch).first())
        if (monthBenQiGod.category() in d.fav) {
            fav += LiuQinItem("月支喜用", "月支为喜用——得父母疼爱。")
        }

        // ── 年柱印星喜用临文昌学堂 ──
        if (yearStemGod.category() == GodCategory.YIN && GodCategory.YIN in d.fav &&
            ssAt(setOf("文昌", "学堂"), setOf("年")).isNotEmpty()
        ) {
            fav += LiuQinItem("年柱印星喜用·文昌", "年柱坐印星为喜用，又临文昌学堂——祖上或父母多为文职之人。")
        }

        // ── 年柱食神旺而喜用 ──
        if ((yearStemGod == TenGod.SHI_SHEN || tenGodOfStem(day, branchHiddenStems.getValue(bazi.yearPillar.branch).first()) == TenGod.SHI_SHEN) &&
            GodCategory.SHI in d.fav && forces[bazi.yearPillar.branch.element]!! >= 2.5
        ) {
            fav += LiuQinItem("年柱食神旺·喜用", "年柱逢食神旺而为喜用——双亲忠厚诚信，身体健壮。")
        }

        // ── 年干官星喜用坐将星 ──
        if (yearStemGod.category() == GodCategory.GUAN && GodCategory.GUAN in d.fav &&
            ssAt(setOf("将星"), setOf("年支")).isNotEmpty()
        ) {
            fav += LiuQinItem("年干官星喜用·坐将星", "年干官星为喜用，支坐将星——双亲位显有威。")
        }

        // ── 年柱桃花生旺 ──
        if (ssAt(setOf("桃花"), setOf("年")).isNotEmpty() && forces[bazi.yearPillar.branch.element]!! >= 2.0) {
            fav += LiuQinItem("年柱桃花生旺", "年柱临桃花而生旺——双亲貌美聪慧。")
        }

        // ── 年柱禄马为喜用 ──
        if (ssAt(setOf("禄神", "驿马"), setOf("年")).isNotEmpty() && yearStemGod.category() in d.fav) {
            fav += LiuQinItem("年柱禄马·喜用", "年柱临禄马为喜用——双亲富贵荣显。")
        }

        // ── 年柱干支关系 ──
        val ysEl = bazi.yearPillar.stem.element
        val ybEl = bazi.yearPillar.branch.element
        when {
            ysEl.generates == ybEl || ybEl.generates == ysEl ->
                fav += LiuQinItem("年柱干支相生", "年柱干支相生——父母恩爱。")
            ysEl.controls == ybEl || ybEl.controls == ysEl ->
                unfav += LiuQinItem("年柱干支相克", "年柱干支相克——父母感情不和。")
            ysEl == ybEl ->
                fav += LiuQinItem("年柱干支比和", "年柱干支比和——父母虽恩爱但多口舌。")
        }

        // ── 父母离异·随喜用之亲（年柱相克+财印一缺位一忌旺——强弱悬殊缘尽；坤造甲戌辛未戊午戊午实锚：随父） ──
        val nianZhuKe = ysEl.controls == ybEl || ybEl.controls == ysEl
        if (nianZhuKe && fatherCong && GodCategory.YIN in d.unf && yinForce >= 2.0 && GodCategory.CAI in d.fav) {
            unfav += LiuQinItem(
                "父母离异·随父",
                "年柱干支相克，而财（父）星缺位从绝、印（母）星旺而为忌——父母强弱悬殊、缘分已尽：" +
                    "父母离异之应，命主随父生活（父星为喜用——与父缘深，得父抚养）。",
            )
        }
        if (nianZhuKe && motherCong && GodCategory.CAI in d.unf && caiForce >= 2.0 && GodCategory.YIN in d.fav) {
            unfav += LiuQinItem(
                "父母离异·随母",
                "年柱干支相克，而印（母）星缺位从绝、财（父）星旺而为忌——父母强弱悬殊、缘分已尽：" +
                    "父母离异之应，命主随母生活（母星为喜用——与母缘深，得母抚养）。",
            )
        }

        // ── 父母星喜忌与缘深缘薄 ──
        if (GodCategory.CAI in d.fav) fav += LiuQinItem("父星喜用·缘深", "父星为喜用神——与父缘深，小时能得父亲宠爱与呵护。")
        if (GodCategory.YIN in d.fav) fav += LiuQinItem("母星喜用·缘深", "母星为喜用神——与母缘深，小时能得母亲宠爱与呵护。")
        if (GodCategory.CAI in d.unf) unfav += LiuQinItem("父星忌·缘薄", "父星为忌神——与父缘薄，事业上难得父亲之助。")
        if (GodCategory.YIN in d.unf) unfav += LiuQinItem("母星忌·缘薄", "母星为忌神——与母缘分稍浅，聚少离多；事业上难得母亲之力。")

        // ══ 不利线 ══

        // 比劫叠叠为忌——早年亡父（父星真从则不克：例十三沙漠饿汉）
        if (forces[dayEl]!! >= 4.0 && GodCategory.BI in d.unf && !fatherCong) {
            unfav += LiuQinItem("比劫叠叠为忌", "四柱比劫叠叠为忌——早年亡父之象。")
        }

        // 财旺为忌印弱——早年丧母（财坏印）
        if (caiForce >= 3.0 && GodCategory.CAI in d.unf && yinForce <= 2.0 && !motherCong) {
            unfav += LiuQinItem("财旺为忌坏印", "四柱财星旺而为忌，印星受制——早年丧母之象。")
        }

        // 月支提纲遭刑冲——不利父母
        if (branchChonged(1)) {
            unfav += LiuQinItem("提纲遭刑冲", "月柱提纲遭刑冲——不利父母。")
        }

        // 月令克年柱纳音——不利父母
        val nianYinEl = Nayin.fromCycleIndex(bazi.yearPillar.cycleIndex).element
        if (bazi.monthPillar.branch.element.controls == nianYinEl) {
            unfav += LiuQinItem("月令克年柱纳音", "月令克年柱纳音（${nianYinEl.chinese}）——不利父母。")
        }

        // 星透干坐本行墓——克亲（例十九 母星乙坐未墓）
        fun tombItem(god: TenGod, starEl: Element, who: String, cong: Boolean) {
            if (cong) return
            val tomb = LiuQinXiJi.tombOf[starEl]!!
            val shengEl = Element.entries.first { it.generates == starEl }
            if (forces[shengEl]!! >= 1.5) return // 有源头生扶不为衰（例二十三 戊坐辰得午火生）
            stemPillars(god).forEach { idx ->
                if (bazi.branches[idx] == tomb && forces[starEl]!! <= 2.0) {
                    unfav += LiuQinItem(
                        "${who}星坐墓·克${who}",
                        "${who}星${bazi.stems[idx].chinese}透干坐${tomb.chinese}墓地，衰而入墓——" +
                            "先天带有克${who}信息，${who}多病或早逝。",
                    )
                }
            }
        }
        tombItem(fatherGod, caiEl, "父", fatherCong)
        tombItem(motherGod, yinEl, "母", motherCong)

        // 星本气根支旬空——不死即远离（例十四 卯木旬空）
        fun kongItem(god: TenGod, who: String, cong: Boolean) {
            if (cong) return
            benQiPillars(god).forEach { idx ->
                if (kong.isKong(idx)) {
                    val br = bazi.branches[idx]
                    val heKe = liuHe.any { (it.branch1 == br || it.branch2 == br) && it.location.contains(pillarsLabel[idx]) }
                    unfav += LiuQinItem(
                        "${who}星旬空",
                        "${who}星${br.chinese}旬空——${who}空则无，不死即远离" +
                            (if (heKe) "；且遭克合，土旺木折，早岁克${who}之象" else "") + "。",
                    )
                }
            }
        }
        kongItem(fatherGod, "父", fatherCong)
        kongItem(motherGod, "母", motherCong)

        // 星弱坐绝地——早亡
        fun jueItem(god: TenGod, starEl: Element, who: String, cong: Boolean) {
            if (cong) return
            val jue = LiuQinXiJi.jueOf[starEl]!!
            if (forces[starEl]!! <= 1.5 && stemPillars(god).any { bazi.branches[it] == jue }) {
                unfav += LiuQinItem("${who}星弱坐绝", "${who}星弱而坐绝地（${jue.chinese}）——${who}早亡之象。")
            }
        }
        jueItem(fatherGod, caiEl, "父", fatherCong)
        jueItem(motherGod, yinEl, "母", motherCong)

        // 众寡不敌——克亲（星有本气根但力≤1.5，克泄耗之行合计≥2.5倍：土旺木折式）
        fun guaItem(god: TenGod, starEl: Element, who: String, cong: Boolean) {
            if (cong) return
            val fStar = forces[starEl]!!
            if (benQiPillars(god).isEmpty() || fStar > 1.5) return
            val keEl = Element.entries.first { it.controls == starEl }
            val xieEl = starEl.generates
            val haoEl = starEl.controls
            val crowd = forces[keEl]!! + forces[xieEl]!! + forces[haoEl]!!
            if (crowd >= 2.5 * fStar) {
                unfav += LiuQinItem(
                    "${who}星众寡不敌",
                    "${who}星（${starEl.chinese}）虽有根而力仅${fStar}，克泄耗之行合计${crowd}——" +
                        "众寡不敌（土旺木折式），早岁克${who}之象。",
                )
            }
        }
        guaItem(fatherGod, caiEl, "父", fatherCong)
        guaItem(motherGod, yinEl, "母", motherCong)

        // 财印强弱比较——弱者之亲先逝（例四父先/例二十三母先）
        if (!fatherCong && (caiForce > 0 || anywhere(fatherGod)) && yinForce >= 1.5 * maxOf(caiForce, 0.5) && yinForce - caiForce >= 1.0) {
            unfav += LiuQinItem(
                "财弱印强·父先",
                "财（父）星弱于印（母）星——母亲身体好，父亲身体欠佳，父有先于母逝之倾向。",
            )
        }
        if (!motherCong && (yinForce > 0 || anywhere(motherGod)) && caiForce >= 1.5 * maxOf(yinForce, 0.5) && caiForce - yinForce >= 1.0 &&
            (benQiPillars(motherGod).any { branchChonged(it) } || yinForce <= 1.5)
        ) {
            unfav += LiuQinItem(
                "印弱财强·母先",
                "印（母）星弱而无生又遭冲克，财（父）星强——父寿长于母，母有先亡之象。",
            )
        }

        // 年柱凶神为忌
        if (ssAt(setOf("羊刃"), setOf("年")).isNotEmpty() && GodCategory.BI in d.unf) {
            unfav += LiuQinItem("年柱羊刃为忌", "年柱带羊刃为忌——克父。")
        }
        if (ssAt(setOf("劫煞", "亡神"), setOf("年")).isNotEmpty() &&
            yearStemGod.category() in d.unf
        ) {
            unfav += LiuQinItem("年柱凶神为忌", "年柱带劫煞/亡神而为忌——不利父母。")
        }
        if ((yearStemGod == TenGod.QI_SHA || ssAt(setOf("羊刃"), setOf("年")).isNotEmpty()) &&
            yearStemGod.category() in d.unf && GodCategory.BI in d.unf
        ) {
            unfav += LiuQinItem("年柱杀刃为忌", "年柱临七杀/羊刃为忌——双亲性暴。")
        }

        val tendency = when {
            fav.size > unfav.size -> "父母缘分偏吉。"
            unfav.size > fav.size -> "父母线偏弱，须结合岁运定应期。"
            else -> "父母线吉凶参半，须结合岁运定应期。"
        }
        return FuMuResult(
            favorable = fav,
            unfavorable = unfav,
            summary = "父星${fatherGod.chinese}（${caiEl.chinese}）${if (fatherCong) "真从" else "力${caiForce}"}、" +
                "母星${motherGod.chinese}（${yinEl.chinese}）${if (motherCong) "真从" else "力${yinForce}"}；父母宫=年柱。" +
                "喜忌口径：${d.basis}。${tendency}",
        )
    }
}
