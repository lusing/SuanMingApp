package cn.xulun.bazi.model

import java.lang.Math.floorMod

/**
 * 五行（金木水火土）。
 * generates = 我生者（木→火→土→金→水→木）；controls = 我克者（木→土→水→火→金→木）。
 */
enum class Element(val chinese: String) {
    WOOD("木"),
    FIRE("火"),
    EARTH("土"),
    METAL("金"),
    WATER("水");

    /** 我生者 */
    val generates: Element get() = entries[(ordinal + 1) % 5]
    /** 我克者 */
    val controls: Element get() = entries[(ordinal + 2) % 5]
}

/** 天干：甲=0 ... 癸=9（ordinal 即序号） */
enum class HeavenlyStem(
    val chinese: String,
    val pinyin: String,
    val yang: Boolean,
    val element: Element,
) {
    JIA("甲", "jiǎ", yang = true, Element.WOOD),
    YI("乙", "yǐ", yang = false, Element.WOOD),
    BING("丙", "bǐng", yang = true, Element.FIRE),
    DING("丁", "dǐng", yang = false, Element.FIRE),
    WU("戊", "wù", yang = true, Element.EARTH),
    JI("己", "jǐ", yang = false, Element.EARTH),
    GENG("庚", "gēng", yang = true, Element.METAL),
    XIN("辛", "xīn", yang = false, Element.METAL),
    REN("壬", "rén", yang = true, Element.WATER),
    GUI("癸", "guǐ", yang = false, Element.WATER);

    companion object {
        /** 序号自动取模到 0..9（用 floorMod 处理负数） */
        fun fromIndex(index: Int): HeavenlyStem = entries[floorMod(index, 10)]
    }
}

/** 地支：子=0 ... 亥=11（ordinal 即序号） */
enum class EarthlyBranch(
    val chinese: String,
    val pinyin: String,
    val animal: String,
    val element: Element,
) {
    ZI("子", "zǐ", "鼠", Element.WATER),
    CHOU("丑", "chǒu", "牛", Element.EARTH),
    YIN("寅", "yín", "虎", Element.WOOD),
    MAO("卯", "mǎo", "兔", Element.WOOD),
    CHEN("辰", "chén", "龙", Element.EARTH),
    SI("巳", "sì", "蛇", Element.FIRE),
    WU("午", "wǔ", "马", Element.FIRE),
    WEI("未", "wèi", "羊", Element.EARTH),
    SHEN("申", "shēn", "猴", Element.METAL),
    YOU("酉", "yǒu", "鸡", Element.METAL),
    XU("戌", "xū", "狗", Element.EARTH),
    HAI("亥", "hài", "猪", Element.WATER);

    companion object {
        /** 序号自动取模到 0..11（用 floorMod 处理负数） */
        fun fromIndex(index: Int): EarthlyBranch = entries[floorMod(index, 12)]
    }
}

/** 十神（以日主为基准） */
enum class TenGod(val chinese: String) {
    BI_JIAN("比肩"),
    JIE_CAI("劫财"),
    SHI_SHEN("食神"),
    SHANG_GUAN("伤官"),
    PIAN_CAI("偏财"),
    ZHENG_CAI("正财"),
    QI_SHA("七杀"),
    ZHENG_GUAN("正官"),
    PIAN_YIN("偏印"),
    ZHENG_YIN("正印"),
}

/** 性别（大运顺逆需要：阳年男/阴年女顺行，阴年男/阳年女逆行） */
enum class Gender(val label: String) {
    MALE("乾造（男）"),
    FEMALE("坤造（女）"),
}
