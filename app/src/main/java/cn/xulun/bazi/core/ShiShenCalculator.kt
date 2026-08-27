package cn.xulun.bazi.core

import cn.xulun.bazi.model.BaziResult
import cn.xulun.bazi.model.Element
import cn.xulun.bazi.model.GodCategory
import cn.xulun.bazi.model.HeavenlyStem
import cn.xulun.bazi.model.ShiShenResult
import cn.xulun.bazi.model.TenGod
import cn.xulun.bazi.model.TenGod.ZHENG_YIN
import cn.xulun.bazi.model.branchHiddenStems
import cn.xulun.bazi.model.tenGodOfStem

/**
 * 十神关系（柱位寓意）计算器。理论见 `.shengke-reference.md` 第六三节起。
 *
 * 两条检测线：
 * - **透干十神**（年/月/时干对日主）×柱位 → 寓意表；
 * - **坐支组合**（透干十神, 支藏本气十神）→ 坐支寓意表（如正印坐正财）。
 * 喜忌注记由 BingYao 用神行/忌神行推断。
 */
object ShiShenCalculator {

    private val pillarStemLabels = listOf("年干", "月干", "日干", "时干")
    private val pillarBranchLabels = listOf("年支", "月支", "日支", "时支")

    // ── 正印透干寓意（第六三节一；2=日支本气档并入坐支线）──
    private val zhengYinTou: Map<Int, String> = mapOf(
        0 to "旺相生富贵书香之家；月透正官祖上父辈掌权；喜用利文途；临长生母聪慧贤良体健寿长；" +
            "身旺印旺无制孤寒刑克少子息；他干又透印少时缺乳或饮他人乳（印多克食）或由他人抚养；" +
            "衰弱遭克祖上贫寒幼时家贫；日主当令三透印财不现与妻缘薄婚姻不顺（特殊格局除外）；" +
            "印衰月透劫财祖产多被手足继承",
        1 to "喜用不遭财克学业有成文才出众闻名四方；喜用官星生印身官印均衡功名显达名扬天下；" +
            "年透正官出身显贵；失令遭财克出身贫寒；日干太强支印重为忌月为兄弟宫手足缘薄或兄妹少",
        3 to "喜用子嗣贤孝多生贵子享子女福；晚运佳大器晚成（月正官岁运逢印必发达）；忌神难享子女福；" +
            "喜用财得令官衰不见不通财印之关一生事业难成贫苦命",
        2 to "日支本气为印——喜用得妻妾之助有贵人相帮；忌神夫妻不睦或不利子息",
    )

    // ── 正印坐支寓意（第六三节二：坐支本气十神 → 文案）──
    private val zhengYinZuo: Map<TenGod, String> = mapOf(
        TenGod.ZHENG_YIN to "印太过过于自尊欲望太高反遭失败；忌时辛苦奔波克子星子女缘薄（女命尤甚）",
        TenGod.PIAN_YIN to "多愁善感子女缘薄多败少成（女命尤甚）",
        TenGod.BI_JIAN to "乐于帮助兄弟朋友；喜用事业顺利发达",
        TenGod.JIE_CAI to "为帮兄弟朋友受小损但对财运无大妨碍",
        TenGod.SHANG_GUAN to "组合不好伤官为忌多遇挫折功败垂成；伤官有制另论",
        TenGod.SHI_SHEN to "印为尊敬信任食为财源——名利俱佳吉祥如意",
        TenGod.ZHENG_CAI to "婆媳不睦忧虑多病；财旺为忌难遇良机贫病愁苦",
        TenGod.PIAN_CAI to "财利快意家庭和睦",
        TenGod.ZHENG_GUAN to "正直诚信慈善有德受人尊敬胸怀宽广忍辱负重可成大器；女命贤内助",
        TenGod.QI_SHA to "杀印相生喜用——勤奋上进恩威并具功成名就",
    )

    // ── 偏印透干寓意（第六三节二）──
    private val pianYinTou: Map<Int, String> = mapOf(
        0 to "喜用一定程度具正印正面特性；月干非正印与生母缘薄（喜用得继母慈爱忌神继母刻薄）；" +
            "喜用有力继母或命主精明干练宜副业技术工作；年月时三偏印旺制食伤财无源婚姻不顺；" +
            "忌祖业无靠幼失双亲教养出身贫寒；用被财制坐衰绝亦出身贫寒",
        1 to "身旺为忌霸道顽固惹是生非具侵犯性易招凶灾横祸（财制或吉神合可减免）；财旺身弱失令出身贫困；" +
            "食神受月柱偏印克制行为常被长辈限制缺乏自由；月干支偏印重叠为忌月为兄弟宫手足缘薄；" +
            "枭神具破坏性展示性——宜外科医生助产士爆破手创造者表演自由业服务业",
        3 to "嗜酒好赌好胜；日刃时枭妻子易难产",
        2 to "日支本气偏印——他柱又透偏印为忌姻缘不称意",
    )

    // ── 偏印坐支寓意 ──
    private val pianYinZuo: Map<TenGod, String> = mapOf(
        TenGod.ZHENG_YIN to "可能兼营多行业或多重权力；忌时凡事多迷惑招致损失",
        TenGod.PIAN_YIN to "身强为忌辛苦奔波生计难为易失权柄身患暗疾慢性病，易被偷盗遭火灾不测",
        TenGod.BI_JIAN to "喜用得贵人暗中相助；多为养子或有继母劳碌波折，经营副业易遭损失",
        TenGod.JIE_CAI to "忌时凶兆劳神费力多败少成；不宜合伙（好景不长终致亏损），婚姻多不顺",
        TenGod.SHANG_GUAN to "忌时灾祸频发经济拮据生活清苦破家散财；女命克夫克子",
        TenGod.SHI_SHEN to "忌时枭神夺食败事破财先成后败凡事无善终；女命难产子女缘薄",
        TenGod.ZHENG_CAI to "喜得长辈上级提拔经营偏业易发达扬名；组合好名利双全",
        TenGod.PIAN_CAI to "忌得偏财制逢凶化吉；喜用被偏财重克损名誉事业",
        TenGod.ZHENG_GUAN to "外实内虚虽有地位权力实际利益不大；女命夫缘不佳",
        TenGod.QI_SHA to "不够稳沉易被人利用劳而无功谋事多败少成；抗上不驯易遭打击",
    )

    // ── 正官透干寓意（第六三节三）──
    private val zhengGuanTou: Map<Int, String> = mapOf(
        0 to "旺相祖上父母显贵；喜用有力少年得志获祖荫事业多长辈权贵扶助；月透正财或正印多为长子主理家政；" +
            "他柱官杀太重身弱不胜夭折之命（印化官杀可免但仍多灾出身寒微）；忌难得祖业；坐空亡难承祖业",
        1 to "独透有根喜用别无官杀身官两停大贵之命；年时又官杀财不露无印平庸（从杀格年时有合例外）；" +
            "非长子官过旺克身易受兄长欺凌（官失令有印深得父母宠爱）；月支本气亦官不遭刑冲身强有印——" +
            "心地善良果敢有为建功立业大富大贵（官被合化破贵气）；得令身弱无印化——体弱多病压力重重怯懦无为" +
            "（女命婚姻不如意易受夫欺凌，再官杀多夫）；月为婚姻宫女命正官被邻柱刑克初恋婚姻易失败",
        2 to "喜用婚姻美满——男得贤妻多名门闺秀，女得贵夫（别柱不另见官星）享夫荣终身幸福；忌婚姻受挫" +
            "夫妻不睦因婚有损终身所累；身强好学上进工于心计应变力强（身弱相反）",
        3 to "喜用有力晚年发达大器晚成；有根喜用子女贤孝（无根虽忠孝无才能）；身强官为用护身逢凶化吉",
    )

    // ── 正官坐支寓意 ──
    private val zhengGuanZuo: Map<TenGod, String> = mapOf(
        TenGod.ZHENG_YIN to "喜用官印相生——功名显达",
        TenGod.PIAN_YIN to "颇具城府，机巧谋略能成大事",
        TenGod.ZHENG_GUAN to "忌时官多为杀——压力重重多忧少乐",
        TenGod.QI_SHA to "劳心苦身受排斥歧视谗言难得上司重用，或公职累己；女命桃色纠纷多夫",
        TenGod.ZHENG_CAI to "身旺担财发家名利双全",
        TenGod.PIAN_CAI to "喜用得长辈权贵提携创业经商财运旺可成富翁",
        TenGod.SHANG_GUAN to "身弱为忌遭人陷害损名位婚姻不幸",
        TenGod.SHI_SHEN to "喜时财官俱得子女有为；女命婚姻美满",
        TenGod.BI_JIAN to "身强为喜威严可敬具领导气质",
        TenGod.JIE_CAI to "手足失和易遭手足朋友连累防色情之灾",
    )

    fun analyze(bazi: BaziResult): ShiShenResult {
        val dayMaster = bazi.dayPillar.stem
        val items = mutableListOf<ShiShenResult.ShiShenItem>()
        val xiJiMap = buildXiJi(bazi)

        fun lineOf(stem: HeavenlyStem) = stem.element
        fun add(label: String, god: TenGod, el: Element, text: String?) {
            if (text != null) items += ShiShenResult.ShiShenItem(label, god, text, xiJiMap[el] ?: "中性")
        }

        // ① 透干十神 ×柱位（日干跳过；日支档=支藏本气十神——正官日支本气档等）
        for (i in listOf(0, 1, 3)) {
            val st = bazi.pillars[i].stem
            val g = tenGodOfStem(dayMaster, st)
            add(pillarStemLabels[i] + g.chinese, g, lineOf(st), touText(g, i))
        }
        run {
            val hm = branchHiddenStems.getValue(bazi.dayPillar.branch).first()
            val g = tenGodOfStem(dayMaster, hm)
            add("日支本气" + g.chinese, g, lineOf(hm), touText(g, 2))
        }
        // ② 坐支组合：透干十神(日支则支藏本气) × 支藏本气十神
        for (i in 0..3) {
            val hiddenMain = branchHiddenStems.getValue(bazi.pillars[i].branch).first()
            val zuo = tenGodOfStem(dayMaster, hiddenMain)
            val tou: TenGod = if (i == 2) zuo else tenGodOfStem(dayMaster, bazi.pillars[i].stem)
            add(pillarBranchLabels[i] + "坐" + zuo.chinese + "（" + tou.chinese + "坐" + zuo.chinese + "）", tou, lineOf(hiddenMain), zuoText(tou, zuo, i))
        }
        return ShiShenResult(items)
    }

    /** 透干寓意查表（正印/偏印成表；其余十神待教材续节扩展）。 */
    private fun touText(g: TenGod, pillar: Int): String? = when (g) {
        ZHENG_YIN -> zhengYinTou[pillar]
        TenGod.PIAN_YIN -> pianYinTou[pillar]
        TenGod.ZHENG_GUAN -> zhengGuanTou[pillar]
        else -> null
    }

    /** 坐支寓意查表 (透干十神, 坐支本气十神)。 */
    private fun zuoText(tou: TenGod, zuo: TenGod, pillar: Int): String? = when (tou) {
        ZHENG_YIN -> zhengYinZuo[zuo]
        TenGod.PIAN_YIN -> pianYinZuo[zuo]
        TenGod.ZHENG_GUAN -> zhengGuanZuo[zuo]
        else -> null
    }

    /** 喜忌注记：BingYao 用神行/忌神行。 */
    private fun buildXiJi(bazi: BaziResult): Map<Element, String> {
        val r = BingYaoCalculator.analyze(bazi)
        val map = mutableMapOf<Element, String>()
        map[r.yongShenElement] = "喜用"
        val dayEl = bazi.dayPillar.stem.element
        r.jiShen.forEach { j ->
            val el = when (j) {
                GodCategory.BI -> dayEl
                GodCategory.YIN -> Element.entries.first { it.generates == dayEl }
                GodCategory.SHI -> dayEl.generates
                GodCategory.CAI -> dayEl.controls
                GodCategory.GUAN -> Element.entries.first { it.controls == dayEl }
            }
            if (el !in map) map[el] = "忌神"
        }
        return map
    }
}
