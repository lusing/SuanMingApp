package cn.xulun.bazi.model

/** 单柱落空亡标记。 */
data class KongWangMark(val pillarIdx: Int, val branch: EarthlyBranch)

/** 八字旬空/空亡分析结果（以日柱旬为基准，遍历四柱）。 */
data class KongWangResult(val marks: List<KongWangMark>) {
    /** 第 [pillarIdx] 柱（0=年 1=月 2=日 3=时）是否落空亡 */
    fun isKong(pillarIdx: Int): Boolean = marks.any { it.pillarIdx == pillarIdx }
}
