package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.BingYaoResult
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.CongGeKind
import cn.xulun.bazi.model.DayStrength
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.GodCategory
import cn.xulun.bazi.model.TiaoHouState
import cn.xulun.bazi.model.YongShenMethod
import cn.xulun.bazi.model.branchHiddenStems
import kotlin.math.min

/**
 * 病药取用（传统扶抑法）：通关、扶抑、顺势、调候。理论见 `.shengke-reference.md` 第卅九~四一节。
 *
 * 与子平真诠用神（[YongShenCalculator]）是两派并行：本器以「病药」为纲——
 * 找最关键之病（致病十神大类），对症下药（用神大类），通关为扶抑之特殊形式。
 *
 * 力量模型（粗称，待八步法引擎升级替换）：
 * - 干 1、支本气 1、月令本气再 +2（共 3）；
 * - 克身（官杀）权重 1.3、泄身（食伤）权重 0.8——克重于泄；
 * - 印有根时通关转移：官杀之力经印部分转为生身（×0.6，受印力上限约束）；
 * - 比劫透干 ≥3 为党众聚势（+1）。
 */
object BingYaoCalculator {

    private const val GUAN_W = 1.3
    private const val SHI_W = 0.8
    private const val TRANSFER_K = 0.6
    private const val CLIMATE_K = 0.9
    private const val STRONG_TH = 1.2

    /** 十神大类相对日主五行的五行。 */
    fun elementOf(category: GodCategory, dayEl: Element): Element = when (category) {
        GodCategory.BI -> dayEl
        GodCategory.YIN -> Element.entries.first { it.generates == dayEl }
        GodCategory.SHI -> dayEl.generates
        GodCategory.CAI -> dayEl.controls
        GodCategory.GUAN -> Element.entries.first { it.controls == dayEl }
    }

    /** 寒暖燥湿折减系数：寒月（亥子丑）土木金火受冻、燥月（巳午未）水金木受灼；免：冬水、夏火土。 */
    private fun climatePenalty(bazi: BaziResult, dayEl: Element): Double {
        val m = bazi.monthPillar.branch
        return when (m) {
            EarthlyBranch.HAI, EarthlyBranch.ZI, EarthlyBranch.CHOU ->
                if (dayEl == Element.WATER) 1.0 else CLIMATE_K
            EarthlyBranch.SI, EarthlyBranch.WU, EarthlyBranch.WEI ->
                if (dayEl == Element.FIRE || dayEl == Element.EARTH) 1.0 else CLIMATE_K
            else -> 1.0
        }
    }

    /** 五行之力：干 1、支本气 1、月令本气再 +2。 */
    fun forces(bazi: BaziResult): Map<Element, Double> {
        val f = Element.entries.associateWith { 0.0 }.toMutableMap()
        bazi.stems.forEach { f[it.element] = f[it.element]!! + 1.0 }
        bazi.branches.forEach { f[it.element] = f[it.element]!! + 1.0 }
        f[bazi.monthPillar.branch.element] = f[bazi.monthPillar.branch.element]!! + 2.0
        return f
    }

    /** 根加分：本气根 +2，仅藏干根 +0.5，无根 0。 */
    private fun rootBonus(bazi: BaziResult, el: Element): Double = when {
        bazi.branches.any { it.element == el } -> 2.0
        bazi.branches.any { br -> branchHiddenStems[br]!!.any { it.element == el } } -> 0.5
        else -> 0.0
    }

    /** 是否通根（本气或藏干）。 */
    private fun rooted(bazi: BaziResult, el: Element): Boolean = rootBonus(bazi, el) > 0.0

    fun analyze(bazi: BaziResult): BingYaoResult {
        val dayEl = bazi.dayPillar.stem.element
        val f = forces(bazi)
        val el = { c: GodCategory -> elementOf(c, dayEl) }
        val fc = { c: GodCategory -> f[el(c)]!! }

        // ── 1. 从格 → 顺势 ──
        val congGe = GeJuCalculator.detectCongGe(bazi)
        if (congGe != null) {
            return shunShi(bazi, congGe.kind, congGe.basis, f, dayEl)
        }

        // ── 2. 强弱（含通关转移、党众与寒暖燥湿折减） ──
        var support = fc(GodCategory.BI) + fc(GodCategory.YIN)
        var guanF = fc(GodCategory.GUAN)
        val transfer = if (rooted(bazi, el(GodCategory.YIN))) min(guanF, fc(GodCategory.YIN)) * TRANSFER_K else 0.0
        support += transfer
        guanF -= transfer
        val dangZhong = bazi.stems.count { it.element == dayEl } >= 3
        if (dangZhong) support += 1.0
        // 寒暖燥湿折减：寒月土木金火受冻、燥月水金木受灼（当令之水/火土免）
        val climatePenalty = climatePenalty(bazi, dayEl)
        if (climatePenalty < 1.0) support *= climatePenalty
        val drain = guanF * GUAN_W + fc(GodCategory.CAI) + fc(GodCategory.SHI) * SHI_W
        val diff = support - drain
        val strength = when {
            diff >= STRONG_TH -> DayStrength.STRONG
            diff < 0 -> DayStrength.WEAK
            else -> DayStrength.BALANCED
        }
        val strengthNote = "扶方${fmt(support)}对耗方${fmt(drain)}" +
            (if (transfer > 0) "（印通根，官杀之力经印转移${fmt(transfer)}生身）" else "") +
            (if (dangZhong) "（比劫透干党众+1）" else "") +
            (if (climatePenalty < 1.0) "（日主失时于寒暖燥湿，力×${climatePenalty}）" else "")

        // ── 3. 中和无病 ──
        if (strength == DayStrength.BALANCED) {
            val yong = GodCategory.YIN
            return BingYaoResult(
                strength = strength, congGe = null, bing = null,
                yongShen = yong, yongShenElement = el(yong),
                method = YongShenMethod.WU_BING, xiShen = null, jiShen = emptyList(),
                tiaoHou = tiaoHouState(bazi, el(yong)),
                basis = "日主中和无大病（${strengthNote}），岁运忌大起大落，顺势取${el(yong).chinese}流通。",
            )
        }

        // ── 4. 找病 ──
        val bing: GodCategory
        if (strength == DayStrength.WEAK) {
            bing = listOf(GodCategory.GUAN, GodCategory.CAI, GodCategory.SHI).maxBy { fc(it) }
        } else {
            bing = listOf(GodCategory.YIN, GodCategory.BI).maxBy { fc(it) }
        }

        // ── 5. 对症下药 ──
        val (yong, method, xi, ji, cure) = if (strength == DayStrength.WEAK) cureWeak(bazi, bing, fc, el) else cureStrong(bazi, bing, fc, el)

        val tiaoHou = tiaoHouState(bazi, el(yong))
        val tiaoHouNote = when (tiaoHou) {
            TiaoHouState.ALIGNED -> "调候用神与扶抑用神同为${el(yong).chinese}，一药双效。"
            TiaoHouState.CONFLICT -> "调候与扶抑对立，以扶抑为主（截肢保命），美中不足。"
            TiaoHouState.WU_GEN -> "调候之神虚透无根，不宜调候，反宜顺势；岁运逢根防水火交战。"
            TiaoHouState.NONE -> ""
        }

        return BingYaoResult(
            strength = strength,
            congGe = null,
            bing = bing,
            yongShen = yong,
            yongShenElement = el(yong),
            method = method,
            xiShen = xi,
            jiShen = ji,
            tiaoHou = tiaoHou,
            basis = "日主${strength.chinese}（${strengthNote}），病在${bing.chinese}（${el(bing).chinese}${fmt(fc(bing))}）。$cure${tiaoHouNote}",
        )
    }

    /** 身弱三病的用药。返回（用神类，法，喜，忌，药理说明）。 */
    private fun cureWeak(
        bazi: BaziResult,
        bing: GodCategory,
        fc: (GodCategory) -> Double,
        el: (GodCategory) -> Element,
    ): Tuple {
        val dayEl = bazi.dayPillar.stem.element
        val yinEl = el(GodCategory.YIN)
        val biEl = el(GodCategory.BI)
        val yinRoot = rootBonus(bazi, yinEl)
        return when (bing) {
            GodCategory.GUAN -> {
                // 首取印（化杀通关=智取）；印太弱不受官生、比劫有力结党时取比劫（强攻）
                val yinTouGan = bazi.stems.any { it.element == yinEl }
                val takeYin = (yinTouGan && yinRoot >= 0.5) || // 印透干有根，径取
                    (fc(GodCategory.YIN) + yinRoot >= fc(GodCategory.BI) - 1)
                if (takeYin) {
                    val weak = if (yinRoot == 0.0 && fc(GodCategory.YIN) <= 1.0)
                        "印星${el(GodCategory.YIN).chinese}虚浮无根，通关力薄，待岁运强${yinEl.chinese}扶起方有力。"
                    else "印星纳${el(GodCategory.GUAN).chinese}之力而生身，化敌为友。"
                    Tuple(GodCategory.YIN, YongShenMethod.TONG_GUAN, GodCategory.BI,
                        listOf(GodCategory.GUAN, GodCategory.CAI), "取${yinEl.chinese}印通关化杀（攻心为上）。$weak")
                } else {
                    Tuple(GodCategory.BI, YongShenMethod.FU, GodCategory.YIN,
                        listOf(GodCategory.GUAN, GodCategory.CAI),
                        "印弱无力通关（官杀越关照克），取${biEl.chinese}比劫帮身抗杀（强攻）。")
                }
            }
            GodCategory.CAI -> Tuple(GodCategory.BI, YongShenMethod.FU, GodCategory.YIN,
                listOf(GodCategory.CAI, GodCategory.SHI),
                "取${biEl.chinese}比劫抑财助身、与日结党（财强克印，印难生身）。")
            GodCategory.SHI -> {
                val takeYin = fc(GodCategory.YIN) + yinRoot >= 1.0
                if (takeYin) Tuple(GodCategory.YIN, YongShenMethod.FU, GodCategory.BI,
                    listOf(GodCategory.SHI, GodCategory.BI),
                    "取${yinEl.chinese}印制食伤而生身（印为食伤克星）。")
                else Tuple(GodCategory.BI, YongShenMethod.FU, null,
                    listOf(GodCategory.SHI, GodCategory.BI),
                    "柱无有力之印，取${biEl.chinese}比劫助身抗泄。")
            }
            else -> Tuple(GodCategory.BI, YongShenMethod.FU, null, emptyList(), "")
        }
    }

    /** 身强二病的用药。 */
    private fun cureStrong(
        bazi: BaziResult,
        bing: GodCategory,
        fc: (GodCategory) -> Double,
        el: (GodCategory) -> Element,
    ): Tuple {
        val caiEl = el(GodCategory.CAI)
        val shiEl = el(GodCategory.SHI)
        val guanEl = el(GodCategory.GUAN)
        val yinEl = el(GodCategory.YIN)
        return when (bing) {
            GodCategory.YIN -> {
                // 财制印耗身（首取）；无财取食伤；官杀生印反增身强，不可取
                if (fc(GodCategory.CAI) >= 1.0) Tuple(GodCategory.CAI, YongShenMethod.YI, GodCategory.SHI,
                    listOf(GodCategory.YIN, GodCategory.GUAN),
                    "取${caiEl.chinese}财制印耗身；官杀生印为忌。")
                else Tuple(GodCategory.SHI, YongShenMethod.YI, null,
                    listOf(GodCategory.YIN, GodCategory.GUAN),
                    "柱无财星，取${shiEl.chinese}食伤泄身耗印；官杀生印为忌。")
            }
            else -> {
                // 比劫为病：官杀可制比劫（须有力有根，且印不夺官）；否则食伤泄秀（财有力时兼通关）
                val guanUsable = fc(GodCategory.GUAN) >= 2.0 && rooted(bazi, guanEl)
                val yinHuaGuan = rooted(bazi, yinEl) && fc(GodCategory.YIN) > 0 && fc(GodCategory.GUAN) > 0
                when {
                    guanUsable && !yinHuaGuan -> Tuple(GodCategory.GUAN, YongShenMethod.YI, GodCategory.CAI,
                        listOf(GodCategory.BI, GodCategory.YIN),
                        "取${guanEl.chinese}官杀制比劫抑强身。")
                    fc(GodCategory.CAI) >= 1.0 -> Tuple(GodCategory.SHI, YongShenMethod.TONG_GUAN, GodCategory.CAI,
                        listOf(GodCategory.BI, GodCategory.YIN),
                        "官杀不可用（${if (yinHuaGuan) "有印通关化官反生身" else "官杀虚浮无力"}），取${shiEl.chinese}食伤通关，泄身生财。")
                    else -> Tuple(GodCategory.SHI, YongShenMethod.YI, null,
                        listOf(GodCategory.BI, GodCategory.YIN),
                        "官杀财星俱不可用，取${shiEl.chinese}食伤泄强身之秀气。")
                }
            }
        }
    }

    /** 从格顺势。 */
    private fun shunShi(bazi: BaziResult, kind: CongGeKind, congBasis: String, f: Map<Element, Double>, dayEl: Element): BingYaoResult {
        return when (kind) {
            CongGeKind.CONG_STRONG -> BingYaoResult(
                strength = DayStrength.STRONG, congGe = cn.xulun.bazi.model.CongGeResult(kind, congBasis),
                bing = null, yongShen = GodCategory.YIN, yongShenElement = elementOf(GodCategory.YIN, dayEl),
                method = YongShenMethod.SHUN_SHI, xiShen = GodCategory.BI,
                jiShen = listOf(GodCategory.CAI, GodCategory.GUAN),
                tiaoHou = TiaoHouState.NONE,
                basis = "$congBasis。强旺之势不可逆，顺势取生扶（印比）为用，最忌财星虚浮触犯旺神（众兄劫财破格）。",
            )
            CongGeKind.CONG_WEAK -> {
                val yong = listOf(GodCategory.CAI, GodCategory.GUAN, GodCategory.SHI)
                    .maxBy { f[elementOf(it, dayEl)]!! }
                BingYaoResult(
                    strength = DayStrength.WEAK, congGe = cn.xulun.bazi.model.CongGeResult(kind, congBasis),
                    bing = null, yongShen = yong, yongShenElement = elementOf(yong, dayEl),
                    method = YongShenMethod.SHUN_SHI, xiShen = null,
                    jiShen = listOf(GodCategory.BI, GodCategory.YIN),
                    tiaoHou = TiaoHouState.NONE,
                    basis = "$congBasis。弱极不受扶，顺势弃身从势，取${elementOf(yong, dayEl).chinese}（${yong.chinese}）为用，忌印比逆势。",
                )
            }
        }
    }

    /** 调候与扶抑的关系。 */
    private fun tiaoHouState(bazi: BaziResult, yongEl: Element): TiaoHouState {
        val tiaoHouEl = TiaoHouCalculator.analyze(bazi).tiaoHouElement ?: return TiaoHouState.NONE
        return when {
            tiaoHouEl == yongEl -> TiaoHouState.ALIGNED
            !rooted(bazi, tiaoHouEl) -> TiaoHouState.WU_GEN
            else -> TiaoHouState.CONFLICT
        }
    }

    private fun fmt(d: Double): String = if (d == d.toLong().toDouble()) d.toLong().toString() else String.format("%.1f", d)

    /** 内部五元组（用神，法，喜，忌，药理）。 */
    private data class Tuple(
        val yong: GodCategory,
        val method: YongShenMethod,
        val xi: GodCategory?,
        val ji: List<GodCategory>,
        val cure: String,
    )
}
