package cn.xulun.bazi.model

/** 取运判定：干支各 XI/JI/PING，综合含参半/一喜一平。 */
enum class QuYunVerdict { XI, JI, PING, JI_ZHONG_XI, XI_ZHONG_PING }

/** 本命子配置（用神+相神+救应所成之局）。 */
data class ZiPeiZhi(
    val familyCh: String,   // 如 "印绶" / "正官" / "未定格"
    val subKey: String,     // 子配置键，如 "GUAN_SHA_JING"
    val subName: String,    // 如 "印格官煞竞透（取清）"
    val note: String,
)

/** 运柱单边（干或支）的喜忌判定。 */
data class PillarVerdict(val godCh: String, val verdict: QuYunVerdict, val reason: String)

data class QuYunSummary(val verdict: QuYunVerdict, val text: String)

/** 命中的取运表条目（喜忌理由）。 */
data class QuYunRow(val name: String, val xiText: String, val jiText: String, val note: String)

/** 取运分析结果（论 X 取运：运柱 × 本命子配置 → 干支喜忌）。 */
data class QuYunResult(
    val ziPeiZhi: ZiPeiZhi,
    val gan: PillarVerdict,
    val zhi: PillarVerdict,
    val summary: QuYunSummary,
    val row: QuYunRow?,
)
