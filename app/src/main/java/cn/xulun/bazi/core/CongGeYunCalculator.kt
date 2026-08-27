package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.CongGeYunResult
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

        // 破格神集合：印、比劫、官杀、克势之行（从杀之食伤——食伤克杀）之不在从势者
        fun poGeShen(el: Element): Boolean = el !in shi &&
            (el == yinEl || el == dayEl || el.controls == dayEl || shi.any { el.controls == it })

        val verdict: YunVerdict
        val newEl: Element?
        val basis: String
        when {
            pb in shi && shi.any { ps.controls == it } -> {
                verdict = YunVerdict.JI; newEl = null
                basis = "岁运${p.fullName}：支${p.branch.chinese}虽在从势，而干${p.stem.chinese}（${ps.chinese}）克从势之行，逆乱格局气势——忌神"
            }
            pb in shi -> {
                verdict = YunVerdict.YONG; newEl = null
                basis = "岁运${p.fullName}之支${p.branch.chinese}（${pb.chinese}）在从势内，助旺格局——用神到位"
            }
            // 支为日主本气行：假从者还原正格（刑动/帮身破格——刘女士/杨先生例）；真从者与势两停转为身势两气（总表戊戌例）
            pb == dayEl -> {
                if (base.fake) {
                    verdict = YunVerdict.HUAN_YUAN; newEl = null
                    basis = "岁运${p.fullName}之支${p.branch.chinese}为日主${dayEl.chinese}之根，原局假从遇根——刑动/帮身破格，还原身弱正格论"
                } else {
                    verdict = YunVerdict.ZHUAN_GE; newEl = dayEl
                    basis = "岁运${p.fullName}：比劫（${dayEl.chinese}）助日主克住旺势，与势成两停——格局变身为势两气成象，取比劫平衡气势"
                }
            }
            // 势生之行（泄势者，从儿之財）：强→转两气取之；弱→忌（化泄无力反杂纯势）
            shi.any { it.generates == pb } && pb != dayEl -> {
                val strong = ps == pb || ps.generates == pb
                if (strong) {
                    verdict = YunVerdict.ZHUAN_GE; newEl = pb
                    basis = "岁运${p.fullName}：${pb.chinese}（${godName(pb, dayEl)}）化泄从势旺气，干支强旺与势两停——格局转两气从格，取${pb.chinese}为用以平衡气势"
                } else {
                    verdict = YunVerdict.JI; newEl = null
                    basis = "岁运${p.fullName}：${pb.chinese}（${godName(pb, dayEl)}）衰弱，无力化泄从势改变格局，反杂乱纯势——忌神"
                }
            }
            // 支为破格神行：强弱定转格或忌（支主导——壬寅例：支官杀当权重于干财）
            poGeShen(pb) -> {
                val strong = ps == pb || ps.generates == pb
                if (strong) {
                    verdict = YunVerdict.ZHUAN_GE; newEl = pb
                    basis = "岁运${p.fullName}：${pb.chinese}（${godName(pb, dayEl)}）干支同心强旺，与从势两停——格局转两气从格，取${pb.chinese}为用（按两气成象取用）"
                } else {
                    verdict = YunVerdict.JI; newEl = null
                    basis = "岁运${p.fullName}：${pb.chinese}（${godName(pb, dayEl)}）衰弱（干不同支不生支），无力转变格局，反杂乱气势/引日主破格伤身——忌神"
                }
            }
            // 干透势行（壬申例：支食伤生势而干透财）
            ps in shi -> {
                verdict = YunVerdict.YONG; newEl = null
                basis = "岁运${p.fullName}之干${p.stem.chinese}（${ps.chinese}）透从势之行，助旺格局——用神到位"
            }
            // 支生势（食伤扶旺）
            shi.any { pb.generates == it } && !poGeShen(pb) -> {
                verdict = YunVerdict.XI; newEl = null
                basis = "岁运${p.fullName}之支${p.branch.chinese}（${pb.chinese}）生扶从势（食伤）——喜神到位"
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
