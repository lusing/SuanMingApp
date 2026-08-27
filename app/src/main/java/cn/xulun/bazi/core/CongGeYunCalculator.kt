package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.CongGeYunResult
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.YunVerdict

/**
 * 从格岁运喜忌分类器。理论见 `.shengke-reference.md` 第五二节（从财格行运喜忌；从官杀/从印/从儿/多从同理）。
 *
 * 判定以**支为主导**（岁运支当权）：
 * - 支 ∈ 从势 → **用神到位**（助旺格局）；
 * - 支生势（食伤之行）→ **喜神到位**（食伤扶旺）；
 * - 支 = 日主本气行 → **还原正格**（岁运为日主之根，刑动/帮身破格——刘女士例）；
 * - 支 = 破格神行（印/比劫/官杀之不在势者）→ 干同支或干生支 = 强 → **转两气从格**
 *   （新用神=该神行，按两气成象取用）；否则 = 弱 → **忌神**（杂乱气势/引日主破格伤身）。
 *
 * 仅当原局为从格（[TeShuGeJuCalculator] 判从）时有意义；非从格返回 null。
 */
object CongGeYunCalculator {

    fun analyze(bazi: BaziResult, extraPillars: List<Pillar>): CongGeYunResult? {
        if (extraPillars.isEmpty()) return null
        val base = TeShuGeJuCalculator.analyze(bazi) ?: return null
        val dayEl = bazi.dayPillar.stem.element
        val shi = base.yongShen
        val p = extraPillars.last() // 以最后一柱（流年优先于大运）判定
        val pb = p.branch.element
        val ps = p.stem.element
        val yinEl = Element.entries.first { it.generates == dayEl }

        // 专旺格行运另有专梯（比劫=用/印=喜/官财食强则两停转格弱则忌——第五四节(三)）
        if (base.kind == cn.xulun.bazi.model.TeShuKind.ZHUAN_WANG) return zhuanWangYun(bazi, base, p)

        val strong = ps == pb || ps.generates == pb // 干支同心（干同支或干生支）
        // 坐支被原局半合/六合拉向从势（化神在势）——岁干之根随支被拉走（姚先生辛亥：亥卯半合木）
        val banHeLaZou = run {
            val tri = listOf(
                setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN) to Element.WATER,
                setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI) to Element.WOOD,
                setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU) to Element.FIRE,
                setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU) to Element.METAL,
            )
            val bs = bazi.branches.toSet()
            // 坐支须为新来（原局已有该支=局已含，岁运再来属加势——总表戊申喜；新来之支被半合拉走——姚亥忌）
            p.branch !in bs && tri.any { (g, el) -> p.branch in g && el in shi && bs.any { it in g && it != p.branch } }
        }

        // 破格神集合：印、比劫、官杀、克势之行（从杀之食伤——食伤克杀）之不在从势者
        fun poGeShen(el: Element): Boolean = el !in shi &&
            (el == yinEl || el == dayEl || el.controls == dayEl || shi.any { el.controls == it })

        val verdict: YunVerdict
        val newEl: Element?
        val basis: String
        when {
            // 支在从势而干克势 → 忌（戊子例：支子在杀而干戊克杀为逆）
            pb in shi && ps !in shi && shi.any { ps.controls == it } && ps.controls == shi.first() && !strong -> {
                verdict = YunVerdict.JI; newEl = null
                basis = "岁运${p.fullName}：支${p.branch.chinese}虽在从势，而干${p.stem.chinese}（${ps.chinese}）克从势之行，逆乱格局气势——忌神"
            }
            // 多行势且日主行不在势（纯两气/三气/四气从）：补最弱行=纠偏用神，补其余行=失衡忌（"木土偏旺"皆忌）
            // 弱行以去月令加成之力计（"旺而不强"——月令之力非党众之实），干支任一为弱行皆论补
            pb in shi && base.kind == cn.xulun.bazi.model.TeShuKind.DUO_CONG && shi.size >= 2 && dayEl !in shi -> {
                val f0x = cn.xulun.bazi.core.BingYaoCalculator.forces(bazi).toMutableMap()
                val yueEl = bazi.monthPillar.branch.element
                f0x[yueEl] = (f0x[yueEl]!! - 2.0).coerceAtLeast(0.0)
                // 结党加成：同行支对 C(n,2) + 干支同行柱——散布之力（庚+申）不如结党（两午/己未双体）
                for (el in shi) {
                    val n = bazi.branches.count { it.element == el }
                    val st = bazi.pillars.count { it.stem.element == el && it.branch.element == el }
                    f0x[el] = f0x[el]!! + n * (n - 1) / 2.0 + st
                }
                val ruo = shi.minBy { f0x[it]!! }
                // 势首克日主（从杀式）或势首生身（从印式）→按单从论：势首=用、生势首=喜；
                // 化气格势首=化神行（乙庚化金，纵土5金4）
                val shiShou = TeShuGeJuCalculator.huaShenElOf(bazi)?.takeIf { it in shi } ?: shi.maxBy { f0x[it]!! }
                val danCong = shiShou.controls == dayEl || shiShou.generates == dayEl
                if (danCong && pb == shiShou) {
                    verdict = YunVerdict.YONG; newEl = null
                    basis = "岁运" + p.fullName + "：" + pb.chinese + "为从势之首（克身之神），助旺格局——用神到位"
                } else if (danCong && pb.generates == shiShou && pb != yinEl) {
                    verdict = YunVerdict.XI; newEl = null
                    basis = "岁运" + p.fullName + "：" + pb.chinese + "生扶势首（" + godName(pb, dayEl) + "生官杀）——喜神到位"
                } else if (pb == ruo || (ps == ruo && pb.generates == ps)) {
                    verdict = YunVerdict.YONG; newEl = null
                    basis = "岁运${p.fullName}：${pb.chinese}为从势中最弱之行，补之以纠原局之偏——用神到位（纠偏，平衡${shi.joinToString("") { it.chinese }}气势）"
                } else {
                    verdict = YunVerdict.JI; newEl = null
                    basis = "岁运${p.fullName}：${pb.chinese}为从势中偏旺之行（最弱为${ruo.chinese}），再补之使气势失衡——忌神"
                }
            }
            // 支在从势 → 用神到位（助旺格局）
            pb in shi -> {
                verdict = YunVerdict.YONG; newEl = null
                basis = "岁运${p.fullName}之支${p.branch.chinese}（${pb.chinese}）在从势内，助旺格局——用神到位"
            }
            // 支为日主本气行：多行从且日主行不在势（两气从）→强比劫助身还原正格（总表例3）；
            // 否则三档：假从遇根/力不足→还原（母慈灭子），力足→两气转格
            pb == dayEl -> {
                if (base.fake || (shi.size >= 2 && dayEl !in shi && strong)) {
                    verdict = YunVerdict.HUAN_YUAN; newEl = null
                    basis = "岁运${p.fullName}：比劫（${dayEl.chinese}）" + (if (base.fake) "为日主之根，原局假从遇根——还原身弱正格论"
                        else "干支强旺助日主抵抗${shi.joinToString("") { it.chinese }}之克耗——还原正格（身弱，取印比助日主任财官）")
                } else if (shi.size >= 2 && dayEl !in shi) {
                    verdict = YunVerdict.JI; newEl = null
                    basis = "岁运${p.fullName}：比劫（${dayEl.chinese}）衰弱，无力助日主改变格局，反引破格伤身——忌神"
                } else {
                val f0 = cn.xulun.bazi.core.BingYaoCalculator.forces(bazi)
                val yunTong = (if (ps == dayEl) 1 else 0) + (if (pb == dayEl) 1 else 0)
                val biLi = f0[dayEl]!! + yunTong
                val shiLi = shi.maxOf { f0[it]!! }
                if (base.fake || biLi < 0.28 * shiLi) {
                    verdict = YunVerdict.HUAN_YUAN; newEl = null
                    basis = "岁运${p.fullName}之支${p.branch.chinese}为日主${dayEl.chinese}之根" +
                        (if (base.fake) "，原局假从遇根——刑动/帮身破格，还原身弱正格论"
                        else "，但比劫之力不足（${biLi} 对 势${shiLi}）——印重身轻正格（母慈灭子），不吉")
                } else {
                    verdict = YunVerdict.ZHUAN_GE; newEl = dayEl
                    basis = "岁运${p.fullName}：比劫（${dayEl.chinese}）助日主力足（${biLi} 对 势${shiLi}），与势成两停——格局变身为势两气成象，取比劫平衡气势"
                }
                }
            }
            // 克势之行（壬寅例：官克财势/壬戌例：衰土克杀势）→强转两气/弱忌
            shi.any { pb.controls == it } -> {
                if (strong) {
                    verdict = YunVerdict.ZHUAN_GE; newEl = pb
                    basis = "岁运${p.fullName}：${pb.chinese}（${godName(pb, dayEl)}）克从势之行，干支强旺与势两停——格局转两气从格，取${pb.chinese}为用（按两气成象取用）"
                } else {
                    verdict = YunVerdict.JI; newEl = null
                    basis = "岁运${p.fullName}：${pb.chinese}（${godName(pb, dayEl)}）衰弱，无力克住从势，反逆乱格局气势——忌神"
                }
            }
            // 势克之行（受势克而抗之，从印之食伤）→强转/弱忌
            shi.any { it.controls == pb } && !poGeShen(pb) -> {
                if (strong) {
                    verdict = YunVerdict.ZHUAN_GE; newEl = pb
                    basis = "岁运${p.fullName}：${pb.chinese}（${godName(pb, dayEl)}）抗住从势之克，干支强旺与势两停——格局转两气从格，取${pb.chinese}为用以平衡气势"
                } else {
                    verdict = YunVerdict.JI; newEl = null
                    basis = "岁运${p.fullName}：${pb.chinese}（${godName(pb, dayEl)}）衰弱，无力抵抗从势之克，反逆乱气势——忌神"
                }
            }
            // 岁干比劫力不足（坐支不生扶干）→引日主破格伤身——忌（姚先生辛亥年：辛坐亥被合化木）
            ps == dayEl && !(ps == pb || pb.generates == ps) &&
                (shi.none { pb.generates == it } || banHeLaZou) -> {
                verdict = YunVerdict.JI; newEl = null
                basis = "岁运" + p.fullName + "：干" + p.stem.chinese + "为日主" + dayEl.chinese + "之比劫而坐支不生扶（根被拉走/力不足），反引日主背叛旺势破格伤身——忌神"
            }
            // 生势而非印（官生印/财生官）→干透势行则用神、否则喜神（丙寅例）；印生势者由势生通道（化泄夺势）
            shi.any { pb.generates == it } && pb != yinEl -> {
                if (ps in shi) {
                    verdict = YunVerdict.YONG; newEl = null
                    basis = "岁运${p.fullName}之干${p.stem.chinese}（${ps.chinese}）透从势之行且支生扶——用神到位"
                } else {
                    verdict = YunVerdict.XI; newEl = null
                    basis = "岁运${p.fullName}之支${p.branch.chinese}（${pb.chinese}，${godName(pb, dayEl)}）生扶从势——喜神到位"
                }
            }
            // 势生之行（泄势者：从儿之財/从财之官——壬寅例）→强转/弱忌
            shi.any { it.generates == pb } -> {
                if (strong) {
                    verdict = YunVerdict.ZHUAN_GE; newEl = pb
                    basis = "岁运${p.fullName}：${pb.chinese}（${godName(pb, dayEl)}）化泄从势旺气，干支强旺与势两停——格局转两气从格，取${pb.chinese}为用以平衡气势"
                } else {
                    verdict = YunVerdict.JI; newEl = null
                    basis = "岁运${p.fullName}：${pb.chinese}（${godName(pb, dayEl)}）衰弱，无力化泄从势改变格局，反杂乱纯势——忌神"
                }
            }
            // 其余破格神（印比官之不在势不克势者）
            poGeShen(pb) -> {
                if (strong) {
                    verdict = YunVerdict.ZHUAN_GE; newEl = pb
                    basis = "岁运${p.fullName}：${pb.chinese}（${godName(pb, dayEl)}）干支同心强旺，与从势两停——格局转两气从格，取${pb.chinese}为用"
                } else {
                    verdict = YunVerdict.JI; newEl = null
                    basis = "岁运${p.fullName}：${pb.chinese}（${godName(pb, dayEl)}）衰弱，无力转变格局，反杂乱气势——忌神"
                }
            }
            else -> {
                verdict = YunVerdict.JI; newEl = null
                basis = "岁运${p.fullName}与从势${shi.joinToString("") { it.chinese }}无关且无力破格——杂乱气势，以平淡偏忌论"
            }
        }

        return CongGeYunResult(
            geName = base.name,
            shi = shi,
            verdict = verdict,
            newYongShen = newEl,
            basis = "【${base.name}·从${shi.joinToString("") { it.chinese }}】$basis",
        )
    }

    private fun godName(el: Element, dayEl: Element): String = when {
        el == dayEl -> "比劫"
        el.generates == dayEl -> "印绶"
        dayEl.generates == el -> "食伤"
        dayEl.controls == el -> "财星"
        else -> "官杀"
    }

    /** 天干五合化神（lo=两干序号较小者：0甲己土/1乙庚金/2丙辛水/3丁壬木/4戊癸火）。 */
    private fun heEl(lo: Int): Element = when (lo) {
        0 -> Element.EARTH; 1 -> Element.METAL; 2 -> Element.WATER; 3 -> Element.WOOD; else -> Element.FIRE
    }

    /**
     * 专旺格行运喜忌。理论见 `.shengke-reference.md` 第五四节(三)：
     * - 比劫（双体或干支覆载相生）→ 用神到位；
     * - 印绶 → 喜神到位（喜用常通称）；
     * - 官杀/财星/食伤：干支双体或覆载相生（财生官/食伤生财/比生食伤）→ 强旺扭转格局——
     *   转身官两停/身财两停/食伤泄秀（转格取用）；
     * - 盖头截脚（干支相克）或**支被原局半合半会拉向专旺行**（辰被三卯半会、未被三卯半合——
     *   失去生辅且合化不完全破坏清纯）→ 激犯旺气——忌神；
     * - 支与原局**合成全三合/三会/六合**归本局（亥补亥卯未——弥补合局不全反加格局气势），
     *   干头破格之神衰微只能顺从 → 喜用（己亥例）。
     */
    private fun zhuanWangYun(bazi: BaziResult, base: cn.xulun.bazi.model.TeShuGeJuResult, p: Pillar): CongGeYunResult {
        val dayEl = bazi.dayPillar.stem.element
        val yinEl = Element.entries.first { it.generates == dayEl }
        val ps = p.stem.element
        val pb = p.branch.element
        val guanEl = Element.entries.first { it.controls == dayEl }
        val caiEl = dayEl.controls
        val shangEl = dayEl.generates

        val triAll = listOf(
            setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI) to Element.WOOD,
            setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU) to Element.METAL,
            setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN) to Element.WATER,
            setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU) to Element.FIRE,
        )
        val huiAll = listOf(
            setOf(EarthlyBranch.YIN, EarthlyBranch.MAO, EarthlyBranch.CHEN) to Element.WOOD,
            setOf(EarthlyBranch.SI, EarthlyBranch.WU, EarthlyBranch.WEI) to Element.FIRE,
            setOf(EarthlyBranch.SHEN, EarthlyBranch.YOU, EarthlyBranch.XU) to Element.METAL,
            setOf(EarthlyBranch.HAI, EarthlyBranch.ZI, EarthlyBranch.CHOU) to Element.WATER,
        )
        val liuMap = listOf(
            (EarthlyBranch.ZI to EarthlyBranch.CHOU) to Element.EARTH,
            (EarthlyBranch.YIN to EarthlyBranch.HAI) to Element.WOOD,
            (EarthlyBranch.MAO to EarthlyBranch.XU) to Element.FIRE,
            (EarthlyBranch.CHEN to EarthlyBranch.YOU) to Element.METAL,
            (EarthlyBranch.SI to EarthlyBranch.SHEN) to Element.WATER,
            (EarthlyBranch.WU to EarthlyBranch.WEI) to Element.EARTH,
        )
        val zhongMap = mapOf(
            Element.WOOD to EarthlyBranch.MAO, Element.FIRE to EarthlyBranch.WU,
            Element.METAL to EarthlyBranch.YOU, Element.WATER to EarthlyBranch.ZI,
        )
        // 原局支是否已变节归局（全局三合三会 / 半合中神在局 / 六合成对，化神向我生我）——
        // 变节之财官支不算隐患（卯未半合之未已向木——木章行运例财星强仍转格两停）
        fun bianJieZhi(b: EarthlyBranch): Boolean {
            if (b.element == dayEl || b.element == yinEl) return true
            val toward = { el: Element -> el == dayEl || el == yinEl }
            val full = (triAll + huiAll).any { (g, el) -> toward(el) && b in g && g.all { it in bazi.branches } }
            val ban = zhongMap[dayEl]?.let { z -> triAll.any { (g, el) -> toward(el) && b in g && b != z && z in bazi.branches } } == true
            val liu = liuMap.any { (pr, el) -> toward(el) && (b == pr.first || b == pr.second) &&
                (if (b == pr.first) pr.second in bazi.branches else pr.first in bazi.branches) }
            return full || ban || liu
        }
        // 假专旺隐患行：原局未变节之财官（干透或孤立本气支）——"强众而敌寡，势在去其寡"
        val yinHuanSet = setOf(caiEl, guanEl).filter { el ->
            bazi.stems.any { it.element == el } || bazi.branches.any { it.element == el && !bianJieZhi(it) }
        }
        // 岁运支补成全三合/三会归专旺行（原局缺此支）
        val joinsFull = (triAll + huiAll).any { (g, el) ->
            p.branch in g && el == dayEl && g.all { it in bazi.branches || it == p.branch } && !bazi.branches.toSet().containsAll(g)
        } || (dayEl == Element.WOOD && ((p.branch == EarthlyBranch.YIN && EarthlyBranch.HAI in bazi.branches) ||
            (p.branch == EarthlyBranch.HAI && EarthlyBranch.YIN in bazi.branches)))
        // 岁运支被原局半合（中神在原局）/半会/六合拉向专旺行——合化不完全或被合住
        val pulledPartial = run {
            if (pb == dayEl || pb == yinEl) return@run false
            val zhong = mapOf(
                Element.WOOD to EarthlyBranch.MAO, Element.FIRE to EarthlyBranch.WU,
                Element.METAL to EarthlyBranch.YOU, Element.WATER to EarthlyBranch.ZI,
            )
            val banHe = zhong[dayEl]?.let { z ->
                triAll.any { (g, _) -> z in g && p.branch in g && p.branch != z && z in bazi.branches }
            } == true
            val gong = when (dayEl) {
                Element.WOOD -> listOf(setOf(EarthlyBranch.YIN, EarthlyBranch.MAO), setOf(EarthlyBranch.MAO, EarthlyBranch.CHEN))
                Element.FIRE -> listOf(setOf(EarthlyBranch.SI, EarthlyBranch.WU), setOf(EarthlyBranch.WU, EarthlyBranch.WEI))
                Element.METAL -> listOf(setOf(EarthlyBranch.SHEN, EarthlyBranch.YOU), setOf(EarthlyBranch.YOU, EarthlyBranch.XU))
                Element.WATER -> listOf(setOf(EarthlyBranch.HAI, EarthlyBranch.ZI), setOf(EarthlyBranch.ZI, EarthlyBranch.CHOU))
                else -> emptyList<Set<EarthlyBranch>>()
            }
            val banHui = gong.any { g -> p.branch in g && g.any { it in bazi.branches && it != p.branch } }
            // 六合向专旺行（子丑合土——壬子庚子之水被原局丑合住，财星力减）
            val liuMap = listOf(
                (EarthlyBranch.ZI to EarthlyBranch.CHOU) to Element.EARTH,
                (EarthlyBranch.YIN to EarthlyBranch.HAI) to Element.WOOD,
                (EarthlyBranch.MAO to EarthlyBranch.XU) to Element.FIRE,
                (EarthlyBranch.CHEN to EarthlyBranch.YOU) to Element.METAL,
                (EarthlyBranch.SI to EarthlyBranch.SHEN) to Element.WATER,
                (EarthlyBranch.WU to EarthlyBranch.WEI) to Element.EARTH,
            )
            val liuPull = liuMap.any { (pr, el) ->
                el == dayEl && (p.branch == pr.first || p.branch == pr.second) &&
                    (if (p.branch == pr.first) pr.second in bazi.branches else pr.first in bazi.branches)
            }
            banHe || banHui || liuPull
        }
        // 拉走是否阻断转格：六合捕获（子丑合住——壬子庚子）恒阻断；
        // 半合/半会唯**墓库支**（丑辰未戌被旺局吞没——戊辰己未之土损、庚辰辛未失生）阻断，
        // 生地支（巳寅亥申）之合为顺势长生不损干（丁巳巳酉合金仍强旺转格）
        val pulledBlocks = run {
            if (pb == dayEl || pb == yinEl) return@run false
            val liuB = listOf(
                (EarthlyBranch.ZI to EarthlyBranch.CHOU) to Element.EARTH,
                (EarthlyBranch.YIN to EarthlyBranch.HAI) to Element.WOOD,
                (EarthlyBranch.MAO to EarthlyBranch.XU) to Element.FIRE,
                (EarthlyBranch.CHEN to EarthlyBranch.YOU) to Element.METAL,
                (EarthlyBranch.SI to EarthlyBranch.SHEN) to Element.WATER,
                (EarthlyBranch.WU to EarthlyBranch.WEI) to Element.EARTH,
            ).any { (pr, el) ->
                el == dayEl && (p.branch == pr.first || p.branch == pr.second) &&
                    (if (p.branch == pr.first) pr.second in bazi.branches else pr.first in bazi.branches)
            }
            val muKu = p.branch in setOf(EarthlyBranch.CHOU, EarthlyBranch.CHEN, EarthlyBranch.WEI, EarthlyBranch.XU)
            liuB || (pulledPartial && muKu)
        }
        // 支与原局半合（中神在原局，p.branch 为生地/墓地）化神即日主行 → 支归局（巳酉合金——辛巳助旺为用）
        val zhiGuiJu = run {
            val zhong = mapOf(
                Element.WOOD to EarthlyBranch.MAO, Element.FIRE to EarthlyBranch.WU,
                Element.METAL to EarthlyBranch.YOU, Element.WATER to EarthlyBranch.ZI,
            )
            val banHeGui = zhong[dayEl]?.let { z ->
                triAll.any { (g, _) -> z in g && p.branch in g && p.branch != z && z in bazi.branches }
            } == true
            val liuGui = listOf(
                (EarthlyBranch.ZI to EarthlyBranch.CHOU) to Element.EARTH,
                (EarthlyBranch.YIN to EarthlyBranch.HAI) to Element.WOOD,
                (EarthlyBranch.MAO to EarthlyBranch.XU) to Element.FIRE,
                (EarthlyBranch.CHEN to EarthlyBranch.YOU) to Element.METAL,
                (EarthlyBranch.SI to EarthlyBranch.SHEN) to Element.WATER,
                (EarthlyBranch.WU to EarthlyBranch.WEI) to Element.EARTH,
            ).any { (pr, el) ->
                el == dayEl && (p.branch == pr.first || p.branch == pr.second) &&
                    (if (p.branch == pr.first) pr.second in bazi.branches else pr.first in bazi.branches)
            }
            banHeGui || liuGui
        }
        // 岁运干与原局透干（含日干）五合化神即日主行——贪合化势顺局：
        // 甲子与日柱己丑天地合化土（格局未变仍取土为用）；甲辰甲戌甲合己化土而支归比劫——不以忌论
        val heHuaShunJu = run {
            val heOk = listOf(bazi.dayPillar.stem, bazi.yearPillar.stem, bazi.monthPillar.stem, bazi.hourPillar.stem)
                .filter { it !== p.stem }
                .any { o ->
                    (p.stem.ordinal - o.ordinal + 10) % 5 == 0 && p.stem.ordinal != o.ordinal &&
                        heEl(minOf(p.stem.ordinal, o.ordinal)) == dayEl
                }
            val zhiHe = listOf(
                (EarthlyBranch.ZI to EarthlyBranch.CHOU) to Element.EARTH,
                (EarthlyBranch.YIN to EarthlyBranch.HAI) to Element.WOOD,
                (EarthlyBranch.MAO to EarthlyBranch.XU) to Element.FIRE,
                (EarthlyBranch.CHEN to EarthlyBranch.YOU) to Element.METAL,
                (EarthlyBranch.SI to EarthlyBranch.SHEN) to Element.WATER,
                (EarthlyBranch.WU to EarthlyBranch.WEI) to Element.EARTH,
            ).any { (pr, el) ->
                el == dayEl && (p.branch == pr.first || p.branch == pr.second) &&
                    (if (p.branch == pr.first) pr.second in bazi.branches else pr.first in bazi.branches)
            }
            heOk && (pb == dayEl || pb == yinEl || zhiHe)
        }
        // 岁运干与原局透干（含日干）五合（不限化神）——被合绊（癸巳戊癸合）
        val ganHe = listOf(bazi.dayPillar.stem, bazi.yearPillar.stem, bazi.monthPillar.stem, bazi.hourPillar.stem)
            .filter { it !== p.stem }
            .any { o -> (p.stem.ordinal - o.ordinal + 10) % 5 == 0 && p.stem.ordinal != o.ordinal }
        // 五合且化神即日主行——真化顺局（乙巳乙庚化金），纵支为墓库被吞亦顺（乙丑酉丑半合仍不以忌论）
        val ganHeDay = listOf(bazi.dayPillar.stem, bazi.yearPillar.stem, bazi.monthPillar.stem, bazi.hourPillar.stem)
            .filter { it !== p.stem }
            .any { o ->
                (p.stem.ordinal - o.ordinal + 10) % 5 == 0 && p.stem.ordinal != o.ordinal &&
                    heEl(minOf(p.stem.ordinal, o.ordinal)) == dayEl
            }

        val verdict: YunVerdict
        val newEl: Element?
        val basis: String
        when {
            // 比劫：双体或覆载相生（干比坐印支、印坐比支）→ 用神到位
            (pb == dayEl && (ps == dayEl || ps == yinEl)) || (ps == dayEl && (pb == dayEl || pb == yinEl)) -> {
                verdict = YunVerdict.YONG; newEl = null
                basis = "岁运${p.fullName}：干支比劫覆载相生，强盛${dayEl.chinese}气加旺格局气势——用神到位"
            }
            // 假专旺（原局未变节之财官隐患——"强众而敌寡，势在去其寡"）：岁运帮扶隐患之行 → 与旺势交战——凶
            // （候先生戊午年助午火、赵先生甲戌年透甲——皆灾；干被原局五合绊者不忌——丙戌运丙辛合不为大忌）
            base.fake && yinHuanSet.any { el -> pb == el || (ps == el && !ganHe) } -> {
                val el = yinHuanSet.first { el -> pb == el || (ps == el && !ganHe) }
                verdict = YunVerdict.JI; newEl = null
                basis = "岁运${p.fullName}：原局${el.chinese}（${godName(el, dayEl)}）为假专旺之先天隐患，" +
                    "今得帮扶由弱变强，与旺${dayEl.chinese}交战——逆忤旺势，凶咎由此而生（忌神）"
            }
            // 五合化势顺局（化神即日主行）：天地合化或支归比印——格局未变仍取日主为用
            heHuaShunJu -> {
                verdict = YunVerdict.YONG; newEl = null
                basis = "岁运${p.fullName}：干${p.stem.chinese}与原局相合化${dayEl.chinese}（贪合化势，支亦归局），格局未变仍取${dayEl.chinese}为用神——不以忌论"
            }
            // 支合入本局成全三合/三会/六合而干为破格之神衰微（支归局+干顺从）→ 喜用（己亥例）
            joinsFull && pb != dayEl && pb != yinEl && ps != dayEl && ps != yinEl -> {
                verdict = YunVerdict.XI; newEl = null
                basis = "岁运${p.fullName}：支${p.branch.chinese}与原局合入本局（弥补合局不全、反加大格局气势），" +
                    "干头${p.stem.chinese}（${ps.chinese}）衰微只能顺从旺${dayEl.chinese}，利大于弊——喜用神"
            }
            // 支与原局半合/六合化神即日主行 → 支归局：真化（干合化神即日主行——乙巳乙庚化金）恒顺局；
            // 干为比劫（辛巳巳酉合金）或干被合绊（癸巳戊癸合）须支未被吞（丙戌比坐墓库被午戌半合吞仍忌）——
            // 助局之利大于杂局之弊，不以忌论（印支本身如辰仍归印档——戊辰己丑双体印为喜）
            zhiGuiJu && pb != yinEl && pb != dayEl &&
                (ganHeDay || (!pulledBlocks && (ps == dayEl || ps == yinEl || ganHe))) -> {
                verdict = if (ps == dayEl || ps == yinEl) YunVerdict.YONG else YunVerdict.XI; newEl = null
                basis = "岁运${p.fullName}：支${p.branch.chinese}与原局半合化${dayEl.chinese}归局" +
                    (if (ps == dayEl || ps == yinEl) "，干${p.stem.chinese}比印扶旺——用神到位"
                     else "，干${p.stem.chinese}被原局合绊衰微顺从——喜用神不以忌论")
            }
            // 印绶 → 喜神到位
            pb == yinEl -> {
                verdict = YunVerdict.XI; newEl = null
                basis = "岁运${p.fullName}：印绶（${yinEl.chinese}）扶旺格局气势——喜神到位" +
                    (if (joinsFull) "（支${p.branch.chinese}并补全本局合局）" else "")
            }
            else -> {
                val po3 = setOf(guanEl, caiEl, shangEl) // 官杀/财/食伤——可扭转格局之三线
                if (ps !in po3 && pb !in po3) {
                    verdict = YunVerdict.JI; newEl = null
                    basis = "岁运${p.fullName}与专旺气势关联不紧——以平淡偏忌论"
                } else {
                    // 干与原局透干五合化神为转格线而支即该行——化势转格（丙子：丙辛合水+子水，食伤泄秀为用）
                    val heEl0 = listOf(bazi.dayPillar.stem, bazi.yearPillar.stem, bazi.monthPillar.stem, bazi.hourPillar.stem)
                        .filter { it !== p.stem }
                        .mapNotNull { o ->
                            if ((p.stem.ordinal - o.ordinal + 10) % 5 == 0 && p.stem.ordinal != o.ordinal)
                                heEl(minOf(p.stem.ordinal, o.ordinal)) else null
                        }
                        .firstOrNull { it in po3 }
                    // 强旺：双体或覆载相生（受生者须为转格线——财生官/食伤生财/比生食伤/印生干）
                    val newE = when {
                        ps == pb -> ps
                        pb.generates == ps && ps in po3 -> ps
                        ps.generates == pb && pb in po3 -> pb
                        heEl0 != null && pb == heEl0 -> heEl0
                        else -> null
                    }
                    if (newE != null && !pulledBlocks) {
                        verdict = YunVerdict.ZHUAN_GE; newEl = newE
                        basis = "岁运${p.fullName}：" + when (newE) {
                            guanEl -> "官杀（${newE.chinese}）干支强旺克住日主，与旺${dayEl.chinese}成身杀两停——转格取官杀为用，财滋杀为喜"
                            caiEl -> "财星（${newE.chinese}）干支强旺耗住日主，成身财两停——转格取财为用，食伤生财为喜"
                            else -> "食伤（${newE.chinese}）干支强旺化泄日主，成食伤泄秀（身伤两停）——转格取食伤为用，并取财星"
                        }
                    } else {
                        verdict = YunVerdict.JI; newEl = null
                        basis = "岁运${p.fullName}：${godName(if (ps in po3) ps else pb, dayEl)}力弱" +
                            (if (pulledPartial) "（支${p.branch.chinese}被原局半会半合拉向旺${dayEl.chinese}，干失生辅且合化不完全破坏清纯）"
                             else "（干支盖头截脚互不相生）") +
                            "，不足以扭转格局，反激犯日主旺气、逆乱专旺之势——忌神"
                    }
                }
            }
        }

        return CongGeYunResult(
            geName = base.name,
            shi = base.yongShen,
            verdict = verdict,
            newYongShen = newEl,
            basis = "【${base.name}】$basis",
        )
    }
}
