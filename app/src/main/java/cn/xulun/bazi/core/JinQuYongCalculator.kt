package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.GodCategory
import cn.xulun.bazi.model.JinQuYongResult
import cn.xulun.bazi.model.JinSpecial
import cn.xulun.bazi.model.JinTiaoHou
import kotlin.math.abs

/**
 * 金之逐月取用（庚辛日主）。理论见 `.shengke-reference.md` 第四七节（春/夏/秋/冬金取用）。
 *
 * 金命专用概念：
 * - **火土并见**（春金：火通木土之关，单土难用）；
 * - **湿土燥土为金命核心**（夏未土炎燥脆金"其印如杀"须水润；燥土得水则湿可生金；冬土燥良湿不佳）；
 * - **申巳相合克力有减 / 午未相合增燥**；
 * - **秋金首取丁火煅炼**（用神细至"午中丁火"）；用火不宜见水；印比过刚则折；
 * - **儿能救母**（水制火存金）；
 * - **用神层次**（病重药轻，退求喜神）；
 * - 金众土多失时不弱，反宜伤食财官。
 *
 * 强弱档位复用 [StrengthEngine]（八步法）。
 */
object JinQuYongCalculator {

    /** 金命：印=土、比劫=金、食伤=水、财=木、官杀=火。 */
    private val YIN = Element.EARTH
    private val BI = Element.METAL
    private val SHI = Element.WATER
    private val CAI = Element.WOOD
    private val GUAN = Element.FIRE

    private fun cat(el: Element): GodCategory = when (el) {
        Element.EARTH -> GodCategory.YIN
        Element.METAL -> GodCategory.BI
        Element.WATER -> GodCategory.SHI
        Element.WOOD -> GodCategory.CAI
        Element.FIRE -> GodCategory.GUAN
    }

    fun analyze(bazi: BaziResult): JinQuYongResult {
        val dayEl = bazi.dayPillar.stem.element
        require(dayEl == Element.METAL) { "金之取用仅限庚辛日主" }
        val dayStem = bazi.dayPillar.stem
        val f = BingYaoCalculator.forces(bazi)
        val fc = { el: Element -> f[el]!! }
        val engine = StrengthEngine.analyze(bazi)
        val strength = engine.verdict
        val m = bazi.monthPillar.branch
        val tou = { el: Element -> bazi.stems.any { it.element == el && it != dayStem } }
        val zhiCount = { el: Element -> bazi.branches.count { it.element == el } }
        val sanHe = DiZhiRelationCalculator.analyze(bazi).sanHeList
        val shiTu = bazi.branches.any { it == EarthlyBranch.CHOU || it == EarthlyBranch.CHEN } // 湿土
        val zaoTu = bazi.branches.any { it == EarthlyBranch.WEI || it == EarthlyBranch.XU }   // 燥土
        val shenSiHe = EarthlyBranch.SHEN in bazi.branches && EarthlyBranch.SI in bazi.branches
        val wuWeiHe = EarthlyBranch.WU in bazi.branches && EarthlyBranch.WEI in bazi.branches
        val huoJu = sanHe.any { it.resultElement == Element.FIRE } ||
            bazi.branches.count { it == EarthlyBranch.SI || it == EarthlyBranch.WU } >= 2

        var yong: GodCategory
        var fu: GodCategory? = null
        var detail: String
        var tiaoHou: JinTiaoHou
        val rule: String

        when (m) {
            EarthlyBranch.YIN -> {
                rule = "寅月金体衰而寒，需火暖身需土生助，火土并见方为理想（火通木土之关）"
                tiaoHou = JinTiaoHou.HUO_TU
                yong = when {
                    fc(CAI) >= 3 && zhiCount(Element.METAL) >= 1 -> cat(BI) // 财重印受制，任财首比劫
                    strength != DayStrength.WEAK && fc(BI) + fc(YIN) >= 5 ->
                        cat(CAI).also { fu = cat(SHI) } // 金众土多失时不弱，反宜伤食财官
                    fc(GUAN) >= 4 -> cat(YIN) // 杀重首印
                    fc(YIN) >= 5 -> cat(GUAN) // 印众木疏? 春土虚不至此，备用
                    else -> cat(BI).also { fu = cat(YIN) }
                }
                detail = "财强身弱印星受制难用，病在财重——任财者首取比劫（日主有根比劫可用）；身弱只可用微火暖身，火众为忌（木火两强受克过重）；有土无火土难为用"
                if (yong == cat(CAI)) detail = "土金皆众未戌藏火暖身——失时不弱当令不强（木嫩逢众土盘曲），首选财，食伤为喜，岁运食伤生财可望致富"
            }
            EarthlyBranch.MAO -> {
                rule = "卯月木更旺金更衰不需调候；卯土更虚生金愈难"
                tiaoHou = JinTiaoHou.BU_XU
                yong = when {
                    strength != DayStrength.WEAK && fc(BI) + fc(YIN) >= 5 -> cat(CAI).also { fu = cat(SHI) }
                    fc(CAI) >= 4 -> cat(BI) // 木众金少火反为病，水木火皆忌
                    else -> cat(BI)
                }
                detail = "身弱宜比劫，水木火皆为所忌；木过坚则金缺，无有力之金帮身凶咎难免；金木力差不大则随岁运而变"
            }
            EarthlyBranch.CHEN -> {
                rule = "辰月木退土得令而春土仍虚，只稍具生气；土众有埋金之嫌"
                tiaoHou = JinTiaoHou.BU_XU
                yong = when {
                    fc(YIN) >= 5 -> cat(CAI) // 土众埋金，木疏土（最忌见火）
                    strength != DayStrength.WEAK && fc(BI) + fc(YIN) >= 6 -> cat(CAI).also { fu = cat(SHI) } // 身印两强
                    strength != DayStrength.WEAK -> cat(CAI).also { fu = cat(SHI) }
                    else -> cat(BI)
                }
                detail = "身印两强财星有气，取木为用水为喜；天干宜见官杀地支不宜（干无印化支有印化）；土众最忌见火（埋金火克交并）"
            }
            EarthlyBranch.SI, EarthlyBranch.WU, EarthlyBranch.WEI -> {
                rule = "夏金火炎土燥调候为要：火旺水制火存金（儿能救母），土燥水润土生金"
                tiaoHou = JinTiaoHou.SHUI_ZHI
                val weiYue = m == EarthlyBranch.WEI
                yong = when {
                    zhiCount(Element.WATER) >= 1 && zhiCount(Element.METAL) >= 1 ->
                        cat(SHI).also { fu = cat(BI) } // 亥中壬水制火存金（水有本气根可为用），庚金辅之
                    strength != DayStrength.WEAK && !weiYue && zhiCount(Element.WATER) == 0 && shiTu ->
                        cat(YIN) // 湿土得用（丑辰蓄水生金晦火一物多用）
                    shiTu && (tou(Element.WATER) || zhiCount(Element.WATER) >= 1) && fc(GUAN) >= 4 ->
                        cat(YIN) // 湿土化杀生身，火太旺壬水湿土并用
                    strength != DayStrength.WEAK && fc(SHI) >= 2 -> cat(SHI).also { fu = cat(BI) } // 水制火存金，庚金辅之
                    fc(GUAN) >= 4 && (tou(Element.WATER) || zhiCount(Element.WATER) >= 1) ->
                        cat(YIN).also { fu = cat(BI) } // 支会火局：湿土通关，金水为喜（用神可待岁运）
                    fc(GUAN) >= 4 -> cat(BI) // 无水湿土只好比劫
                    strength != DayStrength.WEAK -> cat(SHI).also { fu = cat(BI) }
                    else -> cat(YIN).also { fu = cat(BI) }
                }
                detail = when {
                    yong == cat(YIN) && fc(GUAN) >= 4 -> "支会火局克身之力仍大；日主通根月透比劫印生癸润——有救助而弱。用湿土通关，金水为喜神；癸无根喜天干连续相生，通根岁运地支则为用"
                    wuWeiHe -> "午未相合增土之燥，未土脆金难生金（其印如杀）——命局炎燥日主弱，岁运喜金水"
                    yong == cat(YIN) && shiTu -> "湿土（丑辰）化杀生身、蓄水晦火，一物多用；燥土得水则湿方可生金"
                    yong == cat(SHI) -> "以水制火存金（儿能救母），水泄金气故以庚金辅之，金水并用强身敌杀"
                    else -> "未月土最炎燥，其印如杀，须有足够之水未土得润方可生金"
                }
                if (m == EarthlyBranch.WEI && shiTu) detail = "坐下润土生金不脆，辰土重叠晦未中之火以润局，戊己土能生日主——金强（毫厘千里之差）"
                if (weiYue && !shiTu && zhiCount(Element.WATER) == 0) detail = "未月土最炎燥脆金难生金，其印如杀；须有足够之水，未土得润方可生金"
            }
            EarthlyBranch.SHEN, EarthlyBranch.YOU -> {
                rule = "申酉月金当权得令刚锐至极，最宜丁火煅炼成钟鼎之材；印比过刚则折最忌"
                tiaoHou = JinTiaoHou.DING_DUAN
                yong = when {
                    fc(YIN) >= 5 -> cat(CAI) // 病在土众，木疏土
                    strength != DayStrength.WEAK && fc(SHI) >= 3 && fc(YIN) >= 3 -> cat(SHI) // 土金水流通，水泄秀
                    strength != DayStrength.WEAK && fc(GUAN) >= 2 -> cat(GUAN) // 丁火煅炼（午中丁火）
                    strength != DayStrength.WEAK && fc(SHI) >= 2 -> cat(SHI)
                    strength != DayStrength.WEAK -> cat(GUAN)
                    fc(CAI) >= 4 -> cat(BI) // 身弱按病药：财重首比劫
                    else -> cat(YIN) // 杀重首印
                }
                detail = when {
                    yong == cat(SHI) -> "土金水势流通，最宜以水泄秀英华发露；用火有土化水回克逆其势——见火不宜；土过众埋金克水亦非佳"
                    yong == cat(GUAN) -> "身强非火不可制，丙过猛故用午中所藏丁火；略嫌无木官杀无源（有木则化水生火用神无虞）；用火不宜见水"
                    yong == cat(CAI) -> "病在土众首取木疏土；火制金（丙墓月建不强+戊化）难作用，水泄秀被众土越金克水亦不美——用神有层次，东方木运财利如意"
                    else -> "身弱按病药原则：财重首用比劫，杀重首用印绶"
                }
            }
            EarthlyBranch.XU -> {
                rule = "戌月金退而不衰（戌为金之印）；金畏厚土埋没无光"
                tiaoHou = JinTiaoHou.BU_XU
                yong = when {
                    fc(YIN) >= 5 -> cat(CAI) // 土众宜木疏松
                    strength != DayStrength.WEAK -> cat(GUAN).also { fu = cat(CAI) }
                    else -> cat(BI).also { fu = cat(YIN) }
                }
                detail = "戌为带火之土而金之印，退而不衰；土众宜木疏松；土金皆众为身印两强（特殊格局类）"
            }
            else -> { // 亥子丑
                rule = "冬金体寒质弱，宜火暖身；身弱火土并用杀印相生，燥土为良湿土不佳"
                tiaoHou = JinTiaoHou.HUO_NUAN
                yong = when {
                    strength != DayStrength.WEAK -> cat(SHI) // 身强以水泄秀
                    fc(Element.WATER) >= 4 && zaoTu -> cat(YIN).also { fu = cat(GUAN) } // 水众需未戌制之，然后火才能作用
                    else -> cat(YIN).also { fu = cat(GUAN) } // 火土并用
                }
                detail = when {
                    yong == cat(SHI) -> "身强取亥水泄秀为用；印比过刚则折"
                    else -> "火土并用生身暖局止水趋于中和；用土分燥湿——湿土不佳燥土为良；比劫帮身不力远不如印；最忌水木；乙透而天干不见土则不忌反生丁火为喜"
                }
            }
        }

        // 特殊态
        var special = JinSpecial.NONE
        val r = if (engine.aSide + engine.bSide == 0.0) 0.0 else (engine.aSide - engine.bSide) / (engine.aSide + engine.bSide)
        when {
            m == EarthlyBranch.MAO && fc(CAI) >= 3 && fc(CAI) <= fc(BI) + 2 -> {
                special = JinSpecial.SUI_YUN
                detail = "木当令得水生强旺，日主有本气根透劫不算弱，但无印滋生不得令——整体偏弱，取庚金帮身为用；金木力差不大，岁运介入行运用神会变"
            }
            wuWeiHe && (m == EarthlyBranch.WU || m == EarthlyBranch.WEI) -> special = JinSpecial.WU_WEI_HE_ZAO
            shenSiHe && m == EarthlyBranch.SI -> special = JinSpecial.SHEN_SI_HE_JIAN
            yong == cat(CAI) && m == EarthlyBranch.XU && fc(YIN) >= 5 -> special = JinSpecial.CENG_CI
        }

        val basis = "${rule}。日主${strength.chinese}，${detail}。" +
            (if (special != JinSpecial.NONE) "【${special.chinese}】" else "")

        return JinQuYongResult(
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
