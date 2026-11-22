package cn.xulun.bazi.model

/**
 * 盲派金镖门分析结果类型。讲义存档见 `.jinbiao-reference.md`。
 *
 * 口径总纲（第一讲）：用神＝命局中对日主最需要、最有帮助的一种五行之神（十神代称）。
 * 吉凶铁律：帮扶用神（生用/同用）则吉；耗泄克破用神（用所克=耗、用所生=泄、克用=克、冲刑破害=破）则凶。
 * 立场一律**以用神为中心**，不以日主为中心（第三讲从格验证：从杀格印泄用、比耗用，帮身反凶）。
 */
/** 运岁五行相对用神的立场 */
enum class JinBiaoStance(val chinese: String, val auspicious: Boolean) {
    /** 同用神 / 生用神 */
    BANG("帮扶用神", true),
    /** 耗用（用所克）/ 泄用（用所生）/ 克用（克用神） */
    SUN("耗泄克破", false),
}

/** 用神健全度（第二讲：用神得力而健全则八字好，无气或受伤则不好） */
enum class JinBiaoHealth(val chinese: String, val auspicious: Boolean) {
    DE_LI("得力健全", true),
    DAI_SHANG("带伤", false),
    WU_QI("无气受伤", false),
}

/** 大运/流年总评 */
enum class JinBiaoVerdict(val chinese: String) {
    JI("吉"),
    /** 财解围局行官/食运：稍差一点，但不会有什么大问题 */
    PING("中平"),
    XIONG("凶"),
    GAN_ZHI_FEN("干支分论"),
}

/** 弱极分型（第三讲：弱极日元，权威定论 100% 应验） */
enum class JinBiaoRuoJiType(val chinese: String) {
    /** 日元孤立无气，四柱毫无生扶；一点根都没有，全是克耗泄 */
    GU_LI("孤立无气·毫无生扶"),
    /** 微浮印比而印比自顾不暇（无根被克合，不能帮身） */
    WEI_FU("微浮印比·自顾不暇"),
}

/** 旺极/强极分型（第三讲续：两种旺到极端） */
enum class JinBiaoWangJiType(val chinese: String) {
    /** 四柱皆为比劫，无有官杀食伤；即或一二印枭，必然随其旺神 */
    BI_DANG_SHI("比劫当势·随其旺神"),
    /** 命局印比叠叠，绝无财官食伤；印比二人同心，可顺而不可逆 */
    YIN_BI_TONG_XIN("印比同心·强旺之极"),
}

/** 取用路径 */
enum class JinBiaoQuYongWay(val chinese: String) {
    /** 官旺随官（从官杀） */
    SUI_GUAN("官旺随官"),
    /** 财旺随财（从财） */
    SUI_CAI("财旺随财"),
    /** 财官食神并存，必要财运解围 */
    CAI_JIE_WEI("财官食并存·财运解围"),
    /** 独旺从儿（食伤旺随食伤） */
    SUI_SHI("食伤独旺·从儿"),
    /** 旺极专旺：必然随其旺神，用神取比劫（日主本行） */
    SUI_WANG("旺极随旺神"),
    /** 普通身弱：印比扶身，取有力者 */
    FU_SHEN("身弱扶身"),
    /** 案廿四：身弱财党杀攻身（财破印＋财滋杀克身）——非贫即夭 */
    GONG_SHEN("财党杀攻身·非贫即夭"),
    /** 第四讲五理③：身衰遇官煞，枭印乃可化——身弱官杀有力必用印化杀 */
    HUA_SHA("身衰遇官煞·枭印化杀"),
    /** 第四讲五理④：身强遇官煞，伤食显其威——官旺必用食伤制杀制官 */
    ZHI_SHA("身强制杀·伤食显威"),
    /** 第五讲推命经第二段·去寡：身旺通根＋官杀无根气＋财无根不生官＋有食伤——官反以去之为美 */
    QU_GUA("身旺官寡·去官为美"),
    /** 第四讲五理②／第七讲：官煞依乎财——身旺官弱用官配财（财滋弱杀），财生官 */
    PEI_CAI("身旺官弱·财滋弱杀"),
    /** 第四讲五理①：身旺印起坏作用——用财星坏印 */
    HUAI_YIN("身旺印坏·财星坏印"),
    /** 第四讲续理⑥：身旺无官杀——食伤泄体生财（食旺无财亦发泄，好命） */
    SHI_SHENG_CAI("身旺无官·食伤泄秀"),
    /** 第十讲案五十四·身旺食伤喜生财而财有力（当令有源有本气根）——用神即财星；怕比劫夺财（无官护则配置不足起伏大） */
    SHI_CAI_YONG_CAI("身旺食伤生财·用神财星"),
    /** 第十一讲案六十二·身旺无依：印得局身旺而克耗泄全废（官被化尽、财不载无根、食伤根随局化）——喜用没有力量，一事无成；好运温饱、差运穷死 */
    SHEN_WANG_WU_YI("身旺无依·喜用全废"),
    /** 身强：克泄耗取有力者（基线，细则待师传） */
    YI_SHEN("身强抑泄"),
    /** 第八讲案卅五·合官留杀：食伤制官杀而一官干合住食伤——清格，留官杀为用，有情豁免旺衰 */
    HE_LIU("合官留杀·清格"),
    /** 第八讲案卅六·合而化：被合干根气太弱从化（戊癸化火）——火生土、解不开克，无情，用神被制太坏 */
    HUA_WU_QING("合而化·无情"),
    /** 第十讲案四十九·伤官喜官：身旺伤官生财、财被比劫夺、官克比劫救财——用神转官，伤官可以见官 */
    SHANG_JIAN_GUAN("伤官喜官·克劫护财"),
    /** 第十讲案五十二·伤官用劫：身弱泄体过而无印（藏支之印不算有印）——用神一定就是比劫；比劫夺财是好事；最怕见官（伤官见官为祸百端＝用比劫见官，官克比劫） */
    SHANG_YONG_JIE("伤官用劫·比劫帮身"),
    /** 第六讲案十五·气象调候：寒局而调候神（火）有气——得气之寒，遇暖而发 */
    TIAO_HOU("气象调候·遇暖而发"),
    /** 第五讲命三·昆仑得局：地支一五行得局（≥3支）配生源（如四水配金生）——不以日主论，顺其势 */
    KUN_LUN("昆仑得局·顺其势"),
    /** 中和：暂取月令主力（基线，细则待师传） */
    ZHONG_HE("中和取月令"),
}

/** 十神代五行条目（第一讲：十神是五行的代名词） */
data class JinBiaoDaiMing(
    val relation: String,   // 同我/我生/我克/生我/克我
    val element: Element,
    val category: String,   // 比劫/食伤/财星/印绶/官杀
)

/** 干支对用神的一条作用记录 */
data class JinBiaoStanceItem(
    val label: String,      // 如 "运干辛" / "2007丁亥·干"
    val chinese: String,    // 干支汉字
    val element: Element,
    val stance: JinBiaoStance,
    /** 生用神/同用神/泄用神/耗用神/克用神（＋档位注记） */
    val action: String,
    /** 案三补丁：强极得局官杀运化煞生印且运支合入印局——作吉论 */
    val favorOverride: Boolean = false,
    /** 案三补丁：强极得局官杀运化煞生印（未合局）——作中平论 */
    val pingOverride: Boolean = false,
    /** 命一补丁：旺强极局官杀干直克身——犯旺激怒，权在一人不可触犯，作凶论 */
    val xiongOverride: Boolean = false,
)

/** 用神健全度明细 */
data class JinBiaoYongShenHealth(
    val level: JinBiaoHealth,
    /** 通根情况：通本气根/藏干根/无根虚浮 */
    val rootNote: String,
    /** 得生得扶明细（生用、同用之字及位置） */
    val supports: List<String>,
    /** 受克受泄受耗受冲合明细 */
    val damages: List<String>,
)

/** 流年评断（以用神为中心） */
data class JinBiaoLiuNianPing(
    val year: Int,
    val pillarName: String,
    val items: List<JinBiaoStanceItem>,
    val verdict: JinBiaoVerdict,
)

/** 大运评断（以用神为中心） */
data class JinBiaoYunPing(
    val yunName: String,        // 如 "甲子运"
    val ageRange: String,       // 如 "4-13岁"
    val yearRange: String,      // 如 "1982-1991"
    val items: List<JinBiaoStanceItem>,  // 运干+运支
    val verdict: JinBiaoVerdict,
    val years: List<JinBiaoLiuNianPing>,
)

/** 清浊三档（第六讲） */
enum class JinBiaoQingZhuo(val chinese: String, val auspicious: Boolean?) {
    /** 用神有力＋无病神（或病被通关化掉／药制）——富贵真，走什么运都不怕 */
    QING("清·贵命", true),
    /** 病神贴身紧克且根大力强（地支全是病根）——差命穷命，走什么运也好不了 */
    MAN_ZHUO("满盘浊气·差命", false),
    /** 病神力小用神稍大——普通人之命，多成多败好两年坏两年 */
    BAN_QING("半浊半清·多成多败", null),
    /** 案十二：官食财俱各有浊神克之，用神无着落——一事无成，比普通人还差 */
    QUAN_ZHUO("全浊·用神无着落，一事无成", false),
    /** 案十四：用神在而被围死（蒸坏＋克坏）——一点救都没有，走好运只得温饱 */
    QING_KU("清枯·病大无救，好运仅温饱", false),
}

/** 子女四情况（第十一讲：必有儿／必无儿／无儿有女／无儿无女损子——逃不出这个圈） */
enum class JinBiaoZiNvQingKuang(val chinese: String) {
    BI_YOU_ER("必有儿"),
    BI_WU_ER("必无儿"),
    WU_ER_YOU_NV("无儿有女"),
    WU_ER_WU_NV("无儿无女·损子"),
}

/** 子女判读（第十一讲·子星按原局喜忌而定——每十神皆可为子，非子女宫非固定十神） */
data class JinBiaoZiNv(
    /** 四情况 */
    val qingKuang: JinBiaoZiNvQingKuang,
    /** 子星十神（印做儿／财为子／官杀为儿／食伤为儿／比劫为儿） */
    val ziXing: TenGod?,
    /** 子星五行 */
    val ziEl: Element?,
    /** 判读依据（判定树逐层检查＋子星昌衰＋克神运） */
    val basis: String,
)

/** 学历高低（案九十四·学历章开讲：印星原局不坏＝脑子没问题；学历高低看求学运——运干生印/同印即有学历，地支不载不碍） */
enum class JinBiaoXueLiLevel(val chinese: String) {
    /** 求学运一路帮印（好运）——百分之百学历高 */
    GAO("学历高"),
    /** 运干生印/同印（甲申乙酉——地支不载而天干生火）——可以有学历、可以上大学 */
    YOU("有学历·可上大学"),
    /** 北方晦火运／运干不帮印——学历不高；然原局印没坏：脑子没问题、学习不错或好学 */
    BU_GAO("学历不高·脑子没问题好学"),
    /** 原局印星被破坏——此端框架待师传 */
    YIN_HUAI("印被破坏·待师传"),
}

/** 学历判读（案九十四：用神为印之局适用） */
data class JinBiaoXueLi(
    /** 印星原局是否完好（没被贴克/根未拔——头脑底子没问题） */
    val yinWanHao: Boolean,
    val level: JinBiaoXueLiLevel,
    val basis: String,
)

/** 金镖门用神分析总结果 */
data class JinBiaoResult(
    /** 口径说明（模块页眉展示） */
    val kouJing: String,
    val strength: DayStrength,
    val strengthBasis: String,
    /** 是否弱极从势（第三讲） */
    val ruoJi: Boolean,
    /** 弱极分型 */
    val ruoJiType: JinBiaoRuoJiType?,
    /** 弱极判定依据 */
    val ruoJiBasis: String?,
    /** 是否旺极专旺（第三讲续） */
    val wangJi: Boolean,
    /** 旺极分型 */
    val wangJiType: JinBiaoWangJiType?,
    /** 旺极判定依据 */
    val wangJiBasis: String?,
    /** 取用路径 */
    val quYongWay: JinBiaoQuYongWay,
    /** 用神十神（十神代五行之十神） */
    val yongShenGod: TenGod,
    /** 用神五行 */
    val yongShenElement: Element,
    /** 命中用神之字及位置（如 "月干辛（正印）"） */
    val yongShenStars: List<String>,
    /** 取用依据 */
    val quYongBasis: String,
    /** 十神代五行表 */
    val daiMing: List<JinBiaoDaiMing>,
    /** 用神健全度 */
    val health: JinBiaoYongShenHealth,
    /** 帮用五行（喜） */
    val xiElements: List<Element>,
    /** 损用五行（忌） */
    val jiElements: List<Element>,
    /** 大运流年评断（无大运时为空） */
    val yuns: List<JinBiaoYunPing>,
    /** 清浊档位（第六讲） */
    val qingZhuo: JinBiaoQingZhuo,
    /** 清浊判据 */
    val qingZhuoBasis: String,
    /** 综合断语（一律锚定用神，禁泛断） */
    val duanYu: String,
    /** 子女判读（第十一讲；极端格局未判时为 null） */
    val ziNv: JinBiaoZiNv? = null,
    /** 学历判读（案九十四·学历章；用神非印时为 null） */
    val xueLi: JinBiaoXueLi? = null,
    /** 校准注记 */
    val note: String,
)
