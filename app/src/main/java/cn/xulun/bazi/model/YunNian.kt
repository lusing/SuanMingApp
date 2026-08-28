package cn.xulun.bazi.model

/** 流年细断条目（大运流年全面铺开——每流年的关系/合化/喜忌/格局）。 */
data class LiuNianDetail(
    val year: Int,
    /** 干支，如 癸酉 */
    val ganZhi: String,
    /** 干支十神，如 癸偏财·酉伤官 */
    val tenGods: String,
    /** 喜忌，如 干喜支忌 */
    val xiJi: String,
    /** 刑冲克害/三合三会/六合五合/暗合/合化成否 条目 */
    val relations: List<String>,
    /** 格局变化（冲合提纲/三合三会成局）；null=格局无显著变化 */
    val geJu: String?,
)

/** 一步大运的流年细断。 */
data class YunNianYun(
    val pillar: Pillar,
    val label: String,
    val years: List<LiuNianDetail>,
)

/** 流年细断结果。 */
data class YunNianResult(
    /** 喜忌口径依据（LiuQinXiJi 共享派生） */
    val xiJiBasis: String,
    val yuns: List<YunNianYun>,
)
