package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.GodCategory
import cn.xulun.bazi.model.ShuiQuYongResult
import cn.xulun.bazi.model.ShuiSpecial
import cn.xulun.bazi.model.ShuiTiaoHou
import kotlin.math.abs

/**
 * 水之逐月取用（壬癸日主）。理论见 `.shengke-reference.md` 第四八节（春/夏/秋/冬水取用）。
 *
 * 水命专用概念：
 * - **春水散漫无收**（土止其流，最忌金助水流）；
 * - **母慈灭子**两型（印重身轻——金多水浊；木弱不受水生——水多木漂）；
 * - **子辰不以半合论**（辰逢旺火失滋润之性）；
 * - **盖头熏蒸**（旺火不透干反被水盖头，两败俱伤）；
 * - **越金克水**（土太强可越过金直接克水）；
 * - **两气成象**（金水两强顺气势，忌木火）；
 * - **冬水无火为缺陷**（火金并用方生弱水；无火水止而冻成死水）；
 * - 两丑争合一子（合力减损）。
 *
 * 强弱档位复用 [StrengthEngine]（八步法）。
 */
object ShuiQuYongCalculator {

    /** 水命：印=金、比劫=水、食伤=木、财=火、官杀=土。 */
    private val YIN = Element.METAL
    private val BI = Element.WATER
    private val SHI = Element.WOOD
    private val CAI = Element.FIRE
    private val GUAN = Element.EARTH

    private fun cat(el: Element): GodCategory = when (el) {
        Element.METAL -> GodCategory.YIN
        Element.WATER -> GodCategory.BI
        Element.WOOD -> GodCategory.SHI
        Element.FIRE -> GodCategory.CAI
        Element.EARTH -> GodCategory.GUAN
    }

    fun analyze(bazi: BaziResult): ShuiQuYongResult {
        val dayEl = bazi.dayPillar.stem.element
        require(dayEl == Element.WATER) { "水之取用仅限壬癸日主" }
        val dayStem = bazi.dayPillar.stem
        val f = BingYaoCalculator.forces(bazi)
        val fc = { el: Element -> f[el]!! }
        val engine = StrengthEngine.analyze(bazi)
        var strength = engine.verdict
        val gaiTou0 = bazi.branches.count { it.element == Element.FIRE } >= 2 &&
            !bazi.stems.any { it.element == Element.FIRE && it != bazi.dayPillar.stem }
        // 盖头熏蒸：水火交战两败俱伤，日主不能控局——以身弱论（夏二例）
        if (gaiTou0 && strength == DayStrength.BALANCED) strength = DayStrength.WEAK
        val m = bazi.monthPillar.branch
        val tou = { el: Element -> bazi.stems.any { it.element == el && it != dayStem } }
        val zhiCount = { el: Element -> bazi.branches.count { it.element == el } }
        val sanHe = DiZhiRelationCalculator.analyze(bazi).sanHeList
        // 乙庚合化 + 巳酉（半）合 → 金水两气成象（秋二例；无乙庚则不入，秋三例）
        val heHua0 = HeHuaCalculator.analyze(bazi)
        val jinHe = sanHe.any { it.resultElement == Element.METAL } ||
            heHua0.branchBanHeList.any { it.resultElement == Element.METAL }
        val yiGengHe = bazi.stems.any { it == cn.xulun.bazi.model.HeavenlyStem.YI } &&
            bazi.stems.any { it == cn.xulun.bazi.model.HeavenlyStem.GENG }
        val liangQi = jinHe && yiGengHe
        // 辰逢旺火失润：子辰不以半合论（夏一例）
        val chenFengHuo = EarthlyBranch.CHEN in bazi.branches && zhiCount(Element.FIRE) >= 2
        // 盖头熏蒸：旺火不透干（夏二例）
        val gaiTou = zhiCount(Element.FIRE) >= 2 && !tou(Element.FIRE)
        // 越金克水：土太强（夏四例）
        val yueJin = fc(GUAN) >= 5
        // 冬水火态
        val hasHuo = tou(Element.FIRE) || zhiCount(Element.FIRE) >= 1

        var yong: GodCategory
        var fu: GodCategory? = null
        var detail: String
        var tiaoHou: ShuiTiaoHou
        val rule: String

        when (m) {
            EarthlyBranch.YIN, EarthlyBranch.MAO -> {
                rule = "春水散漫无收：水众用土止流，用木次之，最忌金（助水流泄土损木）"
                tiaoHou = if (m == EarthlyBranch.YIN) ShuiTiaoHou.JI_HUO else ShuiTiaoHou.ZHI_LIU
                yong = when {
                    fc(YIN) >= 2 && fc(BI) <= 3 && fc(YIN) + 1 >= fc(BI) -> cat(BI) // 印重身轻母慈灭子，只宜比劫（见少量金不忌）
                    fc(BI) >= 4 -> cat(GUAN) // 水众土止其流
                    fc(GUAN) >= 5 -> cat(YIN) // 杀重身轻，用印（丑中辛金）
                    fc(SHI) >= 4 -> cat(YIN) // 木重泄身，金制木生水
                    strength != DayStrength.WEAK -> cat(GUAN)
                    else -> cat(BI)
                }
                detail = "使水强不致泛滥、弱可周流；比劫不可过众（岁运逢之无土则泛滥）"
                if (yong == cat(BI)) detail = "身弱之因在根不固，用金则印重身轻母慈灭子——只宜比劫帮扶；逢岁运金众以火制金反可生身（财制印而反生之理）"
                if (yong == cat(YIN)) detail = "杀重身轻，用丑中辛金；原局无本气之木不足以取木疏土，岁运逢木不可过多（土得疏而身泄甚）"
            }
            EarthlyBranch.CHEN -> {
                rule = "辰月春水见库乃归宿之地，土多甲木疏之使水流通；土过众不宜金（埋金），土稍众正宜用金"
                tiaoHou = ShuiTiaoHou.ZHI_LIU
                yong = when {
                    fc(GUAN) >= 5 && zhiCount(Element.WOOD) >= 1 -> cat(SHI) // 土多甲木疏
                    fc(GUAN) >= 5 -> cat(YIN) // 无本气木，杀重用印（丑中辛金）
                    fc(GUAN) >= 4 -> cat(YIN) // 土稍众宜金
                    strength != DayStrength.WEAK -> cat(GUAN)
                    else -> cat(BI)
                }
                detail = "春水见库不比寅卯散漫；土太多甲木疏之；土过众不宜金（土众埋金）土稍众正宜用金"
                if (yong == cat(YIN) && fc(GUAN) >= 5) detail = "杀重身轻用丑中辛金；原局无本气之木不足以取木疏土救水，岁运逢木不可过多"
            }
            EarthlyBranch.SI, EarthlyBranch.WU, EarthlyBranch.WEI -> {
                rule = "夏水枯涸：制火之烈、化土之杀、使水周流勿使枯竭"
                tiaoHou = ShuiTiaoHou.JIN_SHUI
                val weiYue = m == EarthlyBranch.WEI
                yong = when {
                    zhiCount(Element.METAL) == 0 && !tou(Element.METAL) && zhiCount(Element.WATER) >= 1 ->
                        cat(BI) // 无金可用，赖日支比劫敌旺火（夏一）
                    fc(GUAN) >= 5 && (zhiCount(Element.METAL) >= 1 || tou(Element.METAL)) ->
                        cat(YIN).also { fu = cat(BI) } // 土强越金克水，金通关辅水（夏四：终生金水）
                    fc(SHI) >= 4 -> cat(YIN).also { fu = cat(BI) } // 木众金喜水
                    strength != DayStrength.WEAK -> cat(GUAN).also { fu = cat(SHI) } // 身旺土为堤岸木泄秀
                    fc(GUAN) >= 4 -> cat(YIN).also { fu = cat(BI) } // 土燥众金通关辅水，不宜木疏
                    else -> cat(BI).also { fu = cat(YIN) } // 身弱火强首比劫辅金
                }
                detail = when {
                    chenFengHuo -> "火强身弱水有枯竭之忧，无金可用，只有赖日支相依为命以敌旺火；辰见旺火失滋润之性——子辰不以半合水局论（岁运逢申金水得源头方论申子辰）"
                    gaiTou -> "火旺得木生炎烈，水虽透惜无强根终属身弱；旺火不透干反被水盖头成熏蒸之势，水火交战两败俱伤（多易患病，内战日主不能控局）"
                    yong == cat(YIN) && yueJin -> "未月遭重克，虽有根有金生有独立能力，但土力太强可越金克水仍属身弱——终生取金水为喜用"
                    else -> "身弱火强首比劫辅金；不宜木疏土（夏木易焚生火有余疏土不力且泄水）；季夏土最燥水弱须金水并用，缺一不美"
                }
            }
            EarthlyBranch.SHEN, EarthlyBranch.YOU -> {
                rule = "秋水通源母旺子相；身强印轻用土喜火，两气成象火不可用，金白水清忌土混（甲木制土）"
                tiaoHou = ShuiTiaoHou.HUO_XU
                yong = when {
                    liangQi -> cat(BI).also { fu = cat(YIN) } // 两强顺气势用金水
                    fc(BI) >= 3 && zhiCount(Element.FIRE) >= 2 && !liangQi ->
                        cat(CAI).also { fu = cat(SHI) } // 水旺足以制火，正格身强用财（午火），喜木（秋三）
                    fc(YIN) >= 4 && fc(BI) <= 3 -> cat(BI) // 金多水浊，须强根比劫
                    strength != DayStrength.WEAK -> cat(GUAN) // 身强印轻用土止水之堤
                    fc(YIN) >= 4 -> cat(BI)
                    else -> cat(YIN).also { fu = cat(BI) }
                }
                detail = when {
                    liangQi -> "巳酉乙庚皆合化金，金水两强只可顺其气势，用金水忌木火；原局木火皆被合化无碍格局，岁运不宜重逢（木太弱不受水生，金当旺必克弱木）"
                    yong == cat(CAI) -> "水旺足以制火，火之余力不大；金当令透干金水皆旺但不能成两强之势——正格身强用财论，取午火为用（巳合酉不可用），喜木"
                    yong == cat(GUAN) -> "身强印轻，无土制则奔流无收——时柱双体之土正可为止水之堤，取之为用"
                    else -> "印重身轻为金多水浊——正格须有强根或干透比劫众方可论强；身弱针对病取用，用神不力辅以喜神"
                }
            }
            EarthlyBranch.XU -> {
                rule = "戌月见申酉金实土虚水得印生；见寅午透丙丁则火破金局土则克水"
                tiaoHou = ShuiTiaoHou.HUO_XU
                yong = when {
                    zhiCount(Element.FIRE) >= 2 && tou(Element.FIRE) -> cat(BI).also { fu = cat(YIN) }
                    strength != DayStrength.WEAK -> cat(GUAN).also { fu = cat(SHI) }
                    else -> cat(YIN).also { fu = cat(BI) }
                }
                detail = "贵在看命局组合，强弱旺衰绝非死法；日主强弱随岁运变化（见酉会金局身强、见午合火局身弱、逢木多身也弱、水多身变强）"
            }
            else -> { // 亥子丑
                rule = "冬水最寒，过寒则冻不能生木润土涤金——调候最紧要，无火为缺陷"
                tiaoHou = ShuiTiaoHou.DONG_HAN
                yong = when {
                    !hasHuo -> cat(CAI).also { fu = cat(YIN) } // 无火为缺陷，火金并用
                    strength != DayStrength.WEAK && (fc(GUAN) >= 3 || tou(Element.EARTH) || fc(BI) >= 4) ->
                        cat(CAI).also { fu = cat(GUAN) } // 身强火土并用（水太旺/官透堤岸）
                    strength != DayStrength.WEAK -> cat(CAI).also { fu = cat(SHI) } // 身强用火调候，木得暖泄秀
                    else -> cat(CAI).also { fu = cat(YIN) } // 身弱需火，火金并用方生弱水
                }
                detail = when {
                    fc(BI) >= 4 && EarthlyBranch.WU in bazi.branches ->
                        "金寒水冻；午被两子夹冲调候不力，丁透又被壬合，幸未为燥土救应午火——水太旺须火土并用，岁运火土同行可望富贵"
                    yong == cat(CAI) && fc(GUAN) >= 1 && tou(Element.EARTH) ->
                        "得禄印比双透身强；午火调候又得寅午半合喜用有力，己土透干正好身强用官病有药治；但原局火不透干暖局乏力——需岁运引动方得真用"
                    else -> "冬水有火则金可暖，金暖则可生水——火金并用方能生起弱水；身强用木亦喜火，用土需火助（无火则水止而冻成死水）"
                }
            }
        }

        // 特殊态
        var special = ShuiSpecial.NONE
        val r = if (engine.aSide + engine.bSide == 0.0) 0.0 else (engine.aSide - engine.bSide) / (engine.aSide + engine.bSide)
        when {
            liangQi -> special = ShuiSpecial.LIANG_QI
            yong == cat(BI) && fc(YIN) >= 4 && (m == EarthlyBranch.YIN || m == EarthlyBranch.MAO || m == EarthlyBranch.SHEN || m == EarthlyBranch.YOU) ->
                special = ShuiSpecial.MU_CI
            m == EarthlyBranch.XU -> {
                special = ShuiSpecial.SUI_YUN
                detail = "日主强弱随岁运变化（见酉会金局克木生水→身强；见午合火局克金→身弱；逢木多身也弱；水多身变强）"
            }
            (m == EarthlyBranch.ZI || m == EarthlyBranch.CHOU) && bazi.branches.count { it == EarthlyBranch.CHOU } >= 2 -> {
                special = ShuiSpecial.SUI_YUN
                detail = "当令得禄结党身偏旺——但两丑争合一子、未土克害子水，子水忙于抗土助身之力减弱；用神不但随大运变，还随流年介入而变"
            }
            !gaiTou0 && abs(r) <= 0.25 -> {
                special = ShuiSpecial.SUI_YUN
                detail = "原局中和，用神宜随运而取"
            }
        }

        val basis = "${rule}。日主${strength.chinese}，${detail}。" +
            (if (special != ShuiSpecial.NONE) "【${special.chinese}】" else "")

        return ShuiQuYongResult(
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
