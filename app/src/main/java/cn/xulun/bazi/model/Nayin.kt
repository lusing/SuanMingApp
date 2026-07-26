package cn.xulun.bazi.model

import java.lang.Math.floorMod

/**
 * 六十甲子纳音（30 项，每项覆盖 2 个相邻六十甲子序号）。
 * 序号 0..1 = 海中金，2..3 = 炉中火，以此类推。
 */
enum class Nayin(val cycleIndex: Int, val chinese: String, val element: Element) {
    HAI_ZHONG_JIN(0, "海中金", Element.METAL),   // 甲子、乙丑
    LU_ZHONG_HUO(1, "炉中火", Element.FIRE),      // 丙寅、丁卯
    DA_LIN_MU(2, "大林木", Element.WOOD),          // 戊辰、己巳
    LU_PANG_TU(3, "路旁土", Element.EARTH),        // 庚午、辛未
    JIAN_FENG_JIN(4, "剑锋金", Element.METAL),     // 壬申、癸酉
    SHAN_TOU_HUO(5, "山头火", Element.FIRE),       // 甲戌、乙亥
    JIAN_XIA_SHUI(6, "涧下水", Element.WATER),     // 丙子、丁丑
    CHENG_TOU_TU(7, "城头土", Element.EARTH),      // 戊寅、己卯
    BAI_LA_JIN(8, "白蜡金", Element.METAL),        // 庚辰、辛巳
    YANG_LIU_MU(9, "杨柳木", Element.WOOD),        // 壬午、癸未
    QUAN_ZHONG_SHUI(10, "泉中水", Element.WATER),  // 甲申、乙酉
    WU_SHANG_TU(11, "屋上土", Element.EARTH),      // 丙戌、丁亥
    PI_LEI_HUO(12, "霹雳火", Element.FIRE),        // 戊子、己丑
    SONG_BO_MU(13, "松柏木", Element.WOOD),        // 庚寅、辛卯
    CHANG_LIU_SHUI(14, "长流水", Element.WATER),   // 壬辰、癸巳
    SHA_ZHONG_JIN(15, "沙中金", Element.METAL),    // 甲午、乙未
    SHAN_XIA_HUO(16, "山下火", Element.FIRE),      // 丙申、丁酉
    PING_DI_MU(17, "平地木", Element.WOOD),         // 戊戌、己亥
    BI_SHANG_TU(18, "壁上土", Element.EARTH),       // 庚子、辛丑
    JIN_BO_JIN(19, "金箔金", Element.METAL),        // 壬寅、癸卯
    FU_DENG_HUO(20, "覆灯火", Element.FIRE),        // 甲辰、乙巳
    TIAN_HE_SHUI(21, "天河水", Element.WATER),      // 丙午、丁未
    DA_YI_TU(22, "大驿土", Element.EARTH),          // 戊申、己酉
    CHA_CHUAN_JIN(23, "钗钏金", Element.METAL),     // 庚戌、辛亥
    SANG_ZHE_MU(24, "桑柘木", Element.WOOD),        // 壬子、癸丑
    DA_XI_SHUI(25, "大溪水", Element.WATER),        // 甲寅、乙卯
    SHA_ZHONG_TU(26, "沙中土", Element.EARTH),      // 丙辰、丁巳
    TIAN_SHANG_HUO(27, "天上火", Element.FIRE),     // 戊午、己未
    SHI_LIU_MU(28, "石榴木", Element.WOOD),         // 庚申、辛酉
    DA_HAI_SHUI(29, "大海水", Element.WATER);       // 壬戌、癸亥

    companion object {
        /** 根据六十甲子序号取纳音（每两个相邻序号共用同一纳音） */
        fun fromCycleIndex(index: Int): Nayin {
            val normalized = floorMod(index, 60)
            return entries[normalized / 2]
        }
    }
}
