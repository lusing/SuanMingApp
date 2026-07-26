package cn.xulun.bazi.model

import java.lang.Math.floorMod

/**
 * 太乙神数数据模型。
 *
 * 四计（岁/月/日/时）共用一盘，仅入局法与阴阳遁判定不同。
 * 规则固化见 G:\code\zhouyi\bazia\.taiyi-reference.md（已验证·五源交叉裁定 + 用户权威讲解）。
 */

/** 太乙九宫（差一宫：1乾 2离 3艮 4震 6兑 7坤 8坎 9巽；独立于奇门 QiPalace，不可复用）。 */
enum class TaiyiPalace(
    val no: Int,
    val trigram: String,
    val element: Element,
    val direction: String,
    val region: String,
    val yang: Boolean,
) {
    QIAN(1, "乾", Element.METAL, "西北", "冀", false),
    LI(2, "离", Element.FIRE, "南", "荆", false),
    GEN(3, "艮", Element.EARTH, "东北", "青", true),
    ZHEN(4, "震", Element.WOOD, "东", "徐", true),
    ZHONG(5, "中", Element.EARTH, "中宫", "", false),
    DUI(6, "兑", Element.METAL, "西", "雍", false),
    KUN(7, "坤", Element.EARTH, "西南", "梁", false),
    KAN(8, "坎", Element.WATER, "北", "兖", true),
    XUN(9, "巽", Element.WOOD, "东南", "扬", true);

    companion object {
        /** 行宫顺行序（跳中五）：1→2→3→4→6→7→8→9 */
        val ring: List<Int> = listOf(1, 2, 3, 4, 6, 7, 8, 9)
        /** 显示方阵（上南下北，太乙数）：[[9,2,7],[4,5,6],[3,8,1]] */
        val gridOrder: List<List<Int>> = listOf(listOf(9, 2, 7), listOf(4, 5, 6), listOf(3, 8, 1))
        fun of(no: Int): TaiyiPalace = entries.first { it.no == no }
        /** 对宫（冲）：乾↔巽 坎↔离 艮↔坤 震↔兑 */
        fun opposite(no: Int): Int = when (no) {
            1 -> 9; 9 -> 1; 2 -> 8; 8 -> 2; 3 -> 7; 7 -> 3; 4 -> 6; 6 -> 4; else -> no
        }
        /** 地内宫（助主）：1,3,4,8；天外宫（助客）：2,6,7,9 */
        fun isInner(no: Int): Boolean = no in setOf(1, 3, 4, 8)
    }
}

/**
 * 十六神（间辰）。idx 0..15 顺时针；8 正神带太乙宫数，8 间辰无。
 * 序：子0 丑1 艮2 寅3 卯4 辰5 巽6 巳7 午8 未9 坤10 申11 酉12 戌13 乾14 亥15。
 * 枚举名对四隅卦加 `_G` 后缀，以区别于 TaiyiPalace/EarthlyBranch 同名项。
 */
enum class SixteenGod(
    val idx: Int,
    val position: String,
    val title: String,
    val isZheng: Boolean,
    val palaceNo: Int?,
) {
    ZI(0, "子", "地主", true, 8),
    CHOU(1, "丑", "阳德", false, null),
    GEN_G(2, "艮", "和德", true, 3),
    YIN(3, "寅", "吕申", false, null),
    MAO(4, "卯", "高丛", true, 4),
    CHEN(5, "辰", "太阳", false, null),
    XUN_G(6, "巽", "大炅", true, 9),
    SI(7, "巳", "大神", false, null),
    WU(8, "午", "大威", true, 2),
    WEI(9, "未", "天道", false, null),
    KUN_G(10, "坤", "大武", true, 7),
    SHEN(11, "申", "武德", false, null),
    YOU(12, "酉", "太簇", true, 6),
    XU(13, "戌", "阴主", false, null),
    QIAN_G(14, "乾", "阴德", true, 1),
    HAI(15, "亥", "大义", false, null);

    /** 所主宫号：正神取本宫数，间辰取顺时针下一正宫宫数。 */
    val governingPalaceNo: Int
        get() = if (isZheng) palaceNo!! else entries.first { it.idx == floorMod(idx + 1, 16) }.palaceNo!!

    companion object {
        fun at(idx: Int): SixteenGod = entries.first { it.idx == floorMod(idx, 16) }
        fun ofBranch(branch: EarthlyBranch): SixteenGod = entries.first { it.position == branch.chinese }
    }
}

/** 四计（共用一盘，仅入局法与阴阳遁判定不同）。 */
enum class TaiyiCountType(val chinese: String) {
    YEAR("年计"), MONTH("月计"), DAY("日计"), HOUR("时计")
}

/** 阴阳遁：岁/月/日计恒阳遁；唯时计冬至后阳遁、夏至后阴遁。 */
enum class TaiyiDun(val chinese: String) { YANG("阳遁"), YIN("阴遁") }

/** 太乙八门（固定卦位）。三吉=开休生；伤/死/惊凶；杜/景中平。auspicious: 1吉 / 0中 / -1凶。 */
enum class TaiyiMen(val chinese: String, val trigram: String, val element: Element, val auspicious: Int) {
    KAI("开", "乾", Element.METAL, 1),
    XIU("休", "坎", Element.WATER, 1),
    SHENG("生", "艮", Element.EARTH, 1),
    SHANG("伤", "震", Element.WOOD, -1),
    DU("杜", "巽", Element.WOOD, 0),
    JING("景", "离", Element.FIRE, 0),
    SI_MEN("死", "坤", Element.EARTH, -1),
    JING2("惊", "兑", Element.METAL, -1);

    companion object {
        /** 顺时针顺布序（飞加太乙宫后按此序排）：开→休→生→伤→杜→景→死→惊 */
        val order: List<TaiyiMen> = listOf(KAI, XIU, SHENG, SHANG, DU, JING, SI_MEN, JING2)
    }
}

/** 局信息。 */
data class TaiyiJu(
    val countType: TaiyiCountType,
    val dun: TaiyiDun,
    val suanBase: Long,        // 积年（岁/月/日计）或 积时（时计）
    val juNo: Int,             // 局数 1..72
    val taiyiPalaceNo: Int,    // 太乙所在宫 1..9（跳5）
    val inGongOffset: Int,     // 入宫以来 0..2
    val liTianDiRen: String,   // 理天/理地/理人
)

/** 格局条目。 */
data class TaiyiPattern(
    val name: String,
    val auspicious: Boolean,
    val basis: String,
    val duanYu: String = "",
)

/** 九宫单宫显示内容。 */
data class TaiyiPalaceCell(
    val palaceNo: Int,
    val occupants: List<String>,   // 落该宫的将/神标记，如 ["太乙","文昌","主大将"]
    val men: TaiyiMen?,            // 太乙盘八门落该宫者
    val tags: List<String> = emptyList(),
)

/** 卷四星宿（基星/福星/游神），显示层，不参与主客格局判定。依《太乙金镜式经》卷四。 */
data class TaiyiJiXing(
    val name: String,        // 君基/臣基/民基/五福/大游/小游
    val location: String,    // 所在：三基=地支名；五福/大游/小游=宫卦名（乾/艮/…）
    val palaceNo: Int?,      // 宫号（五福/大游/小游；三基走支填 null）
    val period: String,      // 周期说明（展示用）
    val inYear: Int,         // 入位以来年数（1-based）
    val auspicious: Boolean, // 五福/小游吉；大游凶；三基中吉
    val note: String = "",   // 占义/出处
)

/**
 * 太乙盘。八将 + 三算(主/客/定) + 大小将 + 定计 + 八门 + 格局 + 数分类 + 卷四星宿。
 * 计之支（太岁）随计法：年计→年支，月→月支，日→日支，时→时支。
 */
data class TaiyiPlate(
    val ju: TaiyiJu,
    val taiSui: EarthlyBranch,
    val heShen: EarthlyBranch,
    val jiShen: EarthlyBranch,
    val wenChang: SixteenGod,
    val shiJi: SixteenGod,
    val zhuSuan: Int,
    val keSuan: Int,
    val dingSuan: Int,
    val zhuDaJiang: Int,
    val zhuCanJiang: Int,
    val keDaJiang: Int,
    val keCanJiang: Int,
    val dingMu: SixteenGod,
    val dingDaJiang: Int,
    val dingCanJiang: Int,
    val zhiShiMen: TaiyiMen,              // 直使门
    val menOnTaiyi: Map<Int, TaiyiMen>,   // 太乙盘八门落宫（宫号→门）
    val patterns: List<TaiyiPattern>,
    val suanClass: List<String>,          // 数分类标签
    val jiXing: List<TaiyiJiXing> = emptyList(),  // 卷四星宿（P2）
) {
    val taiyiPalaceNo: Int get() = ju.taiyiPalaceNo
}

/** 分析条目语气（着色）。 */
enum class TaiyiTone { GOOD, BAD, NEUTRAL }

data class TaiyiAnalysisItem(
    val label: String,
    val text: String,
    val tone: TaiyiTone = TaiyiTone.NEUTRAL,
)
