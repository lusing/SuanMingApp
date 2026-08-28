package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DaYunInfo
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HealthYear
import cn.xulun.bazi.model.HealthYearKind
import cn.xulun.bazi.model.HealthYun
import cn.xulun.bazi.model.HealthYunResult
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.branchHiddenStems

/**
 * 健康·岁运批断引擎（.health-reference.md 三章五节应期）。原局伏病根→大运成形→流年引发：
 * - **病发**：①岁运干支为病根行之克泄耗且全局受损度≥2倍；②病根行之根支被冲刑（根拔）；
 *   ③岁干与病根根支本气之干相同（引动——乙引动乙卯）；④从象之行岁运还原而遭冲克。
 * - **缓解**：岁运干支为通关之行或生扶病根之行（水生木式）——病情缓解。
 * - **车祸**：天干水火冲（岁干与原局干丙壬/丁癸相冲）+ 支冲水火战（子午/巳亥）；
 *   水火各有其力（各≥2）——行驰为水、流血为火。
 * 疾病按五行脏腑对应给病名；伤灾按战对给伤类。
 */
object HealthYunCalculator {

    private val zangFu = mapOf(
        Element.WOOD to "肝胆", Element.FIRE to "心小肠", Element.EARTH to "脾胃",
        Element.METAL to "肺大肠", Element.WATER to "肾膀胱",
    )
    private val ganChong = listOf(
        setOf(HeavenlyStem.BING, HeavenlyStem.REN), setOf(HeavenlyStem.DING, HeavenlyStem.GUI),
    )

    private fun br2(b: EarthlyBranch): EarthlyBranch = EarthlyBranch.entries[(b.ordinal + 6) % 12]

    fun analyze(bazi: BaziResult, daYun: DaYunInfo): HealthYunResult {
        val base = BingYaoCalculator.forces(bazi)
        val dayEl = bazi.dayPillar.stem.element

        // 原局病根行:受损度≥2倍且弱而不从(与 HealthCalculator 同口径)
        val cong = BingYaoCalculator.analyze(bazi).congGe != null
        data class Root(val el: Element, val own: Double, val crowd: Double)
        val roots = Element.entries.mapNotNull { el ->
            val own = base[el]!!
            if (own <= 0.5) return@mapNotNull null
            if (cong && own <= 1.0) return@mapNotNull null
            val crowd = Element.entries.filter { it != el }.filter {
                it.controls == el || el.generates == it || el.controls == it
            }.sumOf { base[it]!! }
            if (crowd >= 2.0 * own && own < 3.0) Root(el, own, crowd) else null
        }
        val rootNote = if (roots.isEmpty()) "原局无明显受损病根之行——岁运纵克亦难成疾"
        else "原局病根：" + roots.joinToString("、") { "${it.el.chinese}行(${zangFu[it.el]})力${it.own}对克泄耗${it.crowd}" }

        // 病根行之根支(本气==病根行)与根支本气干(引动判据)
        fun rootBranches(el: Element) = bazi.branches.filter { it.element == el }

        val yuns = daYun.yuns.filter { it.startAge >= 6 }.map { yun ->
            val years = yun.liuNian.map { ln ->
                val yb = ln.pillar.branch
                val ys = ln.pillar.stem
                val f = base.toMutableMap()
                f[yun.pillar.stem.element] = f[yun.pillar.stem.element]!! + 1
                f[yun.pillar.branch.element] = f[yun.pillar.branch.element]!! + 1
                f[ys.element] = f[ys.element]!! + 1
                f[yb.element] = f[yb.element]!! + 1

                val triggers = mutableListOf<String>()
                var kind = HealthYearKind.PING
                var text = "岁运对病根之行无显著克泄引发——平安之年。"

                // ① 车祸:天干水火冲(岁干与原局干)+支冲水火战,水火各有其力
                val shuiHuoChong = ganChong.any { p ->
                    (ys in p) && bazi.stems.any { it != ys && it in p }
                }
                val zhiShuiHuoZhan = (yb == EarthlyBranch.ZI || yb == EarthlyBranch.WU ||
                    yb == EarthlyBranch.SI || yb == EarthlyBranch.HAI) &&
                    (br2(yb) == yun.pillar.branch || bazi.branches.contains(br2(yb)))
                if (shuiHuoChong && (zhiShuiHuoZhan || f[Element.WATER]!! >= 3)) {
                    triggers += "天干${ys.chinese}与原局水火相冲" +
                        (if (zhiShuiHuoZhan) "；支逢${yb.chinese}${br2(yb).chinese}冲水火交战" else "")
                    kind = HealthYearKind.CHE_HUO
                    text = "岁干与原局天干水火相冲${if (zhiShuiHuoZhan) "，地支水火交战" else ""}——车祸之应：" +
                        "行驰为水、流血为火（水火战与车祸流血之象全同）；此年乘车出行务须谨慎，特定时段避乘可免。"
                }

                // ② 逐病根行判病发(优先)——三引发源+从象还原遭克
                if (kind == HealthYearKind.PING) {
                    // 微行岁运还原而遭冲克(例胡先生丁火藏未原局火0,壬午年午火还原被运支子冲——心脏病)
                    run {
                        Element.entries.forEach { el ->
                            if (base[el]!! <= 0.5 && yb.element == el &&
                                (br2(yb) == yun.pillar.branch || bazi.branches.contains(br2(yb)))
                            ) {
                                    triggers += "从象之${el.chinese}行岁运还原（${yb.chinese}）而遭${br2(yb).chinese}冲克"
                                kind = HealthYearKind.BING
                                text = "从象之${el.chinese}行（${zangFu[el]}）岁运还原不再从，又被${br2(yb).chinese}冲克——" +
                                    "${zangFu[el]}之疾发难之年（例胡先生壬午年午火还原子水冲克心脏病式）。"
                            }
                        }
                    }
                    if (kind == HealthYearKind.PING) {
                        outer@ for (r in roots) {
                            val el = r.el
                            val keEl = Element.entries.first { it.controls == el }
                            val yearEl = setOf(ys.element, yb.element)
                            val crowd2 = Element.entries.filter { it != el }.filter {
                                it.controls == el || el.generates == it || el.controls == it
                            }.sumOf { f[it]!! }
                            when {
                                // 病根根支被岁支冲刑——根拔
                                rootBranches(el).any { br2(yb) == it } -> {
                                    triggers += "${yb.chinese}冲${rootBranches(el).joinToString("") { it.chinese }}——${el.chinese}行之根被拔"
                                    kind = HealthYearKind.BING
                                    text = "流年冲病根${el.chinese}行之根——${zangFu[el]}之疾发难之年，宜就医调养。"
                                    break@outer
                                }
                                // 岁干与根支本气干相同——引动(乙引动乙卯)
                                rootBranches(el).any { br -> branchHiddenStems.getValue(br).firstOrNull() == ys } -> {
                                    triggers += "${ys.chinese}引动原局${rootBranches(el).joinToString("") { it.chinese }}之${el.chinese}"
                                    if (crowd2 >= f[el]!!) {
                                        kind = HealthYearKind.BING
                                        text = "流年引动病根${el.chinese}行而克泄耗之行合力${crowd2}压过其力${f[el]}——" +
                                            "${zangFu[el]}病发之年（引动而力不敌众——例林先生乙丑年查出肝病式）。"
                                    }
                                    break@outer
                                }
                                // 岁运为克病根之行且全局受损≥2倍原局基力
                                // 例外:岁支/岁干为克其克者之行(制敌护根——巳火制金护木式)则病发不取
                                (ys.element == keEl || yb.element == keEl || yun.pillar.branch.element == keEl) &&
                                    crowd2 >= 2.0 * r.own &&
                                    !(ys.element.controls == keEl || yb.element.controls == keEl) -> {
                                    triggers += "岁运${keEl.chinese}当值克${el.chinese}，全局克泄耗${crowd2}对原局基力${r.own}"
                                    kind = HealthYearKind.BING
                                    text = "克病根${el.chinese}行之${keEl.chinese}值岁运，全局受损倍增——" +
                                        "${zangFu[el]}病发或加重之年（例张女士戊寅己卯年胃病显症式）。"
                                    break@outer
                                }
                            }
                        }
                    }
                }

                // ③ 缓解:岁运为通关之行或生扶之行
                if (kind == HealthYearKind.PING) {
                    outer2@ for (r in roots) {
                        val el = r.el
                        val keEl = Element.entries.first { it.controls == el }
                        val shengEl = Element.entries.first { it.generates == el }
                        val tongEl = Element.entries.firstOrNull { t -> keEl.generates == t && t.generates == el }
                        val yearEl = setOf(ys.element, yb.element)
                        if (yearEl.any { it == tongEl || it == shengEl }) {
                            triggers += "岁运${if (tongEl != null && yearEl.contains(tongEl)) "通关（${keEl.chinese}生${tongEl.chinese}生${el.chinese}）" else "生扶（生${el.chinese}）"}病根${el.chinese}行"
                            kind = HealthYearKind.HUAJIE
                            text = "岁运${if (tongEl != null && yearEl.contains(tongEl)) "通关之行到位" else "生扶之行到位"}——" +
                                "病情缓解（例张女士辛巳壬午火通关胃病减、林先生癸亥甲子水生木肝恙缓式）。"
                            break@outer2
                        }
                    }
                }

                HealthYear(
                    ln.year,
                    "${ln.year} ${ys.chinese}${yb.chinese}",
                    kind,
                    triggers,
                    text,
                )
            }
            HealthYun(yun.pillar, "${yun.pillar.fullName} ${yun.startYear}-${yun.endYear}", years)
        }

        val summary = "原局伏病根，大运成形，流年引发——发病年=病根行之克泄耗值岁运/根支被冲/本气干被引动；" +
            "缓解年=通关或生扶之行值岁运。车祸=天干水火冲+支冲水火战（行驰为水流血为火）。预防可减免。"
        return HealthYunResult(rootNote = rootNote, yuns = yuns, summary = summary)
    }
}
