package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DaYunInfo
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.JinBiaoDaiMing
import cn.xulun.bazi.model.JinBiaoHealth
import cn.xulun.bazi.model.JinBiaoLiuNianPing
import cn.xulun.bazi.model.JinBiaoQuYongWay
import cn.xulun.bazi.model.JinBiaoQingZhuo
import cn.xulun.bazi.model.JinBiaoResult
import cn.xulun.bazi.model.JinBiaoRuoJiType
import cn.xulun.bazi.model.JinBiaoStance
import cn.xulun.bazi.model.JinBiaoStanceItem
import cn.xulun.bazi.model.JinBiaoVerdict
import cn.xulun.bazi.model.JinBiaoYongShenHealth
import cn.xulun.bazi.model.JinBiaoYunPing
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.PillarBranchLabels
import cn.xulun.bazi.model.PillarStemLabels
import cn.xulun.bazi.model.TenGod
import cn.xulun.bazi.model.branchHiddenStems
import cn.xulun.bazi.model.tenGodOfStem

/**
 * 盲派金镖门分析引擎。讲义存档见 `.jinbiao-reference.md`（第一～三讲已落地）。
 *
 * 口径总纲：
 * - 第一讲·用神总纲：用神＝对日主最需要最有帮助的一种五行之神（十神代五行）；
 *   帮扶用神（生用/同用）则吉，耗泄克破用神（用所克=耗/用所生=泄/克用=克/冲刑破害=破）则凶。
 *   立场一律**以用神为中心**，不以日主为中心。
 * - 第二讲·用神的作用：判盘标准＝用神得力健全则好、无气受伤则不好；禁泛断（「化官生印=当官」式断语
 *   不问取用只有五分之一命中率）——断语一律锚定用神。
 * - 第三讲·弱极从势：弱极日元官旺随官、财旺随财、财官食并存用财解围；从格行比劫印地必凶。
 *
 * 基线参数（弱极阈值、党力计分）待师传档事实案例校准——凡与事实冲突以事实为准打补丁。
 * 带有事实的案例须同时走老引擎（传统综合引擎）分析对照（见 ShiChuanAnLiTest / JinBiaoAnLiTest）。
 */
object JinBiaoCalculator {

    // ── 基线参数（待案例校准） ──
    private const val RUO_JI_SHARE = 0.28       // 生助方合力占比低于此值 → 弱极倾向
    private const val WEI_FU_MAX_SCORE = 2.5    // 印比党力低于此值 → 微浮（自顾不暇）
    private const val FOLLOW_MIN_SCORE = 2.0    // 从势党最低党力（财/官/食须有力方可随/解围）
    private const val QIANG_JI_YIN = 4.0        // 印党力高于此值且财官食全无 → 强极（印比叠叠同心）
    // 案一百一十八：旺强极「印轻／印重（枭夺食）」不再以印党力线（2.0）分，而以**印得局**分（地支印本气≥3 或三合印局支全）——
    // 「因为印多，印得局了，走泄他的也是不行的；去掉两午火还没事」：两印支＝一二印枭＝印轻，走食伤运没事（见 yinDeJuShi）
    private const val CHEN_XU_HOSTILE = 8.0     // 案四：土日主辰根作虚之敌势门槛（东方木地土虚、水库随水）
    private const val GUAN_WANG = 4.0           // 第四讲：官党力≥此值＝官旺（身强须食制，身弱必印化）
    private const val YIN_HUAI = 4.0            // 第四讲：身强局印党力≥此值＝印起坏作用（须财坏印）
    // 案三补丁：得局＝印旺于比（印为最强方）即可化煞生印；比不弱于印则未得局、走官必大凶

    /** 党力计分：透干 2/字 + 支本气 1.5 + 藏干 0.5 + 月令本气再 3（日主天干自身不计） */
    private const val W_TOU_GAN = 2.0
    private const val W_BEN_QI = 1.5
    private const val W_CANG_GAN = 0.5
    private const val W_YUE_LING = 3.0

    // ── 关系表 ──
    /** 三会局（寅卯辰东方木／巳午未南方火／申酉戌西方金／亥子丑北方水） */
    private val SAN_HUI_JU = listOf(
        setOf(EarthlyBranch.YIN, EarthlyBranch.MAO, EarthlyBranch.CHEN) to Element.WOOD,
        setOf(EarthlyBranch.SI, EarthlyBranch.WU, EarthlyBranch.WEI) to Element.FIRE,
        setOf(EarthlyBranch.SHEN, EarthlyBranch.YOU, EarthlyBranch.XU) to Element.METAL,
        setOf(EarthlyBranch.HAI, EarthlyBranch.ZI, EarthlyBranch.CHOU) to Element.WATER,
    )
    private val LIU_CHONG = mapOf(
        EarthlyBranch.ZI to EarthlyBranch.WU, EarthlyBranch.WU to EarthlyBranch.ZI,
        EarthlyBranch.CHOU to EarthlyBranch.WEI, EarthlyBranch.WEI to EarthlyBranch.CHOU,
        EarthlyBranch.YIN to EarthlyBranch.SHEN, EarthlyBranch.SHEN to EarthlyBranch.YIN,
        EarthlyBranch.MAO to EarthlyBranch.YOU, EarthlyBranch.YOU to EarthlyBranch.MAO,
        EarthlyBranch.CHEN to EarthlyBranch.XU, EarthlyBranch.XU to EarthlyBranch.CHEN,
        EarthlyBranch.SI to EarthlyBranch.HAI, EarthlyBranch.HAI to EarthlyBranch.SI,
    )
    private val GAN_HE = mapOf(
        HeavenlyStem.JIA to HeavenlyStem.JI, HeavenlyStem.JI to HeavenlyStem.JIA,
        HeavenlyStem.YI to HeavenlyStem.GENG, HeavenlyStem.GENG to HeavenlyStem.YI,
        HeavenlyStem.BING to HeavenlyStem.XIN, HeavenlyStem.XIN to HeavenlyStem.BING,
        HeavenlyStem.DING to HeavenlyStem.REN, HeavenlyStem.REN to HeavenlyStem.DING,
        HeavenlyStem.WU to HeavenlyStem.GUI, HeavenlyStem.GUI to HeavenlyStem.WU,
    )
    private val LIU_HE = mapOf(
        EarthlyBranch.ZI to EarthlyBranch.CHOU, EarthlyBranch.CHOU to EarthlyBranch.ZI,
        EarthlyBranch.YIN to EarthlyBranch.HAI, EarthlyBranch.HAI to EarthlyBranch.YIN,
        EarthlyBranch.MAO to EarthlyBranch.XU, EarthlyBranch.XU to EarthlyBranch.MAO,
        EarthlyBranch.CHEN to EarthlyBranch.YOU, EarthlyBranch.YOU to EarthlyBranch.CHEN,
        EarthlyBranch.SI to EarthlyBranch.SHEN, EarthlyBranch.SHEN to EarthlyBranch.SI,
        EarthlyBranch.WU to EarthlyBranch.WEI, EarthlyBranch.WEI to EarthlyBranch.WU,
    )
    private val LIU_HE_EL = mapOf(
        "子丑" to Element.EARTH, "寅亥" to Element.WOOD, "卯戌" to Element.FIRE,
        "辰酉" to Element.METAL, "巳申" to Element.WATER, "午未" to Element.EARTH,
    )

    /** 三合局（生、旺、墓序——中位为旺神／中神）：寅午戌火／亥卯未木／申子辰水／巳酉丑金。 */
    private val SAN_HE_XU = listOf(
        listOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU) to Element.FIRE,
        listOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI) to Element.WOOD,
        listOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN) to Element.WATER,
        listOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU) to Element.METAL,
    )

    /** 六害（穿）：子未／丑午／寅巳／卯辰／申亥／酉戌。 */
    private val LIU_HAI = mapOf(
        EarthlyBranch.ZI to EarthlyBranch.WEI, EarthlyBranch.WEI to EarthlyBranch.ZI,
        EarthlyBranch.CHOU to EarthlyBranch.WU, EarthlyBranch.WU to EarthlyBranch.CHOU,
        EarthlyBranch.YIN to EarthlyBranch.SI, EarthlyBranch.SI to EarthlyBranch.YIN,
        EarthlyBranch.MAO to EarthlyBranch.CHEN, EarthlyBranch.CHEN to EarthlyBranch.MAO,
        EarthlyBranch.SHEN to EarthlyBranch.HAI, EarthlyBranch.HAI to EarthlyBranch.SHEN,
        EarthlyBranch.YOU to EarthlyBranch.XU, EarthlyBranch.XU to EarthlyBranch.YOU,
    )

    /** 六合所化之五行（子丑土/寅亥木/卯戌火/辰酉金/巳申水/午未土）；非六合对返回 null。 */
    private fun liuHeHuaEl(b1: EarthlyBranch, b2: EarthlyBranch): Element? {
        val key = listOf(b1.ordinal, b2.ordinal).sorted().joinToString(",")
        val canonical = mapOf(
            "0,1" to "子丑", "2,11" to "寅亥", "3,10" to "卯戌", "4,9" to "辰酉",
            "5,8" to "巳申", "6,7" to "午未",
        )[key] ?: return null
        return LIU_HE_EL[canonical]
    }

    /** 干之禄支与长生支（案五十四：丙火见根于寅——长生；比劫见根即变强）。 */
    private val LU_ZHANG: Map<HeavenlyStem, Set<EarthlyBranch>> = mapOf(
        HeavenlyStem.JIA to setOf(EarthlyBranch.YIN, EarthlyBranch.HAI),
        HeavenlyStem.YI to setOf(EarthlyBranch.MAO, EarthlyBranch.WU),
        HeavenlyStem.BING to setOf(EarthlyBranch.SI, EarthlyBranch.YIN),
        HeavenlyStem.DING to setOf(EarthlyBranch.WU, EarthlyBranch.YOU),
        HeavenlyStem.WU to setOf(EarthlyBranch.SI, EarthlyBranch.YIN),
        HeavenlyStem.JI to setOf(EarthlyBranch.WU, EarthlyBranch.YOU),
        HeavenlyStem.GENG to setOf(EarthlyBranch.SHEN, EarthlyBranch.SI),
        HeavenlyStem.XIN to setOf(EarthlyBranch.YOU, EarthlyBranch.ZI),
        HeavenlyStem.REN to setOf(EarthlyBranch.HAI, EarthlyBranch.SHEN),
        HeavenlyStem.GUI to setOf(EarthlyBranch.ZI, EarthlyBranch.MAO),
    )

    /** 五行 [x] 相对用神 [yong] 的立场与动作（第一讲吉凶总纲）。 */
    fun stanceAction(yong: Element, x: Element): Pair<JinBiaoStance, String> = when {
        x == yong -> JinBiaoStance.BANG to "同用神"
        x.generates == yong -> JinBiaoStance.BANG to "生用神"
        yong.generates == x -> JinBiaoStance.SUN to "泄用神"
        yong.controls == x -> JinBiaoStance.SUN to "耗用神"
        else -> JinBiaoStance.SUN to "克用神"
    }

    /** 元素党力（不计日主天干自身）。 */
    fun partyScore(bazi: BaziResult, el: Element): Double {
        var s = 0.0
        bazi.stems.forEachIndexed { i, st -> if (i != 2 && st.element == el) s += W_TOU_GAN }
        bazi.branches.forEach { br ->
            if (br.element == el) s += W_BEN_QI
            else if (branchHiddenStems.getValue(br).any { it.element == el }) s += W_CANG_GAN
        }
        if (bazi.monthPillar.branch.element == el) s += W_YUE_LING
        return s
    }

    /** 本气根：四支中是否有本气为 [el] 之支。 */
    private fun hasBenQiRoot(bazi: BaziResult, el: Element): Boolean =
        bazi.branches.any { it.element == el }

    /** 五行对应天干对（如水→壬癸）。 */
    private val elementStems: Map<Element, List<HeavenlyStem>> = Element.entries.associateWith { el ->
        HeavenlyStem.entries.filter { it.element == el }
    }

    fun analyze(bazi: BaziResult, daYun: DaYunInfo?, gender: cn.xulun.bazi.model.Gender? = null): JinBiaoResult {
        val dayMaster = bazi.dayPillar.stem
        val dayEl = dayMaster.element
        val yinEl = Element.entries.first { it.generates == dayEl }
        val biEl = dayEl
        val shiEl = dayEl.generates
        val caiEl = dayEl.controls
        val guanEl = Element.entries.first { it.controls == dayEl }

        // ── 十神代五行表（第一讲） ──
        val daiMing = listOf(
            JinBiaoDaiMing("同我", biEl, "比劫"),
            JinBiaoDaiMing("我生", shiEl, "食伤"),
            JinBiaoDaiMing("我克", caiEl, "财星"),
            JinBiaoDaiMing("生我", yinEl, "印绶"),
            JinBiaoDaiMing("克我", guanEl, "官杀"),
        )

        // ── 日主强弱（案十定则·金镖门三步法）：①看季节——月令党属经三会/三合/六合叛向调整，
        //    归印比→身旺基调，归财官食→身弱基调；②地支作用已入调整；③据以取用。
        //    八步法得分仅作参考（其扶抑口径与金镖门月令党属口径有别——案十两法分歧即教学点）。
        val strengthResult = StrengthEngine.analyze(bazi)
        val aShare = strengthResult.aSide / (strengthResult.aSide + strengthResult.bSide)
        val monthBranch = bazi.monthPillar.branch
        val monthElAdj: Element = run {
            SAN_HUI_JU.firstOrNull { (bs, _) -> bs.all { it in bazi.branches } }
                ?.takeIf { monthBranch in it.first }?.let { return@run it.second }
            // 案一百零二·三合成局而月支本气为印且透月干＋日主坐禄：透干者立——月令不随局叛向（身旺无疑）
            // （对勘案廿七：月令印透然日主坐印支非坐禄——仍弱）
            if (listOf(
                    setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN),
                    setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI),
                    setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU),
                    setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU),
                ).any { it.all { b -> b in bazi.branches } } &&
                monthBranch.element == Element.entries.first { it.generates == dayEl } &&
                bazi.monthPillar.stem.element == monthBranch.element &&
                bazi.dayPillar.branch.element == dayEl
            ) {
                return@run monthBranch.element
            }
            listOf(
                setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN) to Element.WATER,
                setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI) to Element.WOOD,
                setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU) to Element.FIRE,
                setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU) to Element.METAL,
            ).firstOrNull { (trio, _) -> trio.all { it in bazi.branches } }
                ?.takeIf { monthBranch in it.first }?.let { return@run it.second }
            val partner = LIU_HE[monthBranch]
            if (partner != null && partner in bazi.branches &&
                bazi.branches.indexOf(partner) in listOf(0, 2) // 案廿戒：六合调整须与月支相邻（年支/日支），隔位不合
            ) {
                val key = listOf(monthBranch.ordinal, partner.ordinal).sorted().joinToString(",")
                val canonical = mapOf(
                    "0,1" to "子丑", "2,11" to "寅亥", "3,10" to "卯戌", "4,9" to "辰酉",
                    "5,8" to "巳申", "6,7" to "午未",
                )[key]
                LIU_HE_EL[canonical] ?: monthBranch.element
            } else {
                monthBranch.element
            }
        }
        var strength = if (monthElAdj == biEl || monthElAdj == yinEl) DayStrength.STRONG else DayStrength.WEAK
        // 案一百零四·藤萝系甲（天干章落地）：乙木无根而甲透（任何位置——年/时皆可）——藤萝系甲
        // 直接自旺顶个羊刃（可春可秋）；然庚（克甲之神）卡甲乙中间则差多了（单说——位置条件）。
        // 「可别藤萝系乙」——必须阳木甲，乙比肩不算。
        val tengLuo = dayMaster == HeavenlyStem.YI && !hasBenQiRoot(bazi, biEl) &&
            bazi.stems.anyIndexed { i, s -> i != 2 && s == HeavenlyStem.JIA }
        val jiaIdx104 = bazi.stems.indices.firstOrNull { it != 2 && bazi.stems[it] == HeavenlyStem.JIA }
        val tengLuoTie = tengLuo && jiaIdx104 != null && kotlin.math.abs(jiaIdx104!! - 2) == 1 &&
            !(jiaIdx104!! < 2 && bazi.stems.getOrNull(jiaIdx104 + 1)?.element == Element.METAL) &&
            !(jiaIdx104!! > 2 && bazi.stems.getOrNull(jiaIdx104 - 1)?.element == Element.METAL)
        if (tengLuo) strength = DayStrength.STRONG

        // ── 弱极判定（第三讲两型） ──
        val yinS = partyScore(bazi, yinEl)
        val biS = partyScore(bazi, biEl)
        val supportS = yinS + biS
        val supportRooted = hasBenQiRoot(bazi, yinEl) || hasBenQiRoot(bazi, biEl)
        val caiS = partyScore(bazi, caiEl)
        val guanS = partyScore(bazi, guanEl)
        val shiS = partyScore(bazi, shiEl)
        // 案一百零七·印之本气根被财本气支贴冲（子午冲之属）——支神只以冲为重，财坏印拔根
        val yinGenBaCai = bazi.branches.anyIndexed { i, br ->
            br.element == yinEl &&
                ((i + 1 in bazi.branches.indices && bazi.branches[i + 1].element == caiEl) ||
                    (i - 1 in bazi.branches.indices && bazi.branches[i - 1].element == caiEl))
        }
        val attackerStrong = caiS >= FOLLOW_MIN_SCORE || guanS >= FOLLOW_MIN_SCORE

        // 案卅三·退而求其次标志（须在弱极判定前置）：不是克就是泄（杀旺财微食泄）、无印、
        // 日主仅藏根小通根而无本气根——身无力太衰，退而求其次食制，不算弱极；印运反吉（补印化杀帮身）
        // 案四十七裁：印两用局（食伤≥杀8成——制杀太过兼过泄，一定要走印运）非退而求其次之劫
        val tuiErQici = guanS >= 8.0 && shiS >= FOLLOW_MIN_SCORE && shiS * 1.25 < guanS && yinS < FOLLOW_MIN_SCORE && caiS < FOLLOW_MIN_SCORE &&
            !hasBenQiRoot(bazi, biEl) &&
            bazi.branches.count { br -> branchHiddenStems.getValue(br).any { it.element == biEl } } >= 2

        // 师传补丁（案二）：地支三会成局——局中之支随局走（辰随春木走，等于没有），
        // 局内各支的一切印比之力（本气/藏干）作废，帮扶归零。
        val sanHuiJu: Pair<Set<EarthlyBranch>, Element>? =
            SAN_HUI_JU.firstOrNull { (bs, _) -> bs.all { it in bazi.branches } }
        val juBranches = sanHuiJu?.first ?: emptySet()
        val sanHuiBetrayNote = if (sanHuiJu != null) {
            val juEl = sanHuiJu.second
            "地支${juBranches.joinToString("") { it.chinese }}三会${juEl.chinese}局——局中之支随局走（等于没有）"
        } else {
            null
        }
        val supportSAdj = run {
            var s = 0.0
            // 案四补丁：六合化敌之支（寅亥化木等）——成合两支之印比藏干随化走
            val liuHeHuaBranches = mutableSetOf<EarthlyBranch>()
            val liuHeHuaNote = mutableListOf<String>()
            bazi.branches.forEach { b1 ->
                val partner = LIU_HE[b1]
                if (partner != null && partner in bazi.branches) {
                    val key = listOf(b1.ordinal, partner.ordinal).sorted().joinToString(",")
                    val canonical = mapOf(
                        "0,1" to "子丑", "2,11" to "寅亥", "3,10" to "卯戌", "4,9" to "辰酉",
                        "5,8" to "巳申", "6,7" to "午未",
                    )[key]
                    val huaEl = canonical?.let { LIU_HE_EL[it] }
                    if (huaEl != null && (huaEl == caiEl || huaEl == guanEl || huaEl == shiEl)) {
                        liuHeHuaBranches += b1
                        if (liuHeHuaNote.none { it.startsWith(canonical) }) {
                            liuHeHuaNote += "${canonical}合化${huaEl.chinese}——成合之支随化走"
                        }
                    }
                }
            }
            // 案四补丁：土日主之辰根为东方虚土（辰是东方，东方属木，土要虚；又是水库随水）——
            // 敌势滔天时辰根作废（换作戌土则是实实在在的强根，不作此论；坐午火明帮扶亦不作此论）
            val hostileTotal = caiS + guanS + shiS
            val chenXuVoid = dayEl == Element.EARTH && aShare < RUO_JI_SHARE && hostileTotal >= CHEN_XU_HOSTILE
            bazi.stems.forEachIndexed { i, st -> if (i != 2 && (st.element == yinEl || st.element == biEl)) s += W_TOU_GAN }
            bazi.branches.forEach { br ->
                if (br !in juBranches && br !in liuHeHuaBranches && !(chenXuVoid && br == EarthlyBranch.CHEN)) {
                    if (br.element == yinEl || br.element == biEl) s += W_BEN_QI
                    else if (branchHiddenStems.getValue(br).any { it.element == yinEl || it.element == biEl }) s += W_CANG_GAN
                }
            }
            if (chenXuVoid && bazi.branches.contains(EarthlyBranch.CHEN)) {
                liuHeHuaNote += "辰为东方虚土（水库随水）——敌势滔天（${format(hostileTotal)}），辰根作废"
            }
            Triple(s, liuHeHuaBranches, liuHeHuaNote)
        }
        val sanHuiSanLiuHeNote = (sanHuiBetrayNote?.let { listOf(it) } ?: emptyList()) + supportSAdj.third

        val ruoJiType: JinBiaoRuoJiType?
        val ruoJiBasis: String?
        if (strength == DayStrength.WEAK && supportSAdj.first == 0.0) {
            ruoJiType = JinBiaoRuoJiType.GU_LI
            ruoJiBasis = buildString {
                if (sanHuiJu != null || sanHuiSanLiuHeNote.isNotEmpty()) {
                    append("日元孤立无气：")
                    append(sanHuiSanLiuHeNote.joinToString("；"))
                    append("；局内印比之力作废，帮扶党力为 0——一点帮扶都没有")
                    append("；印根随势叛——财坏印反成全从势，越是耗泄克身命主越好")
                } else {
                    append("日元孤立无气：四柱印比全无（党力0），一点根都没有，全是克耗泄")
                }
            }
        } else if (strength == DayStrength.WEAK && !supportRooted &&
            supportSAdj.first < WEI_FU_MAX_SCORE && aShare < RUO_JI_SHARE && attackerStrong &&
            // 案三十：藏根≥3＝有根气不弱极；案卅二/卅三：退而求其次食制不弱极
            bazi.branches.count { br -> branchHiddenStems.getValue(br).any { it.element == biEl } } < 3 &&
            !tuiErQici
        ) {
            ruoJiType = JinBiaoRuoJiType.WEI_FU
            // 师注（案一）：藏根寄于财官食本气之支者「跟着走」——借根无用（戌中丁火随秋金走）
            val hostBranches = bazi.branches.filter { br ->
                br.element != yinEl && br.element != biEl &&
                    branchHiddenStems.getValue(br).any { it.element == yinEl || it.element == biEl }
            }
            val catOf = { el: Element ->
                when (el) {
                    caiEl -> "财"
                    guanEl -> "官杀"
                    shiEl -> "食伤"
                    else -> el.chinese
                }
            }
            ruoJiBasis = buildString {
                append("微浮印比·自顾不暇：印比党力仅 ${format(supportSAdj.first)}（无本气根），生助占比 ${pct(aShare)}")
                if (caiS >= FOLLOW_MIN_SCORE) append("；印被财克（财党力 ${format(caiS)}）")
                if (guanS >= FOLLOW_MIN_SCORE) append("；比劫被官杀克（官党力 ${format(guanS)}）")
                if (hostBranches.isNotEmpty()) {
                    append("；印比仅藏于${hostBranches.joinToString("、") { "${it.chinese}（${catOf(it.element)}之支）" }}——寄于克耗泄之支，跟着走，借根无用")
                }
                if (sanHuiSanLiuHeNote.isNotEmpty()) {
                    append("；${sanHuiSanLiuHeNote.joinToString("；")}")
                }
                append("——想靠印比帮身，靠不住")
            }
        } else {
            ruoJiType = null
            ruoJiBasis = null
        }
        val ruoJi = ruoJiType != null

        // ── 旺极/强极判定（第三讲续 + 案三补丁） ──
        // 师注（案三）：印比本气之支所藏财官食「跟着走」——借根无用（戌中辛、巳中庚等于没有），
        // 敌党分折减后再论财官食是否全无。
        fun hostileScoreAdj(el: Element): Double {
            var s = 0.0
            bazi.stems.forEachIndexed { i, st -> if (i != 2 && st.element == el) s += W_TOU_GAN }
            // 案九辩证：透干通根者不废——壬透则申中壬为根（秋水通源）；不透之藏敌（案三戌中辛巳中庚）方随旺神走
            val touGan = bazi.stems.any { it.element == el }
            bazi.branches.forEach { br ->
                val hostYinBi = br.element == yinEl || br.element == biEl
                if (!hostYinBi || touGan) {
                    if (br.element == el) s += W_BEN_QI
                    else if (branchHiddenStems.getValue(br).any { it.element == el }) s += W_CANG_GAN
                }
            }
            val mb = bazi.monthPillar.branch
            if (mb.element == el && (mb.element != yinEl && mb.element != biEl || touGan)) s += W_YUE_LING
            return s
        }
        val caiSAdj = hostileScoreAdj(caiEl)
        val guanSAdj = hostileScoreAdj(guanEl)
        val shiSAdj = hostileScoreAdj(shiEl)
        // 命一补丁：天干全印比＋敌党折减全零——权在一人，径判旺强极（日主无根不受生之假生不扰此局）
        val stemsYinBiOnly = bazi.stems.all { it.element == biEl || it.element == yinEl }
        // 无根修正（档五十一）：印月身旺基调，然日主无比劫本气根而敌党不弱——仍以身弱论（杀重身轻用印）；
        // 案四十八豁免：坐下即印且印得局压杀（yinS＞guanS——印6.5对杀4.5）——印生身有依，身旺不翻；
        // （案廿七对照：坐下子印而印4.0＜杀6.5——杀压倒印，仍翻弱）
        if (strength == DayStrength.STRONG && !tengLuo && !hasBenQiRoot(bazi, biEl) &&
            !(bazi.dayPillar.branch.element == yinEl && yinS > guanS)
        ) {
            val diSAdjT = caiSAdj + guanSAdj + shiSAdj
            if (diSAdjT > 0.0 && diSAdjT >= (yinS + biS) * 0.8) strength = DayStrength.WEAK
        }
        // 案九十四/九十五·比劫当令而食伤过泄：丑月土寒终究是土（比未戌虚了一点——然终是当令之土，
        // 异于辰之土虚——两说之辨；丑土亦虚于未戌），土当令本已有力量；然金多——日主肯定就过泄了：
        // 翻身弱，取用必帮身（用神露出来者——丙/丁火生身即取之）。
        // 判据（案九十五放宽）：月令本气比劫当令（非经三合/三会/六合叛向）＋食伤透干≥2＋食伤党力≥官旺线，
        // 且①食伤地支得局（本气支≥2或三合全）并压过印比合力（案九十四——两酉得局），
        // 或②丑月寒土（虚于未戌——金多即过泄，一酉半合亦翻：案九十五）。
        // （对勘案六十五：戌月寅午戌三合透一戊——透一非两，身旺不翻。）
        val shiTouCnt94 = bazi.stems.count { it != dayMaster && it.element == shiEl }
        val shiDeJu94 = bazi.branches.count { it.element == shiEl } >= 2 ||
            listOf(
                setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN),
                setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI),
                setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU),
                setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU),
            ).any { it.all { b -> b in bazi.branches } }
        // 案一百零八戒：日主得局（本气根≥2——四库全之属）则食伤不强，不翻弱——
        // 「戊土得局了肯定金弱，没金不行只能用这个金：秀气流行」
        // （案九十四/九十五日主皆仅月令一根本气；本案四支皆本气根）
        val guoXieFanRuo = strength == DayStrength.STRONG &&
            bazi.branches.count { it.element == biEl } <= 1 &&
            monthBranch.element == biEl && monthElAdj == biEl &&
            shiTouCnt94 >= 2 && shiS >= GUAN_WANG &&
            (shiDeJu94 && shiS >= yinS + biS || monthBranch == EarthlyBranch.CHOU)
        if (guoXieFanRuo) strength = DayStrength.WEAK
        // 案一百一十·坐两刃＋飞刃（比劫）透＋财干贴印干相合（丙火虽猛逢辛乃怯——财无根从印而化）
        // ——身旺泄秀之结构：一刃已够旺，何况两刃又露飞刃
        val renZhi110 = YANG_REN[dayMaster]
        val liangRen110 = renZhi110 != null && bazi.dayPillar.branch == renZhi110 &&
            bazi.branches.count { it == renZhi110 } >= 2 &&
            bazi.stems.any { it != dayMaster && it.element == biEl } &&
            bazi.stems.indices.any { i ->
                i != 2 && bazi.stems[i].element == caiEl &&
                    bazi.stems.indices.any { j ->
                        j != 2 && j != i && bazi.stems[j].element == yinEl && kotlin.math.abs(i - j) == 1 &&
                            GAN_HE[bazi.stems[i]] == bazi.stems[j]
                    }
            }
        // 案一百零八·土众身旺、食伤吐秀（秀气流行）的结构判据：日主得局＋食伤透干＋财透护卫
        // ＋官杀不透且无本气根＋印不旺（非案九印为病之局）
        val xiuQi108 = strength == DayStrength.STRONG &&
            bazi.stems.anyIndexed { i, s -> i != 2 && s.element == shiEl } &&
            bazi.stems.anyIndexed { i, s -> i != 2 && s.element == caiEl } &&
            bazi.stems.none { s -> s != dayMaster && s.element == guanEl } && !hasBenQiRoot(bazi, guanEl) &&
            yinS < YIN_HUAI && bazi.branches.count { it.element == biEl } >= 3
        // 案一百零九·土重金埋（结构）：印得局（≥8）、食伤透而被印干贴克（泄秀被克回）、财不透仅藏支
        // ——印过旺用财破印；藏财之支尽被贴冲且冲神藏干克财（藏金暗相坎伐）＝财根气被砍没
        val shiIdx109 = bazi.stems.indices.filter { it != 2 && bazi.stems[it].element == shiEl }
        val yinIdx109 = bazi.stems.indices.filter { it != 2 && bazi.stems[it].element == yinEl }
        val maJin109 = yinS >= 8.0 && shiIdx109.isNotEmpty() &&
            shiIdx109.any { si -> yinIdx109.any { yi -> kotlin.math.abs(si - yi) == 1 } } &&
            bazi.stems.none { it != dayMaster && it.element == caiEl } &&
            bazi.branches.any { br -> branchHiddenStems.getValue(br).any { it.element == caiEl } }
        val caiGen109 = bazi.branches.filter { br -> branchHiddenStems.getValue(br).any { it.element == caiEl } }
        val caiGenBa109 = caiGen109.isNotEmpty() && caiGen109.all { g ->
            val gi = bazi.branches.indexOf(g)
            listOf(gi - 1, gi + 1).any { k ->
                k in bazi.branches.indices && LIU_CHONG[g] == bazi.branches[k] &&
                    branchHiddenStems.getValue(bazi.branches[k]).any { it.element.controls == caiEl }
            }
        }
        val wangJi = (strength == DayStrength.STRONG || stemsYinBiOnly) &&
            guanSAdj == 0.0 && caiSAdj == 0.0 && shiSAdj == 0.0
        val caiZai = hasBenQiRoot(bazi, caiEl) || bazi.branches.any { br ->
            br.element != biEl && br.element != yinEl &&
                branchHiddenStems.getValue(br).any { it.element == caiEl }
        }
        // 强极：命局印比叠叠（印亦成势），印比二人同心，可顺而不可逆
        val qiangJi = wangJi && yinS >= QIANG_JI_YIN
        // 印星得局（案三修订）：印旺于比（印为最强方）即可化煞生印；比不弱于印则未得局、走官必大凶
        val yinDeJu = qiangJi && yinS > biS
        // 案一百一十八·印得局（地支成局——枭夺食之判据）：走泄他的（食伤运）也不行，来了金就是枭神夺食——一定出灾
        val yinDeJuDuo = wangJi && yinDeJuShi(bazi, yinEl)
        val wangJiBasis = when {
            qiangJi -> buildString {
                append("命局印比叠叠（印党力${format(yinS)}、比劫党力${format(biS)}），绝无财官食伤——命局只存在印和比劫")
                if (caiS + guanS + shiS > 0.0) {
                    append("；印比之支所藏财官食跟着走——借根无用（折减后财${format(caiSAdj)}/官${format(guanSAdj)}/食${format(shiSAdj)}皆零）")
                }
                append("；印比二人同心，可顺而不可逆——强旺到极端（与旺极略有区别、大同小异）")
            }
            wangJi -> buildString {
                append("四柱全为比劫，没有财官食伤，一点克耗泄都没有——八字旺到极端")
                if (yinS > 0) append("；即或${format(yinS)}党力印枭，必然随其旺神")
                append("——好坏取决于大运")
            }
            else -> null
        }

        // ── 取用 ──
        // 案廿二：比劫克财之病（财被劫）——财滋弱杀局之病在比劫，食伤反为通关药神（泄比生财）
        // 案六十二·身旺无依判据：印三会得局＋食伤本气根随局化＋财不载无根＋官不透而根被化（官本气支入印局
        // 或与局支六合化印——亥被寅化木：官被泄没了，全化成木）——克耗泄全废
        val shenWangWuYi = SAN_HUI_JU.firstOrNull { (bs, _) -> bs.all { it in bazi.branches } }?.let { (bs, el) ->
            el == yinEl && bazi.branches.any { it.element == shiEl && it in bs } &&
                !caiZai && !hasBenQiRoot(bazi, caiEl) &&
                bazi.stems.none { it != dayMaster && it.element == guanEl } &&
                (bazi.branches.none { it.element == guanEl } ||
                    bazi.branches.filter { it.element == guanEl }.all { gb ->
                        gb in bs || (LIU_HE[gb]?.let { p -> p in bazi.branches && liuHeHuaEl(gb, p) == yinEl } == true)
                    })
        } == true
        val caiZhuBing = biS >= caiS || bazi.branches.any { br ->
            br.element == biEl && LIU_CHONG[br] != null && LIU_CHONG.getValue(br).let { p -> p.element == caiEl && p in bazi.branches }
        }
        // 案一百一十二·用官局喜用两伤（官干坐印被泄、官根被冲克、财根被冲拔——只余一处余气）
        val liangShang112 = xiYongLiangShang112(bazi, yinEl, caiEl, guanEl)

        // ── 昆仑得局（命二/命三）：地支一五行得局（本气≥3支，或三合成局）＋配生源＋日主无自根而漂——不以日主论，顺其势 ──
        val branchCount = Element.entries.associateWith { el -> bazi.branches.count { it.element == el } }
        val sanHeJuEl = listOf(
            setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN) to Element.WATER,
            setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI) to Element.WOOD,
            setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU) to Element.FIRE,
            setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU) to Element.METAL,
        ).firstOrNull { (trio, _) -> trio.all { it in bazi.branches } }?.second
        val kunLunEl = listOfNotNull(
            branchCount.filterKeys { it != biEl }.filterValues { it >= 3 }.maxByOrNull { it.value }?.key,
            sanHeJuEl?.takeIf { it != biEl },
            // 案九十七·一片金水：水日主而地支全是金水本气支（申酉亥子）且配有金生源（金透或金支）——
            // 全局一片金水＝得局，昆仑之水（「有源头可顺不可逆——不要见着金生水就叫昆仑：
            // 全局一片金水才算；若年月是土、仅日时金水——不是」）
            Element.WATER.takeIf {
                biEl == Element.WATER &&
                    bazi.branches.all { b -> b.element == Element.METAL || b.element == Element.WATER } &&
                    bazi.branches.any { b -> b.element == Element.METAL }
            },
        ).firstOrNull()
        val kunLunSource = kunLunEl?.let { el -> Element.entries.first { g -> g.generates == el } }
        // 案四十六：官干坐支藏官之余气（壬坐辰藏癸——杀还有点气）——制杀太过之辨在焉
        val guanZuoYouQi = bazi.stems.filterIndexed { i, s -> i != 2 && s.element == guanEl }.any { gs ->
            val idx = bazi.stems.indexOf(gs)
            branchHiddenStems.getValue(bazi.branches[idx]).any { it.element == guanEl }
        }
        val kunLun = kunLunEl != null && kunLunSource != null &&
            (bazi.stems.any { it.element == kunLunSource } ||
                bazi.branches.any { br -> branchHiddenStems.getValue(br).any { it.element == kunLunSource } }) &&
            // 案五十一裁：食伤局（kunLunEl=食伤）而日支为印本气且日支克食伤局（印夺食——日主有印可依不从）——走普通取用，不归昆仑
            // 护栏：日支在得局之三合局中者随局走不作克论（命二：辰随申子辰水局走——辰不作克水论，仍可从）
            !(kunLunEl == shiEl && bazi.dayPillar.branch.element == yinEl &&
                bazi.dayPillar.branch.element.controls == kunLunEl &&
                listOf(
                    setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN),
                    setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI),
                    setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU),
                    setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU),
                ).none { trio -> bazi.dayPillar.branch in trio && trio.all { it in bazi.branches } }) &&
            // 案廿八裁：局克日主不为顺——火局克庚是杀攻身非顺势；印有气不随杀走（正常八格杀重用印）
            kunLunEl.controls != dayEl &&
            // 案卅三裁：局为日主食伤而杀旺（≥官旺线）——生克框架优先，不顺食弃杀；
            // 案四十六裁：局为日主食伤而官干坐支藏余气（杀还有点气）——制杀太过之辨，不顺食弃势
            // （案十六丙坐子官无气败地、案十八庚坐午官无气——皆无此辨，归昆仑顺势）
            !(kunLunEl == shiEl && (guanS >= GUAN_WANG || (guanS >= FOLLOW_MIN_SCORE && guanZuoYouQi))) &&
            // 案廿七裁：得局之五行恰为日主之印、坐下印本气、官杀旺克身——杀重用印优先于昆仑
            // （辰贴申必生金、申子辰会局被夺——拱局辩证；印在坐下化旺杀即有情）
            !(kunLunEl == yinEl && bazi.dayPillar.branch.element == yinEl && guanS >= GUAN_WANG) &&
            // 案五十七裁：金局得局而日主有两印贴身相生＋有禄根——**肯定不是从格，是正常的命**：
            // 印比有力可依（身有靠），势虽成不作昆仑——正常分析（身弱用印，财得局坏印）
            // 案九十七豁免：昆仑局即日主本行（一片金水——金为生源生我即顺势）——印透不破势
            !(kunLunEl != yinEl && kunLunEl != biEl &&
                (bazi.monthPillar.stem.element == yinEl || bazi.hourPillar.stem.element == yinEl) &&
                hasBenQiRoot(bazi, biEl)) &&
            // 案五十八裁：食伤得局（木旺）而印星透干坐本气禄根（庚坐申——有力可依）——
            // 克泄交加身太衰，肯定要用印星帮身：用神庚申，正常分析不归昆仑（印好才是真的好）
            !(kunLunEl == shiEl && bazi.stems.anyIndexed { i, s ->
                i != 2 && s.element == yinEl && bazi.branches[i].element == yinEl
            }) &&
            // 案八十六裁：日主通根身库（限四库辰戌丑未中藏日主五行——库根之依；长生藏如亥中甲不算）
            // ——非无根而漂：不作昆仑（地支食伤得局泄体——走身弱泄体线；师「通根身库也是在秋天——伤官生财得局了」）
            !(kunLunEl != null && bazi.branches.any { br ->
                br in listOf(EarthlyBranch.CHEN, EarthlyBranch.XU, EarthlyBranch.CHOU, EarthlyBranch.WEI) &&
                    branchHiddenStems.getValue(br).any { it.element == dayEl }
            }) &&
            // 案六十六裁：印局（kunLunEl=印）而比劫透干——身有比劫之助可依（非无根而漂），
            // 不作昆仑：正常身旺用财（四土得局双冲互战——土金旺，日主身旺足以用财）
            !(kunLunEl == yinEl && bazi.stems.anyIndexed { i, s -> i != 2 && s.element == biEl })

        // ── 气象调候（案十五~案二十一）：寒甚要暖有气，暖至要寒有根 ──
        // 寒面（冬水/水局）调候神＝丙火：有气（火藏根（寅中丙/戌中丁亦是根），或透木紧贴生火且木有地载）→用之暖局；
        // 无气（隔位之生、地支不载、无藏根）→调候运反大凶（一事无成/离婚克子），反以不调候为美（顺其寒）。
        // 燥面（夏火/火局）调候神＝壬水：有根（辰丑申中壬癸藏根亦是）→可调候（走金水当官富贵）；
        // 无根（全火土）→走水运犯旺（一败如灰/死）。纯局（地支全水/全火——权在一人）不可调只可顺。
        val hanJu = bazi.monthPillar.branch.let {
            it == EarthlyBranch.HAI || it == EarthlyBranch.ZI ||
                (it == EarthlyBranch.CHOU && bazi.branches.any { b -> b.element == Element.WATER })
        }
        val zaoJu = bazi.monthPillar.branch == EarthlyBranch.SI || bazi.monthPillar.branch == EarthlyBranch.WU
        val huoCangGen = bazi.branches.any { br -> branchHiddenStems.getValue(br).any { it.element == Element.FIRE } }
        val shuiCangGen = bazi.branches.any { br -> branchHiddenStems.getValue(br).any { it.element == Element.WATER } }
        val muZai = bazi.branches.any { br -> br.element == Element.WOOD || branchHiddenStems.getValue(br).any { it.element == Element.WOOD } }
        val jinJuDuo = sanHeJuEl == Element.METAL || branchCount.getValue(Element.METAL) >= 3
        val muTieHuo = run {
            val muIdx = bazi.stems.indexOfFirst { it.element == Element.WOOD }
            muIdx >= 0 && bazi.stems.getOrNull(muIdx + 1)?.element == Element.FIRE
        }
        // 调候神不含日主本干（案十：丙日主非调候神）
        val huoTou = bazi.stems.filterIndexed { i, _ -> i != 2 }.any { it.element == Element.FIRE }
        val shuiTou = bazi.stems.filterIndexed { i, _ -> i != 2 }.any { it.element == Element.WATER }
        val hanTiaoYouQi = huoTou && (huoCangGen || (muTieHuo && muZai && !jinJuDuo))
        val zaoTiaoYouQi = shuiTou && shuiCangGen
        // 案一百零三·调候让位护栏二：印旺得局（≥8）用财而财透且两根（格局自足）——调候降为加分项，
        // 不走调候档（暖是锦上添花〔局长与正科之差〕，非命根子——命局不依赖暖；不论身强身弱——印重为病）。
        val geJuZu103 = yinS >= 8.0 &&
            bazi.stems.any { it != dayMaster && it.element == caiEl } &&
            bazi.branches.count { br -> br.element == caiEl || branchHiddenStems.getValue(br).any { it.element == caiEl } } >= 2
        val tiaoHouEl: Element? = when {
            hanJu && hanTiaoYouQi && !geJuZu103 && !bazi.branches.all { it.element == Element.WATER } -> Element.FIRE
            zaoJu && zaoTiaoYouQi && !geJuZu103 && !bazi.branches.all { it.element == Element.FIRE } -> Element.WATER
            else -> null
        }
        // 案卅四裁：杀旺食透而日主得禄根（禄支不克泄日主——异于库根之泄身）——禄根型食制优先于调候
        val luZhi = when (dayMaster) {
            HeavenlyStem.JIA -> EarthlyBranch.YIN
            HeavenlyStem.YI -> EarthlyBranch.MAO
            HeavenlyStem.BING, HeavenlyStem.WU -> EarthlyBranch.SI
            HeavenlyStem.DING, HeavenlyStem.JI -> EarthlyBranch.WU
            HeavenlyStem.GENG -> EarthlyBranch.SHEN
            HeavenlyStem.XIN -> EarthlyBranch.YOU
            HeavenlyStem.REN -> EarthlyBranch.HAI
            else -> EarthlyBranch.ZI
        }
        val luGen = luZhi in bazi.branches
        // 案四十三裁：调候神本身即食伤（合杀之工具）而官杀两透混杂一官被合——生克合留为纲，调候让位；
        // 调候神即官杀本身（案二十）——调候为纲，合留让位
        val heZhuHunZa = (bazi.stems.count { it.element == guanEl } >= 2 &&
            bazi.stems.filterIndexed { i, s -> i != 2 && (s.element == shiEl || s.element == biEl) }.any { hg ->
                val p = GAN_HE[hg]
                p != null && p.element == guanEl && bazi.stems.any { it == p }
            } && tiaoHouEl == shiEl) ||
            // 案四十五：调候神即印而官杀两透混杂身弱——杀重用印化之以德为纲（生克故事更全），调候让位
            (tiaoHouEl == yinEl && bazi.stems.count { it.element == guanEl } >= 2 && strength == DayStrength.WEAK)
        // 案六十八戒：燥局调候神之根被两重冲（两巳夹冲亥）——根伤不可恃，不走调候线，走生克（比劫抗杀）
        val tiaoHouGenShuangChong = tiaoHouEl != null &&
            bazi.branches.count { LIU_CHONG[it] != null && bazi.branches.contains(LIU_CHONG[it]) && it.element == tiaoHouEl } >= 2 ||
            (tiaoHouEl != null && bazi.branches.any { br -> br.element == tiaoHouEl && bazi.branches.count { it == LIU_CHONG[br] } >= 2 })
        // 案七十戒：调候神贴克透干之印（丙贴庚——调候神即克用神之忌）——不走调候线走生克（身衰印做儿·财坏印）
        val tiaoHouTieYin = bazi.stems.anyIndexed { i, s -> i != 2 && s.element == tiaoHouEl && tiaoHouEl != null && tiaoHouEl != yinEl &&
            ((i - 1 in bazi.stems.indices && bazi.stems[i - 1].element == yinEl && i - 1 != 2) ||
                (i + 1 in bazi.stems.indices && bazi.stems[i + 1].element == yinEl && i + 1 != 2)) }
        val tiaoHou = tiaoHouEl != null && !(guanS >= GUAN_WANG && shiS >= FOLLOW_MIN_SCORE && luGen) && !heZhuHunZa && !tiaoHouGenShuangChong && !tiaoHouTieYin

        // ── 旺中变弱（案七十三）：巳月火日主而巳酉半合化金×2（巳火不专、变化性很大——
        // 金旺跟着金走，火根尽化）＋官杀混杂加力＋财生杀——夏天本可以，一下变弱。
        // 然夏金不至于绝（季节定性：夏天金再旺不至于那么厉害——非金得全局）
        if (strength == DayStrength.STRONG && dayEl == Element.FIRE &&
            bazi.monthPillar.branch == EarthlyBranch.SI &&
            bazi.branches.count { it == EarthlyBranch.YOU } >= 2
        ) {
            strength = DayStrength.WEAK
        }

        // ── 旺中变弱（案七十）：月令建禄占令（即使身弱也弱不到哪去）然地支食伤本气支≥3
        //    （三木过泄、食伤混杂）——对日主过泄——旺中变弱（地支是重要的）
        if (strength == DayStrength.STRONG && bazi.monthPillar.branch.element == biEl &&
            bazi.branches.count { it.element == shiEl } >= 3
        ) {
            strength = DayStrength.WEAK
        }

        // ── 旺中变弱（案六十九）：月令建禄（日主有力量）然官杀本气支两重冲比劫月支
        //    （两酉冲一卯——比劫禄没有杀旺：日主有力量不假，但对立面敌人劲更大）——旺中变弱
        if (strength == DayStrength.STRONG && bazi.monthPillar.branch.element == biEl &&
            bazi.branches.count { it.element == guanEl && LIU_CHONG[it] == bazi.monthPillar.branch } >= 2
        ) {
            strength = DayStrength.WEAK
        }

        // ── 旺中变弱（案六十八）：亥巳逢冲＝去火存金（水把火冲去、巳中庚金独存——火根转金根）
        //    ＋巳丑拱金／巳酉化金——金旺（财得局）——身旺经一系列反应旺中变弱（身弱财旺、财杀得局）
        //    结构性转化（冲去即去）——不比分数
        if (strength == DayStrength.STRONG && dayEl == Element.FIRE &&
            bazi.branches.any { it == EarthlyBranch.HAI } && bazi.branches.any { it == EarthlyBranch.SI } &&
            (EarthlyBranch.CHOU in bazi.branches || EarthlyBranch.YOU in bazi.branches)
        ) {
            strength = DayStrength.WEAK
        }

        // ── 旺中变弱（案四十二）：旺基调（比月）而辰酉化金（辰随酉走——藏乙癸叛向、辰作金论）
        //    官杀党力压倒（≥1.5×折减印比）且月支被六冲（卯酉冲——禄根被杀根冲伤）——旺中变弱
        val chenYouHua = EarthlyBranch.CHEN in bazi.branches && EarthlyBranch.YOU in bazi.branches
        val guanSAdj2 = if (chenYouHua) guanS + 1.5 * bazi.branches.count { it == EarthlyBranch.CHEN } else guanS
        val biSAdj2 = if (chenYouHua) biS - 0.5 * bazi.branches.count { it == EarthlyBranch.CHEN } else biS
        val yinSAdj2 = if (chenYouHua) yinS - 0.5 * bazi.branches.count { it == EarthlyBranch.CHEN } else yinS
        if (strength == DayStrength.STRONG &&
            bazi.monthPillar.branch.element == biEl &&
            guanSAdj2 >= 1.5 * (yinSAdj2 + biSAdj2) &&
            bazi.branches.indexOf(bazi.monthPillar.branch).let { mi ->
                mi in bazi.branches.indices && LIU_CHONG[bazi.monthPillar.branch] == bazi.branches.getOrElse(mi + 1) { bazi.monthPillar.branch }
            }
        ) {
            strength = DayStrength.WEAK
        }

        // ── 旺中变弱（案九十三）：未月终究一印（然与丑月戌月之印不一样——夏土）＋时支禄本可论旺，
        //    然地支拱财局而财透干引化（乙木露干两亥拱未——亥未拱木：拱局透干则实＝财得局）——
        //    财坏印＋食伤旺泄＋官杀克身：印星为用而财得局坏之——旺中变弱（运填拱则坏印成真）
        val gongCaiTrio = listOf(
            setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI) to Element.WOOD,
            setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU) to Element.FIRE,
            setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN) to Element.WATER,
            setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU) to Element.METAL,
        ).firstOrNull { (trio, el) ->
            el == caiEl && bazi.branches.count { it in trio } >= 2 && !trio.all { it in bazi.branches }
        }
        val gongCaiYinHuaRaw = strength == DayStrength.STRONG &&
            bazi.monthPillar.branch.element == yinEl &&
            !bazi.stems.any { it != dayMaster && it.element == yinEl } &&
            bazi.stems.any { it != dayMaster && it.element == caiEl } &&
            gongCaiTrio != null &&
            shiS >= FOLLOW_MIN_SCORE && guanS >= FOLLOW_MIN_SCORE
        // 案一百一十一戒：坐下即印而印旺（印党力＞官杀党力——印贴身生，与无根修正之案四十八豁免同式）
        // ——不因拱财引化翻弱：庚生未月坐辰印，印旺身旺用官（财生出官）。
        // 师以「亥未是一方面」承认拱的关联——翻弱与否不系于拱，系于印旺坐印，故不设相邻条件；
        // 对勘案九十三：日主坐亥泄、食伤（癸透两亥）通源旺泄——翻弱用印。
        val zuoYinWang111 = bazi.dayPillar.branch.element == yinEl && yinS > guanS
        val gongCaiYinHua = gongCaiYinHuaRaw && !zuoYinWang111
        if (gongCaiYinHua) {
            strength = DayStrength.WEAK
        }

        // ── 弱中变旺（案十定则；案廿六护栏）：弱基调而日主坐贴身印比本气／月干印比透，
        //    且月令敌党之支被「印比之支」贴身泄克（官被泄克转衰）——弱中变旺。
        //    护栏①财克印不算（档五十一仍身弱用印）；护栏②敌党压倒性（≥1.5×印比）不翻——
        //    官旺生印＝杀化印（化杀），正是杀重身弱用印（案廿六：官10对印比3.5，杀重用印格）。
        val dayBranchEl = bazi.dayPillar.branch.element
        var ruoZhongBianWang = false
        if (!ruoJi && !wangJi && strength == DayStrength.WEAK &&
            monthElAdj != yinEl && monthElAdj != biEl &&
            // 案七十九豁免：自坐羊刃（「自坐刃就够旺的了」——羊刃跟别的禄不一样，重于禄）＋通根身库＋
            // 比劫透干——一路帮身：食伤泄虽过（护栏②1.5×）亦翻旺——刃98公斤vs食伤100公斤之辨
            (partyScore(bazi, monthElAdj) < 1.5 * (yinS + biS) ||
                (bazi.dayPillar.branch == YANG_REN[dayMaster] && hasBenQiRoot(bazi, biEl) &&
                    bazi.stems.any { it != dayMaster && it.element == biEl })) &&
            (dayBranchEl == yinEl || dayBranchEl == biEl ||
                bazi.monthPillar.stem.element == yinEl || bazi.monthPillar.stem.element == biEl) &&
            ((monthElAdj.generates == dayBranchEl && (dayBranchEl == yinEl || dayBranchEl == biEl)) ||
                (dayBranchEl.controls == monthElAdj && dayBranchEl == biEl) ||
                // 案五十：坐下印本气（兼刃）克月令食伤（午火克酉金）——食伤被印克转衰，弱中变旺
                // 护栏：限食伤月令＋非寒燥调候局（案廿子月寒局调候优先不翻——财月令被印克亦不翻）
                (dayBranchEl == yinEl && dayBranchEl.controls == monthElAdj && monthElAdj == shiEl && !hanJu && !zaoJu) ||
                // 案卅八：月干比透＋月支藏日主余气根（丁透于未月，未中丁两见）——身旺；
                // 案四十九：月干比透＋坐下比劫本气根（己透＋坐戌——两戌厚土）——身旺；
                // 案七十九：自坐刃（羊刃跟别的禄不一样——重于禄）＋通根身库＋比劫透——
                // 刃98公斤足以扛食伤100公斤之泄（「伤官强不代表日主衰」）
                (bazi.monthPillar.stem.element == biEl && dayBranchEl == biEl) ||
                (bazi.monthPillar.stem.element == biEl &&
                    branchHiddenStems.getValue(monthBranch).any { it.element == biEl }) ||
                (bazi.dayPillar.branch == YANG_REN[dayMaster] && hasBenQiRoot(bazi, biEl))
            )
        ) {
            strength = DayStrength.STRONG
            ruoZhongBianWang = true
        }

        // 案一百一十六·羊刃自旺：日主自坐阳刃而局中又见其禄（「丙火自坐羊刃，还配一个巳火，身旺，足以承载食伤生财」）
        // ——财当令、食伤透干生财、无印生身（藏支之印不算）、官杀透而坐禄之上（「癸水露在巳上面，就是个隐患是个病」）：
        // 刃禄俱全即论身旺（不拘敌党 1.5×——对勘案七十九：自坐刃＋身库＋比劫透）；然比财官食稍偏弱——旺中带弱
        val yangRenZiWang116 = !ruoJi && !wangJi && strength == DayStrength.WEAK &&
            bazi.dayPillar.branch == YANG_REN[dayMaster] && luGen && luZhi != bazi.dayPillar.branch &&
            monthElAdj == caiEl &&
            bazi.stems.any { it != dayMaster && it.element == shiEl } &&
            bazi.stems.none { it != dayMaster && it.element == yinEl } && !hasBenQiRoot(bazi, yinEl) &&
            bazi.stems.indices.any { i -> i != 2 && bazi.stems[i].element == guanEl && bazi.branches[i] == luZhi }
        if (yangRenZiWang116) strength = DayStrength.STRONG

        // 案一百二十二·印比得局（「辰酉生金，辰申生金，金旺。虽然说木当令，但是金得局」——就算大王也怕四个2）：
        // 月令非印比而地支印比本气支成势（比劫本气支≥2＋印本气支≥1、合共≥3，且印支与比劫支相邻——生之／合之）——身强
        val biZhis122 = bazi.branches.count { it.element == biEl }
        val yinZhis122 = bazi.branches.count { it.element == yinEl }
        val yinBiDeJu122 = !ruoJi && !wangJi && strength == DayStrength.WEAK &&
            monthElAdj != yinEl && monthElAdj != biEl && biZhis122 >= 2 && yinZhis122 >= 1 && biZhis122 + yinZhis122 >= 3 &&
            bazi.branches.indices.any { k ->
                bazi.branches[k].element == yinEl && listOf(k - 1, k + 1).any { j ->
                    j in bazi.branches.indices && bazi.branches[j].element == biEl
                }
            }
        if (yinBiDeJu122) strength = DayStrength.STRONG

        // 案一百二十三·年柱专旺帮身力大（「年柱有一柱专旺的帮身的力量比较大——要记住了」：庚申之属，干支一气为比劫）：
        // 月令非印比而年柱干支一气比劫、日主坐禄、印比与克泄大致相当（「金有力的……基本上平衡」）——身强
        val nianZhuZhuanWang123 = !ruoJi && !wangJi && strength == DayStrength.WEAK &&
            monthElAdj != yinEl && monthElAdj != biEl &&
            bazi.stems[0].element == biEl && bazi.branches[0].element == biEl &&
            bazi.dayPillar.branch == luZhi && yinS + biS >= 0.6 * (guanS + caiS + shiS)
        if (nianZhuZhuanWang123) strength = DayStrength.STRONG

        // ── 全浊三废（案十二）：官、食、财俱在而各有浊神克之——用神无着落 ──
        // 官废＝官之克者透干力足，且官之源（财）被断（财支逢冲，或比劫根大势压无根之财）
        // 食废＝印透干力足克食，且食无本气根（有强本气根则克不动——案九壬水通根不废）
        // 财废＝财支逢六冲，或财无本气根而比劫势压
        val keGuan = Element.entries.first { it.controls == guanEl }
        val guanFei = guanS > 0.0 && bazi.stems.any { it.element == keGuan } &&
            partyScore(bazi, keGuan) >= guanS &&
            (bazi.branches.any { br -> br.element == caiEl && LIU_CHONG[br] != null && LIU_CHONG.getValue(br) in bazi.branches } ||
                (caiS > 0.0 && !hasBenQiRoot(bazi, caiEl) && biS >= 1.5 * caiS && hasBenQiRoot(bazi, biEl)))
        val shiFei = shiS > 0.0 && bazi.stems.any { it.element == yinEl } &&
            yinS >= shiS && !hasBenQiRoot(bazi, shiEl)
        val caiFei = caiS > 0.0 && (
            bazi.branches.any { br -> br.element == caiEl && LIU_CHONG[br] != null && LIU_CHONG.getValue(br) in bazi.branches } ||
                (!hasBenQiRoot(bazi, caiEl) && biS >= 1.5 * caiS)
            )

        // 案一百一十三·食伤三合局全而泄日主过甚（寅午戌——有了就稍微过了）：身弱无印而日主有根（不算弱极）、
        // 官杀当令旺克身——不取食制（克泄交加），反以泄为病、要印补身
        val shiSanHe113 = SAN_HE_XU.firstOrNull { (trio, el) -> el == shiEl && trio.all { it in bazi.branches } }
        val guoXieQiuYin113 = strength == DayStrength.WEAK && !ruoJi && !wangJi &&
            guanS >= GUAN_WANG && shiSanHe113 != null && yinS < FOLLOW_MIN_SCORE &&
            (hasBenQiRoot(bazi, biEl) ||
                bazi.branches.count { br -> branchHiddenStems.getValue(br).any { it.element == biEl } } >= 3)
        // 案一百一十四·杀露不任：官杀透干而弱、坐支为食伤本气（杀坐土上），干头无食伤而余干皆比劫（干头没土都是火）
        // ——木火一片，杀也不行：不取财滋弱杀（对勘案廿二：杀通根得气、无食伤——仍优先用杀）
        val shaLuBuRen114 = bazi.stems.indices.any { i ->
            i != 2 && bazi.stems[i].element == guanEl && bazi.branches[i].element == shiEl
        } && guanS < GUAN_WANG && bazi.stems.none { it != dayMaster && it.element == shiEl } &&
            bazi.stems.filterIndexed { i, s -> i != 2 && s.element != guanEl }.all { it.element == biEl }
        // 案一百一十四·暗处的财：身旺忌印——食伤被坐下印贴克（不能为用而为喜）、财不透而藏于食伤本气支（没被破坏）
        // ——用财喜食伤；坐下印为病（运支暗冲去病＋暗会财局——更发；合而化比劫——拱火旺）
        val anCai114 = strength == DayStrength.STRONG && !ruoJi && !wangJi && !qiangJi && shaLuBuRen114 &&
            bazi.dayPillar.branch.element == yinEl &&
            bazi.branches.anyIndexed { i, br ->
                br.element == shiEl &&
                    bazi.branches.indices.any { k -> kotlin.math.abs(k - i) == 1 && bazi.branches[k].element == yinEl }
            } &&
            bazi.stems.none { it != dayMaster && it.element == caiEl } &&
            bazi.branches.any { br -> br.element == shiEl && branchHiddenStems.getValue(br).any { it.element == caiEl } }
        // 案一百一十五·身旺印旺用财坏印而财无根（建禄＋印本气支≥3、财透≥2 而无本气根、无官、食伤不透）——
        // 藏根之支向背不从我（巳跟木火一伙）：去寡只论官杀；运支暗冲印病＝发大财
        val anCaiWuGen115 = strength == DayStrength.STRONG && !ruoJi && !wangJi && !qiangJi &&
            bazi.monthPillar.branch == luZhi &&
            bazi.branches.count { it.element == yinEl } >= 3 &&
            bazi.stems.count { it != dayMaster && it.element == caiEl } >= 2 &&
            !hasBenQiRoot(bazi, caiEl) && guanS == 0.0 &&
            bazi.stems.none { it != dayMaster && it.element == shiEl }
        // 案一百一十七·干多不如支重·财旺不载：日主外比劫透干≥2（四辛——故不能从财）而日主与比劫地支全无根
        // （「辛金叫地支不载」）、无印、财当令而地支得局（本气支≥3——力量相差悬殊）——承载不了这么旺的财
        // （透干数按**位置**计——天干与日主同字〔四辛〕时不可用「字≠日主」过滤：位比较非值比较）
        val ganDuoBuZai117 = strength == DayStrength.WEAK && !ruoJi && !wangJi &&
            bazi.stems.indices.count { it != 2 && bazi.stems[it].element == biEl } >= 2 &&
            !hasBenQiRoot(bazi, biEl) &&
            !bazi.branches.any { br -> branchHiddenStems.getValue(br).any { it.element == biEl } } &&
            yinS == 0.0 && monthElAdj == caiEl &&
            bazi.branches.count { it.element == caiEl } >= 3
        // 案一百一十九·印旺用财而地支无火（身强）：印得局（本气支≥3／三会三合支全）、无官可用、财透而地支无食伤本气根——
        // 「要维持平衡，无非就用财来克印」（不论财之根：地支没有火则来木不行——印旺用财怕劫财；
        //   火土虚透无根＝用神无力：走火土运小康、水木运差）
        val yinWangCai119 = strength == DayStrength.STRONG && !ruoJi && !wangJi && !anCaiWuGen115 &&
            yinDeJuShi(bazi, yinEl) &&
            bazi.stems.any { it != dayMaster && it.element == caiEl } &&
            bazi.stems.none { it != dayMaster && it.element == guanEl } && !hasBenQiRoot(bazi, guanEl) &&
            !hasBenQiRoot(bazi, shiEl)
        // 案一百二十·伤官用劫而印全无、官杀透干——克泄交加（泄体太过）：
        // 「这种的命基本上都得蹲大牢，不蹲大牢的得死」「没有印，富裕不了」「来水运也仅仅是温饱」
        val guanIdx120 = bazi.stems.indices.firstOrNull { it != 2 && bazi.stems[it].element == guanEl }
        val keXieJiaoJia120 = strength == DayStrength.WEAK && !ruoJi && !wangJi && shiS >= 8.0 && yinS == 0.0 &&
            guanIdx120 != null
        // 案一百二十一·身旺用官而财当令、食伤之支与财支合（亥卯——食伤反生官之源头：友情）、印透干（闲神护卫——来水挡住护官）
        val shiHeCai121 = bazi.branches.any { s -> s.element == shiEl && bazi.branches.any { c ->
            c.element == caiEl && (SAN_HE_XU.any { (t, _) -> s in t && c in t } || (LIU_HE[s] == c && liuHeHuaEl(s, c) == caiEl))
        } }
        val qingFen121 = strength == DayStrength.STRONG && !ruoJi && !wangJi && monthElAdj == caiEl && shiHeCai121 &&
            bazi.stems.indices.any { it != 2 && bazi.stems[it].element == yinEl }
        // 反事实（无印护卫）：身旺、财当令、食伤合财而印不透——伤官运克官无护卫：犯官灾（打官司／犯错误）
        val wuHu121 = strength == DayStrength.STRONG && !ruoJi && !wangJi && monthElAdj == caiEl && shiHeCai121 &&
            bazi.stems.indices.none { it != 2 && bazi.stems[it].element == yinEl }

        // 案一百二十二·身强用官而病太大（「卯木全让金给克了」「甲木也地支不载」「找地支有没有水——没水那就不行」）：
        // 印比得局＋官所坐之财支被比劫本气支贴冲（酉贴卯）＋食伤既不透干亦无本气支（源头不现）——清枯、来水仅凑合、金运彻底完了
        val bingDa122JieGou = yinBiDeJu122 &&
            bazi.stems.indices.any { i ->
                i != 2 && bazi.stems[i].element == guanEl && listOf(i - 1, i + 1).any { j ->
                    j in bazi.branches.indices && bazi.branches[j].element == biEl &&
                        LIU_CHONG[bazi.branches[j]] == bazi.branches[i]
                }
            } &&
            bazi.stems.indices.none { it != 2 && bazi.stems[it].element == shiEl } && !hasBenQiRoot(bazi, shiEl)
        // 案一百二十三·用官喜帮火（「这局就喜帮火了」「命局用火」）：年柱专旺身强而官杀当令、食伤两透通根（水旺有源）、
        // 财全无（官无源）——「火有水克……不平衡的原因就是有水克火，然后火没有源头」——不以食制、不泄金（水已旺），用官喜财喜帮火
        val yongGuan123 = nianZhuZhuanWang123 && monthElAdj == guanEl &&
            bazi.stems.indices.count { it != 2 && bazi.stems[it].element == shiEl } >= 2 &&
            bazi.branches.any { br -> branchHiddenStems.getValue(br).any { it.element == shiEl } } &&
            partyScore(bazi, caiEl) == 0.0
        // 案一百二十四·有力就能食神制杀（「天干露三个金，没有通根，辛未有点力，日主是不旺，但是日主是有点力……
        // 有力就能食神制杀，跟日主虚透无力强制杀是不一样的」）：身弱而非弱极、杀旺（财生杀）、年柱比劫坐印（辛未）、
        // 比劫多透而地支无根、食伤有本气支且冲官杀本气支（子午冲——去火之药）、财有力（杀有源——制而不绝）
        val youLiShiZhi124 = strength == DayStrength.WEAK && !ruoJi && !wangJi && guanS >= GUAN_WANG &&
            bazi.stems[0].element == biEl && bazi.branches[0].element == yinEl && !hasBenQiRoot(bazi, biEl) &&
            bazi.stems.indices.count { it != 2 && bazi.stems[it].element == biEl } >= 2 &&
            hasBenQiRoot(bazi, shiEl) &&
            bazi.branches.any { s -> s.element == shiEl && LIU_CHONG[s]?.let { it in bazi.branches && it.element == guanEl } == true } &&
            partyScore(bazi, caiEl) >= FOLLOW_MIN_SCORE
        // 案一百二十五·假去寡·印护官（「身旺……说去寡也对，用官也对，得看命主走什么大运」「印护官了，当兵」）：
        // 身旺、伤官当令、独官（官杀本气支仅一、党力弱）被食伤本气支贴克（戌克亥）、印透干有力（甲甲护官）——火土运去寡（火通关）、金水运用官
        val duGuan125 = bazi.branches.count { it.element == guanEl } == 1 && partyScore(bazi, guanEl) <= FOLLOW_MIN_SCORE &&
            bazi.branches.indices.any { k ->
                bazi.branches[k].element == guanEl && listOf(k - 1, k + 1).any { j ->
                    j in bazi.branches.indices && bazi.branches[j].element == shiEl
                }
            }
        // （月令本气须为食伤——卯戌合火只改党属、不改月令本气之行）
        val jiaQuGua125JieGou = strength == DayStrength.STRONG && !ruoJi && !wangJi &&
            bazi.monthPillar.branch.element == shiEl && duGuan125 &&
            bazi.stems.indices.count { it != 2 && bazi.stems[it].element == yinEl } >= 1 && yinS >= GUAN_WANG
        // 案一百二十六·坐下暗印（「身旺中变弱，必须用印，用未中丁火」「吉神深藏终身之福」「丁火不伤，有儿子」）：
        // 身弱、官杀当令（月令本气——日主生于克我当令之月而虚：「春天土虚，相当于水生在夏天，火生冬天一样」）、
        // 食伤透干克官（伤官见官——克泄交加）、日支为比劫本气而藏印（「日主坐未是有力」——坐下暗印）、
        // 印不透干亦无本气根（藏支之印于此亦为有印——异于案五十二「藏支之印不算有印」）、比劫有本气根、
        // 暗印不伤（日支不被冲、克印之财不透干——「未中丁火不伤」；被伤者师未授，不入此档）。
        // 此结构实限土日（坐未／戌藏丁）、金日（坐申藏戊）——余行无比劫本气支藏印者；金日乃据师之类推，待师印证
        val anYin126JieGou = strength == DayStrength.WEAK && !ruoJi && !wangJi &&
            bazi.monthPillar.branch.element == guanEl && bazi.dayPillar.branch.element == biEl &&
            bazi.stems.indices.any { it != 2 && bazi.stems[it].element == shiEl } &&
            bazi.stems.indices.none { it != 2 && bazi.stems[it].element == yinEl } && !hasBenQiRoot(bazi, yinEl) &&
            branchHiddenStems.getValue(bazi.dayPillar.branch).any { it.element == yinEl } &&
            hasBenQiRoot(bazi, biEl) &&
            bazi.branches.none { it != bazi.dayPillar.branch && LIU_CHONG[it] == bazi.dayPillar.branch } &&
            bazi.stems.none { it != dayMaster && it.element == caiEl }

        // 案一百二十七·财官旺·比劫硬扛（当兵专项④类「气势」——判据见 yingKang127Of：「财星当令，财生官旺，
        // 这都挡不住了，完全是拿比劫硬扛」「肯定用神是午火比劫」）
        val yk127 = yingKang127Of(bazi, dayMaster, luZhi, strength, ruoJi, wangJi, yinEl, biEl, caiEl, guanEl)

        // 取用依据文案共用上下文（各案文案自 analyze 抽出——方法体量上限）
        val cb = CtxB(bazi, dayMaster, monthBranch, luZhi, yinEl, biEl, shiEl, caiEl, guanEl)

        val choice: Choice = when {
            // 第三讲·弱极从势：命里谁旺即随谁；无明显独大而财官食并存者，必要财运解围
            ruoJi -> {
                val sorted = listOf(
                    Triple("官杀", guanEl, guanS),
                    Triple("财星", caiEl, caiS),
                    Triple("食伤", shiEl, shiS),
                ).filter { it.third >= FOLLOW_MIN_SCORE }.sortedByDescending { it.third }
                val top = sorted.firstOrNull() ?: Triple("官杀", guanEl, guanS)
                val second = sorted.getOrNull(1)
                // 独大判据：无第二名，或旺者党力 ≥ 次者 1.5 倍
                val dominant = sorted.size <= 1 || (second != null && top.third >= 1.5 * second.third)
                val detail = "官党力${format(guanS)}、财党力${format(caiS)}、食伤党力${format(shiS)}"
                when {
                    !dominant && sorted.any { it.first == "财星" } -> Choice(
                        JinBiaoQuYongWay.CAI_JIE_WEI, caiEl,
                        "弱极从势：财官食神并存而无独旺（$detail），必要财运解围，用神取财星——" +
                            "财能和解食伤与官杀之关系（食伤生财、财生官杀），命局无争妒之风，气静和平；行财运解围而发",
                    )
                    top.first == "官杀" -> Choice(
                        JinBiaoQuYongWay.SUI_GUAN, guanEl,
                        "弱极从势：官旺（$detail）则要随官，用神取官杀——行财官运必发达（权威定论：满足弱极两前提则百断百中）",
                    )
                    top.first == "财星" -> Choice(
                        JinBiaoQuYongWay.SUI_CAI, caiEl,
                        "弱极从势：财旺（$detail）则要随财，用神取财星——行生财之运必富贵显达（权威定论：满足弱极两前提则百断百中）",
                    )
                    else -> Choice(
                        JinBiaoQuYongWay.SUI_SHI, shiEl,
                        "弱极从势：食伤独旺（$detail）从儿，用神取食伤",
                    )
                }
            }
            // 第三讲续·旺极/强极专旺：必然随其旺神，用神取比劫（日主本行）
            wangJi -> Choice(
                JinBiaoQuYongWay.SUI_WANG, biEl,
                buildString {
                    if (qiangJi) {
                        append("强极印比同心：命局只存在印和比劫，可顺而不可逆——用神取比劫（${biEl.chinese}）")
                        append("；只要运行印比，人生定可亨通显达（好八字）；若走财官之地，触怒强神大凶")
                        append(
                            if (yinDeJu) "；幸印星得局（印${format(yinS)}旺于比${format(biS)}），方可化煞生印——官杀运不为大凶，运支合入印局（三合/六合化印）则大发"
                            else "；印未得局（印${format(yinS)}比${format(biS)}——比不弱于印），走官运必大凶",
                        )
                    } else {
                        append("旺极专旺：四柱全为比劫、无有官杀食伤（财亦无）——必然随其旺神，用神取比劫（${biEl.chinese}）")
                        append("；运行比劫印绶富贵自然显达；运行官杀犯旺，当知凶祸即至；此格若行财地，立断九死一生")
                        append(if (!yinDeJuDuo) "；局中印轻（一二${yinEl.chinese}枭、地支不成局），食伤顺泄而行也是好运" else "；局中印重（地支${yinEl.chinese}成局），走食伤运印夺食，不可行")
                    }
                    // 案一百一十八·印得局而食伤全无——身旺全憋着：走泄他的也不行、走好运一样没儿子（不适用返回空串）
                    append(bieZhe118(cb, yinDeJuDuo))
                },
            )
            // 第八讲·合而化（案卅六）：合干根气太弱（仅一弱根）而化神当令——戊癸化火：
            // 火生土、癸去不了戊土＝无情，用神被克太坏——癸丑运之前一事无成多灾多难，帮官杀之运方转
            strength == DayStrength.STRONG && !ruoJi && !wangJi && guanS >= FOLLOW_MIN_SCORE && shiS >= FOLLOW_MIN_SCORE &&
                shiS >= guanS &&
                bazi.stems.filterIndexed { i, s -> i != 2 && s.element == shiEl }.any { sg ->
                    val partner = GAN_HE[sg]
                    partner != null && partner.element == guanEl &&
                        bazi.stems.any { it == partner } &&
                        bazi.branches.count { br -> branchHiddenStems.getValue(br).any { it.element == guanEl } } <= 1 &&
                        zaoJu
                } -> Choice(
                JinBiaoQuYongWay.HUA_WU_QING, guanEl,
                "合而化·无情（案卅六）：身强用官杀，壬水有气不去寡；${shiEl.chinese}重制杀太过。官干合食伤干，然被合之干根气太弱（仅一弱根）而化神（${biEl.chinese}）当令——**戊癸化火**：${biEl.chinese}生${shiEl.chinese}，合干解不开克＝**无情**。${shiEl.chinese}土克${guanEl.chinese}水把用神克得太坏——**帮官杀之运到来之前一事无成、多灾多难**；帮官杀之运（如癸丑壬子）方转".replace("**", ""),
            )
            // 第八讲·合官留杀（案卅五）：身旺有官杀先定官杀；食伤有力制官杀本是差命，
            // 然一官干合住食伤干（戊癸合）——食伤不克官杀＝清格：留下之官杀为用，有情豁免旺衰
            strength == DayStrength.STRONG && !ruoJi && !wangJi && guanS >= FOLLOW_MIN_SCORE && shiS >= FOLLOW_MIN_SCORE &&
                shiS >= guanS &&
                bazi.stems.filterIndexed { i, s -> i != 2 && s.element == shiEl }.any { sg ->
                    val partner = GAN_HE[sg]
                    partner != null && partner.element == guanEl &&
                        bazi.stems.any { it == partner } && // 被合之官干在局
                        bazi.branches.any { br -> branchHiddenStems.getValue(br).any { it.element == guanEl } } && // 有根不化
                        bazi.stems.count { it.element == guanEl } >= 2 // 留下另一官杀
                } -> Choice(
                JinBiaoQuYongWay.HE_LIU, guanEl,
                "合官留杀格（案卅五）：身旺（刃刃通根月令）——有官杀先定官杀（有气不能去尽；身旺不能用食伤，因官杀在局）。${shiEl.chinese}食伤通根有力本克${guanEl.chinese}官杀（用神被制＝差命），然官干合住食伤干（戊癸之合）＝**合官留杀，清格**——食伤不克官杀，留下${guanEl.chinese}为用神。有情豁免旺衰：不必虑${guanEl.chinese}之衰，只要有情就可以（无此合则官杀太衰＝贫夭命）——有情就加分，要富贵（命主当官）。被合之干有根气故不化（化则${biEl.chinese}生${shiEl.chinese}接着克＝无情）；${guanEl.chinese}官杀留一即可——解掉${shiEl.chinese}克${guanEl.chinese}就富贵。制${shiEl.chinese}之运（${yinEl.chinese}）亦好——制${shiEl.chinese}留${guanEl.chinese}".replace("**", ""),
            )
            // 第六讲·气象调候（案十五/案十七/案二十）：先不按生克、按气象论——得气之寒遇暖而发
            // 案一百一十九戒：印得局而财透无官（地支无食伤本气根）——印旺用财维持平衡，调候让位（同案一百零三护栏二）
            tiaoHou && !yinWangCai119 -> Choice(
                JinBiaoQuYongWay.TIAO_HOU, tiaoHouEl!!,
                if (tiaoHouEl == Element.FIRE) {
                    "气象调候（案十五/案二十）：局偏寒（冬水）——先不按生克、按气象论：寒就要用丙火暖局，能不能暖全凭丙火有没有气。" +
                        "丙火${if (huoCangGen) "地支有藏根（寅中丙、戌中丁亦是根）" else "有透干之木紧贴相生且木有地载"}——有气（即使无根，有甲木贴身生之亦算有气），故用丙火调候：**得气之寒，遇暖而发**——走木火之运吉（当官）".replace("**", "")
                } else {
                    "气象调候（案十七）：局燥（夏火）——暖虽至要寒有根：壬水坐库有根（辰丑申中壬癸藏根亦是），可以调候——" +
                        "走金水之运当官富贵，得了大气象"
                },
            )
            // 第五讲命三·昆仑得局：顺其势（不以日主论，围绕得局之五行说事）
            kunLun -> {
                val keEl = Element.entries.first { it.controls == kunLunEl }
                Choice(
                    JinBiaoQuYongWay.KUN_LUN, kunLunEl,
                    buildString {
                        append("昆仑得局（命三）：地支${kunLunEl.chinese}得局配${kunLunSource!!.chinese}生——昆仑之${kunLunEl.chinese}，只可顺不可逆。")
                        append("此势不一定指日主——命局${kunLunEl.chinese}地支得局配${kunLunSource.chinese}生，即围绕${kunLunEl.chinese}说事：")
                        append("顺者${kunLunSource.chinese}（生源）、${kunLunEl.chinese}（本势）、${kunLunEl.generates.chinese}（顺泄）；")
                        append("逆者${keEl.chinese}（克${kunLunEl.chinese}止${kunLunEl.chinese}）与${kunLunEl.controls.chinese}（克${kunLunSource.chinese}逆金水之局）——顺来逆去贫之意，逆则贫命出灾")
                        // 案九十七·一片金水（昆仑即日主本行）
                        if (kunLunEl == biEl) {
                            append("。**一片金水才是昆仑之水（案九十七）**：地支四支全是${kunLunSource.chinese}${kunLunEl.chinese}本气——全局一片${kunLunSource.chinese}${kunLunEl.chinese}、配${kunLunSource.chinese}生${kunLunEl.chinese}，得局了才算；**不要见着${kunLunSource.chinese}生${kunLunEl.chinese}就叫昆仑之水**——若年月是土、只是日时${kunLunSource.chinese}${kunLunEl.chinese}，就不是")
                            append("。**顺逆之机需理会**：${kunLunEl.chinese}旺到极端，只能顺着它、不能逆着它——来${kunLunEl.chinese}来${kunLunEl.generates.chinese}都不算逆（**${kunLunEl.generates.chinese}来根了也不是${kunLunSource.chinese}${kunLunEl.generates.chinese}交战——${kunLunSource.chinese}生${kunLunEl.chinese}${kunLunEl.chinese}生${kunLunEl.generates.chinese}，只能说是泄它，没有逆它；肯定没有来${kunLunSource.chinese}好，但是来了${kunLunEl.generates.chinese}也没事**）；**唯独${keEl.chinese}不可——止不住的**：${keEl.chinese}有两个功能，一者生${kunLunSource.chinese}、一者克我的${kunLunEl.chinese}——旺到极端只论逆不论功（即使稍微给我点好处〔生印〕，但稍微让我不舒服，你就不行——${keEl.chinese}来多少也是生${kunLunSource.chinese}，毕竟${kunLunSource.chinese}在那摆着，但${keEl.chinese}也克我的${kunLunEl.chinese}）")
                            append("。**特殊格局的层次完全是由大运决定**——要是上来就走${keEl.chinese}运那就完了（层次底下的命）；**主要是看中年：这人的层次是由中年运决定的**；子女亦然——逆着它就没有儿子（有儿子也是被克了、或者离婚儿子跟对方了），**特殊的格局方方面面都得看大运**")
                            append("。**枭夺食反例（待后续案例）**：若原局全是${kunLunSource.chinese}、就日主是${kunLunEl.chinese}——这时来了${kunLunEl.generates.chinese}，那才叫枭神夺食，就要出事了（本案${kunLunSource.chinese}生${kunLunEl.chinese}有通关，木是泄不是夺）")
                            // 案九十八·昆仑顺局而透克神（上个透木＝泄〔顺〕，这个透土＝克〔逆〕）
                            val keTou98 = bazi.stems.filter { it != dayMaster && it.element == keEl }
                            if (keTou98.isNotEmpty()) {
                                append("。**昆仑透克神＝病（案九十八）**：上个（案九十七）透的是${kunLunEl.generates.chinese}——**泄的**；这个透${keEl.chinese}——**克的**：**从本质上看就混不了（一个是泄〔顺〕、一个是克〔逆〕）**。${keTou98.joinToString("") { it.chinese }}两${keEl.chinese}都是病——**把${keEl.chinese}去了，才能好得了**；走帮${keEl.chinese}的运就不行。**露一个也不行，不管露几个**（${keEl.chinese}在这摆着一定是两个作用——生${kunLunSource.chinese}、克${kunLunEl.chinese}：不能说生${kunLunSource.chinese}了就不克${kunLunEl.chinese}，也不能说克${kunLunEl.chinese}了就不生${kunLunSource.chinese}）；${keEl.chinese}虚原局克${kunLunEl.chinese}之力不大——然**随时都是个隐患：大运走火${keEl.chinese}把${keEl.chinese}帮起来，力量就大了**")
                                append("。**去寡之辨**：此命没有食神（都是印）——去寡三条件之一「有食伤」不备，**没法去寡**；只能说**大运来了食神才能去寡**（若透${kunLunEl.generates.chinese}食神于时柱〔甲申之属〕——那就好了）。若走甲子乙亥水木之运——去了病，**肯定是个有层次的人**；然此命没走上好运：虽后有${kunLunEl.generates.chinese}木运而**地支都是南方火（火旺${keEl.chinese}相）——来了木也不管事**，只是稍微有点好事、还是坏事多，**总体肯定是过得非常差的**——这个${kunLunEl.chinese}不能逆着它")
                                append("。**性格断**：不能克${kunLunEl.chinese}，却露两${keEl.chinese}——此人**叛逆、经常不服（不服${kunLunEl.chinese}的气势）**；实际也有一个拼搏的劲（不服输）——**但有的人不服输管用、有的人越不服输越不行**：做人的方式决定能不能取得成就（需把做事的方式、性格改变一下）；上个命走戌运时也是不服——那个时候人的性格就改变了、做事方式也跟着改变了")
                            }
                        }
                    },
                )
            }
            // 第八讲续·去杀留官／去官留杀·取清（案四十）：官杀重混杂（党力≥6、通根）——
            // 官太旺之混杂仍要合去一个（任一干合去其一：劫财合杀、食伤合官皆可）＝取清——依然当官
            strength == DayStrength.STRONG && !ruoJi && !wangJi && guanS >= 6.0 &&
                bazi.stems.count { it.element == guanEl } >= 2 &&
                bazi.stems.filterIndexed { i, s -> i != 2 && (s.element == shiEl || s.element == biEl) }.any { hg ->
                    val p = GAN_HE[hg]
                    p != null && p.element == guanEl && bazi.stems.any { it == p }
                } -> Choice(
                JinBiaoQuYongWay.HE_LIU, guanEl,
                "去杀留官·取清（案四十）：官杀通根重、两透混杂（党力${format(guanS)}）——官杀旺加上混杂更旺，**还是要合去一个＝取清**（劫财合杀＝去杀留官，与合官留杀一体两面）。留下之${guanEl.chinese}为用神清纯——**依然当官**。与案卅八成对：官旺混杂宜合取清；官弱赖混杂加力则不宜合".replace("**", ""),
            )
            // 第八讲续·合去混杂（案卅七）：身弱官杀两透混杂（混杂加力克身）而食伤干合去其一——
            // 去留清纯＝有情豁免（不用考虑留下官杀旺而克身）；留下之官杀为用
            // 案七十五戒：身弱而食伤当令成局泄体极重（克泄交加——王炸之泄）——制杀也没用（还不如不制）：
            // 合杀有情不作数，走用劫（用神弱＝财富低——非制杀小故无钱）
            strength == DayStrength.WEAK && !ruoJi &&
                !(bazi.monthPillar.branch.element == shiEl && shiS >= 2.0 * biS) &&
                bazi.stems.count { it.element == guanEl } >= 2 &&
                bazi.stems.filterIndexed { i, s -> i != 2 && s.element == shiEl }.any { sg ->
                    val partner = GAN_HE[sg]
                    partner != null && partner.element == guanEl && bazi.stems.any { it == partner }
                } -> Choice(
                JinBiaoQuYongWay.HE_LIU, guanEl,
                buildString {
                    append("合去混杂·有情（案卅七/卅九/四十二/四十三）：${guanEl.chinese}官杀两透混杂——身弱不喜混杂（壬癸一混杂，${guanEl.chinese}之力更大、克身更重；身旺用官而官弱反喜混杂——混杂能加力）。身弱官杀混杂必须去一个或用印化；无印则食伤干去一官也算有情——化不化不论，合了这个官就叫有情；有情就是有格局，不在于命局旺弱——不用考虑留下的${guanEl.chinese}很旺克身的问题。虽仍身弱，走帮身运也当官。留下${guanEl.chinese}为用神，有情就加分要富贵")
                    // 案四十三：辰水辩证——火土多辰克水（印伤）；金水多辰帮水（案卅七）
                    if (EarthlyBranch.CHEN in bazi.branches && yinEl == Element.WATER) {
                        val huoTu = bazi.branches.count { it.element == Element.EARTH || it.element == Element.FIRE }
                        val jinShui = bazi.branches.count { it.element == Element.METAL || it.element == Element.WATER }
                        append(
                            if (huoTu > jinShui) "；辰水辩证：地支火土多——辰毕竟是土，必克水（印根被两土贴克，身愈弱）" else "；辰水辩证：地支金水多——辰要帮水（蓄印）",
                        )
                    }
                    if (EarthlyBranch.YIN in bazi.branches && EarthlyBranch.HAI in bazi.branches && bazi.dayPillar.stem.element == Element.WOOD) {
                        append("；寅亥化木——六合帮身")
                    }
                },
            )
            // 案廿四·财党杀攻身（财滋弱杀之反例）：身弱而财旺党杀克身、财先破有力之印、日主有根非从——非贫即夭
            strength == DayStrength.WEAK && caiS >= GUAN_WANG && guanS >= FOLLOW_MIN_SCORE && yinS >= FOLLOW_MIN_SCORE &&
                partyScore(bazi, Element.entries.first { it.controls == yinEl }) >= 1.5 * yinS &&
                hasBenQiRoot(bazi, Element.entries.first { it.controls == yinEl }) &&
                hasBenQiRoot(bazi, biEl) -> Choice(
                JinBiaoQuYongWay.GONG_SHEN, biEl,
                "财党杀攻身（案廿四·财滋弱杀反例）：财滋弱杀须日主身强用杀；此造身弱——旺财（党力${format(caiS)}）党杀攻身，" +
                    "财先破印（${caiEl.chinese}克${yinEl.chinese}，印根被冲），财复滋杀（官党力${format(guanS)}）贴身克我，身衰撑不住——非贫即夭。" +
                    "不能用杀（杀为忌，怕财滋杀）；坐下有根不属从格（从杀须日主无一丝根气）——只能比劫帮身或印化杀，而印已破难恃，" +
                    "取比劫（${biEl.chinese}）敌杀撑身；印透之运能化杀稍安（然身太衰，重病难救）",
            )
            // 案六十二·身旺无依：印三会得局而克耗泄全废（官被化尽〔官不透而根被泄化〕、财不载无根、
            // 食伤本气根随局化尽）——「一无所用了，这种的才叫身旺无依」：喜用没有力量——一事无成；
            // 走好大运得温饱，走差大运穷得要死（帮不起来论：原局坏得太重，好运也只能帮到倒数第二）
            strength == DayStrength.STRONG && shenWangWuYi -> Choice(
                JinBiaoQuYongWay.SHEN_WANG_WU_YI, caiEl,
                "**身旺无依（案六十二）**：印三会得局贴身生——日主旺极；取用必须克耗泄：官被化尽（根被泄化、全化成木——用不了）、财地支不载无通根（木旺财也不行）、食伤之根随局化尽——**克耗泄一无所用，才叫身旺无依**。" +
                    "**喜用没有力量——一事无成**（找什么工作都干不好：他也有机会，就是干不好）；走好大运得**温饱**，走差大运**穷得要死**。" +
                    "**帮不起来论**：不是用神衰走好运就行——得看能不能帮起来、究竟坏到什么程度；原局被化太重，好运也只能帮到倒数第二（差生、小学五年级毕业——财富温饱打工）。" +
                    "子星仍取财（虚透没被比劫克——生育能力在：贫富与子嗣两轨，走财得根之运得儿子）",
            )
            // 案一百二十四·有力就能食神制杀——病在午火（财生杀旺）、药在子水（先于「杀重用印」：原局作用方式即食伤制杀）
            // 案一百二十六·坐下暗印——用印（先于身弱「伤官用劫」：印藏日支亦为有印——印能护官、吉神深藏）
            anYin126JieGou -> Choice(JinBiaoQuYongWay.FU_SHEN, yinEl, anYin126Basis(cb))
            // 案一百二十七·财官旺·比劫硬扛——身弱用劫（先于身弱通则：文案整体换为「硬扛之气势·当兵」）
            yk127.ok -> Choice(JinBiaoQuYongWay.SHANG_YONG_JIE, biEl, yingKang127Basis(cb, yk127, gender))
            youLiShiZhi124 -> Choice(JinBiaoQuYongWay.ZHI_SHA, shiEl, youLiShiZhi124Basis(cb))
            // 案一百二十三·金有力而基本平衡、水旺克火而火无源——用官喜帮火（先于身弱「食神制杀」与身强「制杀」）
            yongGuan123 -> Choice(JinBiaoQuYongWay.PEI_CAI, guanEl, yongGuan123Basis(cb))
            // 第四讲·正格五理③／⑥＋案三十食神制杀＋基线：身弱取用
            strength == DayStrength.WEAK -> when {
                // 案一百一十三·食伤泄过为病（先于食制）：官杀当令旺克我、食伤三合局全泄体（有了寅午戌就稍微过了——克泄交加）；
                // 身弱无印而日主有根（不算弱极）——身强方可制之以威，此则反以泄日主为病——不取食制
                // （对勘案卅一/卅二：无火局、只有寅与午则制杀以威没问题），要来印星补我日主之气（原局不现以虚神论）
                guoXieQiuYin113 -> Choice(
                    JinBiaoQuYongWay.GUO_XIE_QIU_YIN, yinEl,
                    guoXie113Basis(cb),
                )
                // 食神制杀格（案三十/卅一/卅二）：日主有根气＝承载得动食神之泄；
                // 根气＝本气根，或藏干通根≥3（案三十），或【无印而杀极旺（≥10）时藏根≥2亦通根得气——退而求其次】（案卅二）。
                // 身弱食神制杀首先考虑有没有印：最好用印化之以德；命局无印只有食神——退而求其次食神制杀（身弱肯定没有用印厉害）。
                // 无根气而食神强制＝克泄交加，更穷困比普通人还差（贫夭负债）。
                // 案四十四戒：坐下即印（众杀混行一仁可化之局）——归杀重用印，不归食制
                bazi.dayPillar.branch.element != yinEl &&
                    // 案六十九·食制以威建禄臂：官旺贴身而日主月令建禄（根强拿得起刀）＋食伤有本气支
                    // （午一位制杀卫护身体——虽有点泄，究竟泄比克强：食神制杀有格局，出身大富）
                (guanS >= GUAN_WANG && (shiS >= FOLLOW_MIN_SCORE ||
                    (bazi.monthPillar.branch.element == biEl && hasBenQiRoot(bazi, shiEl))) && shiS * 1.25 < guanS && (yinS < FOLLOW_MIN_SCORE || luGen) && shiS < 6.0 &&
                    bazi.stems.filterIndexed { i, s -> i != 2 && s.element == shiEl }.none { sg ->
                        val p = GAN_HE[sg]
                        p != null && p.element == guanEl && bazi.stems.any { it == p }
                    } && // 案卅七：食干被官干合住——食在合中不能制杀
                    (hasBenQiRoot(bazi, biEl) ||
                        bazi.branches.count { br -> branchHiddenStems.getValue(br).any { it.element == biEl } } >= 3)) ||
                    tuiErQici -> Choice(
                    JinBiaoQuYongWay.ZHI_SHA, shiEl,
                    buildString {
                        val benQiRoots = bazi.branches.filter { it.element == biEl }
                        val cangRoots = bazi.branches.filter { br -> br.element != biEl && branchHiddenStems.getValue(br).any { it.element == biEl } }
                        append("食神制杀格（案卅一·弱而根气足）：杀党力${format(guanS)}旺、食伤党力${format(shiS)}有力、妙在无${yinEl.chinese}（印不夺食）——用神即食神，印运夺食不可以")
                        if (bazi.hourPillar.branch.element == biEl) append("；时支${bazi.hourPillar.branch.chinese}本气根——气贯生时（坐下与气贯生时对日主帮助都是很大的），通根有力足以食神制杀")
                        if (benQiRoots.isNotEmpty() && bazi.hourPillar.branch.element != biEl) {
                            append("；日主通根${benQiRoots.joinToString("") { it.chinese }}（本气根有力）")
                        }
                        if (cangRoots.size >= 3) append("；日主藏干通根三支（${cangRoots.joinToString("") { it.chinese }}）——有根气；辰乃木之余气——一将当关，群凶自败")
                        if (yinS == 0.0 && guanS >= 10.0 && cangRoots.size >= 2) {
                            append("；身弱食神制杀首先考虑有没有印——最好用印化之以德；命局无印只有食神，日主通根得气（${cangRoots.joinToString("") { it.chinese }}库根通根），退而求其次也可用食神制杀——身弱肯定没有用印的厉害；库土毕竟泄身，身弱而杀旺赖食制卫身")
                        }
                        // 案四十七：制杀太过兼日主过泄（食制支亦录——食有力泄重而官杀虚）——一定要走印运：护杀＋帮身
                        if (guanS > 0.0 && shiS >= 6.0) {
                            append("；食伤得局泄重——明面制杀太过（${guanEl.chinese}虚）兼日主过泄：一定要走印运，一是护杀二是帮身")
                        }
                        if (tuiErQici) {
                            append("；此命不是克就是泄、身无帮扶只有小通根——食制不如印化层次高（若换得帮身之支则食制反高）；日主太衰不能弃之于顾：**印运反是好运——大运补起印来化杀帮身（本科、得财之属）**；若连通根得气都没有，就叫以食伤强制——贫夭的命".replace("**", ""))
                        }
                        if (EarthlyBranch.CHEN in bazi.branches && EarthlyBranch.XU in bazi.branches) {
                            append("；辰戌一冲土越来越旺、藏金暗相砍木——余气根等于没了（幸有${if (bazi.hourPillar.branch.element == biEl) "时支寅木气贯生时" else "本气根"}，根气不毁）")
                        }
                        append("；干头之印行南方火力减（终究差一点），火地支之运帮起食伤——食神制杀得力")
                        if (luGen) {
                            append("；日主之根为禄（${luZhi.chinese}＝${dayMaster.chinese}之禄）——禄根不克泄日主，异于库根之泄身（案卅二/卅三）：玄机正在根之质性——为什么有的命走印运好、有的走别的运好，原因就在这里。禄根型食制层次高：只要把食伤帮起来、身偏弱再把自身补起来——帮食（${shiEl.chinese}）兼帮身（${biEl.chinese}）俱吉，这种的食神制杀是要做官的")
                        }
                        append("；条件铁律：日主必须有根气——无根气还用食神强制官杀，叫克泄交加，更差更穷困，比普通人还要差（贫夭负债）")
                        // 案六十八·去火存金＋身弱财大负债论
                        if (dayEl == Element.FIRE && bazi.branches.any { it == EarthlyBranch.HAI } &&
                            bazi.branches.any { it == EarthlyBranch.SI } &&
                            (EarthlyBranch.CHOU in bazi.branches || EarthlyBranch.YOU in bazi.branches)
                        ) {
                            append("；**亥巳逢冲＝去火存金**（水把火冲去、金独存——火根转金根）＋巳丑拱金／巳酉化金——**金旺财得局：旺中变弱（身弱财旺、财杀得局）**。**身弱财大＝负债论：财星有多旺，就要负多大的债**（财星弱点还行——贪欲小平台小，欠一千两千一万两万；财星特别大——数额巨大的负债）——财星生杀攻身，在财上出事，不能有钱、要负债（中年西方金运即是）")
                        }
                    },
                )
                // 案五十九戒：印须有力（透干／本气根／半合拱局之势）方可化杀——孤藏之印（戌中辛之类）不算有印，
                // 身衰无印则比劫扛（用神比劫）——「如果没有印，比劫也能当儿子看」；
                // 案十三亥未拱木（印局会意待运补成）仍以印化
                // 案一百零三·印重为病用财坏印（水多木漂型：身弱不受生——印得局≥8为病，财透两根为药）：
                // 印旺要用财来坏印——不论身强身弱（寒木向阳之暖只是锦上添花非命根子）
                geJuZu103 -> Choice(
                    JinBiaoQuYongWay.HUAI_YIN, caiEl,
                    buildString {
                        append("**印重为病·用财坏印（案一百零三）**：${monthBranch.chinese}月${yinEl.chinese}印得局（党力${format(yinS)}——亥子丑北方之属）肯定印旺——自学八格：**印旺要用的配置就是财来坏印**；日主${biEl.chinese}无本气根（不受生——水多木漂之象），${caiEl.chinese}财**通两根有力还是能克${yinEl.chinese}的——只能用财：财星有力就是个好命**")
                        append("。**寒木向阳——锦上添花**：${shiEl.chinese}生${caiEl.chinese}（财有源头）兼暖局——**有火当局长、无火（午换戌之属）也当正科：半级之差——不是没有火就不行**；**暖不是命局的命根子**（喜欢暖但不依赖暖——土有力自立；对勘案六三「一切以寒暖为规范」——那是命局全靠调候之局）。**哪有去暖的？没听说过的东西不要自己搞开发——问题的关键点在于用神${caiEl.chinese}**")
                        append("。**木运之辨（护卫链）**：不怕木运——**火护卫${caiEl.chinese}**（木生火缓解）＋${caiEl.chinese}实两根经克（来木被克一下也不要紧，况且有${shiEl.chinese}生他）；然木运肯定不如火土之运；**若${caiEl.chinese}虚走木运就完了——${shiEl.chinese}${caiEl.chinese}最起码得有一个有强根**（都虚则壬寅运〔天干${yinEl.chinese}克${shiEl.chinese}＋地支寅亥化木〕就完了——没有强根，配合也白搭）；**午火主要还是${caiEl.chinese}财之通根**（火之禄更是财之根——一身二任）")
                    },
                )
                // 案九十九·一制一化有情格（待授清单收束）：身弱杀旺而伤官透干制杀＋印透干化杀——
                // 一制一化有情即有格局（丁克庚＋壬化申）；**制化两干离得远就有情**（隔位不合绊）；
                // 若贴着（相邻相合）则两神互绊——没层次。丁弱无妨——在于情分（通根身库即立）。
                guanS >= GUAN_WANG && strength == DayStrength.WEAK && !ruoJi &&
                    yinS >= FOLLOW_MIN_SCORE &&
                    // 案十一戒：官星合日主（甲己合——官向我合有情于我）走通关清命线，非制化结构
                    bazi.stems.none { s -> s != dayMaster && s.element == guanEl && GAN_HE[s] == dayMaster } &&
                    bazi.stems.anyIndexed { i, s -> i != 2 && s.element == shiEl } &&
                    bazi.stems.anyIndexed { i, s -> i != 2 && s.element == yinEl } &&
                    bazi.stems.anyIndexed { i, s -> i != 2 && s.element == guanEl } &&
                    run {
                        val shiIdxs99 = bazi.stems.indices.filter { it != 2 && bazi.stems[it].element == shiEl }
                        val yinIdxs99 = bazi.stems.indices.filter { it != 2 && bazi.stems[it].element == yinEl }
                        // 化神（印干）与制神（食伤干）皆不相邻——离得远就有情（贴着则互绊没层次）
                        yinIdxs99.all { yi -> shiIdxs99.all { si -> kotlin.math.abs(si - yi) > 1 } }
                    } -> Choice(
                    JinBiaoQuYongWay.YI_ZHI_YI_HUA, yinEl,
                    buildString {
                        append("一制一化有情格（案九十九）：${biEl.chinese}生${monthBranch.chinese}月财杀力量大（${caiEl.chinese}党力${format(caiS)}、${guanEl.chinese}杀党力${format(guanS)}——还有${caiEl.chinese}坏印〔${if (EarthlyBranch.XU in bazi.branches && yinEl == Element.WATER && EarthlyBranch.HAI in bazi.branches) "戌克亥" else "财星克印"}〕${if (EarthlyBranch.CHEN in bazi.branches && EarthlyBranch.XU in bazi.branches && biEl == Element.WOOD) "，辰戌冲暗地里把木根坎了一下" else ""}），日主${biEl.chinese}弱；然**日主通根身库**＋**${monthBranch.chinese}月${biEl.chinese}进气**（${biEl.chinese}最怕之方将过、物极必反——进气非无敌，只提供力量气数；要不在${monthBranch.chinese}月进气，${if (EarthlyBranch.CHEN in bazi.branches && EarthlyBranch.XU in bazi.branches && biEl == Element.WOOD) "辰戌一冲把根暗地里坎了之后就丝毫没有根气了——日主之气由${monthBranch.chinese}月进气赋予" else "根被冲坎后就没有根气了"}）——有此气数即可**伤官制杀再配上印：${shiEl.chinese}克${guanEl.chinese}、${yinEl.chinese}化杀，这一制一化有情了**")
                        append("。**离得远就有情**（制化两干隔位——不合绊；或一个在天上制、一个在地下化，都叫一制一化的配合）；**若贴着就不行——就没层次了**。${shiEl.chinese}伤官力量不强也不要紧——**还是一制一化有情，在于情分**（通根身库即立得住；当然旺了更好）；**${shiEl.chinese}在这制一下，总比一个${yinEl.chinese}印星去化这么旺的杀要好**（分担）")
                    },
                )
                // 案九十五戒：过泄为主（食伤力压官杀）——配印档优先，不走杀重用印
                guanS >= FOLLOW_MIN_SCORE && !(guoXieFanRuo && shiS > guanS) &&
                    (bazi.stems.any { it.element == yinEl } || hasBenQiRoot(bazi, yinEl) ||
                    listOf(
                        setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI),
                        setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU),
                        setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN),
                        setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU),
                    ).any { trio -> yinEl == when {
                        trio == setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI) -> Element.WOOD
                        trio == setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU) -> Element.FIRE
                        trio == setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN) -> Element.WATER
                        else -> Element.METAL
                    } && trio.count { it in bazi.branches } >= 2 }) -> Choice(
                    JinBiaoQuYongWay.HUA_SHA, yinEl,
                    buildString {
                        append("杀重用印格（五理③·身衰遇官煞，枭印乃可化）：官党力 ${format(guanS)} 旺克我、身弱——必用印化杀生印出格局。杀重之时要化之以德，不能用食神强制杀：「身弱制杀以威，不若化之以德」")
                        // 案八十五·干多不如支重：比劫透而无根（地支不载）——木生秋天大官杀（异于案七四
                        // 夏飞刃干多有源）；完全指着印星一字定之——财贴克印则官再旺也化不了（经济困难）
                        if (bazi.stems.count { it != dayMaster && it.element == biEl } >= 2 && !hasBenQiRoot(bazi, biEl)) {
                            append("；**干多不如支重（案八十五）**：比劫虽多透而**地支不载**（无根——木生秋天是大官杀，异于案七四夏飞刃有源当令之干多）——只能用印帮身：**完全指着印星一字定之——官再旺没有用**（化官生印「官大坐大官」之说不成立——日主没力量，印被贴克就完了：官杀旺也化不了——经济困难）")
                        }
                        // 案六十七·财贴克印病药论：财干贴克印干（丁贴庚）——财有根有力为原局之病、原局无解：
                        // 走克病之运（北方壬癸）＝有病得药要加分——化官生印很厉害，很大的格局
                        val caiIdxsH = bazi.stems.indices.filter { it != 2 && bazi.stems[it].element == caiEl }
                        val yinIdxsH = bazi.stems.indices.filter { it != 2 && bazi.stems[it].element == yinEl }
                        if (caiIdxsH.isNotEmpty() && yinIdxsH.isNotEmpty() &&
                            caiIdxsH.any { ci -> yinIdxsH.any { yi -> kotlin.math.abs(ci - yi) == 1 } }
                        ) {
                            val keBingEl = Element.entries.first { it.controls == caiEl }
                            append("；**财贴克印（${caiEl.chinese}贴${yinEl.chinese}）——财有根有力（得生），即原局之病，原局无解决之物**：原局不行；**大运好给他解决了就一样行一样好——走${keBingEl.chinese}去病之运（如北方${keBingEl.chinese}地）＝有病得药，要加分：化官生印很厉害，很大的格局**——好运上学习好工作好；往后走生${caiEl.chinese}之运（助病）——不行：肯定破大财，前途不光明。**财富值一定取决于印的强弱、印的力量**（用神印之力）")
                        }
                        // 案九十三·拱财引化坏印：亥未拱财而乙木露干引化（拱局透干则实——财得局）——
                        // 印星为用最不喜欢财星得局来财坏印；财为妻而坏用神——妻起最坏的作用：
                        // 克妻两义（一离婚、一丧妻）；口诀「男坐官星官为妻，官局三合丧贤妻」条件辨析
                        if (gongCaiYinHua) {
                            val gongZhis93 = gongCaiTrio!!.first.filter { it in bazi.branches }.distinct().joinToString("") { it.chinese }
                            append("；**拱财引化·财坏印（案九十三）**：${monthBranch.chinese}月终究一印（然与丑月之印、戌月之印不一样——夏土）${if (luGen) "＋时支${luZhi.chinese}禄" else ""}，本可论旺；然地支${gongZhis93}拱${caiEl.chinese}财局而${caiEl.chinese}透干引化——**拱局透干则实：财得局**（${shiEl.chinese}食伤本旺还会了财局，更兼${guanEl.chinese}官杀克身）——**旺中变弱：印星为用；印星为用，最不喜欢财星得局来财坏印**。**婚姻·财星三合看妻**：以${caiEl.chinese}财为妻，财星三合≠必丧妻——**命中有印，财坏印了（或财党杀了），这时才丧妻**；本案${gongZhis93}拱财坏${monthBranch.chinese}印——**妻子起了最坏的作用（妻子让命主很难受）：一定要克妻——离婚或丧妻两义**。「男坐官星官为妻，官局三合丧贤妻」说的本是**财星党杀为日主所忌**——**不是官局三合就丧贤妻：一定按局里的喜用断**（日主身强有力量，反喜财星党杀，官局三合来增强官杀之力——为好；日主身衰财杀强旺，财杀还这么大力量——才要克妻或离婚）")
                        }
                        if (bazi.dayPillar.branch.element == yinEl) {
                            append("；坐下有印（${bazi.dayPillar.branch.chinese}）——没有此印，这么旺的杀就要克死日主；有坐下印就得了贵：天干杀来克也没关系，坐下印持续生即可。坐下有印就叫有情——别处之气皆向我坐下生拱（杀生印），外界有情就加分，不用管印之旺弱，一样得贵——这叫情分；原命财杀旺，一定要走帮身运才能好")
                            if (guanS >= 10.0) {
                                append("；众杀混行，一仁可化——纵无他印，独坐下一印亦化（官杀全往我坐下来生拱）；克印之运用神被伤——一事无成甚至贫夭，然运一过功能即复，再发富贵")
                            }
                        }
                        if (bazi.branches.contains(EarthlyBranch.CHEN) && bazi.branches.contains(EarthlyBranch.SHEN) &&
                            bazi.branches.indexOf(EarthlyBranch.CHEN) + 1 == bazi.branches.indexOf(EarthlyBranch.SHEN)
                        ) {
                            // 案廿七/案一百之辨：辰申拱水三条件——①辰申相邻②壬水透干引化。
                            // 壬透（案九十九）→辰申拱水（印得拱助）；壬不透（案廿七戊庚甲甲）→会局被夺，
                            // 辰贴申必生金——土生金是谁也改变不了的规矩，不以局论（杀旺取坐下印为用）。
                            if (bazi.stems.any { it != dayMaster && it.element == Element.WATER }) {
                                append("；拱局辩证（案一百）：辰申贴着＋壬水透干引化——**辰申拱水**（供得上水：印得拱助）；若辰申隔位（戌隔之属）——供不了水，**辰土生申金**（土生金助杀）；若壬不透——地支纵拱也拱不动（案廿七会局被夺，辰贴申仍生金）")
                            } else {
                                append("；拱局辩证：辰贴申必生金——土生金是谁也改变不了的规矩，会局被夺不以局论（壬不透拱不动——杀旺取坐下印为用）；寒木向阳——有火时层次更高，但非必需：没有水命主就被旺杀克死，有水即成格，没火一样当官（调候不用刻意考虑）")
                            }
                        }
                        // 案一百·比劫贴合杀而比劫无根无力——从杀合化（乙庚化金），反助其暴：
                        // 辰月杀进气（魁罡之旺）、辰土克亥水（水库仍土——水少则克，财坏印）、
                        // 乙指亥生而亥被克、戌坎辰中乙——乙无力从庚＝合化金；辰戌土生金金旺——
                        // 一个壬水化不了这么旺的杀（对勘案九十九：彼印旺得气＋辰申贴拱水；此杀得气＋比劫被化——位置一变天渊之隔）
                        val biTou100 = bazi.stems.indices.filter { it != 2 && bazi.stems[it].element == biEl }
                        if (biTou100.isNotEmpty() && !hasBenQiRoot(bazi, biEl) &&
                            biTou100.any { i -> GAN_HE[bazi.stems[i]]?.let { p -> bazi.stems.getOrNull(i + 1) == p || bazi.stems.getOrNull(i - 1) == p } == true &&
                                bazi.stems.any { it != dayMaster && it == GAN_HE.getValue(bazi.stems[i]) && it.element == guanEl } } &&
                            ((guanEl == Element.METAL && monthBranch == EarthlyBranch.CHEN) ||
                                (guanEl == Element.FIRE && monthBranch == EarthlyBranch.WEI) ||
                                (guanEl == Element.WATER && monthBranch == EarthlyBranch.CHOU) ||
                                (guanEl == Element.WOOD && monthBranch == EarthlyBranch.XU))
                        ) {
                            append("；**合化从杀·反助其暴（案一百）**：${monthBranch.chinese}月${guanEl.chinese}杀**进气**得气（庚辰之旺——魁罡之属）；${biEl.chinese}比劫无根无力（生${biEl.chinese}之${yinEl.chinese}被${caiEl.chinese}克〔${if (EarthlyBranch.CHEN in bazi.branches && yinEl == Element.WATER && EarthlyBranch.HAI in bazi.branches) "辰土克亥水——水库仍土，没有那么多水就要克水" else "财星克印"}〕${if (EarthlyBranch.CHEN in bazi.branches && EarthlyBranch.XU in bazi.branches && biEl == Element.WOOD) "、戌把辰里乙木坎了" else ""}）——**从了对方＝合化**（${biEl.chinese}${guanEl.chinese}化${guanEl.chinese}）：**反助其暴**。地支${caiEl.chinese}生${guanEl.chinese}——${guanEl.chinese}旺，**指望一个${yinEl.chinese}就化不了这么旺的杀**（对勘案九十九：彼${yinEl.chinese}印旺得气＋辰申贴拱水，此杀得气＋比劫被化金——**位置一变天渊之隔：位置一变力量就变，用生克看八字就是一切看力量**）")
                        }
                        // 案廿八：印为湿土晦火养金——财贴印本是病，然财生官、官生印，土不受木克＝有情（当官富贵）
                        if (yinEl == Element.EARTH && guanEl == Element.FIRE &&
                            (EarthlyBranch.CHEN in bazi.branches || EarthlyBranch.CHOU in bazi.branches)
                        ) {
                            append("；印为湿土（辰）——晦火养金。${caiEl.chinese}贴${yinEl.chinese}本是财坏印之病，然${caiEl.chinese}生${guanEl.chinese}、${guanEl.chinese}生${yinEl.chinese}——这个土就不受木的克了：这就叫有情，有情就要当官富贵。有情豁免：不需虑印之旺衰、亦不虑印化不化得了——符合格局即可，只要印用神不被财破坏；若无有情因素，则须虑印之旺弱——印被破坏即是贫穷命")
                        }
                        append(
                            if (yinS < FOLLOW_MIN_SCORE) "；印弱（党力${format(yinS)}）——印可配官煞，官杀生印为喜"
                            else "；印党力 ${format(yinS)} 得力可化",
                        )
                        if (yinS == 0.0) append("；局中印不现，以虚神论（运岁补入之时为应）")
                        if (shiS >= 6.0) {
                            append("；伤官（食伤）得局（党力${format(shiS)}）泄身过甚——旺中变弱：印两用，一疏其土（克食伤之局）二生日主")
                            append("；局中拱合待成者（如亥未拱木待卯），运支补成印局即用神得局大发")
                        }
                        // 案四十七·制杀太过兼日主过泄：食伤有力泄重而官杀随季节虚（辰月春天木旺土虚）——
                        // 一定要走印运：一是护杀（克食），二是帮身（生身）
                        if (shiS >= GUAN_WANG && guanS >= FOLLOW_MIN_SCORE && shiS * 1.6 >= guanS) {
                            append("；食伤得局（${format(shiS)}）——明面上制杀太过（${guanEl.chinese}官杀虚${if (monthBranch == EarthlyBranch.CHEN) "：辰土虽为根，春天木旺土虚" else ""}）；更患日主过泄（食伤泄体、日主没那么旺）——**一定要走印运：一是护杀（印克食），二是帮身（印生身）**".replace("**", ""))
                        }
                        // 案四十六·制杀太过：需用杀而食伤制之将绝尚有余气——贫夭之基；喜印克食护卫七杀＋生身
                        if (shiS >= 6.0 && guanS < GUAN_WANG && guanS > 0.0) {
                            append("；制杀太过（第九讲）：食伤党力${format(shiS)}重重克制——杀（党力${format(guanS)}）快被克绝、仅存余气（一点气都没有反是去寡之吉，还有点气即制杀太过）——要用印星克食神护卫七杀、其次把日主生起来；终究杀太弱——层次降档：公务员之命而没当官（重者贫夭）")
                        }
                    },
                )
                shiS >= FOLLOW_MIN_SCORE && (bazi.stems.any { it.element == yinEl } || hasBenQiRoot(bazi, yinEl)) -> Choice(
                    JinBiaoQuYongWay.FU_SHEN, yinEl,
                    buildString {
                        append("伤官配印（案五十一）：身弱泄体过（食伤党力${format(shiS)}）——印星制服食伤且生身，两得其用（印党力${format(yinS)}），此为有格局。")
                        append("**伤官配印喜见官**（官能生印——伤官见官＝最好组合，再有个官格局更高）；**不可见财**（用神印无护卫，财来坏印就完了——用神中心论：只考虑印的死活）。运：官运/印运好运，财运坏印庸碌无为不得志。")
                        // 案五十三·丑未冲三层：财坏印于支内＋火余气被灭＋土越冲越旺湿土晦火——旺中变弱
                        if (EarthlyBranch.CHOU in bazi.branches && EarthlyBranch.WEI in bazi.branches) {
                            append("；丑未冲三层（案五十三）：藏干互战——丑中辛克未中乙（**财坏印于支内**）、丑中癸克未中丁（火之余气被灭）＋同气相冲土越冲越旺＋湿土晦火（**火最怕湿土来晦**）——未月火余气之旺基调经地支作用**旺中变弱**；未中印根被冲掉降一点格局，坐下印本气在——一样有格局，行运不违背即好命")
                        }
                        if (shiS >= GUAN_WANG) {
                            append("；**泄体过必须帮身**——比劫虽泄印之气，然帮日主扛食伤之泄（身不倒则印有用武之地）：南方比劫、东方印皆帮身方向，行运不违背即好命（用印显贵多，当官）")
                        }
                        // 案一百零七·伤官配印而印之本气根被财本气支贴冲拔（冲神得食伤生）：
                        // 支神只以冲为重——财坏印拔根；官星若被月令劫住则印失官生——层次高不了
                        if (yinGenBaCai && shiS >= GUAN_WANG) {
                            append("；**财坏印拔根（案一百零七）**：伤官月${shiEl.chinese}生${caiEl.chinese}、${caiEl.chinese}坏印——日主力量弱用印；**支神只以冲为重**：大${caiEl.chinese}有${shiEl.chinese}生力量还是很大，财坏印了——${yinEl.chinese}就全靠${bazi.branches.first { it.element == yinEl }.chinese}根，${caiEl.chinese}把根拔了，就不行了${if (bazi.branches.any { it.element == guanEl } && LIU_CHONG[bazi.branches.first { it.element == guanEl }] == monthBranch) "。本是伤官配印的格局，${guanEl.chinese}本可生火（印喜欢官杀），但是月令${monthBranch.chinese}把它劫住了" else ""}——**这样层次就高不了了：即使走的运再好，也只能是个小康，或者说不如小康**")
                        }
                        // 案九十四·比劫当令过泄配印＋异性克不拔根（学历章开讲）：
                        // 丑月土寒终究是土（虚一点而终是实——辰才是虚土）；金多过泄翻身弱、取用露出来的印。
                        // 「支神只以冲为重，刑与穿兮动不动」：印根被异性克不拔——立得住，层次高（能当官发大财）；
                        // 酉换申（同性贴冲）根拔——纵走好生印运也帮得起来，只是成就不如（老百姓里算好的）。
                        if (guoXieFanRuo) {
                            append("；**比劫当令而食伤过泄（案九十四/九十五）**：${monthBranch.chinese}月${biEl.chinese}寒——终究是${biEl.chinese}（比未戌月虚了一点，然终是当令之${biEl.chinese}；**${EarthlyBranch.CHEN.chinese}才是虚${biEl.chinese}**——两说之辨），本已有力量；然${shiEl.chinese}多（党力${format(shiS)}两透）——**日主肯定就过泄了**：翻身弱，取用帮身——用神露出来的${yinEl.chinese}，**命主的一切就要由${yinEl.chinese}来决定**")
                            val yinZhu94 = bazi.stems.indices.firstOrNull { it != 2 && bazi.stems[it].element == yinEl }
                            val yinGenZhi94 = yinZhu94?.let { bazi.branches[it] }
                            val genEl94 = yinGenZhi94?.element
                            if (genEl94 != null && genEl94.generates == yinEl) {
                                val keGenEl94 = Element.entries.first { it.controls == genEl94 }
                                val keGenZhis94 = bazi.branches.filter { it.element == keGenEl94 }
                                val chongZhi94 = LIU_CHONG[yinGenZhi94]
                                if (keGenZhis94.isNotEmpty() && keGenZhis94.all { (it.ordinal % 2) != (yinGenZhi94.ordinal % 2) } && chongZhi94 !in bazi.branches) {
                                    append("。${yinEl.chinese}坐${yinGenZhi94.chinese}${genEl94.chinese}——有根；地支虽皆${keGenEl94.chinese}克${yinGenZhi94.chinese}，然**异性之克不至于把${yinEl.chinese}的根拔了**（${keGenZhis94.joinToString("") { it.chinese }}是小刀、${yinGenZhi94.chinese}是树——小刀刻不断树）。**支神只以冲为重，刑与穿兮动不动**：${keGenZhis94.joinToString("") { it.chinese }}换成${chongZhi94?.chinese ?: ""}（同性贴冲），${yinEl.chinese}的根就拔了——这个人就不行了。**用神${yinEl.chinese}立得住：层次一下就高了起来——能当官发大财**；纵是根被拔了，走好${genEl94.chinese}${yinEl.chinese}运的时候也能把${yinEl.chinese}帮起来，只不过成就不如现在这个高——**只能是在老百姓里面算好的**")
                                } else if (chongZhi94 != null && chongZhi94 in bazi.branches) {
                                    append("。${yinEl.chinese}坐${yinGenZhi94.chinese}${genEl94.chinese}——${chongZhi94.chinese}贴冲${yinGenZhi94.chinese}（${keGenZhis94.joinToString("") { it.chinese }}党力大）：**${genEl94.chinese}被拔了——${yinEl.chinese}的源头直接被拔，虚${yinEl.chinese}就完全指着大运了**。**支神只以冲为重，刑与穿兮动不动**——比异性克重得多：**${yinEl.chinese}是用神、${genEl94.chinese}是喜神——喜神的力量上不一样，层次就差了好多**（同样不错的命，不如案九十四能当官）。然${yinEl.chinese}火露着——**不一无是处：戌丁两运还是发了财的**（用神之库得根、用神透干之运）")
                                }
                            }
                        }
                        // 案五十七·财得局坏印：金局坏印——财有多旺他就有多穷；印运温饱、食伤生财运身无分文负债
                        if (caiS >= GUAN_WANG && hasBenQiRoot(bazi, caiEl)) {
                            append("；**财星得局坏印（案五十七）**：${caiEl.chinese}得局（辰酉化金＋巳酉丑支全三合）坏印——**财越旺他就越穷：财有多旺他就有多穷**（用印局财为夺印之忌；食伤生财＝给忌神添粮）；运：印运把印补起来**还能温饱**，食伤生财之运**身无分文、负债**——清枯之命（好运仅温饱）")
                        }
                        // 案五十三·忌神潜伏律：忌神透干不贴克（干支异途）而藏根党旺——仍为大忌；去忌神之运最好
                        val keYongStems = bazi.stems.filterIndexed { i, s -> i != 2 && s.element.controls == yinEl }
                        if (keYongStems.isNotEmpty() && !hasBenQiRoot(bazi, Element.entries.first { it.controls == yinEl })) {
                            append("；**忌神潜伏律**：${keYongStems.joinToString("") { it.chinese }}透干虽不贴克用神之字（干支异途——干不克支），然其党旺仍为大忌——流年透用神时忌神乘隙克印；**去忌神之运最好**（运干克去透干忌神——丁卯丙寅之类，当官）")
                        }
                        append("（对勘：案四十九身旺喜官、案五十身旺枭夺食忌官、案五十一身弱配印喜官、案五十二身弱用劫忌官——见官四辨全）")
                    },
                )
                else -> {
                    val useYin = yinS >= biS && (bazi.stems.any { it.element == yinEl } || hasBenQiRoot(bazi, yinEl) || shiS < FOLLOW_MIN_SCORE)
                    // 案五十六·三会化劫：三会成局而局非比劫之党——局内比劫本气根随局走（把比劫都化了）
                    val biGenBeiHua = SAN_HUI_JU.firstOrNull { (bs, _) -> bs.all { it in bazi.branches } }?.let { (bs, el) ->
                        el != biEl && bazi.branches.any { it.element == biEl && it in bs }
                    } == true
                    Choice(
                        if (useYin) JinBiaoQuYongWay.FU_SHEN else JinBiaoQuYongWay.SHANG_YONG_JIE,
                        if (useYin) yinEl else biEl,
                        if (useYin) "身弱扶身：印绶党力 ${format(yinS)}、比劫党力 ${format(biS)}——取印绶（生身）为用（有力者优先）" else
                            if (keXieJiaoJia120) keXieJiaoJia120Basis(cb, shiS, yinS) else shangYongJieBasis(cb, shiS, yinS, biGenBeiHua, ganDuoBuZai117),
                    )
                }
            }
            // 第四讲·正格五理①②④⑥＋第五讲去寡＋基线：身强取用
            strength == DayStrength.STRONG -> when {
                // 第五讲·去寡（案七/案八定则）：三条件差一点都不对，必须全部满足——
                // ①日主强旺；②官杀真无根气（地支藏干全无官杀——天干再透两见亦寡）且财不生官（财无透干无本气根）；③有食伤。
                // 案八之诫：官杀地支藏干有字（丑中癸）或财透干来生（庚）——条件②破，即非去寡，转用官。
                // 案一百零二·枭夺食用财（三选一之筛）：身旺＋官弱不任（被盖头/被局克）＋食伤透而坐支无源被枭夺净
                // ＋财透有气（生源成局）——只能用财（财轻通门户：不旺但绝对有气可用）。
                // 对勘案九：壬坐申通源（食伤极有力夺不净）——仍用食泄秀。
                strength == DayStrength.STRONG && !ruoJi && !wangJi && !qiangJi &&
                    bazi.stems.anyIndexed { i, s -> i != 2 && s.element == shiEl } &&
                    bazi.stems.anyIndexed { i, s -> i != 2 && s.element == yinEl && s.element.controls == shiEl } &&
                    yinS >= YIN_HUAI &&
                    bazi.stems.anyIndexed { i, s -> i != 2 && s.element == shiEl && branchHiddenStems.getValue(bazi.branches[i]).none { it.element == shiEl || it.element.generates == shiEl } } &&
                    bazi.stems.anyIndexed { i, s -> i != 2 && s.element == caiEl } &&
                    bazi.branches.any { br -> branchHiddenStems.getValue(br).any { it.element == caiEl } } &&
                    (guanS < GUAN_WANG || bazi.stems.anyIndexed { i, s -> i != 2 && s.element == shiEl && bazi.branches[i].element == guanEl }) -> Choice(
                    JinBiaoQuYongWay.SHI_CAI_YONG_CAI, caiEl,
                    buildString {
                        append("身旺三选一之筛·枭夺食用财（案一百零二）：${biEl.chinese}生${monthBranch.chinese}月透${yinEl.chinese}坐禄——**身旺无疑**；身旺要么用食伤要么用官杀或者财（**用神＝行业：做什么利于用神就发财——「男怕选错行，女怕嫁错郎」〔女命拿用神当夫——嫁错郎＝选错用神〕**）。先选官——${guanEl.chinese}官被${shiEl.chinese}盖头${if (sanHeJuEl != null && sanHeJuEl == shiEl) "＋地支${sanHeJuEl.chinese}局力大（子远隔在边上、没有中间力量大，但毕竟也是个局）" else ""}——**官杀不行**；选${shiEl.chinese}？虽旺然${yinEl.chinese}土**老是捣乱——枭神夺食**（食坐${bazi.stems.indices.first { it != 2 && bazi.stems[it].element == shiEl }.let { bazi.branches[it].chinese }}无源夺得净）——**不开心也不能用了**；**只能用${caiEl.chinese}财**：坐${bazi.branches[0].chinese}还可以（${caiEl.chinese}没有那么旺，但**绝对有气，可用的**——地支${shiEl.chinese}局生财，**用财没有问题**〔财轻通门户——财星有情，不在乎旺衰〕）")
                        append("。**用神无力×一辈子悖运＝没有层次**：本案用神本弱（${monthBranch.chinese}月${caiEl.chinese}退气＋${monthBranch.chinese}多少克${shiEl.chinese}），又走了六十年${caiEl.chinese}休囚之地（一天好运都没有）——**就没有层次了**；用神无力走三十年好运也能攒下钱（会过日子也是小康）；**要是${EarthlyBranch.MAO.chinese}（换${monthBranch.chinese}）用神非常强——走悖运日子也过得不错**；若走东方运财起好作用——**财轻通门户应期：运不违背就有钱，违背（${caiEl.chinese}死囚之地）就不行**")
                        append("。**子女·用神即儿**：**肯定有儿子**（用神${caiEl.chinese}就是他的儿子——身旺财为子）；然命主走一辈子不利于儿之运——**他儿子也不行**（事业差方方面面都差——用神就是儿则运影响儿子；有的人儿子非用神，那就不影响他儿子）。**宫位因果**：${caiEl.chinese}占年宫克${yinEl.chinese}——爷爷这辈还是可以的；然因果上爷爷做了不正确的事（钱挣了带不走——到命主辈一败而尽）；命主哪月哪天生由其父${yinEl.chinese}（月柱父母宫）决定——父做事不好，老天爷看着，让他产生因果不利子孙。**能留给子孙后代的只有德行**")
                        append("。**出身细则**：年月有好东西还须**不被劫**——好物（如年上${caiEl.chinese}）若在年月被比劫劫住，给不到日主——**有好东西出身也不好**")
                    },
                )
                // 案九十六放宽：财虽在（有根）而**群劫争财**（比劫党力≥2倍财）——财已用不了（不用之财尽可劫），
                // 不能滋官破寡——仍寡（对勘案八：庚财透而非群劫——破寡转用官；案廿五：壬壬透而非群劫——仍财滋弱杀）。
                guanS > 0.0 &&
                    bazi.branches.none { br -> branchHiddenStems.getValue(br).any { it.element == guanEl } } &&
                    (bazi.stems.none { it.element == caiEl } && !hasBenQiRoot(bazi, caiEl) || biS >= 2.0 * caiS) &&
                    shiS >= FOLLOW_MIN_SCORE -> Choice(
                    JinBiaoQuYongWay.QU_GUA, shiEl,
                    buildString {
                        append("推命经第二段·去寡（案七）：看力量首先看地支、地支主宰——日主通根得气身旺；")
                        append("官杀（党力${format(guanS)}，天干再透两见）地支藏干全无——不载即无根气，无用之官，「不用之官尽可去」；")
                        append("财无透干无本气根不生官（党力${format(caiS)}）、食伤有力（党力${format(shiS)}）——去官发富贵，用神取食伤（去官之星）；")
                        append("违背官杀之运好运、坏官越重越好；顺着官杀（生官／官星得地得运）则一世无成")
                        // 案九十六·强众敌寡（群劫争财臂）：财在而不可用
                        if (bazi.stems.none { it.element == caiEl } && !hasBenQiRoot(bazi, caiEl)) {
                            // 财全无——案七型，无注
                        } else {
                            append("；**强众敌寡（案九十六）**：${guanEl.chinese}在${monthBranch.chinese}月**不得气**（地支无根——若${EarthlyBranch.CHEN.chinese}挪到月令，${guanEl.chinese}进气了，那就不行了）——强众敌寡成立，这个杀不但克不了日主，反而生${yinEl.chinese}了，转来转去最后还是${biEl.chinese}旺，${biEl.chinese}旺就要${shiEl.chinese}泄——**去官杀之寡**。**财已经用不了了——群劫争财（比劫${format(biS)}对财${format(caiS)}），只能用食伤泄秀**：「不用之财尽可劫，不用之官尽可伤；可用之官不可伤，可用之财不可劫」——这个财被劫没事。**顺逆之机需理会**：强旺到极点了，只能顺着${biEl.chinese}，不能逆着去克${biEl.chinese}——「要与人间开聋聩，顺逆之机须理会」。只要${guanEl.chinese}得根就不好；上来就走西方${guanEl.chinese}运——层次就非常低了（大运越是帮七杀的越不行）；走帮用神${shiEl.chinese}之运就好——成格了，大运不违背就是个有层次的人")
                        }
                    },
                )
                // 食神制杀格（五理④＋第七讲条件一）：官有本气根之真旺、财不滋官、食伤有力——
                // 且【日主必须有根气】：食神泄体，无根无气承载不了食神之泄，食神制杀反贫夭负债；得根气则制杀得富贵。
                // 弱中变旺局之官已被泄克转衰——衰官不制而用之（案十）
                // 食神制杀格（五理④＋条件一＋案四十八）：官有本气根之真旺、财不滋官（无财或财不透干）、食伤有力——
                // 且【日主必须有根气】：食伤泄体，无根无气承载不了食神之泄（食神制杀反贫夭负债）；得根气则制杀得富贵。
                // 根气三途：本气根／藏干通根≥3（案三十）／坐下印得局压杀（案四十八：印6.5对杀4.5——印生身有依）。
                // 案九十二戒：月令印当令（化官生印）＋比劫有本气根（秋水通源）——官之气尽泄于印、
                // 官弱不任食制：必须得用官（官为用神被食伤克＝病——层次低无学历财富值低）；
                // 案四十八比劫无本气根（印虽得局身旺然杀真旺可制）各保其位
                !(bazi.monthPillar.branch.element == yinEl && hasBenQiRoot(bazi, biEl)) &&
                !ruoZhongBianWang && guanS >= GUAN_WANG && hasBenQiRoot(bazi, guanEl) &&
                    (caiS < FOLLOW_MIN_SCORE || bazi.stems.none { it.element == caiEl }) &&
                    // 案八十二戒：印干贴克食干而官透有力——印看住食伤不使犯官（把官给解放了）——
                    // 身旺是用官的（有印护官制食——不取食制；对勘案五十：官全无而财护食——仍用食）
                    !(bazi.stems.any { it != dayMaster && it.element == yinEl } &&
                        bazi.stems.any { it != dayMaster && it.element == guanEl } &&
                        bazi.stems.indices.any { i -> i != 2 && bazi.stems[i].element == shiEl &&
                            ((i - 1 in bazi.stems.indices && bazi.stems[i - 1].element == yinEl) ||
                                (i + 1 in bazi.stems.indices && bazi.stems[i + 1].element == yinEl)) }) &&
                    (shiS >= FOLLOW_MIN_SCORE || (tengLuo && shiS > 0.0)) &&
                    (hasBenQiRoot(bazi, biEl) || tengLuo || (bazi.dayPillar.branch.element == yinEl && yinS > guanS)) -> Choice(
                    JinBiaoQuYongWay.ZHI_SHA, shiEl,
                    buildString {
                        append("食神制杀格（案四十八·身旺杀旺可制）：官党力 ${format(guanS)} 旺（有本气根）、财不滋官（财党力${format(caiS)}）——必用食伤制杀制官（官不一定不可以制）；食伤党力 ${format(shiS)}。")
                        if (tengLuo) {
                            append("。**藤萝系甲足以食伤制杀（案一百零四）**：杀得局官杀旺，按理不能用食伤制官杀——**克泄交加不如用印生身**（克泄交加＝没层次的命——一般人绝对这么看）；然基础知识扎实**知藤萝系甲可春可秋：有个甲木在，乙木就身旺了，秋天也没事——庚克甲只是一个病，乙木藤萝系甲足以用食伤制金，把病克了就好**。**反事实**：要是日主甲木、月干换成乙木——那就是没有层次的命了（比一般人还差——越走午火越穷困潦倒，过泄了）：**这是乙木的特性**。**病神取象（坤造）**：命局的病就是官杀${guanEl.chinese}＝男人——命主要干的事就是**排斥这个杀→排斥男人**（现实中必有这样的事——同性恋而充当男性角色〔制杀者自居阳位〕）。**婚期变通**：结婚应期五大法则没有错，但得根据实际情况——**她反感男人，最起码是要晚婚的**（还用正常婚姻法则去看＝刻舟求剑——得根据原局的道理：**最忌的就是男人**）")
                        }
                        append("食神制杀条件第一条：日主必须有根气（印得局生身亦是依）——无根无气者承载不了食神之泄（食神制杀反贫夭、负债）；日主得根气，承载得动食神之泄，食神制杀得富贵。")
                        append("格局高低看杀之强弱存续：杀旺可制不贫夭——官贵之格（官二代、处长之子之属）；杀之根被拔克绝（制杀太过——子午冲根之属）则落普通人之列")
                        // 案一百零一·两杀叠透通根＋进气＋两戌实根：地支制杀也好；原局四方有力——好命不怕大运
                        val benQiGen101 = bazi.branches.count { it.element == biEl }
                        if (benQiGen101 >= 2 && hasBenQiRoot(bazi, shiEl) &&
                            ((guanEl == Element.WOOD && monthBranch == EarthlyBranch.XU) ||
                                (guanEl == Element.FIRE && monthBranch == EarthlyBranch.CHOU) ||
                                (guanEl == Element.METAL && monthBranch == EarthlyBranch.CHEN) ||
                                (guanEl == Element.WATER && monthBranch == EarthlyBranch.WEI)) &&
                            LIU_CHONG[bazi.dayPillar.branch]?.let { it in bazi.branches && it.element == shiEl } == true
                        ) {
                            append("。**食神制杀·原局四方有力（案一百零一）**：${guanEl.chinese}杀叠透通根${if (bazi.branches.any { it.element == guanEl }) "寅" else ""}——有力；**${guanEl.chinese}坐${monthBranch.chinese}月进气——更有力量，怎么说也相当于坐一个辰**（进气之力≈余气库之根）；杀旺贴身克我，然**日主通根${benQiGen101}个${biEl.chinese}实土——足以食神制杀（这不属于身弱——两个实的土）**：这人就有层次了。**地支制杀也好**（${LIU_CHONG[bazi.dayPillar.branch]!!.chinese}贴冲${bazi.dayPillar.branch.chinese}＝食冲杀之根——地支制杀成立；口诀「**若在艮坤，怕冲宜静**」——戊坐寅／申不喜冲战，**然杀旺必须要制杀：口诀让位于生克力量**）。**原局四方有力——好命就不怕大运，走什么运都没事**：走水运化了${shiEl.chinese}金——有${biEl.chinese}生着也有劲；走寅卯${guanEl.chinese}旺——金也是有力的：**天生的富贵命**（若没有两${biEl.chinese}实根——完全是杀旺，再走木运就不行了；原局非常有力，走什么都没问题）。**应期入档：子水运庚子年发大财**（食神透干＋财星到位——抖音卖体育用品发财千万）。**取象：${shiEl.chinese}金是经络——${shiEl.chinese}${guanEl.chinese}与寅申之间的关系＝瑜伽／健身／体育之属**（做瑜伽健身教练的多有寅申冲——如木生火之服装理发）")
                        }
                    },
                )
                // 第十讲·伤官喜官（案四十九）：身旺伤官生财、财被比劫夺、官克比劫救财——用神转官，
                // 伤官格可以见官（非祸百端而喜官）——破「金水伤官喜见官、土金伤官忌见官」之市面口诀，一切以喜用定。
                strength == DayStrength.STRONG && !ruoJi && !wangJi &&
                    shiS >= FOLLOW_MIN_SCORE &&
                    bazi.stems.any { it.element == caiEl } &&
                    bazi.stems.filterIndexed { i, s -> i != 2 && s.element == biEl }.isNotEmpty() &&
                    guanS >= FOLLOW_MIN_SCORE && hasBenQiRoot(bazi, guanEl) -> Choice(
                    JinBiaoQuYongWay.SHANG_JIAN_GUAN, guanEl,
                    buildString {
                        append("伤官喜官·克劫护财（案四十九）：身旺（土厚比劫通根透干），${shiEl.chinese}食伤秉令生${caiEl.chinese}财（食伤生财，财为喜用）——最怕比劫：${caiEl.chinese}财被${biEl.chinese}比劫所夺（差命底子）。螳螂捕蝉黄雀在后——${guanEl.chinese}官有力（${guanS.let { format(it) }}）克${biEl.chinese}比劫、解放${caiEl.chinese}财：**用神转官——伤官格可以见官，非祸百端而喜官**。")
                        append("市面口诀「金水伤官喜见官、土金伤官忌见官」不成立——土金伤官反喜官，一切以喜用定（用神中心论：一切围绕用神说事，不考虑伤官死活、财的死活，闲神关系不虑）")
                        append("；官根被食伤冲克＝病（如卯酉冲），泄食生官之运（${shiEl.chinese}生${caiEl.chinese}生${guanEl.chinese}）＝药——大运走助官泄金生木就要当官显贵；帮${shiEl.chinese}（食伤）克用神之运凶")
                    },
                )
                // 案一百一十六·羊刃自旺承载食伤生财（先于财滋弱杀：官杀露于禄上为病、不为用）——用财
                yangRenZiWang116 -> Choice(
                    JinBiaoQuYongWay.SHI_CAI_YONG_CAI, caiEl,
                    yangRen116Basis(cb),
                )
                // 案一百一十四·身旺用暗处的财（先于财滋弱杀）：杀露而坐食伤——不行；食伤被坐下印克——不能为用；
                // 财藏食伤支中未破——用财喜食伤；印为忌
                anCai114 -> Choice(
                    JinBiaoQuYongWay.SHI_CAI_YONG_CAI, caiEl,
                    anCai114Basis(cb),
                )
                // 五理②/第七讲·财滋弱杀（案八/案十/案廿二）：身旺而官在局（官废不能用——案十二）——有财有官杀优先用杀
                // 案六十一戒：官须**透干**方用官配财（案廿二丙透）——官只藏支而弱（寅不旺）不用官：
                // 身旺印重则喜财坏印（HUAI_YIN 殿后接手），金水（食伤生财）为取用
                // 案一百一十四戒：杀露不任（坐食伤、干头无比劫外之物）——不取财滋弱杀
                !shaLuBuRen114 && guanS >= FOLLOW_MIN_SCORE && (!guanFei ||
                    // 案九十二：月令印当令＋比劫通根（官气尽泄于印）——官虽被食克财绝，
                    // 仍必须得用官（非去寡〔官支在〕非食制〔官弱不任制〕）——官为用神被食伤克＝病
                    (bazi.monthPillar.branch.element == yinEl && hasBenQiRoot(bazi, biEl))) &&
                    bazi.stems.any { it != dayMaster && it.element == guanEl } -> Choice(
                    JinBiaoQuYongWay.PEI_CAI, guanEl,
                    peiCaiBasis(cb, strength, ruoZhongBianWang, yinS, caiS, shiS, guanS, caiZhuBing, liangShang112, qingFen121, yinBiDeJu122, bingDa122JieGou),
                )
                // 案八十二·印护官型：食干克官干而印干贴克食干（己贴壬之属）——印看住食伤不使犯官
                // （把官给解放了）——身旺是用官的：走官地（南方）事业发财；对勘案五十（官全无——财护食用食）
                guanS >= FOLLOW_MIN_SCORE && bazi.stems.any { it != dayMaster && it.element == guanEl } &&
                    bazi.stems.any { it != dayMaster && it.element == yinEl } &&
                    bazi.stems.indices.any { i -> i != 2 && bazi.stems[i].element == shiEl &&
                        ((i - 1 in bazi.stems.indices && bazi.stems[i - 1].element == yinEl) ||
                            (i + 1 in bazi.stems.indices && bazi.stems[i + 1].element == yinEl)) } -> Choice(
                    JinBiaoQuYongWay.PEI_CAI, guanEl,
                    "印护官格（案八十二）：身旺无争议——食干克官干而**印干贴克食干（印看住食伤、不让他犯官去——把官给解放了）**：身旺是用官的。" +
                        "**印护官主要关系事业**（有伤官犯官、有印看着食伤——走官地〔南方〕事业上肯定发财）；又关系**儿子的学习与将来发展**（印看伤官＝给儿子一护卫：好条件、留遗产、教学习——捣乱儿子的东西全被印克了：**不但生儿子，儿子也有出息——儿子得你的福气**）。伤官被印看住＝有教养之人（官代表国家法律——管住不安分的伤官、遵纪守法看书学习——**给儿子做榜样、给后代积德**——一环扣一环）",
                )
                // 案一百零八·土众身旺、食伤吐秀（秀气流行）：日主得局＋食伤透干＋财透护卫＋官杀不透无本气根＋印不旺
                // ——先于「身旺食伤生财·用财」：没金不行，只能用这个金（财为护卫、润土养金，不为用）
                xiuQi108 && strength == DayStrength.STRONG && !ruoJi && !wangJi && shiS >= FOLLOW_MIN_SCORE -> Choice(
                    JinBiaoQuYongWay.SHI_SHENG_CAI, shiEl,
                    buildString {
                        append("身旺食伤吐秀（案一百零八）：${biEl.chinese}众${biEl.chinese}旺、${shiEl.chinese}泄秀（党力${format(shiS)}）——用神取${shiEl.chinese}；官杀不透无本气根（藏于冲中——去不去尽都不要紧）。")
                        append("。**秀气流行**：${biEl.chinese}众${biEl.chinese}旺，有${shiEl.chinese}泄秀就是好命。**不是土重金埋**——身旺我用${shiEl.chinese}泄秀，有${shiEl.chinese}叫**秀气**，就不叫土重金埋（土重金埋说的是金日主生于众土，如戊辰壬戌辛未己丑）；日主${biEl.chinese}得局了${shiEl.chinese}肯定弱，然没${shiEl.chinese}不行，只能用这个${shiEl.chinese}。**最怕${yinEl.chinese}**——没${yinEl.chinese}就是好事；时干${caiEl.chinese}护卫${shiEl.chinese}，而且**${caiEl.chinese}可以润${biEl.chinese}而养${shiEl.chinese}**（细节必记）。地支同气相冲、藏干互战——${shiEl.chinese}克${guanEl.chinese}把官杀去尽了（去不去尽都不要紧，都要用${biEl.chinese}里面的食伤吐秀）。**不喜欢再来比劫运**（身旺不喜欢帮身，${shiEl.chinese}也不喜欢${biEl.chinese}多）；运：**${caiEl.chinese}运不差，只是平运；走${shiEl.chinese}运才发——酉丑化${shiEl.chinese}发，层次达到巅峰**")
                    },
                )
                // 案五十四·身旺食伤喜生财而财有力（财有本气根，或得月令之气——案六十五：戌月秋金有气，
                // 「即使是寅午戌也是秋天——烧再大的火，我的金也是有气的，像夏天下再大的雨天也是热的」；
                // 月令季节＞局势，戌换他支金就完了）——用神即财星；
                // 无枭夺食之病（印<印坏线——案五十印旺夺食仍用食伤金，财为药不为用）
                shiS >= FOLLOW_MIN_SCORE && (hasBenQiRoot(bazi, caiEl) ||
                    branchHiddenStems.getValue(monthBranch).any { it.element == caiEl }) && yinS < YIN_HUAI -> Choice(
                    JinBiaoQuYongWay.SHI_CAI_YONG_CAI, caiEl,
                    buildString {
                        append("身旺食伤喜生财（案五十四/六十五）：身旺无官杀、食伤有力（党力${format(shiS)}）而**财星有力**（${caiEl.chinese}" +
                            (if (hasBenQiRoot(bazi, caiEl)) "有本气根、得令有源" else "**得月令之气**——季节至上：局虽${biEl.chinese}旺，${monthBranch.chinese}月${caiEl.chinese}仍有气（如夏雨不冷）；月令季节＞局势，${monthBranch.chinese}换他支${caiEl.chinese}就完了") +
                            "——${shiEl.chinese}食伤贴生）——**用神肯定就是财星**")
                        append("；盖头之劫无碍（毕竟当令食伤生之，${caiEl.chinese}还是旺的）")
                        val biTou = bazi.stems.filter { it != dayMaster && it.element == biEl }
                        if (biTou.isNotEmpty()) {
                            append("；**怕比劫夺财**——然原局${biTou.joinToString("") { it.chinese }}劫有${shiEl.chinese}食伤居中通关（${biEl.chinese}生${shiEl.chinese}生${caiEl.chinese}——通关化劫、泄化比劫之力），原局不夺财、不会有事")
                            append("；然通关乃静态之缓——运来${biEl.chinese}之根（禄/长生）即冲垮：比劫见根变强、${biEl.chinese}旺则${shiEl.chinese}燥（通关之神亦被焚——燥${shiEl.chinese}不生${caiEl.chinese}），比劫直接克掉用神——死亡级大祸")
                        }
                        append("；**官护财配置论**：用财怕比劫夺财（或印枭夺食），首要找官——**有官星就一直富（走什么运都没事）；没官——走反差之运人生大反差（一会儿天上一会儿地下）——命局没有充分配置的八字，起伏就要大；配置好的八字，人生就一路平坦**。救财两途：**先找官，没有官——食伤能把财彻底救起来也行**")
                        if (guanS < FOLLOW_MIN_SCORE) {
                            append("；此局${guanEl.chinese}官全无——配置不足：帮用（${shiEl.chinese}${caiEl.chinese}）之运发财显贵，${biEl.chinese}劫得根之运大祸（对照案四十九有官克劫救财——用神转官，一路平坦）")
                        }
                        // 案五十五·破格→有情救格：财被重重围克已破格，无官护财——
                        // 然食伤通关化劫＋六合化局补财（寅亥化木）＝绝处逢生有情
                        val huaJuBu = bazi.branches.firstOrNull { br ->
                            br.element == caiEl && LIU_HE[br]?.let { p -> p in bazi.branches && liuHeHuaEl(br, p) == caiEl } == true
                        }
                        if (caiS < biS && huaJuBu != null) {
                            val p = LIU_HE[huaJuBu]!!
                            append("；**财被${biEl.chinese}重重围克（两支克＋干盖头）——已破格**；无${guanEl.chinese}官护财；然${shiEl.chinese}通关化劫＋**${huaJuBu.chinese}${p.chinese}合化${caiEl.chinese}补财——绝处逢生，就有情了**（财稍弱，但不至于用不了——有情就有格局）；运：水（通关之神到）木（财）助用**发大财**（北方坎、东方震）；**土金旺运则破**（土克${shiEl.chinese}断通关之源、复生${biEl.chinese}助劫——日主就完）；申酉戌西方比劫之地不行")
                        }
                    },
                )
                // 案一百零九·土重金埋：地支印旺肯定身旺——食伤泄秀被印克回；八格「印过旺用财破印」
                // 然藏财之支被贴冲、冲神藏干克财（藏金暗相坎伐）——财都没了就完了：直断克妻无子
                // 先于「身旺食伤泄秀」（壬水泄秀被印克回——不能用）
                maJin109 && strength == DayStrength.STRONG && !ruoJi && !wangJi -> Choice(
                    JinBiaoQuYongWay.HUAI_YIN, caiEl,
                    buildString {
                        val keShiYin109 = yinIdx109.firstOrNull { yi -> shiIdx109.any { si -> kotlin.math.abs(si - yi) == 1 } }
                        append("土重金埋·印过旺用财破印（案一百零九）：地支印旺（党力${format(yinS)}）肯定身旺——**土重金埋了**；用食伤${shiEl.chinese}泄秀${if (keShiYin109 != null) "又被印${bazi.stems[keShiYin109].chinese}克回去了" else "又被印克回去了"}——不能用；八格：**印过旺的用${caiEl.chinese}来破印**——${caiEl.chinese}藏于${caiGen109.joinToString("、") { it.chinese }}。")
                        if (caiGenBa109) {
                            append("然地支${caiGen109.joinToString("、") { "${it.chinese}${LIU_CHONG[it]?.chinese ?: ""}冲" }}，**藏${Element.entries.first { it.controls == caiEl }.chinese}暗相坎伐，财都没了，就完了**——直断：**克妻，无子**。**辟「冲开财库必发」**：有人说丑冲未开财库、戌冲辰也开财库必发——肯定不对：我用财，就指望财星这一点点根气，被砍伐冲没了还能行吗？**「财官临库喜刑冲」不可以这么用——这叫刻舟求剑，刻舟求剑的人断不对事**。**一切都得根据原局的喜忌来分析**：地支的作用关系把财的根气坎没了、破坏了——走好运管用吗？要饭的要一个馒头，走好运要上两个馒头，不能说就发财了；考试倒数第一，顶多考到倒数第五，不能说学习就好了。命比运重也不能刻舟求剑——得根据实际情况来说事")
                        }
                    },
                )
                // 案一百一十九·印旺用财而财虚透无根：要维持平衡无非用财克印——先于「身旺食伤泄秀」
                yinWangCai119 -> Choice(JinBiaoQuYongWay.HUAI_YIN, caiEl, yinWangCai119Basis(cb))
                shiS >= FOLLOW_MIN_SCORE -> Choice(
                    JinBiaoQuYongWay.SHI_SHENG_CAI, shiEl,
                    shiShengCaiBasis(cb, shiS, yinS, caiS, guanS, liangRen110, caiZai, jiaQuGua125JieGou),
                )
                // 五理①财坏印（殿后）：无官可用、食伤亦无力，而印坏事且财地支有载或双透得气——用财星坏印
                // （案六十一：壬癸双透虽地支不载，财混杂而得气——喜财坏印，用神财）
                yinS >= YIN_HUAI && (caiZai || bazi.stems.count { it != dayMaster && it.element == caiEl } >= 2) -> Choice(
                    JinBiaoQuYongWay.HUAI_YIN, caiEl,
                    huaiYinDianHouBasis(cb, caiS, yinS, caiZai, anCaiWuGen115, geJuZu103, sanHuiSanLiuHeNote),
                )
                else -> {
                    val best = listOf(
                        Triple("官杀", guanEl, guanS),
                        Triple("财星", caiEl, caiS),
                        Triple("食伤", shiEl, shiS),
                    ).maxBy { it.third }
                    Choice(
                        JinBiaoQuYongWay.YI_SHEN, best.second,
                        "身强抑泄：官党力 ${format(guanS)}、财党力 ${format(caiS)}、食伤党力 ${format(shiS)}——" +
                            "取${best.first}为用（基线，细则待师传档）",
                    )
                }
            }
            // 中和：暂取月令主力（基线，细则待师传）
            else -> {
                val monthEl = bazi.monthPillar.branch.element
                val el = if (monthEl == biEl || monthEl == yinEl) {
                    listOf(guanEl to guanS, caiEl to caiS, shiEl to shiS).maxBy { it.second }.first
                } else monthEl
                Choice(
                    JinBiaoQuYongWay.ZHONG_HE, el,
                    "中和取月令之力（${bazi.monthPillar.branch.chinese}）为用；中和取用细则待师传档",
                )
            }
        }

        val yongEl = choice.el
        // 案一百二十二·身强用官而病太大（结构判据 bingDa122JieGou 见取用之前——此合「用官」定档）
        val bingDa122 = bingDa122JieGou && choice.way == JinBiaoQuYongWay.PEI_CAI
        // 案一百二十四·有力就能食神制杀（取用已定——此合「食神制杀」定档）
        val shiZhi124 = youLiShiZhi124 && choice.way == JinBiaoQuYongWay.ZHI_SHA
        // 案一百二十五·假去寡·印护官（取用走身旺食伤泄秀——文案换为「去寡／用官两可、印护官当兵」）
        val jiaQuGua125 = jiaQuGua125JieGou && choice.way == JinBiaoQuYongWay.SHI_SHENG_CAI
        // 案一百二十六·坐下暗印（取用已定——用印）
        val anYin126 = anYin126JieGou && choice.way == JinBiaoQuYongWay.FU_SHEN && yongEl == yinEl
        // 案一百二十七·财官旺·比劫硬扛（取用走身弱用劫）；学历：没有印——学习不好（兵卒）
        val yingKang127 = yk127.ok && choice.way == JinBiaoQuYongWay.SHANG_YONG_JIE && yongEl == biEl
        val xueLi127: cn.xulun.bazi.model.JinBiaoXueLi? = if (yingKang127) xueLi127Of() else null
        // 案一百二十六·学历：印没发露——没学历
        val xueLi126: cn.xulun.bazi.model.JinBiaoXueLi? =
            if (anYin126) xueLi126Of(cb) else null
        // 案一百二十五·学历：兵卒之命——学历低、出身差
        val xueLi125: cn.xulun.bazi.model.JinBiaoXueLi? =
            if (jiaQuGua125) xueLi125Of() else null
        val yongGod = representativeGod(bazi, dayMaster, yongEl)
        // 案一百二十七·巳亥冲去火存金——巳随金走（「巳火当然帮金了」）：不入用神之位
        val yongShenStars = locateElementStars(bazi, dayMaster, yongEl).let { ss ->
            if (yingKang127 && yk127.sui != null) ss.filterNot { it.contains("${yk127.sui.chinese}本气") } else ss
        }

        // ── 用神健全度（第二讲） ──
        val health = assessHealth(bazi, dayMaster, yongEl)

        // ── 清浊三档（第六讲） ──
        // 病神＝克用神之五行；药＝克病神之五行（力足制病方为药）；
        // 病被通关化掉＝病神不透干＋通关之神（生用神者）有力＋病神生通关、通关生用神——转而相生有情（案十金生水生木、案十一水生木生火）。
        val bingEl = Element.entries.first { it.controls == yongEl }
        val yaoEl = Element.entries.first { it.controls == bingEl }
        val tongGuanEl = Element.entries.first { it.generates == yongEl }
        val bingS2 = partyScore(bazi, bingEl)
        val yongS2 = partyScore(bazi, yongEl)
        val yaoS = partyScore(bazi, yaoEl)
        val yongDeLi = bazi.stems.any { it.element == yongEl } || hasBenQiRoot(bazi, yongEl)
        val bingHua = !bazi.stems.any { it.element == bingEl } &&
            partyScore(bazi, tongGuanEl) >= GUAN_WANG &&
            bingEl.generates == tongGuanEl && tongGuanEl.generates == yongEl
        // 案十定则：假药非药——克病神者若本身即此局忌神（如用官局之印泄官、泄秀局之印比），不得计为药
        val wayJiEls: Set<Element> = when (choice.way) {
            JinBiaoQuYongWay.PEI_CAI -> setOf(shiEl, yinEl)
            JinBiaoQuYongWay.SHI_SHENG_CAI -> if (yinS >= YIN_HUAI) setOf(yinEl, biEl) else emptySet()
            // 案一百二十：伤官用劫而克泄交加——食伤本身即泄用神之忌（假药非药），不得计为制官之药
            JinBiaoQuYongWay.SHANG_YONG_JIE -> if (keXieJiaoJia120) setOf(shiEl) else emptySet()
            else -> emptySet()
        }
        val bingZhi = bingS2 > 0.0 && yaoS >= bingS2 && yaoS >= FOLLOW_MIN_SCORE && yaoEl !in wayJiEls
        val quanZhuo = strength == DayStrength.STRONG && !ruoJi && !wangJi && guanFei && shiFei && caiFei
        // ── 清枯（案十四）：身弱全赖印，而日主无本气根、印被克者有根有力、生印之官又过旺蒸之 ──
        // （火炎土焦——燥土不生金，「生金有点难，但也只能用土，最起码土帮我扛了一下火」）——病太大一点救都没有。
        // 案廿八豁免：克印之支尽入官局被化（寅入寅午戌火局——寅随火走不克辰，反生官生印＝有情）则不清枯。
        val keYinEl = Element.entries.first { it.controls == yinEl }
        val guanSanHe = listOf(
            setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU),
            setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU),
            setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN),
            setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI),
        ).firstOrNull { trio -> trio.all { it in bazi.branches } && trio.first().let { _ -> true } }
        val keYinBound = guanSanHe != null &&
            bazi.branches.filter { it.element == keYinEl }.all { it in guanSanHe }
        val qingKu = (choice.way == JinBiaoQuYongWay.HUA_SHA && strength == DayStrength.WEAK &&
            !hasBenQiRoot(bazi, biEl) &&
            partyScore(bazi, keYinEl) >= FOLLOW_MIN_SCORE && hasBenQiRoot(bazi, keYinEl) &&
            guanS >= GUAN_WANG && !keYinBound) ||
            // 案五十七·清枯变体：身弱用印而财得局坏印（财有多旺他就有多穷）——印运仅温饱、食伤生财运身无分文负债
            (choice.way == JinBiaoQuYongWay.FU_SHEN && yongEl == yinEl && strength == DayStrength.WEAK &&
                caiS >= GUAN_WANG && hasBenQiRoot(bazi, caiEl))
        // 案五十三：病神透干而无本气根、用神坐日支本气根——干支异途（干不克支），病不贴克用神：
        // 有格局（坐下印在），行运不违背即好命；未中印根被冲掉仅降一点格局
        val bingYiTU = bingS2 > 0.0 && !hasBenQiRoot(bazi, bingEl) &&
            bazi.dayPillar.branch.element == yongEl
        // 案五十四·用财局病神（比劫）透干而有食伤通关（火生土土生金——通关化劫）：病不直克用神，
        // 原局不会有事；然配置不足（无官护财）——起伏大：帮用之运发财显贵、比劫见根之运大祸
        val bingTongGuanTou = choice.way == JinBiaoQuYongWay.SHI_CAI_YONG_CAI &&
            bazi.stems.any { it != dayMaster && it.element == biEl } &&
            bazi.stems.any { it != dayMaster && it.element == shiEl } &&
            partyScore(bazi, shiEl) >= GUAN_WANG && guanS < FOLLOW_MIN_SCORE
        // 案五十五·化局补用神（寅亥化木补财）：破格被救——绝处逢生有情就有格局
        val huaJuBuYong = bazi.branches.any { br ->
            br.element == yongEl && LIU_HE[br]?.let { p -> p in bazi.branches && liuHeHuaEl(br, p) == yongEl } == true
        }
        // 案五十六·三会化劫：用神（比劫）之根被三会局化尽——用神无力，格局不高
        val yongWuLiHua = choice.way == JinBiaoQuYongWay.SHANG_YONG_JIE &&
            SAN_HUI_JU.firstOrNull { (bs, _) -> bs.all { it in bazi.branches } }?.let { (bs, el) ->
                el != yongEl && bazi.branches.any { it.element == yongEl && it in bs }
            } == true
        // 案八十七·食伤半通关（化得没那么好）：用财局病比劫而食伤有本气根（午火化解比劫生财）——
        // 病压用神而通关之桥在（异于案五四食伤≥劫之全通关）——财富全凭食伤一字定之：
        // 差运（克食伤之源如子水）穷困潦倒；食伤局成之运（戌填寅午）才挣到钱——小康非满盘浊
        val shiBanTongGuan = choice.way == JinBiaoQuYongWay.SHI_CAI_YONG_CAI && bingEl == biEl &&
            hasBenQiRoot(bazi, shiEl) && bingS2 >= 1.5 * yongS2
        // 案八十四·财气通门户：病神（枭印）虽大而拱财暗存（药待运填）——财星有情：
        // 走差运小康（生意也不差选对行）、好运发大财——非满盘浊
        val gongCaiTongMen = bingEl == yinEl &&
            listOf(
                setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI) to Element.WOOD,
                setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU) to Element.FIRE,
                setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN) to Element.WATER,
                setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU) to Element.METAL,
            ).any { (trio, el) -> el == caiEl && bazi.branches.count { it in trio } >= 2 && !trio.all { it in bazi.branches } }
        // 案一百零七·伤官配印而印根被财本气支贴冲拔、冲神得食伤之力：用神在地支被坏——
        // 层次高不了（运再好也只能小康或不如小康）；财星起坏作用＝财神反不真罪加一等
        val yinGenBaCai107 = choice.way == JinBiaoQuYongWay.FU_SHEN && yongEl == yinEl &&
            strength == DayStrength.WEAK && yinGenBaCai && shiS >= GUAN_WANG
        // 案一百一十二·用官局喜用两伤（身旺取官方论）
        val liangShang112Pei = liangShang112?.takeIf { strength == DayStrength.STRONG && choice.way == JinBiaoQuYongWay.PEI_CAI }
        val qingZhuo = when {
            // 案九十九·一制一化有情：有情就是有格局（不虑旺衰——制化相得）
            choice.way == JinBiaoQuYongWay.YI_ZHI_YI_HUA -> JinBiaoQingZhuo.QING
            // 案一百一十三·食伤泄过为病：病在（食伤局泄过）而待药——运支暗冲去病发福最大（档位为落地映射——师未断原局清浊）
            choice.way == JinBiaoQuYongWay.GUO_XIE_QIU_YIN -> JinBiaoQingZhuo.BAN_QING
            yinGenBaCai107 -> JinBiaoQingZhuo.BAN_QING
            // 案一百零九·土重金埋：印过旺用财破印而藏财之根被贴冲砍伐尽——用神无着（财都没了就完了）
            maJin109 && caiGenBa109 && choice.way == JinBiaoQuYongWay.HUAI_YIN -> JinBiaoQingZhuo.QING_KU
            // 案一百一十二·用官局喜用两伤：官被泄被冲克、财根被冲拔——就完了（档位为落地映射）
            liangShang112Pei != null -> JinBiaoQingZhuo.QING_KU
            // 案一百二十三·金有力而基本平衡——好命而稍有病（午火孤：被水克、无源、巳被合走）：未行帮火之运则不得志（档位为落地映射）
            yongGuan123 -> JinBiaoQingZhuo.QING
            // 案一百二十二·身强用官而病太大（金得局克尽官之源头、源头水不现）——好运仅凑合（档位为落地映射）
            bingDa122 -> JinBiaoQingZhuo.QING_KU
            // 案一百二十·伤官用劫而印全无、官杀透干——克泄交加：蹲大牢／死，没有印富裕不了（好运仅温饱；档位为落地映射）
            keXieJiaoJia120 -> JinBiaoQingZhuo.QING_KU
            // 案一百一十七·干多不如支重·财旺不载：用神（比劫）无根无力、承载不了——走好运也仅温饱（档位为落地映射）
            ganDuoBuZai117 -> JinBiaoQingZhuo.QING_KU
            qingKu -> JinBiaoQingZhuo.QING_KU
            quanZhuo -> JinBiaoQingZhuo.QUAN_ZHUO
            yongWuLiHua -> JinBiaoQingZhuo.MAN_ZHUO
            bingS2 == 0.0 -> JinBiaoQingZhuo.QING
            bingHua -> JinBiaoQingZhuo.QING
            bingZhi -> JinBiaoQingZhuo.QING
            bingYiTU -> JinBiaoQingZhuo.QING
            bingTongGuanTou && huaJuBuYong -> JinBiaoQingZhuo.QING
            hasBenQiRoot(bazi, bingEl) && bingS2 >= 1.5 * yongS2 && !bingTongGuanTou && !gongCaiTongMen && !shiBanTongGuan -> JinBiaoQingZhuo.MAN_ZHUO
            shiBanTongGuan -> JinBiaoQingZhuo.BAN_QING
            gongCaiTongMen -> JinBiaoQingZhuo.QING
            else -> JinBiaoQingZhuo.BAN_QING
        }
        // 案十：用官局双浊源——克用神者（如未土克官）之外，克喜神财者（比劫丁火之类）亦是浊
        val jianBing: String? = if (choice.way == JinBiaoQuYongWay.PEI_CAI) {
            val bing2El = Element.entries.first { it.controls == caiEl }
            if (partyScore(bazi, bing2El) >= 1.0) "兼病：${bing2El.chinese}（比劫）克喜神财${caiEl.chinese}——与${bingEl.chinese}同列为浊（一克用神一克喜神）" else null
        } else {
            null
        }
        val qingZhuoBasis = qingZhuoBasisOf(cb, choice, qingZhuo, jianBing, bingEl, yongEl, bingS2, yongS2, yongDeLi, yaoEl, tongGuanEl, yaoS, bingHua, bingYiTU, bingTongGuanTou, gongCaiTongMen, huaJuBuYong, yongWuLiHua, anCai114, liangShang112Pei, ganDuoBuZai117, maJin109, caiGenBa109, caiGen109, shiBanTongGuan, shiSanHe113, yinGenBaCai107, yinWangCai119, keGuan, keYinEl, keXieJiaoJia120, qingFen121, shiZhi124, jiaQuGua125, bingDa122, yongGuan123, anYin126, yingKang127)

        // ── 喜忌（第一讲：帮用=喜，损用=忌） ──
        val xiElements = Element.entries.filter { it.generates == yongEl || it == yongEl }
        val jiElements = Element.entries.filter { it.controls == yongEl || yongEl.generates == it || yongEl.controls == it }

        // ── 子女判读（第十一讲·子女章判定树：子星按原局喜忌而定，来回检查；子星要昌） ──
        // 四情况铁律：必有儿／必无儿／无儿有女／无儿无女损子——逃不出这个圈。
        val ziNv: cn.xulun.bazi.model.JinBiaoZiNv? = ziNvOf(bazi, daYun, choice, strength, ruoJi, wangJi, dayMaster, yinEl, biEl, shiEl, caiEl, guanEl, yinS, biS, caiS, guanS, shiS, monthBranch, guoXieFanRuo, kunLunEl, kunLunSource, gongCaiYinHua, gongCaiTrio, yongEl, caiZai, liangShang112Pei, ganDuoBuZai117, anYin126, yingKang127)

        // ── 学历判读（案九十四·学历章开讲：印为用神之局适用） ──
        // 看学历不能只看初运好坏：①印星原局没被破坏→小时候脑子没毛病、头脑不笨（底子）；
        // ②学历高低看求学运（初运两步）：走好运（生印同印）百分之百学历高；走北方晦火之运
        // （火最怕湿土来晦）学历就不高——但学习肯定不错或者好学（走坏运＝平时学习好、考试发挥失常）；
        // ③大运地支不载不碍（甲申乙酉——支不载而天干生火）：只要天干没把印坏了，就可以有学历、
        // 可以上大学——干定学历。
        val xueLi: cn.xulun.bazi.model.JinBiaoXueLi? = xueLi127 ?: xueLi126 ?: xueLi125 ?: xueLiOf(bazi, daYun, choice, strength, ruoJi, wangJi, qiangJi, yongEl, yinEl, caiEl, shiEl, guanEl, yinS, liangShang112Pei)

        // ── 运年评断（以用神为中心；从格/专旺的反常识运断由用神立场自动成立） ──
        // 财解围局（第三讲）：行财运亨通；行官/食运稍差但无大碍——中平。
        // 旺极局：局中印轻食伤顺泄也是好运；官杀犯旺、财地九死一生——大凶。
        // 强极局（案三修订）：印星得局官杀运化煞生印，运支合入印局（三合/六合化印）则大发；
        //   财地若得食伤干通关或比劫干盖头——顺泄无直接冲克，贫寒不至大凶（大难不死必有后福）。
        // 弱极铁律（第三讲）：行比劫/印运必大凶（勿看流年——轻则破大财，重则牢狱死亡）。
        // 争妒判据（案二）：弱极局食伤与官杀并存（各党力≥2）则打架，须财解围——非用之官/食运稍差无大碍；
        // 无争妒（案四：财官相生一体）——走财走官走食伤皆顺皆吉，唯帮身反害。
        val zhengDou = ruoJi && guanS >= FOLLOW_MIN_SCORE && shiS >= FOLLOW_MIN_SCORE
        val zhongPingNote: Map<Element, String> = buildMap {
            // 案一百零八·秀气流行局：财运不差只是平运（食伤生财不破财）；比劫运不喜欢（身旺不喜帮身）
            if (xiuQi108 && choice.way == JinBiaoQuYongWay.SHI_SHENG_CAI) {
                put(caiEl, "${caiEl.chinese}运不差，只是平运——食伤生财不破财（不用之财尽可劫·儿又生儿）")
                put(biEl, "身旺不喜欢帮身、${shiEl.chinese}也不喜欢${biEl.chinese}多——不喜欢再来比劫运")
            }
            // 案一百零九·土重金埋：原局财根被冲伐尽——好运（财、生财之食伤）也只是多要一个馒头，作中平
            if (maJin109 && choice.way == JinBiaoQuYongWay.HUAI_YIN) {
                put(caiEl, "原局${caiEl.chinese}之根气被冲伐尽——走好运也只是要饭的多要一个馒头，不能说就发财了（案一百零九）")
                put(shiEl, "生${caiEl.chinese}帮用——原局坏绝，好运只多一个馒头（案一百零九）")
            }
            // 案六十四·官已重（官杀本气支贴冲比劫之根——辰酉化金力大）而再生成官（如土生金）：
            // 助忌——提升不了（提升在帮身化杀／制杀以威之运——事实庚寅年甲午年提升）
            if (choice.way == JinBiaoQuYongWay.HE_LIU &&
                bazi.branches.filter { it.element == biEl }.any { bg ->
                    val idx = bazi.branches.indexOf(bg)
                    (idx + 1 in bazi.branches.indices && bazi.branches[idx + 1].element == guanEl && LIU_CHONG[bg] == bazi.branches[idx + 1]) ||
                        (idx - 1 in bazi.branches.indices && bazi.branches[idx - 1].element == guanEl && LIU_CHONG[bg] == bazi.branches[idx - 1])
                }
            ) {
                put(caiEl, "官杀已重（贴冲比劫之根），生官之地助忌——提升不了（提升在帮身化杀之运：泄金生木；火运制杀以威＝大层次——庚寅、甲午年之属）")
            }
            // 案七十七·食制局印运中平档（限双印透干且印克食伤〔水克火夺食〕型——壬壬一被克一独存之式；
            // 案卅四禄根通关型不入此档）：标准格是食伤制杀——印运虽克食伤亦可化杀（解决了问题），
            // 不过平庸、算可以（走差运当普通人、一般运比普通人稍强——八字有层次；事实：印运事业单位平庸小头，快退休走木运当处长）
            if (choice.way == JinBiaoQuYongWay.ZHI_SHA && !tuiErQici && yinEl.controls == shiEl &&
                yinS >= FOLLOW_MIN_SCORE && bazi.stems.count { it != dayMaster && it.element == yinEl } >= 2
            ) {
                put(yinEl, "印运化杀亦解题——然标准格是食伤制杀（印克食伤＝夺食）：**不过平庸、算可以**（不差不穷——走差运当普通人、一般运比普通人稍强）")
            }
            if (choice.way == JinBiaoQuYongWay.CAI_JIE_WEI) {
                put(guanEl, "解围局行此稍差无大碍")
                put(shiEl, "解围局行此稍差无大碍")
            }
            // 案廿四·财党杀攻身：杀地——干有印化则稍安（丙寅之属），无印则攻身
            if (choice.way == JinBiaoQuYongWay.GONG_SHEN) put(guanEl, "杀攻身之地——干有印化则稍安（丙寅之属能化杀），仍险")
            if (ruoJi && zhengDou && choice.way != JinBiaoQuYongWay.CAI_JIE_WEI) {
                if (guanEl != yongEl) put(guanEl, "食官并立有争妒——行此稍差无大碍")
                if (shiEl != yongEl) put(shiEl, "食官并立有争妒——行此稍差无大碍")
            }
        }
        val favorNote: Map<Element, String> = buildMap {
            // 旺强极顺泄：满盘比劫旺到极端，食伤顺泄不克——地支印不成局（一二印枭）则食伤运没事；地支印成局则枭夺不可行
            // 案一百一十八：印轻／重以**地支印成局**分（印本气≥3）——去掉两午火（印减至两支）则走金运没事
            if (wangJi && !yinDeJuDuo) put(shiEl, "印轻顺泄——局中印轻（一二${yinEl.chinese}枭、地支不成局），走${shiEl.chinese}运没事（案一百一十八：地支印成局才有枭神夺食之灾）")
            // 第五讲·去寡：食伤＝去官之星，行此好运；印泄官生身——身旺食伤亦旺，行此亦好运（案七）
            // 案九十六戒：官弱（党力<官旺线）无须印泄——印激已旺之身兼克食伤用神，不入好运
            if (choice.way == JinBiaoQuYongWay.QU_GUA) {
                put(shiEl, "去官之星到位——违背官杀，好运")
                if (guanS >= GUAN_WANG) put(yinEl, "印泄官生身——身旺食伤亦旺，好运")
            }
            // 五理②用官（案八）：财生官——帮官之运好运
            if (choice.way == JinBiaoQuYongWay.PEI_CAI && !caiZhuBing) put(caiEl, "财生官——帮官之运，好运")
            // 案廿二：财被劫病局——食伤为通关药神（泄比劫生财滋杀），北方运解病一样富贵
            // 案廿三：火运双用——杀通根＋克金护财（护卫寅木），当官
            if (choice.way == JinBiaoQuYongWay.PEI_CAI && caiZhuBing) {
                put(caiEl, "财星到——滋弱杀，好运")
                put(shiEl, "泄${biEl.chinese}生${caiEl.chinese}——解${biEl.chinese}克${caiEl.chinese}之病（药神通关），一样发富发贵")
                put(guanEl, "杀得根＋克${biEl.chinese}护${caiEl.chinese}（双用：通根护财）——当官")
            }
            // 昆仑得局（命三）：生源、本势、顺泄皆顺——金水木
            if (choice.way == JinBiaoQuYongWay.KUN_LUN) {
                put(kunLunSource!!, "${kunLunSource.chinese}生${kunLunEl!!.chinese}——顺其势，好运")
                put(kunLunEl, "本势得局——顺其势，好运")
                put(kunLunEl.generates, "顺泄——${kunLunEl.chinese}生${kunLunEl.generates.chinese}（金生水水生木），好运" +
                    if (kunLunEl == biEl) "；**${kunLunEl.generates.chinese}来根了也不是${kunLunSource.chinese}${kunLunEl.generates.chinese}交战——只能说是泄它、没有逆它（肯定没有来${kunLunSource.chinese}好，但是来了${kunLunEl.generates.chinese}也没事——案九十七）**" else "")
            }
            // 案五十一·伤官配印局：官生印好运（配印喜见官）；财坏印大忌（用神无护卫——巳印支运财干盖头亦以支论吉）
            if (choice.way == JinBiaoQuYongWay.FU_SHEN && guanS == 0.0 && caiS < FOLLOW_MIN_SCORE) {
                put(guanEl, "官生印——伤官配印喜见官（最好组合），好运")
                put(caiEl, "财坏印——用神无护卫，来了就完，庸碌无为不得志（干凶支吉以支论）")
            }
            // 案五十六·三会化劫用神无力：火土帮比劫可小富（七杀死亡注记入大凶档，不入作吉五行）
            if (yongWuLiHua) {
                put(yinEl, "火印生比劫用神——帮起来还是可以小富（格局不高，小富而已）")
            }
            // 案五十三·配印泄体过而有透干忌神可去：比劫干克去忌神兼帮日主扛食伤之泄——
            // 「泄体过了的必须得帮身」＋「去忌神之运最好」——南方比劫、东方印皆帮身方向，行运不违背即好命。
            // 无透干忌神可去（案五十一）——比劫但泄印气，不入此喜。
            if (choice.way == JinBiaoQuYongWay.FU_SHEN && yongEl == yinEl && shiS >= GUAN_WANG &&
                biEl.controls == bingEl && bazi.stems.any { it != dayMaster && it.element == bingEl }
            ) {
                put(biEl, "比劫帮身兼克去透干忌神${bingEl.chinese}——泄体过必须帮身（虽泄${yinEl.chinese}印之气，然帮日主扛${shiEl.chinese}之泄：身不倒则印有用武之地）＋去忌神即去病：去忌之运最好，好运")
            }
            // 案卅五：合官留杀局——印运制食留杀（解病清格）好运；食伤再到＝复克用神
            // 案卅六：合而化局——食伤党杀再克用神（火生土），差运
            // 案卅九：身弱合去混杂有情有格局——帮身运当官
            if (choice.way == JinBiaoQuYongWay.HE_LIU) {
                put(yinEl, "制${shiEl.chinese}留${guanEl.chinese}——解病清格，好运")
                put(shiEl, "食伤再到——复克${guanEl.chinese}用神，病")
                if (strength == DayStrength.WEAK) {
                    put(biEl, "帮身之运——八字有格局，走帮身运当官")
                    put(yinEl, "帮身化杀之运——当官")
                }
            }
            // 案九：身旺泄秀——财为泄口；印病时财运兼去病之神（甲克戊）
            // 案卅三/四十七：食制局之印运（壬子型退而求其次·护杀帮身型）——印运反好
            if (choice.way == JinBiaoQuYongWay.ZHI_SHA && tuiErQici) {
                put(yinEl, "印运化杀帮身——大运补起印来（本科、得财之属），好运")
            }
            if (choice.way == JinBiaoQuYongWay.ZHI_SHA && !tuiErQici && shiS >= 6.0) {
                put(yinEl, "印护杀帮身——制杀太过兼过泄之救，好运")
            }
            if (choice.way == JinBiaoQuYongWay.SHI_SHENG_CAI) {
                put(
                    caiEl,
                    if (yinS >= YIN_HUAI && guanS == 0.0) "财克印去病兼护食制枭——克掉忌神印${yinEl.chinese}（亥子丑坎卦），好运"
                    else if (yinS >= YIN_HUAI) "财克印去病——去戊土之病，发泄更畅，好运（甲寅之属尤佳）"
                    else "食神生财——泄秀得衡，好运",
                )
            }
            // 案五十·枭神夺食局：官生枭夺食——伤官不可见官大凶
            if (choice.way == JinBiaoQuYongWay.SHI_SHENG_CAI && yinS >= YIN_HUAI && guanS == 0.0) {
                put(guanEl, "官生枭夺食更凶——伤官不可见官，见官为祸百端（运忌丙寅丁卯之属）")
            }
            // 案四：弱极随势无争妒——财官食相生一体，随财杀而走皆吉
            if (ruoJi && !zhengDou) {
                when (choice.way) {
                    JinBiaoQuYongWay.SUI_CAI -> {
                        put(guanEl, "随财杀——财生官一体，走官亦富贵显达")
                        put(shiEl, "食生财——顺势亦吉")
                    }
                    JinBiaoQuYongWay.SUI_GUAN -> put(caiEl, "财生官——走财运必发达")
                    JinBiaoQuYongWay.SUI_SHI -> put(caiEl, "从儿见财——顺势亦吉")
                    else -> {}
                }
            }
        }
        val daXiongNote: Map<Element, String> = buildMap {
            // 案五十六·三会化劫用神无力：最怕七杀克比劫——死亡
            if (yongWuLiHua) {
                put(guanEl, "最怕七杀——克掉被化无力之比劫用神：**死亡**")
            }
            if (ruoJi) {
                put(biEl, "弱极行比劫印地必大凶（勿以流年论）")
                put(yinEl, "弱极行比劫印地必大凶（勿以流年论）")
            }
            if (qiangJi) {
                if (!yinDeJu) put(guanEl, "官克比劫触怒强神——当即直断出大灾（生命危险、自由受制、倾家荡产、妻离子散）")
                put(caiEl, "财克印犯印之旺——大凶，直断出大灾")
            } else if (wangJi) {
                put(guanEl, "运行官杀犯旺——必然大凶（严重的死亡，轻微的牢狱）")
                put(caiEl, "旺极行财地——立断九死一生（群劫争财，必出人命）")
            }
            // 第五讲·去寡：不用之官尽可去——官星得地（得运）反一事无成
            if (choice.way == JinBiaoQuYongWay.QU_GUA) put(guanEl, "官星得地得禄——顺着官杀，一世无成（不用之官尽可去）")
            // 五理②用官（案八）：制化官杀之运——违背官，差运（财被劫病局之食伤为药，不入此档）
            if (choice.way == JinBiaoQuYongWay.PEI_CAI && !caiZhuBing) {
                put(shiEl, "食伤制官——违背官杀，差运")
                put(yinEl, "印泄官化官——违背官杀，差运")
            }
            if (choice.way == JinBiaoQuYongWay.PEI_CAI && caiZhuBing) {
                put(yinEl, "印生${biEl.chinese}助病——差运")
            }
            // 案九：身旺泄秀而印为病——土金运（印比）病来即差（金虽生水，然身更旺、发泄受阻）
            // 案一百二十五戒：印护官之局——印非病、比劫（火）乃去寡通关之神，不入此档
            if (choice.way == JinBiaoQuYongWay.SHI_SHENG_CAI && yinS >= YIN_HUAI && !jiaQuGua125) {
                put(yinEl, "印为病——病来即差（贫穷之运）")
                put(biEl, "身更旺——发泄受阻，差运")
            }
            // 昆仑得局之逆（命二/命三）：克势者止水（土）、克生源者逆局（火）——顺来逆去贫之意
            // 案九十七：土虽生金（生源之生）亦不抵克水之逆——旺到极端只论逆不论功（止不住的）
            if (choice.way == JinBiaoQuYongWay.KUN_LUN) {
                val keEl = Element.entries.first { it.controls == yongEl }
                put(keEl, "克${yongEl.chinese}止${yongEl.chinese}——土止昆仑之水，触犯强旺之势，凶" +
                    if (kunLunEl == biEl) "；**${keEl.chinese}虽生${kunLunSource!!.chinese}亦不抵克${yongEl.chinese}之逆——${keEl.chinese}有两个功能（生${kunLunSource.chinese}＋克我的${yongEl.chinese}），旺到极端只论逆不论功：稍微让我不舒服就不行（止不住的——案九十七）**" else "")
                put(yongEl.controls, "克生源逆${yongEl.chinese}局——逆则贫命出灾（刑妻克子贫乏）")
            }
            // 案廿四·财党杀攻身：财滋杀攻身——最忌
            if (choice.way == JinBiaoQuYongWay.GONG_SHEN) {
                put(caiEl, "财滋杀攻身——最忌（怕财来滋杀）")
            }
            // 气象调候之反（案十六/案十八/案十九/案二十一）：寒/燥而调候神无根气——强走调候运反大凶；
            // 案廿五裁：格局取用已定（财滋弱杀/弱官）则调候让位——走帮官杀财之运即好运
            // 案九十五戒：配印局而火即用神印——火运帮用，不走反调候档（从儿顺火局不在此列——水运犯旺）
            if (choice.way != JinBiaoQuYongWay.PEI_CAI &&
                !(choice.way == JinBiaoQuYongWay.FU_SHEN && choice.el == Element.FIRE)
            ) {
                if (hanJu && huoTou && !hanTiaoYouQi) {
                    put(Element.FIRE, "调候神丙火无根气（隔位之生、地支不载）——强走火运调候反一事无成（离婚克子之属）；反以不调候为美，顺其寒")
                }
                if (zaoJu && shuiTou && !zaoTiaoYouQi) {
                    put(Element.WATER, "调候神壬癸水无根——强走水运调候即犯火之旺（一败如灰，甚者死）；反以不调候为美，顺其燥")
                }
            }
            // 案九十六·去寡而官弱：印激已旺之身兼克食伤用神——水激之不可（北方水运把食神憋住）
            if (choice.way == JinBiaoQuYongWay.QU_GUA && guanS < GUAN_WANG && yinEl.controls == shiEl) {
                put(yinEl, "印${yinEl.chinese}激已旺之身、克${shiEl.chinese}用神——不可用水激之（北方水运把食神${shiEl.chinese}憋住：事业受制且没儿子——案九十六）")
            }
        }
        // 案三专题先记：八字重地支——运干生运支而干在局无根（党力0）者，干力全化于支（壬水全生寅木，壬寅以寅论）
        val rootlessEls = setOf(caiEl, guanEl, shiEl, yinEl, biEl).filter { partyScore(bazi, it) == 0.0 }.toSet()
        // 案四十五：用神本气根被冲——冲神之支；运支六合合去冲神＝用神绝处逢生
        val chongShen: EarthlyBranch? = health.damages.firstOrNull { it.contains("支神只以冲为重") }?.let { d ->
            bazi.branches.firstOrNull { br ->
                br.element == yongEl && LIU_CHONG[br] != null && d.contains("被${LIU_CHONG[br]!!.chinese}冲")
            }?.let { LIU_CHONG[it] }
        }
        val ctx = YunCtx(
            yongEl = yongEl, biEl = biEl, yinEl = yinEl, shiEl = shiEl, caiEl = caiEl, guanEl = guanEl,
            qiangJi = qiangJi, yinDeJu = yinDeJu,
            chongShen = chongShen,
            wangJiFamily = wangJi || qiangJi, yinDeJuDuo = yinDeJuDuo,
            favorEls = favorNote.keys, zhongPingEls = zhongPingNote.keys, daXiongEls = daXiongNote.keys,
            rootlessEls = rootlessEls,
            daXiongText = daXiongNote, zhongPingText = zhongPingNote, favorText = favorNote,
            yongYin = choice.way == JinBiaoQuYongWay.HUA_SHA,
            yongJie = choice.way == JinBiaoQuYongWay.SHANG_YONG_JIE,
            jiShenTouGans = bazi.stems.filterIndexed { i, s -> i != 2 && s.element == bingEl },
            yongCai = choice.way == JinBiaoQuYongWay.SHI_CAI_YONG_CAI,
            biTouGan = bazi.stems.filterIndexed { i, s -> i != 2 && s.element == biEl },
            yongWuLi = yongWuLiHua,
            renZhi = YANG_REN[dayMaster],
            chongYinGen = if (guoXieFanRuo && choice.way == JinBiaoQuYongWay.FU_SHEN) {
                bazi.stems.indices.firstOrNull { it != 2 && bazi.stems[it].element == yinEl }
                    ?.let { bazi.branches[it] }
                    ?.takeIf { it.element.generates == yinEl && LIU_CHONG[it] != null }
            } else {
                null
            },
            jia95 = guoXieFanRuo && choice.way == JinBiaoQuYongWay.FU_SHEN && yongEl == yinEl,
            yongKu = when (yongEl) {
                Element.FIRE -> EarthlyBranch.XU
                Element.WATER -> EarthlyBranch.CHEN
                Element.WOOD -> EarthlyBranch.WEI
                Element.METAL -> EarthlyBranch.CHOU
                else -> null
            },
            xiShenEl = Element.entries.first { it.generates == yongEl },
            quGuaGan = if (choice.way == JinBiaoQuYongWay.QU_GUA) {
                bazi.stems.firstOrNull { it != dayMaster && it.element == guanEl }
            } else {
                null
            },
            jia98 = choice.way == JinBiaoQuYongWay.KUN_LUN && kunLunEl == biEl &&
                bazi.stems.any { it != dayMaster && it.element == Element.entries.first { e -> e.controls == kunLunEl } },
            zhiShaCai = choice.way == JinBiaoQuYongWay.ZHI_SHA && strength == DayStrength.STRONG,
            xiuQi108 = xiuQi108 && choice.way == JinBiaoQuYongWay.SHI_SHENG_CAI,
            liangRen110 = liangRen110 && choice.way == JinBiaoQuYongWay.SHI_SHENG_CAI,
            bingZhi113 = shiSanHe113?.first?.getOrNull(1)?.takeIf { choice.way == JinBiaoQuYongWay.GUO_XIE_QIU_YIN },
            bingZhi114 = bazi.dayPillar.branch.takeIf { anCai114 && choice.way == JinBiaoQuYongWay.SHI_CAI_YONG_CAI },
            bingZhis115 = if (anCaiWuGen115 && choice.way == JinBiaoQuYongWay.HUAI_YIN) {
                bazi.branches.filter { it.element == yinEl }.distinct()
            } else {
                emptyList()
            },
            gen116 = if (yangRenZiWang116 && choice.way == JinBiaoQuYongWay.SHI_CAI_YONG_CAI) {
                Gen116(
                    bazi.dayPillar.branch, luZhi,
                    bazi.stems.first { it != dayMaster && it.element == guanEl },
                )
            } else {
                null
            },
            wenBao117 = ganDuoBuZai117 && choice.way == JinBiaoQuYongWay.SHANG_YONG_JIE,
            yinWangCai119 = yinWangCai119 && choice.way == JinBiaoQuYongWay.HUAI_YIN,
            geJuZu103Hu = geJuZu103 && hasBenQiRoot(bazi, shiEl) && choice.way == JinBiaoQuYongWay.HUAI_YIN,
            keXie120 = keXieJiaoJia120 && choice.way == JinBiaoQuYongWay.SHANG_YONG_JIE,
            guanGan120 = guanIdx120?.let { bazi.stems[it] },
            guanZuo120 = guanIdx120?.let { bazi.branches[it] },
            youQing121 = qingFen121 && choice.way == JinBiaoQuYongWay.PEI_CAI,
            wuHu121 = wuHu121 && choice.way == JinBiaoQuYongWay.PEI_CAI,
            bingDa122 = bingDa122,
            yongGuan123 = yongGuan123,
            shiZhi124 = youLiShiZhi124 && choice.way == JinBiaoQuYongWay.ZHI_SHA,
            jiaQuGua125 = jiaQuGua125,
            anYin126 = anYin126,
            yingKang127 = yingKang127,
        )
        // 案五补丁：大运连行方局——连续三步大运支成三会方局且方局五行为印比（帮身方向）时，
        // 局内之支皆随方局作印比论（未随南方火局连行——辛未亦帮身破败）；顺势方局（财官食方向）不改支性。
        val yunBranchesSeq = daYun?.yuns?.map { it.pillar.branch } ?: emptyList()
        val bureauOverride = mutableMapOf<Int, Element>()
        for (i in 0 until maxOf(0, yunBranchesSeq.size - 2)) {
            val trio = setOf(yunBranchesSeq[i], yunBranchesSeq[i + 1], yunBranchesSeq[i + 2])
            val juEl = SAN_HUI_JU.firstOrNull { p -> trio.containsAll(p.first) }?.second
            if (juEl != null && (juEl == biEl || juEl == yinEl || juEl == choice.el)) {
                bureauOverride[i] = juEl
                bureauOverride[i + 1] = juEl
                bureauOverride[i + 2] = juEl
            }
        }
        // 案一百一十四戒：运支合日支（病支）化比劫者（戌合卯化火——拱火旺）不随连行方局论（师断戌运不行、离婚）
        ctx.bingZhi114?.let { bz ->
            yunBranchesSeq.forEachIndexed { idx, br ->
                if (br == LIU_HE[bz] && liuHeHuaEl(br, bz) == biEl) bureauOverride.remove(idx)
            }
        }
        val yunPings = daYun?.yuns?.mapIndexed { idx, yun ->
            // 案十一：身弱用印局行印比连行方局（南方火印地）——整运作吉（杀旺印旺身衰补起，富贵）
            // 案一百一十五：身旺印旺用财坏印——运支暗冲印病（申冲寅、酉冲卯）整运作吉（把病去了发大财——师断「走申运发了大财」）
            pingYun(
                bazi.branches, yun.pillar, yun.startAge, yun.startYear, ctx, bureauOverride[idx],
                forceJI = (choice.way == JinBiaoQuYongWay.HUA_SHA && bureauOverride[idx] != null) ||
                    ctx.bingZhis115.any { LIU_CHONG[yun.pillar.branch] == it },
            )
        }

        // ── 断语（一律锚定用神，禁泛断） ──
        val jiYuns = yunPings.orEmpty().filter { it.verdict == JinBiaoVerdict.JI }.map { it.yunName }
        val xiongYuns = yunPings.orEmpty().filter { it.verdict == JinBiaoVerdict.XIONG }.map { it.yunName }
        // 第四讲·五理⑤：二者如替换，定是贫穷汉——原局结构即呈反用之象者
        val tiHuanPin: String? = when {
            !ruoJi && !wangJi && strength == DayStrength.WEAK &&
                shiS >= FOLLOW_MIN_SCORE && guanS >= FOLLOW_MIN_SCORE && yinS < 1.0 &&
                // 案一百一十三戒：食伤泄过为病要印补身之局不以食伤强制——不入贫穷断；
                // 日主有本气根＝有根气（案卅一/卅二：通根得气足以食制）亦非强制
                choice.way != JinBiaoQuYongWay.GUO_XIE_QIU_YIN && !hasBenQiRoot(bazi, biEl) &&
                // 案三十豁免：日主藏干通根≥3处＝有根气，食神制杀成格（贫穷断仅限无根气强制）
                bazi.branches.count { br -> branchHiddenStems.getValue(br).any { it.element == biEl } } < 3 ->
                "五理之五·二者如替换：身弱不用印化杀、原局反以食伤强制动杀（食${format(shiS)}官${format(guanS)}而印仅${format(yinS)}）——克泄交加、弃日主于不顾，定是贫穷汉。"
            !ruoJi && !wangJi && strength == DayStrength.STRONG && !kunLun &&
                guanS >= FOLLOW_MIN_SCORE && yinS >= FOLLOW_MIN_SCORE && shiS < 1.0 ->
                "五理之五·二者如替换：身强反赖印化杀（官${format(guanS)}印${format(yinS)}而食伤仅${format(shiS)}）——身愈强官愈弱，定是贫穷汉。"
            else -> null
        }
        val duanYu = buildString {
            append("日主${strength.chinese}")
            if (ruoJi) append("（弱极·${ruoJiType!!.chinese}）")
            if (qiangJi) append("（强极·${cn.xulun.bazi.model.JinBiaoWangJiType.YIN_BI_TONG_XIN.chinese}）")
            else if (wangJi) append("（旺极·${cn.xulun.bazi.model.JinBiaoWangJiType.BI_DANG_SHI.chinese}）")
            append("，${if (yingKang127) "财官旺·比劫硬扛（身弱用劫——当兵之气势）" else if (jiaQuGua125) "假去寡·印护官（身旺食伤泄秀——走火土运则去寡、走金水运则用官）" else choice.way.chinese}，用神取${yongGod.chinese}（${yongEl.chinese}）")
            if (yongShenStars.isNotEmpty()) append("，见于${yongShenStars.joinToString("、")}")
            else append("——用神五行不现于原局，以虚神论")
            append("。用神${health.level.chinese}（${health.rootNote}）")
            if (health.damages.isNotEmpty()) append("，受${health.damages.size}处克泄耗")
            append("。")
            if (ruoJi) append("此为弱极从势之命，行比劫印地必凶（反常之判，勿以普通身弱喜扶论）。")
            if (ruoJi && !zhengDou) append("随势而走：走财走官走食伤皆顺皆吉（富贵显达、高人一等）；唯走印比帮身运帮之反害，必大凶或贫穷。")
            if (choice.way == JinBiaoQuYongWay.QU_GUA) append("此为去寡之命——不用之官尽可去：违背官杀之运好运（把官杀坏得越严重越好），顺着官杀之运一世无成；日主已旺，不可用水激之。")
            if (choice.way == JinBiaoQuYongWay.YI_ZHI_YI_HUA) append("此为一制一化有情之命——伤官制杀＋印化杀，一制一化有情即有格局（制化两干离得远；贴着则互绊没层次）；行运助制助化皆吉，制化之神被合绊拔根则破格（案九十九）。")
            if (choice.way == JinBiaoQuYongWay.HE_LIU) {
                append("此为合留清格之命——有情即格局：留下之官杀清纯为用，有情就要富贵（当官）。")
                if (gender == cn.xulun.bazi.model.Gender.FEMALE) {
                    append("女命官星得用而清——官星要么代表国家、要么代表丈夫：本人在官方体制有职权，夫亦贵（官星双得之应）。")
                }
            }
            if (choice.way == JinBiaoQuYongWay.GUO_XIE_QIU_YIN) {
                append("此为食伤泄过为病之命——身弱无印而食伤成局（克泄交加）：不以食制，要印补我日主之气（原局印不现，以虚神论——运岁补入之时为应）。运喜去病：运支暗冲病支发福最大（暗冲暗会尤为喜——支神只以冲为重），克病支次之，穿泄病支力最弱——同样是吃药，药跟药有区别。")
            }
            if (anCai114 && choice.way == JinBiaoQuYongWay.SHI_CAI_YONG_CAI) {
                append("此为身旺用暗处之财的命——露者受克、藏者无伤：杀露而坐食伤不行，食伤被印所克不能为用（喜食伤生财），财藏食伤支中未破——用财；印为忌。运喜暗冲去病、暗会财局（更发）；忌合而化比劫（拱火旺——离婚）。")
            }
            if (yangRenZiWang116 && choice.way == JinBiaoQuYongWay.SHI_CAI_YONG_CAI) {
                append("此为羊刃自旺承载食伤生财之命——好命有钱人；官杀露于禄上为病（隐患），局无印生身——身旺亦怕财党官杀。运喜印比（来印运就是好运）；忌官杀得根冲刃拔根（旺者冲衰衰者拔——跟官方扯上关系：蹲大牢）、忌会财局而湿土晦刃。")
            }
            if (ganDuoBuZai117) {
                append("直断：晚婚、克妻离婚；不能有儿子（纵印运生子——运过子没或离婚子跟对方）；无财富——走好运也仅温饱。")
            }
            // 案一百一十九·印旺用财而火土虚透无根——走火土运小康；水木运没有作为
            if (yinWangCai119) append(duanYu119(cb))
            // 案一百二十·伤官用劫而印全无、官杀透干——克泄交加：直断蹲大牢／死；没有印，富裕不了
            if (keXieJiaoJia120) append(zhiDuan120(cb))
            // 案一百二十二·身强用官而病太大——没水（源头）那就不行；金运彻底完了
            if (bingDa122) append(zhiDuan122(cb))
            // 案一百二十三·用官喜帮火——好命而不得志；必须得有木；走水就完了
            if (yongGuan123) append(zhiDuan123(cb))
            // 案一百二十四·有力就能食神制杀——子水运去病发大财；戌运肯定有大灾
            if (shiZhi124) append(zhiDuan124(cb))
            // 案一百二十五·印护官——当兵（兵卒）；假去寡——火土运去寡有层次、金水运用官层次低
            if (jiaQuGua125) append(zhiDuan125(cb))
            // 案一百二十六·坐下暗印——印护官当兵（兵卒）；没学历；有儿子；犯过法——吉神深藏
            if (anYin126) append(zhiDuan126(cb))
            // 案一百二十七·财官旺·比劫硬扛——当兵（兵卒）；学习不好；没有儿子
            if (yingKang127) append(zhiDuan127(cb, yk127, gender))
            if (choice.way == JinBiaoQuYongWay.GONG_SHEN) {
                append("此为财党杀攻身之命——旺财党杀攻身、财先破印：身衰撑不住，非贫即夭（重病之属——肾病严重放弃治疗，身太衰）；丙寅之类印透化杀之运能稍安，然难救本。")
            }
            if (choice.way == JinBiaoQuYongWay.PEI_CAI) {
                append("财滋弱杀与财滋弱官不区分，是一样的（身强＋用官杀＋财生）。")
                // 案一百二十二戒：病太大之局（金得局克尽官之源、源头不现）——勿言「大运解病一样发富发贵」
                // 案一百二十三戒：用官喜帮火之局——走水就去火了，勿言「水运通关一样发富发贵」
                if (caiZhuBing && !bingDa122 && !yongGuan123) append("此为财滋弱杀格（财被劫病局）：杀弱无关系、财弱也无妨——大运解${biEl.chinese}克${caiEl.chinese}之病（${shiEl.chinese}运通关）即一样发富发贵；杀强反不宜，要克身。")
                else append("此为身旺用官之命：帮助官的运（财生官、官杀得地）都算好运，违背官杀、制化官杀之运（食制、印化）就要差。")
                // 案一百二十一·平衡＋情份——走什么都不怕，一生平安（印为闲神护卫）
                if (qingFen121) append(duanYu121(cb))
                if (gender == cn.xulun.bazi.model.Gender.FEMALE) {
                    append("女命用官、官星得用——百分之百：要么在事业单位有官方工作，要么是家庭主妇（官星要么代表国家、要么代表丈夫），不可能在别的地方上班；官星得助，夫运家计应吉。")
                }
            }
            // 案一百二十五戒：印护官之局——印非「格局之病」（无印则犯法），不套「印为病／甲寅去病尤佳」通句
            if (choice.way == JinBiaoQuYongWay.SHI_SHENG_CAI && yinS >= YIN_HUAI && !jiaQuGua125) {
                append("印为格局之病（生身助旺＋贴身克用神）：土金运病来即差——贫穷之运；亥子丑水运帮用神则发富；财运去病（克印）发泄更畅，甲寅之属尤佳。")
            }
            if (qiangJi) {
                append("此为强极印比同心之命，可顺而不可逆：只要运行印比，人生定可亨通显达（好八字）；若走财官之地，触怒强神大凶。")
                if (yinDeJu) {
                    append("幸印星得局（印旺于比），官杀运亦可化煞生印——运支合入印局则大发（寒门贵客）；")
                    append("财地若得食伤干通关或比劫干盖头，顺泄无直接冲克，贫寒不至大凶（大难不死必有后福）。")
                } else {
                    append("印未得局（比不弱于印），走官运必大凶。")
                }
                // 案一百一十八·直断：身旺全憋着——没儿子（走好运亦然）；走金运一定会出灾（不适用返回空串）
                append(duanYu118(cb, yinDeJuDuo))
            } else if (wangJi) {
                append("此为旺极专旺之命，好坏取决于大运：行比劫印绶大运即富贵自然显达（好八字）；行官杀犯旺当知凶祸即至；此格若行财地，立断九死一生。")
                if (!yinDeJuDuo) append("局中印轻（地支不成局），食伤顺泄而行也是好运。") else append("局中印重（地支成局），走食伤运印夺食，不可行。")
            }
            if (jiYuns.isNotEmpty()) append("吉运：${jiYuns.joinToString("、")}。")
            if (xiongYuns.isNotEmpty()) {
                append("凶运：${xiongYuns.joinToString("、")}。")
                if (ruoJi) append("弱极行比劫印运勿看流年，必大凶无疑——轻则破大财，重则牢狱、死亡之灾（应事随八字五行而异）。")
            }
            if (tiHuanPin != null) append(tiHuanPin)
            when (qingZhuo) {
                JinBiaoQingZhuo.QING_KU -> append(
                    // 案一百二十·伤官用劫而印全无、官杀透干——克泄交加
                    if (bingDa122) duanYuQingKu122(cb)
                    else if (keXieJiaoJia120) duanYuQingKu120(cb)
                    else if (ganDuoBuZai117) "此为清枯之命——干多不如支重、承载不了这么旺的财：无财富，走好运也仅温饱。"
                    else "此为清枯之命——用神被坏得太严重、一点救都没有：走好运的时候可能稍微好一点，能得个温饱；走克用神、生忌神之运（如东方木运克土生火）就要一事无成——贫穷清枯。")
                JinBiaoQingZhuo.QUAN_ZHUO -> append("此为全浊之命——官食财俱各有浊神克之，用神无着落：一事无成（学什么学不会、干什么都有人捣乱），比普通老百姓还差——今天干乙木的事没成，明天干庚金的事，就是这样一种状态。")
                JinBiaoQingZhuo.QING -> append(
                    if (choice.way == JinBiaoQuYongWay.KUN_LUN)
                        "此为昆仑顺局之命——原局无病神，然**特殊格局的层次完全是由大运决定**：顺者生源、本势、顺泄皆好运；逆者止水、逆局——来土就完了（层次底下的命）。**主要是看中年：这人的层次是由中年运决定的**（案九十七）。"
                    // 案一百二十三·清命而稍有病——替通句
                    else if (yongGuan123) duanYuQing123(cb)
                    // 案一百二十四·清命而病杀有药——替通句
                    else if (shiZhi124) duanYuQing124(cb)
                    // 案一百二十六·清浊档位为通则映射（师未点名）——师断吉神深藏终身之福：替「贵命走什么运都不怕」通句
                    else if (anYin126) duanYuQing126(cb)
                    // 案一百二十七·层次不高——兵卒的命：替通句
                    else if (yingKang127) duanYuQing127(cb)
                    else "此为清命——用神有力、病神无（或已化已制）：贵命，走什么运都不怕，坏运来了也无病。"
                )
                JinBiaoQingZhuo.MAN_ZHUO -> append("此为满盘浊气之命——病神之力太大、毁坏用神：差命穷命，走什么运也好不了。")
                JinBiaoQingZhuo.BAN_QING -> append("此为半浊半清之命——普通人之命：多成多败、好两年坏两年（帮用去病之运好，帮病之运差）。")
            }
            if (liangShang112Pei != null) {
                append("直断：**克妻损子**（财是妻子——劫刃肆逞，离婚；身旺财为子——财不行找官，官也不行，没儿子）、上学不行——喜用两伤，财运也不行（案一百一十二）。")
            }
            // 案一百二十五／一百二十六／一百二十七·当兵三档之学历已入直断——通则档位标签（「脑子没问题好学」）与师断不合，不附
            if (xueLi != null && xueLi.level != cn.xulun.bazi.model.JinBiaoXueLiLevel.YIN_HUAI &&
                !jiaQuGua125 && !anYin126 && !yingKang127
            ) {
                append("学历：${xueLi.level.chinese}。")
            }
        }

        return JinBiaoResult(
            kouJing = "盲派金镖门 · 用神中心论——帮扶用神则吉，耗泄克破用神则凶；十神代五行，立场以用神为中心",
            strength = strength,
            strengthBasis = "金镖门三步法：月令${monthBranch.chinese}经合会调整属${monthElAdj.chinese}党→" +
                (when {
                    guoXieFanRuo -> "${biEl.chinese}党本已强；然${shiEl.chinese}多（食伤两透党力${format(shiS)}）——日主肯定就过泄了→身弱（${monthBranch.chinese}月${biEl.chinese}寒终究是${biEl.chinese}，虚一点而终是实、亦虚于未戌——异于${EarthlyBranch.CHEN.chinese}之虚）"
                    ruoZhongBianWang -> "弱基调；然坐贴身印比、月令敌党之支被泄克转衰——弱中变旺（身旺）"
                    else -> strength.chinese
                }) +
                (if (anYin126) qiangNote126(cb) else "") +
                (if (yingKang127) qiangNote127(cb, yk127) else "") +
                (if (jiaQuGua125) qiangNote125(cb) else "") +
                (if (shiZhi124) qiangNote124(cb) else "") +
                (if (nianZhuZhuanWang123) qiangNote123(cb) else "") +
                (if (yinBiDeJu122) qiangNote122(cb) else "") +
                (if (qingFen121) qiangNote121(cb) else "") +
                (if (tengLuo) {
                    "；**藤萝系甲（案一百零四·天干章落地）**：乙木无根而甲透——藤萝系甲**直接自旺顶个羊刃（可春可秋——秋天也没事）**；甲木任何位置都可以（年上时上皆算）${if (tengLuoTie) "——本案甲乙相邻（无庚卡中间）：全力之藤" else "——然庚卡甲乙中间则差多了（单说——位置条件）"}；**可别藤萝系乙——没甲木的话就什么都不是了**（必须阳木甲）"
                } else {
                    ""
                }) +
                (if (gongCaiYinHuaRaw && zuoYinWang111) {
                    val gongZhis111 = gongCaiTrio!!.first.filter { it in bazi.branches }.distinct().joinToString("") { it.chinese }
                    "；**印旺身旺（案一百一十一）**：坐下${bazi.dayPillar.branch.chinese}印贴身生、印党力${format(yinS)}压${guanEl.chinese}官杀${format(guanS)}——地支虽有${gongZhis111}拱${caiEl.chinese}、${caiEl.chinese}透干引化，**亦不翻弱**（印生身有依——案四十八豁免同式；对勘案九十三：坐亥泄、食伤透干通源则翻弱）"
                } else {
                    ""
                }) +
                (if (anCai114) {
                    "；**${monthBranch.chinese}虽当令，整个局${biEl.chinese}${yinEl.chinese}一片——身旺**（地支虽是月令当令，仍须看全局之气势；案一百一十四）"
                } else {
                    ""
                }) +
                (if (anCaiWuGen115) {
                    "；**身旺印旺——用财坏印**（建禄通根、${yinEl.chinese}印三支；财透而无根——藏根之支向背不从我；案一百一十五）"
                } else {
                    ""
                }) +
                (if (yangRenZiWang116) {
                    "；**羊刃自旺**（${dayMaster.chinese}自坐${bazi.dayPillar.branch.chinese}刃、又配${luZhi.chinese}禄——身旺，足以承载${shiEl.chinese}生${caiEl.chinese}；然比${shiEl.chinese}${caiEl.chinese}${guanEl.chinese}稍偏弱——旺中带弱；${luZhi.chinese}上之${guanEl.chinese}为病；案一百一十六）"
                } else {
                    ""
                }) +
                (if (ganDuoBuZai117) {
                    "；**干多不如支重**（天干${bazi.stems.count { it.element == biEl }}个${biEl.chinese}而地支全无${biEl.chinese}根——地支不载；${caiEl.chinese}财当令得局——承载不了这么旺的${caiEl.chinese}财；比劫透干多故不能从${caiEl.chinese}财——案一百一十七）"
                } else {
                    ""
                }) +
                (if (strength == DayStrength.WEAK &&
                    ((biEl == Element.WOOD && monthBranch == EarthlyBranch.XU) ||
                        (biEl == Element.FIRE && monthBranch == EarthlyBranch.CHOU) ||
                        (biEl == Element.METAL && monthBranch == EarthlyBranch.CHEN) ||
                        (biEl == Element.WATER && monthBranch == EarthlyBranch.WEI))
                ) {
                    "；${biEl.chinese}生${monthBranch.chinese}月——绝地之方末月**进气**（最怕之方将过、物极必反）：非无敌，只提供力量气数——根虽被冲暗坎，日主之根气由此赋（案九十九）"
                } else {
                    ""
                }) +
                (if (strength == DayStrength.WEAK &&
                    ((biEl == Element.WOOD && monthBranch == EarthlyBranch.CHEN) ||
                        (biEl == Element.FIRE && monthBranch == EarthlyBranch.WEI) ||
                        (biEl == Element.METAL && monthBranch == EarthlyBranch.XU) ||
                        (biEl == Element.WATER && monthBranch == EarthlyBranch.CHOU))
                ) {
                    "；${biEl.chinese}生${monthBranch.chinese}月——旺地之方末月**退气**（飞龙在天、再上即亢龙有悔——开始走下坡路）：**退气也算通根**（只是力量没那么旺）——进退气是判断力量的辅助，不是绝对因素（案一百）"
                } else {
                    ""
                }) +
                (if (yinWangCai119) qiangNote119(cb) else "") +
                (if (keXieJiaoJia120) buCong120(cb) else "") +
                "（八步法参考：${strengthResult.verdict.chinese}，生助占比${pct(aShare)}）",
            ruoJi = ruoJi,
            ruoJiType = ruoJiType,
            ruoJiBasis = ruoJiBasis,
            wangJi = wangJi || qiangJi,
            wangJiType = when {
                qiangJi -> cn.xulun.bazi.model.JinBiaoWangJiType.YIN_BI_TONG_XIN
                wangJi -> cn.xulun.bazi.model.JinBiaoWangJiType.BI_DANG_SHI
                else -> null
            },
            wangJiBasis = wangJiBasis,
            quYongWay = choice.way,
            yongShenGod = yongGod,
            yongShenElement = yongEl,
            yongShenStars = yongShenStars,
            quYongBasis = choice.basis,
            daiMing = daiMing,
            health = health,
            xiElements = xiElements,
            jiElements = jiElements,
            yuns = yunPings.orEmpty(),
            qingZhuo = qingZhuo,
            qingZhuoBasis = qingZhuoBasis,
            duanYu = duanYu,
            ziNv = ziNv,
            xueLi = xueLi,
            note = "金镖门讲义已授一至三讲（弱极从势／旺极专旺／强极印比同心）＋案一~案三补丁（藏根叛向·三会作废·印比支藏敌党作废·得局＝印旺于比·干化于支·化煞合局大发·财地软档）；" +
                "身强/中和取用、特殊格局篇、壬寅大运专题待师传档；弱极阈值（占比$RUO_JI_SHARE、微浮党力$WEI_FU_MAX_SCORE）、强极印线$QIANG_JI_YIN 为基线参数，以事实案例为准打补丁。" +
                "带有事实的案例须同时以老引擎（下方传统综合区块）对照分析。",
        )
    }

    // ── 内部工具 ──

    /** 用神十神之代称：原局有该五行之字则取最有力的透干/本气字之十神，无则按阴阳定偏正。 */
    private fun representativeGod(bazi: BaziResult, dayMaster: HeavenlyStem, el: Element): TenGod {
        bazi.stems.forEachIndexed { i, st ->
            if (i != 2 && st.element == el) return tenGodOfStem(dayMaster, st)
        }
        bazi.branches.forEach { br ->
            if (br.element == el) return tenGodOfStem(dayMaster, branchHiddenStems.getValue(br).first())
        }
        // 用神不现：取与日主同阴阳之干定偏正
        val rep = elementStems.getValue(el).first { it.yang == dayMaster.yang }
        return tenGodOfStem(dayMaster, rep)
    }

    /** 命中用神五行之字及位置（透干优先；日主自身不计）。 */
    private fun locateElementStars(bazi: BaziResult, dayMaster: HeavenlyStem, el: Element): List<String> {
        val out = mutableListOf<String>()
        bazi.stems.forEachIndexed { i, st ->
            if (i != 2 && st.element == el) {
                out += "${PillarStemLabels[i]}${st.chinese}（${tenGodOfStem(dayMaster, st).chinese}）"
            }
        }
        bazi.branches.forEachIndexed { i, br ->
            if (br.element == el) {
                out += "${PillarBranchLabels[i]}${br.chinese}本气（${tenGodOfStem(dayMaster, branchHiddenStems.getValue(br).first()).chinese}）"
            } else {
                branchHiddenStems.getValue(br).forEach { hd ->
                    if (hd.element == el) {
                        out += "${PillarBranchLabels[i]}${br.chinese}中${hd.chinese}（${tenGodOfStem(dayMaster, hd).chinese}）"
                    }
                }
            }
        }
        return out
    }

    /** 用神健全度：通根 → 得扶 → 受伤（克/泄/耗/冲/合绊）。 */
    private fun assessHealth(bazi: BaziResult, dayMaster: HeavenlyStem, yongEl: Element): JinBiaoYongShenHealth {
        val benQiRoots = bazi.branches.filter { it.element == yongEl }
        // 案一百一十五·根之向背：藏干根之支本气克用神，而其五行当令、地支有其生源成党（火当令、木生火——木火两旺）
        // ——此支「跟克我者一伙」，不作我根（巳中庚：巳是根没错，但巳跟木火一伙——木火旺还能说巳是金的根吗）
        val genXiangBei115 = bazi.branches.filter { br ->
            val el = br.element
            el != yongEl && el.controls == yongEl && branchHiddenStems.getValue(br).any { it.element == yongEl } &&
                (bazi.monthPillar.branch.element == el ||
                    bazi.branches.count { it.element == el || it.element.generates == el } >= 3)
        }
        val cangRoots = bazi.branches.filter {
            it.element != yongEl && branchHiddenStems.getValue(it).any { hd -> hd.element == yongEl } && it !in genXiangBei115
        }
        // 案五十六·三会化根：三会成局而局非用神之党——局内用神本气根/藏根随局走（把比劫都化了——用神无力）
        val sanHuiHuaJu = SAN_HUI_JU.firstOrNull { (bs, _) -> bs.all { it in bazi.branches } }
            ?.let { (bs, el) -> if (el != yongEl) bs else null }
        val effBenQi = if (sanHuiHuaJu != null) benQiRoots.filter { it !in sanHuiHuaJu } else benQiRoots
        val effCang = if (sanHuiHuaJu != null) cangRoots.filter { it !in sanHuiHuaJu } else cangRoots
        val huaJinNote = if (sanHuiHuaJu != null && (benQiRoots.any { it in sanHuiHuaJu } || cangRoots.any { it in sanHuiHuaJu })) {
            val huaDiao = (benQiRoots.filter { it in sanHuiHuaJu } + cangRoots.filter { it in sanHuiHuaJu }).joinToString("、") { it.chinese }
            "（${huaDiao}随三会局化——把根都化了，用神无力）"
        } else {
            null
        }
        val rootNote = when {
            effBenQi.isNotEmpty() -> "通本气根（${effBenQi.joinToString("、") { it.chinese }}）" + (huaJinNote ?: "")
            effCang.isNotEmpty() -> "藏干根（${effCang.joinToString("、") { it.chinese }}）" + (huaJinNote ?: "")
            huaJinNote != null -> "本气根藏根随三会局化尽$huaJinNote"
            // 案一百一十五·根之向背：藏根之支本气克我而其党当令成势——此根随支向我之敌，不计
            genXiangBei115.isNotEmpty() ->
                "无根虚浮（${genXiangBei115.joinToString("、") { it.chinese }}中虽有${yongEl.chinese}——${genXiangBei115.joinToString("、") { it.chinese }}跟${Element.entries.first { e -> e.controls == yongEl }.chinese}一伙：不作${yongEl.chinese}根——根之向背）"
            else -> "无根虚浮"
        }

        val supports = mutableListOf<String>()
        val damages = mutableListOf<String>()
        // 案一百一十五·根之向背：藏根之支本气克我而其党当令成势——此根随支向我之敌（巳中庚：巳跟木火一伙）
        genXiangBei115.forEach { br ->
            damages += "${br.chinese}中${yongEl.chinese}根随支向${br.element.chinese}（${br.chinese}是根没错，然${br.chinese}跟${br.element.chinese}一伙——不作${yongEl.chinese}根：根之向背）"
        }

        // 生用/同用之字（不含用神自身首次出现）
        bazi.stems.forEachIndexed { i, st ->
            if (i != 2 && st.element.generates == yongEl) {
                supports += "${PillarStemLabels[i]}${st.chinese}生用"
            }
        }
        bazi.branches.forEachIndexed { i, br ->
            if (br.element.generates == yongEl) supports += "${PillarBranchLabels[i]}${br.chinese}生用"
        }

        // 克用/泄用/耗用之字
        bazi.stems.forEachIndexed { i, st ->
            if (i != 2) {
                val act = when {
                    st.element.controls == yongEl -> "克用"
                    yongEl.generates == st.element -> "泄用"
                    yongEl.controls == st.element -> "耗用"
                    else -> null
                }
                if (act != null) damages += "${PillarStemLabels[i]}${st.chinese}$act"
            }
        }
        bazi.branches.forEachIndexed { i, br ->
            val el = br.element
            val act = when {
                el.controls == yongEl -> "克用"
                yongEl.generates == el -> "泄用"
                yongEl.controls == el -> "耗用"
                else -> null
            }
            if (act != null) damages += "${PillarBranchLabels[i]}${br.chinese}$act"
        }

        // 根被冲——案廿六：冲对之间（隔位）有通关之支卡中属友情，冲化为生情分加分；
        // 案四十五戒：贴身相邻之冲无支可卡——支神只以冲为重，破坏用神（基本无格），
        // 若另有一支生用神（如卯生巳）——家里还有生的，把用神补起来（半救，绝处逢生待运）
        bazi.branches.forEachIndexed { i, br ->
            val chongPartner = LIU_CHONG[br]
            if (chongPartner != null && br.element == yongEl &&
                bazi.branches.any { it == chongPartner }
            ) {
                val pIdx = bazi.branches.indexOf(chongPartner)
                val lo = minOf(i, pIdx)
                val hi = maxOf(i, pIdx)
                val mid = bazi.branches.subList(lo + 1, hi).firstOrNull { m ->
                    chongPartner.element.generates == m.element && m.element.generates == br.element
                }
                if (mid != null) {
                    supports += "根${br.chinese}与${chongPartner.chinese}之冲有${mid.chinese}卡中通关（属友情）——冲化为生，情分加分"
                } else {
                    damages += "${br.chinese}用神之根被${chongPartner.chinese}冲——支神只以冲为重，破坏用神"
                    val repair = bazi.branches.firstOrNull { it != br && it != chongPartner && it.element.generates == yongEl }
                    if (repair != null) {
                        supports += "${repair.chinese}生${br.chinese}用神——家里还有生的，把用神印星补起来（半救）；待运支合去${chongPartner.chinese}冲神——绝处逢生，彻底有情"
                    }
                }
            }
        }
        // 案五十五·六合化局补用神（绝处逢生有情）：用神本气之支与他支六合而化用神之五行
        // （寅亥化木——财被重重围克已破格，化局把用神补起来：财稍弱，但不至于用不了）
        bazi.branches.firstOrNull { br ->
            br.element == yongEl && LIU_HE[br]?.let { p -> p in bazi.branches && liuHeHuaEl(br, p) == yongEl } == true
        }?.let { br ->
            val p = LIU_HE[br]!!
            supports += "${br.chinese}${p.chinese}合化${yongEl.chinese}——化局补用神（绝处逢生有情）：用神稍弱被补起来，不至于用不了"
        }
        // 用神透干被合绊（案九十九戒：须**贴着**（相邻）才合绊——隔位不合，
        // 「离得远就有情」：丁壬遥隔＝一制一化之配合，非绊）
        bazi.stems.forEachIndexed { i, st ->
            val hePartner = GAN_HE[st]
            if (i != 2 && st.element == yongEl && hePartner != null &&
                bazi.stems.anyIndexed { j, o -> j != 2 && j != i && o == hePartner && kotlin.math.abs(j - i) == 1 }
            ) {
                damages += "${st.chinese}用神透干被${hePartner.chinese}贴身合绊"
            }
        }
        // 案一百一十一·官坐食伤（自合）不碍官：用神即官、官干同柱坐食伤本气支、食伤不透而被印支贴克
        // ——亥水被土克着没力量、又跟财一伙（拱局）且润印养财：丁火官没问题
        run {
            val dayElH = dayMaster.element
            val yinElH = Element.entries.first { it.generates == dayElH }
            val shiElH = dayElH.generates
            val caiElH = dayElH.controls
            val guanTouZi = bazi.stems.indices.firstOrNull { i ->
                i != 2 && bazi.stems[i].element == yongEl && bazi.branches[i].element == shiElH
            }
            val keZhe = guanTouZi?.let { gi ->
                bazi.branches.indices.filter { j -> j != gi && (kotlin.math.abs(j - gi) == 1) && bazi.branches[j].element.controls == shiElH }
            }
            if (yongEl.controls == dayElH && guanTouZi != null && !keZhe.isNullOrEmpty() &&
                bazi.stems.none { it != dayMaster && it.element == shiElH }
            ) {
                supports += "${bazi.stems[guanTouZi].chinese}坐${bazi.branches[guanTouZi].chinese}（自合）不碍${yongEl.chinese}用神：${bazi.branches[guanTouZi].chinese}被${bazi.branches[keZhe.first()].chinese}${yinElH.chinese}克着没力量、又跟${caiElH.chinese}一伙（拱局）且润${yinElH.chinese}养${caiElH.chinese}——${yongEl.chinese}官没问题（案一百一十一）"
            }
        }

        val level = when {
            rootNote.startsWith("无根") && damages.isNotEmpty() -> JinBiaoHealth.WU_QI
            rootNote.contains("化尽") && damages.isNotEmpty() -> JinBiaoHealth.WU_QI
            damages.isEmpty() -> JinBiaoHealth.DE_LI
            else -> JinBiaoHealth.DAI_SHANG
        }
        return JinBiaoYongShenHealth(level = level, rootNote = rootNote, supports = supports, damages = damages)
    }

    /** 运评上下文（案三补丁：干化于支／化煞生印／财地软档；命一补丁：旺强极干支分论）。 */
    private class YunCtx(
        val yongEl: Element,
        val biEl: Element,
        val yinEl: Element,
        val shiEl: Element,
        val caiEl: Element,
        val guanEl: Element,
        val qiangJi: Boolean,
        val yinDeJu: Boolean,
        val wangJiFamily: Boolean,       // 旺极/强极
        val yinDeJuDuo: Boolean,         // 案一百一十八：印得局（地支印本气≥3／三合印局支全）——食伤运干支同被枭夺；未得局（一二印支）则食伤顺泄没事
        val chongShen: EarthlyBranch? = null, // 案四十五：用神根之冲神——运支六合合去冲神＝绝处逢生
        val favorEls: Set<Element>,      // 作吉论之五行（旺极印轻食伤顺泄）
        val zhongPingEls: Set<Element>,  // 作中平论之五行（解围局官/食）
        val daXiongEls: Set<Element>,    // 作大凶论之五行（弱极比印/旺强极官财）
        val rootlessEls: Set<Element>,   // 局中党力为零之五行（干生支则化于支）
        val daXiongText: Map<Element, String>,
        val zhongPingText: Map<Element, String>,
        val favorText: Map<Element, String>,
        val yongYin: Boolean = false,        // 身弱用印局（HUA_SHA）——运支合补印局大发（案十三）
        val yongJie: Boolean = false,        // 伤官用劫档（案五十二）——用神为比劫：泄比劫＝差而没什么大事（毕竟没有被克），克比劫＝出事
        val jiShenTouGans: List<HeavenlyStem> = emptyList(), // 案五十三：透干忌神（克用神之五行透干）——运干克去＝去忌神之运最好
        val yongCai: Boolean = false,          // 案五十四：用财局——运支为透干比劫之禄/长生根＝比劫见根，死亡级大祸
        val biTouGan: List<HeavenlyStem> = emptyList(), // 透干比劫（不含日主）
        val yongWuLi: Boolean = false,         // 案五十六：用神被三会化无力——泄耗亦穷困（不作中平），官杀克用神＝死亡
        val renZhi: EarthlyBranch? = null,     // 案六十五：日主（阳干）之羊刃支——在局为病，运支冲之＝去刃病吉
        val chongYinGen: EarthlyBranch? = null, // 案九十四：用神印之根支——运支贴冲＝拔根（支神只以冲为重，刑与穿兮动不动）
        val jia95: Boolean = false,            // 案九十五：过泄配印局——用神库支运＝得根发财／用神透干＝发财／喜神填拱＝变旺好运
        val yongKu: EarthlyBranch? = null,     // 用神之墓库支（火→戌之属——运支临之＝用神得库根）
        val xiShenEl: Element? = null,         // 喜神（生用神之源）——运支填其三合拱局＝喜神变旺
        val quGuaGan: HeavenlyStem? = null,    // 案九十六：去寡局所去之官杀透干——运干同性克之＝去尽（伤官伤尽始为奇）
        val jia98: Boolean = false,            // 案九十八：昆仑顺局而透克神（两土皆病）——运干食伤到＝去病（稍好）；支火土助病＝坏事多
        val zhiShaCai: Boolean = false,        // 案一百零一：食神制杀身旺局——财星到＝食神生财（发财，杀已被制财来即富）
        val xiuQi108: Boolean = false,         // 案一百零八：秀气流行局——运支成食伤三合（酉丑化金）＝层次巅峰
        val liangRen110: Boolean = false,      // 案一百一十：坐两刃身旺泄秀局——运支冲用神本气根（酉冲卯）＝怕
        val bingZhi113: EarthlyBranch? = null, // 案一百一十三：食伤泄过为病之病支（三合局中神——午）：运支暗冲（子）去病发福最大／克病（亥）次之／穿泄（丑）力最弱
        val bingZhi114: EarthlyBranch? = null, // 案一百一十四：身旺用暗财之病支（坐下印克食伤——卯）：运支暗冲去病＋暗会财局（酉）——更发；合之化比劫（戌）——拱火旺
        val bingZhis115: List<EarthlyBranch> = emptyList(), // 案一百一十五：身旺印旺用财坏印之病支（印本气支——寅卯）：运支暗冲去病（申冲两寅、酉冲卯）——发大财；兼六合局支者暗会
        val gen116: Gen116? = null, // 案一百一十六：羊刃自旺之命根——自坐之刃、被官所盖之禄：官杀之支冲刃拔根＝牢狱；会财局而泄刃之湿土＝晦火
        val wenBao117: Boolean = false, // 案一百一十七：干多不如支重·财旺不载——用神无根无力：帮用之运亦仅温饱（吉档降中平）
        val yinWangCai119: Boolean = false, // 案一百一十九：印旺用财而火土虚透无根——火土运小康；水木运差；单天干木运一般；湿土随三会水局不止水
        val geJuZu103Hu: Boolean = false, // 案一百零三（案一百一十九对勘）：印旺用财而地支有食伤本气根（午火根）——地支来木也没事（生午火了），木运不如火土运
        val keXie120: Boolean = false, // 案一百二十：伤官用劫而印全无、官杀透干——克泄交加：比劫运仅温饱（中平）；印运克食伤方平衡；官杀运差（死亡／牢狱）
        val guanGan120: HeavenlyStem? = null, // 案一百二十：透干之官杀（如戊）及其坐支（如午）——「戊午运克泄交加」
        val guanZuo120: EarthlyBranch? = null,
        val youQing121: Boolean = false, // 案一百二十一：身旺用官而印透（闲神护卫）——走什么都不怕：非财官之运皆中平（闲神立功）
        val wuHu121: Boolean = false, // 案一百二十一反事实：身旺用官无印护卫——伤官运克官：犯官灾
        val bingDa122: Boolean = false, // 案一百二十二：身强用官而病太大（金得局克尽官之源、源头水不现）——水运仅凑合（中平）；金运彻底完了
        val yongGuan123: Boolean = false, // 案一百二十三：用官喜帮火——丙戌火通根作吉；木运补官之源（中平——不如丙戌）；走水就完了；土泄火
        val shiZhi124: Boolean = false, // 案一百二十四：有力就能食神制杀——水（药神）去病作吉（子运发大财）；比劫帮身作吉；戌（印克食伤兼拱火）大灾；丑（湿土化杀）作吉
        val jiaQuGua125: Boolean = false, // 案一百二十五：假去寡·印护官——火土运去寡（火通关）作吉（午运发财、戊辰运拆迁款）；金水运只能用官（层次低）、印运木旺没法去寡——中平
        val anYin126: Boolean = false, // 案一百二十六：坐下暗印——用印；伤官见官无印则犯法、有暗印护官则当兵（吉神深藏终身之福）；流年印露＝当一年兵
        val yingKang127: Boolean = false, // 案一百二十七：财官旺·比劫硬扛——身弱用劫（当兵之气势）；子水大运官杀克比劫——肯定要犯法
    )

    /** 案一百一十六·羊刃自旺之命根：自坐之刃、被官所盖之禄、露干之官。 */
    private class Gen116(val ren: EarthlyBranch, val lu: EarthlyBranch, val guan: HeavenlyStem)

    /** 阳干羊刃支（甲刃卯、丙戊刃午、庚刃酉、壬刃子；阴干无刃）。 */
    private val YANG_REN: Map<HeavenlyStem, EarthlyBranch> = mapOf(
        HeavenlyStem.JIA to EarthlyBranch.MAO,
        HeavenlyStem.BING to EarthlyBranch.WU,
        HeavenlyStem.WU to EarthlyBranch.WU,
        HeavenlyStem.GENG to EarthlyBranch.YOU,
        HeavenlyStem.REN to EarthlyBranch.ZI,
    )

    /** 单步大运评断（含逐年流年；流年同用大运之档位口径，细分待壬寅专题）。[zhiBureauEl]＝连行方局之印比覆写；[forceJI]＝用印局印比连行方局整运作吉。 */
    private fun pingYun(
        chartBranches: List<EarthlyBranch>,
        pillar: Pillar,
        startAge: Int,
        startYear: Int,
        ctx: YunCtx,
        zhiBureauEl: Element? = null,
        forceJI: Boolean = false,
    ): JinBiaoYunPing {
        val items = ganZhiItems(chartBranches, pillar.stem, pillar.branch, ctx, zhiBureauEl)
        val years = (startYear..startYear + 9).map { year ->
            val ln = Pillar.fromCycleIndex(year - 4)
            val lnItems = ganZhiItems(chartBranches, ln.stem, ln.branch, ctx, lnAge = startAge + (year - startYear))
            JinBiaoLiuNianPing(year = year, pillarName = ln.fullName, items = lnItems, verdict = verdictOf(lnItems, ctx))
        }
        return JinBiaoYunPing(
            yunName = "${pillar.fullName}运",
            ageRange = "${startAge}-${startAge + 9}岁",
            yearRange = "$startYear-${startYear + 9}",
            items = items,
            verdict = if (forceJI) JinBiaoVerdict.JI else verdictOf(items, ctx),
            years = years,
        )
    }

    /** 一柱干支对用神的作用条目（大运/流年通用）。[zhiElOverride]＝连行方局帮身覆写（大运支专用）。 */
    private fun ganZhiItems(
        chartBranches: List<EarthlyBranch>,
        gan: HeavenlyStem,
        zhi: EarthlyBranch,
        ctx: YunCtx,
        zhiElOverride: Element? = null,
        // 流年虚岁（≥0 即流年条目；大运条目为 -1）——案一百二十六「流年印露出来，都有当一年兵的」仅注流年
        lnAge: Int = -1,
    ): List<JinBiaoStanceItem> {
        val ganEl = gan.element
        val zhiEl = zhiElOverride ?: zhi.element
        val bureauNote =
            if (zhiElOverride != null && zhiElOverride != zhi.element) "·${zhi.chinese}随连行方局以${zhiElOverride.chinese}论（帮身方向）" else ""
        // 案一百一十九·湿土随三会水局：印旺用财之局，运支丑与局支亥子会成亥子丑——「丑是湿土，基本上起不到止水的用」
        val suiShui119 = ctx.yinWangCai119 && zhi == EarthlyBranch.CHOU &&
            chartBranches.count { it == EarthlyBranch.HAI || it == EarthlyBranch.ZI } >= 2
        val zhiEl119 = if (suiShui119) ctx.yinEl else zhiEl

        // 案三专题先记：八字重地支——干生支而干在局无根，干力全化于支（壬水全生寅木，壬寅以寅论）；
        // 命一补丁：旺强极局之官干不化——权在一人不可触犯，直克即激怒（丙运死亡）
        if (ganEl != zhiEl && ganEl.generates == zhiEl && ganEl in ctx.rootlessEls &&
            !(ctx.wangJiFamily && ganEl == ctx.guanEl) &&
            // 案一百一十三戒：用神透干不化于支——原局不现之用神，运岁补入之时为应
            ganEl != ctx.yongEl
        ) {
            val (stance, act) = stanceAction(ctx.yongEl, zhiEl119)
            val huaSha = huaShaNote(zhi, zhiEl119, chartBranches, ctx)
            val note = huaSha?.second
                ?: (if (ctx.yinWangCai119) yunNote119(1, gan, zhi, zhiEl119, ctx, suiShui119, zhiEl119) else null)
                ?: ctx.daXiongText[zhiEl119]
                ?: ctx.zhongPingText[zhiEl119]
                ?: ctx.favorText[zhiEl119]
            return listOf(
                JinBiaoStanceItem(
                    label = "干化于支",
                    chinese = "${gan.chinese}${zhi.chinese}",
                    element = zhiEl119,
                    stance = stance,
                    action = "${gan.chinese}无根全生${zhi.chinese}——干化于支（八字重地支）·${act}" +
                        (note?.let { "·$it" } ?: "") + bureauNote,
                    favorOverride = huaSha?.first == true,
                    pingOverride = huaSha != null && huaSha.first == false,
                ),
            )
        }

        return listOf(ganEl to gan.chinese, zhiEl119 to zhi.chinese).mapIndexed { i, (el, chinese) ->
            val (stance, act) = stanceAction(ctx.yongEl, el)
            // 强极财地软档（案三）：财支得食伤干通关（泄旺生财）或比劫干盖头（干克支）——顺泄无直接冲克
            if (i == 1 && el == ctx.caiEl && ctx.qiangJi && ganEl != zhiEl &&
                (ganEl == ctx.shiEl || ganEl == ctx.biEl)
            ) {
                val soften = if (ganEl == ctx.shiEl) "${gan.chinese}干通关（泄旺生财）" else "${gan.chinese}干盖头（干克支）"
                JinBiaoStanceItem(
                    label = "运支",
                    chinese = chinese,
                    element = el,
                    stance = stance,
                    action = "$act·$soften——顺泄无直接冲克，贫寒不顺，不至大凶（大难不死必有后福）",
                )
            } else {
                // 案四十五：运支六合合去用神之冲神——用神绝处逢生，彻底有情
                val heChong = i == 1 && ctx.chongShen != null && LIU_HE[zhi] == ctx.chongShen
                val huaSha = if (el == ctx.guanEl) huaShaNote(zhi, el, chartBranches, ctx, isStem = i == 0) else null
                // 案十三：身弱用印局——运支与局支合补印局（三合/六合化印，如亥未拱木逢卯）——用神得局大发
                val yinJuBu = i == 1 && ctx.yongYin && el == ctx.yinEl && heYinJu(zhi, chartBranches, ctx.yinEl)
                // 案五十二：运支填实原局拱合待成之局（亥未拱木待卯——卯来成亥卯未）——局神得势：
                // 局神克用神→克掉用神就要出事（伤官见官为祸百端之属）；局神为用神之印→用神得局大发（案十三吉向）；
                // 局神为药神（通关解病，案廿二申子辰水局泄金生木）→不作凶论——药神到一样富贵
                val sanHeChengEl = if (i == 1) sanHeCompletedEl(zhi, chartBranches) else null
                // 案六十五·去刃病：用财局而**阳刃支在局**为病——运支六冲刃支＝大运来药去掉这个病
                // （十几岁庚子运去掉羊刃病：学习好、上班后财运好——就在好运上；
                //  案五四戒：禄支被冲非去刃病——平常论）
                val quRenBing = i == 1 && ctx.yongCai && ctx.renZhi != null &&
                    ctx.renZhi in chartBranches && zhi == LIU_CHONG[ctx.renZhi] &&
                    // 案一百一十六戒：羊刃自旺之局——刃为命根非病（冲之即拔根——牢狱），不作去刃病论
                    ctx.gen116 == null
                // 案七十六·亥运去火存金：用财局而运支亥冲局中巳（亥巳逢冲——去火存金）：
                // 官被冲去、比劫复旺来克财——老破财（今儿挣一百明儿破八十——各种事捣乱）
                val haiSiQuHuo = i == 1 && zhi == EarthlyBranch.HAI && EarthlyBranch.SI in chartBranches &&
                    ctx.yongEl.generates == ctx.guanEl && ctx.guanEl == Element.FIRE
                // 案七十八·寅亥化木翻身：配印局而运支寅合局中亥（合去坏印之财根）＋木生火帮印——
                // 三十年河东河西（坎离震兑各管三十年）：北方差三十年，东方翻身——农民翻身把歌唱
                val yinHaiFanShen = i == 1 && zhi == EarthlyBranch.YIN && EarthlyBranch.HAI in chartBranches &&
                    ctx.yongEl == ctx.yinEl && ctx.caiEl.controls == ctx.yinEl && zhi.element.generates == ctx.yongEl
                // 案八十四·运支填拱：财拱局暗存而运支填其所缺（亥未待卯——卯来填拱）——
                // 财局兑现：发大财＋一下就有子（玄机暗里存——应期）
                // 案八十七·运支填成食伤局：用财局而运支填成食伤三合局（寅午待戌——戌来寅午戌成）——
                // 通关化劫之局兑现：才挣到钱（小康）；此前比劫夺财挣不着
                // （案九十四戒：限用财局——用印局之食伤局支叠强反为过泄，不作此论）
                val tianShiJu = i == 1 && ctx.yongCai &&
                    listOf(
                        setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI) to Element.WOOD,
                        setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU) to Element.FIRE,
                        setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN) to Element.WATER,
                        setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU) to Element.METAL,
                    ).any { (trio, el) -> el == ctx.shiEl && zhi in trio &&
                        chartBranches.count { it in trio } >= 2 }
                val tianGong = i == 1 && zhi.element == ctx.caiEl &&
                    // 案九十三戒：所填财局之神克用神（拱财坏印之局——填拱＝坏印成真）不作财气通门户论
                    ctx.caiEl.controls != ctx.yongEl &&
                    listOf(
                        setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI) to Element.WOOD,
                        setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU) to Element.FIRE,
                        setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN) to Element.WATER,
                        setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU) to Element.METAL,
                    ).any { (trio, el) -> el == ctx.caiEl && zhi in trio &&
                        chartBranches.count { it in trio } >= 2 && !trio.all { it in chartBranches } }
                val sanHeCheng = sanHeChengEl != null && sanHeChengEl!!.controls == ctx.yongEl &&
                    sanHeChengEl !in ctx.favorEls && sanHeChengEl !in ctx.zhongPingEls
                // 案五十三·去忌神：运干克去透干忌神——去忌神即去病，用神无克，去忌之运最好
                val quJiShen = i == 0 && ctx.jiShenTouGans.isNotEmpty() &&
                    ctx.jiShenTouGans.any { ganEl.controls == it.element }
                // 案五十四·用财局比劫见根：运支为透干比劫之禄/长生根位（寅＝丙长生——支虽属木，正是丙火之根）——
                // 比劫变强，通关之神被焚（火旺土燥不生金），直接克掉用神财——死亡级大祸。
                // 案五十五戒：支行为食伤（通关之神到——通关更力，反吉：壬子）或官（官到护财）——不凶
                val biJianGen = i == 1 && ctx.yongCai &&
                    ctx.biTouGan.any { bt -> zhi in LU_ZHANG.getValue(bt) } &&
                    (el == ctx.biEl || el.controls == ctx.yongEl || ctx.yongEl.controls == el) &&
                    el != ctx.shiEl && el != ctx.guanEl
                // 命一补丁·旺强极干支分论：
                // ① 官杀干直克身——犯旺激怒，权在一人不可触犯（虽得局亦然——丙运死亡）
                // ② 食伤干泄可行（天干无印不夺——顺其旺势）；食伤支印得局则枭夺（地支不能泄）
                // 案一百一十八：运柱干支皆食伤（庚申／辛酉）而印得局——干支一体同被枭夺（「走金运一定会出灾——来了金就是枭神夺食」）；
                //   干泄而支非食伤者（命一壬申）仍按「天干泄秀可行」；印未得局（一二印支）——走食伤运没事
                val fanWang = ctx.wangJiFamily && i == 0 && el == ctx.guanEl
                val zhiDuo = ctx.wangJiFamily && i == 1 && el == ctx.shiEl && ctx.yinDeJuDuo
                val yiTiDuo118 = ctx.wangJiFamily && i == 0 && el == ctx.shiEl && ganEl == zhiEl && ctx.yinDeJuDuo
                val shunXieGan = ctx.wangJiFamily && i == 0 && el == ctx.shiEl && !yiTiDuo118
                // 案一百一十九·印旺用财而火土虚透无根：单天干木运（生食伤兼克财）——支属火土者以火土运论（作吉：小康），否则一般的运（中平）
                val yun119 = ctx.yinWangCai119
                val ganMu119 = yun119 && i == 0 && el == ctx.biEl
                val zhiBang119 = ganMu119 && stanceAction(ctx.yongEl, zhiEl119).first == JinBiaoStance.BANG
                // 案一百零三对勘（案一百一十九）：印旺用财而地支有食伤本气根（午火根）——地支来木也没事（生午火了），木运不如火土运
                val muYun103 = ctx.geJuZu103Hu && el == ctx.biEl
                // 案一百二十·伤官用劫而印全无、官杀透干（克泄交加）：比劫帮用亦仅温饱；官杀克比劫用神——差运
                val biYun120 = ctx.keXie120 && stance == JinBiaoStance.BANG && el == ctx.biEl
                val guanYun120 = ctx.keXie120 && el == ctx.guanEl
                // 案一百二十：透官之柱（戊午）整柱再来——克泄交加，死亡
                val guanZhu120 = ctx.keXie120 && i == 0 && gan == ctx.guanGan120 && zhi == ctx.guanZuo120
                // 案一百二十一·身旺用官而印透（闲神护卫）——走什么都不怕：非财官之运皆中平（闲神立功）；反事实（无印护卫）——伤官克官：犯官灾
                val xianShen121 = ctx.youQing121 && el != ctx.caiEl && el != ctx.guanEl
                val fanGuanZai121 = ctx.wuHu121 && el == ctx.shiEl
                // 案一百二十二·身强用官而病太大：来水（找源头）仅凑合——中平；金运彻底完了——凶
                val shuiYun122 = ctx.bingDa122 && el == ctx.shiEl
                val jinYun122 = ctx.bingDa122 && el == ctx.biEl
                // 案一百二十三·用官喜帮火：官杀干坐火库（丙戌——火通根）作吉；木运补官之源——中平（不如丙戌）；走水就完了；土泄火
                val huoTongGen123 = ctx.yongGuan123 && i == 1 && zhi.element == ctx.yinEl && gan.element == ctx.guanEl
                val caiYun123 = ctx.yongGuan123 && i == 0 && el == ctx.caiEl
                val shuiYun123 = ctx.yongGuan123 && el == ctx.shiEl
                val tuYun123 = ctx.yongGuan123 && el == ctx.yinEl && !huoTongGen123
                // 案一百二十四·有力就能食神制杀：水（药神）去病作吉；比劫帮身作吉；印干坐药神支／丑（化杀）作吉；戌（印克食伤兼拱火）大灾
                val shuiYao124 = ctx.shiZhi124 && el == ctx.shiEl
                // 戌（大灾）与丑（湿土化杀）按运支本气论——连行方局之覆写（戌随金、丑随水）不夺其性
                val xuZai124 = ctx.shiZhi124 && i == 1 && zhi == EarthlyBranch.XU
                val chouHua124 = ctx.shiZhi124 && zhi == EarthlyBranch.CHOU && !xuZai124
                val biBang124 = ctx.shiZhi124 && el == ctx.biEl && !xuZai124 && !chouHua124
                val yinZuoYao124 = ctx.shiZhi124 && i == 0 && el == ctx.yinEl && zhi.element == ctx.shiEl
                // 案一百二十五·假去寡（按运柱干支本行论——连行方局之覆写不夺其性）：火土——去官星之寡（火通关）／食伤泄秀，作吉；
                // 金水——只能用官（层次低）、印——木旺护官没法去寡，作中平
                val el125 = if (i == 1) zhi.element else gan.element
                // 运支为印、或运支与局支填成三合印局（未填亥卯未——「走未运亥卯未，木旺」）：整柱以印论（没法去寡）
                val huiJu125 = ctx.jiaQuGua125 && sanHeChengEl == ctx.yinEl
                val zhiMu125 = ctx.jiaQuGua125 && (zhi.element == ctx.yinEl || huiJu125)
                val mu125 = zhiMu125 || (ctx.jiaQuGua125 && el125 == ctx.yinEl)
                val huotu125 = ctx.jiaQuGua125 && !zhiMu125 && (el125 == ctx.biEl || el125 == ctx.shiEl)
                val jinshui125 = ctx.jiaQuGua125 && !zhiMu125 && (el125 == ctx.guanEl || el125 == ctx.caiEl)
                // 案一百二十六·流年印露＝当一年兵——仅流年干、且在参军之龄（「当兵一般都是十八去当兵，二十多岁想当兵人家也不要你——
                // 得切合实际」；虚岁 17–23 为落地映射）
                val yinLu126 = ctx.anYin126 && lnAge in 17..23 && i == 0 && gan.element == ctx.yinEl
                // 案一百二十七·子水大运——官杀克比劫：肯定要犯法（仅大运支；「命主要是走子水大运，肯定要犯法的」）
                val ziFanFa127 = ctx.yingKang127 && lnAge < 0 && i == 1 && zhi == EarthlyBranch.ZI
                // 案九十四·运支贴冲用神印之根（申冲寅）：支神只以冲为重——拔根之运
                val chongYinGenNote = i == 1 && ctx.chongYinGen != null && zhi == LIU_CHONG[ctx.chongYinGen]
                // 案一百一十·坐两刃泄秀局：运支冲用神之本气根（酉冲卯）——支神只以冲为重：怕
                val chongYongGen110 = i == 1 && ctx.liangRen110 &&
                    chartBranches.any { it.element == ctx.shiEl && LIU_CHONG[it] == zhi }
                // 案一百一十三·暗冲去病（暗冲暗会尤为喜，彼冲我兮皆冲起）：食伤局中神为病支——
                // 运支暗冲病支＝去病之药力量最大（发福最大）；克病支次之；穿／泄病支力最弱（药跟药有区别）
                val bingZhi113 = if (i == 1) ctx.bingZhi113 else null
                val anChong113 = bingZhi113 != null && LIU_CHONG[bingZhi113] == zhi
                val keBing113 = bingZhi113 != null && !anChong113 && zhi.element.controls == bingZhi113.element
                val chuanBing113 = bingZhi113 != null && !anChong113 && !keBing113 && LIU_HAI[zhi] == bingZhi113
                // 案一百一十四·暗冲去病＋暗会财局（暗冲暗会尤为喜）：病支＝坐下印（克食伤）——
                // 运支暗冲病支（酉冲卯）更发；运支合病支化比劫（戌合卯化火）——拱火旺，不行
                val bingZhi114 = if (i == 1) ctx.bingZhi114 else null
                val anChong114 = bingZhi114 != null && LIU_CHONG[bingZhi114] == zhi
                val gongBi114 = bingZhi114 != null && !anChong114 && LIU_HE[zhi] == bingZhi114 &&
                    liuHeHuaEl(zhi, bingZhi114) == ctx.biEl
                // 案一百一十五·暗冲暗会尤为喜（身旺印旺用财坏印）：印本气支为病支——运支冲之去病（发大财）；
                // 兼与局支六合者「暗会」；冲少者亦能发财、不如冲多者
                val chongBing115 = if (i == 1) ctx.bingZhis115.filter { LIU_CHONG[zhi] == it } else emptyList()
                val anChong115 = chongBing115.isNotEmpty()
                val anHuiZhi115 = chartBranches.firstOrNull { LIU_HE[it] == zhi }
                val anHui115 = anChong115 && anHuiZhi115 != null
                // 案一百一十六·旺者冲衰衰者拔：官杀之支来（子——露干之官得禄）克被盖之禄、冲自坐之刃——命根被拔＝牢狱
                val gen116 = ctx.gen116
                val baGen116 = i == 1 && gen116 != null && zhi == LIU_CHONG[gen116.ren] && zhi.element == ctx.guanEl
                // 案一百一十六·印比运：来木运就是好运（地支有印之生支则冲刃不拔）；刃禄帮身——承载之运（走木火运肯定是大财）
                val yinYun116 = gen116 != null && el == ctx.yinEl
                val biYun116 = gen116 != null && el == ctx.biEl
                // 案一百一十六·运支会成财局而泄／穿刃（丑：巳酉丑化金＋丑泄午火——湿土晦火）
                val caiJu116 = SAN_HE_XU.firstOrNull { it.second == ctx.caiEl }?.first
                val huiCaiHuiHuo116 = i == 1 && gen116 != null && caiJu116 != null && zhi in caiJu116 &&
                    caiJu116.count { it in chartBranches } == 2 && zhi.element == gen116.ren.element.generates
                // 案九十五·戌丁两运发财（用神得库根／用神透干）＋喜神填拱变旺好运
                val yongKuYun = i == 1 && ctx.jia95 && ctx.yongKu == zhi
                val yongTou95 = i == 0 && ctx.jia95 && ganEl == ctx.yongEl
                val xiTianGong95 = i == 1 && ctx.jia95 && ctx.xiShenEl != null &&
                    listOf(
                        setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI) to Element.WOOD,
                        setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU) to Element.FIRE,
                        setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN) to Element.WATER,
                        setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU) to Element.METAL,
                    ).any { (trio, trioEl) -> trioEl == ctx.xiShenEl && zhi in trio &&
                        trio.all { it == zhi || it in chartBranches } && !trio.all { it in chartBranches } }
                val note = when {
            // 案一百二十五·假去寡——运注（火土去寡作吉／金水只能用官、印旺没法去寡——中平）——本档独占，先于诸通则
            ctx.jiaQuGua125 -> yunNote125(gan, zhi, i, ctx, chartBranches, huotu125, mu125, jinshui125, zhiMu125, huiJu125).takeIf { it.isNotEmpty() }
            // 案一百二十六·流年印露——当一年兵
            yinLu126 -> yunNote126(gan, lnAge, ctx)
            ziFanFa127 -> yunNote127(zhi, ctx)
                    heChong -> "运支${zhi.chinese}合去冲神${ctx.chongShen!!.chinese}——用神绝处逢生，彻底有情（把印绶救起来了）"
                    // 案一百一十四·暗冲暗会尤为喜：本来不去病也发，再加上去病就更发
                    anChong114 -> "运支${zhi.chinese}暗冲病支${ctx.bingZhi114!!.chinese}（${zhi.chinese}${ctx.bingZhi114.chinese}冲——去${ctx.yinEl.chinese}克${ctx.shiEl.chinese}之病）" +
                        (if (tianGong) "＋暗会${ctx.caiEl.chinese}局（填拱——财局兑现）" else "") +
                        "（**暗冲暗会尤为喜**）：**本来不去病也发，现在再加上去病就更发**（案一百一十四）"
                    gongBi114 -> "运支${zhi.chinese}合日支${ctx.bingZhi114!!.chinese}化${ctx.biEl.chinese}——**拱${ctx.biEl.chinese}旺**（比劫得局夺${ctx.caiEl.chinese}财——财为妻）：**不行，离婚**（案一百一十四）"
                    // 案一百一十五·暗冲暗会尤为喜：把病去了发大财——暗冲暗会定发财大小
                    anChong115 -> "运支${zhi.chinese}暗冲病支${chongBing115.joinToString("") { it.chinese }}（${zhi.chinese}${chongBing115.joinToString("") { it.chinese }}冲——去${ctx.yinEl.chinese}旺之病）" +
                        (anHuiZhi115?.let { "＋暗会${it.chinese}（${it.chinese}${zhi.chinese}合）" } ?: "") +
                        "（**暗冲暗会尤为喜，彼冲我兮皆冲起**）——**把病去了发大财**" +
                        (if (chongBing115.size < 2 && !anHui115) "（冲少力轻——也能发财，不如冲多兼暗会之运）" else "") +
                        "（案一百一十五）"
                    // 案一百一十六·旺者冲衰衰者拔：官杀拔根——蹲大牢；会财局而湿土晦刃——不行；印运——好运
                    baGen116 -> "运支${zhi.chinese}——${gen116!!.guan.chinese}${ctx.guanEl.chinese}得根（${zhi.chinese}为${gen116.guan.chinese}之根）把${gen116.lu.chinese}（禄）克下去、${zhi.chinese}${gen116.ren.chinese}冲（**旺者冲衰衰者拔，衰神冲旺旺神发**）：**${gen116.ren.chinese}被拔了没根就完了——承载不了**（这局全靠${gen116.ren.chinese}帮身——${gen116.lu.chinese}上面有个${gen116.guan.chinese}病）；**${ctx.guanEl.chinese}杀来把我的根拔了——那是跟官方扯上关系的：蹲大牢**（要是能出得来，走${ctx.yinEl.chinese}${ctx.biEl.chinese}运肯定是大财——坏运过去转而就是好运；案一百一十六）"
                    huiCaiHuiHuo116 -> "运支${zhi.chinese}——${caiJu116!!.filter { it != zhi }.joinToString("") { it.chinese }}${zhi.chinese}化${ctx.caiEl.chinese}（${ctx.caiEl.chinese}局），而且${zhi.chinese}泄／穿${gen116!!.ren.chinese}——**反正都是湿土晦火：命主肯定也不行**（就指着${gen116.ren.chinese}刃自旺才能承载这么旺的财；案一百一十六）"
                    // 案一百一十六·甲子运干支之辨：干头生我被旺财所克——先不谈，关系大在刃被拔（干支异途）
                    i == 0 && yinYun116 && ctx.caiEl.controls == ctx.yinEl &&
                        zhi == LIU_CHONG[gen116!!.ren] && zhi.element == ctx.guanEl ->
                        "运干${gan.chinese}生我是没错，然${ctx.caiEl.chinese}旺克${ctx.yinEl.chinese}——先不谈，**关系大在${gen116!!.ren.chinese}被拔**（干支异途——以支论；案一百一十六）"
                    yinYun116 -> "来${ctx.yinEl.chinese}运就是好运——有${ctx.yinEl.chinese}生身则根不被拔（地支有${ctx.yinEl.chinese}，来${LIU_CHONG[gen116!!.ren]!!.chinese}都没事；案一百一十六）"
                    // 案一百一十六·刃禄帮身：财旺须身承载——走木火运肯定是大财（旺中带弱：以帮身论）
                    biYun116 -> "${gen116!!.ren.chinese}刃${gen116.lu.chinese}禄帮身——承载之运（财旺须身承载；走${ctx.yinEl.chinese}${ctx.biEl.chinese}运肯定是大财——案一百一十六）"
                    // 案一百一十七·干多不如支重：用神无根、承载不了——帮用之运亦仅温饱
                    ctx.wenBao117 && stance == JinBiaoStance.BANG ->
                        "帮用——然${ctx.yongEl.chinese}地支不载、用神无根，原局承载不了：**好运亦仅温饱**（纵此运生子——运过子没、或离婚子跟对方；案一百一十七）"
                    // 案一百一十三·暗冲去病：把病暗冲了，药的力量最大，发福也是最大
                    anChong113 -> "运支${zhi.chinese}暗冲病支${ctx.bingZhi113!!.chinese}（**暗冲暗会尤为喜，彼冲我兮皆冲起**——暗冲就是在地支暗处冲）：**支神只以冲为重——把病暗冲了，药的力量最大，发福也是最大**（案一百一十三）"
                    keBing113 -> "运支${zhi.chinese}克病支${ctx.bingZhi113!!.chinese}——补我日主之气：有效，然不如暗冲（同样是吃药，药跟药有区别——案一百一十三）"
                    chuanBing113 -> "运支${zhi.chinese}穿病支${ctx.bingZhi113!!.chinese}（兼泄之）——同样是吃药，药跟药有区别：去病之力最弱，不如冲、克（案一百一十三）"
                    // 案一百一十三·食伤局泄过为病：干支再来食伤（丙丁／午巳之属）——助病（泄日主更过）
                    ctx.bingZhi113 != null && el == ctx.shiEl ->
                        "${if (i == 0) "运干" else "运支"}${chinese}助${ctx.shiEl.chinese}局之病——泄日主更过（差）"
                    // 案一百零一·食神制杀身旺局：财星到＝食神生财——发财（杀已被制，财来即富）
                    ctx.zhiShaCai && el == ctx.caiEl ->
                        "食神生财——**发财之运／应财之年**（杀已被制，财来即富——案一百零一：丙子运庚子年抖音卖体育用品发财千万）"
                    // 案一百零八·秀气流行局：运支与局支成食伤三合（酉丑化金）——走金运才发，层次巅峰
                    i == 1 && ctx.xiuQi108 && zhi.element == ctx.shiEl &&
                        zhi in listOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU) &&
                        chartBranches.any { it in listOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU) && it != zhi } ->
                        "运支${zhi.chinese}与局支会${ctx.shiEl.chinese}局（酉丑化金之属）——**走${ctx.shiEl.chinese}运才发，层次达到巅峰**（案一百零八）"
                    // 案一百零八·秀气流行最怕火：原局没火是好事——来了坏秀气（不作大凶档，恐掩支上金运之吉）
                    ctx.xiuQi108 && el == ctx.yinEl ->
                        "秀气流行最怕${ctx.yinEl.chinese}——原局没${ctx.yinEl.chinese}是好事，来了坏秀气（案一百零八）"
                    // 案九十八·昆仑透克神之病局：运干食伤到＝去病（大运来了食神才能去寡）——
                    // 然支火土助病：来了木也不管事，稍微有点好事、坏事多（总体过得非常差）
                    i == 0 && ctx.jia98 && ganEl == ctx.yongEl.generates ->
                        "运干${gan.chinese}食伤到——克去透干之${Element.entries.first { it.controls == ctx.yongEl }.chinese}病（**大运来了食神才能去寡**）：稍微有点好事；然地支火土把病帮起来——**来了木也不管事，还是坏事多（总体过得非常差——案九十八）**"
                    // 案九十六·同性之克去官最力（丙阳火克庚阳金）——伤官伤尽始为奇
                    i == 0 && ctx.quGuaGan != null && ganEl.controls == ctx.quGuaGan!!.element && gan.yang == ctx.quGuaGan!!.yang ->
                        "运干${gan.chinese}同性克${ctx.quGuaGan!!.chinese}力量最大——**把官杀去尽**（伤官伤尽始为奇）：**丙运要比丁运好**（案九十六）"
                    quRenBing -> "运支${zhi.chinese}冲去${ctx.biEl.chinese}刃之病——大运来药去掉这个病：学习好、财运好（就在好运上）"
                    haiSiQuHuo -> "运支亥冲局中巳（**亥巳逢冲——去火存金**）：官被冲去、${ctx.biEl.chinese}比劫复旺来克${ctx.caiEl.chinese}财——**老破财**（各种各样的事儿来捣乱：今儿挣一百，明儿破八十——虽有亥卯化木亦不作吉论）"
                    yinHaiFanShen -> "运支寅合局中亥（**寅亥化木**——合去坏印之${ctx.caiEl.chinese}财根）＋木生火帮${ctx.yinEl.chinese}印——**翻身**（三十年河东河西：坎离震兑四方各管三十年——北方差三十年，东方翻身农民把歌唱；走木生火运就要翻身了——福报在后面）"
                    // 案一百一十一·用官局填拱：财局兑现生官——帮官之运（不作案八十四「财气通门户」论——彼为枭夺食找财之局）
                    tianGong -> if (ctx.yongEl == ctx.guanEl)
                        "运支${zhi.chinese}填${ctx.caiEl.chinese}拱局（财局兑现）——**财旺生官：帮官之运**（用神是官不是用财——不作案八十四财气通门户论；案一百一十一）"
                    else
                        "运支${zhi.chinese}填${ctx.caiEl.chinese}拱局（玄机暗里存——财局兑现）：**财气通门户应期——发大财＋一下就有子**（儿子也有出息——往后好运越来越好）"
                    chongYinGenNote -> "运支${zhi.chinese}贴冲${ctx.chongYinGen!!.chinese}——**用神${ctx.yinEl.chinese}之根被拔（支神只以冲为重，刑与穿兮动不动）**：此人就不行了——纵运干${gan.chinese}为好${ctx.yinEl.chinese}运能帮得起来，只不过成就不如——**只能是在老百姓里面算好的**（案九十四）"
                    chongYongGen110 -> "运支${zhi.chinese}冲${ctx.shiEl.chinese}——**冲用神${ctx.shiEl.chinese}之本气根：怕走${zhi.chinese}金大运**（支神只以冲为重）：七八十岁走人就死了；年轻走是差运——这么好的八字只能是不得志（在当官的单位上班，升职不了）；若局中有${ctx.caiEl.chinese}闲神坐支，则卫护${ctx.shiEl.chinese}克${zhi.chinese}——闲神立功，一样是好运（案一百一十）"
                    yongKuYun -> "运支${zhi.chinese}为${ctx.yongEl.chinese}用神之库——**用神得库根：发财之运**（案九十五：**戌丁两运还是发了财的**——不一无是处）"
                    yongTou95 -> "运干${gan.chinese}即${ctx.yongEl.chinese}用神透干得力——**发财之运**（案九十五：丁运发财）"
                    xiTianGong95 -> "运支${zhi.chinese}填${ctx.xiShenEl!!.chinese}喜神拱局（亥卯待未之属）——**喜神变旺：还是好运**；金木交战也无碍——**${ctx.xiShenEl.chinese}不是用神**（假如日主完全指望着${ctx.xiShenEl.chinese}、地支都是${ctx.shiEl.chinese}，来${ctx.xiShenEl.chinese}的时候被反克了那就不行——这个命不是这种情况）（案九十五）"
                    tianShiJu -> "运支${zhi.chinese}临${ctx.shiEl.chinese}食伤三合局（${ctx.shiEl.chinese}局填成／叠强——通关化劫之局兑现）：**才挣到钱（小康）**——此前比劫太旺夺财挣不着（食伤化比劫化得没那么好）；克${ctx.shiEl.chinese}之源的运（子水之属）穷困潦倒——财富全凭食伤一字定之（案八十七）"
                    // 案一百二十一·平衡之局走什么都不怕（闲神立功）／反事实·无印护卫——伤官克官犯官灾
                    xianShen121 -> yunNote121(el, ctx, gan, zhi)
                    // 案一百二十四·有力就能食神制杀——运注（药神去病／比劫帮身／戌大灾／丑化杀／印干坐药神）
                    ctx.shiZhi124 -> yunNote124(gan, zhi, el, ctx, chartBranches, shuiYao124, biBang124, xuZai124, chouHua124, yinZuoYao124, i).takeIf { it.isNotEmpty() }
                    // 案一百二十三·用官喜帮火——运注（土干克去克火之水者，仍按去忌神论——不注泄火）
                    ctx.yongGuan123 && !quJiShen -> yunNote123(huoTongGen123, caiYun123, shuiYun123, tuYun123, gan, zhi, ctx, chartBranches).takeIf { it.isNotEmpty() }
                    // 案一百二十二·来水找源头——凑合（化水局挣钱多）／金运——彻底完了
                    shuiYun122 -> yunNote122(gan, zhi, ctx, chartBranches, false)
                    jinYun122 -> yunNote122(gan, zhi, ctx, chartBranches, true)
                    fanGuanZai121 -> "运${if (i == 0) "干" else "支"}${chinese}${ctx.shiEl.chinese}伤官克${ctx.guanEl.chinese}官——**无${ctx.yinEl.chinese}印护卫：犯官灾**（肯定打官司有事儿或者犯错误；案一百二十一反事实）"
                    fanWang -> "官干直克身——犯旺激怒（权在一人，不可触犯），大凶"
                    zhiDuo -> "地支食伤被印枭夺——印多、地支成局（师：「印多，印得局了」），走泄他的也是不行的：来了${ctx.shiEl.chinese}就是枭神夺食——**${ctx.shiEl.chinese}来必出灾（走${ctx.shiEl.chinese}运一定会出灾）**（反事实：印减至两支、地支不成局则没事——案一百一十八）"
                    yiTiDuo118 -> "干支一体（${gan.chinese}${zhi.chinese}皆${ctx.shiEl.chinese}）——地支被${ctx.yinEl.chinese}印枭夺，干亦随之：来了${ctx.shiEl.chinese}就是枭神夺食——**${ctx.shiEl.chinese}来必出灾（走${ctx.shiEl.chinese}运一定会出灾）**（印多、地支成局——「天干无${ctx.yinEl.chinese}不夺」让位于枭神夺食；案一百一十八）"
                    shunXieGan -> "天干泄秀（天干无${ctx.yinEl.chinese}不夺）——顺其旺势，可行"
                    yinJuBu -> "运支合补印局（${zhi.chinese}与局支合成${ctx.yinEl.chinese}局）——亥卯未拱局补成，用神得局大发"
                    biJianGen -> "运支${zhi.chinese}为透干比劫${ctx.biTouGan.joinToString("") { it.chinese }}之根（禄/长生）——比劫见根变强，复与局支拱${ctx.biEl.chinese}局——通关之神被焚（${ctx.biEl.chinese}旺则${ctx.shiEl.chinese}燥，燥${ctx.shiEl.chinese}不生${ctx.caiEl.chinese}）：比劫直接克掉用神${ctx.caiEl.chinese}——大凶（死亡级大祸）；若局有官星则官克比劫护财，不至于此（配置论）"
                    sanHeCheng -> "运支${zhi.chinese}填实${sanHeChengEl!!.chinese}三合局（原局拱合待成——局中两支早等着${zhi.chinese}来，${sanHeChengEl.chinese}旺了）——局神得势克掉用神，就要出事（伤官见官为祸百端）" +
                        (if (sanHeChengEl == ctx.caiEl && ctx.yongEl == ctx.yinEl) "；所填乃坏印之财局——**财局兑现＝财坏印成真（财为妻而毁用神——克妻之应之属：离婚或丧妻）**" else "")
                    quJiShen && ctx.yongCai -> "运干${gan.chinese}官杀克透干比劫${ctx.jiShenTouGans.joinToString("") { j -> j.chinese }}——**官护财**（用财怕比劫夺财，首要找官：有官就没事，没官就不行——干护支凶仍以支论）"
                    quJiShen -> "运干${gan.chinese}克去透干忌神${ctx.jiShenTouGans.joinToString("") { j -> j.chinese }}——去忌神即去病，用神无克（去忌之运最好）"
                    // 案一百一十九·印旺用财之运注（火土运小康／水木运差／单天干木运一般／地支木更差／湿土不止水）
                    yun119 -> yunNote119(i, gan, zhi, el, ctx, suiShui119, zhiEl119)
                    muYun103 -> "运${if (i == 0) "干" else "支"}${chinese}${ctx.biEl.chinese}生${ctx.shiEl.chinese}——**地支来${ctx.biEl.chinese}也没事（生${ctx.shiEl.chinese}了：${ctx.shiEl.chinese}护卫${ctx.caiEl.chinese}财）**；然${ctx.biEl.chinese}运不如${ctx.shiEl.chinese}${ctx.caiEl.chinese}之运（案一百零三——对勘案一百一十九：地支没有火则来木不行）"
                    // 案一百二十·伤官用劫而克泄交加——运注（比劫仅温饱／印克食伤方平衡／官杀差运／食伤助泄）
                    ctx.keXie120 -> yunNote120(gan, zhi, el, ctx, biYun120, guanYun120, guanZhu120).takeIf { it.isNotEmpty() }
                    huaSha != null -> huaSha.second
                    else -> ctx.daXiongText[el] ?: ctx.zhongPingText[el]
                        ?: (if (!(ctx.wangJiFamily && ctx.yinDeJuDuo && el == ctx.shiEl)) ctx.favorText[el] else null)
                }
                JinBiaoStanceItem(
                    label = if (i == 0) "运干" else "运支",
                    chinese = chinese,
                    // 案一百一十四·运支合日支化比劫（戌合卯化火）——以化神论（克用神）
                    element = if (gongBi114) ctx.biEl else el,
                    stance = if (gongBi114) JinBiaoStance.SUN else stance,
                    // 案一百二十三·丙戌——火通根作火论（不注「随连行方局以金论」）；案一百二十四·戌——印克药神拱火（同）
                    action = (if (gongBi114) "克用神" else act) + (note?.let { "·$it" } ?: "") + (if (i == 1 && !huoTongGen123 && !xuZai124 && !chouHua124 && !huotu125 && !zhiMu125) bureauNote else ""),
                    favorOverride = huotu125 || (!ctx.jiaQuGua125 && (huaSha?.first == true || shunXieGan || yinJuBu || heChong || quRenBing || yinHaiFanShen || tianGong || tianShiJu || yongKuYun || xiTianGong95 || anChong113 || anChong114 || anChong115 || yinYun116 || biYun116 || (ctx.zhiShaCai && el == ctx.caiEl) ||
                        // 案一百一十九·单天干木运而支属火土——以火土运论（作吉：小康）；案一百二十三·丙戌火通根——肯定旺（作吉）；
                        // 案一百二十四·药神（水）去病、比劫帮身、印干坐药神支、丑土化杀——作吉
                        (ganMu119 && zhiBang119) || huoTongGen123 || shuiYao124 || biBang124 || chouHua124 || yinZuoYao124)),
                    // 案一百一十三·穿泄病支（丑穿午）：有效而药力最弱——归中平（映射：师只说「有效果吗？同样是吃药，药跟药有区别」）
                    // 案一百一十九·单天干木运——「不能算好运，但也不算差，就是一般的运」（中平）；
                    // 案一百零三对勘：印旺用财而有午火根——地支来木也没事（中平）
                    pingOverride = (mu125 || jinshui125) || (!ctx.jiaQuGua125 && ((huaSha != null && huaSha.first == false) || chuanBing113 || (ganMu119 && !zhiBang119) || muYun103 ||
                        // 案一百二十·伤官用劫而克泄交加——比劫帮用亦仅温饱（中平）
                        biYun120 || xianShen121 || shuiYun122 || caiYun123)),
                    // 案一百二十一·平衡之局（闲神立功）——不作凶论；反事实·无印护卫——伤官克官犯官灾；案一百二十二·金运彻底完了；案一百二十三·走水就完了
                    // 案一百二十五·印旺护官之运（未填亥卯未之属）——没法去寡而已，不作「填局出事」凶论
                    xiongOverride = ziFanFa127 || (!(ctx.jiaQuGua125 && mu125) && (fanGuanZai121 || jinYun122 || shuiYun123 || xuZai124 || (!xianShen121 && !shuiYun122 &&
                        (fanWang || zhiDuo || yiTiDuo118 || guanYun120 || sanHeCheng || biJianGen || haiSiQuHuo || chongYongGen110 || gongBi114 ||
                        baGen116 || huiCaiHuiHuo116)))),
                )
            }
        }
    }

    /**
     * 强极得局之官杀（干支同论）——化煞生印：支合入印局→作吉大发；干/未合之支→作中平不为大凶。
     * 返回 (作吉?, 注记)；非此档返回 null。
     */
    private fun huaShaNote(
        branch: EarthlyBranch,
        el: Element,
        chartBranches: List<EarthlyBranch>,
        ctx: YunCtx,
        isStem: Boolean = false,
    ): Pair<Boolean, String>? {
        if (!ctx.qiangJi || !ctx.yinDeJu || el != ctx.guanEl) return null
        return if (!isStem && heYinJu(branch, chartBranches, ctx.yinEl)) {
            true to "印星得局，化煞生印——${branch.chinese}合入印局，大发（寒门贵客）"
        } else {
            false to "印星得局，化煞生印——官来生印，不为大凶"
        }
    }

    /** 运支填实原局拱合待成之局（局中已有两支，如亥未拱木待卯——卯来即成亥卯未）：返回所成局之五行。
     *  案五十七戒：原局三支已全者（巳酉丑本全），运支重复其一支非「填实」——不作成局论。 */
    private fun sanHeCompletedEl(branch: EarthlyBranch, chartBranches: List<EarthlyBranch>): Element? {
        val sanHe = listOf(
            setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU) to Element.FIRE,
            setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN) to Element.WATER,
            setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI) to Element.WOOD,
            setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU) to Element.METAL,
        )
        return sanHe.firstOrNull { (t, _) ->
            branch in t && t.all { it == branch || it in chartBranches } && !t.all { it in chartBranches }
        }?.second
    }

    /** 运支与局中支成三合印局（寅午戌火/申子辰水/亥卯未木/巳酉丑金）或六合化印。 */
    private fun heYinJu(branch: EarthlyBranch, chartBranches: List<EarthlyBranch>, yinEl: Element): Boolean {
        val sanHe = listOf(
            setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU) to Element.FIRE,
            setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN) to Element.WATER,
            setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI) to Element.WOOD,
            setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU) to Element.METAL,
        )
        sanHe.forEach { (trio, el) ->
            if (branch in trio && trio.all { it == branch || it in chartBranches } && el == yinEl) return true
        }
        val partner = LIU_HE[branch]
        if (partner != null && partner in chartBranches) {
            val key = listOf(branch.ordinal, partner.ordinal).sorted().joinToString(",")
            val canonical = mapOf(
                "0,1" to "子丑", "2,11" to "寅亥", "3,10" to "卯戌", "4,9" to "辰酉",
                "5,8" to "巳申", "6,7" to "午未",
            )[key]
            if (canonical != null && LIU_HE_EL[canonical] == yinEl) return true
        }
        return false
    }

    /** 取用候选（analyze 与学历判读共用）。 */
    private data class Choice(val way: JinBiaoQuYongWay, val el: Element, val basis: String)

    /** 学历判读（案九十四~一百·学历章：印为用神之局适用；案一百扩杀印局、财坏印型、弃印就财注；案一百一十二扩用官局喜用两伤「上学笨」档）。 */
    private fun xueLiOf(
        bazi: BaziResult,
        daYun: DaYunInfo?,
        choice: Choice,
        strength: DayStrength,
        ruoJi: Boolean,
        wangJi: Boolean,
        qiangJi: Boolean,
        yongEl: Element,
        yinEl: Element,
        caiEl: Element,
        shiEl: Element,
        guanEl: Element,
        yinS: Double,
        liangShang112: LiangShang112? = null,
    ): cn.xulun.bazi.model.JinBiaoXueLi? {
        // 案一百一十二·用官局喜用两伤：劫财见财的人上学笨，笨不笨取决于官星——此局官也不行
        // （印未坏——上学笨非印之故，故 yinWanHao 仍记 true）
        if (!(ruoJi || wangJi || qiangJi) && liangShang112 != null) {
            return cn.xulun.bazi.model.JinBiaoXueLi(
                yinWanHao = true,
                level = cn.xulun.bazi.model.JinBiaoXueLiLevel.SHANG_XUE_BEN,
                basis = "**用财而劫财见财的人，念书的时候一定笨**——笨不笨又取决于官星：**官是什么？官代表主见、思路**——劫财见财又有官，说明这人学习的时候是有主见的（为什么当官的人就听自己的：自己有思路；没思路就只能被别人劫）；如果没有这个官，什么都得问，最后什么都还不明白。**此局喜用两伤——官也不行：此人上学不行（学习肯定不行）**（案一百一十二）",
            )
        }
        return if (!(ruoJi || wangJi || qiangJi) && yongEl == yinEl &&
            (choice.way == JinBiaoQuYongWay.FU_SHEN || choice.way == JinBiaoQuYongWay.HUA_SHA) && daYun != null
        ) {
            val yinIdxsX = bazi.stems.indices.filter { it != 2 && bazi.stems[it].element == yinEl }
            val caiIdxsX = bazi.stems.indices.filter { it != 2 && bazi.stems[it].element == caiEl }
            val yinTieKeX = yinIdxsX.isNotEmpty() && caiIdxsX.isNotEmpty() &&
                yinIdxsX.any { yi -> caiIdxsX.any { ci -> kotlin.math.abs(ci - yi) == 1 } }
            val yinGenChongX = bazi.branches
                .filter { br -> branchHiddenStems.getValue(br).any { it.element == yinEl } }
                .any { br -> LIU_CHONG[br] in bazi.branches }
            val wanHao = !yinTieKeX && !yinGenChongX
            val qiuXue = daYun.yuns.take(2).map { y ->
                val ganEl = y.pillar.stem.element
                val zhiEl = y.pillar.branch.element
                val gan = when {
                    ganEl == yinEl || ganEl.generates == yinEl -> 2
                    ganEl.controls == yinEl -> -2
                    // 火最怕湿土来晦（案五十三/九十四）——湿土干（己）晦火印
                    yinEl == Element.FIRE && y.pillar.stem == HeavenlyStem.JI -> -2
                    else -> 0
                }
                val zhi = when {
                    zhiEl == yinEl || zhiEl.generates == yinEl -> 1
                    zhiEl.controls == yinEl -> -1
                    // 湿土支（辰丑）晦火
                    yinEl == Element.FIRE && (y.pillar.branch == EarthlyBranch.CHOU || y.pillar.branch == EarthlyBranch.CHEN) -> -1
                    else -> 0
                }
                Triple(y.pillar.fullName, gan, zhi)
            }
            val ganBang = qiuXue.any { it.second > 0 }
            val ganHuai = qiuXue.any { it.second < 0 }
            val zhiHuai = qiuXue.any { it.third < 0 }
            val total = qiuXue.sumOf { it.second + it.third }
            // 案一百零七·印透干而支根被冲、初运天干未走克印之运：学习还不错——
            // 印透只说上学时头脑好；这命没力量考不上清华，只能说考个普通的大学。
            // （对勘案一百：初运干己戊克印——仍走印坏档）
            val puTong107 = !wanHao && yinIdxsX.isNotEmpty() && yinGenChongX && qiuXue.all { it.second >= 0 }
            val level = when {
                puTong107 -> cn.xulun.bazi.model.JinBiaoXueLiLevel.YOU
                !wanHao -> cn.xulun.bazi.model.JinBiaoXueLiLevel.YIN_HUAI
                ganBang && !ganHuai && total >= 3 -> cn.xulun.bazi.model.JinBiaoXueLiLevel.GAO
                ganBang -> cn.xulun.bazi.model.JinBiaoXueLiLevel.YOU
                else -> cn.xulun.bazi.model.JinBiaoXueLiLevel.BU_GAO
            }
            cn.xulun.bazi.model.JinBiaoXueLi(
                yinWanHao = wanHao,
                level = level,
                basis = buildString {
                    if (puTong107) {
                        append("**印透干、初运（${qiuXue.joinToString("、") { it.first }}）天干没有走克${yinEl.chinese}之运——学习还不错**（案一百零七）：印透出来只能说上学的时候头脑好、先天对学习还算灵光——**这命没力量考不上清华，只能说考个普通的大学**。**学习好不代表以后行**（清华毕业也有个别在社会不行）：**事业层次取决于用神${yinEl.chinese}之力**——用神在地支被坏得太严重（而且是相冲），就注定没有太大的成就。三档：大名牌毕业（原局用神知识水平过硬）——就算没有经济头脑、走差运也不差，瘦死的骆驼比马大；学习还不错但不过硬、又没经济头脑、完全指着印的、地支印坏了——不行；上学时学习没脑子而弃印就财者——经济头脑强能发财")
                    } else if (!wanHao) {
                        val caiHuaiYin100 = yinGenChongX &&
                            bazi.branches.filter { br -> branchHiddenStems.getValue(br).any { it.element == yinEl } }
                                .any { br -> LIU_CHONG[br]?.let { c -> bazi.branches.contains(c) && c.element == caiEl } == true }
                        if (caiHuaiYin100) {
                            append("**${caiEl.chinese}坏印（印根被${caiEl.chinese}支贴冲——原局就没有学习的灵感：一个印一个食伤都代表上学时候的学习灵感）——来了好运学习也不会太好：学习取决于原局**（区别于虚透未伤型〔案九十五〕——虚透上学取决于大运）。**印和食伤不能伤**——学习成绩很大一部分由先天悟性决定（努力一样的两个孩子成绩不一样；有人走好运很努力还是学不好——案一百）")
                        } else {
                            append("**印在原局就坏了（${if (yinTieKeX) "${caiEl.chinese}财贴克${yinEl.chinese}印" else "印根被贴冲"}）——说明他头脑上、思路上是存在问题的：即使走了好运，他学习也不好、考不上好学校、学历不高**。要区分：有的人初运走好运就学习好上好大学；有的人初运走好运他学习也不好——原因就在印坏不坏。**印、食神这两个对于学业非常重要——因为学习是看脑子和思路**（案九十五）")
                        }
                    } else {
                        append("**印星原局没被破坏——小时候脑子没毛病、头脑肯定不笨**（底子在）；学历高低看求学运（${qiuXue.joinToString("、") { it.first }}）：")
                        append(
                            when (level) {
                                cn.xulun.bazi.model.JinBiaoXueLiLevel.GAO -> "求学运一路生印同印（好运）——**百分之百学历高**"
                                cn.xulun.bazi.model.JinBiaoXueLiLevel.YOU -> "运干生${yinEl.chinese}同${yinEl.chinese}而地支不载（甲申乙酉之属）——**地支不载不碍：只要天干没把印坏了，就可以有学历、可以上大学**"
                                cn.xulun.bazi.model.JinBiaoXueLiLevel.BU_GAO ->
                                    (if (zhiHuai && yinEl == Element.FIRE) "走北方晦${yinEl.chinese}之运（${qiuXue.filter { it.third < 0 || it.second < 0 }.joinToString("、") { it.first }}——**${yinEl.chinese}最怕湿土来晦**：来己土给他晦火了那就不行了）——**学历就不高**"
                                    else "求学运干不帮${yinEl.chinese}印（${qiuXue.joinToString("、") { it.first }}）——学历不高") +
                                        "；然原局印没坏——**学习肯定是不错或者好学**（走坏运＝平时学习好、到考试的时候发挥失常）"
                                else -> ""
                            }
                        )
                        // 案九十五·印之源头（生印之喜神）三态：源头被贴冲拔——虚印完全指着大运；
                        // 源头旺而根无冲——即使走坏运也能考上大学
                        val xiEl95 = Element.entries.first { it.generates == yinEl }
                        val xiGenZhis95 = bazi.branches.filter { br -> br.element == xiEl95 || branchHiddenStems.getValue(br).any { it.element == xiEl95 } }
                        val xiGenBa = xiGenZhis95.isNotEmpty() && xiGenZhis95.any { br -> LIU_CHONG[br] != null && LIU_CHONG[br] in bazi.branches }
                        val xiWang = partyScore(bazi, xiEl95) >= GUAN_WANG && !xiGenBa
                        when {
                            xiGenBa -> append("。**学习情况全指${yinEl.chinese}印**：源头（${xiEl95.chinese}喜神）被旺${shiEl.chinese}贴冲**直接拔了——虚${yinEl.chinese}就完全指着大运了**：运不帮（晦${yinEl.chinese}）就一样没有学历；**若${xiEl95.chinese}在地支很旺、没被这么旺的${shiEl.chinese}克——${yinEl.chinese}有源头很旺，那么即使走坏运，也能考上大学**（案九十五）。这人脑子没问题、头脑比较灵光、知书达理——印星得用是个好学之人，但运不行，一样没有学历")
                            xiWang -> append("。印有源头（喜神${xiEl95.chinese}旺而根稳）——**即使走坏运，也能考上大学**（源头在，坏运只是打折——案九十五）")
                        }
                        append("。**看学历首先看印星（和食伤）**：印没坏＝脑子没问题——初运坏只是发挥失常，初运好（${yinEl.chinese}印之好运）则学历必高")
                    }
                },
            )
        } else {
            null
        }
    }

    /** 子女判读（第十一讲·子女章＋各案档）——自 analyze 抽出（方法体量上限）。 */
    private fun ziNvOf(
        bazi: BaziResult,
        daYun: DaYunInfo?,
        choice: Choice,
        strength: DayStrength,
        ruoJi: Boolean,
        wangJi: Boolean,
        dayMaster: HeavenlyStem,
        yinEl: Element,
        biEl: Element,
        shiEl: Element,
        caiEl: Element,
        guanEl: Element,
        yinS: Double,
        biS: Double,
        caiS: Double,
        guanS: Double,
        shiS: Double,
        monthBranch: EarthlyBranch,
        guoXieFanRuo: Boolean,
        kunLunEl: Element?,
        kunLunSource: Element?,
        gongCaiYinHua: Boolean,
        gongCaiTrio: Pair<Set<EarthlyBranch>, Element>?,
        yongEl: Element,
        caiZai: Boolean,
        liangShang112: LiangShang112? = null,
        ganDuoBuZai117: Boolean = false,
        anYin126: Boolean = false,
        yingKang127: Boolean = false,
    ): cn.xulun.bazi.model.JinBiaoZiNv? {
        return if (ruoJi) {
            // 案七十一（案四盘复看）·从势局子女另裁：特殊格局不按身衰印做儿/身旺财为子正常树断——
            // 只要走好运（顺势之运）就可以生儿子；此命不需要印（帮身反而不好——最忌帮身）
            cn.xulun.bazi.model.JinBiaoZiNv(
                qingKuang = cn.xulun.bazi.model.JinBiaoZiNvQingKuang.BI_YOU_ER,
                ziXing = representativeGod(bazi, dayMaster, choice.el),
                ziEl = choice.el,
                basis = "**特殊格局（弱极从势）——不按「身衰印做儿／身旺财为子」正常判定树来断**" +
                    "（辰为震位虚土：气虚而薄〔未戌为六九月实土，辰异于彼〕、寅亥化木木得局——辰自顾不暇不能帮身）：**只要走好运（顺势之运）就可以生儿子**——顺运内应期。" +
                    "此命**不需要印**：帮身反而不好——**最忌帮身**；走金运金生水水生木（不帮身即好运）——学历事业不错（唯戌运比劫帮身学习不好）；水旺则金木顺流，水不旺则金木争克而平庸",
            )
        } else if (wangJi) {
            // 案一百一十八·旺极／强极而原局食伤全无——憋着生不出来：特殊格局「只要走好运就有儿子」
            // 以**有发泄（食伤）**为前提（案七十一／九十六：泄就代表生育能力）；此命身旺全憋着了、无处发泄——
            // 走好运（土运）一样没儿子。印比之支藏食伤（跟着走作废）者不在此档——仍待师传。
            if (partyScore(bazi, shiEl) != 0.0) {
                null // 专旺之子女另裁——待师传
            } else {
                // 印得局（地支成局）——食伤运被枭夺一定出灾（走泄他的也不行）；未成局——食伤运没事，然原局无发泄不由此得子
                val duo118 = yinDeJuShi(bazi, yinEl)
                cn.xulun.bazi.model.JinBiaoZiNv(
                    qingKuang = cn.xulun.bazi.model.JinBiaoZiNvQingKuang.BI_WU_ER,
                    ziXing = representativeGod(bazi, dayMaster, shiEl),
                    ziEl = shiEl,
                    basis = "**特殊格局（旺极／强极）无${shiEl.chinese}食伤——憋着生不出来（案一百一十八）**：特殊格局本「**只要走好运就有儿子**」（案七十一／案九十六——以**有发泄**为前提：泄就代表生育能力）；此命**命主身旺，全憋着了，无处发泄——太旺了憋着生不出来**：走好运有没有儿子？走${biEl.chinese}运" +
                        (if (biEl == Element.EARTH) "（戊辰之属）" else "") + "——**没有**；走好运（" +
                        (if (biEl == Element.EARTH) "己未之属" else "印比顺运") + "）——**一样没儿子**。只能说这人**刚猛**（别人都怕我，只能说我厉害）。" +
                        "运注：印比顺运不生子（憋着）；走${shiEl.chinese}运（食伤到）" +
                        (if (duo118) "——印多、地支成局，枭神夺食**走${shiEl.chinese}运一定会出灾**，亦不由此得子" else "虽无夺食之灾——原局无发泄，亦不由此得子"),
                )
            }
        } else {
            val yinYouLi = bazi.stems.any { it != dayMaster && it.element == yinEl } || hasBenQiRoot(bazi, yinEl)
            val yinTouZuoBen = bazi.stems.anyIndexed { i, s -> i != 2 && s.element == yinEl && bazi.branches[i].element == yinEl }
            // 案七十·财贴克印（全线优先）：财干贴克印干（丙贴庚之类）——螳螂捕蝉黄雀在后：
            // 没有生儿子的功能——能不能生儿子人一出生就决定了（大运走水也生不出来）
            val caiIdxsW = bazi.stems.indices.filter { it != 2 && bazi.stems[it].element == caiEl }
            val yinIdxsW = bazi.stems.indices.filter { it != 2 && bazi.stems[it].element == yinEl }
            // 案七十七戒·隔克不算克：须**所有**印干皆被财干贴克才功能废——一印被克而另一印隔开独存＝功能在
            val caiTieYin = caiIdxsW.isNotEmpty() && yinIdxsW.isNotEmpty() &&
                yinIdxsW.all { yi -> caiIdxsW.any { ci -> kotlin.math.abs(ci - yi) == 1 } }
            data class ZN(val el: Element, val er: Boolean, val nv: Boolean, val text: String)
            // 案六十三·调候优先：寒暖特殊气象局——一切以寒暖为规范，不用分析生克：
            // 走东南暖运就是好——什么都有什么都顺——必然有儿子有孙子，没有缺陷
            val zn: ZN = if (choice.way == JinBiaoQuYongWay.TIAO_HOU) {
                ZN(yongEl, true, false,
                    "**调候优先（案六十三）**：寒暖特殊气象局**不用分析生克——一切以寒暖为规范**：调候神有根有气、走东南暖运就是好——什么都有什么都顺——**必然有儿子、有孙子——没有缺陷**（辅证：日主坐印足以食伤制杀——食伤偏弱，帮起食伤即好）")
            } else if (choice.way == JinBiaoQuYongWay.QU_GUA) {
                // 案九十六·特殊格局（去寡从旺）子女另裁：不能按正常判定树论——
                // 有食伤（泄＝生育能力）＋走好运（顺势）→不光事业好一般都有儿子；
                // 一直走克食伤之运把食神憋住→没儿子；从旺无食神不这样论。
                // 不用之财尽可劫——群劫争财不碍子。
                val keShiEl96 = Element.entries.first { it.controls == shiEl }
                val shunCnt96 = daYun?.yuns?.take(5)?.count { y ->
                    val ge = y.pillar.stem.element
                    val ze = y.pillar.branch.element
                    ge == shiEl || ge.generates == shiEl || ze == shiEl || ze.generates == shiEl
                } ?: 0
                val biShiCnt96 = daYun?.yuns?.take(5)?.count { y ->
                    y.pillar.stem.element == keShiEl96 || y.pillar.branch.element == keShiEl96
                } ?: 0
                ZN(shiEl, shunCnt96 > biShiCnt96, false,
                    "**特殊格局（去寡）子女另裁（案九十六）——不按正常判定树论**：这局不用财——**不用之财尽可劫，随便劫**（群劫争财不碍子）；用食伤泄秀——**只要泄秀泄得好就有儿子：身旺发泄发得好＝生育能力强，泄就代表生育能力**。" +
                        (if (shunCnt96 > biShiCnt96) "**特殊格局走好运不光事业好，一般还都有儿子**（得看他走的运——本案前运顺势〔帮${shiEl.chinese}〕多）" else "**然前运一直走${keShiEl96.chinese}地把食神${shiEl.chinese}憋住了——也是没有儿子的**") +
                        "。若一直走北方${keShiEl96.chinese}运把食神${shiEl.chinese}给憋住了——那么也是没有儿子的。**从旺没有食神，就不能按这样说**（旺极无食另裁——待师传）。口诀：「**不用之财尽可劫，不用之官尽可伤；可用之官不可伤，可用之财不可劫**」")
            } else if (choice.way == JinBiaoQuYongWay.KUN_LUN) {
                // 案九十七·昆仑顺局子女看运：顺（生源/本势/顺泄）有儿；逆（克局止水/克生源）
                // 没儿子——有儿子也是被克了或离婚跟对方（特殊格局方方面面都得看大运）
                val shunEls97 = setOf(kunLunSource!!, kunLunEl!!, kunLunEl!!.generates)
                val niEls97 = setOf(
                    Element.entries.first { it.controls == kunLunEl },
                    kunLunEl.controls,
                )
                val shunCnt97 = daYun?.yuns?.take(5)?.count { y ->
                    y.pillar.stem.element in shunEls97 || y.pillar.branch.element in shunEls97
                } ?: 0
                val niCnt97 = daYun?.yuns?.take(5)?.count { y ->
                    y.pillar.stem.element in niEls97 || y.pillar.branch.element in niEls97
                } ?: 0
                ZN(kunLunEl.generates, shunCnt97 > niCnt97, false,
                    "**昆仑顺局子女看运（案九十七）——特殊格局方方面面都得看大运**：顺者${kunLunSource.chinese}（生源）、${kunLunEl.chinese}（本势）、${kunLunEl.generates.chinese}（顺泄——泄就代表生育能力）；逆者${Element.entries.first { it.controls == kunLunEl }.chinese}（克${kunLunEl.chinese}止${kunLunEl.chinese}——止不住的）与${kunLunEl.controls.chinese}（克生源逆局）。" +
                        (if (shunCnt97 > niCnt97) "**前运顺多——有儿子**" else "**前运逆多——没儿子**") +
                        "；**逆着它就没有儿子——有儿子也是被克了、或者离婚儿子跟对方了**（本案前五运顺${shunCnt97}步逆${niCnt97}步）")
            } else if (anYin126) {
                // 案一百二十六·坐下暗印不伤——身衰印做儿有子（「同样都跟这个丁火有关系——事儿跟事儿不一样」）
                ZN(yinEl, true, false, ziNv126Text(bazi, yinEl))
            } else if (yingKang127) {
                // 案一百二十七·身衰印做儿——没有印，没儿子（财官旺、全指望比劫硬扛）
                ZN(biEl, false, false, ziNv127Text(bazi, yinEl, guanEl))
            } else if (strength == DayStrength.STRONG) {
                // 身旺：基准财为子——查比劫夺财（重夺/地支得局不载）→官旺救或食伤得局化劫；无财无官食伤泄秀；食被枭夺找财
                // 「鲁莽地说就错了」——有财不可遽断有子，必来回检查螳螂捕蝉黄雀在后
                val duoCai = biS >= 1.2 * caiS ||
                    (bazi.branches.count { it.element == biEl } >= 2 && bazi.monthPillar.branch.element == biEl)
                // 案六十六·贴克无儿：比劫透干贴克财干而其间无食伤通关（地支复冲伐财根）——
                // 「天干也是贴着财克、地支戌冲辰也砍伐」：纵走三十年财运发财——生不了儿子
                // （走好运只说明财富事业好，不能说能生儿子——有钱不一定有儿子）
                val caiIdxs = bazi.stems.indices.filter { it != 2 && bazi.stems[it].element == caiEl }
                val biIdxs = bazi.stems.indices.filter { it != 2 && bazi.stems[it].element == biEl }
                val shiIdxs = bazi.stems.indices.filter { it != 2 && bazi.stems[it].element == shiEl }
                // 财干与（比劫透干或日主）相邻＝贴克；食伤透干居比劫与财之间＝通关隔开（案六五戊隔——不贴克）
                val keZhuIdxs = biIdxs + 2
                // 案九十一·劫合化财（财变没了）：财干被劫干合而化神得势（丁壬合化木——化神局）
                // ——财变没了＋劫通根有力：生不出儿子只能生闺女；财是情缘线——财断克夫（念头论）
                val caiIdx91 = caiIdxs.firstOrNull { ci ->
                    val partner = GAN_HE[bazi.stems[ci]]
                    partner != null && bazi.stems.anyIndexed { j, o -> j != 2 && j != ci && o == partner && o.element == biEl }
                }
                val huaShen91 = Element.entries.firstOrNull { el -> el != caiEl && el != biEl && partyScore(bazi, el) >= 6.0 }
                val jieHeHuaCai = caiIdx91 != null && huaShen91 != null && biS >= FOLLOW_MIN_SCORE
                val tieKe = !jieHeHuaCai && biIdxs.isNotEmpty() && caiIdxs.isNotEmpty() &&
                    caiIdxs.any { ci -> keZhuIdxs.any { k -> kotlin.math.abs(ci - k) == 1 } } &&
                    !caiIdxs.any { ci ->
                        keZhuIdxs.any { k ->
                            val lo = minOf(ci, k)
                            val hi = maxOf(ci, k)
                            shiIdxs.any { si -> si in (lo + 1) until hi }
                        }
                    }
                when {
                    caiS > 0.0 && jieHeHuaCai -> ZN(caiEl, false, true,
                        "身旺财为子而**财干被劫干合化（${bazi.stems[caiIdx91!!].chinese}与劫合化${huaShen91!!.chinese}——化神得势，财变没了）**：劫通根有力（壬通根两亥之属）把财克了——**生不出儿子，只能生闺女**（案九十一）。**财是命中情缘线，男女财星不可断；情线一断财门关，男女皆是半路缘**——财是官杀（夫/六亲星）唯一的念头：**把念头去了就要克夫**（六亲死亡念头论：星弱被克不死——**把六亲星的念头克死了才死**——夫星全靠财生这口气，财被合化变没了＝希望没了；知道这点去离婚没事，90%去了念头要出人命）")
                    caiS > 0.0 && tieKe -> {
                        val guanJiu = guanS >= GUAN_WANG ||
                            bazi.stems.anyIndexed { i, s -> i != 2 && s.element == guanEl && bazi.branches[i].element == guanEl }
                        // 案一百一十一·财生出官：比劫贴克财而用神即官（财生官——螳螂捕蝉黄雀在后）：
                        // 就财论是没儿子的，然用神是官不是用财——劫克财克不尽（财通根多），多少能生官：有儿子（生得费劲）
                        val caiShengGuan111 = choice.way == JinBiaoQuYongWay.PEI_CAI && yongEl == guanEl &&
                            bazi.stems.any { it != dayMaster && it.element == guanEl } &&
                            jieKeCai111(bazi, yinEl, biEl, caiEl, guanS)
                        when {
                            // 案一百一十二·喜用两伤：身旺财为子——财不行找官，官也不行——没儿子（克妻损子）
                            liangShang112 != null -> ZN(caiEl, false, false,
                                "**身旺财为子，财不行找官，官也不行，也没儿子**（案一百一十二·喜用两伤）：${caiEl.chinese}之根被${liangShang112.chongShu}${liangShang112.chongShen.chinese}${liangShang112.beiChong.chinese}冲拔（${liangShang112.beiChong.chinese}中${caiEl.chinese}之根被拔——对勘案一百一十一：劫克财克不尽则财生出官有子），只余${liangShang112.shengCaiGen.joinToString("") { it.chinese }}中一点——**只值 10 分，还是个差生**；${guanEl.chinese}又被${yinEl.chinese}泄、被冲克——财不行、官也不行。**财是妻子，劫刃肆逞——离婚**（克妻两义：离婚／丧妻——克妻损子）。**此人上学不行、喜用两伤财运也不行**")
                            guanJiu -> ZN(guanEl, true, false, "身旺财为子而**比劫贴克财**——然官杀克劫护财（官透坐根有力）：**官杀为儿子，有官杀就有儿子**（怕食地——食伤制官来捣乱）")
                            caiShengGuan111 -> {
                                val caiGenN111 = bazi.branches.count { br -> branchHiddenStems.getValue(br).any { it.element == caiEl } }
                                ZN(guanEl, true, false,
                                    "身旺财为子而**比劫贴着克财——就财论是没儿子的**（儿子不取决于财了：如果不看别的，光看着财是没儿子的——案六十六贴克档）；然**螳螂捕蝉黄雀在后——财生出官来了，有儿子**（案一百一十一）：**用神是官、不是用财**——劫克财克不尽（${caiEl.chinese}通${caiGenN111}根），多少能生这个${guanEl.chinese}官，**不是说克一下就没了**；官不贴着劫也无问题（**挨着层次更高**）。**有儿子、事业还好——当官**；然**生儿子挺费劲**" +
                                        (if (bazi.stems.none { it != dayMaster && it.element == shiEl })
                                            "（食伤毕竟代表发泄，被这么旺的${yinEl.chinese}克着没力量）"
                                            else "") +
                                        "——实际命主有儿子")
                            }
                            else -> ZN(caiEl, false, false,
                                "**比劫贴着克财**（天干贴克＋地支冲伐财根——戌冲辰也砍伐）：**生不了儿子**——即使走三十年好运助起财星发了财也一样（**走好运只说明财富事业好，不能说能生儿子——有钱不一定有儿子**；原局贴着克＝功能层面废——运不能救，类比案六十四辰酉贴克）")
                        }
                    }
                    caiS > 0.0 && duoCai -> {
                        // 比劫夺财（重夺／地支得局——财虽有根而地支不载）——官旺救或食伤得局化劫
                        when {
                            // 案七十四·羊刃肆逞档：刃当令带印生＋飞刃透干（羊刃飞到天上叫飞刃）＋贴克财＋无官制刃
                            // ——功能废：大运走的再好也生不出来（财旺只代表财富还可以——财是精卵气血被刃破坏成分）
                            bazi.monthPillar.branch == YANG_REN[dayMaster] &&
                                bazi.stems.indices.count { it != 2 && bazi.stems[it].element == biEl } >= 2 &&
                                !bazi.stems.any { it.element == guanEl } && !hasBenQiRoot(bazi, guanEl) -> ZN(caiEl, false, false,
                                "**羊刃肆逞**（刃当令带印生〔带源头的羊刃〕＋飞刃透干〔羊刃飞到天上叫飞刃〕——贴着财克，远重于普通劫）而**无官杀制刃——没有这个功能：大运走的再好，也生不出来儿子**。财旺没错（还有食伤生财——俩人都旺）——**但财旺只代表财富还可以：财就是我的精卵、是我的气血——羊刃给我捣乱、破坏成分，就生不出来**。" +
                                    "**投胎论：申酉这俩儿子都过来投胎来了，但地支都是劫的——在我这待不住：生的也得流产，或者说童子命、或早年克子——只要他生了，肯定要克子的。没有这个功能，即使生了也得没了**（对勘案七十三：异性小刀轻克——没那么严重；本案羊刃带源飞刃——功能废）")
                            guanS >= GUAN_WANG || bazi.stems.anyIndexed { i, s -> i != 2 && s.element == guanEl && bazi.branches[i].element == guanEl } -> ZN(guanEl, true, false,
                                run {
                                    val yinTieShi82 = bazi.stems.indices.any { i -> i != 2 && bazi.stems[i].element == yinEl &&
                                        bazi.stems.indices.any { j -> j != 2 && bazi.stems[j].element == shiEl && kotlin.math.abs(j - i) == 1 } }
                                    "身旺财为子（财没露）而**比劫夺财**——官杀克劫解放财（官透坐根有力）：**官杀为儿子，有官杀就有儿子**。**暗克非冲不拔根**（如午亥之克——不是贴身的冲克：若亥换成子才叫拔根，午亥只作丁根弱论）" +
                                        (if (yinTieShi82) "；食神克官而**印贴克食神护官（把官给解放了）——百分之百有儿子**；再走官地（南方）之运——**直接锁定不可能没儿子（富贵得子）**。**不但生儿子，儿子也有出息**：印看住伤官＝给儿子一护卫（好条件、留遗产、教学习——捣乱儿子的东西全被印克了，儿子得福气）；**印护官主要关系事业**（身旺用官、印看食伤不使犯官——走官地事业发财）" else "；怕食地——食伤制官杀来捣乱（印不透干：无护官之神，食伤旺地损子）") +
                                        "。**辟谬**：穿一下变女孩、阳男阴女之说——与生男生女无关（案八十二）"
                                })
                            // 案七十二·明暗子星＋信号投胎论：官透而虚（甚至被化）然无贴克——儿子在这飘着等投胎：
                            // 看大运（运帮子星＝接收得到信号就能生；运全克子星＝接收不到、生不出来；
                            // 贴克则不看运——功能废吃药也不管事）。明以官杀为子，暗仍以财星为子（官为护财而设）
                            bazi.stems.any { it != dayMaster && it.element == guanEl } && run {
                                val guanIdxs = bazi.stems.indices.filter { it != 2 && bazi.stems[it].element == guanEl }
                                val keGuanEl = Element.entries.first { it.controls == guanEl }
                                val keIdxs = bazi.stems.indices.filter { it != 2 && bazi.stems[it].element == keGuanEl }
                                keIdxs.none { k -> guanIdxs.any { g -> kotlin.math.abs(k - g) == 1 } }
                            } -> {
                                val bangYun = daYun?.yuns?.take(5)?.count { y ->
                                    y.pillar.stem.element == guanEl || y.pillar.branch.element == guanEl ||
                                        y.pillar.stem.element.generates == guanEl || y.pillar.branch.element.generates == guanEl
                                } ?: 0
                                val keYun = daYun?.yuns?.take(5)?.count { y ->
                                    val keGuanEl2 = Element.entries.first { it.controls == guanEl }
                                    y.pillar.stem.element == keGuanEl2
                                } ?: 0
                                ZN(guanEl, bangYun > keYun, bangYun <= keYun,
                                    "**明以官杀为子，暗仍以财星为子**（身旺财为子被劫——官护财克劫：官为护财而设，本质仍财——案七十二）；官透而虚（乙被巳化之属）然**无贴克——儿子就在这飘着等投胎：此时才看大运**——" +
                                        (if (bangYun > keYun) "运帮子星（如东方${guanEl.chinese}旺之运）**接收得到信号——就能生儿子**" else "运全走克子星之干（庚辛之属）——**接收不到儿子的信号，生不出来**") +
                                        "；**若有贴克则不看大运**（功能废——哪怕走帮运也没儿子，吃药也不管事）")
                            }
                            shiS >= GUAN_WANG && shiS >= biS -> ZN(shiEl, true, false,
                                "身旺财为子而比劫夺财——**食伤得局完全把比劫化了**（劫不至重、间接解放财；比劫在年上、中间食伤${shiEl.chinese}隔着——不贴克）：**还能有儿子——更可以生儿子**（财得月令之气，帮的时候更旺；**对勘案六十二**：虚透无气之财——只因无比劫克才生、运过即损——**主要就是看季节：得气帮得起，无气帮不起**）（案六十补笔）")
                            // 案七十六·盖头官救档：比劫干同柱盖头坐财支（辛坐卯）而官支在局未被克（巳无水克）
                            // ——转官为儿：有这个功能可以生儿子（官来的年就生了）
                            bazi.stems.anyIndexed { i, s -> i != 2 && s.element == biEl && bazi.branches[i].element == caiEl } &&
                                hasBenQiRoot(bazi, guanEl) &&
                                partyScore(bazi, Element.entries.first { it.controls == guanEl }) < FOLLOW_MIN_SCORE -> ZN(guanEl, true, false,
                                "身旺财为子而**比劫盖头贴克财支**（${biEl.chinese}坐${caiEl.chinese}——同柱贴克：没儿子？**不一定——还要看有没有官：转官了**。${guanEl.chinese}官支在局**没有被克**（克${guanEl.chinese}之神无）——**有这个官我就有儿子：有这个功能可以生儿子——官来的年就生了**（案七十六）。**能生儿子不代表能发财**（能生儿的穷人有的是）：没被克只说明有生儿子的功能，但都是泄我的——**财官皆弱顶多小康、衣食无忧（这就算好命）**：化官生印转印身更旺；用神弱而**无克我捣乱者＝无灾无难（没有什么大疾病车祸——这不错了）**")
                            // 案一百零五·坏绝档：身旺财为子而财之本气支被比劫贴冲＋生财之食伤根被枭冲拔
                            // ——官财食之源俱伤：没儿子（网测有儿则儿必出事）；克妻至死或和尚命——终态无妻无子
                            caiS > 0.0 && run {
                                val caiGenZhi105 = bazi.branches.filter { it.element == caiEl }
                                val shiGenZhi105 = bazi.branches.filter { it.element == shiEl }
                                caiGenZhi105.isNotEmpty() && shiGenZhi105.isNotEmpty() &&
                                    caiGenZhi105.any { g -> val gi = bazi.branches.indexOf(g); (gi - 1 in bazi.branches.indices && bazi.branches[gi - 1].element == biEl) || (gi + 1 in bazi.branches.indices && bazi.branches[gi + 1].element == biEl) } &&
                                    shiGenZhi105.any { g -> LIU_CHONG[g] != null && LIU_CHONG[g] in bazi.branches }
                            } -> ZN(caiEl, false, false,
                                "**身旺没儿子（案一百零五·坏绝档）**：财为子而**财之本气支被比劫贴冲**（${caiEl.chinese}被${biEl.chinese}贴冲——劫冲财）＋**生财之${shiEl.chinese}食伤之根被枭冲拔**（支神只以冲为重——当令之${yinEl.chinese}冲拔${shiEl.chinese}根：枭夺食之重）——**没儿子**。若网测命主说有儿子——**那他儿子就要出事了：要么没了、要么蹲监狱、要么离异跟着对方走了**（同样的八字俩人发生的不一样，但都是克妻损子之理——有的当和尚、有的没结婚、有的结了克死或离了：**最终状态都得是无妻无子**）。**克妻**：枭神夺食克老丈人妨妻家——然支神只以冲为重，当令大${yinEl.chinese}＋劫冲财支，**克的就不是老丈人这么简单了——要把妻子克死了**（或另一种情况：不结婚——和尚命）。**财运也不好**：${shiEl.chinese}运来${if (EarthlyBranch.YIN in bazi.branches && EarthlyBranch.HAI in bazi.branches) "寅亥化木" else "通关"}能好点，**但原局坏得太绝了——只能温饱**（**好运分层定律**：有的走好运发大财、有的发小财、有的平平常常、有的只能温饱——原是要饭的一次要一个馒头，走好运能要两个：不能代表他就好了）")
                            // 案八十四·拱暗财档：财半合拱局暗存（亥未拱木）——有暗财＝有子（不仅有子还有钱）
                            // 案八十八·木火两旺转而生土（多生终得一子）：比劫得局夺财而食伤通关有力
                            // （木生火火生土——财有暗源）——基本上生不出儿子，然一路南方食伤运＋好流年
                            // ——使劲生碰出来的（生四个才出一个；无这旺火生一百个也生不出来）
                            caiS > 0.0 && shiS >= 2 * FOLLOW_MIN_SCORE && hasBenQiRoot(bazi, shiEl) &&
                                biEl.generates == shiEl && shiEl.generates == caiEl &&
                                // 护案八十四：财已拱局暗存（亥未拱木）者优先走财气通门户档——非多生型
                                !listOf(
                                    setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI) to Element.WOOD,
                                    setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU) to Element.FIRE,
                                    setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN) to Element.WATER,
                                    setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU) to Element.METAL,
                                ).any { (trio, el) -> el == caiEl && bazi.branches.count { it in trio } >= 2 && !trio.all { b -> b in bazi.branches } }
                            -> ZN(caiEl, true, false,
                                "身旺财为子而**比劫得局夺财——基本上生不出来儿子，然${biEl.chinese}${shiEl.chinese}两旺转而生${caiEl.chinese}**（比劫生食伤、食伤生财——财有暗源通关）：**多生终得一子**——一路南方${shiEl.chinese}运不停的生，**碰着好流年就生出来儿子了（生四个才出一个——使劲生）**；**若没有这比较旺的${shiEl.chinese}，生一百个也生不出来儿子**。**即使生出来的儿子，儿子也好不了**（后面走克${caiEl.chinese}之运——儿子没好日子）；无${shiEl.chinese}通关之比劫夺财局则无儿只生女（案八十八）")
                            // 案一百零六·合解冲救子档：食伤独根被印贴冲本没儿——冲神被合化（化神比劫）又有儿子
                            caiS > 0.0 && run {
                                val shiGen106b = bazi.branches.filter { it.element == shiEl }
                                val chongYin106b = shiGen106b.firstOrNull { g -> LIU_CHONG[g] != null && bazi.branches.contains(LIU_CHONG[g]) && LIU_CHONG[g]!!.element == yinEl }
                                chongYin106b != null && bazi.branches.count { it.element == shiEl } == 1 &&
                                    LIU_HE[LIU_CHONG[chongYin106b]!!]?.let { p -> p in bazi.branches && p.element == biEl } == true
                            } -> ZN(shiEl, true, false,
                                "**合解冲救子（案一百零六）**：印贴冲独食之根——**本来没儿子**；然冲神被六合化${biEl.chinese}（绝处逢生有情）——**又有儿子了**。就是在来回来去的变——就在于你对**生克力量上的掌握、情分方面的掌握**：反映在现实生活中他就有**生儿子的技巧、有巧劲**")
                            // 案一百零八·秀气流行有儿子：身旺用伤官泄秀——发泄能力强＝生育能力强
                            // （即使比劫见财也有儿子；用食伤的走财运不破财——不用之财尽可劫）
                            caiS > 0.0 && choice.way == JinBiaoQuYongWay.SHI_SHENG_CAI && yongEl == shiEl &&
                                bazi.branches.count { it.element == biEl } >= 3 &&
                                bazi.stems.any { it != dayMaster && it.element == shiEl } -> ZN(shiEl, true, false,
                                "**秀气流行有儿子（案一百零八）**：身旺官杀都制没有了、这么旺的财也被比劫克了——**也有儿子**：这局用伤官泄秀，秀气流行——**发泄能力强，生育能力强就有儿子**（即使比劫见财了还有儿子——食伤足够让我泄体）。**用食伤的走财运，即使比劫见财仍不破财**（不用之财尽可劫；食伤继续发泄生财——儿又生儿：走财运不但不破财还发财——**劫财见财必破财是有条件的**）")
                            caiS > 0.0 && shiS > 0.0 -> run {
                                val gongTrio2 = listOf(
                                    setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI) to Element.WOOD,
                                    setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU) to Element.FIRE,
                                    setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN) to Element.WATER,
                                    setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU) to Element.METAL,
                                ).firstOrNull { (trio, el) -> el == caiEl && bazi.branches.count { it in trio } >= 2 && !trio.all { it in bazi.branches } }
                                if (gongTrio2 != null) {
                                    val missing2 = gongTrio2.first.first { it !in bazi.branches }
                                    ZN(caiEl, true, false,
                                        "身旺财为子而**财拱局暗存**（${gongTrio2.first.filter { it in bazi.branches }.joinToString("") { it.chinese }}拱${caiEl.chinese}——玄机暗里存）：**有暗财的——命主是有子的（不仅有子，他还有钱——财气通门户：财星有情不在乎旺衰）**。**运支${missing2.chinese}填拱——一下就有子了**（且财局兑现发大财——儿子也有出息：往后好运越来越好）。取象：干${caiEl.chinese}之行（衣饰／古玩把件／家具／二手车／工艺品之属——同样八字不一定同行、五行同类）——走差运生意也不差（选对行）、小康；好运发大财（案八十四）")
                                } else {
                                    ZN(caiEl, false, true,
                                        "身旺财为子而**比劫地支得局夺财——财虽有根而地支不载**（终究土得局）；官弱救不动、食伤弱化不动——**没生儿子**；然有食伤生财（财透得气）——**有生育能力，生女儿不生儿**；运注：子星得地之运（如亥水之属）可生儿；若火土局之运生子——必是闺女（火生土旺，财不载）")
                                }
                            }
                            else -> ZN(caiEl, false, false, "身旺财为子而**比劫夺财、官救食化俱无**——**没儿子**")
                        }
                    }
                    caiS > 0.0 -> {
                        // 案九十·飞刃贴克财支：比劫干同柱盖头坐财支（丙坐申——羊刃飞出来贴着捣乱）
                        // 而官杀全无（无救）——食伤生财只主财富不管子事：生女儿不生儿（B超说儿子也没用）
                        if (bazi.stems.anyIndexed { i, s -> i != 2 && s.element == biEl && bazi.branches[i].element == caiEl } &&
                            !hasBenQiRoot(bazi, guanEl) && bazi.stems.none { it != dayMaster && it.element == guanEl }
                        ) {
                            ZN(caiEl, false, true,
                                "身旺财为子而**比劫羊刃飞出来贴着捣乱**（${biEl.chinese}坐${caiEl.chinese}——同柱盖头贴克财支：时干比劫通根克财）：**生的是女儿——生不出来儿子**（食神生${caiEl.chinese}财也不管事儿——**食伤生财是指财富**，救不了子星）；**再走印比之运（寅亥化木之属）——百分之百生女儿**（即使B超检查说生个儿子也没用——B超也有错的时候——案九十）")
                        } else if (choice.way == JinBiaoQuYongWay.HUAI_YIN && strength == DayStrength.STRONG && yinS >= 8.0 &&
                            bazi.stems.none { it != dayMaster && it.element == caiEl } && run {
                                // 案一百零九·藏财之支尽被贴冲且冲神藏干克财（藏金暗相坎伐）——财根气被砍没
                                val gens = bazi.branches.filter { br -> branchHiddenStems.getValue(br).any { it.element == caiEl } }
                                gens.isNotEmpty() && gens.all { g ->
                                    val gi = bazi.branches.indexOf(g)
                                    listOf(gi - 1, gi + 1).any { k ->
                                        k in bazi.branches.indices && LIU_CHONG[g] == bazi.branches[k] &&
                                            branchHiddenStems.getValue(bazi.branches[k]).any { it.element.controls == caiEl }
                                    }
                                }
                            }
                        ) {
                            // 案一百零九·土重金埋：印过旺用财破印而财根被冲伐尽——直断克妻无子
                            ZN(caiEl, false, false,
                                "**克妻，无子（案一百零九·土重金埋）**：地支印旺肯定身旺——土重金埋；食伤泄秀又被印克回去；印过旺用${caiEl.chinese}破印——${caiEl.chinese}只藏于地支，然两组贴冲藏干互战（藏${Element.entries.first { it.controls == caiEl }.chinese}暗相坎伐）**财都没了，就完了**。**辟「冲开财库必发」**：用${caiEl.chinese}就指望${caiEl.chinese}星这一点点根气，被砍伐冲没了还能行吗——「财官临库喜刑冲」不可以这么用（**刻舟求剑**）；一切都得根据原局喜忌：原局把${caiEl.chinese}的根气坎没了，走好运也只是要饭的多要一个馒头")
                        } else if (!caiZai && !hasBenQiRoot(bazi, caiEl)) {
                            // 案六十二·财虚透而没被比劫克——生育能力在（贫富与子嗣两轨）：走财得根运得儿子
                            val deGenYuns = daYun?.yuns
                                ?.filter { yun -> branchHiddenStems.getValue(yun.pillar.branch).any { it.element == caiEl } }
                                ?.map { it.pillar.fullName } ?: emptyList()
                            ZN(caiEl, true, false,
                                "**身旺财为子——财虚透而没被比劫克**：**生育能力在**（**贫富与子嗣两轨**——富人也生儿子，穷人也生儿子：大运帮不起来＝财富不行温饱打工，**不代表穷人就生不了儿子**——只要财没被破坏，大运一帮就有机会生）" +
                                    "；**走财得根的大运是得儿子的**（如${if (deGenYuns.isNotEmpty()) deGenYuns.joinToString("、") + "之属" else "财藏根支之运"}）" +
                                    "；**财运过后防损子**——其后克泄财之运损子：死／送人／**离婚判给对方（孩子跟人家也行——反正不在你身边就破解了）**；若离婚儿子还判给你——儿子就要没了（必须的）")
                        } else {
                            ZN(caiEl, true, false, "**身旺财为子**（有财即有子；怕劫地——比劫给我克了）")
                        }
                    }
                    guanS >= FOLLOW_MIN_SCORE -> run {
                        // 案八十·拔根档：官煞为儿而官之本气根支被贴冲拔根（子午冲——两丙之根被拔）——
                        // 拔根比虚透罪加一等：没儿子生闺女（运支救根只救财富不救生育——子午冲在功能废；
                        // 年轻走克官之干〔壬癸水〕百分之百生不出来；走生官支〔寅卯〕救起——不好说可生）
                        val guanGenZhi = bazi.branches.filter { it.element == guanEl }
                        val baGen = guanGenZhi.any { g ->
                            val gi = bazi.branches.indexOf(g)
                            (gi - 1 in bazi.branches.indices && LIU_CHONG[g] == bazi.branches[gi - 1]) ||
                                (gi + 1 in bazi.branches.indices && LIU_CHONG[g] == bazi.branches[gi + 1])
                        }
                        if (baGen) {
                            ZN(guanEl, false, true,
                                "身旺无财——**官煞为儿，然${guanEl.chinese}官之本气根被贴冲拔根**（如子午冲——两${guanEl.chinese}之根被拔）：**拔根比虚透罪加一等**（虚透之${guanEl.chinese}〔坐辰申之属〕都比被拔根的强）——官衰弱、${guanEl.chinese}又坐随党之支——**基本上没儿子了**。运之解：年轻走克${guanEl.chinese}之干（壬癸之属）——**百分之百生不出来**；走生${guanEl.chinese}之支（寅卯之属）救起官根——**只说明财富好，解不了贴冲这事——没有生儿子的功能**（除非运支合去冲神）。**命主生闺女，财富还可以**（案八十·对勘案四十八印得局官二代）")
                        } else {
                            ZN(guanEl, true, false, "身旺无财——**官煞为儿**（喜官杀克我；怕食地——食伤制官杀捣乱；月上比劫劫住年官替我受克则子归挡者）")
                        }
                    }
                    shiS >= FOLLOW_MIN_SCORE && yinS < YIN_HUAI -> ZN(shiEl, true, false, "身旺无财无官——**食伤泄秀为儿**：泄得好没破坏＝**多子**")
                    shiS >= FOLLOW_MIN_SCORE -> {
                        if (caiS > 0.0) ZN(caiEl, true, false, "身旺食伤泄秀而**枭印夺食**——**财坏印解放食伤：仍以财为子，有子**")
                        else ZN(shiEl, false, false, "身旺食伤泄秀而**枭印夺食又无财救**——**没儿子**（伤官不见财，男孩不见面）")
                    }
                    else -> ZN(biEl, false, false, "身旺而财官食俱无——子女另裁（待师传）")
                }
            } else {
                // 身弱：印做儿／比劫为儿——官杀欺衰查印化、财坏印查比劫护、无印比劫扛
                val keYinS = partyScore(bazi, caiEl) // 财坏印（克印之神）
                val guQiS = guanS
                when {
                    // 案七十·贴克优先（非官旺线）：身衰印做儿而财贴克印——没有生儿子的功能
                    caiTieYin && yinYouLi && guQiS < FOLLOW_MIN_SCORE -> ZN(yinEl, false, true,
                        "**身衰印做儿（${yinEl.chinese}是我的儿子）然不变之中也有变化——螳螂捕蝉黄雀在后：${caiEl.chinese}财有力贴身克我（印）——那就生不出来**。**即使大运走帮身之运也是生不出来：这个人没有生儿子的功能——能不能生儿子，人一出生就决定了**（案七十）。因果全息：祖坟西方有变压器之类、父辈做事风格（${caiEl.chinese}老克印）、婆家积德行善——万物全息连着串儿（生女与否看解放之运）")
                    guQiS >= FOLLOW_MIN_SCORE && yinYouLi -> {
                        // 官杀欺衰主——除非有印：印为儿；螳螂捕蝉黄雀在后查财坏印；木坚金缺查子星所克过旺
                        val haoQueS = partyScore(bazi, yinEl.controls) // 子星所克之神过旺——木坚金缺（剩一点气生闺女）
                        // 案六十七·财贴克印：财干贴克印干——忌神深重贴着我克：运去病也不修复生育程序
                        val caiIdxs2 = bazi.stems.indices.filter { it != 2 && bazi.stems[it].element == caiEl }
                        val yinIdxs2 = bazi.stems.indices.filter { it != 2 && bazi.stems[it].element == yinEl }
                        val caiTieKeYin = caiIdxs2.isNotEmpty() && yinIdxs2.isNotEmpty() &&
                            yinIdxs2.all { yi -> caiIdxs2.any { ci -> kotlin.math.abs(ci - yi) == 1 } }
                        when {
                            // 案九十三·拱财坏印未贴克档：财拱局透干引化得局（亥未拱木乙露干）而印当令——
                            // 财未透干贴克印干（隔不贴克）＝生育功能在：生得出儿子（走官杀运生儿子——印星应期）；
                            // 然拱财坏用神印——财为妻起最坏的作用：他得离婚；
                            // 反事实：印边之支若换作两财本气支贴克——不光离婚，儿子也生不出来
                            gongCaiYinHua && !caiTieKeYin &&
                                !bazi.branches.anyIndexed { i, br ->
                                    br.element == yinEl &&
                                        ((i + 1 in bazi.branches.indices && bazi.branches[i + 1].element == caiEl) ||
                                            (i - 1 in bazi.branches.indices && bazi.branches[i - 1].element == caiEl))
                                } -> ZN(yinEl, true, false,
                                "**身衰印做儿（案九十三）**：${guanEl.chinese}官杀克身＋${shiEl.chinese}食伤旺泄——日主转弱，${yinEl.chinese}印当令为儿。财虽得局（${gongCaiTrio!!.first.filter { it in bazi.branches }.distinct().joinToString("") { it.chinese }}拱${caiEl.chinese}——拱局透干则实）而坏印，然**财毕竟没有露出来贴着克**（${caiEl.chinese}干隔位不贴印、${yinEl.chinese}印本气支亦未被${caiEl.chinese}本气支贴克）——**生育功能在：生得出儿子**（${monthBranch.chinese}土当令，他可以生出来儿子）。**运注：走${guanEl.chinese}运的时候生儿子**（官杀生${yinEl.chinese}印——印星应期）。**然拱财坏${monthBranch.chinese}印——财为妻而毁用神（妻子起了最坏的作用，妻子让命主很难受）：他得离婚**（克妻两义：一离婚、一丧妻）。**反事实**：${monthBranch.chinese}边之支若换成两${caiEl.chinese}（财本气支贴克印本气支）——**他不光离婚，而且也生不出来儿子**（贴克＝功能废）")
                            // 案七十三·异性轻克档：印本气根被财支贴克而阴阳异性（酉是小刀寅是树——小刀刻不断树）
                            // ＋印有官杀贴生（壬坐寅之属）——弱而不废：弱代表穷，不代表生不了儿子（可以生；
                            // 若同性重克〔大申金贴寅〕则废——生不了）。印弱＝事业层次不行；儿子学习随印运
                            bazi.branches.anyIndexed { i, br ->
                                br.element == yinEl &&
                                    ((i + 1 in bazi.branches.indices && bazi.branches[i + 1].element == caiEl && (bazi.branches[i + 1].ordinal % 2) != (br.ordinal % 2)) ||
                                        (i - 1 in bazi.branches.indices && bazi.branches[i - 1].element == caiEl && (bazi.branches[i - 1].ordinal % 2) != (br.ordinal % 2))) &&
                                    bazi.stems.anyIndexed { j, s -> j != 2 && s.element == guanEl && bazi.branches[j] == br }
                            } -> ZN(yinEl, true, false,
                                "**印为儿而印本气根被财支贴克——然异性之克不重（${caiEl.chinese}是小刀、${yinEl.chinese}是树——小刀刻不断树）＋官杀贴生（壬坐寅之属）：印只是弱，没有被彻底克坏——弱代表她穷，不代表她生不了儿子**（**是可以生儿子的**；若是大${caiEl.chinese}〔同性重克〕贴着克——命主就生不了儿子了）。**一切由印星决定**：印弱＝事业层次不行、财富值低（又走克印之运则贫穷）；印星是儿子——**印运即子运：儿子学习随印运**（走克印之运儿子学习差）")
                            // 案七十七·隔克不算克档：财贴克一印而另一印隔开未被贴克——功能在：能生儿子
                            caiIdxs2.isNotEmpty() && yinIdxs2.size >= 2 &&
                                yinIdxs2.any { yi -> caiIdxs2.any { ci -> kotlin.math.abs(ci - yi) == 1 } } &&
                                yinIdxs2.any { yi -> caiIdxs2.none { ci -> kotlin.math.abs(ci - yi) == 1 } } -> ZN(yinEl, true, false,
                                "**隔克不算克**：${caiEl.chinese}财贴克一印（年干），然**另一印隔开未被贴克——功能在：能生儿子**（虽是异性的克贴着克，也是有影响的——但一印被克一印独存，具备生儿子的功能）。**况且走印运（帮身化杀）——很确定可以生儿子：百分之百有儿子**（对勘案六十七：独印被有力之财贴克＝功能废；本案一印独存＝功能在——案七十七）")
                            // 案八十三·贴克三态（生儿子态）：财贴克印而①印有劲（印干坐支通源——秋水通源）
                            // ②异性之克 ③财被局泄化（泄财之神得局——金得局泄土土完了：秋土本虚＋金旺泄土）
                            // ——即使印被贴身克，命主也生儿子（穷人也生儿子）
                            caiTieKeYin && run {
                                val yinIdx = yinIdxs2.firstOrNull { yi -> caiIdxs2.any { ci -> kotlin.math.abs(ci - yi) == 1 } }
                                yinIdx != null &&
                                    branchHiddenStems.getValue(bazi.branches[yinIdx]).any { it.element == yinEl } &&
                                    bazi.stems[yinIdx].yang != bazi.stems[caiIdxs2.first { ci -> kotlin.math.abs(ci - yinIdx) == 1 }].yang &&
                                    partyScore(bazi, caiEl.generates) >= 6.0 && bazi.monthPillar.branch.element == caiEl.generates &&
                                    // 财无生源（生财之神党力<1.5——扣除随泄神成局之支：案八三巳随巳酉丑金局不作火源论；
                                    // 案六七丁有卯源带源头——不入此态）
                                    run {
                                        val scEl = Element.entries.first { it.generates == caiEl }
                                        var sc = partyScore(bazi, scEl)
                                        // 泄神成局之支随局走——其本气（如金局中之巳火根）不作生财之源论
                                        val juTrio = listOf(
                                            setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU) to Element.METAL,
                                            setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN) to Element.WATER,
                                            setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI) to Element.WOOD,
                                            setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU) to Element.FIRE,
                                        ).firstOrNull { (trio, el) -> el == caiEl.generates && trio.all { it in bazi.branches } }
                                        if (juTrio != null) {
                                            sc -= 1.5 * juTrio.first.count { it.element == scEl && it in bazi.branches }
                                        }
                                        sc < 1.5
                                    }
                            } -> ZN(yinEl, true, false,
                                "官杀欺衰主有印化（**印为儿**），财虽贴身克印——然**贴克三态（案八十三）**：①**印有劲**（${yinEl.chinese}干坐支通源——秋水通源）②**异性之克**（不重）③**财被局泄化**（泄财之${caiEl.generates.chinese}得局——${caiEl.chinese}土全被化了：秋土本虚＋${caiEl.generates.chinese}旺泄土＋得局——**土完了**）——**即使印被贴身克，命主也生儿子（穷人也生儿子）**。**对勘系谱**：案七三（夏＋异性克印有劲——生儿）↔ 案六七（带源头火旺贴克——生不了）↔ 案七四（大夏飞刃贴克——生不出来）↔ 案八一（贴克多大劲——生不了）——**贴克之废在三件俱反：印无劲／同性重／财带源有力**")
                            caiTieKeYin -> ZN(yinEl, false, true,
                                "官杀欺衰主有印化（**印为儿**），然**财贴着我的印来克**（财有力贴着克——螳螂捕蝉黄雀在后；忌神深重贴着我克——他给我捣乱干扰）：**生理上没有生男孩的功能、染色体就不对了**——即使大运解决这个病也一样生不出来儿子（八字程序先天受损，医院拍片也是一样——**解决了病也没有用：运去病不修复生育功能**）；走好运把印解放了——**能生个闺女**；若再走克${yinEl.chinese}之运（如南方火地）——**连闺女都生不出——必无子之命**（八字里男女都是一样看的——案六十七）")
                            keYinS >= 1.5 * yinS && keYinS > 0 -> run {
                                // 案八十五·支层贴克（戌大实土贴克子）：干多不如支重地支不载——
                                // 印本气支被财本气支贴克（大实土力量大）——戌土一克没儿子（官再旺没用印一字定之）
                                val zhiTieKe = bazi.branches.anyIndexed { i, br ->
                                    br.element == yinEl &&
                                        ((i + 1 in bazi.branches.indices && bazi.branches[i + 1].element == caiEl) ||
                                            (i - 1 in bazi.branches.indices && bazi.branches[i - 1].element == caiEl))
                                }
                                ZN(yinEl, false, false,
                                    "官杀欺衰主有印化（印为儿），然**财坏印坏成**（螳螂捕蝉黄雀在后）——**没儿子**" +
                                        (if (zhiTieKe) "。**干多不如支重（案八十五）**：地支不载（比劫透而无根），**${caiEl.chinese}大实土贴身克${yinEl.chinese}本气支**（力大——戌克子之属）——**一克没儿子**；层次**完全指着印星一字定之**（化官生印「官大坐大官」之说不成立——日主没力量，**官再旺没有用**：印被贴克就完了，官杀旺也化不了——**经济困难、还没有儿子**）" else "") +
                                        "。**一印双象论（案八十一）**：此印**既当财富看、也当他的儿子**——印废则**贫而无子**（双失：走好运温饱解决衣食无忧——**发不了财**也**生不了儿子：原命没有这个功能**）。**无子五象**：贫而无子／**夭而无子**（走克印墓地基本活不成）／富而无子／贵而无子／贱而无子——以局配之；**未运穿克子水（六害）之属——牢狱之应**（蹲大牢——穿害之应期）")
                            }
                            // 案九十五·财全藏支不透（无贴克）——丁没事儿：有儿子（喜神被拔只降层次不碍子——
                            // 先于木坚金缺：食伤反克虽重，印透干立得住即功能在）
                            guoXieFanRuo && caiIdxs2.isEmpty() -> ZN(yinEl, true, false,
                                run {
                                    val yinZhu95 = bazi.stems.indices.firstOrNull { j -> j != 2 && bazi.stems[j].element == yinEl }
                                    val genZhu95 = yinZhu95?.let { bazi.branches[it] }
                                    val chong95 = genZhu95?.let { LIU_CHONG[it] }
                                    "**身衰印做儿（案九十五）**：${shiEl.chinese}多过泄身弱用印，印（${yinEl.chinese}）为儿。**${caiEl.chinese}财全藏地支不透干——没有贴着克印：${yinEl.chinese}没事儿——有儿子**（${genZhu95?.chinese ?: ""}喜神被拔只关乎层次学历，不碍子嗣——富贵子嗣两轨）。**反事实**：${chong95?.chinese ?: ""}换${caiEl.chinese}本气支（亥水之属）、${genZhu95?.chinese ?: ""}换巳火——巳亥逢冲**去火存金（火根转金根）：印根尽废——那就没儿子了**"
                                })
                            haoQueS >= 2.0 * yinS && yinS > 0.5 -> ZN(yinEl, false, true, "**官杀欺衰主——除非有印，印为儿**（印星就是我的儿子，虽在年上——有没有儿子取决于他，印好才是真的好）；然**木坚金缺**（子星所克之神党力${format(haoQueS)}压过子星${format(yinS)}——食伤得局反克，金剩一点气；若是酉金被冲彻底毁，连闺女都生不出来）——**没儿子，可生闺女**")
                            haoQueS >= 2.0 * yinS -> ZN(yinEl, false, false, "官杀欺衰主有印化（印为儿），然子星被所克之旺神拖至**彻底缺绝**——**连闺女都生不出来**（若是酉金还走火运，必然不生育）")
                            keYinS > 0.0 -> ZN(yinEl, false, true, "官杀欺衰主有印化（印为儿），然**财轻克而未坏成印**——能生个闺女；万一财更轻凑合生出儿子——**必然逆子**（财有多重儿子就有多不孝：轻则没礼貌、中则啃老、重则破大财打骂爹妈）")
                            else -> ZN(yinEl, true, false, "官杀欺衰主——**除非有印，有印就有子**：印为儿，子星昌（怕财地——局里有财命局隐患、大运有财损子运）")
                        }
                    }
                    guQiS >= FOLLOW_MIN_SCORE -> {
                        // 官杀欺衰而无印——比劫扛得住才有子；比劫子星被克泄过重——凑合生子必损＋生女儿
                        val biKeS = partyScore(bazi, Element.entries.first { it.controls == biEl })
                        val biXieS = partyScore(bazi, biEl.generates)
                        // 案六十四·贴身深克：比劫子星之本气根被官杀本气支贴身冲（相邻）而官杀党力压过——
                        // 忌神深重贴身克用神：没儿子只能生女儿（没有这个能力和功能）
                        val tieChong = bazi.branches.filter { it.element == biEl }.any { bg ->
                            val idx = bazi.branches.indexOf(bg)
                            (idx - 1 in bazi.branches.indices && bazi.branches[idx - 1].element == guanEl && LIU_CHONG[bg] == bazi.branches[idx - 1]) ||
                                (idx + 1 in bazi.branches.indices && bazi.branches[idx + 1].element == guanEl && LIU_CHONG[bg] == bazi.branches[idx + 1])
                        }
                        // 案八十九·官杀得局废比劫：官杀之支三会/三合成局而比劫之根尽随局（辰在寅卯辰中）
                        // 或尽虚土（丑）——比劫全废扛不住：没儿子（走火运也生不出来——官杀得局功能废）
                        val guanDeJu = SAN_HUI_JU.any { (bs, _) -> bs.all { it in bazi.branches } && bazi.monthPillar.branch in bs } ||
                            listOf(
                                setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN),
                                setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI),
                                setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU),
                                setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU),
                            ).any { trio -> guanS >= 6.0 && trio.all { it in bazi.branches } }
                        val biGenFei = bazi.branches.filter { it.element == biEl }.all { b ->
                            b in (SAN_HUI_JU.firstOrNull { (bs, _) -> bs.all { it in bazi.branches } }?.first ?: emptySet()) ||
                                b == EarthlyBranch.CHOU || b == EarthlyBranch.CHEN
                        }
                        when {
                            guanDeJu && bazi.branches.any { it.element == biEl } && biGenFei -> ZN(biEl, false, false,
                                "**官杀得局废比劫（案八十九）**：官杀之支会局（寅卯辰方局之属）——身衰印坐儿而无印，**生得出来儿子吗？不能——没这功能，生不出来儿子**（比劫之根尽随局〔辰在寅卯辰中随木〕或尽虚土〔丑〕——**比劫全废扛不住：走火运也生不出来**——官杀得局功能废）。日主有丑辰两根＋**食伤制杀有一定层次（食神去官杀之混杂——庚合乙之属）——国家单位上班（牙医之属）：有一定层次不代表有钱**——身弱只能用劫而劫废：**挣了钱也存不住——白领层次发不了财**（一路没走火土运一辈子发不了大财小财）；**食神制杀兼泄身——食神主生殖：老搞邪淫天天破财**（官杀混杂老来骚扰——今儿破明儿破——挣多少也没用）")
                            // 案七十五·虚劫不管事档：比劫本气根尽是辰丑虚土（古训：辰丑之土非彼未戌之实也）
                            // 而泄神会局得令（大王王炸）——比劫都虚了不管事：必须找印；无印＝没有功能——生闺女
                            bazi.branches.filter { it.element == biEl }.all { it == EarthlyBranch.CHEN || it == EarthlyBranch.CHOU } &&
                                bazi.branches.any { it.element == biEl } &&
                                partyScore(bazi, shiEl) >= 6.0 && bazi.monthPillar.branch.element == shiEl -> ZN(biEl, false, true,
                                "**比劫都虚了不管事**（比劫本气根尽是${biEl.chinese}之辰丑虚土——古训：**辰丑之土非彼未戌月之实也**）而**泄神${shiEl.chinese}会局得令＝王炸大王**（会局在当令季节＝最大的牌；对勘案七十三夏局＝小王——同样是会局也不一样）——**必须找印；命局没有印，就不能生儿子了：八字没有这个功能——不能生儿子，生的是闺女**（泄体太过、金得局、克泄交加）。**穷人也可以生女儿**（贫富子嗣两轨）；用神只能用劫而劫很弱——**劫财决定财富，劫很弱＝没儿子也富不了**（无格局只能打工：并不是因为制的杀小没钱，而是用神弱了财富值才低；走帮用好运＝有积蓄〔破财少会理财攒钱〕而不发富）")
                            // 案六十九·身体条件档：月令比劫根被两官支冲＋食伤贴泄＋无印——克泄交加
                            // 身体不行（气血不足）：八字不具备生儿子功能——再有钱再格局也流产/生不了男孩
                            bazi.monthPillar.branch.element == biEl &&
                                bazi.branches.count { it.element == guanEl && LIU_CHONG[it] == bazi.monthPillar.branch } >= 2 &&
                                hasBenQiRoot(bazi, shiEl) -> ZN(biEl, false, false,
                                "**克泄交加·身体条件不行**（两${guanEl.chinese}贴身克＋${shiEl.chinese}食伤贴泄＋**无一印生扶**）：**生小孩得看身体条件——再有钱、身体不行气血不足也生不出来**。**八字就不具备生儿子这个功能：再有格局（食神制杀以威出身大富）也要流产、生不了男孩**——走好运也没用、吃再多补品保健品也没用（**没生出来不代表八字没格局**——两轨并行；怀孕流掉之应〔己亥年之类〕）。身衰印坐儿——没有印，就没有儿")
                            tieChong && guanS >= biS -> ZN(biEl, false, true,
                                "**官杀欺衰主而无印——比劫子星之根被官杀贴身冲克**（贴冲＋辰酉合化金力大——忌神深重贴身克用神）：**没儿子，只能生女儿**——属于**没有这个能力和功能**（即使走印化杀之运，生的也不是儿子——运化忌只说明财运事业好了，**不能说明有儿子**；看子女跟看财运不一样——富贵贫贱与有没有儿子没有关系）")
                            biS < FOLLOW_MIN_SCORE -> ZN(biEl, false, false, "**官杀欺衰主，无印、比劫又扛不住**——**没儿子**（官杀无制化攻身——生了儿子也得夭折）")
                            biKeS >= 0.75 * biS && biXieS >= 1.5 * biS -> ZN(biEl, false, true,
                                "**官杀欺衰主而无印——比劫扛得住（坐根有气）＝比劫为儿**；然**子星被克泄得太重**（两杀克＋食伤泄＋会局泄根——${biKeS}克${biXieS}泄压过子星${format(biS)}）：大运帮起来（壬子之属）**凑合生出来儿子——终究损子**（运补暂生而生出来没活成之属——**墓中子星要送出**破解）；**比劫有气坐根——生女儿百分之百**（有生育能力；若无会局之泄，走帮身运是可以有儿子的）")
                            else -> ZN(biEl, true, false, "**官杀欺衰主而无印**——比劫扛得住（够旺）**比劫为儿也有子**（身弱杀日之间比劫卡住挡杀护我＝我有儿子——**即使走差运也有儿子：有这个功能，走到好流年就可以生**；扛不住则无子。**详推反向**：身旺喜杀克我——中间比劫卡住杀克不到我——**反而生不了儿子（此时有侄儿——子归挡者）**；身弱怕杀克我——比劫卡住——**可以生儿子**——案六十八）")
                        }
                    }
                    yinYouLi && keYinS < FOLLOW_MIN_SCORE -> {
                        if (yinTouZuoBen) ZN(yinEl, true, false, "**身衰印做儿**（印为儿，印星就是我的儿子——印好才是真的好）")
                        else ZN(yinEl, true, false, "**身衰印做儿**（有印有比劫而无财——印肯定是儿子，印此时不作母亲论；怕财地）")
                    }
                    yinYouLi -> {
                        // 案七十八·隔克不算克（干层）：财干克印干而**隔位非贴**＋印贴身生（丁贴日主）——
                        // 印弱＝儿子不给力、生得比较衰——不代表生不了儿子（能生；癸没贴着丁克）
                        val caiIdxs3 = bazi.stems.indices.filter { it != 2 && bazi.stems[it].element == caiEl }
                        val yinIdxs3 = bazi.stems.indices.filter { it != 2 && bazi.stems[it].element == yinEl }
                        val caiGanGe = caiIdxs3.isNotEmpty() && yinIdxs3.isNotEmpty() &&
                            caiIdxs3.any { ci -> yinIdxs3.all { yi -> kotlin.math.abs(ci - yi) > 1 } }
                        when {
                            caiGanGe && (bazi.monthPillar.stem.element == yinEl || bazi.dayPillar.branch.element == yinEl) -> ZN(yinEl, true, false,
                                "身衰印做儿而财坏印——**得详推：${caiEl.chinese}财没有贴着${yinEl.chinese}印克（隔位）——印还是贴着生的**：**印弱是弱——儿子不给力、儿子生的比较衰——不代表我生不了儿子**（能生；只是儿子财富值低/${caiEl.chinese}运直接判死刑没有财富值——然三十年河东河西：坎离震兑四方各管三十年，北方差不一定东方差——**寅亥化木翻身在后面**——案七十八）")
                            biS >= FOLLOW_MIN_SCORE -> ZN(biEl, true, false, "身衰印做儿而**财坏印**——**比劫克财护印：比劫为儿，有子**")
                            else -> ZN(yinEl, false, false, "身衰印做儿而**财坏印、比劫又无**——**没儿子**")
                        }
                    }
                    else -> {
                        // 案八十六·比劫为子的真正本质：明以比劫为子，暗仍以财星为子——比劫须真正
                        // 把日主帮到身旺（方能胜任财）才有儿子；比劫被食伤泄太重（无力）——比劫没法
                        // 帮你生（帮＝事业有情跟着挣钱；生孩子不能让人家帮生）——没儿子
                        val biXieS2 = partyScore(bazi, biEl.generates)
                        when {
                            // 案一百一十七·干多不如支重：比劫虚透无根（地支不载）——虚劫不管事：不能有儿子；
                            // 纵走印运好运生子——运过子没、或离婚子跟对方（运补暂生、运过损子）
                            ganDuoBuZai117 -> ZN(biEl, false, false,
                                "**身弱无印而比劫虚透无根——干多不如支重，${dayMaster.chinese}${biEl.chinese}叫地支不载**：比劫连自身都帮不了——**不能有儿子**（承载不了这么旺的${caiEl.chinese}财——没这个本事和能力；案一百一十七）。运注：**纵走印运好运生了儿子——好运过去儿子也得没了，或者离婚儿子跟对方走了**（不在身边即破解）")
                            biS < FOLLOW_MIN_SCORE -> ZN(biEl, false, false, "身弱无印无比劫——**没儿子**")
                            biXieS2 >= 2.0 * biS -> ZN(biEl, false, false,
                                "身弱局里没有印——比劫线，然**比劫为子的真正本质（案八十六）**：**明以比劫为子，暗仍以财星为子——比劫得真正把你帮到身旺了、你才能胜任这个财，才有儿子**。此局比劫被${shiEl.chinese}食伤泄太重（${format(biXieS2)}泄对${format(biS)}——秋天土金两旺比劫没劲）——**比劫没法帮你生儿子**：有比劫帮＝比劫对你有情、有贵人帮——**这说的是事业**（跟着比劫挣钱、财富比劫一子定之——寅卯拱火东南运事业层次提升）；但**生儿子不能让比劫去帮你生——比劫帮代表比劫有儿子，比劫生的儿子不是你的儿子**（挣钱有让人家帮挣的——生孩子不能让人家帮生）→**比劫没帮到身旺——没儿子**（再走湿土泄火运，百分之百生不出）")
                            else -> ZN(biEl, true, false, "身弱局里没有印（必财旺或官杀旺）——**比劫就是儿子**（抗杀挡杀镇财给我力量）：有比劫才有儿子")
                        }
                    }
                }
            }
            // 运注：子星之克神运＝损子／生不出之运
            val keZiEl = Element.entries.first { it.controls == zn.el }
            val keZiYuns = daYun?.yuns?.filter { it.pillar.stem.element == keZiEl || it.pillar.branch.element == keZiEl }
                ?.map { it.pillar.fullName } ?: emptyList()
            cn.xulun.bazi.model.JinBiaoZiNv(
                qingKuang = when {
                    zn.er -> cn.xulun.bazi.model.JinBiaoZiNvQingKuang.BI_YOU_ER
                    zn.nv -> cn.xulun.bazi.model.JinBiaoZiNvQingKuang.WU_ER_YOU_NV
                    else -> cn.xulun.bazi.model.JinBiaoZiNvQingKuang.BI_WU_ER
                },
                ziXing = representativeGod(bazi, dayMaster, zn.el),
                ziEl = zn.el,
                basis = zn.text + (if (keZiYuns.isNotEmpty()) "；运注：**${keZiEl.chinese}为子星之克神**——行${keZiYuns.joinToString("、")}运克子星（生不出儿子／损子）" else ""),
            )
        }
    }

    private fun verdictOf(items: List<JinBiaoStanceItem>, ctx: YunCtx): JinBiaoVerdict = when {
        // 命一补丁：官干犯旺激怒／地支食伤被枭夺——作凶论（权在一人不可触犯）
        items.any { it.xiongOverride } -> JinBiaoVerdict.XIONG
        // 案一百二十一·平衡之局（闲神立功）——非财官之运不作凶论（大凶档让位）：走什么都不怕；
        // 案一百二十四·有力就能食神制杀——药神（水）到位、比劫帮身、湿土化杀之运皆吉（整运论——「子水运去病发大财」「申运肯定是好运」）
        ctx.shiZhi124 && items.any { it.favorOverride } -> JinBiaoVerdict.JI
        // 案一百二十五·假去寡——火土运去寡（泄秀）有层次（吉）；金水运只能用官、印运没法去寡（层次低——中平）
        ctx.jiaQuGua125 && items.any { it.favorOverride } -> JinBiaoVerdict.JI
        ctx.jiaQuGua125 && items.any { it.pingOverride } -> JinBiaoVerdict.PING
        // 案一百二十二·病太大之局——来水（找源头）仅凑合（大凶档让位）；案一百二十三·木运补官之源——不如丙戌（中平）
        (ctx.youQing121 || ctx.bingDa122 || ctx.yongGuan123) && items.any { it.pingOverride } -> JinBiaoVerdict.PING
        // 大凶优先（弱极行比劫印「地」、旺强极犯旺/行财——干支混见亦凶，勿以流年论）
        items.any { it.element in ctx.daXiongEls } -> JinBiaoVerdict.XIONG
        // 中平档：解围局官/食、化煞生印未合局
        items.any { it.pingOverride || it.element in ctx.zhongPingEls } -> JinBiaoVerdict.PING
        // 吉档：全帮扶，或顺泄/化煞合局之作吉覆写（作吉元素视同帮扶）
        // 案一百一十七戒：干多不如支重——用神无根无力、承载不了，帮用之运亦仅温饱（吉档降中平）
        items.all { it.stance == JinBiaoStance.BANG || it.favorOverride || it.element in ctx.favorEls } ->
            if (ctx.wenBao117) JinBiaoVerdict.PING else JinBiaoVerdict.JI
        // 案九十五戒：干泄耗而支/干得「发财之运」覆写（用神库根／用神透干）——作分论不作凶
        items.any { it.favorOverride && it.action.contains("发财之运") } -> JinBiaoVerdict.GAN_ZHI_FEN
        // 案五十二分档（限伤官用劫档——用神为比劫）：克比劫＝出事大凶；纯泄耗比劫（毕竟没有被克）＝差而没什么大事，中平
        // 案五十六戒：用神被三会化无力——泄耗亦穷困潦倒（不作中平降档）
        items.all { it.stance == JinBiaoStance.SUN && it.element !in ctx.favorEls } ->
            if (ctx.yongJie && !ctx.yongWuLi && items.none { it.action.contains("克用神") }) JinBiaoVerdict.PING else JinBiaoVerdict.XIONG
        else -> JinBiaoVerdict.GAN_ZHI_FEN
    }

    /**
     * 案一百一十一·印旺身旺而劫贴克财（财生出官之结构）：坐下即印且印党力＞官杀党力（印贴身生）
     * ＋劫干贴邻财干（其间无食伤透干通关）＋财藏根≥3（乙木通四根——劫克不尽，多少能生官）。
     */
    private fun jieKeCai111(bazi: BaziResult, yinEl: Element, biEl: Element, caiEl: Element, guanS: Double): Boolean {
        if (bazi.dayPillar.branch.element != yinEl || partyScore(bazi, yinEl) <= guanS) return false
        val caiIdxs = bazi.stems.indices.filter { it != 2 && bazi.stems[it].element == caiEl }
        val biIdxs = bazi.stems.indices.filter { it != 2 && bazi.stems[it].element == biEl }
        val shiIdxs = bazi.stems.indices.filter { it != 2 && bazi.stems[it].element == bazi.dayPillar.stem.element.generates }
        if (caiIdxs.isEmpty() || biIdxs.isEmpty()) return false
        val tieKe = caiIdxs.any { ci ->
            biIdxs.any { k ->
                kotlin.math.abs(ci - k) == 1 &&
                    shiIdxs.none { si -> si in (minOf(ci, k) + 1) until maxOf(ci, k) }
            }
        }
        if (!tieKe) return false
        // 案一百一十二戒：藏财之支被冲拔（两丑未冲拔乙根）者不计——根拔则克尽，非「克不尽」
        return bazi.branches.count { br ->
            branchHiddenStems.getValue(br).any { it.element == caiEl } &&
                (LIU_CHONG[br]?.let { it !in bazi.branches } ?: true)
        } >= 3
    }

    /** 各案取用依据文案的共用上下文（自 analyze 抽出——方法体量上限）。 */
    private class CtxB(
        val bazi: BaziResult,
        val dayMaster: HeavenlyStem,
        val monthBranch: EarthlyBranch,
        val luZhi: EarthlyBranch,
        val yinEl: Element,
        val biEl: Element,
        val shiEl: Element,
        val caiEl: Element,
        val guanEl: Element,
    )

    /** 案一百一十三·食伤泄过为病·要印补身——取用依据。 */
    private fun guoXie113Basis(c: CtxB): String = buildString {
        val ju = SAN_HE_XU.first { it.second == c.shiEl }
        val zhongShen = ju.first[1]
        val chongZhi = LIU_CHONG[zhongShen]!!
        val chuanZhi = LIU_HAI[zhongShen]!!
        val keBingEl = Element.entries.first { it.controls == c.shiEl }
        val genZhis = c.bazi.branches.filter { br -> br.element == c.biEl }.joinToString("") { it.chinese }
        append("**食伤泄过为病·要印补身（案一百一十三）**：${c.guanEl.chinese}在月令当令——${c.guanEl.chinese}旺；${c.shiEl.chinese}也旺，地支${ju.first.joinToString("") { it.chinese }}有个${c.shiEl.chinese}局。日主就一个通根${genZhis}（${if (c.bazi.dayPillar.branch == c.luZhi) "自坐禄旺" else "本气根"}），也不算弱极点；经通根一系列的作用关系——日主是身弱的")
        append("。**这时候就不喜欢${c.shiEl.chinese}局了**：同样官杀来克我，**身强才可以${c.shiEl.chinese}制之以威**（有根有力量）；食伤是泄我的——**${ju.first.joinToString("") { it.chinese }}泄过了**（如果没有这个${c.shiEl.chinese}局，只有一个${ju.first[0].chinese}${ju.first[0].element.chinese}和${ju.first[1].chinese}${ju.first[1].element.chinese}就没问题——制杀以威是没有问题的；有了就稍微有点过了，**克泄交加**）")
        append("。**反以泄日主为病——就要来${c.yinEl.chinese}印补我日主之气**（克${c.shiEl.chinese}、化${c.guanEl.chinese}、生身）：这局的最终目的就是益我日主之气，就要${c.yinEl.chinese}来；原局${c.yinEl.chinese}不现，以虚神论（运岁补入之时为应）")
        append("。**量·辨证论**：分析问题一定要考虑这个量——吃泻药治便秘，泻药吃多了要拉肚子（中医的辨证治疗）：五行生克与中医是一套东西，都讲辨证——配药不靠五行神煞、全凭力量")
        append("。运：「**暗冲暗会尤为喜，彼冲我兮皆冲起**」——${zhongShen.chinese}把我泄过了是病了；**支神只以冲为重**：运支暗冲病支（${chongZhi.chinese}冲${zhongShen.chinese}——暗冲就是在地支暗处冲）＝去病之药力量最大，**发福最大**；${keBingEl.chinese}克病支${zhongShen.chinese}补我日主之气——有效，不如暗冲；穿／泄病支（${chuanZhi.chinese}穿${zhongShen.chinese}之属）——同样是吃药，药跟药有区别（力最弱）")
    }

    /** 案一百一十六·羊刃自旺承载食伤生财——取用依据。 */
    private fun yangRen116Basis(c: CtxB): String = buildString {
        val ren = c.bazi.dayPillar.branch
        val chong = LIU_CHONG[ren]!!
        val guanTou = c.bazi.stems.indices.first { it != 2 && c.bazi.stems[it].element == c.guanEl }
        val guanGan = c.bazi.stems[guanTou]
        val luWei = c.bazi.branches.indexOf(c.luZhi)
        // 财之三合局（土财无局）：会成财局而运支泄刃者（巳酉丑之丑——火生土）
        val caiJu = SAN_HE_XU.firstOrNull { it.second == c.caiEl }?.first
        val que = caiJu?.firstOrNull { it !in c.bazi.branches }?.takeIf { it.element == ren.element.generates }
        // 日主三合局之生支（寅午戌之寅——印）：有此则冲刃不拔
        val yinZhi116 = SAN_HE_XU.firstOrNull { it.second == c.biEl }?.first?.firstOrNull { it.element == c.yinEl }
        append("**羊刃自旺·承载食伤生财（案一百一十六）**：${c.dayMaster.chinese}自坐${ren.chinese}（羊刃），还配一个${c.luZhi.chinese}——**身旺，足以承载${c.shiEl.chinese}生${c.caiEl.chinese}**（首先肯定是好命有钱人）；然日主比${c.shiEl.chinese}${c.caiEl.chinese}${c.guanEl.chinese}（三者合力）**稍微还是有点偏弱**（旺中带弱）。**${guanGan.chinese}${c.guanEl.chinese}露在${c.luZhi.chinese}上面，就是个隐患是个病**（禄被官盖——不可恃）；命局五行没有${c.yinEl.chinese}来生身——**即使身旺也怕${c.caiEl.chinese}星党${c.guanEl.chinese}杀来克我**（亦忌讳此）。取用：身旺财旺——用${c.caiEl.chinese}（${c.shiEl.chinese}生${c.caiEl.chinese}）；${c.guanEl.chinese}为病不为用（对勘案廿二：杀通根得气、上无盖——官透即用官）")
        append("。运（旺中变弱之应）：**「旺者冲衰衰者拔，衰神冲旺旺神发」**——${chong.chinese}运：${guanGan.chinese}${c.guanEl.chinese}得根把${PillarBranchLabels[luWei]}${c.luZhi.chinese}（禄）克下去、${chong.chinese}${ren.chinese}冲——**${ren.chinese}被拔了没根就完了，就承载不了了**（这局全靠${ren.chinese}帮身——${c.luZhi.chinese}上面有个${c.guanEl.chinese}病）；干头${c.yinEl.chinese}生我是没错，然${c.caiEl.chinese}旺克${c.yinEl.chinese}——先不谈，**关系大在${ren.chinese}被拔**（干支异途）。**${c.guanEl.chinese}杀来把我的根拔了——那是跟官方扯上关系的：蹲大牢**；要是能出得来，走${c.yinEl.chinese}${c.biEl.chinese}运肯定是大财（坏运过去转而就是好运——同样蹲过大牢，出来也是不一样的）" +
            (que?.let { q -> "。${q.chinese}运也不好：${caiJu!!.filter { it != q }.joinToString("") { it.chinese }}${q.chinese}化${c.caiEl.chinese}（${c.caiEl.chinese}局），而且${q.chinese}泄／穿${ren.chinese}——**反正都是湿土晦火**" } ?: "") +
            "。**要是来${c.yinEl.chinese}运就是好运**" +
            (yinZhi116?.let { y -> "——要是有个${c.yinEl.chinese}还可以单论：**地支有${y.chinese}，来${chong.chinese}都没事**" } ?: ""))
    }

    /** 案一百一十四·身旺用暗处的财——取用依据。 */
    private fun anCai114Basis(c: CtxB): String = buildString {
        val bingZhi = c.bazi.dayPillar.branch
        val shiZhi = c.bazi.branches.first { it.element == c.shiEl }
        val chongZhi = LIU_CHONG[bingZhi]!!
        val heZhi = LIU_HE[bingZhi]!!
        val caiTrio = SAN_HE_XU.firstOrNull { it.second == c.caiEl }
        val gong = caiTrio?.first?.filter { it in c.bazi.branches }
        val que = caiTrio?.first?.firstOrNull { it !in c.bazi.branches }
        append("**身旺用暗处的财（案一百一十四）**：${c.monthBranch.chinese}虽当令，整个局${c.biEl.chinese}${c.yinEl.chinese}一片——**身旺**。身旺忌讳的是${c.yinEl.chinese}印；喜用${c.shiEl.chinese}食伤、${c.caiEl.chinese}财、${c.guanEl.chinese}官杀——**哪个好用用哪个**：①${c.guanEl.chinese}杀露着而坐${c.shiEl.chinese}（杀坐${c.shiEl.chinese}上），${c.biEl.chinese}${c.yinEl.chinese}一片——**杀也不行**（干头没${c.shiEl.chinese}都是${c.biEl.chinese}：杀不旺肯定不行——露者受克）；②${c.shiEl.chinese}食伤被${c.yinEl.chinese}所克（${bingZhi.chinese}克${shiZhi.chinese}）——不能为用；③${c.caiEl.chinese}财藏在${shiZhi.chinese}里" +
            (if (gong != null && gong.size >= 2 && que != null) "（${gong.joinToString("") { it.chinese }}拱${que.chinese}）" else "") +
            "——**没被破坏**。**故最终用暗处的财**（就算没有${c.yinEl.chinese}克${c.shiEl.chinese}，也用暗处财——藏者无伤）；**用财，喜${c.shiEl.chinese}**（${c.shiEl.chinese}生${c.caiEl.chinese}——没有${shiZhi.chinese}，这财也不行）。**${c.yinEl.chinese}是忌讳的**")
        append("。运：「**暗冲暗会尤为喜**」——${chongZhi.chinese}运：暗冲去${bingZhi.chinese}之病（${chongZhi.chinese}${bingZhi.chinese}冲）" +
            (if (que != null && que == chongZhi) "＋暗会${c.caiEl.chinese}局" else "") +
            "——**本来不去病也发，现在再加上去病就更发**；${heZhi.chinese}运：合${bingZhi.chinese}化${c.biEl.chinese}——**拱${c.biEl.chinese}旺，不行（离婚）**")
    }

    /** 案一百一十二·喜用两伤明细：冲神、被冲之支、财根被拔后所余之支。 */
    private class LiangShang112(
        val chongShen: EarthlyBranch,
        val beiChong: EarthlyBranch,
        val shengCaiGen: List<EarthlyBranch>,
        /** 被冲之支在局个数（≥2 即「两丑未冲」） */
        val chongN: Int,
    ) {
        /** 「两丑未冲」之数词（一组则不书） */
        val chongShu: String get() = if (chongN >= 2) "两" else ""
    }

    /**
     * 案一百一十二·用官局喜用两伤：官干透干而同柱坐印本气支（丁坐丑——丑泄丁）；
     * 冲对之中被冲之支藏官藏财（未中丁乙）、冲神藏干克官克财（丑中癸辛）——官根被冲克、财根被冲拔
     * （两丑未冲——隔位亦论：师未提隔位，径以拔根断）；财根拔后所剩不过一处（辰中乙只值 10 分）。
     */
    private fun xiYongLiangShang112(bazi: BaziResult, yinEl: Element, caiEl: Element, guanEl: Element): LiangShang112? {
        val guanZuoYin = bazi.stems.indices.any { i ->
            i != 2 && bazi.stems[i].element == guanEl && bazi.branches[i].element == yinEl
        }
        if (!guanZuoYin) return null
        val keGuan = Element.entries.first { it.controls == guanEl }
        val keCai = Element.entries.first { it.controls == caiEl }
        val chongShen = bazi.branches.firstOrNull { br ->
            val p = LIU_CHONG[br]
            p != null && p in bazi.branches &&
                branchHiddenStems.getValue(br).let { h -> h.any { it.element == keGuan } && h.any { it.element == keCai } } &&
                branchHiddenStems.getValue(p).let { h -> h.any { it.element == guanEl } && h.any { it.element == caiEl } }
        } ?: return null
        val beiChong = LIU_CHONG[chongShen]!!
        val caiGenZhis = bazi.branches.filter { br -> branchHiddenStems.getValue(br).any { it.element == caiEl } }
        val sheng = caiGenZhis.filter { br -> LIU_CHONG[br]?.let { it in bazi.branches } != true }
        if (hasBenQiRoot(bazi, caiEl) || sheng.size == caiGenZhis.size || sheng.size > 1) return null
        return LiangShang112(chongShen, beiChong, sheng.distinct(), bazi.branches.count { it == beiChong })
    }

    /** 案一百一十七·干多不如支重·财旺不载——取用依据（身弱无印仍取比劫，然比劫虚透无根——用神无力）。 */
    private fun ganDuo117Basis(c: CtxB): String = buildString {
        val nBi = c.bazi.stems.indices.count { it != 2 && c.bazi.stems[it].element == c.biEl }
        val caiZhi = c.bazi.branches.filter { it.element == c.caiEl }.joinToString("") { it.chinese }
        append("**干多不如支重·财旺不载（案一百一十七）**：天干${nBi + 1}个${c.biEl.chinese}（日主外比劫透${nBi}）而地支全无${c.biEl.chinese}根——**${c.dayMaster.chinese}${c.biEl.chinese}叫地支不载**：干多无用。")
        append("${c.caiEl.chinese}财当令而得局（${caiZhi}）——**力量相差悬殊，命主承载不了这么旺的财**：事业不能有成、经济条件不能好；即使能结婚也肯定**晚婚**，结了也是**克妻离婚**；**不能有儿子**；**不能有财富——即使走好运了也就是个温饱**（纵走印运好运生了儿子，好运过去儿子也得没了、或离婚儿子跟对方走了）。")
        append("身弱无印——仍只有比劫可取（案五十二），然比劫虚透无根：**用神无力**——帮不起来。**${nBi + 1}个${c.biEl.chinese}不能从财**（比劫透干多——「老想着克这个财」：真这么大的钱，而命主没这个本事和能力，只能过得差——**不但没有能力，还非要往上撞**）；**反事实：去掉两个${c.biEl.chinese}（日主外只剩一个比劫）就从财了——好命，走的一直是财运，只要不帮身就有层次**（从者气静平和——心态不一样）。**只要会对比力量——以不变应万遍**")
    }

    /** 伤官用劫（案五十二）／干多不如支重·财旺不载（案一百一十七）——取用依据（自 analyze 抽出：方法体量上限）。 */
    private fun shangYongJieBasis(c: CtxB, shiS: Double, yinS: Double, biGenBeiHua: Boolean, ganDuoBuZai117: Boolean): String = buildString {
        val bazi = c.bazi
        val dayEl = c.dayMaster.element
        val biEl = c.biEl
        val shiEl = c.shiEl
        val yinEl = c.yinEl
        val caiEl = c.caiEl
        val guanEl = c.guanEl
        if (ganDuoBuZai117) {
            append(ganDuo117Basis(c))
        } else {
                                    append("伤官用劫·一定用比劫（案五十二）：日主过泄（食伤党力${format(shiS)}）而**没有印星**（印绶党力${format(yinS)}——藏支之印不算有印，配印须印透干或印有本气根）——用神一定就是比劫。")
                                    append("**比劫夺财是好事**（身弱用劫不考虑财：比劫离财近夺得更干净、离得远夺不着也没事，因为能帮我的身）；**只要比劫好了我就好，比劫坏了我就坏**——八字好坏完全围绕比劫用神来说事。")
                                    append("**最怕见官**：官克比劫——「伤官见官为祸百端」皆日主衰弱用比劫帮身、见官则比劫受克所以有祸（若局中有印，见官不但无祸反而有福——官的双作用：一者生印、二者克比劫：**用印见官是好事，用比劫见官就是为祸百端**）。")
                                    append("运：一路印比帮比劫皆好运（印运帮比劫兼克食伤＝**伤官配印之效**，双用）——用神得助，可以承载食伤生财，**可以做官**；食伤运泄比劫差但没什么大事（毕竟没有被克）；官杀运克用神出事；以食伤制官更不可（身弱不能伤制官＝克泄交加，此时最忌官）")
                                    if (biGenBeiHua) {
                                        val (bs, el) = SAN_HUI_JU.first { (b, _) -> b.all { it in bazi.branches } }
                                        append("；**三会化劫（案五十六）**：地支${bs.joinToString("") { it.chinese }}三会${el.chinese}局——**把比劫都化了**（日主本来坐个根可以，这么一化就身弱了）；劫被化**用神无力——八字格局就不高**：走火土一路帮比劫可**小富**；走食伤运财运（本来就身弱还要食伤生财）**穷困潦倒——即使财再旺也不管用，财越旺他越穷、食伤越旺他越穷**（身弱不任财）；**最怕七杀**克无力之比劫——**死亡**")
                                    }
                                    append("；**财富值取决于用神比劫之力**（${biEl.chinese}有多旺就多大财、有多衰就多衰的财运；做功之神〔如木克土工程〕的力量与财富值一点儿关系都没有——同样做功，有的挣一千万有的挣一万：**用神好财才是真的好**）")
                                    // 案六十八·去火存金＋身弱财大负债论
                                    if (dayEl == Element.FIRE && bazi.branches.any { it == EarthlyBranch.HAI } &&
                                        bazi.branches.any { it == EarthlyBranch.SI } &&
                                        (EarthlyBranch.CHOU in bazi.branches || EarthlyBranch.YOU in bazi.branches)
                                    ) {
                                        append("；**亥巳逢冲＝去火存金**（水把火冲去、金独存——火根转金根）＋巳丑拱金／巳酉化金——**金旺财得局：旺中变弱（身弱财旺、财杀得局）**。**身弱财大＝负债论：财星有多旺，就要负多大的债**（财星弱点还行——贪欲小平台小，欠一千两千一万两万；财星特别大——数额巨大的负债）——财星生杀攻身，在财上出事，不能有钱、要负债")
                                    }
        }
    }

    /**
     * 印得局（地支成局）：印本气支≥3（地支一五行得局）或三合／三会印局支全。
     * 案一百一十八·枭神夺食之判据——师断：「走金运一定会出灾，因为印多，印得局了，走泄他的也是不行的。
     * 去掉两午火还没事。这全是午火，来了金就是枭神夺食。」——两印支（一二印枭）＝印轻未得局：走食伤运没事
     * （原基线「印党力≥2 即夺」据此改校）。
     * 案一百一十九·印旺用财之判据（亥亥子子／亥子丑三会皆得局）。
     */
    private fun yinDeJuShi(bazi: BaziResult, yinEl: Element): Boolean =
        bazi.branches.count { it.element == yinEl } >= 3 ||
            SAN_HE_XU.any { (trio, el) -> el == yinEl && trio.all { it in bazi.branches } } ||
            SAN_HUI_JU.any { (trio, el) -> el == yinEl && trio.all { it in bazi.branches } }

    /**
     * 案一百一十八·旺强极印得局而原局食伤全无——取用依据附注（自 analyze 抽出：方法体量上限；不适用返回空串）。
     * 「命主身旺，全憋着了，无处发泄。只能说这人刚猛」「比如走好运己未，一样是没儿子的。
     * 因为我太旺了，憋着生不出来的。」
     */
    private fun bieZhe118(c: CtxB, yinDeJuDuo: Boolean): String {
        if (!yinDeJuDuo || partyScore(c.bazi, c.shiEl) != 0.0) return ""
        val yinZhis = c.bazi.branches.filter { it.element == c.yinEl }.joinToString("") { it.chinese }
        val yinZhi = c.bazi.branches.first { it.element == c.yinEl }
        return "。**印多，地支${yinZhis}全是${c.yinEl.chinese}——走泄他的也是不行的（案一百一十八：「因为印多，印得局了」）**：来了${c.shiEl.chinese}就是枭神夺食——**走${c.shiEl.chinese}运一定会出灾**（反事实：去掉两个${yinZhi.chinese}——印减至两支、地支不成局则没事）。" +
            "命主身旺**全憋着了、无处发泄**——只能说这人**刚猛**（别人都怕我，只能说我厉害）；**走好运（${c.biEl.chinese}运）一样没儿子——太旺了憋着生不出来**"
    }

    /** 案一百一十八·直断（同上判据；不适用返回空串）：没儿子（走好运亦然）；走金运一定会出灾。 */
    private fun duanYu118(c: CtxB, yinDeJuDuo: Boolean): String {
        if (!yinDeJuDuo || partyScore(c.bazi, c.shiEl) != 0.0) return ""
        return "直断：**没儿子——身旺全憋着、无处发泄**（走好运〔${c.biEl.chinese}运〕一样没儿子：太旺了憋着生不出来）；" +
            "**走${c.shiEl.chinese}运一定会出灾**（印多、地支成局——来了${c.shiEl.chinese}就是枭神夺食）。"
    }

    /** 财坏印（殿后）·案一百一十五／案一百零三——取用依据（自 analyze 抽出：方法体量上限）。 */
    private fun huaiYinDianHouBasis(
        c: CtxB,
        caiS: Double,
        yinS: Double,
        caiZai: Boolean,
        anCaiWuGen115: Boolean,
        geJuZu103: Boolean,
        sanHuiSanLiuHeNote: List<String>,
    ): String = buildString {
        val bazi = c.bazi
        val dayMaster = c.dayMaster
        val monthBranch = c.monthBranch
        val yinEl = c.yinEl
        val biEl = c.biEl
        val shiEl = c.shiEl
        val caiEl = c.caiEl
        val guanEl = c.guanEl
        append("正格五理·印可配官煞（身旺印坏）：印党力 ${format(yinS)} 起坏作用而官食俱不可用——必用财星坏印（财党力 ${format(caiS)}，${if (caiZai) "地支有载" else if (anCaiWuGen115) "透而无根" else "双透得气"}）。")
        append("身足够旺——**喜财坏印，财好大家都好**（财官就是机会：身板够硬就差机会）；财富值取决于用神财之力（案六十一）")
    // 案一百一十五·身旺印旺用财坏印而财无根：藏根之支向背不从我（巳跟木火一伙）；
    // 去寡只论官杀不论财（「用之为财不可劫」玄机在「用」字）；运：申暗冲暗会发大财＞酉
    if (anCaiWuGen115) {
        val keCai = Element.entries.first { it.controls == caiEl }
        val shengKeCai = Element.entries.first { it.generates == keCai }
        val xiangBei = bazi.branches.filter { br ->
            br.element == keCai && branchHiddenStems.getValue(br).any { it.element == caiEl }
        }.distinct()
        val yinZhis = bazi.branches.filter { it.element == yinEl }.distinct()
        val chongZhis = yinZhis.map { LIU_CHONG[it]!! }.distinct()
        val zhu = chongZhis.maxByOrNull { z -> bazi.branches.count { LIU_CHONG[it] == z } }!!
        val zhuBing = bazi.branches.filter { LIU_CHONG[it] == zhu }.joinToString("") { it.chinese }
        val anHui = bazi.branches.firstOrNull { LIU_HE[it] == zhu }
        val ci = chongZhis.filter { it != zhu }.joinToString("、") { it.chinese }
        append("。**身旺印旺，用财来坏印（案一百一十五）**：天干透${bazi.stems.count { it != dayMaster && it.element == caiEl }}个${caiEl.chinese}财但没根——" +
            (xiangBei.takeIf { it.isNotEmpty() }?.let { xs ->
                "${xs.joinToString("、") { it.chinese }}中虽有${caiEl.chinese}，然地支${shengKeCai.chinese}${keCai.chinese}两旺、${keCai.chinese}当令、还有${shengKeCai.chinese}生${keCai.chinese}：**${xs.joinToString("、") { it.chinese }}还是跟${shengKeCai.chinese}${keCai.chinese}一伙了**（${xs.joinToString("、") { it.chinese }}是根没错——${shengKeCai.chinese}${keCai.chinese}旺还能说${xs.joinToString("、") { it.chinese }}是${caiEl.chinese}的根吗？**根之向背**：得分析跟谁是一伙的）；"
            } ?: "") +
            "**去寡只论官杀——没有说财还要去寡的**（除非不用财）：此命用财，有比劫就不行了（比劫见财必破财）——「**用之为财不可劫**」，玄机在于一个「用」字。运：「**暗冲暗会尤为喜，彼冲我兮皆冲起**」——${zhu.chinese}运暗冲病支${zhuBing}（去印旺之病）" +
            (anHui?.let { "＋暗会${it.chinese}（${it.chinese}${zhu.chinese}合）" } ?: "") +
            "——**把病去了发大财**" +
            (if (ci.isNotEmpty()) "；${ci}运也能发财，不如${zhu.chinese}" else "") +
            "；走${caiEl.chinese}得根之运发财——没暗冲也能得财，不大。**暗冲暗会定发财大小**（有的命来好运如枯苗得雨、有的发财费劲——差在有没有暗冲暗会）")
    }
        // 案一百零三·印旺得局用财（调候让位为加分项）
        if (geJuZu103) {
            append("。**印旺得局用财·暖为锦上添花（案一百零三）**：${monthBranch.chinese}月${yinEl.chinese}印得局（${if (sanHuiSanLiuHeNote.isNotEmpty()) "三会" else "会局"}）肯定印旺——印旺要用的配置就是**财来坏印**；无${guanEl.chinese}官可用，${caiEl.chinese}财**通两根有力还是能克${yinEl.chinese}的——只能用财：财星有力就是个好命**。**寒木向阳——锦上添花**：${shiEl.chinese}生${caiEl.chinese}（财有源头）兼暖局——**有火当局长、无火（午换戌）也当正科：半级之差——不是没有火就不行**；**暖不是命局的命根子**（喜欢暖但不依赖暖——命局不依赖暖、土有力自立；对勘案六三「一切以寒暖为规范」——那是命局全靠调候之局）。**哪有去暖的？没听说过的东西不要自己搞开发——问题的关键点在于用神${caiEl.chinese}**")
            append("。**木运之辨（护卫链）**：不怕木运——**火护卫${caiEl.chinese}**（木生火缓解）＋${caiEl.chinese}实两根经克（来木被克一下也不要紧，况且有${shiEl.chinese}生他）；然木运肯定不如火土之运；**若${caiEl.chinese}虚（无实根）走木运就完了**——**丙己最起码得有一个有强根**（都虚则壬寅运〔天干${yinEl.chinese}克${shiEl.chinese}＋地支寅亥化木〕就完了——没有强根，他俩配合也白搭）；**午火主要还是${caiEl.chinese}财之通根**（火之禄更是财之根——一身二任）")
        }
    }

    /**
     * 案一百一十九·印旺用财而火土虚透无根——取用依据。
     * 师断：「地支四个印没有金，不是昆仑之水。取用取什么呀？要维持平衡嘛，无非就用财来克印。
     * 有个火生，但是这火土还是虚透，没有根。」
     */
    private fun yinWangCai119Basis(c: CtxB): String = buildString {
        val nYin = c.bazi.branches.count { it.element == c.yinEl }
        val ht = "${c.shiEl.chinese}${c.caiEl.chinese}"
        val haiZi = EarthlyBranch.HAI in c.bazi.branches && EarthlyBranch.ZI in c.bazi.branches
        val caiXu = !hasBenQiRoot(c.bazi, c.caiEl)
        append("**印旺用财·维持平衡（案一百一十九）**：地支${nYin}个${c.yinEl.chinese}印、没有${c.guanEl.chinese}——**不是昆仑之水**（昆仑须地支得局再配生源）；${c.yinEl.chinese}印得局、印旺——**要维持平衡，无非就用${c.caiEl.chinese}财来克印**（**除特殊格局〔气象格、从格成势——从旺从弱从杀从财〕外，任何八字都讲究平衡**；走${c.biEl.chinese}运也是生扶太过——就又不平衡了）。有个${c.shiEl.chinese}生（${c.shiEl.chinese}生${c.caiEl.chinese}），")
        append(
            if (caiXu) "然${ht}还是**虚透没有根——用神无力**"
            else "然地支没有${c.shiEl.chinese}本气根——${c.caiEl.chinese}纵有根亦无${c.shiEl.chinese}护卫：来${c.biEl.chinese}还是一样不行",
        )
        append("；**没有${c.guanEl.chinese}就是好事**——${c.shiEl.chinese}与${c.caiEl.chinese}可用。运：")
        append(
            if (caiXu) "**走${ht}之运肯定能小康**（${c.shiEl.chinese}生${c.caiEl.chinese}、${c.caiEl.chinese}克${c.yinEl.chinese}——就平衡了，虽然说这个用神无力）"
            else "**走${ht}之运好**（${c.shiEl.chinese}生${c.caiEl.chinese}、${c.caiEl.chinese}克${c.yinEl.chinese}——就平衡了）",
        )
        append("；走${c.yinEl.chinese}${c.biEl.chinese}之运把${c.shiEl.chinese}伤了、把${c.caiEl.chinese}也伤了——**那就不行了，就没有作为了**（水木之运肯定是差运）；**单天干${c.biEl.chinese}运**（生${c.shiEl.chinese}兼克${c.caiEl.chinese}）——不能算好运但也不算差，一般的运；**地支走${c.biEl.chinese}**（${c.yinEl.chinese}生${c.biEl.chinese}、${c.biEl.chinese}克${c.caiEl.chinese}）——还不如天干走${c.biEl.chinese}；**湿土（丑）基本上起不到止水的作用**")
        append(
            if (haiZi) "（毕竟有个亥子丑——随三会水局）" else "（${c.yinEl.chinese}旺之局湿土随会局）",
        )
        append("。对勘案一百零三（丙子己亥乙丑壬午）：彼有午火根——地支来木也没事（生午火了）；此命地支没有火——走木运就不行（即使地支换进燥土，来木还是一样不行）——**其实就是印旺用财，怕劫财**——十神之间这样互相配合（之前学什么格局的，不一样都是讲配合嘛）。**没有「去财星之寡」这么一说**（去寡只论官杀——不从）。对照案一百一十五（庚寅辛巳丙寅辛卯）：彼亦财弱，申运扶起三金、暗冲寅木——有暗冲才加分（只申酉没有暗冲也就一般）；此无暗冲——火土运小康")
    }

    /** 案一百一十九·用神无力之辞（财虚透无根／财虽有根而地支无火）。 */
    private fun yongWuLi119(c: CtxB): String =
        if (!hasBenQiRoot(c.bazi, c.caiEl)) "虚透无根——用神无力"
        else "虽不虚而地支无${c.shiEl.chinese}本气根——来${c.biEl.chinese}还是一样不行（印旺用财怕劫财）"

    /** 案一百一十九·印旺用财之运注（火土运小康／水木运差／单天干木运一般／地支木更差／湿土不止水）。[zhiEff]＝运支有效五行。 */
    private fun yunNote119(i: Int, gan: HeavenlyStem, zhi: EarthlyBranch, el: Element, ctx: YunCtx, suiShui: Boolean, zhiEff: Element): String {
        val ht = "${ctx.shiEl.chinese}${ctx.caiEl.chinese}"
        return when {
            suiShui && i == 1 -> "${zhi.chinese}是湿土——基本上起不到止水的作用（毕竟有个亥子丑：随三会水局）——不作${ctx.caiEl.chinese}财之根（案一百一十九）"
            i == 0 && el == ctx.yinEl && zhiEff == ctx.biEl ->
                "${gan.chinese}克${ctx.shiEl.chinese}（把${ctx.shiEl.chinese}伤了）、地支${zhi.chinese}又克${ctx.caiEl.chinese}（把${ctx.caiEl.chinese}也伤了）——**水木运肯定是差运：就没有作为了**（案一百一十九）"
            i == 0 && el == ctx.yinEl -> "${gan.chinese}克${ctx.shiEl.chinese}——把${ctx.shiEl.chinese}伤了；${ctx.yinEl.chinese}生印——又失衡：差运（案一百一十九）"
            i == 1 && el == ctx.yinEl -> "${zhi.chinese}${ctx.yinEl.chinese}生印——印更旺、又失衡：差运（案一百一十九）"
            i == 0 && el == ctx.biEl -> "单天干${ctx.biEl.chinese}运：生${ctx.shiEl.chinese}、克${ctx.caiEl.chinese}——**不能算好运，但也不算差，就是一般的运**（案一百一十九）"
            i == 1 && el == ctx.biEl -> "地支走${ctx.biEl.chinese}：${ctx.yinEl.chinese}生${ctx.biEl.chinese}、${ctx.biEl.chinese}克${ctx.caiEl.chinese}——生扶太过又失衡，**还不如天干走${ctx.biEl.chinese}**（案一百一十九）"
            el == ctx.shiEl -> "${ctx.shiEl.chinese}生${ctx.caiEl.chinese}——财有源头：**走${ht}之运肯定能小康**（虽然说用神无力；案一百一十九）"
            el == ctx.caiEl -> "${ctx.caiEl.chinese}克${ctx.yinEl.chinese}——财来维持平衡：**走${ht}之运肯定能小康**（虽然说用神无力；案一百一十九）"
            el == ctx.guanEl -> "${ctx.guanEl.chinese}生${ctx.yinEl.chinese}助印——没有${ctx.guanEl.chinese}才是好事：差运（案一百一十九）"
            else -> ""
        }
    }

    /** 案一百一十九·清浊判据（用神无力：走火土运小康、水木运没有作为；档位为落地映射）。 */
    private fun qingZhuo119Basis(c: CtxB): String =
        "——**用神${c.caiEl.chinese}财${yongWuLi119(c)}**：走${c.shiEl.chinese}${c.caiEl.chinese}运能小康、走${c.yinEl.chinese}${c.biEl.chinese}运把${c.shiEl.chinese}${c.caiEl.chinese}都伤了——没有作为（病神＝${c.biEl.chinese}劫财——印旺用财怕劫财）。半浊半清——档位为落地映射：师曰「走火土能小康，虽然说这个用神无力」（案一百一十九）"

    /** 案一百一十九·断语（印旺用财而火土虚透无根）。 */
    private fun duanYu119(c: CtxB): String =
        "此为印旺用财之命——${c.yinEl.chinese}印得局印旺，要维持平衡无非用${c.caiEl.chinese}克${c.yinEl.chinese}；${c.shiEl.chinese}${c.caiEl.chinese}${yongWuLi119(c)}：**走${c.shiEl.chinese}${c.caiEl.chinese}之运肯定能小康**；走${c.yinEl.chinese}${c.biEl.chinese}之运把${c.shiEl.chinese}${c.caiEl.chinese}都伤了——没有作为（没有「去财星之寡」这么一说——不从；案一百一十九）。"

    /** 案一百一十九·旺衰注。 */
    private fun qiangNote119(c: CtxB): String =
        "；**印得局印旺——要维持平衡无非用${c.caiEl.chinese}克${c.yinEl.chinese}；${c.shiEl.chinese}${c.caiEl.chinese}${yongWuLi119(c)}（走${c.shiEl.chinese}${c.caiEl.chinese}运小康；案一百一十九）**"

    /**
     * 案一百二十·伤官用劫而印全无、官杀透干（克泄交加）——取用依据。
     * 师断：「癸水有个帮身，带个地支根，地支都是泄的，没有克，若地支是三个土另说。所以他肯定不是从格……
     * 木泄水太过了，必须帮身的，没有印的，肯定用比劫的。」「这种的命基本上都得蹲大牢，不蹲大牢的得死。」
     */
    private fun keXieJiaoJia120Basis(c: CtxB, shiS: Double, yinS: Double): String {
        val guanIdx = c.bazi.stems.indices.first { it != 2 && c.bazi.stems[it].element == c.guanEl }
        val guanGan = c.bazi.stems[guanIdx]
        // 用神（比劫）之根支与食伤支三合／六合（亥卯之属）——根被合泄（「亥卯合局要给木加分」）
        val genHe = c.bazi.branches.indices.firstOrNull { k ->
            val br = c.bazi.branches[k]
            br.element == c.biEl && listOf(k - 1, k + 1).any { j ->
                j in c.bazi.branches.indices && c.bazi.branches[j].element == c.shiEl &&
                    (SAN_HE_XU.any { (trio, _) -> br in trio && c.bazi.branches[j] in trio } ||
                        (LIU_HE[br] == c.bazi.branches[j] && liuHeHuaEl(br, c.bazi.branches[j]) == c.shiEl))
            }
        }
        val heZhu = genHe?.let { "${c.bazi.stems[it].chinese}${c.bazi.branches[it].chinese}" }
        val heZhis = genHe?.let { k -> c.bazi.branches[k].chinese + c.bazi.branches.first { it.element == c.shiEl }.chinese }
        val geMu = c.bazi.stems.indices.firstOrNull { it == 1 && c.bazi.stems[it].element == c.shiEl }?.let { c.bazi.stems[it].chinese }
        return "伤官用劫·不从格·找平衡（案一百二十）：日主过泄（食伤党力${format(shiS)}——${c.shiEl.chinese}泄${c.biEl.chinese}太过）而**没有印星**（印绶党力${format(yinS)}——五行没有${c.yinEl.chinese}）——用神一定就是比劫。" +
            "**不从格之由**：${c.dayMaster.chinese}有个帮身、带个地支根；地支都是${c.shiEl.chinese}泄的、没有克（若地支是三个${c.guanEl.chinese}另说）——**肯定不是从格**，按正常的分析——要找平衡：哪儿不平衡了？**${c.shiEl.chinese}泄${c.biEl.chinese}太过了，必须帮身；没有印——肯定用比劫**（只要比劫好了我就好，比劫坏了我就坏；用比劫见官——官克比劫，伤官见官为祸百端）。" +
            "**直断：这种的命基本上都得蹲大牢，不蹲大牢的得死**（跟女命犯伤官人是一样的）：泄体太过了，五行没有${c.yinEl.chinese}——得找把这食伤克了：**克一下${c.shiEl.chinese}才能平衡，得要${c.yinEl.chinese}克**（原局无${c.yinEl.chinese}——印运补入）；还有个${guanGan.chinese}${c.guanEl.chinese}——**命局克泄交加**；**没有印，富裕不了**。" +
            "时柱之辨" + (if (heZhu != null && heZhis != null) "：${heZhu}——**${heZhis}合局要给${c.shiEl.chinese}加分（用神之根被合泄）**；换作不与此合的比劫专位（壬子之属），就要比${heZhu}强" else "") +
            "；月令若是${c.biEl.chinese}旺之地（壬子之属）——那就泄秀、是好命。" +
            "${guanGan.chinese}${c.dayMaster.chinese}离得远（中间隔着${geMu ?: c.shiEl.chinese}）——不能以合化论（合化章另授；化不化也不影响这局：总之${c.shiEl.chinese}${c.caiEl.chinese}旺、身衰）。只要不是特殊格局——就是一个平衡俩字儿"
    }


    /** 案一百二十·清枯判据（伤官用劫而印全无、官杀透干——克泄交加；档位为落地映射）。 */
    private fun qingKu120Basis(c: CtxB): String =
        "——**伤官用劫而印全无、${c.guanEl.chinese}又透干——克泄交加（泄体太过）**：用神${c.biEl.chinese}（比劫）被${c.shiEl.chinese}泄、被${c.guanEl.chinese}克，无印解救——直断蹲大牢／死；**没有印，富裕不了——来${c.biEl.chinese}运也仅仅是温饱**。清枯——档位为落地映射：师曰「没有印，富裕不了」「来水运也仅仅是温饱」（案一百二十）"

    /** 案一百二十·断语清枯句（替通句——通句所举「东方木运」之例不切此局）。 */
    private fun duanYuQingKu120(c: CtxB): String =
        "此为清枯之命——伤官用劫而印全无、${c.guanEl.chinese}又透干（克泄交加）：没有印，富裕不了——比劫好运亦仅温饱；透官之柱再来则克泄交加、大祸乃至死亡（案一百二十）。"

    /** 案一百二十·直断。 */
    private fun zhiDuan120(c: CtxB): String =
        "直断：**这种的命基本上都得蹲大牢，不蹲大牢的得死**（泄体太过、无印而${c.guanEl.chinese}透——克泄交加，与女犯伤官同论）；**没有印，富裕不了**——来${c.biEl.chinese}运亦仅温饱；得${c.yinEl.chinese}克${c.shiEl.chinese}方能平衡（案一百二十）。"

    /** 案一百二十·旺衰注（不从格之由）。 */
    private fun buCong120(c: CtxB): String =
        "；**有帮身、带地支根（地支皆${c.shiEl.chinese}泄而无克）——肯定不是从格**；${c.shiEl.chinese}泄${c.biEl.chinese}太过——找平衡：无印必用比劫（案一百二十）"

    /** 案一百二十·运注（比劫帮用仅温饱／印克食伤方平衡／透官之柱再来——死亡／官杀克比劫差运／食伤助泄）。 */
    private fun yunNote120(gan: HeavenlyStem, zhi: EarthlyBranch, el: Element, ctx: YunCtx, biYun: Boolean, guanYun: Boolean, isGuanZhu: Boolean): String = when {
        biYun -> "帮用——然用神之根被${ctx.shiEl.chinese}合泄、原局无${ctx.yinEl.chinese}：**来${ctx.biEl.chinese}运也仅仅是温饱**（没有印，富裕不了；案一百二十）"
        el == ctx.yinEl -> "${ctx.yinEl.chinese}克${ctx.shiEl.chinese}——**克一下${ctx.shiEl.chinese}才能平衡（得要${ctx.yinEl.chinese}克）**：伤官配印（案一百二十）"
        isGuanZhu -> "${gan.chinese}${zhi.chinese}运克泄交加——**死亡**（讲死亡的单说：知道${gan.chinese}${zhi.chinese}运肯定是差运就行了；这种命不蹲大牢就得死；案一百二十）"
        guanYun -> "${ctx.guanEl.chinese}克${ctx.biEl.chinese}比劫用神——**克泄交加：肯定是差运**（这种命不蹲大牢就得死；案一百二十）"
        el == ctx.shiEl -> "助${ctx.shiEl.chinese}之泄——泄${ctx.biEl.chinese}更过（差；案一百二十）"
        else -> ""
    }


    /**
     * 案一百二十一·身旺用官——平衡与情份——取用依据附注。
     * 师断：「是身旺？还是财官旺？俩都旺……你不要去想到底是哪个力量大，他都有力量」「只要日主身有力，
     * 他就可以承载财官的。那么他就一定要用官用财，官没有问题首先要用官的」「这己土是可以护卫丁火的」。
     */
    private fun qingFen121Basis(c: CtxB): String {
        val shiZhi = c.bazi.branches.first { it.element == c.shiEl }
        val caiZhi = c.bazi.branches.first { it.element == c.caiEl }
        val yinGan = c.bazi.stems.indices.first { it != 2 && c.bazi.stems[it].element == c.yinEl }
        val guanGan = c.bazi.stems.indices.first { it != 2 && c.bazi.stems[it].element == c.guanEl }
        val riZhu = "${c.bazi.dayPillar.stem.chinese}${c.bazi.dayPillar.branch.chinese}"
        val shiZhu = "${c.bazi.hourPillar.stem.chinese}${c.bazi.hourPillar.branch.chinese}"
        val kuiGang = riZhu == "庚申" && shiZhu == "庚辰"
        return "；**身旺用官·平衡与情份（案一百二十一）**：是身旺还是财官旺？**俩都旺——都有力量，不要纠结到底哪个力量大**（想这个就走偏路、进死胡同了）。先看地支：${shiZhi.chinese}生${caiZhi.chinese}、${caiZhi.chinese}当令——${c.caiEl.chinese}肯定旺；${c.bazi.branches.first { it.element == c.yinEl }.chinese}生${c.biEl.chinese}" +
            (if (kuiGang) "（${riZhu}、${shiZhu}——一个专旺的、一个是魁罡）" else "") + "——${c.biEl.chinese}肯定旺；虽${c.biEl.chinese}旺克${c.caiEl.chinese}，然${c.caiEl.chinese}当令又有源头（${c.shiEl.chinese}生之）——**即使被克了也有平衡**。**只要日主身有力，就可以承载财官——一定要用官用财；官没有问题首先要用官**（当官儿与发财之间一般都选当官——**贵**）。用${c.guanEl.chinese}：${c.guanEl.chinese}有${c.caiEl.chinese}生——虽${c.biEl.chinese}克${c.caiEl.chinese}，然${c.caiEl.chinese}有源头，官的源头没有问题；**${c.bazi.stems[yinGan].chinese}${c.yinEl.chinese}可以护卫${c.bazi.stems[guanGan].chinese}${c.guanEl.chinese}**。" +
            "帮身的是一伙、克耗泄的是一伙——两者平衡就行。**${c.yinEl.chinese}印在这儿没有用——身旺不需要${c.yinEl.chinese}生身——叫「闲神」**：平时不起作用，当${c.shiEl.chinese}来（伤官克官）时，${c.bazi.stems[yinGan].chinese}${c.yinEl.chinese}挡住护卫——**走什么都不怕，一生平安**（反事实：若无${c.bazi.stems[yinGan].chinese}${c.yinEl.chinese}——命局也平衡，然走${c.shiEl.chinese}运命主得犯官灾：肯定打官司有事儿或者犯错误）。" +
            "**平衡之外还有情份**：${c.shiEl.chinese}（${shiZhi.chinese}）本是泄身制官之物（用官不喜欢食伤），在此反而生我官的源头（${shiZhi.chinese}生${caiZhi.chinese}）——成好东西了，**都算友情**（小人都要变成好人、不得不顺着他做事儿）。对勘案九十五（辛亥辛丑己酉丁卯）：彼日元过泄——${c.shiEl.chinese}生${c.caiEl.chinese}却离得太远起不了太大作用、${caiZhi.chinese}又被冲克——不平衡，日主衰必用${c.yinEl.chinese}（戌丁两运十年发财、学历不行）。**只要差不多平衡、身强能承载财官就没问题——记住平衡俩字儿**"
    }

    /** 案一百二十一·运注（平衡之局走什么都不怕：闲神护卫／伤官被挡／比劫克财有源）。 */
    private fun yunNote121(el: Element, ctx: YunCtx, gan: HeavenlyStem, zhi: EarthlyBranch): String {
        val zi = if (el == gan.element) gan.chinese else zhi.chinese
        return when (el) {
            ctx.yinEl -> "${zi}${ctx.yinEl.chinese}——身旺不需${ctx.yinEl.chinese}生身：**闲神**（${ctx.shiEl.chinese}来伤官克官时，${ctx.yinEl.chinese}挡住护卫${ctx.guanEl.chinese}）——没事（案一百二十一）"
            ctx.shiEl -> "${zi}${ctx.shiEl.chinese}伤官克${ctx.guanEl.chinese}官——**有${ctx.yinEl.chinese}闲神挡住护卫（来水有${ctx.yinEl.chinese}给${ctx.guanEl.chinese}护卫）**：没事（反事实：无${ctx.yinEl.chinese}则此运犯官灾；案一百二十一）"
            ctx.biEl -> "${zi}${ctx.biEl.chinese}克${ctx.caiEl.chinese}——${ctx.caiEl.chinese}当令有源头（${ctx.shiEl.chinese}生之），即使被克了也有平衡：没事（案一百二十一）"
            else -> "平衡之局——走什么都不怕（案一百二十一）"
        }
    }

    /** 案一百二十一·清浊附注。 */
    private fun qingZhuo121Note(c: CtxB): String =
        "——**平衡＋情份（案一百二十一）**：帮身一伙、克耗泄一伙，两者平衡；${c.shiEl.chinese}本泄身制官，反生${c.guanEl.chinese}之源头——友情；${c.yinEl.chinese}为闲神——${c.shiEl.chinese}来克${c.guanEl.chinese}时挡住护卫：**走什么都不怕，一生平安**"

    /** 案一百二十一·断语附注。 */
    private fun duanYu121(c: CtxB): String =
        "**此命局是平衡的——走什么都不怕，一生平安**：来${c.shiEl.chinese}（伤官克${c.guanEl.chinese}）有${c.yinEl.chinese}给${c.guanEl.chinese}护卫——${c.yinEl.chinese}印是闲神（身旺不需生身，${c.shiEl.chinese}来时立功）；若没有这${c.yinEl.chinese}护卫——走${c.shiEl.chinese}运命主得犯官灾。身旺与财官旺不纠结——首用${c.guanEl.chinese}（贵）。（案一百二十一）"

    /** 案一百二十一·旺衰注。 */
    private fun qiangNote121(c: CtxB): String =
        "；**身旺与${c.caiEl.chinese}${c.guanEl.chinese}旺不纠结——皆有力量；日主身有力，足以承载${c.caiEl.chinese}${c.guanEl.chinese}（案一百二十一）**"

    /** 财滋弱杀（案廿二／廿五／八／九十二／一百一十一／一百一十二／一百二十一）——取用依据（自 analyze 抽出：方法体量上限）。 */
    private fun peiCaiBasis(
        c: CtxB,
        strength: DayStrength,
        ruoZhongBianWang: Boolean,
        yinS: Double,
        caiS: Double,
        shiS: Double,
        guanS: Double,
        caiZhuBing: Boolean,
        liangShang112: LiangShang112?,
        qingFen121: Boolean,
        yinBiDeJu122: Boolean,
        bingDa122: Boolean,
    ): String = buildString {
        val cb = c
        val bazi = c.bazi
        val dayMaster = c.dayMaster
        val monthBranch = c.monthBranch
        val luZhi = c.luZhi
        val yinEl = c.yinEl
        val biEl = c.biEl
        val shiEl = c.shiEl
        val caiEl = c.caiEl
        val guanEl = c.guanEl
        val biS = partyScore(bazi, biEl)
        val zuoYinWang111 = bazi.dayPillar.branch.element == yinEl && yinS > guanS
        // 案一百一十一·印旺身旺：日主无本气根而坐下印、印党力压官杀——印贴身生（非「通根得气」）
        val yinWang111 = zuoYinWang111 && !hasBenQiRoot(bazi, biEl)
        append("财滋弱杀格（案廿二）：身旺（${when {
            yinBiDeJu122 -> "${yinEl.chinese}${biEl.chinese}得局——虽${caiEl.chinese}当令，然${biEl.chinese}得局（${c.monthBranch.chinese}月亦不论）"
            ruoZhongBianWang -> "弱中变旺——坐贴身印比、月令敌党之支被日支泄克转衰"
            yinWang111 -> "印旺——${monthBranch.chinese}月坐${bazi.dayPillar.branch.chinese}印、印贴身生（印党力${format(yinS)}）"
            else -> "土金两旺通根得气"
        }}），官杀露而弱（党力${format(guanS)}）${if (shiS < FOLLOW_MIN_SCORE) "、无食伤" else ""}——有财有官杀优先用杀，命主用官")
        append("；财滋弱杀与财滋弱官不区分，是一样的（①身强②用官杀③财生）；财配置（党力${format(caiS)}）${when {
            // 案一百二十二·病太大——勿言「大运解病一样富贵」
            bingDa122 -> "——原局${biEl.chinese}得局克${caiEl.chinese}（官之源被克尽）：**病太大**——来${shiEl.chinese}亦只凑合，勿言大运解病一样富贵（案一百二十二）"
            caiZhuBing -> "——原局${biEl.chinese}克${caiEl.chinese}财被劫，然杀弱无关系、财弱也无妨：只要大运解病（如${shiEl.chinese}运泄${biEl.chinese}生${caiEl.chinese}通关）一样发富发贵，勿见杀弱财弱断贫"
            else -> "——官星指着财来生，有财就是好格局"
        }}")
        // 案九十二·印局化官用官：月令印当令＋比劫通根（秋水通源）——官气尽泄于印
        // 官弱被食伤克＝病——非去寡（官支在）非食制（官弱不任制）：必须用官
        if (bazi.monthPillar.branch.element == yinEl && hasBenQiRoot(bazi, biEl) && shiS >= FOLLOW_MIN_SCORE) {
            append("；**印局化官·必须用官（案九十二）**：月令${yinEl.chinese}当令化官生印＋比劫通根（秋水通源）——官之气尽泄于印转弱：**不是去官星之寡（地支官支在——没法去寡）也非食制（官弱不任制——除非强众敌寡身旺官食并见才可食去官寡）——必须得用官**。然用神${guanEl.chinese}官衰被${shiEl.chinese}食伤克＝病：**没有学历**（虽有食伤秀气然用神是官——犯官读书不好）、**财富值也低**——层次低（做功取象须先按用神定层次再取象：用神衰即往低层次看——木克土非工地即杂役之属）。**婚姻（劫财见财）**：财绝而壬戌自合比劫克财＋秋水通源比劫很强——基本要离婚；官星看比劫护财然官弱（印泄）＋走金水运化官（官星被泄管不住比劫）——**必须离婚**（官强旺则不管走什么运也离不了）；官＝**看着妻子的力度**（查手机管教之属——官星力量大小＝看得严不严／威严之有无——官弱管得松、运一化官干脆不管——比劫随时夺走）")
        }
        if (biS >= 6.0 && EarthlyBranch.SI in bazi.branches && guanEl == Element.FIRE) {
            append("；地支${biEl.chinese}旺——巳随${biEl.chinese}走（作${biEl.chinese}根不作火根），${guanEl.chinese}杀真弱而通寅根：杀有根气故不能去杀（去杀须杀无一丝根气），必须财滋弱杀、禁制杀（制杀贫穷欠债）")
        }
        append("；杀强反不宜（要克身）；走帮官杀之运好运，走加重${biEl.chinese}病之运差")
        // 案四十一：官杀弱全指财滋——怕劫克财、必护财；官干合去劫干（丙辛合）
        // ＝合去病神、解放财（有情）——南方火运双功：助官＋火克金卫护财（当官）
        val jieGai = bazi.stems.filterIndexed { i, s -> i != 2 && s.element == biEl }.any { bg ->
            val p = GAN_HE[bg]
            p != null && p.element == guanEl && bazi.stems.any { it == p }
        }
        if (jieGai) {
            append("；官杀弱全指${caiEl.chinese}财滋养——**怕劫克财、必护财**（护住喜神方能助起用神）。官杀干合去劫干（如丙辛合）＝**合去病神、解放${caiEl.chinese}财——有情**（地支之克另论：只要有去劫这个动作就叫有情）；被合之杀去而用留下之官。南方${guanEl.chinese}运双功：官杀得助＋${guanEl.chinese}克${biEl.chinese}卫护${caiEl.chinese}财——喜神卫、用神助，**当官**".replace("**", ""))
        }
        // 案卅八：官杀两透本赖混杂加力，比肩合去其一——不宜合（官愈弱）；幸被合之干坐强根不化，金水运补起
        if (bazi.stems.count { it.element == guanEl } >= 2 &&
            bazi.stems.filterIndexed { i, s -> i != 2 && s.element == biEl }.any { bg ->
                val p = GAN_HE[bg]
                p != null && p.element == guanEl && bazi.stems.any { it == p }
            }
        ) {
            append("；官杀两透本赖混杂加力（官弱喜混杂），比肩合去其一——不宜合（愈合官愈弱）；幸被合之干坐强根（壬坐申金旺）合而不化（若化${yinEl.chinese}成印即无情）——走一路金水之运把官杀补起来亦发")
        }
        // 案一百一十二·用官局喜用两伤（先于案一百一十一「财生出官」——财根被冲拔则克尽）
        val ls112 = liangShang112
        if (strength == DayStrength.STRONG && ls112 != null) {
            val guanZuo112 = bazi.stems.indices.first { it != 2 && bazi.stems[it].element == guanEl }
            append("；**喜用两伤（案一百一十二）**：同样用${guanEl.chinese}官——${guanEl.chinese}坐${bazi.branches[guanZuo112].chinese}被${yinEl.chinese}泄（这是一方面，主要在冲）；**${ls112.chongShu}${ls112.chongShen.chinese}${ls112.beiChong.chinese}冲，${caiEl.chinese}的根就拔了**（上一个〔案一百一十一〕${biEl.chinese}克${caiEl.chinese}是克不尽的——此则${ls112.beiChong.chinese}中${caiEl.chinese}之根被${ls112.chongShen.chinese}冲拔）；${ls112.chongShen.chinese}${ls112.beiChong.chinese}冲**克${guanEl.chinese}**（${ls112.beiChong.chinese}中${guanEl.chinese}之根被冲克）——用神被泄被克、喜神被伤没：**喜用两伤就完了**。${ls112.shengCaiGen.joinToString("") { it.chinese }}中虽有一点${caiEl.chinese}——**只值 10 分**（没有是 0 分、有是 10 分，还是个差生）。**直断：克妻损子**（财是妻子——劫刃肆逞，离婚；身旺财为子——财不行找官，官也不行，没儿子）、**学习肯定不行**（劫财见财的人念书一定笨——笨不笨取决于官星：官代表主见、思路；此局官也不行）")
        } else if (jieKeCai111(bazi, yinEl, biEl, caiEl, guanS)) {
            // 案一百一十一·印旺身旺·财生出官：劫干贴克财干而用神是官——就财论无子，然财生出官有子
            val caiGenN111 = bazi.branches.count { br ->
                branchHiddenStems.getValue(br).any { it.element == caiEl } &&
                    (LIU_CHONG[br]?.let { it !in bazi.branches } ?: true)
            }
            val guanZuoShi111 = bazi.stems.indices.any { i ->
                i != 2 && bazi.stems[i].element == guanEl && bazi.branches[i].element == shiEl
            }
            append("；**印旺身旺·财生出官（案一百一十一）**：劫财${biEl.chinese}透干贴克${caiEl.chinese}财——**就财论是没儿子的**（儿子不取决于财了）；然**螳螂捕蝉黄雀在后——财生出官来了**：**用神是官、不是用财**，劫克财克不尽（${caiEl.chinese}通${caiGenN111}根），多少能生这个${guanEl.chinese}官——**不是说克一下就没了**；官不贴着劫也无问题（**挨着层次更高**）" +
                (if (guanZuoShi111 && bazi.stems.none { it != dayMaster && it.element == shiEl })
                    "。**这局不用食伤**：${shiEl.chinese}被旺${yinEl.chinese}克着没力量、又跟${caiEl.chinese}一伙且润${yinEl.chinese}养${caiEl.chinese}——官坐其上（自合）不碍官"
                    else "") +
                "。身旺用食伤、用财、用官，**无非就是克耗泄我，让我达到平衡就能生儿子**——有儿子、事业还好（**当官**）")
        }
        // 案一百二十一·身旺用官：财官皆旺不纠结——首用官（贵）；印为闲神护卫；平衡＋情份——走什么都不怕
        if (qingFen121) append(qingFen121Basis(cb))
        // 案一百二十二·身强用官而病太大：官之源被得局之金克尽、源头水不现
        if (bingDa122) append(bingDa122Basis(c))
    }

    /**
     * 案一百二十二·身强用官而病太大——取用依据附注。
     * 师断：「卯木全让金给克了，卯木不平衡，那不平衡就不行」「甲木也地支不载，起不到实质性作用」
     * 「所以要找地支有没有水……没水那就不行呗」「补救又如何？金得局了，已经得了绝症了」。
     */
    private fun bingDa122Basis(c: CtxB): String {
        val biZhis = c.bazi.branches.filter { it.element == c.biEl }.joinToString("") { it.chinese }
        val yinZhis = c.bazi.branches.filter { it.element == c.yinEl }.joinToString("") { it.chinese }
        // 官所坐之财支、贴冲它的比劫支
        val guanZuo = c.bazi.stems.indices.first { it != 2 && c.bazi.stems[it].element == c.guanEl }
        val caiZhi = c.bazi.branches[guanZuo]
        val biChong = c.bazi.branches.first { it.element == c.biEl && LIU_CHONG[it] == caiZhi }
        // 另一透干之财（甲木生丁之属）
        val caiGan = c.bazi.stems.indices.firstOrNull { it != 2 && c.bazi.stems[it].element == c.caiEl && it != guanZuo }?.let { c.bazi.stems[it] }
        // 食伤之三合局（申子辰之属）——局中两支待一
        val shiJu = SAN_HE_XU.firstOrNull { it.second == c.shiEl }?.first
        val gongZhis = shiJu?.filter { it in c.bazi.branches }
        val queZhi = shiJu?.firstOrNull { it !in c.bazi.branches }
        return "；**身强用官而病太大（案一百二十二）**：地支${yinZhis}生${c.biEl.chinese}——${c.biEl.chinese}旺；虽${c.caiEl.chinese}当令，然${c.biEl.chinese}得局（**就算大王也怕四个2**）；${biChong.chinese}贴着${caiZhi.chinese}克，还配上${yinZhis}了。日主身强，得用${c.caiEl.chinese}${c.guanEl.chinese}——${c.guanEl.chinese}官没被伤，**优先用官**；而官就指着一个${caiZhi.chinese}${c.caiEl.chinese}了——**${caiZhi.chinese}${c.caiEl.chinese}全让${c.biEl.chinese}给克了：${c.caiEl.chinese}不平衡，不平衡就不行**。" +
            (caiGan?.let { "还有个${it.chinese}${c.caiEl.chinese}生${c.guanEl.chinese}，然${it.chinese}${c.caiEl.chinese}也**地支不载，起不到实质性作用**（地支${c.biEl.chinese}得局——也是不平衡）。" } ?: "") +
            "**所以要找地支有没有${c.shiEl.chinese}（源头）**——被坏得太严重，**没${c.shiEl.chinese}那就不行**（${yinZhis}中${c.caiEl.chinese}之余气——补救又如何？${c.biEl.chinese}得局已得绝症，吃片药补救不了——还是不平衡）。命局有${c.shiEl.chinese}就能维持平衡；${caiZhi.chinese}虽站月令，${c.caiEl.chinese}来就怕${c.biEl.chinese}——**${caiZhi.chinese}稍剩一小口气儿、能吃口饭**（反事实：${caiZhi.chinese}与${biChong.chinese}换位——${c.caiEl.chinese}就克尽死了）。" +
            (if (gongZhis != null && gongZhis.size == 2 && queZhi != null)
                "**${gongZhis.joinToString("") { it.chinese }}拱${c.shiEl.chinese}之辨**：这里没有${c.shiEl.chinese}，拱不了——就是${yinZhis}生${c.biEl.chinese}（**没有${c.shiEl.chinese}，土肯定要生金的**）；${gongZhis.joinToString("") { it.chinese }}也就稍微拱出一点点、不起作用；要有个${gongZhis.joinToString("") { it.chinese }}${queZhi.chinese}" +
                    (caiGan?.let { "，或者${it.chinese}${c.caiEl.chinese}换成${c.shiEl.chinese}透干引化" } ?: "") + "，就要拱${c.shiEl.chinese}的（**绝不会前后矛盾**——金多没水，土肯定生金）。"
            else "") +
            "**找平衡之法——找源头**：被克了就要找有没有生他的——火被克找木、木被克找水、土被克找火、金被克找土；研究来研究去就是五行之间的配合"
    }

    /** 案一百二十二·清枯判据（身强用官而病太大——好运仅凑合；档位为落地映射）。 */
    private fun qingKu122Basis(c: CtxB): String =
        "——**身强用官而病太大（案一百二十二）**：${c.biEl.chinese}得局，官之源${c.caiEl.chinese}被${c.biEl.chinese}贴冲克伐——${c.caiEl.chinese}不平衡；透干之${c.caiEl.chinese}地支不载、余气补救不了（绝症）；找源头——地支无${c.shiEl.chinese}。**${c.caiEl.chinese}稍剩一小口气儿、能吃口饭：来${c.shiEl.chinese}也只凑合、衣食无忧；${c.biEl.chinese}运彻底完了**。清枯——档位为落地映射：师曰「来水的时候肯定日子过得还凑合，但是也不能好到哪里去」"

    /** 案一百二十二·直断。 */
    private fun zhiDuan122(c: CtxB): String =
        "直断：**病太大——没${c.shiEl.chinese}（源头）那就不行**：官之源${c.caiEl.chinese}稍剩一小口气儿、能吃口饭；大运来${c.shiEl.chinese}一定好——然只过得凑合、衣食无忧（化${c.shiEl.chinese}局挣钱多——相对其层次而言）；**${c.biEl.chinese}运太不平衡——彻底完了**（找平衡＝找源头：${c.caiEl.chinese}被克找${c.shiEl.chinese}；案一百二十二）。"

    /** 案一百二十二·断语清枯句。 */
    private fun duanYuQingKu122(c: CtxB): String =
        "此为清枯之命——身强用官而${c.caiEl.chinese}（官之源）被得局之${c.biEl.chinese}克伐、源头${c.shiEl.chinese}不现：好运（来${c.shiEl.chinese}）仅凑合、衣食无忧；${c.biEl.chinese}运彻底完了（案一百二十二）。"

    /** 案一百二十二·旺衰注（印比得局）。 */
    private fun qiangNote122(c: CtxB): String =
        "；**${c.yinEl.chinese}${c.biEl.chinese}得局——虽${c.caiEl.chinese}当令，然${c.biEl.chinese}得局（「就算大王也怕四个2」）——身强（案一百二十二）**"

    /** 案一百二十二·运注（来水找源头——凑合，化局挣钱多／金运——彻底完了）。[huaJu]＝运支与局支成食伤三合局（申子辰）。 */
    private fun yunNote122(gan: HeavenlyStem, zhi: EarthlyBranch, ctx: YunCtx, chartBranches: List<EarthlyBranch>, jin: Boolean): String {
        val zi = "${gan.chinese}${zhi.chinese}"
        val huaJu = SAN_HE_XU.firstOrNull { it.second == ctx.shiEl }?.first?.let { t -> zhi in t && t.all { it == zhi || it in chartBranches } } == true
        return when {
            jin -> "${zi}——${ctx.biEl.chinese}运太不平衡了：**彻底完了**（${ctx.biEl.chinese}得局再帮——${ctx.caiEl.chinese}更受克；案一百二十二）"
            huaJu -> "运支${zhi.chinese}——${SAN_HE_XU.first { it.second == ctx.shiEl }.first.joinToString("") { it.chinese }}化${ctx.shiEl.chinese}局（填实）：**化${ctx.shiEl.chinese}局了肯定挣钱多**（找源头——${ctx.caiEl.chinese}被克找${ctx.shiEl.chinese}）；然病太大——只是相对其层次低下而言的好（案一百二十二）"
            else -> "${zi}——来${ctx.shiEl.chinese}（找源头：${ctx.caiEl.chinese}被克找${ctx.shiEl.chinese}）：**大运来${ctx.shiEl.chinese}一定好**；然病太大——日子过得还凑合、衣食无忧，也好不到哪里去（案一百二十二）"
        }
    }

    /** 身旺食伤泄秀（案九／五十／一百引／八十四／一百一十／一百六）——取用依据（自 analyze 抽出：方法体量上限）。 */
    private fun shiShengCaiBasis(
        c: CtxB,
        shiS: Double,
        yinS: Double,
        caiS: Double,
        guanS: Double,
        liangRen110: Boolean,
        caiZai: Boolean,
        jiaQuGua125JieGou: Boolean,
    ): String = buildString {
        val bazi = c.bazi
        // 案一百二十五·假去寡·印护官（文案整体替换——印非「格局之病」：无印则犯法、有印则当兵）
        if (jiaQuGua125JieGou) return jiaQuGua125Basis(c)
        val dayMaster = c.dayMaster
        val yinEl = c.yinEl
        val biEl = c.biEl
        val shiEl = c.shiEl
        val caiEl = c.caiEl
        val guanEl = c.guanEl
        val renZhi110 = YANG_REN[dayMaster]
        append("正格续理·身旺食生财（案九）：身旺无官杀必泄体得衡——用神取食伤（党力 ${format(shiS)}）泄刚煞之气")
        // 案一百一十·坐两刃身旺、财印相合、卯木泄秀——子卯刑没用；走财运好；怕酉冲卯；财为闲神非调候
        if (liangRen110) {
            val yongGenZhi110 = bazi.branches.firstOrNull { it.element == shiEl }
            append("。**坐两刃身旺·${shiEl.chinese}泄秀（案一百一十）**：${shiEl.chinese}当令旺，然**日主坐${renZhi110!!.chinese}刃自旺——何况两刃，又露一个飞刃（${biEl.chinese}）——还配了${yinEl.chinese}源头：日主是身旺的**；${caiEl.chinese}贴${yinEl.chinese}相合——**${caiEl.chinese}虽猛，逢${yinEl.chinese}乃怯**（如乙从庚：被合者极弱从了对方即化）——且${caiEl.chinese}坐${bazi.branches[0].chinese}**临绝**（「绝」非长生表之绝，是坐官上：生克论命不按长生表）无根，合${yinEl.chinese}而化；没有此合日主也身旺。**取用${shiEl.chinese}泄秀；${renZhi110!!.chinese}${yongGenZhi110?.chinese ?: ""}刑有用吗？没用——泄秀就好**。**走财运好**：凡是身旺泄秀的走财运就好（食神继续发泄——儿又生儿）；劫财虽制财，然正因有劫我才身旺、有食伤才喜财运——走财运不必考虑劫（**没有食伤走财运，才叫群劫争财**）。**怕走${yongGenZhi110?.let { LIU_CHONG[it]?.chinese } ?: "冲神"}运冲${yongGenZhi110?.chinese ?: shiEl.chinese}**（冲${shiEl.chinese}）：七八十岁走人就死了；年轻走是差运——这么好的八字只能说是不得志（在当官的单位上班，升职不了）。${caiEl.chinese}是**闲神**——平时不起作用，来了忌神才起作用（闲神立功）；**不是调候**（调候须暖有根，没根不算调候——走火运好是财来了，与调候无关）")
        }
        // 案一百零六·合解冲绝处逢生有情：独食根被印贴冲本夭亡——冲神被六合化木（贪合忘冲＋化神生用）
        run {
            val shiGenZhi106 = bazi.branches.filter { it.element == shiEl }
            val chongYin106 = shiGenZhi106.firstOrNull { g -> LIU_CHONG[g] != null && bazi.branches.contains(LIU_CHONG[g]) && LIU_CHONG[g]!!.element == yinEl }
            if (chongYin106 != null && bazi.branches.count { it.element == shiEl } == 1) {
                val chongShen106 = LIU_CHONG[chongYin106]!!
                val hePartner106 = LIU_HE[chongShen106]
                if (hePartner106 != null && hePartner106 in bazi.branches && hePartner106.element == biEl) {
                    append("。**合解冲·绝处逢生有情（案一百零六）**：${guanEl.chinese}杀克我没错然**无力被${yinEl.chinese}化了**（病不大——走${guanEl.chinese}运也不好，走火土运把这小毛病去了更好）——只能选${shiEl.chinese}泄身；然${chongShen106.chinese}贴冲${chongYin106.chinese}——**身强独一位食伤而根被冲＝夭亡的命**。然**${chongShen106.chinese}${hePartner106.chinese}化${hePartner106.element.chinese}绝处逢生：本来${chongShen106.chinese}${yinEl.chinese}无情冲${chongYin106.chinese}，化${hePartner106.element.chinese}了就转而有情**（**合解冲——八字里特殊的一个情况**：贪合忘冲＋化神生${shiEl.chinese}）。**反事实**：${chongYin106.chinese}换午、${chongShen106.chinese}换子——子水也生寅木然**子午逢冲不叫有情——一样夭亡**（通关只从夭亡减到蹲大牢——仍然解决不了；唯${chongShen106.chinese}${hePartner106.chinese}一下合了才叫有情）。**发财大小不在财星旺衰，在有情还是无情两字**——即使小${shiEl.chinese}很弱，绝处逢生了：**孤注一掷就绝处逢生，这人就有层次**。运走土运把印克了——**弃印就财＝三命通会「正官配印，不如乘马」**（马是财——财起好作用就弃印乘马：**财就是有情那就要发财；官有情了当官；用神弱一样发大财——就因为有情**；己未运财开始发的）")
                }
            }
        }
        if (yinS >= YIN_HUAI) {
            append("；印星两坏（生身助旺＋贴身克用神）——印为格局之病，病去或用神强旺方能好")
            if (!caiZai) append("；财地支不载（金旺土生金、支全金局）——用财无出息，不如用食伤（秋水通源，壬水有劲）")
            // 案一百引·弃印就财（学历章）：印旺为病夺食——上学呆学习不好；
            // 财运弃印就财——不念书早出社会就发财（学习也好不了——无学历照样发财）
            append("；**弃印就财·学历断（案一百引）**：印非常旺——上学的时候有点呆、大脑不灵活，学习肯定不好；来了财运**弃印就财**——只能说你不念书早点出社会，就发财了（**学习也好不了**——没有学历一样发财、比上大学挣钱多事业好：弃印就财之命社会上常见——塞翁失马焉知非福）")
        }
        // 案八十四·财气通门户（玄机暗里存）：枭夺食而找财——财不透而半合拱局暗存
        // （亥未拱木之属）——财起好作用即有俩钱：财星有情财来找他——不在乎财星旺衰
        if (yinS >= YIN_HUAI) {
            val gongTrio = listOf(
                setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI) to Element.WOOD,
                setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU) to Element.FIRE,
                setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN) to Element.WATER,
                setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU) to Element.METAL,
            ).firstOrNull { (trio, el) -> el == caiEl && bazi.branches.count { it in trio } >= 2 && !trio.all { it in bazi.branches } }
            if (gongTrio != null) {
                val missingZhi = gongTrio.first.first { it !in bazi.branches }
                append("；**财气通门户（案八十四·玄机暗里存）**：枭夺食找财——财不透而**拱局暗存**（${gongTrio.first.filter { it in bazi.branches }.joinToString("") { it.chinese }}拱${caiEl.chinese}）——**财起好作用即有俩钱：财星有情、财来找他——不在乎财星旺衰，只要有情就叫财气通门户**（干${caiEl.chinese}之行必挣钱——走差运生意也不差〔选对行〕、小康；**运支${missingZhi.chinese}填拱——财局兑现发大财**〔东窗事发之属〕；燥土辨：秋土凉必生金——火多土烫才不生，勿刻舟求剑见未即谓不生）")
            }
        }
        if (yinS >= YIN_HUAI && guanS == 0.0) {
            append("；枭神夺食之局（印${format(yinS)}旺贴克食伤）——好在${caiEl.chinese}财护食制枭（${shiEl.chinese}生${caiEl.chinese}、${caiEl.chinese}克${yinEl.chinese}）＝药，是为好命；最怕官：财生官、官生枭——夺食更凶。伤官不可见官，见官为祸百端（身旺逢枭夺食最不可见官）；亥子丑坎卦克忌神好运，丙寅丁卯官星来即凶。金水伤官／土金伤官口诀皆不成立——一切用生克分析做最后判断（案四十九喜官、案五十忌官，分野即枭夺食有无）")
        }
        append("；财党力 ${format(caiS)}")
        if (caiS < FOLLOW_MIN_SCORE) append("——虽财不现，食伤够旺亦必发泄，也是好命")
    }

    /**
     * 案一百二十三·金有力而基本平衡、水旺克火而火无源——用官喜帮火——取用依据。
     * 师断：「对比一下力量，庚申，辛酉，金有力的……午火当令，基本上平衡，但是火有水克……
     * 不平衡的原因就是有水克火，然后火没有源头……这局就喜帮火了。」
     */
    private fun yongGuan123Basis(c: CtxB): String {
        val nianZhu = "${c.bazi.yearPillar.stem.chinese}${c.bazi.yearPillar.branch.chinese}"
        val riZhu = "${c.bazi.dayPillar.stem.chinese}${c.bazi.dayPillar.branch.chinese}"
        val yueZhi = c.monthBranch
        // 与日支三合化比劫之官杀支（巳酉之属）——「巳酉化金」
        val riZ = c.bazi.dayPillar.branch
        val heZ = c.bazi.branches.firstOrNull { br ->
            br != riZ && br.element == c.guanEl &&
                SAN_HE_XU.any { (t, el) -> el == c.biEl && riZ in t && br in t }
        }
        val ganLie = c.bazi.stems.joinToString("") { it.chinese }
        val touShi = c.bazi.stems.indices.filter { it != 2 && c.bazi.stems[it].element == c.shiEl }.joinToString("") { c.bazi.stems[it].chinese }
        return "**用官喜帮火·基本平衡（案一百二十三）**：对比力量——${nianZhu}、${riZhu}（**年柱一柱专旺，帮身的力量比较大——要记住了**），${c.biEl.chinese}有力的；命局不燥" +
            (heZ?.let { "，${heZ.chinese}${riZ.chinese}化${c.biEl.chinese}（也有${c.guanEl.chinese}气的，只是不克${riZ.chinese}${c.biEl.chinese}——如果局燥${heZ.chinese}就作${c.guanEl.chinese}）" } ?: "") +
            "。${yueZhi.chinese}${c.guanEl.chinese}当令——**基本上平衡**；但${c.guanEl.chinese}有${c.shiEl.chinese}克——${c.shiEl.chinese}通根，天干也是${ganLie}——${c.shiEl.chinese}都有源头的（${c.biEl.chinese}生之），**${c.shiEl.chinese}也旺**：${c.shiEl.chinese}一旺${c.guanEl.chinese}就稍弱——**稍微有点儿不平衡**；**不平衡的原因就是有${c.shiEl.chinese}克${c.guanEl.chinese}，而${c.guanEl.chinese}没有源头**（无${c.caiEl.chinese}）。${c.biEl.chinese}一直生着${c.shiEl.chinese}，${c.shiEl.chinese}越是制${c.guanEl.chinese}、${c.guanEl.chinese}就越小——**这局就喜帮${c.guanEl.chinese}了（命局用${c.guanEl.chinese}）**。" +
            "**命局基本平衡——从八字来说是一个不错的命**；但${c.shiEl.chinese}克${c.guanEl.chinese}：${yueZhi.chinese}${c.guanEl.chinese}没有源头" +
            (heZ?.let { "，而且${heZ.chinese}${c.guanEl.chinese}因${heZ.chinese}${riZ.chinese}合也帮不了${yueZhi.chinese}${c.guanEl.chinese}" } ?: "") +
            "——**这${yueZhi.chinese}${c.guanEl.chinese}就有点孤了**：八字还是稍微有点病的（没有${c.caiEl.chinese}）。层次：就算没走好运也不次，只不过不得志——好比在事业单位有一个很不错的工作：**是好命，但当不上官儿**（不至于像上案那种完全不行的）。" +
            "**不是说官没有源头就不好**——是${yueZhi.chinese}${c.guanEl.chinese}被${touShi}${c.shiEl.chinese}克，这时候没有源头才变得没有力量了（反事实：没有透${c.shiEl.chinese}，这局也是相对平衡——**他肯定是官命**）；要平衡就**必须得有${c.caiEl.chinese}**（补${yueZhi.chinese}${c.guanEl.chinese}之源）。${yueZhi.chinese}${c.guanEl.chinese}当令——不属于去寡（非强众敌寡）；**没有泄${c.biEl.chinese}的道理**（这局本来就${c.shiEl.chinese}旺；泄秀就要制官了——不需要${c.shiEl.chinese}旺制去官）。运之辨：${c.yinEl.chinese}帮${c.biEl.chinese}还是帮${c.guanEl.chinese}？得结合天干看——庚${EarthlyBranch.XU.chinese}一柱则申酉戌会局${c.biEl.chinese}肯定旺；**丙${EarthlyBranch.XU.chinese}——${c.guanEl.chinese}通根，肯定旺**（天干丙也克${c.biEl.chinese}——${c.guanEl.chinese}维持平衡）；壬${EarthlyBranch.XU.chinese}是拱火、喜忧参半，不如丙${EarthlyBranch.XU.chinese}；**${c.caiEl.chinese}运也行、不如丙${EarthlyBranch.XU.chinese}**（命局关键是为了补${yueZhi.chinese}${c.guanEl.chinese}，而不是为了化什么）；**走${c.shiEl.chinese}肯定不好**——走${c.shiEl.chinese}就去${c.guanEl.chinese}了，走${c.shiEl.chinese}就完了；${c.yinEl.chinese}既生${c.biEl.chinese}也泄${c.guanEl.chinese}（**这局${c.guanEl.chinese}不旺**）"
    }

    /** 案一百二十三·清浊附注（基本平衡而稍有病——好命而不得志；档位为落地映射）。 */
    private fun qing123Note(c: CtxB): String =
        "——**基本平衡而稍有病（案一百二十三）**：${c.biEl.chinese}有力、${c.monthBranch.chinese}${c.guanEl.chinese}当令——两停；${c.shiEl.chinese}通根有源克${c.guanEl.chinese}，${c.monthBranch.chinese}${c.guanEl.chinese}无源（无${c.caiEl.chinese}）——**${c.monthBranch.chinese}${c.guanEl.chinese}有点孤**。**好命而不得志**（未行帮${c.guanEl.chinese}之运：好比在事业单位有一份很不错的工作，但当不上官儿）；补${c.guanEl.chinese}（丙${EarthlyBranch.XU.chinese}）、补源（${c.caiEl.chinese}）则贵。清——档位为落地映射：师曰「从八字来说是一个不错的命」"

    /** 案一百二十三·断语清命句（稍有病——替「走什么运都不怕」通句）。 */
    private fun duanYuQing123(c: CtxB): String =
        "此为清命而稍有病之命——${c.monthBranch.chinese}${c.guanEl.chinese}孤（被${c.shiEl.chinese}克、无源）：未行帮${c.guanEl.chinese}之运则好命而不得志；丙${EarthlyBranch.XU.chinese}补${c.guanEl.chinese}则旺（案一百二十三）。"

    /** 案一百二十三·直断。 */
    private fun zhiDuan123(c: CtxB): String =
        "直断：**不错的命——好命而不得志**（好比在事业单位有一份很不错的工作，但当不上官儿——未行帮${c.guanEl.chinese}之运）；病：${c.monthBranch.chinese}${c.guanEl.chinese}被${c.shiEl.chinese}克而无源（无${c.caiEl.chinese}）——**必须得有${c.caiEl.chinese}、喜帮${c.guanEl.chinese}**（不是说官没有源头就不好——是被克而无源才无力）；**丙${EarthlyBranch.XU.chinese}${c.guanEl.chinese}通根——肯定旺**；**走${c.shiEl.chinese}就完了**（${c.monthBranch.chinese}${c.guanEl.chinese}当令不属去寡）；没有泄${c.biEl.chinese}的道理（${c.shiEl.chinese}已旺、非强众敌寡；案一百二十三）。"

    /** 案一百二十三·旺衰注（年柱专旺帮身力大）。 */
    private fun qiangNote123(c: CtxB): String =
        "；**${c.biEl.chinese}有力（${c.bazi.yearPillar.fullName}、${c.bazi.dayPillar.fullName}——年柱一柱专旺，帮身的力量比较大）；${c.monthBranch.chinese}${c.guanEl.chinese}当令——基本上平衡，${c.shiEl.chinese}旺克${c.guanEl.chinese}而${c.guanEl.chinese}无源——稍微有点儿不平衡（案一百二十三）**"

    /** 案一百二十三·运注（丙戌火通根／木运补官之源／走水就完了／土泄火）。 */
    private fun yunNote123(huoTongGen: Boolean, caiYun: Boolean, shuiYun: Boolean, tuYun: Boolean, gan: HeavenlyStem, zhi: EarthlyBranch, ctx: YunCtx, chartBranches: List<EarthlyBranch>): String {
        val yue = chartBranches.getOrElse(1) { EarthlyBranch.WU }
        val zi = "${gan.chinese}${zhi.chinese}"
        return when {
            huoTongGen -> "运支${zhi.chinese}——${ctx.guanEl.chinese}通根（与局支拱${ctx.guanEl.chinese}）：**肯定旺**；天干${gan.chinese}也克${ctx.biEl.chinese}——**${ctx.guanEl.chinese}维持平衡**（${ctx.yinEl.chinese}帮${ctx.biEl.chinese}还是帮${ctx.guanEl.chinese}得结合天干看；案一百二十三）"
            caiYun -> "运干${gan.chinese}${ctx.caiEl.chinese}补${yue.chinese}${ctx.guanEl.chinese}之源（化${ctx.shiEl.chinese}生${ctx.guanEl.chinese}）——**肯定也行，但不如丙${EarthlyBranch.XU.chinese}**（命局关键是为了补${yue.chinese}${ctx.guanEl.chinese}，而不是为了化什么；案一百二十三）"
            shuiYun -> "${zi}——**走${ctx.shiEl.chinese}肯定不好**：命局用${ctx.guanEl.chinese}，走${ctx.shiEl.chinese}就去${ctx.guanEl.chinese}了（${yue.chinese}${ctx.guanEl.chinese}当令不属去寡——非强众敌寡）——**走${ctx.shiEl.chinese}就完了**（案一百二十三）"
            tuYun -> "${zi}——${ctx.yinEl.chinese}既生${ctx.biEl.chinese}也泄${ctx.guanEl.chinese}：**这局${ctx.guanEl.chinese}不旺**（${ctx.yinEl.chinese}运加重不平衡；案一百二十三）"
            else -> ""
        }
    }

    /**
     * 案一百二十四·有力就能食神制杀——病在午火（财生杀旺）、药在子水——取用依据。
     * 师断：「天干露三个金，没有通根，辛未有点力，日主是不旺，但是日主是有点力……有力就能食神制杀，
     * 跟日主虚透无力强制杀是不一样的」「子水就是去火的药，食神制杀了」。
     */
    private fun youLiShiZhi124Basis(c: CtxB): String {
        val nianZhu = c.bazi.yearPillar
        val riZ = c.bazi.dayPillar.branch
        // 去火之药（食伤本气支）与其所冲之官杀本气支（子午之属）
        val yaoZhi = c.bazi.branches.first { it.element == c.shiEl }
        val bingZhi = LIU_CHONG[yaoZhi]!!.takeIf { it in c.bazi.branches }
        val nBi = c.bazi.stems.indices.count { it != 2 && c.bazi.stems[it].element == c.biEl }
        val chou = EarthlyBranch.CHOU.chinese
        val xu = EarthlyBranch.XU.chinese
        return "**有力就能食神制杀·病在${bingZhi?.chinese ?: ""}${c.guanEl.chinese}（案一百二十四）**：年柱${nianZhu.fullName}——${nianZhu.stem.chinese}坐${nianZhu.branch.chinese}${c.yinEl.chinese}不如坐${chou}${c.yinEl.chinese}（湿${c.yinEl.chinese}生${c.biEl.chinese}），但起码是个印——不符合特殊格局的要求：按正常格局分析，**没有力量的要补上，有力量的要抑制**。毛病在哪？**${c.caiEl.chinese}生${c.guanEl.chinese}旺——病在${bingZhi?.chinese ?: ""}${c.guanEl.chinese}（${c.guanEl.chinese}本气支）**：旺了就要想办法解决。" +
            "**${yaoZhi.chinese}${c.shiEl.chinese}就是去${c.guanEl.chinese}的药——食神制杀**（${yaoZhi.chinese}${bingZhi?.chinese ?: ""}冲——去${c.guanEl.chinese}；「去${c.guanEl.chinese}对、存${c.biEl.chinese}不对——局里没有${c.biEl.chinese}」）。天干露${nBi + 1}个${c.biEl.chinese}（含日主）、没有通根，${nianZhu.fullName}有点力——**日主不旺，但日主是有点力**（不考虑杀的情况是有力）：**有力就能食神制杀——跟日主虚透无力强制杀（克泄交加）是不一样的**；日主还是偏弱，**有${chou}${c.yinEl.chinese}来化更好**（若没有${yaoZhi.chinese}${c.shiEl.chinese}，有个${chou}${c.yinEl.chinese}印也行——${chou}${c.yinEl.chinese}泄${c.guanEl.chinese}）。" +
            "**不是克泄交加**：${yaoZhi.chinese}${c.shiEl.chinese}没泄、${c.shiEl.chinese}在此也不旺——就是制杀的。**这个杀有源头**（${c.caiEl.chinese}生之）——就算制了也不会把杀制绝，**还是平衡的**（若没有${c.caiEl.chinese}就是制杀太过了）。年上${nianZhu.branch.chinese}${c.yinEl.chinese}肯定喜欢（帮比劫），**但用神肯定是${yaoZhi.chinese}${c.shiEl.chinese}**——${c.yinEl.chinese}也被${c.caiEl.chinese}克，再者${c.guanEl.chinese}也旺。" +
            "运：**${c.shiEl.chinese}运去病发大财**；${c.yinEl.chinese}干坐${c.shiEl.chinese}支（戊${yaoZhi.chinese}之属）统看也可以——${c.yinEl.chinese}泄${c.guanEl.chinese}生${c.biEl.chinese}、${c.shiEl.chinese}克${bingZhi?.chinese ?: ""}${c.guanEl.chinese}，去病了；**${xu}运（印来了）肯定有大灾——把${c.shiEl.chinese}给克了，而且还拱${c.guanEl.chinese}**；${c.biEl.chinese}运肯定是好运（帮身——冲${c.caiEl.chinese}与否不在问题点上）。**原局的作用方式就是食伤制杀**（最好是食伤制杀、印星化杀）。反事实：${yaoZhi.chinese}换${EarthlyBranch.WEI.chinese}——层次就不高了，也管事；换${chou}——层次就高了。**思考要往问题点上去动**：问题点在于杀太旺了要抑制——没有力量要补，力量过了要抑制"
    }

    /** 案一百二十四·清浊附注。 */
    private fun qing124Note(c: CtxB): String {
        val yaoZhi = c.bazi.branches.first { it.element == c.shiEl }
        val bingZhi = LIU_CHONG[yaoZhi]!!.takeIf { it in c.bazi.branches }
        return "——**病在${bingZhi?.chinese ?: ""}${c.guanEl.chinese}、药在${yaoZhi.chinese}${c.shiEl.chinese}（案一百二十四）**：${c.caiEl.chinese}生${c.guanEl.chinese}旺（病）；${yaoZhi.chinese}${c.shiEl.chinese}冲去${c.guanEl.chinese}——食神制杀（药在局）；杀有源（${c.caiEl.chinese}生）——制而不绝、还是平衡的；日主有点力（年柱${c.bazi.yearPillar.fullName}）——非虚透无力强制。**子水运去病发大财；戌运克药神兼拱火——大灾**"
    }

    /** 案一百二十四·戌运（印来了却克药神、且拱火）——肯定有大灾。 */
    private fun xuNote124(zhi: EarthlyBranch, ctx: YunCtx): String =
        "运支${zhi.chinese}——**印来了，但还是把${ctx.shiEl.chinese}（药神）给克了，而且还拱${ctx.guanEl.chinese}：肯定有大灾**（案一百二十四）"

    /** 案一百二十四·丑运（湿土印化杀——泄火生金）。 */
    private fun chouNote124(gan: HeavenlyStem, zhi: EarthlyBranch, i: Int, ctx: YunCtx): String =
        (if (i == 0) "运干${gan.chinese}${ctx.yinEl.chinese}——与${zhi.chinese}一体，印化${ctx.guanEl.chinese}（层次高）" else "运支${zhi.chinese}——${ctx.yinEl.chinese}印化${ctx.guanEl.chinese}：${zhi.chinese}${ctx.yinEl.chinese}泄${ctx.guanEl.chinese}生${ctx.biEl.chinese}（「有${zhi.chinese}${ctx.yinEl.chinese}来化更好」——层次高）") + "（案一百二十四）"

    /** 案一百二十四·断语清命句（病杀有药）。 */
    private fun duanYuQing124(c: CtxB): String =
        "此为清命而病杀有药之命——病在午火（${c.caiEl.chinese}生${c.guanEl.chinese}旺）、药在子${c.shiEl.chinese}（食神制杀）：${c.shiEl.chinese}运去病发大财；${EarthlyBranch.XU.chinese}运克药神、拱${c.guanEl.chinese}——大灾（案一百二十四）。"

    /** 案一百二十四·直断。 */
    private fun zhiDuan124(c: CtxB): String =
        "直断：**病在午火（${c.caiEl.chinese}生${c.guanEl.chinese}旺）、药在子${c.shiEl.chinese}——食神制杀**（日主有点力——有力就能食神制杀，非虚透无力强制）；杀有源——制而不绝、还是平衡的；**${c.shiEl.chinese}运去病发大财**（戊${EarthlyBranch.ZI.chinese}统看也可以）；**${EarthlyBranch.XU.chinese}运肯定有大灾**（印来了却克${c.shiEl.chinese}、且拱${c.guanEl.chinese}）；${c.biEl.chinese}运肯定是好运（帮身）（案一百二十四）。"

    /** 案一百二十四·旺衰注（日主有点力）。 */
    private fun qiangNote124(c: CtxB): String =
        "；**日主不旺，但日主是有点力**（天干${c.bazi.stems.indices.count { it != 2 && c.bazi.stems[it].element == c.biEl } + 1}个${c.biEl.chinese}（含日主）透而无根，年柱${c.bazi.yearPillar.fullName}有印——不考虑杀则有力）：**有力就能食神制杀**，非虚透无力强制（案一百二十四）**"

    /**
     * 案一百二十四·运注。[yao]=药神（食伤）、[bi]=比劫、[xu]=戌（大灾）、[chou]=丑（化杀）、[yinZuoYao]=印干坐药神支／丑。
     */
    private fun yunNote124(
        gan: HeavenlyStem,
        zhi: EarthlyBranch,
        el: Element,
        ctx: YunCtx,
        chartBranches: List<EarthlyBranch>,
        yao: Boolean,
        bi: Boolean,
        xu: Boolean,
        chou: Boolean,
        yinZuoYao: Boolean,
        i: Int,
    ): String {
        val zi = "${gan.chinese}${zhi.chinese}"
        val yaoZhi = chartBranches.first { it.element == ctx.shiEl }
        val bingZhi = LIU_CHONG[yaoZhi]!!.takeIf { it in chartBranches }
        val bing = (bingZhi?.chinese ?: "") + ctx.guanEl.chinese
        // 戌（大灾）、丑（化杀）先判——按运支支本气论，不随连行方局之覆写
        if (xu) return xuNote124(zhi, ctx)
        if (chou) return chouNote124(gan, zhi, i, ctx)
        return when {
            yao -> "${zi}——**去${ctx.guanEl.chinese}的药（食神制杀）**：${yaoZhi.chinese}${bingZhi?.chinese ?: ""}冲去${bing}；**${ctx.shiEl.chinese}运去病发大财**（不是克泄交加——${ctx.shiEl.chinese}在此不旺、就是制杀的；案一百二十四）"
            bi -> "${zi}——${ctx.biEl.chinese}帮身：**肯定是好运**（问题点在杀太旺要抑制——冲${ctx.caiEl.chinese}与否不在问题点上；案一百二十四）"
            xu -> xuNote124(zhi, ctx)
            chou -> chouNote124(gan, zhi, i, ctx)
            yinZuoYao -> "运干${gan.chinese}${ctx.yinEl.chinese}——泄${ctx.guanEl.chinese}生${ctx.biEl.chinese}（统看也可以：${gan.chinese}${ctx.yinEl.chinese}泄${ctx.guanEl.chinese}生${ctx.biEl.chinese}、${zhi.chinese}${ctx.shiEl.chinese}克${bing}，去病了；案一百二十四）"
            el == ctx.guanEl -> "${zi}——${ctx.caiEl.chinese}生${ctx.guanEl.chinese}旺：病在${ctx.guanEl.chinese}（旺了就要抑制；案一百二十四）"
            el == ctx.yinEl -> "${zi}——年上${ctx.yinEl.chinese}之类肯定喜欢（帮比劫），但${ctx.yinEl.chinese}也被${ctx.caiEl.chinese}克、${ctx.guanEl.chinese}又旺——非用神（案一百二十四）"
            else -> ""
        }
    }

    /** 案一百二十五·本盘（丁日，地支亥戌卯辰）——事实性运断（午运发财、未运没发财、2012 拆迁款）仅限此盘。 */
    private fun isChart125(c: CtxB): Boolean = c.dayMaster == HeavenlyStem.DING && isChart125(c.bazi.branches)

    private fun isChart125(branches: List<EarthlyBranch>): Boolean =
        branches == listOf(EarthlyBranch.HAI, EarthlyBranch.XU, EarthlyBranch.MAO, EarthlyBranch.CHEN)

    /**
     * 案一百二十五·假去寡·印护官——当兵——取用依据。
     * 师断：「日主身旺，这局说去寡也对，用官也对，得看命主走什么大运」「印护官了，当兵」
     * 「要是没有这个甲木印就是犯法」「此命是假去寡，但也叫去寡」。
     */
    private fun jiaQuGua125Basis(c: CtxB): String {
        val yinStems = c.bazi.stems.indices.filter { it != 2 && c.bazi.stems[it].element == c.yinEl }.joinToString("") { c.bazi.stems[it].chinese }
        val guanZhi = c.bazi.branches.first { it.element == c.guanEl }
        val shiZhis = c.bazi.branches.filter { it.element == c.shiEl }.joinToString("") { it.chinese }
        val jinShui = c.caiEl.chinese + c.guanEl.chinese
        val haiZhi = EarthlyBranch.HAI.chinese
        val maoZhi = EarthlyBranch.MAO.chinese
        val weiZhi = EarthlyBranch.WEI.chinese
        val wuZhi = EarthlyBranch.WU.chinese
        val chenZhi = EarthlyBranch.CHEN.chinese
        return "**假去寡·印护官——当兵（案一百二十五）**：**日主身旺（没争议）**——取用克耗泄：**这局说去寡也对，用官也对，得看命主走什么大运**（总之克耗泄：**印护小弱官**、或者**去寡**都行——**用官肯定不如去寡有层次**）。**用神打架**：${yinStems}${c.yinEl.chinese}克${shiZhis}${c.shiEl.chinese}（印制食伤）——${yinStems}${c.yinEl.chinese}确实也是护${guanZhi.chinese}${c.guanEl.chinese}；身旺有官肯定优先以官星为用（**身旺肯定不能伤官制官**）：走${jinShui}运没法去官星之寡，只能用官；用官又有伤官制官本不正确——那肯定得犯官非（伤官见官为祸百端：不老实、犯国家的东西）——**但命局有印护官了：保卫国家了，那就是护官的——印护官了，当兵**（没有什么劫财七杀、羊刃驾杀，一样当兵）。**要是没有这个${yinStems}${c.yinEl.chinese}印就是犯法**；当然这个${guanZhi.chinese}${c.guanEl.chinese}是**独官**，也不能说是犯法（如果再有一${c.guanEl.chinese}支那肯定就是犯法）；但要是没有印，把${c.guanEl.chinese}去尽了也不犯法——去不尽就犯法了。" +
            "**走南方火土运把${c.guanEl.chinese}去尽——去官星之寡（食伤泄秀）**：需${c.biEl.chinese}来通关——**此命是假去寡，但也叫去寡**（表面上看有印不是去寡，实际上还是叫去寡；当走上火运的时候就去寡了——**没有死定义，一定要学活**：走${jinShui}时就得用官，走火土旺时就得用${c.shiEl.chinese}泄秀）。**从八字上来讲肯定浊——不浊就是军官，而不是兵卒**。" +
            (if (isChart125(c)) "运：癸酉壬申（${jinShui}）没有层次——只能用官，用神太弱；**未运没发财**（${haiZhi}${maoZhi}${weiZhi}——${c.yinEl.chinese}旺护官，没法去寡）；**午运发财**（${wuZhi}${c.biEl.chinese}去官星之寡）；**2012（壬${chenZhi}）厂房拆迁拿了拆迁款**——厂房是${c.shiEl.chinese}、发${c.shiEl.chinese}上的财（${c.shiEl.chinese}起了好作用——去官星之寡把${c.guanEl.chinese}去了）。层次：当兵时没有发财、出身也不好、**学历低**（当兵的除上军校的大格局以外，大部分学历低、出生差——兵卒的命）；命主是泄秀时发的财（官太弱）。" else "") +
            "当兵四类对号：**无理之人又配印**（伤官犯官配印）／**护住了国家的东西**（印护官——保卫国家）；口诀（劫财合杀、羊刃驾杀）不作据"
    }

    /** 案一百二十五·清浊附注（浊——兵卒）。 */
    private fun zhuo125Note(c: CtxB): String =
        "——**浊（案一百二十五）**：用神打架（${c.yinEl.chinese}克${c.shiEl.chinese}——印护官，而身旺不可伤官制官）；独官去不尽——**不浊就是军官，而不是兵卒**；走火土运去官星之寡（${c.biEl.chinese}通关）则成泄秀格——假去寡也叫去寡。半浊半清——档位为落地映射：师曰「从八字上来讲，肯定浊」"

    /** 案一百二十五·直断。 */
    private fun zhiDuan125(c: CtxB): String =
        "直断：**印护官——当兵（兵卒之命）**（身旺不可伤官制官：有印护官即保卫国家；若无印则伤官见官为祸百端——犯法）；**浊**——不浊就是军官；**${c.caiEl.chinese}${c.guanEl.chinese}运用官——没有层次；火土运去寡（${c.shiEl.chinese}泄秀）——有层次**（午运发财、2012 厂房拆迁拿了拆迁款）；**假去寡，但也叫去寡**（一个八字两个用神？没有死定义——要学活）（案一百二十五）。"

    /** 案一百二十五·旺衰注。 */
    private fun qiangNote125(c: CtxB): String =
        "；**身旺——没争议：去寡（火土运）／用官（${c.caiEl.chinese}${c.guanEl.chinese}运）两可，得看走什么大运；用神打架（${c.yinEl.chinese}克${c.shiEl.chinese}）需${c.biEl.chinese}通关——假去寡也叫去寡（案一百二十五）**"

    /** 案一百二十五·学历（兵卒之命——学历低、出身差）。 */
    private fun xueLi125Of(): cn.xulun.bazi.model.JinBiaoXueLi =
        cn.xulun.bazi.model.JinBiaoXueLi(
            yinWanHao = true,
            level = cn.xulun.bazi.model.JinBiaoXueLiLevel.BU_GAO,
            basis = "**当兵（兵卒）——学历低、出身差（案一百二十五）**：当兵的除上军校的大格局以外，大部分学历低、出生差——这说的是兵卒的命；此命当兵时没有发财、出身也不好（旺印再生身——上学呆；学习不好不一定不发财——差生也挣钱）。",
        )

    /** 案一百二十五·运注（火土去寡作吉／金水只能用官、印旺没法去寡——中平）。 */
    private fun yunNote125(gan: HeavenlyStem, zhi: EarthlyBranch, i: Int, ctx: YunCtx, chartBranches: List<EarthlyBranch>, huotu: Boolean, mu: Boolean, jinshui: Boolean, zhiMu: Boolean, huiJu: Boolean): String {
        val here = isChart125(chartBranches)
        val zi = "${gan.chinese}${zhi.chinese}"
        val haiZhi = EarthlyBranch.HAI.chinese
        val maoZhi = EarthlyBranch.MAO.chinese
        val weiZhi = EarthlyBranch.WEI.chinese
        val chenZhi = EarthlyBranch.CHEN.chinese
        return when {
            huotu && here && i == 1 && zhi == EarthlyBranch.WU ->
                "运支${zhi.chinese}——**${ctx.biEl.chinese}去官星之寡（火通关）：发财**（走南方的时候去官星之寡发财；案一百二十五）"
            huotu && here && i == 1 && zhi == EarthlyBranch.CHEN ->
                "运支${zhi.chinese}——**${ctx.shiEl.chinese}起了好作用：去官星之寡把${ctx.guanEl.chinese}去了——2012 壬${chenZhi}年厂房拆迁拿了拆迁款**（厂房是${ctx.shiEl.chinese}、发${ctx.shiEl.chinese}上的财；案一百二十五）"
            huotu -> "${zi}——火土运：**去官星之寡（${ctx.biEl.chinese}通关）／${ctx.shiEl.chinese}泄秀——有层次**（走火土旺时就得用${ctx.shiEl.chinese}泄秀；用官不如去寡有层次；案一百二十五）"
            mu && here && i == 1 && zhi == EarthlyBranch.WEI ->
                "运支${zhi.chinese}——${haiZhi}${maoZhi}${weiZhi}、${ctx.yinEl.chinese}旺护官：**没法去寡——没发财**（只能用官、层次低；案一百二十五）"
            mu && huiJu -> "${zi}——运支${zhi.chinese}与局支会成${ctx.yinEl.chinese}局：**${ctx.yinEl.chinese}旺护官，没法去寡**（只能用官、层次低；案一百二十五）"
            mu && zhiMu -> "${zi}——运支${zhi.chinese}为${ctx.yinEl.chinese}（印）：**${ctx.yinEl.chinese}旺护官，没法去寡**（只能用官、层次低；案一百二十五）"
            mu -> "${zi}——${ctx.yinEl.chinese}旺护官：**没法去寡**（只能用官、层次低；案一百二十五）"
            jinshui -> "${zi}——${ctx.caiEl.chinese}${ctx.guanEl.chinese}运：**只能用官**（没法去官星之寡）——用神太弱，**没有层次**（案一百二十五）"
            else -> ""
        }
    }

    /** 清浊判据文案（自 analyze 抽出——方法体量上限）。同名局部别名＝块内零变量替换。 */
    private fun qingZhuoBasisOf(
        cb: CtxB,
        choice: Choice,
        qingZhuo: JinBiaoQingZhuo,
        jianBing: String?,
        bingEl: Element,
        yongEl: Element,
        bingS2: Double,
        yongS2: Double,
        yongDeLi: Boolean,
        yaoEl: Element,
        tongGuanEl: Element,
        yaoS: Double,
        bingHua: Boolean,
        bingYiTU: Boolean,
        bingTongGuanTou: Boolean,
        gongCaiTongMen: Boolean,
        huaJuBuYong: Boolean,
        yongWuLiHua: Boolean,
        anCai114: Boolean,
        liangShang112Pei: LiangShang112?,
        ganDuoBuZai117: Boolean,
        maJin109: Boolean,
        caiGenBa109: Boolean,
        caiGen109: List<EarthlyBranch>,
        shiBanTongGuan: Boolean,
        shiSanHe113: Pair<List<EarthlyBranch>, Element>?,
        yinGenBaCai107: Boolean,
        yinWangCai119: Boolean,
        keGuan: Element,
        keYinEl: Element,
        keXieJiaoJia120: Boolean,
        qingFen121: Boolean,
        shiZhi124: Boolean,
        jiaQuGua125: Boolean,
        bingDa122: Boolean,
        yongGuan123: Boolean,
        anYin126: Boolean,
        yingKang127: Boolean,
    ): String = buildString {
        val bazi = cb.bazi
        val biEl = cb.biEl
        val caiEl = cb.caiEl
        val guanEl = cb.guanEl
        val monthBranch = cb.monthBranch
        val shiEl = cb.shiEl
        val yinEl = cb.yinEl
        append("病神＝${bingEl.chinese}（克${yongEl.chinese}用神，党力${format(bingS2)}）；用神党力${format(yongS2)}${if (yongDeLi) "（得力）" else "（不得力）"}")
        if (jianBing != null) append("；$jianBing")
        when (qingZhuo) {
            JinBiaoQingZhuo.QING_KU -> append(
                // 案一百二十·伤官用劫而印全无、官杀透干——克泄交加（档位为落地映射）
                if (bingDa122) qingKu122Basis(cb)
                else if (keXieJiaoJia120) qingKu120Basis(cb)
                else if (ganDuoBuZai117)
                    "——**干多不如支重·财旺不载**：天干${bazi.stems.count { it.element == biEl }}个${biEl.chinese}而地支全无${biEl.chinese}根——用神${biEl.chinese}（比劫）无根虚浮、无力；${caiEl.chinese}财当令得局——**力量相差悬殊，承载不了**：无财富，**走好运也仅温饱**（档位为落地映射——师只说「过得差」「也就是个温饱」；案一百一十七）"
                else if (liangShang112Pei != null)
                    "——用官局**喜用两伤**：用神${guanEl.chinese}被${yinEl.chinese}泄、又被${liangShang112Pei.chongShen.chinese}${liangShang112Pei.beiChong.chinese}冲克；喜神${caiEl.chinese}之根被冲拔——只余${liangShang112Pei.shengCaiGen.joinToString("、") { it.chinese }}中一点（**只值 10 分——不过一点余气，不能说${caiEl.chinese}旺**）：一点救没有——**喜用两伤就完了**（直断克妻损子；档位为落地映射——师只说「就完了」；案一百一十二）"
                else if (maJin109 && caiGenBa109 && choice.way == JinBiaoQuYongWay.HUAI_YIN)
                    "——印过旺用${caiEl.chinese}破印，然${caiEl.chinese}仅藏于${caiGen109.joinToString("、") { it.chinese }}、根气被两组贴冲砍伐殆尽（藏干互战暗相坎伐）——用神无着：**财都没了，就完了**（直断克妻无子——原局上人基本就完了）；**辟「冲开财库必发」**：「财官临库喜刑冲」不可以这么用——刻舟求剑；原局坏绝，好运也只是多要一个馒头（案一百零九）"
                else if (choice.way == JinBiaoQuYongWay.FU_SHEN)
                    "——用神${yongEl.chinese}（印）被得局之${caiEl.chinese}财坏（财有多旺他就有多穷——食伤生财＝给忌神添粮）：病大难救——**清枯之命**：印运把印补起来还能温饱，食伤生财之运身无分文、负债"
                else "——用神${yongEl.chinese}（印）虽可取（帮身扛杀），然生印之${guanEl.chinese}过旺蒸之（火炎土焦，燥土难以生金——生金有点难，但也只能用土）、又被${keYinEl.chinese}克（党力${format(partyScore(bazi, keYinEl))}有根）——病太大，一点救都没有：**清枯之命**，走好运只得温饱，走克用生忌之运一事无成（贫穷清枯）".replace("**", ""),
            )
            JinBiaoQingZhuo.QUAN_ZHUO -> append("——官（${guanEl.chinese}）被${keGuan.chinese}克且财源被断、食（${shiEl.chinese}）被印克无本气根、财（${caiEl.chinese}）被比劫冲克——三者俱废，用神无着落：全浊，一事无成（学什么都不会、干什么都有人捣乱），比普通老百姓还差")
            JinBiaoQingZhuo.QING -> append(
                // 案一百二十三·基本平衡而稍有病——好命而不得志（档位为落地映射）
                if (yongGuan123) qing123Note(cb)
                else if (bingS2 == 0.0) "——原局无病神，用神有力无扰，此为清（贵命，走什么运都不怕）" +
                    (if (choice.way == JinBiaoQuYongWay.KUN_LUN) "**——然昆仑顺局无病而**层次完全由大运决定**：顺则贵（生源／本势／顺泄）、逆则贫（止水／逆局——来土就完了）；主要看中年运**（案九十七）**" else "")
                else if (choice.way == JinBiaoQuYongWay.YI_ZHI_YI_HUA) "——**一制一化有情：有情就是有格局（不虑旺衰——${shiEl.chinese}制${guanEl.chinese}、${yinEl.chinese}化杀相得）**；${shiEl.chinese}弱无妨——在于情分；若制化两干贴着（相合互绊）——就没层次了（案九十九）"
                else if (bingHua) "——病神不透干，经${tongGuanEl.chinese}通关转而相生有情（${bingEl.chinese}生${tongGuanEl.chinese}生${yongEl.chinese}），病不为病——此为清（贵命，走什么运都不怕）"
                else if (bingYiTU) "——病神透干而无本气根、用神坐日支本气根——干支异途（干不克支），病不贴克用神：有格局（坐下印在），行运不违背即好命（用印显贵多，当官）"
                else if (gongCaiTongMen) "——**财气通门户（玄机暗里存）**：病神（枭印）虽大而**拱财暗存**（药待运填）——**财星有情不在乎旺衰：财来找他**——走差运生意也不差（选对行）小康；运填拱局——**发大财**（有钱人没走好运：好运一发就是大的——幸福的人还有三十年好日子）"
                else if (bingTongGuanTou && huaJuBuYong) "——**破格→有情救格**：${yongEl.chinese}用神被${bingEl.chinese}重重围克本已破格，无${guanEl.chinese}官护财；然${shiEl.chinese}通关化劫＋六合化局补用神（绝处逢生）——**有情就有格局**：水木运助起用神发大财；土金旺运（克${shiEl.chinese}生${bingEl.chinese}）则破"
                else "——病神被药${yaoEl.chinese}所制（药力${format(yaoS)}），此为清（贵命）",
            )
            JinBiaoQingZhuo.MAN_ZHUO -> append(
                if (yongWuLiHua) "——**用神之根被三会局化尽——用神无力，八字格局就不高**：本来就身弱还要食伤生财——穷困潦倒（即使财再旺也不管用：**财越旺他越穷、食伤越旺他越穷**——身弱不任财）；唯走火土一路帮比劫可小富；最忌七杀克比劫——死亡"
                else "——病神本气得根、力量太大（≥用神1.5倍）毁坏用神，满盘浊气——差命穷命，走什么运也好不了",
            )
            JinBiaoQingZhuo.BAN_QING -> append(
                // 案一百一十九·印旺用财而火土虚透无根——用神无力：走火土运小康、水木运没有作为（档位为落地映射）
                if (yinWangCai119) qingZhuo119Basis(cb)
                else if (choice.way == JinBiaoQuYongWay.GUO_XIE_QIU_YIN && shiSanHe113 != null)
                    "——**${shiEl.chinese}局（${shiSanHe113.first.joinToString("") { it.chinese }}）泄日主过甚为病**（有了就稍微过了——克泄交加）：反以泄日主为病、要${yinEl.chinese}印补身（原局不现以虚神论）；病在而待药——运支暗冲病支（${LIU_CHONG[shiSanHe113.first[1]]!!.chinese}冲${shiSanHe113.first[1].chinese}）去病**发福最大**（暗冲暗会尤为喜）、克病支次之、穿泄病支力最弱：帮用去病之运好、助病之运差（档位为落地映射——师未断原局清浊；案一百一十三）"
                else if (yinGenBaCai107) "——**伤官配印而印根被财拔（案一百零七）**：用神${yinEl.chinese}就全靠${bazi.branches.first { it.element == yinEl }.chinese}根，${caiEl.chinese}有${shiEl.chinese}生力量大——把根拔了就不行了${if (bazi.branches.any { it.element == guanEl } && LIU_CHONG[bazi.branches.first { it.element == guanEl }] == monthBranch) "；${guanEl.chinese}官本可生印（印喜官杀），然月令${monthBranch.chinese}把它劫住了" else ""}——**层次高不了：即使走的运再好，也只能是个小康，或者说不如小康**。**财神反不真，罪加一等**：财星起了坏作用，财就不找你——不容易发财；同样用神虚，有的穷、有的一般差，分野在此：用神在地支被坏得严重（而且是相冲）就不行"
                else if (bingTongGuanTou) "——病神（透干比劫）有食伤通关（${biEl.chinese}生${shiEl.chinese}生${caiEl.chinese}），原局不夺财、不会有事；然${guanEl.chinese}官全无——**配置不足：起伏就要大**（一会儿天上一会儿地下）——帮用（${shiEl.chinese}${caiEl.chinese}）之运发财显贵、比劫见根之运大祸；配置好的八字（有官护财）人生就一路平坦（案四十九对勘）"
                else if (shiBanTongGuan) "——**食伤半通关（案八十七·化得没那么好）**：用财局病比劫（${bingEl.chinese}${format(bingS2)}）压用（${yongEl.chinese}${format(yongS2)}），然${shiEl.chinese}食伤有本气根**化解比劫生财（${shiEl.chinese}化${biEl.chinese}）——财富全凭食伤一字定之**：克${shiEl.chinese}之源的差运（${guanEl.chinese}克${biEl.chinese}之运）穷困潦倒；${shiEl.chinese}局成之运（三合填成或叠强）**才挣到钱（小康）**——财富稍微还可以（对照案六十理发师：一个理发一个卖衣裳——同层都没儿子）" +
                    (if (anCai114 && choice.way == JinBiaoQuYongWay.SHI_CAI_YONG_CAI)
                        "；**暗处的财（案一百一十四）**：${yinEl.chinese}克${shiEl.chinese}——通关化得没那么好之由；**用${caiEl.chinese}喜${shiEl.chinese}——没有${shiEl.chinese}这${caiEl.chinese}也不行**。运：${LIU_CHONG[bazi.dayPillar.branch]!!.chinese}暗冲去${bazi.dayPillar.branch.chinese}之病＋暗会${caiEl.chinese}局——**本来不去病也发，再加上去病就更发**；${LIU_HE[bazi.dayPillar.branch]!!.chinese}合${bazi.dayPillar.branch.chinese}化${biEl.chinese}——拱${biEl.chinese}旺，不行（离婚）"
                    else "")
                else "——病在而力未制（药为假药或力不足），半浊半清——普通人之命：多成多败，好两年坏两年（帮用去病之运好、帮病之运差），跟清命不一样",
            )
        }
        // 案一百二十一·平衡＋情份——走什么都不怕
        if (qingFen121) append(qingZhuo121Note(cb))
        // 案一百二十四·病在午火（财生杀旺）、药在子水（食神制杀）——杀有源制而不绝
        if (shiZhi124) append(qing124Note(cb))
        // 案一百二十五·浊——用神打架（印护官）；不浊就是军官而不是兵卒
        if (jiaQuGua125) append(zhuo125Note(cb))
        // 案一百二十六·吉神深藏终身之福——印能护官、有人照
        if (anYin126) append(qing126Note(cb))
        // 案一百二十七·层次不高——兵卒的命（财官旺、全指望比劫硬扛、没有印）
        if (yingKang127) append(cengCi127Note(cb))
    }

    /** 月建自寅起之序（寅 0…丑 11）及四季、孟仲季之名——案一百二十六文案用。 */
    private fun yueXu126(branch: EarthlyBranch): Int = (branch.ordinal + 10) % 12
    private fun ji126(branch: EarthlyBranch): String = listOf("春", "夏", "秋", "冬")[yueXu126(branch) / 3]
    private fun mzj126(branch: EarthlyBranch): String = listOf("孟", "仲", "季")[yueXu126(branch) % 3]

    /** 案一百二十六·本盘（己日，地支酉寅未辰）——事实性断语（丁丑戊寅己卯当兵、村干部判一年、丁亥年）仅限此盘。 */
    private fun isChart126(c: CtxB): Boolean =
        c.dayMaster == HeavenlyStem.JI && c.bazi.branches == listOf(EarthlyBranch.YOU, EarthlyBranch.YIN, EarthlyBranch.WEI, EarthlyBranch.CHEN)

    /**
     * 案一百二十六·坐下暗印·印护官——当兵——取用依据。
     * 师断：「用印还是用官？用印……身旺中变弱了，必须用印，用未中丁火」「这样也叫无理……这样的人犯法，
     * 但是这有个未中丁火暗印，你犯法也没事儿，这叫吉神深藏终身之福」「未中丁火不伤，有儿子」。
     * 事实性断语（初学毕业／村干部判一年／丁亥年／辰换戌）仅限本盘。
     */
    private fun anYin126Basis(c: CtxB): String {
        val bazi = c.bazi
        val riZhu = bazi.dayPillar
        val yinStem = branchHiddenStems.getValue(riZhu.branch).first { it.element == c.yinEl }
        val shiGans = bazi.stems.indices.filter { it != 2 && bazi.stems[it].element == c.shiEl }.joinToString("") { bazi.stems[it].chinese }
        val daGan = if (bazi.stems[1].element == c.shiEl) bazi.stems[1].chinese else shiGans
        val biGans = bazi.stems.indices.filter { it != 2 && bazi.stems[it].element == c.biEl }.joinToString("") { bazi.stems[it].chinese }
        val biZhis = bazi.branches.filter { it.element == c.biEl }.joinToString("") { it.chinese }
        val here = isChart126(c)
        val ji = ji126(c.monthBranch)
        val elXu = "${c.dayMaster.chinese}${c.dayMaster.element.chinese}生在${mzj126(c.monthBranch)}${ji}（${c.monthBranch.chinese}月）${c.dayMaster.element.chinese}虚"
        // 师举三例：土生春（土虚）、水生夏、火生冬——日主生在克我当令之月而虚；木秋、金夏据此类推（待师印证）
        val leiBi = when (c.dayMaster.element) {
            Element.EARTH -> "春天土虚，相当于水生在夏天、火生冬天一样"
            Element.WATER, Element.FIRE -> "师：「春天土虚，相当于水生在夏天，火生冬天一样」"
            else -> "据师「春天土虚，相当于水生在夏天，火生冬天一样」类推——待师印证"
        }
        return "**坐下暗印·印护官——当兵（案一百二十六）**：用印还是用官？**用印**——${elXu}（${leiBi}）；${daGan}与${c.monthBranch.chinese}（${c.guanEl.chinese}）打架叫**克泄交加**（伤官见官）；日主坐${riZhu.branch.chinese}是有力，但是偏弱的——在${ji}，${c.monthBranch.chinese}${c.guanEl.chinese}当令，食伤（${shiGans}）泄体，是有比劫帮身，但是需要用印。${c.dayMaster.chinese}有比劫${biGans}${biZhis}身旺没错，但${ji}克泄交加——**身旺中变弱了，必须用印，用${riZhu.branch.chinese}中${yinStem.chinese}${c.yinEl.chinese}（暗印）**。" +
            "此时身弱不用官；伤官见官、克泄交加了也不行——**这样也叫无理**（就像女人对其夫君无礼一样——暴燥无理的人）。**这样的人犯法**，但有个${riZhu.branch.chinese}中${yinStem.chinese}${c.yinEl.chinese}暗印——**犯法也没事儿：吉神深藏，终身之福**（印能护官" + (if (here) "；丁丑戊寅己卯当兵" else "") + "）：**有印护官了，国家的东西给保卫了——有印就代表他能去参军**（流年印露出来，都有当一年兵的；当没当过兵看原局——**跟应期没关系**" + (if (here) "（这人不露流年也当兵）" else "") + "：当兵一般十八去当兵，二十多岁人家也不要你——得切合实际）。要是没有印，那就是「贱人」了——贱人犯官（男的当贱人要犯法）。**印没露出来，这样人就不老实，犯事儿；有印出来看着你，就有这个法律概念——没有这个概念就犯法**。" +
            "**能说伤官见官的人就要犯法吗？不对——得看原局情况，不能刻舟求剑。不要问喜用有没有关系——一切都跟喜用有关系**（身弱不喜欢克泄交加；上个命身旺也不喜欢伤官制官）：当兵的原因就是伤官制官、命局有印——根本没有什么劫财抗杀，一样当兵。层次：**没学历**" + (if (here) "（初学毕业）" else "") + "——印没发露读不了书（兵卒多数学习不好；除军校或上大学后再参军的不一样——大部分人当兵是因为上学上不成、家庭条件也不好）；**有儿子**——${riZhu.branch.chinese}中${yinStem.chinese}${c.yinEl.chinese}不伤（同样都跟这个${yinStem.chinese}${c.yinEl.chinese}有关系——事儿跟事儿不一样）" +
            (if (here) "；**犯过法**——当村干部老打人、被判了一年（丁亥年殴打上访群众——进去了也没事儿：吉神深藏，有印有人照你、跟上面领导都认识）；反事实：时支换${EarthlyBranch.XU.chinese}——身也强，但克泄交加、又在${ji}，虚了" else "") +
            "。**需要整体把握，不要单看某个字**——什么是秘籍？开篇就是秘籍，生克就是秘籍；不知道生克就是瞎说"
    }

    /** 案一百二十六·清浊附注（吉神深藏终身之福——清浊档位沿用通则，师未点名）。 */
    private fun qing126Note(c: CtxB): String {
        val riZhi = c.bazi.dayPillar.branch
        val yinStem = branchHiddenStems.getValue(riZhi).first { it.element == c.yinEl }
        return "——**吉神深藏，终身之福（案一百二十六）**：${yinStem.chinese}${c.yinEl.chinese}印藏${riZhi.chinese}而不透——犯法亦无大事（有印有人照、进去也没事儿）；印能护官——印护官则当兵、无印则犯法（伤官见官不可刻舟求剑）。清浊档位沿用通则——师未点名"
    }

    /** 案一百二十六·断语清命句（清浊档位为通则映射——师未点名清浊贵贱；替「贵命、走什么运都不怕」通句）。 */
    private fun duanYuQing126(c: CtxB): String {
        val riZhi = c.bazi.dayPillar.branch
        val yinStem = branchHiddenStems.getValue(riZhi).first { it.element == c.yinEl }
        return "清浊档位为通则映射（师未点名清浊贵贱）；师断此处：**吉神深藏，终身之福**——${yinStem.chinese}${c.yinEl.chinese}印藏${riZhi.chinese}而不透、却能护官：犯法也没事儿、进去也没事儿（有印有人照你、跟上面领导都认识）（案一百二十六）。"
    }

    /** 案一百二十六·直断（事实性断语仅限本盘）。 */
    private fun zhiDuan126(c: CtxB): String {
        val riZhi = c.bazi.dayPillar.branch
        val yinStem = branchHiddenStems.getValue(riZhi).first { it.element == c.yinEl }
        val here = isChart126(c)
        return "直断：**印护官——当兵（兵卒）**（伤官见官本属克泄交加、无印则犯法；坐下${riZhi.chinese}中${yinStem.chinese}${c.yinEl.chinese}暗印护官——**吉神深藏终身之福，犯法也没事儿**）；**没学历**" + (if (here) "（初学毕业）" else "") +
            "——印没发露读不了书；**有儿子**——${riZhi.chinese}中${yinStem.chinese}${c.yinEl.chinese}不伤；" +
            (if (here) "**犯过法**（当村干部老打人、被判一年；丁亥年殴打上访群众——进去了也没事儿）；" else "") +
            "一切都跟喜用有关系、不能刻舟求剑（案一百二十六）。"
    }

    /** 案一百二十六·旺衰注（身旺中变弱）。 */
    private fun qiangNote126(c: CtxB): String {
        val riZhi = c.bazi.dayPillar.branch
        val yinStem = branchHiddenStems.getValue(riZhi).first { it.element == c.yinEl }
        return "；**身旺中变弱——${mzj126(c.monthBranch)}${ji126(c.monthBranch)}${c.dayMaster.element.chinese}虚、伤官见官克泄交加，必须用印（坐下${riZhi.chinese}中${yinStem.chinese}${c.yinEl.chinese}暗印）（案一百二十六）**"
    }

    /** 案一百二十六·学历（印没发露——没学历）。 */
    private fun xueLi126Of(c: CtxB): cn.xulun.bazi.model.JinBiaoXueLi =
        cn.xulun.bazi.model.JinBiaoXueLi(
            yinWanHao = true,
            level = cn.xulun.bazi.model.JinBiaoXueLiLevel.BU_GAO,
            basis = "**印没发露——没学历" + (if (isChart126(c)) "（初学毕业）" else "") + "（案一百二十六）**：克泄交加，需印星发露才能学习；然坐下暗印在、有此功能（能生儿子）——兵卒多数学习不好（除军校或上大学后再参军的以外）。",
        )

    /** 案一百二十六·子女（身衰印做儿·暗印不伤）。 */
    private fun ziNv126Text(bazi: BaziResult, yinEl: Element): String {
        val riZhi = bazi.dayPillar.branch
        val yinStem = branchHiddenStems.getValue(riZhi).first { it.element == yinEl }
        return "**身衰印做儿·暗印不伤（案一百二十六）**：${yinEl.chinese}藏${riZhi.chinese}而不透——**${riZhi.chinese}中${yinStem.chinese}${yinEl.chinese}不伤，有儿子**；同样都跟这个${yinStem.chinese}${yinEl.chinese}有关系——事儿跟事儿不一样（学历无、儿子有）"
    }

    /** 案一百二十六·流年印露＝当一年兵（[age]＝流年虚岁；仅参军之龄的流年干为印者）。 */
    private fun yunNote126(gan: HeavenlyStem, age: Int, ctx: YunCtx): String =
        "流年干${gan.chinese}${ctx.yinEl.chinese}——印露：**当一年兵**（「有印就代表他能去参军，流年印露出来，都有当一年兵的」；${age}岁——当兵一般都是十八去当兵；当没当过兵看原局——跟应期没有关系；二十多岁想当兵，人家也不要你——得切合实际；案一百二十六）"

    /** 案一百二十七·财官旺·比劫硬扛之结构：[ok]＝入档；[gen]＝硬扛之根（日主之禄支）；[sui]＝随财走之比劫本气支（巳）。 */
    private class YK127(val ok: Boolean, val gen: EarthlyBranch?, val sui: EarthlyBranch?)

    /**
     * 案一百二十七·财官旺·比劫硬扛（当兵专项④类「气势」）——判据。
     * 师断：「财星当令，财生官旺，这都挡不住了，完全是拿比劫硬扛」「拿比劫在这硬扛，你不服，不成从杀格」
     * 「这局没有印，用劫……肯定用神是午火比劫」「午火要占月令，还不是这回事」「亥水还是水」。
     */
    private fun yingKang127Of(
        bazi: BaziResult,
        dayMaster: HeavenlyStem,
        luZhi: EarthlyBranch,
        strength: DayStrength,
        ruoJi: Boolean,
        wangJi: Boolean,
        yinEl: Element,
        biEl: Element,
        caiEl: Element,
        guanEl: Element,
    ): YK127 {
        fun chongByGuan(s: EarthlyBranch) = bazi.branches.any { g -> g.element == guanEl && LIU_CHONG[g] == s }
        val guanBen = bazi.branches.firstOrNull { it.element == guanEl }
        // 硬扛之根：日主之禄支在局且不被冲者（午），否则取不被官杀本气支冲之比劫本气支
        val gen = luZhi.takeIf { it in bazi.branches && !chongByGuan(it) }
            ?: bazi.branches.firstOrNull { it.element == biEl && !chongByGuan(it) }
        // 随财走之比劫本气支（巳）：被官杀本气支冲（亥巳去火存金）、或与财本气支同三合局（巳酉拱金）
        val sui = bazi.branches.firstOrNull { s ->
            s.element == biEl && s != gen &&
                (chongByGuan(s) ||
                    SAN_HE_XU.any { (trio, el) -> el == caiEl && s in trio && bazi.branches.any { it.element == caiEl } })
        }
        val ok = strength == DayStrength.WEAK && !ruoJi && !wangJi && gen != null && guanBen != null &&
            // 财当令（月令本气）——「午火要占月令，还不是这回事」
            bazi.monthPillar.branch.element == caiEl &&
            // 财生官：财本气支贴生官杀本气支（酉生亥——「酉金配亥水是俩王了」）
            bazi.branches.indices.any { k ->
                bazi.branches[k] == guanBen && listOf(k - 1, k + 1).any { j -> j in bazi.branches.indices && bazi.branches[j].element == caiEl }
            } &&
            // 没有印：印不透干、无本气根；藏支者只许藏于官杀本气支（「亥水还是水」——印藏官杀水里不算有印；
            // 印藏坐下比劫支者归案一百二十六）
            bazi.stems.none { it != dayMaster && it.element == yinEl } && !hasBenQiRoot(bazi, yinEl) &&
            bazi.branches.all { br -> br.element == guanEl || branchHiddenStems.getValue(br).none { it.element == yinEl } }
        return YK127(ok, gen, sui)
    }

    /** 案一百二十七·文案共用：硬扛之根、随财走之支及其冲神／拱局之财支。 */
    private class Ctx127(
        val gen: EarthlyBranch,
        val sui: EarthlyBranch?,
        val guanZhi: EarthlyBranch,
        val chongShen: EarthlyBranch?,
        val caiMate: EarthlyBranch?,
    )

    private fun ctx127Of(c: CtxB, yk: YK127): Ctx127 {
        val bazi = c.bazi
        val sui = yk.sui
        val chongShen = sui?.let { s -> bazi.branches.firstOrNull { g -> g.element == c.guanEl && LIU_CHONG[g] == s } }
        val caiMate = sui?.let { s ->
            bazi.branches.firstOrNull { br ->
                br.element == c.caiEl && SAN_HE_XU.any { (trio, el) -> el == c.caiEl && s in trio && br in trio }
            }
        }
        return Ctx127(yk.gen!!, sui, bazi.branches.first { it.element == c.guanEl }, chongShen, caiMate)
    }

    /** 案一百二十七·取用依据（身弱用劫——财官旺·比劫硬扛之气势；男命断当兵、女命不断）。 */
    private fun yingKang127Basis(c: CtxB, yk: YK127, gender: cn.xulun.bazi.model.Gender?): String {
        val x = ctx127Of(c, yk)
        val sui = x.sui
        val yinGan = branchHiddenStems.getValue(x.guanZhi).firstOrNull { it.element == c.yinEl }
        val nan = gender != cn.xulun.bazi.model.Gender.FEMALE
        // 巳亥冲，去火存金是一方面，即便没有金生水也足够了，水克火贴着克也厉害
        val chong = x.chongShen?.let { g ->
            "${sui!!.chinese}${g.chinese}冲——**去${c.biEl.chinese}存${c.caiEl.chinese}是一方面**，即便没有${c.caiEl.chinese}生${c.guanEl.chinese}也足够了：**${c.guanEl.chinese}克${c.biEl.chinese}贴着克也厉害**。"
        } ?: ""
        // 亥中甲算吉神深藏——但亥是水：印藏官杀水里不算有印
        val yinNote = if (yinGan != null)
            "${x.guanZhi.chinese}中虽藏${yinGan.chinese}${c.yinEl.chinese}（算吉神深藏），然**${x.guanZhi.chinese}${c.guanEl.chinese}还是${c.guanEl.chinese}**——终究是克身的：不能说${x.guanZhi.chinese}里头有块糖，就按${c.yinEl.chinese}去走（印藏官杀水里；异于案一百二十六「比劫里头有印是比劫」）。**这局没有印，用劫**。"
        else "**这局没有印，用劫**。"
        // 金生水旺、巳酉拱金、亥巳去火存金——全是财官了，巳火当然帮金了
        val ju = listOfNotNull(
            if (x.caiMate != null) "${sui!!.chinese}${x.caiMate.chinese}拱${c.caiEl.chinese}" else null,
            x.chongShen?.let { g -> "${g.chinese}${sui!!.chinese}去${c.biEl.chinese}存${c.caiEl.chinese}" },
        )
        val juNote = if (ju.isEmpty()) "" else
            "**${c.caiEl.chinese}生${c.guanEl.chinese}旺、${ju.joinToString("、")}——全是财官了**，${sui!!.chinese}${c.biEl.chinese}当然帮${c.caiEl.chinese}了。"
        val dangBing = if (nan)
            "③**这样的人不服——他就当兵**：当兵的人不怕困难，命局财官旺克${x.gen.chinese}${c.biEl.chinese}就是困难，${x.gen.chinese}${c.biEl.chinese}就在这硬扛顶着就不服——**他就当兵**（「劫财七杀两相连，早早离家去戍边」说的就是这种情况——然那种不一定当兵，**非得有这种气势才当兵**）。" +
                "**气势**：财杀这么旺，我就全指望比劫硬扛——我这人就不怕事儿；知道任务完不成、无法承载，还要去跟你干——宁可牺牲了也去（坚韧不拔、不怕死）。当兵的除了护卫国家的（护官型——案一百二十五／一百二十六），另外就是找这种气势（男命拿官当国家；女命拿官当你的丈夫——古代女人没有当兵的，当兵只断男命）。"
        else
            "③**女命——不断当兵**：男命拿官当国家、女命拿官当你的丈夫；古代女人没有当兵的。"
        val fanFa = if (c.guanEl == Element.WATER) "走${EarthlyBranch.ZI.chinese}${c.guanEl.chinese}大运肯定要犯法" else
            "走${c.guanEl.chinese}官杀大运肯定要犯法（师举子水大运为例）"
        return "**财官旺·比劫硬扛——" + (if (nan) "当兵" else "气势（女命不断当兵）") + "（案一百二十七·当兵专项④类「气势」）**：①**取用——身弱用印比**：${c.dayMaster.chinese}${c.dayMaster.element.chinese}生${c.monthBranch.chinese}月**财当令**；${c.biEl.chinese}有力是没错，然**${c.monthBranch.chinese}${c.caiEl.chinese}配${x.guanZhi.chinese}${c.guanEl.chinese}是俩王了**——不能说日主有力、${c.biEl.chinese}有力你就行了，**得找平衡**（不要认为找平衡是在开玩笑）。${x.gen.chinese}${c.biEl.chinese}隔着一个${x.guanZhi.chinese}${c.guanEl.chinese}能克这个财吗？不行（隔位不克）。" +
            chong + "**没有印就要用劫**——" + yinNote + juNote + "**肯定用神是${x.gen.chinese}${c.biEl.chinese}比劫**。" +
            "②**用神比劫不一定就当兵——要找一股气势**：财星当令、财生官旺，这都挡不住了——**完全是拿比劫硬扛**（${x.gen.chinese}${c.biEl.chinese}不占月令——若占月令则不是这回事；这完全指望着${x.gen.chinese}${c.biEl.chinese}硬扛）；${c.monthBranch.chinese}${c.caiEl.chinese}生${x.guanZhi.chinese}${c.guanEl.chinese}都占有绝对力量，拿比劫在这硬扛——**不服，不成从杀格**。" +
            dangBing +
            "④**层次不高——兵卒的命**：没有印，学习不好、书香不就，脑瓜里那些想法没有。**中间卡个印就不当兵了**（有印就有层次、学习好了，不去当小兵卒）。反事实：**$fanFa**（官杀要克他——犯法与当兵很多相似：都是不服的人，有的人犯法、有的人当兵）。"
    }

    /** 案一百二十七·旺衰注（身弱——俩王；硬扛不服，不成从杀格）。 */
    private fun qiangNote127(c: CtxB, yk: YK127): String {
        val x = ctx127Of(c, yk)
        val qu = x.chongShen?.let { g -> "；${g.chinese}${x.sui!!.chinese}冲去${c.biEl.chinese}存${c.caiEl.chinese}" } ?: ""
        return "；**身弱——${c.monthBranch.chinese}${c.caiEl.chinese}配${x.guanZhi.chinese}${c.guanEl.chinese}俩王（财官旺），${c.biEl.chinese}有力也得找平衡$qu；${x.gen.chinese}${c.biEl.chinese}硬扛不服——不成从杀格（案一百二十七）**"
    }

    /** 案一百二十七·清浊附注（层次不高——兵卒的命）。 */
    private fun cengCi127Note(c: CtxB): String =
        "——**层次不高：兵卒的命（案一百二十七）**：财官旺而全指望比劫硬扛、没有印——师未点名清浊，档位沿用通则"

    /** 案一百二十七·断语清命句（清浊档位为通则映射——师未点名；替「贵命、走什么运都不怕」通句）。 */
    private fun duanYuQing127(c: CtxB): String =
        "清浊档位为通则映射（师未点名清浊）；师断**层次不高——兵卒的命**（没有印、学习不好，全指望比劫硬扛）（案一百二十七）。"

    /** 案一百二十七·直断（男命断当兵；女命不断）。 */
    private fun zhiDuan127(c: CtxB, yk: YK127, gender: cn.xulun.bazi.model.Gender?): String {
        val x = ctx127Of(c, yk)
        val fanFa = if (c.guanEl == Element.WATER) "走${EarthlyBranch.ZI.chinese}${c.guanEl.chinese}大运肯定要犯法" else
            "走${c.guanEl.chinese}官杀大运肯定要犯法（师举子水大运为例）"
        return "直断：" +
            (if (gender != cn.xulun.bazi.model.Gender.FEMALE)
                "**当兵（兵卒）——财官旺、比劫硬扛的气势（不服、不怕困难、坚韧不拔）**（用神${x.gen.chinese}${c.biEl.chinese}比劫硬扛——不成从杀格）；"
            else "**财官旺、${x.gen.chinese}${c.biEl.chinese}比劫硬扛的不服之气**（女命拿官当丈夫——不断当兵）；") +
            "**学习不好**（没有印——层次不高）；**没有儿子**（身衰印做儿，没有印）；$fanFa（案一百二十七）。"
    }

    /** 案一百二十七·学历（没有印——学习不好；兵卒之命）。 */
    private fun xueLi127Of(): cn.xulun.bazi.model.JinBiaoXueLi =
        cn.xulun.bazi.model.JinBiaoXueLi(
            yinWanHao = false,
            level = cn.xulun.bazi.model.JinBiaoXueLiLevel.BU_GAO,
            basis = "**没有印——学习不好（兵卒的命·层次不高）（案一百二十七）**：脑瓜里那些想法没有、书香不就——全指望比劫硬扛；中间卡个印就有层次、学习好了（不去当小兵卒）。",
        )

    /** 案一百二十七·子女（身衰印做儿——没有印，没儿子）。 */
    private fun ziNv127Text(bazi: BaziResult, yinEl: Element, guanEl: Element): String {
        val guanZhi = bazi.branches.first { it.element == guanEl }
        val yinGan = branchHiddenStems.getValue(guanZhi).firstOrNull { it.element == yinEl }
        val note = if (yinGan != null)
            "${guanZhi.chinese}中虽藏${yinGan.chinese}${yinEl.chinese}，然${guanZhi.chinese}${guanEl.chinese}还是${guanEl.chinese}——不算有印；"
        else ""
        return "**身衰印做儿，没有印——没儿子（案一百二十七）**：${note}身弱财官旺、全指望比劫硬扛"
    }

    /** 案一百二十七·子水大运——官杀克比劫：肯定要犯法（犯法章待授）。 */
    private fun yunNote127(zhi: EarthlyBranch, ctx: YunCtx): String =
        "运支${zhi.chinese}——${ctx.guanEl.chinese}官杀克比劫：**肯定要犯法**（「命主要是走子水大运，肯定要犯法的，因为官杀要克他」——犯法章待授；案一百二十七）"

    private fun format(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.1f", v)
    private fun pct(v: Double): String = String.format("%.0f%%", v * 100)
}

/** List.forEachIndexed 的带索引版本变体（干合检查用）。 */
private inline fun <T> List<T>.anyIndexed(predicate: (Int, T) -> Boolean): Boolean {
    forEachIndexed { i, t -> if (predicate(i, t)) return true }
    return false
}
