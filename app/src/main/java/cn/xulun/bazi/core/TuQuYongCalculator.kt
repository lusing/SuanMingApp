package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.GodCategory
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.TuQuYongResult
import cn.xulun.bazi.model.TuSpecial
import cn.xulun.bazi.model.TuTiaoHou
import cn.xulun.bazi.model.branchHiddenStems
import kotlin.math.abs

/**
 * 土之逐月取用（戊己日主）。理论见 `.shengke-reference.md` 第四六节（春/夏/秋/冬土取用）。
 *
 * 土命专用概念：
 * - **春土虚而不实**（藏干有木、卯辰贴根——木实土虚）；
 * - **水金并用**（水有金源头则弱而不绝，成既济）；
 * - **合局难化**（中神受制/被冲/化神不透，待时而用）；
 * - **丁壬合火失暖**（冬月印被合不能暖身，取支中丙火）；
 * - **身强局暖反取财官**（冬土四众围克，过强则抑）；
 * - **喜用相生不宜相战**（官杀伤食不并用）。
 *
 * 强弱档位复用 [StrengthEngine]（八步法）。
 */
object TuQuYongCalculator {

    /** 土命：印=火、比劫=土、食伤=金、财=水、官杀=木。 */
    private val YIN = Element.FIRE
    private val BI = Element.EARTH
    private val SHI = Element.METAL
    private val CAI = Element.WATER
    private val GUAN = Element.WOOD

    private fun cat(el: Element): GodCategory = when (el) {
        Element.FIRE -> GodCategory.YIN
        Element.EARTH -> GodCategory.BI
        Element.METAL -> GodCategory.SHI
        Element.WATER -> GodCategory.CAI
        Element.WOOD -> GodCategory.GUAN
    }

    fun analyze(bazi: BaziResult): TuQuYongResult {
        val dayEl = bazi.dayPillar.stem.element
        require(dayEl == Element.EARTH) { "土之取用仅限戊己日主" }
        val dayStem = bazi.dayPillar.stem
        val f = BingYaoCalculator.forces(bazi)
        val fc = { el: Element -> f[el]!! }
        val engine = StrengthEngine.analyze(bazi)
        val strength = engine.verdict
        val m = bazi.monthPillar.branch
        val tou = { el: Element -> bazi.stems.any { it.element == el && it != dayStem } }
        val zhiCount = { el: Element -> bazi.branches.count { it.element == el } }
        val sanHe = DiZhiRelationCalculator.analyze(bazi).sanHeList
        val liuHe = HeHuaCalculator.analyze(bazi).branchLiuHeList
        val shiTu = bazi.branches.any { it == EarthlyBranch.CHOU || it == EarthlyBranch.CHEN }
        val zaoTu = bazi.branches.any { it == EarthlyBranch.WEI || it == EarthlyBranch.XU }
        val dingRenHe = bazi.stems.any { it == HeavenlyStem.DING } && bazi.stems.any { it == HeavenlyStem.REN }
        // 春土虚：藏干有木（支中木气培根，木实土虚）
        val xuTu = bazi.branches.count { br -> branchHiddenStems[br]!!.any { it.element == Element.WOOD } } >= 2
        // 申子辰难化：化神（金）不透，或中神（子）被未/丑当令贴克
        val shuiJuNanHua = sanHe.any { it.resultElement == Element.WATER } &&
            (!tou(Element.METAL) || EarthlyBranch.WEI in bazi.branches || EarthlyBranch.CHOU in bazi.branches)
        val jinJuNanHua = sanHe.any { it.resultElement == Element.METAL } &&
            (bazi.branches.contains(EarthlyBranch.MAO) || !tou(Element.METAL)) // 酉被卯冲或金不透

        var yong: GodCategory
        var fu: GodCategory? = null
        var detail: String
        var tiaoHou: TuTiaoHou
        val rule: String

        when (m) {
            EarthlyBranch.YIN, EarthlyBranch.MAO -> {
                rule = if (m == EarthlyBranch.YIN) "寅月寒气未除木实土虚，首取火调候兼化杀生身（一物两用）"
                else "卯月阳气渐进调候为次扶抑紧要；春土虚而不实"
                tiaoHou = TuTiaoHou.XU_HUO
                yong = when {
                    fc(GUAN) >= 3 && tou(Element.FIRE) -> cat(YIN).also { fu = cat(BI) } // 杀重首火，土为喜
                    fc(GUAN) >= 3 && zhiCount(Element.FIRE) + zhiCount(Element.EARTH) == 0 -> cat(SHI) // 火土皆无，金制杀去病
                    fc(GUAN) >= 3 -> cat(BI) // 无火用土帮身
                    fc(SHI) >= 4 -> cat(YIN) // 伤食重火制
                    fc(CAI) >= 4 -> cat(YIN).also { fu = cat(BI) } // 财重带寒，火土并用
                    strength != DayStrength.WEAK -> cat(GUAN).also { fu = cat(CAI) } // 身强杀浅木疏，水生木财官并用
                    else -> cat(YIN).also { fu = cat(BI) }
                }
                detail = "身弱杀重首丙火（丁次）化杀生身一物两用；最忌水，木不宜重见；身强杀浅木疏土（金衰绝不如木），忌金伤木宜水生木"
                if (xuTu) detail = "地支藏干皆有木，木实土虚，土多未必强；根不固未拔以身弱正格论。" + detail
            }
            EarthlyBranch.CHEN -> {
                rule = "辰月土司令木退气，既旺且实，春末之木仍有制土之力"
                tiaoHou = TuTiaoHou.BU_XU
                yong = when {
                    strength != DayStrength.WEAK -> cat(SHI).also { fu = cat(CAI) } // 当令得势身旺财弱，金水为用
                    fc(GUAN) >= 3 -> cat(YIN).also { fu = cat(BI) }
                    else -> cat(BI)
                }
                detail = "当令得势，身旺财弱取金水为用（金泄秀生水财）；身弱则火土并用"
            }
            EarthlyBranch.SI, EarthlyBranch.WU, EarthlyBranch.WEI -> {
                rule = "夏土炎热干燥，最宜水金并用（水须有金源头），不可过燥"
                tiaoHou = TuTiaoHou.SHUI_JIN
                val zaoJi = fc(CAI) <= 1 && !bazi.branches.any { it == EarthlyBranch.ZI || it == EarthlyBranch.HAI }
                yong = when {
                    zaoJi -> cat(CAI) // 燥极，调候之水为喜
                    strength != DayStrength.WEAK && fc(YIN) >= 4 -> cat(CAI).also { fu = cat(SHI) } // 印重只宜金水
                    strength != DayStrength.WEAK && fc(YIN) <= 2 && fc(GUAN) >= 2 -> cat(GUAN) // 印轻带水之木抑身
                    strength != DayStrength.WEAK -> cat(CAI).also { fu = cat(SHI) } // 水金并用
                    fc(CAI) >= 4 -> cat(BI) // 财重首比劫（喜燥土）
                    fc(SHI) >= 4 -> cat(BI) // 食伤重仍比劫
                    else -> cat(BI)
                }
                detail = when {
                    yong == cat(CAI) && fc(CAI) <= 1 -> "燥极（水无根调候不力），岁运金水弱则破格、强则五行流通不破——重在气势"
                    yong == cat(BI) && zaoTu -> "身弱财重首比劫，尤喜燥土（戌未），湿土次之火再次"
                    yong == cat(BI) && shiTu -> "身弱用比劫，尤喜湿土（丑辰）；不宜火增燥气"
                    else -> "水有源头（金生）则弱而不绝成既济之功；有金无水、有水无金皆缺陷"
                }
                if (shuiJuNanHua) detail += "；申子辰全而中神受制、化神不透——合局难化待时"
            }
            EarthlyBranch.SHEN, EarthlyBranch.YOU -> {
                rule = if (m == EarthlyBranch.SHEN) "申月火土之气未尽，比劫众宜金水泄身调候"
                else "酉月寒升炎退调候不首要，土更虚不宜金水太过"
                tiaoHou = TuTiaoHou.BU_XU
                yong = when {
                    strength != DayStrength.WEAK && fc(BI) >= 4 && fc(SHI) < 5 -> cat(SHI).also { fu = cat(CAI) } // 比劫众金水
                    fc(SHI) >= 5 -> cat(BI) // 金多土虚不宜水，土帮身
                    fc(CAI) >= 4 -> cat(YIN).also { fu = cat(BI) } // 水为病火土去病
                    strength != DayStrength.WEAK -> cat(SHI).also { fu = cat(CAI) }
                    else -> cat(BI).also { fu = cat(YIN) }
                }
                detail = "秋土被泄不如夏土之实，扶抑贵补虚就实；有火可用火制金生土"
                if (m == EarthlyBranch.SHEN && fc(CAI) >= 1) detail = "丑生金申辰拱水土众而不实，壬透则湿；丁火虚浮被壬遥合不足以除湿（光辉收敛）"
            }
            EarthlyBranch.XU -> {
                rule = "戌月寒升调候需虑；戌燥而不炎（火收敛收藏之地）"
                tiaoHou = TuTiaoHou.XU_HUO
                yong = when {
                    strength != DayStrength.WEAK -> cat(SHI).also { fu = cat(CAI) }
                    else -> cat(BI).also { fu = cat(YIN) }
                }
                detail = "戌为带火之土而火气收敛，燥而不炎；合会火局另当别论"
            }
            else -> { // 亥子丑
                rule = "冬土气寒喜火暖；身弱则水金木皆忌（木为火之原神宜适量）"
                tiaoHou = TuTiaoHou.DONG
                yong = when {
                    strength != DayStrength.WEAK && fc(BI) >= 4 -> cat(GUAN).also { fu = cat(CAI) } // 身强局暖反取财官
                    fc(GUAN) >= 4 -> cat(YIN).also { fu = cat(BI) }
                    dingRenHe && zhiCount(Element.FIRE) >= 1 -> cat(YIN).also { fu = cat(BI) } // 丁壬合火失暖，取支中丙火
                    else -> cat(YIN).also { fu = cat(BI) }
                }
                detail = when {
                    yong == cat(GUAN) -> "土众围克寡不敌众，燥土得火生而局不寒湿——身强反宜木疏，水木为喜；不需火调候，岁运不宜叠见金水"
                    dingRenHe -> "丁壬合使丁火不能暖身；取巳中丙火为用，喜戌中戊土帮身；身弱最忌金（水得金生克弱火）"
                    else -> "冬土调候为要，火暖生身；金生水泄土增寒为忌"
                }
            }
        }

        // 特殊态
        var special = TuSpecial.NONE
        val r = if (engine.aSide + engine.bSide == 0.0) 0.0 else (engine.aSide - engine.bSide) / (engine.aSide + engine.bSide)
        when {
            m == EarthlyBranch.SI || m == EarthlyBranch.WU || m == EarthlyBranch.WEI -> {
                if (fc(CAI) <= 1 && !bazi.branches.any { it == EarthlyBranch.ZI || it == EarthlyBranch.HAI })
                    special = TuSpecial.ZAO_JI
                else if (strength == DayStrength.BALANCED) { special = TuSpecial.SUI_YUN; detail = "原局中和，喜忌根据行运而定" + detail }
            }
            m == EarthlyBranch.SHEN && dingRenHe -> {
                special = TuSpecial.SUI_YUN
                detail = "壬透而湿；丁火虚浮无根被壬遥合，不足以除湿反被水克土晦（光辉收敛）——原局中和，喜忌根据行运而定"
            }
            m == EarthlyBranch.CHOU && fc(Element.WOOD) >= 2 && fc(Element.WATER) >= 2 -> {
                special = TuSpecial.SUI_YUN
                detail = "丑月得令有帮看似强，但水木气势不弱制身有力——权衡之下原局中和，用神只能随运而取（木金运身弱取火土，火土运取财官为喜）"
            }
            jinJuNanHua && (m == EarthlyBranch.CHOU) -> {
                special = TuSpecial.SUI_YUN
                detail = "巳酉丑相合但中神酉被卯冲、天干金不透——合而难化待时而用。原局中和偏强，随运取舍（官杀重运取火土，火土运取金水）"
            }
            abs(r) <= 0.25 -> {
                special = TuSpecial.SUI_YUN
                detail = "权衡之下原局中和，用神只能随运而取（木金运身弱取火土，火土运取财官为喜）"
            }
            m == EarthlyBranch.YIN || m == EarthlyBranch.MAO -> if (xuTu) special = TuSpecial.XU_TU
        }

        val basis = "${rule}。日主${strength.chinese}，${detail}。" +
            (if (special != TuSpecial.NONE) "【${special.chinese}】" else "")

        return TuQuYongResult(
            monthRule = rule,
            strength = strength,
            yongShen = yong,
            fuYong = fu,
            detail = detail,
            tiaoHou = tiaoHou,
            special = special,
            basis = basis,
        )
    }
}
