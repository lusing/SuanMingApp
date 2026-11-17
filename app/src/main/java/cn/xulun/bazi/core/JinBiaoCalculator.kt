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
    private const val YIN_QING = 2.0            // 旺极局印党力低于此值 → 印轻（方可行食伤）
    private const val QIANG_JI_YIN = 4.0        // 印党力高于此值且财官食全无 → 强极（印比叠叠同心）
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

        // ── 弱极判定（第三讲两型） ──
        val yinS = partyScore(bazi, yinEl)
        val biS = partyScore(bazi, biEl)
        val supportS = yinS + biS
        val supportRooted = hasBenQiRoot(bazi, yinEl) || hasBenQiRoot(bazi, biEl)
        val caiS = partyScore(bazi, caiEl)
        val guanS = partyScore(bazi, guanEl)
        val shiS = partyScore(bazi, shiEl)
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
        if (strength == DayStrength.STRONG && !hasBenQiRoot(bazi, biEl) &&
            !(bazi.dayPillar.branch.element == yinEl && yinS > guanS)
        ) {
            val diSAdjT = caiSAdj + guanSAdj + shiSAdj
            if (diSAdjT > 0.0 && diSAdjT >= (yinS + biS) * 0.8) strength = DayStrength.WEAK
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
            !(kunLunEl != yinEl &&
                (bazi.monthPillar.stem.element == yinEl || bazi.hourPillar.stem.element == yinEl) &&
                hasBenQiRoot(bazi, biEl)) &&
            // 案五十八裁：食伤得局（木旺）而印星透干坐本气禄根（庚坐申——有力可依）——
            // 克泄交加身太衰，肯定要用印星帮身：用神庚申，正常分析不归昆仑（印好才是真的好）
            !(kunLunEl == shiEl && bazi.stems.anyIndexed { i, s ->
                i != 2 && s.element == yinEl && bazi.branches[i].element == yinEl
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
        val tiaoHouEl: Element? = when {
            hanJu && hanTiaoYouQi && !bazi.branches.all { it.element == Element.WATER } -> Element.FIRE
            zaoJu && zaoTiaoYouQi && !bazi.branches.all { it.element == Element.FIRE } -> Element.WATER
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

        data class Choice(val way: JinBiaoQuYongWay, val el: Element, val basis: String)

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
                        append(if (yinS < YIN_QING) "；局中印轻（${format(yinS)}），食伤顺泄而行也是好运" else "；局中印重（${format(yinS)}），走食伤运印夺食，不可行")
                    }
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
            tiaoHou -> Choice(
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
                    "昆仑得局（命三）：地支${kunLunEl.chinese}得局（${branchCount.getValue(kunLunEl)}支）配${kunLunSource!!.chinese}生——昆仑之${kunLunEl.chinese}，只可顺不可逆。" +
                        "此势不一定指日主——命局${kunLunEl.chinese}地支得局配${kunLunSource.chinese}生，即围绕${kunLunEl.chinese}说事：" +
                        "顺者${kunLunSource.chinese}（生源）、${kunLunEl.chinese}（本势）、${kunLunEl.generates.chinese}（顺泄）；" +
                        "逆者${keEl.chinese}（克${kunLunEl.chinese}止${kunLunEl.chinese}）与${kunLunEl.controls.chinese}（克${kunLunSource.chinese}逆金水之局）——顺来逆去贫之意，逆则贫命出灾",
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
            // 第四讲·正格五理③／⑥＋案三十食神制杀＋基线：身弱取用
            strength == DayStrength.WEAK -> when {
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
                guanS >= FOLLOW_MIN_SCORE && (bazi.stems.any { it.element == yinEl } || hasBenQiRoot(bazi, yinEl) ||
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
                        if (bazi.dayPillar.branch.element == yinEl) {
                            append("；坐下有印（${bazi.dayPillar.branch.chinese}）——没有此印，这么旺的杀就要克死日主；有坐下印就得了贵：天干杀来克也没关系，坐下印持续生即可。坐下有印就叫有情——别处之气皆向我坐下生拱（杀生印），外界有情就加分，不用管印之旺弱，一样得贵——这叫情分；原命财杀旺，一定要走帮身运才能好")
                            if (guanS >= 10.0) {
                                append("；众杀混行，一仁可化——纵无他印，独坐下一印亦化（官杀全往我坐下来生拱）；克印之运用神被伤——一事无成甚至贫夭，然运一过功能即复，再发富贵")
                            }
                        }
                        if (bazi.branches.contains(EarthlyBranch.CHEN) && bazi.branches.contains(EarthlyBranch.SHEN) &&
                            bazi.branches.indexOf(EarthlyBranch.CHEN) + 1 == bazi.branches.indexOf(EarthlyBranch.SHEN)
                        ) {
                            append("；拱局辩证：辰贴申必生金——土生金是谁也改变不了的规矩，会局被夺不以局论（杀旺取坐下印为用）；寒木向阳——有火时层次更高，但非必需：没有水命主就被旺杀克死，有水即成格，没火一样当官（调候不用刻意考虑）")
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
                            append("；食伤得局（${format(shiS)}）——明面上制杀太过（${guanEl.chinese}官杀虚：辰土虽为根，春天木旺土虚）；更患日主过泄（食伤泄体、日主没那么旺）——**一定要走印运：一是护杀（印克食），二是帮身（印生身）**".replace("**", ""))
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
                            buildString {
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
                            },
                    )
                }
            }
            // 第四讲·正格五理①②④⑥＋第五讲去寡＋基线：身强取用
            strength == DayStrength.STRONG -> when {
                // 第五讲·去寡（案七/案八定则）：三条件差一点都不对，必须全部满足——
                // ①日主强旺；②官杀真无根气（地支藏干全无官杀——天干再透两见亦寡）且财不生官（财无透干无本气根）；③有食伤。
                // 案八之诫：官杀地支藏干有字（丑中癸）或财透干来生（庚）——条件②破，即非去寡，转用官。
                guanS > 0.0 &&
                    bazi.branches.none { br -> branchHiddenStems.getValue(br).any { it.element == guanEl } } &&
                    bazi.stems.none { it.element == caiEl } && !hasBenQiRoot(bazi, caiEl) &&
                    shiS >= FOLLOW_MIN_SCORE -> Choice(
                    JinBiaoQuYongWay.QU_GUA, shiEl,
                    "推命经第二段·去寡（案七）：看力量首先看地支、地支主宰——日主通根得气身旺；" +
                        "官杀（党力${format(guanS)}，天干再透两见）地支藏干全无——不载即无根气，无用之官，「不用之官尽可去」；" +
                        "财无透干无本气根不生官（党力${format(caiS)}）、食伤有力（党力${format(shiS)}）——去官发富贵，用神取食伤（去官之星）；" +
                        "违背官杀之运好运、坏官越重越好；顺着官杀（生官／官星得地得运）则一世无成",
                )
                // 食神制杀格（五理④＋第七讲条件一）：官有本气根之真旺、财不滋官、食伤有力——
                // 且【日主必须有根气】：食神泄体，无根无气承载不了食神之泄，食神制杀反贫夭负债；得根气则制杀得富贵。
                // 弱中变旺局之官已被泄克转衰——衰官不制而用之（案十）
                // 食神制杀格（五理④＋条件一＋案四十八）：官有本气根之真旺、财不滋官（无财或财不透干）、食伤有力——
                // 且【日主必须有根气】：食伤泄体，无根无气承载不了食神之泄（食神制杀反贫夭负债）；得根气则制杀得富贵。
                // 根气三途：本气根／藏干通根≥3（案三十）／坐下印得局压杀（案四十八：印6.5对杀4.5——印生身有依）。
                !ruoZhongBianWang && guanS >= GUAN_WANG && hasBenQiRoot(bazi, guanEl) &&
                    (caiS < FOLLOW_MIN_SCORE || bazi.stems.none { it.element == caiEl }) &&
                    // 案八十二戒：印干贴克食干而官透有力——印看住食伤不使犯官（把官给解放了）——
                    // 身旺是用官的（有印护官制食——不取食制；对勘案五十：官全无而财护食——仍用食）
                    !(bazi.stems.any { it != dayMaster && it.element == yinEl } &&
                        bazi.stems.any { it != dayMaster && it.element == guanEl } &&
                        bazi.stems.indices.any { i -> i != 2 && bazi.stems[i].element == shiEl &&
                            ((i - 1 in bazi.stems.indices && bazi.stems[i - 1].element == yinEl) ||
                                (i + 1 in bazi.stems.indices && bazi.stems[i + 1].element == yinEl)) }) &&
                    shiS >= FOLLOW_MIN_SCORE &&
                    (hasBenQiRoot(bazi, biEl) || (bazi.dayPillar.branch.element == yinEl && yinS > guanS)) -> Choice(
                    JinBiaoQuYongWay.ZHI_SHA, shiEl,
                    "食神制杀格（案四十八·身旺杀旺可制）：官党力 ${format(guanS)} 旺（有本气根）、财不滋官（财党力${format(caiS)}）——必用食伤制杀制官（官不一定不可以制）；食伤党力 ${format(shiS)}。" +
                        "食神制杀条件第一条：日主必须有根气（印得局生身亦是依）——无根无气者承载不了食神之泄（食神制杀反贫夭、负债）；日主得根气，承载得动食神之泄，食神制杀得富贵。" +
                        "格局高低看杀之强弱存续：杀旺可制不贫夭——官贵之格（官二代、处长之子之属）；杀之根被拔克绝（制杀太过——子午冲根之属）则落普通人之列",
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
                // 五理②/第七讲·财滋弱杀（案八/案十/案廿二）：身旺而官在局（官废不能用——案十二）——有财有官杀优先用杀
                // 案六十一戒：官须**透干**方用官配财（案廿二丙透）——官只藏支而弱（寅不旺）不用官：
                // 身旺印重则喜财坏印（HUAI_YIN 殿后接手），金水（食伤生财）为取用
                guanS >= FOLLOW_MIN_SCORE && !guanFei &&
                    bazi.stems.any { it != dayMaster && it.element == guanEl } -> Choice(
                    JinBiaoQuYongWay.PEI_CAI, guanEl,
                    buildString {
                        append("财滋弱杀格（案廿二）：身旺（${if (ruoZhongBianWang) "弱中变旺——坐贴身印比、月令敌党之支被日支泄克转衰" else "土金两旺通根得气"}），官杀露而弱（党力${format(guanS)}）${if (shiS < FOLLOW_MIN_SCORE) "、无食伤" else ""}——有财有官杀优先用杀，命主用官")
                        append("；财滋弱杀与财滋弱官不区分，是一样的（①身强②用官杀③财生）；财配置（党力${format(caiS)}）${if (caiZhuBing) "——原局${biEl.chinese}克${caiEl.chinese}财被劫，然杀弱无关系、财弱也无妨：只要大运解病（如${shiEl.chinese}运泄${biEl.chinese}生${caiEl.chinese}通关）一样发富发贵，勿见杀弱财弱断贫" else "——官星指着财来生，有财就是好格局"}")
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
                    },
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
                shiS >= FOLLOW_MIN_SCORE -> Choice(
                    JinBiaoQuYongWay.SHI_SHENG_CAI, shiEl,
                    buildString {
                        append("正格续理·身旺食生财（案九）：身旺无官杀必泄体得衡——用神取食伤（党力 ${format(shiS)}）泄刚煞之气")
                        if (yinS >= YIN_HUAI) {
                            append("；印星两坏（生身助旺＋贴身克用神）——印为格局之病，病去或用神强旺方能好")
                            if (!caiZai) append("；财地支不载（金旺土生金、支全金局）——用财无出息，不如用食伤（秋水通源，壬水有劲）")
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
                    },
                )
                // 五理①财坏印（殿后）：无官可用、食伤亦无力，而印坏事且财地支有载或双透得气——用财星坏印
                // （案六十一：壬癸双透虽地支不载，财混杂而得气——喜财坏印，用神财）
                yinS >= YIN_HUAI && (caiZai || bazi.stems.count { it != dayMaster && it.element == caiEl } >= 2) -> Choice(
                    JinBiaoQuYongWay.HUAI_YIN, caiEl,
                    "正格五理·印可配官煞（身旺印坏）：印党力 ${format(yinS)} 起坏作用而官食俱不可用——必用财星坏印（财党力 ${format(caiS)}，${if (caiZai) "地支有载" else "双透得气"}）。" +
                        "身足够旺——**喜财坏印，财好大家都好**（财官就是机会：身板够硬就差机会）；财富值取决于用神财之力（案六十一）",
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
        val yongGod = representativeGod(bazi, dayMaster, yongEl)
        val yongShenStars = locateElementStars(bazi, dayMaster, yongEl)

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
        // 案八十四·财气通门户：病神（枭印）虽大而拱财暗存（药待运填）——财星有情：
        // 走差运小康（生意也不差选对行）、好运发大财——非满盘浊
        val gongCaiTongMen = bingEl == yinEl &&
            listOf(
                setOf(EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI) to Element.WOOD,
                setOf(EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU) to Element.FIRE,
                setOf(EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN) to Element.WATER,
                setOf(EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU) to Element.METAL,
            ).any { (trio, el) -> el == caiEl && bazi.branches.count { it in trio } >= 2 && !trio.all { it in bazi.branches } }
        val qingZhuo = when {
            qingKu -> JinBiaoQingZhuo.QING_KU
            quanZhuo -> JinBiaoQingZhuo.QUAN_ZHUO
            yongWuLiHua -> JinBiaoQingZhuo.MAN_ZHUO
            bingS2 == 0.0 -> JinBiaoQingZhuo.QING
            bingHua -> JinBiaoQingZhuo.QING
            bingZhi -> JinBiaoQingZhuo.QING
            bingYiTU -> JinBiaoQingZhuo.QING
            bingTongGuanTou && huaJuBuYong -> JinBiaoQingZhuo.QING
            hasBenQiRoot(bazi, bingEl) && bingS2 >= 1.5 * yongS2 && !bingTongGuanTou && !gongCaiTongMen -> JinBiaoQingZhuo.MAN_ZHUO
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
        val qingZhuoBasis = buildString {
            append("病神＝${bingEl.chinese}（克${yongEl.chinese}用神，党力${format(bingS2)}）；用神党力${format(yongS2)}${if (yongDeLi) "（得力）" else "（不得力）"}")
            if (jianBing != null) append("；$jianBing")
            when (qingZhuo) {
                JinBiaoQingZhuo.QING_KU -> append(
                    if (choice.way == JinBiaoQuYongWay.FU_SHEN)
                        "——用神${yongEl.chinese}（印）被得局之${caiEl.chinese}财坏（财有多旺他就有多穷——食伤生财＝给忌神添粮）：病大难救——**清枯之命**：印运把印补起来还能温饱，食伤生财之运身无分文、负债"
                    else "——用神${yongEl.chinese}（印）虽可取（帮身扛杀），然生印之${guanEl.chinese}过旺蒸之（火炎土焦，燥土难以生金——生金有点难，但也只能用土）、又被${keYinEl.chinese}克（党力${format(partyScore(bazi, keYinEl))}有根）——病太大，一点救都没有：**清枯之命**，走好运只得温饱，走克用生忌之运一事无成（贫穷清枯）".replace("**", ""),
                )
                JinBiaoQingZhuo.QUAN_ZHUO -> append("——官（${guanEl.chinese}）被${keGuan.chinese}克且财源被断、食（${shiEl.chinese}）被印克无本气根、财（${caiEl.chinese}）被比劫冲克——三者俱废，用神无着落：全浊，一事无成（学什么都不会、干什么都有人捣乱），比普通老百姓还差")
                JinBiaoQingZhuo.QING -> append(
                    if (bingS2 == 0.0) "——原局无病神，用神有力无扰，此为清（贵命，走什么运都不怕）"
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
                    if (bingTongGuanTou) "——病神（透干比劫）有食伤通关（${biEl.chinese}生${shiEl.chinese}生${caiEl.chinese}），原局不夺财、不会有事；然${guanEl.chinese}官全无——**配置不足：起伏就要大**（一会儿天上一会儿地下）——帮用（${shiEl.chinese}${caiEl.chinese}）之运发财显贵、比劫见根之运大祸；配置好的八字（有官护财）人生就一路平坦（案四十九对勘）"
                    else "——病在而力未制（药为假药或力不足），半浊半清——普通人之命：多成多败，好两年坏两年（帮用去病之运好、帮病之运差），跟清命不一样",
                )
            }
        }

        // ── 喜忌（第一讲：帮用=喜，损用=忌） ──
        val xiElements = Element.entries.filter { it.generates == yongEl || it == yongEl }
        val jiElements = Element.entries.filter { it.controls == yongEl || yongEl.generates == it || yongEl.controls == it }

        // ── 子女判读（第十一讲·子女章判定树：子星按原局喜忌而定，来回检查；子星要昌） ──
        // 四情况铁律：必有儿／必无儿／无儿有女／无儿无女损子——逃不出这个圈。
        val ziNv: cn.xulun.bazi.model.JinBiaoZiNv? = if (ruoJi) {
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
            null // 专旺之子女另裁——待师传
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
                val tieKe = biIdxs.isNotEmpty() && caiIdxs.isNotEmpty() &&
                    caiIdxs.any { ci -> keZhuIdxs.any { k -> kotlin.math.abs(ci - k) == 1 } } &&
                    !caiIdxs.any { ci ->
                        keZhuIdxs.any { k ->
                            val lo = minOf(ci, k)
                            val hi = maxOf(ci, k)
                            shiIdxs.any { si -> si in (lo + 1) until hi }
                        }
                    }
                when {
                    caiS > 0.0 && tieKe -> {
                        val guanJiu = guanS >= GUAN_WANG ||
                            bazi.stems.anyIndexed { i, s -> i != 2 && s.element == guanEl && bazi.branches[i].element == guanEl }
                        if (guanJiu) {
                            ZN(guanEl, true, false, "身旺财为子而**比劫贴克财**——然官杀克劫护财（官透坐根有力）：**官杀为儿子，有官杀就有儿子**（怕食地——食伤制官来捣乱）")
                        } else {
                            ZN(caiEl, false, false,
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
                                "身旺财为子（财没露）而**比劫夺财**——官杀克劫解放财（官透坐根有力）：**官杀为儿子，有官杀就有儿子**。**暗克非冲不拔根**（如午亥之克——不是贴身的冲克：若亥换成子才叫拔根，午亥只作丁根弱论）；食神克官而**印贴克食神护官（把官给解放了）——百分之百有儿子**；再走官地（南方）之运——**直接锁定不可能没儿子（富贵得子）**。**不但生儿子，儿子也有出息**：印看住伤官＝给儿子一护卫（好条件、留遗产、教学习——捣乱儿子的东西全被印克了，儿子得福气）；**印护官主要关系事业**（身旺用官、印看食伤不使犯官——走官地事业发财）。**辟谬**：穿一下变女孩、阳男阴女之说——与生男生女无关（案八十二）")
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
                            // 案八十四·拱暗财档：财半合拱局暗存（亥未拱木）——有暗财＝有子（不仅有子还有钱）
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
                        if (!caiZai && !hasBenQiRoot(bazi, caiEl)) {
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
                        when {
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
                        if (biS >= FOLLOW_MIN_SCORE) ZN(biEl, true, false, "身弱局里没有印（必财旺或官杀旺）——**比劫就是儿子**（抗杀挡杀镇财给我力量）：有比劫才有儿子")
                        else ZN(biEl, false, false, "身弱无印无比劫——**没儿子**")
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
            // 旺极顺泄：满盘比劫旺到极端，食伤顺泄不克——局中印轻则食伤运也是好运；印重则夺食不可行
            if (wangJi && !qiangJi && yinS < YIN_QING) put(shiEl, "旺极顺泄——局中印轻，食伤运也是好运")
            // 第五讲·去寡：食伤＝去官之星，行此好运；印泄官生身——身旺食伤亦旺，行此亦好运（案七）
            if (choice.way == JinBiaoQuYongWay.QU_GUA) {
                put(shiEl, "去官之星到位——违背官杀，好运")
                put(yinEl, "印泄官生身——身旺食伤亦旺，好运")
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
                put(kunLunEl.generates, "顺泄——${kunLunEl.chinese}生${kunLunEl.generates.chinese}（金生水水生木），好运")
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
            if (choice.way == JinBiaoQuYongWay.SHI_SHENG_CAI && yinS >= YIN_HUAI) {
                put(yinEl, "印为病——病来即差（贫穷之运）")
                put(biEl, "身更旺——发泄受阻，差运")
            }
            // 昆仑得局之逆（命二/命三）：克势者止水（土）、克生源者逆局（火）——顺来逆去贫之意
            if (choice.way == JinBiaoQuYongWay.KUN_LUN) {
                val keEl = Element.entries.first { it.controls == yongEl }
                put(keEl, "克${yongEl.chinese}止${yongEl.chinese}——土止昆仑之水，触犯强旺之势，凶")
                put(yongEl.controls, "克生源逆${yongEl.chinese}局——逆则贫命出灾（刑妻克子贫乏）")
            }
            // 案廿四·财党杀攻身：财滋杀攻身——最忌
            if (choice.way == JinBiaoQuYongWay.GONG_SHEN) {
                put(caiEl, "财滋杀攻身——最忌（怕财来滋杀）")
            }
            // 气象调候之反（案十六/案十八/案十九/案二十一）：寒/燥而调候神无根气——强走调候运反大凶；
            // 案廿五裁：格局取用已定（财滋弱杀/弱官）则调候让位——走帮官杀财之运即好运
            if (choice.way != JinBiaoQuYongWay.PEI_CAI) {
                if (hanJu && huoTou && !hanTiaoYouQi) {
                    put(Element.FIRE, "调候神丙火无根气（隔位之生、地支不载）——强走火运调候反一事无成（离婚克子之属）；反以不调候为美，顺其寒")
                }
                if (zaoJu && shuiTou && !zaoTiaoYouQi) {
                    put(Element.WATER, "调候神壬癸水无根——强走水运调候即犯火之旺（一败如灰，甚者死）；反以不调候为美，顺其燥")
                }
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
            wangJiFamily = wangJi || qiangJi, yinZhong = yinS >= YIN_QING,
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
        val yunPings = daYun?.yuns?.mapIndexed { idx, yun ->
            // 案十一：身弱用印局行印比连行方局（南方火印地）——整运作吉（杀旺印旺身衰补起，富贵）
            pingYun(
                bazi.branches, yun.pillar, yun.startAge, yun.startYear, ctx, bureauOverride[idx],
                forceJI = choice.way == JinBiaoQuYongWay.HUA_SHA && bureauOverride[idx] != null,
            )
        }

        // ── 断语（一律锚定用神，禁泛断） ──
        val jiYuns = yunPings.orEmpty().filter { it.verdict == JinBiaoVerdict.JI }.map { it.yunName }
        val xiongYuns = yunPings.orEmpty().filter { it.verdict == JinBiaoVerdict.XIONG }.map { it.yunName }
        // 第四讲·五理⑤：二者如替换，定是贫穷汉——原局结构即呈反用之象者
        val tiHuanPin: String? = when {
            !ruoJi && !wangJi && strength == DayStrength.WEAK &&
                shiS >= FOLLOW_MIN_SCORE && guanS >= FOLLOW_MIN_SCORE && yinS < 1.0 &&
                // 案三十豁免：日主藏干通根≥3处＝有根气，食神制杀成格（贫穷断仅限无根气强制）
                bazi.branches.count { br -> branchHiddenStems.getValue(br).any { it.element == biEl } } < 3 ->
                "五理之五·二者如替换：身弱不用印化杀、原局反以食伤强制动杀（食${format(shiS)}官${format(guanS)}而印仅${format(yinS)}）——克泄交加、弃日主于不顾，定是贫穷汉。"
            !ruoJi && !wangJi && strength == DayStrength.STRONG &&
                guanS >= FOLLOW_MIN_SCORE && yinS >= FOLLOW_MIN_SCORE && shiS < 1.0 ->
                "五理之五·二者如替换：身强反赖印化杀（官${format(guanS)}印${format(yinS)}而食伤仅${format(shiS)}）——身愈强官愈弱，定是贫穷汉。"
            else -> null
        }
        val duanYu = buildString {
            append("日主${strength.chinese}")
            if (ruoJi) append("（弱极·${ruoJiType!!.chinese}）")
            if (qiangJi) append("（强极·${cn.xulun.bazi.model.JinBiaoWangJiType.YIN_BI_TONG_XIN.chinese}）")
            else if (wangJi) append("（旺极·${cn.xulun.bazi.model.JinBiaoWangJiType.BI_DANG_SHI.chinese}）")
            append("，${choice.way.chinese}，用神取${yongGod.chinese}（${yongEl.chinese}）")
            if (yongShenStars.isNotEmpty()) append("，见于${yongShenStars.joinToString("、")}")
            else append("——用神五行不现于原局，以虚神论")
            append("。用神${health.level.chinese}（${health.rootNote}）")
            if (health.damages.isNotEmpty()) append("，受${health.damages.size}处克泄耗")
            append("。")
            if (ruoJi) append("此为弱极从势之命，行比劫印地必凶（反常之判，勿以普通身弱喜扶论）。")
            if (ruoJi && !zhengDou) append("随势而走：走财走官走食伤皆顺皆吉（富贵显达、高人一等）；唯走印比帮身运帮之反害，必大凶或贫穷。")
            if (choice.way == JinBiaoQuYongWay.QU_GUA) append("此为去寡之命——不用之官尽可去：违背官杀之运好运（把官杀坏得越严重越好），顺着官杀之运一世无成；日主已旺，不可用水激之。")
            if (choice.way == JinBiaoQuYongWay.HE_LIU) {
                append("此为合留清格之命——有情即格局：留下之官杀清纯为用，有情就要富贵（当官）。")
                if (gender == cn.xulun.bazi.model.Gender.FEMALE) {
                    append("女命官星得用而清——官星要么代表国家、要么代表丈夫：本人在官方体制有职权，夫亦贵（官星双得之应）。")
                }
            }
            if (choice.way == JinBiaoQuYongWay.GONG_SHEN) {
                append("此为财党杀攻身之命——旺财党杀攻身、财先破印：身衰撑不住，非贫即夭（重病之属——肾病严重放弃治疗，身太衰）；丙寅之类印透化杀之运能稍安，然难救本。")
            }
            if (choice.way == JinBiaoQuYongWay.PEI_CAI) {
                append("财滋弱杀与财滋弱官不区分，是一样的（身强＋用官杀＋财生）。")
                if (caiZhuBing) append("此为财滋弱杀格（财被劫病局）：杀弱无关系、财弱也无妨——大运解${biEl.chinese}克${caiEl.chinese}之病（${shiEl.chinese}运通关）即一样发富发贵；杀强反不宜，要克身。")
                else append("此为身旺用官之命：帮助官的运（财生官、官杀得地）都算好运，违背官杀、制化官杀之运（食制、印化）就要差。")
                if (gender == cn.xulun.bazi.model.Gender.FEMALE) {
                    append("女命用官、官星得用——百分之百：要么在事业单位有官方工作，要么是家庭主妇（官星要么代表国家、要么代表丈夫），不可能在别的地方上班；官星得助，夫运家计应吉。")
                }
            }
            if (choice.way == JinBiaoQuYongWay.SHI_SHENG_CAI && yinS >= YIN_HUAI) {
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
            } else if (wangJi) {
                append("此为旺极专旺之命，好坏取决于大运：行比劫印绶大运即富贵自然显达（好八字）；行官杀犯旺当知凶祸即至；此格若行财地，立断九死一生。")
                if (yinS < YIN_QING) append("局中印轻，食伤顺泄而行也是好运。") else append("局中印重，走食伤运印夺食，不可行。")
            }
            if (jiYuns.isNotEmpty()) append("吉运：${jiYuns.joinToString("、")}。")
            if (xiongYuns.isNotEmpty()) {
                append("凶运：${xiongYuns.joinToString("、")}。")
                if (ruoJi) append("弱极行比劫印运勿看流年，必大凶无疑——轻则破大财，重则牢狱、死亡之灾（应事随八字五行而异）。")
            }
            if (tiHuanPin != null) append(tiHuanPin)
            when (qingZhuo) {
                JinBiaoQingZhuo.QING_KU -> append("此为清枯之命——用神被坏得太严重、一点救都没有：走好运的时候可能稍微好一点，能得个温饱；走克用神、生忌神之运（如东方木运克土生火）就要一事无成——贫穷清枯。")
                JinBiaoQingZhuo.QUAN_ZHUO -> append("此为全浊之命——官食财俱各有浊神克之，用神无着落：一事无成（学什么学不会、干什么都有人捣乱），比普通老百姓还差——今天干乙木的事没成，明天干庚金的事，就是这样一种状态。")
                JinBiaoQingZhuo.QING -> append("此为清命——用神有力、病神无（或已化已制）：贵命，走什么运都不怕，坏运来了也无病。")
                JinBiaoQingZhuo.MAN_ZHUO -> append("此为满盘浊气之命——病神之力太大、毁坏用神：差命穷命，走什么运也好不了。")
                JinBiaoQingZhuo.BAN_QING -> append("此为半浊半清之命——普通人之命：多成多败、好两年坏两年（帮用去病之运好，帮病之运差）。")
            }
        }

        return JinBiaoResult(
            kouJing = "盲派金镖门 · 用神中心论——帮扶用神则吉，耗泄克破用神则凶；十神代五行，立场以用神为中心",
            strength = strength,
            strengthBasis = "金镖门三步法：月令${monthBranch.chinese}经合会调整属${monthElAdj.chinese}党→" +
                (if (ruoZhongBianWang) "弱基调；然坐贴身印比、月令敌党之支被泄克转衰——弱中变旺（身旺）" else strength.chinese) +
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
        val cangRoots = bazi.branches.filter {
            it.element != yongEl && branchHiddenStems.getValue(it).any { hd -> hd.element == yongEl }
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
            else -> "无根虚浮"
        }

        val supports = mutableListOf<String>()
        val damages = mutableListOf<String>()

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
        // 用神透干被合绊
        bazi.stems.forEachIndexed { i, st ->
            val hePartner = GAN_HE[st]
            if (i != 2 && st.element == yongEl && hePartner != null &&
                bazi.stems.anyIndexed { j, o -> j != 2 && j != i && o == hePartner }
            ) {
                damages += "${st.chinese}用神透干被${hePartner.chinese}合绊"
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
        val yinZhong: Boolean,           // 印党力≥印轻线（地支食伤被枭夺）
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
    )

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
            val lnItems = ganZhiItems(chartBranches, ln.stem, ln.branch, ctx)
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
    ): List<JinBiaoStanceItem> {
        val ganEl = gan.element
        val zhiEl = zhiElOverride ?: zhi.element
        val bureauNote =
            if (zhiElOverride != null && zhiElOverride != zhi.element) "·${zhi.chinese}随连行方局以${zhiElOverride.chinese}论（帮身方向）" else ""

        // 案三专题先记：八字重地支——干生支而干在局无根，干力全化于支（壬水全生寅木，壬寅以寅论）；
        // 命一补丁：旺强极局之官干不化——权在一人不可触犯，直克即激怒（丙运死亡）
        if (ganEl != zhiEl && ganEl.generates == zhiEl && ganEl in ctx.rootlessEls &&
            !(ctx.wangJiFamily && ganEl == ctx.guanEl)
        ) {
            val (stance, act) = stanceAction(ctx.yongEl, zhiEl)
            val huaSha = huaShaNote(zhi, zhiEl, chartBranches, ctx)
            val note = huaSha?.second
                ?: ctx.daXiongText[zhiEl]
                ?: ctx.zhongPingText[zhiEl]
                ?: ctx.favorText[zhiEl]
            return listOf(
                JinBiaoStanceItem(
                    label = "干化于支",
                    chinese = "${gan.chinese}${zhi.chinese}",
                    element = zhiEl,
                    stance = stance,
                    action = "${gan.chinese}无根全生${zhi.chinese}——干化于支（八字重地支）·${act}" +
                        (note?.let { "·$it" } ?: "") + bureauNote,
                    favorOverride = huaSha?.first == true,
                    pingOverride = huaSha != null && huaSha.first == false,
                ),
            )
        }

        return listOf(ganEl to gan.chinese, zhiEl to zhi.chinese).mapIndexed { i, (el, chinese) ->
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
                    ctx.renZhi in chartBranches && zhi == LIU_CHONG[ctx.renZhi]
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
                val tianGong = i == 1 && zhi.element == ctx.caiEl &&
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
                // ② 食伤干泄可行（天干无印不夺——顺其旺势）；食伤支印重则枭夺（地支不能泄）
                val fanWang = ctx.wangJiFamily && i == 0 && el == ctx.guanEl
                val zhiDuo = ctx.wangJiFamily && i == 1 && el == ctx.shiEl && ctx.yinZhong
                val shunXieGan = ctx.wangJiFamily && i == 0 && el == ctx.shiEl
                val note = when {
                    heChong -> "运支${zhi.chinese}合去冲神${ctx.chongShen!!.chinese}——用神绝处逢生，彻底有情（把印绶救起来了）"
                    quRenBing -> "运支${zhi.chinese}冲去${ctx.biEl.chinese}刃之病——大运来药去掉这个病：学习好、财运好（就在好运上）"
                    haiSiQuHuo -> "运支亥冲局中巳（**亥巳逢冲——去火存金**）：官被冲去、${ctx.biEl.chinese}比劫复旺来克${ctx.caiEl.chinese}财——**老破财**（各种各样的事儿来捣乱：今儿挣一百，明儿破八十——虽有亥卯化木亦不作吉论）"
                    yinHaiFanShen -> "运支寅合局中亥（**寅亥化木**——合去坏印之${ctx.caiEl.chinese}财根）＋木生火帮${ctx.yinEl.chinese}印——**翻身**（三十年河东河西：坎离震兑四方各管三十年——北方差三十年，东方翻身农民把歌唱；走木生火运就要翻身了——福报在后面）"
                    tianGong -> "运支${zhi.chinese}填${ctx.caiEl.chinese}拱局（玄机暗里存——财局兑现）：**财气通门户应期——发大财＋一下就有子**（儿子也有出息——往后好运越来越好）"
                    fanWang -> "官干直克身——犯旺激怒（权在一人，不可触犯），大凶"
                    zhiDuo -> "地支食伤被印枭夺——地支不能泄，凶"
                    shunXieGan -> "天干泄秀（天干无${ctx.yinEl.chinese}不夺）——顺其旺势，可行"
                    yinJuBu -> "运支合补印局（${zhi.chinese}与局支合成${ctx.yinEl.chinese}局）——亥卯未拱局补成，用神得局大发"
                    biJianGen -> "运支${zhi.chinese}为透干比劫${ctx.biTouGan.joinToString("") { it.chinese }}之根（禄/长生）——比劫见根变强，复与局支拱${ctx.biEl.chinese}局——通关之神被焚（${ctx.biEl.chinese}旺则${ctx.shiEl.chinese}燥，燥${ctx.shiEl.chinese}不生${ctx.caiEl.chinese}）：比劫直接克掉用神${ctx.caiEl.chinese}——大凶（死亡级大祸）；若局有官星则官克比劫护财，不至于此（配置论）"
                    sanHeCheng -> "运支${zhi.chinese}填实${sanHeChengEl!!.chinese}三合局（原局拱合待成——局中两支早等着${zhi.chinese}来，${sanHeChengEl.chinese}旺了）——局神得势克掉用神，就要出事（伤官见官为祸百端）"
                    quJiShen && ctx.yongCai -> "运干${gan.chinese}官杀克透干比劫${ctx.jiShenTouGans.joinToString("") { j -> j.chinese }}——**官护财**（用财怕比劫夺财，首要找官：有官就没事，没官就不行——干护支凶仍以支论）"
                    quJiShen -> "运干${gan.chinese}克去透干忌神${ctx.jiShenTouGans.joinToString("") { j -> j.chinese }}——去忌神即去病，用神无克（去忌之运最好）"
                    huaSha != null -> huaSha.second
                    else -> ctx.daXiongText[el] ?: ctx.zhongPingText[el]
                        ?: (if (!(ctx.wangJiFamily && ctx.yinZhong && el == ctx.shiEl)) ctx.favorText[el] else null)
                }
                JinBiaoStanceItem(
                    label = if (i == 0) "运干" else "运支",
                    chinese = chinese,
                    element = el,
                    stance = stance,
                    action = act + (note?.let { "·$it" } ?: "") + (if (i == 1) bureauNote else ""),
                    favorOverride = huaSha?.first == true || shunXieGan || yinJuBu || heChong || quRenBing || yinHaiFanShen || tianGong,
                    pingOverride = huaSha != null && huaSha.first == false,
                    xiongOverride = fanWang || zhiDuo || sanHeCheng || biJianGen || haiSiQuHuo,
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

    private fun verdictOf(items: List<JinBiaoStanceItem>, ctx: YunCtx): JinBiaoVerdict = when {
        // 命一补丁：官干犯旺激怒／地支食伤被枭夺——作凶论（权在一人不可触犯）
        items.any { it.xiongOverride } -> JinBiaoVerdict.XIONG
        // 大凶优先（弱极行比劫印「地」、旺强极犯旺/行财——干支混见亦凶，勿以流年论）
        items.any { it.element in ctx.daXiongEls } -> JinBiaoVerdict.XIONG
        // 中平档：解围局官/食、化煞生印未合局
        items.any { it.pingOverride || it.element in ctx.zhongPingEls } -> JinBiaoVerdict.PING
        // 吉档：全帮扶，或顺泄/化煞合局之作吉覆写（作吉元素视同帮扶）
        items.all { it.stance == JinBiaoStance.BANG || it.favorOverride || it.element in ctx.favorEls } -> JinBiaoVerdict.JI
        // 案五十二分档（限伤官用劫档——用神为比劫）：克比劫＝出事大凶；纯泄耗比劫（毕竟没有被克）＝差而没什么大事，中平
        // 案五十六戒：用神被三会化无力——泄耗亦穷困潦倒（不作中平降档）
        items.all { it.stance == JinBiaoStance.SUN && it.element !in ctx.favorEls } ->
            if (ctx.yongJie && !ctx.yongWuLi && items.none { it.action.contains("克用神") }) JinBiaoVerdict.PING else JinBiaoVerdict.XIONG
        else -> JinBiaoVerdict.GAN_ZHI_FEN
    }

    private fun format(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.1f", v)
    private fun pct(v: Double): String = String.format("%.0f%%", v * 100)
}

/** List.forEachIndexed 的带索引版本变体（干合检查用）。 */
private inline fun <T> List<T>.anyIndexed(predicate: (Int, T) -> Boolean): Boolean {
    forEachIndexed { i, t -> if (predicate(i, t)) return true }
    return false
}
