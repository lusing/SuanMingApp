package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.TeShuGeJuResult
import cn.xulun.bazi.model.TeShuKind
import cn.xulun.bazi.model.branchHiddenStems

/**
 * 特殊格局取用：专旺 / 假从（单从·杂从）/ 多从 / 战局通关 / 三气成象。
 * 理论见 `.shengke-reference.md` 第四九节（一）~（六）。
 *
 * 检测序：专旺（四支皆同我生我，含三合/三会变节支——从革炎上润下曲直稼穑）→
 * 从格（无有效根+日行力≤四成，透劫印被克泄殆尽者为假从；从势=有力诸行，
 * 相生者连成多从链，相战两强取通关）→ 三气成象（日主有力参与三行连环相生，
 * 弱者补之）。岁运还原：从格遇日主得禄之支 / 专旺成象遇破势之行——回正格（返回 null）。
 */
object TeShuGeJuCalculator {

    private val GRID_NAME = mapOf(
        Element.METAL to "从革格", Element.FIRE to "炎上格", Element.WATER to "润下格",
        Element.WOOD to "曲直格", Element.EARTH to "稼穑格",
    )

    /** 天干五合化神（甲己土、乙庚金、丙辛水、丁壬木、戊癸火）。 */
    private fun huaShenOf(a: cn.xulun.bazi.model.HeavenlyStem, b: cn.xulun.bazi.model.HeavenlyStem): Element {
        val lo = minOf(a.ordinal, b.ordinal)
        return when (lo) {
            0 -> Element.EARTH; 1 -> Element.METAL; 2 -> Element.WATER; 3 -> Element.WOOD; else -> Element.FIRE
        }
    }

    /** 五行相生序中 x 与 y 之间的通关神（x 生关，关生 y）。 */
    private fun tongGuan(x: Element, y: Element): Element? = Element.entries.firstOrNull { m ->
        x.generates == m && m.generates == y
    }

    /** 两气成象格名（相生五格+相克五格）。 */
    private val PAIR_NAME = mapOf(
        setOf(Element.WOOD, Element.FIRE) to "木火通明",
        setOf(Element.FIRE, Element.EARTH) to "火土成慈",
        setOf(Element.EARTH, Element.METAL) to "土金毓秀",
        setOf(Element.METAL, Element.WATER) to "金白水清",
        setOf(Element.WATER, Element.WOOD) to "水灵木秀",
        setOf(Element.METAL, Element.WOOD) to "金木栋梁",
        setOf(Element.WOOD, Element.EARTH) to "木土疏通",
        setOf(Element.EARTH, Element.WATER) to "土水池沼",
        setOf(Element.WATER, Element.FIRE) to "水火既济",
        setOf(Element.FIRE, Element.METAL) to "火金器皿",
    )

    /** 两气成象：日主行与相生邻行（印/食伤）双强均衡成象——弱者补之为首用（木旺火弱宜火运）。 */
    private fun liangQiOf(bazi: BaziResult, dayEl: Element, partner: Element, f: Map<Element, Double>): TeShuGeJuResult {
        // 弱旺直以合局转移后之力计（酉丑半合使金乘旺土不因丑令虚胖；疏通例盘木6土4直取土弱）
        val ruo = if (f[dayEl]!! <= f[partner]!!) dayEl else partner
        val wang = if (ruo == dayEl) partner else dayEl
        val name = (PAIR_NAME[setOf(dayEl, partner)] ?: "两气成象") + "格"
        val ji = Element.entries.filter { it != dayEl && it != partner }
        return TeShuGeJuResult(
            kind = TeShuKind.LIANG_QI, name = name,
            yongShen = listOf(ruo, wang), xi = listOf(wang),
            ji = ji, fake = false,
            restoreHint = "岁运强旺第三行则转三气成象（相生者）或正格（相战者）；衰弱第三行逆乱气势为忌",
            basis = "日主${dayEl.chinese}与${partner.chinese}两行气势强旺均衡、相互化生，" +
                "他行${ji.joinToString("") { it.chinese }}微弱不能克破——${name}（两气成象）。" +
                "${wang.chinese}偏旺${ruo.chinese}偏弱，宜行${ruo.chinese}运补弱为用（木旺火弱宜火、火旺木弱宜木）；" +
                "忌逆乱格局气势之衰弱第三行。",
        )
    }

    /** 化气格化神行：日干与年/月/时干五合且化神有力（≥2.5，变节力转移后）——乙庚化金式。 */
    fun huaShenElOf(bazi: BaziResult): Element? {
        val partner = listOf(bazi.yearPillar.stem, bazi.monthPillar.stem, bazi.hourPillar.stem).firstOrNull { g ->
            (g.ordinal - bazi.dayPillar.stem.ordinal + 10) % 5 == 0 && g.ordinal != bazi.dayPillar.stem.ordinal
        } ?: return null
        val hs = huaShenOf(partner, bazi.dayPillar.stem)
        val f0 = BingYaoCalculator.forces(bazi).toMutableMap()
        val heJu0 = DiZhiRelationCalculator.analyze(bazi).sanHeList.map { it.branches to it.resultElement } +
            HeHuaCalculator.analyze(bazi).branchSanHuiList.map { it.branches to it.resultElement }
        for ((bs, el) in heJu0) for (b in bs) {
            if (b.element != el && b in bazi.branches) { f0[b.element] = f0[b.element]!! - 1; f0[el] = f0[el]!! + 1 }
        }
        val rulerB0 = bazi.monthPillar.branch
        heJu0.firstOrNull { (bs, el) -> rulerB0 in bs && el != rulerB0.element }?.let { (_, el) ->
            f0[rulerB0.element] = f0[rulerB0.element]!! - 2; f0[el] = f0[el]!! + 2
        }
        return hs.takeIf { f0[it]!! >= 2.5 }
    }

    fun analyze(bazi: BaziResult, extraPillars: List<Pillar> = emptyList()): TeShuGeJuResult? {
        val dayEl = bazi.dayPillar.stem.element
        val yinEl = Element.entries.first { it.generates == dayEl }
        val f = BingYaoCalculator.forces(bazi)
        val fc = { el: Element -> f[el]!! }
        val branches = bazi.branches
        val total = Element.entries.sumOf { f[it]!! }.coerceAtLeast(0.001)

        // ── 1. 专旺：四支皆同我/生我（含变节），干无克耗 ──
        val sanHe = DiZhiRelationCalculator.analyze(bazi).sanHeList
        val heHuaR = HeHuaCalculator.analyze(bazi)
        val sanHui = heHuaR.branchSanHuiList
        // 变节向导全集：三合/三会/半合(需中神)/六合（卯未半合木、寅亥六合木式——专旺格成格条件二）
        val heJuAll = sanHe.map { it.branches.toSet() to it.resultElement } +
            sanHui.map { it.branches.toSet() to it.resultElement } +
            heHuaR.branchBanHeList.map { setOf(it.branch1, it.branch2) to it.resultElement } +
            heHuaR.branchLiuHeList.map { setOf(it.branch1, it.branch2) to it.resultElement }
        fun allyBranch(b: EarthlyBranch): Boolean {
            if (b.element == dayEl || b.element == yinEl) return true
            // 变节向同我/生我之合局成员
            return heJuAll.any { (bs, el) -> b in bs && (el == dayEl || el == yinEl) }
        }
        val allAlly = branches.all { allyBranch(it) }
        // 假专旺之支：非同党支为财官而孤弱无援（强众敌寡，势在去其寡——壬子壬子壬午庚子之午、庚戌甲申庚申辛巳之巳）——
        // 财官极弱不能损伤日主，仍属专旺而留先天隐患
        val notAlly = branches.filter { !allyBranch(it) }
        val foeEls = setOf(dayEl.controls, Element.entries.first { it.controls == dayEl })
        val weakFoeBranchOk = notAlly.isNotEmpty() && notAlly.all { b ->
            b.element in foeEls && branches.count { it.element == b.element } <= 1 &&
                bazi.stems.none { it.element == b.element }
        }
        // 专旺份额按变节力转移计（巳在巳酉丑金局内其火力归金——乙庚化金例二）。
        // 三合/三会全局直转；半合/六合须**合向强方**（化神行力≥被转行）方化——
        // 两午夹戌半局成化（丙午戊戌丙午癸巳，火5≥土4）；午未合而火6强于土3则不化
        // （丁未丙午戊午丁巳本为从印格——合而不化，从旺不从我）
        val fZhuan = f.toMutableMap()
        for ((bs, el) in (sanHe.map { it.branches.toSet() to it.resultElement } + sanHui.map { it.branches.toSet() to it.resultElement })) {
            for (b in bs) if (b.element != el && b in branches) { fZhuan[b.element] = fZhuan[b.element]!! - 1; fZhuan[el] = fZhuan[el]!! + 1 }
        }
        for ((bs, el) in (heHuaR.branchBanHeList.map { setOf(it.branch1, it.branch2) to it.resultElement } +
            heHuaR.branchLiuHeList.map { setOf(it.branch1, it.branch2) to it.resultElement }).distinctBy { it.first to it.second }) {
            val srcs = bs.filter { it.element != el && it in branches }.map { it.element }.distinct()
            if (srcs.isNotEmpty() && srcs.all { s -> f[s]!! <= f[el]!! }) {
                for (b in bs) if (b.element != el && b in branches) { fZhuan[b.element] = fZhuan[b.element]!! - 1; fZhuan[el] = fZhuan[el]!! + 1 }
                val ruler = bazi.monthPillar.branch
                if (ruler in bs && ruler.element != el) { fZhuan[ruler.element] = fZhuan[ruler.element]!! - 2; fZhuan[el] = fZhuan[el]!! + 2 }
            }
        }
        val totalZhuan = Element.entries.sumOf { fZhuan[it]!! }.coerceAtLeast(0.001)
        // 克耗透干：与日干五合且化神即日主行（甲己合化土于己日）者不算克透——化气格（己未甲戌己丑戊辰例）；
        // 与**任意透干**五合且化神即日主行乘旺（癸合戊化火于丙日——丙午戊戌丙午癸巳例三）亦不破格；
        // 与日干五合而化神无力——合而不化贪合忘克（壬合丁于丁日——壬午丙午丁巳丙午例四）不破格；
        // 泄耗透干坐支截脚（支克干）力微不破格——乙卯癸未乙卯丁亥之丁坐亥
        val chouKeTou = bazi.stems.any {
            (it.element == dayEl.controls || it.element.controls == dayEl) && it !== bazi.dayPillar.stem &&
                !(run {
                    val partner = bazi.dayPillar.stem
                    (it.ordinal - partner.ordinal + 10) % 5 == 0 && it.ordinal != partner.ordinal &&
                        (huaShenOf(it, partner) == dayEl || fZhuan[huaShenOf(it, partner)]!! < 2.5)
                }) &&
                !(run {
                    val others = listOf(bazi.yearPillar.stem, bazi.monthPillar.stem, bazi.hourPillar.stem)
                        .filter { o -> o !== it }
                    others.any { o ->
                        (it.ordinal - o.ordinal + 10) % 5 == 0 && huaShenOf(it, o) == dayEl && fZhuan[dayEl]!! >= 2.5
                    }
                }) &&
                !(run {
                    val pl = listOf(bazi.yearPillar, bazi.monthPillar, bazi.hourPillar).firstOrNull { p -> p.stem === it }
                    val jieJiao = pl != null && pl.branch.element.controls == it.element
                    jieJiao && (it.element == dayEl.controls || it.element == dayEl.generates)
                }) &&
                // 官杀透干无本气支根且坐支不生扶、**单透**——极弱隐患不破格（戊申庚申庚戌丁丑之丁）；
                // 双透者敌众不寡，不入专旺（癸未丁巳丙午癸巳双癸——调候无根例仍归病药顺势）
                !(run {
                    it.element.controls == dayEl && bazi.stems.count { s -> s.element == it.element } == 1 &&
                        branches.none { b -> b.element == it.element } &&
                        listOf(bazi.yearPillar, bazi.monthPillar, bazi.hourPillar).firstOrNull { p -> p.stem === it }
                            ?.let { pl -> !(pl.branch.element.generates == it.element || pl.branch.element == it.element) } == true
                })
        }
        // 官杀透干无根之隐患标记（假专旺）
        val ruoGuanTou = listOf(bazi.yearPillar.stem, bazi.monthPillar.stem, bazi.hourPillar.stem).any {
            it.element.controls == dayEl && bazi.stems.count { s -> s.element == it.element } == 1 &&
                branches.none { b -> b.element == it.element } &&
                listOf(bazi.yearPillar, bazi.monthPillar, bazi.hourPillar).firstOrNull { p -> p.stem === it }
                    ?.let { pl -> !(pl.branch.element.generates == it.element || pl.branch.element == it.element) } == true
        }
        // 专旺须日主自身有力（≥2.5）——日主力微而印众者为从印非专旺（张先生/印总表例）
        if ((allAlly || weakFoeBranchOk) && !chouKeTou && fZhuan[dayEl]!! >= 2.5 && fZhuan[dayEl]!! + fZhuan[yinEl]!! >= 0.6 * totalZhuan) {
            val usedBianJie = branches.any { it.element != dayEl && it.element != yinEl } // 变节支使格不纯
            // 岁运破格：克势之行（双体或透+根）→ 还原正格
            val poEl = dayEl.controls // 我克者泄我；克我者伤势——取克我行为破格先锋
            val keEl = Element.entries.first { it.controls == dayEl }
            val broken = extraPillars.any { p ->
                (p.stem.element == keEl && p.branch.element == keEl) || (p.stem.element == poEl && p.branch.element == poEl)
            }
            if (broken) return null
            // 两气成象分流：印/食伤之相生邻行与日行均衡（份额≥八成）——
            // 均衡门槛随月令：日主当令（己巳戊辰戊午戊午火3土7月令辰=0.43成慈）取0.4，
            // 印当令则须高度均衡0.8（甲午丙寅丙寅甲午木6火4通明；庚申庚辰庚申庚辰土4金6=0.67仍专旺——金众土寡）
            val partner0 = listOf(yinEl, dayEl.generates).maxBy { fZhuan[it]!! }
            val divertTh = if (bazi.monthPillar.branch.element == dayEl) 0.4 else 0.8
            // 有财官隐患（假专旺）者不分流——戊申庚申庚戌丁丑之丁官无根仍留专旺档
            if (!ruoGuanTou && fZhuan[partner0]!! >= divertTh * fZhuan[dayEl]!! && fZhuan[partner0]!! >= 2.0 &&
                (fZhuan[dayEl]!! + fZhuan[partner0]!!) >= 0.8 * totalZhuan) {
                return liangQiOf(bazi, dayEl, partner0, fZhuan)
            }
            val yong = listOf(dayEl, yinEl, dayEl.generates) // 比印首选，有力食伤次之
            // 双档命名：全三合/三会成局且月令会宫（木：寅卯辰）或化气五合成格（乙庚合金于庚日）→
            // 曲直/炎上/稼穑/从革/润下（成格条件与层次更高）；否则"X专旺格"（普通档——亥卯未全合而月令未不在会宫亦普通）。
            // 土之会局从严：辰戌丑未须四库全，或丑未戌三刑局——丑戌辰三支（缺未）只算普通档（己丑甲戌戊戌丙辰例三）
            val fullJu = sanHe.any { it.resultElement == dayEl } ||
                sanHui.any { it.resultElement == dayEl && (dayEl != Element.EARTH || it.branches.toSet().size >= 4) } ||
                (dayEl == Element.EARTH && setOf(EarthlyBranch.CHOU, EarthlyBranch.XU, EarthlyBranch.WEI).all { it in branches })
            val gong = when (dayEl) {
                Element.WOOD -> setOf(EarthlyBranch.YIN, EarthlyBranch.MAO, EarthlyBranch.CHEN)
                Element.FIRE -> setOf(EarthlyBranch.SI, EarthlyBranch.WU, EarthlyBranch.WEI)
                Element.METAL -> setOf(EarthlyBranch.SHEN, EarthlyBranch.YOU, EarthlyBranch.XU)
                Element.WATER -> setOf(EarthlyBranch.HAI, EarthlyBranch.ZI, EarthlyBranch.CHOU)
                else -> setOf(EarthlyBranch.CHEN, EarthlyBranch.XU, EarthlyBranch.CHOU, EarthlyBranch.WEI)
            }
            val huaQiCheng = listOf(bazi.yearPillar.stem, bazi.monthPillar.stem, bazi.hourPillar.stem).any { g ->
                (g.ordinal - bazi.dayPillar.stem.ordinal + 10) % 5 == 0 && g.ordinal != bazi.dayPillar.stem.ordinal &&
                    huaShenOf(g, bazi.dayPillar.stem) == dayEl && fZhuan[dayEl]!! >= 2.5
            }
            val geMing = if ((fullJu && bazi.monthPillar.branch in gong) || huaQiCheng) GRID_NAME[dayEl]!! else "${dayEl.chinese}专旺格"
            // 假专旺：孤弱财官支留存（未变节）或官透无根——先天隐患，岁运帮扶即与旺势交战
            val jiaZhuan = (weakFoeBranchOk && notAlly.isNotEmpty()) || ruoGuanTou
            val yinHuanTxt = if (jiaZhuan) {
                val hs = (notAlly.map { it.element } + if (ruoGuanTou) listOf(Element.entries.first { it.controls == dayEl }) else emptyList()).distinct()
                "局中${hs.joinToString("") { it.chinese }}极弱不能损伤日主而留**先天隐患**（假专旺）——岁运帮扶之便与旺势交战破格生灾，受抑去之则真。"
            } else ""
            return TeShuGeJuResult(
                kind = TeShuKind.ZHUAN_WANG, name = geMing,
                yongShen = yong, xi = listOf(dayEl.generates), ji = listOf(keEl, poEl),
                fake = usedBianJie || jiaZhuan, restoreHint = "岁运${keEl.chinese}${poEl.chinese}破势则还原正格取用",
                basis = "四柱干支与日主一气${if (usedBianJie && !jiaZhuan) "（有变节支，格不太标准）" else ""}，" +
                    yinHuanTxt +
                    "顺我者昌逆我者亡。用神首选比印（${dayEl.chinese}${yinEl.chinese}），其次有力之食伤（${dayEl.generates.chinese}）。" +
                    "岁运食伤有力则取泄秀，泄神被合绊则回专旺，木火破势则变正格。",
            )
        }

        // ── 2. 从格（单从/杂从/多从/战局） ──
        // 有效根：本气支（非刑支、非合走），藏干根须本气不克
        val xingBranches = mutableSetOf<EarthlyBranch>()
        for (grp in listOf(
            setOf(EarthlyBranch.YIN, EarthlyBranch.SI, EarthlyBranch.SHEN),
            setOf(EarthlyBranch.CHOU, EarthlyBranch.XU, EarthlyBranch.WEI),
        )) if (grp.count { it in branches } >= 2) xingBranches += grp.filter { it in branches }
        // 本气根：本气支非刑支，且非合局变节成员（辰在申子辰水局内不算土根——总表例）
        val heJu = sanHe.map { it.branches to it.resultElement } + sanHui.map { it.branches to it.resultElement }
        val bianJieBranches = heJu.flatMap { (bs, _) -> bs }
        val hasBenQiRoot = branches.any { it.element == dayEl && it !in xingBranches &&
            (it !in bianJieBranches || heJu.any { (bs, el) -> it in bs && el == dayEl }) }

        // ── 1b. 两气成象（日主行+强旺邻行双强均衡，他行纯净微弱） ──
        // 相生邻行（印/食伤）：甲寅丙寅丙寅甲午（午非印比不入专旺）/癸卯戊午甲寅丙寅（戊癸合化去土水）式；
        // 相克邻行（官/财）：金木栋梁辛酉辛卯甲寅癸酉式（通关神癸微弱≤1不破格）
        // 守卫：自立（有效根或比劫两透）+比例≥0.4（防从儿从印）+他行各≤0.5（相生对）/≤1.0（相克对通关神可容）
        run {
            // 朴素力图（干1支1月令+2——合教材"党众成林"之力感）+ 合局转移
            // （三合三会全局+半合中神+月令随化神）——亥卯未合木使癸未癸亥癸卯癸亥之木成势
            val fLQ = Element.entries.associateWith { el ->
                (bazi.stems.count { it.element == el } + branches.count { it.element == el }).toDouble() +
                    (if (bazi.monthPillar.branch.element == el) 2.0 else 0.0)
            }.toMutableMap()
            val rulerB = bazi.monthPillar.branch
            for ((bs, el) in heJu) for (b in bs) {
                if (b.element != el && b in branches) { fLQ[b.element] = fLQ[b.element]!! - 1; fLQ[el] = fLQ[el]!! + 1 }
            }
            heJu.firstOrNull { (bs, el) -> rulerB in bs && el != rulerB.element }?.let { (_, el) ->
                fLQ[rulerB.element] = (fLQ[rulerB.element]!! - 2).coerceAtLeast(0.0); fLQ[el] = fLQ[el]!! + 2
            }
            for (bh in heHuaR.branchBanHeList) {
                val el = bh.resultElement
                // 仅真半合（中神在原局）转移——拱局（无中神）不动力
                val zhongB = when (el) {
                    Element.WOOD -> EarthlyBranch.MAO; Element.FIRE -> EarthlyBranch.WU
                    Element.METAL -> EarthlyBranch.YOU; Element.WATER -> EarthlyBranch.ZI; else -> null
                } ?: continue
                if (zhongB !in branches) continue
                for (b in listOf(bh.branch1, bh.branch2)) {
                    if (b.element != el && b in branches) { fLQ[b.element] = (fLQ[b.element]!! - 1).coerceAtLeast(0.0); fLQ[el] = fLQ[el]!! + 1 }
                }
                if (rulerB == bh.branch1 || rulerB == bh.branch2) {
                    if (rulerB.element != el) { fLQ[rulerB.element] = (fLQ[rulerB.element]!! - 2).coerceAtLeast(0.0); fLQ[el] = fLQ[el]!! + 2 }
                }
            }
            val partner = listOf(yinEl, dayEl.generates, Element.entries.first { it.controls == dayEl }, dayEl.controls)
                .maxBy { fLQ[it]!! }
            val isWarPair = partner.controls == dayEl || dayEl.controls == partner
            // 相克对守卫：通关之神（非五合化解）透干不分流——扶财一戊戌乙丑甲辰丁卯之丁通关透
            // 与栋梁例一癸酉通关透同构异断（书内矛盾取病药侧）；甲己合化掉之甲不算（池沼例三）
            val tongGuanEl = if (isWarPair) Element.entries.firstOrNull { m -> partner.generates == m && m.generates == dayEl } ?: Element.entries.firstOrNull { m -> dayEl.generates == m && m.generates == partner } else null
            val side = listOf(bazi.yearPillar.stem, bazi.monthPillar.stem, bazi.hourPillar.stem)
            val neutralized = mutableSetOf<Element>()
            for (i in side.indices) for (j in i + 1 until side.size) {
                val a = side[i]; val b2 = side[j]
                if ((a.ordinal - b2.ordinal + 10) % 5 == 0 && a.ordinal != b2.ordinal) {
                    val hs = huaShenOf(a, b2)
                    if ((hs == dayEl || hs == partner) && fLQ[hs]!! >= 2.5) { neutralized += a.element; neutralized += b2.element }
                }
            }
            val tgTou = tongGuanEl != null && side.any { it.element == tongGuanEl && it.element !in neutralized }
            val totalLQ = (Element.entries.sumOf { fLQ[it]!! } - neutralized.size.toDouble()).coerceAtLeast(0.001)
            // 他行守卫：第三行总力≤1（池沼例一丙印单透容、乙巳乙酉乙亥乙酉火水并见拒）；
            // 相生对官杀之行须零（癸丑甲子癸卯癸亥之丑官——仍归病药泄秀）
            val thirdForce = Element.entries.filter { it != dayEl && it != partner }
                .sumOf { fLQ[it]!! - (if (it in neutralized) 1.0 else 0.0) }
            val guanNon0 = !isWarPair && Element.entries.first { it.controls == dayEl }.let { fLQ[it]!! - (if (it in neutralized) 1.0 else 0.0) > 0 }
            val othersOk = thirdForce <= 1.0 && !guanNon0
            val ratio = minOf(fLQ[dayEl]!!, fLQ[partner]!!) / maxOf(fLQ[dayEl]!!, fLQ[partner]!!)
            // 三气领地：第三行有本气根且与两行成链（木生火生土——陈小姐戊午丙辰丙寅甲午三气成象非两气）——让位
            val sanQiLike = Element.entries.filter { it != dayEl && it != partner }.any { el ->
                branches.any { it.element == el } &&
                    (el.generates == dayEl || dayEl.generates == el || el.generates == partner || partner.generates == el)
            }
            // 相克对结构守卫：两行须有双体柱（壬子壬子丁巳丙午之壬子丙午）或天干地支各尽两行
            // （辛卯辛卯辛卯辛卯/乙未戊寅乙未戊寅）——癸丑丙辰癸未癸丑无此结构仍归病药
            val structureOk = !isWarPair || bazi.pillars.any { it.stem.element == it.branch.element && (it.stem.element == dayEl || it.stem.element == partner) } ||
                (bazi.stems.all { it.element == dayEl || it.element == partner } && branches.all { it.element == dayEl || it.element == partner })
            // 自立条件：有效根或比劫两透（庚申己丑戊戌庚申之戌被丑刑——干戊己两透亦可自立）
            val selfStanding = hasBenQiRoot || bazi.stems.count { it.element == dayEl } >= 2
            val ratioOk = if (isWarPair) ratio >= 0.6 else ratio >= 0.4
            if (!tgTou && !sanQiLike && structureOk && selfStanding && fLQ[dayEl]!! >= 2.0 && fLQ[partner]!! >= 2.0 && ratioOk &&
                (fLQ[dayEl]!! + fLQ[partner]!!) >= 0.8 * totalLQ && othersOk) {
                return liangQiOf(bazi, dayEl, partner, fLQ)
            }
        }

        if (!hasBenQiRoot) {
            // 变节力转移：合局成员本气之力与月令加成随化神计（丑在亥子丑水局内之力归水）
            val fAdj = f.toMutableMap()
            for ((bs, el) in heJu) for (b in bs) {
                if (b.element != el && b in branches) { fAdj[b.element] = fAdj[b.element]!! - 1; fAdj[el] = fAdj[el]!! + 1 }
            }
            val rulerB = bazi.monthPillar.branch
            heJu.firstOrNull { (bs, el) -> rulerB in bs && el != rulerB.element }?.let { (bs, el) ->
                fAdj[rulerB.element] = fAdj[rulerB.element]!! - 2; fAdj[el] = fAdj[el]!! + 2
            }
            val fcAdj = { el: Element -> fAdj[el]!! }
            val maxOther = Element.entries.filter { it != dayEl }.maxOf { fAdj[it]!! }
            // 透干比劫/印：被克泄殆尽或坐下截脚者无力（假从之"虚助"）
            // 印为势首时印透不算虚助（从印格之戊己——印是用神非帮扶）
            val yinIsShi = fAdj[yinEl]!! >= 2.5 && fAdj[yinEl]!! >= maxOther
            val helperStems = listOf(bazi.yearPillar.stem, bazi.monthPillar.stem, bazi.hourPillar.stem)
                .filter { it.element == dayEl || (it.element == yinEl && !yinIsShi) }
            // 虚助判定：透干之印比被克泄殆尽（≥2.5×）且坐支不生扶（坐支本气生或同该干——壬坐申长生不算虚）
            val weakHelpers = helperStems.filter { s ->
                val self = fAdj[s.element]!!
                val foes = Element.entries.filter { e -> e.controls == s.element || s.element.controls == e }.sumOf { fAdj[it]!! }
                val pl = listOf(bazi.yearPillar, bazi.monthPillar, bazi.hourPillar).firstOrNull { it.stem === s }
                val zuoSheng = pl?.let { it.branch.element.generates == s.element || it.branch.element == s.element } == true
                foes >= 2.5 * self && !zuoSheng
            }
            // 截脚按实际柱位（枚举同值防首配错柱）；湿土（丑辰）不截水根——藏水之根（A5 教材例）
            val jieJiaoStems = helperStems.filter { s ->
                listOf(bazi.yearPillar, bazi.monthPillar, bazi.hourPillar).any { pl ->
                    pl.stem === s && pl.branch.element.controls == s.element &&
                        !(s.element == Element.WATER && (pl.branch == EarthlyBranch.CHOU || pl.branch == EarthlyBranch.CHEN))
                }
            }
            // 合化入势从真：透劫/印与另一透干五合且化神在从势内（癸合戊化火/乙合庚化金）——不以虚助论
            val heHuaRuShi = helperStems.filter { s ->
                val o = bazi.stems.firstOrNull { o ->
                    o !== bazi.dayPillar.stem && o !== s &&
                        (s.ordinal - o.ordinal + 10) % 5 == 0 && s.ordinal != o.ordinal
                }
                // 五合除名须化神行有力（≥2）：癸合戊化火(火4)从真；杨先生丁壬合化木(木1)不除仍假
                o != null && fAdj[huaShenOf(s, o)]!! >= 2
            }
            val weakSet = (jieJiaoStems + weakHelpers).toSet() - heHuaRuShi.toSet()
            val fake = weakSet.isNotEmpty()
            // 日行有效力：透干虚助被殆尽/截脚者折尽
            val effDayLine = (fcAdj(dayEl) - weakSet.size).coerceAtLeast(0.0)
            if (effDayLine <= 0.4 * maxOther) {
                // 从势：有力诸行（≥2.5），相生连链；连接行力可低
                val others = Element.entries.filter { it != dayEl }
                val strong = others.filter { fAdj[it]!! >= 2.5 }
                if (strong.isNotEmpty()) {
                    // 链首：化气格取化神行（日干五合且化神力强——乙庚化金势首为金非生金之土）
                    val huaShenEl = run {
                        val partner = listOf(bazi.yearPillar.stem, bazi.monthPillar.stem, bazi.hourPillar.stem).firstOrNull { g ->
                            (g.ordinal - bazi.dayPillar.stem.ordinal + 10) % 5 == 0 && g.ordinal != bazi.dayPillar.stem.ordinal
                        }
                        partner?.let { huaShenOf(it, bazi.dayPillar.stem) }?.takeIf { fAdj[it]!! >= 2.5 }
                    }
                    // 链组装：自最强行向前向后延（生我者/我生者中在 strong 或为连接行）
                    val maxEl = huaShenEl ?: strong.maxBy { fAdj[it]!! }
                    val chain = linkedSetOf(maxEl)
                    var grew = true
                    while (grew) {
                        grew = false
                        for (e in others) {
                            if (e in chain) continue
                            val tou = bazi.stems.any { it.element == e && it !== bazi.dayPillar.stem }
                            val benZhi = branches.any { it.element == e }
                            // 强行直入；弱端只顺下游（链生它，势之所趋）——上游饲喂者被吸尽不入；
                            // 印行不入从链，唯力≥2 且透干或本气支者例外（火土金之印金——三气从格例二）
                            val downstream = chain.any { c -> c.generates == e }
                            val yinOk = e != yinEl || (fAdj[e]!! >= 2 && (tou || benZhi))
                            if (e in strong || (fAdj[e]!! >= 1 && downstream && yinOk)) {
                                chain += e; grew = true
                            }
                        }
                    }
                    // 上游饲喂者入链：力≥2 且透干（火生土式——杨先生例）；各倚旺势：链≥2行时力≥2且透干之非印行不拘生克（水木土——例三）；多趟回扫
                    var grew2 = true
                    while (grew2) {
                        grew2 = false
                        for (e in others) {
                            if (e in chain) continue
                            val tou = bazi.stems.any { it.element == e && it !== bazi.dayPillar.stem }
                            val benZhi = branches.any { it.element == e }
                            val present = tou || benZhi
                            if (fAdj[e]!! >= 2 && present && chain.any { c -> e.generates == c }) { chain += e; grew2 = true }
                            else if (chain.size >= 2 && fAdj[e]!! >= 2 && present && e != yinEl) { chain += e; grew2 = true }
                            // 链≥3行时门槛再降：力≥2，或月柱透该行（提纲司权——总表壬水1恃月令入；缪辰土1年支/李湘辛1时柱不入）
                            else if (chain.size >= 3 && present && (fAdj[e]!! >= 2 || bazi.monthPillar.stem.element == e)) { chain += e; grew2 = true }
                        }
                    }
                    // 相生拓扑序：先生它者为链首
                    val chainList = mutableListOf<Element>()
                    val rest = chain.toMutableSet()
                    while (rest.isNotEmpty()) {
                        val head = rest.firstOrNull { e -> rest.none { c -> c.generates == e } } ?: rest.first()
                        chainList += head; rest.remove(head)
                    }
                    // 战局：链内/力前两行相克力差小 → 通关
                    val top2 = others.sortedByDescending { fAdj[it]!! }.take(2)
                    if (top2.size == 2) {
                        val (a, b) = top2
                        val xKeY = a.controls == b; val yKeX = b.controls == a
                        if ((xKeY || yKeX) && kotlin.math.abs(fAdj[a]!! - fAdj[b]!!) <= 1.5) {
                            val (winner, loser) = if (xKeY) a to b else b to a
                            val guan = tongGuan(winner, loser)
                            // 战局仅当：通关神即日主（弃命取己通关——王先生式）；
                            // 通关神非日主者（火金器皿相战式）入两气从格——多从格例三
                            if (guan != null && guan == dayEl) {
                                // 岁运还原：日主得禄之支
                                if (extraPillars.any { it.branch.element == dayEl }) return null
                                return TeShuGeJuResult(
                                    kind = TeShuKind.ZHAN_JU, name = "${a.chinese}${b.chinese}交战",
                                    yongShen = listOf(guan), xi = emptyList(), ji = listOf(a, b),
                                    fake = fake, restoreHint = "岁运${guan.chinese}到位解战；日主得根则还原正格",
                                    basis = "日主弱极无依，气势在${a.chinese}${b.chinese}而两强相战势均力敌——依附谁都不理想，" +
                                        "首取通关之${guan.chinese}以解战局（战者和为贵，合平衡流通原则）。宜北方水地江边居住等趋吉避凶。",
                                )
                            }
                        }
                    }
                    // 官杀双透且官力不足（非强势成员）且未被彻底制住 → 阻从（调候矛盾局壬癸双透例）；
                    // 官被克官之行彻底制住（≥2.5×官力——丙戌庚寅甲午庚午例火6制金2）不阻
                    val guanEl0 = Element.entries.first { it.controls == dayEl }
                    val guanTouCount = listOf(bazi.yearPillar.stem, bazi.monthPillar.stem, bazi.hourPillar.stem)
                        .count { it.element == guanEl0 }
                    // 化气格豁免：官干与日干五合且化神将入从势（丁壬合化木）——官即合伴非破格之神
                    val huaQiExempt = listOf(bazi.yearPillar.stem, bazi.monthPillar.stem, bazi.hourPillar.stem).any { g ->
                        g.element == guanEl0 && (g.ordinal - bazi.dayPillar.stem.ordinal + 10) % 5 == 0 && g.ordinal != bazi.dayPillar.stem.ordinal &&
                            fAdj[huaShenOf(g, bazi.dayPillar.stem)]!! >= 2.5
                    }
                    val zhiGuanEl = Element.entries.first { it.controls == guanEl0 }
                    val guanCrushed = fAdj[zhiGuanEl]!! >= 2.5 * fAdj[guanEl0]!!
                    if (fAdj[guanEl0]!! < 2.5 && guanTouCount >= 2 && !guanCrushed && !huaQiExempt) {
                        // 落入三气成象/正格
                    } else {
                    // 岁运还原：日主得禄之支（如庚禄在申）或印得本气根（如壬通根子生日干——朱先生丙子年）→ 回正格身弱
                    if (extraPillars.any { it.branch.element == dayEl || it.branch.element == yinEl }) return null
                    val kind = if (chainList.size == 1) TeShuKind.JIA_CONG
                    else if (fake) TeShuKind.JIA_CONG else TeShuKind.DUO_CONG
                    // 命名按势首（最强成员）与日主之关系：財→从财、官→从杀、食伤→从儿、印→从印
                    // （杨先生火財5:4土官→从财；丁巳例火食6:2土財→从儿——同构异名，关系定之）
                    // 化气格以化神行定名（乙庚化金→从杀，纵生金之土力5居首）
                    val shiShou = huaShenEl ?: chainList.maxBy { fAdj[it]!! }
                    val relName = when {
                        shiShou == dayEl.controls -> "从财"
                        shiShou.controls == dayEl -> "从杀"
                        shiShou == dayEl.generates -> "从儿"
                        shiShou == yinEl -> "从印"
                        else -> "从"
                    }
                    val name = (if (fake) "假" else "") + relName + "格"
                    val rootDesc = if (fake) "日主无有效根（透干之助被克泄殆尽或截脚无力，假从），" else "日主无有效根，弱极无依，"
                    val chainText = chainList.joinToString("") { it.chinese }
                    val ji = listOf(yinEl, dayEl).filter { it !in chainList }
                    return TeShuGeJuResult(
                        kind = kind, name = name,
                        yongShen = chainList, xi = chainList.drop(1), ji = ji,
                        fake = fake,
                        restoreHint = "贴身比劫伺机而动，岁运${dayEl.chinese}得禄（如${luZhiOf(dayEl)}）则还原正格身弱论",
                        basis = rootDesc + "弃命从" + chainText + "——" + chainText +
                            (if (chainList.size >= 3) "连续相生" else "相生为势") +
                            "，取" + chainText + "为喜用，忌印比逆势。" + (if (fake) "逢流年亦可还原，切不可一味按从格断。" else ""),
                    )
                    }
                }
            }
        }

        // ── 3. 三气成象：日主有力参与三行连环相生，弱者补之 ──
        val engine = StrengthEngine.analyze(bazi)
        if (engine.verdict != DayStrength.WEAK) {
            for (start in Element.entries) {
                val mid = start.generates
                val end = mid.generates
                if (start == end) continue
                val lines = listOf(start, mid, end)
                if (dayEl !in lines) continue
                // 印重身轻（印≥日×2.5，身弱不受生）→从印非三气（丁卯壬寅丁卯壬寅）；潮州李印:日=4:2 仍三气
                val yinGuoZhong = f[yinEl]!! >= f[dayEl]!! * 2.5
                if (!yinGuoZhong && lines.all { f[it]!! >= 2 } && Element.entries.filter { it !in lines }.all { f[it]!! <= 1.5 }) {
                    val sorted = lines.sortedBy { f[it]!! } // 弱者在前
                    // 破格之行值运有力 → 正格取用
                    val poLines = Element.entries.filter { it !in lines }
                    if (extraPillars.any { p -> p.stem.element in poLines && p.branch.element in poLines }) return null
                    return TeShuGeJuResult(
                        kind = TeShuKind.SAN_QI,
                        name = lines.joinToString("") { it.chinese } + "三气成象",
                        yongShen = sorted, xi = sorted.drop(1), ji = poLines,
                        fake = false, restoreHint = "破格之行（${poLines.joinToString("") { it.chinese }}）值运有力则按正格取用",
                        basis = "日主有力，${lines.joinToString("→") { it.chinese }}三气成象秀气流通。" +
                            "三行中弱者补之：首补${sorted.first().chinese}（原局${sorted.joinToString("") { "${it.chinese}${fmtI(f[it]!!)}" }}）。" +
                            "岁运用神到位（补弱之行）则兴旺；破格之行值运有力则按正格取用。",
                    )
                }
            }
        }
        return null
    }

    private fun branchElementOf(bazi: BaziResult, s: cn.xulun.bazi.model.HeavenlyStem): Element? =
        pillarOfStem(bazi, s)?.branch?.element

    private fun pillarOfStem(bazi: BaziResult, s: cn.xulun.bazi.model.HeavenlyStem): Pillar? =
        bazi.pillars.firstOrNull { it.stem == s }

    /** 日主禄位支名（用于还原提示）。 */
    private fun luZhiOf(el: Element): String = when (el) {
        Element.WOOD -> "寅卯"; Element.FIRE -> "巳午"; Element.EARTH -> "午辰戌丑未"
        Element.METAL -> "申酉"; Element.WATER -> "亥子"
    }

    private fun fmtI(d: Double): String = if (d == d.toLong().toDouble()) d.toLong().toString() else String.format("%.1f", d)
}
