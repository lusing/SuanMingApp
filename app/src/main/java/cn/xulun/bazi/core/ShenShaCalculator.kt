package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.Pillar
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
private val zaiSha = arrayOf(EarthlyBranch.WU, EarthlyBranch.YOU, EarthlyBranch.ZI, EarthlyBranch.MAO)     // 灾煞=冲将星（白虎煞）
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
        val yg = groupOf(bazi.yearPillar.branch)
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
        // 学堂词馆（年命纳音查月日时：长生=学堂、临官=词馆；柱纳音同行者为"正"——
        // 教材明训"以十干查词馆有误"；旧干查学堂表弃用）
        val ynEl = bazi.yearPillar.nayin.element
        val xueTangZhi = when (ynEl) {
            Element.WOOD -> EarthlyBranch.HAI; Element.FIRE -> EarthlyBranch.YIN
            Element.EARTH -> EarthlyBranch.SHEN; Element.METAL -> EarthlyBranch.SI; else -> EarthlyBranch.SHEN
        }
        val ciGuanZhi = when (ynEl) {
            Element.WOOD -> EarthlyBranch.YIN; Element.FIRE -> EarthlyBranch.SI
            Element.EARTH -> EarthlyBranch.HAI; Element.METAL -> EarthlyBranch.SHEN; else -> EarthlyBranch.HAI
        }
        for ((i, pl) in bazi.pillars.withIndex()) {
            if (i == 0) continue // 以年柱纳音查月日时
            val same = pl.nayin.element == ynEl
            if (pl.branch == xueTangZhi) out.add(ShenShaItem(if (same) "正学堂" else "学堂", PillarBranchLabels[i], branch = pl.branch))
            if (pl.branch == ciGuanZhi) out.add(ShenShaItem(if (same) "正词馆" else "词馆", PillarBranchLabels[i], branch = pl.branch))
        }
        // 金舆（禄神前二位——年日干查：甲辰乙巳丙未丁申戊未己申庚戌辛亥壬丑癸寅）
        val jinYu = mapOf(
            HeavenlyStem.JIA to EarthlyBranch.CHEN, HeavenlyStem.YI to EarthlyBranch.SI,
            HeavenlyStem.BING to EarthlyBranch.WEI, HeavenlyStem.DING to EarthlyBranch.SHEN,
            HeavenlyStem.WU to EarthlyBranch.WEI, HeavenlyStem.JI to EarthlyBranch.SHEN,
            HeavenlyStem.GENG to EarthlyBranch.XU, HeavenlyStem.XIN to EarthlyBranch.HAI,
            HeavenlyStem.REN to EarthlyBranch.CHOU, HeavenlyStem.GUI to EarthlyBranch.YIN,
        )
        for (jg in listOf(bazi.yearPillar.stem, dayStem).distinct()) {
            scanBranch(bazi, "金舆", jinYu.getValue(jg), out)
        }
        // 天医（月支前一位——以月支查年日时支：寅月丑、卯月寅……丑月子）
        val tianYi2 = EarthlyBranch.entries[(monthBranch.ordinal + 11) % 12]
        for ((i, pl) in bazi.pillars.withIndex()) {
            if (i == 1) continue // 以月支查年日时
            if (pl.branch == tianYi2) out.add(ShenShaItem("天医", PillarBranchLabels[i], branch = pl.branch))
        }
        // 天赦（四季查日柱：春戊寅/夏甲午/秋戊申/冬甲子——天生地立，凶神退避）
        val tianShe = when (monthBranch) {
            EarthlyBranch.YIN, EarthlyBranch.MAO, EarthlyBranch.CHEN ->
                Pillar(HeavenlyStem.WU, EarthlyBranch.YIN)
            EarthlyBranch.SI, EarthlyBranch.WU, EarthlyBranch.WEI ->
                Pillar(HeavenlyStem.JIA, EarthlyBranch.WU)
            EarthlyBranch.SHEN, EarthlyBranch.YOU, EarthlyBranch.XU ->
                Pillar(HeavenlyStem.WU, EarthlyBranch.SHEN)
            else -> Pillar(HeavenlyStem.JIA, EarthlyBranch.ZI)
        }
        if (bazi.dayPillar == tianShe) out.add(ShenShaItem("天赦", "日柱", branch = tianShe.branch))
        // 魁罡（日柱四座神：壬辰庚戌庚辰戊戌——聪明机巧文章振发，临事果断有威杀之气）
        // 天罗地网（戌亥互见为天罗、辰巳互见为地网——年支或日支查其余三支；辰戌巳亥全=齐见）
        val luoWang = mapOf(
            EarthlyBranch.XU to ("天罗" to EarthlyBranch.HAI), EarthlyBranch.HAI to ("天罗" to EarthlyBranch.XU),
            EarthlyBranch.CHEN to ("地网" to EarthlyBranch.SI), EarthlyBranch.SI to ("地网" to EarthlyBranch.CHEN),
        )
        val bs = bazi.branches
        val quan = setOf(EarthlyBranch.CHEN, EarthlyBranch.XU, EarthlyBranch.SI, EarthlyBranch.HAI)
        if (quan.all { it in bs }) {
            out.add(ShenShaItem("天罗地网", "辰戌巳亥全", branch = EarthlyBranch.XU))
        } else {
            for (qb in listOf(bazi.yearPillar.branch, dayBranch)) {
                luoWang[qb]?.let { (nm, tgt) ->
                    bazi.pillars.forEachIndexed { i, pl ->
                        if ((i != 0 || qb != bazi.yearPillar.branch) && (i != 3 || qb != dayBranch) && pl.branch == tgt) {
                            if (out.none { it.name == nm && it.branch == tgt }) out.add(ShenShaItem(nm, PillarBranchLabels[i], branch = tgt))
                        }
                    }
                }
            }
        }
        if (bazi.dayPillar.fullName in listOf("壬辰", "庚戌", "庚辰", "戊戌")) {
            out.add(ShenShaItem("魁罡", "日柱", branch = bazi.dayPillar.branch))
        }
        // 金神（时柱三座：乙丑/己巳/癸酉——破财之神要制伏入火乡为胜；带七煞阳刃真贵人）
        if (bazi.hourPillar.fullName in listOf("乙丑", "己巳", "癸酉")) {
            out.add(ShenShaItem("金神", "时柱", branch = bazi.hourPillar.branch))
        }
        // 阴注阳受（荫星，月支查表——寅子/卯亥/辰戌/巳酉/午戌/未亥/申子/酉丑/戌寅/亥卯/子寅/丑丑；查年日时支）
        val yinZhu = mapOf(
            EarthlyBranch.YIN to EarthlyBranch.ZI, EarthlyBranch.MAO to EarthlyBranch.HAI,
            EarthlyBranch.CHEN to EarthlyBranch.XU, EarthlyBranch.SI to EarthlyBranch.YOU,
            EarthlyBranch.WU to EarthlyBranch.XU, EarthlyBranch.WEI to EarthlyBranch.HAI,
            EarthlyBranch.SHEN to EarthlyBranch.ZI, EarthlyBranch.YOU to EarthlyBranch.CHOU,
            EarthlyBranch.XU to EarthlyBranch.YIN, EarthlyBranch.HAI to EarthlyBranch.MAO,
            EarthlyBranch.ZI to EarthlyBranch.YIN, EarthlyBranch.CHOU to EarthlyBranch.CHOU,
        )
        yinZhu[monthBranch]?.let { yz ->
            bazi.pillars.forEachIndexed { i, pl ->
                if (i != 1 && pl.branch == yz) out.add(ShenShaItem("阴注阳受", PillarBranchLabels[i], branch = yz))
            }
        }
        scanBranch(bazi, "羊刃", yangRen.getValue(dayStem), out)
        scanBranch(bazi, "禄神", luShen.getValue(dayStem), out)
        // 日支起
        scanBranch(bazi, "桃花", taoHua[g], out)
        // 驿马（三合长生冲位，年支或日支查其余各支——年支组扫月日时；日支组沿 xuanzong 全柱扫）
        if (yg != g) {
            bazi.pillars.drop(1).forEachIndexed { i, p ->
                if (p.branch == yiMa[yg]) out.add(ShenShaItem("驿马", PillarBranchLabels[i + 1], branch = p.branch))
            }
        }
        scanBranch(bazi, "驿马", yiMa[g], out)
        // 华盖（三合墓库，年支或日支查**其余各支**——年支组扫月日时；日支组沿 xuanzong 全柱扫）
        if (yg != g) {
            bazi.pillars.drop(1).forEachIndexed { i, p ->
                if (p.branch == huaGai[yg]) out.add(ShenShaItem("华盖", PillarBranchLabels[i + 1], branch = p.branch))
            }
        }
        scanBranch(bazi, "华盖", huaGai[g], out)
        // 将星（三合中神，年支或日支查**其余各支**——年支组扫月日时；日支组沿 xuanzong 全柱扫）
        if (yg != g) {
            bazi.pillars.drop(1).forEachIndexed { i, p ->
                if (p.branch == jiangXing[yg]) out.add(ShenShaItem("将星", PillarBranchLabels[i + 1], branch = p.branch))
                if (p.branch == zaiSha[yg]) out.add(ShenShaItem("灾煞", PillarBranchLabels[i + 1], branch = p.branch))
                if (p.branch == jieSha[yg]) out.add(ShenShaItem("劫煞", PillarBranchLabels[i + 1], branch = p.branch))
            }
        }
        scanBranch(bazi, "将星", jiangXing[g], out)
        scanBranch(bazi, "灾煞", zaiSha[g], out)
        scanBranch(bazi, "劫煞", jieSha[g], out)
        scanBranch(bazi, "亡神", wangShen[g], out)
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
