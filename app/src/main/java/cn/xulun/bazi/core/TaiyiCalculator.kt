package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.EarthlyBranch
import cn.xulun.bazi.model.SixteenGod
import cn.xulun.bazi.model.TaiyiCountType
import cn.xulun.bazi.model.TaiyiDun
import cn.xulun.bazi.model.TaiyiJu
import cn.xulun.bazi.model.TaiyiJiXing
import cn.xulun.bazi.model.TaiyiMen
import cn.xulun.bazi.model.TaiyiPalace
import cn.xulun.bazi.model.TaiyiPattern
import cn.xulun.bazi.model.TaiyiPlate
import java.lang.Math.floorMod
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * 太乙神数排盘。四计（岁/月/日/时）共用一盘，仅入局法 + 阴阳遁判定不同。
 *
 * - 年计：积年 = 10153917 + 公元年（演纪上元混沌甲子锚，公元0=10153917）；恒阳遁。
 * - 时计：时实 = 距当年冬至积日 × 12 + 时辰序；冬至后阳遁、夏至后阴遁（按过节气时刻切换）。
 *
 * 规则固化见 .taiyi-reference.md。当前实现：入局+太乙落宫+阴阳遁（Task2），
 * 将/算/门/格局由后续步骤填充（当前为占位）。
 */
object TaiyiCalculator {

    /** 阳遁行宫序（跳中五） */
    private val RING_YANG = TaiyiPalace.ring                 // [1,2,3,4,6,7,8,9]
    /** 阴遁行宫序（逆，起九宫） */
    private val RING_YIN = listOf(9, 8, 7, 6, 4, 3, 2, 1)

    /** 阳遁文昌18位序（自武德 idx11 起；阴德(乾14)/大武(坤10)各重留一算）。值为十六神 idx。 */
    private val SEQ18_YANG = intArrayOf(11, 12, 13, 14, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 10)
    /** 阴遁文昌18位序（自吕申 idx3 起；和德(艮2)/大炅(巽6)各重留一算）。 */
    private val SEQ18_YIN = intArrayOf(3, 4, 5, 6, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0, 1, 2, 2)

    /** 太岁六合（合神）：子丑 寅亥 卯戌 辰酉 巳申 午未。 */
    private val LIUHE: Map<EarthlyBranch, EarthlyBranch> = mapOf(
        EarthlyBranch.ZI to EarthlyBranch.CHOU, EarthlyBranch.CHOU to EarthlyBranch.ZI,
        EarthlyBranch.YIN to EarthlyBranch.HAI, EarthlyBranch.HAI to EarthlyBranch.YIN,
        EarthlyBranch.MAO to EarthlyBranch.XU, EarthlyBranch.XU to EarthlyBranch.MAO,
        EarthlyBranch.CHEN to EarthlyBranch.YOU, EarthlyBranch.YOU to EarthlyBranch.CHEN,
        EarthlyBranch.SI to EarthlyBranch.SHEN, EarthlyBranch.SHEN to EarthlyBranch.SI,
        EarthlyBranch.WU to EarthlyBranch.WEI, EarthlyBranch.WEI to EarthlyBranch.WU,
    )

    fun build(dt: LocalDateTime, type: TaiyiCountType): TaiyiPlate {
        val bazi = BaziCalculator.calculate(dt)
        val ju = computeJu(dt, bazi, type)
        val taiSui = taiSuiBranch(bazi, type)
        val heShen = computeHeShen(taiSui)
        val jiShen = computeJiShen(taiSui, ju.dun)
        val wenChang = computeWenChang(ju.suanBase, ju.dun)
        val shiJi = computeShiJi(wenChang, jiShen)
        val taiyiIdx = PALACE_TO_ZHENG_IDX.getValue(ju.taiyiPalaceNo)
        val zhuSuan = computeSuan(wenChang.idx, taiyiIdx)
        val keSuan = computeSuan(shiJi.idx, taiyiIdx)
        val dingMu = computeDingMu(wenChang, taiSui, heShen)
        val dingSuan = computeSuan(dingMu.idx, taiyiIdx)
        val zhuDa = daJiang(zhuSuan); val zhuCan = canJiang(zhuDa)
        val keDa = daJiang(keSuan); val keCan = canJiang(keDa)
        val dingDa = daJiang(dingSuan); val dingCan = canJiang(dingDa)
        val zhiShiMen = computeZhiShiMen(ju.suanBase, ju.dun, type)
        val menOnTaiyi = computeMenOnTaiyi(zhiShiMen, ju.taiyiPalaceNo)
        val patterns = computePatterns(ju.taiyiPalaceNo, wenChang, shiJi, zhuDa, zhuCan, keDa, keCan, zhiShiMen)
        val suanClass = classifySuan(zhuSuan, keSuan, ju.taiyiPalaceNo)
        val jiXing = computeJiXing(dt.year, ju.taiyiPalaceNo, ju.inGongOffset)
        return TaiyiPlate(
            ju = ju,
            taiSui = taiSui,
            heShen = heShen,
            jiShen = jiShen,
            wenChang = wenChang,
            shiJi = shiJi,
            zhuSuan = zhuSuan, keSuan = keSuan, dingSuan = dingSuan,
            zhuDaJiang = zhuDa, zhuCanJiang = zhuCan,
            keDaJiang = keDa, keCanJiang = keCan,
            dingMu = dingMu,
            dingDaJiang = dingDa, dingCanJiang = dingCan,
            zhiShiMen = zhiShiMen,
            menOnTaiyi = menOnTaiyi,
            patterns = patterns,
            suanClass = suanClass,
            jiXing = jiXing,
        )
    }

    /**
     * 直使门。
     * 年计：order[积年 mod 240 / 30]（卷一「推八門占嵗計法」命起开门以次休生门；开元十二年甲子开门锚）。
     * 时计：王希明法——门数 = 积时 mod 120 ÷ 30，阳遁池[开,生,惊,休]、阴遁池[杜,死,伤,景]，30时一移
     * （卷一「推八門用法」王希明「门则随数起，时尽则移，不拘其甲丙己辛日」+「推冬至/夏至加時所直門法」；
     *  夏至法阙文「命起杜门以次而行」依王希明阴遁四门序补；两遁连续积时同轨仅换池，不加 +2192 平气偏移——用户裁定 2026-07-26）。
     */
    private fun computeZhiShiMen(suanBase: Long, dun: TaiyiDun, type: TaiyiCountType): TaiyiMen {
        if (type == TaiyiCountType.YEAR) {
            val idx = floorMod(suanBase + 1, 240).toInt() / 30
            return TaiyiMen.order[idx]
        }
        val menShu = floorMod(suanBase, 120).toInt() / 30
        return (if (dun == TaiyiDun.YANG) TaiyiMen.poolYang else TaiyiMen.poolYin)[menShu]
    }

    /** 八门飞加太乙：直使落太乙宫，按 TaiyiPalace.ring 顺时针顺布开→休→生→伤→杜→景→死→惊（不入中五）。 */
    private fun computeMenOnTaiyi(zhiShi: TaiyiMen, taiyiNo: Int): Map<Int, TaiyiMen> {
        val startDoor = TaiyiMen.order.indexOf(zhiShi)
        val startGong = TaiyiPalace.ring.indexOf(taiyiNo)
        val map = HashMap<Int, TaiyiMen>()
        for (i in 0 until 8) {
            map[TaiyiPalace.ring[(startGong + i) % 8]] = TaiyiMen.order[(startDoor + i) % 8]
        }
        return map
    }

    /** 十六神所主宫号：正神取本宫数，间辰取顺时针下一正宫宫数。 */
    private fun godPalaceNo(god: SixteenGod): Int = god.governingPalaceNo

    /** 击/迫 子格局：以十六神idx与太乙idx之差判定 ±1辰/±2宫，+=外/−=内。 */
    private fun poJi(
        idx: Int, tyIdx: Int, kind: String, out: MutableList<TaiyiPattern>, label: String = "",
    ) {
        val diff = floorMod(idx - tyIdx, 16)
        val sub = when (diff) {
            1 -> "外辰$kind"; 15 -> "内辰$kind"; 2 -> "外宫$kind"; 14 -> "内宫$kind"; else -> return
        }
        out += TaiyiPattern(sub + if (label.isNotEmpty()) "($label)" else "", auspicious = false,
            basis = "$label 临太乙" + if (diff % 2 == 1) "辰" else "宫")
    }

    /** 格局判定（v1：掩/击/迫/囚/关/格/对/杜塞/文武同道；四郭固/四郭杜/提挟/执提/主客相得为 P1）。 */
    private fun computePatterns(
        taiyiNo: Int, wenChang: SixteenGod, shiJi: SixteenGod,
        zhuDa: Int, zhuCan: Int, keDa: Int, keCan: Int,
        zhiShiMen: TaiyiMen,
    ): List<TaiyiPattern> {
        val out = mutableListOf<TaiyiPattern>()
        val tyIdx = PALACE_TO_ZHENG_IDX.getValue(taiyiNo)
        val opp = TaiyiPalace.opposite(taiyiNo)
        val wchGong = godPalaceNo(wenChang)
        val sjGong = godPalaceNo(shiJi)

        if (sjGong == taiyiNo) out += TaiyiPattern("掩", false, "始击临太乙宫")
        poJi(shiJi.idx, tyIdx, "击", out)
        poJi(wenChang.idx, tyIdx, "迫", out, "文昌")
        listOf("主大将" to zhuDa, "主参将" to zhuCan, "客大将" to keDa, "客参将" to keCan).forEach { (n, g) ->
            if (g != 5) poJi(PALACE_TO_ZHENG_IDX.getValue(g), tyIdx, "迫", out, n)
        }
        listOf("主大将" to zhuDa, "主参将" to zhuCan, "客大将" to keDa, "客参将" to keCan).forEach { (n, g) ->
            if (g == taiyiNo) out += TaiyiPattern("囚", false, "$n 落太乙$taiyiNo 宫")
        }
        // 关：两将（非中宫）同宫
        val gangs = listOf("主大将" to zhuDa, "主参将" to zhuCan, "客大将" to keDa, "客参将" to keCan)
            .filter { it.second != 5 }
        val guanPair = gangs.indices.flatMap { i -> (i + 1 until gangs.size).map { j -> gangs[i] to gangs[j] } }
            .firstOrNull { it.first.second == it.second.second }
        if (guanPair != null) out += TaiyiPattern("关", false, "${guanPair.first.first}与${guanPair.second.first}同${guanPair.first.second}宫")
        // 格：始击/客大将/客参将 对宫太乙
        if (sjGong == opp) out += TaiyiPattern("格", false, "始击对太乙宫")
        if (keDa == opp || keCan == opp) out += TaiyiPattern("格", false, "客将对太乙宫")
        // 对：文昌对宫太乙
        if (wchGong == opp) out += TaiyiPattern("对", false, "文昌对太乙宫")
        // 杜塞：大将落中五
        if (zhuDa == 5 || keDa == 5) out += TaiyiPattern("杜塞", false, "大将入中宫，隔绝太乙")
        // 文武同道：文昌与太乙同宫
        if (wchGong == taiyiNo) out += TaiyiPattern("文武同道", true, "文昌与太乙同宫")

        // ── 复合格局（依金镜卷三/淘金歌；古文条件较模糊，此处取合理简化，触发稀少）──
        val names = out.map { it.name }.toSet()
        // 提挟：主客大小将有落太乙宫者，且文昌或始击临太乙邻辰
        val hasTiXie = listOf(zhuDa, zhuCan, keDa, keCan).any { it == taiyiNo } &&
            listOf(wenChang.idx, shiJi.idx).any { floorMod(it - tyIdx, 16) in setOf(1, 15) }
        if (hasTiXie) out += TaiyiPattern("提挟", false, "主客将临太乙，目挟其侧")
        // 四郭固：文昌落太乙宫且客大小将亦临太乙
        if (wchGong == taiyiNo && (keDa == taiyiNo || keCan == taiyiNo))
            out += TaiyiPattern("四郭固", false, "文昌与客将共囚太乙宫")
        // 四郭杜：客参将并文昌宫、主大将并客大将宫，兼掩/迫/关/格/提挟
        if (keCan == wchGong && zhuDa == keDa &&
            (names.any { it == "掩" || it.contains("迫") || it == "关" || it == "格" } || hasTiXie))
            out += TaiyiPattern("四郭杜", false, "客参并文昌、主大并客大，兼凶格")
        // 执提：开门(乾)直使而太乙在巽9，或生门(艮)直使而太乙在坤7（门宫相冲）
        if ((zhiShiMen == TaiyiMen.KAI && taiyiNo == 9) || (zhiShiMen == TaiyiMen.SHENG && taiyiNo == 7))
            out += TaiyiPattern("执提", false, "${zhiShiMen.chinese}门直使临太乙对宫")
        return out
    }

    /**
     * 卷四星宿（基星/福星/游神 + 符神/乙神）。依《太乙金镜式经》卷四原文核定（非参考文档 §17 通行版）：
     * - 三基「命起戌邦」（§17「起午」为讹传）；甲寅上元积年系（近锚 year-113，与卷四远锚 285011 在 mod360/12 等价）。
     * - 五福独立积年锚 year+12607（卷四 Image14「开元十二年积一万三千三百三十一岁」＝12607+724；724AD→艮第11年 实例）。
     * - 大游甲寅游锚 year-353，起七宫；小游＝太乙宫（卷四「与太乙同法」）。
     * - 四神/天乙/直符（卷四 Image36/39/42）：演纪积年，宫数＝入纪年÷3，命起 1/6/5 宫顺尽九宫(含中五)＋绛宫/明堂/玉堂三堂＝12位。
     *   （卷四原文模型，非 §17/淘金歌「十二支特殊序」；地乙/飞符卷四无独立推法，不列。）
     */
    private fun computeJiXing(year: Int, taiyiPalaceNo: Int, inGongOffset: Int): List<TaiyiJiXing> {
        val out = mutableListOf<TaiyiJiXing>()
        val ruZhou = floorMod(year - 113, 360)          // 三基入周（0..359）；戌邦 idx=10
        // 君基：大周360，30年/邦
        val junBang = ruZhou / 30
        out += TaiyiJiXing("君基", EarthlyBranch.fromIndex(floorMod(10 + junBang, 12)).chinese, null,
            "360年/30年一邦", ruZhou % 30 + 1, true, "治国安民·依卷四起戌邦")
        // 臣基：小周36，3年/邦
        val chenXiao = ruZhou % 36
        out += TaiyiJiXing("臣基", EarthlyBranch.fromIndex(floorMod(10 + chenXiao / 3, 12)).chinese, null,
            "360年/3年一邦", chenXiao % 3 + 1, true, "辅上治下·依卷四起戌邦")
        // 民基：小周12，1年/邦
        val minXiao = ruZhou % 12
        out += TaiyiJiXing("民基", EarthlyBranch.fromIndex(floorMod(10 + minXiao, 12)).chinese, null,
            "12年/1年一邦", minXiao + 1, true, "求财·依卷四起戌邦")
        // 五福：独立锚 year+12607，225年/5宫/45年，序乾→艮→巽→坤→中
        val wuFuRu = floorMod(year + 12607, 225).let { if (it == 0) 225 else it }
        val wuFuGong = (wuFuRu - 1) / 45
        val wuFuSeq = listOf("乾", "艮", "巽", "坤", "中")
        out += TaiyiJiXing("五福", wuFuSeq[wuFuGong], null,
            "225年/45年一宫", wuFuRu - wuFuGong * 45, true, "福佑无灾·依卷四独立积年")
        // 大游：甲寅游锚 year-353，元4320/小周288/36年宫，起七宫，序[7,8,9,1,2,3,4,6]
        val daYouXiao = floorMod(year - 353, 4320) % 288
        val daYouSeq = listOf(7, 8, 9, 1, 2, 3, 4, 6)
        val daYouPalace = daYouSeq[daYouXiao / 36]
        out += TaiyiJiXing("大游", "${TaiyiPalace.of(daYouPalace).trigram}${daYouPalace}宫", daYouPalace,
            "288年/36年一宫", daYouXiao % 36 + 1, false, "肃杀·依卷四起七宫")
        // 小游：＝太乙宫（卷四「与太乙同法」）
        out += TaiyiJiXing("小游", "${TaiyiPalace.of(taiyiPalaceNo).trigram}${taiyiPalaceNo}宫", taiyiPalaceNo,
            "24年/3年一宫(同太乙)", inGongOffset + 1, false, "游移不安·同太乙宫")

        // ── 卷四符神/乙神（四神/天乙/直符；地乙/飞符卷四无独立推法，故不列）──
        // 与小游同演纪积年系（mod180 与卷四 Image32「2637+year=3361@724」等价）。
        // 入纪年＝积年mod180 mod36；宫数＝入纪年÷3（0..11）。
        // 命起 X 宫顺行尽九宫（含中五，1..9 自然序环绕）＋绛宫/明堂/玉堂三堂＝12位。
        // 卷四原文模型（Image36/39/42），非 §17/淘金歌「十二支特殊序」通行版。
        val ruJiNian = floorMod(floorMod(year + 10153917, 180), 36)
        val gongShu = ruJiNian / 3                              // 0..11
        val inFsYear = ruJiNian % 3 + 1                          // 入宫第N年
        val sanTang = listOf("绛宫", "明堂", "玉堂")
        // 命起 start 宫顺尽九宫（含中五）再接三堂；返回 1..9=宫号、10/11/12=绛/明/玉
        fun fsSeq(start: Int): List<Int> =
            (0 until 9).map { floorMod(start - 1 + it, 9) + 1 } + listOf(10, 11, 12)
        fun fsLoc(pos: Int): Pair<String, Int?> = if (pos <= 9)
            "${TaiyiPalace.of(pos).trigram}${pos}宫" to pos else sanTang[pos - 10] to null
        val (loc1, pno1) = fsLoc(fsSeq(1)[gongShu])             // 四神 起一宫（水神·半吉）
        out += TaiyiJiXing("四神", loc1, pno1, "36年/3年一宫·九宫含中五+三堂", inFsYear, true,
            "纪纲有道则昌无道则殃·水神多变莫测·依卷四起一宫(非淘金歌十二支序)")
        val (loc2, pno2) = fsLoc(fsSeq(6)[gongShu])             // 天乙 起六宫（金神·凶）
        out += TaiyiJiXing("天乙", loc2, pno2, "36年/3年一宫·九宫含中五+三堂", inFsYear, false,
            "金神主兵革·定胜负·依卷四起六宫")
        val (loc3, pno3) = fsLoc(fsSeq(5)[gongShu])             // 直符 起五宫（火神·凶）
        out += TaiyiJiXing("直符", loc3, pno3, "36年/3年一宫·九宫含中五+三堂", inFsYear, false,
            "火神天帝使者·巡察万民·依卷四起五宫")
        return out
    }

    /** 数分类：长/短、和数(上/次/下)、不和。 */
    private fun classifySuan(zhuSuan: Int, keSuan: Int, taiyiNo: Int): List<String> {
        val out = mutableListOf<String>()
        val yangGong = taiyiNo in setOf(3, 4, 8, 9)
        listOf("主" to zhuSuan, "客" to keSuan).forEach { (n, s) ->
            out += "${n}算" + if (s >= 11) "长数" else "短数"
            when (s) {
                in setOf(14, 18, 33) -> out += "${n}算上和"
                in setOf(23, 29, 32) -> out += "${n}算次和"
                in setOf(12, 16, 27, 34, 38) -> out += "${n}算下和"
            }
            val suanYang = s % 2 == 1
            if (s in setOf(22, 24, 26, 28) || (yangGong && suanYang) || (!yangGong && !suanYang)) {
                out += "${n}算不和"
            }
        }
        return out
    }

    /** 合神 = 太岁六合所在支。 */
    private fun computeHeShen(taiSui: EarthlyBranch): EarthlyBranch = LIUHE.getValue(taiSui)

    /** 计神：阳遁起寅(2)、阴遁起申(8)，逆行十二辰。 */
    private fun computeJiShen(taiSui: EarthlyBranch, dun: TaiyiDun): EarthlyBranch {
        val start = if (dun == TaiyiDun.YANG) 2 else 8
        return EarthlyBranch.fromIndex(floorMod(start - taiSui.ordinal, 12))
    }

    /** 文昌（天目）：SEQ18[suanSteps mod 18]，阳遁自武德起、阴遁自吕申起（重留见表）。 */
    private fun computeWenChang(suanSteps: Long, dun: TaiyiDun): SixteenGod {
        val seq = if (dun == TaiyiDun.YANG) SEQ18_YANG else SEQ18_YIN
        return SixteenGod.at(seq[floorMod(suanSteps, 18)])
    }

    /** 始击（客目）：计神加和德(艮 idx2)→文昌顺移相同距离。 */
    private fun computeShiJi(wenChang: SixteenGod, jiShen: EarthlyBranch): SixteenGod {
        val jiShenIdx = SixteenGod.ofBranch(jiShen).idx
        val distance = floorMod(2 - jiShenIdx, 16)
        return SixteenGod.at(floorMod(wenChang.idx + distance, 16))
    }

    /** 太乙宫号 → 正神十六神 idx（太乙恒落正宫）。 */
    private val PALACE_TO_ZHENG_IDX: Map<Int, Int> = mapOf(
        1 to 14, 2 to 8, 3 to 2, 4 to 4, 6 to 12, 7 to 10, 8 to 0, 9 to 6,
    )

    /**
     * 主/客/定算：从目(文昌/始击/定目)所在位顺行至太乙(不含)，累加正神宫数；
     * 目在间辰则起点+1；目即太乙(同宫)返回0(囚特例)。
     */
    private fun computeSuan(muIdx: Int, taiyiIdx: Int): Int {
        if (muIdx == taiyiIdx) return 0
        var s = 0
        var pos = muIdx
        while (pos != taiyiIdx) {
            val g = SixteenGod.at(pos)
            if (g.isZheng) {
                s += g.palaceNo!!
            } else if (pos == muIdx) {
                s += 1
            }
            pos = floorMod(pos + 1, 16)
        }
        return s
    }

    /** 定目：合神加太岁旋转——定目idx = (文昌idx + 太岁idx − 合神idx) mod 16。 */
    private fun computeDingMu(
        wenChang: SixteenGod, taiSui: EarthlyBranch, heShen: EarthlyBranch,
    ): SixteenGod {
        val taiSuiIdx = SixteenGod.ofBranch(taiSui).idx
        val heShenIdx = SixteenGod.ofBranch(heShen).idx
        return SixteenGod.at(floorMod(wenChang.idx + taiSuiIdx - heShenIdx, 16))
    }

    /** 大将宫 = 算 mod 10；若整十(0)改 算 mod 9（「十算仍将九去之」）。 */
    private fun daJiang(suan: Int): Int {
        val d = suan % 10
        if (d != 0) return d
        val r = suan % 9
        return if (r == 0) 9 else r
    }

    /** 参将宫 = (大将宫 × 3) mod 10（「三因大将满十去」）。 */
    private fun canJiang(daJiang: Int): Int {
        val c = (daJiang * 3) % 10
        return if (c == 0) 10 else c
    }

    /** 计之地支（太岁）：年计→年支，月→月支，日→日支，时→时支。 */
    private fun taiSuiBranch(bazi: BaziResult, type: TaiyiCountType): EarthlyBranch = when (type) {
        TaiyiCountType.YEAR -> bazi.yearPillar.branch
        TaiyiCountType.MONTH -> bazi.monthPillar.branch
        TaiyiCountType.DAY -> bazi.dayPillar.branch
        TaiyiCountType.HOUR -> bazi.hourPillar.branch
    }

    /** 入局 + 太乙落宫 + 阴阳遁。suanSteps = 自上元起的 0-based 步数（年计=年，时计=时辰）。 */
    private fun computeJu(dt: LocalDateTime, bazi: BaziResult, type: TaiyiCountType): TaiyiJu {
        val (dun, suanSteps) = when (type) {
            TaiyiCountType.YEAR -> {
                TaiyiDun.YANG to (10153916L + dt.year)   // 积年(10153917+year) - 1
            }
            TaiyiCountType.HOUR -> {
                val (anchorDz, dun) = hourDunAndAnchor(dt)
                val dongZhiJiShi = (29277L + anchorDz.year) * 146097L / 400L * 12L // 上元→冬至子时 积时
                val days = ChronoUnit.DAYS.between(anchorDz.toLocalDate(), dt.toLocalDate())
                val relShiShi = days * 12 + bazi.hourPillar.branch.ordinal
                dun to (dongZhiJiShi + relShiShi)
            }
            TaiyiCountType.MONTH, TaiyiCountType.DAY ->
                TODO("月计/日计入局待实现（P2）")
        }
        val jingShu = floorMod(suanSteps, 24)              // 太乙净数（24时/年周期）
        val gongXu = jingShu / 3                           // 0..7
        val taiyiNo = (if (dun == TaiyiDun.YANG) RING_YANG else RING_YIN)[gongXu]
        val offset = jingShu % 3                           // 0=理天 1=理地 2=理人
        val liTianDiRen = listOf("理天", "理地", "理人")[offset]
        val juNo = floorMod(suanSteps, 72).toInt() + 1     // 1..72
        return TaiyiJu(
            countType = type, dun = dun, suanBase = suanSteps,
            juNo = juNo, taiyiPalaceNo = taiyiNo,
            inGongOffset = offset, liTianDiRen = liTianDiRen,
        )
    }

    /** 时计：取最近冬至（≤ dt）为锚，并按「过夏至/冬至时刻」判定阴阳遁。 */
    private fun hourDunAndAnchor(dt: LocalDateTime): Pair<LocalDateTime, TaiyiDun> {
        val y = dt.year
        val dzThis = SolarTerm.getSolarTerm(y, 21)            // 冬至（y 年 12 月）
        val anchorDzYear = if (!dt.isBefore(dzThis)) y else y - 1
        val anchorDz = SolarTerm.getSolarTerm(anchorDzYear, 21)
        val followingXz = SolarTerm.getSolarTerm(anchorDzYear + 1, 9)  // 锚冬至之后的夏至
        val dun = if (dt.isBefore(followingXz)) TaiyiDun.YANG else TaiyiDun.YIN
        return anchorDz to dun
    }
}
