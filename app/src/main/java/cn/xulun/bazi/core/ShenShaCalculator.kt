package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.PillarBranchLabels
import cn.xulun.bazi.model.PillarStemLabels
import cn.xulun.bazi.model.ShenShaItem
import cn.xulun.bazi.model.ShenShaResult

/**
 * 通用神煞计算器（13 种）。逐字移植自 xuanzong `utils/bazi/shensha.js`。
 *
 * - 日干起：天乙贵人、文昌、学堂、羊刃、禄神
 * - 日支起（三合局分组）：桃花、驿马、华盖、将星、亡神、劫煞
 * - 月支起：月德贵人（干）、天德贵人（干或支）
 *
 * 仅返回目标干/支实际落在原局某柱的神煞项，命中柱位用四柱干/支标签。
 */
object ShenShaCalculator {

    /** 三合局分组：0=申子辰 1=亥卯未 2=寅午戌 3=巳酉丑 */
    fun groupOf(b: EarthlyBranch): Int = when (b) {
        EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN -> 0
        EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI -> 1
        EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU -> 2
        EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU -> 3
    }

    // ── 天乙贵人（双支，神峰通考版——甲戊兼牛羊、庚辛逢虎马；年干日干均可查）：
    // 甲戊→丑未 乙己→子申 丙丁→亥酉 庚辛→寅午 壬癸→卯巳（渊海"甲戊庚牛羊"经实践检验不及此版）
    private val tianYi: Map<HeavenlyStem, List<EarthlyBranch>> = mapOf(
        HeavenlyStem.JIA to listOf(EarthlyBranch.CHOU, EarthlyBranch.WEI),
        HeavenlyStem.WU to listOf(EarthlyBranch.CHOU, EarthlyBranch.WEI),
        HeavenlyStem.GENG to listOf(EarthlyBranch.YIN, EarthlyBranch.WU),
        HeavenlyStem.YI to listOf(EarthlyBranch.ZI, EarthlyBranch.SHEN),
        HeavenlyStem.JI to listOf(EarthlyBranch.ZI, EarthlyBranch.SHEN),
        HeavenlyStem.BING to listOf(EarthlyBranch.HAI, EarthlyBranch.YOU),
        HeavenlyStem.DING to listOf(EarthlyBranch.HAI, EarthlyBranch.YOU),
        HeavenlyStem.REN to listOf(EarthlyBranch.MAO, EarthlyBranch.SI),
        HeavenlyStem.GUI to listOf(EarthlyBranch.MAO, EarthlyBranch.SI),
        HeavenlyStem.XIN to listOf(EarthlyBranch.YIN, EarthlyBranch.WU),
    )

    // ── 太极贵人（科名星，年日干查）：甲乙→子午 丙丁→卯酉 戊己→辰戌丑未(四库) 庚辛→寅亥 壬癸→巳申 ──
    private val taiJi: Map<HeavenlyStem, List<EarthlyBranch>> = mapOf(
        HeavenlyStem.JIA to listOf(EarthlyBranch.ZI, EarthlyBranch.WU),
        HeavenlyStem.YI to listOf(EarthlyBranch.ZI, EarthlyBranch.WU),
        HeavenlyStem.BING to listOf(EarthlyBranch.MAO, EarthlyBranch.YOU),
        HeavenlyStem.DING to listOf(EarthlyBranch.MAO, EarthlyBranch.YOU),
        HeavenlyStem.WU to listOf(EarthlyBranch.CHEN, EarthlyBranch.XU, EarthlyBranch.CHOU, EarthlyBranch.WEI),
        HeavenlyStem.JI to listOf(EarthlyBranch.CHEN, EarthlyBranch.XU, EarthlyBranch.CHOU, EarthlyBranch.WEI),
        HeavenlyStem.GENG to listOf(EarthlyBranch.YIN, EarthlyBranch.HAI),
        HeavenlyStem.XIN to listOf(EarthlyBranch.YIN, EarthlyBranch.HAI),
        HeavenlyStem.REN to listOf(EarthlyBranch.SI, EarthlyBranch.SHEN),
        HeavenlyStem.GUI to listOf(EarthlyBranch.SI, EarthlyBranch.SHEN),
    )

    // ── 日干 → 单支 ──
    // 文昌：甲巳 乙午 丙戊申 丁己酉 庚亥 辛子 壬寅 癸卯
    private val wenChang: Map<HeavenlyStem, EarthlyBranch> = mapOf(
        HeavenlyStem.JIA to EarthlyBranch.SI,
        HeavenlyStem.YI to EarthlyBranch.WU,
        HeavenlyStem.BING to EarthlyBranch.SHEN,
        HeavenlyStem.WU to EarthlyBranch.SHEN,
        HeavenlyStem.DING to EarthlyBranch.YOU,
        HeavenlyStem.JI to EarthlyBranch.YOU,
        HeavenlyStem.GENG to EarthlyBranch.HAI,
        HeavenlyStem.XIN to EarthlyBranch.ZI,
        HeavenlyStem.REN to EarthlyBranch.YIN,
        HeavenlyStem.GUI to EarthlyBranch.MAO,
    )
    // 学堂：甲亥 乙午 丙戊寅 丁己酉 庚巳 辛子 壬申 癸卯
    private val xueTang: Map<HeavenlyStem, EarthlyBranch> = mapOf(
        HeavenlyStem.JIA to EarthlyBranch.HAI,
        HeavenlyStem.YI to EarthlyBranch.WU,
        HeavenlyStem.BING to EarthlyBranch.YIN,
        HeavenlyStem.WU to EarthlyBranch.YIN,
        HeavenlyStem.DING to EarthlyBranch.YOU,
        HeavenlyStem.JI to EarthlyBranch.YOU,
        HeavenlyStem.GENG to EarthlyBranch.SI,
        HeavenlyStem.XIN to EarthlyBranch.ZI,
        HeavenlyStem.REN to EarthlyBranch.SHEN,
        HeavenlyStem.GUI to EarthlyBranch.MAO,
    )
    // 羊刃（帝旺位）：甲卯 乙寅 丙午 丁巳 戊午 己巳 庚酉 辛申 壬子 癸亥
    private val yangRen: Map<HeavenlyStem, EarthlyBranch> = mapOf(
        HeavenlyStem.JIA to EarthlyBranch.MAO,
        HeavenlyStem.YI to EarthlyBranch.YIN,
        HeavenlyStem.BING to EarthlyBranch.WU,
        HeavenlyStem.DING to EarthlyBranch.SI,
        HeavenlyStem.WU to EarthlyBranch.WU,
        HeavenlyStem.JI to EarthlyBranch.SI,
        HeavenlyStem.GENG to EarthlyBranch.YOU,
        HeavenlyStem.XIN to EarthlyBranch.SHEN,
        HeavenlyStem.REN to EarthlyBranch.ZI,
        HeavenlyStem.GUI to EarthlyBranch.HAI,
    )
    // 禄神：甲寅 乙卯 丙戊巳 丁己午 庚申 辛酉 壬亥 癸子
    private val luShen: Map<HeavenlyStem, EarthlyBranch> = mapOf(
        HeavenlyStem.JIA to EarthlyBranch.YIN,
        HeavenlyStem.YI to EarthlyBranch.MAO,
        HeavenlyStem.BING to EarthlyBranch.SI,
        HeavenlyStem.WU to EarthlyBranch.SI,
        HeavenlyStem.DING to EarthlyBranch.WU,
        HeavenlyStem.JI to EarthlyBranch.WU,
        HeavenlyStem.GENG to EarthlyBranch.SHEN,
        HeavenlyStem.XIN to EarthlyBranch.YOU,
        HeavenlyStem.REN to EarthlyBranch.HAI,
        HeavenlyStem.GUI to EarthlyBranch.ZI,
    )

    // ── 日支三合局分组(0..3) → 地支 ──
    private val taoHua = arrayOf(EarthlyBranch.YOU, EarthlyBranch.ZI, EarthlyBranch.MAO, EarthlyBranch.WU)      // [9,0,3,6]
    private val yiMa = arrayOf(EarthlyBranch.YIN, EarthlyBranch.SI, EarthlyBranch.SHEN, EarthlyBranch.HAI)       // [2,5,8,11]
    private val huaGai = arrayOf(EarthlyBranch.CHEN, EarthlyBranch.WEI, EarthlyBranch.XU, EarthlyBranch.CHOU)    // [4,7,10,1]
    private val jiangXing = arrayOf(EarthlyBranch.ZI, EarthlyBranch.MAO, EarthlyBranch.WU, EarthlyBranch.YOU)    // [0,3,6,9]
    private val wangShen = arrayOf(EarthlyBranch.HAI, EarthlyBranch.YIN, EarthlyBranch.SI, EarthlyBranch.SHEN)   // [11,2,5,8]
    private val jieSha = arrayOf(EarthlyBranch.SI, EarthlyBranch.SHEN, EarthlyBranch.HAI, EarthlyBranch.YIN)     // [5,8,11,2]

    // ── 月支三合局分组 → 月德贵人天干：申子辰→壬 亥卯未→甲 寅午戌→丙 巳酉丑→庚 ──
    private val yueDe = arrayOf(HeavenlyStem.REN, HeavenlyStem.JIA, HeavenlyStem.BING, HeavenlyStem.GENG)       // [8,0,2,6]

    // ── 月支 → 月德合（干五合）：子辰申丁 / 丑巳酉乙 / 寅午戌辛 / 卯未亥己 ──
    private val yueDeHe = arrayOf(HeavenlyStem.DING, HeavenlyStem.JI, HeavenlyStem.XIN, HeavenlyStem.YI)        // [3,5,7,1]

    // ── 月支 → 天德贵人（干或支）：寅丁 卯申 辰壬 巳辛 午亥 未甲 申癸 酉寅 戌丙 亥乙 子巳 丑庚 ──
    private data class TianDe(val isStem: Boolean, val stem: HeavenlyStem? = null, val branch: EarthlyBranch? = null)
    private val tianDe: Map<EarthlyBranch, TianDe> = mapOf(
        EarthlyBranch.ZI to TianDe(false, branch = EarthlyBranch.SI),       // 子→巳(支)
        EarthlyBranch.CHOU to TianDe(true, stem = HeavenlyStem.GENG),       // 丑→庚(干)
        EarthlyBranch.YIN to TianDe(true, stem = HeavenlyStem.DING),        // 寅→丁(干)
        EarthlyBranch.MAO to TianDe(false, branch = EarthlyBranch.SHEN),    // 卯→申(支)
        EarthlyBranch.CHEN to TianDe(true, stem = HeavenlyStem.REN),        // 辰→壬(干)
        EarthlyBranch.SI to TianDe(true, stem = HeavenlyStem.XIN),          // 巳→辛(干)
        EarthlyBranch.WU to TianDe(false, branch = EarthlyBranch.HAI),      // 午→亥(支)
        EarthlyBranch.WEI to TianDe(true, stem = HeavenlyStem.JIA),         // 未→甲(干)
        EarthlyBranch.SHEN to TianDe(true, stem = HeavenlyStem.GUI),        // 申→癸(干)
        EarthlyBranch.YOU to TianDe(false, branch = EarthlyBranch.YIN),     // 酉→寅(支)
        EarthlyBranch.XU to TianDe(true, stem = HeavenlyStem.BING),         // 戌→丙(干)
        EarthlyBranch.HAI to TianDe(true, stem = HeavenlyStem.YI),          // 亥→乙(干)
    )

    // ── 天德合（月支查）：与天德干五合/支六合——寅壬 卯巳 辰丁 巳丙 午寅 未己 申戊 酉亥 戌辛 亥庚 子申 丑乙 ──
    private val tianDeHe: Map<EarthlyBranch, TianDe> = mapOf(
        EarthlyBranch.ZI to TianDe(false, branch = EarthlyBranch.SHEN),     // 子→申(支)
        EarthlyBranch.CHOU to TianDe(true, stem = HeavenlyStem.YI),         // 丑→乙(干)
        EarthlyBranch.YIN to TianDe(true, stem = HeavenlyStem.REN),         // 寅→壬(干)
        EarthlyBranch.MAO to TianDe(false, branch = EarthlyBranch.SI),      // 卯→巳(支)
        EarthlyBranch.CHEN to TianDe(true, stem = HeavenlyStem.DING),       // 辰→丁(干)
        EarthlyBranch.SI to TianDe(true, stem = HeavenlyStem.BING),         // 巳→丙(干)
        EarthlyBranch.WU to TianDe(false, branch = EarthlyBranch.YIN),      // 午→寅(支)
        EarthlyBranch.WEI to TianDe(true, stem = HeavenlyStem.JI),          // 未→己(干)
        EarthlyBranch.SHEN to TianDe(true, stem = HeavenlyStem.WU),         // 申→戊(干)
        EarthlyBranch.YOU to TianDe(false, branch = EarthlyBranch.HAI),     // 酉→亥(支)
        EarthlyBranch.XU to TianDe(true, stem = HeavenlyStem.XIN),          // 戌→辛(干)
        EarthlyBranch.HAI to TianDe(true, stem = HeavenlyStem.GENG),        // 亥→庚(干)
    )

    /** 计算原局神煞，顺序固定（与 xuanzong calcShenSha 一致）。 */
    fun analyze(bazi: BaziResult): ShenShaResult {
        val dayStem = bazi.dayPillar.stem
        val dayBranch = bazi.dayPillar.branch
        val monthBranch = bazi.monthPillar.branch
        val g = groupOf(dayBranch)
        val mg = groupOf(monthBranch)
        val out = mutableListOf<ShenShaItem>()

        // 日干起（天乙贵人、太极贵人年干、日干均可查——并集去重）
        scanBranchAny(bazi, "天乙贵人", (tianYi.getValue(bazi.yearPillar.stem) + tianYi.getValue(dayStem)).distinct(), out)
        scanBranchAny(bazi, "太极贵人", (taiJi.getValue(bazi.yearPillar.stem) + taiJi.getValue(dayStem)).distinct(), out)
        // 三奇贵人：天上甲戊庚/地下乙丙丁/人中壬癸辛——须顺布紧连（年月日或月日时），间隔逆乱不取
        val stems3 = bazi.stems
        for ((name, tri) in listOf(
            "三奇贵人（天上）" to listOf(HeavenlyStem.JIA, HeavenlyStem.WU, HeavenlyStem.GENG),
            "三奇贵人（地下）" to listOf(HeavenlyStem.YI, HeavenlyStem.BING, HeavenlyStem.DING),
            "三奇贵人（人中）" to listOf(HeavenlyStem.REN, HeavenlyStem.GUI, HeavenlyStem.XIN),
        )) {
            if (stems3.subList(0, 3) == tri) out.add(ShenShaItem(name, "年月日干连", stem = stems3[2]))
            if (stems3.subList(1, 4) == tri) out.add(ShenShaItem(name, "月日时干连", stem = stems3[3]))
        }
        // 文昌（食神之禄，年干日干均可查——并集逐支扫）
        for (tg in listOf(bazi.yearPillar.stem, dayStem).distinct()) {
            scanBranch(bazi, "文昌", wenChang.getValue(tg), out)
        }
        scanBranch(bazi, "学堂", xueTang.getValue(dayStem), out)
        scanBranch(bazi, "羊刃", yangRen.getValue(dayStem), out)
        scanBranch(bazi, "禄神", luShen.getValue(dayStem), out)
        // 日支起
        scanBranch(bazi, "桃花", taoHua[g], out)
        scanBranch(bazi, "驿马", yiMa[g], out)
        scanBranch(bazi, "华盖", huaGai[g], out)
        scanBranch(bazi, "将星", jiangXing[g], out)
        scanBranch(bazi, "亡神", wangShen[g], out)
        scanBranch(bazi, "劫煞", jieSha[g], out)
        // 月支起
        scanStem(bazi, "月德贵人", yueDe[mg], out)
        // 月德合：与月德贵人五合之干（寅午戌丙→辛、申子辰壬→丁、亥卯未甲→己、巳酉丑庚→乙）——命无月德有合亦可补
        scanStem(bazi, "月德合", yueDeHe[mg], out)
        tianDe[monthBranch]?.let { td ->
            if (td.isStem) scanStem(bazi, "天德贵人", td.stem!!, out)
            else scanBranch(bazi, "天德贵人", td.branch!!, out)
        }
        // 天德合：与天德之星干五合或支六合者（正丁天德壬为合；二月申天德巳为合）——命中无天德而只有天德合亦起相似作用
        tianDeHe[monthBranch]?.let { th ->
            if (th.isStem) scanStem(bazi, "天德合", th.stem!!, out)
            else scanBranch(bazi, "天德合", th.branch!!, out)
        }
        return ShenShaResult(out)
    }

    /** 命中任一目标地支的柱都产出（天乙贵人可能双支命中多柱）。 */
    private fun scanBranchAny(bazi: BaziResult, name: String, targets: List<EarthlyBranch>, out: MutableList<ShenShaItem>) {
        bazi.pillars.forEachIndexed { i, p ->
            if (p.branch in targets) out.add(ShenShaItem(name, PillarBranchLabels[i], branch = p.branch))
        }
    }

    private fun scanBranch(bazi: BaziResult, name: String, target: EarthlyBranch, out: MutableList<ShenShaItem>) {
        bazi.pillars.forEachIndexed { i, p ->
            if (p.branch == target) out.add(ShenShaItem(name, PillarBranchLabels[i], branch = target))
        }
    }

    private fun scanStem(bazi: BaziResult, name: String, target: HeavenlyStem, out: MutableList<ShenShaItem>) {
        bazi.pillars.forEachIndexed { i, p ->
            if (p.stem == target) out.add(ShenShaItem(name, PillarStemLabels[i], stem = target))
        }
    }
}
