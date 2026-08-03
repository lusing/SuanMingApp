package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.Pillar
import cn.xulun.bazi.model.PillarVerdict
import cn.xulun.bazi.model.QuYunResult
import cn.xulun.bazi.model.QuYunRow
import cn.xulun.bazi.model.QuYunSummary
import cn.xulun.bazi.model.QuYunVerdict
import cn.xulun.bazi.model.TenGod
import cn.xulun.bazi.model.YongShenResult
import cn.xulun.bazi.model.ZiPeiZhi
import cn.xulun.bazi.model.tenGodOfBranch
import cn.xulun.bazi.model.tenGodOfStem

/**
 * 取运（《子平真诠》论 X 取运）。逐字移植自 xuanzong `utils/bazi/quyun.js`。
 *
 * 运柱 × 本命格局子配置 → 运干/支 喜/忌/平 + 理由。行干行支分别（运干与支可一喜一忌）。
 * 运喜忌由本命「子配置」（用神+相神+救应所成之局）决定，非泛泛十神喜忌。
 */
object QuYunCalculator {

    private val TOU_GAN_IDX = listOf(0, 1, 3) // 年/月/时透干（日干不计）

    private enum class Family {
        ZHENG_GUAN, CAI, YIN, SHI_SHEN, QI_SHA, SHANG_GUAN, YANG_REN, JIAN_LU
    }

    private val FAMILY_CH = mapOf(
        Family.ZHENG_GUAN to "正官", Family.CAI to "财", Family.YIN to "印绶", Family.SHI_SHEN to "食神",
        Family.QI_SHA to "偏官（七煞）", Family.SHANG_GUAN to "伤官", Family.YANG_REN to "阳刃", Family.JIAN_LU to "建禄月劫",
    )

    /** 取运表 token（运柱十神归类）。 */
    private enum class Token { BI_JIE, SHI_SHANG, CAI, GUAN, SHA, GUAN_SHA, YIN, SHEN_WANG }

    private fun roleOf(god: TenGod): String = when (god) {
        TenGod.BI_JIAN, TenGod.JIE_CAI -> "比劫"
        TenGod.SHI_SHEN, TenGod.SHANG_GUAN -> "食伤"
        TenGod.ZHENG_CAI, TenGod.PIAN_CAI -> "财"
        TenGod.ZHENG_GUAN -> "官"
        TenGod.QI_SHA -> "杀"
        TenGod.ZHENG_YIN, TenGod.PIAN_YIN -> "印"
    }

    private fun matches(god: TenGod, token: Token): Boolean {
        val r = roleOf(god)
        return when (token) {
            Token.BI_JIE -> r == "比劫"
            Token.SHI_SHANG -> r == "食伤"
            Token.CAI -> r == "财"
            Token.GUAN -> god == TenGod.ZHENG_GUAN
            Token.SHA -> god == TenGod.QI_SHA
            Token.GUAN_SHA -> r == "官" || r == "杀"
            Token.YIN -> r == "印"
            Token.SHEN_WANG -> r == "比劫" || r == "印"
        }
    }

    private fun familyOf(ys: YongShenResult): Family? {
        val n = ys.geJuName
        if (n.contains("阳刃")) return Family.YANG_REN
        if (n.contains("建禄") || n.contains("月劫")) return Family.JIAN_LU
        return when (ys.yongShen?.tenGod) {
            TenGod.ZHENG_CAI, TenGod.PIAN_CAI -> Family.CAI
            TenGod.ZHENG_YIN, TenGod.PIAN_YIN -> Family.YIN
            TenGod.ZHENG_GUAN -> Family.ZHENG_GUAN
            TenGod.QI_SHA -> Family.QI_SHA
            TenGod.SHI_SHEN -> Family.SHI_SHEN
            TenGod.SHANG_GUAN -> Family.SHANG_GUAN
            else -> null
        }
    }

    private fun touGanGods(bazi: BaziResult, dayMaster: HeavenlyStem): Set<TenGod> =
        TOU_GAN_IDX.map { tenGodOfStem(dayMaster, bazi.pillars[it].stem) }.toSet()

    private data class Row(
        val name: String, val xi: List<Token>, val ji: List<Token>,
        val xiText: String, val jiText: String, val note: String = "",
    )

    // 取运运喜忌表 —— 据《子平真诠》三十二~四十六各格取运
    private val QUYUN_TABLE: Map<Family, Map<String, Row>> = mapOf(
        Family.ZHENG_GUAN to mapOf( // 三十二
            "YONG_CAI_YIN" to Row("正官用财印", listOf(Token.SHEN_WANG, Token.CAI), listOf(Token.SHA, Token.GUAN), "助身（印比）或助官（财）", "官露通用忌：杂煞、重官、逢合、刑冲"),
            "YONG_CAI" to Row("正官用财", listOf(Token.YIN, Token.SHEN_WANG), listOf(Token.SHI_SHANG), "印绶、身旺之地", "切忌食伤（泄身/伤官见官）"),
            "PEI_YIN" to Row("正官佩印", listOf(Token.CAI, Token.SHI_SHANG), listOf(Token.SHA), "财乡、伤食反吉", "忌财党煞；官重身轻则身旺为宜"),
            "DAI_SHANG_YIN" to Row("正官带伤食用印制", listOf(Token.GUAN_SHA, Token.YIN), listOf(Token.CAI), "官旺、印旺之乡", "财运切忌（去印护伤）；印叠则财运无害"),
            "DAI_SHA_JIE" to Row("正官带煞用劫合煞", listOf(Token.CAI, Token.SHI_SHANG, Token.SHEN_WANG, Token.YIN), listOf(Token.SHA), "财、伤食、身旺、印绶皆可行", "不可复露七煞"),
            "HE_SHA_SHANG" to Row("正官用伤官合煞", listOf(Token.SHI_SHANG, Token.CAI), listOf(Token.YIN), "伤食、财俱可行", "不宜逢印（印制伤则不合煞）"),
            "DAI_SHA" to Row("正官带煞未取清", listOf(Token.SHI_SHANG, Token.BI_JIE), listOf(Token.SHA, Token.GUAN), "食伤取清、比劫合煞", "官煞混杂未清为忌"),
            "GUAN_LU" to Row("正官官露", listOf(Token.CAI, Token.YIN), listOf(Token.SHA, Token.GUAN, Token.SHI_SHANG), "财印生护", "伤官见官、杂煞、重官、逢合、刑冲"),
        ),
        Family.CAI to mapOf( // 三十四
            "WANG_SHENG_GUAN" to Row("财旺生官", listOf(Token.SHEN_WANG, Token.YIN), listOf(Token.SHA, Token.SHI_SHANG), "身旺、印绶", "不利七煞、伤官"),
            "YONG_SHI_YIN" to Row("财用食印", listOf(Token.CAI, Token.SHI_SHANG, Token.BI_JIE, Token.YIN), listOf(Token.GUAN_SHA), "财轻→财食；身轻→比印", "官运碍；煞反不忌"),
            "DAI_SHA" to Row("财带七煞", listOf(Token.SHI_SHANG, Token.SHEN_WANG), emptyList(), "食伤、身旺之方（合煞/制煞）", "不论合煞制煞皆喜食伤身旺"),
            "YONG_SHA_YIN" to Row("财用煞印", listOf(Token.YIN, Token.SHI_SHANG), listOf(Token.CAI), "印旺最宜、伤食顺意", "逢财必忌"),
            "PEI_YIN" to Row("财格佩印", listOf(Token.GUAN_SHA, Token.YIN), emptyList(), "官乡；身弱最喜印旺", ""),
            "YONG_SHI_SHENG" to Row("财用食伤生财", listOf(Token.CAI, Token.SHI_SHANG, Token.SHEN_WANG), listOf(Token.GUAN_SHA), "财食轻→财食；财食重→帮身；煞不忌", "官印反晦"),
            "DAI_SHANG" to Row("财带伤官", listOf(Token.CAI), listOf(Token.SHA, Token.GUAN_SHA, Token.YIN), "财运亨", "煞运不利；官印未见其美"),
            "CAI" to Row("单用财", listOf(Token.SHI_SHANG, Token.GUAN_SHA), listOf(Token.BI_JIE), "食伤生财、官杀护财", "比劫夺财"),
        ),
        Family.YIN to mapOf( // 三十六
            "GUAN_SHA_JING" to Row("印格官煞竞透", listOf(Token.SHI_SHANG, Token.YIN, Token.SHEN_WANG), listOf(Token.CAI), "食伤、印旺身旺（取清）", "再透官煞并行财运立灾"),
            "YONG_GUAN" to Row("印用官", listOf(Token.CAI, Token.SHI_SHANG), emptyList(), "财运反吉、伤食最利（官露印重）", ""),
            "YONG_SHA" to Row("印用七煞", listOf(Token.SHI_SHANG, Token.SHEN_WANG), listOf(Token.CAI), "伤食、身旺", "一见财乡其凶立至"),
            "YONG_SHANG_SHI" to Row("印用伤食", listOf(Token.CAI, Token.SHI_SHANG, Token.SHA), listOf(Token.GUAN), "财运反吉、伤食利；煞运反能为福", "官运反见其灾"),
            "DUO_YONG_CAI" to Row("印多用财", listOf(Token.BI_JIE, Token.GUAN_SHA, Token.YIN), listOf(Token.CAI), "劫地、官印亦亨", "财乡则忌（印轻者见财更不利）"),
            "YIN" to Row("印绶", listOf(Token.GUAN_SHA, Token.SHI_SHANG), listOf(Token.CAI), "官杀生印、伤食泄秀", "贪财坏印"),
        ),
        Family.SHI_SHEN to mapOf( // 三十八
            "YONG_SHA_YIN" to Row("食用煞印", listOf(Token.YIN, Token.SHI_SHANG, Token.SHEN_WANG), listOf(Token.CAI, Token.GUAN_SHA), "印旺；身旺则食伤为福", "切忌财乡；行官行煞亦不吉"),
            "DAI_SHA" to Row("食神带煞", listOf(Token.YIN, Token.SHEN_WANG, Token.SHI_SHANG), listOf(Token.CAI), "印绶、身旺、食伤（食重煞轻则印利/财反吉）", "财则最忌"),
            "DAI_YIN" to Row("食神带印", listOf(Token.CAI, Token.SHI_SHANG), listOf(Token.YIN, Token.GUAN_SHA), "财运最利、食伤亦吉（食旺带印）", "印最忌；官煞不吉"),
            "SHENG_CAI" to Row("食神生财", listOf(Token.CAI, Token.SHI_SHANG, Token.SHEN_WANG), listOf(Token.GUAN_SHA), "财重食轻→财食；财食重→帮身", "官煞之方俱不美"),
            "SHI" to Row("单用食神", listOf(Token.CAI, Token.SHI_SHANG), listOf(Token.YIN, Token.GUAN), "食神有气、财运则富", "枭印夺食；忌官（金水不忌）"),
        ),
        Family.QI_SHA to mapOf( // 四十
            "YONG_SHI_ZHI" to Row("煞用食制", listOf(Token.SHI_SHANG, Token.SHA, Token.SHEN_WANG), listOf(Token.GUAN, Token.YIN), "煞重食轻→助食；煞轻食重→助煞；煞食均根轻→助身", "忌正官混杂；畏印绶夺食"),
            "YONG_SHANG" to Row("煞用伤官", listOf(Token.SHI_SHANG, Token.SHA, Token.SHEN_WANG), listOf(Token.GUAN, Token.YIN), "行运与食制同", "忌正官混杂；畏印"),
            "YONG_YIN" to Row("煞用印绶", listOf(Token.SHI_SHANG, Token.YIN, Token.SHEN_WANG), listOf(Token.CAI), "伤官为美、印绶身旺俱福", "不利财乡"),
            "YONG_CAI" to Row("煞用财", listOf(Token.SHI_SHANG, Token.CAI, Token.YIN, Token.SHEN_WANG), emptyList(), "去印存食→伤食/财；助煞不及→食印/帮身", ""),
            "DAI_GUAN" to Row("煞带正官（去清）", listOf(Token.SHEN_WANG, Token.SHI_SHANG), emptyList(), "身轻→助身；食轻→助食", "莫去取清之物、无伤制煞之神"),
            "YONG_REN" to Row("煞用刃当", listOf(Token.SHA, Token.SHI_SHANG, Token.YIN), emptyList(), "煞轻刃重→助煞；刃轻煞重→制伏；印运无伤", ""),
            "SHA" to Row("七煞无制", listOf(Token.SHI_SHANG, Token.YIN), listOf(Token.CAI), "食制、印化", "财党杀攻身"),
        ),
        Family.SHANG_GUAN to mapOf( // 四十二
            "JIAN_CAI_YIN" to Row("伤官兼用财印", listOf(Token.YIN, Token.CAI), emptyList(), "财多带印→助印；印多带财→助财", "两清不相碍，调停中和"),
            "YONG_SHA_YIN" to Row("伤官用煞印", listOf(Token.YIN, Token.SHI_SHANG), listOf(Token.GUAN, Token.CAI), "印运最利、伤食亦亨", "杂官非吉、逢财即危"),
            "DAI_SHA" to Row("伤官带煞", listOf(Token.YIN, Token.SHI_SHANG, Token.SHEN_WANG), listOf(Token.CAI), "喜印；煞根重→伤食/印身旺", "忌财（伤重煞轻则财亦吉）"),
            "YONG_CAI" to Row("伤官用财", listOf(Token.YIN, Token.BI_JIE, Token.CAI, Token.SHI_SHANG), emptyList(), "财旺身轻→印比；身强财浅→财运/伤官", ""),
            "PEI_YIN" to Row("伤官佩印", listOf(Token.GUAN_SHA, Token.YIN), listOf(Token.CAI), "运行官煞为宜、印运吉；伤食不碍", "财地则凶"),
            "YONG_GUAN" to Row("伤官用官（金水）", listOf(Token.CAI, Token.YIN, Token.BI_JIE, Token.SHI_SHANG), listOf(Token.SHI_SHANG), "运喜财印（金水独宜）", "不利食伤（官露财印两旺则比劫伤官亦吉）"),
            "SHANG" to Row("伤官", listOf(Token.CAI, Token.YIN), listOf(Token.GUAN), "伤官生财、伤官佩印", "伤官见官（金水不忌）"),
        ),
        Family.YANG_REN to mapOf( // 四十四
            "GUAN_SHA" to Row("阳刃官煞并出", listOf(Token.SHI_SHANG, Token.BI_JIE), listOf(Token.CAI, Token.GUAN_SHA), "制伏、身旺", "财地官乡反为不吉"),
            "YONG_GUAN" to Row("阳刃用官", listOf(Token.GUAN_SHA, Token.YIN, Token.BI_JIE), listOf(Token.SHI_SHANG), "助官；官根深则印比反美", "不喜伤食合官"),
            "YONG_SHA" to Row("阳刃用煞", listOf(Token.SHA, Token.SHEN_WANG, Token.YIN, Token.SHI_SHANG), emptyList(), "煞不旺→助煞；煞太重→身旺/印/伤食不忌", ""),
            "YANG_REN" to Row("阳刃无制", listOf(Token.GUAN_SHA), emptyList(), "官煞制刃", "无官煞制伏为败，宜行官煞"),
        ),
        Family.JIAN_LU to mapOf( // 四十六
            "YONG_GUAN" to Row("禄劫用官", listOf(Token.CAI, Token.YIN, Token.GUAN_SHA), listOf(Token.SHA, Token.SHI_SHANG), "印护→财；财生→印；宜官植根", "官逢合、煞相乘、伤食相侮"),
            "YONG_CAI" to Row("禄劫用财带食伤", listOf(Token.CAI, Token.SHI_SHANG, Token.YIN, Token.BI_JIE), listOf(Token.GUAN_SHA), "财食重→喜印不忌比；财食轻→助财", "遇官非福；逢煞无伤"),
            "YONG_SHA" to Row("禄劫用煞", listOf(Token.SHA, Token.SHI_SHANG, Token.CAI), emptyList(), "食重煞轻→助煞；食轻煞重→助食（合煞存财则伤食/财运不忌）", ""),
            "YONG_SHI_SHANG" to Row("禄劫用伤食", listOf(Token.CAI, Token.SHI_SHANG), listOf(Token.YIN, Token.GUAN), "财运最宜；煞不忌（伤食太重则印不忌）", "行印非吉、透官不美"),
            "JIAN_LU" to Row("禄劫", listOf(Token.CAI, Token.GUAN_SHA), listOf(Token.BI_JIE), "财官为用", "比劫助身夺财"),
        ),
    )

    /** 识别本命子配置（所就之局）。 */
    fun identifyZiPeiZhi(bazi: BaziResult, ys: YongShenResult): ZiPeiZhi {
        val dayMaster = bazi.dayPillar.stem
        val family = familyOf(ys)
        val gods = touGanGods(bazi, dayMaster)
        fun has(vararg ks: TenGod) = ks.any { it in gods }
        val hasCai = has(TenGod.ZHENG_CAI, TenGod.PIAN_CAI)
        val hasGuan = TenGod.ZHENG_GUAN in gods
        val hasSha = TenGod.QI_SHA in gods
        val hasYin = has(TenGod.ZHENG_YIN, TenGod.PIAN_YIN)
        val hasShi = TenGod.SHI_SHEN in gods
        val hasShang = TenGod.SHANG_GUAN in gods
        val hasShiShang = hasShi || hasShang
        val hasBiJie = has(TenGod.BI_JIAN, TenGod.JIE_CAI)
        val familyCh = family?.let { FAMILY_CH[it] } ?: "未定格"
        fun pick(subKey: String, subName: String, note: String = "") =
            ZiPeiZhi(familyCh, subKey, subName, note)

        return when (family) {
            Family.ZHENG_GUAN -> when {
                hasCai && hasYin -> pick("YONG_CAI_YIN", "正官用财印（财印并透）", "身稍轻取助身、官稍轻取助官")
                hasShang && hasYin -> pick("DAI_SHANG_YIN", "正官带伤食用印制")
                hasSha && hasBiJie -> pick("DAI_SHA_JIE", "正官带煞用劫合煞")
                hasSha && hasShang -> pick("HE_SHA_SHANG", "正官用伤官合煞")
                hasSha -> pick("DAI_SHA", "正官带煞（未取清）")
                hasCai -> pick("YONG_CAI", "正官用财（财生官）")
                hasYin -> pick("PEI_YIN", "正官佩印（印护官）")
                else -> pick("GUAN_LU", "正官官露（孤官无辅）", "通用忌：逢合/杂煞/重官/地支刑冲")
            }
            Family.CAI -> when {
                hasSha && hasYin -> pick("YONG_SHA_YIN", "财用煞印（印化煞）")
                hasSha -> pick("DAI_SHA", "财带七煞（合煞存财/制煞生财）")
                hasGuan -> pick("WANG_SHENG_GUAN", "财旺生官（官护财）")
                hasShi && hasYin -> pick("YONG_SHI_YIN", "财用食印（食印不相碍）")
                hasShiShang -> pick("YONG_SHI_SHENG", "财用食伤生财")
                hasYin -> pick("PEI_YIN", "财格佩印（印帮身取贵）")
                else -> pick("CAI", "单用财", "食伤生之、官杀护之为佳")
            }
            Family.YIN -> when {
                hasGuan && hasSha -> pick("GUAN_SHA_JING", "印格官煞竞透（取清）")
                hasGuan -> pick("YONG_GUAN", "印用官（官印双全）")
                hasSha -> pick("YONG_SHA", "印用七煞（杀印相生）")
                hasShiShang -> pick("YONG_SHANG_SHI", "印用伤食（泄秀）")
                hasCai -> pick("DUO_YONG_CAI", "印多用财 / 贪财坏印", "印重透财抑太过→富；印轻财重→贪财坏印败")
                else -> pick("YIN", "印绶（孤印）")
            }
            Family.SHI_SHEN -> when {
                hasSha && hasYin -> pick("YONG_SHA_YIN", "食用煞印")
                hasSha -> pick("DAI_SHA", "食神带煞")
                hasYin -> pick("DAI_YIN", "食神带印（枭夺食/食旺带印）")
                hasCai -> pick("SHENG_CAI", "食神生财")
                else -> pick("SHI", "单用食神（有气）")
            }
            Family.QI_SHA -> when {
                hasShi -> pick("YONG_SHI_ZHI", "煞用食制")
                hasShang -> pick("YONG_SHANG", "煞用伤官")
                hasYin -> pick("YONG_YIN", "煞用印绶（杀印相生）")
                hasCai -> pick("YONG_CAI", "煞用财（去印存食/助煞不及）")
                hasGuan -> pick("DAI_GUAN", "煞带正官（去清）")
                hasBiJie -> pick("YONG_REN", "煞无食制用刃当")
                else -> pick("SHA", "七煞无制（须制伏）")
            }
            Family.SHANG_GUAN -> {
                val jinShui = TiaoHouCalculator.isJinShuiShangGuan(dayMaster, bazi.monthPillar.branch)
                when {
                    hasCai && hasYin -> pick("JIAN_CAI_YIN", "伤官兼用财印（两清不相碍）")
                    hasSha && hasYin -> pick("YONG_SHA_YIN", "伤官用煞印")
                    hasSha -> pick("DAI_SHA", "伤官带煞")
                    hasCai -> pick("YONG_CAI", "伤官用财（化伤生财）")
                    hasYin -> pick("PEI_YIN", "伤官佩印")
                    hasGuan && jinShui -> pick("YONG_GUAN", "伤官用官（金水独宜）")
                    else -> pick("SHANG", if (jinShui) "金水伤官" else "伤官（忌见官，非金水）")
                }
            }
            Family.YANG_REN -> when {
                hasGuan && hasSha -> pick("GUAN_SHA", "阳刃官煞并出（取清）")
                hasGuan -> pick("YONG_GUAN", "阳刃用官（官制刃）")
                hasSha -> pick("YONG_SHA", "阳刃用煞（煞制刃）")
                else -> pick("YANG_REN", "阳刃无官煞制伏")
            }
            Family.JIAN_LU -> {
                val t = ys.yongShen?.tenGod
                when (t) {
                    TenGod.ZHENG_GUAN -> pick("YONG_GUAN", "禄劫用官")
                    TenGod.ZHENG_CAI, TenGod.PIAN_CAI -> pick("YONG_CAI", "禄劫用财（带食伤化劫生财）")
                    TenGod.QI_SHA -> pick("YONG_SHA", "禄劫用煞（食制/合煞存财）")
                    TenGod.SHI_SHEN, TenGod.SHANG_GUAN -> pick("YONG_SHI_SHANG", "禄劫用伤食（泄秀）")
                    else -> pick("JIAN_LU", "禄劫（无财官食杀可取）")
                }
            }
            null -> ZiPeiZhi("未定格", "UNKNOWN", "格局未定，取运无从配之", "")
        }
    }

    private fun classify(god: TenGod, r: Row?): Pair<QuYunVerdict, String> {
        if (r == null) return QuYunVerdict.PING to "取运表未载此子配置细则，大略同本命喜忌"
        if (r.ji.any { matches(god, it) }) return QuYunVerdict.JI to r.jiText
        if (r.xi.any { matches(god, it) }) return QuYunVerdict.XI to r.xiText
        return QuYunVerdict.PING to "于本子配置为平运"
    }

    /** 判一运柱（大运或流年）干/支对本命子配置的喜忌。 */
    fun analyze(bazi: BaziResult, ys: YongShenResult, yunPillar: Pillar): QuYunResult {
        val dayMaster = bazi.dayPillar.stem
        val zpz = identifyZiPeiZhi(bazi, ys)
        val table = QUYUN_TABLE[familyOf(ys)] ?: emptyMap()
        val r = table[zpz.subKey]

        val ganGod = tenGodOfStem(dayMaster, yunPillar.stem)
        val zhiGod = tenGodOfBranch(dayMaster, yunPillar.branch)
        val (ganVerdict, ganReason) = classify(ganGod, r)
        val (zhiVerdict, zhiReason) = classify(zhiGod, r)
        val gan = PillarVerdict(ganGod.chinese, ganVerdict, ganReason)
        val zhi = PillarVerdict(zhiGod.chinese, zhiVerdict, zhiReason)

        val verdict = when {
            ganVerdict == QuYunVerdict.XI && zhiVerdict == QuYunVerdict.XI -> QuYunVerdict.XI
            ganVerdict == QuYunVerdict.JI && zhiVerdict == QuYunVerdict.JI -> QuYunVerdict.JI
            ganVerdict == QuYunVerdict.JI || zhiVerdict == QuYunVerdict.JI -> QuYunVerdict.JI_ZHONG_XI
            ganVerdict == QuYunVerdict.XI || zhiVerdict == QuYunVerdict.XI -> QuYunVerdict.XI_ZHONG_PING
            else -> QuYunVerdict.PING
        }
        val summaryText = when (verdict) {
            QuYunVerdict.XI -> "喜运"
            QuYunVerdict.JI -> "忌运"
            QuYunVerdict.JI_ZHONG_XI -> "干支喜忌参半"
            QuYunVerdict.XI_ZHONG_PING -> "干支一喜一平"
            QuYunVerdict.PING -> "平运"
        }

        return QuYunResult(
            ziPeiZhi = zpz,
            gan = gan,
            zhi = zhi,
            summary = QuYunSummary(verdict, summaryText),
            row = r?.let { QuYunRow(it.name, it.xiText, it.jiText, it.note) },
        )
    }
}
