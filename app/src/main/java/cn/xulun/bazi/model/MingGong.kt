package cn.xulun.bazi.model

/**
 * 命宫/胎元/小限/小运结果。理论见 `.shengke-reference.md` 第廿二~廿四节。
 *
 * - 命宫：26−(月数+时数)得支数（寅=1…丑=12，过本月中气月数+1），年上起月法配干；可视为第五柱。
 * - 胎元：月干进一位、月支进三位（怀胎十月之意）。
 * - 小限：命宫起，一年一换，60 甲子序逐年 −1，立春交接。
 * - 小运：时柱起，一年一换（满周岁交脱），阳男阴女顺排、阴男阳女逆排。
 */
data class MingGongResult(
    /** 命宫柱 */
    val mingGong: Pillar,
    /** 命宫星名，如"天贵星" */
    val starName: String,
    /** 命宫星断语 */
    val starText: String,
    /** 命宫推算说明（含中气修正与否） */
    val mingGongBasis: String,
    /** 胎元柱 */
    val taiYuan: Pillar,
    /** 小限所示流年（[analyze] 的 flowYear 或出生年） */
    val xiaoXianYear: Int,
    /** 该年小限柱 */
    val xiaoXian: Pillar,
    /** 小运所示虚岁 */
    val xiaoYunAge: Int,
    /** 该岁小运柱 */
    val xiaoYun: Pillar,
    /** 小运顺逆（阳男阴女顺） */
    val xiaoYunForward: Boolean,
) {
    override fun toString(): String =
        "命宫${mingGong.fullName}（$starName）胎元${taiYuan.fullName}小限${xiaoXian.fullName}小运${xiaoYun.fullName}"
}
