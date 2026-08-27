package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.GodCategory
import cn.xulun.bazi.model.MuQuYongResult
import cn.xulun.bazi.model.MuSpecial
import cn.xulun.bazi.model.MuTiaoHou

/**
 * 木之逐月取用（甲乙日主）。理论见 `.shengke-reference.md` 第四四节（春/夏/秋/冬木取用）。
 *
 * 逐月决策树 + 特殊检测：
 * - **用神被合两失其用**（春三例）：印透（壬）食伤透（丁）而丁壬合——通关与制杀之神俱废；
 * - **随运而取**：中和局；
 * - **待岁运**：通关之神有根而力不足（春五例）；
 * - **自调**：寒月火成势（火局/透干+众支/拱午）不需再调候（春二/秋三例）；
 * - **寒极无火**：冬月原局无火，岁运忌虚浮之火犯旺（冬三例）；
 * - **水藏非本气**：印仅藏于非本气（申辰藏壬）则通关不力，取坐禄比劫（秋一例）。
 *
 * 强弱档位复用 [StrengthEngine]（八步法）。
 */
object MuQuYongCalculator {

    /** 木命专用：印=水、比劫=木、食伤=火、财=土、官杀=金。 */
    private val YIN = Element.WATER
    private val BI = Element.WOOD
    private val SHI = Element.FIRE
    private val CAI = Element.EARTH
    private val GUAN = Element.METAL

    private fun cat(el: Element): GodCategory = when (el) {
        Element.WOOD -> GodCategory.BI
        Element.WATER -> GodCategory.YIN
        Element.FIRE -> GodCategory.SHI
        Element.EARTH -> GodCategory.CAI
        Element.METAL -> GodCategory.GUAN
    }

    fun analyze(bazi: BaziResult): MuQuYongResult {
        val dayEl = bazi.dayPillar.stem.element
        require(dayEl == Element.WOOD) { "木之取用仅限甲乙日主" }
        val f = BingYaoCalculator.forces(bazi)
        val fc = { el: Element -> f[el]!! }
        val engine = StrengthEngine.analyze(bazi)
        val strength = engine.verdict
        val m = bazi.monthPillar.branch
        val tou = { el: Element -> bazi.stems.any { it.element == el && it != bazi.dayPillar.stem } }
        val zhiCount = { el: Element -> bazi.branches.count { it.element == el } }
        val zangGen = { el: Element -> bazi.branches.any { br -> cn.xulun.bazi.model.branchHiddenStems[br]!!.any { it.element == el } } }
        val sanHe = DiZhiRelationCalculator.analyze(bazi).sanHeList
        val gong = HeHuaCalculator.analyze(bazi).branchGongList
        val fireChengShi = sanHe.any { it.resultElement == Element.FIRE } ||
            (tou(Element.FIRE) && zhiCount(Element.FIRE) >= 2) ||
            gong.any { it.resultElement == Element.FIRE }
        val waterChengShi = sanHe.any { it.resultElement == Element.WATER } ||
            (tou(Element.WATER) && zhiCount(Element.WATER) >= 2)
        val hasFire = tou(Element.FIRE) || zhiCount(Element.FIRE) > 0 || zangGen(Element.FIRE)
        val yinTou = tou(YIN)
        val yinBenQi = zhiCount(YIN) > 0 // 印之本气支（亥子），非本气（申辰藏壬）不算

        // 特殊检测一：丁壬合两失其用（印食俱透而合）
        val dingTou = bazi.stems.any { it == cn.xulun.bazi.model.HeavenlyStem.DING }
        val renTou = bazi.stems.any { it == cn.xulun.bazi.model.HeavenlyStem.REN }
        val heZhi = dingTou && renTou

        // 特殊检测二：寒极无火（冬月）
        val winter = m == EarthlyBranch.HAI || m == EarthlyBranch.ZI || m == EarthlyBranch.CHOU

        var yong: GodCategory
        var fu: GodCategory? = null
        var detail: String
        var tiaoHou: MuTiaoHou
        val rule: String

        when (m) {
            EarthlyBranch.YIN -> {
                rule = "寅月犹有余寒，调候首取丙火（无丙用丁），火宜透干有根、贵在适中"
                if (fireChengShi) {
                    tiaoHou = MuTiaoHou.BU_XU
                    yong = cat(YIN)
                    detail = "火局成势驱尽寒气，不可再用火调候；日主被众火所泄当令不强（寅被火泄），取水抑火生身（水火既济）"
                } else {
                    tiaoHou = MuTiaoHou.XU_HUO
                    detail = when {
                        bazi.stems.any { it == cn.xulun.bazi.model.HeavenlyStem.BING } -> "丙火透干，泄秀调候一石二鸟"
                        dingTou -> "无丙用丁，泄秀调候"
                        else -> "丙丁皆无，调候有缺陷"
                    }
                    yong = when {
                        strength == DayStrength.STRONG && fc(Element.WATER) >= 3 -> cat(CAI).also { fu = cat(SHI) } // 身强水众，土制水，火土并用
                        strength == DayStrength.STRONG && fc(BI) >= 4 -> cat(SHI).also { fu = cat(GUAN) } // 比劫众，火土金兼用
                        strength == DayStrength.WEAK && fc(GUAN) >= 3 -> cat(YIN).also { fu = cat(SHI) } // 金众，水通关兼火
                        strength == DayStrength.WEAK && fc(CAI) >= 5 -> cat(BI) // 土众，比劫
                        strength == DayStrength.WEAK && fc(SHI) >= 4 -> cat(YIN) // 火众，水制火生身
                        strength == DayStrength.STRONG -> cat(SHI)
                        else -> cat(YIN)
                    }
                }
            }
            EarthlyBranch.MAO -> {
                rule = "卯月阳气上升，一般不需调候；质坚根固气专"
                tiaoHou = MuTiaoHou.BU_XU
                yong = when {
                    strength != DayStrength.WEAK && fc(SHI) > 0 -> cat(SHI) // 泄秀（火不宜多）
                    strength != DayStrength.WEAK && fc(GUAN) > 0 -> cat(GUAN)
                    strength != DayStrength.WEAK && fc(CAI) > 0 -> cat(CAI)
                    fc(Element.WATER) >= 3 -> cat(CAI) // 水多宜土，不可用金
                    else -> cat(YIN)
                }
                detail = "身强最宜火泄秀（火不宜过多）；无火用金；无火金用土；水多宜土不可金"
            }
            EarthlyBranch.CHEN -> {
                rule = "辰月土当令，木盘根于辰不失其旺"
                tiaoHou = MuTiaoHou.BU_JIN
                yong = when {
                    strength == DayStrength.STRONG -> cat(GUAN)
                    strength == DayStrength.WEAK && fc(CAI) >= 5 -> cat(BI) // 土重比劫疏土
                    strength == DayStrength.WEAK && fc(GUAN) >= 3 && yinTou && zangGen(YIN) -> cat(YIN) // 金强壬透有根水通关
                    strength == DayStrength.WEAK && fc(GUAN) >= 3 -> cat(SHI) // 水不透，火制金（不佳）
                    strength == DayStrength.WEAK && fc(SHI) >= 4 -> cat(YIN) // 火多水制
                    fc(Element.WATER) >= 4 -> cat(CAI) // 水多木漂土制水培木
                    else -> cat(BI)
                }
                detail = "身强首金；身弱土重首比劫疏土（无比劫用水）；金强水通关（无水火制金不佳）；火多水制；水多木漂土制水"
                if (strength == DayStrength.WEAK && fc(GUAN) >= 3 && !(yinTou && zangGen(YIN))) detail = "水深藏非本气，通关不力，火制金（效果不佳）"
            }
            EarthlyBranch.SI -> {
                rule = "巳月火旺而不烈，以水调候，水火既济而发荣"
                tiaoHou = MuTiaoHou.YONG_SHUI
                yong = when {
                    strength == DayStrength.WEAK -> cat(YIN)
                    strength == DayStrength.STRONG -> cat(SHI)
                    else -> cat(YIN)
                }
                detail = "喜有水制衡旺火，水火既济则生机勃发"
            }
            EarthlyBranch.WU, EarthlyBranch.WEI -> {
                rule = "午未月调候为要，最宜湿土蓄水晦火培木（一物多用），土忌太过"
                tiaoHou = MuTiaoHou.YONG_SHUI
                yong = when {
                    strength != DayStrength.WEAK -> cat(CAI).also { fu = cat(GUAN) } // 身强首湿土次金
                    fc(SHI) >= 3 -> cat(YIN) // 身弱火太过用水
                    fc(CAI) >= 5 -> cat(BI) // 身弱土太过用木
                    fc(GUAN) >= 3 -> cat(YIN) // 身弱金多用火? 教材：金多为病用水
                    else -> cat(YIN)
                }
                detail = if (strength == DayStrength.WEAK && fc(SHI) >= 3) {
                    val shuangTi = bazi.pillars.firstOrNull { it.stem.element == Element.WATER && it.branch.element == Element.WATER }
                    if (shuangTi != null) "以${shuangTi.fullName}双体之水为用（冲克旺火，木不至焚灭）"
                    else "湿土（辰丑）蓄水晦火培木一物多用；夏木即使强盛一般也不宜用火"
                } else "湿土（辰丑）蓄水晦火培木一物多用；夏木即使强盛一般也不宜用火"
            }
            EarthlyBranch.SHEN -> {
                rule = "初秋肃杀渐生而余炎未退，调候不可忽视"
                tiaoHou = MuTiaoHou.BU_JIN
                yong = when {
                    strength == DayStrength.STRONG -> cat(GUAN).also { fu = cat(CAI) }
                    strength == DayStrength.WEAK && yinBenQi -> cat(YIN)
                    else -> cat(BI) // 水深藏非本气，取比劫（坐禄帮身）
                }
                detail = if (strength == DayStrength.WEAK && !yinBenQi)
                    "用水通关则申辰之水深藏非本气，幸自坐禄地，取比劫帮身；湿土生金兼蓄水培木，调候不为紧要"
                else "身弱最宜水化杀润木；身强首金次土不宜火"
            }
            EarthlyBranch.YOU -> {
                rule = "酉月金气最专，凉而不寒，调候不为紧要"
                tiaoHou = MuTiaoHou.BU_JIN
                yong = when {
                    strength != DayStrength.WEAK -> cat(GUAN).also { fu = cat(CAI) } // 土金并用财官相生
                    yinBenQi -> cat(YIN) // 水化杀
                    else -> cat(BI) // 无水用木帮身
                }
                detail = "身强土金并用，财官相生富贵可求；身弱用水化杀不宜见土克水，无水用木帮身"
            }
            EarthlyBranch.XU -> {
                rule = "戌月金退寒进，需调候；拱午（寅戌）则寒气自除"
                if (fireChengShi) {
                    tiaoHou = MuTiaoHou.BU_XU
                    detail = "寅戌拱午，寒气自除，不需再用火调候"
                } else {
                    tiaoHou = MuTiaoHou.XU_HUO
                    detail = "身强首火暖身泄秀，无火用燥土；有水不宜金（金水增寒）"
                }
                yong = when {
                    strength != DayStrength.WEAK && !fireChengShi -> cat(SHI)
                    strength != DayStrength.WEAK -> cat(SHI)
                    fc(CAI) >= 5 -> cat(BI) // 土厚用木
                    fc(GUAN) >= 3 -> cat(YIN).also { fu = cat(SHI) } // 金重水通关火制金
                    else -> cat(BI)
                }
            }
            EarthlyBranch.HAI, EarthlyBranch.ZI -> {
                rule = if (m == EarthlyBranch.HAI) "亥月木长生得令，须有火方生发；最忌金生水增寒伐木"
                else "子月水旺至极，偏重火土并用，比劫稍次"
                tiaoHou = if (hasFire) MuTiaoHou.XU_HUO else MuTiaoHou.HAN_JI
                yong = when {
                    tiaoHou == MuTiaoHou.HAN_JI -> cat(BI) // 寒极无火，姑以比劫待运
                    strength != DayStrength.WEAK -> cat(SHI).also { fu = cat(CAI) } // 先火次土
                    fc(Element.WATER) >= 4 -> cat(SHI).also { fu = cat(CAI) } // 水太过，火土并用（木火土并用之要）
                    fc(CAI) >= 5 -> cat(BI) // 土过木疏
                    fc(SHI) >= 4 -> cat(YIN) // 火过水制（不过分）
                    else -> cat(YIN)
                }
                detail = if (tiaoHou == MuTiaoHou.HAN_JI)
                    "寒极无调候之火，岁运反忌见虚浮之火犯旺（原局无根之火引战）"
                else "寒木用火，调候泄秀一物两用；适量的火不泄木反暖木（反生之理）"
            }
            else -> { // 丑月
                rule = "丑月寒湿之土难培木根，仍需火调候；亥子会局成化则丑土以水论"
                tiaoHou = if (hasFire) MuTiaoHou.XU_HUO else MuTiaoHou.HAN_JI
                yong = when {
                    tiaoHou == MuTiaoHou.HAN_JI -> cat(BI)
                    strength != DayStrength.WEAK -> cat(SHI).also { fu = cat(CAI) }
                    else -> cat(SHI).also { fu = cat(CAI) }
                }
                detail = if (tiaoHou == MuTiaoHou.HAN_JI)
                    "命局寒极，原局无调候之火，岁运反忌见虚浮之火犯旺，恐成土水交战"
                else "寒木用火；水为忌而透癸喜火者以戊合癸（合去忌神）"
            }
        }

        // 特殊态归并
        var special = MuSpecial.NONE
        val r = if (engine.aSide + engine.bSide == 0.0) 0.0 else (engine.aSide - engine.bSide) / (engine.aSide + engine.bSide)
        val nearBalanced = strength == DayStrength.BALANCED || kotlin.math.abs(r) <= 0.25
        // 官杀有效力：计入合局变节之党（巳酉丑成员本气非金者）
        val guanEff = fc(GUAN) + sanHe.filter { it.resultElement == Element.METAL }.sumOf { 2 }
        // 会合相抵：我方三会（木）被克方之六合（金）牵制，势力相抵近中和（春四例）
        val liuHe = HeHuaCalculator.analyze(bazi).branchLiuHeList
        val sanHui = HeHuaCalculator.analyze(bazi).branchSanHuiList
        val huaHeDui = (sanHe.any { it.resultElement == Element.WOOD } || sanHui.any { it.resultElement == Element.WOOD }) &&
            liuHe.any { it.resultElement == Element.METAL }
        if (heZhi && guanEff >= 3) {
            special = MuSpecial.HE_ZHI
            detail = "身轻杀重，欲壬水通关或丁火制金，惜丁壬一合两失其用（化神受损难成化），虽透而无力"
        } else if (nearBalanced || huaHeDui) {
            special = MuSpecial.SUI_YUN
            if (detail.contains("双体之水为用")) detail += "；日主接近中和，随运轻重取用"
            else detail = "日主接近中和，原局诸行皆可为喜用，全局取用轻重视岁运而定，以平衡流通为准则"
        } else if (strength == DayStrength.WEAK && yong == cat(YIN) && yinTou && zangGen(YIN) && fc(GUAN) >= 3 && fc(YIN) <= 2) {
            special = MuSpecial.DAI_SUI_YUN
            detail = "金重为病，壬水有根可化杀生身，唯力不足，有待岁运弥补"
        }

        val basis = "${rule}。日主${strength.chinese}，${detail}。" +
            (if (special != MuSpecial.NONE) "【${special.chinese}】" else "")

        return MuQuYongResult(
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
