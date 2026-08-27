package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.GodCategory
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.HuoQuYongResult
import cn.xulun.bazi.model.HuoSpecial
import cn.xulun.bazi.model.HuoTiaoHou
import cn.xulun.bazi.model.branchHiddenStems

/**
 * 火之逐月取用（丙丁日主）。理论见 `.shengke-reference.md` 第四五节（春/夏/秋/冬火取用）。
 *
 * 火命专用概念：
 * - **调候神分丙壬/丁癸**（丁见壬有化木生火之势，故丁用癸）；
 * - **丙辛合财反怯可用金**（夏一例：木火两强无辛则两行成象，透辛作合反可用）；
 * - **湿土（丑辰）降火、燥土（未戌）助炎**；燥土见水乃湿（有金水时权可用）；
 * - **申为水长生较酉美**（喜水时优先申金）；
 * - **合局不化仍为根**（冬二例：巳酉丑不化，巳仍为丁根）；
 * - **巳被申隔合乏力待时**（秋四例：一用终生木火）；
 * - **木多火窒·印重反忌**（秋五例：身弱无强根逢印重，生多为克）；
 * - **午未从强而化**（火众透丙丁化火、土众透戊己化土）。
 *
 * 强弱档位复用 [StrengthEngine]（八步法）。
 */
object HuoQuYongCalculator {

    /** 火命：印=木、比劫=火、食伤=土、财=金、官杀=水。 */
    private val YIN = Element.WOOD
    private val BI = Element.FIRE
    private val SHI = Element.EARTH
    private val CAI = Element.METAL
    private val GUAN = Element.WATER

    private fun cat(el: Element): GodCategory = when (el) {
        Element.WOOD -> GodCategory.YIN
        Element.FIRE -> GodCategory.BI
        Element.EARTH -> GodCategory.SHI
        Element.METAL -> GodCategory.CAI
        Element.WATER -> GodCategory.GUAN
    }

    fun analyze(bazi: BaziResult): HuoQuYongResult {
        val dayEl = bazi.dayPillar.stem.element
        require(dayEl == Element.FIRE) { "火之取用仅限丙丁日主" }
        val dayStem = bazi.dayPillar.stem
        val f = BingYaoCalculator.forces(bazi)
        val fc = { el: Element -> f[el]!! }
        val engine = StrengthEngine.analyze(bazi)
        val strength = engine.verdict
        val m = bazi.monthPillar.branch
        val tou = { el: Element -> bazi.stems.any { it.element == el && it != dayStem } }
        val zhiCount = { el: Element -> bazi.branches.count { it.element == el } }
        val zangGen = { el: Element -> bazi.branches.any { br -> branchHiddenStems[br]!!.any { it.element == el } } }
        val rel = DiZhiRelationCalculator.analyze(bazi)
        val heHua = HeHuaCalculator.analyze(bazi)
        val sanHe = rel.sanHeList
        val liuHe = heHua.branchLiuHeList
        val shiTu = bazi.branches.any { it == EarthlyBranch.CHOU || it == EarthlyBranch.CHEN } // 湿土
        val zaoTu = bazi.branches.any { it == EarthlyBranch.WEI || it == EarthlyBranch.XU }   // 燥土
        val shenPresent = EarthlyBranch.SHEN in bazi.branches
        val siShenHe = EarthlyBranch.SI in bazi.branches && shenPresent // 巳申合（隔合乏力）
        val wuWeiHuaHuo = EarthlyBranch.WU in bazi.branches && EarthlyBranch.WEI in bazi.branches &&
            (tou(Element.FIRE) || bazi.stems.any { it == HeavenlyStem.BING }) // 午未化火之势
        val bingXinHe = dayStem == HeavenlyStem.BING && tou(Element.METAL)
        val congGe = GeJuCalculator.detectCongGe(bazi)

        var yong: GodCategory
        var fu: GodCategory? = null
        var detail: String
        var tiaoHou: HuoTiaoHou
        val rule: String

        when (m) {
            EarthlyBranch.YIN, EarthlyBranch.MAO -> {
                rule = if (m == EarthlyBranch.YIN) "寅月火得木生而余寒犹存，木火相辅相成，木不宜太过"
                else "卯月火气渐升，取用同寅月而身强须虑调候"
                tiaoHou = HuoTiaoHou.BU_XU
                yong = when {
                    strength != DayStrength.WEAK && fc(BI) >= 4 -> cat(SHI).also { fu = cat(CAI) } // 身强比劫多，用土金
                    strength != DayStrength.WEAK && sanHe.any { it.resultElement == Element.FIRE } && fc(YIN) <= 3 ->
                        cat(GUAN) // 支合火局可用水（印旺未化忌水）
                    strength != DayStrength.WEAK && fc(YIN) >= 4 -> cat(SHI).also { fu = cat(CAI) } // 身印两强首土次金忌水
                    strength != DayStrength.WEAK -> cat(SHI) // 泄秀
                    fc(GUAN) >= 3 -> cat(YIN).also { fu = cat(BI) } // 身弱官杀众用印兼比劫
                    fc(SHI) >= 5 -> cat(YIN) // 土多晦火木疏土
                    fc(CAI) >= 3 -> cat(BI).also { fu = cat(YIN) } // 身弱财多用比劫兼印
                    else -> cat(YIN)
                }
                detail = "身强用土泄秀（己土有强根尤佳）；身弱土厚则湿土晦火，用木疏土生身，最忌金"
                if (yong == cat(SHI) && fc(SHI) >= 4 && shiTu) detail = "湿土（丑辰）晦火培根泄秀，火土辗转相生，上下通关则金水运亦无碍"
            }
            EarthlyBranch.CHEN -> {
                rule = "辰月土当令而木之余气犹存，火亦不衰"
                tiaoHou = HuoTiaoHou.BU_XU
                yong = when {
                    strength != DayStrength.WEAK && fc(YIN) >= 3 -> cat(SHI) // 身印两强不忌土反可用土
                    fc(SHI) >= 5 -> cat(YIN) // 身弱土厚，木疏（无木用火）最忌金
                    else -> cat(YIN)
                }
                detail = "身印两强不忌土反可用土；身弱土厚为晦火忌神，用木疏之（无木用火），最忌金"
                if (yong == cat(YIN) && fc(SHI) >= 5) detail = "双体湿土当权，蓄水晦火，土泄火气，日主偏弱，宜木疏土生身"
            }
            EarthlyBranch.SI, EarthlyBranch.WU, EarthlyBranch.WEI -> {
                rule = if (m == EarthlyBranch.WEI) "未月余焰不弱而未土最燥，调候不可少；午未从强而化"
                else "巳午月火烈，湿土泄秀、金水调候，忌木火"
                tiaoHou = HuoTiaoHou.XU_SHUI
                if (dayStem == HeavenlyStem.DING) tiaoHou = HuoTiaoHou.XU_SHUI
                yong = when {
                    congGe != null && !tou(Element.METAL) -> cat(BI) // 炎上之势顺而不可逆（财透则破其纯）
                    strength != DayStrength.WEAK && bingXinHe && fc(YIN) >= 3 -> cat(CAI) // 丙辛合财反怯可用金
                    fc(SHI) >= 4 && fc(YIN) <= 1 -> cat(BI).also { tiaoHou = HuoTiaoHou.BU_XU } // 土众晦火旺而不强，无木只得用火
                    strength != DayStrength.WEAK && fc(GUAN) >= 4 -> cat(YIN) // 身弱杀强水太过，木化杀
                    strength != DayStrength.WEAK && fc(CAI) >= 4 -> cat(BI) // 财多身弱火任财，金自调候
                    strength != DayStrength.WEAK -> cat(SHI).also { fu = cat(CAI) } // 湿土泄秀金水调候
                    fc(SHI) >= 5 -> cat(YIN) // 土众身弱木疏+调候水
                    else -> cat(YIN).also { fu = cat(BI) }
                }
                detail = when {
                    yong == cat(CAI) -> "丙辛作合而反怯，故可用金；命局炎热金脆，宜岁运湿土生金方为得宜"
                    yong == cat(BI) && fc(SHI) >= 5 -> "土众晦火，旺而不强，最宜木疏惜无木，只得用火帮身；水贴身金透则已不需调候"
                    yong == cat(SHI) -> (if (shiTu) "湿土（丑辰）泄秀降炎" else "无湿土，燥土权可用（燥土见水乃湿）") +
                        "，金水调候为辅；申为水长生较酉美"
                    else -> "湿土泄秀，金水调候；泄秀不可太过，重叠之土反晦火"
                }
                if (wuWeiHuaHuo) detail += "；午未相见化阴阳：火众透丙丁化火、土众透戊己化土，从强而化"
            }
            EarthlyBranch.SHEN -> {
                rule = "申月火气未退尽，扶抑调候兼顾"
                tiaoHou = HuoTiaoHou.JIAN_GU
                val shiShiBuRuo = strength != DayStrength.WEAK || (fc(BI) + fc(YIN) >= 5 && tou(Element.WOOD))
                yong = when {
                    shiShiBuRuo -> cat(CAI).also { fu = cat(GUAN) } // 失时不弱用庚金，申中壬水为喜
                    else -> cat(YIN).also { fu = cat(BI) } // 金水得令，首取寅木兼午火
                }
                detail = if (shiShiBuRuo)
                    "半合火局失时不弱，正用庚金之财；偏燥喜水，申中壬水为喜神（申为水长生）"
                else "金水得令强盛；寅午半合暗生午火可自立门户，首取寅木为用兼午火，金旺抑木为忌"
            }
            EarthlyBranch.YOU -> {
                rule = "酉月火势大衰，不宜金水叠现，反喜木火生助"
                tiaoHou = HuoTiaoHou.BU_XU
                yong = when {
                    strength != DayStrength.WEAK -> cat(SHI).also { fu = cat(CAI) }
                    strength == DayStrength.WEAK && !zangGen(Element.WOOD) && zhiCount(Element.FIRE) >= 1 ->
                        cat(BI) // 印虚浮无根喜神不力，午火无克破赖以立身，取火为用
                    else -> cat(YIN).also { fu = cat(BI) }
                }
                detail = "午火失令支会金局则日主衰弱，午火无水克破赖以立身，甲木虚浮则喜神不力，取火为用"
            }
            EarthlyBranch.XU -> {
                rule = "戌月火入墓光辉收敛，土众为病，忌金水，首木次火"
                tiaoHou = HuoTiaoHou.BU_XU
                yong = when {
                    fc(YIN) >= 5 -> cat(BI).also { fu = cat(CAI) } // 木多火窒，火金并用
                    strength != DayStrength.WEAK -> cat(YIN).also { fu = cat(BI) }
                    else -> cat(YIN).also { fu = cat(BI) }
                }
                detail = "墓月之火光辉收敛；土众为病忌金水，首木次火"
            }
            else -> { // 亥子丑
                rule = "冬水旺火死，用木通关化强扶弱；木亦赖火，相生相益"
                tiaoHou = HuoTiaoHou.DONG
                yong = when {
                    fc(GUAN) >= 5 && fc(YIN) <= 2 -> cat(SHI) // 水过强木弱不能通关，土止水（不得已）
                    else -> cat(YIN).also { fu = cat(BI) } // 木通关，火为喜
                }
                detail = "用木通关化杀生身；水过强木弱则用土止水（食伤制官杀，不得已）；忌金生水克木。" +
                    "幸有巳火之根则弱而有依（合局不化仍为根；若无根则入从杀）"
                if (sanHe.any { it.resultElement == Element.WATER }) detail = "支合水局成化，日主有克无生，身很弱，赖时支巳火方不入从杀"
            }
        }

        // 特殊态
        var special = HuoSpecial.NONE
        val r = if (engine.aSide + engine.bSide == 0.0) 0.0 else (engine.aSide - engine.bSide) / (engine.aSide + engine.bSide)
        when {
            siShenHe && m == EarthlyBranch.SHEN -> {
                special = HuoSpecial.GE_HE_DAI_SHI
                detail = "通巳本气之根，但巳被申隔合乏力，只可待时而用；纵行木火之运全局仍弱，一生取木火为喜用（一用终生）"
            }
            fc(YIN) >= 5 -> {
                special = HuoSpecial.MU_DU_HUO_ZHI
                detail = "四柱皆木克土太过，土当令而不强；木众火窒，根未被冲拔不能从格以身弱论。" +
                    "身弱无强根逢印重反为忌（生多为克）；唯火金并用方见去病之功"
            }
            bingXinHe && yong == cat(CAI) -> special = HuoSpecial.HE_CAI
            m == EarthlyBranch.CHOU && EarthlyBranch.SI in bazi.branches && sanHe.any { it.resultElement == Element.METAL } -> {
                special = HuoSpecial.BU_HUA_RENG_GEN
                detail = "土金两旺日主衰弱，幸巳酉丑合局不化，巳仍为丁火之根，弱而有依；时透乙木有根可取为用，火为喜神"
            }
            kotlin.math.abs(r) <= 0.25 && special == HuoSpecial.NONE -> {
                special = HuoSpecial.SUI_YUN
                if (!detail.contains("一用终生")) detail = "日主接近中和，原局诸行皆可为喜用，全局取用轻重视岁运而定"
            }
        }

        val basis = "${rule}。日主${strength.chinese}，${detail}。" +
            (if (special != HuoSpecial.NONE) "【${special.chinese}】" else "")

        return HuoQuYongResult(
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
