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
                // 势首克日主（从杀式链含印行）→按单从论：势首=用、生势首=喜（乙庚化金例二：金用土喜）
                val shiShou = shi.maxBy { f0x[it]!! }
                if (shiShou.controls == dayEl && pb == shiShou) {
                    verdict = YunVerdict.YONG; newEl = null
                    basis = "岁运" + p.fullName + "：" + pb.chinese + "为从势之首（克身之神），助旺格局——用神到位"
                } else if (shiShou.controls == dayEl && pb.generates == shiShou && pb != yinEl) {
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
}
