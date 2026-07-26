package cn.xulun.bazi.model

/** 奇门遁甲用干（六仪三奇，遁甲序）。戊己庚辛壬癸为六仪，丁丙乙为三奇（逆序）。 */
enum class QiGan(val chinese: String, val stem: HeavenlyStem, val isQi: Boolean) {
    WU("戊", HeavenlyStem.WU, false),
    JI("己", HeavenlyStem.JI, false),
    GENG("庚", HeavenlyStem.GENG, false),
    XIN("辛", HeavenlyStem.XIN, false),
    REN("壬", HeavenlyStem.REN, false),
    GUI("癸", HeavenlyStem.GUI, false),
    DING("丁", HeavenlyStem.DING, true),
    BING("丙", HeavenlyStem.BING, true),
    YI("乙", HeavenlyStem.YI, true);

    companion object {
        /** 遁甲布列顺序（戊→…→乙） */
        val order: List<QiGan> = entries
        /** 由天干反查（甲不在其中） */
        fun of(stem: HeavenlyStem): QiGan? = entries.firstOrNull { it.stem == stem }
    }
}

/** 六甲旬首地支 → 所遁之仪（甲子戊、甲戌己、甲申庚、甲午辛、甲辰壬、甲寅癸） */
val xunShouDunYi: Map<EarthlyBranch, QiGan> = mapOf(
    EarthlyBranch.ZI to QiGan.WU,
    EarthlyBranch.XU to QiGan.JI,
    EarthlyBranch.SHEN to QiGan.GENG,
    EarthlyBranch.WU to QiGan.XIN,
    EarthlyBranch.CHEN to QiGan.REN,
    EarthlyBranch.YIN to QiGan.GUI,
)

/** 洛书九宫（后天八卦），palaceNo = 洛书号 1..9 */
enum class QiPalace(
    val no: Int,
    val trigram: String,
    val element: Element,
    val direction: String,
) {
    KAN(1, "坎", Element.WATER, "北"),
    KUN(2, "坤", Element.EARTH, "西南"),
    ZHEN(3, "震", Element.WOOD, "东"),
    XUN(4, "巽", Element.WOOD, "东南"),
    ZHONG(5, "中", Element.EARTH, "中宫"),
    QIAN(6, "乾", Element.METAL, "西北"),
    DUI(7, "兑", Element.METAL, "西"),
    GEN(8, "艮", Element.EARTH, "东北"),
    LI(9, "离", Element.FIRE, "南");

    companion object {
        fun of(no: Int): QiPalace = entries.first { it.no == no }
        /** 后天八卦顺时针外八宫环序（自坎起）：坎→艮→震→巽→离→坤→兑→乾 */
        val ring: List<QiPalace> = listOf(KAN, GEN, ZHEN, XUN, LI, KUN, DUI, QIAN)
        fun ringIndexOf(no: Int): Int = ring.indexOfFirst { it.no == no }
    }
}

/** 九星（带本位宫洛书号与五行） */
enum class NineStar(val chinese: String, val homePalace: Int, val element: Element, val auspicious: Boolean) {
    PENG("天蓬", 1, Element.WATER, false),
    RUI("天芮", 2, Element.EARTH, false),
    CHONG("天冲", 3, Element.WOOD, true),
    FU("天辅", 4, Element.WOOD, true),
    QIN("天禽", 5, Element.EARTH, true), // 寄中宫
    XIN_S("天心", 6, Element.METAL, true),
    ZHU("天柱", 7, Element.METAL, false),
    REN_S("天任", 8, Element.EARTH, true),
    YING("天英", 9, Element.FIRE, false);

    companion object {
        fun atPalace(no: Int): NineStar = entries.first { it.homePalace == no }
    }
}

/** 八门（带本位宫洛书号与吉凶） */
enum class EightDoor(val chinese: String, val homePalace: Int, val auspicious: Boolean) {
    XIU("休", 1, true),
    SI("死", 2, false),
    SHANG("伤", 3, false),
    DU("杜", 4, false),
    JING("景", 9, true),
    KAI("开", 6, true),
    JING2("惊", 7, false),
    SHENG("生", 8, true);

    companion object {
        fun atPalace(no: Int): EightDoor? = entries.firstOrNull { it.homePalace == no }
    }
}

/** 八神（顺序：直符起，阳遁顺布阴遁逆布） */
enum class EightSpirit(val chinese: String, val auspicious: Boolean) {
    ZHI_FU("直符", true),
    TENG_SHE("腾蛇", false),
    TAI_YIN("太阴", true),
    LIU_HE("六合", true),
    BAI_HU("白虎", false),
    XUAN_WU("玄武", false),
    JIU_DI("九地", true),
    JIU_TIAN("九天", true);
}

/** 三元 */
enum class Yuan(val chinese: String) { SHANG("上元"), ZHONG("中元"), XIA("下元") }

/** 阴阳遁 */
enum class Dun(val chinese: String) { YANG("阳遁"), YIN("阴遁") }

/** 定局法 */
enum class JuMethod(val chinese: String, val desc: String) {
    CHAIBU("拆补法", "以实际节气定局，残候补回，不置闰（现代主流）"),
    CHAOJIE("超神接气", "符头定元，大雪/芒种超神≥9天置闰（古法）"),
}

/** 定局结果 */
data class JuResult(
    val dun: Dun,
    val ju: Int,            // 1..9
    val termName: String,   // 当前节气名
    val termIndex: Int,     // termNames 序号
    val yuan: Yuan,
    val method: JuMethod,
    val fuTouName: String,  // 符头干支
    val isLeap: Boolean,    // 置闰法命中闰候
)

/** 单宫排盘内容 */
data class PalaceCell(
    val palace: QiPalace,
    val star: NineStar,
    val door: EightDoor?,      // 中宫无门
    val spirit: EightSpirit?,  // 中宫一般无神（直符落中宫时寄）
    val heavenGan: QiGan,      // 天盘干
    val earthGan: QiGan,       // 地盘干
    val isZhiFuPalace: Boolean,
    val isZhiShiPalace: Boolean,
)

/** 奇门盘 */
data class QiMenPlate(
    val cells: Map<Int, PalaceCell>,   // key = 洛书宫号 1..9
    val zhiFuStar: NineStar,
    val zhiShiDoor: EightDoor,
    val ju: JuResult,
    val dayPillar: Pillar,
    val hourPillar: Pillar,
    val jiGong: String,   // 中宫寄宫说明
)

/** 格局 */
data class QiPattern(
    val name: String,
    val auspicious: Boolean,
    val basis: String,
)

/** 单宫分析条目语气（用于着色） */
enum class PalaceTone { GOOD, BAD, NEUTRAL }

/** 单宫分析的一条判读 */
data class PalaceAnalysisItem(
    val label: String,
    val text: String,
    val tone: PalaceTone = PalaceTone.NEUTRAL,
)

/** 单宫分析结果：宫位信息 + 各层生克判读 + 本宫格局 + 综合断语 */
data class PalaceAnalysis(
    val palaceNo: Int,
    val title: String,        // 如「离9宫」
    val subtitle: String,     // 如「南方 · 火地」
    val tags: List<String>,   // 直符宫/直使宫/空亡 等
    val items: List<PalaceAnalysisItem>,
    val patterns: List<QiPattern>,   // 落在本宫的格局
    val summary: String,
)
