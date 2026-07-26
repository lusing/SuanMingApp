package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.GeJuChengBai
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.PillarBranchLabels
import cn.xulun.bazi.model.PillarStemLabels
import cn.xulun.bazi.model.TenGod
import cn.xulun.bazi.model.XiJi
import cn.xulun.bazi.model.XiJiItem
import cn.xulun.bazi.model.YongShenSource
import cn.xulun.bazi.model.YongShenStar
import cn.xulun.bazi.model.branchHiddenStems
import cn.xulun.bazi.model.tenGodOfStem

/**
 * 子平真诠（清·沈孝瞻）「月令格局派」用神 / 相神 / 成败分析器。
 *
 * 与现代扶抑/平衡派截然不同：**用神 = 月令所藏之神**（透干→会支→本气，不据旺衰选取）；
 * **相神**为护卫用神、使格局成立的关键字（"凡全局之格，赖此一字而成者，均谓之相也"）；
 * 据顺逆总纲（善神财官印食顺用生护，不善神杀伤刃劫逆用制化）与《论用神成败救应》判定成格/败格。
 *
 * 用神/格局/身强弱复用 [GeJuCalculator.determine]（透干→本气，子平真诠取用主轴），
 * 地支冲合刑复用 [DiZhiRelationCalculator]。本类专注相神、成败、救应、顺逆断语。
 */
object YongShenCalculator {

    private val hiddenPositions = listOf("本气", "中气", "余气")
    /** 年/月/时干柱序（跳过日柱 index=2） */
    private val touGanIndexes = listOf(0, 1, 3)

    /** 天干五合（用于判相神被合）：甲己 乙庚 丙辛 丁壬 戊癸 */
    private val hePartner: Map<HeavenlyStem, HeavenlyStem> = mapOf(
        HeavenlyStem.JIA to HeavenlyStem.JI, HeavenlyStem.JI to HeavenlyStem.JIA,
        HeavenlyStem.YI to HeavenlyStem.GENG, HeavenlyStem.GENG to HeavenlyStem.YI,
        HeavenlyStem.BING to HeavenlyStem.XIN, HeavenlyStem.XIN to HeavenlyStem.BING,
        HeavenlyStem.DING to HeavenlyStem.REN, HeavenlyStem.REN to HeavenlyStem.DING,
        HeavenlyStem.WU to HeavenlyStem.GUI, HeavenlyStem.GUI to HeavenlyStem.WU,
    )

    fun analyze(bazi: BaziResult): cn.xulun.bazi.model.YongShenResult {
        val dayMaster = bazi.dayPillar.stem
        val geju = GeJuCalculator.determine(bazi)
        val monthBranch = bazi.monthPillar.branch
        val strength = geju.strength

        val isYangRen = geju.name == "阳刃格"
        val isJianLu = geju.tenGod == TenGod.BI_JIAN
        val isYueJie = geju.tenGod == TenGod.JIE_CAI && !isYangRen

        // 1. 用神定位
        val (yongShen, source, yongShenBasis) = locateYongShen(bazi, geju, dayMaster, monthBranch, isYangRen, isJianLu, isYueJie)
        val yongGod = yongShen?.tenGod ?: geju.tenGod

        // 2. 扫描原局十神（透干 + 藏支）
        val touGanStars = scanTouGan(bazi, dayMaster)
        val cangStars = scanCang(bazi, dayMaster)
        val touGanGods = touGanStars.map { it.tenGod }.toSet()

        // 3. 相神（透干优先 → 藏支；主相神 → 备相神）
        val (xiang, xiangRole, xiangBasis) = locateXiangShen(yongGod, isYangRen, touGanStars, cangStars)

        // 4. 成败
        val rel = DiZhiRelationCalculator.analyze(bazi)
        val (chengBai, reason) = evaluateChengBai(
            yongGod, geju.name, isYangRen, monthBranch, bazi, dayMaster,
            xiang, xiangRole, touGanGods, touGanStars + cangStars, rel, strength,
        )

        // 5. 喜忌
        val xiJi = buildXiJi(yongGod, isYangRen, isJianLu || isYueJie)

        // 6. 断语
        val duanYu = buildDuanYu(geju.name, yongGod, isYangRen, isJianLu || isYueJie, xiang, xiangRole, chengBai, strength)

        return cn.xulun.bazi.model.YongShenResult(
            yongShen = yongShen,
            geJuName = geju.name,
            source = source,
            yongShenBasis = yongShenBasis,
            xiangShen = xiang,
            xiangShenRole = xiangRole,
            xiangShenBasis = xiangBasis,
            chengBai = chengBai,
            chengBaiReason = reason,
            xiJi = xiJi,
            duanYu = duanYu,
            strength = strength,
        )
    }

    // ── 用神定位 ──────────────────────────────────────────
    private fun locateYongShen(
        bazi: BaziResult,
        geju: cn.xulun.bazi.model.GeJuResult,
        dayMaster: HeavenlyStem,
        monthBranch: EarthlyBranch,
        isYangRen: Boolean,
        isJianLu: Boolean,
        isYueJie: Boolean,
    ): Triple<YongShenStar?, YongShenSource, String> {
        val benQi = branchHiddenStems.getValue(monthBranch).first()

        if (isYangRen) {
            return Triple(
                YongShenStar(benQi, geju.tenGod, "月支·本气", hidden = true),
                YongShenSource.BEN_QI,
                "月令${monthBranch.chinese}为日主${dayMaster.chinese}之阳刃（本气${benQi.chinese}为劫财），不善神逆用",
            )
        }
        if (isJianLu || isYueJie) {
            // 月令为比劫，无外用神 → 于他柱透干寻财官煞食为用（优先官杀，次财，次食伤）
            val touGan = scanTouGan(bazi, dayMaster)
            val order = listOf(TenGod.ZHENG_GUAN, TenGod.QI_SHA, TenGod.ZHENG_CAI, TenGod.PIAN_CAI, TenGod.SHI_SHEN, TenGod.SHANG_GUAN)
            val found = order.firstOrNull { g -> touGan.any { it.tenGod == g } }?.let { g -> touGan.first { it.tenGod == g } }
            return if (found != null) {
                Triple(
                    found, YongShenSource.OTHER,
                    "${geju.name}：月令${monthBranch.chinese}为比劫，本身不可为用；于${found.location}透${found.stem.chinese}（${found.tenGod.chinese}）取为用，复配相神",
                )
            } else {
                Triple(null, YongShenSource.OTHER, "${geju.name}：月令为比劫无外用神，他柱亦无财官煞食透干，格偏")
            }
        }

        // 正格：透干 → 本气
        if (geju.touGanStem != null) {
            val loc = touGanLocationOf(bazi, geju.touGanStem)
            return Triple(
                YongShenStar(geju.touGanStem, geju.tenGod, loc, hidden = false),
                YongShenSource.TOU_GAN,
                geju.basis,
            )
        }
        return Triple(
            YongShenStar(benQi, geju.tenGod, "月支·本气", hidden = true),
            YongShenSource.BEN_QI,
            geju.basis,
        )
    }

    // ── 十神扫描（仿 StudyCalculator.findStars） ──────────
    private fun scanTouGan(bazi: BaziResult, dayMaster: HeavenlyStem): List<YongShenStar> =
        touGanIndexes.map { i ->
            val stem = bazi.pillars[i].stem
            YongShenStar(stem, tenGodOfStem(dayMaster, stem), PillarStemLabels[i], hidden = false)
        }

    private fun scanCang(bazi: BaziResult, dayMaster: HeavenlyStem): List<YongShenStar> {
        val r = mutableListOf<YongShenStar>()
        bazi.branches.forEachIndexed { i, branch ->
            branchHiddenStems.getValue(branch).forEachIndexed { hi, stem ->
                val pos = hiddenPositions.getOrElse(hi) { "藏" }
                r.add(YongShenStar(stem, tenGodOfStem(dayMaster, stem), "${PillarBranchLabels[i]}·$pos", hidden = true))
            }
        }
        return r
    }

    private fun touGanLocationOf(bazi: BaziResult, stem: HeavenlyStem): String =
        touGanIndexes.filter { bazi.pillars[it].stem == stem }.joinToString("、") { PillarStemLabels[it] }

    // ── 相神 ──────────────────────────────────────────────
    private data class XiangSpec(
        val primary: Set<TenGod>,
        val backup: Set<TenGod>,
        val roleOf: (TenGod) -> String,
    )

    private fun xiangSpec(yongGod: TenGod, isYangRen: Boolean): XiangSpec? = when {
        isYangRen -> XiangSpec(
            primary = setOf(TenGod.ZHENG_GUAN, TenGod.QI_SHA),
            backup = setOf(TenGod.ZHENG_CAI, TenGod.PIAN_CAI, TenGod.ZHENG_YIN, TenGod.PIAN_YIN),
            roleOf = { if (it == TenGod.ZHENG_GUAN || it == TenGod.QI_SHA) "官杀制刃" else "财印辅官杀" },
        )
        yongGod == TenGod.ZHENG_GUAN -> XiangSpec(
            setOf(TenGod.ZHENG_CAI, TenGod.PIAN_CAI), setOf(TenGod.ZHENG_YIN, TenGod.PIAN_YIN),
        ) { if (it == TenGod.ZHENG_CAI || it == TenGod.PIAN_CAI) "财生官" else "印护官" }
        yongGod == TenGod.QI_SHA -> XiangSpec(
            setOf(TenGod.SHI_SHEN), setOf(TenGod.ZHENG_YIN, TenGod.PIAN_YIN),
        ) { if (it == TenGod.SHI_SHEN) "食神制杀" else "印化杀·杀印相生" }
        yongGod == TenGod.ZHENG_CAI || yongGod == TenGod.PIAN_CAI -> XiangSpec(
            setOf(TenGod.SHI_SHEN, TenGod.SHANG_GUAN), setOf(TenGod.ZHENG_GUAN, TenGod.QI_SHA),
        ) { if (it == TenGod.SHI_SHEN || it == TenGod.SHANG_GUAN) "食伤生财" else "官杀护财" }
        yongGod == TenGod.ZHENG_YIN || yongGod == TenGod.PIAN_YIN -> XiangSpec(
            primary = setOf(TenGod.QI_SHA, TenGod.ZHENG_GUAN),
            backup = setOf(TenGod.BI_JIAN, TenGod.JIE_CAI),
        ) { if (it == TenGod.QI_SHA || it == TenGod.ZHENG_GUAN) "杀印相生" else "比劫护印" }
        yongGod == TenGod.SHI_SHEN -> XiangSpec(
            setOf(TenGod.ZHENG_CAI, TenGod.PIAN_CAI), setOf(TenGod.QI_SHA),
        ) { if (it == TenGod.ZHENG_CAI || it == TenGod.PIAN_CAI) "食神生财" else "食神带杀（弃食就杀印）" }
        yongGod == TenGod.SHANG_GUAN -> XiangSpec(
            setOf(TenGod.ZHENG_CAI, TenGod.PIAN_CAI), setOf(TenGod.ZHENG_YIN, TenGod.PIAN_YIN),
        ) { if (it == TenGod.ZHENG_CAI || it == TenGod.PIAN_CAI) "伤官生财" else "伤官佩印" }
        else -> null
    }

    private fun locateXiangShen(
        yongGod: TenGod,
        isYangRen: Boolean,
        touGanStars: List<YongShenStar>,
        cangStars: List<YongShenStar>,
    ): Triple<YongShenStar?, String, String> {
        val spec = xiangSpec(yongGod, isYangRen)
            ?: return Triple(null, "", "用神未定或月令比劫格偏，无对口相神")
        // 透干优先（主→备），藏支次之
        touGanStars.firstOrNull { it.tenGod in spec.primary }?.let {
            return Triple(it, spec.roleOf(it.tenGod), "${it.location}透${it.stem.chinese}（${it.tenGod.chinese}），${spec.roleOf(it.tenGod)}为相")
        }
        touGanStars.firstOrNull { it.tenGod in spec.backup }?.let {
            return Triple(it, spec.roleOf(it.tenGod), "${it.location}透${it.stem.chinese}（${it.tenGod.chinese}），${spec.roleOf(it.tenGod)}为相")
        }
        cangStars.firstOrNull { it.tenGod in spec.primary }?.let {
            return Triple(it, spec.roleOf(it.tenGod), "${it.location}藏${it.stem.chinese}（${it.tenGod.chinese}），${spec.roleOf(it.tenGod)}为相（藏支未透，力弱）")
        }
        cangStars.firstOrNull { it.tenGod in spec.backup }?.let {
            return Triple(it, spec.roleOf(it.tenGod), "${it.location}藏${it.stem.chinese}（${it.tenGod.chinese}），${spec.roleOf(it.tenGod)}为相（藏支未透，力弱）")
        }
        return Triple(null, "", "原局无得力相神，格欠清纯")
    }

    // ── 成败（《论用神成败救应》） ────────────────────────
    private class ChengTiao(val pred: Boolean, val desc: String)
    private class BaiTiao(val pred: Boolean, val desc: String, val jiu: Boolean = false, val jiuDesc: String = "")

    @Suppress("UNUSED_PARAMETER")
    private fun evaluateChengBai(
        yongGod: TenGod,
        geJuName: String,
        isYangRen: Boolean,
        monthBranch: EarthlyBranch,
        bazi: BaziResult,
        dayMaster: HeavenlyStem,
        xiang: YongShenStar?,
        xiangRole: String,
        touGanGods: Set<TenGod>,
        allStars: List<YongShenStar>,
        rel: cn.xulun.bazi.model.DiZhiRelationResult,
        @Suppress("UNUSED_PARAMETER") strength: DayStrength,
    ): Pair<GeJuChengBai, String> {
        val tou = touGanGods
        fun t(g: TenGod) = g in tou
        val monthChong = rel.chongList.any { it.branch1 == monthBranch || it.branch2 == monthBranch }

        val chengTiao = mutableListOf<ChengTiao>()
        val baiTiao = mutableListOf<BaiTiao>()

        when {
            isYangRen -> {
                chengTiao += ChengTiao(t(TenGod.ZHENG_GUAN) || t(TenGod.QI_SHA), "透官或杀制刃")
                chengTiao += ChengTiao(t(TenGod.ZHENG_CAI) || t(TenGod.PIAN_CAI) || t(TenGod.ZHENG_YIN) || t(TenGod.PIAN_YIN), "露财或印辅官杀")
                baiTiao += BaiTiao(!t(TenGod.ZHENG_GUAN) && !t(TenGod.QI_SHA), "阳刃无官杀制伏")
                baiTiao += BaiTiao(t(TenGod.SHANG_GUAN), "刃逢伤官")
            }
            yongGod == TenGod.ZHENG_GUAN -> {
                chengTiao += ChengTiao(t(TenGod.ZHENG_CAI) || t(TenGod.PIAN_CAI), "财透生官")
                chengTiao += ChengTiao(t(TenGod.ZHENG_YIN) || t(TenGod.PIAN_YIN), "印透护官")
                baiTiao += BaiTiao(
                    t(TenGod.SHANG_GUAN), "伤官见官",
                    jiu = t(TenGod.ZHENG_YIN) || t(TenGod.PIAN_YIN), jiuDesc = "透印制伤护官",
                )
                baiTiao += BaiTiao(t(TenGod.QI_SHA), "官杀混杂")
                baiTiao += BaiTiao(monthChong, "官星（月令）被冲")
            }
            yongGod == TenGod.QI_SHA -> {
                chengTiao += ChengTiao(t(TenGod.SHI_SHEN), "食神制杀")
                chengTiao += ChengTiao(t(TenGod.ZHENG_YIN) || t(TenGod.PIAN_YIN), "印化杀·杀印相生")
                // 财党杀，惟无制无化方为败
                baiTiao += BaiTiao(
                    (t(TenGod.ZHENG_CAI) || t(TenGod.PIAN_CAI)) && !t(TenGod.SHI_SHEN) && !t(TenGod.ZHENG_YIN) && !t(TenGod.PIAN_YIN),
                    "财党杀攻身，无制无化",
                )
            }
            yongGod == TenGod.ZHENG_CAI || yongGod == TenGod.PIAN_CAI -> {
                chengTiao += ChengTiao(t(TenGod.SHI_SHEN) || t(TenGod.SHANG_GUAN), "食伤生财")
                chengTiao += ChengTiao(t(TenGod.ZHENG_GUAN) || t(TenGod.QI_SHA), "官杀护财")
                baiTiao += BaiTiao(
                    t(TenGod.BI_JIAN) || t(TenGod.JIE_CAI), "比劫夺财",
                    jiu = t(TenGod.SHI_SHEN) || t(TenGod.SHANG_GUAN) || t(TenGod.ZHENG_GUAN) || t(TenGod.QI_SHA),
                    jiuDesc = "透食伤化劫或官杀制劫",
                )
                baiTiao += BaiTiao((t(TenGod.ZHENG_CAI) || t(TenGod.PIAN_CAI)) && t(TenGod.QI_SHA) && !t(TenGod.SHI_SHEN), "财透七杀党杀攻身")
            }
            yongGod == TenGod.ZHENG_YIN || yongGod == TenGod.PIAN_YIN -> {
                chengTiao += ChengTiao(t(TenGod.QI_SHA), "杀印相生")
                chengTiao += ChengTiao(t(TenGod.ZHENG_GUAN), "官印双全")
                baiTiao += BaiTiao(
                    t(TenGod.ZHENG_CAI) || t(TenGod.PIAN_CAI), "贪财坏印",
                    jiu = t(TenGod.BI_JIAN) || t(TenGod.JIE_CAI), jiuDesc = "比劫制财护印",
                )
            }
            yongGod == TenGod.SHI_SHEN -> {
                chengTiao += ChengTiao(t(TenGod.ZHENG_CAI) || t(TenGod.PIAN_CAI), "食神生财")
                baiTiao += BaiTiao(
                    t(TenGod.PIAN_YIN), "枭神夺食",
                    jiu = t(TenGod.ZHENG_CAI) || t(TenGod.PIAN_CAI) || t(TenGod.QI_SHA),
                    jiuDesc = "生财护食或弃食就杀",
                )
            }
            yongGod == TenGod.SHANG_GUAN -> {
                chengTiao += ChengTiao(t(TenGod.ZHENG_CAI) || t(TenGod.PIAN_CAI), "伤官生财")
                chengTiao += ChengTiao(t(TenGod.ZHENG_YIN) || t(TenGod.PIAN_YIN), "伤官佩印")
                // 伤官见官：金水伤官（金日主生于冬）调候可见官，不忌
                val jinShui = dayMaster.element == Element.METAL &&
                    monthBranch in setOf(EarthlyBranch.HAI, EarthlyBranch.ZI, EarthlyBranch.CHOU)
                baiTiao += BaiTiao(
                    t(TenGod.ZHENG_GUAN) && !jinShui, "伤官见官",
                    jiu = t(TenGod.ZHENG_YIN) || t(TenGod.PIAN_YIN), jiuDesc = "佩印制伤，官不为祸",
                )
            }
            else -> {
                // 月令比劫且他柱无财官煞食透干（格偏）：无成无败
            }
        }

        // 相神健全度：相神被冲/合则立败（"相神相伤，立败其格"）
        var xiangBrokenDesc: String? = null
        if (xiang != null) {
            xiangBrokenDesc = xiangBrokenReason(xiang, bazi, rel)
            if (xiangBrokenDesc != null) baiTiao += BaiTiao(true, xiangBrokenDesc)
        }

        // 决断
        val effectiveBai = baiTiao.filter { it.pred && !it.jiu }
        val neutralizedBai = baiTiao.filter { it.pred && it.jiu }
        val chengFromXiang = xiang != null && xiangBrokenDesc == null
        val hasCheng = chengTiao.any { it.pred } || chengFromXiang
        val chengDescs = (chengTiao.filter { it.pred }.map { it.desc } +
            (if (chengFromXiang && xiangRole.isNotEmpty()) listOf("${xiangRole}·相神健全") else emptyList())).distinct()

        val parts = mutableListOf<String>()
        if (chengDescs.isNotEmpty()) parts += "成：${chengDescs.joinToString("、")}"
        if (effectiveBai.isNotEmpty()) parts += "败：${effectiveBai.joinToString("、") { it.desc }}"
        if (neutralizedBai.isNotEmpty()) parts += "救应：${neutralizedBai.joinToString("、") { "${it.desc}（${it.jiuDesc}）" }}"

        val reason = if (parts.isEmpty()) "无相神成格、亦无破神败格" else parts.joinToString("；")
        val chengBai = when {
            effectiveBai.isEmpty() && hasCheng -> GeJuChengBai.CHENG
            effectiveBai.isEmpty() && !hasCheng && neutralizedBai.isNotEmpty() -> GeJuChengBai.BAI_ZHONG_CHENG
            effectiveBai.isEmpty() && !hasCheng -> GeJuChengBai.PIAN
            effectiveBai.isNotEmpty() && hasCheng -> GeJuChengBai.CHENG_ZHONG_BAI
            else -> GeJuChengBai.BAI
        }
        return chengBai to reason
    }

    /** 相神被冲/合的败因；健全返回 null */
    private fun xiangBrokenReason(
        xiang: YongShenStar,
        bazi: BaziResult,
        rel: cn.xulun.bazi.model.DiZhiRelationResult,
    ): String? {
        if (xiang.hidden) {
            // 藏支相神：仅查所在支被冲
            val br = bazi.branches.firstOrNull { branchHiddenStems.getValue(it).contains(xiang.stem) } ?: return null
            return if (rel.chongList.any { it.branch1 == br || it.branch2 == br }) "相神${xiang.stem.chinese}（藏${br.chinese}）被冲" else null
        }
        val reasons = mutableListOf<String>()
        val idx = touGanIndexes.firstOrNull { bazi.pillars[it].stem == xiang.stem }
        if (idx != null) {
            val xiangBranch = bazi.pillars[idx].branch
            if (rel.chongList.any { it.branch1 == xiangBranch || it.branch2 == xiangBranch }) {
                reasons += "相神${xiang.stem.chinese}坐支${xiangBranch.chinese}被冲"
            }
        }
        val partner = hePartner[xiang.stem]
        if (partner != null && touGanIndexes.any { bazi.pillars[it].stem == partner }) {
            reasons += "相神${xiang.stem.chinese}逢${partner.chinese}合"
        }
        return if (reasons.isEmpty()) null else reasons.joinToString("；")
    }

    // ── 喜忌（运岁参考） ─────────────────────────────────
    private fun buildXiJi(yongGod: TenGod, isYangRen: Boolean, isJianLuYueJie: Boolean): List<XiJiItem> {
        if (isYangRen) return listOf(
            XiJiItem(XiJi.XI, TenGod.ZHENG_GUAN, "官星制刃"),
            XiJiItem(XiJi.XI, TenGod.QI_SHA, "七杀制刃"),
            XiJiItem(XiJi.JI, TenGod.SHANG_GUAN, "伤官泄刃生非"),
        )
        if (isJianLuYueJie) return listOf(
            XiJiItem(XiJi.XI, TenGod.ZHENG_CAI, "财星为用"),
            XiJiItem(XiJi.XI, TenGod.ZHENG_GUAN, "官星为用"),
            XiJiItem(XiJi.JI, TenGod.BI_JIAN, "比劫助身夺财"),
        )
        return when (yongGod) {
            TenGod.ZHENG_GUAN -> listOf(
                XiJiItem(XiJi.XI, TenGod.ZHENG_CAI, "财生官"), XiJiItem(XiJi.XI, TenGod.ZHENG_YIN, "印护官"),
                XiJiItem(XiJi.JI, TenGod.SHANG_GUAN, "伤官见官"), XiJiItem(XiJi.JI, TenGod.QI_SHA, "官杀混杂"),
            )
            TenGod.QI_SHA -> listOf(
                XiJiItem(XiJi.XI, TenGod.SHI_SHEN, "食制杀"), XiJiItem(XiJi.XI, TenGod.ZHENG_YIN, "印化杀"),
                XiJiItem(XiJi.JI, TenGod.ZHENG_CAI, "财党杀"),
            )
            TenGod.ZHENG_CAI, TenGod.PIAN_CAI -> listOf(
                XiJiItem(XiJi.XI, TenGod.SHI_SHEN, "食伤生财"), XiJiItem(XiJi.XI, TenGod.ZHENG_GUAN, "官杀护财"),
                XiJiItem(XiJi.JI, TenGod.BI_JIAN, "比劫夺财"),
            )
            TenGod.ZHENG_YIN, TenGod.PIAN_YIN -> listOf(
                XiJiItem(XiJi.XI, TenGod.QI_SHA, "杀印相生"), XiJiItem(XiJi.XI, TenGod.ZHENG_GUAN, "官印双全"),
                XiJiItem(XiJi.JI, TenGod.ZHENG_CAI, "贪财坏印"),
            )
            TenGod.SHI_SHEN -> listOf(
                XiJiItem(XiJi.XI, TenGod.ZHENG_CAI, "食神生财"),
                XiJiItem(XiJi.JI, TenGod.PIAN_YIN, "枭神夺食"),
            )
            TenGod.SHANG_GUAN -> listOf(
                XiJiItem(XiJi.XI, TenGod.ZHENG_CAI, "伤官生财"), XiJiItem(XiJi.XI, TenGod.ZHENG_YIN, "伤官佩印"),
                XiJiItem(XiJi.JI, TenGod.ZHENG_GUAN, "伤官见官"),
            )
            else -> listOf(XiJiItem(XiJi.XI, TenGod.ZHENG_CAI, "财官为用"), XiJiItem(XiJi.JI, TenGod.BI_JIAN, "比劫"))
        }
    }

    // ── 综合断语 ──────────────────────────────────────────
    private fun buildDuanYu(
        geJuName: String,
        yongGod: TenGod,
        isYangRen: Boolean,
        isJianLuYueJie: Boolean,
        xiang: YongShenStar?,
        xiangRole: String,
        chengBai: GeJuChengBai,
        strength: DayStrength,
    ): String {
        val shunNi = when {
            isYangRen -> "逆用"
            yongGod == TenGod.QI_SHA || yongGod == TenGod.SHANG_GUAN -> "逆用（制化）"
            else -> "顺用（生护）"
        }
        val xiangPart = if (xiang != null && xiangRole.isNotEmpty()) "，相神${xiang.stem.chinese}（${xiangRole}）" else "，无得力相神、格欠清纯"
        val chengPart = when (chengBai) {
            GeJuChengBai.CHENG -> "，相神健全、格局成"
            GeJuChengBai.CHENG_ZHONG_BAI -> "，然带忌破字，成中有败"
            GeJuChengBai.BAI_ZHONG_CHENG -> "，本败得救应而反成"
            GeJuChengBai.BAI -> "，破字当头、格局败"
            GeJuChengBai.PIAN -> "，无相无破、格偏"
        }
        val jianLuPart = if (isJianLuYueJie) "（月令比劫，于他柱寻用）" else ""
        return "${geJuName}，${shunNi}${xiangPart}${chengPart}，日主${strength.chinese}${jianLuPart}。"
    }
}
