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

    // ── 偏官透干寓意（第六三节四）──
    private val pianGuanTou: Map<Int, String> = mapOf(
        0 to "一般非长子；身强见刃行财运生杀富贵双全贵格；身弱忌受人欺侮体弱多病；" +
            "忌祖业难承七杀攻身太过出身寒微一生多病（女命婚姻不幸）",
        1 to "一般非长子；月支本气为印组合好多属贵格；食神制而得用——富贵",
        2 to "别无官杀格局清秀喜杀——聪明机巧应变力强做事果决雷厉风行（性急之嫌）；" +
            "身弱忌杀+柱再有官杀财——攻身太过（日支七杀乃日主截脚克力甚大）一生多灾多病事多波折，" +
            "再逢官杀岁运有生命之忧；忌——配偶暴躁夫妻不睦体弱多病（食制印化化凶为吉）；" +
            "身强喜用+杀处墓绝——有志难伸功名难就",
        3 to "喜用他柱无官杀（或被合化）贵命晚年尤佳；不宜他干再见正官（合官留杀另论），" +
            "有印官印身流转停匀——文武全才大贵；独透柱透财印日主有力——财丰位高名扬四方；" +
            "独透喜用男得贤孝子晚年享子福；忌男多不肖子为子操心",
    )

    // ── 偏官坐支寓意 ──
    private val pianGuanZuo: Map<TenGod, String> = mapOf(
        TenGod.ZHENG_YIN to "杀躁烈印引善负面抑制正面显——聪明果敢正直好义威德并具，开拓不冒进事业大成；女得佳偶婆媳好",
        TenGod.PIAN_YIN to "行为冒进性躁易怒；逆忤上司遭祸（女尤甚）",
        TenGod.ZHENG_GUAN to "身弱缺主见意志不坚不宜官场；女婚姻不顺防情海纠纷",
        TenGod.QI_SHA to "身弱最忌多谋少成事烦恼，或盲目冒进或胆小退却遭排挤难得子福；女为夫累忧郁沉闷",
        TenGod.ZHENG_CAI to "身强宜工商业可望大富",
        TenGod.PIAN_CAI to "易为金钱失义失和不宜工业，偏业可获利但波折动荡大",
        TenGod.SHANG_GUAN to "身弱忌易惹事端受陷害遭不测，子息难靠体弱多病（印化可解）",
        TenGod.SHI_SHEN to "喜欢自由性格外向，事业有成有败宜把握时机见好即收；女婚姻有阻",
        TenGod.BI_JIAN to "与兄弟朋友易矛盾受损失，防欺骗暗算；女受夫连累",
        TenGod.JIE_CAI to "仗义疏财取财或有不正；易生异性风波女多婚",
    )

    // ── 正财透干寓意（第六三节五）──
    private val zhengCaiTou: Map<Int, String> = mapOf(
        0 to "上辈富足——喜用承福荫忌难得祖产；忌出身贫寒幼时经济困难；他柱伤食财生助过强旺必克母亲文途有阻；父慷慨乐助",
        1 to "得令双亲富贵但勤俭持家；年劫财父辈先贫后富时劫财先富后贫；身强得父母资财兄弟资助；" +
            "月为婚姻宫喜用男婚早妻为近处人忌初恋不易成功；勤劳朴实节俭善积蓄（吝啬小气之嫌）",
        2 to "身强喜财有气得贤淑之妻因妻致富；临将星娶名门闺秀；身强胜财事业心强富责任感努力可致富；" +
            "不遭刑冲合害别无财星夫妻情浓忠贞纯良；财星不与日柱合而与他柱合妻易外遇；日支被合岁运引发婚姻变故；" +
            "日支遭刑冲合害婚姻总有不称心",
        3 to "喜用晚年发富（忌奔波辛苦少得）；喜用子女成家后富贵晚年享子女福；临桃花喜用中年以后获美妻良缘因此发富",
    )

    // ── 正财坐支寓意 ──
    private val zhengCaiZuo: Map<TenGod, String> = mapOf(
        TenGod.ZHENG_YIN to "印主名被财盖头——过分贪财名誉地位有损；财为妻印为母同柱母妻不睦",
        TenGod.PIAN_YIN to "欠诚实缺毅力；人缘好副业获大利",
        TenGod.ZHENG_GUAN to "正直有为威望高获上级提携名利兼得；女必得佳夫一生幸福",
        TenGod.QI_SHA to "夫妻情浓恩施于下受人拥护从商高利；女挚爱夫君如鸳鸯",
        TenGod.ZHENG_CAI to "经商获大利信守诺言；男婚美女少子女",
        TenGod.PIAN_CAI to "收入多开支亦大兼营多行业获利但辛苦；注意桃色纠纷",
        TenGod.SHANG_GUAN to "伤官生财身强喜用佳；身弱忌求财反破财劳心费力",
        TenGod.SHI_SHEN to "身强喜用妻贤子孝幸福有加",
        TenGod.BI_JIAN to "财缘佳女缘佳，为兄弟亲友破费小财财路宽广",
        TenGod.JIE_CAI to "身弱忌糊涂君子不利父难得父产纵得亦少",
    )

    // ── 偏财透干寓意（第六三节六）──
    private val pianCaiTou: Map<Int, String> = mapOf(
        0 to "旺相父长寿；旺相喜用祖业丰余得祖荫；忌幼年家贫；早恋（忌不易成婚）；" +
            "年干支偏财重叠多父之兆；坐驿马父远方创业（遭克过重恐客死他乡）",
        1 to "年干劫财先贫后富；时干劫财先富后贫；忌挥霍浪费家计不丰；身弱逢月干支偏财易为异性破费引桃色风波；" +
            "偏财强旺克印太过身弱顽固不化文运不畅不重礼节为人粗俗",
        2 to "年月柱再有财星婚前多恋结发妻非第一恋人；他柱再无财星妻聪明善待丈夫；衰弱妻多病财运不佳；" +
            "临将星妻出名门有气质；月干正财妾夺妻权多爱妾少爱妻",
        3 to "身强晚年发富；旺子女成家而富；喜用老来享子女福（忌难靠子女）",
    )

    // ── 偏财坐支寓意 ──
    private val pianCaiZuo: Map<TenGod, String> = mapOf(
        TenGod.ZHENG_YIN to "喜获福匪浅忌损名节",
        TenGod.PIAN_YIN to "偏激劳苦拖累宜偏业",
        TenGod.ZHENG_GUAN to "责任心强得父爱谋事易成",
        TenGod.QI_SHA to "经营得利辛劳难免；为女人散财女婚姻波折",
        TenGod.ZHENG_CAI to "身强财利丰厚不免辛劳易为女人破费；女再婚之虑",
        TenGod.PIAN_CAI to "富经济头脑经营路子宽身强财源滚滚宜出外发展",
        TenGod.SHANG_GUAN to "身强财源不断别人的失败利自己的成功；经营富谋略手段高明",
        TenGod.SHI_SHEN to "身强最宜求财左右逢源获厚利，得父或妾助如虎添翼；从政名利双收",
        TenGod.BI_JIAN to "财运不佳易为兄弟朋友破财防桃色破财伤身败名；偏财弱比肩强父体弱多病",
        TenGod.JIE_CAI to "易散财财产分配兄弟纠纷为女人情海风波；父烦恼多病（透劫尤甚）",
    )

    // ── 伤官透干寓意（第六三节七）──
    private val shangGuanTou: Map<Int, String> = mapOf(
        0 to "难承祖业与父母缘薄为生计劳累奔波；旺而无制父脾气暴躁；身强喜幼时家境富裕（身弱忌贫苦）；" +
            "身弱忌因痼疾不治而折寿（印制可解）；年时伤官出干难养儿子",
        1 to "手足失和不利婚姻；女命月干支皆伤官夫缘极差；强旺易被人攻击帮人反遭恩将仇报；" +
            "性高傲过旺个性太强言语偏激树敌；得令温柔不足（透正印口恶心善）；喜标新立异否定别人" +
            "逞强好胜心服口不服；泄身太过反主贫困自作聪明；坐日刃年轻时多为别人办事不能独立创业",
        2 to "清高性急机敏官场失利无远虑有近忧；女命伤官旺不屈服于丈夫克夫（制化无妨）；" +
            "太旺男娶美貌妻高才女难长相厮守终必离婚；有印制贵命；他柱劫财太重贫困",
        3 to "旺而无制子女不服管教惹是生非连累父母；男命伤官旺妻子头胎多为女儿；喜用晚年享子女福；" +
            "居子女宫受克太过恐子女夭折；时透伤官时支劫财晚年财缘不佳多有损耗",
    )

    // ── 伤官坐支寓意 ──
    private val shangGuanZuo: Map<TenGod, String> = mapOf(
        TenGod.ZHENG_YIN to "有名望易显达",
        TenGod.PIAN_YIN to "不宜同时经营几种行业否则其中恐有失败",
        TenGod.ZHENG_GUAN to "爱出风头喜骂人易失权柄无视规矩夫妻离别",
        TenGod.QI_SHA to "易惹麻烦难获佳遇得不偿失劳而无功，遭人污陷蒙不白之冤",
        TenGod.ZHENG_CAI to "身强利求财与人合作别人吃亏自己得利，得妻助锦上添花",
        TenGod.PIAN_CAI to "颇具经济头脑善于经营手段高明财利丰盈（日强保财弱易失财）；贪恋色欲谨防致祸",
        TenGod.SHANG_GUAN to "出言不逊无视法纪具侵犯破坏性行为偏激六亲有损（日干弱多病多灾）",
        TenGod.SHI_SHEN to "喜欢自由易与人冲突",
        TenGod.BI_JIAN to "手足失和夫妻口角",
        TenGod.JIE_CAI to "蛮横偏激为财淡泊亲情常失财；不宜合伙求财婚姻不顺",
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
        TenGod.QI_SHA -> pianGuanTou[pillar]
        TenGod.ZHENG_CAI -> zhengCaiTou[pillar]
        TenGod.PIAN_CAI -> pianCaiTou[pillar]
        TenGod.SHANG_GUAN -> shangGuanTou[pillar]
        else -> null
    }

    /** 坐支寓意查表 (透干十神, 坐支本气十神)。 */
    private fun zuoText(tou: TenGod, zuo: TenGod, pillar: Int): String? = when (tou) {
        ZHENG_YIN -> zhengYinZuo[zuo]
        TenGod.PIAN_YIN -> pianYinZuo[zuo]
        TenGod.ZHENG_GUAN -> zhengGuanZuo[zuo]
        TenGod.QI_SHA -> pianGuanZuo[zuo]
        TenGod.ZHENG_CAI -> zhengCaiZuo[zuo]
        TenGod.PIAN_CAI -> pianCaiZuo[zuo]
        TenGod.SHANG_GUAN -> shangGuanZuo[zuo]
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
