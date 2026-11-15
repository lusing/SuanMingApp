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

    fun analyze(bazi: BaziResult, daYun: DaYunInfo?): JinBiaoResult {
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

        // ── 日主强弱（复用老引擎八步法，作定位参考） ──
        val strengthResult = StrengthEngine.analyze(bazi)
        val strength = strengthResult.verdict
        val aShare = strengthResult.aSide / (strengthResult.aSide + strengthResult.bSide)

        // ── 弱极判定（第三讲两型） ──
        val yinS = partyScore(bazi, yinEl)
        val biS = partyScore(bazi, biEl)
        val supportS = yinS + biS
        val supportRooted = hasBenQiRoot(bazi, yinEl) || hasBenQiRoot(bazi, biEl)
        val caiS = partyScore(bazi, caiEl)
        val guanS = partyScore(bazi, guanEl)
        val shiS = partyScore(bazi, shiEl)
        val attackerStrong = caiS >= FOLLOW_MIN_SCORE || guanS >= FOLLOW_MIN_SCORE

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
            supportSAdj.first < WEI_FU_MAX_SCORE && aShare < RUO_JI_SHARE && attackerStrong
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
            bazi.branches.forEach { br ->
                val hostYinBi = br.element == yinEl || br.element == biEl
                if (!hostYinBi) {
                    if (br.element == el) s += W_BEN_QI
                    else if (branchHiddenStems.getValue(br).any { it.element == el }) s += W_CANG_GAN
                }
            }
            val mb = bazi.monthPillar.branch
            if (mb.element == el && mb.element != yinEl && mb.element != biEl) s += W_YUE_LING
            return s
        }
        val caiSAdj = hostileScoreAdj(caiEl)
        val guanSAdj = hostileScoreAdj(guanEl)
        val shiSAdj = hostileScoreAdj(shiEl)
        val wangJi = strength == DayStrength.STRONG && guanSAdj == 0.0 && caiSAdj == 0.0 && shiSAdj == 0.0
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
            // 第四讲·正格五理③／⑥＋基线：身弱取用
            strength == DayStrength.WEAK -> when {
                guanS >= FOLLOW_MIN_SCORE -> Choice(
                    JinBiaoQuYongWay.HUA_SHA, yinEl,
                    buildString {
                        append("正格五理·身衰遇官煞，枭印乃可化：官党力 ${format(guanS)} 有力——必用印化杀生印出格局")
                        append(
                            if (yinS < FOLLOW_MIN_SCORE) "；印弱（党力${format(yinS)}）——印可配官煞，官杀生印为喜"
                            else "；印党力 ${format(yinS)} 得力可化",
                        )
                        if (yinS == 0.0) append("；局中印不现，以虚神论（运岁补入之时为应）")
                    },
                )
                shiS >= FOLLOW_MIN_SCORE -> Choice(
                    JinBiaoQuYongWay.FU_SHEN, yinEl,
                    "正格续理·身衰伤配印：身衰须帮身，食伤（党力 ${format(shiS)}）泄体——喜印星制服食伤且生身，两得其用（印党力 ${format(yinS)}），此为有格局",
                )
                else -> {
                    val useYin = yinS >= biS
                    Choice(
                        JinBiaoQuYongWay.FU_SHEN, if (useYin) yinEl else biEl,
                        "身弱扶身：印绶党力 ${format(yinS)}、比劫党力 ${format(biS)}——取" +
                            (if (useYin) "印绶（生身）" else "比劫（助身）") + "为用（有力者优先）",
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
                // 五理④制杀：官有本气根之真旺、而财不滋官（无财则官无可倚）——伤食显其威
                guanS >= GUAN_WANG && hasBenQiRoot(bazi, guanEl) && caiS < FOLLOW_MIN_SCORE && shiS >= FOLLOW_MIN_SCORE -> Choice(
                    JinBiaoQuYongWay.ZHI_SHA, shiEl,
                    "正格五理·身强遇官煞，伤食显其威：官党力 ${format(guanS)} 旺（有本气根）、财不滋官（财党力${format(caiS)}）——必用食伤制杀制官（官不一定不可以制）；食伤党力 ${format(shiS)}",
                )
                // 五理②用官（案八实证）：身旺官杀有根气（藏干有字）而财来生——官杀混杂克我反好，命主用官
                guanS >= FOLLOW_MIN_SCORE && caiS >= FOLLOW_MIN_SCORE -> Choice(
                    JinBiaoQuYongWay.PEI_CAI, guanEl,
                    "正格五理·官煞依乎财（案八）：身旺通根，官杀（党力${format(guanS)}）地支有根气、财（党力${format(caiS)}）透而生官——" +
                        "身旺官杀混杂来克我反好，又财来生，命主用官；走帮助官的运（财运、官杀运）都算好运，走违背官杀、制化官杀的运就要差",
                )
                yinS >= YIN_HUAI -> Choice(
                    JinBiaoQuYongWay.HUAI_YIN, caiEl,
                    "正格五理·印可配官煞（身旺印坏）：印党力 ${format(yinS)} 起坏作用——必用财星坏印（财党力 ${format(caiS)}）",
                )
                guanS >= FOLLOW_MIN_SCORE -> Choice(
                    JinBiaoQuYongWay.PEI_CAI, guanEl,
                    "正格五理·官煞依乎财：身旺官弱（官党力 ${format(guanS)}）用官——配财生官，有财就是好格局（财党力 ${format(caiS)}）",
                )
                shiS >= FOLLOW_MIN_SCORE -> Choice(
                    JinBiaoQuYongWay.SHI_SHENG_CAI, shiEl,
                    "正格续理·身旺食生财：身旺无官杀必泄体得衡——食伤泄秀生财（食伤党力 ${format(shiS)}、财党力 ${format(caiS)}）；" +
                        (if (caiS < FOLLOW_MIN_SCORE) "虽财不现，食伤够旺亦必发泄，也是好命" else "食神生出财来，命局得到平衡"),
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

        // ── 喜忌（第一讲：帮用=喜，损用=忌） ──
        val xiElements = Element.entries.filter { it.generates == yongEl || it == yongEl }
        val jiElements = Element.entries.filter { it.controls == yongEl || yongEl.generates == it || yongEl.controls == it }

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
            if (choice.way == JinBiaoQuYongWay.CAI_JIE_WEI) {
                put(guanEl, "解围局行此稍差无大碍")
                put(shiEl, "解围局行此稍差无大碍")
            }
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
            if (choice.way == JinBiaoQuYongWay.PEI_CAI) put(caiEl, "财生官——帮官之运，好运")
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
            // 五理②用官（案八）：制化官杀之运——违背官，差运
            if (choice.way == JinBiaoQuYongWay.PEI_CAI) {
                put(shiEl, "食伤制官——违背官杀，差运")
                put(yinEl, "印泄官化官——违背官杀，差运")
            }
        }
        // 案三专题先记：八字重地支——运干生运支而干在局无根（党力0）者，干力全化于支（壬水全生寅木，壬寅以寅论）
        val rootlessEls = setOf(caiEl, guanEl, shiEl, yinEl, biEl).filter { partyScore(bazi, it) == 0.0 }.toSet()
        val ctx = YunCtx(
            yongEl = yongEl, biEl = biEl, yinEl = yinEl, shiEl = shiEl, caiEl = caiEl, guanEl = guanEl,
            qiangJi = qiangJi, yinDeJu = yinDeJu,
            favorEls = favorNote.keys, zhongPingEls = zhongPingNote.keys, daXiongEls = daXiongNote.keys,
            rootlessEls = rootlessEls,
            daXiongText = daXiongNote, zhongPingText = zhongPingNote, favorText = favorNote,
        )
        // 案五补丁：大运连行方局——连续三步大运支成三会方局且方局五行为印比（帮身方向）时，
        // 局内之支皆随方局作印比论（未随南方火局连行——辛未亦帮身破败）；顺势方局（财官食方向）不改支性。
        val yunBranchesSeq = daYun?.yuns?.map { it.pillar.branch } ?: emptyList()
        val bureauOverride = mutableMapOf<Int, Element>()
        for (i in 0 until maxOf(0, yunBranchesSeq.size - 2)) {
            val trio = setOf(yunBranchesSeq[i], yunBranchesSeq[i + 1], yunBranchesSeq[i + 2])
            val juEl = SAN_HUI_JU.firstOrNull { (bs, _) -> trio.containsAll(bs) }?.second
            if (juEl != null && (juEl == biEl || juEl == yinEl)) {
                bureauOverride[i] = juEl
                bureauOverride[i + 1] = juEl
                bureauOverride[i + 2] = juEl
            }
        }
        val yunPings = daYun?.yuns?.mapIndexed { idx, yun ->
            pingYun(bazi.branches, yun.pillar, yun.startAge, yun.startYear, ctx, bureauOverride[idx])
        }

        // ── 断语（一律锚定用神，禁泛断） ──
        val jiYuns = yunPings.orEmpty().filter { it.verdict == JinBiaoVerdict.JI }.map { it.yunName }
        val xiongYuns = yunPings.orEmpty().filter { it.verdict == JinBiaoVerdict.XIONG }.map { it.yunName }
        // 第四讲·五理⑤：二者如替换，定是贫穷汉——原局结构即呈反用之象者
        val tiHuanPin: String? = when {
            !ruoJi && !wangJi && strength == DayStrength.WEAK &&
                shiS >= FOLLOW_MIN_SCORE && guanS >= FOLLOW_MIN_SCORE && yinS < 1.0 ->
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
            if (choice.way == JinBiaoQuYongWay.PEI_CAI) append("此为身旺用官之命：帮助官的运（财生官、官杀得地）都算好运，违背官杀、制化官杀之运（食制、印化）就要差。")
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
        }

        return JinBiaoResult(
            kouJing = "盲派金镖门 · 用神中心论——帮扶用神则吉，耗泄克破用神则凶；十神代五行，立场以用神为中心",
            strength = strength,
            strengthBasis = "八步法定强弱：生助合力 ${format(strengthResult.aSide)} 对 克泄耗合力 ${format(strengthResult.bSide)}（占比 ${pct(aShare)}）——${strengthResult.ranking}",
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
            duanYu = duanYu,
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
        val rootNote = when {
            benQiRoots.isNotEmpty() -> "通本气根（${benQiRoots.joinToString("、") { it.chinese }}）"
            cangRoots.isNotEmpty() -> "藏干根（${cangRoots.joinToString("、") { it.chinese }}）"
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

        // 根被冲
        bazi.branches.forEachIndexed { i, br ->
            val chongPartner = LIU_CHONG[br]
            if (chongPartner != null && br.element == yongEl &&
                bazi.branches.any { it == chongPartner }
            ) {
                damages += "${br.chinese}用神之根被${chongPartner.chinese}冲"
            }
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
            damages.isEmpty() -> JinBiaoHealth.DE_LI
            else -> JinBiaoHealth.DAI_SHANG
        }
        return JinBiaoYongShenHealth(level = level, rootNote = rootNote, supports = supports, damages = damages)
    }

    /** 运评上下文（案三补丁：干化于支／化煞生印／财地软档）。 */
    private class YunCtx(
        val yongEl: Element,
        val biEl: Element,
        val yinEl: Element,
        val shiEl: Element,
        val caiEl: Element,
        val guanEl: Element,
        val qiangJi: Boolean,
        val yinDeJu: Boolean,
        val favorEls: Set<Element>,      // 作吉论之五行（旺极印轻食伤顺泄）
        val zhongPingEls: Set<Element>,  // 作中平论之五行（解围局官/食）
        val daXiongEls: Set<Element>,    // 作大凶论之五行（弱极比印/旺强极官财）
        val rootlessEls: Set<Element>,   // 局中党力为零之五行（干生支则化于支）
        val daXiongText: Map<Element, String>,
        val zhongPingText: Map<Element, String>,
        val favorText: Map<Element, String>,
    )

    /** 单步大运评断（含逐年流年；流年同用大运之档位口径，细分待壬寅专题）。[zhiBureauEl]＝连行方局之印比覆写。 */
    private fun pingYun(
        chartBranches: List<EarthlyBranch>,
        pillar: Pillar,
        startAge: Int,
        startYear: Int,
        ctx: YunCtx,
        zhiBureauEl: Element? = null,
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
            verdict = verdictOf(items, ctx),
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

        // 案三专题先记：八字重地支——干生支而干在局无根，干力全化于支（壬水全生寅木，壬寅以寅论）
        if (ganEl != zhiEl && ganEl.generates == zhiEl && ganEl in ctx.rootlessEls) {
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
                val huaSha = if (el == ctx.guanEl) huaShaNote(zhi, el, chartBranches, ctx, isStem = i == 0) else null
                val note = huaSha?.second
                    ?: ctx.daXiongText[el]
                    ?: ctx.zhongPingText[el]
                    ?: ctx.favorText[el]
                JinBiaoStanceItem(
                    label = if (i == 0) "运干" else "运支",
                    chinese = chinese,
                    element = el,
                    stance = stance,
                    action = act + (note?.let { "·$it" } ?: "") + (if (i == 1) bureauNote else ""),
                    favorOverride = huaSha?.first == true,
                    pingOverride = huaSha != null && huaSha.first == false,
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
        // 大凶优先（弱极行比劫印「地」、旺强极犯旺/行财——干支混见亦凶，勿以流年论）
        items.any { it.element in ctx.daXiongEls } -> JinBiaoVerdict.XIONG
        // 中平档：解围局官/食、化煞生印未合局
        items.any { it.pingOverride || it.element in ctx.zhongPingEls } -> JinBiaoVerdict.PING
        // 吉档：全帮扶，或顺泄/化煞合局之作吉覆写（作吉元素视同帮扶）
        items.all { it.stance == JinBiaoStance.BANG || it.favorOverride || it.element in ctx.favorEls } -> JinBiaoVerdict.JI
        items.all { it.stance == JinBiaoStance.SUN } -> JinBiaoVerdict.XIONG
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
