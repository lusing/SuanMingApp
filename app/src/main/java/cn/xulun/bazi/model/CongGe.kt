package cn.xulun.bazi.model

/** 从格种类。 */
enum class CongGeKind { CONG_STRONG, CONG_WEAK }

/** 从格检测结果（粗判）。null 表示非从格。只影响「运喜」取向，不改强弱档位。 */
data class CongGeResult(val kind: CongGeKind, val basis: String)
